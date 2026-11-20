const { app, BrowserWindow, desktopCapturer, dialog, ipcMain, safeStorage, shell } = require("electron");
const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");
const { autoUpdater } = require("electron-updater");
const { Config } = require("./lib/config");
const { Accounts, describeError } = require("./lib/auth");
const { launch } = require("./lib/launch");
const { startBridge } = require("./lib/ponte");
const musica = require("./lib/musica");
const texturas = require("./lib/texturas");
const usermods = require("./lib/usermods");
const i18n = require("./src/i18n");
const { SinalDiscord } = require("./lib/discord");
const radio = require("./lib/radio");
const versoes = require("./lib/versoes");
const mcversoes = require("./lib/mcversoes");
const { Gravacao } = require("./lib/gravacao");
const optifine = require("./lib/optifine");
const vigiaOptifine = new optifine.Vigia();

// Janelas (inclusive o login da Microsoft) em português.
app.commandLine.appendSwitch("lang", "pt-BR");

const ICON = path.join(__dirname, "src", "logo.png");
// Idioma escolhido em Ajustes (vazio = o do sistema). As telas traduzem sozinhas; aqui só os diálogos.
const idioma = () => i18n.escolher((config && config.get().idioma) || app.getLocale());
const tr = (texto) => i18n.t(texto, idioma());

let win;
let consoleWin = null;
let config;
let accounts;
let game = null;
let launching = false;
let bridge = null;
let gravacao = null;

// Segredos (token da Microsoft, chave de atualização) ficam criptografados pelo sistema quando possível.
function encrypt(text) {
  if (!text) return "";
  const buf = safeStorage.isEncryptionAvailable() ? safeStorage.encryptString(text) : Buffer.from(text);
  return buf.toString("base64");
}

function decrypt(raw) {
  if (!raw) return "";
  try {
    const buf = Buffer.from(raw, "base64");
    return safeStorage.isEncryptionAvailable() ? safeStorage.decryptString(buf) : buf.toString();
  } catch {
    return "";
  }
}

function secretStore() {
  return {
    get() {
      const token = decrypt(config.get().msRefreshTokenEnc);
      return token ? { msRefreshToken: token } : {};
    },
    set({ msRefreshToken }) {
      config.set({ msRefreshTokenEnc: encrypt(msRefreshToken) });
    },
  };
}

function modSourceDir() {
  return app.isPackaged ? path.join(process.resourcesPath, "mods") : path.join(__dirname, "..", "mod", "build", "libs");
}

// A versão anterior do mod vem no instalador, em resources/anterior (no CI, mod/build/anterior).
function modAnteriorDir() {
  return app.isPackaged ? path.join(process.resourcesPath, "anterior") : path.join(__dirname, "..", "mod", "build", "anterior");
}

function listaVersoes() {
  const lista = versoes.listar(modSourceDir(), modAnteriorDir());
  const escolhida = versoes.escolher(lista, config.get().versaoJogo).qual;
  const sem = (v) => v && { versao: v.versao };
  return { atual: sem(lista.atual), anterior: sem(lista.anterior), escolhida };
}

function publicConfig() {
  const { msRefreshTokenEnc, updateTokenEnc, ...rest } = config.get();
  return { ...rest, hasUpdateToken: !!updateTokenEnc };
}

// Visual do launcher: "normal" (o modelo completo) ou "lite" (a tela leve). Escolhido em Ajustes, vale na hora.
const modoLauncher = () => (config && config.get().modoLauncher === "lite" ? "lite" : "normal");
const telaDoModo = () => (modoLauncher() === "lite" ? path.join(__dirname, "src", "index.html") : path.join(__dirname, "src", "normal", "index.html"));

function createWindow() {
  win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 640,
    backgroundColor: "#05070F",
    frame: false,
    title: "Pulsar Client",
    icon: ICON,
    // O Rádio Pulsar começa a tocar por pedido do jogo, sem clique na janela do launcher.
    webPreferences: { preload: path.join(__dirname, "preload.js"), contextIsolation: true, sandbox: true, autoplayPolicy: "no-user-gesture-required", backgroundThrottling: false },
  });
  win.loadFile(telaDoModo());
  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });
}

