package dev.pulsarclient.mods;

import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/** Cronômetro: uma tecla (P no começo) inicia e pausa; zere pelo painel. Bom para marcar partidas e treinos. */
public class Cronometro extends HudMod {
    public final KeyBinding key = new KeyBinding("Cronômetro (iniciar/pausar)", Keyboard.KEY_P, "Pulsar Client");
    private final TeclaDoJogo startKey = add(new TeclaDoJogo("tecla_iniciar", "Tecla para iniciar e pausar", key));
    private final ActionSetting reset = add(new ActionSetting("zerar", "Zerar o cronômetro", new Runnable() {
        @Override public void run() { zerar(); }
    }));

    private long startedAt;
    private long accumulated;
    private boolean running;

    public Cronometro() {
        super("cronometro", "Cronômetro", "Marca tempo de partidas e treinos", Category.HUD, 5, 226, false);
    }

    private void zerar() {
        running = false;
        accumulated = 0;
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (!isEnabled() || !key.isPressed()) return;
        long now = System.currentTimeMillis();
        if (running) accumulated += now - startedAt;
        else startedAt = now;
        running = !running;
    }

    private String value() {
        long ms = accumulated + (running ? System.currentTimeMillis() - startedAt : 0);
        long tenths = ms / 100;
        return String.format("%02d:%02d.%d", tenths / 600, tenths / 10 % 60, tenths % 10) + (running ? "" : " (pausado)");
    }

    @Override public int width() { return labelWidth("Tempo", "00:00.0 (pausado)"); }
    @Override public int height() { return 8; }
    @Override public void render(boolean preview) { drawLabel("Tempo", value()); }
}
