package dev.pulsarclient.gui;

import dev.pulsarclient.mods.KitPvp;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * Layout do Kit PvP: à esquerda o inventário (3 fileiras e a barra embaixo), à direita os itens de PvP.
 * Clique num item da direita e depois nos espaços onde ele vai (dá para pôr em vários). Clicando num espaço
 * com item, ele sai do lugar e vai para o mouse; botão direito apaga o espaço ou solta o item do mouse.
 */
public class GuiKitPvp extends GuiScreen {
    private static final int TEXTO = 0xFFE6ECFF, CINZA = 0xFF8892B0, CIANO = 0xFF7FE7FF;
    private static final int CEL = 18;
    private static final int KIT = 1, COPIAR = 10, LIMPAR = 11, FECHAR = 12;

    private final GuiScreen parent;
    private final KitPvp mod;
    private final List<ItemStack> paleta = KitPvp.paleta();
    private ItemStack[] layout;
    private ItemStack mao;
    private int px, py, pw, ph, gx, gy, ax, colunas, linhasVisiveis, rolagem;

    public GuiKitPvp(GuiScreen parent, KitPvp mod) {
        this.parent = parent;
        this.mod = mod;
    }

    @Override
    public void initGui() {
        pw = Math.min(410, width - 16);
        ph = Math.min(236, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        gx = px + 10;
        gy = py + 34;
        ax = gx + 9 * CEL + 14;
        colunas = Math.max(1, (px + pw - 10 - ax) / CEL);
        linhasVisiveis = Math.max(1, (py + ph - 40 - gy) / CEL);
        rolagem = Math.max(0, Math.min(rolagem, maxRolagem()));
        layout = mod.layout(mod.kitAtual()).clone();

        buttonList.clear();
        for (int k = 0; k < 3; k++) {
            GuiButton b = new PulsarButton(KIT + k, px + pw - 10 - (3 - k) * 52 + 4, py + 6, 48, 16, "Kit " + (k + 1));
            b.enabled = k != mod.kitAtual();
            buttonList.add(b);
        }
        int by = gy + 4 * CEL + 10;
        GuiButton copiar = new PulsarButton(COPIAR, gx, by, 9 * CEL, 18, "Copiar do meu inventário");
        copiar.enabled = mc.thePlayer != null;
        buttonList.add(copiar);
        buttonList.add(new PulsarButton(LIMPAR, gx, by + 22, 9 * CEL / 2 - 2, 18, "Limpar"));
        buttonList.add(new PulsarButton(FECHAR, gx + 9 * CEL / 2 + 2, by + 22, 9 * CEL / 2 - 2, 18, "Fechar"));
    }

    private int maxRolagem() {
        int linhas = (paleta.size() + colunas - 1) / colunas;
        return Math.max(0, linhas - linhasVisiveis);
    }

    private void salvar() {
        mod.salvar(mod.kitAtual(), layout);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id >= KIT && b.id < KIT + 3) {
            mod.usarKit(b.id - KIT);
            dev.pulsarclient.PulsarClient.get().save();
        } else if (b.id == COPIAR && mc.thePlayer != null) {
            for (int i = 0; i < KitPvp.ESPACOS; i++) layout[i] = KitPvp.modelo(mc.thePlayer.inventory.getStackInSlot(i));
            salvar();
        } else if (b.id == LIMPAR) {
            layout = new ItemStack[KitPvp.ESPACOS];
            salvar();
        } else if (b.id == FECHAR) {
            mc.displayGuiScreen(parent);
            return;
        }
        initGui();
    }

    // ---------- posições ----------

    /** Espaço do inventário (0 a 35) na posição, ou -1. A barra (0 a 8) fica embaixo, separada. */
    private int espacoEm(int mx, int my) {
        if (mx < gx || mx >= gx + 9 * CEL) return -1;
        int col = (mx - gx) / CEL;
        if (my >= gy && my < gy + 3 * CEL) return 9 + (my - gy) / CEL * 9 + col;
        int barra = gy + 3 * CEL + 4;
        if (my >= barra && my < barra + CEL) return col;
        return -1;
    }

    private int xEspaco(int i) {
        return gx + (i % 9) * CEL;
    }

    private int yEspaco(int i) {
        return i < 9 ? gy + 3 * CEL + 4 : gy + (i / 9 - 1) * CEL;
    }

