// Abre o Minecraft 1.8.9 no modo escolhido: Forge, OptiFine ou OptiFine + Forge.
// Usa a .minecraft do jogador (igual Lunar e Silent) para reaproveitar o que já foi baixado,
// com uma pasta de jogo própria (veja instance.js) para os mods não se misturarem.
const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");
const AdmZip = require("adm-zip");
const { Client } = require("minecraft-launcher-core");
const { ensureJava } = require("./java");
const { prepareInstance } = require("./instance");

const MC_VERSION = "1.8.9";
const ASSET_INDEX = "1.8";
const FORGE_VERSION = "1.8.9-11.15.1.2318-1.8.9";
const FORGE_URL = `https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE_VERSION}/forge-${FORGE_VERSION}-universal.jar`;
const MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
const OPTIFINE_VERSION_ID = "1.8.9-OptiFine-Pulsar";
const OPTIFINE_MOD_NAME = "OptiFine-pulsar.jar";
// Som no Windows: o OpenAL que vem com o 1.8.9 é de 2015 e fica mudo com vários fones e placas de som atuais
// (Lunar, Silent e cia. trazem um mais novo). Usamos o OpenAL Soft 1.23.1, o mesmo do Minecraft atual, e o antigo fica de reserva.
const OPENAL_VERSAO = "1.23.1";
const OPENAL_URL = "https://repo1.maven.org/maven2/org/lwjgl/lwjgl-openal/3.3.3/lwjgl-openal-3.3.3-natives-windows.jar";
const OPENAL_SHA1 = "cf83862ae95d98496b26915024c7e666d8ab1c8f";

// Flags de JVM para menos travadas (coleta de lixo curta e previsível).
const JVM_FLAGS = [
  "-XX:+UseG1GC",
  "-XX:+UnlockExperimentalVMOptions",
  "-XX:G1NewSizePercent=20",
  "-XX:G1ReservePercent=20",
  "-XX:MaxGCPauseMillis=50",
  "-XX:G1HeapRegionSize=16M",
  "-Dfml.readTimeout=180",
  // Registro do jogo em UTF-8: no Windows os acentos chegavam quebrados no console do Pulsar ("Telesc�pio").
  "-Dfile.encoding=UTF-8",
  "-Dsun.stdout.encoding=UTF-8",
  "-Dsun.stderr.encoding=UTF-8",
];

const LOADERS = {
  forge: { forge: true, optifine: false },
  optifine: { forge: false, optifine: true },
  forge_optifine: { forge: true, optifine: true },
};

