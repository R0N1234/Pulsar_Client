"""Gera a arte espacial do Pulsar Client: fundo do menu, logo "PC" e a imagem da tela de carregamento.

Uso: python3 tools/arte_espaco.py
As imagens saem prontas nas pastas do mod e do launcher. A semente é fixa, então o resultado é sempre igual.
"""
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD_TEX = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "pulsar", "textures", "gui")
SPLASH = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "pulsar", "textures", "splash")
FONT = "/usr/share/fonts/opentype/inter/InterDisplay-Bold.otf"

BG_EDGE = (5, 7, 15)
BG_MID = (12, 20, 46)
BEAM = (127, 231, 255)


def gradient(w, h, cx, cy, inner, outer):
    img = Image.new("RGB", (w, h), outer)
    px = img.load()
    rmax = math.hypot(max(cx, w - cx), max(cy, h - cy))
    for y in range(h):
        for x in range(w):
            t = min(1.0, math.hypot(x - cx, y - cy) / rmax) ** 0.8
            px[x, y] = tuple(int(inner[i] + (outer[i] - inner[i]) * t) for i in range(3))
    return img


def glow_layer(size, draw_fn, blur):
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def add_galaxy(img, rnd, cx, cy, radius, color, tilt):
    """Galáxia espiral simples: núcleo brilhante e braços feitos de pontos borrados."""
    w, h = img.size
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for arm in range(2):
        for i in range(260):
            t = i / 260
            ang = arm * math.pi + t * 3.2 * math.pi
            r = t * radius
            x = cx + math.cos(ang) * r
            y = cy + math.sin(ang) * r * tilt
            x += rnd.gauss(0, radius * 0.05)
            y += rnd.gauss(0, radius * 0.05 * tilt)
            a = int(150 * (1 - t))
            s = 1.5 + 2 * (1 - t)
            d.ellipse([x - s, y - s, x + s, y + s], fill=color + (a,))
    d.ellipse([cx - radius * 0.18, cy - radius * 0.18 * tilt, cx + radius * 0.18, cy + radius * 0.18 * tilt], fill=(255, 240, 230, 200))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(radius * 0.04)))
    img.alpha_composite(glow_layer((w, h), lambda g: g.ellipse(
        [cx - radius * 0.6, cy - radius * 0.6 * tilt, cx + radius * 0.6, cy + radius * 0.6 * tilt], fill=color + (60,)), radius * 0.25))


def add_planet(img, cx, cy, r, base, ring=None):
    """Planeta com luz vindo da esquerda e anel opcional."""
    w, h = img.size
    size = int(r * 2 + 4)
    body = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = body.load()
    for y in range(size):
        for x in range(size):
            dx = (x - size / 2) / r
            dy = (y - size / 2) / r
            dd = dx * dx + dy * dy
            if dd > 1:
                continue
            dz = math.sqrt(1 - dd)
            light = max(0.08, (-dx * 0.6 - dy * 0.35 + dz * 0.7))
            band = 0.9 + 0.1 * math.sin(dy * 9)
            edge = min(1.0, (1 - dd) * 12)
            px[x, y] = tuple(min(255, int(c * light * band)) for c in base) + (int(255 * edge),)
    if ring:
        ring_back = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        rd = ImageDraw.Draw(ring_back)
        rd.ellipse([cx - r * 2.0, cy - r * 0.45, cx + r * 2.0, cy + r * 0.45], outline=ring + (150,), width=max(2, int(r * 0.12)))
        img.alpha_composite(ring_back)
    img.alpha_composite(glow_layer((w, h), lambda g: g.ellipse([cx - r * 1.15, cy - r * 1.15, cx + r * 1.15, cy + r * 1.15], fill=base + (45,)), r * 0.3))
    img.alpha_composite(body, (int(cx - size / 2), int(cy - size / 2)))
    if ring:
        # Metade da frente do anel passa por cima do planeta.
        front = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        fd = ImageDraw.Draw(front)
        fd.arc([cx - r * 2.0, cy - r * 0.45, cx + r * 2.0, cy + r * 0.45], 0, 180, fill=ring + (190,), width=max(2, int(r * 0.12)))
        img.alpha_composite(front)


