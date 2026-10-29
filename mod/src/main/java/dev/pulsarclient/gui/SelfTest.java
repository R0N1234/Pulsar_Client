package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Só roda com -Dpulsar.selftest=true (teste do GitHub Actions).
 * Abre cada tela do Pulsar a partir do menu inicial e avisa no log, para o teste tirar um print de cada.
 */
public class SelfTest {
    private static final int STEP = 60;
    private int ticks = -1;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (ticks < 0) {
            if (mc.currentScreen instanceof GuiPulsarMenu || mc.currentScreen instanceof GuiMainMenu) ticks = 0;
            return;
        }
        ticks++;
        if (ticks % STEP != 0) return;
        GuiPulsarMenu menu = new GuiPulsarMenu();
        switch (ticks / STEP) {
            case 1: show("menu", menu); break;
            case 2: show("painel", new GuiPulsarPanel(menu)); break;
            case 3: show("motor", new GuiPulsarPanel(menu, Category.MOTOR, PulsarClient.get().motor())); break;
            case 4: show("video", new GuiVideoSettings(menu, mc.gameSettings)); break;
            case 5: show("esc", new GuiIngameMenu()); break;
            case 6: show("hud", new GuiHudEditor(menu)); break;
            case 7:
                mc.displayGuiScreen(menu);
                System.out.println("[Pulsar] selftest: fim");
                break;
            default: break;
        }
    }

    private static void show(String name, net.minecraft.client.gui.GuiScreen screen) {
        Minecraft.getMinecraft().displayGuiScreen(screen);
        System.out.println("[Pulsar] selftest: " + name);
    }
}
