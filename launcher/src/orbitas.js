// Início no modelo Órbita: três órbitas em volta do pulsar (Novidades, Sugestões e Discord).
// Cada planeta é um item; clicar abre a aba dele na janela de abas.
// A primeira novidade é a da versão atual: ela também aparece na janela de novidades depois de atualizar.
(function () {
  const DISCORD = "https://discord.gg/UUW5jTfDKH";
  const REPO = "https://github.com/R0N1234/Pulsar_Client";

  const NOVIDADES = [
    {
      id: "0.9.1",
      nome: "0.9.1",
      titulo: "Versão 0.9.1",
      itens: [
        "Novo mod Focar Jogador: ao bater ou apanhar de alguém, aparece um aviso com a cabeça da skin e o nick dele. Aperte a tecla (V) e o jogador fica focado: nick destacado e uma caixa em volta dele quando está à vista. Tecla, cor e o tempo sem ver são configuráveis.",
        "Novo mod Sequências de Abate: FIRST BLOOD, DOUBLE KILL... GODLIKE em letras vermelhas com sangue escorrendo e a voz do Pulsar anunciando. Duas listas (Pulsar e Arena), tempo entre abates, duração, volume e som ligado ou desligado. Suas próprias vozes entram pela pasta pulsar/sons/sequencias.",
      ],
    },
    {
      id: "0.9.0",
      nome: "0.9.0",
      titulo: "Versão 0.9.0",
      itens: [
        "Launcher novo no modelo Órbita: Novidades, Sugestões e Discord giram em volta do pulsar e cada item abre a sua própria aba.",
        "Ícone de contas no canto de cima, à direita: entre com a Microsoft, troque de conta ou use um nick offline.",
        "Três versões de PvP novas além da 1.8.9: 1.7.10, 1.16.5 e 1.21.8, no Minecraft puro e com o Java certo baixado sozinho.",
        "Janela de novidades: toda vez que o Pulsar atualizar, ela abre no meio da tela com o que mudou.",
        "Novo mod Anti-Batata: avisa quando a regeneração da maçã dourada vai acabar, com contagem na tela (arrastável) e/ou a tela piscando na cor que você escolher. Pode até comer outra maçã sozinho faltando 1 segundo, só com a espada na mão e batendo.",
        "Novo mod Filtro de Coleta: escolha os itens de PvP que você pega do chão (botão esquerdo deixa verde, botão direito deixa vermelho). Os vermelhos são descartados na hora, e blocos e minérios sempre são pegos.",
        "Kit PvP: o nome do item sob o mouse não fica mais apagado atrás das sombras do layout no inventário e no baú.",
      ],
    },
    {
      id: "0.8.6",
      nome: "0.8.6",
      titulo: "Versão 0.8.6",
      itens: [
        "Novo mod Kit PvP: monte no painel onde fica cada item do seu inventário e, no baú, Shift + botão direito leva cada item direto para o lugar (vale o tipo e o minério, não o encantamento).",
      ],
    },
    {
      id: "0.8.5",
      nome: "0.8.5",
      titulo: "Versão 0.8.5",
      itens: [
        "Cosméticos do Pulsar: a Órbita Pulsar (acessório) e a Supernova (emote) são grátis, e os outros chegam em breve. Aperte B no jogo para os emotes. O Telescópio virou um zoom igual ao do OptiFine (segure C). Mais FPS: a fonte e o Motor Pulsar param de esperar a placa de vídeo a cada quadro.",
      ],
    },
    {
      id: "0.8.4",
      nome: "0.8.4",
      titulo: "Versão 0.8.4",
      itens: [
        "Som no Windows com o driver de áudio novo (o mesmo do Minecraft atual), para quem ficava sem som com alguns fones e placas de som.",
      ],
    },
    {
      id: "0.8.3",
      nome: "0.8.3",
      titulo: "Versão 0.8.3",
      itens: [
        "Textos compridos do painel agora quebram em duas linhas e passam sozinhos com o mouse em cima. A Barra Pulsar lê a música do Spotify pelos controles de mídia do Windows, sem login. Novo botão Sugestões no menu do jogo. O W-Tap e Blockhit saiu do client e virou um mod separado.",
      ],
    },
    {
      id: "0.8.0",
      nome: "0.8.0",
      titulo: "Versão 0.8.0",
      itens: [
        "Barra Pulsar no topo da tela com FPS, ms, hora e a música do Spotify (com capa). O fundo do menu virou um universo 360° girando, com Andrômeda, os planetas e um pulsar. Painel do Shift direito e opções dos mods com visual novo, ícones e favoritos. No Motor Pulsar, com muita gente perto, só os mais próximos ficam com detalhe completo e os de longe ficam só com a skin. Novos mods: Pacote Ativo, Barra de Chefe e Aura de Encanto.",
      ],
    },
  ];

  const SUGESTOES = [
    {
      id: "sugerir-mod",
      nome: "Sugerir um mod",
      titulo: "Sugerir um mod",
      itens: [
        "Tem uma ideia de mod ou de mudança no Pulsar? Conte o que ele faz e em que momento do PvP ajuda.",
        "As sugestões ficam abertas para todo mundo ver e comentar, e as mais pedidas entram nas próximas versões.",
      ],
      acoes: [{ rotulo: "Escrever sugestão", link: REPO + "/issues/new?template=sugestao.yml" }],
    },
    {
      id: "relatar-problema",
      nome: "Relatar um problema",
      titulo: "Relatar um problema",
      itens: [
        "Algo deu errado? Conte o que aconteceu e cole o registro do jogo (botão copiar no Registro do jogo ou no Console do Pulsar).",
        "Com o registro dá para achar o problema bem mais rápido.",
      ],
      acoes: [
        { rotulo: "Relatar problema", link: REPO + "/issues/new?template=problema.yml" },
        { rotulo: "Abrir o console", acao: "console" },
      ],
    },
    {
      id: "ver-sugestoes",
      nome: "Ideias da galera",
      titulo: "Ideias da galera",
      itens: ["Veja o que os outros jogadores já pediram, comente e apoie as ideias que você também quer no Pulsar."],
      acoes: [{ rotulo: "Ver sugestões", link: REPO + "/issues" }],
    },
    {
      id: "sugerir-jogo",
      nome: "Pelo jogo",
      titulo: "Sugestões dentro do jogo",
      itens: ["No menu do jogo (Esc) tem o botão Sugestões: ele abre a mesma página sem precisar sair do Minecraft."],
    },
  ];

  const DISCORD_ITENS = [
    {
      id: "servidor",
      nome: "Servidor do Pulsar",
      titulo: "Servidor do Pulsar no Discord",
      itens: [
        "Entre no Discord do Pulsar para achar gente para treinar, ver as novidades antes de todo mundo e falar direto com quem faz o client.",
      ],
      acoes: [{ rotulo: "Entrar no Discord", link: DISCORD }],
    },
    {
      id: "suporte",
      nome: "Ajuda",
      titulo: "Ajuda no Discord",
      itens: [
        "Travou, ficou sem som ou o jogo não abre? Mande a sua dúvida no Discord junto com o registro do jogo.",
        "Copie o registro no Console do Pulsar e cole na mensagem.",
      ],
      acoes: [
        { rotulo: "Pedir ajuda", link: DISCORD },
        { rotulo: "Abrir o console", acao: "console" },
      ],
    },
    {
      id: "sinal",
      nome: "Sinal Discord",
      titulo: "Sinal Discord",
      itens: [
        "Com o Discord aberto, o seu perfil mostra que você está jogando com o Pulsar, a versão do Minecraft e onde você está (menu, sozinho ou o servidor).",
        "Dá para desligar na aba Mods, no mod Sinal Discord.",
      ],
      acoes: [{ rotulo: "Abrir Mods", acao: "mods" }],
    },
  ];

  // Da órbita de dentro para a de fora.
  window.PULSAR_ORBITAS = [
    { id: "discord", nome: "Discord", cor: "nebula", itens: DISCORD_ITENS },
    { id: "sugestoes", nome: "Sugestões", cor: "beam", itens: SUGESTOES },
    { id: "novidades", nome: "Novidades", cor: "ember", itens: NOVIDADES },
  ];
  window.PULSAR_NOVIDADES = NOVIDADES;
})();
