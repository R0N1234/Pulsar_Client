package dev.pulsarclient.gui;

import dev.pulsarclient.mods.Category;
import dev.pulsarclient.mods.PulsarMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Ícones do painel do Pulsar: desenhos brancos numa imagem só (tools/icones.py), pintados na cor pedida.
 * Cada ícone é um quadrado com textura, então custa o mesmo que uma letra.
 */
public final class Icones {
    private static final ResourceLocation ATLAS = new ResourceLocation("pulsar", "textures/gui/icones.png");
    public static final int ESTRELA = 0, ESTRELA_VAZIA = 1, ENGRENAGEM = 2, LUPA = 3, TECLADO = 4, MOUSE = 5, ESPADA = 6,
            ESCUDO = 7, POCAO = 8, MIRA = 9, MONITOR = 10, SINAL = 11, BUSSOLA = 12, RELOGIO = 13, CHIP = 14, GLOBO = 15,
            LISTA = 16, CORACAO = 17, OLHO = 18, TELESCOPIO = 19, ORBITA = 20, COMETA = 21, PESSOA = 22, CUBO = 23,
            BRILHO = 24, NUVEM = 25, SETA_BAIXO = 26, GOTA = 27, BALAO = 28, NOTA = 29, RAIO = 30, QUADRO = 31,
            LETRA = 32, BARRA = 33, BOMBA = 34, AMPULHETA = 35, MOCHILA = 36, IMPACTO = 37, CAIXA = 38, VOLTAR = 39,
            AMIGOS = 40, MEDALHA = 41, GRADE = 42, PINCEL = 43, CAMERA = 44;
    private static final Map<String, Integer> DOS_MODS = new HashMap<String, Integer>();

    static {
        String[][] lista = {
                {"teclas_pulse", "4"}, {"frequencia_cps", "5"}, {"fps", "10"}, {"latencia", "11"}, {"coordenadas", "12"},
                {"bussola", "12"}, {"relogio", "13"}, {"cronometro", "35"}, {"memoria", "14"}, {"servidor", "15"},
                {"saturacao", "17"}, {"propulsao", "30"}, {"mira_pulsar", "9"}, {"blindagem", "7"}, {"efeitos", "8"},
                {"combo", "37"}, {"alcance", "6"}, {"tremor_dano", "37"}, {"campo_colisao", "38"}, {"contador_itens", "36"},
                {"contador_tnt", "34"}, {"recargas", "35"}, {"anti_batata", "35"}, {"filtro_coleta", "36"}, {"focar_jogador", "9"}, {"sequencias", "37"}, {"gravacao", "44"}, {"animacoes_classicas", "6"}, {"impacto", "37"},
                {"wtap_blockhit", "6"}, {"inventario", "36"}, {"telescopio", "19"}, {"orbita_livre", "20"},
                {"meu_nick", "22"}, {"poeira_estelar", "24"}, {"supernova", "18"}, {"contorno", "23"}, {"atmosfera", "25"},
                {"placar", "16"}, {"lista_tab", "16"}, {"fonte_pulsar", "32"}, {"cores_vivas", "43"}, {"rastro_cometa", "21"},
                {"atalhos_texto", "28"}, {"gg_automatico", "28"}, {"sinal_discord", "28"}, {"radio_pulsar", "29"},
                {"nebulosa", "27"}, {"gravidade_itens", "26"}, {"capturas", "44"}, {"icone_pulsar", "41"}, {"nivel", "41"},
                {"amigos", "40"}, {"motor_pulsar", "30"}, {"barra_pulsar", "29"}, {"barra_chefe", "33"},
                {"pacote_ativo", "31"}, {"aura_encanto", "24"},
        };
        for (String[] l : lista) DOS_MODS.put(l[0], Integer.parseInt(l[1]));
    }

    private Icones() {}

    /** Ícone de um mod (ou o da categoria dele, se o mod não tem um próprio). */
    public static int doMod(PulsarMod mod) {
        Integer i = DOS_MODS.get(mod.id());
        return i != null ? i : daCategoria(mod.category());
    }

    public static int daCategoria(Category c) {
        if (c == null) return GRADE;
        switch (c) {
            case COMBATE: return ESPADA;
            case HUD: return MONITOR;
            case VISUAL: return OLHO;
            case SERVIDORES: return GLOBO;
            case SOCIAL: return AMIGOS;
            case MOTOR: return RAIO;
            default: return GRADE;
        }
    }

    /** Desenha o ícone i num quadrado de lado tam, na cor argb. */
    public static void desenhar(int i, float x, float y, float tam, int argb) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ATLAS);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color((argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
        float u0 = (i % 8) / 8f, v0 = (i / 8) / 8f, d = 1 / 8f;
        Tessellator tes = Tessellator.getInstance();
        WorldRenderer wr = tes.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + tam, 0).tex(u0, v0 + d).endVertex();
        wr.pos(x + tam, y + tam, 0).tex(u0 + d, v0 + d).endVertex();
        wr.pos(x + tam, y, 0).tex(u0 + d, v0).endVertex();
        wr.pos(x, y, 0).tex(u0, v0).endVertex();
        tes.draw();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
