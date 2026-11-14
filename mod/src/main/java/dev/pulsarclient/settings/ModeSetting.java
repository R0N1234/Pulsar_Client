package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Uma escolha entre opções fixas; clicar passa para a próxima. */
public class ModeSetting extends Setting {
    private String[] options;
    private int index;

    public ModeSetting(String key, String label, int index, String... options) {
        super(key, label);
        this.options = options;
        this.index = index;
    }

    public int index() { return index; }
    public void set(int i) { if (i >= 0 && i < options.length) index = i; }
    public String get() { return options[index]; }
    public String[] options() { return options; }

    /** Troca a lista de opções (vozes achadas numa pasta, por exemplo), mantendo a escolhida se ela ainda existir. */
    public void setOptions(String... novas) {
        if (novas == null || novas.length == 0) return;
        String atual = get();
        options = novas;
        index = 0;
        for (int i = 0; i < options.length; i++) if (options[i].equals(atual)) index = i;
    }

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
