// Compila o instalador e o atualizador do Pulsar (Pulsar.cs) com o csc do .NET Framework que vem no Windows.
//
//   node instalador/compilar.js atualizador   -> launcher/build/atualizador/PulsarAtualizador.exe (vai junto com o launcher)
//   node instalador/compilar.js instalador    -> launcher/dist/Pulsar-Client-Instalador-<versão>.exe
//                                                (leva dentro o instalador do electron-builder; rode depois do "npm run dist")
const fs = require("fs");
const os = require("os");
const path = require("path");
const { execFileSync } = require("child_process");

const modo = process.argv[2];
if (modo !== "instalador" && modo !== "atualizador") {
  console.error("Uso: node instalador/compilar.js instalador|atualizador");
  process.exit(2);
}
const aqui = __dirname;
const launcher = path.join(aqui, "..", "launcher");
const versao = require(path.join(launcher, "package.json")).version;

const csc = path.join(process.env.WINDIR || "C:\\Windows", "Microsoft.NET", "Framework64", "v4.0.30319", "csc.exe");
if (!fs.existsSync(csc)) {
  console.error(`Não achei o compilador C# do Windows em ${csc}.`);
  process.exit(1);
}

function tamanhoPasta(dir) {
  let total = 0;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    total += e.isDirectory() ? tamanhoPasta(p) : fs.statSync(p).size;
  }
  return total;
}

const recursos = [
  [path.join(launcher, "build", "icon.png"), "logo.png"],
  // O "PC" no anel ciano -> violeta da tela de carregamento do jogo (tools/arte_espaco.py).
  [path.join(aqui, "..", "mod", "src", "main", "resources", "assets", "pulsar", "textures", "splash", "logo.png"), "anel.png"],
  [path.join(aqui, "fontes", "Exo2-Regular.ttf"), "Exo2-Regular.ttf"],
  [path.join(aqui, "fontes", "Exo2-Bold.ttf"), "Exo2-Bold.ttf"],
];
let tamanho = 0;
let saida;
if (modo === "instalador") {
  const dist = path.join(launcher, "dist");
  const setup = path.join(dist, `Pulsar Client Setup ${versao}.exe`);
  if (!fs.existsSync(setup)) {
    console.error(`Falta ${setup}: rode "npm run dist" no launcher antes.`);
    process.exit(1);
  }
  recursos.push([setup, "setup.exe"]);
  // Tamanho da pasta instalada, para a barra de progresso.
  tamanho = tamanhoPasta(path.join(dist, "win-unpacked"));
  saida = path.join(dist, `Pulsar-Client-Instalador-${versao}.exe`);
} else {
  saida = path.join(launcher, "build", "atualizador", "PulsarAtualizador.exe");
}
fs.mkdirSync(path.dirname(saida), { recursive: true });

const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "pulsar-csc-"));
const build = path.join(tmp, "Build.cs");
const [maior, menor, patch] = versao.split(/[.-]/).map((n) => parseInt(n, 10) || 0);
fs.writeFileSync(
  build,
  `using System.Reflection;
[assembly: AssemblyTitle("Pulsar Client")]
[assembly: AssemblyProduct("Pulsar Client")]
[assembly: AssemblyFileVersion("${maior}.${menor}.${patch}.0")]
[assembly: AssemblyVersion("${maior}.${menor}.${patch}.0")]
namespace PulsarInstalador {
  static class Build {
    public const string Versao = "${versao}";
    public const long Tamanho = ${tamanho};
  }
}
`
);

const args = [
  "/nologo",
  // O Pulsar.cs é UTF-8 (acentos nas telas).
  "/codepage:65001",
  "/target:winexe",
  "/optimize+",
  "/platform:anycpu",
  `/out:${saida}`,
  `/win32icon:${path.join(launcher, "build", "icon.ico")}`,
  `/win32manifest:${path.join(aqui, "app.manifest")}`,
  "/r:System.Windows.Forms.dll",
  "/r:System.Drawing.dll",
  ...recursos.map(([arq, nome]) => `/resource:${arq},${nome}`),
  path.join(aqui, "Pulsar.cs"),
  build,
];
try {
  execFileSync(csc, args, { stdio: "inherit" });
} finally {
  fs.rmSync(tmp, { recursive: true, force: true });
}
console.log(`${modo} pronto: ${saida} (${Math.round(fs.statSync(saida).size / 1024 / 1024)} MB)`);
