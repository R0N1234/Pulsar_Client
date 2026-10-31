package dev.pulsarclient.gui;

import dev.pulsarclient.rede.Rede;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela de Amigos: à esquerda os pedidos e a lista de amigos (online primeiro, com o servidor de cada um),
 * à direita a conversa com o amigo escolhido. Com a tela aberta a rede atualiza a cada 5 segundos.
 */
public class GuiAmigos extends GuiScreen {
    private static final int BG = 0xF20A0F1E;
    private static final int CARD = 0xFF111A2D;
    private static final int CARD_HOVER = 0xFF18233C;
    private static final int LINE = 0x337FE7FF;
    private static final int BEAM = 0xFF7FE7FF;
    private static final int TEXT = 0xFFE6ECFF;
    private static final int MUTED = 0xFF8892B0;
    private static final int VERDE = 0xFF6EE7A8;
    private static final int VERMELHO = 0xFFFF6B6B;
    private static final int ROW = 22;
    private static final int LW = 150;

    private static final int ADD = 1;
    private static final int ENVIAR = 2;
    private static final int VOLTAR = 3;

    private final GuiScreen parent;
    private int px, py, pw, ph;
    private int listTop, listBottom;
    private int rolagem;
    private String escolhido;
    private String escolhidoNome = "";
    private GuiTextField nick;
    private GuiTextField msg;

