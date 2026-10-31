// Rede do Pulsar: quem está usando o Pulsar agora (ícone PC no nome), nível por tempo de jogo e amigos com chat.
// Um Worker da Cloudflare com um Durable Object só ("global"), que guarda tudo num SQLite pequeno.
//
// Contas originais entram como um servidor do Minecraft faz: o jogo avisa a Mojang (joinServer) e a rede
// confere (hasJoined). Assim ninguém se passa por outra conta original, sem senha nenhuma passando por aqui.
// Contas offline (pirata) só aparecem com o ícone e o nível: amigos e chat ficam para contas originais.
//
//   POST /entrar   {nome, serverId}                 → {token, uuid, nome}
//   POST /pulso    {token, servidor, ver: [uuids], oculto}  → nível, quem usa Pulsar entre os "ver", amigos, pedidos e mensagens
//   POST /offline  {nome, ver: [uuids], oculto}             → o mesmo, para conta offline (sem amigos)
//   (oculto: a pessoa não aparece como online para os outros, mas continua vendo os ícones)
//   POST /amigos/pedir      {token, nome}
//   POST /amigos/responder  {token, uuid, aceitar}
//   POST /amigos/remover    {token, uuid}
//   POST /chat              {token, para, texto}
import { DurableObject } from "cloudflare:workers";

const ONLINE_MS = 3 * 60 * 1000;
const SESSAO_MS = 7 * 24 * 60 * 60 * 1000;
const MAX_VER = 200;
const MAX_AMIGOS = 100;
const MAX_MENSAGENS = 100;
const NICK = /^[A-Za-z0-9_]{3,16}$/;
const UUID = /^[0-9a-f]{32}$/;

const json = (dados, status = 200) =>
  new Response(JSON.stringify(dados), { status, headers: { "content-type": "application/json; charset=utf-8" } });
const erro = (texto, status = 200) => json({ ok: false, erro: texto }, status);
const semTraco = (u) => String(u || "").toLowerCase().replace(/-/g, "");

// Nível pelo tempo de jogo com o Pulsar: 15 min → 2, 1 h → 3, 4 h → 5, 15 h → 8, 100 h → 21.
function nivel(minutos) {
  return 1 + Math.floor(Math.sqrt(Math.max(0, minutos) / 15));
}
function minutosDoNivel(n) {
  return (n - 1) * (n - 1) * 15;
}

// UUID de conta offline, igual ao dos servidores ("OfflinePlayer:<nick>", MD5 versão 3).
async function uuidOffline(nome) {
  const h = new Uint8Array(await crypto.subtle.digest("MD5", new TextEncoder().encode("OfflinePlayer:" + nome)));
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  return [...h].map((b) => b.toString(16).padStart(2, "0")).join("");
}

export default {
  async fetch(req, env) {
    if (req.method === "GET") return json({ ok: true, nome: "Pulsar Rede" });
    if (req.method !== "POST") return erro("Pedido desconhecido.", 405);
    return env.REDE.get(env.REDE.idFromName("global")).fetch(req);
  },
};

