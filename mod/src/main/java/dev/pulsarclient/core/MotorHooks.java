package dev.pulsarclient.core;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

/**
 * Pontos onde o Motor Pulsar entra no código do Minecraft (veja {@link MotorTransformer}).
 * O jogo chama estes métodos; o mod Motor Pulsar preenche os filtros. Sem filtro, tudo segue como no jogo normal.
 * Qualquer erro num filtro desliga só aquele filtro, e o jogo continua.
 */
public final class MotorHooks {
    public interface EntityFilter { boolean shouldRender(Entity entity); }
    public interface TileFilter { boolean shouldRender(TileEntity tile); }
    public interface ParticleFilter { boolean allow(EntityFX particle); }

    public static volatile EntityFilter entities;
    public static volatile TileFilter tiles;
    public static volatile ParticleFilter particles;
    public static volatile boolean animations = true;
    /** Partículas novas por tick; 0 = sem limite. */
    public static volatile int particleLimit;

    private static int particlesThisTick;

    private MotorHooks() {}

    public static boolean shouldRenderEntity(Entity entity) {
        EntityFilter f = entities;
        if (f == null) return true;
        try {
            return f.shouldRender(entity);
        } catch (Throwable t) {
            entities = null;
            System.err.println("[Pulsar] Motor: filtro de entidades desligado por erro: " + t);
            return true;
        }
    }

    public static boolean shouldRenderTile(TileEntity tile) {
        TileFilter f = tiles;
        if (f == null) return true;
        try {
            return f.shouldRender(tile);
        } catch (Throwable t) {
            tiles = null;
            System.err.println("[Pulsar] Motor: filtro de blocos especiais desligado por erro: " + t);
            return true;
        }
    }

    public static boolean animate() {
        return animations;
    }

    public static boolean allowParticle(EntityFX particle) {
        ParticleFilter f = particles;
        if (f != null && particle != null) {
            try {
                if (!f.allow(particle)) return false;
            } catch (Throwable t) {
                particles = null;
                System.err.println("[Pulsar] Motor: filtro de partículas desligado por erro: " + t);
            }
        }
        int limit = particleLimit;
        return limit <= 0 || ++particlesThisTick <= limit;
    }

    public static void newTick() {
        particlesThisTick = 0;
    }
}
