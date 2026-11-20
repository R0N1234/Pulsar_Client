// Teste de ponta a ponta usado no GitHub Actions: abre o Minecraft 1.8.9 de verdade
// (conta offline, tela virtual) e confere se o Forge carregou o Pulsar.
// Com E2E_PRINTS=<pasta>, o mod abre cada tela do Pulsar sozinho e o teste tira um print de cada.
// Uso: node test/e2e.js <pasta-temporária>
const fs = require("fs");
const { execFile } = require("child_process");
const path = require("path");
const { Authenticator } = require("minecraft-launcher-core");
const { launch } = require("../lib/launch");
const { startBridge } = require("../lib/ponte");
const { uuidOffline } = require("../lib/auth");

const work = path.resolve(process.argv[2] || "e2e-work");
const logFile = path.join(work, "e2e.log");
const SUCCESS = /\[Pulsar\] Pulsar Client .*iniciado/;
const SELFTEST = /\[Pulsar\] selftest: ([\w-]+)/;
const PRINTS = process.env.E2E_PRINTS ? path.resolve(process.env.E2E_PRINTS) : "";
const READY = /Sound engine started|Created: \d+x\d+ textures-atlas/;
const TIMEOUT_MS = 10 * 60 * 1000;
// Com os prints ligados, o teste também confere estas linhas no log (cada uma prova uma parte funcionando).
const REQUIRED = [
  [/Emotes: gancho instalado em ModelBiped/, "gancho dos emotes"],
  [/Fonte: estado do OpenGL lido do jogo/, "fonte sem perguntar ao driver a cada letra"],
  [/Motor: conferência de erros do OpenGL desligada/, "jogo sem perguntar ao driver por erros a cada quadro"],
  [/Cosméticos: camada de acessórios em \d+ bonecos/, "camada dos acessórios"],
  [/selftest: emote-ok/, "emote mexendo no boneco"],
  [/selftest: acessorio-ok/, "acessório desenhado"],
  [/selftest: kit-ok/, "Kit PvP leva os itens do baú para o layout"],
  [/selftest: batata-aviso-ok/, "Anti-Batata avisando o fim da regeneração"],
  [/selftest: batata-pisca-ok/, "Anti-Batata piscando a tela"],
  [/selftest: batata-ok/, "Anti-Batata comendo a maçã sozinho e voltando para a espada"],
  [/Anti-Batata: comendo a maçã/, "Anti-Batata trocando para a maçã"],
  [/selftest: filtro-ok/, "Filtro de Coleta descartando os itens vermelhos do chão"],
  [/selftest: focar-aviso\b(?!-)/, "Focar Jogador perguntando ao bater em alguém"],
  [/selftest: focar-ok/, "Focar Jogador com o nick destacado e a caixa"],
  [/selftest: sequencia-ok/, "Sequências de Abate contando o abate e desenhando"],
  [/selftest: sequencia-chat-ok/, "Sequências reconhecendo os avisos de abate do chat do servidor"],
  [/selftest: perfil-ok/, "Perfil do Motor virando código e voltando igual"],
  [/selftest: perfil-completo-ok/, "Perfil completo levando mods, HUD e atalhos (sem os de login)"],
  [/selftest: efeitos-ok/, "Efeitos escondendo só o efeito escolhido"],
  [/selftest: colisao-ok/, "Campo de Colisão mudando o tamanho da caixa"],
  [/selftest: sons-locais-ok/, "som da pasta pulsar/sons por cima do som do jar"],
  [/selftest: sequencia-dupla-ok/, "DOUBLE KILL dentro do intervalo"],
  [/selftest: sequencia-arena-ok/, "lista Arena com MULTI KILL no terceiro abate"],
  [/Sons locais: lendo de/, "pacote de sons locais instalado"],
  [/selftest: vozes-ok/, "vozes MASCULINO_ e Teste/ achadas na pasta de sons, com variação e multikill"],
  [/selftest: testar-todos-ok/, "Testar todos os sons tocando a fila"],
  [/\[e2e\] Gravação: iniciar/, "Gravação pedindo para o launcher começar a gravar"],
  [/\[e2e\] Gravação: clipe/, "Gravação pedindo o clipe"],
  [/selftest: gravacao-ok/, "Gravação com os pedidos de gravar e clipe"],
  [/selftest: novo-ok/, "etiqueta NOVO nos mods novos até a pessoa mexer"],
  [/Tela de carregamento do Pulsar instalada/, "tela de carregamento"],
  [/Motor: gancho de entidades instalado/, "gancho de entidades do Motor"],
  [/Motor: gancho de blocos especiais instalado/, "gancho de baús e placas do Motor"],
  [/Motor: gancho de animações instalado/, "gancho de animações do Motor"],
  [/Motor: gancho de partículas instalado/, "gancho de partículas do Motor"],
  [/Motor: gancho de blocos instalado/, "gancho de blocos do Motor"],
  [/Motor: placas leves instaladas/, "placas leves do Motor"],
  [/selftest: plantas-ok/, "grama alta e flores fora dos chunks"],
  [/selftest: placas-ok/, "placa longe sem texto"],
  [/Conta trocada para PulsarTroca/, "troca de conta pela ponte do launcher"],
  [/Conta trocada para NickTeste \(offline\)/, "nick offline criado e usado dentro do jogo"],
  [/selftest: mundo-pronto/, "mundo de teste"],
  [/Tecla G: Telescópio desligado/, "tecla escolhida no painel desliga o mod"],
  [/Tecla G: Telescópio ligado/, "a mesma tecla liga o mod de novo"],
  [/selftest: zoom-ok/, "sensibilidade do mouse igual depois do zoom"],
  [/selftest: bonecos/, "bonecos longe otimizados pelo Motor"],
  [/selftest: bonecos-pele\b(?!-)/, "bonecos longe só com a skin"],
  [/selftest: barra-ok/, "barra de rolagem do painel arrastada com o mouse"],
  [/selftest: fov-ok/, "Propulsão sem zoom ao correr"],
  [/Motor: molduras leves instaladas/, "molduras leves do Motor"],
  [/selftest: molduras-ok/, "molduras desenhadas no modo leve"],
  [/Gravidade de Itens: instalada/, "Gravidade de Itens trocando o desenho dos itens"],
  [/selftest: itens\b(?!-)/, "itens deitados no chão"],
  [/Nebulosa: shader carregado/, "shader da Nebulosa"],
  [/selftest: nebulosa-ok/, "desfoque no menu"],
  [/Capturas: print /, "Capturas achou a print nova"],
  [/Rastro de Cometa: shader carregado/, "shader do Rastro de Cometa"],
  [/\[e2e\] Sinal Discord: ligado (menu|sozinho)/, "Sinal Discord avisando o launcher pela ponte"],
  [/\[e2e\] Rádio: tocando space_station/, "Rádio Pulsar pedindo para o launcher tocar"],
  [/\[e2e\] Rádio: parado/, "Rádio Pulsar pedindo para parar"],
  [/Fonte: ganchos instalados em FontRenderer/, "ganchos da Fonte do Pulsar"],
  [/Fonte: espaçamento com fração instalado/, "espaçamento da Fonte do Pulsar"],
  [/Fonte do Pulsar carregada/, "Fonte do Pulsar carregada"],
  [/Animações: gancho do balanço instalado/, "gancho das Animações Clássicas"],
  [/Impacto: gancho da cor do dano instalado/, "gancho do Impacto"],
  [/Cores Vivas: shader carregado/, "shader do Cores Vivas"],
  [/Texturas: F8thful baixado/, "download de textura do catálogo"],
  [/Texturas: usando F8thful/, "textura ligada dentro do jogo"],
  [/Aura de Encanto: gancho dos itens instalado/, "gancho do brilho dos itens (Aura de Encanto)"],
  [/Aura de Encanto: gancho da armadura instalado/, "gancho do brilho da armadura (Aura de Encanto)"],
  [/selftest: encanto-ok/, "Aura de Encanto pintando o brilho"],
  [/selftest: pacote-ok/, "Pacote Ativo na tela"],
  [/selftest: chefe-ok/, "Barra de Chefe na tela"],
  [/Barra Pulsar: tocando Órbita Lenta/, "Barra Pulsar recebendo a música pela ponte"],
  [/Barra Pulsar: capa 16x16/, "capa da música chegando na Barra Pulsar"],
  [/selftest: musica-ok/, "Barra Pulsar mostrando a música"],
  [/Universo: céu 360 girando/, "universo 360 no fundo do menu"],
  [/selftest: universo-ok/, "universo desenhado"],
];
// Com E2E_REDE=<endereço> (wrangler dev no CI), o jogo também conversa com a rede do Pulsar (ícone, nível e amigos).
const REDE = process.env.E2E_REDE || "";
const CAPA_TESTE = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAL0lEQVR4nGOssDnBAAPsq83h7J+hJ7GKMzGQCGivgYUYdyOLD0Y/EOPu0XiguQYAdbwXi7uh378AAAAASUVORK5CYII=";
// Com E2E_OPTIFINE (caminho do jar do OptiFine 1.8.9), o jogo abre no modo Forge + OptiFine, igual ao de muita gente.
const OPTIFINE = process.env.E2E_OPTIFINE ? path.resolve(process.env.E2E_OPTIFINE) : "";
if (process.platform === "win32") REQUIRED.push([/Som: OpenAL Soft [\d.]+ pronto/, "OpenAL novo para o som no Windows"]);
if (REDE) {
  REQUIRED.push([/Rede: conectado como \w+ \(offline\)/, "rede do Pulsar respondendo (conta offline)"]);
  REQUIRED.push([/selftest: amigos/, "tela de Amigos"]);
}
const FORBIDDEN = [
  /selftest: mundo-falhou/,
  /selftest: textura-falhou/,
  /selftest: zoom-(falhou|sem-mira-lenta)/,
  /selftest: bonecos-falhou/,
  /selftest: bonecos-pele-falhou/,
  /selftest: barra-(falhou|sem-barra)/,
  /selftest: fov-falhou/,
  /selftest: molduras-falhou/,
  /selftest: (plantas|placas)-falhou/,
  /Motor: placas leves indisponíveis/,
  /Motor: filtro de blocos desligado/,
  /selftest: (itens|nebulosa)-falhou/,
  /selftest: (encanto|pacote|chefe|musica|universo)-falhou/,
  /selftest: (emote|acessorio)-falhou/,
  /selftest: kit-(falhou|sem-bau)/,
  /selftest: (batata-aviso|batata-pisca|batata|filtro|focar-aviso|focar|sequencia|sequencia-chat|sons-locais|sequencia-dupla|sequencia-arena|vozes|testar-todos|gravacao|novo|perfil|perfil-completo|efeitos|colisao)-falhou/,
  /Kit PvP: não achei/,
  /Emotes desligados por erro/,
  /Gravidade de Itens: não instalada/,
  /Motor: molduras leves indisponíveis/,
  /Motor: bonecos otimizados (indisponíveis|desligados)/,
  /\[Pulsar\] Texturas: .*(não carregou|Não deu)/,
  /\[Pulsar\] O mod .* deu erro/,
  /\[Pulsar\] .*desligad[oa]s? por erro/,
  /\[Pulsar\] .*não carregou/,
  /\[Pulsar\] .*ganchos não instalados/,
  /\[Pulsar\] Lista do TAB: textos do servidor indisponíveis/,
];
// Com o OptiFine, o boneco de mentira longe (só no cliente, sem servidor) não é desenhado, então essa conferência fica de fora.
// Jogadores de verdade chegam pelo servidor e aparecem normalmente.
if (OPTIFINE) {
  REQUIRED.splice(REQUIRED.findIndex(([re]) => String(re) === String(/selftest: bonecos/)), 1);
  FORBIDDEN.splice(FORBIDDEN.findIndex((re) => String(re) === String(/selftest: bonecos-falhou/)), 1);
  REQUIRED.splice(REQUIRED.findIndex(([re]) => String(re) === String(/selftest: bonecos-pele\b(?!-)/)), 1);
  FORBIDDEN.splice(FORBIDDEN.findIndex((re) => String(re) === String(/selftest: bonecos-pele-falhou/)), 1);
}