def add_stars(img, rnd, count, fade_edges=False):
    w, h = img.size
    d = ImageDraw.Draw(img)
    for _ in range(count):
        x = rnd.uniform(0, w)
        y = rnd.uniform(0, h)
        b = rnd.random() ** 3
        f = 1.0
        if fade_edges:
            f = min(1.0, min(x, y, w - x, h - y) / (w * 0.18))
        a = int((60 + 195 * b) * f)
        tint = rnd.choice([(255, 255, 255), (200, 225, 255), (255, 235, 210), (190, 240, 255)])
        s = 0.6 + 1.4 * b
        d.ellipse([x - s, y - s, x + s, y + s], fill=tint + (a,))
        if b > 0.75:
            d.line([x - s * 4, y, x + s * 4, y], fill=tint + (a // 3,))
            d.line([x, y - s * 4, x, y + s * 4], fill=tint + (a // 3,))


def space(w, h, seed, fade_edges=False, center=None):
    rnd = random.Random(seed)
    cx, cy = center or (w * 0.5, h * 0.45)
    img = gradient(w, h, cx, cy, BG_MID, BG_EDGE).convert("RGBA")
    # Nebulosas bem suaves dão profundidade sem custar nada no jogo (é tudo uma imagem só).
    for color, nx, ny, nr in [((90, 60, 200), 0.2, 0.3, 0.35), ((30, 140, 200), 0.82, 0.7, 0.3), ((160, 50, 140), 0.7, 0.15, 0.22)]:
        img.alpha_composite(glow_layer((w, h), lambda g, c=color, x=nx, y=ny, r=nr: g.ellipse(
            [w * x - w * r, h * y - h * r, w * x + w * r, h * y + h * r], fill=c + (38,)), w * 0.08))
    add_stars(img, rnd, int(w * h / 1300), fade_edges)
    return img, rnd


def emblem(size):
    """Logo do Pulsar Client: "PC" dentro de um círculo, com os dois feixes do pulsar."""
    s = size
    c = s / 2
    r = s * 0.30
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    # Feixes do pulsar, saindo do círculo para cima e para baixo.
    beams = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    bd = ImageDraw.Draw(beams)
    for i in range(int(s * 0.2)):
        t = i / (s * 0.2)
        a = int(200 * (1 - t))
        wdt = s * 0.012 * (1 - t * 0.6)
        bd.rectangle([c - wdt, c - r - i, c + wdt, c - r - i + 1], fill=BEAM + (a,))
        bd.rectangle([c - wdt, c + r + i - 1, c + wdt, c + r + i], fill=BEAM + (a,))
    img.alpha_composite(beams.filter(ImageFilter.GaussianBlur(s * 0.006)))
    img.alpha_composite(glow_layer((s, s), lambda g: g.ellipse([c - r * 1.12, c - r * 1.12, c + r * 1.12, c + r * 1.12], outline=BEAM + (170,), width=int(s * 0.04)), s * 0.03))
    d = ImageDraw.Draw(img)
    d.ellipse([c - r, c - r, c + r, c + r], fill=(8, 14, 32, 235), outline=BEAM + (255,), width=max(2, int(s * 0.018)))
    d.ellipse([c - r * 0.88, c - r * 0.88, c + r * 0.88, c + r * 0.88], outline=BEAM + (70,), width=max(1, int(s * 0.006)))
    font = ImageFont.truetype(FONT, int(s * 0.25))
    text = "PC"
    box = d.textbbox((0, 0), text, font=font)
    tx = c - (box[0] + box[2]) / 2
    ty = c - (box[1] + box[3]) / 2
    img.alpha_composite(glow_layer((s, s), lambda g: g.text((tx, ty), text, font=font, fill=BEAM + (200,)), s * 0.02))
    ImageDraw.Draw(img).text((tx, ty), text, font=font, fill=(255, 255, 255, 255))
    return img


def menu_background():
    w, h = 1920, 1080
    img, rnd = space(w, h, 7)
    add_galaxy(img, rnd, w * 0.16, h * 0.22, 150, (150, 120, 255), 0.45)
    add_galaxy(img, rnd, w * 0.86, h * 0.14, 80, (110, 210, 255), 0.6)
    add_galaxy(img, rnd, w * 0.72, h * 0.86, 60, (255, 150, 200), 0.5)
    add_planet(img, w * 0.85, h * 0.62, 95, (230, 140, 80), ring=(240, 210, 170))
    add_planet(img, w * 0.12, h * 0.8, 46, (90, 150, 255))
    add_planet(img, w * 0.6, h * 0.1, 18, (180, 180, 200))
    return img.convert("RGB")


def splash_background():
    """Fundo da tela de carregamento. Menor que o do menu, porque fica na memória de vídeo durante o jogo todo."""
    w, h = 1280, 720
    img, rnd = space(w, h, 11, center=(w * 0.5, h * 0.44))
    add_galaxy(img, rnd, w * 0.15, h * 0.2, 100, (150, 120, 255), 0.45)
    add_galaxy(img, rnd, w * 0.88, h * 0.12, 50, (110, 210, 255), 0.6)
    add_planet(img, w * 0.86, h * 0.7, 58, (230, 140, 80), ring=(240, 210, 170))
    add_planet(img, w * 0.1, h * 0.78, 28, (90, 150, 255))
    return img.convert("RGB")


def main():
    os.makedirs(MOD_TEX, exist_ok=True)
    os.makedirs(SPLASH, exist_ok=True)
    menu_background().save(os.path.join(MOD_TEX, "fundo.png"), optimize=True)
    emblem(256).save(os.path.join(MOD_TEX, "logo.png"), optimize=True)
    splash_background().save(os.path.join(SPLASH, "fundo.png"), optimize=True)
    emblem(512).save(os.path.join(SPLASH, "logo.png"), optimize=True)
    print("ok")


if __name__ == "__main__":
    main()
