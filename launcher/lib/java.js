// Java para o Minecraft: o 8 para a 1.8.9, 1.7.10 e 1.16.5, e o 21 para a 1.21.
// Se o jogador não escolheu um Java em Ajustes (vale só para as versões do Java 8),
// o launcher baixa o Temurin (Eclipse Adoptium) uma vez e guarda na pasta do Pulsar.
const fs = require("fs");
const path = require("path");
const { execFile } = require("child_process");
const AdmZip = require("adm-zip");
const { baixarBuffer, progressoNoLog } = require("./baixar");

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
  fs.mkdirSync(dest, { recursive: true });
  // No Windows o .zip é aberto aqui mesmo: o tar.exe não existe em todo Windows
  // e se perde com pasta de usuário com acento ou caractere especial.
  if (archive.endsWith(".zip")) {
    new AdmZip(archive).extractAllTo(dest, true);
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    execFile("tar", ["-xf", archive, "-C", dest], (err) => (err ? reject(err) : resolve()));
  });
}

/** Roda "java -version" e devolve a primeira linha. Falha com o erro real se o Java não abrir. */
function versaoJava(java) {
  return new Promise((resolve, reject) => {
    execFile(java, ["-version"], { timeout: 30000, windowsHide: true }, (err, stdout, stderr) => {
      const saida = `${stderr || ""}${stdout || ""}`.trim();
      if (err) {
        const motivo = err.killed ? "não respondeu em 30s" : err.code || err.message;
        reject(new Error(`O Java em "${java}" não abriu (${motivo}).${saida ? " Saída: " + saida.split("\n")[0] : ""}`));
      } else resolve(saida.split("\n")[0] || "versão desconhecida");
    });
  });
}

async function ensureJava({ javaPath, runtimeDir, onStatus, onLog = () => {}, major = 8 }) {
  const java = await acharOuBaixarJava({ javaPath, runtimeDir, onStatus, onLog, major });
  onLog(`[Pulsar] Java: ${java}`);
  try {
    onLog(`[Pulsar] Java: ${await versaoJava(java)}`);
  } catch (e) {
    // Java baixado pelo Pulsar e estragado (download cortado, antivírus): apaga para baixar de novo na próxima.
    if (!(javaPath && major === 8)) fs.rmSync(path.join(runtimeDir, `java${major}`), { recursive: true, force: true });
    throw new Error(e.message + (javaPath && major === 8 ? " Confira o Java escolhido em Ajustes ou deixe o campo vazio." : " Tente jogar de novo para o Pulsar baixar o Java outra vez."));
  }
  return java;
}

async function acharOuBaixarJava({ javaPath, runtimeDir, onStatus, onLog, major }) {
  // O Java escolhido em Ajustes é um Java 8 (o campo pede isso); a 1.21 sempre usa o Java 21 do Pulsar.
  if (javaPath && major === 8) {
    if (!fs.existsSync(javaPath)) throw new Error(`O Java escolhido em Ajustes não existe: ${javaPath}`);
    onLog("[Pulsar] Usando o Java escolhido em Ajustes");
    return javaPath;
  }
  const nome = `Java ${major}`;
  const dir = path.join(runtimeDir, `java${major}`);
  const found = findJava(dir);
  if (found) return found;

  onStatus({ step: "java", text: `Baixando o ${nome} (só na primeira vez)` });
  onLog(`[Pulsar] ${nome} não encontrado em ${dir}, baixando (só na primeira vez)`);
  let dados;
  try {
    dados = await baixarBuffer(adoptiumUrl(major), { onProgresso: progressoNoLog(nome, onLog) });
  } catch (e) {
    throw new Error(`Não consegui baixar o ${nome}: ${e.message}` + (major === 8 ? " Ou escolha um Java 8 em Ajustes." : ""));
  }
  const archive = path.join(runtimeDir, process.platform === "win32" ? `java${major}.zip` : `java${major}.tar.gz`);
  fs.mkdirSync(runtimeDir, { recursive: true });
  fs.writeFileSync(archive, dados);

  onStatus({ step: "java", text: `Instalando o ${nome}` });
  onLog(`[Pulsar] Instalando o ${nome} em ${dir}`);
  fs.rmSync(dir, { recursive: true, force: true });
  try {
    await extract(archive, dir);
  } catch (e) {
    fs.rmSync(dir, { recursive: true, force: true });
    throw new Error(`Não consegui instalar o ${nome} (${e.message}).`);
  } finally {
    fs.rmSync(archive, { force: true });
  }
  const java = findJava(dir);
  if (!java) throw new Error(`O ${nome} foi baixado, mas não encontrei o executável.` + (major === 8 ? " Escolha um Java 8 em Ajustes." : ""));
  return java;
}

module.exports = { ensureJava };
