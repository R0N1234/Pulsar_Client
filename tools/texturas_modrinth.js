// Consulta o Modrinth atrás de pacotes de textura para 1.8.9 (usado no CI, que tem acesso à internet).
// Saída: tools/saida/texturas.json com os mais baixados e os arquivos de cada versão 1.8.9.
const fs = require("fs");
const path = require("path");

const API = "https://api.modrinth.com/v2";
const UA = { "User-Agent": "R0N1234/Pulsar_Client (consulta de texturas)" };

async function get(url) {
  const r = await fetch(url, { headers: UA });
  if (!r.ok) throw new Error(`${r.status} ${url}`);
  return r.json();
}

async function main() {
  const out = { busca: [], detalhes: {} };
  const facets = encodeURIComponent(JSON.stringify([["project_type:resourcepack"], ["versions:1.8.9"]]));
  for (const termo of ["", "pvp", "faithful", "fps", "default", "clear", "low fire", "hd", "realistic", "16x"]) {
    for (let offset = 0; offset < (termo ? 50 : 200); offset += 50) {
      const q = termo ? `&query=${encodeURIComponent(termo)}` : "";
      const r = await get(`${API}/search?facets=${facets}&limit=50&offset=${offset}&index=downloads${q}`);
      for (const h of r.hits) {
        if (out.busca.find((x) => x.slug === h.slug)) continue;
        out.busca.push({ slug: h.slug, id: h.project_id, titulo: h.title, desc: h.description, downloads: h.downloads,
          licenca: h.license, categorias: h.categories, versoes: (h.versions || []).filter((v) => v.startsWith("1.8")) });
      }
      if (r.hits.length < 50) break;
    }
  }
  out.busca.sort((a, b) => b.downloads - a.downloads);
  for (const p of out.busca.slice(0, 80)) {
    const versoes = await get(`${API}/project/${p.id}/version?game_versions=${encodeURIComponent('["1.8.9"]')}`);
    out.detalhes[p.slug] = versoes.slice(0, 3).map((v) => ({
      id: v.id, nome: v.name, numero: v.version_number, mc: v.game_versions.filter((g) => g.startsWith("1.8")),
      arquivos: v.files.map((f) => ({ url: f.url, nome: f.filename, tamanho: f.size, sha1: f.hashes.sha1, primario: f.primary })),
    }));
  }
  fs.mkdirSync(path.join(__dirname, "saida"), { recursive: true });
  fs.writeFileSync(path.join(__dirname, "saida", "texturas.json"), JSON.stringify(out, null, 1));
  console.log(`${out.busca.length} pacotes`);
}

main().catch((e) => { console.error(e); process.exit(1); });
