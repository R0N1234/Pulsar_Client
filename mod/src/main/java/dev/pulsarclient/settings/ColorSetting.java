package dev.pulsarclient.settings;

/** Cores do tema do Pulsar, escolhidas pelo nome. */
public class ColorSetting extends ModeSetting {
    private static final String[] NAMES = {"Ciano", "Branco", "Laranja", "Roxo", "Verde", "Vermelho", "Amarelo"};
    private static final int[] RGB = {0x7FE7FF, 0xFFFFFF, 0xFF8A3D, 0x9B7CFF, 0x6EE7A8, 0xFF5A5A, 0xFFD84A};

    public ColorSetting(String key, String label, int index) {
        super(key, label, index, NAMES);
    }

    public int rgb() { return RGB[index()]; }
    public int argb(int alpha) { return (alpha << 24) | rgb(); }
}
