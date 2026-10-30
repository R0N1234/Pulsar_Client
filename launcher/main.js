const { app, BrowserWindow, dialog, ipcMain, safeStorage, shell } = require("electron");
const path = require("path");
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

function setupUpdates() {
  if (!app.isPackaged || updatesWired) return;
  updatesWired = true;
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;
  autoUpdater.on("update-available", (i) => send("update:status", { state: "downloading", version: i.version, percent: 0 }));
  autoUpdater.on("update-not-available", () => send("update:status", { state: "none" }));
  autoUpdater.on("download-progress", (p) => send("update:status", { state: "downloading", percent: Math.round(p.percent) }));
  autoUpdater.on("update-downloaded", (i) => send("update:status", { state: "ready", version: i.version }));
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
  accounts = new Accounts(secretStore());

  ipcMain.handle("config:get", () => publicConfig());
  ipcMain.handle("config:set", (_e, patch) => {
    const { msRefreshTokenEnc, updateTokenEnc, hasUpdateToken, ...safe } = patch || {};
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
      bridge = await startBridge({ onSwitchAccount: switchAccountFromGame });
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
    if (game) throw new Error("Feche o Minecraft antes de atualizar.");
    autoUpdater.quitAndInstall();
  });

  ipcMain.on("window:minimize", () => win.minimize());
  ipcMain.on("window:close", () => {
    if (consoleWin && !consoleWin.isDestroyed()) consoleWin.close();
    win.close();
  });

  createWindow();
  win.webContents.once("did-finish-load", setupUpdates);
});

function closeBridge() {
  if (bridge) bridge.close();
  bridge = null;
}

// Pedido do botão "Trocar conta" no menu do jogo: abre o login da Microsoft por cima do Minecraft
// e devolve a sessão nova para o jogo usar sem fechar.
async function switchAccountFromGame() {
  try {
    const profile = await accounts.login({ onTop: true, icon: ICON });
    send("account:changed", profile);
    return accounts.session();
  } catch (err) {
    throw new Error(describeError(err));
  }
}

app.on("window-all-closed", () => app.quit());
