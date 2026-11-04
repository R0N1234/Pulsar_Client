// Música tocando no Spotify, para a Barra Pulsar dentro do jogo.
// No Windows, o título da janela do Spotify é "Artista - Música" enquanto toca e só "Spotify ..." quando para.
// Um PowerShell fica lendo esse título a cada 2 segundos (um processo só, aberto quando o jogo pede e fechado
// quando ele para de pedir). A capa vem da busca pública do iTunes, só quando a opção está ligada no jogo.
const { spawn } = require("child_process");
const crypto = require("crypto");

const SCRIPT = [
  "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
  "while ($true) {",
  "  $t = ''",
  "  $p = Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle } | Select-Object -First 1",
  "  if ($p) { $t = $p.MainWindowTitle }",
  "  [Console]::Out.WriteLine('T:' + $t)",
  "  [Console]::Out.Flush()",
  "  Start-Sleep -Seconds 2",
  "}",
].join("\n");

const PARADO = /^(spotify( free| premium)?|advertisement|propaganda|anúncio)$/i;

let leitor = null;
let titulo = "";
let ultimoPedido = 0;
let vigia = null;
const capas = new Map();

/** "Artista - Música" vira { artista, titulo }; títulos de pausa ou propaganda viram null. */
function separar(t) {
  const s = String(t || "").trim();
  if (!s || PARADO.test(s)) return null;
  const i = s.indexOf(" - ");
  if (i <= 0) return null;
  return { artista: s.slice(0, i).trim(), titulo: s.slice(i + 3).trim() };
}

function abrirLeitor() {
  if (leitor || process.platform !== "win32") return;
  try {
    leitor = spawn("powershell.exe", ["-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", "-"], {
      windowsHide: true,
      stdio: ["pipe", "pipe", "ignore"],
    });
  } catch {
    leitor = null;
    return;
  }
  let resto = "";
  leitor.stdout.setEncoding("utf8");
  leitor.stdout.on("data", (d) => {
    resto += d;
    const linhas = resto.split(/\r?\n/);
    resto = linhas.pop();
    for (const l of linhas) if (l.startsWith("T:")) titulo = l.slice(2);
  });
  leitor.on("exit", () => {
    leitor = null;
  });
  leitor.on("error", () => {
    leitor = null;
  });
  leitor.stdin.end(SCRIPT);
  // Sem pedidos do jogo por 30 s (barra desligada ou jogo fechado): fecha o PowerShell.
  vigia = setInterval(() => {
    if (Date.now() - ultimoPedido > 30000) parar();
  }, 10000);
}

function parar() {
  if (vigia) clearInterval(vigia);
  vigia = null;
  if (leitor) {
    try {
      leitor.kill();
    } catch {}
  }
  leitor = null;
  titulo = "";
}

/** Capa da música (JPEG 60x60 em base64) pela busca do iTunes, guardada por música. */
async function buscarCapa(m) {
  const chave = `${m.artista}\u0000${m.titulo}`.toLowerCase();
  if (capas.has(chave)) return capas.get(chave);
  capas.set(chave, null);
  try {
    const termo = encodeURIComponent(`${m.artista} ${m.titulo}`.slice(0, 120));
    const r = await fetch(`https://itunes.apple.com/search?term=${termo}&entity=song&limit=1`, { signal: AbortSignal.timeout(5000) });
    const j = await r.json();
    const url = j && j.results && j.results[0] && j.results[0].artworkUrl100;
    if (!url) return null;
    const img = await fetch(url.replace(/\/\d+x\d+bb\./, "/60x60bb."), { signal: AbortSignal.timeout(5000) });
    if (!img.ok) return null;
    const b64 = Buffer.from(await img.arrayBuffer()).toString("base64");
    if (b64.length > 60000) return null;
    capas.set(chave, b64);
    if (capas.size > 50) capas.delete(capas.keys().next().value);
    return b64;
  } catch {
    return null;
  }
}

/**
 * Pedido do jogo: { capa: true/false, capaId: "id da capa que o jogo já tem" }.
 * Devolve { tocando, titulo, artista, capaId, capa? } — a capa só vai quando muda.
 */
async function agora(corpo = {}) {
  ultimoPedido = Date.now();
  abrirLeitor();
  const m = separar(titulo);
  if (!m) return { tocando: false };
  const out = { tocando: true, titulo: m.titulo, artista: m.artista, capaId: "" };
  if (corpo.capa) {
    const chave = `${m.artista}\u0000${m.titulo}`.toLowerCase();
    // Busca em segundo plano: o jogo pergunta de novo em 2 s e aí a capa já está pronta.
    if (!capas.has(chave)) buscarCapa(m);
    const b64 = capas.get(chave);
    if (b64) {
      out.capaId = crypto.createHash("sha1").update(chave).digest("hex").slice(0, 12);
      if (corpo.capaId !== out.capaId) out.capa = b64;
    }
  }
  return out;
}

module.exports = { agora, parar, separar };
