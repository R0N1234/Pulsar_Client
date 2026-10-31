package dev.pulsarclient.hud;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.gui.GuiPulsarPanel;
import dev.pulsarclient.gui.Universo;
import dev.pulsarclient.mods.HudMod;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor de HUD, aberto pelo painel do Pulsar. Só aparecem os mods de HUD ligados (liga e desliga no painel).
 * Arraste para mover, roda do mouse muda o tamanho e o botão direito abre as opções do mod.
 * A cruz no meio da tela divide os 4 cantos e puxa o HUD como um ímã para alinhar no centro.
 */
public class GuiHudEditor extends GuiScreen {
    private static final int OUTLINE = 0x887FE7FF;
    private static final int CROSS = 0x307FE7FF;
    private static final int CROSS_ON = 0xD07FE7FF;
    private static final int SNAP = 4;

    private final GuiScreen parent;
    private HudMod dragging;
    private int offsetX;
    private int offsetY;
    private boolean snappedX;
    private boolean snappedY;

    public GuiHudEditor(GuiScreen parent) {
        this.parent = parent;
    }

    /** Os HUDs ligados, na ordem em que são desenhados. */
    private List<HudMod> visible() {
        List<HudMod> out = new ArrayList<HudMod>();
        for (HudMod mod : PulsarClient.get().hudMods()) if (mod.isEnabled()) out.add(mod);
        return out;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x55000000);
        drawCross();
        drawCenteredString(fontRendererObj, "Pulsar · Editor de HUD", width / 2, 12, 0xFF7FE7FF);
        drawCenteredString(fontRendererObj, "Arraste para mover. Roda do mouse muda o tamanho. Botão direito abre as opções.", width / 2, 24, 0xFFB0BCD8);

        List<HudMod> mods = visible();
        if (mods.isEmpty()) {
            drawCenteredString(fontRendererObj, "Nenhum HUD ligado. Ligue os mods de HUD no painel (Shift direito).", width / 2, height / 2 + 14, 0xFFB0BCD8);
        }
        ScaledResolution res = new ScaledResolution(mc);
        HudMod hovered = dragging != null ? dragging : modAt(mouseX, mouseY);
        for (HudMod mod : mods) {
            try {
                drawMod(mod, res, mod == hovered);
            } catch (Throwable t) {
                PulsarClient.get().fail(mod, t);
            }
        }
        if (hovered != null) {
            String info = hovered.name() + " · " + hovered.sizeText();
            int ty = hovered.y - 13 < 2 ? hovered.y + hovered.boxHeight() + 5 : hovered.y - 13;
            fontRendererObj.drawStringWithShadow(info, hovered.x, ty, 0xFFFFFFFF);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Cruz no meio da tela: acende o lado em que o HUD arrastado está encostado. */
    private void drawCross() {
        int cx = width / 2;
        int cy = height / 2;
        drawRect(cx, 0, cx + 1, height, dragging != null && snappedX ? CROSS_ON : CROSS);
        drawRect(0, cy, width, cy + 1, dragging != null && snappedY ? CROSS_ON : CROSS);
        drawRect(cx - 2, cy - 2, cx + 3, cy + 3, CROSS_ON);
    }

    private void drawMod(HudMod mod, ScaledResolution res, boolean hovered) {
        PulsarClient.keepOnScreen(mod, res);
        outline(mod.x - 3, mod.y - 3, mod.x + mod.boxWidth() + 3, mod.y + mod.boxHeight() + 3, hovered ? 0xFF7FE7FF : OUTLINE);
        mod.draw(true);
    }

    private static void outline(int l, int t, int r, int b, int color) {
        Gui.drawRect(l, t, r, t + 1, color);
        Gui.drawRect(l, b - 1, r, b, color);
        Gui.drawRect(l, t, l + 1, b, color);
        Gui.drawRect(r - 1, t, r, b, color);
    }

    private HudMod modAt(int mouseX, int mouseY) {
        // Do último para o primeiro, para pegar o que está desenhado por cima.
        List<HudMod> mods = visible();
        for (int i = mods.size() - 1; i >= 0; i--) {
            if (mods.get(i).contains(mouseX, mouseY)) return mods.get(i);
        }
        return null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        HudMod mod = modAt(mouseX, mouseY);
        if (mod == null) return;
        if (button == 1) {
            mc.displayGuiScreen(GuiPulsarPanel.options(this, mod));
        } else if (button == 0) {
            dragging = mod;
            offsetX = mouseX - mod.x;
            offsetY = mouseY - mod.y;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (dragging == null) return;
        int w = dragging.boxWidth();
        int h = dragging.boxHeight();
        int cx = width / 2;
        int cy = height / 2;
        // Ímã da cruz: o meio ou um dos lados do HUD encosta na linha quando chega perto.
        int x = mouseX - offsetX;
        int y = mouseY - offsetY;
        int sx = snap(x, cx - w / 2, cx, cx - w);
        int sy = snap(y, cy - h / 2, cy, cy - h);
        snappedX = sx != Integer.MIN_VALUE;
        snappedY = sy != Integer.MIN_VALUE;
        dragging.x = clamp(snappedX ? sx : x, 2, width - w - 2);
        dragging.y = clamp(snappedY ? sy : y, 2, height - h - 2);
    }

    private static int snap(int v, int... targets) {
        for (int t : targets) if (Math.abs(v - t) <= SNAP) return t;
        return Integer.MIN_VALUE;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
        snappedX = false;
        snappedY = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        HudMod mod = dragging != null ? dragging : modAt(mouseX, mouseY);
        if (mod != null) mod.resize(wheel > 0 ? 1 : -1);
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