fs.mkdirSync(work, { recursive: true });
const log = fs.createWriteStream(logFile);
let pulsarLoaded = false;
let gameReady = false;
let selftestDone = !PRINTS;
let prints = Promise.resolve();
let proc;
if (PRINTS) fs.mkdirSync(PRINTS, { recursive: true });

// Espera a tela desenhar e tira um print da tela inteira (Linux: tela virtual do xvfb; Windows: pelo .NET).
function takePrint(name, delay = 1500) {
  const arquivo = path.join(PRINTS, `${name}.png`);
  const tirar =
    process.platform === "win32"
      ? (r) =>
          execFile(
            "powershell",
            [
              "-NoProfile",
              "-Command",
              "Add-Type -AssemblyName System.Windows.Forms,System.Drawing; $b=[System.Windows.Forms.Screen]::PrimaryScreen.Bounds; " +
                "$i=New-Object System.Drawing.Bitmap $b.Width,$b.Height; $g=[System.Drawing.Graphics]::FromImage($i); " +
                `$g.CopyFromScreen($b.Location,[System.Drawing.Point]::Empty,$b.Size); $i.Save('${arquivo.replace(/'/g, "''")}')`,
            ],
            () => r()
          )
      : (r) => execFile("import", ["-window", "root", arquivo], () => r());
  prints = prints.then(() => new Promise((r) => setTimeout(r, delay))).then(() => new Promise(tirar));
}

