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
LAUNCHER_BUILD = os.path.join(ROOT, "launcher", "build")
LAUNCHER_SRC = os.path.join(ROOT, "launcher", "src")
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


def diagonal(s, a, b):
    """Imagem s x s com a cor indo de a (em cima à esquerda) para b (embaixo à direita)."""
    g = Image.new("RGB", (2, 2))
    g.putpixel((0, 0), a)
    g.putpixel((1, 1), b)
    mid = tuple((a[i] + b[i]) // 2 for i in range(3))
    g.putpixel((1, 0), mid)
    g.putpixel((0, 1), mid)
    return g.resize((s, s), Image.BILINEAR)


def emblem(size, beams_out=True, anel_gradiente=False):
    """Logo do Pulsar Client: "PC" na frente de um pulsar (estrela de nêutrons com os dois feixes de luz).
    anel_gradiente: o anel vai do ciano ao violeta, com brilho mais forte (tela de carregamento)."""
    s = size
    c = s / 2
    r = s * 0.34
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    angle = math.radians(-32)
    reach = s * (0.5 if beams_out else 0.36)

    def beam_layer(alpha, half_width, length):
        layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        for sign in (1, -1):
            # Cone saindo do centro: estreito no núcleo, mais largo e mais fraco na ponta.
            steps = 40
            for i in range(steps):
                t0, t1 = i / steps, (i + 1) / steps
                a = int(alpha * (1 - t0) ** 1.4)
                w0, w1 = half_width * (0.25 + t0), half_width * (0.25 + t1)
                pts = []
                for t, w in ((t0, w0), (t1, w1), (t1, -w1), (t0, -w0)):
                    dist = t * length * sign
                    px = c + math.cos(angle) * dist - math.sin(angle) * w
                    py = c + math.sin(angle) * dist + math.cos(angle) * w
                    pts.append((px, py))
                d.polygon(pts, fill=BEAM + (a,))
        return layer

    # Feixes por fora do círculo (o brilho que "vaza") e por dentro.
    if beams_out:
        img.alpha_composite(beam_layer(200, s * 0.06, reach).filter(ImageFilter.GaussianBlur(s * 0.01)))
    if anel_gradiente:
        cores = diagonal(s, (64, 224, 255), (168, 92, 255)).convert("RGBA")
        brilho = Image.new("L", (s, s), 0)
        ImageDraw.Draw(brilho).ellipse([c - r * 1.04, c - r * 1.04, c + r * 1.04, c + r * 1.04], outline=255, width=int(s * 0.05))
        cores.putalpha(brilho.filter(ImageFilter.GaussianBlur(s * 0.035)).point(lambda v: int(v * 0.95)))
        img.alpha_composite(cores)
    else:
        img.alpha_composite(glow_layer((s, s), lambda g: g.ellipse([c - r * 1.1, c - r * 1.1, c + r * 1.1, c + r * 1.1], outline=BEAM + (150,), width=int(s * 0.035)), s * 0.03))

    disc = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    dd = ImageDraw.Draw(disc)
    dd.ellipse([c - r, c - r, c + r, c + r], fill=(7, 12, 30, 245))
    # Dentro do círculo: campo magnético, feixes fortes e o núcleo brilhante.
    field = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    fd = ImageDraw.Draw(field)
    for k in (0.55, 0.8):
        fd.ellipse([c - r * k, c - r * k * 0.42, c + r * k, c + r * k * 0.42], outline=(165, 135, 255, 150), width=max(1, int(s * 0.006)))
    field = field.rotate(math.degrees(-angle) + 90, center=(c, c))
    disc.alpha_composite(field)
    disc.alpha_composite(beam_layer(255, s * 0.075, r * 1.05).filter(ImageFilter.GaussianBlur(s * 0.006)))
    disc.alpha_composite(beam_layer(255, s * 0.03, r).filter(ImageFilter.GaussianBlur(s * 0.003)))
    disc.alpha_composite(glow_layer((s, s), lambda g: g.ellipse([c - r * 0.5, c - r * 0.5, c + r * 0.5, c + r * 0.5], fill=(127, 231, 255, 210)), s * 0.045))
    disc.alpha_composite(glow_layer((s, s), lambda g: g.ellipse([c - r * 0.2, c - r * 0.2, c + r * 0.2, c + r * 0.2], fill=(255, 255, 255, 255)), s * 0.015))
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).ellipse([c - r, c - r, c + r, c + r], fill=255)
    clipped = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    clipped.paste(disc, (0, 0), mask)
    img.alpha_composite(clipped)
    d = ImageDraw.Draw(img)
    if anel_gradiente:
        linha = Image.new("L", (s, s), 0)
        ImageDraw.Draw(linha).ellipse([c - r, c - r, c + r, c + r], outline=255, width=max(2, int(s * 0.018)))
        cores = diagonal(s, (64, 224, 255), (168, 92, 255)).convert("RGBA")
        cores.putalpha(linha)
        img.alpha_composite(cores)
    else:
        d.ellipse([c - r, c - r, c + r, c + r], outline=BEAM + (255,), width=max(2, int(s * 0.02)))

    font = ImageFont.truetype(FONT, int(s * 0.27))
    text = "PC"
    box = d.textbbox((0, 0), text, font=font)
    tx = c - (box[0] + box[2]) / 2
    ty = c - (box[1] + box[3]) / 2
    # Sombra escura para o "PC" ler bem por cima do núcleo claro.
    img.alpha_composite(glow_layer((s, s), lambda g: g.text((tx, ty), text, font=font, fill=(2, 6, 18, 230)), s * 0.03))
    img.alpha_composite(glow_layer((s, s), lambda g: g.text((tx, ty), text, font=font, fill=BEAM + (160,)), s * 0.015))
    ImageDraw.Draw(img).text((tx, ty), text, font=font, fill=(255, 255, 255, 255))
    return img


