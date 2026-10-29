package dev.pulsarclient.hud;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.mods.HudMod;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/**
 * Editor de HUD, aberto pelo painel do Pulsar.
 * Arraste com o botão esquerdo para mover. Botão direito liga ou desliga o mod.
 */
public class GuiHudEditor extends GuiScreen {
    private static final int OUTLINE = 0x887FE7FF;
    private static final int OUTLINE_OFF = 0x55FF6B6B;

    private final GuiScreen parent;
    private HudMod dragging;
    private int offsetX;
    private int offsetY;

    public GuiHudEditor(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x55000000);
        drawCenteredString(fontRendererObj, "Pulsar · Editor de HUD", width / 2, 12, 0xFF7FE7FF);
        drawCenteredString(fontRendererObj, "Arraste para mover. Botão direito liga ou desliga. Esc para voltar.", width / 2, 24, 0xFFB0BCD8);

        for (HudMod mod : PulsarClient.get().hudMods()) {
            int color = mod.isEnabled() ? OUTLINE : OUTLINE_OFF;
            outline(mod.x - 3, mod.y - 3, mod.x + mod.width() + 3, mod.y + mod.height() + 3, color);
            mod.render(true);
            if (!mod.isEnabled()) {
                drawRect(mod.x - 2, mod.y - 2, mod.x + mod.width() + 2, mod.y + mod.height() + 2, 0x99000000);
            }
            if (mod.contains(mouseX, mouseY)) {
                fontRendererObj.drawStringWithShadow(mod.name(), mod.x, mod.y - 13, 0xFFFFFFFF);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static void outline(int l, int t, int r, int b, int color) {
        Gui.drawRect(l, t, r, t + 1, color);
        Gui.drawRect(l, b - 1, r, b, color);
        Gui.drawRect(l, t, l + 1, b, color);
        Gui.drawRect(r - 1, t, r, b, color);
    }

    private HudMod modAt(int mouseX, int mouseY) {
        // Do último para o primeiro, para pegar o que está desenhado por cima.
        for (int i = PulsarClient.get().hudMods().size() - 1; i >= 0; i--) {
            HudMod mod = PulsarClient.get().hudMods().get(i);
            if (mod.contains(mouseX, mouseY)) return mod;
        }
        return null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        HudMod mod = modAt(mouseX, mouseY);
        if (mod == null) return;
        if (button == 1) {
            mod.setEnabled(!mod.isEnabled());
        } else if (button == 0) {
            dragging = mod;
            offsetX = mouseX - mod.x;
            offsetY = mouseY - mod.y;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (dragging == null) return;
        dragging.x = clamp(mouseX - offsetX, 2, width - dragging.width() - 2);
        dragging.y = clamp(mouseY - offsetY, 2, height - dragging.height() - 2);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        PulsarClient.get().save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
