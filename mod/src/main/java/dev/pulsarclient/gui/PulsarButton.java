package dev.pulsarclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Botão no estilo do Pulsar: fundo escuro translúcido e borda ciano quando o mouse passa por cima. */
public class PulsarButton extends GuiButton {
    private static final int BG = 0xB00A0F1E;
    private static final int BG_HOVER = 0xD0142540;
    private static final int LINE = 0x557FE7FF;
    private static final int BEAM = 0xFF7FE7FF;

    public PulsarButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        int r = xPosition + width;
        int b = yPosition + height;
        drawRect(xPosition, yPosition, r, b, hovered ? BG_HOVER : BG);
        int line = hovered ? BEAM : LINE;
        drawRect(xPosition, yPosition, r, yPosition + 1, line);
        drawRect(xPosition, b - 1, r, b, line);
        drawRect(xPosition, yPosition, xPosition + 1, b, line);
        drawRect(r - 1, yPosition, r, b, line);
        if (hovered) drawRect(xPosition + 1, b - 2, r - 1, b - 1, 0x887FE7FF);
        int color = !enabled ? 0xFF6B7490 : hovered ? BEAM : 0xFFE6ECFF;
        drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, color);
    }
}
