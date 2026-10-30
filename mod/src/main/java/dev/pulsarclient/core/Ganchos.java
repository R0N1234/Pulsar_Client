package dev.pulsarclient.core;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.EntityLivingBase;

import java.nio.FloatBuffer;

/**
 * Pontos onde os mods do Pulsar entram no código do Minecraft (veja {@link GanchosTransformer}).
 * O jogo chama estes métodos; cada mod preenche a sua parte. Sem mod ligado, o jogo segue normal.
 * Um erro dentro de um mod desliga só aquela parte, e o jogo continua.
 */
public final class Ganchos {
    /** Fonte do Pulsar. Valores negativos (ou {@link #NAO}) querem dizer "o jogo desenha do jeito dele". */
    public interface Fonte {
        boolean ativa(FontRenderer fonte);
        float desenhar(float x, float y, char c, boolean italico);
        /** Largura do caractere em pixels de interface, ou -1 se a fonte não tem esse caractere. */
        float largura(char c);
    }

    /** Cor e intensidade do "vermelho" de quando a entidade leva dano: {r, g, b, a}, ou null para o padrão. */
    public interface CorDano { float[] cor(EntityLivingBase entidade); }

    public interface Balanco { boolean ativo(); }

    public static final int NAO = Integer.MIN_VALUE;

    /** Mesma tabela de caracteres da fonte do Minecraft 1.8.9 (índice usado por renderDefaultChar). */
    private static final String TABELA = "ÀÁÂÈÊËÍÓÔÕÚßãõğİ"
            + "ıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000"
            + "ÇüéâäàåçêëèïîìÄÅ"
            + "ÉæÆôöòûùÿÖÜø£Ø×ƒ"
            + "áíóúñÑªº¿®¬½¼¡«»"
            + "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐"
            + "└┴┬├─┼╞╟╚╔╩╦╠═╬╧"
            + "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀"
            + "αβΓπΣσμτΦΘΩδ∞∅∈∩"
            + "≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000";

    public static volatile Fonte fonte;
    public static volatile CorDano corDano;
    public static volatile Balanco balanco;

    // Quais ganchos entraram no jogo (preenchido pelo GanchosTransformer).
    public static volatile boolean ganchoFonte;
    public static volatile boolean ganchoAvanco;
    public static volatile boolean ganchoBalanco;
    public static volatile boolean ganchoCorDano;

    private Ganchos() {}

    // ---------- fonte ----------

    private static Fonte fonteAtiva(FontRenderer fr) {
        Fonte f = fonte;
        if (f == null) return null;
        try {
            return f.ativa(fr) ? f : null;
        } catch (Throwable t) {
            falhaFonte(t);
            return null;
        }
    }

    /** renderDefaultChar: índice na tabela da fonte do Minecraft. */
    public static float caractere(FontRenderer fr, float x, float y, int indice, boolean italico) {
        if (indice < 0 || indice >= TABELA.length()) return -1f;
        return unicode(fr, x, y, TABELA.charAt(indice), italico);
    }

    /** renderUnicodeChar. */
    public static float unicode(FontRenderer fr, float x, float y, char c, boolean italico) {
        Fonte f = fonteAtiva(fr);
        if (f == null) return -1f;
        try {
            if (f.largura(c) < 0) return -1f;
            float w = f.desenhar(x, y, c, italico);
            return ganchoAvanco ? w : Math.round(w);
        } catch (Throwable t) {
            falhaFonte(t);
            return -1f;
        }
    }

    /** getCharWidth. */
    public static int larguraCaractere(FontRenderer fr, char c) {
        if (c == '§') return NAO;
        Fonte f = fonteAtiva(fr);
        if (f == null) return NAO;
        try {
            float w = f.largura(c);
            return w < 0 ? NAO : Math.round(w);
        } catch (Throwable t) {
            falhaFonte(t);
            return NAO;
        }
    }

    /** getStringWidth: soma as larguras de verdade (com fração) e arredonda só no fim. */
    public static int larguraTexto(FontRenderer fr, String texto) {
        Fonte f = fonteAtiva(fr);
        if (f == null) return NAO;
        if (texto == null) return 0;
        try {
            float total = 0;
            boolean negrito = false;
            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (c == '§' && i + 1 < texto.length()) {
                    char k = Character.toLowerCase(texto.charAt(++i));
                    if (k == 'l') negrito = true;
                    else if (k == 'r' || k >= '0' && k <= '9' || k >= 'a' && k <= 'f') negrito = false;
                    continue;
                }
                float w = f.largura(c);
                if (w < 0) w = fr.getCharWidth(c);
                else if (!ganchoAvanco) w = Math.round(w);
                if (w <= 0) continue;
                total += w;
                if (negrito) total += 1f;
            }
            return Math.round(total);
        } catch (Throwable t) {
            falhaFonte(t);
            return NAO;
        }
    }

    /** Fim de cada letra em renderStringAtPos: o jogo cortava a fração; com a Fonte do Pulsar a fração fica. */
    public static float avanco(float largura) {
        Fonte f = fonte;
        return f != null && fonteLigada ? largura : (float) (int) largura;
    }

    /** Atualizado pela Fonte do Pulsar a cada tick, para o avanço não precisar consultar nada por letra. */
    public static volatile boolean fonteLigada;

    private static void falhaFonte(Throwable t) {
        fonte = null;
        fonteLigada = false;
        System.err.println("[Pulsar] Fonte do Pulsar desligada por erro: " + t);
        t.printStackTrace();
    }

    // ---------- animações clássicas ----------

    /** Balanço do braço enquanto bloqueia, come, bebe ou puxa o arco (como na 1.7). */
    public static float balancoUso(float balanco) {
        Balanco b = Ganchos.balanco;
        try {
            return b != null && b.ativo() ? balanco : 0f;
        } catch (Throwable t) {
            Ganchos.balanco = null;
            System.err.println("[Pulsar] Animações Clássicas desligadas por erro: " + t);
            return 0f;
        }
    }

    // ---------- cor do dano ----------

    public static void corDano(FloatBuffer buffer, EntityLivingBase entidade) {
        CorDano c = corDano;
        if (c == null || entidade == null || entidade.hurtTime <= 0 && entidade.deathTime <= 0) return;
        try {
            float[] rgba = c.cor(entidade);
            if (rgba == null) return;
            for (int i = 0; i < 4; i++) buffer.put(i, rgba[i]);
        } catch (Throwable t) {
            corDano = null;
            System.err.println("[Pulsar] Impacto desligado por erro: " + t);
        }
    }
}
