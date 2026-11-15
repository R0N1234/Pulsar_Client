package dev.pulsarclient.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cabeça da skin de qualquer nick (lista de contas). A skin vem da Mojang pelo nome: um nick offline mostra a skin
 * da conta original com o mesmo nome, se existir. Enquanto carrega, ou sem internet, fica a skin padrão do jogo.
 */
public final class Cabecas {
    private static final Map<String, ResourceLocation> SKINS = new ConcurrentHashMap<String, ResourceLocation>();
    private static final Map<String, Boolean> PEDIDOS = new ConcurrentHashMap<String, Boolean>();

    private Cabecas() {}

    /** UUID que o jogo usa para um nick offline (decide a skin padrão, Steve ou Alex). */
    public static UUID offline(String nome) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nome).getBytes(StandardCharsets.UTF_8));
    }

    /** Já tem a skin da internet para esse nick? */
    public static boolean carregada(String nome) {
        return nome != null && SKINS.containsKey(nome.toLowerCase());
    }

    public static void desenhar(String nome, int x, int y, int tamanho) {
        if (nome == null || nome.isEmpty()) return;
        String chave = nome.toLowerCase();
        ResourceLocation tex = SKINS.get(chave);
        if (tex == null) {
            if (PEDIDOS.putIfAbsent(chave, Boolean.TRUE) == null) carregar(nome, chave);
            tex = DefaultPlayerSkin.getDefaultSkin(offline(nome));
        }
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, tamanho, tamanho, 64, 64);
        // Segunda camada (chapéu, capuz, cabelo), um pouco maior como no jogo.
        Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, tamanho, tamanho, 64, 64);
    }

    private static void carregar(final String nome, final String chave) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UUID id = uuidDoNome(nome);
                    if (id == null) return;
                    final Minecraft mc = Minecraft.getMinecraft();
                    final GameProfile perfil = mc.getSessionService().fillProfileProperties(new GameProfile(id, nome), false);
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> texturas = mc.getSkinManager().loadSkinFromCache(perfil);
                            MinecraftProfileTexture skin = texturas.get(MinecraftProfileTexture.Type.SKIN);
                            if (skin != null) SKINS.put(chave, mc.getSkinManager().loadSkin(skin, MinecraftProfileTexture.Type.SKIN));
                        }
                    });
                } catch (Throwable e) {
                    // Sem internet ou nick sem conta original: fica a skin padrão.
                    System.out.println("[Pulsar] Cabeça de " + nome + ": skin padrão (" + e.getClass().getSimpleName() + ")");
                }
            }
        }, "Pulsar cabeça");
        t.setDaemon(true);
        t.start();
    }

    /** Nome para UUID pela API pública da Mojang. null quando ninguém tem esse nick. */
    private static UUID uuidDoNome(String nome) throws Exception {
        if (!nome.matches("[A-Za-z0-9_]{1,16}")) return null;
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + nome).openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        if (c.getResponseCode() != 200) return null;
        InputStream in = c.getInputStream();
        try {
            JsonObject o = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return o.has("id") ? UUIDTypeAdapter.fromString(o.get("id").getAsString()) : null;
        } finally {
            in.close();
        }
    }
}
