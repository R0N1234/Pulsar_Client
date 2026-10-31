package dev.pulsarclient.rede;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.pulsarclient.PulsarClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Rede do Pulsar: quem está usando o Pulsar agora (ícone PC no nome), o nível de cada um e os amigos com chat.
 * Uma linha separada conversa com o servidor da rede a cada minuto (a cada 5 s com a tela de Amigos aberta);
 * o jogo só lê o que ela guardou, então a rede lenta ou fora do ar nunca trava o jogo.
 *
 * Conta original entra como num servidor do Minecraft (a Mojang confirma quem é, sem senha passar por aqui).
 * Conta offline aparece com o ícone e o nível, mas sem amigos e chat.
 */
public final class Rede {
    public static final class Amigo {
        public final String uuid, nome, servidor;
        public final boolean online;
        public final int nivel;

        Amigo(String uuid, String nome, boolean online, String servidor, int nivel) {
            this.uuid = uuid;
            this.nome = nome;
            this.online = online;
            this.servidor = servidor;
            this.nivel = nivel;
        }
    }

    public static final class Pedido {
        public final String uuid, nome;

        Pedido(String uuid, String nome) {
            this.uuid = uuid;
            this.nome = nome;
        }
    }

    public static final class Mensagem {
        public final String amigo, nome, texto;
        public final boolean minha;

        Mensagem(String amigo, String nome, String texto, boolean minha) {
            this.amigo = amigo;
            this.nome = nome;
            this.texto = texto;
            this.minha = minha;
        }
    }

    private static final long NORMAL = 60000;
    private static final long RAPIDO = 5000;

    private static final String ENDERECO = endereco();
    private static final Rede INSTANCIA = new Rede();

    // Lido pelo jogo, escrito pela linha da rede.
    private volatile Map<String, Integer> pulsar = Collections.emptyMap();
    private volatile List<Amigo> amigos = Collections.emptyList();
    private volatile List<Pedido> pedidos = Collections.emptyList();
    private volatile int nivel = 1, minutos, proximo = 15;
    private volatile boolean offline, conectado;
    private volatile String estado = "";
    private volatile String aviso = "";
    private final ConcurrentLinkedQueue<Mensagem> chegando = new ConcurrentLinkedQueue<Mensagem>();
    private final Map<String, List<Mensagem>> conversas = new HashMap<String, List<Mensagem>>();

    // Escrito pelo jogo a cada segundo, lido pela linha da rede.
    private volatile List<String> ver = Collections.emptyList();
    private volatile String servidor = "";
    private volatile boolean mostrarServidor = true, oculto, ativa, rapido, teste;

    private String token;
    private String contaDoToken = "";
    private final Object sinal = new Object();
    private final ConcurrentLinkedQueue<Runnable> acoes = new ConcurrentLinkedQueue<Runnable>();
    private Thread linha;
    private int ticks;
    private Map<String, Boolean> onlineAntes;

    private Rede() {}

    public static Rede get() {
        return INSTANCIA;
    }

