package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

/** Mods de HUD de uma linha só: Frequência CPS, FPS, Latência e Coordenadas. */
public final class SimpleHudMods {
    private SimpleHudMods() {}

    private abstract static class LineMod extends PulsarMod {
        LineMod(String id, String name, int x, int y) { super(id, name, x, y); }

        abstract String label();
        abstract String value();

        @Override public int width() { return labelWidth(label(), value()); }
        @Override public int height() { return 8; }
        @Override public void render(boolean preview) { drawLabel(label(), value()); }
    }

    /** Frequência CPS: cliques por segundo em Hz, como a rotação de um pulsar. */
    public static class FrequenciaCps extends LineMod {
        public FrequenciaCps() { super("frequencia_cps", "Frequência CPS", 5, 86); }
        @Override String label() { return "CPS"; }
        @Override String value() { return ClickTracker.left() + " | " + ClickTracker.right() + " Hz"; }
    }

    public static class Fps extends LineMod {
        public Fps() { super("fps", "FPS", 5, 100); }
        @Override String label() { return "FPS"; }
        @Override String value() { return String.valueOf(Minecraft.getDebugFPS()); }
    }

    public static class Latencia extends LineMod {
        public Latencia() { super("latencia", "Latência", 5, 114); }
        @Override String label() { return "Ping"; }

        @Override
        String value() {
            if (mc.isSingleplayer() || mc.thePlayer == null || mc.getNetHandler() == null) return "local";
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            return info == null ? "?" : info.getResponseTime() + "ms";
        }
    }

    public static class Coordenadas extends LineMod {
        public Coordenadas() { super("coordenadas", "Coordenadas", 5, 128); }
        @Override String label() { return "XYZ"; }

        @Override
        String value() {
            if (mc.thePlayer == null) return "0 / 64 / 0 N";
            return (int) Math.floor(mc.thePlayer.posX) + " / " + (int) Math.floor(mc.thePlayer.posY) + " / "
                    + (int) Math.floor(mc.thePlayer.posZ) + " " + facing();
        }

        private String facing() {
            switch (mc.thePlayer.getHorizontalFacing()) {
                case NORTH: return "N";
                case SOUTH: return "S";
                case EAST: return "L";
                default: return "O";
            }
        }
    }
}
