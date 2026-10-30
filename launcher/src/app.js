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
  if (b.dataset.view === "texturas") loadTexturas();
  if (b.dataset.view === "mods" && aba === "seus") loadUserMods();
});
$("btnMin").onclick = () => api.minimize();
$("btnClose").onclick = () => api.close();

/* ---------- conta ---------- */
// estado: { microsoft: {name, uuid} | null, offline: [nicks], ativa: "microsoft" | nick, liberado }
let contas = { microsoft: null, offline: [], ativa: "microsoft", liberado: false };

const prontaParaJogar = () => contas.ativa !== "microsoft" || !!contas.microsoft;

function el(tag, cls, texto) {
  const e = document.createElement(tag);
  if (cls) e.className = cls;
  if (texto !== undefined) e.textContent = texto;
  return e;
}

function avatar(nome, offline) {
  return el("span", "ava" + (offline ? " off" : ""), (nome || "?").slice(0, 1).toUpperCase());
}

function renderAccount(estado) {
  if (estado) contas = estado;
  const box = $("acct");
  box.textContent = "";
  const offline = contas.ativa !== "microsoft";
  const nome = offline ? contas.ativa : contas.microsoft && contas.microsoft.name;
  if (!nome && !contas.offline.length && !contas.liberado) {
    const b = el("button", "btn-ghost", "Entrar com Microsoft");
    b.type = "button";
    b.onclick = doLogin;
    box.append(b);
    return;
  }
  const atual = el("button", "acct-now");
  atual.type = "button";
  atual.setAttribute("aria-haspopup", "true");
  atual.setAttribute("aria-expanded", String(!$("acctMenu").hidden));
  atual.append(avatar(nome || "?", offline));
  const info = el("span", "acct-info");
  info.append(el("span", "name", nome || "Sem conta"), el("span", "st" + (offline ? " off" : ""), offline ? "● offline" : nome ? "● original" : "entre para jogar"));
  atual.append(info, el("span", "caret", "▾"));
  atual.onclick = (e) => {
    e.stopPropagation();
    $("acctMenu").hidden ? abrirContas() : fecharContas();
  };
  box.append(atual);
  renderMenuContas();
}

function renderMenuContas() {
  const m = $("acctMenu");
  m.textContent = "";
  m.append(el("h5", "", "Conta original"));
  if (contas.microsoft) {
    const linha = el("div", "conta" + (contas.ativa === "microsoft" ? " on" : ""));
    const usar = el("button", "conta-usar");
    usar.type = "button";
    usar.append(avatar(contas.microsoft.name, false), el("span", "", contas.microsoft.name), el("small", "", contas.ativa === "microsoft" ? "em uso" : "usar"));
    usar.onclick = () => acaoConta(() => api.useAccount("microsoft"));
    const sair = el("button", "linkbtn conta-x", "sair");
    sair.type = "button";
    sair.onclick = () => acaoConta(() => api.logout());
    linha.append(usar, sair);
    m.append(linha);
  } else {
    const b = el("button", "btn-ghost conta-ms", "Entrar com Microsoft");
    b.type = "button";
    b.onclick = doLogin;
    m.append(b);
  }

  m.append(el("h5", "", "Contas offline"));
  if (!contas.liberado) {
    m.append(el("p", "help", "Entre uma vez com uma conta Microsoft que tenha o Minecraft para liberar os nicks offline."));
    return;
  }
  for (const nick of contas.offline) {
    const linha = el("div", "conta" + (contas.ativa === nick ? " on" : ""));
    const usar = el("button", "conta-usar");
    usar.type = "button";
    usar.append(avatar(nick, true), el("span", "", nick), el("small", "", contas.ativa === nick ? "em uso" : "usar"));
    usar.onclick = () => acaoConta(() => api.useAccount(nick));
    const x = el("button", "linkbtn conta-x", "×");
    x.type = "button";
    x.setAttribute("aria-label", "Apagar " + nick);
    x.onclick = () => acaoConta(() => api.removeOffline(nick));
    linha.append(usar, x);
    m.append(linha);
  }
  const form = el("form", "conta-nova");
  const input = el("input");
  input.id = "novoNick";
  input.placeholder = "Novo nick";
  input.maxLength = 16;
  input.autocomplete = "off";
  input.spellcheck = false;
  const add = el("button", "btn-ghost", "Adicionar");
  add.type = "submit";
  form.append(input, add);
  form.onsubmit = (e) => {
    e.preventDefault();
    acaoConta(() => api.addOffline(input.value), true);
  };
  m.append(form, el("p", "help", "Para servidores que aceitam conta offline (pirata), LAN e jogar sem internet. Dentro do jogo dá para trocar pelo botão da conta no menu."));
  const erro = el("p", "help err");
  erro.id = "contaErro";
  m.append(erro);
}

