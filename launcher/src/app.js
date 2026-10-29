const api = window.pulsar;
const MODS = window.PULSAR_MODS;
const CATS = window.PULSAR_CATS;
const $ = (id) => document.getElementById(id);

let enabled = {};
let cat = "Todos";

/* ---------- céu de estrelas ---------- */
function drawStars() {
  const cv = $("stars");
  const dpr = window.devicePixelRatio || 1;
  cv.width = innerWidth * dpr;
  cv.height = innerHeight * dpr;
  const g = cv.getContext("2d");
  let s = 7;
  const r = () => (s = (s * 1664525 + 1013904223) >>> 0) / 4294967296;
  for (let i = 0; i < 480; i++) {
    const x = r() * cv.width, y = r() * cv.height, k = r(), a = 0.25 + r() * 0.7;
    g.fillStyle = k > 0.96 ? `rgba(200,240,255,${a})` : k > 0.9 ? `rgba(190,170,255,${a * 0.8})` : `rgba(233,241,255,${a * 0.7})`;
    const size = (k > 0.97 ? 2 : 1) * dpr;
    g.fillRect(x, y, size, size);
  }
}
addEventListener("resize", drawStars);
drawStars();

/* ---------- navegação ---------- */
$("nav").addEventListener("click", (e) => {
  const b = e.target.closest("button[data-view]");
  if (!b) return;
  document.querySelectorAll("#nav button").forEach((x) => x.classList.toggle("on", x === b));
  document.querySelectorAll(".view").forEach((v) => v.classList.toggle("on", v.id === "view-" + b.dataset.view));
});
$("btnMin").onclick = () => api.minimize();
$("btnClose").onclick = () => api.close();

/* ---------- conta ---------- */
function renderAccount(profile) {
  const box = $("acct");
  box.textContent = "";
  if (!profile) {
    const b = document.createElement("button");
    b.type = "button";
    b.className = "btn-ghost";
    b.textContent = "Entrar com Microsoft";
    b.onclick = doLogin;
    box.append(b);
    return;
  }
  const info = document.createElement("div");
  info.style.lineHeight = "1.2";
  info.innerHTML = `<span class="name"></span><br><span class="st">● conectado</span> <button type="button" class="link">sair</button>`;
  info.querySelector(".name").textContent = profile.name;
  info.querySelector(".link").onclick = async () => {
    await api.logout();
    renderAccount(null);
  };
  box.append(info);
}

async function doLogin() {
  setStatus("Abrindo o login da Microsoft…");
  try {
    renderAccount(await api.login());
    setStatus("Pronto para jogar");
  } catch (err) {
    setStatus("Não deu para entrar: " + cleanErr(err), true);
  }
}

/* ---------- jogar ---------- */
function setStatus(text, isError = false) {
  const el = $("status");
  el.textContent = text;
  el.classList.toggle("err", isError);
}
const cleanErr = (err) => String(err && err.message ? err.message : err).replace(/^Error invoking remote method '[^']+': (Error: )?/, "");

$("btnPlay").onclick = async () => {
  $("btnPlay").disabled = true;
  document.body.classList.add("playing");
  try {
    await api.launch();
  } catch (err) {
    setStatus(cleanErr(err), true);
    $("btnPlay").disabled = false;
    document.body.classList.remove("playing");
    $("bar").hidden = true;
  }
};

api.onStatus((s) => {
  setStatus(s.text);
  const bar = $("bar");
  if (s.step === "download" && s.total) {
    bar.hidden = false;
    $("barFill").style.width = Math.round((s.done / s.total) * 100) + "%";
  } else {
    bar.hidden = true;
  }
  if (s.step === "closed") {
    $("btnPlay").disabled = false;
    document.body.classList.remove("playing");
    if (!/erro/.test(s.text)) setStatus(s.text);
    else setStatus(s.text, true);
  }
});

const logLines = [];
api.onLog((line) => {
  logLines.push(line.trimEnd());
  if (logLines.length > 2000) logLines.splice(0, logLines.length - 2000);
  const pre = $("log");
  pre.textContent = logLines.join("\n");
  pre.scrollTop = pre.scrollHeight;
});

$("btnCopyLog").onclick = async () => {
  try {
    await navigator.clipboard.writeText(logLines.join("\n"));
    $("btnCopyLog").textContent = "copiado";
  } catch {
    $("btnCopyLog").textContent = "não deu para copiar";
  }
  setTimeout(() => ($("btnCopyLog").textContent = "copiar"), 2000);
};

/* ---------- mods ---------- */
function isOn(m) {
  return m.pronto && enabled[m.id] !== false;
}

function updateCounts() {
  const ready = MODS.filter((m) => m.pronto);
  const on = ready.filter(isOn).length;
  $("homeModCount").textContent = `${on} de ${ready.length}`;
  $("homeSub").hidden = $("loader").value === "optifine";
  $("modsSummary").textContent = `${on} mods ligados. ${ready.length} prontos e ${MODS.length - ready.length} a caminho.`;
  document.querySelectorAll("#mc [data-mod]").forEach((el) => {
    const m = MODS.find((x) => x.id === el.dataset.mod);
    el.hidden = !(m && isOn(m));
  });
}

function renderChips() {
  const box = $("chips");
  box.textContent = "";
  ["Todos", ...Object.keys(CATS)].forEach((c) => {
    const b = document.createElement("button");
    b.type = "button";
    b.textContent = c;
    b.setAttribute("aria-pressed", String(c === cat));
    b.onclick = () => {
      cat = c;
      renderChips();
      renderGrid();
    };
    box.append(b);
  });
}

