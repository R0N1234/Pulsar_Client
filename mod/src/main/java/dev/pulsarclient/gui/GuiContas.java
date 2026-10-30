package dev.pulsarclient.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contas dentro do jogo: a conta original (Microsoft) e os nicks offline guardados no launcher.
 * Um clique troca de conta sem fechar o jogo; dá para criar nick novo aqui mesmo.
 */
public class GuiContas extends GuiScreen {
    private static final int BG = 0xF20A0F1E;
    private static final int CARD = 0xFF111A2D;
    private static final int CARD_HOVER = 0xFF18233C;
    private static final int LINE = 0x337FE7FF;
    private static final int BEAM = 0xFF7FE7FF;
    private static final int TEXT = 0xFFE6ECFF;
    private static final int MUTED = 0xFF8892B0;
    private static final int NEBULA = 0xFF9B7CFF;
    private static final int VERDE = 0xFF6EE7A8;
    private static final int ROW = 22;

    private static final int ADD = 1;
    private static final int MICROSOFT = 2;
    private static final int BACK = 3;

    private final GuiScreen parent;
    private GuiTextField nick;
    private int px, py, pw, ph, listTop, visiveis, rolagem;

    public GuiContas(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(300, width - 16);
        ph = Math.min(280, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listTop = py + 40;
        // Embaixo da lista: campo do nick novo, aviso e os botões.
        visiveis = Math.max(2, (py + ph - 84 - listTop) / ROW);
        nick = new GuiTextField(0, fontRendererObj, px + 10, py + ph - 76, pw - 82, 18);
        nick.setMaxStringLength(16);
        buttonList.clear();
        buttonList.add(new PulsarButton(ADD, px + pw - 68, py + ph - 77, 58, 20, "Adicionar"));
        buttonList.add(new PulsarButton(MICROSOFT, px + 10, py + ph - 28, 150, 20, "Outra conta Microsoft"));
        buttonList.add(new PulsarButton(BACK, px + pw - 70, py + ph - 28, 60, 20, "Voltar"));
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
        drawRect(0, 0, width, height, 0x66000000);
        drawRect(px, py, px + pw, py + ph, BG);
        GuiPulsarPanel.outline(px, py, px + pw, py + ph, LINE);

        String atual = mc.getSession().getUsername();
        Conta.drawHead(px + 10, py + 9, 20);
        fontRendererObj.drawString("Contas", px + 36, py + 9, TEXT);
        fontRendererObj.drawString("Jogando como " + atual, px + 36, py + 20, MUTED);

        Conta.Lista l = Conta.lista();
        List<String[]> linhas = linhas(l);
        if (!Conta.temLauncher()) {
            texto("Abra o jogo pelo launcher do Pulsar para trocar de conta.", listTop + 4, MUTED);
        } else if (l == null) {
            texto("Carregando as contas do launcher...", listTop + 4, MUTED);
        } else if (linhas.isEmpty()) {
            texto("Nenhuma conta ainda. Entre com a conta Microsoft.", listTop + 4, MUTED);
        }
        rolagem = Math.max(0, Math.min(rolagem, linhas.size() - visiveis));
        for (int i = 0; i < Math.min(visiveis, linhas.size() - rolagem); i++) {
            String[] c = linhas.get(i + rolagem);
            int y = listTop + i * ROW;
            boolean original = "microsoft".equals(c[0]);
            boolean emUso = c[1].equals(atual);
            boolean hover = !emUso && inside(mouseX, mouseY, px + 10, y, px + pw - 10, y + ROW - 3);
            drawRect(px + 10, y, px + pw - 10, y + ROW - 3, hover ? CARD_HOVER : CARD);
            if (emUso) GuiPulsarPanel.outline(px + 10, y, px + pw - 10, y + ROW - 3, VERDE);
            int cor = original ? BEAM : NEBULA;
            drawRect(px + 14, y + 3, px + 27, y + 16, (cor & 0x00FFFFFF) | 0x44000000);
            drawCenteredString(fontRendererObj, c[1].substring(0, 1).toUpperCase(), px + 21, y + 6, cor);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(c[1], pw - 120), px + 33, y + 6, TEXT);
            String tag = emUso ? "em uso" : hover ? "usar" : original ? "original" : "offline";
            fontRendererObj.drawString(tag, px + pw - 14 - fontRendererObj.getStringWidth(tag), y + 6, emUso ? VERDE : hover ? BEAM : cor);
        }
        if (linhas.size() > visiveis) {
            String mais = (rolagem + visiveis < linhas.size() ? "role para ver mais  " : "") + (rolagem + 1) + "-" + Math.min(linhas.size(), rolagem + visiveis) + " de " + linhas.size();
            fontRendererObj.drawString(mais, px + pw - 10 - fontRendererObj.getStringWidth(mais), listTop + visiveis * ROW, MUTED);
        }

        if (l != null && !l.liberado) {
            texto("Entre uma vez com a conta original no launcher para liberar os nicks offline.", py + ph - 72, MUTED);
        } else {
            nick.drawTextBox();
            if (nick.getText().isEmpty() && !nick.isFocused()) fontRendererObj.drawString("Novo nick offline", px + 14, py + ph - 71, MUTED);
        }
        String status = Conta.status();
        if (!status.isEmpty()) texto(status, py + ph - 50, Conta.busy() ? BEAM : 0xFFB0BCD8);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void texto(String t, int y, int cor) {
        List<String> ls = fontRendererObj.listFormattedStringToWidth(t, pw - 20);
        for (int i = 0; i < ls.size() && i < 2; i++) fontRendererObj.drawString(ls.get(i), px + 10, y + i * 10, cor);
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
        if (button != 0 || Conta.busy()) return;
        List<String[]> linhas = linhas(Conta.lista());
        String atual = mc.getSession().getUsername();
        for (int i = 0; i < Math.min(visiveis, linhas.size() - rolagem); i++) {
            int y = listTop + i * ROW;
            if (!inside(mouseX, mouseY, px + 10, y, px + pw - 10, y + ROW - 3)) continue;
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
}
