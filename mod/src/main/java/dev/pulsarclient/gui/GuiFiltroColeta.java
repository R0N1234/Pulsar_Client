package dev.pulsarclient.gui;

import dev.pulsarclient.mods.FiltroColeta;
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
 * Itens do Filtro de Coleta: cada item de PvP numa caixinha. Botão esquerdo deixa verde (pega do chão),
 * botão direito deixa vermelho (descarta). Segurando o botão e passando o mouse, pinta vários de uma vez.
 */
public class GuiFiltroColeta extends GuiScreen {
    private static final int TEXTO = 0xFFE6ECFF, CINZA = 0xFF8892B0, CIANO = 0xFF7FE7FF;
    private static final int VERDE_FUNDO = 0xB0216B45, VERDE_BORDA = 0xFF6EE7A8;
    private static final int VERMELHO_FUNDO = 0xB07A1F28, VERMELHO_BORDA = 0xFFFF5A5A;
    private static final int CEL = 20;
    private static final int TUDO_VERDE = 1, TUDO_VERMELHO = 2, FECHAR = 3;

    private final GuiScreen parent;
    private final FiltroColeta mod;
    private final List<ItemStack> itens = FiltroColeta.itens();
    private int px, py, pw, ph, gx, gy, colunas, linhasVisiveis, rolagem;
    /** Botão segurado para pintar arrastando: 0 verde, 1 vermelho, -1 nenhum. */
    private int pintando = -1;

    public GuiFiltroColeta(GuiScreen parent, FiltroColeta mod) {
        this.parent = parent;
        this.mod = mod;
    }

    @Override
    public void initGui() {
        pw = Math.min(400, width - 16);
        ph = Math.min(250, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        colunas = Math.max(1, (pw - 24) / CEL);
        gx = px + (pw - colunas * CEL) / 2;
        gy = py + 30;
        int linhas = (itens.size() + colunas - 1) / colunas;
        linhasVisiveis = Math.max(1, Math.min(linhas, (ph - 30 - 76) / CEL));
        rolagem = Math.max(0, Math.min(rolagem, maxRolagem()));

        buttonList.clear();
        int bw = (pw - 20 - 8) / 3, by = py + ph - 26;
        buttonList.add(new PulsarButton(TUDO_VERDE, px + 10, by, bw, 18, "Tudo verde"));
        buttonList.add(new PulsarButton(TUDO_VERMELHO, px + 14 + bw, by, bw, 18, "Tudo vermelho"));
        buttonList.add(new PulsarButton(FECHAR, px + 18 + 2 * bw, by, bw, 18, "Fechar"));
    }

    private int maxRolagem() {
        int linhas = (itens.size() + colunas - 1) / colunas;
        return Math.max(0, linhas - linhasVisiveis);
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == TUDO_VERDE) mod.marcarTodos(false);
        else if (b.id == TUDO_VERMELHO) mod.marcarTodos(true);
        else if (b.id == FECHAR) mc.displayGuiScreen(parent);
    }

    /** Índice do item na posição, ou -1. */
    private int itemEm(int mx, int my) {
        if (mx < gx || mx >= gx + colunas * CEL || my < gy || my >= gy + linhasVisiveis * CEL) return -1;
        int i = ((my - gy) / CEL + rolagem) * colunas + (mx - gx) / CEL;
        return i < itens.size() ? i : -1;
    }

    @Override
    protected void mouseClicked(int mx, int my, int botao) throws IOException {
        super.mouseClicked(mx, my, botao);
        int i = itemEm(mx, my);
        if (i < 0 || botao > 1) return;
        pintando = botao;
        mod.marcar(itens.get(i), botao == 1);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int botao, long tempo) {
        super.mouseClickMove(mx, my, botao, tempo);
        if (pintando < 0 || botao != pintando) return;
        int i = itemEm(mx, my);
        if (i >= 0) mod.marcar(itens.get(i), pintando == 1);
    }

    @Override
    protected void mouseReleased(int mx, int my, int estado) {
        super.mouseReleased(mx, my, estado);
        pintando = -1;
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
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        drawDefaultBackground();
        Desenho.caixaBorda(px, py, px + pw, py + ph, 0xF20A0F1E, 0xFF223A5E, 6);
        fontRendererObj.drawStringWithShadow("Filtro de Coleta", px + 10, py + 10, CIANO);
        String status = mod.isEnabled() ? "§aligado" : "§cdesligado";
        String direita = mod.quantosVermelhos() + " para descartar · " + status;
        fontRendererObj.drawString(direita, px + pw - 10 - fontRendererObj.getStringWidth(direita), py + 10, CINZA);

        int sob = itemEm(mx, my);
        for (int k = 0; k < linhasVisiveis * colunas; k++) {
            int i = rolagem * colunas + k;
            if (i >= itens.size()) break;
            int x = gx + (k % colunas) * CEL, y = gy + (k / colunas) * CEL;
            boolean vermelho = mod.descarta(itens.get(i));
            drawRect(x + 1, y + 1, x + CEL - 1, y + CEL - 1, vermelho ? VERMELHO_FUNDO : VERDE_FUNDO);
            Desenho.borda(x + 1, y + 1, x + CEL - 1, y + CEL - 1, i == sob ? 0xFFFFFFFF : vermelho ? VERMELHO_BORDA : VERDE_BORDA, 2);
        }
        if (maxRolagem() > 0) {
            int alto = linhasVisiveis * CEL, barra = Math.max(10, alto * linhasVisiveis / (linhasVisiveis + maxRolagem()));
            int y = gy + (alto - barra) * rolagem / maxRolagem();
            drawRect(px + pw - 6, y, px + pw - 4, y + barra, CIANO);
        }

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        for (int k = 0; k < linhasVisiveis * colunas; k++) {
            int i = rolagem * colunas + k;
            if (i >= itens.size()) break;
            itemRender.renderItemAndEffectIntoGUI(itens.get(i), gx + (k % colunas) * CEL + 2, gy + (k / colunas) * CEL + 2);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();

        int ty = gy + linhasVisiveis * CEL + 6;
        if (sob >= 0) {
            ItemStack s = itens.get(sob);
            boolean vermelho = mod.descarta(s);
            fontRendererObj.drawStringWithShadow(s.getDisplayName() + (vermelho ? " §c(descarta)" : " §a(pega)"), gx, ty, TEXTO);
        } else {
            fontRendererObj.drawString("§aBotão esquerdo: pega§8  ·  §cBotão direito: descarta", gx, ty, CINZA);
        }
        fontRendererObj.drawString("Blocos, minérios e itens fora da lista sempre são pegos.", gx, ty + 11, CINZA);
        fontRendererObj.drawString("Segure o botão e arraste para pintar vários.", gx, ty + 22, CINZA);
        if (!mod.isEnabled()) fontRendererObj.drawString("Ligue o mod no painel para o filtro funcionar.", gx, ty + 33, 0xFFFFD84A);

        super.drawScreen(mx, my, partialTicks);
    }
}