    public GuiAmigos(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(400, width - 16);
        ph = Math.min(260, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listTop = py + 60;
        listBottom = py + ph - 34;
        nick = new GuiTextField(0, fontRendererObj, px + 15, py + 41, LW - 34, 10);
        nick.setEnableBackgroundDrawing(false);
        nick.setMaxStringLength(16);
        int rx = px + 10 + LW + 8;
        msg = new GuiTextField(1, fontRendererObj, rx + 5, py + ph - 55, px + pw - 10 - 58 - rx - 10, 10);
        msg.setEnableBackgroundDrawing(false);
        msg.setMaxStringLength(200);
        buttonList.clear();
        buttonList.add(new PulsarButton(ADD, px + 10 + LW - 20, py + 36, 20, 18, "+"));
        buttonList.add(new PulsarButton(ENVIAR, px + pw - 62, py + ph - 61, 52, 20, "Enviar"));
        buttonList.add(new PulsarButton(VOLTAR, px + pw - 62, py + ph - 28, 52, 20, "Voltar"));
        Rede.get().rapido(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        Rede.get().rapido(false);
    }

    @Override
    public void updateScreen() {
        nick.updateCursorCounter();
        msg.updateCursorCounter();
        boolean pode = Rede.noAr() && !Rede.get().offline();
        nick.setEnabled(pode);
        buttonList.get(0).enabled = pode && !nick.getText().trim().isEmpty();
        boolean conversa = pode && escolhido != null;
        msg.setVisible(conversa);
        buttonList.get(1).visible = conversa;
        buttonList.get(1).enabled = conversa && !msg.getText().trim().isEmpty();
    }

    /** Linhas da esquerda: pedidos primeiro, depois os amigos. */
    private List<Object> linhas() {
        List<Object> out = new ArrayList<Object>();
        out.addAll(Rede.get().pedidos());
        out.addAll(Rede.get().amigos());
        return out;
    }

    private int visiveis() {
        return Math.max(1, (listBottom - listTop) / ROW);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x66000000);
        drawRect(px, py, px + pw, py + ph, BG);
        GuiPulsarPanel.outline(px, py, px + pw, py + ph, LINE);

        Rede rede = Rede.get();
        Universo.icone(px + 10, py + 8, 20);
        fontRendererObj.drawString("Amigos", px + 36, py + 9, TEXT);
        String nivel = "Seu nível " + rede.nivel() + "  ·  " + Math.max(0, rede.proximo() - rede.minutos()) + " min para o " + (rede.nivel() + 1);
        fontRendererObj.drawString(nivel, px + 36, py + 20, MUTED);

        if (!Rede.noAr()) {
            centro("A rede do Pulsar ainda não está no ar.", MUTED);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        // Campo para adicionar amigo.
        int fx = px + 10, fy = py + 36, fr = px + 10 + LW - 24, fb = py + 54;
        drawRect(fx, fy, fr, fb, CARD);
        GuiPulsarPanel.outline(fx, fy, fr, fb, nick.isFocused() ? BEAM : LINE);
        nick.drawTextBox();
        if (nick.getText().isEmpty() && !nick.isFocused()) fontRendererObj.drawString("Nick do amigo", px + 15, py + 41, MUTED);

        List<Object> linhas = linhas();
        int vis = visiveis();
        rolagem = Math.max(0, Math.min(rolagem, linhas.size() - vis));
        if (rede.offline()) {
            texto("Amigos e chat precisam de conta original (Microsoft). O ícone e o nível funcionam com qualquer conta.", px + 10, listTop + 2, LW, MUTED);
        } else if (linhas.isEmpty()) {
            texto(rede.conectado() ? "Nenhum amigo ainda. Digite o nick de quem usa o Pulsar e clique em +." : "Conectando...", px + 10, listTop + 2, LW, MUTED);
        }
        for (int i = 0; i < Math.min(vis, linhas.size() - rolagem); i++) {
            Object o = linhas.get(i + rolagem);
            int y = listTop + i * ROW;
            int l = px + 10, r = px + 10 + LW;
            boolean hover = inside(mouseX, mouseY, l, y, r, y + ROW - 2);
            if (o instanceof Rede.Pedido) {
                Rede.Pedido p = (Rede.Pedido) o;
                drawRect(l, y, r, y + ROW - 2, CARD);
                GuiPulsarPanel.outline(l, y, r, y + ROW - 2, 0x66FFD84A);
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(p.nome, LW - 50), l + 5, y + 2, TEXT);
                fontRendererObj.drawString("quer ser seu amigo", l + 5, y + 11, MUTED);
                botaoMini(r - 38, y + 3, "✓", VERDE, mouseX, mouseY);
                botaoMini(r - 19, y + 3, "×", VERMELHO, mouseX, mouseY);
            } else {
                Rede.Amigo a = (Rede.Amigo) o;
                boolean sel = a.uuid.equals(escolhido);
                drawRect(l, y, r, y + ROW - 2, hover || sel ? CARD_HOVER : CARD);
                if (sel) GuiPulsarPanel.outline(l, y, r, y + ROW - 2, BEAM);
                drawRect(l + 5, y + 4, l + 9, y + 8, a.online ? VERDE : 0xFF4A5470);
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(a.nome, LW - 50), l + 13, y + 2, a.online ? TEXT : MUTED);
                String nv = "Nv " + a.nivel;
                fontRendererObj.drawString(nv, r - 5 - fontRendererObj.getStringWidth(nv), y + 2, BEAM);
                String onde = a.online ? (a.servidor.isEmpty() ? "online" : a.servidor) : "offline";
                fontRendererObj.drawString(fontRendererObj.trimStringToWidth(onde, LW - 18), l + 13, y + 11, MUTED);
            }
        }
        if (linhas.size() > vis) {
            String mais = (rolagem + 1) + "-" + Math.min(linhas.size(), rolagem + vis) + " de " + linhas.size();
            fontRendererObj.drawString(mais, px + 10, listBottom + 2, MUTED);
        }

        drawConversa(mouseX, mouseY);

        String aviso = !rede.aviso().isEmpty() ? rede.aviso() : rede.estado();
        if (!aviso.isEmpty()) texto(aviso, px + 10 + LW + 8, py + ph - 27, pw - LW - 88, 0xFFB0BCD8);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawConversa(int mouseX, int mouseY) {
        int l = px + 10 + LW + 8, r = px + pw - 10, t = py + 36, b = py + ph - 66;
        drawRect(l, t, r, b, 0x660A0F1E);
        GuiPulsarPanel.outline(l, t, r, b, LINE);
        if (escolhido == null) {
            texto(Rede.get().offline() ? "" : "Escolha um amigo para conversar.", l + 6, t + 6, r - l - 12, MUTED);
            return;
        }
        fontRendererObj.drawString(escolhidoNome, l + 6, t + 5, TEXT);
        boolean hover = inside(mouseX, mouseY, r - 50, t + 3, r - 4, t + 14);
        fontRendererObj.drawString("remover", r - 6 - fontRendererObj.getStringWidth("remover"), t + 5, hover ? VERMELHO : MUTED);
        drawRect(l + 4, t + 16, r - 4, t + 17, LINE);
        // Mensagens de baixo para cima (a mais nova embaixo).
        List<String> quebradas = new ArrayList<String>();
        List<Integer> cores = new ArrayList<Integer>();
        for (Rede.Mensagem m : Rede.get().conversa(escolhido)) {
            String linha = (m.minha ? "Você" : m.nome) + ": " + m.texto;
            for (String s : fontRendererObj.listFormattedStringToWidth(linha, r - l - 12)) {
                quebradas.add(s);
                cores.add(m.minha ? BEAM : TEXT);
            }
        }
        int y = b - 12;
        for (int i = quebradas.size() - 1; i >= 0 && y > t + 19; i--, y -= 10) fontRendererObj.drawString(quebradas.get(i), l + 6, y, cores.get(i));
        // Campo da mensagem.
        int fy = py + ph - 61, fb = py + ph - 41;
        drawRect(l, fy, px + pw - 66, fb, CARD);
        GuiPulsarPanel.outline(l, fy, px + pw - 66, fb, msg.isFocused() ? BEAM : LINE);
        msg.drawTextBox();
        if (msg.getText().isEmpty() && !msg.isFocused()) fontRendererObj.drawString("Mensagem para " + escolhidoNome, l + 5, py + ph - 55, MUTED);
    }

    private void botaoMini(int x, int y, String s, int cor, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, x + 15, y + 14);
        drawRect(x, y, x + 15, y + 14, hover ? (cor & 0x00FFFFFF) | 0x55000000 : (cor & 0x00FFFFFF) | 0x22000000);
        drawCenteredString(fontRendererObj, s, x + 8, y + 3, cor);
    }

