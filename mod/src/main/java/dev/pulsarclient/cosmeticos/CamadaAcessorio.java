package dev.pulsarclient.cosmeticos;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Desenha o acessório do jogador, preso na cabeça ou nas costas do boneco. Tudo é geometria simples com cor
 * (sem textura), com brilho próprio, para pesar quase nada.
 *
 * Espaço da cabeça (depois de bipedHead.postRender): 1 = um bloco; a cabeça vai de y -0,5 (topo) a 0, de x e z
 * -0,25 a 0,25, e o rosto fica no lado z negativo. Espaço do corpo: de y 0 (ombros) a 0,75, costas em z +0,125.
 */
final class CamadaAcessorio implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;

    CamadaAcessorio(RenderPlayer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbAmount, float partialTicks, float age,
                              float headYaw, float headPitch, float scale) {
        Cosmeticos c = Cosmeticos.get();
        if (c == null || p.isInvisible()) return;
        String id = c.acessorioDe(p);
        if (id.isEmpty()) return;
        float tempo = (p.ticksExisted + partialTicks) / 20f;
        Cosmeticos.desenhou();

        float bx = OpenGlHelper.lastBrightnessX, by = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        try {
            if (id.equals("asas")) {
                renderer.getMainModel().bipedBody.postRender(0.0625f);
                asas(tempo, !p.onGround);
            } else {
                if (p.isSneaking()) GlStateManager.translate(0, 0.2f, 0);
                renderer.getMainModel().bipedHead.postRender(0.0625f);
                if (id.equals("orbita")) orbita(tempo);
                else if (id.equals("coroa")) coroa(tempo);
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }

    // ---------- Órbita Pulsar (grátis) ----------

    /** Um núcleo azul girando sobre a cabeça, o feixe do pulsar e duas órbitas inclinadas, como no logo. */
    private static void orbita(float t) {
        GlStateManager.translate(0, -0.78f + (float) Math.sin(t * 2) * 0.025f, 0);
        GlStateManager.depthMask(false);
        GlStateManager.pushMatrix();
        GlStateManager.rotate(t * 40, 0, 1, 0);
        for (int i = 0; i < 2; i++) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(i == 0 ? 24 : -24, 1, 0, 0.3f);
            GlStateManager.rotate(t * (i == 0 ? 90 : -70), 0, 1, 0);
            anel(0.33f + i * 0.04f, 0.022f, i == 0 ? 0x9B7CFF : 0x6F8CFF, 0.85f);
            GlStateManager.popMatrix();
        }
        GlStateManager.popMatrix();

        // Feixe: duas faixas cruzadas, somando luz.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.pushMatrix();
        GlStateManager.rotate(t * 120, 0, 1, 0);
        GlStateManager.rotate(35, 0, 0, 1);
        for (int i = 0; i < 2; i++) {
            GlStateManager.rotate(90, 0, 1, 0);
            feixe(0.42f, 0.035f, 0x7FE7FF);
        }
        GlStateManager.popMatrix();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);

        GlStateManager.pushMatrix();
        GlStateManager.rotate(t * 160, 0, 1, 0);
        GlStateManager.rotate(45, 1, 0, 1);
        caixa(-0.045f, -0.045f, -0.045f, 0.045f, 0.045f, 0.045f, 0xBFF6FF, 1f);
        GlStateManager.popMatrix();
    }

    // ---------- Coroa Estelar ----------

    private static void coroa(float t) {
        float h = 0.27f, base = -0.47f, topo = -0.59f;
        int ouro = 0xF2C744, sombra = 0xB8902A;
        // Faixa da coroa em volta da cabeça, por fora e por dentro.
        for (int lado = 0; lado < 4; lado++) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(lado * 90, 0, 1, 0);
            quad(-h, topo, -h, h, topo, -h, h, base, -h, -h, base, -h, ouro, 1f);
            quad(-h + 0.01f, topo, -h + 0.01f, h - 0.01f, topo, -h + 0.01f, h - 0.01f, base, -h + 0.01f, -h + 0.01f, base, -h + 0.01f, sombra, 1f);
            // Três pontas de estrela por lado; a do meio é maior.
            for (int i = 0; i < 3; i++) {
                float cx = (i - 1) * 0.18f;
                float alto = i == 1 ? 0.15f : 0.1f;
                tri(cx - 0.07f, topo, -h, cx + 0.07f, topo, -h, cx, topo - alto, -h, ouro, 1f);
            }
            GlStateManager.popMatrix();
        }
        // Gema azul na frente e roxas nos lados, piscando de leve.
        float brilho = 0.8f + 0.2f * (float) Math.sin(t * 3);
        caixa(-0.035f, base - 0.085f, -h - 0.025f, 0.035f, base - 0.015f, -h + 0.005f, 0x4AA8FF, brilho);
        caixa(-h - 0.025f, base - 0.075f, -0.03f, -h + 0.005f, base - 0.02f, 0.03f, 0x9B7CFF, brilho);
        caixa(h - 0.005f, base - 0.075f, -0.03f, h + 0.025f, base - 0.02f, 0.03f, 0x9B7CFF, brilho);
    }

    // ---------- Asas de Nebulosa ----------

    private static void asas(float t, boolean voando) {
        float bate = (float) Math.sin(t * (voando ? 9 : 2.6f)) * (voando ? 28 : 12);
        GlStateManager.depthMask(false);
        for (int lado = -1; lado <= 1; lado += 2) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(lado * 0.06f, 0.18f, 0.14f);
            GlStateManager.rotate(lado * (25 + bate), 0, 1, 0);
            asa(lado);
            GlStateManager.popMatrix();
        }
        GlStateManager.depthMask(true);
    }

    /** Uma asa em leque: cinco penas saindo da raiz, roxas perto do corpo e azuis nas pontas. */
    private static void asa(int lado) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        float[][] pontas = {{0.95f, -0.45f}, {0.98f, -0.15f}, {0.85f, 0.15f}, {0.62f, 0.4f}, {0.35f, 0.55f}};
        for (int i = 0; i < pontas.length - 1; i++) {
            float[] a = pontas[i], b = pontas[i + 1];
            vertice(wr, 0, 0, 0, 0x9B7CFF, 0.75f);
            vertice(wr, lado * a[0], a[1], 0.12f * a[0], 0x7FE7FF, 0.35f);
            vertice(wr, lado * b[0], b[1], 0.12f * b[0], 0x7FE7FF, 0.35f);
        }
        tess.draw();
    }

    // ---------- formas ----------

    private static void anel(float raio, float largura, int cor, float alfa) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int n = 40;
        for (int i = 0; i <= n; i++) {
            double a = i * Math.PI * 2 / n;
            float cx = (float) Math.cos(a), cz = (float) Math.sin(a);
            // A órbita some e volta pela volta, como um rastro.
            float f = alfa * (0.35f + 0.65f * (float) i / n);
            vertice(wr, cx * (raio - largura), 0, cz * (raio - largura), cor, f);
            vertice(wr, cx * (raio + largura), 0, cz * (raio + largura), cor, f);
        }
        tess.draw();
    }

    private static void feixe(float comprimento, float largura, int cor) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int s = -1; s <= 1; s += 2) {
            vertice(wr, -largura, 0, 0, cor, 0.7f);
            vertice(wr, largura, 0, 0, cor, 0.7f);
            vertice(wr, 0, s * comprimento, 0, cor, 0f);
        }
        tess.draw();
    }

    private static void caixa(float x0, float y0, float z0, float x1, float y1, float z1, int cor, float luz) {
        int r = (int) ((cor >> 16 & 255) * luz), g = (int) ((cor >> 8 & 255) * luz), b = (int) ((cor & 255) * luz);
        int c = r << 16 | g << 8 | b;
        quad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, c, 1f);
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, c, 1f);
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, c, 1f);
        quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, c, 1f);
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, c, 1f);
        quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, c, 1f);
    }

    private static void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                             float dx, float dy, float dz, int cor, float alfa) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertice(wr, ax, ay, az, cor, alfa);
        vertice(wr, bx, by, bz, cor, alfa);
        vertice(wr, cx, cy, cz, cor, alfa);
        vertice(wr, dx, dy, dz, cor, alfa);
        tess.draw();
    }

    private static void tri(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, int cor, float alfa) {
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        vertice(wr, ax, ay, az, cor, alfa);
        vertice(wr, bx, by, bz, cor, alfa);
        vertice(wr, cx, cy, cz, cor, alfa);
        tess.draw();
    }

    private static void vertice(WorldRenderer wr, float x, float y, float z, int cor, float alfa) {
        wr.pos(x, y, z).color((cor >> 16 & 255) / 255f, (cor >> 8 & 255) / 255f, (cor & 255) / 255f, alfa).endVertex();
    }
}
