package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.mods.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Põe botões do Pulsar no menu do Esc, nas Opções de vídeo e no menu inicial. */
public class MenuButtons {
    private static final int PANEL = 0x50A1;
    private static final int MOTOR = 0x50A2;

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        if (gui instanceof GuiIngameMenu) {
            // Abaixo do "Sair", onde o menu do Esc não tem nada.
            event.buttonList.add(new GuiButton(PANEL, gui.width / 2 - 100, gui.height / 4 + 128, "Pulsar Client"));
        } else if (gui instanceof GuiVideoSettings) {
            event.buttonList.add(new GuiButton(MOTOR, gui.width - 96, 4, 90, 20, "Motor Pulsar"));
        } else if (gui instanceof GuiMainMenu) {
            event.buttonList.add(new GuiButton(PANEL, 4, 4, 80, 20, "Pulsar"));
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        int id = event.button.id;
        if (id != PANEL && id != MOTOR) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (id == PANEL) mc.displayGuiScreen(new GuiPulsarPanel(event.gui));
        else mc.displayGuiScreen(new GuiPulsarPanel(event.gui, Category.MOTOR, PulsarClient.get().motor()));
        event.setCanceled(true);
    }
}
