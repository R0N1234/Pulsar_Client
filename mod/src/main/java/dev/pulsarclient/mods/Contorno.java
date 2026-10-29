package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/** Contorno: cor e espessura da linha em volta do bloco que você está mirando. */
public class Contorno extends PulsarMod {
    private final ColorSetting color = add(new ColorSetting("cor", "Cor", 0));
    private final SliderSetting width = add(new SliderSetting("espessura", "Espessura", 2.5, 1, 6, 0.5, ""));
    private final SliderSetting alpha = add(new SliderSetting("opacidade", "Opacidade", 80, 20, 100, 5, "%"));
    private final BoolSetting throughWalls = add(new BoolSetting("atras", "Aparecer atrás de blocos", false));

    public Contorno() {
        super("contorno", "Contorno", "Cor e espessura do contorno do bloco", Category.VISUAL, false);
    }

    @SubscribeEvent
    public void onHighlight(DrawBlockHighlightEvent event) {
        if (!isEnabled() || event.target == null || event.target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        BlockPos pos = event.target.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block.getMaterial() == Material.air || !mc.theWorld.getWorldBorder().contains(pos)) return;
        event.setCanceled(true);

        EntityPlayer p = event.player;
        float pt = event.partialTicks;
        double x = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
        double y = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
        double z = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
        block.setBlockBoundsBasedOnState(mc.theWorld, pos);
        AxisAlignedBB box = block.getSelectedBoundingBox(mc.theWorld, pos).expand(0.002, 0.002, 0.002).offset(-x, -y, -z);

        int rgb = color.rgb();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, (float) (alpha.get() / 100.0));
        GL11.glLineWidth((float) width.get());
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        if (throughWalls.get()) GlStateManager.disableDepth();
        RenderGlobal.drawSelectionBoundingBox(box);
        if (throughWalls.get()) GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glLineWidth(1f);
    }
}
