package dev.pulsarclient.mods;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.core.MotorFlags;
import dev.pulsarclient.core.MotorHooks;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.Display;

/**
 * Motor Pulsar: o otimizador do próprio client.
 *
 * Parte dele roda por ganchos dentro do Minecraft (veja core/MotorTransformer): entidades atrás de paredes ou longe,
 * baús e placas longe, animações de textura e limite de partículas. O resto usa eventos do Forge:
 * FPS em segundo plano, suportes de armadura, distância dos nomes e os perfis prontos.
 */
public class MotorPulsar extends PulsarMod {
    private static final String[] PROFILES = {"Desempenho", "Desempenho + Qualidade", "Qualidade+"};
    private static final int[] PARTICLE_LIMITS = {0, 300, 100, 30};

    private final ModeSetting profile = add(new ModeSetting("perfil", "Perfil", 1, PROFILES));
    private final ActionSetting applyProfile = add(new ActionSetting("aplicar", "Aplicar o perfil escolhido", new Runnable() {
        @Override public void run() { applyProfile(profile.index()); }
    }));
    private final BoolSetting background = add(new BoolSetting("fundo", "Poupar PC em segundo plano", true));
    private final SliderSetting backgroundFps = add(new SliderSetting("fundo_fps", "FPS em segundo plano", 15, 5, 60, 5, ""));
    private final BoolSetting occlusion = add(new BoolSetting("oclusao", "Não desenhar o que está atrás de paredes", true));
    private final BoolSetting armorStands = add(new BoolSetting("suportes", "Esconder suportes de armadura", true));
    private final BoolSetting cullMobs = add(new BoolSetting("mobs", "Limitar distância dos mobs", true));
    private final SliderSetting mobDistance = add(new SliderSetting("mobs_dist", "Distância dos mobs", 48, 16, 128, 8, " blocos"));
    private final SliderSetting itemDistance = add(new SliderSetting("itens_dist", "Itens no chão, flechas e XP até", 32, 8, 64, 4, " blocos"));
    private final SliderSetting tileDistance = add(new SliderSetting("blocos_dist", "Baús, placas e cabeças até", 48, 16, 64, 4, " blocos"));
    private final BoolSetting limitNames = add(new BoolSetting("nomes", "Limitar distância dos nomes", false));
    private final SliderSetting nameDistance = add(new SliderSetting("nomes_dist", "Distância dos nomes", 32, 8, 64, 4, " blocos"));
    private final ModeSetting particles = add(new ModeSetting("particulas", "Partículas", 1, "Todas", "Menos", "Mínimas"));
    private final ModeSetting particleLimit = add(new ModeSetting("particulas_limite", "Limite de partículas novas", 1,
            "Sem limite", "300 por tick", "100 por tick", "30 por tick"));
    private final BoolSetting animations = add(new BoolSetting("animacoes", "Animação de água, lava e fogo", true));
    private final ActionSetting freeMemory = add(new ActionSetting("memoria", "Liberar memória agora", new Runnable() {
        @Override public void run() { System.gc(); }
    }));

    private final Oclusao oclusao = new Oclusao();
    private int appliedParticles = -1;
    private World lastWorld;

    public MotorPulsar() {
        super("motor_pulsar", "Motor Pulsar", "Otimizador do Pulsar: mais FPS e menos travadas", Category.MOTOR, true);
        MotorHooks.entities = new MotorHooks.EntityFilter() {
            @Override public boolean shouldRender(Entity entity) { return shouldDraw(entity); }
        };
        MotorHooks.tiles = new MotorHooks.TileFilter() {
            @Override public boolean shouldRender(TileEntity tile) { return shouldDraw(tile); }
        };
    }

    /** Estado dos ganchos, para o painel e o log. */
    public static String hooksSummary() {
        return "entidades=" + MotorFlags.entities + " blocos=" + MotorFlags.tiles
                + " animações=" + MotorFlags.animations + " partículas=" + MotorFlags.particles;
    }

