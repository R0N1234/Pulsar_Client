"""Gera a Fonte do Pulsar usada dentro do Minecraft: um atlas de glifos e a tabela de medidas.

A fonte é a Exo 2 (SIL Open Font License, veja tools/fontes/OFL.txt), no peso SemiBold.
As medidas ficam em "pixels de interface" do Minecraft: a linha tem 9 de altura e a base das letras fica em 7,
igual à fonte original do jogo. O atlas é desenhado 4x maior para ficar nítido em qualquer escala de interface.

Uso: python3 tools/fonte_pulsar.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

AQUI = os.path.dirname(os.path.abspath(__file__))
FONTE = os.path.join(AQUI, "fontes", "Exo2.ttf")
SAIDA = os.path.join(AQUI, "..", "mod", "src", "main", "resources", "assets", "pulsar", "fonte")

R = 4            # texels por pixel de interface
ALTURA_MAIUSCULA = 6.6
BASE = 7.0       # linha de base, medida do topo da linha
ESPACAMENTO = 0.25
PAD = 2          # texels livres em volta de cada glifo
LARGURA_ATLAS = 1024

# Mesma tabela de caracteres da fonte do Minecraft 1.8.9 (sem os desenhos de caixa, que a Exo 2 não tem).
TABELA_MC = (
    "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ"
    " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
    "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒ"
    "áíóúñÑªº¿®¬½¼¡«»"
    "αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■"
)
EXTRAS = "".join(chr(c) for c in range(0xA0, 0x180)) + "–—‘’“”•…€™"


def fonte(tamanho):
    f = ImageFont.truetype(FONTE, tamanho)
    f.set_variation_by_name("SemiBold")
    return f


def main():
    # Acha o tamanho em que a altura do "H" dá ALTURA_MAIUSCULA pixels de interface.
    teste = fonte(400)
    l, t, r, b = teste.getbbox("H", anchor="ls")
    tamanho = round(400 * ALTURA_MAIUSCULA * R / (b - t))
    f = fonte(tamanho)

    def desenho(c):
        img = Image.new("L", (tamanho * 2, tamanho * 2), 0)
        ImageDraw.Draw(img).text((tamanho // 2, tamanho), c, font=f, fill=255, anchor="ls")
        return img.tobytes()

    # Glifo que a fonte usa quando não tem o caractere: serve para descobrir o que falta.
    falta = desenho("\ue000")

    chars = []
    for c in TABELA_MC + EXTRAS:
        if c in chars or c == "§" or c == "­":
            continue
        if c != " " and c != " " and desenho(c) == falta:
            continue
        chars.append(c)

    glifos = []
    for c in chars:
        avanco = f.getlength(c) / R + ESPACAMENTO
        if c in (" ", " "):
            glifos.append((c, None, 0, 0, max(avanco, 3.2)))
            continue
        l, t, r, b = f.getbbox(c, anchor="ls")
        w, h = r - l + 2 * PAD, b - t + 2 * PAD
        img = Image.new("L", (w, h), 0)
        ImageDraw.Draw(img).text((PAD - l, PAD - t), c, font=f, fill=255, anchor="ls")
        glifos.append((c, img, (l - PAD) / R, BASE + (t - PAD) / R, avanco))

    # Empilha em prateleiras.
    x = y = altura_linha = 0
    posicoes = []
    for c, img, ox, oy, avanco in glifos:
        if img is None:
            posicoes.append((0, 0))
            continue
        if x + img.width > LARGURA_ATLAS:
            x, y, altura_linha = 0, y + altura_linha, 0
        posicoes.append((x, y))
        x += img.width
        altura_linha = max(altura_linha, img.height)
    altura = 1
    while altura < y + altura_linha:
        altura *= 2

    atlas = Image.new("RGBA", (LARGURA_ATLAS, altura), (255, 255, 255, 0))
    linhas = ["# caractere x y largura altura ox oy avanco (atlas %dx%d, %d texels por pixel)" % (LARGURA_ATLAS, altura, R)]
    for (c, img, ox, oy, avanco), (px, py) in zip(glifos, posicoes):
        if img is not None:
            branco = Image.new("RGBA", img.size, (255, 255, 255, 0))
            branco.putalpha(img)
            atlas.paste(branco, (px, py))
            linhas.append("%d %d %d %d %d %.3f %.3f %.3f" % (ord(c), px, py, img.width, img.height, ox, oy, avanco))
        else:
            linhas.append("%d 0 0 0 0 0 0 %.3f" % (ord(c), avanco))

    os.makedirs(SAIDA, exist_ok=True)
    atlas.save(os.path.join(SAIDA, "atlas.png"), optimize=True)
    with open(os.path.join(SAIDA, "glifos.txt"), "w", encoding="utf-8") as out:
        out.write("\n".join(linhas) + "\n")
    with open(os.path.join(AQUI, "fontes", "OFL.txt"), encoding="utf-8") as src, \
            open(os.path.join(SAIDA, "LICENCA-Exo2.txt"), "w", encoding="utf-8") as dst:
        dst.write("Fonte Exo 2, por Natanael Gama, usada sob a SIL Open Font License 1.1.\n\n" + src.read())
    print("%d glifos, atlas %dx%d, tamanho %d" % (len(glifos), LARGURA_ATLAS, altura, tamanho))


if __name__ == "__main__":
    main()
