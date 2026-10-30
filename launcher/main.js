const { app, BrowserWindow, dialog, ipcMain, safeStorage, shell } = require("electron");
const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");
const { autoUpdater } = require("electron-updater");
const { Config } = require("./lib/config");
const { Accounts, describeError } = require("./lib/auth");
const { launch } = require("./lib/launch");
const { startBridge } = require("./lib/ponte");
const texturas = require("./lib/texturas");
const usermods = require("./lib/usermods");

// Janelas (inclusive o login da Microsoft) em português.
app.commandLine.appendSwitch("lang", "pt-BR");

const ICON = path.join(__dirname, "src", "logo.png");

let win;
let consoleWin = null;
let config;
let accounts;
let game = null;
let launching = false;
let bridge = null;

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

function publicConfig() {
  const { msRefreshTokenEnc, updateTokenEnc, ...rest } = config.get();
  return { ...rest, hasUpdateToken: !!updateTokenEnc };
}

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
    webPreferences: { preload: path.join(__dirname, "preload.js"), contextIsolation: true, sandbox: true },
  });
  win.loadFile(path.join(__dirname, "src", "index.html"));
  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });
}

// Atualização automática: as versões saem nos Releases do repositório privado Pulsar_Client.
// Para baixar de um repositório privado, o launcher usa uma chave de leitura que o dono
// cola em Ajustes (nenhuma chave vai dentro do instalador). Sem chave, não procura atualização.
const UPDATE_REPO = { provider: "github", owner: "R0N1234", repo: "Pulsar_Client", private: true };
let updatesWired = false;
let updateTimer = null;

function checkUpdates() {
  const token = decrypt(config.get().updateTokenEnc);
  if (!app.isPackaged || !token) return;
  autoUpdater.setFeedURL({ ...UPDATE_REPO, token });
  autoUpdater.checkForUpdates().catch((e) => {
    console.error("[updater]", e);
    send("update:status", { state: "error", text: "Não consegui procurar atualização. Confira a chave em Ajustes." });
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

function flushLogs() {
  flushTimer = null;
  if (pendingLines.length === 0) return;
  const batch = pendingLines;
  pendingLines = [];
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
    width: 960,
    height: 580,
    minWidth: 560,
    minHeight: 320,
    backgroundColor: "#05070F",
    title: "Console do Pulsar",
    icon: ICON,
    autoHideMenuBar: true,
    webPreferences: { preload: path.join(__dirname, "console-preload.js"), contextIsolation: true, sandbox: true },
  });
  consoleWin.loadFile(path.join(__dirname, "src", "console.html"));
  consoleWin.on("closed", () => (consoleWin = null));
}

app.whenReady().then(() => {
  config = new Config(app.getPath("userData"));
  accounts = new Accounts(secretStore(), { get: () => config.get(), set: (patch) => config.set(patch) });

  ipcMain.handle("config:get", () => publicConfig());
  ipcMain.handle("config:set", (_e, patch) => {
    // Contas só mudam pelos pedidos de conta (a liberação do offline não pode vir da tela).
    const { msRefreshTokenEnc, updateTokenEnc, hasUpdateToken, contasOffline, contaAtiva, originalVerificada, ...safe } = patch || {};
    config.set(safe);
    return publicConfig();
  });
  ipcMain.handle("optifine:pick", async () => {
    const r = await dialog.showOpenDialog(win, {
      title: "Escolha o OptiFine 1.8.9",
      filters: [{ name: "OptiFine", extensions: ["jar"] }],
      properties: ["openFile"],
    });
    if (r.canceled || !r.filePaths[0]) return publicConfig();
    config.set({ optifinePath: r.filePaths[0] });
    return publicConfig();
  });
  ipcMain.handle("link:open", (_e, url) => {
    if (/^https:\/\/(optifine\.net|adoptium\.net|modrinth\.com)\//.test(String(url))) shell.openExternal(url);
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
        title: "Escolha mods Forge para o Minecraft 1.8.9",
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
    try {
      await accounts.fresh();
      const cfg = config.get();
      logLines.length = 0;
      send("game:log-reset");
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
      });
      game = await launch({
        config: cfg,
        authorization: accounts.mclc(),
        modSourceDir: modSourceDir(),
        runtimeDir: path.join(app.getPath("userData"), "runtime"),
        extraJvmArgs: [`-Dpulsar.ponte=${bridge.port}`, `-Dpulsar.chave=${bridge.key}`],
        onStatus: (s) => send("game:status", s),
        onLog: pushLog,
        onClose: (code) => {
          game = null;
          closeBridge();
          flushLogs();
          send("game:status", { step: "closed", text: code === 0 ? "Pronto para jogar" : `O jogo fechou com erro (código ${code})` });
        },
      });
      return true;
    } catch (err) {
      game = null;
      closeBridge();
      throw err;
    } finally {
      launching = false;
    }
  });

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
async function tirarPrints(dir) {
  const espera = (ms) => new Promise((r) => setTimeout(r, ms));
  const js = (code) => win.webContents.executeJavaScript(code);
  const print = async (nome) => {
    fs.writeFileSync(path.join(dir, `launcher-${nome}.png`), (await win.webContents.capturePage()).toPNG());
    console.log(`[prints] ${nome}`);
  };
  const aba = (v) => js(`document.querySelector('#nav button[data-view="${v}"]').click()`);
  try {
    fs.mkdirSync(dir, { recursive: true });
    await espera(4000);
    await print("inicio");
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
    await js(`document.querySelector('.tx-card[data-id="f8thful"] .tx-btn').click()`);
    for (let i = 0; i < 60 && !(await js(`!!document.querySelector('.tx-card[data-id="f8thful"] .tx-sub')?.textContent.includes("apagar")`)); i++) await espera(500);
    await js(`document.querySelector('.tx-card[data-id="f8thful"] .tx-btn').click()`);
    await espera(1000);
    await print("texturas-usando");
    console.log(`[prints] options: ${(fs.readFileSync(path.join(config.get().gameDir, "pulsar", "instancia-1.8.9", "options.txt"), "utf8").match(/resourcePacks:.*/) || ["?"])[0]}`);
    await aba("settings");
    await espera(800);
    await print("ajustes");
    await aba("home");
    send("update:status", { state: "ready", version: "0.6.1", notas: "Exemplo de novidades da versão nova.\nCorreções e mods novos." });
    await espera(1200);
    await print("atualizacao");
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
