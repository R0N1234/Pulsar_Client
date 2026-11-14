package dev.pulsarclient.core;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.ProgressManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Tela de carregamento do Pulsar: universo de fundo, "PC" no anel ciano -> violeta e uma barra em pílula embaixo.
 * Roda na thread de carregamento do Forge, com o OpenGL dela. São só três imagens e alguns retângulos.
 */
public final class PulsarSplash {
    private static boolean failed;
    private static boolean ready;
    private static int background;
    private static int backgroundW;
    private static int backgroundH;
    private static int logo;
    private static FontRenderer font;
    private static float shown;

    private PulsarSplash() {}

    /** Chamado a cada quadro. true = o Pulsar desenhou e o Forge não precisa desenhar nada. */
    public static boolean draw() {
        if (failed) return false;
        try {
            if (!ready) init();
            render();
            return true;
        } catch (Throwable t) {
            failed = true;
            System.err.println("[Pulsar] Tela de carregamento do Pulsar falhou, voltando para a do Forge: " + t);
            return false;
        }
    }

    private static void init() throws Exception {
        BufferedImage bg = image("/assets/pulsar/textures/splash/fundo.png");
        background = upload(bg);
        backgroundW = bg.getWidth();
        backgroundH = bg.getHeight();
        logo = upload(image("/assets/pulsar/textures/splash/logo.png"));
        try {
            Field f = SplashProgress.class.getDeclaredField("fontRenderer");
            f.setAccessible(true);
            font = (FontRenderer) f.get(null);
        } catch (Throwable t) {
            font = null;
        }
        ready = true;
    }

    private static void render() {
        int w = Display.getWidth();
        int h = Display.getHeight();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, w, h, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Fundo cobrindo a janela toda, sem esticar a imagem.
        float scale = Math.max((float) w / backgroundW, (float) h / backgroundH);
        float bw = backgroundW * scale;
        float bh = backgroundH * scale;
        image(background, (w - bw) / 2, (h - bh) / 2, bw, bh);

        float size = Math.min(w, h) * 0.46f;
        float cx = w / 2f;
        float cy = h * 0.40f;
        // O logo respira devagar, para mostrar que o jogo não travou.
        float pulso = 1f + 0.015f * (float) Math.sin(System.currentTimeMillis() / 700.0);
        float ls = size * pulso;
        image(logo, cx - ls / 2, cy - ls / 2, ls, ls);

        float target = progress();
        shown = Math.max(shown, shown + (target - shown) * 0.12f);
        // Barra em pílula: trilho escuro com contorno, enchimento ciano -> violeta com brilho.
        float barW = Math.min(w * 0.40f, h * 0.72f);
        float barH = Math.max(6f, h / 72f);
        float bx = cx - barW / 2;
        float by = cy + size * 0.56f;
        pilula(bx - 1.5f, by - 1.5f, barW + 3, barH + 3, 0x8A93D8, 0x8A93D8, 0.55f);
        pilula(bx, by, barW, barH, 0x0B1030, 0x0B1030, 0.95f);
        float fw = Math.max(barH, barW * shown);
        if (shown > 0.005f) {
            pilula(bx - barH * 0.4f, by - barH * 0.4f, fw + barH * 0.8f, barH * 1.8f, 0x35D6F5, 0x9B6BFF, 0.18f);
            pilula(bx, by, fw, barH, 0x35D6F5, mistura(0x35D6F5, 0x9B6BFF, shown), 1f);
            pilula(bx + barH * 0.3f, by + barH * 0.18f, Math.max(0, fw - barH * 0.6f), barH * 0.28f, 0xFFFFFF, 0xFFFFFF, 0.28f);
        }

        if (font != null) {
            String text = "Carregando o Minecraft  \u00b7  " + Math.round(shown * 100) + "%";
            // Letra grande o bastante para ler de longe (TV): uns 3,5% da altura da tela.
            float s = Math.max(2f, h / 270f * 1.25f);
            float tw = font.getStringWidth(text) * s;
            GL11.glPushMatrix();
            GL11.glTranslatef(cx - tw / 2f, by + barH + h * 0.035f, 0);
            GL11.glScalef(s, s, 1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            font.drawString(text, 1, 1, 0x101530);
            font.drawString(text, 0, 0, 0xF2F5FF);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    /** Retângulo de pontas redondas (raio = metade da altura), com a cor indo da esquerda para a direita. */
    private static void pilula(float x, float y, float w, float h, int rgbEsq, int rgbDir, float alpha) {
        if (w <= 0 || h <= 0) return;
        float r = Math.min(h / 2f, w / 2f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        cor(mistura(rgbEsq, rgbDir, 0.5f), alpha);
        GL11.glVertex2f(x + w / 2, y + h / 2);
        int n = 16;
        // Ponta direita (de cima para baixo) e depois a esquerda (de baixo para cima).
        for (int lado = 0; lado < 2; lado++) {
            float ccx = lado == 0 ? x + w - r : x + r;
            for (int i = 0; i <= n; i++) {
                double a = -Math.PI / 2 + Math.PI * i / n + (lado == 0 ? 0 : Math.PI);
                float px = ccx + (float) Math.cos(a) * r;
                float py = y + h / 2 + (float) Math.sin(a) * r;
                cor(mistura(rgbEsq, rgbDir, (px - x) / w), alpha);
                GL11.glVertex2f(px, py);
            }
        }
        cor(mistura(rgbEsq, rgbDir, (x + w - r - x) / w), alpha);
        GL11.glVertex2f(x + w - r, y);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
    }

    private static void cor(int rgb, float alpha) {
        GL11.glColor4f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
    }

    private static int mistura(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = 0;
        for (int sh = 0; sh < 24; sh += 8) {
            int ca = a >> sh & 255, cb = b >> sh & 255;
            r |= Math.round(ca + (cb - ca) * t) << sh;
        }
        return r;
    }

    /** Fração carregada: a barra principal do Forge mais o pedaço da barra de dentro dela. */
    private static float progress() {
        Iterator<ProgressManager.ProgressBar> it = ProgressManager.barIterator();
        if (!it.hasNext()) return shown;
        ProgressManager.ProgressBar main = it.next();
        if (main.getSteps() <= 0) return shown;
        float f = (float) main.getStep() / main.getSteps();
        if (it.hasNext()) {
            ProgressManager.ProgressBar sub = it.next();
            if (sub.getSteps() > 0) f += (float) sub.getStep() / sub.getSteps() / main.getSteps();
        }
        return Math.min(1f, f);
    }

    private static void image(int texture, float x, float y, float w, float h) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y + h);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static BufferedImage image(String path) throws Exception {
        InputStream in = PulsarSplash.class.getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("imagem não encontrada: " + path);
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }

    private static int upload(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int p : px) buf.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
        buf.flip();
        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        return id;
    }
}