def app_emblem(s):
    """Ícone de aplicativo: o mesmo pulsar com "PC", mas ocupando o quadro todo, como os ícones do Discord e do Chrome."""
    c = s / 2
    r = s * 0.475
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    angle = math.radians(-32)

    disc = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    dd = ImageDraw.Draw(disc)
    # Fundo do disco: azul do espaço, mais claro no meio.
    for i in range(48, 0, -1):
        k = i / 48
        col = tuple(int(BG_EDGE[j] + (BG_MID[j] + 18 - BG_EDGE[j]) * (1 - k) ** 1.5) for j in range(3))
        dd.ellipse([c - r * k, c - r * k, c + r * k, c + r * k], fill=col + (255,))
    field = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    fd = ImageDraw.Draw(field)
    for k in (0.5, 0.78):
        fd.ellipse([c - r * k, c - r * k * 0.4, c + r * k, c + r * k * 0.4], outline=(165, 135, 255, 140), width=max(1, int(s * 0.007)))
    field = field.rotate(math.degrees(-angle) + 90, center=(c, c))
    disc.alpha_composite(field)

    def beam_layer(alpha, half_width, length):
        layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        for sign in (1, -1):
            steps = 40
            for i in range(steps):
                t0, t1 = i / steps, (i + 1) / steps
                a = int(alpha * (1 - t0) ** 1.2)
                w0, w1 = half_width * (0.25 + t0), half_width * (0.25 + t1)
                pts = []
                for t, w in ((t0, w0), (t1, w1), (t1, -w1), (t0, -w0)):
                    dist = t * length * sign
                    pts.append((c + math.cos(angle) * dist - math.sin(angle) * w, c + math.sin(angle) * dist + math.cos(angle) * w))
                d.polygon(pts, fill=BEAM + (a,))
        return layer

    disc.alpha_composite(beam_layer(230, s * 0.08, r * 1.02).filter(ImageFilter.GaussianBlur(s * 0.008)))
    disc.alpha_composite(beam_layer(255, s * 0.03, r).filter(ImageFilter.GaussianBlur(s * 0.003)))
    disc.alpha_composite(glow_layer((s, s), lambda g: g.ellipse([c - r * 0.42, c - r * 0.42, c + r * 0.42, c + r * 0.42], fill=(127, 231, 255, 190)), s * 0.05))
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).ellipse([c - r, c - r, c + r, c + r], fill=255)
    img.paste(disc, (0, 0), mask)
    ImageDraw.Draw(img).ellipse([c - r, c - r, c + r, c + r], outline=BEAM + (255,), width=max(2, int(s * 0.03)))

    font = ImageFont.truetype(FONT.replace("Bold", "Black"), int(s * 0.42))
    text = "PC"
    d = ImageDraw.Draw(img)
    box = d.textbbox((0, 0), text, font=font)
    tx = c - (box[0] + box[2]) / 2
    ty = c - (box[1] + box[3]) / 2
    # Sombra escura em volta das letras: o "PC" continua legível em 16 px, por cima do brilho do núcleo.
    img.alpha_composite(glow_layer((s, s), lambda g: g.text((tx, ty), text, font=font, fill=(2, 6, 18, 255)), s * 0.035))
    img.alpha_composite(glow_layer((s, s), lambda g: g.text((tx, ty), text, font=font, fill=(2, 6, 18, 200)), s * 0.012))
    ImageDraw.Draw(img).text((tx, ty), text, font=font, fill=(255, 255, 255, 255))
    return img


