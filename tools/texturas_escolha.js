// Confere de perto os pacotes de textura candidatos para a aba Texturas (roda no CI, que alcança o Modrinth).
// Para cada um: baixa a versão 1.8.9, abre o zip e anota o formato do pack, a resolução e quantas texturas
// de blocos e itens ele troca, se depende do OptiFine e o ícone do projeto.
// Saída: tools/saida/texturas_escolha.json
const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const API = "https://api.modrinth.com/v2";
const UA = { "User-Agent": "R0N1234/Pulsar_Client (escolha de texturas)" };
const MAX_BYTES = 120 * 1024 * 1024;

const CANDIDATOS = [
  // PvP e desempenho
  "bare-bones", "f8thful", "defrosted_pack", "looshy", "kreklinokpackpvp", "pvp-for-cuties", "simple-bedwars",
  "blade-16x", "betterbedwars", "technofault", "prism-original", "pvp-zegarn", "simple-pvp-edit",
  // Qualidade
  "faithful-32x", "faithful-64x", "classic-faithful-32x", "classic-faithful-64x", "modernity", "default-hd-128x",
  "clayful-128x", "autumnpack", "lively-by-alexio", "jicklus", "new-default-plus", "optimum-realism",
];
const BUSCAS = ["faithful", "32x", "64x", "hd", "realistic", "pvp", "fps"];

async function get(url) {
  const r = await fetch(url, { headers: UA });
  if (!r.ok) throw new Error(`${r.status} ${url}`);
  return r.json();
}

async function baixar(url) {
  const r = await fetch(url, { headers: UA });
  if (!r.ok) throw new Error(`${r.status} ${url}`);
  return Buffer.from(await r.arrayBuffer());
}

// Leitor mínimo de zip: diretório central e arquivos guardados ou comprimidos com deflate.
function lerZip(buf) {
  let fim = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65557); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { fim = i; break; }
  }
  if (fim < 0) throw new Error("zip sem diretório central");
  const total = buf.readUInt16LE(fim + 10);
  let p = buf.readUInt32LE(fim + 16);
  const entradas = [];
  for (let n = 0; n < total; n++) {
    if (buf.readUInt32LE(p) !== 0x02014b50) break;
    const metodo = buf.readUInt16LE(p + 10);
    const comprimido = buf.readUInt32LE(p + 20);
    const nomeLen = buf.readUInt16LE(p + 28);
    const extraLen = buf.readUInt16LE(p + 30);
    const comentLen = buf.readUInt16LE(p + 32);
    const local = buf.readUInt32LE(p + 42);
    const nome = buf.slice(p + 46, p + 46 + nomeLen).toString("utf8");
    entradas.push({ nome, metodo, comprimido, local });
    p += 46 + nomeLen + extraLen + comentLen;
  }
  const ler = (e) => {
    const inicio = e.local + 30 + buf.readUInt16LE(e.local + 26) + buf.readUInt16LE(e.local + 28);
    const dados = buf.slice(inicio, inicio + e.comprimido);
    return e.metodo === 0 ? dados : zlib.inflateRawSync(dados);
  };
  return { entradas, ler };
}

function examinar(buf) {
  const { entradas, ler } = lerZip(buf);
  // Alguns zips têm o pack dentro de uma pasta: acha onde está o pack.mcmeta.
  const meta = entradas.find((e) => /(^|\/)pack\.mcmeta$/i.test(e.nome) && e.nome.split("/").length <= 2);
  const raiz = meta ? meta.nome.replace(/pack\.mcmeta$/i, "") : "";
  const sob = (pasta) => entradas.filter((e) => e.nome.startsWith(raiz + pasta) && e.nome.endsWith(".png"));
  let formato = null;
  let descricao = "";
  if (meta) {
    try {
      const j = JSON.parse(ler(meta).toString("utf8").replace(/^﻿/, ""));
      formato = j.pack && j.pack.pack_format;
      descricao = j.pack && (typeof j.pack.description === "string" ? j.pack.description : JSON.stringify(j.pack.description));
    } catch (e) {
      formato = "inválido";
    }
  }
  const blocos = sob("assets/minecraft/textures/blocks/");
  const itens = sob("assets/minecraft/textures/items/");
  const blocosNovos = sob("assets/minecraft/textures/block/");
  let resolucao = null;
  const amostra = blocos.find((e) => /\/(stone|dirt|cobblestone|planks_oak)\.png$/.test(e.nome)) || blocos[0];
  if (amostra) {
    const png = ler(amostra);
    if (png.readUInt32BE(12) === 0x49484452) resolucao = png.readUInt32BE(16);
  }
  return {
    raizNoZip: raiz,
    formato,
    descricao,
    texturasBlocos: blocos.length,
    texturasItens: itens.length,
    texturasBlocosVersaoNova: blocosNovos.length,
    resolucao,
    optifine: entradas.some((e) => /assets\/minecraft\/(mcpatcher|optifine)\//.test(e.nome)),
    temPackPng: entradas.some((e) => e.nome === raiz + "pack.png"),
    arquivos: entradas.length,
  };
}

async function main() {
  const slugs = new Set(CANDIDATOS);
  const facets = encodeURIComponent(JSON.stringify([["project_type:resourcepack"], ["versions:1.8.9"]]));
  for (const termo of BUSCAS) {
    const r = await get(`${API}/search?facets=${facets}&limit=15&index=downloads&query=${encodeURIComponent(termo)}`);
    for (const h of r.hits) slugs.add(h.slug);
  }
  const out = [];
  for (const slug of slugs) {
    const item = { slug };
    out.push(item);
    try {
      const p = await get(`${API}/project/${encodeURIComponent(slug)}`);
      Object.assign(item, { id: p.id, titulo: p.title, desc: p.description, icone: p.icon_url, licenca: p.license && p.license.id,
        downloads: p.downloads, categorias: p.categories });
      const versoes = await get(`${API}/project/${p.id}/version?game_versions=${encodeURIComponent('["1.8.9"]')}`);
      if (!versoes.length) { item.erro = "sem versão 1.8.9"; continue; }
      const v = versoes[0];
      const f = v.files.find((x) => x.primary) || v.files[0];
      Object.assign(item, { versao: v.id, versaoNome: v.name, versaoNumero: v.version_number, mc: v.game_versions,
        arquivo: { url: f.url, nome: f.filename, tamanho: f.size, sha1: f.hashes.sha1, sha512: f.hashes.sha512 } });
      if (f.size > MAX_BYTES) { item.erro = "grande demais para examinar"; continue; }
      item.exame = examinar(await baixar(f.url));
      console.log(slug, JSON.stringify(item.exame));
    } catch (e) {
      item.erro = String(e.message || e);
      console.log(slug, "ERRO", item.erro);
    }
  }
  fs.mkdirSync(path.join(__dirname, "saida"), { recursive: true });
  fs.writeFileSync(path.join(__dirname, "saida", "texturas_escolha.json"), JSON.stringify(out, null, 1));
  console.log(`${out.length} pacotes examinados`);
}

main().catch((e) => { console.error(e); process.exit(1); });
