package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.mods.MiraPulsar;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Desenho da Mira de Pulsar: uma grade de quadradinhos. Botão esquerdo pinta, botão direito apaga
 * (dá para arrastar). Ao lado, a mira no tamanho real, do jeito que aparece no jogo.
 */
public class GuiMiraEditor extends GuiScreen {
    private static final int BG = 0xF20A0F1E;
    private static final int LINE = 0x337FE7FF;
    private static final int BEAM = 0xFF7FE7FF;
    private static final int TEXT = 0xFFE6ECFF;
    private static final int MUTED = 0xFF8892B0;
    private static final int N = MiraPulsar.GRADE;

    private static final int LIMPAR = 1;
    private static final int ESPELHAR = 2;
    private static final int USAR = 3;
    private static final int VOLTAR = 4;

    private final GuiScreen parent;
    private final MiraPulsar mira;
    private final boolean[][] grid;
    private int cell, gx, gy, px, py, pw, ph;
    // Pintando (true) ou apagando (false) enquanto arrasta; null = sem arrastar.
    private Boolean painting;

    public GuiMiraEditor(GuiScreen parent, MiraPulsar mira) {
        this.parent = parent;
        this.mira = mira;
        this.grid = mira.grid();
    }

    @Override
    public void initGui() {
        cell = Math.max(5, Math.min(11, (height - 90) / N));
        pw = Math.min(width - 16, N * cell + 150);
        ph = N * cell + 62;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        gx = px + 10;
        gy = py + 26;
        int bx = gx + N * cell + 14;
        int bw = px + pw - 10 - bx;
        buttonList.clear();
        buttonList.add(new PulsarButton(LIMPAR, bx, py + ph - 80, bw, 18, "Limpar"));
        buttonList.add(new PulsarButton(ESPELHAR, bx, py + ph - 58, bw, 18, "Espelhar (simétrica)"));
        buttonList.add(new PulsarButton(USAR, bx, py + ph - 36, bw, 18, "Usar esta mira"));
        buttonList.add(new PulsarButton(VOLTAR, px + 10, py + ph - 28, 60, 18, "Voltar"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x66000000);
        drawRect(px, py, px + pw, py + ph, BG);
        GuiPulsarPanel.outline(px, py, px + pw, py + ph, LINE);
        fontRendererObj.drawString("Desenhar a mira", px + 10, py + 9, TEXT);
        fontRendererObj.drawString("Esquerdo pinta, direito apaga", px + pw - 10 - fontRendererObj.getStringWidth("Esquerdo pinta, direito apaga"), py + 9, MUTED);

        int color = mira.color();
        int m = N / 2;
        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                int l = gx + x * cell, t = gy + y * cell;
                boolean meio = x == m || y == m;
                drawRect(l, t, l + cell - 1, t + cell - 1, grid[x][y] ? color : meio ? 0xFF1A2540 : 0xFF111A2D);
            }
        }
        // O quadradinho do meio é o centro da tela.
        GuiPulsarPanel.outline(gx + m * cell - 1, gy + m * cell - 1, gx + (m + 1) * cell, gy + (m + 1) * cell, BEAM);

        // Prévia no tamanho real, num fundo claro e num escuro.
        int bx = gx + N * cell + 14;
        int bw = px + pw - 10 - bx;
        int half = bw / 2;
        fontRendererObj.drawString("No jogo:", bx, gy, MUTED);
        drawRect(bx, gy + 12, bx + half, gy + 62, 0xFF8EB8E8);
        drawRect(bx + half, gy + 12, bx + bw, gy + 62, 0xFF2B2B2B);
        MiraPulsar.drawGrid(grid, bx + half / 2, gy + 37, mira.pixel(), color, mira.outlined());
        MiraPulsar.drawGrid(grid, bx + half + (bw - half) / 2, gy + 37, mira.pixel(), color, mira.outlined());
        fontRendererObj.drawString("Cor, contorno e", bx, gy + 68, MUTED);
        fontRendererObj.drawString("espessura: no painel", bx, gy + 78, MUTED);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private boolean paint(int mouseX, int mouseY) {
        if (painting == null || mouseX < gx || mouseY < gy) return false;
        int x = (mouseX - gx) / cell, y = (mouseY - gy) / cell;
        if (x >= N || y >= N) return false;
        grid[x][y] = painting;
        return true;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0 && button != 1) return;
        painting = button == 0;
        if (!paint(mouseX, mouseY)) painting = null;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long time) {
        paint(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        painting = null;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == LIMPAR) {
            for (boolean[] col : grid) java.util.Arrays.fill(col, false);
        } else if (button.id == ESPELHAR) {
            // Copia o que está pintado para os quatro lados, deixando a mira simétrica.
            for (int x = 0; x < N; x++) for (int y = 0; y < N; y++) {
                if (grid[x][y]) {
                    grid[N - 1 - x][y] = true;
                    grid[x][N - 1 - y] = true;
                    grid[N - 1 - x][N - 1 - y] = true;
                }
            }
        } else if (button.id == USAR) {
            mira.setGrid(grid);
            mira.useDrawing();
            if (!mira.isEnabled()) mira.setEnabled(true);
            close();
        } else if (button.id == VOLTAR) {
            close();
        }
    }

    private void close() {
        mira.setGrid(grid);
        PulsarClient.get().save();
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) close();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
