package dev.pulsarclient.mods;

import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/** Propulsão: correr fica ligado sem segurar a tecla. Equivale ao Toggle Sprint. */
public class Propulsao extends HudMod {
    public final KeyBinding key = new KeyBinding("Propulsão (correr fixo)", Keyboard.KEY_R, "Pulsar Client");
    private final TeclaDoJogo modeKey = add(new TeclaDoJogo("tecla_correr", "Tecla do correr fixo", key));
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
