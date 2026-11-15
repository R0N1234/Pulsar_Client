// Cabeça da skin do Minecraft nas contas do launcher. A skin vem da Mojang pelo nick (o processo principal busca
// o endereço); aqui só recortamos o rosto (8x8) e a segunda camada num canvas, com os pixels nítidos.
(function () {
  "use strict";
  // Skin padrão (Steve) para nicks sem conta original ou sem internet.
  const PADRAO = "https://textures.minecraft.net/texture/1a4af718455d4aab528e7a61f86fa25e6a369d1768dcb13f7df319a713eb810b";
  const urls = new Map();
  const imagens = new Map();

  function urlDe(nome) {
    const k = String(nome || "").toLowerCase();
    if (!urls.has(k)) urls.set(k, window.pulsar && window.pulsar.skinUrl ? window.pulsar.skinUrl(nome).catch(() => null) : Promise.resolve(null));
    return urls.get(k);
  }

  function imagem(url) {
    if (!imagens.has(url)) {
      imagens.set(url, new Promise((ok, falhou) => {
        const img = new Image();
        img.onload = () => ok(img);
        img.onerror = falhou;
        img.src = url;
      }));
    }
    return imagens.get(url);
  }

  function desenhar(img) {
    const c = document.createElement("canvas");
    c.width = 64;
    c.height = 64;
    c.className = "skin-cabeca";
    const g = c.getContext("2d");
    g.imageSmoothingEnabled = false;
    g.drawImage(img, 8, 8, 8, 8, 0, 0, 64, 64);
    g.drawImage(img, 40, 8, 8, 8, 0, 0, 64, 64);
    return c;
  }

  /** Coloca a cabeça dentro do elemento do avatar (a letra fica por baixo até a skin chegar). */
  async function aplicar(alvo, nome) {
    if (!alvo || !nome) return;
    let img = null;
    try {
      const url = await urlDe(nome);
      img = await imagem(url || PADRAO);
    } catch {
      try {
        img = await imagem(PADRAO);
      } catch {
        return;
      }
    }
    alvo.querySelectorAll(".skin-cabeca").forEach((x) => x.remove());
    alvo.append(desenhar(img));
    alvo.classList.add("tem-skin");
  }

  window.Cabecas = { aplicar };
})();
