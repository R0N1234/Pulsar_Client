package dev.pulsarclient.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Uma cor qualquer, escolhida na paleta do painel (quadradinho com a cor). Salva como "#RRGGBB".
 * Configs antigas guardavam o nome da cor ("Ciano", "Vermelho"...), e esses nomes ainda são lidos.
 */
public class ColorSetting extends Setting {
    public static final int CIANO = 0x7FE7FF;
    public static final int BRANCO = 0xFFFFFF;
    public static final int LARANJA = 0xFF8A3D;
    public static final int ROXO = 0x9B7CFF;
    public static final int VERDE = 0x6EE7A8;
    public static final int VERMELHO = 0xFF5A5A;
    public static final int AMARELO = 0xFFD84A;

    /** Cores prontas da paleta: as do Pulsar primeiro, depois as mais usadas. */
    public static final int[] PRONTAS = {
            CIANO, ROXO, LARANJA, VERDE, VERMELHO, AMARELO, BRANCO,
            0xFF0000, 0xFF4FD8, 0x3C6BFF, 0x00FF7F, 0xFFA500, 0x8B5A2B, 0x808080, 0x000000,
    };

    private static final String[] NOMES_ANTIGOS = {"Ciano", "Branco", "Laranja", "Roxo", "Verde", "Vermelho", "Amarelo", "Roxo Pulsar", "Rosa"};
    private static final int[] CORES_ANTIGAS = {CIANO, BRANCO, LARANJA, ROXO, VERDE, VERMELHO, AMARELO, 0x9B5CFF, 0xFF4FD8};

    private int rgb;

    public ColorSetting(String key, String label, int rgb) {
        super(key, label);
        set(rgb);
    }

    public int rgb() { return rgb; }
    public int argb(int alpha) { return (alpha << 24) | rgb; }
    public void set(int rgb) { this.rgb = rgb & 0xFFFFFF; }
    public String hex() { return String.format("#%06X", rgb); }

    /** Matiz, saturação e brilho (0 a 1) para RGB, como na paleta. */
    public static int hsb(float h, float s, float b) {
        h = (h - (float) Math.floor(h)) * 6f;
        int i = (int) h;
        float f = h - i;
        float p = b * (1 - s);
        float q = b * (1 - s * f);
        float t = b * (1 - s * (1 - f));
        float r, g, bl;
        switch (i) {
            case 0: r = b; g = t; bl = p; break;
            case 1: r = q; g = b; bl = p; break;
            case 2: r = p; g = b; bl = t; break;
            case 3: r = p; g = q; bl = b; break;
            case 4: r = t; g = p; bl = b; break;
            default: r = b; g = p; bl = q; break;
        }
        return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(bl * 255);
    }

    /** RGB para {matiz, saturação, brilho}. */
    public static float[] hsb(int rgb) {
        float r = (rgb >> 16 & 255) / 255f;
        float g = (rgb >> 8 & 255) / 255f;
        float b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) / 6f;
            else if (max == g) h = ((b - r) / d + 2) / 6f;
            else h = ((r - g) / d + 4) / 6f;
            if (h < 0) h += 1;
        }
        return new float[] {h, max == 0 ? 0 : d / max, max};
    }

    @Override public JsonElement save() { return new JsonPrimitive(hex()); }

    @Override
    public void load(JsonElement v) {
        if (!v.isJsonPrimitive()) return;
        String s = v.getAsString().trim();
        if (s.startsWith("#") && s.length() == 7) {
            try {
                set(Integer.parseInt(s.substring(1), 16));
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        for (int i = 0; i < NOMES_ANTIGOS.length; i++) if (NOMES_ANTIGOS[i].equals(s)) set(CORES_ANTIGAS[i]);
    }
}
