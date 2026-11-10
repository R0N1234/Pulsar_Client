package dev.pulsarclient.mods;

import dev.pulsarclient.core.Ganchos;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Fonte do Pulsar: troca a fonte de todo o jogo (menus, chat, HUD, nomes, placas, itens e textos de outros mods)
 * pela Exo 2, uma fonte lisa com cara de espaço. Desligando, volta a fonte do Minecraft.
 *
 * As letras vêm de um atlas pronto (tools/fonte_pulsar.py), desenhado 4x maior e com mipmaps,
 * então ficam nítidas em qualquer escala de interface. O desenho entra pelos ganchos do FontRenderer
 * ({@link dev.pulsarclient.core.GanchosTransformer}); letras que a fonte não tem continuam com a do jogo.
 */
public class FontePulsar extends PulsarMod implements Ganchos.Fonte {
    private static final String ATLAS = "/assets/pulsar/fonte/atlas.png";
    private static final String GLIFOS = "/assets/pulsar/fonte/glifos.txt";

    /** Glifo: posição no atlas (u, v), tamanho e deslocamento em pixels de interface e o avanço até a próxima letra. */
    private static final class Glifo {
        float u0, v0, u1, v1, ox, oy, w, h, avanco;
    }

    private final Glifo[] glifos = new Glifo[65536];
    private final FloatBuffer cor = BufferUtils.createFloatBuffer(16);
    private int textura = -1;
    private boolean carregada;
    private boolean falhou;
    private Thread threadDoJogo;
    private boolean ligadaAntes;

    public FontePulsar() {
        super("fonte_pulsar", "Fonte do Pulsar", "Troca a fonte de todo o jogo pela fonte do Pulsar", Category.VISUAL, true);
        Ganchos.fonte = this;
    }

    @Override
    public void tick() {
        if (isEnabled() && !carregada && !falhou) carregar();
        boolean ligada = isEnabled() && carregada && Ganchos.ganchoFonte;
        Ganchos.fonteLigada = ligada;
        if (ligada != ligadaAntes) {
            ligadaAntes = ligada;
            // As linhas do chat foram quebradas com a largura da fonte antiga.
            if (mc.ingameGUI != null) mc.ingameGUI.getChatGUI().refreshChat();
        }
    }

    // ---------- Ganchos.Fonte ----------

    @Override
    public boolean ativa(FontRenderer fonte) {
        // Só na thread do jogo: a tela de carregamento do Forge desenha texto numa thread própria.
        return Ganchos.fonteLigada && Thread.currentThread() == threadDoJogo && fonte != mc.standardGalacticFontRenderer;
    }

    @Override
    public float largura(char c) {
        Glifo g = glifos[c];
        return g == null ? -1f : g.avanco;
    }

    @Override
    public float desenhar(float x, float y, char c, boolean italico) {
        Glifo g = glifos[c];
        if (g.w <= 0) return g.avanco;
        // Texto desenhado sem transparência (o jogo faz isso no placar, por exemplo) continua opaco,
        // mas com a borda lisa: liga a transparência só para esta letra, com a cor sem alfa.
        boolean misturaLigada = EstadoGl.misturaLigada();
        if (!misturaLigada) {
            EstadoGl.corAtual(cor);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(cor.get(0), cor.get(1), cor.get(2), 1f);
        }
        GlStateManager.bindTexture(textura);
        float x0 = x + g.ox;
        float y0 = y + g.oy;
        float x1 = x0 + g.w;
        float y1 = y0 + g.h;
        // Itálico igual ao do jogo: o topo da linha vai 1 pixel para a direita e a base 1 para a esquerda.
        float s0 = italico ? 1f - g.oy / 4f : 0f;
        float s1 = italico ? 1f - (g.oy + g.h) / 4f : 0f;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(g.u0, g.v0);
        GL11.glVertex3f(x0 + s0, y0, 0f);
        GL11.glTexCoord2f(g.u0, g.v1);
        GL11.glVertex3f(x0 + s1, y1, 0f);
        GL11.glTexCoord2f(g.u1, g.v0);
        GL11.glVertex3f(x1 + s0, y0, 0f);
        GL11.glTexCoord2f(g.u1, g.v1);
        GL11.glVertex3f(x1 + s1, y1, 0f);
        GL11.glEnd();
        if (!misturaLigada) {
            GlStateManager.disableBlend();
            GlStateManager.color(cor.get(0), cor.get(1), cor.get(2), cor.get(3));
        }
        return g.avanco;
    }

    // ---------- carregamento ----------

