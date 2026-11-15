// Modo Normal do launcher: o modelo completo, ligado ao launcher de verdade (window.pulsar, o mesmo do modo Lite).
// O texto é escrito em português e traduzido na hora de mostrar (i18n.js); o HTML fixo é traduzido pelo i18n.definir.
(() => {
  "use strict";
  const api = window.pulsar;
  const MODS = window.PULSAR_MODS;
  const CATS = window.PULSAR_CATS;
  const NOVOS = window.PULSAR_NOVOS || [];
  const ORBITAS = window.PULSAR_ORBITAS || [];
  const NOVIDADES = window.PULSAR_NOVIDADES || [];
  const i18n = window.i18n;
  const t = (texto) => i18n.t(texto);
  const $ = (id) => document.getElementById(id);
  const $$ = (sel, raiz = document) => Array.from(raiz.querySelectorAll(sel));

  let idiomaGuardado = "";
  try {
    idiomaGuardado = localStorage.getItem("pulsar.idioma") || "";
  } catch {}
  i18n.definir(idiomaGuardado || navigator.language);

  const cleanErr = (err) => String(err && err.message ? err.message : err).replace(/^Error invoking remote method '[^']+': (Error: )?/, "");

  function el(tag, cls, texto) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (texto !== undefined) e.textContent = texto;
    return e;
  }
  function icone(id, cls) {
    const s = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    if (cls) s.setAttribute("class", cls);
    const u = document.createElementNS("http://www.w3.org/2000/svg", "use");
    u.setAttribute("href", "#" + id);
    s.append(u);
    return s;
  }
  function botao(cls, texto, onclick, ico) {
    const b = el("button", cls);
    b.type = "button";
    if (ico) b.append(icone(ico));
    b.append(document.createTextNode(texto));
    if (onclick) b.onclick = onclick;
    return b;
  }

  /* ---------- avisos (toasts) ---------- */
  function toast(titulo, texto, tom) {
    const item = el("div", "toast " + (tom || ""));
    const copy = el("span");
    copy.append(el("strong", "", t(titulo)), el("small", "", t(texto || "")));
    const x = el("button");
    x.type = "button";
    x.setAttribute("aria-label", t("Fechar aviso"));
    x.append(icone("i-x"));
    x.onclick = () => item.remove();
    item.append(el("i"), copy, x);
    $("toastRegion").append(item);
    setTimeout(() => {
      item.style.opacity = "0";
      item.style.transform = "translateX(20px)";
      setTimeout(() => item.remove(), 260);
    }, 4300);
  }

  /* ---------- abas ---------- */
  let aba = "pulsar";
  function setTab(tab) {
    $$(".tab-panel").forEach((p) => {
      const on = p.dataset.panel === tab;
      p.classList.toggle("is-active", on);
      p.hidden = !on;
    });
    $$("#nav [data-tab]").forEach((b) => {
      const on = b.dataset.tab === tab;
      b.classList.toggle("is-active", on);
      b.setAttribute("aria-selected", String(on));
    });
    $("stage").scrollTo({ top: 0 });
    if (tab === "texturas") loadTexturas();
    if (tab === "mods" && aba === "seus") loadUserMods();
  }
  $("nav").addEventListener("click", (e) => {
    const b = e.target.closest("button[data-view]");
    if (b) setTab(b.dataset.view);
  });
  $$("[data-tab-target]").forEach((b) => b.addEventListener("click", () => setTab(b.dataset.tabTarget)));
  $$("[data-tab-link]").forEach((a) =>
    a.addEventListener("click", (e) => {
      e.preventDefault();
      setTab(a.dataset.tabLink);
    })
  );
  $("btnMin").onclick = () => api.minimize();
  $("btnMax").onclick = () => api.maximize();
  $("btnClose").onclick = () => api.close();

  /* ---------- menus de cima ---------- */
  function setPopovers(ativo) {
    [
      { button: $("notificationButton"), menu: $("notificationMenu"), nome: "novidades" },
      { button: $("profileButton"), menu: $("profileMenu"), nome: "conta" },
    ].forEach((it) => {
      const abrir = ativo === it.nome;
      it.menu.hidden = !abrir;
      it.button.setAttribute("aria-expanded", String(abrir));
    });
    if (ativo === "conta") renderMenuContas();
  }
  $("notificationButton").addEventListener("click", (e) => {
    e.stopPropagation();
    setPopovers($("notificationMenu").hidden ? "novidades" : null);
  });
  $("profileButton").addEventListener("click", (e) => {
    e.stopPropagation();
    setPopovers($("profileMenu").hidden ? "conta" : null);
  });
  $$(".popover").forEach((p) => p.addEventListener("click", (e) => e.stopPropagation()));
  document.addEventListener("click", () => setPopovers(null));

  /* ---------- painel lateral (planetas, novidades) ---------- */
  let ultimoFoco = null;
  function abrirPainel({ eyebrow, tag, titulo, paragrafos, lista, acoes }) {
    ultimoFoco = document.activeElement;
    $("drawerEyebrow").textContent = t(eyebrow || "Transmissão Pulsar");
    $("drawerTag").textContent = t(tag || "");
    $("drawerTitle").textContent = t(titulo);
    const body = $("drawerBody");
    body.textContent = "";
    for (const p of paragrafos || []) body.append(el("p", "", t(p)));
    if (lista && lista.length) {
      const ul = el("ul");
      for (const p of lista) ul.append(el("li", "", t(p)));
      body.append(ul);
    }
    const acts = $("drawerActions");
    acts.textContent = "";
    for (const a of acoes || []) {
      acts.append(
        botao("secondary-button", t(a.rotulo), () => {
          fecharPainel();
          acaoDoItem(a);
        }, a.link ? "i-external" : "i-check")
      );
    }
    if (!(acoes || []).length) {
      const ok = botao("secondary-button", t("Entendido"), fecharPainel, "i-check");
      ok.dataset.closeDrawer = "";
      acts.append(ok);
    }
    $("drawerLayer").hidden = false;
    requestAnimationFrame(() => document.querySelector(".drawer-close").focus());
  }
  function fecharPainel() {
    if ($("drawerLayer").hidden) return;
    $("drawerLayer").hidden = true;
    if (ultimoFoco && typeof ultimoFoco.focus === "function") ultimoFoco.focus();
  }
  $$("[data-close-drawer]").forEach((b) => b.addEventListener("click", fecharPainel));

  function acaoDoItem(a) {
    if (a.link) return api.openLink(a.link);
    if (a.acao === "console") return api.openConsole();
    if (a.acao === "mods") setTab("mods");
  }

  const tituloItem = (o, it) => (o.id === "novidades" ? t(`Versão ${it.id}`) : t(it.titulo));
  function abrirItem(o, it) {
    setPopovers(null);
    abrirPainel({
      eyebrow: o.id === "novidades" ? "Novidades" : o.nome,
      tag: o.id === "novidades" ? "Pulsar Client" : "Sinal recebido",
      titulo: tituloItem(o, it),
      paragrafos: o.id === "novidades" ? [] : it.itens,
      lista: o.id === "novidades" ? it.itens : [],
      acoes: it.acoes || [],
    });
  }

  /* ---------- Início: mapa orbital ---------- */
  // As novidades ficam nos planetas âmbar (história); sugestões e Discord nos ciano e violeta.
  const POS_HISTORIA = ["n1", "n2", "n3", "n4", "n5", "n6", "n7"];
  const POS_ACAO = ["a1", "a2", "a3", "a4", "a5", "a6", "a7"];
  function renderOrbitas() {
    const mapa = $("orbitalMap");
    $$(".orbit-node", mapa).forEach((n) => n.remove());
    const novidades = ORBITAS.find((o) => o.id === "novidades");
    if (novidades) {
      novidades.itens.slice(0, POS_HISTORIA.length).forEach((it, i) => {
        const b = el("button", "orbit-node history " + POS_HISTORIA[i]);
        b.type = "button";
        b.dataset.orbita = "novidades";
        b.dataset.item = it.id;
        b.append(el("i"), el("span", "", it.nome));
        b.setAttribute("aria-label", `${t("Novidades")}: ${tituloItem(novidades, it)}`);
        b.onclick = () => abrirItem(novidades, it);
        mapa.append(b);
      });
    }
    let k = 0;
    for (const o of ORBITAS.filter((x) => x.id !== "novidades")) {
      for (const it of o.itens) {
        if (k >= POS_ACAO.length) break;
        const cor = o.id === "discord" ? "violet" : "cyan";
        const b = el("button", `orbit-node action ${cor} ${POS_ACAO[k++]}`);
        b.type = "button";
        b.dataset.orbita = o.id;
        b.dataset.item = it.id;
        b.append(el("i"), el("span", "", t(it.nome)));
        b.setAttribute("aria-label", `${t(o.nome)}: ${tituloItem(o, it)}`);
        b.onclick = () => abrirItem(o, it);
        mapa.append(b);
      }
    }
    const chips = $("signalChips");
    chips.textContent = "";
    for (const o of [...ORBITAS].reverse()) {
      const cor = o.id === "novidades" ? "amber" : o.id === "discord" ? "violet" : "cyan";
      const b = el("button", "signal-chip " + cor);
      b.type = "button";
      b.dataset.orbita = o.id;
      b.append(el("i"), el("span", "", t(o.nome)), el("b", "", String(o.itens.length)));
      b.onclick = () => abrirOrbita(o);
      chips.append(b);
    }
  }
  function abrirOrbita(o) {
    ultimoFoco = document.activeElement;
    $("drawerEyebrow").textContent = t("Órbita");
    $("drawerTag").textContent = String(o.itens.length);
    $("drawerTitle").textContent = t(o.nome);
    const body = $("drawerBody");
    body.textContent = "";
    const acts = $("drawerActions");
    acts.textContent = "";
    for (const it of o.itens) acts.append(botao("secondary-button ghost", tituloItem(o, it), () => abrirItem(o, it)));
    $("drawerLayer").hidden = false;
  }
  $("pulsarCore").onclick = () =>
    abrirPainel({
      eyebrow: "Pulsar Core",
      tag: "1.8.9 PvP",
      titulo: "Sistema Pulsar",
      paragrafos: [
        "O núcleo liga o launcher, os mods, as texturas e as suas configurações. Em volta dele giram as novidades, as sugestões e o Discord.",
        "Clique em um planeta para abrir. Na aba Mods você monta o seu loadout; em Texturas troca o visual do jogo; em Versões escolhe a versão do Pulsar.",
      ],
    });

  function renderNovidadesMenu() {
    const lista = $("notificationList");
    lista.textContent = "";
    const o = ORBITAS.find((x) => x.id === "novidades");
    if (!o) return;
    $("notificationCount").textContent = t(`${o.itens.length} versões`);
    o.itens.forEach((it, i) => {
      const b = el("button");
      b.type = "button";
      b.append(el("i", "notice-icon " + (i === 0 ? "cyan" : i === 1 ? "violet" : "amber")));
      const s = el("span");
      s.append(el("strong", "", tituloItem(o, it)), el("small", "", t(it.itens[0] || "")));
      b.append(s);
      b.onclick = () => abrirItem(o, it);
      lista.append(b);
    });
  }

  /* ---------- conta ---------- */
  let contas = { microsoft: null, offline: [], ativa: "microsoft", liberado: false };
  const prontaParaJogar = () => contas.ativa !== "microsoft" || !!contas.microsoft;

  function avatar(nome, offline, cls) {
    const a = el("span", "avatar " + (cls || "") + (offline ? " off" : "") + (nome ? "" : " none"));
    if (nome) {
      a.append(el("b", "", nome.slice(0, 1).toUpperCase()));
      window.Cabecas.aplicar(a, nome);
    } else a.append(icone("i-user"));
    return a;
  }

  function renderAccount(estado) {
    if (estado) contas = estado;
    const offline = contas.ativa !== "microsoft";
    const nome = offline ? contas.ativa : contas.microsoft && contas.microsoft.name;
    const av = avatar(nome, offline);
    av.id = "profileAvatar";
    $("profileAvatar").replaceWith(av);
    $("profileName").textContent = nome || t("Entrar");
    $("profileState").textContent = t(offline ? "offline" : nome ? "conta original" : "conta Microsoft");
    $("profileDot").className = offline ? "off" : nome ? "" : "none";
    renderMenuContas();
    renderContaAjustes();
  }

  function linhaConta(nome, offline, emUso, usar, remover, rotuloRemover) {
    const linha = el("div", "conta" + (emUso ? " on" : ""));
    const b = el("button", "conta-usar");
    b.type = "button";
    b.append(avatar(nome, offline), el("span", "", nome), el("small", "", t(emUso ? "em uso" : "usar")));
    b.onclick = () => acaoConta(usar);
    const x = botao("linkbtn conta-x", rotuloRemover, () => acaoConta(remover));
    if (rotuloRemover === "×") x.setAttribute("aria-label", t("Apagar " + nome));
    linha.append(b, x);
    return linha;
  }

  function blocoOffline(raiz, erroId) {
    if (!contas.liberado) {
      raiz.append(el("p", "help", t("Entre uma vez com uma conta Microsoft que tenha o Minecraft para liberar os nicks offline.")));
      return;
    }
    for (const nick of contas.offline) raiz.append(linhaConta(nick, true, contas.ativa === nick, () => api.useAccount(nick), () => api.removeOffline(nick), "×"));
    const form = el("form", "conta-nova");
    const input = el("input");
    input.placeholder = t("Novo nick");
    input.maxLength = 16;
    input.autocomplete = "off";
    input.spellcheck = false;
    const add = botao("secondary-button compact", t("Adicionar"));
    add.type = "submit";
    form.append(input, add);
    form.onsubmit = (e) => {
      e.preventDefault();
      acaoConta(() => api.addOffline(input.value), erroId);
    };
    raiz.append(form);
    const erro = el("p", "help err");
    erro.id = erroId;
    raiz.append(erro);
  }

  function renderMenuContas() {
    const m = $("profileMenu");
    m.textContent = "";
    const topo = el("div");
    const offline = contas.ativa !== "microsoft";
    const nome = offline ? contas.ativa : contas.microsoft && contas.microsoft.name;
    const info = el("span");
    info.append(el("strong", "", nome || t("Sem conta")), el("small", "", t(offline ? "Conta offline" : nome ? "Conta Microsoft" : "Entre para jogar")));
    topo.append(avatar(nome, offline, "large"), info);
    m.append(topo);
    m.append(el("h5", "", t("Conta original")));
    if (contas.microsoft) m.append(linhaConta(contas.microsoft.name, false, contas.ativa === "microsoft", () => api.useAccount("microsoft"), () => api.logout(), t("sair")));
    else m.append(botao("secondary-button conta-ms", t("Entrar com Microsoft"), doLogin));
    m.append(el("h5", "", t("Contas offline")));
    blocoOffline(m, "contaErro");
    const ger = botao("", t("Gerenciar conta"), () => {
      setPopovers(null);
      setTab("settings");
      setSettingsTab("account");
    });
    m.append(ger);
  }

  function renderContaAjustes() {
    const card = $("accountCard");
    card.textContent = "";
    const offline = contas.ativa !== "microsoft";
    const nome = offline ? contas.ativa : contas.microsoft && contas.microsoft.name;
    const info = el("div");
    info.append(el("strong", "", nome || t("Sem conta")), el("small", "", t(offline ? "Conta offline em uso" : nome ? "Conta Microsoft em uso" : "Entre com a sua conta Microsoft para jogar")));
    const acoes = el("div", "conta-acoes");
    if (contas.microsoft) {
      if (contas.ativa !== "microsoft") acoes.append(botao("secondary-button compact", t("Usar a conta Microsoft"), () => acaoConta(() => api.useAccount("microsoft"))));
      acoes.append(botao("secondary-button compact ghost", t("Sair da conta"), () => acaoConta(() => api.logout())));
    } else acoes.append(botao("secondary-button compact", t("Entrar com Microsoft"), doLogin));
    card.append(avatar(nome, offline, "xlarge"), info, acoes);
    const lista = $("offlineList");
    lista.textContent = "";
    blocoOffline(lista, "contaErro2");
  }

  async function acaoConta(fn, erroId) {
    try {
      renderAccount(await fn());
      setStatus(prontaParaJogar() ? "Pronto para jogar" : "Entre com sua conta Microsoft para jogar");
    } catch (err) {
      const e = erroId && $(erroId);
      if (e) e.textContent = t(cleanErr(err));
      else toast("Conta", cleanErr(err), "error");
    }
  }

  async function doLogin() {
    setPopovers(null);
    setStatus("Abrindo o login da Microsoft…");
    try {
      renderAccount(await api.login());
      setStatus("Pronto para jogar");
      toast("Conta conectada", "Pronto para jogar.");
    } catch (err) {
      setStatus("Não deu para entrar: " + cleanErr(err), true);
    }
  }

  /* ---------- jogar ---------- */
  let ultimoStatus = ["", false];
  function setStatus(text, isError = false) {
    ultimoStatus = [text, isError];
    const s = $("status");
    s.textContent = t(text);
    s.classList.toggle("err", isError);
  }
  function setTopo(jogando) {
    document.body.classList.toggle("playing", jogando);
    $("topStatusText").textContent = t(jogando ? "Jogando" : "Pronto");
    $("topStatusText").classList.toggle("jogando", jogando);
    $("connectionPill").querySelector("span").textContent = t(jogando ? "Jogo aberto" : "Pronto");
    $("btnPlay").classList.toggle("is-running", jogando);
  }

  let mcVersoes = [];
  let mcEscolhida = "1.8.9";
  const mcAtual = () => mcVersoes.find((v) => v.id === mcEscolhida) || mcVersoes[0];
  const ehPulsar = () => !mcAtual() || mcAtual().tipo === "pulsar";

  function renderMc() {
    const v = mcAtual();
    if (!v) return;
    $("vBig").classList.toggle("van", v.tipo !== "pulsar");
    $("vSelo").textContent = t(v.selo);
    $("vNum").textContent = v.id;
    $("vDesc").textContent = t(v.desc);
    const feat = $("vFeat");
    feat.textContent = "";
    const chips = v.tipo === "pulsar" ? [["i-sword", "Animações 1.7"], ["i-zap", "Motor Pulsar"], [null, `+ ${MODS.filter((m) => m.pronto).length} mods`]] : [["i-cube", "Minecraft puro"], [null, `Java ${v.java}`]];
    for (const [ico, c] of chips) {
      const s = el("span");
      if (ico) s.append(icone(ico));
      s.append(document.createTextNode(t(c)));
      feat.append(s);
    }
    const rows = $("vRows");
    rows.textContent = "";
    for (const x of mcVersoes) {
      const b = el("button", x.id === v.id ? "is-active" : "", x.id);
      b.type = "button";
      b.dataset.versao = x.id;
      b.setAttribute("role", "radio");
      b.setAttribute("aria-checked", String(x.id === v.id));
      b.title = t(x.selo);
      b.disabled = document.body.classList.contains("playing");
      b.onclick = () => escolherMc(x.id);
      rows.append(b);
    }
    $("playVer").textContent = v.id;
    $("modeRow").hidden = v.tipo !== "pulsar";
    updateCounts();
    renderVersoes();
  }

  async function escolherMc(id) {
    if (id === mcEscolhida) return;
    try {
      const cfg = await api.setConfig({ version: id });
      mcEscolhida = cfg.version;
      renderMc();
      const v = mcAtual();
      setStatus(v.tipo === "pulsar" ? (prontaParaJogar() ? "Pronto para jogar" : "Entre com sua conta Microsoft para jogar") : `Minecraft ${v.id} puro: sem os mods do Pulsar.`);
    } catch (err) {
      setStatus(cleanErr(err), true);
    }
  }

  $("btnPlay").onclick = async () => {
    $("btnPlay").disabled = true;
    setTopo(true);
    renderMc();
    try {
      await api.launch();
    } catch (err) {
      setStatus(cleanErr(err), true);
      $("btnPlay").disabled = false;
      setTopo(false);
      renderMc();
      $("bar").hidden = true;
    }
  };

  api.onStatus((s) => {
    setStatus(s.text);
    const bar = $("bar");
    if (s.step === "download" && s.total) {
      bar.hidden = false;
      $("barFill").style.width = Math.round((s.done / s.total) * 100) + "%";
    } else bar.hidden = true;
    if (s.step === "closed") {
      if (!$("panel-texturas").hidden) loadTexturas();
      $("btnPlay").disabled = false;
      setTopo(false);
      renderMc();
      setStatus(s.text, /erro/.test(s.text));
    }
  });

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

  /* ---------- gravação (mod Gravação, pela ponte) ---------- */
  let recDesde = 0;
  let recTimer = 0;
  function renderRec() {
    const pill = $("recPill");
    pill.hidden = !recDesde;
    if (!recDesde) return;
    const s = Math.max(0, Math.round((Date.now() - recDesde) / 1000));
    $("recTempo").textContent = `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
  }
  function estadoGravacao(r) {
    recDesde = r && r.gravando ? r.desde || Date.now() : 0;
    clearInterval(recTimer);
    if (recDesde) recTimer = setInterval(renderRec, 1000);
    renderRec();
    if (r && r.arquivo) toast("Vídeo salvo", r.arquivo.split(/[\\/]/).pop());
  }
  api.onGravacao(estadoGravacao);
  $("recPill").onclick = () => api.abrirPastaVideos();

  /* ---------- mods ---------- */
  let enabled = {};
  let cat = "Todos";
  const isOn = (m) => m.pronto && (enabled[m.id] ?? m.padrao);
  const ICONES_CAT = { Combate: "i-sword", HUD: "i-grid", Visual: "i-eye", Servidores: "i-zap", Social: "i-user", Desempenho: "i-spark" };

  function updateCounts() {
    const ready = MODS.filter((m) => m.pronto);
    const on = ready.filter(isOn).length;
    $("homeModCount").textContent = String(on);
    $("homeSub").hidden = $("loader").value === "optifine" || !ehPulsar();
    $("modsNavCount").textContent = String(on);
    $("modsHeadingCount").textContent = t(`${on} ligados`);
    $("modsSummary").textContent = t(`${on} mods ligados. ${ready.length} prontos e ${MODS.length - ready.length} a caminho.`);
  }

  function renderChips() {
    const box = $("chips");
    box.textContent = "";
    ["Todos", ...Object.keys(CATS)].forEach((c) => {
      const b = el("button", "filter-chip" + (c === cat ? " is-active" : ""), t(c));
      b.type = "button";
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
    const lista = MODS.filter((m) => (cat === "Todos" || m.cat === cat) && (!onlyReady || m.pronto) && (!q || busca(m).includes(q))).sort((a, b) => b.pronto - a.pronto);
    for (const m of lista) {
      const on = isOn(m);
      const c = el("article", "mod-card" + (on ? " is-active" : "") + (m.pronto ? "" : " soon"));
      c.dataset.mod = m.id;
      c.style.setProperty("--cor", CATS[m.cat] || "var(--cyan)");
      const ico = el("div", "mod-icon");
      ico.append(icone(ICONES_CAT[m.cat] || "i-grid"));
      const copy = el("div", "mod-copy");
      const head = el("div");
      head.append(el("h3", "", t(m.nome)), el("span", "category-tag", t(m.cat)));
      if (NOVOS.includes(m.id)) head.append(el("span", "novo-tag", t("novo")));
      const meta = el("span", "mod-meta");
      meta.append(el("b", m.pronto ? "ready" : "", t(m.pronto ? "pronto" : "em breve")), el("i", "", "•"), el("span", "", t("Minecraft 1.8.9")));
      copy.append(head, el("p", "", t(m.desc)), meta);
      const acts = el("div", "mod-actions");
      const sw = el("label", "switch-control");
      sw.setAttribute("aria-label", t((on ? "Desligar " : "Ligar ") + m.nome));
      const input = el("input");
      input.type = "checkbox";
      input.setAttribute("role", "switch");
      input.checked = on;
      input.disabled = !m.pronto;
      input.onchange = async () => {
        try {
          enabled = await api.setMod(m.id, !isOn(m));
        } catch (err) {
          toast("Mods", cleanErr(err), "error");
        }
        renderGrid();
        updateCounts();
      };
      sw.append(input, el("i"));
      acts.append(sw);
      c.append(ico, copy, acts);
      grid.append(c);
    }
    $("modEmpty").hidden = lista.length > 0;
    grid.hidden = lista.length === 0;
  }
  $("search").addEventListener("input", renderGrid);
  $("onlyReady").addEventListener("change", renderGrid);

  /* ---------- seus mods ---------- */
  $("modsSeg").addEventListener("click", (e) => {
    const b = e.target.closest("button[data-aba]");
    if (!b) return;
    aba = b.dataset.aba;
    $$("#modsSeg button").forEach((x) => x.setAttribute("aria-selected", String(x === b)));
    const seus = aba === "seus";
    $("userMods").hidden = !seus;
    $("modsPulsar").hidden = seus;
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
      const vazio = el("div", "um-empty");
      vazio.append(el("b", "", t("Nenhum mod seu ainda.")), el("span", "", t("Clique em Adicionar mods ou arraste os arquivos .jar para cá.")));
      box.append(vazio);
      return;
    }
    for (const m of lista) {
      const row = el("div", "um-row" + (m.ligado ? "" : " off"));
      const sw = el("label", "switch-control");
      sw.setAttribute("aria-label", t((m.ligado ? "Desligar " : "Ligar ") + m.nome));
      const input = el("input");
      input.type = "checkbox";
      input.setAttribute("role", "switch");
      input.checked = !!m.ligado;
      input.onchange = () => umAcao(() => api.alternarMod(m.arquivo, !m.ligado));
      sw.append(input, el("i"));
      const info = el("div", "um-info");
      const det = [m.versao && t("versão " + m.versao), m.mc && "Minecraft " + m.mc, m.arquivo].filter(Boolean).join("  ·  ");
      const small = el("small", m.aviso ? "warn" : "", m.aviso ? t(m.aviso) : det);
      info.append(el("b", "", m.nome), small);
      const del = botao("linkbtn um-del", t("remover"), () => umAcao(() => api.removerMod(m.arquivo), m.nome + " foi para a Lixeira."));
      row.append(sw, info, del);
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
  const loadUserMods = () => umAcao(() => api.listUserMods());

  async function adicionarMods(caminhos) {
    try {
      const r = await api.adicionarMods(caminhos);
      renderUserMods((ultimosMods = r.mods));
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
  addEventListener("dragover", (e) => e.preventDefault());
  addEventListener("drop", (e) => e.preventDefault());

  /* ---------- texturas ---------- */
  const progresso = {};
  let filtroTextura = "all";
  let ultimaLista = [];

  function txMsg(text, isError = false) {
    $("txMsg").textContent = t(text);
    $("txMsg").classList.toggle("err", isError);
  }
  function tamanho(bytes) {
    const mb = bytes / (1024 * 1024);
    return mb < 10 ? mb.toFixed(1).replace(".", ",") + " MB" : Math.round(mb) + " MB";
  }

  function renderTexturas(lista) {
    ultimaLista = lista;
    const grid = $("textureGrid");
    grid.textContent = "";
    const visiveis = lista.filter((p) => filtroTextura === "all" || (filtroTextura === "baixadas" ? p.baixado || p.emUso : p.tipo === filtroTextura));
    for (const p of visiveis) {
      const pct = progresso[p.id];
      const c = el("article", "texture-card" + (p.emUso ? " is-active" : "") + (pct !== undefined ? " is-busy" : ""));
      c.dataset.id = p.id;
      const prev = el("div", "texture-preview");
      const img = el("img");
      img.alt = "";
      img.loading = "lazy";
      img.src = p.icone;
      img.onerror = () => img.remove();
      const badges = el("div", "texture-badges");
      if (p.emUso) badges.append(el("span", "badge cyan", t("Em uso")));
      else if (p.baixado) badges.append(el("span", "badge ghost", t("Baixada")));
      else badges.append(el("span", "badge amber", t("Nuvem")));
      badges.append(el("span", "badge ghost", t(p.tipo === "pvp" ? "PvP" : "Qualidade")));
      prev.append(img, badges, el("span", "res", p.resolucao));
      const body = el("div", "texture-card-body");
      const row = el("div", "texture-title-row");
      const tit = el("div");
      tit.append(el("h3", "", p.nome), el("p", "", `${p.resolucao}  ·  ${tamanho(p.tamanho)}`));
      row.append(tit);
      const acts = el("div", "texture-actions");
      const pagina = botao("linkbtn", t("página"), () => api.openLink(p.pagina));
      const btn = el("button", "apply-texture");
      btn.type = "button";
      if (pct !== undefined) {
        btn.style.setProperty("--p", pct + "%");
        btn.textContent = pct + "%";
        btn.disabled = true;
        acts.append(pagina, btn);
      } else if (p.emUso) {
        btn.textContent = t("Desligar");
        btn.title = t("Clique para desligar");
        btn.onclick = () => txAcao(() => api.usarTextura(p.id, false), p.nome + " desligada.");
        acts.append(pagina, btn);
      } else if (p.baixado) {
        btn.textContent = t("Usar pack");
        btn.onclick = () => txAcao(() => api.usarTextura(p.id, true), `Usando ${p.nome}. Ela aparece na próxima vez que o jogo abrir.`);
        acts.append(pagina, botao("linkbtn tx-del", t("apagar"), () => txAcao(() => api.removerTextura(p.id), p.nome + " apagada.")), btn);
      } else {
        btn.textContent = t("Baixar");
        btn.onclick = () => baixarTextura(p);
        acts.append(pagina, btn);
      }
      body.append(row, el("p", "desc", t(p.desc)), acts);
      c.append(prev, body);
      grid.append(c);
    }
    const emUso = lista.find((p) => p.emUso);
    $("activeTextureLabel").textContent = emUso ? t(`${emUso.nome} em uso`) : t("Textura padrão em uso");
    $("launcherTexture").textContent = emUso ? emUso.nome : t("Textura padrão");
    $("textureMeta").textContent = t(`${lista.filter((p) => p.baixado || p.emUso).length} de ${lista.length} baixadas`);
  }

  async function txAcao(fn, ok = "") {
    try {
      renderTexturas(await fn());
      txMsg(ok);
      if (ok) toast("Texturas", ok);
    } catch (err) {
      txMsg(cleanErr(err), true);
    }
  }
  const loadTexturas = () => txAcao(() => api.listTexturas());

  async function baixarTextura(p) {
    progresso[p.id] = 0;
    renderTexturas(ultimaLista);
    txMsg("");
    try {
      const lista = await api.baixarTextura(p.id);
      delete progresso[p.id];
      renderTexturas(lista);
      txMsg(p.nome + " baixada. Clique em Usar pack.");
    } catch (err) {
      delete progresso[p.id];
      renderTexturas(ultimaLista);
      txMsg(`Não deu para baixar ${p.nome}: ${cleanErr(err)}`, true);
    }
  }
  api.onTexturaProgresso(({ id, pct }) => {
    if (!(id in progresso)) return;
    progresso[id] = pct;
    const btn = document.querySelector(`.texture-card[data-id="${CSS.escape(id)}"] .apply-texture`);
    if (!btn) return;
    btn.style.setProperty("--p", pct + "%");
    btn.textContent = pct + "%";
  });
  $("textureFilters").addEventListener("click", (e) => {
    const b = e.target.closest("[data-texture-filter]");
    if (!b) return;
    filtroTextura = b.dataset.textureFilter;
    $$("[data-texture-filter]").forEach((x) => x.classList.toggle("is-active", x === b));
    renderTexturas(ultimaLista);
  });
  $("btnTexFolder").onclick = () => api.abrirPastaTexturas();

  /* ---------- versões do Pulsar ---------- */
  let ultimasVersoes = null;
  function renderVersoes(v) {
    if (v) ultimasVersoes = v;
    v = ultimasVersoes;
    if (!v) return;
    const box = $("versoes");
    box.textContent = "";
    const card = (qual, info, rotulo, texto) => {
      const a = el("article", `release-card ${qual === "atual" ? "current" : "previous"}` + (info ? "" : " disabled") + (info && v.escolhida === qual ? " is-chosen" : ""));
      a.classList.add("versao", qual);
      const rail = el("div", "release-rail");
      rail.append(el("span"));
      const top = el("div", "release-top");
      top.append(el("span", "release-number", info ? info.versao : "—"), el("span", "badge " + (qual === "atual" ? "cyan" : "amber"), t(rotulo)));
      if (info && v.escolhida === qual) top.append(el("span", "badge ghost", t("Jogando com esta")));
      a.append(rail, top, el("h3", "", t(qual === "atual" ? "Órbita atual" : "Rota de retorno")), el("p", "", t(texto)));
      const ul = el("ul");
      const nov = info && NOVIDADES.find((n) => n.id === info.versao);
      for (const p of nov ? nov.itens.slice(0, 3) : []) {
        const li = el("li");
        li.append(icone("i-check"), el("span", "", t(p)));
        ul.append(li);
      }
      a.append(ul);
      const foot = el("div", "release-footer");
      foot.append(el("span", "", info ? "Pulsar " + info.versao : ""));
      const b = botao("primary-small" + (info && v.escolhida === qual ? " is-current" : ""), !info ? t("Indisponível") : v.escolhida === qual ? t("✓ Jogando com esta") : t("Jogar com esta versão"), () => escolherVersao(qual));
      b.disabled = !info;
      b.setAttribute("role", "radio");
      b.setAttribute("aria-checked", String(!!info && v.escolhida === qual));
      foot.append(b);
      a.append(foot);
      box.append(a);
    };
    card("atual", v.atual, "Atual", "A versão mais nova, com tudo o que saiu agora.");
    card("anterior", v.anterior, "Anterior", v.anterior ? "A versão de antes da última atualização, do jeito que ela era." : "Ainda não tem versão anterior. Ela aparece aqui depois da próxima atualização.");
    const aviso = $("homeVersao");
    aviso.hidden = !(v.escolhida === "anterior" && v.anterior) || !ehPulsar();
    if (!aviso.hidden) aviso.textContent = t(`Jogando com a versão anterior (${v.anterior.versao})`);
  }
  async function escolherVersao(qual) {
    try {
      renderVersoes(await api.escolherVersao(qual));
      $("versaoMsg").textContent = "";
    } catch (e) {
      $("versaoMsg").textContent = t(cleanErr(e));
    }
  }

  /* ---------- idioma ---------- */
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
  function retraduzir() {
    renderIdiomas();
    renderChips();
    renderGrid();
    updateCounts();
    renderAccount(null);
    if (ultimosMods) renderUserMods(ultimosMods);
    if (ultimaLista.length) renderTexturas(ultimaLista);
    setStatus(...ultimoStatus);
    setTopo(document.body.classList.contains("playing"));
    logVazio();
    if (ultimaAtualizacao) mostrarAtualizacao(ultimaAtualizacao);
    $("memOut").textContent = t($("mem").value + " GB");
    ["umMsg", "txMsg", "saved", "versaoMsg"].forEach((id) => ($(id).textContent = ""));
    renderVersoes();
    renderMc();
    renderOrbitas();
    renderNovidadesMenu();
    if (!$("novModal").hidden) abrirNovidades();
    api.getConfig().then((cfg) => {
      renderOptifine(cfg);
      renderUpdateToken(cfg);
    });
  }

  /* ---------- ajustes ---------- */
  function setSettingsTab(tab) {
    $$("[data-settings-tab]").forEach((b) => b.classList.toggle("is-active", b.dataset.settingsTab === tab));
    $$("[data-settings-panel]").forEach((p) => {
      const on = p.dataset.settingsPanel === tab;
      p.classList.toggle("is-active", on);
      p.hidden = !on;
    });
  }
  $$("[data-settings-tab]").forEach((b) => b.addEventListener("click", () => setSettingsTab(b.dataset.settingsTab)));

  function renderOptifine(cfg) {
    const e = $("ofFile");
    e.textContent = cfg.optifinePath ? cfg.optifinePath.split(/[\\/]/).pop() : t("Nenhum arquivo escolhido");
    e.classList.toggle("ok", !!cfg.optifinePath);
  }
  function renderUpdateToken(cfg) {
    $("updToken").value = "";
    $("updToken").placeholder = t(cfg.hasUpdateToken ? "Chave salva. Cole outra para trocar." : "Cole a chave do GitHub (github_pat_…)");
  }
  async function loadSettings() {
    const cfg = await api.getConfig();
    $("mem").value = cfg.memoryMaxGb;
    $("memOut").textContent = t(cfg.memoryMaxGb + " GB");
    $("java").value = cfg.javaPath;
    $("dir").value = cfg.gameDir;
    $("loader").value = cfg.loader;
    $("openConsole").checked = cfg.openConsole !== false;
    $("modoLauncher").value = cfg.modoLauncher === "lite" ? "lite" : "normal";
    renderUpdateToken(cfg);
    renderOptifine(cfg);
    updateCounts();
  }
  $("mem").addEventListener("input", () => ($("memOut").textContent = t($("mem").value + " GB")));
  $("btnUpdToken").onclick = async () => {
    const cfg = await api.setUpdateToken($("updToken").value);
    renderUpdateToken(cfg);
    toast("Atualização", cfg.hasUpdateToken ? "Chave salva. Procurando atualização…" : "Chave removida.");
  };
  $("btnCheckUpdate").onclick = () => {
    toast("Atualização", "Procurando atualização…");
    api.checkUpdate().catch(() => {});
  };
  $("btnPickOf").onclick = async () => renderOptifine(await api.pickOptifine());
  $("btnOfSite").onclick = () => abrirOptifine(true);
  $("loader").addEventListener("change", async () => {
    const cfg = await api.setConfig({ loader: $("loader").value });
    updateCounts();
    if (cfg.loader !== "forge" && !cfg.optifinePath) setStatus("Escolha o arquivo do OptiFine em Ajustes para usar esse modo.", true);
    else if (cfg.loader === "optifine") setStatus("Só OptiFine: mais leve, mas sem os mods do Pulsar.");
    else setStatus("Pronto para jogar");
  });
  // O visual (Normal ou Lite) troca na hora: o launcher recarrega a tela.
  $("modoLauncher").addEventListener("change", () => api.setConfig({ modoLauncher: $("modoLauncher").value }).catch((err) => toast("Ajustes", cleanErr(err), "error")));
  $("openConsole").addEventListener("change", () => api.setConfig({ openConsole: $("openConsole").checked }).catch(() => {}));
  $("btnSalvar").onclick = async () => {
    try {
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
      toast("Ajustes salvos", "Suas preferências foram aplicadas.");
      setTimeout(() => ($("saved").textContent = ""), 2000);
    } catch (err) {
      toast("Ajustes", cleanErr(err), "error");
    }
  };

  // Aparência (só deste visual): guardada no navegador do launcher.
  const VISUAL_PADRAO = { motion: 70, glow: 80, reducedEffects: false };
  let visual = { ...VISUAL_PADRAO };
  function aplicarVisual() {
    document.body.classList.toggle("reduced-effects", !!visual.reducedEffects);
    document.documentElement.style.setProperty("--motion-scale", visual.reducedEffects ? ".001" : String(Math.max(0.2, visual.motion / 100)));
    document.documentElement.style.setProperty("--core-glow", String(Math.max(0.25, visual.glow / 100)));
    $("motionValue").textContent = visual.motion + "%";
    $("glowValue").textContent = visual.glow + "%";
    $$("[data-visual]").forEach((c) => {
      if (c.type === "checkbox") c.checked = !!visual[c.dataset.visual];
      else c.value = visual[c.dataset.visual];
    });
    try {
      localStorage.setItem("pulsar.visual", JSON.stringify(visual));
    } catch {}
  }
  try {
    visual = { ...VISUAL_PADRAO, ...(JSON.parse(localStorage.getItem("pulsar.visual")) || {}) };
  } catch {}
  $$("[data-visual]").forEach((c) =>
    c.addEventListener("input", () => {
      visual[c.dataset.visual] = c.type === "checkbox" ? c.checked : Number(c.value);
      aplicarVisual();
    })
  );
  $("btnVisualPadrao").onclick = () => {
    visual = { ...VISUAL_PADRAO };
    aplicarVisual();
    toast("Visual", "A aparência voltou ao padrão do Pulsar.");
  };
  aplicarVisual();
  if (matchMedia("(pointer: fine)").matches) {
    document.addEventListener(
      "pointermove",
      (e) => {
        document.documentElement.style.setProperty("--mx", e.clientX + "px");
        document.documentElement.style.setProperty("--my", e.clientY + "px");
      },
      { passive: true }
    );
  }

  /* ---------- atualização ---------- */
  let ultimaAtualizacao = null;
  let avisada = "";
  api.onUpdate(mostrarAtualizacao);
  function mostrarAtualizacao(u) {
    ultimaAtualizacao = u;
    const b = $("update");
    b.hidden = false;
    if (u.state === "downloading") {
      b.className = "update-pill";
      b.textContent = t(`Baixando atualização${u.version ? " " + u.version : ""}… ${u.percent || 0}%`);
    } else if (u.state === "none") b.hidden = true;
    else if (u.state === "error") {
      b.className = "update-pill";
      b.textContent = t(u.text);
    } else if (u.state === "ready") {
      b.className = "update-pill ready";
      b.textContent = t(`Atualizar para ${u.version}`);
      if (avisada !== u.version) {
        avisada = u.version;
        abrirAtualizacao(u);
      }
    }
  }
  function abrirAtualizacao(u) {
    $("updVer").textContent = u.version || "";
    $("updNotes").textContent = u.notas || t("Melhorias e correções para o seu PvP.");
    $("updHelp2").textContent = t("O Pulsar fecha, atualiza e abre de novo sozinho em alguns segundos.");
    $("updHelp2").classList.remove("err");
    $("updModal").hidden = false;
  }
  $("update").onclick = () => {
    if ($("update").classList.contains("ready")) $("updModal").hidden = false;
  };
  $("updNow").onclick = async () => {
    try {
      $("updNow").disabled = true;
      await api.installUpdate();
    } catch (err) {
      $("updHelp2").textContent = t(cleanErr(err));
      $("updHelp2").classList.add("err");
    } finally {
      $("updNow").disabled = false;
    }
  };
  $("updLater").onclick = () => ($("updModal").hidden = true);
  $$("[data-close-upd]").forEach((b) => b.addEventListener("click", () => ($("updModal").hidden = true)));

  /* ---------- OptiFine: janela que recomenda e instala sozinho ---------- */
  let esperandoOptifine = false;
  function abrirOptifine(baixarJa) {
    $("ofEspera").hidden = true;
    $("ofBaixar").disabled = false;
    $("ofModal").hidden = false;
    if (baixarJa) baixarOptifine();
  }
  function fecharOptifine() {
    $("ofModal").hidden = true;
    if (esperandoOptifine) api.cancelarOptifine().catch(() => {});
    esperandoOptifine = false;
    api.setConfig({ optifineAviso: true }).catch(() => {});
  }
  async function baixarOptifine() {
    $("ofBaixar").disabled = true;
    const r = await api.baixarOptifine().catch((e) => ({ estado: "erro", erro: e.message }));
    if (r.estado === "esperando") {
      esperandoOptifine = true;
      $("ofEspera").hidden = false;
    } else if (r.estado === "erro") {
      $("ofBaixar").disabled = false;
      toast("OptiFine", r.erro || "Não deu para abrir o site do OptiFine.", "erro");
    }
  }
  api.onOptifine((s) => {
    esperandoOptifine = false;
    $("ofEspera").hidden = true;
    $("ofBaixar").disabled = false;
    if (s.estado === "pronto") {
      $("ofModal").hidden = true;
      if (s.config) {
        $("loader").value = s.config.loader;
        renderOptifine(s.config);
        updateCounts();
      }
      setStatus("Pronto para jogar");
      toast("OptiFine instalado", "O modo agora é OptiFine + Forge + Pulsar.");
    } else if (s.estado === "desistiu") {
      toast("OptiFine", "O download não apareceu na pasta Downloads. Se baixou em outro lugar, escolha o arquivo em Ajustes.");
    } else if (s.estado === "erro") {
      toast("OptiFine", s.erro || "Não deu para instalar o OptiFine.", "erro");
    }
  });
  $("ofBaixar").onclick = baixarOptifine;
  $("ofDepois").onclick = fecharOptifine;
  $$("[data-close-of]").forEach((b) => b.addEventListener("click", fecharOptifine));
  // Primeira vez (sem OptiFine e sem ter visto a janela): aparece depois das novidades da versão.
  async function sugerirOptifine() {
    const cfg = await api.getConfig().catch(() => null);
    if (cfg && !cfg.optifinePath && !cfg.optifineAviso && $("novModal").hidden && $("updModal").hidden) abrirOptifine(false);
  }

  /* ---------- novidades da versão ---------- */
  let novidadeAberta = null;
  function abrirNovidades(nov) {
    if (nov) novidadeAberta = nov;
    nov = novidadeAberta;
    if (!nov) return;
    $("novVer").textContent = nov.id;
    const ul = $("novList");
    ul.textContent = "";
    for (const p of nov.itens) ul.append(el("li", "", t(p)));
    $("novModal").hidden = false;
  }
  function fecharNovidades() {
    $("novModal").hidden = true;
    if (novidadeAberta) api.setConfig({ novidadesVistas: novidadeAberta.id }).catch(() => {});
    setTimeout(sugerirOptifine, 400);
  }
  $("novOk").onclick = fecharNovidades;
  $("novTodas").onclick = () => {
    fecharNovidades();
    setTab("home");
    const o = ORBITAS.find((x) => x.id === "novidades");
    if (o) abrirOrbita(o);
  };
  $$("[data-close-nov]").forEach((b) => b.addEventListener("click", fecharNovidades));
  async function verNovidades(cfg) {
    const versao = await api.appVersion().catch(() => "");
    const nov = NOVIDADES.find((n) => n.id === versao);
    $("notificationDot").hidden = !(nov && cfg.novidadesVistas !== versao);
    if (nov && cfg.novidadesVistas !== versao) abrirNovidades(nov);
    else setTimeout(sugerirOptifine, 800);
  }

  document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    if (!$("updModal").hidden) $("updModal").hidden = true;
    else if (!$("novModal").hidden) fecharNovidades();
    else if (!$("ofModal").hidden) fecharOptifine();
    else if (!$("drawerLayer").hidden) fecharPainel();
    else setPopovers(null);
  });

  /* ---------- rádio pulsar ---------- */
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
    const cfg = await api.getConfig().catch(() => ({}));
    if (cfg.idioma && i18n.escolher(cfg.idioma) !== i18n.atual()) i18n.definir(cfg.idioma);
    renderIdiomas();
    renderOrbitas();
    renderNovidadesMenu();
    api.appVersion()
      .then((v) => {
        for (const id of ["brandClientVersion", "sidebarVersion", "runtimeVersion", "settingsVersion"]) $(id).textContent = v;
      })
      .catch(() => {});
    mcVersoes = await api.mcVersoes().catch(() => []);
    mcEscolhida = cfg.version || "1.8.9";
    renderMc();
    api.listVersoes().then(renderVersoes).catch(() => {});
    enabled = await api.getMods().catch(() => ({}));
    renderChips();
    renderGrid();
    await loadSettings().catch(() => {});
    api.listUserMods().then((l) => renderUserMods((ultimosMods = l))).catch(() => {});
    api.listTexturas().then(renderTexturas).catch(() => {});
    api.gravacaoEstado().then(estadoGravacao).catch(() => {});
    renderAccount(null);
    setStatus("Conectando sua conta…");
    const estado = await api.restoreAccount().catch(() => null);
    renderAccount(estado);
    setStatus(prontaParaJogar() ? "Pronto para jogar" : "Entre com sua conta Microsoft para jogar");
    verNovidades(cfg);
  })();
})();
