package dev.pulsarclient.gui;

import com.google.gson.JsonElement;
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
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Troca de conta pelo menu do jogo: a conta Microsoft (original) e os nicks offline guardados no launcher.
 * O login acontece no launcher (que já sabe falar com a Microsoft); o jogo pede pela ponte local
 * (127.0.0.1, com a chave que o launcher passou ao abrir o jogo) e recebe a sessão nova.
 */
public final class Conta {
    private static final String PORT = System.getProperty("pulsar.ponte");
    private static final String KEY = System.getProperty("pulsar.chave");

    /** Contas que o launcher tem. */
    public static final class Lista {
        public final String microsoft;
        public final List<String> offline;
        public final String ativa;
        public final boolean liberado;

        Lista(String microsoft, List<String> offline, String ativa, boolean liberado) {
            this.microsoft = microsoft;
            this.offline = offline;
            this.ativa = ativa;
            this.liberado = liberado;
        }
    }

    private static volatile String status = "";
    private static volatile boolean busy;
    private static volatile Lista lista;
    private static volatile ResourceLocation skin;
    private static String skinFor;

    private Conta() {}

    public static String status() { return status; }
    public static boolean busy() { return busy; }
    public static boolean temLauncher() { return PORT != null && KEY != null; }
    /** Última lista recebida do launcher (null antes da primeira). */
    public static Lista lista() { return lista; }

    /** Abre o login da Microsoft no launcher, para entrar com outra conta original. */
    public static void trocar() {
        pedir("/conta/trocar", null, "Entre na conta na janela da Microsoft que abriu...", true);
    }

    /** Joga com uma conta que o launcher já tem: "microsoft" ou um nick offline. */
    public static void usar(String conta) {
        JsonObject corpo = new JsonObject();
        corpo.addProperty("conta", conta);
        pedir("/conta/usar", corpo, "Trocando de conta...", true);
    }

    /** Cria um nick offline no launcher e já joga com ele. */
    public static void adicionarOffline(String nick) {
        JsonObject corpo = new JsonObject();
        corpo.addProperty("nick", nick);
        pedir("/conta/offline", corpo, "Criando o nick...", true);
    }

    /** Pede a lista de contas ao launcher (em segundo plano). */
    public static void atualizarLista() {
        pedir("/conta/lista", null, null, false);
    }

    private static void pedir(final String caminho, final JsonObject corpo, String aguarde, final boolean troca) {
        if (!temLauncher()) {
            status = "Abra o jogo pelo launcher do Pulsar para trocar de conta.";
            return;
        }
        if (troca) {
            if (busy) return;
            busy = true;
            status = aguarde;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonObject r = enviar(caminho, corpo);
                    if (!r.has("ok") || !r.get("ok").getAsBoolean()) {
                        String erro = r.has("erro") ? r.get("erro").getAsString() : "Não deu para trocar de conta.";
                        if (troca) status = erro;
                        return;
                    }
                    if (troca) aplicar(r);
                    else lista = ler(r);
                } catch (Throwable e) {
                    status = "Não deu para falar com o launcher. Ele ainda está aberto?";
                    System.err.println("[Pulsar] Conta: " + caminho + " falhou: " + e);
                } finally {
                    if (troca) {
                        busy = false;
                        atualizarLista();
                    }
                }
            }
        }, "Pulsar conta");
        t.setDaemon(true);
        t.start();
    }

    private static Lista ler(JsonObject r) {
        String ms = r.has("microsoft") && r.get("microsoft").isJsonObject() ? r.getAsJsonObject("microsoft").get("name").getAsString() : null;
        List<String> off = new ArrayList<String>();
        if (r.has("offline") && r.get("offline").isJsonArray()) {
            for (JsonElement e : r.getAsJsonArray("offline")) off.add(e.getAsString());
        }
        String ativa = r.has("ativa") ? r.get("ativa").getAsString() : "microsoft";
        boolean liberado = r.has("liberado") && r.get("liberado").getAsBoolean();
        return new Lista(ms, Collections.unmodifiableList(off), ativa, liberado);
    }

    private static void aplicar(JsonObject r) {
        final String name = r.get("name").getAsString();
        final String uuid = r.get("uuid").getAsString().replace("-", "");
        final String token = r.get("token").getAsString();
        final boolean offline = r.has("offline") && r.get("offline").getAsBoolean();
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    usarSessao(new Session(name, uuid, token, offline ? "legacy" : "mojang"));
                    status = "Agora você está jogando como " + name + (offline ? " (offline)." : ".");
                    System.out.println("[Pulsar] Conta trocada para " + name + (offline ? " (offline)" : ""));
                } catch (Throwable e) {
                    status = "Não deu para trocar de conta: " + e;
                }
            }
        });
    }

    private static JsonObject enviar(String caminho, JsonObject corpo) throws Exception {
        URL url = new URL("http://127.0.0.1:" + PORT + caminho);
        HttpURLConnection c = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        c.setRequestMethod("POST");
        c.setRequestProperty("X-Pulsar-Chave", KEY);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setConnectTimeout(3000);
        // O login pode demorar: o jogador digita e-mail, senha e código.
        c.setReadTimeout(10 * 60 * 1000);
        c.setDoOutput(true);
        OutputStream out = c.getOutputStream();
        try {
            if (corpo != null) out.write(corpo.toString().getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
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
