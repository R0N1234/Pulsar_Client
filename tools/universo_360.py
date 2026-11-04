"""Gera o universo 360° do menu do Pulsar: o céu que gira devagar atrás do menu, os planetas e o pulsar.

Uso: python3 tools/universo_360.py
Saídas (pasta de texturas do mod):
  ceu.png       4096x1024, emenda sem costura nas bordas da esquerda e da direita (dá a volta inteira).
                Via Láctea, nebulosas, estrelas e a galáxia de Andrômeda.
  planetas.png  1024x128, os 8 planetas (128x128 cada), desenhados por cima do céu com paralaxe.
  pulsar.png    256x256: na esquerda o núcleo (128x128), na direita o feixe de luz (64x256).
A semente é fixa, então o resultado é sempre igual.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD_TEX = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "pulsar", "textures", "gui")
W, H = 4096, 1024
rng = np.random.default_rng(80)


def ruido(h, w, potencia, semente):
    """Ruído suave que emenda nas bordas (filtrado na frequência, então é periódico nos dois eixos)."""
    r = np.random.default_rng(semente)
    f = np.fft.fft2(r.standard_normal((h, w)))
    fy = np.fft.fftfreq(h)[:, None]
    fx = np.fft.fftfreq(w)[None, :]
    k = np.sqrt(fx * fx + fy * fy)
    k[0, 0] = 1
    n = np.real(np.fft.ifft2(f / k ** potencia))
    n -= n.min()
    return n / n.max()


def wrap_dx(x, cx):
    """Distância horizontal mais curta dando a volta no céu."""
    d = (x - cx) % W
    return np.where(d > W / 2, d - W, d)


def ceu():
    y = np.arange(H)[:, None].astype(np.float32)
    x = np.arange(W)[None, :].astype(np.float32)
    img = np.zeros((H, W, 3), np.float32)
    # Fundo: azul bem escuro, um pouco mais claro na faixa do meio.
    meio = np.exp(-((y - H * 0.52) / (H * 0.45)) ** 2)
    img += np.array([5, 7, 15], np.float32)
    img += meio[..., None] * np.array([8, 13, 32], np.float32)

    # Via Láctea: uma faixa ondulada que dá a volta inteira.
    centro = H * 0.56 + H * 0.12 * np.sin(2 * math.pi * x / W + 0.8)
    faixa = np.exp(-((y - centro) / (H * 0.11)) ** 2)
    nuvem = ruido(H, W, 1.25, 3)
    poeira = ruido(H, W, 1.4, 4)
    brilho = faixa * (0.35 + 0.9 * nuvem ** 2) * (1 - 0.75 * np.clip((poeira - 0.45) * 3, 0, 1) * faixa)
    img += brilho[..., None] * np.array([70, 80, 120], np.float32)

    # Nebulosas coloridas espalhadas (roxo, ciano, rosa), suaves.
    for cor, cx, cy, rx, ry, sem in [
        ((90, 50, 190), 0.12, 0.30, 0.10, 0.28, 5),
        ((30, 130, 200), 0.40, 0.72, 0.08, 0.22, 6),
        ((170, 50, 140), 0.66, 0.30, 0.09, 0.25, 7),
        ((60, 90, 220), 0.88, 0.65, 0.10, 0.24, 8),
    ]:
        d = (wrap_dx(x, cx * W) / (rx * W)) ** 2 + ((y - cy * H) / (ry * H)) ** 2
        n = ruido(H, W, 1.1, sem)
        img += (np.exp(-d) * (0.3 + 0.8 * n))[..., None] * np.array(cor, np.float32) * 0.55

    # Resto da supernova em volta do pulsar (o pulsar em si é desenhado no jogo, girando).
    px, py = 0.6 * W, 0.3 * H
    d = np.sqrt((wrap_dx(x, px) / 150) ** 2 + ((y - py) / 120) ** 2)
    # Casca irregular e fraca (fica atrás, só dá um ar de "algo explodiu aqui").
    fiapos = ruido(H, W, 0.9, 9)
    casca = np.exp(-((d - 1 - 0.35 * (fiapos - 0.5)) / 0.3) ** 2) * np.clip(fiapos * 1.6 - 0.35, 0, 1)
    img += casca[..., None] * np.array([50, 110, 160], np.float32) * 0.45
    img += np.exp(-d * d * 3)[..., None] * np.array([20, 45, 90], np.float32)

    # Estrelas: muitas pequenas e algumas fortes. Mais estrelas dentro da Via Láctea.
    estrelas = np.zeros((H, W), np.float32)
    n = 26000
    sx = rng.integers(0, W, n)
    sy = rng.integers(0, H, n)
    extra = rng.integers(0, W, 14000)
    ey = (H * 0.56 + H * 0.12 * np.sin(2 * math.pi * extra / W + 0.8) + rng.normal(0, H * 0.08, 14000)).astype(int) % H
    sx = np.concatenate([sx, extra])
    sy = np.concatenate([sy, ey])
    b = rng.random(sx.size) ** 4
    np.maximum.at(estrelas, (sy, sx), 0.25 + 0.75 * b)
    img_est = Image.fromarray((estrelas * 255).astype(np.uint8))
    # Brilho em volta de cada estrela: borrão que dá a volta (cola um pedaço de cada lado antes de borrar).
    larga = np.concatenate([estrelas[:, -16:], estrelas, estrelas[:, :16]], axis=1)
    halo = np.asarray(Image.fromarray((larga * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.6)), np.float32)[:, 16:-16] / 255
    tint = np.stack([np.ones_like(estrelas), 0.95 + 0.05 * ruido(H, W, 0.5, 10), 1.0 + 0.0 * estrelas], -1)
    img += (estrelas[..., None] * 230 + halo[..., None] * 500) * tint * np.array([0.95, 0.97, 1.0])
    del img_est

    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    andromeda(out, int(0.22 * W), int(0.36 * H))
    estrelas_fortes(out)
    return out.convert("RGB")


def andromeda(img, cx, cy):
    """Galáxia de Andrômeda: disco espiral bem inclinado, núcleo amarelado, braços azulados e faixas de poeira."""
    s = 900
    y, x = np.mgrid[0:s, 0:s].astype(np.float32)
    x = (x - s / 2) / (s / 2)
    y = (y - s / 2) / (s / 2)
    r = np.sqrt(x * x + y * y) + 1e-6
    ang = np.arctan2(y, x)
    disco = np.exp(-r / 0.28)
    nucleo = np.exp(-(r / 0.06) ** 2)
    bracos = 0.5 + 0.5 * np.cos(2 * (ang - np.log(r) * 2.6))
    poeira = np.clip(np.cos(2 * (ang - np.log(r) * 2.6) + 1.2), 0, 1) ** 3 * (r > 0.12) * (r < 0.8)
    textura = ruido(s, s, 0.8, 11)
    luz = disco * (0.55 + 0.6 * bracos * (0.6 + 0.6 * textura)) * (1 - 0.6 * poeira)
    rgb = np.zeros((s, s, 4), np.float32)
    azul = np.array([150, 185, 255], np.float32)
    amarelo = np.array([255, 225, 175], np.float32)
    mistura = np.clip(r / 0.35, 0, 1)[..., None]
    cor = amarelo * (1 - mistura) + azul * mistura
    rgb[..., :3] = cor * np.clip(luz * 1.4 + nucleo, 0, 1.2)[..., None]
    rgb[..., 3] = np.clip((luz * 1.6 + nucleo) * 255, 0, 255) * (r < 1)
    rgb[..., :3] = np.clip(rgb[..., :3], 0, 255)
    gal = Image.fromarray(rgb.astype(np.uint8), "RGBA")
    # Inclinada: achata e gira, como a foto de verdade (um charuto comprido).
    gal = gal.resize((s, int(s * 0.32)), Image.LANCZOS).rotate(32, expand=True, resample=Image.BICUBIC)
    gal = gal.resize((int(gal.width * 1.05), int(gal.height * 1.05)), Image.LANCZOS)
    glow = gal.filter(ImageFilter.GaussianBlur(22))
    img.alpha_composite(glow, (cx - glow.width // 2, cy - glow.height // 2))
    img.alpha_composite(gal, (cx - gal.width // 2, cy - gal.height // 2))
    # As duas galáxias anãs que acompanham Andrômeda.
    d = ImageDraw.Draw(img)
    for ox, oy, rr in [(110, -70, 12), (-210, 120, 17)]:
        camada = Image.new("RGBA", (80, 80), (0, 0, 0, 0))
        ImageDraw.Draw(camada).ellipse([40 - rr, 40 - rr * 0.7, 40 + rr, 40 + rr * 0.7], fill=(255, 235, 210, 170))
        camada = camada.filter(ImageFilter.GaussianBlur(rr * 0.45))
        img.alpha_composite(camada, (cx + ox - 40, cy + oy - 40))
    del d


def estrelas_fortes(img):
    """Algumas estrelas grandes com cruzinha de brilho (desenhadas dos dois lados quando encostam na borda)."""
    camada = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(camada)
    for _ in range(140):
        x, y = rng.uniform(0, W), rng.uniform(0, H)
        tint = [(255, 255, 255), (200, 225, 255), (255, 230, 200), (190, 240, 255)][rng.integers(0, 4)]
        s = rng.uniform(1.0, 2.2)
        for xx in (x - W, x, x + W):
            if -30 < xx < W + 30:
                d.ellipse([xx - s, y - s, xx + s, y + s], fill=tint + (255,))
                d.line([xx - s * 6, y, xx + s * 6, y], fill=tint + (90,))
                d.line([xx, y - s * 6, xx, y + s * 6], fill=tint + (90,))
    img.alpha_composite(camada.filter(ImageFilter.GaussianBlur(0.6)))
    img.alpha_composite(camada)


def esfera(tam, r, cor_fn, anel=None):
    """Planeta iluminado pela esquerda. cor_fn(lat, lon) devolve a cor da superfície (arrays)."""
    y, x = np.mgrid[0:tam, 0:tam].astype(np.float32)
    dx = (x + 0.5 - tam / 2) / r
    dy = (y + 0.5 - tam / 2) / r
    dd = dx * dx + dy * dy
    dentro = dd <= 1
    dz = np.sqrt(np.clip(1 - dd, 0, 1))
    lat = np.arcsin(np.clip(dy, -1, 1))
    lon = np.arctan2(dx, dz)
    cor = cor_fn(lat, lon)
    luz = np.clip(-dx * 0.55 - dy * 0.25 + dz * 0.8, 0.06, 1.15)[..., None]
    rgba = np.zeros((tam, tam, 4), np.float32)
    rgba[..., :3] = np.clip(cor * luz, 0, 255)
    borda = np.clip((1 - np.sqrt(dd)) * r * 1.2, 0, 1)
    rgba[..., 3] = 255 * borda * dentro
    # Atmosfera fininha na borda iluminada.
    atm = np.clip(1 - np.abs(np.sqrt(dd) - 1) * r / 2.5, 0, 1) * np.clip(-dx, 0, 1)
    rgba[..., :3] += atm[..., None] * cor.mean(axis=(0, 1)) * 0.5 if cor.ndim == 3 else 0
    rgba[..., 3] = np.maximum(rgba[..., 3], atm * 120)
    planeta = Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA")
    if not anel:
        return planeta
    base = Image.new("RGBA", (tam, tam), (0, 0, 0, 0))
    c = tam / 2
    tras = Image.new("RGBA", (tam, tam), (0, 0, 0, 0))
    ImageDraw.Draw(tras).ellipse([c - r * 2.0, c - r * 0.5, c + r * 2.0, c + r * 0.5], outline=anel + (170,), width=max(3, int(r * 0.22)))
    base.alpha_composite(tras)
    base.alpha_composite(planeta)
    frente = Image.new("RGBA", (tam, tam), (0, 0, 0, 0))
    ImageDraw.Draw(frente).arc([c - r * 2.0, c - r * 0.5, c + r * 2.0, c + r * 0.5], 0, 180, fill=anel + (210,), width=max(3, int(r * 0.22)))
    base.alpha_composite(frente)
    return base.rotate(-12, resample=Image.BICUBIC)


def planetas():
    # Desenha em 256 e reduz para 128: bordas e anel ficam lisos.
    tam = 256
    n = ruido(256, 256, 1.6, 21)

    def mapa(lat, lon, sem=0):
        # Lê o ruído como um mapa da superfície (latitude e longitude).
        iy = ((lat / math.pi + 0.5) * 255).astype(int) % 256
        ix = (((lon / (2 * math.pi)) + 0.5 + sem * 0.13) * 255).astype(int) % 256
        return n[iy, ix]

    def solido(c1, c2, sem):
        def f(lat, lon):
            t = mapa(lat, lon, sem)[..., None]
            return np.array(c1, np.float32) * (1 - t) + np.array(c2, np.float32) * t
        return f

    def faixas(cores, sem, freq=9):
        def f(lat, lon):
            t = (np.sin(lat * freq + mapa(lat, lon, sem) * 2.2) * 0.5 + 0.5)[..., None]
            return np.array(cores[0], np.float32) * (1 - t) + np.array(cores[1], np.float32) * t
        return f

    def terra(lat, lon):
        t = mapa(lat, lon, 4)
        mar = np.array([30, 80, 190], np.float32)
        chao = np.array([70, 150, 80], np.float32)
        gelo = np.array([235, 240, 250], np.float32)
        terra_ = (t > 0.55)[..., None]
        cor = np.where(terra_, chao, mar)
        polo = (np.abs(lat) > 1.2)[..., None]
        nuvem = (mapa(lat * 1.3, lon * 2, 7) > 0.72)[..., None]
        return np.where(polo | nuvem, gelo, cor)

    lista = [
        esfera(tam, 80, solido((150, 140, 135), (95, 90, 90), 1)),                    # Mercúrio
        esfera(tam, 108, faixas([(230, 200, 140), (250, 225, 175)], 2, 4)),             # Vênus
        esfera(tam, 112, terra),                                                         # Terra
        esfera(tam, 92, solido((200, 90, 50), (140, 60, 40), 3)),                       # Marte
        esfera(tam, 124, faixas([(205, 160, 120), (240, 220, 190)], 5, 14)),            # Júpiter
        esfera(tam, 60, faixas([(225, 200, 140), (245, 230, 185)], 6, 10), anel=(225, 205, 160)),  # Saturno
        esfera(tam, 104, solido((150, 220, 230), (120, 200, 215), 8)),                   # Urano
        esfera(tam, 104, faixas([(50, 90, 220), (80, 130, 245)], 9, 6)),                 # Netuno
    ]
    atlas = Image.new("RGBA", (128 * 8, 128), (0, 0, 0, 0))
    for i, p in enumerate(lista):
        atlas.alpha_composite(p.resize((128, 128), Image.LANCZOS), (i * 128, 0))
    return atlas


def pulsar():
    """Núcleo (esquerda, 128x128) e feixe (direita, 64x256, apontando para cima e para baixo a partir do meio)."""
    atlas = np.zeros((256, 256, 4), np.float32)
    y, x = np.mgrid[0:128, 0:128].astype(np.float32)
    r = np.hypot(x + 0.5 - 64, y + 0.5 - 64) / 64
    brilho = np.exp(-(r / 0.12) ** 2) + 0.55 * np.exp(-(r / 0.35) ** 2) + 0.18 * np.exp(-(r / 0.8) ** 2)
    branco = np.exp(-(r / 0.07) ** 2)
    nuc = np.zeros((128, 128, 4), np.float32)
    nuc[..., 0] = 127 + 128 * branco
    nuc[..., 1] = 231 + 24 * branco
    nuc[..., 2] = 255
    nuc[..., 3] = np.clip(brilho * 255, 0, 255) * (r < 1)
    atlas[0:128, 0:128] = nuc
    y, x = np.mgrid[0:256, 0:64].astype(np.float32)
    t = np.abs(y + 0.5 - 128) / 128
    larg = 0.06 + 0.5 * t
    lado = np.abs(x + 0.5 - 32) / 32 / larg
    feixe = np.exp(-lado ** 2 * 2.5) * (1 - t) ** 1.5 * (t > 0.02)
    fx = np.zeros((256, 64, 4), np.float32)
    fx[..., 0] = 140 + 100 * np.exp(-lado ** 2 * 10)
    fx[..., 1] = 235
    fx[..., 2] = 255
    fx[..., 3] = np.clip(feixe * 255, 0, 255)
    atlas[:, 128:192] = fx
    return Image.fromarray(atlas.astype(np.uint8), "RGBA")


def main():
    os.makedirs(MOD_TEX, exist_ok=True)
    ceu().save(os.path.join(MOD_TEX, "ceu.png"), optimize=True)
    planetas().save(os.path.join(MOD_TEX, "planetas.png"), optimize=True)
    pulsar().save(os.path.join(MOD_TEX, "pulsar.png"), optimize=True)
    print("ok")


if __name__ == "__main__":
    main()
