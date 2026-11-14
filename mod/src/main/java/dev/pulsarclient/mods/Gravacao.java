package dev.pulsarclient.mods;

import com.google.gson.JsonObject;
import dev.pulsarclient.Ponte;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Gravação: grava vídeo do jogo e salva clipes dos últimos segundos, como no Lunar Client.
 * Quem grava é o launcher do Pulsar (ele captura a janela do jogo com o som do computador); o jogo só manda
 * os pedidos pela ponte local e mostra o "REC" com o tempo na tela. Os vídeos vão para Vídeos/Pulsar Client.
 *
 * Teclas: F9 começa e para a gravação; F10 salva o clipe dos últimos segundos (com "Clipe sempre pronto" ligado,
 * o launcher mantém os últimos segundos guardados enquanto você joga).
 */
public class Gravacao extends HudMod {
    private static final int VERMELHO = 0xFFFF3B3B;

    private final KeySetting teclaGravar = add(new KeySetting("tecla_gravar", "Tecla para começar e parar de gravar", Keyboard.KEY_F9));
    private final KeySetting teclaClipe = add(new KeySetting("tecla_clipe", "Tecla para salvar o clipe", Keyboard.KEY_F10));
    private final BoolSetting clipePronto = add(new BoolSetting("clipe_pronto", "Clipe sempre pronto (guarda os últimos segundos)", false));
    private final SliderSetting clipeSegundos = add(new SliderSetting("clipe_segundos", "Tamanho do clipe", 30, 15, 120, 5, "s"));
    private final ModeSetting qualidade = add(new ModeSetting("qualidade", "Qualidade", 1, "720p", "1080p", "Tela inteira"));
    private final ModeSetting quadros = add(new ModeSetting("quadros", "Quadros por segundo", 0, "30", "60"));
    private final BoolSetting somDoJogo = add(new BoolSetting("som", "Gravar o som do computador", true));
    private final BoolSetting microfone = add(new BoolSetting("microfone", "Gravar o microfone", false));
    private final BoolSetting avisos = add(new BoolSetting("avisos", "Avisos no chat com botões", true));
    private final ActionSetting gravar = add(new ActionSetting("gravar", "Começar ou parar de gravar", new Runnable() {
        @Override public void run() { alternarGravacao(); }
    }));
    private final ActionSetting clipe = add(new ActionSetting("clipe", "Salvar o clipe agora", new Runnable() {
        @Override public void run() { salvarClipe(); }
    }));
    private final ActionSetting pasta = add(new ActionSetting("pasta", "Abrir a pasta dos vídeos", new Runnable() {
        @Override public void run() { pedir("pasta"); }
    }));

    /** O que o launcher respondeu por último: gravando desde quando (ms) e se o clipe está guardando. */
    private volatile boolean gravando;
    private volatile long desde;
    private volatile boolean guardando;
    private volatile String ultimoErro = "";
    private boolean pedidoNoAr;
    private int ticks;
    private int bufferAvisado = -1;

    private static int pedidos;
    private static final List<String> ultimosPedidos = new ArrayList<String>();

    public Gravacao() {
        super("gravacao", "Gravação", "Grava o jogo e salva clipes dos últimos segundos (como no Lunar), com REC na tela", Category.SOCIAL, 4, 4, true);
    }

    /** Teste automático: quantos pedidos foram para o launcher e quais. */
    public static int pedidos() { return pedidos; }
    public static List<String> ultimosPedidos() { return ultimosPedidos; }
    public boolean gravando() { return gravando; }

    @Override
    protected void onDisable() {
        if (gravando) pedir("parar");
        if (guardando) pedirBuffer(false);
    }