    private int paletaEm(int mx, int my) {
        if (mx < ax || mx >= ax + colunas * CEL || my < gy || my >= gy + linhasVisiveis * CEL) return -1;
        int i = ((my - gy) / CEL + rolagem) * colunas + (mx - ax) / CEL;
        return i < paleta.size() ? i : -1;
    }

    // ---------- mouse e teclado ----------

    @Override
    protected void mouseClicked(int mx, int my, int botao) throws IOException {
        super.mouseClicked(mx, my, botao);
        int p = paletaEm(mx, my);
        if (p >= 0 && botao == 0) {
            mao = paleta.get(p).copy();
            return;
        }
        int e = espacoEm(mx, my);
        if (e >= 0) {
            if (botao == 1) {
                layout[e] = null;
            } else if (mao != null) {
                layout[e] = mao.copy();
            } else if (layout[e] != null) {
                mao = layout[e];
                layout[e] = null;
            }
            salvar();
            return;
        }
        if (botao == 1) mao = null;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int roda = Mouse.getEventDWheel();
        if (roda != 0) rolagem = Math.max(0, Math.min(maxRolagem(), rolagem + (roda > 0 ? -1 : 1)));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            if (mao != null) mao = null;
            else mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ---------- desenho ----------

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        drawDefaultBackground();
        Desenho.caixaBorda(px, py, px + pw, py + ph, 0xF20A0F1E, 0xFF223A5E, 6);
        fontRendererObj.drawStringWithShadow("Kit PvP", px + 10, py + 10, CIANO);
        fontRendererObj.drawString("Inventário", gx, gy - 11, CINZA);
        fontRendererObj.drawString("Itens de PvP", ax, gy - 11, CINZA);

        ItemStack sobMouse = null;
        int e = espacoEm(mx, my);
        for (int i = 0; i < KitPvp.ESPACOS; i++) {
            int x = xEspaco(i), y = yEspaco(i);
            boolean em = i == e;
            drawRect(x, y, x + CEL - 1, y + CEL - 1, em ? 0xC0142540 : 0xA0060A16);
            Desenho.borda(x, y, x + CEL - 1, y + CEL - 1, em ? 0xFF7FE7FF : 0xFF1A2B48, 2);
            if (em) sobMouse = layout[i];
        }
        int p = paletaEm(mx, my);
        for (int k = 0; k < linhasVisiveis * colunas; k++) {
            int i = rolagem * colunas + k;
            if (i >= paleta.size()) break;
            int x = ax + (k % colunas) * CEL, y = gy + (k / colunas) * CEL;
            if (i == p) {
                drawRect(x, y, x + CEL - 1, y + CEL - 1, 0xC0142540);
                sobMouse = paleta.get(i);
            }
        }
        if (maxRolagem() > 0) {
            int alto = linhasVisiveis * CEL, barra = Math.max(10, alto * linhasVisiveis / (linhasVisiveis + maxRolagem()));
            int y = gy + (alto - barra) * rolagem / maxRolagem();
            drawRect(px + pw - 6, y, px + pw - 4, y + barra, 0xFF7FE7FF);
        }

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        for (int i = 0; i < KitPvp.ESPACOS; i++) if (layout[i] != null) itemRender.renderItemAndEffectIntoGUI(layout[i], xEspaco(i) + 1, yEspaco(i) + 1);
        for (int k = 0; k < linhasVisiveis * colunas; k++) {
            int i = rolagem * colunas + k;
            if (i >= paleta.size()) break;
            itemRender.renderItemAndEffectIntoGUI(paleta.get(i), ax + (k % colunas) * CEL + 1, gy + (k / colunas) * CEL + 1);
        }
        RenderHelper.disableStandardItemLighting();

        int by = gy + 4 * CEL + 54;
        String[] dicas = {
                "Clique num item da direita e depois nos espaços.",
                "Botão direito apaga o espaço.",
                "No baú: Shift + botão direito no item.",
                "Vale o tipo e o minério, não o encantamento.",
        };
        for (int i = 0; i < dicas.length && by + i * 10 < py + ph - 4; i++) fontRendererObj.drawString(dicas[i], gx, by + i * 10, CINZA);
        if (sobMouse != null) fontRendererObj.drawStringWithShadow(sobMouse.getDisplayName(), ax, py + ph - 16, TEXTO);

        super.drawScreen(mx, my, partialTicks);

        if (mao != null) {
            GlStateManager.translate(0, 0, 200);
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(mao, mx - 8, my - 8);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.translate(0, 0, -200);
        }
        GlStateManager.disableRescaleNormal();
    }
}