async function download(url, dest) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Não consegui baixar ${url} (HTTP ${res.status})`);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const tmp = dest + ".part";
  fs.writeFileSync(tmp, Buffer.from(await res.arrayBuffer()));
  fs.renameSync(tmp, dest);
}

async function ensureForge(root) {
  const jar = path.join(root, "pulsar", "forge", `forge-${FORGE_VERSION}-universal.jar`);
  if (!fs.existsSync(jar)) await download(FORGE_URL, jar);
  return jar;
}

/** Pasta com o OpenAL64.dll novo (só Windows 64 bits). Baixa uma vez e confere pelo SHA-1. */
async function ensureOpenAL(root) {
  const dir = path.join(root, "pulsar", "openal", OPENAL_VERSAO);
  const dll = path.join(dir, "OpenAL64.dll");
  if (fs.existsSync(dll) && fs.statSync(dll).size > 100000) return dir;
  const res = await fetch(OPENAL_URL);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const jar = Buffer.from(await res.arrayBuffer());
  const sha1 = require("crypto").createHash("sha1").update(jar).digest("hex");
  if (sha1 !== OPENAL_SHA1) throw new Error("arquivo diferente do esperado");
  const entry = new AdmZip(jar).getEntry("windows/x64/org/lwjgl/openal/OpenAL.dll");
  if (!entry) throw new Error("OpenAL.dll não veio no pacote");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(dll + ".part", entry.getData());
  fs.renameSync(dll + ".part", dll);
  return dir;
}

// Jar do Minecraft 1.8.9 puro, necessário para preparar o OptiFine.
async function ensureVanillaJar(root) {
  const jar = path.join(root, "versions", MC_VERSION, `${MC_VERSION}.jar`);
  if (fs.existsSync(jar)) return jar;
  const manifest = await (await fetch(MANIFEST_URL)).json();
  const entry = manifest.versions.find((v) => v.id === MC_VERSION);
  const versionJson = await (await fetch(entry.url)).json();
  await download(versionJson.downloads.client.url, jar);
  return jar;
}

function isPulsarJar(f) {
  return /^pulsar-client-.*\.jar$/.test(f);
}

// Copia o jar do Pulsar para a pasta mods da instância do Pulsar.
function installPulsarMod(modsDir, modSourceDir) {
  const jars = fs.existsSync(modSourceDir)
    ? fs.readdirSync(modSourceDir).filter((f) => isPulsarJar(f) && !/-(sources|dev)\.jar$/.test(f))
    : [];
  if (jars.length === 0) {
    throw new Error(`O mod do Pulsar não foi encontrado em ${modSourceDir}. Rode "./gradlew build" na pasta mod primeiro.`);
  }
  fs.mkdirSync(modsDir, { recursive: true });
  for (const f of fs.readdirSync(modsDir)) if (isPulsarJar(f)) fs.rmSync(path.join(modsDir, f));
  // O mais novo pela data do arquivo: em ordem alfabética, 0.10.0 viria antes de 0.9.0.
  const mtime = (f) => fs.statSync(path.join(modSourceDir, f)).mtimeMs;
  const newest = jars.sort((a, b) => mtime(a) - mtime(b)).at(-1);
  fs.copyFileSync(path.join(modSourceDir, newest), path.join(modsDir, newest));
}

// No modo OptiFine + Forge, o OptiFine entra como mod. Nos outros modos, sai da pasta.
function syncOptifineMod(modsDir, optifinePath) {
  const dest = path.join(modsDir, OPTIFINE_MOD_NAME);
  if (optifinePath) {
    fs.mkdirSync(modsDir, { recursive: true });
    fs.copyFileSync(optifinePath, dest);
  } else if (fs.existsSync(dest)) {
    fs.rmSync(dest);
  }
}

function checkOptifineFile(optifinePath) {
  if (!optifinePath || !fs.existsSync(optifinePath)) {
    throw new Error("Escolha o arquivo do OptiFine 1.8.9 em Ajustes antes de jogar nesse modo.");
  }
  if (!/1\.8\.9/.test(path.basename(optifinePath))) {
    throw new Error("O arquivo do OptiFine precisa ser da versão 1.8.9 (por exemplo OptiFine_1.8.9_HD_U_M5.jar).");
  }
}

function run(java, args, onLog) {
  return new Promise((resolve, reject) => {
    const p = spawn(java, args);
    p.stdout.on("data", (d) => onLog(String(d)));
    p.stderr.on("data", (d) => onLog(String(d)));
    p.on("error", reject);
    p.on("close", (code) => (code === 0 ? resolve() : reject(new Error(`O preparo do OptiFine falhou (código ${code}).`))));
  });
}

// Monta uma versão "1.8.9-OptiFine-Pulsar" do mesmo jeito que o instalador oficial do OptiFine.
async function ensureOptifineVersion(root, optifinePath, java, onLog) {
  const ofVersion = (path.basename(optifinePath).match(/1\.8\.9_HD_[A-Z]+_[A-Z0-9]+/) || ["1.8.9_HD_custom"])[0];
  const vanillaJar = await ensureVanillaJar(root);
  const zip = new AdmZip(optifinePath);
  const libRoot = path.join(root, "libraries");

  const ofLib = path.join(libRoot, "optifine", "OptiFine", ofVersion, `OptiFine-${ofVersion}.jar`);
  if (!fs.existsSync(ofLib)) {
    fs.mkdirSync(path.dirname(ofLib), { recursive: true });
    if (zip.getEntry("optifine/Patcher.class")) {
      await run(java, ["-cp", optifinePath, "optifine.Patcher", vanillaJar, optifinePath, ofLib], onLog);
    } else {
      fs.copyFileSync(optifinePath, ofLib);
    }
  }

  let launchwrapper = { name: "net.minecraft:launchwrapper:1.12", url: "https://libraries.minecraft.net/" };
  const lwEntry = zip.getEntry("launchwrapper-of.txt");
  if (lwEntry) {
    const lwVersion = zip.readAsText(lwEntry).trim();
    const lwJar = path.join(libRoot, "optifine", "launchwrapper-of", lwVersion, `launchwrapper-of-${lwVersion}.jar`);
    if (!fs.existsSync(lwJar)) {
      fs.mkdirSync(path.dirname(lwJar), { recursive: true });
      fs.writeFileSync(lwJar, zip.readFile(`launchwrapper-of-${lwVersion}.jar`));
    }
    launchwrapper = { name: `optifine:launchwrapper-of:${lwVersion}` };
  }

  const dir = path.join(root, "versions", OPTIFINE_VERSION_ID);
  fs.mkdirSync(dir, { recursive: true });
  fs.copyFileSync(vanillaJar, path.join(dir, `${OPTIFINE_VERSION_ID}.jar`));
  fs.writeFileSync(
    path.join(dir, `${OPTIFINE_VERSION_ID}.json`),
    JSON.stringify(
      {
        id: OPTIFINE_VERSION_ID,
        inheritsFrom: MC_VERSION,
        type: "release",
        mainClass: "net.minecraft.launchwrapper.Launch",
        minecraftArguments:
          "--username ${auth_player_name} --version ${version_name} --gameDir ${game_directory} --assetsDir ${assets_root} " +
          "--assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} " +
          "--userProperties ${user_properties} --userType ${user_type} --tweakClass optifine.OptiFineTweaker",
        libraries: [{ name: `optifine:OptiFine:${ofVersion}` }, launchwrapper],
      },
      null,
      2
    )
  );
  return OPTIFINE_VERSION_ID;
}

async function launch({ config, authorization, modSourceDir, runtimeDir, onStatus, onLog, onClose, extraJvmArgs = [] }) {
  const root = config.gameDir;
  const loader = LOADERS[config.loader] ? config.loader : "forge";
  const { forge, optifine } = LOADERS[loader];
  const gameDir = prepareInstance(root);
  const modsDir = path.join(gameDir, "mods");

  if (optifine) checkOptifineFile(config.optifinePath);

  const java = await ensureJava({ javaPath: config.javaPath, runtimeDir, onStatus });

  // O jogo procura o OpenAL na ordem das pastas: primeiro o novo, depois o original (nativos do 1.8.9).
  // O -Djava.library.path daqui vem depois do que o launcher-core põe, e o último é o que vale.
  const somArgs = [];
  if (process.platform === "win32" && process.arch === "x64") {
    try {
      const oal = await ensureOpenAL(root);
      somArgs.push(`-Djava.library.path=${oal}${path.delimiter}${path.resolve(root, "natives", MC_VERSION)}`);
      onLog(`[Pulsar] Som: OpenAL Soft ${OPENAL_VERSAO} pronto`);
    } catch (e) {
      onLog(`[Pulsar] Som: fiquei com o OpenAL original (${e.message})`);
    }
  }

  const options = {
    authorization,
    root,
    version: { number: MC_VERSION, type: "release" },
    memory: { max: `${config.memoryMaxGb}G`, min: "1G" },
    javaPath: java,
    customArgs: [...JVM_FLAGS, ...somArgs, ...extraJvmArgs],
    overrides: { assetIndex: ASSET_INDEX, gameDirectory: gameDir },
  };

  if (forge) {
    onStatus({ step: "forge", text: "Preparando o Forge 1.8.9" });
    options.forge = await ensureForge(root);
    onStatus({ step: "mod", text: "Instalando o Pulsar" });
    installPulsarMod(modsDir, modSourceDir);
    syncOptifineMod(modsDir, optifine ? config.optifinePath : null);
  } else {
    onStatus({ step: "optifine", text: "Preparando o OptiFine" });
    syncOptifineMod(modsDir, null);
    options.version.custom = await ensureOptifineVersion(root, config.optifinePath, java, onLog);
  }

  const client = new Client();
  let lastStep = "";
  client.on("debug", (l) => {
    onLog(l);
    // Mostra a etapa atual, para não parecer travado durante downloads longos.
    const m = String(l).match(/\[MCLC\]: (Attempting to download|Downloaded|Collected|Generating|Launching)/);
    if (m && m[1] !== lastStep) {
      lastStep = m[1];
      onStatus({ step: "prepare", text: STEP_TEXT[m[1]] });
    }
  });
  client.on("data", (l) => onLog(l));
  client.on("progress", (p) => onStatus({ step: "download", text: `Baixando ${PROGRESS_TEXT[p.type] || p.type}`, done: p.task, total: p.total }));

  onStatus({ step: "start", text: "Verificando os arquivos do Minecraft" });
  const proc = await client.launch(options);
  if (!proc) throw new Error("O Minecraft não abriu. Veja o registro do jogo para o motivo.");
  proc.on("close", (code) => onClose(code));
  onStatus({ step: "running", text: "Jogando" });
  return proc;
}

const STEP_TEXT = {
  "Attempting to download": "Baixando arquivos do Minecraft",
  Downloaded: "Arquivos baixados",
  Collected: "Bibliotecas prontas",
  Generating: "Preparando o Forge",
  Launching: "Abrindo o Minecraft",
};
const PROGRESS_TEXT = {
  assets: "sons e texturas",
  natives: "arquivos do sistema",
  classes: "bibliotecas",
  "classes-custom": "bibliotecas do Forge",
  "classes-maven-custom": "bibliotecas do Forge",
};

module.exports = { launch, FORGE_VERSION, LOADERS };
