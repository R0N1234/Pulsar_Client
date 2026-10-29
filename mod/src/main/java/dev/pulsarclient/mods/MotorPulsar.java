package dev.pulsarclient.mods;

import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

/**
 * Motor Pulsar: o otimizador do próprio client.
 * Corta o que mais pesa em PVP (entidades longe, suportes de armadura, partículas)
 * e segura o FPS quando a janela está em segundo plano.
 */
public class MotorPulsar extends PulsarMod {
    private final BoolSetting background = add(new BoolSetting("fundo", "Poupar PC em segundo plano", true));
    private final SliderSetting backgroundFps = add(new SliderSetting("fundo_fps", "FPS em segundo plano", 15, 5, 60, 5, ""));
    private final BoolSetting armorStands = add(new BoolSetting("suportes", "Esconder suportes de armadura", true));
    private final BoolSetting cullMobs = add(new BoolSetting("mobs", "Limitar distância dos mobs", true));
    private final SliderSetting mobDistance = add(new SliderSetting("mobs_dist", "Distância dos mobs", 48, 16, 128, 8, " blocos"));
    private final ModeSetting particles = add(new ModeSetting("particulas", "Partículas", 1, "Todas", "Menos", "Mínimas"));
    private final ActionSetting preset = add(new ActionSetting("desempenho", "Aplicar modo desempenho", new Runnable() {
        @Override public void run() { applyPerformancePreset(); }
    }));
    private final ActionSetting freeMemory = add(new ActionSetting("memoria", "Liberar memória agora", new Runnable() {
        @Override public void run() { System.gc(); }
    }));

    public MotorPulsar() {
        super("motor_pulsar", "Motor Pulsar", "Otimizador do Pulsar: mais FPS e menos travadas", Category.MOTOR, true);
    }

    @Override
    public void tick() {
        if (!isEnabled()) return;
        if (mc.gameSettings.particleSetting != particles.index()) mc.gameSettings.particleSetting = particles.index();
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isEnabled() || !background.get()) return;
        if (!Display.isActive()) Display.sync(backgroundFps.getInt());
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        if (!isEnabled() || mc.thePlayer == null) return;
        EntityLivingBase entity = event.entity;
        if (entity == mc.thePlayer) return;
        // Suporte com nome é holograma de servidor: continua visível.
        if (armorStands.get() && entity instanceof EntityArmorStand && !entity.hasCustomName()) {
            event.setCanceled(true);
            return;
        }
        if (cullMobs.get() && !(entity instanceof EntityPlayer)) {
            double max = mobDistance.get();
            if (entity.getDistanceSqToEntity(mc.thePlayer) > max * max) event.setCanceled(true);
        }
    }

    /** Ajustes de vídeo que mais rendem FPS na 1.8.9, de uma vez. */
    private void applyPerformancePreset() {
        mc.gameSettings.fancyGraphics = false;
        mc.gameSettings.clouds = 0;
        mc.gameSettings.ambientOcclusion = 0;
        mc.gameSettings.entityShadows = false;
        mc.gameSettings.useVbo = true;
        mc.gameSettings.fboEnable = true;
        mc.gameSettings.enableVsync = false;
        mc.gameSettings.limitFramerate = 260;
        mc.gameSettings.mipmapLevels = 0;
        Display.setVSyncEnabled(false);
        if (mc.renderGlobal != null) mc.renderGlobal.loadRenderers();
        mc.gameSettings.saveOptions();
    }
}
