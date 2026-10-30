package dev.pulsarclient.mods;

import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Atalhos de Texto: cada tecla escolhida manda uma mensagem ou um comando no servidor
 * (por exemplo, F6 manda "/lobby"). Funciona com teclas do teclado e com os botões laterais do mouse.
 */
public class AtalhosTexto extends PulsarMod {
    private static final int QUANTOS = 6;
    private static final long ESPERA = 1000;

    private final KeySetting[] teclas = new KeySetting[QUANTOS];
    private final TextSetting[] textos = new TextSetting[QUANTOS];
    private final long[] ultimo = new long[QUANTOS];

    public AtalhosTexto() {
        super("atalhos_texto", "Atalhos de Texto", "Uma tecla manda uma mensagem ou um comando no servidor", Category.SERVIDORES, true);
        String[] exemplos = {"/lobby", "gg", "", "", "", ""};
        for (int i = 0; i < QUANTOS; i++) {
            teclas[i] = add(new KeySetting("tecla_" + (i + 1), "Atalho " + (i + 1) + ": tecla", 0));
            textos[i] = add(new TextSetting("texto_" + (i + 1), "Atalho " + (i + 1) + ": texto", exemplos[i], 100));
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) apertou(Keyboard.getEventKey());
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() >= 0) apertou(Mouse.getEventButton() - 100);
    }

    private void apertou(int codigo) {
        if (!isEnabled() || codigo == 0 || mc.currentScreen != null || mc.thePlayer == null) return;
        long agora = System.currentTimeMillis();
        for (int i = 0; i < QUANTOS; i++) {
            String texto = textos[i].get().trim();
            if (teclas[i].get() != codigo || texto.isEmpty()) continue;
            // Segurar a tecla não vira spam: no máximo uma mensagem por segundo em cada atalho.
            if (agora - ultimo[i] < ESPERA) continue;
            ultimo[i] = agora;
            mandar(texto);
        }
    }

    private void mandar(String texto) {
        mc.ingameGUI.getChatGUI().addToSentMessages(texto);
        // Comandos de outros mods (que rodam só no seu jogo) vão para o Forge, como no chat normal.
        if (texto.startsWith("/") && ClientCommandHandler.instance.executeCommand(mc.thePlayer, texto) != 0) return;
        mc.thePlayer.sendChatMessage(texto);
    }
}
