// Teste da rede do Pulsar com o Worker rodando no próprio PC (wrangler dev, sem Mojang de verdade).
// Uso: npm test   (sobe o wrangler dev sozinho na porta 8799)
import { spawn } from "node:child_process";
import assert from "node:assert/strict";

const BASE = "http://127.0.0.1:8799";
const dev = spawn("npx", ["wrangler", "dev", "--port", "8799", "--ip", "127.0.0.1", "--var", "TESTE_SEM_MOJANG:1", "--persist-to", ".teste-estado"], {
  cwd: new URL("..", import.meta.url).pathname,
  stdio: ["ignore", "pipe", "pipe"],
  detached: true,
  env: { ...process.env, WRANGLER_SEND_METRICS: "false", CI: "1" },
});
let saida = "";
dev.stdout.on("data", (d) => (saida += d));
dev.stderr.on("data", (d) => (saida += d));

const post = async (caminho, corpo) => (await fetch(BASE + caminho, { method: "POST", body: JSON.stringify(corpo) })).json();

async function esperar() {
  for (let i = 0; i < 120; i++) {
    try {
      if ((await fetch(BASE)).ok) return;
    } catch {}
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error("wrangler dev não subiu:\n" + saida);
}

try {
  await esperar();
  const id = (n) => String(n).repeat(32).slice(0, 32);
  const rick = await post("/entrar", { nome: "RickPvP", serverId: id(1) });
  const ana = await post("/entrar", { nome: "AnaPvP", serverId: id(2) });
  assert.ok(rick.ok && rick.token && rick.uuid === id(1), JSON.stringify(rick));
  assert.ok(ana.ok && ana.uuid === id(2));

  // Presença: Ana pulsa, Rick vê Ana como usuária do Pulsar (e um desconhecido não).
  await post("/pulso", { token: ana.token, servidor: "mc.exemplo.com", ver: [] });
  let r = await post("/pulso", { token: rick.token, servidor: "mc.exemplo.com", ver: [ana.uuid, id(9)] });
  assert.ok(r.ok && r.nivel === 1, JSON.stringify(r));
  assert.deepEqual(Object.keys(r.pulsar), [ana.uuid]);

  // Conta offline aparece também, pelo UUID de offline.
  const off = await post("/offline", { nome: "Pirata_1", ver: [ana.uuid] });
  assert.ok(off.ok && /^[0-9a-f]{32}$/.test(off.uuid) && off.uuid[12] === "3", JSON.stringify(off));
  assert.ok(off.pulsar[ana.uuid]);
  r = await post("/pulso", { token: rick.token, ver: [off.uuid] });
  assert.ok(r.pulsar[off.uuid], "offline visível");

  // Amigos: offline não pode ser adicionado; Ana sim.
  assert.equal((await post("/amigos/pedir", { token: rick.token, nome: "Pirata_1" })).ok, false);
  assert.equal((await post("/amigos/pedir", { token: rick.token, nome: "AnaPvP" })).ok, true);
  r = await post("/pulso", { token: ana.token, servidor: "mc.exemplo.com", ver: [] });
  assert.equal(r.pedidos.length, 1);
  assert.equal(r.pedidos[0].nome, "RickPvP");
  assert.equal((await post("/chat", { token: rick.token, para: ana.uuid, texto: "oi" })).ok, false, "chat antes de aceitar");
  assert.equal((await post("/amigos/responder", { token: ana.token, uuid: rick.uuid, aceitar: true })).ok, true);
  r = await post("/pulso", { token: rick.token, servidor: "mc.exemplo.com", ver: [] });
  assert.equal(r.amigos.length, 1);
  assert.equal(r.amigos[0].nome, "AnaPvP");
  assert.equal(r.amigos[0].online, true);
  assert.equal(r.amigos[0].servidor, "mc.exemplo.com");

  // Chat: entregue uma vez só, sem cores do Minecraft.
  assert.equal((await post("/chat", { token: rick.token, para: ana.uuid, texto: "§cbora x1?" })).ok, true);
  r = await post("/pulso", { token: ana.token, ver: [] });
  assert.deepEqual(r.mensagens.map((m) => [m.nome, m.texto]), [["RickPvP", "cbora x1?"]]);
  r = await post("/pulso", { token: ana.token, ver: [] });
  assert.equal(r.mensagens.length, 0);

  // Oculto: some da lista dos outros, mas continua recebendo.
  r = await post("/pulso", { token: ana.token, oculto: true, ver: [rick.uuid] });
  assert.ok(r.ok && r.pulsar[rick.uuid]);
  r = await post("/pulso", { token: rick.token, ver: [ana.uuid] });
  assert.equal(r.pulsar[ana.uuid], undefined, "Ana oculta");
  assert.equal(r.amigos[0].online, false);

  // Token inválido e remoção.
  assert.equal((await post("/pulso", { token: "x", ver: [] })).erro, "sessao");
  assert.equal((await post("/amigos/remover", { token: ana.token, uuid: rick.uuid })).ok, true);
  r = await post("/pulso", { token: rick.token, ver: [] });
  assert.equal(r.amigos.length, 0);
  console.log("OK: rede do Pulsar (entrar, presença, offline, nível, amigos, chat)");
} catch (e) {
  console.error("FALHOU:", e.message);
  process.exitCode = 1;
} finally {
  // O wrangler abre outros processos (workerd): fecha o grupo todo.
  try {
    process.kill(-dev.pid, "SIGTERM");
  } catch {}
}
