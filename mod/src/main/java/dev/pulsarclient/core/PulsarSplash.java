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
 * Tela de carregamento do Pulsar: universo de fundo, "PC" no círculo e uma barra embaixo.
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
        float cy = h * 0.42f;
        image(logo, cx - size / 2, cy - size / 2, size, size);

        float target = progress();
        shown = Math.max(shown, shown + (target - shown) * 0.12f);
        float barW = Math.min(w * 0.36f, 460f);
        float barH = Math.max(4f, h / 150f);
        float bx = cx - barW / 2;
        float by = cy + size * 0.52f;
        rect(bx - 1, by - 1, barW + 2, barH + 2, 0x7FE7FF, 0.45f);
        rect(bx, by, barW, barH, 0x0A0F1E, 0.95f);
        rect(bx, by, barW * shown, barH, 0x7FE7FF, 1f);

        if (font != null) {
            String text = "Carregando o Minecraft   " + Math.round(shown * 100) + "%";
            int s = Math.max(1, Math.round(h / 400f));
            GL11.glPushMatrix();
            GL11.glTranslatef(cx - font.getStringWidth(text) * s / 2f, by + barH + 10, 0);
            GL11.glScalef(s, s, 1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0.9f, 0.93f, 1f, 1f);
            font.drawString(text, 0, 0, 0xE6ECFF);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
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

    private static void rect(float x, float y, float w, float h, int rgb, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
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
