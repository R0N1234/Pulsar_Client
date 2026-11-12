// Teste do GitHub Actions: abre de verdade cada versão de PvP sem o Pulsar (1.7.10, 1.16.5 e 1.21.8)
// pelo mesmo código do botão JOGAR (lib/launch.js), com conta offline e tela virtual.
// Confere se o jogo chegou no menu (som ligado ou texturas montadas), tira um print e fecha.
// Uso: node test/versoes-pvp.js <pasta-temporária> [versão…]
const fs = require("fs");
const path = require("path");
const { execFile } = require("child_process");
const { Authenticator } = require("minecraft-launcher-core");
const { launch } = require("../lib/launch");
const { VERSOES } = require("../lib/mcversoes");

const work = path.resolve(process.argv[2] || "versoes-work");
const pedidas = process.argv.slice(3);
const lista = VERSOES.filter((v) => v.tipo !== "pulsar" && (!pedidas.length || pedidas.includes(v.id)));
const PRONTO = /Sound engine started|Created: \d+x\d+(x\d+)? (minecraft:)?textures[/-]/;
const TEMPO = 12 * 60 * 1000;
fs.mkdirSync(work, { recursive: true });
const logGeral = fs.createWriteStream(path.join(work, "versoes.log"));

function print(arquivo) {
  return new Promise((r) => execFile("import", ["-window", "root", arquivo], () => r()));
}

async function testar(v) {
  const log = fs.createWriteStream(path.join(work, `${v.id}.log`));
  const escreve = (l) => {
    log.write(String(l).replace(/\n?$/, "\n"));
  };
  const auth = await Authenticator.getAuth("PulsarTeste");
  return new Promise(async (resolve) => {
    let proc = null;
    let fim = false;
    const terminar = (ok, motivo) => {
      if (fim) return;
      fim = true;
      clearTimeout(timer);
      const linha = `${ok ? "OK" : "FALHOU"} ${v.id}: ${motivo}`;
      console.log(linha);
      logGeral.write(linha + "\n");
      if (proc && proc.exitCode === null) proc.kill("SIGKILL");
      setTimeout(() => {
        log.end();
        resolve(ok);
      }, 1500);
    };
    const timer = setTimeout(() => terminar(false, "o jogo não chegou no menu em 12 minutos"), TEMPO);
    let viu = false;
    const onLog = (texto) => {
      escreve(texto);
      if (!viu && PRONTO.test(String(texto))) {
        viu = true;
        // Espera o menu aparecer na tela antes do print.
        setTimeout(async () => {
          await print(path.join(work, `versao-${v.id}.png`));
          terminar(true, String(texto).trim().slice(0, 140));
        }, 20000);
      }
    };
    try {
      proc = await launch({
        config: { gameDir: path.join(work, "mc"), version: v.id, memoryMaxGb: 2, javaPath: "", loader: "forge" },
        authorization: auth,
        modSourceDir: "",
        runtimeDir: path.join(work, "runtime"),
        onStatus: (s) => escreve(`[status] ${s.text}`),
        onLog,
        onClose: (code) => terminar(viu, `o jogo fechou (código ${code})${viu ? "" : " antes do menu"}`),
      });
      // A pasta do jogo é a da versão (não a da 1.8.9).
      const pasta = path.join(work, "mc", "pulsar", `instancia-${v.id}`);
      if (!fs.existsSync(pasta)) terminar(false, `a pasta ${pasta} não foi criada`);
    } catch (e) {
      escreve(e.stack || e);
      terminar(false, e.message);
    }
  });
}

(async () => {
  let falhas = 0;
  for (const v of lista) if (!(await testar(v))) falhas++;
  logGeral.end();
  process.exit(falhas ? 1 : 0);
})();
