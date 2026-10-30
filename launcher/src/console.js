// Console do Pulsar: mostra o registro do Minecraft numa janela própria, com cores e filtro.
const api = window.pulsarConsole;
const $ = (id) => document.getElementById(id);
const MAX = 5000;
const log = $("log");
const all = [];

function kind(line) {
  if (/ERROR|FATAL|Exception|\tat |Caused by/.test(line)) return "e";
  if (/WARN/.test(line)) return "w";
  if (/\[Pulsar\]/.test(line)) return "p";
  if (/\[CHAT\]/.test(line)) return "c";
  return "";
}

function visible(line, k) {
  if ($("onlyErr").checked && k !== "e") return false;
  const q = $("filter").value.trim().toLowerCase();
  return !q || line.toLowerCase().includes(q);
}

function add(lines) {
  const frag = document.createDocumentFragment();
  for (const line of lines) {
    const k = kind(line);
    all.push(line);
    const el = document.createElement("div");
    el.textContent = line;
    el.className = k + (visible(line, k) ? "" : " h");
    frag.append(el);
  }
  log.append(frag);
  while (log.childElementCount > MAX) log.firstElementChild.remove();
  if (all.length > MAX) all.splice(0, all.length - MAX);
  $("count").textContent = `${all.length} linhas`;
  if ($("follow").checked) log.scrollTop = log.scrollHeight;
}

function refilter() {
  for (const el of log.children) {
    const k = el.className.replace(" h", "").trim();
    el.className = k + (visible(el.textContent, k) ? "" : " h");
  }
  if ($("follow").checked) log.scrollTop = log.scrollHeight;
}

$("filter").addEventListener("input", refilter);
$("onlyErr").addEventListener("change", refilter);
$("clear").onclick = () => {
  log.textContent = "";
  all.length = 0;
  $("count").textContent = "0 linhas";
};
$("copy").onclick = async () => {
  try {
    await navigator.clipboard.writeText(all.join("\n"));
    $("copy").textContent = "Copiado";
  } catch {
    $("copy").textContent = "Não deu para copiar";
  }
  setTimeout(() => ($("copy").textContent = "Copiar tudo"), 2000);
};
// Rolar para cima pausa a rolagem automática; voltar ao fim liga de novo.
log.addEventListener("scroll", () => {
  $("follow").checked = log.scrollTop + log.clientHeight >= log.scrollHeight - 30;
});

api.lines().then(add);
api.onLines(add);
