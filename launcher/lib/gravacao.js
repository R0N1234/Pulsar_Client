// Gravação de vídeo do jogo, como no Lunar Client: o mod Gravação pede pela ponte e o launcher grava.
// Quem captura é uma janela escondida do launcher (src/gravador.html), com a captura de tela do Chromium:
// ela pega a janela do Minecraft (ou a tela inteira), o som do computador e, se pedido, o microfone,
// e manda os pedaços do vídeo para cá, que grava em Vídeos/Pulsar Client.
//
// Pedidos: {acao: "iniciar" | "parar" | "clipe" | "buffer" | "pasta", clipe, qualidade, fps, som, microfone, ligado}
// Resposta: {gravando, desde, guardando, arquivo, pasta}
const fs = require("fs");
const path = require("path");

const ESPERA_MS = 20000;

class Gravacao {
  // janela(): cria a janela escondida do gravador; app: o app do Electron; abrirPasta(p): abre a pasta para a pessoa.
  constructor({ app, BrowserWindow, desktopCapturer, ipcMain, shell, preload, html, log }) {
    this.app = app;
    this.BrowserWindow = BrowserWindow;
    this.desktopCapturer = desktopCapturer;
    this.ipcMain = ipcMain;
    this.shell = shell;
    this.preload = preload;
    this.html = html;
    this.log = log || (() => {});
    this.win = null;
    this.pronta = null;
    this.estado = { gravando: false, desde: 0, guardando: false };
    this.pendentes = new Map();
    this.arquivos = new Map();
    this.salvos = new Map();
    this.seq = 0;
    this.ipcMain.on("gravar:resposta", (_e, r) => this.resposta(r));
    this.ipcMain.on("gravar:pedaco", (_e, p) => this.pedaco(p));
    this.ipcMain.on("gravar:log", (_e, texto) => this.log(`[Pulsar] Gravação: ${texto}`));
  }

  pasta() {
    let base;
    try {
      base = this.app.getPath("videos");
    } catch {
      base = this.app.getPath("home");
    }
    const p = path.join(base, "Pulsar Client");
    fs.mkdirSync(p, { recursive: true });
    return p;
  }

  // A janela do gravador fica escondida e só existe enquanto precisar (gravando ou guardando o clipe).
  async janela() {
    if (this.win && !this.win.isDestroyed()) {
      await this.pronta;
      return this.win;
    }
    this.win = new this.BrowserWindow({
      show: false,
      width: 320,
      height: 200,
      webPreferences: { preload: this.preload, contextIsolation: true, sandbox: true, backgroundThrottling: false },
    });
    this.pronta = this.win.loadFile(this.html);
    this.win.on("closed", () => {
      this.win = null;
      this.estado = { gravando: false, desde: 0, guardando: false };
    });
    await this.pronta;
    return this.win;
  }

  fecharJanela() {
    if (this.win && !this.win.isDestroyed()) this.win.close();
    this.win = null;
  }

  // A janela do Minecraft entre as fontes de captura; se não achar, a tela principal.
  async fonte() {
    const fontes = await this.desktopCapturer.getSources({ types: ["window", "screen"], thumbnailSize: { width: 0, height: 0 } });
    const telas = fontes.filter((f) => f.id.startsWith("screen:"));
    const jogo = fontes.find((f) => f.id.startsWith("window:") && /minecraft/i.test(f.name));
    const escolhida = jogo || telas[0] || fontes[0];
    if (!escolhida) throw new Error("Não achei nenhuma janela ou tela para gravar.");
    return { id: escolhida.id, nome: escolhida.name, telaId: telas[0] ? telas[0].id : "" };
  }