function finish(code, why) {
  console.log(`\n== ${why}`);
  if (proc) try { process.kill(-proc.pid); } catch { try { proc.kill("SIGKILL"); } catch {} }
  log.end(() => process.exit(code));
}

const onLog = (line) => {
  const text = String(line);
  log.write(text.endsWith("\n") ? text : text + "\n");
  if (SUCCESS.test(text)) pulsarLoaded = true;
  if (READY.test(text)) gameReady = true;
  // Prints da tela de carregamento: um quando o Forge acha os mods e outro quando o Pulsar inicia.
  if (PRINTS && /Forge Mod Loader has identified/.test(text)) takePrint("carregando-1", 300);
  if (PRINTS && SUCCESS.test(text)) takePrint("carregando-2", 300);
  if (PRINTS) seen.push(text);
  const step = PRINTS && text.match(SELFTEST);
  if (step && step[1] === "fim") prints.then(() => { selftestDone = true; check(); });
  else if (step && !step[1].includes("-")) takePrint(step[1]);
  check();
};

const seen = [];

function check() {
  if (!(pulsarLoaded && gameReady && selftestDone)) return;
  if (PRINTS) {
    const all = seen.join("\n");
    const missing = REQUIRED.filter(([re]) => !re.test(all)).map(([, what]) => what);
    const bad = FORBIDDEN.filter((re) => re.test(all)).map(String);
    if (missing.length || bad.length) return finish(1, `FALHOU: faltou ${missing.join(", ") || "nada"}; erros ${bad.join(", ") || "nenhum"}`);
  }
  finish(0, "OK: Forge carregou o Pulsar e o jogo abriu");
}

