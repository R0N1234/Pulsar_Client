"""Gera as camadas do logo animado do menu principal do Pulsar (o "PC" do Rick dentro do anel com órbitas).

Uso: python3 tools/logo_menu.py
Saída em mod/src/main/resources/assets/pulsar/textures/gui/:
  logo_anel.png     disco escuro, anel ciano -> violeta e o brilho em volta (fica parado)
  logo_vortice.png  braços de nebulosa dentro do disco (gira devagar no jogo)
  logo_orbitas.png  as duas órbitas inclinadas (as luas andam por elas no jogo, ver Universo.logoMenu)
  logo_letras.png   as letras "PC" do Rick, brancas com um brilho leve

Todas são 512x512 com o centro do anel no meio. As órbitas usam os mesmos números de ORBITAS_LOGO em Universo.java.
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "pulsar", "textures", "gui")
N = 512
SS = 4  # desenha 4x maior e reduz: bordas lisas
C = N / 2
RAIO = 150  # raio do anel
CIANO = (64, 224, 255)
VIOLETA = (168, 92, 255)
# Órbitas: semi-eixo maior, semi-eixo menor, inclinação em graus (sentido horário, y para baixo).
ORBITAS = [(246, 74, 24.0), (236, 66, -22.0)]

# Letras "PC" do Rick (traçadas do print do menu), no espaço 0..537 x 0..473.
P = [(45, 22), (290, 22), (355, 87), (355, 152), (282, 225), (118, 230), (118, 350), (45, 420), (45, 165),
     (250, 165), (290, 125), (265, 87), (45, 87)]
LC = [(345, 170), (545, 170), (582, 212), (582, 262), (525, 262), (498, 232), (408, 232), (300, 335), (300, 385),
      (365, 433), (498, 433), (525, 402), (582, 402), (582, 460), (545, 495), (330, 495), (232, 410), (232, 300)]


def mistura(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def grande():
    return Image.new("RGBA", (N * SS, N * SS), (0, 0, 0, 0))


def reduzir(img):
    return img.resize((N, N), Image.LANCZOS)


def gradiente_diagonal():
    """Cor do ciano (em cima à esquerda) ao violeta (embaixo à direita), na escala grande."""
    g = Image.new("RGB", (N * SS, N * SS))
    px = g.load()
    w = N * SS
    for y in range(0, w, SS):
        for x in range(0, w, SS):
            c = mistura(CIANO, VIOLETA, (x + y) / (2 * w) * 1.6 - 0.3)
            for dy in range(SS):
                for dx in range(SS):
                    px[x + dx, y + dy] = c
    return g


def anel(grad):
    s = SS
    img = grande()
    # Brilho de fora: anel largo e desfocado.
    brilho = Image.new("L", img.size, 0)
    ImageDraw.Draw(brilho).ellipse([(C - RAIO - 6) * s, (C - RAIO - 6) * s, (C + RAIO + 6) * s, (C + RAIO + 6) * s],
                                   outline=255, width=26 * s)
    brilho = brilho.filter(ImageFilter.GaussianBlur(18 * s))
    camada = grad.convert("RGBA")
    camada.putalpha(brilho.point(lambda v: int(v * 0.85)))
    img.alpha_composite(camada)
    # Disco escuro com o centro um pouco violeta.
    disco = Image.new("RGBA", img.size, (0, 0, 0, 0))
    dd = ImageDraw.Draw(disco)
    for i in range(60, -1, -1):
        r = RAIO * i / 60
        c = mistura((10, 8, 34), (34, 18, 78), 1 - i / 60)
        dd.ellipse([(C - r) * s, (C - r) * s, (C + r) * s, (C + r) * s], fill=c + (250,))
    img.alpha_composite(disco)
    # O anel em si, fino e forte, e um brilho branco por cima.
    linha = Image.new("L", img.size, 0)
    ImageDraw.Draw(linha).ellipse([(C - RAIO) * s, (C - RAIO) * s, (C + RAIO) * s, (C + RAIO) * s], outline=255, width=7 * s)
    camada = grad.convert("RGBA")
    camada.putalpha(linha)
    img.alpha_composite(camada)
    luz = Image.new("L", img.size, 0)
    ImageDraw.Draw(luz).ellipse([(C - RAIO + 2) * s, (C - RAIO + 2) * s, (C + RAIO - 2) * s, (C + RAIO - 2) * s],
                                outline=255, width=2 * s)
    branco = Image.new("RGBA", img.size, (230, 250, 255, 0))
    branco.putalpha(luz.point(lambda v: int(v * 0.55)))
    img.alpha_composite(branco)
    return reduzir(img)


def vortice():
    s = SS
    img = grande()
    m = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(m)
    for braco in range(3):
        base = braco * 2 * math.pi / 3
        pts = []
        for i in range(120):
            t = i / 119
            r = 12 + t * (RAIO - 22)
            a = base + t * 3.4
            pts.append(((C + r * math.cos(a)) * s, (C + r * math.sin(a)) * s))
        d.line(pts, fill=255, width=int(16 * s), joint="curve")
    m = m.filter(ImageFilter.GaussianBlur(14 * s))
    # Só dentro do disco.
    corte = Image.new("L", img.size, 0)
    ImageDraw.Draw(corte).ellipse([(C - RAIO + 6) * s, (C - RAIO + 6) * s, (C + RAIO - 6) * s, (C + RAIO - 6) * s], fill=255)
    m = ImageChops.multiply(m, corte.filter(ImageFilter.GaussianBlur(10 * s)))
    cor = Image.new("RGBA", img.size, (120, 70, 230, 0))
    cor.putalpha(m.point(lambda v: int(v * 0.55)))
    img.alpha_composite(cor)
    return reduzir(img)


def orbitas(grad):
    s = SS
    img = grande()
    for a, b, inc in ORBITAS:
        m = Image.new("L", img.size, 0)
        pts = []
        cs, sn = math.cos(math.radians(inc)), math.sin(math.radians(inc))
        for i in range(721):
            t = i / 720 * 2 * math.pi
            x, y = a * math.cos(t), b * math.sin(t)
            pts.append(((C + x * cs - y * sn) * s, (C + x * sn + y * cs) * s))
        ImageDraw.Draw(m).line(pts, fill=255, width=3 * s, joint="curve")
        glow = m.filter(ImageFilter.GaussianBlur(5 * s))
        camada = grad.convert("RGBA")
        camada.putalpha(ImageChops.add(m.point(lambda v: int(v * 0.85)), glow.point(lambda v: int(v * 0.8))))
        img.alpha_composite(camada)
    # Dentro do disco as órbitas ficam fracas, como se passassem por trás do vidro.
    fraco = Image.new("L", img.size, 255)
    ImageDraw.Draw(fraco).ellipse([(C - RAIO + 4) * s, (C - RAIO + 4) * s, (C + RAIO - 4) * s, (C + RAIO - 4) * s], fill=80)
    img.putalpha(ImageChops.multiply(img.getchannel("A"), fraco.filter(ImageFilter.GaussianBlur(3 * s))))
    return reduzir(img)


def letras():
    s = SS
    img = grande()
    largura = 172.0
    k = largura / 537.0
    alto = 473 * k
    x0, y0 = C - largura / 2, C - alto / 2

    m = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(m)
    # Encaixa a caixa das letras (45..582 x 22..495) no centro.
    def p2(p):
        return ((x0 + (p[0] - 45) * k) * s, (y0 + (p[1] - 22) * k) * s)

    d.polygon([p2(p) for p in P], fill=255)
    d.polygon([p2(p) for p in LC], fill=255)
    glow = m.filter(ImageFilter.GaussianBlur(7 * s))
    luz = Image.new("RGBA", img.size, (150, 230, 255, 0))
    luz.putalpha(glow.point(lambda v: int(v * 0.6)))
    img.alpha_composite(luz)
    branco = Image.new("RGBA", img.size, (250, 252, 255, 0))
    branco.putalpha(m)
    img.alpha_composite(branco)
    return reduzir(img)


def main():
    grad = gradiente_diagonal()
    anel(grad).save(os.path.join(OUT, "logo_anel.png"), optimize=True)
    vortice().save(os.path.join(OUT, "logo_vortice.png"), optimize=True)
    orbitas(grad).save(os.path.join(OUT, "logo_orbitas.png"), optimize=True)
    letras().save(os.path.join(OUT, "logo_letras.png"), optimize=True)
    print("logo do menu gerado")


if __name__ == "__main__":
    main()