export class Rede extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.env = env;
    this.sql = ctx.storage.sql;
    // Quem está jogando agora fica só na memória: uuid → {visto, servidor, offline}
    this.online = new Map();
    // Limite simples por IP: pedidos por minuto.
    this.pedidosIp = new Map();
    this.sql.exec(`CREATE TABLE IF NOT EXISTS jogadores (uuid TEXT PRIMARY KEY, nome TEXT, nome_min TEXT, minutos REAL DEFAULT 0, visto INTEGER DEFAULT 0, offline INTEGER DEFAULT 0)`);
    this.sql.exec(`CREATE INDEX IF NOT EXISTS jogadores_nome ON jogadores (nome_min)`);
    this.sql.exec(`CREATE TABLE IF NOT EXISTS sessoes (token TEXT PRIMARY KEY, uuid TEXT, expira INTEGER)`);
    this.sql.exec(`CREATE TABLE IF NOT EXISTS amizades (de TEXT, para TEXT, aceita INTEGER DEFAULT 0, PRIMARY KEY (de, para))`);
    this.sql.exec(`CREATE TABLE IF NOT EXISTS mensagens (id INTEGER PRIMARY KEY AUTOINCREMENT, de TEXT, para TEXT, texto TEXT, quando INTEGER)`);
    this.sql.exec(`CREATE INDEX IF NOT EXISTS mensagens_para ON mensagens (para)`);
  }

  async fetch(req) {
    const ip = req.headers.get("cf-connecting-ip") || "local";
    if (this.demais(ip)) return erro("Calma: pedidos demais em pouco tempo.", 429);
    let corpo;
    try {
      corpo = await req.json();
    } catch {
      return erro("Pedido inválido.", 400);
    }
    const caminho = new URL(req.url).pathname;
    try {
      switch (caminho) {
        case "/entrar": return json(await this.entrar(corpo));
        case "/pulso": return json(this.pulso(this.sessao(corpo.token), corpo));
        case "/offline": return json(await this.offline(corpo));
        case "/amigos/pedir": return json(this.pedir(this.sessao(corpo.token), corpo.nome));
        case "/amigos/responder": return json(this.responder(this.sessao(corpo.token), semTraco(corpo.uuid), !!corpo.aceitar));
        case "/amigos/remover": return json(this.remover(this.sessao(corpo.token), semTraco(corpo.uuid)));
        case "/chat": return json(this.chat(this.sessao(corpo.token), semTraco(corpo.para), corpo.texto));
        default: return erro("Pedido desconhecido.", 404);
      }
    } catch (e) {
      if (e instanceof Aviso) return erro(e.message);
      console.error(e);
      return erro("Erro na rede do Pulsar.", 500);
    }
  }

  demais(ip) {
    const agora = Date.now();
    const p = this.pedidosIp.get(ip);
    if (!p || agora - p.desde > 60000) {
      this.pedidosIp.set(ip, { desde: agora, n: 1 });
      if (this.pedidosIp.size > 5000) this.pedidosIp.clear();
      return false;
    }
    return ++p.n > 120;
  }

  // ---------- contas ----------

  async entrar({ nome, serverId }) {
    nome = String(nome || "");
    serverId = String(serverId || "");
    if (!NICK.test(nome) || !/^[0-9a-f-]{8,64}$/i.test(serverId)) throw new Aviso("Pedido de entrada inválido.");
    let perfil;
    if (this.env.TESTE_SEM_MOJANG === "1") perfil = { id: semTraco(serverId).padEnd(32, "0").slice(0, 32), name: nome };
    else {
      const r = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(nome)}&serverId=${encodeURIComponent(serverId)}`);
      if (r.status !== 200) throw new Aviso("A Mojang não confirmou a conta. Entre de novo pelo launcher.");
      perfil = await r.json();
    }
    const uuid = semTraco(perfil.id);
    if (!UUID.test(uuid)) throw new Aviso("A Mojang mandou uma conta inválida.");
    this.salvarJogador(uuid, perfil.name, false);
    const token = crypto.randomUUID().replace(/-/g, "") + crypto.randomUUID().replace(/-/g, "");
    const agora = Date.now();
    this.sql.exec(`DELETE FROM sessoes WHERE expira < ?`, agora);
    this.sql.exec(`INSERT INTO sessoes (token, uuid, expira) VALUES (?, ?, ?)`, token, uuid, agora + SESSAO_MS);
    return { ok: true, token, uuid, nome: perfil.name };
  }

  sessao(token) {
    const linha = this.sql.exec(`SELECT uuid FROM sessoes WHERE token = ? AND expira > ?`, String(token || ""), Date.now()).toArray()[0];
    if (!linha) throw new Aviso("sessao");
    return linha.uuid;
  }

  salvarJogador(uuid, nome, offline) {
    this.sql.exec(
      `INSERT INTO jogadores (uuid, nome, nome_min, offline) VALUES (?, ?, ?, ?)
       ON CONFLICT (uuid) DO UPDATE SET nome = excluded.nome, nome_min = excluded.nome_min`,
      uuid, nome, nome.toLowerCase(), offline ? 1 : 0,
    );
  }

  jogador(uuid) {
    return this.sql.exec(`SELECT uuid, nome, minutos FROM jogadores WHERE uuid = ?`, uuid).toArray()[0];
  }

  // ---------- presença e nível ----------

  marcarOnline(uuid, servidor, offline) {
    const agora = Date.now();
    const antes = this.online.get(uuid);
    // O tempo só conta entre pulsos seguidos (no máximo 3 minutos por pulso).
    if (antes && agora - antes.visto < ONLINE_MS) {
      this.sql.exec(`UPDATE jogadores SET minutos = minutos + ?, visto = ? WHERE uuid = ?`, (agora - antes.visto) / 60000, agora, uuid);
    } else {
      this.sql.exec(`UPDATE jogadores SET visto = ? WHERE uuid = ?`, agora, uuid);
    }
    this.online.set(uuid, { visto: agora, servidor: String(servidor || "").slice(0, 60), offline });
    if (this.online.size > 20000) this.limparOnline();
  }

  limparOnline() {
    const agora = Date.now();
    for (const [u, o] of this.online) if (agora - o.visto > ONLINE_MS) this.online.delete(u);
  }

  estaOnline(uuid) {
    const o = this.online.get(uuid);
    return !!o && Date.now() - o.visto < ONLINE_MS;
  }

  // Quem dos jogadores que a pessoa está vendo (TAB) usa o Pulsar agora, e o nível de cada um.
  quemUsa(ver) {
    const out = {};
    const lista = Array.isArray(ver) ? ver.slice(0, MAX_VER).map(semTraco).filter((u) => UUID.test(u)) : [];
    const online = lista.filter((u) => this.estaOnline(u));
    if (!online.length) return out;
    const marcas = online.map(() => "?").join(",");
    for (const j of this.sql.exec(`SELECT uuid, minutos FROM jogadores WHERE uuid IN (${marcas})`, ...online)) out[j.uuid] = nivel(j.minutos);
    return out;
  }

  meuNivel(uuid) {
    const j = this.jogador(uuid);
    const m = j ? j.minutos : 0;
    const n = nivel(m);
    return { nivel: n, minutos: Math.floor(m), proximo: minutosDoNivel(n + 1) };
  }

  pulso(uuid, { servidor, ver, oculto }) {
    if (oculto) this.online.delete(uuid);
    else this.marcarOnline(uuid, servidor, false);
    return { ok: true, ...this.meuNivel(uuid), pulsar: this.quemUsa(ver), ...this.social(uuid) };
  }

  async offline({ nome, ver, oculto }) {
    nome = String(nome || "");
    if (!NICK.test(nome)) throw new Aviso("Nick inválido.");
    const uuid = await uuidOffline(nome);
    this.salvarJogador(uuid, nome, true);
    if (oculto) this.online.delete(uuid);
    else this.marcarOnline(uuid, "", true);
    return { ok: true, uuid, ...this.meuNivel(uuid), pulsar: this.quemUsa(ver) };
  }

  // ---------- amigos ----------

  social(uuid) {
    const amigos = [];
    const pedidos = [];
    for (const a of this.sql.exec(
      `SELECT a.de, a.para, a.aceita, j.nome, j.minutos FROM amizades a JOIN jogadores j ON j.uuid = (CASE WHEN a.de = ? THEN a.para ELSE a.de END)
       WHERE a.de = ? OR a.para = ?`,
      uuid, uuid, uuid,
    )) {
      const outro = a.de === uuid ? a.para : a.de;
      if (a.aceita) {
        const o = this.online.get(outro);
        const on = this.estaOnline(outro);
        amigos.push({ uuid: outro, nome: a.nome, online: on, servidor: on && o ? o.servidor : "", nivel: nivel(a.minutos) });
      } else if (a.para === uuid) pedidos.push({ uuid: outro, nome: a.nome });
    }
    amigos.sort((x, y) => y.online - x.online || x.nome.localeCompare(y.nome));
    // Mensagens novas: entregues uma vez e apagadas daqui.
    const mensagens = this.sql
      .exec(`SELECT m.id, m.de, m.texto, m.quando, j.nome FROM mensagens m LEFT JOIN jogadores j ON j.uuid = m.de WHERE m.para = ? ORDER BY m.id LIMIT 50`, uuid)
      .toArray();
    if (mensagens.length) this.sql.exec(`DELETE FROM mensagens WHERE para = ? AND id <= ?`, uuid, mensagens[mensagens.length - 1].id);
    return { amigos, pedidos, mensagens: mensagens.map((m) => ({ de: m.de, nome: m.nome || "?", texto: m.texto, quando: m.quando })) };
  }

  amizade(a, b) {
    return this.sql.exec(`SELECT de, para, aceita FROM amizades WHERE (de = ? AND para = ?) OR (de = ? AND para = ?)`, a, b, b, a).toArray()[0];
  }

  pedir(uuid, nome) {
    nome = String(nome || "").trim();
    if (!NICK.test(nome)) throw new Aviso("Nick inválido.");
    const alvo = this.sql.exec(`SELECT uuid, nome FROM jogadores WHERE nome_min = ? AND offline = 0`, nome.toLowerCase()).toArray()[0];
    if (!alvo) throw new Aviso(`${nome} ainda não entrou no Pulsar com conta original.`);
    if (alvo.uuid === uuid) throw new Aviso("Esse é você.");
    const total = this.sql.exec(`SELECT COUNT(*) AS n FROM amizades WHERE de = ? OR para = ?`, uuid, uuid).one().n;
    if (total >= MAX_AMIGOS) throw new Aviso(`Dá para ter até ${MAX_AMIGOS} amigos e pedidos.`);
    const a = this.amizade(uuid, alvo.uuid);
    if (a && a.aceita) return { ok: true, aviso: `${alvo.nome} já é seu amigo.` };
    if (a && a.de === alvo.uuid) {
      this.sql.exec(`UPDATE amizades SET aceita = 1 WHERE de = ? AND para = ?`, alvo.uuid, uuid);
      return { ok: true, aviso: `Agora você e ${alvo.nome} são amigos.` };
    }
    if (!a) this.sql.exec(`INSERT INTO amizades (de, para, aceita) VALUES (?, ?, 0)`, uuid, alvo.uuid);
    return { ok: true, aviso: `Pedido mandado para ${alvo.nome}.` };
  }

  responder(uuid, outro, aceitar) {
    const a = this.amizade(uuid, outro);
    if (!a || a.aceita || a.para !== uuid) throw new Aviso("Esse pedido não existe mais.");
    if (aceitar) this.sql.exec(`UPDATE amizades SET aceita = 1 WHERE de = ? AND para = ?`, outro, uuid);
    else this.sql.exec(`DELETE FROM amizades WHERE de = ? AND para = ?`, outro, uuid);
    return { ok: true };
  }

  remover(uuid, outro) {
    this.sql.exec(`DELETE FROM amizades WHERE (de = ? AND para = ?) OR (de = ? AND para = ?)`, uuid, outro, outro, uuid);
    return { ok: true };
  }

  chat(uuid, para, texto) {
    texto = String(texto || "").replace(/[\u0000-\u001f§]/g, "").trim().slice(0, 200);
    if (!texto) throw new Aviso("Mensagem vazia.");
    const a = this.amizade(uuid, para);
    if (!a || !a.aceita) throw new Aviso("Só dá para mandar mensagem para amigos.");
    const n = this.sql.exec(`SELECT COUNT(*) AS n FROM mensagens WHERE para = ?`, para).one().n;
    if (n >= MAX_MENSAGENS) throw new Aviso("A caixa do seu amigo está cheia. Espere ele entrar.");
    this.sql.exec(`INSERT INTO mensagens (de, para, texto, quando) VALUES (?, ?, ?, ?)`, uuid, para, texto, Date.now());
    return { ok: true };
  }
}

class Aviso extends Error {}
