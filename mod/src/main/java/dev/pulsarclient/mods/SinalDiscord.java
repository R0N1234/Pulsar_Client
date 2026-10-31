package dev.pulsarclient.mods;

import com.google.gson.JsonObject;
import dev.pulsarclient.Ponte;
import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Sinal Discord: o launcher mostra no perfil do Discord que você está jogando com o Pulsar.
 * O jogo só conta onde você está (menu, mundo sozinho ou servidor) e o launcher fala com o Discord.
 * Manda um aviso quando muda, e repete de vez em quando para o caso de o Discord ter aberto depois.
 */
public class SinalDiscord extends PulsarMod {
    private static final int A_CADA = 40;
    private static final int REPETIR = 20 * 60;

    private final BoolSetting servidor = add(new BoolSetting("mostrar_servidor", "Mostrar o servidor", true));

    private String enviado = "";
    private int ticks;
    private int desdeAviso;

    public SinalDiscord() {
        super("sinal_discord", "Sinal Discord", "Mostra no Discord o que você está jogando", Category.SOCIAL, true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % A_CADA != 0 || !Ponte.disponivel()) return;
        desdeAviso += A_CADA;
        String lugar = "menu";
        String ip = "";
        if (mc.theWorld != null) {
            ServerData dados = mc.getCurrentServerData();
            if (mc.isSingleplayer() || dados == null) lugar = "sozinho";
            else {
                lugar = "servidor";
                if (servidor.get()) ip = dados.serverIP;
            }
        }
        String estado = isEnabled() + "|" + lugar + "|" + ip;
        // Desligado só avisa uma vez; ligado repete a cada minuto.
        if (estado.equals(enviado) && (!isEnabled() || desdeAviso < REPETIR)) return;
        enviado = estado;
        desdeAviso = 0;
        JsonObject j = new JsonObject();
        j.addProperty("ligado", isEnabled());
        j.addProperty("lugar", lugar);
        j.addProperty("servidor", ip);
        Ponte.avisar("/presenca", j);
    }
}
