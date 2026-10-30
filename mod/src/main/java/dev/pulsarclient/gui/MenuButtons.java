package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.mods.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Troca o menu principal pelo do Pulsar e põe botões do Pulsar no menu do Esc e nas Opções de vídeo.
 *
 * No menu do Esc também entra o Multijogador: dá para escolher outro servidor sem sair do atual.
 * A conexão antiga só é fechada quando a nova começa; voltando sem escolher, o jogador continua no servidor.
 */
public class MenuButtons {
    private static final int PANEL = 0x50A1;
    private static final int MOTOR = 0x50A2;
    private static final int MULTI = 0x50A3;

    /** Conexão com o servidor de onde o jogador abriu o Multijogador do Esc. */
    private NetworkManager anterior;
    /** O jogador abriu o Multijogador pelo Esc e ainda não voltou para o menu do Esc. */
    private boolean trocando;

    @SubscribeEvent
    public void onOpen(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.gui instanceof GuiConnecting && anterior != null) {
            // O jogo já largou o mundo antigo ao começar a nova conexão; falta avisar o servidor antigo.
            NetworkManager velha = anterior;
            anterior = null;
            if (velha.isChannelOpen()) velha.closeChannel(new ChatComponentText("Trocando de servidor"));
            return;
        }
        if (event.gui instanceof GuiIngameMenu) {
            // Voltando do Multijogador depois de uma conexão que não deu certo: já não há mundo para voltar.
            if (trocando && mc.theWorld == null) event.gui = new GuiPulsarMenu();
            trocando = false;
            anterior = null;
            return;
        }
        // Só o menu do jogo puro; um menu de outro mod que estenda o do jogo fica como está.
        if (event.gui != null && event.gui.getClass() == GuiMainMenu.class) event.gui = new GuiPulsarMenu();
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        if (gui instanceof GuiIngameMenu) {
            // Abaixo do "Sair", onde o menu do Esc não tem nada. No mundo do singleplayer não há servidor para trocar.
            int y = gui.height / 4 + 128;
            if (Minecraft.getMinecraft().isIntegratedServerRunning()) {
                event.buttonList.add(new GuiButton(PANEL, gui.width / 2 - 100, y, "Pulsar Client"));
            } else {
                event.buttonList.add(new GuiButton(PANEL, gui.width / 2 - 100, y, 98, 20, "Pulsar Client"));
                event.buttonList.add(new GuiButton(MULTI, gui.width / 2 + 2, y, 98, 20, I18n.format("menu.multiplayer")));
            }
        } else if (gui instanceof GuiVideoSettings) {
            event.buttonList.add(new GuiButton(MOTOR, gui.width - 96, 4, 90, 20, "Motor Pulsar"));
        } else if (gui instanceof GuiMainMenu) {
            event.buttonList.add(new GuiButton(PANEL, 4, 4, 80, 20, "Pulsar"));
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        int id = event.button.id;
        if (id != PANEL && id != MOTOR && id != MULTI) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (id == MULTI) {
            if (!(event.gui instanceof GuiIngameMenu)) return;
            anterior = mc.getNetHandler() != null ? mc.getNetHandler().getNetworkManager() : null;
            trocando = true;
            mc.displayGuiScreen(new GuiMultiplayer(event.gui));
        } else if (id == PANEL) mc.displayGuiScreen(new GuiPulsarPanel(event.gui));
        else mc.displayGuiScreen(new GuiPulsarPanel(event.gui, Category.MOTOR, PulsarClient.get().motor()));
        event.setCanceled(true);
    }
}
