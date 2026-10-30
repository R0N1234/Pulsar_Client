package dev.pulsarclient.gui;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Nome e ícone da janela do jogo: "Pulsar Client" com o logo PC. */
public final class Janela {
    private Janela() {}

    public static void aplicar(String version) {
        try {
            Display.setTitle("Pulsar Client " + version + " (Minecraft 1.8.9)");
            Display.setIcon(new ByteBuffer[]{icon(16), icon(32), icon(64)});
        } catch (Throwable t) {
            System.err.println("[Pulsar] Não deu para trocar o ícone da janela: " + t);
        }
    }

    static ByteBuffer icon(int size) throws Exception {
        InputStream in = Janela.class.getResourceAsStream("/assets/pulsar/textures/gui/icone_" + size + ".png");
        if (in == null) throw new IllegalStateException("ícone " + size + " não encontrado");
        BufferedImage img;
        try {
            img = ImageIO.read(in);
        } finally {
            in.close();
        }
        int[] px = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        ByteBuffer buf = BufferUtils.createByteBuffer(px.length * 4);
        for (int p : px) buf.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
        buf.flip();
        return buf;
    }
}
