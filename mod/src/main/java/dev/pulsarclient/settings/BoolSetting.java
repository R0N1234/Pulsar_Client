package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting {
    private boolean value;

    public BoolSetting(String key, String label, boolean value) {
        super(key, label);
        this.value = value;
    }

    public boolean get() { return value; }
    public void set(boolean value) { this.value = value; }
    public void toggle() { value = !value; }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement v) { if (v.isJsonPrimitive()) value = v.getAsBoolean(); }
}
