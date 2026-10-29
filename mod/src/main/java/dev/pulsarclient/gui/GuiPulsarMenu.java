package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.fml.client.GuiModList;

/** Menu principal do Pulsar: o universo de fundo, o logo "PC" e os botões do jogo. */
public class GuiPulsarMenu extends GuiScreen {
    private static final int SINGLE = 1;
    private static final int MULTI = 2;
    private static final int OPTIONS = 3;
    private static final int PANEL = 4;
    private static final int LANGUAGE = 5;
    private static final int MODS = 6;
    private static final int QUIT = 7;

    private int logoSize;
    private int logoY;

    @Override
    public void initGui() {
        logoSize = Math.max(48, Math.min(96, height / 4));
        int blockHeight = logoSize + 14 + 92;
        logoY = Math.max(4, (height - blockHeight) / 2 - 8);
        int y = logoY + logoSize + 16;
        int x = width / 2 - 100;
        buttonList.clear();
        buttonList.add(new PulsarButton(SINGLE, x, y, 200, 20, "Um jogador"));
        buttonList.add(new PulsarButton(MULTI, x, y + 24, 200, 20, "Multijogador"));
        buttonList.add(new PulsarButton(OPTIONS, x, y + 48, 98, 20, "Opções"));
        buttonList.add(new PulsarButton(PANEL, x + 102, y + 48, 98, 20, "Pulsar Client"));
        buttonList.add(new PulsarButton(LANGUAGE, x, y + 72, 64, 20, "Idioma"));
        buttonList.add(new PulsarButton(MODS, x + 68, y + 72, 64, 20, "Mods"));
        buttonList.add(new PulsarButton(QUIT, x + 136, y + 72, 64, 20, "Sair"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case SINGLE: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
            case MULTI: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case OPTIONS: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case PANEL: mc.displayGuiScreen(new GuiPulsarPanel(this)); break;
            case LANGUAGE: mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager())); break;
            case MODS: mc.displayGuiScreen(new GuiModList(this)); break;
            case QUIT: mc.shutdown(); break;
            default: break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Universo.draw(width, height);
        Universo.logo(width / 2 - logoSize / 2, logoY, logoSize);
        drawCenteredString(fontRendererObj, "P U L S A R   C L I E N T", width / 2, logoY + logoSize + 2, 0xFF7FE7FF);
        fontRendererObj.drawStringWithShadow("Pulsar Client " + PulsarClient.VERSION, 4, height - 20, 0xFFE6ECFF);
        fontRendererObj.drawStringWithShadow("Minecraft 1.8.9", 4, height - 10, 0xFF8892B0);
        String copy = "Copyright Mojang AB. Do not distribute!";
        fontRendererObj.drawStringWithShadow(copy, width - fontRendererObj.getStringWidth(copy) - 4, height - 10, 0xFF8892B0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
