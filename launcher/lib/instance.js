// Pasta própria do Pulsar dentro da .minecraft: .minecraft/pulsar/instancia-1.8.9.
// Ela tem a sua própria pasta mods (o Forge 1.8.9 carrega tudo o que estiver em mods/,
// então mods de outras versões na .minecraft fariam o jogo fechar).
// Mundos, resource packs e prints continuam sendo os da .minecraft, por atalhos de pasta.
const fs = require("fs");
const path = require("path");

const SHARED_DIRS = ["saves", "resourcepacks", "screenshots"];
const SEED_FILES = ["options.txt", "servers.dat"];
const OLD_FILES = /^(pulsar-client-.*|OptiFine-pulsar)\.jar$/;

function instanceDir(root) {
  return path.join(root, "pulsar", "instancia-1.8.9");
}

function linkShared(target, link) {
  fs.mkdirSync(target, { recursive: true });
  let st = null;
  try {
    st = fs.lstatSync(link);
  } catch {}
  if (st && st.isSymbolicLink()) return;
  if (st) {
    // Só troca por atalho se a pasta estiver vazia, para nunca apagar nada do jogador.
    if (!st.isDirectory() || fs.readdirSync(link).length > 0) return;
    fs.rmdirSync(link);
  }
  try {
    // "junction" no Windows não precisa de administrador.
    fs.symlinkSync(target, link, "junction");
  } catch {
    fs.mkdirSync(link, { recursive: true });
  }
}

function prepareInstance(root) {
  const dir = instanceDir(root);
  fs.mkdirSync(path.join(dir, "mods"), { recursive: true });
  for (const name of SHARED_DIRS) linkShared(path.join(root, name), path.join(dir, name));
  // Opções e lista de servidores começam iguais às da .minecraft; depois o Pulsar guarda as dele.
  for (const f of SEED_FILES) {
    const src = path.join(root, f);
    const dst = path.join(dir, f);
    if (!fs.existsSync(dst) && fs.existsSync(src)) fs.copyFileSync(src, dst);
  }
  // A 0.2.1 colocava o Pulsar em .minecraft/mods/1.8.9 e guardava a configuração em .minecraft/pulsar.
  const oldMods = path.join(root, "mods", "1.8.9");
  if (fs.existsSync(oldMods)) {
    for (const f of fs.readdirSync(oldMods)) if (OLD_FILES.test(f)) fs.rmSync(path.join(oldMods, f));
  }
  const oldCfg = path.join(root, "pulsar", "config.json");
  const newCfg = path.join(dir, "pulsar", "config.json");
  if (fs.existsSync(oldCfg) && !fs.existsSync(newCfg)) {
    fs.mkdirSync(path.dirname(newCfg), { recursive: true });
    fs.copyFileSync(oldCfg, newCfg);
  }
  return dir;
}

module.exports = { instanceDir, prepareInstance };
