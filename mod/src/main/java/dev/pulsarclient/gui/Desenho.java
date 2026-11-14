package dev.pulsarclient.gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Formas simples para o painel e a HUD do Pulsar, sem textura: caixas com cantos arredondados
 * desenhadas em uma chamada só (faixas horizontais), então custam quase o mesmo que um retângulo.
 */
public final class Desenho {
    private Desenho() {}

    /** Caixa com cantos arredondados de raio r (em pixels de interface; 0 = retângulo comum). */
    public static void caixa(float l, float t, float r, float b, int argb, int raio) {
        if (r <= l || b <= t) return;
        raio = Math.max(0, Math.min(raio, (int) Math.min((r - l) / 2, (b - t) / 2)));
        Tessellator tes = Tessellator.getInstance();
        WorldRenderer wr = tes.getWorldRenderer();
        comecar(argb);
        wr.begin(7, DefaultVertexFormats.POSITION);
        if (raio == 0) {
            quad(wr, l, t, r, b);
        } else {
            for (int i = 0; i < raio; i++) {
                double dy = raio - i - 0.5;
                float d = (float) (raio - Math.sqrt(raio * raio - dy * dy));
                quad(wr, l + d, t + i, r - d, t + i + 1);
                quad(wr, l + d, b - i - 1, r - d, b - i);
            }
            quad(wr, l, t + raio, r, b - raio);
        }
        tes.draw();
        terminar();
    }

    /** Caixa arredondada com borda de 1 pixel: a borda é uma caixa um pouco maior por baixo (cantos lisos). */
    public static void caixaBorda(float l, float t, float r, float b, int fill, int border, int raio) {
        caixa(l, t, r, b, border, raio);
        caixa(l + 1, t + 1, r - 1, b - 1, fill, Math.max(0, raio - 1));
    }

    /** Só a borda de uma caixa arredondada, com 1 pixel de espessura. */
    public static void borda(float l, float t, float r, float b, int argb, int raio) {
        if (r <= l || b <= t) return;
        raio = Math.max(0, Math.min(raio, (int) Math.min((r - l) / 2, (b - t) / 2)));
        Tessellator tes = Tessellator.getInstance();
        WorldRenderer wr = tes.getWorldRenderer();
        comecar(argb);
        wr.begin(7, DefaultVertexFormats.POSITION);
        quad(wr, l + raio, t, r - raio, t + 1);
        quad(wr, l + raio, b - 1, r - raio, b);
        quad(wr, l, t + raio, l + 1, b - raio);
        quad(wr, r - 1, t + raio, r, b - raio);
        for (int i = 0; i < raio; i++) {
            double dy = raio - i - 0.5;
            float d = (float) (raio - Math.sqrt(raio * raio - dy * dy));
            float d2 = i + 1 < raio ? (float) (raio - Math.sqrt(raio * raio - (dy - 1) * (dy - 1))) : raio;
            float w = Math.max(1, d2 - d + 0.5f);
            quad(wr, l + d, t + i, l + d + w, t + i + 1);
            quad(wr, r - d - w, t + i, r - d, t + i + 1);
            quad(wr, l + d, b - i - 1, l + d + w, b - i);
            quad(wr, r - d - w, b - i - 1, r - d, b - i);
        }
        tes.draw();
        terminar();
    }

    /**
     * Caixa arredondada com a cor indo da esquerda (corEsq) para a direita (corDir), como as bordas ciano -> violeta
     * dos botões do menu. Mesmas faixas da {@link #caixa}, cada uma com a cor certa nas duas pontas.
     */
    public static void caixaGradiente(float l, float t, float r, float b, int corEsq, int corDir, int raio) {
        if (r <= l || b <= t) return;
        raio = Math.max(0, Math.min(raio, (int) Math.min((r - l) / 2, (b - t) / 2)));
        Tessellator tes = Tessellator.getInstance();
        WorldRenderer wr = tes.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_SMOOTH);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        if (raio == 0) {
            faixa(wr, l, t, r, b, l, r, corEsq, corDir);
        } else {
            for (int i = 0; i < raio; i++) {
                double dy = raio - i - 0.5;
                float d = (float) (raio - Math.sqrt(raio * raio - dy * dy));
                faixa(wr, l + d, t + i, r - d, t + i + 1, l, r, corEsq, corDir);
                faixa(wr, l + d, b - i - 1, r - d, b - i, l, r, corEsq, corDir);
            }
            faixa(wr, l, t + raio, r, b - raio, l, r, corEsq, corDir);
        }
        tes.draw();
        GlStateManager.shadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        terminar();
    }

    /** Borda de 1 pixel com a cor indo da esquerda para a direita, por fora de um miolo de cor lisa. */
    public static void caixaBordaGradiente(float l, float t, float r, float b, int fill, int bordaEsq, int bordaDir, int raio) {
        caixaGradiente(l, t, r, b, bordaEsq, bordaDir, raio);
        caixa(l + 1, t + 1, r - 1, b - 1, fill, Math.max(0, raio - 1));
    }

    private static void faixa(WorldRenderer wr, float l, float t, float r, float b, float x0, float x1, int c0, int c1) {
        int cl = misturar(c0, c1, (l - x0) / Math.max(1f, x1 - x0));
        int cr = misturar(c0, c1, (r - x0) / Math.max(1f, x1 - x0));
        cor(wr.pos(l, b, 0), cl);
        cor(wr.pos(r, b, 0), cr);
        cor(wr.pos(r, t, 0), cr);
        cor(wr.pos(l, t, 0), cl);
    }

    private static void cor(WorldRenderer wr, int argb) {
        wr.color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).endVertex();
    }

    /** Chave liga/desliga em forma de pílula (largura w, altura h). */
    public static void chave(int x, int y, int w, int h, boolean ligado, float hover, int corLigado) {
        int fundo = ligado ? corLigado : misturar(0xFF2A3350, 0xFF36415F, hover);
        caixa(x, y, x + w, y + h, fundo, h / 2);
        int k = h - 4;
        int kx = ligado ? x + w - 2 - k : x + 2;
        caixa(kx, y + 2, kx + k, y + 2 + k, ligado ? 0xFF0A1020 : 0xFFE6ECFF, k / 2);
    }

    /** Mistura duas cores ARGB (f = 0 dá a primeira, 1 a segunda). */
    public static int misturar(int a, int b, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = a >>> s & 255, cb = b >>> s & 255;
            r |= Math.round(ca + (cb - ca) * f) << s;
        }
        return r;
    }

    private static void quad(WorldRenderer wr, float l, float t, float r, float b) {
        wr.pos(l, b, 0).endVertex();
        wr.pos(r, b, 0).endVertex();
        wr.pos(r, t, 0).endVertex();
        wr.pos(l, t, 0).endVertex();
    }

    private static void comecar(int argb) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    private static void terminar() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