setTimeout(() => finish(1, `FALHOU: tempo esgotado (pulsar=${pulsarLoaded}, jogo=${gameReady}, telas=${selftestDone})`), TIMEOUT_MS);

(async () => {
  try {
    // Ponte de conta de mentira: o jogo pede para trocar e recebe uma conta; a lista tem uma original e nicks offline.
    const original = { name: "PulsarTroca", uuid: "0123456789abcdef0123456789abcdef", token: "0" };
    const contas = { microsoft: { name: original.name, uuid: original.uuid }, offline: ["RickPvP", "Rick_Alt2"], ativa: "microsoft", liberado: true };
    const sessao = (c) => (c === "microsoft" ? original : { name: c, uuid: uuidOffline(c), token: "0", offline: true });
    let gravando = false;
    const bridge = PRINTS
      ? await startBridge({
          onSwitchAccount: async () => original,
          onList: async () => contas,
          onUse: async (c) => sessao((contas.ativa = c)),
          onAddOffline: async (n) => {
            if (!contas.offline.includes(n)) contas.offline.push(n);
            return sessao((contas.ativa = n));
          },
          onPresenca: (p) => onLog(`[e2e] Sinal Discord: ${p.ligado ? "ligado" : "desligado"} ${p.lugar} ${p.servidor || ""}`),
          onRadio: (p) => onLog(`[e2e] Rádio: ${p.tocando ? "tocando " + p.estacao + " " + p.volume + "%" : "parado"}`),
          // Gravação de mentira: responde como o launcher responderia, sem gravar nada.
          onGravar: async (p) => {
            onLog(`[e2e] Gravação: ${p.acao} ${p.qualidade} ${p.fps}fps clipe ${p.clipe}s`);
            gravando = p.acao === "iniciar" ? true : p.acao === "parar" ? false : gravando;
            return { gravando, desde: Date.now(), guardando: p.acao === "buffer" ? !!p.ligado : gravando, arquivo: p.acao === "parar" || p.acao === "clipe" ? path.join(work, "teste.webm") : undefined, pasta: work };
          },
          // Spotify de mentira: uma música com capa, para a Barra Pulsar mostrar no jogo.
          onMusica: async (p) => ({ tocando: true, titulo: "Órbita Lenta", artista: "Pulsar", capaId: "teste", ...(p.capa && p.capaId !== "teste" ? { capa: CAPA_TESTE } : {}) }),
        })
      : null;
    const bridgeArgs = bridge ? [`-Dpulsar.ponte=${bridge.port}`, `-Dpulsar.chave=${bridge.key}`] : [];
    proc = await launch({
      config: { gameDir: path.join(work, "minecraft"), memoryMaxGb: 2, javaPath: process.env.E2E_JAVA || "", loader: OPTIFINE ? "forge_optifine" : "forge", optifinePath: OPTIFINE },
      authorization: Authenticator.getAuth("PulsarTeste"),
      modSourceDir: path.resolve(__dirname, "..", "..", "mod", "build", "libs"),
      runtimeDir: path.join(work, "runtime"),
      onStatus: (s) => console.log(`[status] ${s.text}${s.total ? ` ${s.done}/${s.total}` : ""}`),
      onLog,
      extraJvmArgs: PRINTS ? ["-Dpulsar.selftest=true", ...bridgeArgs, ...(REDE ? [`-Dpulsar.rede=${REDE}`] : [])] : [],
      onClose: (code) => finish(pulsarLoaded && gameReady && selftestDone ? 0 : 1, `O jogo fechou com código ${code}`),
    });
  } catch (err) {
    onLog(String(err && err.stack ? err.stack : err));
    finish(1, "FALHOU: " + err.message);
  }
})();
