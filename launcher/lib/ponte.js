// Ponte entre o jogo e o launcher, para trocar de conta pelo menu do Minecraft.
// Só escuta no próprio computador (127.0.0.1) e exige uma chave nova a cada vez que o jogo abre.
//   POST /conta/trocar   abre o login da Microsoft e devolve a sessão nova
//   POST /conta/lista    contas do launcher (a Microsoft e os nicks offline)
//   POST /conta/usar     {"conta": "microsoft" | nick} e devolve a sessão dessa conta
//   POST /conta/offline  {"nick": "..."} cria o nick offline e devolve a sessão dele
const http = require("http");
const crypto = require("crypto");

function lerCorpo(req) {
  return new Promise((resolve) => {
    let dados = "";
    req.setEncoding("utf8");
    req.on("data", (c) => {
      dados += c;
      if (dados.length > 4096) req.destroy();
    });
    req.on("end", () => {
      try {
        resolve(dados ? JSON.parse(dados) : {});
      } catch {
        resolve({});
      }
    });
    req.on("error", () => resolve({}));
  });
}

function startBridge({ onSwitchAccount, onList, onUse, onAddOffline }) {
  const key = crypto.randomBytes(24).toString("hex");
  let busy = false;
  const server = http.createServer(async (req, res) => {
    const reply = (code, body) => {
      res.writeHead(code, { "Content-Type": "application/json; charset=utf-8" });
      res.end(JSON.stringify(body));
    };
    if (req.headers["x-pulsar-chave"] !== key) return reply(403, { ok: false, erro: "Chave inválida." });
    if (req.method !== "POST") return reply(404, { ok: false, erro: "Pedido desconhecido." });
    const corpo = await lerCorpo(req);
    const responder = async (fn) => {
      try {
        reply(200, { ok: true, ...(await fn()) });
      } catch (err) {
        reply(200, { ok: false, erro: String((err && err.message) || err) });
      }
    };
    if (req.url === "/conta/lista" && onList) return responder(() => onList());
    if (req.url === "/conta/usar" && onUse) return responder(() => onUse(String(corpo.conta || "")));
    if (req.url === "/conta/offline" && onAddOffline) return responder(() => onAddOffline(String(corpo.nick || "")));
    if (req.url !== "/conta/trocar") return reply(404, { ok: false, erro: "Pedido desconhecido." });
    if (busy) return reply(409, { ok: false, erro: "A janela de login já está aberta." });
    busy = true;
    try {
      await responder(() => onSwitchAccount());
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
