// Rádio Pulsar: as estações que o mod do jogo pode pedir (pelo id). Quem toca é a janela do launcher.
// Rádios abertas da internet, feitas para tocar em qualquer player (SomaFM, Nightride FM, Radio Paradise e laut.fm).
// Cada estação tem endereços reserva: se um servidor recusar, a janela tenta o próximo.
const soma = (canal) => [1, 2, 4, 6].map((n) => `https://ice${n}.somafm.com/${canal}-128-mp3`);
const ESTACOES = {
  space_station: { nome: "Space Station", urls: soma("spacestation") },
  groove_salad: { nome: "Groove Salad", urls: soma("groovesalad") },
  def_con: { nome: "DEF CON", urls: soma("defcon") },
  fluid: { nome: "Fluid", urls: soma("fluid") },
  nightride: { nome: "Nightride", urls: ["https://stream.nightride.fm/nightride.mp3"] },
  chillsynth: { nome: "Chillsynth", urls: ["https://stream.nightride.fm/chillsynth.mp3"] },
  radio_paradise: { nome: "Radio Paradise", urls: ["https://stream.radioparadise.com/mp3-128", "https://stream.radioparadise.com/aac-128"] },
  lofi: { nome: "Lofi", urls: ["https://lofi.stream.laut.fm/lofi", "https://stream.laut.fm/lofi"] },
};

// Pedido do jogo ({tocando, estacao, volume}) vira o comando para a janela tocar.
function comando(p) {
  const e = ESTACOES[String((p && p.estacao) || "")] || ESTACOES.space_station;
  const volume = Math.max(0, Math.min(100, Number(p && p.volume) || 0));
  return { tocando: !!(p && p.tocando), nome: e.nome, urls: e.urls.slice(), volume };
}

module.exports = { ESTACOES, comando };
