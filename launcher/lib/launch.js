// Abre o Minecraft 1.8.9 com Forge e o mod do Pulsar.
// O minecraft-launcher-core baixa o jogo, as bibliotecas e os assets da Mojang na primeira vez.
const fs = require("fs");
const path = require("path");
const { Client } = require("minecraft-launcher-core");

const FORGE_VERSION = "1.8.9-11.15.1.2318-1.8.9";
const FORGE_URL = `https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE_VERSION}/forge-${FORGE_VERSION}-universal.jar`;

async function download(url, dest) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Não consegui baixar ${url} (HTTP ${res.status})`);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const tmp = dest + ".part";
  fs.writeFileSync(tmp, Buffer.from(await res.arrayBuffer()));
  fs.renameSync(tmp, dest);
}

async function ensureForge(gameDir) {
  const jar = path.join(gameDir, "pulsar", "forge", `forge-${FORGE_VERSION}-universal.jar`);
  if (!fs.existsSync(jar)) await download(FORGE_URL, jar);
  return jar;
}

// Copia o jar do mod (gerado em mod/build/libs, ou embutido no instalador) para a pasta mods.
function installPulsarMod(gameDir, modSourceDir) {
  const jars = fs.existsSync(modSourceDir)
    ? fs.readdirSync(modSourceDir).filter((f) => /^pulsar-client-.*\.jar$/.test(f) && !/-(sources|dev)\.jar$/.test(f))
    : [];
  if (jars.length === 0) {
    throw new Error(`O mod do Pulsar não foi encontrado em ${modSourceDir}. Rode "./gradlew build" na pasta mod primeiro.`);
  }
  const modsDir = path.join(gameDir, "mods");
  fs.mkdirSync(modsDir, { recursive: true });
  for (const f of fs.readdirSync(modsDir)) {
    if (/^pulsar-client-.*\.jar$/.test(f)) fs.rmSync(path.join(modsDir, f));
  }
  const newest = jars.sort().at(-1);
  fs.copyFileSync(path.join(modSourceDir, newest), path.join(modsDir, newest));
  return newest;
}

async function launch({ config, authorization, modSourceDir, onStatus, onLog, onClose }) {
  const { gameDir, memoryMaxGb, javaPath } = config;

  onStatus({ step: "forge", text: "Preparando o Forge 1.8.9" });
  const forge = await ensureForge(gameDir);

  onStatus({ step: "mod", text: "Instalando o Pulsar" });
  installPulsarMod(gameDir, modSourceDir);

  const client = new Client();
  client.on("debug", (l) => onLog(l));
  client.on("data", (l) => onLog(l));
  client.on("progress", (p) => onStatus({ step: "download", text: `Baixando ${p.type}`, done: p.task, total: p.total }));
  client.on("close", (code) => onClose(code));

  onStatus({ step: "start", text: "Abrindo o Minecraft" });
  const proc = await client.launch({
    authorization,
    root: gameDir,
    version: { number: "1.8.9", type: "release" },
    forge,
    memory: { max: `${memoryMaxGb}G`, min: "1G" },
    javaPath: javaPath || undefined,
  });
  if (!proc) throw new Error("O Minecraft não abriu. Confira se o Java 8 está instalado (veja Ajustes).");
  onStatus({ step: "running", text: "Jogando" });
  return proc;
}

module.exports = { launch, FORGE_VERSION };
