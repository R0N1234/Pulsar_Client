// Versões do mod do Pulsar: a atual (que veio com o launcher) e só uma anterior (a última lançada antes dela).
// A anterior vem dentro do instalador (resources/anterior), então funciona até em quem instalou agora.
const fs = require("fs");
const path = require("path");

function jarDe(pasta) {
  if (!pasta || !fs.existsSync(pasta)) return null;
  const jars = fs.readdirSync(pasta).filter((f) => /^pulsar-client-.+\.jar$/.test(f) && !/-(sources|dev)\.jar$/.test(f));
  if (!jars.length) return null;
  const mtime = (f) => fs.statSync(path.join(pasta, f)).mtimeMs;
  const jar = jars.sort((a, b) => mtime(a) - mtime(b)).at(-1);
  return { versao: jar.replace(/^pulsar-client-/, "").replace(/\.jar$/, ""), pasta, jar };
}

// { atual: {versao, pasta, jar} | null, anterior: {…} | null }. A anterior some se for igual à atual.
function listar(pastaAtual, pastaAnterior) {
  const atual = jarDe(pastaAtual);
  let anterior = jarDe(pastaAnterior);
  if (anterior && atual && anterior.versao === atual.versao) anterior = null;
  return { atual, anterior };
}

// Pasta do mod para jogar: a anterior só se ela existir e estiver escolhida.
function escolher(lista, escolhida) {
  if (escolhida === "anterior" && lista.anterior) return { qual: "anterior", ...lista.anterior };
  return { qual: "atual", ...(lista.atual || {}) };
}

module.exports = { listar, escolher, jarDe };