  // Manda o pedido para a janela do gravador e espera a resposta dela.
  async pedir(corpo) {
    const acao = String(corpo.acao || "");
    if (acao === "pasta") {
      this.shell.openPath(this.pasta());
      return { ...this.estado, pasta: this.pasta() };
    }
    if (!["iniciar", "parar", "clipe", "buffer"].includes(acao)) throw new Error("Pedido de gravação desconhecido.");
    if (acao === "parar" && !this.estado.gravando) return { ...this.estado, pasta: this.pasta() };
    if (acao === "buffer" && !corpo.ligado && !this.estado.guardando) return { ...this.estado, pasta: this.pasta() };
    const win = await this.janela();
    const id = ++this.seq;
    const opts = {
      clipe: Math.max(5, Math.min(300, Number(corpo.clipe) || 30)),
      qualidade: String(corpo.qualidade || "1080p"),
      fps: Number(corpo.fps) === 60 ? 60 : 30,
      som: corpo.som !== false,
      microfone: !!corpo.microfone,
      ligado: !!corpo.ligado,
    };
    const precisaFonte = acao === "iniciar" || (acao === "buffer" && opts.ligado) || acao === "clipe";
    const fonte = precisaFonte ? await this.fonte() : null;
    const nome = acao === "clipe" ? this.nomeArquivo("Clipe") : acao === "parar" ? this.nomeArquivo("Pulsar") : "";
    const resposta = new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pendentes.delete(id);
        reject(new Error("O gravador não respondeu."));
      }, ESPERA_MS);
      this.pendentes.set(id, { resolve, reject, timer, nome });
    });
    win.webContents.send("gravar:comando", { id, acao, fonte, opts, nome });
    const r = await resposta;
    this.estado = { gravando: !!r.gravando, desde: Number(r.desde) || 0, guardando: !!r.guardando };
    if (!this.estado.gravando && !this.estado.guardando) this.fecharJanela();
    return { ...this.estado, arquivo: r.arquivo, pasta: this.pasta() };
  }

  nomeArquivo(prefixo) {
    const d = new Date();
    const dois = (n) => String(n).padStart(2, "0");
    return `${prefixo} ${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())} ${dois(d.getHours())}-${dois(d.getMinutes())}-${dois(d.getSeconds())}`;
  }

  // Os pedaços do vídeo chegam em ordem; o último traz fim=true.
  pedaco({ id, extensao, dados, fim }) {
    let a = this.arquivos.get(id);
    if (!a) {
      const p = this.pendentes.get(id);
      const nome = (p && p.nome) || this.nomeArquivo("Pulsar");
      const caminho = path.join(this.pasta(), `${nome}.${extensao || "webm"}`);
      a = { caminho, stream: fs.createWriteStream(caminho), bytes: 0 };
      this.arquivos.set(id, a);
    }
    if (dados && dados.byteLength) {
      a.stream.write(Buffer.from(dados));
      a.bytes += dados.byteLength;
    }
    if (fim) {
      a.stream.end();
      this.arquivos.delete(id);
      this.salvos.set(id, a);
      this.log(`[Pulsar] Gravação: salvo ${a.caminho} (${Math.round(a.bytes / 1024)} KB)`);
    }
  }

  resposta(r) {
    const p = this.pendentes.get(r.id);
    if (!p) return;
    clearTimeout(p.timer);
    this.pendentes.delete(r.id);
    if (r.ok === false) return p.reject(new Error(r.erro || "A gravação falhou."));
    // Os pedaços chegam antes da resposta; o arquivo só é entregue depois de gravado por inteiro no disco.
    const a = this.salvos.get(r.id) || this.arquivos.get(r.id);
    this.salvos.delete(r.id);
    if (!a) return p.resolve(r);
    if (a.stream.writableFinished) return p.resolve({ ...r, arquivo: a.caminho });
    a.stream.once("finish", () => p.resolve({ ...r, arquivo: a.caminho }));
    a.stream.once("error", (e) => p.reject(e));
  }

  // Ao fechar o jogo (ou o launcher), o que estiver gravando é salvo.
  async parar() {
    if (!this.win || this.win.isDestroyed()) return;
    try {
      if (this.estado.gravando) await this.pedir({ acao: "parar" });
      if (this.estado.guardando) await this.pedir({ acao: "buffer", ligado: false });
    } catch (e) {
      this.log(`[Pulsar] Gravação: ${e.message}`);
    }
    this.fecharJanela();
  }
}

module.exports = { Gravacao };
