// Teste de ponta a ponta usado no GitHub Actions: abre o Minecraft 1.8.9 de verdade
// (conta offline, tela virtual) e confere se o Forge carregou o Pulsar.
// Uso: node test/e2e.js <pasta-temporária>
const fs = require("fs");
const path = require("path");
const { Authenticator } = require("minecraft-launcher-core");
const { launch } = require("../lib/launch");

const work = path.resolve(process.argv[2] || "e2e-work");
const logFile = path.join(work, "e2e.log");
const SUCCESS = /\[Pulsar\] Pulsar Client iniciado/;
const READY = /Sound engine started|Created: \d+x\d+ textures-atlas/;
const TIMEOUT_MS = 8 * 60 * 1000;

fs.mkdirSync(work, { recursive: true });
const log = fs.createWriteStream(logFile);
let pulsarLoaded = false;
let gameReady = false;
let proc;

function finish(code, why) {
  console.log(`\n== ${why}`);
  if (proc) try { process.kill(-proc.pid); } catch { try { proc.kill("SIGKILL"); } catch {} }
  log.end(() => process.exit(code));
}

const onLog = (line) => {
  const text = String(line);
  log.write(text.endsWith("\n") ? text : text + "\n");
  if (SUCCESS.test(text)) pulsarLoaded = true;
  if (READY.test(text)) gameReady = true;
  if (pulsarLoaded && gameReady) finish(0, "OK: Forge carregou o Pulsar e o jogo abriu");
};

setTimeout(() => finish(1, `FALHOU: tempo esgotado (pulsar=${pulsarLoaded}, jogo=${gameReady})`), TIMEOUT_MS);

(async () => {
  try {
    proc = await launch({
      config: { gameDir: path.join(work, "minecraft"), memoryMaxGb: 2, javaPath: process.env.E2E_JAVA || "", loader: "forge", optifinePath: "" },
      authorization: Authenticator.getAuth("PulsarTeste"),
      modSourceDir: path.resolve(__dirname, "..", "..", "mod", "build", "libs"),
      runtimeDir: path.join(work, "runtime"),
      onStatus: (s) => console.log(`[status] ${s.text}${s.total ? ` ${s.done}/${s.total}` : ""}`),
      onLog,
      onClose: (code) => finish(pulsarLoaded && gameReady ? 0 : 1, `O jogo fechou com código ${code}`),
    });
  } catch (err) {
    onLog(String(err && err.stack ? err.stack : err));
    finish(1, "FALHOU: " + err.message);
  }
})();
