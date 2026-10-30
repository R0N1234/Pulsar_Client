package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Texto salvo em config.json. Com rótulo, aparece no painel como campo de texto; sem rótulo, fica escondido. */
public class TextSetting extends Setting {
    private final boolean visible;
    private final int maxLength;
    private String value;

    /** Texto escondido, guardado pelo mod (ex.: slots travados). */
    public TextSetting(String key, String value) {
        super(key, key);
        this.value = value;
        this.visible = false;
        this.maxLength = Integer.MAX_VALUE;
    }

    /** Campo de texto no painel. */
    public TextSetting(String key, String label, String value, int maxLength) {
        super(key, label);
        this.value = value;
        this.visible = true;
        this.maxLength = maxLength;
    }

    public String get() { return value; }
    public void set(String value) { this.value = value == null ? "" : value.length() > maxLength ? value.substring(0, maxLength) : value; }
    public int maxLength() { return maxLength; }

    @Override public boolean visible() { return visible; }
    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement v) { if (v.isJsonPrimitive()) set(v.getAsString()); }
}
