// Rádio Pulsar: as estações que o mod do jogo pode pedir (pelo id). Quem toca é a janela do launcher.
// Rádios abertas da internet, feitas para tocar em qualquer player (SomaFM, Nightride FM, Radio Paradise e laut.fm).
const ESTACOES = {
  space_station: { nome: "Space Station", url: "https://ice2.somafm.com/spacestation-128-mp3" },
  groove_salad: { nome: "Groove Salad", url: "https://ice2.somafm.com/groovesalad-128-mp3" },
  def_con: { nome: "DEF CON", url: "https://ice2.somafm.com/defcon-128-mp3" },
  fluid: { nome: "Fluid", url: "https://ice2.somafm.com/fluid-128-mp3" },
  nightride: { nome: "Nightride", url: "https://stream.nightride.fm/nightride.mp3" },
  chillsynth: { nome: "Chillsynth", url: "https://stream.nightride.fm/chillsynth.mp3" },
  radio_paradise: { nome: "Radio Paradise", url: "https://stream.radioparadise.com/mp3-128" },
  lofi: { nome: "Lofi", url: "https://lofi.stream.laut.fm/lofi" },
};

// Pedido do jogo ({tocando, estacao, volume}) vira o comando para a janela tocar.
function comando(p) {
  const e = ESTACOES[String((p && p.estacao) || "")] || ESTACOES.space_station;
  const volume = Math.max(0, Math.min(100, Number(p && p.volume) || 0));
  return { tocando: !!(p && p.tocando), nome: e.nome, url: e.url, volume };
}

module.exports = { ESTACOES, comando };
