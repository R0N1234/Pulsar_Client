// Confere se os arquivos que o main.js abre (preloads, telas, ícone) entraram mesmo no instalador.
// Um arquivo esquecido na lista "files" do package.json só quebra no PC de quem instalou.
const fs = require("fs");
const path = require("path");
const asar = require("@electron/asar");

const root = path.join(__dirname, "..");
const archive = path.join(root, "dist", "win-unpacked", "resources", "app.asar");
const inside = new Set(asar.listPackage(archive).map((f) => f.replace(/\\/g, "/").replace(/^\//, "")));

const main = fs.readFileSync(path.join(root, "main.js"), "utf8");
const wanted = [...main.matchAll(/path\.join\(__dirname((?:\s*,\s*"[^"]+")+)\s*\)/g)]
  .map((m) => [...m[1].matchAll(/"([^"]+)"/g)].map((x) => x[1]).join("/"))
  .filter((p) => !p.startsWith(".."));

const missing = wanted.filter((p) => !inside.has(p));
console.log(`Arquivos usados pelo main.js: ${wanted.join(", ")}`);
if (missing.length) {
  console.error(`Faltando no instalador: ${missing.join(", ")}`);
  process.exit(1);
}
console.log("Todos os arquivos estão no instalador.");
