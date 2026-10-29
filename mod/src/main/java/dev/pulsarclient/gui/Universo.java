package dev.pulsarclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Fundo do universo do Pulsar para os menus. É uma imagem só, então custa o mesmo que o fundo de terra do jogo. */
public final class Universo {
    public static final ResourceLocation FUNDO = new ResourceLocation("pulsar", "textures/gui/fundo.png");
    public static final ResourceLocation LOGO = new ResourceLocation("pulsar", "textures/gui/logo.png");
    private static final int W = 1920;
    private static final int H = 1080;

    private Universo() {}

    /** Desenha o fundo cobrindo a tela inteira (em coordenadas da interface), sem esticar. */
    public static void draw(int width, int height) {
        float scale = Math.max((float) width / W, (float) height / H);
        int dw = Math.round(W * scale);
        int dh = Math.round(H * scale);
        bind(FUNDO);
        Gui.drawScaledCustomSizeModalRect((width - dw) / 2, (height - dh) / 2, 0, 0, W, H, dw, dh, W, H);
    }

    /** Logo "PC" no círculo, quadrado de lado size. */
    public static void logo(int x, int y, int size) {
        bind(LOGO);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }

    private static void bind(ResourceLocation tex) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
