package dev.pulsarclient.core;

/** Quais ganchos do Motor Pulsar entraram no jogo. Classe sem nada do Minecraft, para poder ser usada durante o carregamento. */
public final class MotorFlags {
    public static volatile boolean entities;
    public static volatile boolean tiles;
    public static volatile boolean animations;
    public static volatile boolean particles;

    private MotorFlags() {}

    static void installed(String label) {
        if (label.equals("entidades")) entities = true;
        else if (label.equals("blocos especiais")) tiles = true;
        else if (label.equals("animações")) animations = true;
        else if (label.equals("partículas")) particles = true;
    }
}