// Sinal Discord: enquanto o jogo está aberto, o perfil do Discord mostra "Pulsar Client" e onde a pessoa está.
// O mod Sinal Discord dentro do jogo avisa pela ponte quando muda (e quando é desligado).
const presenca = {
  discord: null,
  inicio: 0,
  ligado: false,
  estado: "No menu principal",
  timer: null,
  versao: "1.8.9",
  comecar(ligado, versao = "1.8.9") {
    this.discord = this.discord || new SinalDiscord(pushLog);
    this.versao = versao;
    this.inicio = Date.now();
    this.ligado = ligado;
    // Sem o mod do Pulsar (1.7.10, 1.16.5, 1.21) o jogo não avisa onde a pessoa está.
    this.estado = versao === "1.8.9" ? "No menu principal" : "Jogando";
    this.mostrar();
    // Se o Discord abrir depois do jogo, a próxima volta conecta.
    clearInterval(this.timer);
    this.timer = setInterval(() => this.ligado && !this.discord.pronto && this.mostrar(), 30000);
  },
  atualizar(p) {
    if (!this.discord || !p) return;
    const lugar = String(p.lugar || "menu");
    const ip = String(p.servidor || "").slice(0, 60);
    this.ligado = p.ligado !== false;
    this.estado = lugar === "sozinho" ? "Jogando sozinho" : lugar === "servidor" ? (ip ? `Jogando em ${ip}` : "Jogando em um servidor") : "No menu principal";
    this.mostrar();
  },
  mostrar() {
    if (!this.ligado) return this.discord.apagar();
    this.discord.mostrar({ detalhes: `Minecraft ${this.versao} · PvP`, estado: tr(this.estado), inicio: this.inicio, versao: app.getVersion() }).catch(() => {});
  },
  parar() {
    clearInterval(this.timer);
    this.timer = null;
    if (this.discord) this.discord.apagar();
  },
};

// Atualização automática: as versões saem nos Releases do repositório Pulsar_Client, que é público.
// Quem colou uma chave de leitura em Ajustes (da época em que o repositório era privado) continua usando ela.
const UPDATE_REPO = { provider: "github", owner: "R0N1234", repo: "Pulsar_Client" };
let updatesWired = false;
let updateTimer = null;

function checkUpdates() {
  if (!app.isPackaged) return;
  const token = decrypt(config.get().updateTokenEnc);
  autoUpdater.setFeedURL(token ? { ...UPDATE_REPO, private: true, token } : UPDATE_REPO);
  autoUpdater.checkForUpdates().catch((e) => {
    console.error("[updater]", e);
    send("update:status", { state: "error", text: "Não consegui procurar atualização agora. Tento de novo daqui a pouco." });
  });
}

