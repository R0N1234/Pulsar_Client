package dev.pulsarclient.mods;

import dev.pulsarclient.gui.GuiAmigos;
import dev.pulsarclient.rede.Rede;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.KeySetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Amigos: lista de amigos que usam o Pulsar, quem está online e em qual servidor, e chat entre vocês.
 * Abre com a tecla escolhida (padrão O) ou pelo botão nas opções do mod. Precisa de conta original.
 */
public class Amigos extends PulsarMod {
    private final KeySetting abrir = add(new KeySetting("tecla_abrir", "Tecla para abrir os Amigos", Keyboard.KEY_O));
    private final BoolSetting entradas = add(new BoolSetting("avisar_entradas", "Avisar quando um amigo entrar", true));
    private final BoolSetting mensagens = add(new BoolSetting("avisar_mensagens", "Mostrar mensagens no chat do jogo", true));
    private final BoolSetting servidor = add(new BoolSetting("mostrar_servidor", "Amigos veem em qual servidor você está", true));

    public Amigos() {
        super("amigos", "Amigos", "Lista de amigos e chat dentro do client", Category.SOCIAL, true);
        add(new ActionSetting("abrir", "Abrir os Amigos", new Runnable() {
            @Override
            public void run() {
                mc.displayGuiScreen(new GuiAmigos(mc.currentScreen));
            }
        }));
    }

    public boolean avisarEntradas() { return isEnabled() && entradas.get(); }
    public boolean avisarMensagens() { return isEnabled() && mensagens.get(); }
    public boolean mostrarServidor() { return servidor.get(); }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) apertou(Keyboard.getEventKey());
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() >= 0) apertou(Mouse.getEventButton() - 100);
    }

    private void apertou(int codigo) {
        if (!isEnabled() || codigo == 0 || codigo != abrir.get() || mc.currentScreen != null || !Rede.noAr()) return;
        mc.displayGuiScreen(new GuiAmigos(null));
    }
}
