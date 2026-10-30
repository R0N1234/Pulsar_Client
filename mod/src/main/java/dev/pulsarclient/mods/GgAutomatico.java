package dev.pulsarclient.mods;

import dev.pulsarclient.settings.SliderSetting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * GG Automático: quando o chat anuncia o fim da partida, manda "gg" (ou a mensagem que você escolher).
 * Reconhece os avisos de fim de partida mais comuns em servidores brasileiros e internacionais,
 * e aceita frases extras. Manda no máximo uma vez a cada 15 segundos.
 */
public class GgAutomatico extends PulsarMod {
    // Avisos que começam a linha (depois de espaços e símbolos), como "Vencedor: Fulano" ou " 1st Killer - Fulano".
    private static final Pattern INICIO = Pattern.compile(
            "^[^\\p{L}\\p{N}]*(1st killer|1st place|1st -|winner:|winners:|winning team|winner #1|vencedor:|vencedores:|vencedor -|"
                    + "time vencedor|equipe vencedora|vit[oó]ria!|voc[eê] venceu|voc[eê] ganhou|fim de jogo|fim da partida|partida encerrada)");
    // Frases de sistema no meio da linha; só valem em linhas sem ": " (que costuma ser mensagem de jogador).
    private static final String[] FRASES = {"venceu a partida", "venceram a partida", "ganhou a partida", "ganharam a partida",
            "won the game", "has won", "reward summary"};
    private static final long ESPERA_ENTRE = 15000;

    private final TextSetting mensagem = add(new TextSetting("mensagem", "Mensagem", "gg", 100));
    private final SliderSetting atraso = add(new SliderSetting("atraso", "Esperar antes de mandar", 1, 0, 5, 0.5, "s"));
    private final TextSetting extras = add(new TextSetting("frases", "Frases extras de fim de partida (separe com ;)", "", 200));

    private long ultimo;
    private long mandarEm;

    public GgAutomatico() {
        super("gg_automatico", "GG Automático", "Manda GG no chat quando a partida acaba", Category.SERVIDORES, false);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled() || event.type == 2 || mc.thePlayer == null || event.message == null) return;
        String linha = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (linha == null || !fimDePartida(linha.toLowerCase(Locale.ROOT).trim())) return;
        long agora = System.currentTimeMillis();
        if (agora - ultimo < ESPERA_ENTRE) return;
        ultimo = agora;
        mandarEm = agora + Math.round(atraso.get() * 1000);
    }

    private boolean fimDePartida(String linha) {
        if (INICIO.matcher(linha).find()) return true;
        boolean pareceJogador = linha.contains(": ");
        if (!pareceJogador) {
            for (String f : FRASES) if (linha.contains(f)) return true;
        }
        for (String f : extras.get().split(";")) {
            String t = f.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && linha.contains(t)) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        if (mandarEm == 0 || System.currentTimeMillis() < mandarEm) return;
        mandarEm = 0;
        String m = mensagem.get().trim();
        if (isEnabled() && mc.thePlayer != null && !m.isEmpty()) mc.thePlayer.sendChatMessage(m);
    }
}