    // ---------- teclas ----------

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) apertou(Keyboard.getEventKey());
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() >= 0) apertou(Mouse.getEventButton() - 100);
    }

    private void apertou(int codigo) {
        if (!isEnabled() || codigo == 0 || mc.currentScreen != null) return;
        if (codigo == teclaGravar.get()) alternarGravacao();
        else if (codigo == teclaClipe.get()) salvarClipe();
    }

    public void alternarGravacao() {
        pedir(gravando ? "parar" : "iniciar");
    }

    public void salvarClipe() {
        if (!guardando && !gravando) {
            // Sem buffer ligado, o clipe começa a guardar agora e avisa.
            pedirBuffer(true);
            aviso(EnumChatFormatting.GRAY + "O clipe começou a guardar agora. Aperte de novo daqui a alguns segundos para salvar.", null);
            return;
        }
        pedir("clipe");
    }

    @Override
    public void tick() {
        ticks++;
        if (!isEnabled()) return;
        // Com "Clipe sempre pronto", o launcher guarda os últimos segundos enquanto você estiver num mundo.
        boolean querBuffer = clipePronto.get() && mc.theWorld != null;
        int estado = querBuffer ? 1 : 0;
        if (estado != bufferAvisado && ticks % 20 == 0) {
            bufferAvisado = estado;
            if (querBuffer != guardando) pedirBuffer(querBuffer);
        }
    }

    // ---------- ponte com o launcher ----------

    private JsonObject corpo(String acao) {
        JsonObject j = new JsonObject();
        j.addProperty("acao", acao);
        j.addProperty("clipe", clipeSegundos.getInt());
        j.addProperty("qualidade", qualidade.get());
        j.addProperty("fps", Integer.parseInt(quadros.get()));
        j.addProperty("som", somDoJogo.get());
        j.addProperty("microfone", microfone.get());
        return j;
    }

    private void pedirBuffer(boolean ligar) {
        JsonObject j = corpo("buffer");
        j.addProperty("ligado", ligar);
        enviar(j);
    }

    private void pedir(String acao) {
        enviar(corpo(acao));
    }

    private void enviar(final JsonObject j) {
        final String acao = j.get("acao").getAsString();
        pedidos++;
        synchronized (ultimosPedidos) {
            ultimosPedidos.add(acao);
            if (ultimosPedidos.size() > 20) ultimosPedidos.remove(0);
        }
        if (!Ponte.disponivel()) {
            aviso(EnumChatFormatting.RED + "A gravação precisa do launcher do Pulsar aberto (o jogo foi aberto por outro launcher).", null);
            return;
        }
        if (pedidoNoAr && !"buffer".equals(acao)) return;
        pedidoNoAr = true;
        Ponte.perguntar("/gravar", j, new Ponte.Resposta() {
            @Override
            public void chegou(final JsonObject r) {
                pedidoNoAr = false;
                mc.addScheduledTask(new Runnable() {
                    @Override public void run() { resposta(acao, r); }
                });
            }
        });
        // Se o launcher não responder, libera o próximo pedido em alguns segundos.
        new Thread(new Runnable() {
            @Override public void run() {
                try { Thread.sleep(6000); } catch (InterruptedException ignored) {}
                pedidoNoAr = false;
            }
        }, "Pulsar - gravação").start();
    }

    /** Resposta do launcher: {ok, gravando, desde, guardando, arquivo, pasta, erro}. */
    private void resposta(String acao, JsonObject r) {
        boolean ok = r.has("ok") && r.get("ok").getAsBoolean();
        if (r.has("gravando")) gravando = r.get("gravando").getAsBoolean();
        if (r.has("desde")) desde = r.get("desde").getAsLong();
        if (r.has("guardando")) guardando = r.get("guardando").getAsBoolean();
        String erro = r.has("erro") ? r.get("erro").getAsString() : "";
        ultimoErro = ok ? "" : erro;
        System.out.println("[Pulsar] Gravação: " + acao + " -> " + (ok ? "ok" : "erro " + erro) + (gravando ? " (gravando)" : "") + (guardando ? " (clipe guardando)" : ""));
        if (!ok) {
            aviso(EnumChatFormatting.RED + "Gravação: " + (erro.isEmpty() ? "o launcher não respondeu." : erro), null);
            return;
        }
        if ("iniciar".equals(acao)) aviso(EnumChatFormatting.GRAY + "Gravando. Aperte " + teclaGravar.display() + " para parar.", null);
        if (("parar".equals(acao) || "clipe".equals(acao)) && r.has("arquivo")) {
            String arquivo = r.get("arquivo").getAsString();
            String pastaVideos = r.has("pasta") ? r.get("pasta").getAsString() : "";
            String nome = arquivo.replace('\\', '/');
            nome = nome.substring(nome.lastIndexOf('/') + 1);
            aviso(EnumChatFormatting.GRAY + ("clipe".equals(acao) ? "Clipe salvo: " : "Vídeo salvo: ") + EnumChatFormatting.WHITE + nome + " ", botoes(arquivo, pastaVideos));
        }
    }

    private IChatComponent botoes(String arquivo, String pastaVideos) {
        IChatComponent c = botao("Abrir", new ClickEvent(ClickEvent.Action.OPEN_FILE, arquivo), "Abrir o vídeo");
        if (!pastaVideos.isEmpty()) {
            c.appendSibling(new ChatComponentText(" "));
            c.appendSibling(botao("Pasta", new ClickEvent(ClickEvent.Action.OPEN_FILE, pastaVideos), "Abrir a pasta dos vídeos"));
        }
        return c;
    }

    private static IChatComponent botao(String texto, ClickEvent clique, String dica) {
        IChatComponent c = new ChatComponentText("[" + texto + "]");
        c.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA).setBold(true).setChatClickEvent(clique)
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(dica))));
        return c;
    }

    private void aviso(String texto, IChatComponent extra) {
        if (!avisos.get() || mc.thePlayer == null) return;
        IChatComponent linha = new ChatComponentText(EnumChatFormatting.RED + "[Gravação] " + texto);
        if (extra != null) linha.appendSibling(extra);
        mc.thePlayer.addChatMessage(linha);
    }

    // ---------- desenho ----------

    private String tempo() {
        long s = Math.max(0, (System.currentTimeMillis() - desde) / 1000);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    @Override public int width() { return font().getStringWidth("REC 00:00") + 14; }
    @Override public int height() { return 12; }

    @Override
    public void render(boolean preview) {
        boolean rec = gravando || preview;
        if (!rec && !guardando) return;
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + width() + 2, y + height() + 2, BACKGROUND);
        // Bolinha vermelha piscando enquanto grava; cinza quando só o clipe está guardando.
        boolean acesa = !rec || (System.currentTimeMillis() / 500) % 2 == 0;
        int cor = rec ? VERMELHO : 0xFF8892B0;
        if (acesa) Gui.drawRect(x + 2, y + 3, x + 8, y + 9, cor);
        Gui.drawRect(x + 3, y + 2, x + 7, y + 10, acesa ? cor : 0x00000000);
        String texto = rec ? "REC " + (preview ? "00:00" : tempo()) : "CLIPE";
        font().drawStringWithShadow(texto, x + 12, y + 2, rec ? WHITE : 0xFF8892B0);
    }
}
