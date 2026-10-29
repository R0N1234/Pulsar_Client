// Java 8 para a 1.8.9. Se o jogador não escolheu um Java em Ajustes,
// o launcher baixa o Temurin 8 (Eclipse Adoptium) uma vez e guarda na pasta do Pulsar.
const fs = require("fs");
const path = require("path");
const { execFile } = require("child_process");

function adoptiumUrl() {
  const os = { win32: "windows", darwin: "mac", linux: "linux" }[process.platform];
  // O Temurin 8 não tem build para Mac com chip Apple; a versão x64 roda pelo Rosetta.
  const arch = process.arch === "arm64" && process.platform !== "darwin" ? "aarch64" : "x64";
  return `https://api.adoptium.net/v3/binary/latest/8/ga/${os}/${arch}/jre/hotspot/normal/eclipse`;
}

function javaBinaryName() {
  return process.platform === "win32" ? "javaw.exe" : "java";
}

function findJava(dir) {
  if (!fs.existsSync(dir)) return null;
  const stack = [dir];
  while (stack.length) {
    const d = stack.pop();
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) stack.push(p);
      else if (e.name === javaBinaryName() && path.basename(path.dirname(p)) === "bin") return p;
    }
  }
  return null;
}

function extract(archive, dest) {
  // O tar do Windows 10+ também abre .zip.
  return new Promise((resolve, reject) => {
    fs.mkdirSync(dest, { recursive: true });
    execFile("tar", ["-xf", archive, "-C", dest], (err) => (err ? reject(err) : resolve()));
  });
}

async function ensureJava({ javaPath, runtimeDir, onStatus }) {
  if (javaPath) {
    if (!fs.existsSync(javaPath)) throw new Error(`O Java escolhido em Ajustes não existe: ${javaPath}`);
    return javaPath;
  }
  const dir = path.join(runtimeDir, "java8");
  const found = findJava(dir);
  if (found) return found;

  onStatus({ step: "java", text: "Baixando o Java 8 (só na primeira vez)" });
  const res = await fetch(adoptiumUrl());
  if (!res.ok) throw new Error(`Não consegui baixar o Java 8 (HTTP ${res.status}). Escolha um Java 8 em Ajustes.`);
  const archive = path.join(runtimeDir, process.platform === "win32" ? "java8.zip" : "java8.tar.gz");
  fs.mkdirSync(runtimeDir, { recursive: true });
  fs.writeFileSync(archive, Buffer.from(await res.arrayBuffer()));

  onStatus({ step: "java", text: "Instalando o Java 8" });
  fs.rmSync(dir, { recursive: true, force: true });
  await extract(archive, dir);
  fs.rmSync(archive, { force: true });
  const java = findJava(dir);
  if (!java) throw new Error("O Java 8 foi baixado, mas não encontrei o executável. Escolha um Java 8 em Ajustes.");
  return java;
}

module.exports = { ensureJava };
