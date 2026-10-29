const { app, BrowserWindow, dialog, ipcMain, safeStorage, shell } = require("electron");
const path = require("path");
const { autoUpdater } = require("electron-updater");
const { Config } = require("./lib/config");
const { Accounts } = require("./lib/auth");
const { launch } = require("./lib/launch");

let win;
let config;
let accounts;
let game = null;

// Guarda o token da Microsoft criptografado pelo sistema quando possível.
function secretStore() {
  return {
    get() {
      const raw = config.get().msRefreshTokenEnc;
      if (!raw) return {};
      try {
        const buf = Buffer.from(raw, "base64");
        return { msRefreshToken: safeStorage.isEncryptionAvailable() ? safeStorage.decryptString(buf) : buf.toString() };
      } catch {
        return {};
      }
    },
    set({ msRefreshToken }) {
      if (!msRefreshToken) return config.set({ msRefreshTokenEnc: "" });
      const buf = safeStorage.isEncryptionAvailable() ? safeStorage.encryptString(msRefreshToken) : Buffer.from(msRefreshToken);
      config.set({ msRefreshTokenEnc: buf.toString("base64") });
    },
  };
}

function modSourceDir() {
  return app.isPackaged ? path.join(process.resourcesPath, "mods") : path.join(__dirname, "..", "mod", "build", "libs");
}

function publicConfig() {
  const { msRefreshTokenEnc, ...rest } = config.get();
  return rest;
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
    webPreferences: { preload: path.join(__dirname, "preload.js"), contextIsolation: true, sandbox: true },
  });
  win.loadFile(path.join(__dirname, "src", "index.html"));
  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });
}

// Atualização automática: procura versão nova ao abrir e a cada 30 minutos,
// baixa em segundo plano e instala quando o jogador reiniciar o launcher.
function setupUpdates() {
  if (!app.isPackaged) return;
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;
  autoUpdater.on("update-available", (i) => send("update:status", { state: "downloading", version: i.version, percent: 0 }));
  autoUpdater.on("download-progress", (p) => send("update:status", { state: "downloading", percent: Math.round(p.percent) }));
  autoUpdater.on("update-downloaded", (i) => send("update:status", { state: "ready", version: i.version }));
  autoUpdater.on("error", (e) => console.error("[updater]", e));
  const check = () => autoUpdater.checkForUpdates().catch((e) => console.error("[updater]", e));
  check();
  setInterval(check, 30 * 60 * 1000);
}

const send = (channel, payload) => win && !win.isDestroyed() && win.webContents.send(channel, payload);

app.whenReady().then(() => {
  config = new Config(app.getPath("userData"));
  accounts = new Accounts(secretStore());

  ipcMain.handle("config:get", () => publicConfig());
  ipcMain.handle("config:set", (_e, patch) => {
    const { msRefreshTokenEnc, ...safe } = patch || {};
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
    if (/^https:\/\/(optifine\.net|adoptium\.net)\//.test(String(url))) shell.openExternal(url);
  });
  ipcMain.handle("mods:get", () => config.getEnabledMods());
  ipcMain.handle("mods:set", (_e, id, enabled) => config.setModEnabled(String(id), !!enabled));

  ipcMain.handle("account:restore", () => accounts.restore());
  ipcMain.handle("account:login", () => accounts.login());
  ipcMain.handle("account:logout", () => accounts.logout());

  ipcMain.handle("game:launch", async () => {
    if (game) throw new Error("O Minecraft já está aberto.");
    game = await launch({
      config: config.get(),
      authorization: accounts.mclc(),
      modSourceDir: modSourceDir(),
      runtimeDir: path.join(app.getPath("userData"), "runtime"),
      onStatus: (s) => send("game:status", s),
      onLog: (l) => send("game:log", String(l)),
      onClose: (code) => {
        game = null;
        send("game:status", { step: "closed", text: code === 0 ? "Pronto para jogar" : `O jogo fechou com erro (código ${code})` });
      },
    }).catch((err) => {
      game = null;
      throw err;
    });
    return true;
  });

  ipcMain.handle("app:version", () => app.getVersion());
  ipcMain.handle("update:install", () => {
    if (game) throw new Error("Feche o Minecraft antes de atualizar.");
    autoUpdater.quitAndInstall();
  });

  ipcMain.on("window:minimize", () => win.minimize());
  ipcMain.on("window:close", () => win.close());

  createWindow();
  win.webContents.once("did-finish-load", setupUpdates);
});

app.on("window-all-closed", () => app.quit());
