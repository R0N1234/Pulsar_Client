// Confere se cada estação do Rádio Pulsar responde com áudio (roda no CI, que tem internet aberta).
// Só informa: rádio da internet às vezes sai do ar e isso não deve travar o build.
// Pede como o launcher pede (navegador Chromium do Electron), para o resultado valer para quem joga.
const { ESTACOES } = require("../lib/radio");
const NAVEGADOR = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) PulsarClient Chrome/144.0.0.0 Electron/40.0.0 Safari/537.36";

async function testar(url) {
  const ctl = new AbortController();
  const prazo = setTimeout(() => ctl.abort(), 10000);
  try {
    const res = await fetch(url, { signal: ctl.signal, headers: { "User-Agent": NAVEGADOR, Accept: "*/*", Range: "bytes=0-" } });
    const leitor = res.body.getReader();
    const { value } = await leitor.read();
    ctl.abort();
    const tipo = res.headers.get("content-type") || "?";
    const ok = res.ok && /audio|mpeg|aac|ogg|octet/.test(tipo) && value && value.length > 0;
    return { ok, texto: `HTTP ${res.status} ${tipo} ${value ? value.length : 0} bytes` };
  } catch (e) {
    return { ok: false, texto: e.name === "AbortError" ? "sem resposta em 10 s" : e.message };
  } finally {
    clearTimeout(prazo);
  }
}

(async () => {
  const linhas = await Promise.all(
    Object.entries(ESTACOES).map(async ([id, { nome, urls }]) => {
      const r = await Promise.all(urls.map(testar));
      const algum = r.some((x) => x.ok);
      return [`${algum ? "OK " : "RUIM"} ${id} (${nome})`, ...urls.map((u, i) => `     ${r[i].ok ? "ok" : "--"} ${u}: ${r[i].texto}`)].join("\n");
    })
  );
  console.log(linhas.join("\n"));
})();
