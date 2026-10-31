package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * Tecla de uma ação do mod (zoom, órbita, cronômetro...) escolhida no painel do mod.
 * É a mesma tecla de Opções > Controles: mudar num lugar muda no outro, e ela fica salva no options.txt do jogo.
 */
public class TeclaDoJogo extends KeySetting {
    private final KeyBinding bind;

    public TeclaDoJogo(String key, String label, KeyBinding bind) {
        super(key, label, 0);
        this.bind = bind;
    }

    public KeyBinding bind() { return bind; }

    @Override public int get() { return bind.getKeyCode(); }

    @Override
    public void set(int code) {
        bind.setKeyCode(code);
        KeyBinding.resetKeyBindingArrayAndHash();
        Minecraft.getMinecraft().gameSettings.saveOptions();
    }

    @Override public JsonElement save() { return null; }
    @Override public void load(JsonElement v) {}
}
