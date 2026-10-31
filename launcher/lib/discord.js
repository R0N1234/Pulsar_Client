// Sinal Discord: mostra no perfil do Discord que a pessoa está jogando com o Pulsar.
// Fala direto com o Discord aberto no PC (o "IPC" local dele), sem biblioteca extra e sem internet.
// Se o Discord estiver fechado, não faz nada e tenta de novo na próxima atualização.
const net = require("net");
const path = require("path");
const crypto = require("crypto");

// Aplicativo "Pulsar Client" no portal de desenvolvedores do Discord (o ID é público).
const CLIENT_ID = "1566134732822020236";
const OP = { HANDSHAKE: 0, FRAME: 1, CLOSE: 2, PING: 3, PONG: 4 };
const ESPERA_RECONECTAR = 15000;

function caminhos() {
  if (process.platform === "win32") return Array.from({ length: 10 }, (_, i) => `\\\\?\\pipe\\discord-ipc-${i}`);
  const base = process.env.XDG_RUNTIME_DIR || process.env.TMPDIR || process.env.TMP || process.env.TEMP || "/tmp";
  const pastas = [base, path.join(base, "app", "com.discordapp.Discord"), path.join(base, "snap.discord")];
  return pastas.flatMap((p) => Array.from({ length: 10 }, (_, i) => path.join(p, `discord-ipc-${i}`)));
}

function pacote(op, dados) {
  const json = Buffer.from(JSON.stringify(dados), "utf8");
  const cab = Buffer.alloc(8);
  cab.writeInt32LE(op, 0);
  cab.writeInt32LE(json.length, 4);
  return Buffer.concat([cab, json]);
}

function abrir(caminho) {
  return new Promise((resolve, reject) => {
    const s = net.createConnection(caminho);
    const falhou = (e) => {
      s.destroy();
      reject(e);
    };
    s.once("error", falhou);
    s.once("connect", () => {
      s.off("error", falhou);
      resolve(s);
    });
  });
}

class SinalDiscord {
  constructor(log = () => {}) {
    this.log = log;
    this.sock = null;
    this.pronto = false;
    this.conectando = null;
    this.ultimaTentativa = 0;
    this.atividade = null;
    this.buffer = Buffer.alloc(0);
  }

  async conectar() {
    if (this.pronto) return true;
    if (this.conectando) return this.conectando;
    if (Date.now() - this.ultimaTentativa < ESPERA_RECONECTAR) return false;
    this.ultimaTentativa = Date.now();
    this.conectando = (async () => {
      for (const c of caminhos()) {
        let s;
        try {
          s = await abrir(c);
        } catch {
          continue;
        }
        this.sock = s;
        this.buffer = Buffer.alloc(0);
        s.on("data", (d) => this.receber(d));
        s.on("error", () => this.fechou());
        s.on("close", () => this.fechou());
        const pronto = new Promise((resolve) => (this.aoFicarPronto = resolve));
        s.write(pacote(OP.HANDSHAKE, { v: 1, client_id: CLIENT_ID }));
        const ok = await Promise.race([pronto, new Promise((r) => setTimeout(() => r(false), 5000))]);
        if (ok) return true;
        this.fechar();
      }
      return false;
    })().finally(() => (this.conectando = null));
    return this.conectando;
  }

  receber(dados) {
    this.buffer = Buffer.concat([this.buffer, dados]);
    while (this.buffer.length >= 8) {
      const op = this.buffer.readInt32LE(0);
      const tam = this.buffer.readInt32LE(4);
      if (this.buffer.length < 8 + tam) return;
      let msg = {};
      try {
        msg = JSON.parse(this.buffer.subarray(8, 8 + tam).toString("utf8"));
      } catch {}
      this.buffer = this.buffer.subarray(8 + tam);
      if (op === OP.PING) this.sock && this.sock.write(pacote(OP.PONG, msg));
      else if (op === OP.CLOSE) this.fechar();
      else if (msg.evt === "READY") {
        this.pronto = true;
        this.log("[Pulsar] Sinal Discord: conectado ao Discord");
        if (this.aoFicarPronto) this.aoFicarPronto(true);
      } else if (msg.evt === "ERROR") {
        this.log(`[Pulsar] Sinal Discord: o Discord recusou (${(msg.data && msg.data.message) || "sem motivo"})`);
      }
    }
  }

  fechou() {
    this.pronto = false;
    this.sock = null;
  }

  fechar() {
    const s = this.sock;
    this.fechou();
    if (s) {
      try {
        s.end(pacote(OP.CLOSE, {}));
      } catch {
        s.destroy();
      }
    }
  }

  enviarAtividade() {
    if (!this.pronto || !this.sock) return;
    const args = { pid: process.pid };
    if (this.atividade) args.activity = this.atividade;
    this.sock.write(pacote(OP.FRAME, { cmd: "SET_ACTIVITY", args, nonce: crypto.randomUUID() }));
  }

  // atividade: { detalhes, estado, inicio (ms), versao } ou null para apagar.
  async mostrar(a) {
    this.atividade = a
      ? {
          details: String(a.detalhes || "").slice(0, 128),
          state: String(a.estado || "").slice(0, 128) || undefined,
          timestamps: a.inicio ? { start: Math.floor(a.inicio) } : undefined,
          assets: { large_image: "pulsar", large_text: `Pulsar Client ${a.versao || ""}`.trim() },
          instance: false,
        }
      : null;
    if (await this.conectar()) this.enviarAtividade();
  }

  async apagar() {
    this.atividade = null;
    if (this.pronto) this.enviarAtividade();
    this.fechar();
  }
}

module.exports = { SinalDiscord, CLIENT_ID };