async function acaoConta(fn, focarNick = false) {
  try {
    renderAccount(await fn());
    setStatus(prontaParaJogar() ? "Pronto para jogar" : "Entre com sua conta Microsoft para jogar");
    if (focarNick && $("novoNick")) $("novoNick").focus();
  } catch (err) {
    const e = $("contaErro");
    if (e) e.textContent = cleanErr(err);
    else setStatus(cleanErr(err), true);
  }
}

function abrirContas() {
  renderMenuContas();
  $("acctMenu").hidden = false;
  const b = document.querySelector(".acct-now");
  if (b) b.setAttribute("aria-expanded", "true");
}

function fecharContas() {
  $("acctMenu").hidden = true;
  const b = document.querySelector(".acct-now");
  if (b) b.setAttribute("aria-expanded", "false");
}
document.addEventListener("click", (e) => {
  if (!$("acctMenu").hidden && !e.target.closest("#acctMenu") && !e.target.closest("#acct")) fecharContas();
});
addEventListener("keydown", (e) => {
  if (e.key === "Escape") fecharContas();
});

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
    // A textura pode ter sido trocada dentro do jogo.
    if ($("view-texturas").classList.contains("on")) loadTexturas();
    $("btnPlay").disabled = false;
    document.body.classList.remove("playing");
    if (!/erro/.test(s.text)) setStatus(s.text);
    else setStatus(s.text, true);
  }
});

// O registro completo fica no Console do Pulsar; aqui só as últimas linhas, redesenhadas no máximo uma vez por quadro.
const logLines = [];
let logFrame = 0;
api.onLog((lines) => {
  logLines.push(...lines);
  if (logLines.length > 400) logLines.splice(0, logLines.length - 400);
  if (logFrame) return;
  logFrame = requestAnimationFrame(() => {
    logFrame = 0;
    const pre = $("log");
    pre.textContent = logLines.join("\n");
    pre.scrollTop = pre.scrollHeight;
  });
});
api.onLogReset(() => (logLines.length = 0));
api.onAccount((estado) => renderAccount(estado));
$("btnConsole").onclick = () => api.openConsole();

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
  return m.pronto && (enabled[m.id] ?? m.padrao);
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

/* ---------- seus mods ---------- */
let aba = "pulsar";
$("modsSeg").addEventListener("click", (e) => {
  const b = e.target.closest("button[data-aba]");
  if (!b) return;
  aba = b.dataset.aba;
  document.querySelectorAll("#modsSeg button").forEach((x) => x.setAttribute("aria-selected", String(x === b)));
  const seus = aba === "seus";
  $("userMods").hidden = !seus;
  document.querySelector(".mods-tools").hidden = seus;
  document.querySelector(".mods-body").hidden = seus;
  if (seus) loadUserMods();
});

function umMsg(text, isError = false) {
  $("umMsg").textContent = text;
  $("umMsg").classList.toggle("err", isError);
}

function renderUserMods(lista) {
  const box = $("umList");
  box.textContent = "";
  $("userModCount").textContent = lista.length ? String(lista.length) : "";
  if (!lista.length) {
    const vazio = document.createElement("div");
    vazio.className = "um-empty";
    vazio.innerHTML = "<b>Nenhum mod seu ainda.</b><span>Clique em Adicionar mods ou arraste os arquivos .jar para cá.</span>";
    box.append(vazio);
    return;
  }
  for (const m of lista) {
    const row = document.createElement("div");
    row.className = "um-row" + (m.ligado ? "" : " off");
    row.innerHTML = `<button class="tog" type="button"></button><div class="um-info"><b></b><small></small></div><button type="button" class="linkbtn um-del">remover</button>`;
    row.querySelector("b").textContent = m.nome;
    const det = [m.versao && "versão " + m.versao, m.mc && "Minecraft " + m.mc, m.arquivo].filter(Boolean).join("  ·  ");
    row.querySelector("small").textContent = m.aviso || det;
    if (m.aviso) row.querySelector("small").className = "warn";
    const t = row.querySelector(".tog");
    t.setAttribute("aria-pressed", String(m.ligado));
    t.setAttribute("aria-label", (m.ligado ? "Desligar " : "Ligar ") + m.nome);
    t.onclick = () => umAcao(() => api.alternarMod(m.arquivo, !m.ligado));
    row.querySelector(".um-del").onclick = () => umAcao(() => api.removerMod(m.arquivo), m.nome + " foi para a Lixeira.");
    box.append(row);
  }
}

