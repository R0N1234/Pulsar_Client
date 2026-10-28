const { app, BrowserWindow, ipcMain, safeStorage, shell } = require("electron");
const path = require("path");
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

  ipcMain.on("window:minimize", () => win.minimize());
  ipcMain.on("window:close", () => win.close());

  createWindow();
});

app.on("window-all-closed", () => app.quit());
