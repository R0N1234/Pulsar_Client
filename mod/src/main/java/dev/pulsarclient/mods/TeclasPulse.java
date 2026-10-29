package dev.pulsarclient.mods;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

/** Teclas Pulse: WASD, cliques com CPS e barra de espaço. Equivale ao Keystrokes. */
public class TeclasPulse extends HudMod {
    private static final int KEY = 22;
    private static final int GAP = 2;
        private static final int PRESSED_TEXT = 0xFF04121A;

    public TeclasPulse() {
        super("teclas_pulse", "Teclas Pulse", "WASD, cliques e pulo na tela", Category.COMBATE, 5, 5);
    }

    @Override public int width() { return KEY * 3 + GAP * 2; }
    @Override public int height() { return KEY * 2 + GAP * 3 + 18 + 10; }

    @Override
    public void render(boolean preview) {
        int full = width();
        int half = (full - GAP) / 2;
        key(mc.gameSettings.keyBindForward, "W", x + KEY + GAP, y, KEY, KEY);
        key(mc.gameSettings.keyBindLeft, "A", x, y + KEY + GAP, KEY, KEY);
        key(mc.gameSettings.keyBindBack, "S", x + KEY + GAP, y + KEY + GAP, KEY, KEY);
        key(mc.gameSettings.keyBindRight, "D", x + (KEY + GAP) * 2, y + KEY + GAP, KEY, KEY);
        int rowY = y + (KEY + GAP) * 2;
        key(mc.gameSettings.keyBindAttack, ClickTracker.left() + " CPS", x, rowY, half, 18);
        key(mc.gameSettings.keyBindUseItem, ClickTracker.right() + " CPS", x + half + GAP, rowY, half, 18);
        key(mc.gameSettings.keyBindJump, "", x, rowY + 18 + GAP, full, 8);
    }

    private void key(KeyBinding bind, String text, int kx, int ky, int w, int h) {
        boolean down = GameSettings.isKeyDown(bind);
        Gui.drawRect(kx, ky, kx + w, ky + h, down ? color.argb(0xCC) : background.get() ? BACKGROUND : 0x22000000);
        if (text.isEmpty()) {
            Gui.drawRect(kx + w / 4, ky + h / 2 - 1, kx + w * 3 / 4, ky + h / 2, down ? PRESSED_TEXT : WHITE);
            return;
        }
        int tx = kx + (w - font().getStringWidth(text)) / 2;
        int ty = ky + (h - 8) / 2;
        if (down) font().drawString(text, tx, ty, PRESSED_TEXT);
        else font().drawStringWithShadow(text, tx, ty, WHITE);
    }
}
