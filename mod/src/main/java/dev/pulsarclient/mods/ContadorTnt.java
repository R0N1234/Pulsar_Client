package dev.pulsarclient.mods;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Contador TNT: mostra em cima da TNT acesa quantos segundos faltam para explodir. */
public class ContadorTnt extends PulsarMod {
    private static final double RANGE = 48;

    public ContadorTnt() {
        super("contador_tnt", "Contador TNT", "Tempo até a TNT explodir", Category.COMBATE, false);
    }

    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.theWorld == null || mc.thePlayer == null) return;
        RenderManager rm = mc.getRenderManager();
        float pt = event.partialTicks;
        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!(e instanceof EntityTNTPrimed) || e.getDistanceSqToEntity(mc.thePlayer) > RANGE * RANGE) continue;
            int fuse = ((EntityTNTPrimed) e).fuse;
            double seconds = Math.max(0, (fuse - pt) / 20.0);
            // Verde com tempo, amarelo na metade, vermelho no fim.
            int color = seconds > 2 ? 0xFF6EE7A8 : seconds > 1 ? 0xFFFFD84A : 0xFFFF5A5A;
            double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * pt - rm.viewerPosX;
            double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * pt - rm.viewerPosY + e.height + 0.5;
            double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * pt - rm.viewerPosZ;
            Etiqueta.draw(String.format("%.1fs", seconds), x, y, z, color, true, true);
        }
    }
}
