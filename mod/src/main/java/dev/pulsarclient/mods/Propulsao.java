package dev.pulsarclient.mods;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/** Propulsão: correr fica ligado sem segurar a tecla. Equivale ao Toggle Sprint. */
public class Propulsao extends PulsarMod {
    private boolean active = true;

    public Propulsao() {
        super("propulsao", "Propulsão", 5, 142);
    }

    public void toggle() {
        active = !active;
        if (!active) release();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) release();
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
