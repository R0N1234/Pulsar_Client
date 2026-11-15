"""Logo do Pulsar Client parado (o mesmo do menu do jogo: anel, órbitas com luas e o "PC" do Rick).

Uso: python3 tools/logo_icone.py
Gera:
  launcher/build/icon.ico e icon.png  ícone da área de trabalho, da barra de tarefas e do instalador
  launcher/src/logo.png               logo dentro do launcher (janela e telas)
  mod/.../textures/splash/logo.png    carregamento do Minecraft (e o anel da tela de atualização)
  mod/.../textures/gui/logo.png       logo dentro do jogo
  mod/.../textures/gui/icone.png      ícone pequeno ao lado do nome de quem usa o Pulsar
  mod/.../textures/gui/janela16.png e janela32.png  ícone da janela do Minecraft
Nos tamanhos pequenos (até 48 px) as órbitas saem e o anel ocupa o quadrado todo, para o PC continuar legível.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

import logo_menu as L

ROOT = L.ROOT
LAUNCHER_BUILD = os.path.join(ROOT, "launcher", "build")
LAUNCHER_SRC = os.path.join(ROOT, "launcher", "src")
TEX = os.path.join(ROOT, "mod", "src", "main", "resources", "assets", "pulsar", "textures")


def luas(img, orbitas):
    """Duas luas brilhando nas órbitas, como no menu animado."""
    s = L.SS
    camada = L.grande()
    d = ImageDraw.Draw(camada)
    for (a, b, inc), t in zip(orbitas, (math.radians(200), math.radians(25))):
        cs, sn = math.cos(math.radians(inc)), math.sin(math.radians(inc))
        x, y = a * math.cos(t), b * math.sin(t)
        px, py = (L.C + x * cs - y * sn) * s, (L.C + x * sn + y * cs) * s
        for raio, cor in ((16, (127, 231, 255, 90)), (9, (180, 240, 255, 170)), (4.5, (255, 255, 255, 255))):
            d.ellipse([px - raio * s, py - raio * s, px + raio * s, py + raio * s], fill=cor)
    camada = camada.filter(ImageFilter.GaussianBlur(2 * s))
    img.alpha_composite(L.reduzir(camada))


def completo(raio, orbitas):
    """Anel, nebulosa, órbitas, luas e letras num quadrado 512x512 transparente."""
    L.RAIO, L.ORBITAS = raio, orbitas
    grad = L.gradiente_diagonal()
    img = L.anel(grad)
    img.alpha_composite(L.vortice())
    img.alpha_composite(L.orbitas(grad))
    luas(img, orbitas)
    img.alpha_composite(L.letras())
    return img


def main():
    os.makedirs(LAUNCHER_BUILD, exist_ok=True)
    # Grande: anel com 68% da largura (o mesmo do logo antigo, a tela de atualização conta com isso) e órbitas até a borda.
    grande = completo(174, [(248, 78, 24.0), (240, 70, -22.0)])
    # Pequeno: só o anel e o PC, ocupando quase tudo.
    pequeno = completo(232, [])

    grande.save(os.path.join(LAUNCHER_BUILD, "icon.png"), optimize=True)
    grande.save(os.path.join(LAUNCHER_SRC, "logo.png"), optimize=True)
    grande.save(os.path.join(TEX, "splash", "logo.png"), optimize=True)
    grande.resize((256, 256), Image.LANCZOS).save(os.path.join(TEX, "gui", "logo.png"), optimize=True)
    pequeno.resize((32, 32), Image.LANCZOS).save(os.path.join(TEX, "gui", "icone.png"), optimize=True)
    pequeno.resize((16, 16), Image.LANCZOS).save(os.path.join(TEX, "gui", "janela16.png"), optimize=True)
    pequeno.resize((32, 32), Image.LANCZOS).save(os.path.join(TEX, "gui", "janela32.png"), optimize=True)

    sizes = [16, 20, 24, 32, 40, 48, 64, 128, 256]
    frames = [(pequeno if n <= 48 else grande).resize((n, n), Image.LANCZOS) for n in sizes]
    frames[-1].save(os.path.join(LAUNCHER_BUILD, "icon.ico"), sizes=[(n, n) for n in sizes], append_images=frames[:-1])
    print("logo novo gerado")


if __name__ == "__main__":
    main()
