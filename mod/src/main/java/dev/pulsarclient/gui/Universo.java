package dev.pulsarclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Fundo do universo do Pulsar para os menus: um céu de 360° (Via Láctea, nebulosas e Andrômeda) que gira bem devagar,
 * os planetas passando na frente (mais rápido, porque estão mais perto) e um pulsar girando com os feixes de luz.
 * São só uns 12 quadrados com textura por quadro, quase o mesmo custo do fundo parado de antes.
 */
public final class Universo {
    public static final ResourceLocation CEU = new ResourceLocation("pulsar", "textures/gui/ceu.png");
    public static final ResourceLocation PLANETAS = new ResourceLocation("pulsar", "textures/gui/planetas.png");
    public static final ResourceLocation PULSAR = new ResourceLocation("pulsar", "textures/gui/pulsar.png");
    public static final ResourceLocation LOGO = new ResourceLocation("pulsar", "textures/gui/logo.png");
    // Ícone PC pequeno (32x32), para o nome dos jogadores e o TAB.
    public static final ResourceLocation ICONE = new ResourceLocation("pulsar", "textures/gui/icone.png");
    private static final int CW = 4096;
    private static final int CH = 1024;
    /** Uma volta inteira no céu a cada 4 minutos. */
    private static final double VOLTA_MS = 240000;
    /** Onde o pulsar fica no céu (fração da largura e da altura da imagem). */
    private static final float PULSAR_U = 0.6f;
    private static final float PULSAR_V = 0.3f;
    /** Planetas: posição na volta, altura (fração da tela), tamanho (fração da altura da tela) e paralaxe. */
    private static final float[][] ORBITAS = {
            {0.30f, 0.70f, 0.035f, 1.20f}, // Mercúrio
            {0.42f, 0.22f, 0.050f, 1.30f}, // Vênus
            {0.55f, 0.80f, 0.070f, 1.45f}, // Terra
            {0.68f, 0.18f, 0.045f, 1.35f}, // Marte
            {0.08f, 0.74f, 0.130f, 1.70f}, // Júpiter
            {0.84f, 0.62f, 0.140f, 1.60f}, // Saturno
            {0.94f, 0.26f, 0.060f, 1.25f}, // Urano
            {0.18f, 0.14f, 0.055f, 1.20f}, // Netuno
    };
    private static final long INICIO = Minecraft.getSystemTime();
    private static long quadros;

    private Universo() {}

    /** Quantas vezes o universo foi desenhado (teste automático). */
    public static long quadros() {
        return quadros;
    }

    /** Desenha o universo cobrindo a tela inteira (em coordenadas da interface). */
    public static void draw(int width, int height) {
        if (quadros++ == 0) System.out.println("[Pulsar] Universo: céu 360 girando");
        long ms = Minecraft.getSystemTime() - INICIO;
        double voltas = ms / VOLTA_MS;
        float giro = (float) (voltas - Math.floor(voltas));
        // A imagem cobre a altura da tela; em telas muito largas, cobre a largura.
        float s = Math.max((float) height / CH, (float) width / CW);
        float volta = CW * s;
        float alto = CH * s;
        float topo = (height - alto) / 2;
        Tessellator tes = Tessellator.getInstance();
        WorldRenderer wr = tes.getWorldRenderer();

        bind(CEU);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager.disableBlend();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        quad(wr, 0, topo, width, topo + alto, giro, 0, giro + width / volta, 1);
        tes.draw();

        GlStateManager.enableBlend();
        pulsar(tes, wr, wrap(PULSAR_U - giro, volta, width, height * 0.6f), topo + PULSAR_V * alto, width, height, ms);

        bind(PLANETAS);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        for (int i = 0; i < ORBITAS.length; i++) {
            float[] o = ORBITAS[i];
            float tam = o[2] * height;
            float x = wrap(o[0] - giro, volta * o[3], width, tam);
            if (x < -tam || x > width + tam) continue;
            float y = o[1] * height;
            quad(wr, x - tam / 2, y - tam / 2, x + tam / 2, y + tam / 2, i / 8f, 0, (i + 1) / 8f, 1);
        }
        tes.draw();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Pulsar: o eixo magnético gira em volta do eixo de rotação (inclinado), então os feixes varrem o espaço
     * como um farol, e o núcleo pisca quando um feixe aponta para a gente.
     */
    private static void pulsar(Tessellator tes, WorldRenderer wr, float px, float py, int width, int height, long ms) {
        float nucleo = height * 0.07f;
        if (px < -height * 0.6f || px > width + height * 0.6f) return;
        double w = ms / 2600.0 * Math.PI * 2;
        double beta = Math.toRadians(55);
        double ax = Math.sin(beta) * Math.cos(w);
        double ay = Math.cos(beta);
        double az = Math.sin(beta) * Math.sin(w);
        float comp = (float) Math.hypot(ax, ay);
        float ang = (float) Math.toDegrees(Math.atan2(ax, ay)) - 20f;
        float flash = (float) Math.pow(Math.abs(az) / Math.sin(beta), 10);

        bind(PULSAR);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float meio = height * 0.42f * comp;
        float larg = nucleo * 0.8f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(px, py, 0);
        GlStateManager.rotate(ang, 0, 0, 1);
        GlStateManager.color(1f, 1f, 1f, 0.75f + 0.25f * flash);
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        quad(wr, -larg / 2, -meio, larg / 2, meio, 0.5f, 0, 0.75f, 1);
        tes.draw();
        GlStateManager.popMatrix();
        float n = nucleo * (1f + 0.5f * flash);
        GlStateManager.color(1f, 1f, 1f, 0.8f + 0.2f * flash);
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        quad(wr, px - n / 2, py - n / 2, px + n / 2, py + n / 2, 0, 0, 0.5f, 0.5f);
        tes.draw();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Posição na tela de algo que está na fração u da volta (já descontado o giro), dando a volta quando passa. */
    private static float wrap(float u, float volta, int width, float margem) {
        float x = (u - (float) Math.floor(u)) * volta;
        if (x > width + margem) x -= volta;
        return x;
    }

    private static void quad(WorldRenderer wr, float l, float t, float r, float b, float u0, float v0, float u1, float v1) {
        wr.pos(l, b, 0).tex(u0, v1).endVertex();
        wr.pos(r, b, 0).tex(u1, v1).endVertex();
        wr.pos(r, t, 0).tex(u1, v0).endVertex();
        wr.pos(l, t, 0).tex(u0, v0).endVertex();
    }

    /** Logo "PC" no círculo, quadrado de lado size. */
    public static void logo(int x, int y, int size) {
        bind(LOGO);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }

    /** Ícone PC pequeno, quadrado de lado size. */
    public static void icone(int x, int y, int size) {
        bind(ICONE);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, size, size, size, size);
    }

    private static void bind(ResourceLocation tex) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
