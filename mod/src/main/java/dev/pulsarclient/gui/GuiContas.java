package dev.pulsarclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contas dentro do jogo: a conta original (Microsoft) e os nicks offline guardados no launcher.
 * Cada conta mostra a cabeça da skin; um clique troca de conta sem fechar o jogo, e dá para criar nick novo aqui mesmo.
 */
public class GuiContas extends GuiScreen {
    private static final int FUNDO = 0xF0080D22;
    private static final int CARD = 0xC0141B38;
    private static final int CARD_HOVER = 0xE01B2550;
    private static final int LINHA = 0x40A9B8E8;
    private static final int CIANO = 0xFF4DE8FF;
    private static final int VIOLETA = 0xFFA57CFF;
    private static final int VERDE = 0xFF5BF0B0;
    private static final int TEXTO = 0xFFF2F5FF;
    private static final int APAGADO = 0xFF8C97BC;
    private static final int ROW = 27;

    private static final int ADD = 1;
    private static final int MICROSOFT = 2;
    private static final int BACK = 3;

    private final GuiScreen parent;
    private GuiTextField nick;
    private int px, py, pw, ph, listTop, visiveis, rolagem, campoT, campoR;

    public GuiContas(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(340, width - 16);
        ph = Math.min(300, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listTop = py + 50;
        campoT = py + ph - 62;
        campoR = px + pw - 84;
        // Embaixo da lista: aviso, campo do nick novo e os botões.
        visiveis = Math.max(2, (campoT - 14 - listTop) / ROW);
        // Campo sem o fundo do Minecraft: a caixa é desenhada no estilo do Pulsar.
        nick = new GuiTextField(0, fontRendererObj, px + 20, campoT + 6, campoR - px - 30, 10);
        nick.setEnableBackgroundDrawing(false);
        nick.setMaxStringLength(16);
        buttonList.clear();
        buttonList.add(new Botao(ADD, campoR + 6, campoT, px + pw - 14 - campoR - 6, 20, "Adicionar", false));
        int metade = (pw - 28 - 10) * 3 / 5;
        buttonList.add(new Botao(MICROSOFT, px + 14, py + ph - 32, metade, 22, "Outra conta Microsoft", false));
        buttonList.add(new Botao(BACK, px + 24 + metade, py + ph - 32, pw - 38 - metade, 22, "Voltar", true));
        Conta.atualizarLista();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        nick.updateCursorCounter();
        Conta.Lista l = Conta.lista();
        boolean pode = l != null && l.liberado && !Conta.busy();
        nick.setEnabled(pode);
        nick.setVisible(l == null || l.liberado);
        buttonList.get(0).visible = l == null || l.liberado;
        buttonList.get(0).enabled = pode && !nick.getText().trim().isEmpty();
        buttonList.get(1).enabled = !Conta.busy() && Conta.temLauncher();
    }

    /** Linhas da lista: a conta original (se tiver) e os nicks offline. */
    private List<String[]> linhas(Conta.Lista l) {
        List<String[]> out = new ArrayList<String[]>();
        if (l == null) return out;
        if (l.microsoft != null) out.add(new String[] {"microsoft", l.microsoft});
        for (String n : l.offline) out.add(new String[] {n, n});
        return out;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x70000000);
        // Painel de vidro com borda ciano -> violeta e um brilho fraco em volta.
        Desenho.caixaGradiente(px - 3, py - 3, px + pw + 3, py + ph + 3, 0x2235D6F5, 0x229B6BFF, 14);
        Desenho.caixaBordaGradiente(px, py, px + pw, py + ph, FUNDO, CIANO, VIOLETA, 12);

        String atual = mc.getSession().getUsername();
        Cabecas.desenhar(atual, px + 16, py + 12, 26);
        drawRect(px + 50, py + 12, px + 51, py + 38, LINHA);
        GlStateManager.pushMatrix();
        GlStateManager.translate(px + 60, py + 12, 0);
        GlStateManager.scale(1.6f, 1.6f, 1f);
        fontRendererObj.drawString("Contas", 0, 0, TEXTO);
        GlStateManager.popMatrix();
        fontRendererObj.drawString("Jogando como " + fontRendererObj.trimStringToWidth(atual, pw - 150), px + 60, py + 29, APAGADO);

        Conta.Lista l = Conta.lista();
        List<String[]> linhas = linhas(l);
        if (!Conta.temLauncher()) {
            texto("Abra o jogo pelo launcher do Pulsar para trocar de conta.", listTop + 4, APAGADO);
        } else if (l == null) {
            texto("Carregando as contas do launcher...", listTop + 4, APAGADO);
        } else if (linhas.isEmpty()) {
            texto("Nenhuma conta ainda. Entre com a conta Microsoft.", listTop + 4, APAGADO);
        }
        rolagem = Math.max(0, Math.min(rolagem, linhas.size() - visiveis));
        for (int i = 0; i < Math.min(visiveis, linhas.size() - rolagem); i++) {
            String[] c = linhas.get(i + rolagem);
            int y = listTop + i * ROW;
            int l0 = px + 14, r0 = px + pw - 14, b0 = y + ROW - 4;
            boolean original = "microsoft".equals(c[0]);
            boolean emUso = c[1].equals(atual);
            boolean hover = !emUso && inside(mouseX, mouseY, l0, y, r0, b0) && !Conta.busy();
            int cor = emUso ? VERDE : original ? CIANO : VIOLETA;
            if (emUso) {
                Desenho.caixa(l0 - 2, y - 2, r0 + 2, b0 + 2, 0x285BF0B0, 7);
                Desenho.caixaBorda(l0, y, r0, b0, 0xE0102A2E, VERDE, 6);
            } else {
                Desenho.caixaBorda(l0, y, r0, b0, hover ? CARD_HOVER : CARD, hover ? (cor & 0xFFFFFF) | 0x90000000 : 0x30A9B8E8, 6);
            }
            Cabecas.desenhar(c[1], l0 + 5, y + 3, 17);
            int meio = y + (ROW - 4) / 2;
            Desenho.caixa(l0 + 28, meio - 1, l0 + 31, meio + 2, cor, 1);
            fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(c[1], pw - 140), l0 + 36, meio - 4, TEXTO);
            String tag = emUso ? "em uso" : hover ? "usar" : original ? "original" : "offline";
            int tw = fontRendererObj.getStringWidth(tag);
            int tagCor = hover ? CIANO : cor;
            fontRendererObj.drawStringWithShadow(tag, r0 - 8 - tw, meio - 4, tagCor);
            Desenho.caixa(r0 - 15 - tw, meio - 1, r0 - 12 - tw, meio + 2, tagCor, 1);
        }
        if (linhas.size() > visiveis) {
            String mais = (rolagem + visiveis < linhas.size() ? "role para ver mais  " : "") + (rolagem + 1) + "-" + Math.min(linhas.size(), rolagem + visiveis) + " de " + linhas.size();
            fontRendererObj.drawString(mais, px + pw - 14 - fontRendererObj.getStringWidth(mais), listTop + visiveis * ROW - 2, APAGADO);
        }

