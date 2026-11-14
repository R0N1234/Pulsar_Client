// Gravador do Pulsar: roda numa janela escondida do launcher e grava a janela do jogo com a captura de tela
// do Chromium (MediaRecorder). Dois jeitos de gravar, que podem rodar juntos:
//   - gravação: começa e para quando a pessoa aperta a tecla; sai um vídeo inteiro;
//   - clipe: dois gravadores revezando, para sempre ter guardados os últimos segundos (entre N e 2N),
//     e "clipe" salva o mais antigo deles num arquivo.
// Os pedaços do vídeo vão para o main.js (lib/gravacao.js), que escreve em Vídeos/Pulsar Client.
(() => {
  "use strict";
  const api = window.gravador;
  const PEDACO = 4 * 1024 * 1024;

  let stream = null;
  let streamChave = "";
  let gravacao = null; // { rec, chunks, desde }
  let buffer = null; // { recs: [{rec, chunks, desde}], timer, opts }
  let mime = "";

  function escolherMime() {
    const lista = [
      'video/mp4;codecs="avc1.42E01E,mp4a.40.2"',
      'video/mp4;codecs="avc1.42E01E,opus"',
      "video/mp4",
      'video/webm;codecs="h264,opus"',
      'video/webm;codecs="vp9,opus"',
      'video/webm;codecs="vp8,opus"',
      "video/webm",
    ];
    return lista.find((m) => window.MediaRecorder && MediaRecorder.isTypeSupported(m)) || "";
  }

  const extensao = () => (mime.startsWith("video/mp4") ? "mp4" : "webm");

  function tamanho(qualidade) {
    if (qualidade === "720p") return { maxWidth: 1280, maxHeight: 720 };
    if (qualidade === "1080p") return { maxWidth: 1920, maxHeight: 1080 };
    return {};
  }

  // Abre a captura da janela do jogo. O som do computador vem junto quando o Chromium deixa (Windows);
  // se a janela não vier com som, tenta pegar o som pela tela inteira; o microfone entra misturado.
  async function abrirStream(fonte, opts) {
    const chave = `${fonte.id}|${opts.qualidade}|${opts.fps}|${opts.som}|${opts.microfone}`;
    if (stream && streamChave === chave && stream.getVideoTracks().some((t) => t.readyState === "live")) return stream;
    fecharStream();
    const video = { mandatory: { chromeMediaSource: "desktop", chromeMediaSourceId: fonte.id, maxFrameRate: opts.fps, ...tamanho(opts.qualidade) } };
    let s = null;
    if (opts.som) {
      try {
        s = await navigator.mediaDevices.getUserMedia({ audio: { mandatory: { chromeMediaSource: "desktop", chromeMediaSourceId: fonte.id } }, video });
      } catch (e) {
        api.log(`som junto da janela não deu (${e.name}); tentando só o vídeo`);
      }
    }
    if (!s) s = await navigator.mediaDevices.getUserMedia({ audio: false, video });
    const audios = [];
    if (opts.som && s.getAudioTracks().length === 0 && fonte.telaId) {
      try {
        const tela = await navigator.mediaDevices.getUserMedia({
          audio: { mandatory: { chromeMediaSource: "desktop", chromeMediaSourceId: fonte.telaId } },
          video: { mandatory: { chromeMediaSource: "desktop", chromeMediaSourceId: fonte.telaId, maxWidth: 16, maxHeight: 16, maxFrameRate: 1 } },
        });
        tela.getVideoTracks().forEach((t) => t.stop());
        audios.push(...tela.getAudioTracks());
      } catch (e) {
        api.log(`sem som do computador (${e.name})`);
      }
    } else {
      audios.push(...s.getAudioTracks());
    }
    if (opts.microfone) {
      try {
        const mic = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: false, noiseSuppression: true }, video: false });
        audios.push(...mic.getAudioTracks());
      } catch (e) {
        api.log(`sem microfone (${e.name})`);
      }
    }
    const final = new MediaStream(s.getVideoTracks());
    if (audios.length === 1) final.addTrack(audios[0]);
    else if (audios.length > 1) {
      // Mais de uma fonte de som (computador + microfone): mistura numa só, porque o MediaRecorder grava uma.
      const ctx = new AudioContext();
      const destino = ctx.createMediaStreamDestination();
      for (const t of audios) ctx.createMediaStreamSource(new MediaStream([t])).connect(destino);
      final.addTrack(destino.stream.getAudioTracks()[0]);
      final._ctx = ctx;
      final._fontes = audios;
    }
    final._base = s;
    final.getVideoTracks()[0].addEventListener("ended", () => api.log("a janela do jogo fechou; captura encerrada"));
    stream = final;
    streamChave = chave;
    api.log(`capturando ${fonte.nome} (${opts.qualidade}, ${opts.fps} fps, som ${final.getAudioTracks().length ? "sim" : "não"}, ${mime || "sem codec"})`);
    return final;
  }

  function fecharStream() {
    if (!stream) return;
    try {
      stream.getTracks().forEach((t) => t.stop());
      if (stream._base) stream._base.getTracks().forEach((t) => t.stop());
      if (stream._fontes) stream._fontes.forEach((t) => t.stop());
      if (stream._ctx) stream._ctx.close();
    } catch {}
    stream = null;
    streamChave = "";
  }

  function bitrate(opts) {
    const base = opts.qualidade === "720p" ? 6e6 : opts.qualidade === "1080p" ? 10e6 : 14e6;
    return opts.fps === 60 ? base * 1.5 : base;
  }

  function novoGravador(s, opts) {
    const rec = new MediaRecorder(s, { mimeType: mime || undefined, videoBitsPerSecond: bitrate(opts), audioBitsPerSecond: 160000 });
    const item = { rec, chunks: [], desde: Date.now() };
    rec.ondataavailable = (e) => {
      if (e.data && e.data.size) item.chunks.push(e.data);
    };
    rec.start(1000);
    return item;
  }

  // Para o gravador e espera o último pedaço chegar.
  function parar(item) {
    return new Promise((resolve) => {
      if (item.rec.state === "inactive") return resolve(item.chunks);
      item.rec.onstop = () => resolve(item.chunks);
      try {
        item.rec.stop();
      } catch {
        resolve(item.chunks);
      }
    });
  }

  // Manda o vídeo em pedaços de 4 MB para o launcher escrever no arquivo.
  async function enviar(id, chunks) {
    const blob = new Blob(chunks, { type: mime || "video/webm" });
    const total = blob.size;
    const ext = extensao();
    if (!total) {
      api.pedaco({ id, extensao: ext, dados: new ArrayBuffer(0), fim: true });
      return 0;
    }
    for (let i = 0; i < total; i += PEDACO) {
      const dados = await blob.slice(i, Math.min(total, i + PEDACO)).arrayBuffer();
      api.pedaco({ id, extensao: ext, dados, fim: i + PEDACO >= total });
    }
    return total;
  }

  const estado = () => ({
    gravando: !!gravacao,
    desde: gravacao ? gravacao.desde : 0,
    guardando: !!buffer,
  });

  // ---------- clipe: dois gravadores revezando ----------
  function ligarBuffer(s, opts) {
    if (buffer) return;
    buffer = { recs: [novoGravador(s, opts)], opts, timer: null };
    buffer.timer = setInterval(() => rodarBuffer(s), opts.clipe * 1000);
  }

  function rodarBuffer(s) {
    if (!buffer) return;
    buffer.recs.push(novoGravador(s, buffer.opts));
    // Fica no máximo com dois: o mais antigo (entre N e 2N segundos) e o novo.
    while (buffer.recs.length > 2) {
      const velho = buffer.recs.shift();
      parar(velho).then(() => (velho.chunks.length = 0));
    }
  }

  async function desligarBuffer() {
    if (!buffer) return;
    clearInterval(buffer.timer);
    const recs = buffer.recs;
    buffer = null;
    for (const r of recs) await parar(r);
  }

  async function salvarClipe(id, s, opts) {
    if (!buffer) {
      ligarBuffer(s, opts);
      throw new Error("O clipe começou a guardar agora; aperte de novo daqui a alguns segundos.");
    }
    const mais = buffer.recs.shift();
    // Já começa outro no lugar, para o próximo clipe não ficar curto.
    buffer.recs.push(novoGravador(s, buffer.opts));
    const chunks = await parar(mais);
    const bytes = await enviar(id, chunks);
    api.log(`clipe de ${Math.round((Date.now() - mais.desde) / 1000)} s, ${Math.round(bytes / 1024)} KB`);
  }

  // ---------- pedidos do launcher ----------
  async function tratar(c) {
    if (!mime) mime = escolherMime();
    if (!window.MediaRecorder) throw new Error("Este computador não tem o gravador de vídeo do Chromium.");
    const { acao, opts } = c;
    if (acao === "iniciar") {
      if (gravacao) return estado();
      const s = await abrirStream(c.fonte, opts);
      gravacao = novoGravador(s, opts);
      return estado();
    }
    if (acao === "parar") {
      if (!gravacao) return estado();
      const g = gravacao;
      gravacao = null;
      const chunks = await parar(g);
      const bytes = await enviar(c.id, chunks);
      api.log(`gravação de ${Math.round((Date.now() - g.desde) / 1000)} s, ${Math.round(bytes / 1024)} KB`);
      if (!buffer) fecharStream();
      return estado();
    }
    if (acao === "buffer") {
      if (opts.ligado) ligarBuffer(await abrirStream(c.fonte, opts), opts);
      else {
        await desligarBuffer();
        if (!gravacao) fecharStream();
      }
      return estado();
    }
    if (acao === "clipe") {
      await salvarClipe(c.id, await abrirStream(c.fonte, opts), opts);
      return estado();
    }
    throw new Error("Pedido desconhecido.");
  }

  api.onComando(async (c) => {
    try {
      const r = await tratar(c);
      api.responder({ id: c.id, ok: true, ...r });
    } catch (e) {
      api.log(`erro em ${c.acao}: ${e && e.message ? e.message : e}`);
      api.responder({ id: c.id, ok: false, erro: String((e && e.message) || e), ...estado() });
    }
  });
})();