    /** Endereço da rede: -Dpulsar.rede=... (testes) ou o arquivo assets/pulsar/rede.txt dentro do mod. */
    private static String endereco() {
        String prop = System.getProperty("pulsar.rede");
        if (prop != null && !prop.trim().isEmpty()) return prop.trim().replaceAll("/+$", "");
        InputStream in = Rede.class.getResourceAsStream("/assets/pulsar/rede.txt");
        if (in == null) return "";
        try {
            return IOUtils.toString(in, "UTF-8").trim().replaceAll("/+$", "");
        } catch (Exception e) {
            return "";
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static boolean noAr() {
        return !ENDERECO.isEmpty();
    }

    // ---------- leitura pelo jogo ----------

    public static String semTraco(UUID u) {
        return u == null ? "" : u.toString().replace("-", "");
    }

    /** Nível de quem usa o Pulsar, ou 0 se a pessoa não usa (ou a rede ainda não respondeu). */
    public int nivelDe(EntityPlayer p) {
        if (p == null) return 0;
        if (p == Minecraft.getMinecraft().thePlayer) return nivel;
        Integer n = pulsar.get(semTraco(p.getUniqueID()));
        return n == null ? 0 : n;
    }

    public int nivelDe(GameProfile perfil) {
        if (perfil == null || perfil.getId() == null) return 0;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && perfil.getId().equals(mc.thePlayer.getUniqueID())) return nivel;
        Integer n = pulsar.get(semTraco(perfil.getId()));
        return n == null ? 0 : n;
    }

    public List<Amigo> amigos() { return amigos; }
    public List<Pedido> pedidos() { return pedidos; }
    public int nivel() { return nivel; }
    public int minutos() { return minutos; }
    public int proximo() { return proximo; }
    public boolean offline() { return offline; }
    public boolean conectado() { return conectado; }
    public String estado() { return estado; }
    public String aviso() { return aviso; }

    public List<Mensagem> conversa(String uuid) {
        synchronized (conversas) {
            List<Mensagem> l = conversas.get(uuid);
            return l == null ? Collections.<Mensagem>emptyList() : new ArrayList<Mensagem>(l);
        }
    }

    /** Tela de Amigos aberta: conversa com a rede a cada 5 segundos. */
    public void rapido(boolean sim) {
        rapido = sim;
        if (sim) acordar();
    }

    // ---------- ações (rodam na linha da rede) ----------

    public void pedirAmizade(final String nome) {
        acao("/amigos/pedir", corpo("nome", nome));
    }

    public void responder(String uuid, boolean aceitar) {
        JsonObject j = corpo("uuid", uuid);
        j.addProperty("aceitar", aceitar);
        acao("/amigos/responder", j);
    }

    public void remover(String uuid) {
        acao("/amigos/remover", corpo("uuid", uuid));
    }

    public void mandar(final String para, final String nome, final String texto) {
        guardar(new Mensagem(para, nome, texto, true));
        acao("/chat", corpo("para", para, "texto", texto));
    }

    private static JsonObject corpo(String... pares) {
        JsonObject j = new JsonObject();
        for (int i = 0; i + 1 < pares.length; i += 2) j.addProperty(pares[i], pares[i + 1]);
        return j;
    }

    private void acao(final String caminho, final JsonObject j) {
        acoes.add(new Runnable() {
            @Override
            public void run() {
                if (token == null) {
                    aviso = offline ? "Amigos precisam de conta original (Microsoft)." : "Sem conexão com a rede do Pulsar.";
                    return;
                }
                j.addProperty("token", token);
                try {
                    JsonObject r = post(caminho, j);
                    aviso = r.has("aviso") ? r.get("aviso").getAsString() : r.has("erro") ? r.get("erro").getAsString() : "";
                } catch (Exception e) {
                    aviso = "Sem conexão com a rede do Pulsar.";
                }
            }
        });
        acordar();
    }

    private void acordar() {
        synchronized (sinal) {
            sinal.notifyAll();
        }
    }

    // ---------- jogo: a cada segundo ----------

    public void iniciar() {
        if (!noAr() || linha != null) return;
        linha = new Thread(new Runnable() {
            @Override
            public void run() {
                laco();
            }
        }, "Pulsar - rede");
        linha.setDaemon(true);
        linha.start();
        System.out.println("[Pulsar] Rede: usando " + ENDERECO);
    }

    /** Configuração vinda dos mods (Ícone, Nível e Amigos). */
    public void configurar(boolean ativa, boolean oculto, boolean mostrarServidor) {
        boolean ligou = ativa && !this.ativa;
        this.ativa = ativa;
        this.oculto = oculto;
        this.mostrarServidor = mostrarServidor;
        // Ligou agora (ou o jogo acabou de abrir): conversa com a rede já, sem esperar um minuto.
        if (ligou) acordar();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % 20 != 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        // Jogadores do TAB (quem dá para ver agora): lido aqui, na linha do jogo.
        List<String> lista = new ArrayList<String>();
        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo i : mc.getNetHandler().getPlayerInfoMap()) {
                if (lista.size() >= 200) break;
                if (i.getGameProfile().getId() != null) lista.add(semTraco(i.getGameProfile().getId()));
            }
        }
        ver = lista;
        ServerData dados = mc.getCurrentServerData();
        servidor = mc.theWorld == null ? "" : mc.isSingleplayer() || dados == null ? "um mundo sozinho" : dados.serverIP;
        mostrarNovidades(mc);
    }

    // Mensagens novas e amigos que entraram viram linhas no chat.
    private void mostrarNovidades(Minecraft mc) {
        if (mc.thePlayer == null) return;
        dev.pulsarclient.mods.Amigos mod = (dev.pulsarclient.mods.Amigos) PulsarClient.get().mod("amigos");
        Mensagem m;
        while ((m = chegando.poll()) != null) {
            if (mod != null && mod.avisarMensagens() && !(mc.currentScreen instanceof dev.pulsarclient.gui.GuiAmigos)) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                        EnumChatFormatting.LIGHT_PURPLE + "[Amigos] " + EnumChatFormatting.WHITE + m.nome + ": " + EnumChatFormatting.GRAY + m.texto));
            }
        }
        Map<String, Boolean> agora = new HashMap<String, Boolean>();
        for (Amigo a : amigos) agora.put(a.uuid, a.online);
        if (onlineAntes != null && mod != null && mod.avisarEntradas()) {
            for (Amigo a : amigos) {
                Boolean antes = onlineAntes.get(a.uuid);
                if (a.online && antes != null && !antes) {
                    mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                            EnumChatFormatting.LIGHT_PURPLE + "[Amigos] " + EnumChatFormatting.WHITE + a.nome + EnumChatFormatting.GRAY + " entrou no jogo."));
                }
            }
        }
        onlineAntes = agora;
    }

    // ---------- linha da rede ----------

    private void laco() {
        while (true) {
            try {
                Runnable r;
                while ((r = acoes.poll()) != null) r.run();
                if (ativa && !teste) pulsar();
            } catch (Throwable t) {
                conectado = false;
                estado = "Sem conexão com a rede do Pulsar.";
            }
            synchronized (sinal) {
                try {
                    if (acoes.isEmpty()) sinal.wait(rapido ? RAPIDO : NORMAL);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private void pulsar() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        String conta = mc.getSession().getUsername() + "|" + mc.getSession().getPlayerID();
        if (!conta.equals(contaDoToken)) {
            token = null;
            contaDoToken = conta;
            offline = !entrar(mc);
        }
        JsonArray lista = new JsonArray();
        for (String u : ver) lista.add(new com.google.gson.JsonPrimitive(u));
        JsonObject j = new JsonObject();
        j.add("ver", lista);
        j.addProperty("oculto", oculto);
        JsonObject r;
        if (offline) {
            j.addProperty("nome", mc.getSession().getUsername());
            r = post("/offline", j);
        } else {
            j.addProperty("token", token);
            j.addProperty("servidor", mostrarServidor ? servidor : "");
            r = post("/pulso", j);
            if (!r.get("ok").getAsBoolean() && "sessao".equals(texto(r, "erro"))) {
                contaDoToken = "";
                return;
            }
        }
        if (!r.get("ok").getAsBoolean()) {
            estado = texto(r, "erro");
            return;
        }
        ler(r);
        if (!conectado) {
            System.out.println("[Pulsar] Rede: conectado como " + mc.getSession().getUsername() + " (" + (offline ? "offline" : "original") + "), nível " + nivel);
        }
        conectado = true;
        estado = "";
    }

    /** Conta original: avisa a Mojang que vai "entrar num servidor" e a rede confere. Falhou = conta offline. */
    private boolean entrar(Minecraft mc) {
        String acesso = mc.getSession().getToken();
        if (acesso == null || acesso.length() < 20 || acesso.equals(mc.getSession().getPlayerID())) return false;
        try {
            String serverId = UUID.randomUUID().toString().replace("-", "");
            mc.getSessionService().joinServer(mc.getSession().getProfile(), acesso, serverId);
            JsonObject j = corpo("nome", mc.getSession().getUsername(), "serverId", serverId);
            JsonObject r = post("/entrar", j);
            if (!r.get("ok").getAsBoolean()) {
                estado = texto(r, "erro");
                return false;
            }
            token = r.get("token").getAsString();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ler(JsonObject r) {
        nivel = r.get("nivel").getAsInt();
        minutos = r.get("minutos").getAsInt();
        proximo = r.get("proximo").getAsInt();
        Map<String, Integer> p = new HashMap<String, Integer>();
        for (Map.Entry<String, JsonElement> e : r.getAsJsonObject("pulsar").entrySet()) p.put(e.getKey(), e.getValue().getAsInt());
        pulsar = p;
        if (!r.has("amigos")) {
            amigos = Collections.emptyList();
            pedidos = Collections.emptyList();
            return;
        }
        List<Amigo> a = new ArrayList<Amigo>();
        for (JsonElement e : r.getAsJsonArray("amigos")) {
            JsonObject o = e.getAsJsonObject();
            a.add(new Amigo(texto(o, "uuid"), texto(o, "nome"), o.get("online").getAsBoolean(), texto(o, "servidor"), o.get("nivel").getAsInt()));
        }
        amigos = a;
        List<Pedido> ps = new ArrayList<Pedido>();
        for (JsonElement e : r.getAsJsonArray("pedidos")) {
            JsonObject o = e.getAsJsonObject();
            ps.add(new Pedido(texto(o, "uuid"), texto(o, "nome")));
        }
        pedidos = ps;
        for (JsonElement e : r.getAsJsonArray("mensagens")) {
            JsonObject o = e.getAsJsonObject();
            Mensagem m = new Mensagem(texto(o, "de"), texto(o, "nome"), texto(o, "texto"), false);
            guardar(m);
            chegando.add(m);
        }
    }

    private void guardar(Mensagem m) {
        synchronized (conversas) {
            List<Mensagem> l = conversas.get(m.amigo);
            if (l == null) conversas.put(m.amigo, l = new ArrayList<Mensagem>());
            l.add(m);
            if (l.size() > 100) l.remove(0);
        }
    }

    private static String texto(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static JsonObject post(String caminho, JsonObject corpo) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(ENDERECO + caminho).openConnection();
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("User-Agent", "PulsarClient/" + PulsarClient.VERSION);
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setDoOutput(true);
        OutputStream out = c.getOutputStream();
        try {
            out.write(corpo.toString().getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
        InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
        try {
            return new JsonParser().parse(IOUtils.toString(in, "UTF-8")).getAsJsonObject();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    // ---------- teste (SelfTest) ----------

    /** Coloca dados de mentira, para os prints do teste mostrarem os ícones e a tela de Amigos. */
    public void dadosDeTeste(List<UUID> usuarios) {
        // Daqui em diante a rede para de pulsar, para os dados de mentira não serem trocados pelos de verdade.
        teste = true;
        offline = false;
        conectado = true;
        nivel = 6;
        minutos = 412;
        proximo = 540;
        Map<String, Integer> p = new LinkedHashMap<String, Integer>(pulsar);
        int n = 4;
        for (UUID u : usuarios) p.put(semTraco(u), n += 5);
        pulsar = p;
        List<Amigo> a = new ArrayList<Amigo>();
        a.add(new Amigo("a1", "AnaPvP", true, "mc.exemplo.com", 12));
        a.add(new Amigo("a2", "Rick_Alt2", true, "um mundo sozinho", 7));
        a.add(new Amigo("a3", "Lucas_99", false, "", 3));
        amigos = a;
        List<Pedido> ps = new ArrayList<Pedido>();
        ps.add(new Pedido("p1", "NovoAmigo"));
        pedidos = ps;
        guardar(new Mensagem("a1", "AnaPvP", "bora x1 no lobby?", false));
        guardar(new Mensagem("a1", "Você", "bora, entra no 3", true));
    }
}
