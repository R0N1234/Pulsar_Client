// Configuração do launcher e do mod.
// O launcher guarda as preferências dele em launcher.json.
// A lista de mods ligados vai para <pasta do jogo>/pulsar/config.json,
// o mesmo arquivo que o mod lê dentro do Minecraft (e onde ele salva as posições da HUD).
const fs = require("fs");
const path = require("path");
const os = require("os");
const { instanceDir } = require("./instance");

// A mesma pasta .minecraft do launcher oficial (o Lunar e o Silent também usam ela).
function minecraftDir() {
  if (process.platform === "win32") return path.join(process.env.APPDATA || os.homedir(), ".minecraft");
  if (process.platform === "darwin") return path.join(os.homedir(), "Library", "Application Support", "minecraft");
  return path.join(os.homedir(), ".minecraft");
}

const CONFIG_VERSION = 2;

const DEFAULTS = {
  configVersion: CONFIG_VERSION,
  gameDir: minecraftDir(),
  memoryMaxGb: 3,
  javaPath: "",
  loader: "forge",
  optifinePath: "",
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
    const saved = readJson(this.file, {});
    // A 0.1 usava uma pasta .pulsar separada; a partir da 0.2 o padrão é a .minecraft.
    if ((saved.configVersion || 1) < CONFIG_VERSION) delete saved.gameDir;
    this.data = { ...DEFAULTS, ...saved, configVersion: CONFIG_VERSION };
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
    return path.join(instanceDir(this.data.gameDir), "pulsar", "config.json");
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

module.exports = { Config, minecraftDir };