// Texto das novidades da versão (o GitHub manda em HTML).
function notasDaVersao(info) {
  const bruto = Array.isArray(info.releaseNotes) ? info.releaseNotes.map((n) => n.note || "").join("\n") : String(info.releaseNotes || "");
  const texto = bruto
    .replace(/<br\s*\/?>|<\/(p|li|h\d)>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&#39;/g, "'")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
  return texto.length > 600 ? texto.slice(0, 597) + "..." : texto;
}

// Atualização baixada, esperando o jogador clicar em Atualizar.
let atualizacao = null;

function setupUpdates() {
  if (!app.isPackaged || updatesWired) return;
  updatesWired = true;
  autoUpdater.autoDownload = true;
  // Quem instala é a tela de atualização do Pulsar (PulsarAtualizador.exe), quando o jogador escolher.
  autoUpdater.autoInstallOnAppQuit = false;
  autoUpdater.on("update-available", (i) => send("update:status", { state: "downloading", version: i.version, percent: 0 }));
  autoUpdater.on("update-not-available", () => send("update:status", { state: "none" }));
  autoUpdater.on("download-progress", (p) => send("update:status", { state: "downloading", percent: Math.round(p.percent) }));
  autoUpdater.on("update-downloaded", (i) => {
    atualizacao = { version: i.version, arquivo: i.downloadedFile, notas: notasDaVersao(i) };
    send("update:status", { state: "ready", version: i.version, notas: atualizacao.notas });
  });
  autoUpdater.on("error", (e) => {
    console.error("[updater]", e);
    send("update:status", { state: "error", text: "A atualização falhou. O launcher tenta de novo daqui a pouco." });
  });
  checkUpdates();
  updateTimer = setInterval(checkUpdates, 30 * 60 * 1000);
}

const send = (channel, payload) => win && !win.isDestroyed() && win.webContents.send(channel, payload);

// ---------- console do jogo ----------
// Os logs do Minecraft chegam aos milhares na abertura. Eles são juntados e enviados em lotes,
// para o launcher não travar desenhando linha por linha.
const LOG_LIMIT = 5000;
const logLines = [];
let pendingLines = [];
let flushTimer = null;

function pushLog(text) {
  const lines = String(text).replace(/\r/g, "").split("\n");
  if (lines[lines.length - 1] === "") lines.pop();
  for (const l of lines) logLines.push(l);
  if (logLines.length > LOG_LIMIT) logLines.splice(0, logLines.length - LOG_LIMIT);
  pendingLines.push(...lines);
  if (!flushTimer) flushTimer = setTimeout(flushLogs, 100);
}

// Cópia do console em arquivo (pasta do Pulsar/logs/ultima-abertura.log), para o jogador mandar quando algo der errado.
function arquivoLog() {
  return path.join(app.getPath("userData"), "logs", "ultima-abertura.log");
}

function gravarLog(linhas, novo = false) {
  try {
    fs.mkdirSync(path.dirname(arquivoLog()), { recursive: true });
    const texto = linhas.map((l) => l + "\n").join("");
    if (novo) fs.writeFileSync(arquivoLog(), texto);
    else fs.appendFileSync(arquivoLog(), texto);
  } catch {}
}

function flushLogs() {
  flushTimer = null;
  if (pendingLines.length === 0) return;
  const batch = pendingLines;
  pendingLines = [];
  gravarLog(batch);
  send("game:log", batch);
  if (consoleWin && !consoleWin.isDestroyed()) consoleWin.webContents.send("console:lines", batch);
}

function openConsole() {
  if (consoleWin && !consoleWin.isDestroyed()) {
    consoleWin.show();
    consoleWin.focus();
    return;
  }
  consoleWin = new BrowserWindow({
    width: 1180,
    height: 720,
    minWidth: 640,
    minHeight: 320,
    backgroundColor: "#050816",
    title: tr("Console do Pulsar"),
    icon: ICON,
    autoHideMenuBar: true,
    // Barra de título escura do próprio console, com os botões do Windows por cima.
    titleBarStyle: "hidden",
    titleBarOverlay: { color: "#06091A", symbolColor: "#E6ECFF", height: 40 },
    webPreferences: { preload: path.join(__dirname, "console-preload.js"), contextIsolation: true, sandbox: true },
  });
  consoleWin.loadFile(path.join(__dirname, "src", "console.html"), { query: { idioma: idioma() } });
  consoleWin.on("closed", () => (consoleWin = null));
}

// Erro inesperado no launcher (por exemplo dentro da biblioteca que abre o jogo): vai para o console em vez de sumir.
process.on("uncaughtException", (err) => {
  console.error(err);
  try {
    pushLog(`[Pulsar] ERRO inesperado no launcher: ${err && err.stack ? err.stack : err}`);
  } catch {}
});
process.on("unhandledRejection", (err) => {
  console.error(err);
  try {
    pushLog(`[Pulsar] ERRO inesperado no launcher: ${err && err.stack ? err.stack : err}`);
  } catch {}
});

app.whenReady().then(() => {
  config = new Config(app.getPath("userData"));
  accounts = new Accounts(secretStore(), { get: () => config.get(), set: (patch) => config.set(patch) });
  gravacao = new Gravacao({
    app, BrowserWindow, desktopCapturer, ipcMain, shell,
    preload: path.join(__dirname, "gravador-preload.js"),
    html: path.join(__dirname, "src", "gravador.html"),
    log: pushLog,
  });

  ipcMain.handle("config:get", () => publicConfig());
  ipcMain.handle("config:set", (_e, patch) => {
    // Contas só mudam pelos pedidos de conta (a liberação do offline não pode vir da tela).
    const { msRefreshTokenEnc, updateTokenEnc, hasUpdateToken, contasOffline, contaAtiva, originalVerificada, ...safe } = patch || {};
    // Só as versões do Minecraft que o Pulsar sabe abrir.
    if ("version" in safe) {
      if (game || launching) throw new Error("Feche o Minecraft antes de trocar de versão.");
      safe.version = mcversoes.achar(String(safe.version)).id;
    }
    if ("novidadesVistas" in safe) safe.novidadesVistas = String(safe.novidadesVistas).slice(0, 20);
    if ("modoLauncher" in safe) safe.modoLauncher = safe.modoLauncher === "lite" ? "lite" : "normal";
    const antes = modoLauncher();
    config.set(safe);
    // Trocou de visual: a janela recarrega na outra tela.
    if ("modoLauncher" in safe && modoLauncher() !== antes && win && !win.isDestroyed()) setTimeout(() => win.loadFile(telaDoModo()), 50);
    return publicConfig();
  });
  ipcMain.handle("optifine:pick", async () => {
    const r = await dialog.showOpenDialog(win, {
      title: tr("Escolha o OptiFine 1.8.9"),
      filters: [{ name: "OptiFine", extensions: ["jar"] }],
      properties: ["openFile"],
    });
    if (r.canceled || !r.filePaths[0]) return publicConfig();
    config.set({ optifinePath: r.filePaths[0] });
    return publicConfig();
  });
  // Instalar o OptiFine: usa o que já estiver na pasta Downloads ou abre o site oficial e espera o download terminar.
  ipcMain.handle("optifine:baixar", () => {
    const downloads = app.getPath("downloads");
    const pronto = (arquivo) => {
      try {
        const destino = optifine.guardar(arquivo, path.join(app.getPath("userData"), "optifine"));
        config.set({ optifinePath: destino, loader: "forge_optifine", optifineAviso: true });
        console.log("[OptiFine] instalado: " + destino);
        send("optifine:estado", { estado: "pronto", config: publicConfig() });
      } catch (e) {
        send("optifine:estado", { estado: "erro", erro: e.message });
      }
    };
    const existente = optifine.procurar(downloads);
    if (existente && optifine.valido(existente)) {
      pronto(existente);
      return { estado: "pronto" };
    }
    shell.openExternal("https://optifine.net/downloads");
    vigiaOptifine.comecar(downloads, {
      aoAchar: pronto,
      aoDesistir: () => send("optifine:estado", { estado: "desistiu" }),
    });
    return { estado: "esperando", pasta: downloads };
  });
  ipcMain.handle("optifine:cancelar", () => vigiaOptifine.parar());
  // Endereço da skin de um nick (para a cabeça nas contas). Sem conta original com esse nome, ou sem internet: null.
  const skins = new Map();
  ipcMain.handle("skin:url", async (_e, nome) => {
    nome = String(nome || "");
    if (!/^[A-Za-z0-9_]{1,16}$/.test(nome)) return null;
    const k = nome.toLowerCase();
    const c = skins.get(k);
    if (c && Date.now() - c.t < 60 * 60 * 1000) return c.url;
    let url = null;
    try {
      const r = await fetch(`https://api.mojang.com/users/profiles/minecraft/${nome}`, { signal: AbortSignal.timeout(6000) });
      if (r.status === 200) {
        const { id } = await r.json();
        const p = await fetch(`https://sessionserver.mojang.com/session/minecraft/profile/${id}`, { signal: AbortSignal.timeout(6000) });
        if (p.ok) {
          const tex = ((await p.json()).properties || []).find((x) => x.name === "textures");
          const dados = tex ? JSON.parse(Buffer.from(tex.value, "base64").toString("utf8")) : null;
          const skin = dados && dados.textures && dados.textures.SKIN && dados.textures.SKIN.url;
          if (skin && /^https?:\/\/textures\.minecraft\.net\//.test(skin)) url = skin.replace(/^http:/, "https:");
        }
      }
      skins.set(k, { url, t: Date.now() });
    } catch {
      // Sem internet: tenta de novo na próxima vez.
    }
    return url;
  });
  ipcMain.handle("link:open", (_e, url) => {
    if (/^https:\/\/(optifine\.net|adoptium\.net|modrinth\.com|discord\.gg|github\.com\/R0N1234\/Pulsar_Client)\//.test(String(url))) shell.openExternal(url);
  });
  ipcMain.handle("versoes:list", () => listaVersoes());
  ipcMain.handle("mc:versoes", () => mcversoes.VERSOES);
  ipcMain.handle("versoes:escolher", (_e, qual) => {
    semJogo("Feche o Minecraft antes de trocar de versão.");
    config.set({ versaoJogo: qual === "anterior" ? "anterior" : "atual" });
    return listaVersoes();
  });
  ipcMain.handle("mods:get", () => config.getEnabledMods());
  ipcMain.handle("mods:set", (_e, id, enabled) => config.setModEnabled(String(id), !!enabled));

  // Aba Texturas: com o jogo aberto, a troca é feita pela aba Texturas dentro do jogo (ele regrava o options.txt ao fechar).
  const gameDir = () => config.get().gameDir;
  const semJogo = (texto) => {
    if (game || launching) throw new Error(texto);
  };
  ipcMain.handle("texturas:list", () => texturas.listar(gameDir()));
  ipcMain.handle("texturas:baixar", async (_e, id) => {
    await texturas.baixar(gameDir(), String(id), (pct) => send("texturas:progresso", { id: String(id), pct }));
    return texturas.listar(gameDir());
  });
  ipcMain.handle("texturas:usar", (_e, id, ligar) => {
    semJogo("O jogo está aberto: troque a textura pela aba Texturas dentro do jogo.");
    texturas.usar(gameDir(), String(id), !!ligar);
    return texturas.listar(gameDir());
  });
  ipcMain.handle("texturas:remover", (_e, id) => {
    semJogo("Feche o Minecraft antes de apagar uma textura.");
    texturas.remover(gameDir(), String(id));
    return texturas.listar(gameDir());
  });
  ipcMain.handle("texturas:pasta", () => shell.openPath(texturas.pastaPacotes(gameDir())));

  // Seus mods: mods Forge 1.8.9 do jogador, na pasta mods da instância do Pulsar.
  ipcMain.handle("usermods:list", () => usermods.listar(gameDir()));
  ipcMain.handle("usermods:adicionar", async (_e, caminhos) => {
    semJogo("Feche o Minecraft antes de mudar os mods.");
    let arquivos = Array.isArray(caminhos) ? caminhos.map(String).filter(Boolean) : [];
    if (!arquivos.length) {
      const r = await dialog.showOpenDialog(win, {
        title: tr("Escolha mods Forge para o Minecraft 1.8.9"),
        filters: [{ name: "Mods (.jar)", extensions: ["jar"] }],
        properties: ["openFile", "multiSelections"],
      });
      if (r.canceled) return { mods: usermods.listar(gameDir()), recusados: [] };
      arquivos = r.filePaths;
    }
    const recusados = usermods.adicionar(gameDir(), arquivos);
    return { mods: usermods.listar(gameDir()), recusados };
  });
  ipcMain.handle("usermods:alternar", (_e, arquivo, ligar) => {
    semJogo("Feche o Minecraft antes de mudar os mods.");
    usermods.alternar(gameDir(), String(arquivo), !!ligar);
    return usermods.listar(gameDir());
  });
  ipcMain.handle("usermods:remover", async (_e, arquivo) => {
    semJogo("Feche o Minecraft antes de mudar os mods.");
    // Vai para a Lixeira, para o jogador poder recuperar.
    await shell.trashItem(usermods.seguro(gameDir(), String(arquivo)));
    return usermods.listar(gameDir());
  });
  ipcMain.handle("usermods:pasta", () => shell.openPath(usermods.pasta(gameDir())));

  ipcMain.handle("account:restore", () => accounts.restore());
  ipcMain.handle("account:login", () => accounts.login({ icon: ICON }).catch((e) => Promise.reject(new Error(describeError(e)))));
  ipcMain.handle("account:logout", () => accounts.logout());
  ipcMain.handle("account:usar", (_e, conta) => accounts.usar(String(conta)));
  ipcMain.handle("account:offline-add", (_e, nick) => accounts.adicionarOffline(String(nick)));
  ipcMain.handle("account:offline-remove", (_e, nick) => accounts.removerOffline(String(nick)));

  ipcMain.handle("game:launch", async () => {
    if (game || launching) throw new Error("O Minecraft já está aberto.");
    launching = true;
    let ultimaEtapa = "";
    try {
      await accounts.fresh();
      const cfg = config.get();
      logLines.length = 0;
      pendingLines = [];
      send("game:log-reset");
      gravarLog([`Pulsar Client ${app.getVersion()} - ${new Date().toISOString()} - ${process.platform} ${process.arch}`], true);
      pushLog("──────── Abrindo o Minecraft ────────");
      if (cfg.openConsole) openConsole();
      bridge = await startBridge({
        onSwitchAccount: switchAccountFromGame,
        onList: async () => accounts.estado(),
        onUse: async (conta) => {
          if (conta === "microsoft" && !accounts.profile()) throw new Error("Entre com a conta Microsoft no launcher primeiro.");
          return contaDoJogo(() => accounts.usar(conta));
        },
        onAddOffline: async (nick) => contaDoJogo(() => accounts.adicionarOffline(nick)),
        onPresenca: (p) => presenca.atualizar(p),
        onMusica: (p) => musica.agora(p),
        onRadio: (p) => {
          const c = radio.comando(p);
          pushLog(c.tocando ? `[Pulsar] Rádio: tocando ${c.nome} (volume ${c.volume}%)` : "[Pulsar] Rádio: parado");
          send("radio:comando", c);
        },
        // Gravação: o launcher grava a janela do jogo (lib/gravacao.js) e devolve o arquivo salvo.
        onGravar: async (p) => {
          const r = await gravacao.pedir(p);
          pushLog(`[Pulsar] Gravação: ${p.acao}${r.arquivo ? " -> " + r.arquivo : ""}`);
          send("gravacao:estado", r);
          return r;
        },
      });
      const mcv = mcversoes.achar(cfg.version);
      presenca.comecar(mcv.tipo === "pulsar" ? config.getEnabledMods().sinal_discord !== false : true, mcv.id);
      const versao = versoes.escolher(versoes.listar(modSourceDir(), modAnteriorDir()), cfg.versaoJogo);
      if (versao.qual === "anterior" && mcv.tipo === "pulsar") pushLog(`[Pulsar] Jogando com a versão anterior do Pulsar (${versao.versao}).`);
      game = await launch({
        config: cfg,
        authorization: accounts.mclc(),
        modSourceDir: versao.pasta || modSourceDir(),
        runtimeDir: path.join(app.getPath("userData"), "runtime"),
        extraJvmArgs: [`-Dpulsar.ponte=${bridge.port}`, `-Dpulsar.chave=${bridge.key}`],
        onStatus: (s) => {
          send("game:status", s);
          // A etapa também vai para o console, para não parecer que travou (o progresso de download fica só na barra).
          if (s.step !== "download" && s.text && s.text !== ultimaEtapa) pushLog(`[Pulsar] ${(ultimaEtapa = s.text)}`);
        },
        onLog: pushLog,
        onClose: (code) => {
          game = null;
          presenca.parar();
          musica.parar();
          send("radio:comando", { tocando: false });
          gravacao.parar().then(() => send("gravacao:estado", gravacao.estado));
          closeBridge();
          flushLogs();
          send("game:status", { step: "closed", text: code === 0 ? "Pronto para jogar" : `O jogo fechou com erro (código ${code})` });
        },
      });
      return true;
    } catch (err) {
      // O erro aparecia só na barra do launcher; agora vai também para o console e para o arquivo de log.
      pushLog(`[Pulsar] ERRO: o Minecraft não abriu: ${err && err.message ? err.message : err}`);
      if (err && err.stack) pushLog(err.stack);
      pushLog(`[Pulsar] Registro salvo em ${arquivoLog()}`);
      flushLogs();
      game = null;
      presenca.parar();
      musica.parar();
      closeBridge();
      throw err;
    } finally {
      launching = false;
    }
  });

  ipcMain.on("radio:erro", (_e, texto) => pushLog(`[Pulsar] Rádio: ${String(texto).slice(0, 200)}`));
  ipcMain.handle("console:open", () => openConsole());
  ipcMain.handle("console:lines", () => logLines.slice());

  ipcMain.handle("app:version", () => app.getVersion());
  ipcMain.handle("update:token", (_e, token) => {
    config.set({ updateTokenEnc: encrypt(String(token || "").trim()) });
    checkUpdates();
    return publicConfig();
  });
  ipcMain.handle("update:check", () => checkUpdates());
  ipcMain.handle("update:install", () => {
    if (game || launching) throw new Error("Feche o Minecraft antes de atualizar.");
    if (!abrirAtualizador()) autoUpdater.quitAndInstall();
  });

  ipcMain.on("window:minimize", () => win.minimize());
  ipcMain.on("window:maximize", () => (win.isMaximized() ? win.unmaximize() : win.maximize()));
  ipcMain.handle("gravacao:estado", () => ({ ...gravacao.estado, pasta: gravacao.pasta() }));
  ipcMain.handle("gravacao:pasta", () => shell.openPath(gravacao.pasta()));
  ipcMain.on("window:close", () => {
    if (consoleWin && !consoleWin.isDestroyed()) consoleWin.close();
    win.close();
  });

  createWindow();
  win.webContents.once("did-finish-load", setupUpdates);
  if (process.env.PULSAR_PRINTS) win.webContents.once("did-finish-load", () => tirarPrints(process.env.PULSAR_PRINTS));
});

function tamanhoPasta(dir) {
  let total = 0;
  try {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, e.name);
      total += e.isDirectory() ? tamanhoPasta(p) : fs.statSync(p).size;
    }
  } catch {}
  return total;
}

// Abre a tela de atualização do Pulsar e fecha o launcher; ela instala a versão nova e abre o Pulsar de novo.
// Ela roda de uma cópia na pasta temporária, porque a pasta do Pulsar vai ser trocada durante a atualização.
function abrirAtualizador() {
  const origem = path.join(process.resourcesPath, "atualizador", "PulsarAtualizador.exe");
  if (process.platform !== "win32" || !atualizacao || !fs.existsSync(origem) || !fs.existsSync(atualizacao.arquivo || "")) return false;
  try {
    const copia = path.join(app.getPath("temp"), `PulsarAtualizador-${Date.now()}.exe`);
    fs.copyFileSync(origem, copia);
    const args = ["--atualizar", atualizacao.arquivo, "--versao", atualizacao.version, "--pid", String(process.pid),
      "--app", process.execPath, "--tamanho", String(tamanhoPasta(path.dirname(process.execPath)))];
    spawn(copia, args, { detached: true, stdio: "ignore" }).unref();
  } catch (e) {
    console.error("[updater]", e);
    return false;
  }
  if (updateTimer) clearInterval(updateTimer);
  if (consoleWin && !consoleWin.isDestroyed()) consoleWin.close();
  app.quit();
  return true;
}

// Teste no Windows do CI (PULSAR_PRINTS=pasta): abre cada aba, salva um print e fecha o launcher.
// Roda no visual que estiver configurado (Normal de fábrica) e, no fim, troca para o outro e tira um print dele também.
async function tirarPrints(dir) {
  const espera = (ms) => new Promise((r) => setTimeout(r, ms));
  const js = (code) => win.webContents.executeJavaScript(code);
  const print = async (nome) => {
    fs.writeFileSync(path.join(dir, `launcher-${nome}.png`), (await win.webContents.capturePage()).toPNG());
    console.log(`[prints] ${nome}`);
  };
  const aba = (v) => js(`document.querySelector('#nav button[data-view="${v}"]').click()`);
  const clicar = (sel) => js(`document.querySelector('${sel}')?.click()`);
  const normal = modoLauncher() === "normal";
  // Os mesmos passos nos dois visuais; só os seletores mudam.
  const S = normal
    ? {
        planeta: (o) => `.orbit-node[data-orbita="${o}"]`,
        depoisPlanetas: `[data-close-drawer]`,
        listaNovidades: `#notificationButton`,
        abasAbertas: `[...document.querySelectorAll("#notificationList button strong")].map((b) => b.textContent).join(" | ")`,
        fecharNovidades: `document.querySelector("#notificationButton").click()`,
        contas: `#profileButton`,
        texBtn: `.texture-card[data-id="f8thful"] .apply-texture`,
        texBaixada: `!!document.querySelector('.texture-card[data-id="f8thful"] .tx-del')`,
        anterior: `.versao.anterior:not(.disabled) .primary-small`,
        atual: `.versao.atual .primary-small`,
      }
    : {
        planeta: (o) => `.onode[data-orbita="${o}"]`,
        depoisPlanetas: null,
        listaNovidades: `#oLeg button[data-orbita="novidades"]`,
        abasAbertas: `[...document.querySelectorAll(".o-tab")].map((b) => b.textContent).join(" | ")`,
        fecharNovidades: `document.querySelector('#oLeg button[data-orbita="novidades"]').click(); document.querySelectorAll(".o-tab .x").forEach((x) => x.click())`,
        contas: `.acct-now`,
        texBtn: `.tx-card[data-id="f8thful"] .tx-btn`,
        texBaixada: `!!document.querySelector('.tx-card[data-id="f8thful"] .tx-sub')?.textContent.includes("apagar")`,
        anterior: `.versao.anterior:not([disabled])`,
        atual: `.versao.atual`,
      };
  try {
    fs.mkdirSync(dir, { recursive: true });
    await espera(4000);
    console.log(`[prints] visual: ${modoLauncher()}`);
    // Janela de novidades da versão (abre sozinha na primeira vez de cada versão).
    if (await js(`!document.getElementById("novModal").hidden`)) {
      await print("novidades");
      await js(`document.getElementById("novOk").click()`);
      await espera(500);
    }
    // Primeira vez sem OptiFine: a janela que recomenda instalar.
    await espera(900);
    if (await js(`!!document.getElementById("ofModal") && !document.getElementById("ofModal").hidden`)) {
      await print("optifine");
      await js(`document.getElementById("ofDepois").click()`);
      await espera(400);
    }
    // Lista do "Modo" aberta (o visual do client, no lugar da lista branca do Windows).
    await js(`document.querySelector('.seletor[data-de="loader"] .seletor-botao')?.click()`);
    await espera(400);
    await print("modo");
    await js(`document.querySelector('.seletor[data-de="loader"] .seletor-botao')?.click()`);
    await espera(300);
    await print("inicio");
    // Órbitas: um planeta de cada órbita abre a sua aba (no Normal, o painel lateral); a legenda abre a lista da órbita.
    for (const o of ["novidades", "sugestoes", "discord"]) {
      await clicar(S.planeta(o));
      if (S.depoisPlanetas && o !== "discord") await clicar(S.depoisPlanetas);
    }
    await espera(600);
    await print("orbita-abas");
    if (S.depoisPlanetas) await clicar(S.depoisPlanetas);
    await clicar(S.listaNovidades);
    await espera(600);
    await print("orbita-novidades");
    console.log(`[prints] abas: ${await js(S.abasAbertas)}`);
    await js(S.fecharNovidades);
    // Versões de PvP: escolhe a 1.21.8, confere e volta para a 1.8.9.
    await clicar(`#vRows button[data-versao="1.21.8"]`);
    await espera(600);
    await print("versao-1.21.8");
    console.log(`[prints] versão do Minecraft: ${config.get().version}`);
    await clicar(`#vRows button[data-versao="1.8.9"]`);
    await espera(400);
    await clicar(S.contas);
    await espera(400);
    await print("contas");
    await clicar(S.contas);
    await aba("mods");
    await espera(800);
    await print("mods");
    await js(`document.querySelector('#modsSeg button[data-aba="seus"]').click()`);
    await espera(800);
    await print("seus-mods");
    await aba("texturas");
    await espera(4000);
    await print("texturas");
    // Baixa e liga o pacote mais leve pelo launcher, como o jogador faria.
    await clicar(S.texBtn);
    for (let i = 0; i < 60 && !(await js(S.texBaixada)); i++) await espera(500);
    await clicar(S.texBtn);
    await espera(1000);
    await print("texturas-usando");
    try {
      console.log(`[prints] options: ${(fs.readFileSync(path.join(config.get().gameDir, "pulsar", "instancia-1.8.9", "options.txt"), "utf8").match(/resourcePacks:.*/) || ["?"])[0]}`);
    } catch (e) {
      console.log(`[prints] options: ${e.message}`);
    }
    // Aba Versões: escolhe a anterior (o Início avisa), confere e volta para a atual.
    await aba("versoes");
    await espera(800);
    await print("versoes");
    console.log(`[prints] versões: ${JSON.stringify(listaVersoes())}`);
    await clicar(S.anterior);
    await espera(800);
    await print("versoes-anterior");
    console.log(`[prints] escolhida: ${listaVersoes().escolhida}`);
    await aba("home");
    await espera(800);
    await print("inicio-anterior");
    await aba("versoes");
    await clicar(S.atual);
    await espera(800);
    console.log(`[prints] depois de voltar: ${listaVersoes().escolhida}`);
    await aba("settings");
    await espera(800);
    await print("ajustes");
    if (normal) {
      for (const s of ["minecraft", "visual", "update", "account"]) {
        await clicar(`[data-settings-tab="${s}"]`);
        await espera(300);
        await print(`ajustes-${s}`);
      }
      await clicar(`[data-settings-tab="general"]`);
    }
    await aba("home");
    send("update:status", { state: "ready", version: "0.6.1", notas: "Exemplo de novidades da versão nova.\nCorreções e mods novos." });
    await espera(1200);
    await print("atualizacao");
    // Gravação: grava a própria tela por alguns segundos pelo mesmo caminho do mod (não falha o teste se o PC não deixar).
    try {
      const r1 = await gravacao.pedir({ acao: "iniciar", qualidade: "720p", fps: 30, som: false });
      await espera(3000);
      const r2 = await gravacao.pedir({ acao: "parar" });
      console.log(`[prints] gravação: ${r1.gravando ? "gravou" : "não começou"} ${r2.arquivo ? path.basename(r2.arquivo) + " " + fs.statSync(r2.arquivo).size + " bytes" : "sem arquivo"}`);
    } catch (e) {
      console.log(`[prints] gravação: ${e.message}`);
    }
    // O outro visual: troca em Ajustes, a tela recarrega, print do Início e volta.
    const outro = normal ? "lite" : "normal";
    config.set({ modoLauncher: outro });
    await win.loadFile(telaDoModo());
    await espera(4000);
    await js(`if (!document.getElementById("novModal").hidden) document.getElementById("novOk").click()`);
    await espera(400);
    await print(`${outro}-inicio`);
    await aba("mods");
    await espera(800);
    await print(`${outro}-mods`);
    await aba("settings");
    await espera(800);
    await print(`${outro}-ajustes`);
    console.log(`[prints] visual depois: ${modoLauncher()}`);
    config.set({ modoLauncher: normal ? "normal" : "lite" });
  } catch (e) {
    console.error("[prints] erro:", e);
  }
  app.quit();
}

function closeBridge() {
  if (bridge) bridge.close();
  bridge = null;
}

// Pedido do botão "Trocar conta" no menu do jogo: abre o login da Microsoft por cima do Minecraft
// e devolve a sessão nova para o jogo usar sem fechar.
async function switchAccountFromGame() {
  try {
    send("account:changed", await accounts.login({ onTop: true, icon: ICON }));
    return accounts.session();
  } catch (err) {
    throw new Error(describeError(err));
  }
}

// Troca pedida pelo jogo para uma conta que o launcher já tem (a Microsoft ou um nick offline).
async function contaDoJogo(mudar) {
  const estado = mudar();
  if (estado.ativa === "microsoft") await accounts.fresh();
  const sessao = accounts.session();
  if (!sessao) throw new Error("Entre com a conta Microsoft no launcher primeiro.");
  send("account:changed", estado);
  return sessao;
}

app.on("window-all-closed", () => app.quit());
