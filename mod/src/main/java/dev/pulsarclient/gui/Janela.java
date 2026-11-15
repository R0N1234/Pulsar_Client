package dev.pulsarclient.gui;

import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Nome e ícone da janela do jogo: o logo do Pulsar aparece na barra de tarefas enquanto o Minecraft está aberto. */
public final class Janela {
    private Janela() {}

    public static void aplicar(String version) {
        try {
            Display.setTitle("Minecraft 1.8.9 - Pulsar Client " + version);
        } catch (Throwable t) {
            System.err.println("[Pulsar] Não deu para trocar o nome da janela: " + t);
        }
        try {
            ByteBuffer pequeno = icone("/assets/pulsar/textures/gui/janela16.png");
            ByteBuffer grande = icone("/assets/pulsar/textures/gui/janela32.png");
            if (pequeno != null && grande != null) {
                Display.setIcon(new ByteBuffer[]{pequeno, grande});
                System.out.println("[Pulsar] Ícone da janela: logo do Pulsar");
            }
        } catch (Throwable t) {
            System.err.println("[Pulsar] Não deu para trocar o ícone da janela: " + t);
        }
    }

    /** PNG do jar em RGBA, do jeito que o LWJGL pede. */
    private static ByteBuffer icone(String caminho) throws Exception {
        InputStream in = Janela.class.getResourceAsStream(caminho);
        if (in == null) return null;
        BufferedImage img;
        try {
            img = ImageIO.read(in);
        } finally {
            in.close();
        }
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        ByteBuffer buf = ByteBuffer.allocate(w * h * 4);
        for (int p : px) buf.putInt(p << 8 | p >>> 24);
        buf.flip();
        return buf;
    }
}
