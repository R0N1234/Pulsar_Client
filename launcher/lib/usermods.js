// "Seus mods": mods Forge 1.8.9 que o jogador coloca junto com o Pulsar, na pasta mods da instância do Pulsar.
// Desligar renomeia para .jar.disabled (o Forge ignora). Mods de outras versões ou do Fabric são barrados,
// porque fariam o jogo fechar ao abrir.
const fs = require("fs");
const path = require("path");
const AdmZip = require("adm-zip");
const { instanceDir } = require("./instance");

const DO_PULSAR = /^(pulsar-client-.*|OptiFine-pulsar)\.jar$/;
const DESLIGADO = ".disabled";

function pasta(gameDir) {
  const dir = path.join(instanceDir(gameDir), "mods");
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

// Nome, versão e versão do Minecraft lidos do mcmod.info (quando o mod tem).
function ler(arquivo) {
  const info = { nome: "", versao: "", mc: "", tipo: "forge" };
  try {
    const zip = new AdmZip(arquivo);
    if (zip.getEntry("fabric.mod.json") || zip.getEntry("quilt.mod.json")) info.tipo = "fabric";
    else if (zip.getEntry("META-INF/mods.toml") || zip.getEntry("META-INF/neoforge.mods.toml")) info.tipo = "forge-novo";
    const mcmod = zip.getEntry("mcmod.info");
    if (mcmod) {
      let dados = JSON.parse(zip.readAsText(mcmod).replace(/^﻿/, ""));
      if (dados && dados.modList) dados = dados.modList;
      const m = Array.isArray(dados) ? dados[0] : dados;
      if (m) Object.assign(info, { nome: String(m.name || ""), versao: String(m.version || ""), mc: String(m.mcversion || "") });
    }
  } catch {
    info.tipo = "invalido";
  }
  return info;
}

function problema(info) {
  if (info.tipo === "fabric") return "Mod do Fabric: não funciona no Forge 1.8.9.";
  if (info.tipo === "forge-novo") return "Mod de uma versão nova do Minecraft: não funciona na 1.8.9.";
  if (info.tipo === "invalido") return "Arquivo danificado ou que não é um mod.";
  if (info.mc && !/^1\.8(\.\d)?$/.test(info.mc) && !/\$\{/.test(info.mc)) return `Feito para o Minecraft ${info.mc}, não para o 1.8.9.`;
  return "";
}

function listar(gameDir) {
  const dir = pasta(gameDir);
  return fs
    .readdirSync(dir)
    .filter((f) => (f.endsWith(".jar") || f.endsWith(".jar" + DESLIGADO)) && !DO_PULSAR.test(f))
    .map((f) => {
      const info = ler(path.join(dir, f));
      const base = f.endsWith(DESLIGADO) ? f.slice(0, -DESLIGADO.length) : f;
      return { arquivo: f, nome: info.nome || base.replace(/\.jar$/, ""), versao: info.versao, mc: info.mc,
        ligado: !f.endsWith(DESLIGADO), aviso: problema(info) };
    })
    .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR", { sensitivity: "base" }));
}

// Copia os .jar escolhidos. Os que com certeza não funcionam ficam de fora, com o motivo.
function adicionar(gameDir, arquivos) {
  const dir = pasta(gameDir);
  const recusados = [];
  for (const origem of arquivos) {
    const nome = path.basename(origem);
    if (!nome.toLowerCase().endsWith(".jar")) {
      recusados.push(`${nome}: só arquivos .jar`);
      continue;
    }
    if (DO_PULSAR.test(nome)) {
      recusados.push(`${nome}: o Pulsar já vem instalado`);
      continue;
    }
    if (/optifine/i.test(nome)) {
      recusados.push(`${nome}: o Pulsar já vem com o OptiFine`);
      continue;
    }
    const aviso = problema(ler(origem));
    if (aviso && !/^Feito para/.test(aviso)) {
      recusados.push(`${nome}: ${aviso}`);
      continue;
    }
    // Versão do Minecraft diferente: entra desligado, o jogador decide.
    fs.copyFileSync(origem, path.join(dir, aviso ? nome + DESLIGADO : nome));
  }
  return recusados;
}

function seguro(gameDir, arquivo) {
  const dir = pasta(gameDir);
  const alvo = path.join(dir, path.basename(String(arquivo)));
  if (DO_PULSAR.test(path.basename(alvo)) || !fs.existsSync(alvo)) throw new Error("Mod não encontrado.");
  return alvo;
}

function alternar(gameDir, arquivo, ligar) {
  const atual = seguro(gameDir, arquivo);
  const ligado = !atual.endsWith(DESLIGADO);
  if (ligado === !!ligar) return;
  const novo = ligado ? atual + DESLIGADO : atual.slice(0, -DESLIGADO.length);
  try {
    fs.renameSync(atual, novo);
  } catch (e) {
    throw new Error(e.code === "EBUSY" || e.code === "EPERM" ? "Feche o Minecraft antes de mudar os mods." : e.message);
  }
}

module.exports = { listar, adicionar, alternar, seguro, pasta };
