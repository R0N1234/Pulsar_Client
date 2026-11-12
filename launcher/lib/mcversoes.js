// Versões do Minecraft que o Pulsar abre. A 1.8.9 é a principal (Forge + mods do Pulsar).
// As outras são as mais jogadas no PvP hoje e abrem no Minecraft puro (vanilla), cada uma com a sua pasta de jogo
// e o Java certo baixado sozinho: 1.7.10 (PvP clássico das practice), 1.16.5 (último combate antigo em versão nova,
// muito usado em kits e crystal antigo) e 1.21.8 (combate novo com recarga, usado nos tier tests de espada e crystal).
const VERSOES = [
  {
    id: "1.8.9",
    tipo: "pulsar",
    java: 8,
    selo: "RECOMENDADA PVP",
    desc: "Combate clássico, sem recarga de ataque. Com o Forge, o Motor Pulsar e todos os mods do Pulsar.",
  },
  {
    id: "1.7.10",
    tipo: "vanilla",
    java: 8,
    selo: "PVP CLÁSSICO",
    desc: "A versão das practice antigas: hit mais seco e o blockhit original. Abre no Minecraft puro, sem os mods do Pulsar.",
  },
  {
    id: "1.16.5",
    tipo: "vanilla",
    java: 8,
    selo: "KITS E CRYSTAL",
    desc: "Combate novo com recarga, mundo atual e muitos servidores de kit. Abre no Minecraft puro, sem os mods do Pulsar.",
  },
  {
    id: "1.21.8",
    tipo: "vanilla",
    java: 21,
    selo: "PVP MODERNO",
    desc: "A versão dos tier tests de espada, machado e crystal. Abre no Minecraft puro, sem os mods do Pulsar. Usa o Java 21 (o Pulsar baixa sozinho).",
  },
];

const PRINCIPAL = "1.8.9";

function achar(id) {
  return VERSOES.find((v) => v.id === id) || VERSOES[0];
}

module.exports = { VERSOES, PRINCIPAL, achar };