    @Override
    public void tick() {
        MotorHooks.newTick();
        oclusao.tick();
        if (mc.theWorld != lastWorld) {
            lastWorld = mc.theWorld;
            oclusao.clear();
        }
        MotorHooks.animations = !isEnabled() || animations.get();
        MotorHooks.particleLimit = isEnabled() ? PARTICLE_LIMITS[particleLimit.index()] : 0;
        if (!isEnabled()) return;
        // Só aplica quando a opção do Motor muda, para não brigar com a opção de partículas do jogo.
        if (particles.index() != appliedParticles) {
            appliedParticles = particles.index();
            mc.gameSettings.particleSetting = appliedParticles;
        }
    }

    // ---------- entidades ----------

    private boolean shouldDraw(Entity e) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return true;
        Entity view = mc.getRenderViewEntity();
        if (view == null || e == view || e == mc.thePlayer) return true;
        // Suporte com nome é holograma de servidor: continua visível.
        if (armorStands.get() && e instanceof EntityArmorStand && !e.hasCustomName()) return false;
        double d2 = e.getDistanceSqToEntity(view);
        if (e instanceof EntityItem || e instanceof EntityXPOrb || e instanceof EntityArrow) {
            double max = itemDistance.get();
            if (d2 > max * max) return false;
        } else if (cullMobs.get() && e instanceof EntityLivingBase && !(e instanceof EntityPlayer)) {
            double max = mobDistance.get();
            if (d2 > max * max) return false;
        }
        // Em terceira pessoa a câmera fica atrás do jogador, então a conta pelos olhos dele não vale.
        return !(occlusion.get() && mc.gameSettings.thirdPersonView == 0 && oclusao.hidden(mc.theWorld, view.getPositionEyes(1f), e));
    }

    private boolean shouldDraw(TileEntity t) {
        if (!isEnabled()) return true;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return true;
        double max = tileDistance.get();
        return t.getDistanceSq(view.posX, view.posY, view.posZ) <= max * max;
    }

    /** Sem o gancho de entidades (por exemplo, com outro mod mexendo no mesmo lugar), o evento do Forge faz o serviço. */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        if (!MotorFlags.entities && !shouldDraw(event.entity)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderName(RenderLivingEvent.Specials.Pre event) {
        if (!isEnabled() || !limitNames.get() || mc.thePlayer == null || event.entity == mc.thePlayer) return;
        double max = nameDistance.get();
        if (event.entity.getDistanceSqToEntity(mc.thePlayer) > max * max) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isEnabled() || !background.get()) return;
        if (!Display.isActive()) Display.sync(backgroundFps.getInt());
    }

    // ---------- perfis ----------

    /**
     * 0 = Desempenho: tudo no mínimo, para PC fraco ou para ter o máximo de FPS.
     * 1 = Desempenho + Qualidade: visual bonito no que pesa pouco, corta o que pesa muito.
     * 2 = Qualidade+: visual completo; o Motor só corta o que não aparece na tela.
     */
    private void applyProfile(int p) {
        GameSettings gs = mc.gameSettings;
        gs.fancyGraphics = p > 0;
        gs.clouds = p;
        gs.ambientOcclusion = p;
        gs.entityShadows = p == 2;
        gs.particleSetting = 2 - p;
        gs.useVbo = true;
        gs.fboEnable = true;
        gs.enableVsync = false;
        gs.limitFramerate = 260;
        if (p == 0 && gs.renderDistanceChunks > 8) gs.renderDistanceChunks = 8;
        Display.setVSyncEnabled(false);
        int mipmap = p == 0 ? 0 : p == 1 ? 2 : 4;
        if (gs.mipmapLevels != mipmap) gs.setOptionFloatValue(GameSettings.Options.MIPMAP_LEVELS, mipmap);

        particles.set(2 - p);
        appliedParticles = 2 - p;
        occlusion.set(true);
        armorStands.set(p < 2);
        cullMobs.set(true);
        mobDistance.set(p == 0 ? 32 : p == 1 ? 48 : 96);
        itemDistance.set(p == 0 ? 24 : p == 1 ? 32 : 64);
        tileDistance.set(p == 0 ? 32 : p == 1 ? 48 : 64);
        limitNames.set(p == 0);
        nameDistance.set(32);
        particleLimit.set(p == 0 ? 2 : p == 1 ? 1 : 0);
        animations.set(p > 0);

        if (mc.renderGlobal != null && mc.theWorld != null) mc.renderGlobal.loadRenderers();
        gs.saveOptions();
        PulsarClient.get().save();
    }
}
