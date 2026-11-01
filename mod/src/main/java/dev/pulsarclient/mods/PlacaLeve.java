package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.IChatComponent;

/**
 * Placas leves do Motor Pulsar: longe, a placa continua aparecendo, mas sem o texto.
 * Em lobby de servidor, com dezenas de placas, o texto de cada uma é desenhado letra por letra a cada quadro;
 * é isso que pesa, e de longe ninguém consegue ler mesmo.
 */
public class PlacaLeve extends TileEntitySignRenderer {
    /** Distância (em blocos) até onde o texto aparece; 0 = sempre. */
    public interface Regra {
        int distanciaTexto();
    }

    private static Regra regra;
    private static long semTexto;
    private final IChatComponent[] guardado = new IChatComponent[4];

    public static void usar(Regra r) {
        regra = r;
    }

    /** Quantas placas já saíram sem texto (teste automático). */
    public static long semTexto() {
        return semTexto;
    }

    @Override
    public void renderTileEntityAt(TileEntitySign placa, double x, double y, double z, float partialTicks, int destroyStage) {
        Regra r = regra;
        int max = r == null ? 0 : r.distanciaTexto();
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (max <= 0 || view == null || destroyStage >= 0 || placa.getDistanceSq(view.posX, view.posY, view.posZ) <= (double) max * max) {
            super.renderTileEntityAt(placa, x, y, z, partialTicks, destroyStage);
            return;
        }
        semTexto++;
        IChatComponent[] texto = placa.signText;
        int n = Math.min(texto.length, guardado.length);
        for (int i = 0; i < n; i++) {
            guardado[i] = texto[i];
            texto[i] = null;
        }
        try {
            super.renderTileEntityAt(placa, x, y, z, partialTicks, destroyStage);
        } finally {
            for (int i = 0; i < n; i++) {
                texto[i] = guardado[i];
                guardado[i] = null;
            }
        }
    }
}
