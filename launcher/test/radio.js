// Confere se cada estação do Rádio Pulsar responde com áudio (roda no CI, que tem internet aberta).
// Só informa: rádio da internet às vezes sai do ar e isso não deve travar o build.
const { ESTACOES } = require("../lib/radio");

async function testar(id, { nome, url }) {
  const ctl = new AbortController();
  const prazo = setTimeout(() => ctl.abort(), 10000);
  try {
    const res = await fetch(url, { signal: ctl.signal, headers: { "Icy-MetaData": "0" } });
    const leitor = res.body.getReader();
    const { value } = await leitor.read();
    ctl.abort();
    const tipo = res.headers.get("content-type") || "?";
    const ok = res.ok && /audio|mpeg|aac|ogg|octet/.test(tipo) && value && value.length > 0;
    return `${ok ? "OK " : "RUIM"} ${id} (${nome}): HTTP ${res.status} ${tipo} ${value ? value.length : 0} bytes`;
  } catch (e) {
    return `RUIM ${id} (${nome}): ${e.name === "AbortError" ? "sem resposta em 10 s" : e.message}`;
  } finally {
    clearTimeout(prazo);
  }
}

(async () => {
  const linhas = await Promise.all(Object.entries(ESTACOES).map(([id, e]) => testar(id, e)));
  console.log(linhas.join("\n"));
})();
