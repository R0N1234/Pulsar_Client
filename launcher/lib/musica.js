// Música tocando no Spotify, para a Barra Pulsar dentro do jogo.
// No Windows, um PowerShell fica lendo a música a cada 2 segundos (um processo só, aberto quando o jogo pede e
// fechado quando ele para de pedir). Ele lê de dois jeitos, sem pedir login nem permissão nenhuma:
//  1. Pelos controles de mídia do Windows (o mesmo painel que aparece ao mudar o volume): artista, música e se está tocando.
//  2. Pelo título da janela do Spotify ("Artista - Música" enquanto toca), se o primeiro não estiver disponível.
// A capa vem da busca pública do iTunes, só quando a opção está ligada no jogo.
const { spawn, execFile } = require("child_process");
const crypto = require("crypto");

function script(umaVez) {
  return [
    "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8",
    `$umaVez = $${umaVez ? "true" : "false"}`,
    "$mgr = $null",
    "try {",
    "  Add-Type -AssemblyName System.Runtime.WindowsRuntime",
    "  $asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1",
    "  function Esperar($op, [Type]$tipo) { $t = $asTask.MakeGenericMethod($tipo).Invoke($null, @($op)); [void]$t.Wait(3000); $t.Result }",
    "  $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]",
    "  $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime]",
    "  $mgr = Esperar ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
    "  [Console]::Out.WriteLine('I:controles de mídia ' + [bool]$mgr)",
    "} catch { [Console]::Out.WriteLine('I:controles de mídia indisponíveis: ' + $_.Exception.Message) }",
    "while ($true) {",
    "  $t = ''",
    "  $p = Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle } | Select-Object -First 1",
    "  if ($p) { $t = $p.MainWindowTitle }",
    "  [Console]::Out.WriteLine('T:' + $t)",
    "  if ($mgr) {",
    "    try {",
    "      $s = $mgr.GetSessions() | Where-Object { $_.SourceAppUserModelId -match 'Spotify' } | Select-Object -First 1",
    "      if ($s) {",
    "        $tocando = [int]($s.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing')",
    "        $m = Esperar ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
    "        [Console]::Out.WriteLine('M:' + $tocando + \"`t\" + $m.Artist + \"`t\" + $m.Title)",
    "      } else { [Console]::Out.WriteLine('M:') }",
    "    } catch { [Console]::Out.WriteLine('M:') }",
    "  }",
    "  [Console]::Out.Flush()",
    "  if ($umaVez) { break }",
    "  Start-Sleep -Seconds 2",
    "}",
  ].join("\n");
}

// O script vai codificado na linha de comando: mandado pela entrada padrão, o PowerShell não roda blocos de várias linhas.
function argumentos(umaVez) {
  return ["-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-EncodedCommand", Buffer.from(script(umaVez), "utf16le").toString("base64")];
}

const PARADO = /^(spotify( free| premium)?|advertisement|propaganda|anúncio)$/i;

let leitor = null;
let titulo = "";
// Última leitura dos controles de mídia: { tocando, artista, titulo, quando } ou null (Spotify fechado).
let midia = null;
let midiaQuando = 0;
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

/** Uma linha do PowerShell: "T:título da janela" ou "M:tocando<tab>artista<tab>música". */
function lerLinha(l) {
  if (l.startsWith("T:")) titulo = l.slice(2);
  else if (l.startsWith("M:")) {
    const p = l.slice(2).split("\t");
    midia = p.length >= 3 ? { tocando: p[0] === "1", artista: p[1].trim(), titulo: p.slice(2).join(" ").trim() } : null;
    midiaQuando = Date.now();
  } else if (l.startsWith("I:")) console.log("[musica] " + l.slice(2));
}

function abrirLeitor() {
  if (leitor || process.platform !== "win32") return;
  try {
    leitor = spawn("powershell.exe", argumentos(false), { windowsHide: true, stdio: ["ignore", "pipe", "ignore"] });
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
    for (const l of linhas) lerLinha(l);
  });
  leitor.on("exit", () => {
    leitor = null;
  });
  leitor.on("error", () => {
    leitor = null;
  });
  // Sem pedidos do jogo por 30 s (barra desligada ou jogo fechado): fecha o PowerShell.
  if (!vigia) {
    vigia = setInterval(() => {
      if (Date.now() - ultimoPedido > 30000) parar();
    }, 10000);
  }
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
  midia = null;
}

/** A música atual: pelos controles de mídia (se responderam há pouco), senão pelo título da janela. */
function musicaAtual() {
  if (Date.now() - midiaQuando < 8000 && midia) {
    if (!midia.tocando || !midia.titulo) return null;
    return { artista: midia.artista, titulo: midia.titulo };
  }
  return separar(titulo);
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
  const m = musicaAtual();
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

/** Roda a leitura uma vez só e devolve o que o PowerShell escreveu (teste do CI no Windows). */
function testar() {
  return new Promise((resolve) => {
    execFile("powershell.exe", argumentos(true), { windowsHide: true, timeout: 30000 }, (err, stdout, stderr) => {
      resolve({ erro: err ? String(err.message || err) : "", saida: String(stdout || ""), erros: String(stderr || "") });
    });
  });
}

module.exports = { agora, parar, separar, lerLinha, musicaAtual, testar };