async function umAcao(fn, ok = "") {
  try {
    renderUserMods(await fn());
    umMsg(ok);
  } catch (err) {
    umMsg(cleanErr(err), true);
  }
}

async function loadUserMods() {
  await umAcao(() => api.listUserMods());
}

async function adicionarMods(caminhos) {
  try {
    const r = await api.adicionarMods(caminhos);
    renderUserMods(r.mods);
    if (r.recusados.length) umMsg("Não entraram: " + r.recusados.join(" · "), true);
    else umMsg(caminhos && caminhos.length ? "Mods adicionados. Eles carregam na próxima vez que o jogo abrir." : "");
  } catch (err) {
    umMsg(cleanErr(err), true);
  }
}
$("btnAddMods").onclick = () => adicionarMods([]);
$("btnModsFolder").onclick = () => api.abrirPastaMods();
$("userMods").addEventListener("dragover", (e) => {
  e.preventDefault();
  $("userMods").classList.add("drop");
});
$("userMods").addEventListener("dragleave", () => $("userMods").classList.remove("drop"));
$("userMods").addEventListener("drop", (e) => {
  e.preventDefault();
  $("userMods").classList.remove("drop");
  const caminhos = [...e.dataTransfer.files].map((f) => api.caminhoDe(f)).filter(Boolean);
  if (caminhos.length) adicionarMods(caminhos);
});
// Arquivo solto fora da lista não abre no lugar do launcher.
addEventListener("dragover", (e) => e.preventDefault());
addEventListener("drop", (e) => e.preventDefault());

/* ---------- texturas ---------- */
const progresso = {};

function txMsg(text, isError = false) {
  $("txMsg").textContent = text;
  $("txMsg").classList.toggle("err", isError);
}

function tamanho(bytes) {
  const mb = bytes / (1024 * 1024);
  return mb < 10 ? mb.toFixed(1).replace(".", ",") + " MB" : Math.round(mb) + " MB";
}

function renderTexturas(lista) {
  $("txPvp").textContent = "";
  $("txQual").textContent = "";
  for (const p of lista) {
    const c = document.createElement("div");
    c.className = "tx-card" + (p.emUso ? " on" : "");
    c.dataset.id = p.id;
    c.innerHTML = `<div class="tx-ico"><img alt="" loading="lazy"><span></span></div>
      <div class="tx-info"><b></b><p></p><small></small></div>
      <div class="tx-act"><button type="button" class="tx-btn"></button><button type="button" class="linkbtn tx-sub"></button></div>`;
    const img = c.querySelector("img");
    img.src = p.icone;
    img.onerror = () => img.remove();
    c.querySelector(".tx-ico span").textContent = p.resolucao;
    c.querySelector("b").textContent = p.nome;
    c.querySelector("p").textContent = p.desc;
    c.querySelector("small").textContent = `${p.resolucao}  ·  ${tamanho(p.tamanho)}`;
    const btn = c.querySelector(".tx-btn");
    const sub = c.querySelector(".tx-sub");
    const pct = progresso[p.id];
    if (pct !== undefined) {
      btn.className = "tx-btn busy";
      btn.style.setProperty("--p", pct + "%");
      btn.textContent = pct + "%";
      btn.disabled = true;
      sub.hidden = true;
    } else if (p.emUso) {
      btn.className = "tx-btn using";
      btn.textContent = "Em uso";
      btn.title = "Clique para desligar";
      btn.onclick = () => txAcao(() => api.usarTextura(p.id, false), p.nome + " desligada.");
      sub.textContent = "página";
      sub.onclick = () => api.openLink(p.pagina);
    } else if (p.baixado) {
      btn.textContent = "Usar";
      btn.onclick = () => txAcao(() => api.usarTextura(p.id, true), `Usando ${p.nome}. Ela aparece na próxima vez que o jogo abrir.`);
      sub.textContent = "apagar";
      sub.onclick = () => txAcao(() => api.removerTextura(p.id), p.nome + " apagada.");
    } else {
      btn.className = "tx-btn get";
      btn.textContent = "Baixar";
      btn.onclick = () => baixarTextura(p);
      sub.textContent = "página";
      sub.onclick = () => api.openLink(p.pagina);
    }
    (p.tipo === "pvp" ? $("txPvp") : $("txQual")).append(c);
  }
}