    private void centro(String s, int cor) {
        drawCenteredString(fontRendererObj, s, px + pw / 2, py + ph / 2 - 4, cor);
    }

    private void texto(String s, int x, int y, int largura, int cor) {
        List<String> ls = fontRendererObj.listFormattedStringToWidth(s, largura);
        for (int i = 0; i < ls.size() && i < 4; i++) fontRendererObj.drawString(ls.get(i), x, y + i * 10, cor);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ADD) adicionar();
        else if (button.id == ENVIAR) enviar();
        else if (button.id == VOLTAR) mc.displayGuiScreen(parent);
    }

    private void adicionar() {
        String n = nick.getText().trim();
        if (n.isEmpty() || Rede.get().offline()) return;
        Rede.get().pedirAmizade(n);
        nick.setText("");
    }

    private void enviar() {
        String t = msg.getText().trim();
        if (t.isEmpty() || escolhido == null) return;
        Rede.get().mandar(escolhido, escolhidoNome, t);
        msg.setText("");
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        nick.mouseClicked(mouseX, mouseY, button);
        msg.mouseClicked(mouseX, mouseY, button);
        if (inside(mouseX, mouseY, px + 10, py + 36, px + 10 + LW - 24, py + 54)) nick.setFocused(true);
        if (msg.getVisible() && inside(mouseX, mouseY, px + 10 + LW + 8, py + ph - 61, px + pw - 66, py + ph - 41)) msg.setFocused(true);
        if (button != 0) return;
        int r = px + pw - 10, t = py + 36;
        if (escolhido != null && inside(mouseX, mouseY, r - 50, t + 3, r - 4, t + 14)) {
            Rede.get().remover(escolhido);
            escolhido = null;
            return;
        }
        List<Object> linhas = linhas();
        for (int i = 0; i < Math.min(visiveis(), linhas.size() - rolagem); i++) {
            int y = listTop + i * ROW;
            int l = px + 10, rr = px + 10 + LW;
            if (!inside(mouseX, mouseY, l, y, rr, y + ROW - 2)) continue;
            Object o = linhas.get(i + rolagem);
            if (o instanceof Rede.Pedido) {
                Rede.Pedido p = (Rede.Pedido) o;
                if (inside(mouseX, mouseY, rr - 38, y + 3, rr - 23, y + 17)) Rede.get().responder(p.uuid, true);
                else if (inside(mouseX, mouseY, rr - 19, y + 3, rr - 4, y + 17)) Rede.get().responder(p.uuid, false);
            } else {
                Rede.Amigo a = (Rede.Amigo) o;
                escolhido = a.uuid;
                escolhidoNome = a.nome;
                msg.setFocused(true);
            }
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
        boolean enter = keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER;
        if (nick.isFocused()) {
            if (enter) {
                adicionar();
                return;
            }
            boolean imprimivel = typedChar >= 32 && typedChar != 127 && !isCtrlKeyDown();
            boolean valido = (typedChar >= 'a' && typedChar <= 'z') || (typedChar >= 'A' && typedChar <= 'Z') || (typedChar >= '0' && typedChar <= '9') || typedChar == '_';
            if (imprimivel && !valido) return;
            nick.textboxKeyTyped(typedChar, keyCode);
        } else if (msg.isFocused()) {
            if (enter) {
                enviar();
                return;
            }
            msg.textboxKeyTyped(typedChar, keyCode);
        }
    }

    /** Só para o teste: abre já com uma conversa escolhida. */
    public GuiAmigos escolher(String uuid, String nome) {
        escolhido = uuid;
        escolhidoNome = nome;
        return this;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static boolean inside(int mouseX, int mouseY, int l, int t, int r, int b) {
        return mouseX >= l && mouseX < r && mouseY >= t && mouseY < b;
    }
}
