package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.MathHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Mods de HUD de uma linha só: CPS, FPS, Latência, Coordenadas, Relógio, Memória e outros. */
public final class SimpleHudMods {
    private SimpleHudMods() {}

    private abstract static class LineMod extends HudMod {
        LineMod(String id, String name, String desc, int x, int y) { super(id, name, desc, Category.HUD, x, y); }
        LineMod(String id, String name, String desc, int x, int y, boolean on) { super(id, name, desc, Category.HUD, x, y, on); }

        abstract String label();
        abstract String value();

        @Override public int width() { return labelWidth(label(), value()); }
        @Override public int height() { return 8; }
        @Override public void render(boolean preview) { drawLabel(label(), value()); }
    }

    /** Frequência CPS: cliques por segundo em Hz, como a rotação de um pulsar. */
    public static class FrequenciaCps extends LineMod {
        public FrequenciaCps() { super("frequencia_cps", "Frequência CPS", "Cliques por segundo em Hz", 5, 86); }
        @Override String label() { return "CPS"; }
        @Override String value() { return ClickTracker.left() + " | " + ClickTracker.right() + " Hz"; }
    }

    public static class Fps extends LineMod {
        public Fps() { super("fps", "FPS", "Quadros por segundo", 5, 100); }
        @Override String label() { return "FPS"; }
        @Override String value() { return String.valueOf(Minecraft.getDebugFPS()); }
    }

    public static class Latencia extends LineMod {
        public Latencia() { super("latencia", "Latência", "Seu ping no servidor", 5, 114); }
        @Override String label() { return "Ping"; }

        @Override
        String value() {
            if (mc.isSingleplayer() || mc.thePlayer == null || mc.getNetHandler() == null) return "local";
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            return info == null ? "?" : info.getResponseTime() + "ms";
        }
    }

    public static class Coordenadas extends LineMod {
        public Coordenadas() { super("coordenadas", "Coordenadas", "Posição X, Y, Z e direção", 5, 128); }
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

    public static class Relogio extends LineMod {
        private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        public Relogio() { super("relogio", "Relógio", "Hora do seu computador", 5, 156, false); }
        @Override String label() { return "Hora"; }
        @Override String value() { return format.format(new Date()); }
    }

    public static class Memoria extends LineMod {
        public Memoria() { super("memoria", "Memória", "Uso de RAM do jogo", 5, 170, false); }
        @Override String label() { return "RAM"; }

        @Override
        String value() {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            long max = rt.maxMemory() / 1048576L;
            return used + "/" + max + " MB (" + (used * 100 / Math.max(1, max)) + "%)";
        }
    }

    public static class Bussola extends LineMod {
        private static final String[] DIRS = {"S", "SO", "O", "NO", "N", "NE", "L", "SE"};
        public Bussola() { super("bussola", "Bússola", "Direção para onde você está olhando", 5, 184, false); }
        @Override String label() { return "Direção"; }

        @Override
        String value() {
            if (mc.thePlayer == null) return "N 180°";
            float yaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
            int deg = Math.round(yaw < 0 ? yaw + 360 : yaw);
            return DIRS[MathHelper.floor_double((deg + 22.5) / 45.0) & 7] + " " + deg + "°";
        }
    }

    public static class Servidor extends LineMod {
        public Servidor() { super("servidor", "Servidor", "Endereço do servidor atual", 5, 198, false); }
        @Override String label() { return "IP"; }

        @Override
        String value() {
            ServerData data = mc.getCurrentServerData();
            return data == null ? "Um jogador" : data.serverIP;
        }
    }

    public static class Saturacao extends LineMod {
        public Saturacao() { super("saturacao", "Saturação", "Mostra a saturação da fome", 5, 212, false); }
        @Override String label() { return "Saturação"; }

        @Override
        String value() {
            if (mc.thePlayer == null) return "5.0";
            return String.format("%.1f", mc.thePlayer.getFoodStats().getSaturationLevel());
        }
    }
}
