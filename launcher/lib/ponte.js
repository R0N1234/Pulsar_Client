// Ponte entre o jogo e o launcher, para trocar de conta Microsoft pelo menu do Minecraft.
// Só escuta no próprio computador (127.0.0.1) e exige uma chave nova a cada vez que o jogo abre.
const http = require("http");
const crypto = require("crypto");

function startBridge({ onSwitchAccount }) {
  const key = crypto.randomBytes(24).toString("hex");
  let busy = false;
  const server = http.createServer(async (req, res) => {
    const reply = (code, body) => {
      res.writeHead(code, { "Content-Type": "application/json; charset=utf-8" });
      res.end(JSON.stringify(body));
    };
    if (req.headers["x-pulsar-chave"] !== key) return reply(403, { ok: false, erro: "Chave inválida." });
    if (req.method !== "POST" || req.url !== "/conta/trocar") return reply(404, { ok: false, erro: "Pedido desconhecido." });
    if (busy) return reply(409, { ok: false, erro: "A janela de login já está aberta." });
    busy = true;
    try {
      reply(200, { ok: true, ...(await onSwitchAccount()) });
    } catch (err) {
      reply(200, { ok: false, erro: String((err && err.message) || err) });
    } finally {
      busy = false;
    }
  });
  // O login pode levar alguns minutos; o jogo espera a resposta sem prazo curto.
  server.requestTimeout = 0;
  server.headersTimeout = 0;
  return new Promise((resolve, reject) => {
    server.on("error", reject);
    server.listen(0, "127.0.0.1", () => resolve({ port: server.address().port, key, close: () => server.close() }));
  });
}

module.exports = { startBridge };
