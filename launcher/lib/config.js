// Configuração do launcher e do mod.
// O launcher guarda as preferências dele em launcher.json.
// A lista de mods ligados vai para <pasta do jogo>/pulsar/config.json,
// o mesmo arquivo que o mod lê dentro do Minecraft (e onde ele salva as posições da HUD).
const fs = require("fs");
const path = require("path");
const os = require("os");

function defaultGameDir() {
  if (process.platform === "win32") return path.join(process.env.APPDATA || os.homedir(), ".pulsar");
  if (process.platform === "darwin") return path.join(os.homedir(), "Library", "Application Support", "pulsar");
  return path.join(os.homedir(), ".pulsar");
}

const DEFAULTS = {
  gameDir: defaultGameDir(),
  memoryMaxGb: 3,
  javaPath: "",
  profile: "Ranqueada",
  version: "1.8.9",
};

function readJson(file, fallback) {
  try {
    return JSON.parse(fs.readFileSync(file, "utf8"));
  } catch {
    return fallback;
  }
}

function writeJson(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(data, null, 2));
}

class Config {
  constructor(userDataDir) {
    this.file = path.join(userDataDir, "launcher.json");
    this.data = { ...DEFAULTS, ...readJson(this.file, {}) };
  }

  get() {
    return { ...this.data };
  }

  set(patch) {
    this.data = { ...this.data, ...patch };
    writeJson(this.file, this.data);
    return this.get();
  }

  modConfigFile() {
    return path.join(this.data.gameDir, "pulsar", "config.json");
  }

  // { "teclas_pulse": true, ... }
  getEnabledMods() {
    const cfg = readJson(this.modConfigFile(), {});
    const out = {};
    for (const [id, m] of Object.entries(cfg.mods || {})) out[id] = m.enabled !== false;
    return out;
  }

  // Só mexe no campo "enabled", para não apagar as posições da HUD que o mod salvou.
  setModEnabled(id, enabled) {
    const file = this.modConfigFile();
    const cfg = readJson(file, {});
    cfg.mods = cfg.mods || {};
    cfg.mods[id] = { ...(cfg.mods[id] || {}), enabled };
    writeJson(file, cfg);
    return this.getEnabledMods();
  }
}

module.exports = { Config };
