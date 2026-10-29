package dev.pulsarclient.mods;

/** Supernova: brilho máximo, enxerga em cavernas e à noite sem tocha. */
public class Supernova extends PulsarMod {
    private static final float FULL = 100f;
    private float saved = -1;

    public Supernova() {
        super("supernova", "Supernova", "Brilho total, enxerga no escuro", Category.VISUAL, false);
    }

    @Override
    protected void onEnable() {
        if (saved < 0) saved = mc.gameSettings.gammaSetting;
        mc.gameSettings.gammaSetting = FULL;
    }

    @Override
    protected void onDisable() {
        mc.gameSettings.gammaSetting = saved >= 0 && saved <= 1 ? saved : 1f;
        saved = -1;
    }

    @Override
    public void tick() {
        if (isEnabled() && mc.gameSettings.gammaSetting != FULL) onEnable();
    }
}
