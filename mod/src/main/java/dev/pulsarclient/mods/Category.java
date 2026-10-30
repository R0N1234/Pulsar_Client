package dev.pulsarclient.mods;

public enum Category {
    COMBATE("Combate"),
    HUD("HUD"),
    VISUAL("Visual"),
    SERVIDORES("Servidores"),
    MOTOR("Motor Pulsar");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
