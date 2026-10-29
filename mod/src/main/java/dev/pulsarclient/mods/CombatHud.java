package dev.pulsarclient.mods;

/** Combo e Alcance: números de combate que aparecem na HUD. */
public final class CombatHud {
    private CombatHud() {}

    public static class Combo extends HudMod {
        public Combo() { super("combo", "Combo", "Conta quantos hits seguidos você deu", Category.COMBATE, 10000, 190, false); }

        private String value() {
            int c = CombatTracker.combo();
            return c == 0 ? "sem combo" : c + " hit" + (c == 1 ? "" : "s");
        }

        @Override public int width() { return labelWidth("Combo", value()); }
        @Override public int height() { return 8; }
        @Override public void render(boolean preview) { drawLabel("Combo", preview && CombatTracker.combo() == 0 ? "3 hits" : value()); }
    }

    public static class Alcance extends HudMod {
        public Alcance() { super("alcance", "Alcance", "Distância de cada hit que você acerta", Category.COMBATE, 10000, 204, false); }

        private String value() {
            double r = CombatTracker.reach();
            return r < 0 ? "--" : String.format("%.2f blocos", r);
        }

        @Override public int width() { return labelWidth("Alcance", "0.00 blocos"); }
        @Override public int height() { return 8; }
        @Override public void render(boolean preview) { drawLabel("Alcance", preview && CombatTracker.reach() < 0 ? "2.87 blocos" : value()); }
    }
}
