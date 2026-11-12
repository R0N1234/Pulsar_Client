// Java para o Minecraft: o 8 para a 1.8.9, 1.7.10 e 1.16.5, e o 21 para a 1.21.
// Se o jogador não escolheu um Java em Ajustes (vale só para as versões do Java 8),
// o launcher baixa o Temurin (Eclipse Adoptium) uma vez e guarda na pasta do Pulsar.
const fs = require("fs");
const path = require("path");
const { execFile } = require("child_process");

function adoptiumUrl(major) {
  const os = { win32: "windows", darwin: "mac", linux: "linux" }[process.platform];
  // O Temurin 8 não tem build para Mac com chip Apple; a versão x64 roda pelo Rosetta.
  const arch = process.arch === "arm64" && (process.platform !== "darwin" || major > 8) ? "aarch64" : "x64";
  return `https://api.adoptium.net/v3/binary/latest/${major}/ga/${os}/${arch}/jre/hotspot/normal/eclipse`;
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

async function ensureJava({ javaPath, runtimeDir, onStatus, major = 8 }) {
  // O Java escolhido em Ajustes é um Java 8 (o campo pede isso); a 1.21 sempre usa o Java 21 do Pulsar.
  if (javaPath && major === 8) {
    if (!fs.existsSync(javaPath)) throw new Error(`O Java escolhido em Ajustes não existe: ${javaPath}`);
    return javaPath;
  }
  const nome = `Java ${major}`;
  const dir = path.join(runtimeDir, `java${major}`);
  const found = findJava(dir);
  if (found) return found;

  onStatus({ step: "java", text: `Baixando o ${nome} (só na primeira vez)` });
  const res = await fetch(adoptiumUrl(major));
  if (!res.ok) throw new Error(`Não consegui baixar o ${nome} (HTTP ${res.status}).` + (major === 8 ? " Escolha um Java 8 em Ajustes." : ""));
  const archive = path.join(runtimeDir, process.platform === "win32" ? `java${major}.zip` : `java${major}.tar.gz`);
  fs.mkdirSync(runtimeDir, { recursive: true });
  fs.writeFileSync(archive, Buffer.from(await res.arrayBuffer()));

  onStatus({ step: "java", text: `Instalando o ${nome}` });
  fs.rmSync(dir, { recursive: true, force: true });
  await extract(archive, dir);
  fs.rmSync(archive, { force: true });
  const java = findJava(dir);
  if (!java) throw new Error(`O ${nome} foi baixado, mas não encontrei o executável.` + (major === 8 ? " Escolha um Java 8 em Ajustes." : ""));
  return java;
}

module.exports = { ensureJava };