let ultimaLista = [];
async function txAcao(fn, ok = "") {
  try {
    renderTexturas((ultimaLista = await fn()));
    txMsg(ok);
  } catch (err) {
    txMsg(cleanErr(err), true);
  }
}

async function loadTexturas() {
  await txAcao(() => api.listTexturas());
}

async function baixarTextura(p) {
  progresso[p.id] = 0;
  renderTexturas(ultimaLista);
  txMsg("");
  try {
    ultimaLista = await api.baixarTextura(p.id);
    txMsg(p.nome + " baixada. Clique em Usar.");
  } catch (err) {
    txMsg(`Não deu para baixar ${p.nome}: ${cleanErr(err)}`, true);
  } finally {
    delete progresso[p.id];
    renderTexturas(ultimaLista);
  }
}

// Durante o download só o botão do pacote muda (a lista não é redesenhada a cada pedaço).
api.onTexturaProgresso(({ id, pct }) => {
  if (!(id in progresso)) return;
  progresso[id] = pct;
  const btn = document.querySelector(`.tx-card[data-id="${CSS.escape(id)}"] .tx-btn`);
  if (!btn) return;
  btn.style.setProperty("--p", pct + "%");
  btn.textContent = pct + "%";
});
$("btnTexFolder").onclick = () => api.abrirPastaTexturas();

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
  $("openConsole").checked = cfg.openConsole !== false;
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
  const cfg = await api.setConfig({
    memoryMaxGb: Number($("mem").value),
    javaPath: $("java").value.trim(),
    gameDir: $("dir").value.trim(),
    openConsole: $("openConsole").checked,
  });
  $("dir").value = cfg.gameDir;
  enabled = await api.getMods();
  updateCounts();
  renderGrid();
  loadUserMods();
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
    b.textContent = `Atualizar para ${u.version}`;
    // A janela aparece uma vez por versão; depois fica o botão no topo.
    if (avisada !== u.version) {
      avisada = u.version;
      abrirAtualizacao(u);
    }
  }
});

let avisada = "";
function abrirAtualizacao(u) {
  $("updVer").textContent = u.version || "";
  $("updNotes").textContent = u.notas || "Melhorias e correções para o seu PvP.";
  $("updHelp2").textContent = "O Pulsar fecha, atualiza e abre de novo sozinho em alguns segundos.";
  $("updHelp2").classList.remove("err");
  $("updModal").hidden = false;
}

async function instalarAtualizacao() {
  try {
    $("updNow").disabled = true;
    await api.installUpdate();
  } catch (err) {
    $("updHelp2").textContent = cleanErr(err);
    $("updHelp2").classList.add("err");
    setStatus(cleanErr(err), true);
  } finally {
    $("updNow").disabled = false;
  }
}
$("update").onclick = () => {
  if (!$("update").classList.contains("ready")) return;
  $("updModal").hidden = false;
};
$("updNow").onclick = instalarAtualizacao;
$("updLater").onclick = () => ($("updModal").hidden = true);
$("updModal").addEventListener("click", (e) => {
  if (e.target === $("updModal")) $("updModal").hidden = true;
});

/* ---------- início ---------- */
(async function init() {
  enabled = await api.getMods();
  renderChips();
  renderGrid();
  await loadSettings();
  api.listUserMods().then(renderUserMods).catch(() => {});
  api.appVersion().then((v) => ($("appVersion").textContent = "CLIENT " + v)).catch(() => {});
  renderAccount(null);
  setStatus("Conectando sua conta…");
  const estado = await api.restoreAccount().catch(() => null);
  renderAccount(estado);
  setStatus(prontaParaJogar() ? "Pronto para jogar" : "Entre com sua conta Microsoft para jogar");
})();
