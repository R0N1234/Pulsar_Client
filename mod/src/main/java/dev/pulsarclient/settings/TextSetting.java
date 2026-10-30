package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Texto salvo em config.json que não aparece no painel. */
public class TextSetting extends Setting {
    private String value;

    public TextSetting(String key, String value) {
        super(key, key);
        this.value = value;
    }

    public String get() { return value; }
    public void set(String value) { this.value = value; }

    @Override public boolean visible() { return false; }
    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement v) { if (v.isJsonPrimitive()) value = v.getAsString(); }
}
