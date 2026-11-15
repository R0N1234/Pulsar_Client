package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Campo de Colisão: uma caixa em volta de cada jogador, do tamanho da área de acerto.
 * Só jogadores (itens, flechas e outros bichos não ganham caixa), com cor, espessura da linha e tamanho ajustáveis.
 */
public class CampoColisao extends PulsarMod {
    private static final double RANGE = 64;

    private final ColorSetting cor = add(new ColorSetting("cor", "Cor da caixa", ColorSetting.CIANO));
    private final SliderSetting espessura = add(new SliderSetting("espessura", "Espessura da linha", 2, 1, 6, 0.5, ""));
    private final SliderSetting tamanho = add(new SliderSetting("tamanho", "Tamanho da caixa", 100, 50, 200, 5, "%"));
    private final SliderSetting opacidade = add(new SliderSetting("opacidade", "Opacidade", 100, 20, 100, 5, "%"));
    private final BoolSetting preencher = add(new BoolSetting("preencher", "Preencher a caixa", false));
    private final BoolSetting proprio = add(new BoolSetting("proprio", "Mostrar em mim (3ª pessoa)", false));

    public CampoColisao() {
        super("campo_colisao", "Campo de Colisão", "Caixa de acerto em volta dos jogadores", Category.COMBATE, false);
    }

    @Override
    protected void onEnable() {
        // Versões antigas ligavam o F3+B do jogo, que põe caixa em tudo. Agora a caixa é nossa e só nos jogadores.
        if (mc.getRenderManager() != null) mc.getRenderManager().setDebugBoundingBox(false);
    }

    /** Caixa de um jogador já no tamanho escolhido, crescendo ou encolhendo a partir do meio dele. */
    public static AxisAlignedBB caixa(AxisAlignedBB bb, double escala) {
        double cx = (bb.minX + bb.maxX) / 2, cy = (bb.minY + bb.maxY) / 2, cz = (bb.minZ + bb.maxZ) / 2;
        double hx = (bb.maxX - bb.minX) / 2 * escala, hy = (bb.maxY - bb.minY) / 2 * escala, hz = (bb.maxZ - bb.minZ) / 2 * escala;
        return new AxisAlignedBB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) return;
        RenderManager rm = mc.getRenderManager();
        float pt = event.partialTicks;
        int rgb = cor.rgb();
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        float a = (float) (opacidade.get() / 100.0);
        double escala = tamanho.get() / 100.0;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth((float) espessura.get());
        try {
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p.isDead || p.isInvisibleToPlayer(mc.thePlayer)) continue;
                if (p == mc.thePlayer && (!proprio.get() || mc.gameSettings.thirdPersonView == 0)) continue;
                if (p != mc.thePlayer && p.getDistanceSqToEntity(mc.thePlayer) > RANGE * RANGE) continue;
                double x = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt - rm.viewerPosX;
                double y = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt - rm.viewerPosY;
                double z = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt - rm.viewerPosZ;
                AxisAlignedBB bb = p.getEntityBoundingBox();
                AxisAlignedBB local = caixa(bb.offset(-p.posX, -p.posY, -p.posZ), escala).offset(x, y, z);
                if (preencher.get()) {
                    GlStateManager.color(r, g, b, a * 0.18f);
                    caixaCheia(local);
                }
                GlStateManager.color(r, g, b, a);
                RenderGlobal.drawSelectionBoundingBox(local);
            }
        } finally {
            GL11.glLineWidth(1f);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.popMatrix();
        }
    }

    private static void caixaCheia(AxisAlignedBB c) {
        GL11.glBegin(GL11.GL_QUADS);
        double[][] f = {
                {c.minX, c.minY, c.minZ, c.maxX, c.minY, c.minZ, c.maxX, c.minY, c.maxZ, c.minX, c.minY, c.maxZ},
                {c.minX, c.maxY, c.minZ, c.minX, c.maxY, c.maxZ, c.maxX, c.maxY, c.maxZ, c.maxX, c.maxY, c.minZ},
                {c.minX, c.minY, c.minZ, c.minX, c.maxY, c.minZ, c.maxX, c.maxY, c.minZ, c.maxX, c.minY, c.minZ},
                {c.minX, c.minY, c.maxZ, c.maxX, c.minY, c.maxZ, c.maxX, c.maxY, c.maxZ, c.minX, c.maxY, c.maxZ},
                {c.minX, c.minY, c.minZ, c.minX, c.minY, c.maxZ, c.minX, c.maxY, c.maxZ, c.minX, c.maxY, c.minZ},
                {c.maxX, c.minY, c.minZ, c.maxX, c.maxY, c.minZ, c.maxX, c.maxY, c.maxZ, c.maxX, c.minY, c.maxZ},
        };
        for (double[] q : f) for (int i = 0; i < 12; i += 3) GL11.glVertex3d(q[i], q[i + 1], q[i + 2]);
        GL11.glEnd();
    }
}
