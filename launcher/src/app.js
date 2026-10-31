const api = window.pulsar;
const MODS = window.PULSAR_MODS;
const CATS = window.PULSAR_CATS;
const $ = (id) => document.getElementById(id);
const i18n = window.i18n;
const t = (texto) => i18n.t(texto);
// Primeira tradução já na abertura, sem esperar a configuração (evita piscar em português).
let idiomaGuardado = "";
try {
  idiomaGuardado = localStorage.getItem("pulsar.idioma") || "";
} catch {}
i18n.definir(idiomaGuardado || navigator.language);

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
    const b = el("button", "btn-ghost", t("Entrar com Microsoft"));
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
  info.append(el("span", "name", nome || t("Sem conta")), el("span", "st" + (offline ? " off" : ""), t(offline ? "● offline" : nome ? "● original" : "entre para jogar")));
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
  m.append(el("h5", "", t("Conta original")));
  if (contas.microsoft) {
    const linha = el("div", "conta" + (contas.ativa === "microsoft" ? " on" : ""));
    const usar = el("button", "conta-usar");
    usar.type = "button";
    usar.append(avatar(contas.microsoft.name, false), el("span", "", contas.microsoft.name), el("small", "", t(contas.ativa === "microsoft" ? "em uso" : "usar")));
    usar.onclick = () => acaoConta(() => api.useAccount("microsoft"));
    const sair = el("button", "linkbtn conta-x", t("sair"));
    sair.type = "button";
    sair.onclick = () => acaoConta(() => api.logout());
    linha.append(usar, sair);
    m.append(linha);
  } else {
    const b = el("button", "btn-ghost conta-ms", t("Entrar com Microsoft"));
    b.type = "button";
    b.onclick = doLogin;
    m.append(b);
  }

  m.append(el("h5", "", t("Contas offline")));
  if (!contas.liberado) {
    m.append(el("p", "help", t("Entre uma vez com uma conta Microsoft que tenha o Minecraft para liberar os nicks offline.")));
    return;
  }
  for (const nick of contas.offline) {
    const linha = el("div", "conta" + (contas.ativa === nick ? " on" : ""));
    const usar = el("button", "conta-usar");
    usar.type = "button";
    usar.append(avatar(nick, true), el("span", "", nick), el("small", "", t(contas.ativa === nick ? "em uso" : "usar")));
    usar.onclick = () => acaoConta(() => api.useAccount(nick));
    const x = el("button", "linkbtn conta-x", "×");
    x.type = "button";
    x.setAttribute("aria-label", t("Apagar " + nick));
    x.onclick = () => acaoConta(() => api.removeOffline(nick));
    linha.append(usar, x);
    m.append(linha);
  }
  const form = el("form", "conta-nova");
  const input = el("input");
  input.id = "novoNick";
  input.placeholder = t("Novo nick");
  input.maxLength = 16;
  input.autocomplete = "off";
  input.spellcheck = false;
  const add = el("button", "btn-ghost", t("Adicionar"));
  add.type = "submit";
  form.append(input, add);
  form.onsubmit = (e) => {
    e.preventDefault();
    acaoConta(() => api.addOffline(input.value), true);
  };
  m.append(form, el("p", "help", t("Para servidores que aceitam conta offline (pirata), LAN e jogar sem internet. Dentro do jogo dá para trocar pelo botão da conta no menu.")));
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
    if (e) e.textContent = t(cleanErr(err));
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
// O texto chega em português (do launcher ou daqui) e é traduzido na hora de mostrar.
let ultimoStatus = ["", false];
function setStatus(text, isError = false) {
  ultimoStatus = [text, isError];
  const el = $("status");
  el.textContent = t(text);
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
// O registro fica fora da tradução automática (são as linhas do jogo); só o aviso de vazio muda de idioma.
function logVazio() {
  if (!logLines.length) $("log").textContent = t("Os logs do Minecraft aparecem aqui quando o jogo abrir.");
}
logVazio();
api.onAccount((estado) => renderAccount(estado));
$("btnConsole").onclick = () => api.openConsole();

$("btnCopyLog").onclick = async () => {
  try {
    await navigator.clipboard.writeText(logLines.join("\n"));
    $("btnCopyLog").textContent = t("copiado");
  } catch {
    $("btnCopyLog").textContent = t("não deu para copiar");
  }
  setTimeout(() => ($("btnCopyLog").textContent = t("copiar")), 2000);
};

/* ---------- mods ---------- */
function isOn(m) {
  return m.pronto && (enabled[m.id] ?? m.padrao);
}

function updateCounts() {
  const ready = MODS.filter((m) => m.pronto);
  const on = ready.filter(isOn).length;
  $("homeModCount").textContent = t(`${on} de ${ready.length}`);
  $("homeSub").hidden = $("loader").value === "optifine";
  $("modsSummary").textContent = t(`${on} mods ligados. ${ready.length} prontos e ${MODS.length - ready.length} a caminho.`);
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
    b.textContent = t(c);
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
  const busca = (m) => (m.nome + " " + m.desc + " " + t(m.nome) + " " + t(m.desc)).toLowerCase();
  MODS.filter((m) => (cat === "Todos" || m.cat === cat) && (!onlyReady || m.pronto) && (!q || busca(m).includes(q)))
    .sort((a, b) => b.pronto - a.pronto)
    .forEach((m) => {
      const c = document.createElement("div");
      c.className = "mcard" + (m.pronto ? "" : " soon");
      c.innerHTML = `<div class="head"><b></b><button class="tog" type="button"></button></div><p></p><small><span></span><span></span></small>`;
      c.querySelector("b").textContent = t(m.nome);
      c.querySelector("p").textContent = t(m.desc);
      const [catEl, stEl] = c.querySelectorAll("small span");
      catEl.textContent = t(m.cat);
      stEl.textContent = t(m.pronto ? "pronto" : "em breve");
      if (m.pronto) stEl.className = "ready";
      const tog = c.querySelector(".tog");
      tog.setAttribute("aria-label", t((isOn(m) ? "Desligar " : "Ligar ") + m.nome));
      tog.setAttribute("aria-pressed", String(isOn(m)));
      tog.disabled = !m.pronto;
      tog.onclick = async () => {
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
  $("umMsg").textContent = t(text);
  $("umMsg").classList.toggle("err", isError);
}

function renderUserMods(lista) {
  const box = $("umList");
  box.textContent = "";
  $("userModCount").textContent = lista.length ? String(lista.length) : "";
  if (!lista.length) {
    const vazio = document.createElement("div");
    vazio.className = "um-empty";
    vazio.append(el("b", "", t("Nenhum mod seu ainda.")), el("span", "", t("Clique em Adicionar mods ou arraste os arquivos .jar para cá.")));
    box.append(vazio);
    return;
  }
  for (const m of lista) {
    const row = document.createElement("div");
    row.className = "um-row" + (m.ligado ? "" : " off");
    row.innerHTML = `<button class="tog" type="button"></button><div class="um-info"><b></b><small></small></div><button type="button" class="linkbtn um-del"></button>`;
    row.querySelector(".um-del").textContent = t("remover");
    row.querySelector("b").textContent = m.nome;
    const det = [m.versao && t("versão " + m.versao), m.mc && "Minecraft " + m.mc, m.arquivo].filter(Boolean).join("  ·  ");
    row.querySelector("small").textContent = m.aviso ? t(m.aviso) : det;
    if (m.aviso) row.querySelector("small").className = "warn";
    const tog = row.querySelector(".tog");
    tog.setAttribute("aria-pressed", String(m.ligado));
    tog.setAttribute("aria-label", t((m.ligado ? "Desligar " : "Ligar ") + m.nome));
    tog.onclick = () => umAcao(() => api.alternarMod(m.arquivo, !m.ligado));
    row.querySelector(".um-del").onclick = () => umAcao(() => api.removerMod(m.arquivo), m.nome + " foi para a Lixeira.");
    box.append(row);
  }
}

let ultimosMods = null;
async function umAcao(fn, ok = "") {
  try {
    renderUserMods((ultimosMods = await fn()));
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
  $("txMsg").textContent = t(text);
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
    c.querySelector("p").textContent = t(p.desc);
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
      btn.textContent = t("Em uso");
      btn.title = t("Clique para desligar");
      btn.onclick = () => txAcao(() => api.usarTextura(p.id, false), p.nome + " desligada.");
      sub.textContent = t("página");
      sub.onclick = () => api.openLink(p.pagina);
    } else if (p.baixado) {
      btn.textContent = t("Usar");
      btn.onclick = () => txAcao(() => api.usarTextura(p.id, true), `Usando ${p.nome}. Ela aparece na próxima vez que o jogo abrir.`);
      sub.textContent = t("apagar");
      sub.onclick = () => txAcao(() => api.removerTextura(p.id), p.nome + " apagada.");
    } else {
      btn.className = "tx-btn get";
      btn.textContent = t("Baixar");
      btn.onclick = () => baixarTextura(p);
      sub.textContent = t("página");
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

/* ---------- idioma ---------- */
// Bandeira à esquerda e só o nome da língua. A troca vale na hora, sem precisar salvar.
function renderIdiomas() {
  const box = $("idiomas");
  box.textContent = "";
  for (const i of i18n.IDIOMAS) {
    const b = el("button");
    b.type = "button";
    b.setAttribute("role", "radio");
    b.setAttribute("aria-checked", String(i.id === i18n.atual()));
    b.setAttribute("aria-label", i.rotulo);
    const flag = el("span", "flag");
    flag.innerHTML = i18n.bandeira(i.id);
    b.append(flag, el("span", "", i.nome));
    b.onclick = () => trocarIdioma(i.id);
    box.append(b);
  }
}

async function trocarIdioma(id) {
  if (id === i18n.atual()) return;
  i18n.definir(id);
  retraduzir();
  try {
    await api.setConfig({ idioma: i18n.atual() });
  } catch {}
}

// Redesenha o que o launcher monta na hora (o HTML fixo o i18n.definir já trocou).
function retraduzir() {
  renderIdiomas();
  renderChips();
  renderGrid();
  updateCounts();
  renderAccount(null);
  if (!$("acctMenu").hidden) renderMenuContas();
  if (ultimosMods) renderUserMods(ultimosMods);
  if (ultimaLista.length) renderTexturas(ultimaLista);
  setStatus(...ultimoStatus);
  logVazio();
  if (ultimaAtualizacao) mostrarAtualizacao(ultimaAtualizacao);
  $("memOut").textContent = t($("mem").value + " GB");
  ["umMsg", "txMsg", "saved"].forEach((id) => ($(id).textContent = ""));
  api.getConfig().then((cfg) => {
    renderOptifine(cfg);
    $("updToken").placeholder = t(cfg.hasUpdateToken ? "Chave salva. Cole outra para trocar." : "Cole a chave do GitHub (github_pat_…)");
  });
}

/* ---------- ajustes ---------- */
function renderOptifine(cfg) {
  const el = $("ofFile");
  el.textContent = cfg.optifinePath ? cfg.optifinePath.split(/[\\/]/).pop() : t("Nenhum arquivo escolhido");
  el.classList.toggle("ok", !!cfg.optifinePath);
}

async function loadSettings() {
  const cfg = await api.getConfig();
  $("mem").value = cfg.memoryMaxGb;
  $("memOut").textContent = t(cfg.memoryMaxGb + " GB");
  $("java").value = cfg.javaPath;
  $("dir").value = cfg.gameDir;
  $("loader").value = cfg.loader;
  $("openConsole").checked = cfg.openConsole !== false;
  renderUpdateToken(cfg);
  renderOptifine(cfg);
  updateCounts();
}
$("mem").addEventListener("input", () => ($("memOut").textContent = t($("mem").value + " GB")));
function renderUpdateToken(cfg) {
  $("updToken").value = "";
  $("updToken").placeholder = t(cfg.hasUpdateToken ? "Chave salva. Cole outra para trocar." : "Cole a chave do GitHub (github_pat_…)");
}
$("btnUpdToken").onclick = async () => {
  const cfg = await api.setUpdateToken($("updToken").value);
  renderUpdateToken(cfg);
  $("saved").textContent = t(cfg.hasUpdateToken ? "Chave salva. Procurando atualização…" : "Chave removida.");
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
  $("saved").textContent = t("Salvo.");
  setTimeout(() => ($("saved").textContent = ""), 2000);
});

/* ---------- atualização ---------- */
let ultimaAtualizacao = null;
api.onUpdate(mostrarAtualizacao);
function mostrarAtualizacao(u) {
  ultimaAtualizacao = u;
  const b = $("update");
  b.hidden = false;
  if (u.state === "downloading") {
    b.className = "update";
    b.textContent = t(`Baixando atualização${u.version ? " " + u.version : ""}… ${u.percent || 0}%`);
    if (u.version) b.dataset.version = u.version;
  } else if (u.state === "none") {
    b.hidden = true;
  } else if (u.state === "error") {
    b.className = "update";
    b.textContent = t(u.text);
  } else if (u.state === "ready") {
    b.className = "update ready";
    b.textContent = t(`Atualizar para ${u.version}`);
    // A janela aparece uma vez por versão; depois fica o botão no topo.
    if (avisada !== u.version) {
      avisada = u.version;
      abrirAtualizacao(u);
    }
  }
}

let avisada = "";
function abrirAtualizacao(u) {
  $("updVer").textContent = u.version || "";
  $("updNotes").textContent = u.notas || t("Melhorias e correções para o seu PvP.");
  $("updHelp2").textContent = t("O Pulsar fecha, atualiza e abre de novo sozinho em alguns segundos.");
  $("updHelp2").classList.remove("err");
  $("updModal").hidden = false;
}

async function instalarAtualizacao() {
  try {
    $("updNow").disabled = true;
    await api.installUpdate();
  } catch (err) {
    $("updHelp2").textContent = t(cleanErr(err));
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

/* ---------- rádio pulsar ---------- */
// O jogo pede pela ponte e o launcher toca. Um elemento de áudio só, criado na primeira vez.
// Cada estação vem com endereços reserva: se um falhar, tenta o próximo antes de avisar.
let radio = null;
let radioPedido = null;
function radioTocar(i) {
  const c = radioPedido;
  if (!c || i >= c.urls.length) {
    if (c) api.radioErro(`não deu para tocar ${c.nome} (${radio && radio.error ? "erro " + radio.error.code : "sem resposta"})`);
    return;
  }
  radio.dataset.indice = String(i);
  radio.src = c.urls[i];
  radio.play().catch((e) => {
    // "AbortError" é só a troca de estação no meio; os outros erros caem no evento "error".
    if (e && e.name === "NotAllowedError") api.radioErro(`não deu para tocar ${c.nome}: ${e.message}`);
  });
}
api.onRadio((c) => {
  if (!c.tocando) {
    radioPedido = null;
    if (radio) {
      radio.pause();
      radio.removeAttribute("src");
      radio.load();
    }
    return;
  }
  if (!radio) {
    radio = new Audio();
    radio.preload = "none";
    radio.addEventListener("error", () => radioPedido && radioTocar(Number(radio.dataset.indice || 0) + 1));
  }
  radio.volume = Math.max(0, Math.min(1, c.volume / 100));
  const mesma = radioPedido && radioPedido.nome === c.nome;
  radioPedido = c;
  if (mesma && !radio.paused) return;
  radioTocar(0);
});

/* ---------- início ---------- */
(async function init() {
  // O idioma salvo no launcher manda; o do navegador só vale até a configuração chegar.
  const cfg = await api.getConfig().catch(() => ({}));
  if (cfg.idioma && i18n.escolher(cfg.idioma) !== i18n.atual()) i18n.definir(cfg.idioma);
  renderIdiomas();
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