        String status = Conta.status();
        if (!status.isEmpty()) texto(status, campoT - 12, Conta.busy() ? CIANO : 0xFFB0BCD8);
        if (l != null && !l.liberado) {
            texto("Entre uma vez com a conta original no launcher para liberar os nicks offline.", campoT + 2, APAGADO);
        } else {
            Desenho.caixaBorda(px + 14, campoT, campoR, campoT + 20, 0xE00A1028, nick.isFocused() ? CIANO : 0x55A9B8E8, 6);
            nick.drawTextBox();
            if (nick.getText().isEmpty() && !nick.isFocused()) fontRendererObj.drawString("Novo nick offline", px + 20, campoT + 6, APAGADO);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void texto(String t, int y, int cor) {
        List<String> ls = fontRendererObj.listFormattedStringToWidth(t, pw - 28);
        for (int i = 0; i < ls.size() && i < 2; i++) fontRendererObj.drawString(ls.get(i), px + 14, y + i * 10, cor);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ADD) adicionar();
        else if (button.id == MICROSOFT) Conta.trocar();
        else if (button.id == BACK) mc.displayGuiScreen(parent);
    }

    private void adicionar() {
        String n = nick.getText().trim();
        if (n.isEmpty() || Conta.busy()) return;
        Conta.adicionarOffline(n);
        nick.setText("");
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        nick.mouseClicked(mouseX, mouseY, button);
        // A caixa inteira do campo (e não só a linha do texto) dá foco.
        if (nick.getVisible() && inside(mouseX, mouseY, px + 14, campoT, campoR, campoT + 20)) nick.setFocused(true);
        if (button != 0 || Conta.busy()) return;
        List<String[]> linhas = linhas(Conta.lista());
        String atual = mc.getSession().getUsername();
        for (int i = 0; i < Math.min(visiveis, linhas.size() - rolagem); i++) {
            int y = listTop + i * ROW;
            if (!inside(mouseX, mouseY, px + 14, y, px + pw - 14, y + ROW - 4)) continue;
            String[] c = linhas.get(i + rolagem);
            if (c[1].equals(atual)) return;
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new net.minecraft.util.ResourceLocation("gui.button.press"), 1f));
            Conta.usar(c[0]);
            return;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int roda = Mouse.getEventDWheel();
        if (roda != 0) rolagem += roda > 0 ? -1 : 1;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (nick.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                adicionar();
                return;
            }
            // Nick do Minecraft: só letras sem acento, números e _.
            boolean imprimivel = typedChar >= 32 && typedChar != 127 && !isCtrlKeyDown();
            boolean valido = (typedChar >= 'a' && typedChar <= 'z') || (typedChar >= 'A' && typedChar <= 'Z') || (typedChar >= '0' && typedChar <= '9') || typedChar == '_';
            if (imprimivel && !valido) return;
            nick.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static boolean inside(int mouseX, int mouseY, int l, int t, int r, int b) {
        return mouseX >= l && mouseX < r && mouseY >= t && mouseY < b;
    }

    /** Botão com cantos arredondados e borda fina; o principal (Voltar) tem a borda ciano. */
    private static final class Botao extends GuiButton {
        private final boolean principal;

        Botao(int id, int x, int y, int w, int h, String texto, boolean principal) {
            super(id, x, y, w, h, texto);
            this.principal = principal;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
            int l = xPosition, t = yPosition, r = xPosition + width, b = yPosition + height;
            boolean aceso = hovered && enabled;
            if (principal && aceso) Desenho.caixa(l - 2, t - 2, r + 2, b + 2, 0x304DE8FF, 7);
            int borda = principal ? (aceso ? 0xFFA6F4FF : CIANO) : aceso ? 0xFFB9A4FF : 0x8C8E8FE0;
            int fundo = principal ? (aceso ? 0xF0123346 : 0xE00C1A30) : aceso ? 0xF01A1F4A : 0xE00E1330;
            Desenho.caixaBorda(l, t, r, b, fundo, borda, 6);
            int cor = !enabled ? 0xFF6B7490 : principal ? (aceso ? 0xFFFFFFFF : CIANO) : aceso ? 0xFFFFFFFF : TEXTO;
            drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, cor);
        }
    }
}
