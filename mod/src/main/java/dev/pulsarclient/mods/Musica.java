package dev.pulsarclient.mods;

import com.google.gson.JsonObject;
import dev.pulsarclient.Ponte;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Música tocando no Spotify, contada pelo launcher (ele lê o título da janela do Spotify).
 * A Barra Pulsar pergunta a cada 2 segundos enquanto está ligada; sem launcher, nada aparece.
 */
final class Musica {
    private static volatile boolean tocando;
    private static volatile String titulo = "";
    private static volatile String artista = "";
    private static volatile String capaId = "";
    private static volatile byte[] capaNova;
    private static ResourceLocation capa;
    private static DynamicTexture textura;
    private static long ultimoPedido;
    private static volatile boolean esperando;
    private static long desde;

    private Musica() {}

    static boolean tocando() { return tocando; }
    static String titulo() { return titulo; }
    static String artista() { return artista; }
    /** Quando a música atual começou (para o letreiro andar do começo). */
    static long desde() { return desde; }

    /** Pergunta ao launcher, no máximo a cada 2 segundos. Chamado pela Barra Pulsar a cada tick. */
    static void atualizar(boolean comCapa) {
        long agora = System.currentTimeMillis();
        // Sem resposta (launcher fechado ou ocupado): depois de 10 s, tenta de novo.
        if (esperando && agora - ultimoPedido > 10000) esperando = false;
        if (esperando || agora - ultimoPedido < 2000 || !Ponte.disponivel()) return;
        ultimoPedido = agora;
        esperando = true;
        JsonObject corpo = new JsonObject();
        corpo.addProperty("capa", comCapa);
        corpo.addProperty("capaId", capaId);
        Ponte.perguntar("/musica", corpo, new Ponte.Resposta() {
            @Override
            public void chegou(JsonObject r) {
                try {
                    boolean t = r.has("tocando") && r.get("tocando").getAsBoolean();
                    String novoTitulo = t && r.has("titulo") ? r.get("titulo").getAsString() : "";
                    if (!novoTitulo.equals(titulo)) {
                        desde = System.currentTimeMillis();
                        if (t) System.out.println("[Pulsar] Barra Pulsar: tocando " + novoTitulo);
                    }
                    titulo = novoTitulo;
                    artista = t && r.has("artista") ? r.get("artista").getAsString() : "";
                    if (r.has("capa")) {
                        capaNova = DatatypeConverter.parseBase64Binary(r.get("capa").getAsString());
                        capaId = r.has("capaId") ? r.get("capaId").getAsString() : "";
                    } else if (!t || !r.has("capaId") || r.get("capaId").getAsString().isEmpty()) {
                        capaId = "";
                    }
                    tocando = t;
                } finally {
                    esperando = false;
                }
            }
        });
    }

    /** Capa da música atual como textura (criada na linha do jogo), ou null. */
    static ResourceLocation capa() {
        byte[] nova = capaNova;
        if (nova != null) {
            capaNova = null;
            apagarCapa();
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(nova));
                if (img != null) {
                    textura = new DynamicTexture(img);
                    capa = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("pulsar_capa", textura);
                    System.out.println("[Pulsar] Barra Pulsar: capa " + img.getWidth() + "x" + img.getHeight());
                }
            } catch (Exception e) {
                System.out.println("[Pulsar] Barra Pulsar: capa inválida (" + e + ")");
            }
        }
        if (capaId.isEmpty() && capa != null) apagarCapa();
        return capa;
    }

    private static void apagarCapa() {
        if (capa != null) Minecraft.getMinecraft().getTextureManager().deleteTexture(capa);
        capa = null;
        textura = null;
    }
}
