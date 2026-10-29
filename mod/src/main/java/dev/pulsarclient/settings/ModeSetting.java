package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Uma escolha entre opções fixas; clicar passa para a próxima. */
public class ModeSetting extends Setting {
    private final String[] options;
    private int index;

    public ModeSetting(String key, String label, int index, String... options) {
        super(key, label);
        this.options = options;
        this.index = index;
    }

    public int index() { return index; }
    public String get() { return options[index]; }
    public void next() { index = (index + 1) % options.length; }
    public void previous() { index = (index + options.length - 1) % options.length; }

    @Override public JsonElement save() { return new JsonPrimitive(get()); }

    @Override
    public void load(JsonElement v) {
        if (!v.isJsonPrimitive()) return;
        String s = v.getAsString();
        for (int i = 0; i < options.length; i++) if (options[i].equals(s)) index = i;
    }
}
