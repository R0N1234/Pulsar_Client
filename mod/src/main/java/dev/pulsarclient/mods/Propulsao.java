package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/** Propulsão: correr fica ligado sem segurar a tecla. Equivale ao Toggle Sprint. */
public class Propulsao extends HudMod {
    public final KeyBinding key = new KeyBinding("Propulsão (correr fixo)", Keyboard.KEY_R, "Pulsar Client");
    private final TeclaDoJogo modeKey = add(new TeclaDoJogo("tecla_correr", "Tecla do correr fixo", key));
    // POV fixo: a tela não dá aquele zoom ao correr (nem com poção de Velocidade). O zoom do arco continua.
    private final BoolSetting semZoom = add(new BoolSetting("sem_zoom", "Tirar o zoom da tela ao correr", false));
    private boolean active = true;

    public Propulsao() {
        super("propulsao", "Propulsão", "Correr fica ligado sem segurar a tecla", Category.COMBATE, 5, 142);
    }

    /** A tecla do mod (R no começo): alterna entre correr fixo e manual sem desligar o mod. */
    public void toggleActive() {
        active = !active;
        if (!active) release();
    }

    @Override
    protected void onDisable() {
        release();
    }

    @Override
    public void tick() {
        if (!isEnabled() || !active || mc.thePlayer == null) return;
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        KeyBinding.setKeyBindState(sprint.getKeyCode(), true);
    }

    // Devolve à tecla de correr o estado real dela, para não ficar presa.
    private void release() {
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        KeyBinding.setKeyBindState(sprint.getKeyCode(), GameSettings.isKeyDown(sprint));
    }

    public boolean semZoom() {
        return isEnabled() && semZoom.get();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFov(FOVUpdateEvent event) {
        if (!semZoom()) return;
        EntityPlayer p = event.entity;
        float fov = 1.0f;
        if (p.isUsingItem() && p.getItemInUse() != null && p.getItemInUse().getItem() == Items.bow) {
            float puxado = p.getItemInUseDuration() / 20.0f;
            puxado = puxado > 1.0f ? 1.0f : puxado * puxado;
            fov *= 1.0f - puxado * 0.15f;
        }
        event.newfov = fov;
    }

    private String text() {
        return active ? "[Correndo (fixo)]" : "[Correr: manual]";
    }

    @Override public int width() { return font().getStringWidth(text()); }
    @Override public int height() { return 8; }

    @Override
    public void render(boolean preview) {
        drawLabel("", text());
    }
}
