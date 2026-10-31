package dev.pulsarclient.mods;

import dev.pulsarclient.gui.GuiMiraEditor;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Mira de Pulsar: troca a mira do jogo por uma configurável, ou por uma desenhada quadradinho por quadradinho. */
public class MiraPulsar extends PulsarMod {
    /** Tamanho da grade do desenho (ímpar, para ter um quadradinho no meio). */
    public static final int GRADE = 15;
    private static final String DESENHO_PADRAO = padrao();

    private final ModeSetting shape = add(new ModeSetting("forma", "Formato", 0, "Cruz", "Cruz + ponto", "Ponto", "Quadrado", "Desenhada"));
    private final ActionSetting draw = add(new ActionSetting("desenhar", "Desenhar a mira", new Runnable() {
        @Override public void run() { mc.displayGuiScreen(new GuiMiraEditor(mc.currentScreen, MiraPulsar.this)); }
    }));
    // Quadradinhos do desenho, linha por linha: "1" pintado, "0" vazio.
    private final TextSetting drawing = add(new TextSetting("desenho", DESENHO_PADRAO));
    private final ColorSetting color = add(new ColorSetting("cor", "Cor", ColorSetting.CIANO));
    private final SliderSetting size = add(new SliderSetting("tamanho", "Tamanho", 5, 1, 12, 1, ""));
    private final SliderSetting gap = add(new SliderSetting("espaco", "Espaço no meio", 2, 0, 8, 1, ""));
    private final SliderSetting thick = add(new SliderSetting("espessura", "Espessura", 1, 1, 3, 1, ""));
    private final BoolSetting outline = add(new BoolSetting("contorno", "Contorno preto", true));

    public MiraPulsar() {
        super("mira_pulsar", "Mira de Pulsar", "Mira com formato, cor e tamanho, ou desenhada por você", Category.COMBATE, false);
    }

    private static String padrao() {
        StringBuilder b = new StringBuilder();
        int m = GRADE / 2;
        for (int y = 0; y < GRADE; y++) {
            for (int x = 0; x < GRADE; x++) {
                boolean braco = (x == m && Math.abs(y - m) >= 2 && Math.abs(y - m) <= 5) || (y == m && Math.abs(x - m) >= 2 && Math.abs(x - m) <= 5);
                b.append(braco || (x == m && y == m) ? '1' : '0');
            }
        }
        return b.toString();
    }

    /** O desenho como grade [x][y]. */
    public boolean[][] grid() {
        boolean[][] g = new boolean[GRADE][GRADE];
        String s = drawing.get();
        if (s.length() != GRADE * GRADE) s = DESENHO_PADRAO;
        for (int i = 0; i < s.length(); i++) g[i % GRADE][i / GRADE] = s.charAt(i) == '1';
        return g;
    }

    public void setGrid(boolean[][] g) {
        StringBuilder b = new StringBuilder();
        for (int y = 0; y < GRADE; y++) for (int x = 0; x < GRADE; x++) b.append(g[x][y] ? '1' : '0');
        drawing.set(b.toString());
    }

    /** Usado pelo editor ao confirmar: passa a usar o desenho. */
    public void useDrawing() {
        shape.set(4);
    }

    public int color() {
        return color.argb(0xFF);
    }

    public boolean outlined() {
        return outline.get();
    }

    public int pixel() {
        return thick.getInt();
    }

    /** Desenha a mira desenhada com o meio em (cx, cy). Usado na tela do jogo e na prévia do editor. */
    public static void drawGrid(boolean[][] g, int cx, int cy, int px, int c, boolean outline) {
        int m = GRADE / 2;
        if (outline) {
            // Contorno só em volta do desenho, sem escurecer onde os quadradinhos se encostam.
            boolean[][] borda = new boolean[GRADE + 2][GRADE + 2];
            for (int x = 0; x < GRADE; x++) for (int y = 0; y < GRADE; y++) {
                if (!g[x][y]) continue;
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= GRADE || ny >= GRADE || !g[nx][ny]) borda[nx + 1][ny + 1] = true;
                }
            }
            for (int x = 0; x < GRADE + 2; x++) for (int y = 0; y < GRADE + 2; y++) {
                if (!borda[x][y]) continue;
                int l = cx + (x - 1 - m) * px, t = cy + (y - 1 - m) * px;
                Gui.drawRect(l, t, l + px, t + px, 0xAA000000);
            }
        }
        for (int x = 0; x < GRADE; x++) for (int y = 0; y < GRADE; y++) {
            if (!g[x][y]) continue;
            int l = cx + (x - m) * px, t = cy + (y - m) * px;
            Gui.drawRect(l, t, l + px, t + px, c);
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (!isEnabled() || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (mc.gameSettings.thirdPersonView != 0) return;
        event.setCanceled(true);
        int cx = event.resolution.getScaledWidth() / 2;
        int cy = event.resolution.getScaledHeight() / 2;
        int s = size.getInt();
        int g = gap.getInt();
        int t = thick.getInt();
        int c = color.argb(0xFF);
        String mode = shape.get();
        if (mode.equals("Desenhada")) {
            drawGrid(grid(), cx, cy, t, c, outline.get());
            return;
        }
        if (mode.startsWith("Cruz")) {
            bar(cx - g - s, cy - t / 2, cx - g, cy - t / 2 + t, c);
            bar(cx + g + 1, cy - t / 2, cx + g + 1 + s, cy - t / 2 + t, c);
            bar(cx - t / 2, cy - g - s, cx - t / 2 + t, cy - g, c);
            bar(cx - t / 2, cy + g + 1, cx - t / 2 + t, cy + g + 1 + s, c);
        }
        if (mode.equals("Cruz + ponto") || mode.equals("Ponto")) bar(cx - t / 2, cy - t / 2, cx - t / 2 + t, cy - t / 2 + t, c);
        if (mode.equals("Quadrado")) {
            bar(cx - s, cy - s, cx + s + 1, cy - s + t, c);
            bar(cx - s, cy + s + 1 - t, cx + s + 1, cy + s + 1, c);
            bar(cx - s, cy - s, cx - s + t, cy + s + 1, c);
            bar(cx + s + 1 - t, cy - s, cx + s + 1, cy + s + 1, c);
        }
    }

    private void bar(int l, int t, int r, int b, int c) {
        if (outline.get()) Gui.drawRect(l - 1, t - 1, r + 1, b + 1, 0xAA000000);
        Gui.drawRect(l, t, r, b, c);
    }
}