def app_icons():
    """Ícone do launcher, do instalador e da tela de atualização. O jogo usa o ícone do próprio Minecraft."""
    big = app_emblem(1024)
    os.makedirs(LAUNCHER_BUILD, exist_ok=True)
    sizes = [16, 20, 24, 32, 40, 48, 64, 128, 256]
    frames = [big.resize((n, n), Image.LANCZOS) for n in sizes]
    frames[-1].save(os.path.join(LAUNCHER_BUILD, "icon.ico"), sizes=[(n, n) for n in sizes], append_images=frames[:-1])
    big.resize((512, 512), Image.LANCZOS).save(os.path.join(LAUNCHER_BUILD, "icon.png"), optimize=True)
    big.resize((512, 512), Image.LANCZOS).save(os.path.join(LAUNCHER_SRC, "logo.png"), optimize=True)


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
    # Nuvens violeta e azuis nas bordas, como no modelo do Rick, deixando o meio limpo para o logo e a barra.
    nuvens = random.Random(23)
    for _ in range(26):
        x = nuvens.choice([nuvens.uniform(0, w * 0.32), nuvens.uniform(w * 0.68, w)])
        y = nuvens.uniform(0, h)
        rx, ry = nuvens.uniform(60, 220), nuvens.uniform(30, 90)
        cor = nuvens.choice([(120, 60, 220), (90, 50, 190), (50, 90, 210), (150, 70, 210)])
        img.alpha_composite(glow_layer((w, h), lambda g, x=x, y=y, rx=rx, ry=ry, cor=cor: g.ellipse(
            [x - rx, y - ry, x + rx, y + ry], fill=cor + (nuvens.randint(18, 34),)), 40))
    # Estrelinhas em cruz, violeta e brancas.
    d = ImageDraw.Draw(img)
    for _ in range(14):
        x, y = nuvens.uniform(0, w), nuvens.uniform(0, h)
        if abs(x - w / 2) < w * 0.2 and abs(y - h * 0.45) < h * 0.3:
            continue
        cor = nuvens.choice([(190, 160, 255), (230, 230, 255)])
        k = nuvens.uniform(3, 6)
        d.line([x - k, y, x + k, y], fill=cor + (200,), width=2)
        d.line([x, y - k, x, y + k], fill=cor + (200,), width=2)
    return img.convert("RGB")


def main():
    os.makedirs(MOD_TEX, exist_ok=True)
    os.makedirs(SPLASH, exist_ok=True)
    menu_background().save(os.path.join(MOD_TEX, "fundo.png"), optimize=True)
    splash_background().save(os.path.join(SPLASH, "fundo.png"), optimize=True)
    # Os logos e ícones (launcher, instalador, carregamento e jogo) saem de tools/logo_icone.py.
    print("ok")


if __name__ == "__main__":
    main()
