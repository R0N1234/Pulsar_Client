"""Gera os ícones do painel do Pulsar (Shift direito): desenhos brancos simples, pintados na cor certa dentro do jogo.

Uso: python3 tools/icones.py
Saída: mod/src/main/resources/assets/pulsar/textures/gui/icones.png (512x512, 8x8 ícones de 64x64).
A ordem dos ícones é a mesma da lista ICONES em gui/Icones.java.
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "pulsar", "textures", "gui", "icones.png")
S = 256  # desenha grande e reduz para 64: bordas lisas
L = 18   # espessura da linha
B = (255, 255, 255, 255)


def estrela(d, cheia=True):
    pts = []
    for i in range(10):
        r = 110 if i % 2 == 0 else 46
        a = -math.pi / 2 + i * math.pi / 5
        pts.append((128 + r * math.cos(a), 136 + r * math.sin(a)))
    if cheia:
        d.polygon(pts, fill=B)
    else:
        d.line(pts + [pts[0]], fill=B, width=L, joint="curve")


def engrenagem(d):
    for i in range(8):
        a = i * math.pi / 4
        x, y = 128 + 92 * math.cos(a), 128 + 92 * math.sin(a)
        d.ellipse([x - 22, y - 22, x + 22, y + 22], fill=B)
    d.ellipse([40, 40, 216, 216], fill=B)
    d.ellipse([92, 92, 164, 164], fill=(0, 0, 0, 0))


def lupa(d):
    d.ellipse([30, 30, 170, 170], outline=B, width=L + 4)
    d.line([150, 150, 226, 226], fill=B, width=L + 10)


def teclado(d):
    d.rounded_rectangle([16, 60, 240, 196], 22, outline=B, width=L)
    for row, y in enumerate((92, 126)):
        for i in range(5):
            x = 48 + i * 40
            d.rectangle([x - 9, y - 9, x + 9, y + 9], fill=B)
    d.rectangle([80, 152, 176, 168], fill=B)


def mouse(d):
    d.rounded_rectangle([64, 20, 192, 236], 64, outline=B, width=L)
    d.line([128, 20, 128, 104], fill=B, width=L)
    d.line([64, 104, 192, 104], fill=B, width=L)


def espada(d):
    d.line([60, 196, 214, 42], fill=B, width=L + 8)
    d.polygon([(214, 42), (232, 24), (226, 54)], fill=B)
    d.line([40, 150, 106, 216], fill=B, width=L + 4)
    d.line([30, 226, 70, 186], fill=B, width=L + 8)


def escudo(d):
    d.polygon([(128, 20), (220, 56), (210, 150), (128, 236), (46, 150), (36, 56)], outline=B, width=L)
    d.line([128, 40, 128, 210], fill=B, width=L)


def pocao(d):
    d.rectangle([104, 20, 152, 40], fill=B)
    d.line([108, 40, 108, 96], fill=B, width=L)
    d.line([148, 40, 148, 96], fill=B, width=L)
    d.ellipse([40, 84, 216, 244], outline=B, width=L)
    d.chord([58, 102, 198, 226], 10, 170, fill=B)


def mira(d):
    d.ellipse([40, 40, 216, 216], outline=B, width=L)
    for a, b in [((128, 8), (128, 92)), ((128, 164), (128, 248)), ((8, 128), (92, 128)), ((164, 128), (248, 128))]:
        d.line([a, b], fill=B, width=L)
    d.ellipse([116, 116, 140, 140], fill=B)


def monitor(d):
    d.rounded_rectangle([20, 36, 236, 176], 16, outline=B, width=L)
    d.line([128, 176, 128, 216], fill=B, width=L)
    d.line([76, 220, 180, 220], fill=B, width=L)
    d.line([60, 140, 100, 100, 132, 124, 196, 72], fill=B, width=L - 4, joint="curve")


def sinal(d):
    for i, h in enumerate((60, 110, 160, 210)):
        x = 36 + i * 52
        d.rounded_rectangle([x, 230 - h, x + 34, 230], 8, fill=B)


def bussola(d):
    d.ellipse([20, 20, 236, 236], outline=B, width=L)
    d.polygon([(128, 46), (156, 128), (128, 128)], fill=B)
    d.polygon([(128, 210), (100, 128), (128, 128)], outline=B, width=10)


def relogio(d):
    d.ellipse([20, 20, 236, 236], outline=B, width=L)
    d.line([128, 128, 128, 60], fill=B, width=L)
    d.line([128, 128, 180, 150], fill=B, width=L)


def chip(d):
    d.rounded_rectangle([56, 56, 200, 200], 14, outline=B, width=L)
    d.rectangle([96, 96, 160, 160], fill=B)
    for i in range(3):
        o = 84 + i * 44
        for a, b in [((o, 16), (o, 56)), ((o, 200), (o, 240)), ((16, o), (56, o)), ((200, o), (240, o))]:
            d.line([a, b], fill=B, width=12)


def globo(d):
    d.ellipse([20, 20, 236, 236], outline=B, width=L)
    d.ellipse([78, 20, 178, 236], outline=B, width=L - 4)
    d.line([20, 128, 236, 128], fill=B, width=L - 4)
    d.line([40, 74, 216, 74], fill=B, width=L - 6)
    d.line([40, 182, 216, 182], fill=B, width=L - 6)


def lista(d):
    for i in range(4):
        y = 46 + i * 54
        d.ellipse([22, y - 14, 50, y + 14], fill=B)
        d.line([74, y, 234, y], fill=B, width=L)


def coracao(d):
    d.ellipse([24, 40, 132, 148], fill=B)
    d.ellipse([124, 40, 232, 148], fill=B)
    d.polygon([(30, 116), (226, 116), (128, 228)], fill=B)


def olho(d):
    d.arc([0, 60, 256, 316], 215, 325, fill=B, width=L)
    d.arc([0, -60, 256, 196], 35, 145, fill=B, width=L)
    d.ellipse([92, 92, 164, 164], fill=B)


def telescopio(d):
    d.polygon([(30, 120), (190, 40), (214, 92), (54, 172)], outline=B, width=L)
    d.line([122, 140, 80, 236], fill=B, width=L)
    d.line([122, 140, 168, 236], fill=B, width=L)


def orbita(d):
    d.ellipse([10, 84, 246, 172], outline=B, width=L - 4)
    d.ellipse([84, 84, 172, 172], fill=B)
    d.ellipse([212, 100, 244, 132], fill=B)


def cometa(d):
    d.ellipse([150, 30, 226, 106], fill=B)
    for i, w in enumerate((L, L - 4, L - 8)):
        o = (i - 1) * 34
        d.line([160 + o * 0.3, 90 + o, 30 + o * 0.3, 220 + o], fill=B, width=w)


def pessoa(d):
    d.ellipse([88, 16, 168, 96], fill=B)
    d.rounded_rectangle([68, 104, 188, 190], 24, fill=B)
    d.rectangle([84, 186, 120, 244], fill=B)
    d.rectangle([136, 186, 172, 244], fill=B)


def cubo(d):
    t, m, b = (128, 22), (128, 120), (128, 236)
    l1, r1, l2, r2 = (32, 72), (224, 72), (32, 186), (224, 186)
    d.line([t, r1, r2, b, l2, l1, t], fill=B, width=L, joint="curve")
    d.line([l1, m, r1], fill=B, width=L, joint="curve")
    d.line([m, b], fill=B, width=L)


def brilho(d):
    def faisca(cx, cy, r):
        d.polygon([(cx, cy - r), (cx + r * 0.25, cy - r * 0.25), (cx + r, cy), (cx + r * 0.25, cy + r * 0.25),
                   (cx, cy + r), (cx - r * 0.25, cy + r * 0.25), (cx - r, cy), (cx - r * 0.25, cy - r * 0.25)], fill=B)
    faisca(110, 140, 96)
    faisca(200, 52, 44)


def nuvem(d):
    d.ellipse([20, 110, 110, 200], fill=B)
    d.ellipse([70, 60, 180, 170], fill=B)
    d.ellipse([140, 100, 236, 196], fill=B)
    d.rectangle([64, 150, 190, 200], fill=B)


def seta_baixo(d):
    d.line([128, 24, 128, 200], fill=B, width=L + 4)
    d.polygon([(60, 150), (196, 150), (128, 232)], fill=B)


def gota(d):
    d.polygon([(128, 18), (204, 140), (52, 140)], fill=B)
    d.ellipse([50, 92, 206, 240], fill=B)


def balao(d):
    d.rounded_rectangle([20, 30, 236, 186], 36, outline=B, width=L)
    d.polygon([(64, 176), (110, 176), (52, 240)], fill=B)
    for x in (84, 128, 172):
        d.ellipse([x - 13, 96, x + 13, 122], fill=B)


def nota(d):
    d.line([100, 196, 100, 40], fill=B, width=L)
    d.line([100, 40, 214, 20], fill=B, width=L)
    d.line([214, 20, 214, 170], fill=B, width=L)
    d.ellipse([36, 168, 112, 230], fill=B)
    d.ellipse([150, 142, 226, 204], fill=B)


def raio(d):
    d.polygon([(150, 10), (50, 146), (120, 146), (100, 246), (206, 104), (136, 104)], fill=B)


def quadro(d):
    d.rounded_rectangle([18, 40, 238, 216], 16, outline=B, width=L)
    d.polygon([(40, 196), (104, 116), (150, 172), (176, 140), (216, 196)], fill=B)
    d.ellipse([160, 66, 196, 102], fill=B)


def letra(d):
    d.line([40, 230, 128, 26, 216, 230], fill=B, width=L + 6, joint="curve")
    d.line([78, 156, 178, 156], fill=B, width=L)


def barra(d):
    d.rounded_rectangle([14, 104, 242, 152], 24, outline=B, width=L - 4)
    d.rounded_rectangle([28, 118, 160, 138], 10, fill=B)


def bomba(d):
    d.ellipse([28, 68, 196, 236], fill=B)
    d.line([150, 80, 196, 34], fill=B, width=L)
    brilho_peq = [(214, 16), (226, 34), (244, 30), (230, 46), (240, 62), (220, 54), (206, 66), (208, 44)]
    d.polygon(brilho_peq, fill=B)


def ampulheta(d):
    d.line([50, 22, 206, 22], fill=B, width=L)
    d.line([50, 234, 206, 234], fill=B, width=L)
    d.line([66, 22, 190, 234], fill=B, width=L)
    d.line([190, 22, 66, 234], fill=B, width=L)
    d.polygon([(90, 210), (166, 210), (128, 150)], fill=B)


def mochila(d):
    d.rounded_rectangle([40, 60, 216, 240], 30, outline=B, width=L)
    d.arc([86, 14, 170, 100], 180, 360, fill=B, width=L)
    d.rounded_rectangle([76, 140, 180, 200], 12, fill=B)


def impacto(d):
    pts = []
    for i in range(16):
        r = 116 if i % 2 == 0 else 56
        a = i * math.pi / 8
        pts.append((128 + r * math.cos(a), 128 + r * math.sin(a)))
    d.polygon(pts, fill=B)


def caixa_linha(d):
    d.rectangle([40, 30, 216, 226], outline=B, width=L - 6)
    for x, y in [(40, 30), (216, 30), (40, 226), (216, 226)]:
        d.rectangle([x - 18, y - 18, x + 18, y + 18], fill=B)


def seta_voltar(d):
    d.line([200, 128, 60, 128], fill=B, width=L + 6)
    d.line([128, 52, 52, 128, 128, 204], fill=B, width=L + 6, joint="curve")


def amigos(d):
    d.ellipse([46, 40, 116, 110], fill=B)
    d.ellipse([140, 40, 210, 110], fill=B)
    d.pieslice([10, 120, 152, 270], 180, 360, fill=B)
    d.pieslice([104, 120, 246, 270], 180, 360, fill=B)


def medalha(d):
    d.polygon([(70, 20), (110, 20), (140, 110), (100, 110)], fill=B)
    d.polygon([(146, 20), (186, 20), (156, 110), (116, 110)], fill=B)
    d.ellipse([54, 98, 202, 246], outline=B, width=L)
    d.ellipse([100, 144, 156, 200], fill=B)


def grade(d):
    for x in (24, 140):
        for y in (24, 140):
            d.rounded_rectangle([x, y, x + 92, y + 92], 18, fill=B)


def pincel(d):
    d.line([220, 30, 110, 150], fill=B, width=L + 6)
    d.ellipse([40, 140, 130, 230], fill=B)


def camera(d):
    d.rounded_rectangle([16, 70, 240, 220], 22, outline=B, width=L)
    d.rectangle([84, 40, 172, 72], fill=B)
    d.ellipse([84, 100, 172, 188], outline=B, width=L)


def controle(d):
    # Controle de videogame: corpo arredondado com cruz e dois botões.
    d.rounded_rectangle([14, 64, 242, 196], 60, outline=B, width=L)
    d.line([62, 130, 110, 130], fill=B, width=L - 4)
    d.line([86, 106, 86, 154], fill=B, width=L - 4)
    d.ellipse([156, 102, 180, 126], fill=B)
    d.ellipse([182, 132, 206, 156], fill=B)


def camadas(d):
    # Três camadas empilhadas (texturas).
    for dy in (0, 52, 104):
        pts = [(128, 30 + dy), (230, 80 + dy), (128, 130 + dy), (26, 80 + dy)]
        if dy == 0:
            d.polygon(pts, outline=B, width=L)
        else:
            d.line([(26, 80 + dy), (128, 130 + dy), (230, 80 + dy)], fill=B, width=L, joint="curve")


def peca(d):
    # Peça de quebra-cabeça (mods): contorno da forma cheia, tirado com uma erosão.
    from PIL import ImageFilter
    m = Image.new("L", (S, S), 0)
    md = ImageDraw.Draw(m)
    md.rectangle([40, 76, 196, 232], fill=255)
    md.ellipse([88, 24, 148, 84], fill=255)
    md.ellipse([188, 124, 248, 184], fill=255)
    dentro = m.filter(ImageFilter.MinFilter(L + 1))
    from PIL import ImageChops
    borda = ImageChops.subtract(m, dentro)
    d._image.paste(B, (0, 0), borda)


def sair(d):
    # Porta com a seta saindo.
    d.line([(150, 40), (40, 40), (40, 216), (150, 216)], fill=B, width=L, joint="curve")
    d.line([92, 128, 226, 128], fill=B, width=L)
    d.line([(180, 82), (228, 128), (180, 174)], fill=B, width=L, joint="curve")


def camisa(d):
    # Camiseta (cosméticos).
    pts = [(88, 36), (128, 60), (168, 36), (236, 80), (206, 130), (184, 116), (184, 224), (72, 224), (72, 116), (50, 130), (20, 80)]
    d.polygon(pts, outline=B, width=L)


ICONES = [
    lambda d: estrela(d, True), lambda d: estrela(d, False), engrenagem, lupa, teclado, mouse, espada, escudo,
    pocao, mira, monitor, sinal, bussola, relogio, chip, globo,
    lista, coracao, olho, telescopio, orbita, cometa, pessoa, cubo,
    brilho, nuvem, seta_baixo, gota, balao, nota, raio, quadro,
    letra, barra, bomba, ampulheta, mochila, impacto, caixa_linha, seta_voltar,
    amigos, medalha, grade, pincel, camera, controle, camadas, peca,
    sair, camisa,
]


def main():
    atlas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    for i, fn in enumerate(ICONES):
        img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        fn(ImageDraw.Draw(img))
        atlas.alpha_composite(img.resize((60, 60), Image.LANCZOS), ((i % 8) * 64 + 2, (i // 8) * 64 + 2))
    atlas.save(OUT, optimize=True)
    print(len(ICONES), "ícones")


if __name__ == "__main__":
    main()
