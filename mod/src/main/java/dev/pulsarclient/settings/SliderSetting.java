package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class SliderSetting extends Setting {
    private final double min;
    private final double max;
    private final double step;
    private final String unit;
    private double value;

    public SliderSetting(String key, String label, double value, double min, double max, double step, String unit) {
        super(key, label);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
        set(value);
    }

    public double get() { return value; }
    public int getInt() { return (int) Math.round(value); }
    public double min() { return min; }
    public double max() { return max; }

    public void set(double v) {
        double snapped = Math.round((v - min) / step) * step + min;
        value = Math.max(min, Math.min(max, snapped));
    }

    /** Posição de 0 a 1 na barra. */
    public double fraction() { return (value - min) / (max - min); }
    public void setFraction(double f) { set(min + f * (max - min)); }

    public String display() {
        String n = step >= 1 ? String.valueOf((int) Math.round(value)) : String.format("%.1f", value);
        return n + unit;
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement v) { if (v.isJsonPrimitive()) set(v.getAsDouble()); }
}
