// Downloads do launcher com limite de tempo e progresso.
// Sem isso, uma conexão que para no meio (rede lenta, antivírus, firewall) deixava o launcher
// esperando para sempre sem nenhuma mensagem no console.
const fs = require("fs");
const path = require("path");

// Tempo máximo sem chegar nenhum dado antes de desistir.
const SEM_DADOS_MS = 60000;

/**
 * Baixa uma URL e devolve o conteúdo (Buffer). onProgresso(recebido, total) é chamado durante o download.
 * Falha com uma mensagem clara quando a conexão fica parada por mais de 1 minuto.
 */
async function baixarBuffer(url, { onProgresso, semDadosMs = SEM_DADOS_MS } = {}) {
  const ctrl = new AbortController();
  let parado = false;
  let timer;
  const vigiar = () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      parado = true;
      ctrl.abort();
    }, semDadosMs);
  };
  vigiar();
  try {
    const res = await fetch(url, { signal: ctrl.signal });
    if (!res.ok) throw new Error(`Não consegui baixar ${url} (HTTP ${res.status})`);
    const total = Number(res.headers.get("content-length")) || 0;
    const partes = [];
    let recebido = 0;
    for await (const parte of res.body) {
      vigiar();
      partes.push(Buffer.from(parte));
      recebido += parte.length;
      if (onProgresso) onProgresso(recebido, total);
    }
    return Buffer.concat(partes);
  } catch (e) {
    if (parado) throw new Error(`O download parou (mais de ${Math.round(semDadosMs / 1000)}s sem resposta): ${url}. Confira a internet, o antivírus ou o firewall.`);
    if (e && e.message && e.message.startsWith("Não consegui baixar")) throw e;
    const causa = (e && e.cause && (e.cause.code || e.cause.message)) || (e && e.message) || String(e);
    throw new Error(`Não consegui baixar ${url} (${causa}). Confira a internet, o antivírus ou o firewall.`);
  } finally {
    clearTimeout(timer);
  }
}

/** Baixa para um arquivo (grava em .part e renomeia no fim, para não sobrar arquivo pela metade). */
async function baixarArquivo(url, destino, opcoes) {
  const dados = await baixarBuffer(url, opcoes);
  fs.mkdirSync(path.dirname(destino), { recursive: true });
  fs.writeFileSync(destino + ".part", dados);
  fs.renameSync(destino + ".part", destino);
}

/** Baixa um JSON. */
async function baixarJson(url, opcoes) {
  return JSON.parse((await baixarBuffer(url, opcoes)).toString("utf8"));
}

/** Gera um onProgresso que escreve no console a cada 10%. */
function progressoNoLog(nome, onLog) {
  let ultimo = -1;
  return (recebido, total) => {
    const mb = (recebido / 1048576).toFixed(1);
    if (!total) {
      const passo = Math.floor(recebido / 10485760);
      if (passo > ultimo) {
        ultimo = passo;
        onLog(`[Pulsar] Baixando ${nome}: ${mb} MB`);
      }
      return;
    }
    const pct = Math.floor((recebido / total) * 10) * 10;
    if (pct > ultimo) {
      ultimo = pct;
      onLog(`[Pulsar] Baixando ${nome}: ${pct}% (${mb} de ${(total / 1048576).toFixed(1)} MB)`);
    }
  };
}

module.exports = { baixarBuffer, baixarArquivo, baixarJson, progressoNoLog };
