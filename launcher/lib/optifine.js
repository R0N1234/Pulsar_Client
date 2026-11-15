// Ajuda a instalar o OptiFine 1.8.9. O OptiFine não pode vir junto com o Pulsar (a licença dele não deixa),
// então o launcher abre o site oficial e fica olhando a pasta Downloads: quando o arquivo termina de baixar,
// ele é copiado para a pasta do launcher e já fica escolhido.
const fs = require("fs");
const path = require("path");

// OptiFine_1.8.9_HD_U_M5.jar (e variações: preview_, HD_U_L5...). Download em andamento termina em .crdownload/.part e não entra.
const NOME = /^(preview_)?OptiFine_1\.8\.9[^\\/]*\.jar$/i;
const MINIMO = 500 * 1024;

/** O arquivo parece um jar inteiro (começa como zip e tem um tamanho de verdade)? */
function valido(arquivo) {
  try {
    const st = fs.statSync(arquivo);
    if (!st.isFile() || st.size < MINIMO) return false;
    const fd = fs.openSync(arquivo, "r");
    const b = Buffer.alloc(4);
    fs.readSync(fd, b, 0, 4, 0);
    fs.closeSync(fd);
    return b[0] === 0x50 && b[1] === 0x4b;
  } catch {
    return false;
  }
}

/** O OptiFine 1.8.9 mais novo da pasta (modificado depois de "desde", em ms). "" se não tiver. */
function procurar(pasta, desde = 0) {
  let nomes = [];
  try {
    nomes = fs.readdirSync(pasta);
  } catch {
    return "";
  }
  let melhor = "";
  let quando = -1;
  for (const n of nomes) {
    if (!NOME.test(n)) continue;
    const p = path.join(pasta, n);
    try {
      const t = fs.statSync(p).mtimeMs;
      if (t >= desde && t > quando) {
        melhor = p;
        quando = t;
      }
    } catch {}
  }
  return melhor;
}

/** Copia para a pasta do launcher (o arquivo na Downloads pode ser apagado depois). Devolve o caminho novo. */
function guardar(origem, pastaDestino) {
  fs.mkdirSync(pastaDestino, { recursive: true });
  const destino = path.join(pastaDestino, path.basename(origem));
  if (path.resolve(origem) !== path.resolve(destino)) fs.copyFileSync(origem, destino);
  return destino;
}

/** Olha a pasta até o OptiFine aparecer inteiro (o tamanho para de mudar), ou até o tempo acabar. */
class Vigia {
  constructor() {
    this.timer = null;
  }

  ativo() {
    return !!this.timer;
  }

  comecar(pasta, { aoAchar, aoDesistir, minutos = 20, desde = Date.now() - 60 * 1000 }) {
    this.parar();
    const fim = Date.now() + minutos * 60 * 1000;
    let ultimo = "";
    let tamanho = -1;
    this.timer = setInterval(() => {
      if (Date.now() > fim) {
        this.parar();
        if (aoDesistir) aoDesistir();
        return;
      }
      const f = procurar(pasta, desde);
      if (!f) return;
      let t = -1;
      try {
        t = fs.statSync(f).size;
      } catch {}
      // Mesmo arquivo com o mesmo tamanho em duas olhadas seguidas: terminou de baixar.
      if (f === ultimo && t === tamanho && valido(f)) {
        this.parar();
        aoAchar(f);
        return;
      }
      ultimo = f;
      tamanho = t;
    }, 1500);
  }

  parar() {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }
}

module.exports = { NOME, valido, procurar, guardar, Vigia };
