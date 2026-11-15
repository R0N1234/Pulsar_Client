// Listas de escolha (o "Modo", o visual do launcher...) com o visual do Pulsar.
// A lista do Windows abre branca e com o texto claro, sem dar para ler. Aqui o <select> continua existindo
// (o resto do launcher lê e muda o valor dele normalmente), mas fica escondido atrás de um botão e de uma lista nossa.
(function () {
  "use strict";
  const SETA = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
  const CHECK = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/></svg>';
  let aberto = null;

  function fecharAberto() {
    if (aberto) aberto();
  }

  function melhorar(sel) {
    if (sel.dataset.seletor || sel.multiple) return;
    sel.dataset.seletor = "1";
    const caixa = document.createElement("div");
    caixa.className = "seletor";
    if (sel.id) caixa.dataset.de = sel.id;
    const botao = document.createElement("button");
    botao.type = "button";
    botao.className = "seletor-botao";
    botao.setAttribute("aria-haspopup", "listbox");
    botao.setAttribute("aria-expanded", "false");
    const rotuloAria = sel.getAttribute("aria-label");
    if (rotuloAria) botao.setAttribute("aria-label", rotuloAria);
    const texto = document.createElement("span");
    texto.className = "seletor-texto";
    botao.appendChild(texto);
    botao.insertAdjacentHTML("beforeend", SETA);
    caixa.appendChild(botao);
    const lista = document.createElement("div");
    lista.className = "seletor-lista";
    lista.setAttribute("role", "listbox");
    lista.hidden = true;
    sel.insertAdjacentElement("afterend", caixa);
    document.body.appendChild(lista);
    sel.classList.add("seletor-nativo");
    sel.tabIndex = -1;

    function rotulo() {
      const o = sel.options[sel.selectedIndex];
      texto.textContent = o ? o.textContent : "";
      botao.disabled = sel.disabled;
    }

    function escolher(i) {
      fechar();
      botao.focus();
      if (sel.selectedIndex === i) return;
      sel.selectedIndex = i;
      rotulo();
      sel.dispatchEvent(new Event("input", { bubbles: true }));
      sel.dispatchEvent(new Event("change", { bubbles: true }));
    }

    function montar() {
      lista.textContent = "";
      Array.from(sel.options).forEach((o, i) => {
        const item = document.createElement("button");
        item.type = "button";
        item.className = "seletor-item";
        item.setAttribute("role", "option");
        item.setAttribute("aria-selected", String(i === sel.selectedIndex));
        item.disabled = o.disabled;
        const t = document.createElement("span");
        t.textContent = o.textContent;
        item.appendChild(t);
        item.insertAdjacentHTML("beforeend", CHECK);
        item.addEventListener("click", (e) => { e.preventDefault(); escolher(i); });
        lista.appendChild(item);
      });
    }

    // A lista fica no <body> (position: fixed) para não ser cortada por painéis com rolagem.
    function posicionar() {
      const r = caixa.getBoundingClientRect();
      const largura = Math.max(r.width, 200);
      const esquerda = Math.min(Math.max(8, r.right - largura), window.innerWidth - largura - 8);
      lista.style.width = largura + "px";
      lista.style.left = esquerda + "px";
      const altura = lista.offsetHeight;
      const embaixo = window.innerHeight - r.bottom - 8;
      if (embaixo >= altura || embaixo >= r.top) {
        lista.style.top = r.bottom + 6 + "px";
        lista.classList.remove("acima");
      } else {
        lista.style.top = Math.max(8, r.top - altura - 6) + "px";
        lista.classList.add("acima");
      }
    }

    function abrir() {
      if (sel.disabled) return;
      fecharAberto();
      montar();
      lista.hidden = false;
      posicionar();
      caixa.classList.add("aberto");
      botao.setAttribute("aria-expanded", "true");
      aberto = fechar;
      const atual = lista.querySelector('[aria-selected="true"]') || lista.querySelector(".seletor-item");
      if (atual) atual.focus({ preventScroll: true });
    }

    function fechar() {
      lista.hidden = true;
      caixa.classList.remove("aberto");
      botao.setAttribute("aria-expanded", "false");
      if (aberto === fechar) aberto = null;
    }

    botao.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (lista.hidden) abrir();
      else fechar();
    });
    botao.addEventListener("keydown", (e) => {
      if (e.key === "ArrowDown" || e.key === "ArrowUp") {
        e.preventDefault();
        abrir();
      }
    });
    lista.addEventListener("keydown", (e) => {
      const itens = Array.from(lista.querySelectorAll(".seletor-item:not(:disabled)"));
      const i = itens.indexOf(document.activeElement);
      if (e.key === "Escape" || e.key === "Tab") {
        e.preventDefault();
        fechar();
        botao.focus();
      } else if (e.key === "ArrowDown" && itens.length) {
        e.preventDefault();
        itens[(i + 1) % itens.length].focus();
      } else if (e.key === "ArrowUp" && itens.length) {
        e.preventDefault();
        itens[(i - 1 + itens.length) % itens.length].focus();
      }
    });

    // O <label> em volta (a linha inteira do "Modo") também abre a lista.
    const label = caixa.closest("label");
    if (label) {
      label.addEventListener("click", (e) => {
        if (caixa.contains(e.target)) return;
        e.preventDefault();
        abrir();
      });
    }

    sel.addEventListener("change", rotulo);
    new MutationObserver(() => {
      rotulo();
      if (!lista.hidden) montar();
    }).observe(sel, { childList: true, subtree: true, attributes: true, characterData: true });

    // O launcher muda o valor direto (sel.value = ...), sem evento: o botão acompanha.
    for (const prop of ["value", "selectedIndex"]) {
      const d = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, prop);
      Object.defineProperty(sel, prop, {
        configurable: true,
        get() { return d.get.call(sel); },
        set(v) { d.set.call(sel, v); rotulo(); },
      });
    }
    rotulo();
  }

  function tudo() {
    document.querySelectorAll("select").forEach(melhorar);
  }

  document.addEventListener("mousedown", (e) => {
    if (!aberto) return;
    if (e.target.closest && (e.target.closest(".seletor-lista") || e.target.closest(".seletor.aberto"))) return;
    fecharAberto();
  }, true);
  window.addEventListener("resize", fecharAberto);
  window.addEventListener("blur", fecharAberto);
  document.addEventListener("scroll", (e) => {
    if (aberto && !(e.target.closest && e.target.closest(".seletor-lista"))) fecharAberto();
  }, true);

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", tudo);
  else tudo();
  new MutationObserver(tudo).observe(document.documentElement, { childList: true, subtree: true });
  window.Seletor = { melhorar, tudo };
})();
