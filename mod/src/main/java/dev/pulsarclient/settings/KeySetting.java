package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.settings.GameSettings;

/**
 * Uma tecla escolhida no painel. Usa o mesmo código das teclas do Minecraft:
 * maior que zero é tecla do teclado, negativo é botão do mouse (botão - 100) e zero é nenhuma.
 */
public class KeySetting extends Setting {
    private int code;

    public KeySetting(String key, String label, int code) {
        super(key, label);
        this.code = code;
    }

    public int get() { return code; }
    public void set(int code) { this.code = code; }

    public String display() {
        return get() == 0 ? "Nenhuma" : GameSettings.getKeyDisplayString(get());
    }

    @Override public JsonElement save() { return new JsonPrimitive(code); }
    @Override public void load(JsonElement v) { if (v.isJsonPrimitive()) code = v.getAsInt(); }
}
