package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Conta os hits que você realmente acertou, para o Combo e o Alcance.
 * Um ataque só vira hit quando o servidor confirma: o alvo pisca de dano logo depois do clique.
 */
public final class CombatTracker {
    private static final int CONFIRM_TICKS = 8;
    private static final int COMBO_TIMEOUT = 40;
    private static final int REACH_TIMEOUT = 60;

    private static int ticks;
    private static EntityLivingBase target;
    private static int attackTick;
    private static double pendingReach;
    private static int targetHurt;
    private static int selfHurt;
    private static int combo;
    private static int lastHitTick = -1000;
    private static double reach = -1;
    private static PoeiraEstelar onHit;

    public static void listen(PoeiraEstelar mod) { onHit = mod; }

    public static int combo() { return ticks - lastHitTick > COMBO_TIMEOUT ? 0 : combo; }
    public static double reach() { return ticks - lastHitTick > REACH_TIMEOUT ? -1 : reach; }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.entityPlayer != mc.thePlayer || !(event.target instanceof EntityLivingBase)) return;
        target = (EntityLivingBase) event.target;
        targetHurt = target.hurtTime;
        attackTick = ticks;
        MovingObjectPosition hit = mc.objectMouseOver;
        Entity eye = mc.getRenderViewEntity();
        pendingReach = hit != null && hit.hitVec != null && eye != null ? eye.getPositionEyes(1f).distanceTo(hit.hitVec) : -1;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            target = null;
            combo = 0;
            return;
        }
        if (mc.thePlayer.hurtTime > selfHurt) combo = 0;
        selfHurt = mc.thePlayer.hurtTime;
        if (target == null) return;
        if (target.hurtTime > targetHurt && ticks - attackTick <= CONFIRM_TICKS) {
            combo = ticks - lastHitTick > COMBO_TIMEOUT ? 1 : combo + 1;
            lastHitTick = ticks;
            if (pendingReach > 0) reach = pendingReach;
            if (onHit != null) onHit.onHit(target);
            target = null;
            return;
        }
        targetHurt = target.hurtTime;
        if (ticks - attackTick > CONFIRM_TICKS) target = null;
    }
}
