package dev.pulsarclient.mods;

import dev.pulsarclient.gui.Desenho;
import dev.pulsarclient.gui.Universo;
import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.ResourceLocation;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Barra Pulsar: um mini painel no topo da tela com o nome do client, FPS, ms e a hora.
 * Com música tocando no Spotify, a barra cresce e mostra a capa, o nome da música e barrinhas mexendo.
 * Tudo em retângulos e texto: o custo é o mesmo de um HUD comum.
 */
public class BarraPulsar extends HudMod {
    private static final int H = 16;
    private static final int TEXT = 0xFFE6ECFF;
    private static final int MUTED = 0xFF8892B0;
    private static final int SEP = 0x337FE7FF;
    private static final int MUSICA_MAX = 110;

    private final BoolSetting mostrarFps = add(new BoolSetting("fps", "Mostrar FPS", true));
    private final BoolSetting mostrarMs = add(new BoolSetting("ms", "Mostrar ms (ping)", true));
    private final BoolSetting mostrarHora = add(new BoolSetting("hora", "Mostrar a hora", true));
    private final BoolSetting mostrarMusica = add(new BoolSetting("musica", "Música do Spotify", true));
    private final BoolSetting capaMusica = add(new BoolSetting("capa", "Capa da música (busca na internet)", true));
    private final SimpleDateFormat hora = new SimpleDateFormat("HH:mm");
    private static long desenhosComMusica;

    public BarraPulsar() {
        super("barra_pulsar", "Barra Pulsar", "Mini painel no topo: client, FPS, ms, hora e a música do Spotify", Category.HUD, CENTRO, 2, true);
    }

    /** Quantas vezes a barra saiu com a música (teste automático). */
    public static long desenhosComMusica() {
        return desenhosComMusica;
    }

    @Override
    public void tick() {
        if (isEnabled() && mostrarMusica.get()) Musica.atualizar(capaMusica.get());
    }

    private boolean comMusica() {
        return mostrarMusica.get() && Musica.tocando() && !Musica.titulo().isEmpty();
    }

    private String fps() {
        return Minecraft.getDebugFPS() + " FPS";
    }

    private String ms() {
        if (mc.isSingleplayer() || mc.thePlayer == null || mc.getNetHandler() == null) return "local";
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return (info == null ? 0 : info.getResponseTime()) + " ms";
    }

    @Override
    public int width() {
        int w = 6 + 12 + font().getStringWidth("Pulsar Client") + 6;
        if (mostrarFps.get()) w += 9 + font().getStringWidth("9999 FPS");
        if (mostrarMs.get()) w += 9 + font().getStringWidth("999 ms");
        if (mostrarHora.get()) w += 9 + font().getStringWidth("00:00");
        if (comMusica()) w += 9 + 14 + Math.min(MUSICA_MAX, font().getStringWidth(textoMusica())) + 4 + 11;
        return w;
    }

    @Override
    public int height() {
        return H;
    }

    private String textoMusica() {
        String a = Musica.artista();
        return a.isEmpty() ? Musica.titulo() : Musica.titulo() + " · " + a;
    }

    @Override
    public void render(boolean preview) {
        int x0 = x;
        // Com a barra de chefe do jogo no topo (servidores usam muito), a barra desce para não ficar por cima.
        int y0 = y + (y < 24 && !preview && bossDoJogo() ? 22 : 0);
        int w = width();
        if (background.get()) {
            Desenho.caixa(x0, y0, x0 + w, y0 + H, 0xD00A0F1E, 5);
            Desenho.borda(x0, y0, x0 + w, y0 + H, SEP, 5);
        }
        int cx = x0 + 5;
        int ty = y0 + 4;
        Universo.icone(cx, y0 + 2, 12);
        cx += 15;
        cx = font().drawStringWithShadow("Pulsar Client", cx, ty, TEXT) + 2;
        int cor = accent();
        if (mostrarFps.get()) cx = segmento(cx, y0, fps(), cor);
        if (mostrarMs.get()) cx = segmento(cx, y0, ms(), cor);
        if (mostrarHora.get()) cx = segmento(cx, y0, hora.format(new Date()), TEXT);
        if (comMusica()) {
            desenhosComMusica++;
            separador(cx, y0);
            cx += 6;
            ResourceLocation capa = capaMusica.get() ? Musica.capa() : null;
            if (capa != null) {
                mc.getTextureManager().bindTexture(capa);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
                Gui.drawModalRectWithCustomSizedTexture(cx, y0 + 2, 0, 0, 12, 12, 12, 12);
            } else {
                Desenho.caixa(cx, y0 + 2, cx + 12, y0 + 14, 0xFF1DB954, 2);
                font().drawString("♪", cx + 3, y0 + 4, 0xFF0A1020);
            }
            cx += 15;
            cx = font().drawStringWithShadow(letreiro(textoMusica()), cx, ty, TEXT) + 3;
            barrinhas(cx, y0);
        }
    }

    private int segmento(int cx, int y0, String texto, int cor) {
        separador(cx, y0);
        return font().drawStringWithShadow(texto, cx + 6, y0 + 4, cor) + 2;
    }

    private void separador(int cx, int y0) {
        Gui.drawRect(cx + 1, y0 + 4, cx + 2, y0 + H - 4, SEP);
    }

    /** Nome longo anda como letreiro (uma letra por vez), começando do início quando a música muda. */
    private String letreiro(String s) {
        if (font().getStringWidth(s) <= MUSICA_MAX) return s;
        String volta = s + "     ";
        long t = System.currentTimeMillis() - Musica.desde();
        // Para 2 s no começo e depois anda 4 letras por segundo.
        int passo = t < 2000 ? 0 : (int) ((t - 2000) / 250 % volta.length());
        String girado = volta.substring(passo) + volta.substring(0, passo);
        return font().trimStringToWidth(girado, MUSICA_MAX);
    }

    /** Três barrinhas subindo e descendo, como um equalizador, na cor do Spotify. */
    private void barrinhas(int cx, int y0) {
        double t = System.currentTimeMillis() / 1000.0;
        double[] vel = {7.1, 9.3, 6.2};
        double[] fase = {0, 1.7, 3.1};
        for (int i = 0; i < 3; i++) {
            int h = 2 + (int) Math.round(6 * Math.abs(Math.sin(t * vel[i] + fase[i])));
            int bx = cx + i * 3;
            Gui.drawRect(bx, y0 + 12 - h, bx + 2, y0 + 12, 0xFF1DB954);
        }
    }

    private static boolean bossDoJogo() {
        return BossStatus.bossName != null && BossStatus.statusBarTime > 0 && !BarraChefe.ativa();
    }
}
