package dev.pulsarclient.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Troca de conta Microsoft pelo menu do jogo. O login acontece no launcher (que já sabe falar com a Microsoft);
 * o jogo pede pela ponte local (127.0.0.1, com a chave que o launcher passou ao abrir o jogo) e recebe a sessão nova.
 */
public final class Conta {
    private static final String PORT = System.getProperty("pulsar.ponte");
    private static final String KEY = System.getProperty("pulsar.chave");

    private static volatile String status = "";
    private static volatile boolean busy;
    private static volatile ResourceLocation skin;
    private static String skinFor;

    private Conta() {}

    public static String status() { return status; }
    public static boolean busy() { return busy; }

    public static void trocar() {
        if (busy) return;
        if (PORT == null || KEY == null) {
            status = "Abra o jogo pelo launcher do Pulsar para trocar de conta.";
            return;
        }
        busy = true;
        status = "Entre na conta na janela da Microsoft que abriu...";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JsonObject r = pedirTroca();
                    if (!r.has("ok") || !r.get("ok").getAsBoolean()) {
                        status = r.has("erro") ? r.get("erro").getAsString() : "Não deu para trocar de conta.";
                        return;
                    }
                    final String name = r.get("name").getAsString();
                    final String uuid = r.get("uuid").getAsString().replace("-", "");
                    final String token = r.get("token").getAsString();
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                usarSessao(new Session(name, uuid, token, "mojang"));
                                status = "Agora você está jogando como " + name + ".";
                                System.out.println("[Pulsar] Conta trocada para " + name);
                            } catch (Throwable e) {
                                status = "Não deu para trocar de conta: " + e;
                            }
                        }
                    });
                } catch (Throwable e) {
                    status = "Não deu para falar com o launcher. Ele ainda está aberto?";
                    System.err.println("[Pulsar] Troca de conta falhou: " + e);
                } finally {
                    busy = false;
                }
            }
        }, "Pulsar troca de conta");
        t.setDaemon(true);
        t.start();
    }

    private static JsonObject pedirTroca() throws Exception {
        URL url = new URL("http://127.0.0.1:" + PORT + "/conta/trocar");
        HttpURLConnection c = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        c.setRequestMethod("POST");
        c.setRequestProperty("X-Pulsar-Chave", KEY);
        c.setConnectTimeout(3000);
        // O login pode demorar: o jogador digita e-mail, senha e código.
        c.setReadTimeout(10 * 60 * 1000);
        c.setDoOutput(true);
        c.getOutputStream().close();
        InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
        try {
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } finally {
            in.close();
        }
    }

    private static void usarSessao(Session session) throws Exception {
        Field f = ReflectionHelper.findField(Minecraft.class, "session", "field_71449_j");
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(Minecraft.getMinecraft(), session);
        skinFor = null;
    }

    /** Cabeça da skin da conta atual (a padrão até a skin chegar da internet). */
    public static void drawHead(int x, int y, int size) {
        Minecraft mc = Minecraft.getMinecraft();
        GameProfile profile = mc.getSession().getProfile();
        String who = profile.getName();
        if (who != null && !who.equals(skinFor)) {
            skinFor = who;
            skin = null;
            loadSkin(profile);
        }
        ResourceLocation tex = skin != null ? skin
                : profile.getId() != null ? DefaultPlayerSkin.getDefaultSkin(profile.getId()) : DefaultPlayerSkin.getDefaultSkinLegacy();
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
        Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, size, size, 64, 64);
    }

    private static void loadSkin(final GameProfile base) {
        if (base.getId() == null) return;
        final String who = base.getName();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Minecraft mc = Minecraft.getMinecraft();
                    final GameProfile full = mc.getSessionService().fillProfileProperties(new GameProfile(base.getId(), base.getName()), false);
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = mc.getSkinManager().loadSkinFromCache(full);
                            MinecraftProfileTexture t = textures.get(MinecraftProfileTexture.Type.SKIN);
                            if (t != null && who.equals(skinFor)) skin = mc.getSkinManager().loadSkin(t, MinecraftProfileTexture.Type.SKIN);
                        }
                    });
                } catch (Throwable ignored) {
                    // Sem internet ou conta offline: fica a skin padrão.
                }
            }
        }, "Pulsar skin");
        t.setDaemon(true);
        t.start();
    }
}