    private void carregar() {
        try {
            BufferedImage img = lerImagem();
            int w = img.getWidth();
            int h = img.getHeight();
            int lidos = lerGlifos(w, h);
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int p : px) buf.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
            buf.flip();
            textura = GL11.glGenTextures();
            GlStateManager.bindTexture(textura);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            GlStateManager.bindTexture(0);
            threadDoJogo = Thread.currentThread();
            carregada = true;
            System.out.println("[Pulsar] Fonte do Pulsar carregada: " + lidos + " letras" + (Ganchos.ganchoAvanco ? ", espaçamento com fração" : ""));
        } catch (Throwable t) {
            falhou = true;
            System.err.println("[Pulsar] A Fonte do Pulsar não carregou, fica a do Minecraft: " + t);
        }
    }

    private BufferedImage lerImagem() throws Exception {
        InputStream in = FontePulsar.class.getResourceAsStream(ATLAS);
        if (in == null) throw new IllegalStateException("atlas não encontrado");
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }

    private int lerGlifos(int w, int h) throws Exception {
        InputStream in = FontePulsar.class.getResourceAsStream(GLIFOS);
        if (in == null) throw new IllegalStateException("tabela de letras não encontrada");
        int lidos = 0;
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        try {
            String linha;
            while ((linha = r.readLine()) != null) {
                if (linha.isEmpty() || linha.startsWith("#")) continue;
                String[] p = linha.split(" ");
                int c = Integer.parseInt(p[0]);
                if (c <= 0 || c >= glifos.length || p.length < 8) continue;
                // Medidas do atlas em texels; o desenho é em pixels de interface (4 texels por pixel).
                float x = Float.parseFloat(p[1]);
                float y = Float.parseFloat(p[2]);
                float gw = Float.parseFloat(p[3]);
                float gh = Float.parseFloat(p[4]);
                Glifo g = new Glifo();
                g.u0 = x / w;
                g.v0 = y / h;
                g.u1 = (x + gw) / w;
                g.v1 = (y + gh) / h;
                g.w = gw / 4f;
                g.h = gh / 4f;
                g.ox = Float.parseFloat(p[5]);
                g.oy = Float.parseFloat(p[6]);
                g.avanco = Float.parseFloat(p[7]);
                glifos[c] = g;
                lidos++;
            }
        } finally {
            r.close();
        }
        return lidos;
    }

    /**
     * Estado do OpenGL que o jogo já guarda no GlStateManager. Perguntar ao driver (glIsEnabled, glGetFloat)
     * a cada letra fazia a placa de vídeo parar e esperar: com placar, TAB e HUD na tela eram centenas de
     * esperas por quadro, e isso derrubava o FPS. Se a leitura do GlStateManager falhar, pergunta ao driver como antes.
     */
    static final class EstadoGl {
        private static java.lang.reflect.Field mistura, misturaLigada, cor;
        private static java.lang.reflect.Field[] rgba;
        private static boolean pronto, falhou;

        private static boolean preparar() {
            if (pronto || falhou) return pronto;
            try {
                for (java.lang.reflect.Field f : GlStateManager.class.getDeclaredFields()) {
                    String tipo = f.getType().getName();
                    if (tipo.endsWith("$BlendState")) mistura = f;
                    else if (tipo.endsWith("$Color")) cor = f;
                }
                mistura.setAccessible(true);
                cor.setAccessible(true);
                Object blend = mistura.get(null);
                for (java.lang.reflect.Field f : blend.getClass().getDeclaredFields()) {
                    if (f.getType().getName().endsWith("$BooleanState")) {
                        f.setAccessible(true);
                        Object estado = f.get(blend);
                        for (java.lang.reflect.Field b : estado.getClass().getDeclaredFields()) {
                            if (b.getType() == boolean.class) {
                                b.setAccessible(true);
                                misturaLigada = b;
                            }
                        }
                        mistura = f;
                        break;
                    }
                }
                java.util.List<java.lang.reflect.Field> floats = new java.util.ArrayList<java.lang.reflect.Field>();
                for (java.lang.reflect.Field f : cor.getType().getDeclaredFields()) {
                    if (f.getType() == float.class) {
                        f.setAccessible(true);
                        floats.add(f);
                    }
                }
                if (misturaLigada == null || floats.size() != 4) throw new IllegalStateException("campos " + misturaLigada + ", " + floats.size());
                rgba = floats.toArray(new java.lang.reflect.Field[4]);
                misturaLigada.getBoolean(mistura.get(EstadoGl.blend()));
                pronto = true;
                System.out.println("[Pulsar] Fonte: estado do OpenGL lido do jogo, sem perguntar ao driver");
            } catch (Throwable t) {
                falhou = true;
                System.err.println("[Pulsar] Fonte: estado do OpenGL pelo driver (" + t + ")");
            }
            return pronto;
        }

        private static Object blend() throws IllegalAccessException {
            for (java.lang.reflect.Field f : GlStateManager.class.getDeclaredFields()) {
                if (f.getType().getName().endsWith("$BlendState")) {
                    f.setAccessible(true);
                    return f.get(null);
                }
            }
            return null;
        }

        private static Object blendCache;

        static boolean misturaLigada() {
            if (preparar()) {
                try {
                    if (blendCache == null) blendCache = mistura.get(blend());
                    return misturaLigada.getBoolean(blendCache);
                } catch (Throwable t) {
                    falhou = true;
                    pronto = false;
                }
            }
            return GL11.glIsEnabled(GL11.GL_BLEND);
        }

        static void corAtual(FloatBuffer destino) {
            destino.clear();
            if (preparar()) {
                try {
                    Object c = cor.get(null);
                    for (int i = 0; i < 4; i++) destino.put(i, rgba[i].getFloat(c));
                    return;
                } catch (Throwable t) {
                    falhou = true;
                    pronto = false;
                }
            }
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, destino);
        }
    }
}