function renderGrid() {
  const q = $("search").value.trim().toLowerCase();
  const onlyReady = $("onlyReady").checked;
  const grid = $("grid");
  grid.textContent = "";
  MODS.filter((m) => (cat === "Todos" || m.cat === cat) && (!onlyReady || m.pronto) && (!q || (m.nome + " " + m.desc).toLowerCase().includes(q)))
    .sort((a, b) => b.pronto - a.pronto)
    .forEach((m) => {
      const c = document.createElement("div");
      c.className = "mcard" + (m.pronto ? "" : " soon");
      c.innerHTML = `<div class="head"><b></b><button class="tog" type="button"></button></div><p></p><small><span></span><span></span></small>`;
      c.querySelector("b").textContent = m.nome;
      c.querySelector("p").textContent = m.desc;
      const [catEl, stEl] = c.querySelectorAll("small span");
      catEl.textContent = m.cat;
      stEl.textContent = m.pronto ? "pronto" : "em breve";
      if (m.pronto) stEl.className = "ready";
      const t = c.querySelector(".tog");
      t.setAttribute("aria-label", (isOn(m) ? "Desligar " : "Ligar ") + m.nome);
      t.setAttribute("aria-pressed", String(isOn(m)));
      t.disabled = !m.pronto;
      t.onclick = async () => {
        enabled = await api.setMod(m.id, !isOn(m));
        renderGrid();
        updateCounts();
      };
      grid.append(c);
    });
}
$("search").addEventListener("input", renderGrid);
$("onlyReady").addEventListener("change", renderGrid);

/* ---------- ajustes ---------- */
function renderOptifine(cfg) {
  const el = $("ofFile");
  el.textContent = cfg.optifinePath ? cfg.optifinePath.split(/[\\/]/).pop() : "Nenhum arquivo escolhido";
  el.classList.toggle("ok", !!cfg.optifinePath);
}

async function loadSettings() {
  const cfg = await api.getConfig();
  $("mem").value = cfg.memoryMaxGb;
  $("memOut").textContent = cfg.memoryMaxGb + " GB";
  $("java").value = cfg.javaPath;
  $("dir").value = cfg.gameDir;
  $("loader").value = cfg.loader;
  renderUpdateToken(cfg);
  renderOptifine(cfg);
  updateCounts();
}
$("mem").addEventListener("input", () => ($("memOut").textContent = $("mem").value + " GB"));
function renderUpdateToken(cfg) {
  $("updToken").value = "";
  $("updToken").placeholder = cfg.hasUpdateToken ? "Chave salva. Cole outra para trocar." : "Cole a chave do GitHub (github_pat_…)";
}
$("btnUpdToken").onclick = async () => {
  const cfg = await api.setUpdateToken($("updToken").value);
  renderUpdateToken(cfg);
  $("saved").textContent = cfg.hasUpdateToken ? "Chave salva. Procurando atualização…" : "Chave removida.";
};
$("btnPickOf").onclick = async () => renderOptifine(await api.pickOptifine());
$("btnOfSite").onclick = () => api.openLink("https://optifine.net/downloads");
$("loader").addEventListener("change", async () => {
  const cfg = await api.setConfig({ loader: $("loader").value });
  updateCounts();
  if (cfg.loader !== "forge" && !cfg.optifinePath) setStatus("Escolha o arquivo do OptiFine em Ajustes para usar esse modo.", true);
  else if (cfg.loader === "optifine") setStatus("Só OptiFine: mais leve, mas sem os mods do Pulsar.");
  else setStatus("Pronto para jogar");
});
$("settingsForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  await api.setConfig({ memoryMaxGb: Number($("mem").value), javaPath: $("java").value.trim(), gameDir: $("dir").value.trim() });
  enabled = await api.getMods();
  updateCounts();
  renderGrid();
  $("saved").textContent = "Salvo.";
  setTimeout(() => ($("saved").textContent = ""), 2000);
});

/* ---------- atualização ---------- */
api.onUpdate((u) => {
  const b = $("update");
  b.hidden = false;
  if (u.state === "downloading") {
    b.className = "update";
    b.textContent = `Baixando atualização${u.version ? " " + u.version : ""}… ${u.percent || 0}%`;
    if (u.version) b.dataset.version = u.version;
  } else if (u.state === "none") {
    b.hidden = true;
  } else if (u.state === "error") {
    b.className = "update";
    b.textContent = u.text;
  } else if (u.state === "ready") {
    b.className = "update ready";
    b.textContent = `Atualizar para ${u.version} e reiniciar`;
  }
});
$("update").onclick = async () => {
  if (!$("update").classList.contains("ready")) return;
  try {
    await api.installUpdate();
  } catch (err) {
    setStatus(cleanErr(err), true);
  }
};

/* ---------- início ---------- */
(async function init() {
  enabled = await api.getMods();
  renderChips();
  renderGrid();
  await loadSettings();
  api.appVersion().then((v) => ($("appVersion").textContent = "CLIENT " + v)).catch(() => {});
  renderAccount(null);
  setStatus("Conectando sua conta…");
  const profile = await api.restoreAccount().catch(() => null);
  renderAccount(profile);
  setStatus(profile ? "Pronto para jogar" : "Entre com sua conta Microsoft para jogar");
})();
