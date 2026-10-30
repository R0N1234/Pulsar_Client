// Aba Texturas do launcher: os 10 pacotes escolhidos pelo Pulsar (texturas.json, o mesmo catálogo do mod).
// Baixa do Modrinth para .minecraft/resourcepacks (conferindo o SHA-1) e liga o pacote no options.txt
// da instância do Pulsar, por cima dos outros pacotes do jogador.
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { instanceDir, prepareInstance } = require("./instance");
const CATALOGO = require("./texturas.json");

function pastaPacotes(gameDir) {
  return path.join(gameDir, "resourcepacks");
}

function opcoes(gameDir) {
  return path.join(instanceDir(gameDir), "options.txt");
}

function baixado(gameDir, p) {
  try {
    return fs.statSync(path.join(pastaPacotes(gameDir), p.arquivo)).size === p.tamanho;
  } catch {
    return false;
  }
}

// Linha "resourcePacks:[...]" do options.txt do jogo (lista em JSON, o último fica por cima).
function lerAtivos(gameDir) {
  try {
    const linha = fs.readFileSync(opcoes(gameDir), "utf8").split(/\r?\n/).find((l) => l.startsWith("resourcePacks:"));
    const lista = linha ? JSON.parse(linha.slice("resourcePacks:".length)) : [];
    return Array.isArray(lista) ? lista : [];
  } catch {
    return [];
  }
}

function salvarAtivos(gameDir, lista) {
  // Antes do primeiro jogo a instância ainda não tem options.txt: ela é criada agora, com as opções da .minecraft.
  prepareInstance(gameDir);
  const arquivo = opcoes(gameDir);
  let linhas = [];
  try {
    linhas = fs.readFileSync(arquivo, "utf8").split(/\r?\n/);
  } catch {}
  const nova = "resourcePacks:" + JSON.stringify(lista);
  const i = linhas.findIndex((l) => l.startsWith("resourcePacks:"));
  if (i >= 0) linhas[i] = nova;
  else linhas.splice(linhas.length && linhas[linhas.length - 1] === "" ? linhas.length - 1 : linhas.length, 0, nova);
  fs.mkdirSync(path.dirname(arquivo), { recursive: true });
  fs.writeFileSync(arquivo, linhas.join("\n"));
}

function listar(gameDir) {
  const ativos = lerAtivos(gameDir);
  return CATALOGO.map((p) => ({
    id: p.id, nome: p.nome, tipo: p.tipo, desc: p.desc, resolucao: p.resolucao, tamanho: p.tamanho,
    icone: p.icone, pagina: p.pagina,
    baixado: baixado(gameDir, p),
    emUso: ativos.includes(p.arquivo),
  }));
}

function pacote(id) {
  const p = CATALOGO.find((x) => x.id === id);
  if (!p) throw new Error("Pacote desconhecido.");
  return p;
}

const baixando = new Set();

async function baixar(gameDir, id, onProgress) {
  const p = pacote(id);
  if (baixando.has(id)) return;
  baixando.add(id);
  const destino = path.join(pastaPacotes(gameDir), p.arquivo);
  const parte = destino + ".part";
  try {
    fs.mkdirSync(path.dirname(destino), { recursive: true });
    const res = await fetch(p.url, { headers: { "User-Agent": "PulsarClient (texturas)" } });
    if (!res.ok || !res.body) throw new Error(`O Modrinth respondeu ${res.status}.`);
    const hash = crypto.createHash("sha1");
    const out = fs.createWriteStream(parte);
    let lidos = 0;
    let ultimo = -1;
    for await (const pedaco of res.body) {
      hash.update(pedaco);
      lidos += pedaco.length;
      if (!out.write(pedaco)) await new Promise((r) => out.once("drain", r));
      const pct = Math.min(99, Math.floor((lidos * 100) / p.tamanho));
      if (pct !== ultimo) onProgress((ultimo = pct));
    }
    await new Promise((r, j) => out.end((e) => (e ? j(e) : r())));
    if (hash.digest("hex") !== p.sha1) throw new Error("O arquivo chegou diferente do esperado. Tente de novo.");
    fs.renameSync(parte, destino);
  } catch (e) {
    try {
      fs.rmSync(parte, { force: true });
    } catch {}
    throw e;
  } finally {
    baixando.delete(id);
  }
}

// Liga um pacote do catálogo (tirando os outros do catálogo) ou desliga. Os pacotes do jogador ficam como estão.
function usar(gameDir, id, ligar) {
  const p = pacote(id);
  if (ligar && !baixado(gameDir, p)) throw new Error("Baixe o pacote antes de usar.");
  const nossos = new Set(CATALOGO.map((x) => x.arquivo));
  const lista = lerAtivos(gameDir).filter((n) => !nossos.has(n));
  if (ligar) lista.push(p.arquivo);
  salvarAtivos(gameDir, lista);
}

function remover(gameDir, id) {
  const p = pacote(id);
  usar(gameDir, id, false);
  fs.rmSync(path.join(pastaPacotes(gameDir), p.arquivo), { force: true });
}

module.exports = { listar, baixar, usar, remover, pastaPacotes };
