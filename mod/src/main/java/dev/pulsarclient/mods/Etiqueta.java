package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/** Texto flutuando no mundo virado para a câmera, igual ao nome dos jogadores. */
public final class Etiqueta {
    private Etiqueta() {}

    /** x, y, z relativos à câmera. */
    public static void draw(String text, double x, double y, double z, int color, boolean background, boolean shadow) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        FontRenderer font = mc.fontRendererObj;
        float scale = 0.016666668f * 1.6f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(rm.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int half = font.getStringWidth(text) / 2;
        if (background) {
            GlStateManager.disableTexture2D();
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-half - 1, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            wr.pos(-half - 1, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            wr.pos(half + 1, 8, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            wr.pos(half + 1, -1, 0).color(0f, 0f, 0f, 0.25f).endVertex();
            tess.draw();
            GlStateManager.enableTexture2D();
        }
        // Primeiro meio transparente (aparece através de blocos), depois cheio por cima.
        font.drawString(text, -half, 0, 0x20000000 | (color & 0xFFFFFF));
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        if (shadow) font.drawStringWithShadow(text, -half, 0, color);
        else font.drawString(text, -half, 0, color);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    /** Igual a {@link #draw}, mas sem aparecer através de blocos (como o nome de quem está agachado). */
    public static void drawComProfundidade(String text, double x, double y, double z, int color) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        FontRenderer font = mc.fontRendererObj;
        float scale = 0.016666668f * 1.6f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(rm.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int half = font.getStringWidth(text) / 2;
        font.drawStringWithShadow(text, -half, 0, color);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}
