package dev.pulsarclient.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.ResourcePackRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aba Texturas dentro do jogo: os pacotes escolhidos pelo Pulsar, separados em PvP (mais FPS) e Qualidade.
 * Um clique baixa, outro liga; o pacote em uso fica marcado. O resto dos pacotes continua na tela do jogo.
 */
public class GuiTexturas extends GuiScreen {
    private static final int BG = 0xF20A0F1E;
    private static final int CARD = 0xFF111A2D;
    private static final int CARD_HOVER = 0xFF18233C;
    private static final int LINE = 0x337FE7FF;
    private static final int BEAM = 0xFF7FE7FF;
    private static final int TEXT = 0xFFE6ECFF;
    private static final int MUTED = 0xFF8892B0;
    private static final int PVP = 0xFFFF8A3D;
    private static final int QUALIDADE = 0xFF9B7CFF;
    private static final int VERDE = 0xFF6EE7A8;
    private static final int ERRO = 0xFFFF6B6B;
    private static final int BTN_W = 46;

    private final GuiScreen parent;
    private final List<Texturas.Pacote> pvp = new ArrayList<Texturas.Pacote>();
    private final List<Texturas.Pacote> qualidade = new ArrayList<Texturas.Pacote>();
    private int px, py, pw, ph, linha, colW, topo;
    private String aviso = "";
    private int avisoCor = MUTED;
    private boolean algumBaixando;

    public GuiTexturas(GuiScreen parent) {
        this.parent = parent;
        for (Texturas.Pacote p : Texturas.catalogo()) (p.pvp() ? pvp : qualidade).add(p);
    }

    @Override
    public void initGui() {
        pw = Math.min(460, width - 16);
        ph = Math.min(280, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        topo = py + 46;
        int linhas = Math.max(pvp.size(), qualidade.size());
        linha = Math.max(24, Math.min(38, (ph - 46 - 30) / Math.max(1, linhas)));
        colW = (pw - 30) / 2;
        Texturas.atualizarLista();
    }

    @Override
    public void updateScreen() {
        // Quando um download termina, a lista do jogo é relida (para o ícone e o botão Usar aparecerem).
        boolean baixando = false;
        for (Texturas.Pacote p : Texturas.catalogo()) baixando |= p.progresso() >= 0;
        if (algumBaixando && !baixando) Texturas.atualizarLista();
        algumBaixando = baixando;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x66000000);
        drawRect(px, py, px + pw, py + ph, BG);
        GuiPulsarPanel.outline(px, py, px + pw, py + ph, LINE);

        Universo.logo(px + 8, py + 6, 26);
        fontRendererObj.drawString("Texturas", px + 40, py + 9, TEXT);
        fontRendererObj.drawString("Escolhidas pelo Pulsar para a 1.8.9. Baixe uma vez e troque quando quiser.", px + 40, py + 20, MUTED);

        coluna("PvP  ·  mais FPS", PVP, pvp, px + 10, mouseX, mouseY);
        coluna("Qualidade", QUALIDADE, qualidade, px + 20 + colW, mouseX, mouseY);

        // Rodapé: aviso e botões.
        int by = py + ph - 22;
        botao("Voltar", px + 10, by, 60, mouseX, mouseY, BEAM);
        botao("Outros pacotes", px + pw - 100, by, 90, mouseX, mouseY, BEAM);
        String msg = mensagem();
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(msg, pw - 180), px + 78, by + 5, avisoCor);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String mensagem() {
        for (Texturas.Pacote p : Texturas.catalogo()) {
            if (p.erro() != null) {
                avisoCor = ERRO;
                return p.erro();
            }
        }
        for (Texturas.Pacote p : Texturas.catalogo()) {
            if (p.progresso() >= 0) {
                avisoCor = BEAM;
                return "Baixando " + p.nome + "... " + p.progresso() + "%";
            }
        }
        if (aviso.isEmpty()) avisoCor = MUTED;
        return aviso;
    }

    private void coluna(String titulo, int cor, List<Texturas.Pacote> lista, int x, int mouseX, int mouseY) {
        fontRendererObj.drawString(titulo, x, py + 34, cor);
        drawRect(x, py + 43, x + colW, py + 44, (cor & 0x00FFFFFF) | 0x55000000);
        for (int i = 0; i < lista.size(); i++) {
            int y = topo + 2 + i * linha;
            cartao(lista.get(i), cor, x, y, mouseX, mouseY);
        }
    }

    private void cartao(Texturas.Pacote p, int cor, int x, int y, int mouseX, int mouseY) {
        int h = linha - 3;
        boolean hover = inside(mouseX, mouseY, x, y, x + colW, y + h);
        drawRect(x, y, x + colW, y + h, hover ? CARD_HOVER : CARD);
        boolean emUso = Texturas.emUso(p);
        if (emUso) GuiPulsarPanel.outline(x, y, x + colW, y + h, VERDE);

        // Ícone do pacote (depois de baixado) ou um quadrado com a resolução.
        int s = h - 6;
        int ix = x + 3;
        int iy = y + 3;
        ResourcePackRepository.Entry e = Texturas.baixado(p) ? Texturas.entrada(p) : null;
        if (e != null) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            e.bindTexturePackIcon(mc.getTextureManager());
            Gui.drawModalRectWithCustomSizedTexture(ix, iy, 0, 0, s, s, s, s);
        } else {
            drawRect(ix, iy, ix + s, iy + s, (cor & 0x00FFFFFF) | 0x33000000);
            drawCenteredString(fontRendererObj, p.resolucao, ix + s / 2, iy + (s - 8) / 2, cor);
        }

        int tx = ix + s + 5;
        int tw = colW - (tx - x) - BTN_W - 6;
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(p.nome, tw), tx, y + (h >= 30 ? 4 : 2), TEXT);
        String info = p.resolucao + "  ·  " + tamanho(p.tamanho);
        if (h >= 30) {
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(p.desc, tw), tx, y + 14, MUTED);
            fontRendererObj.drawString(info, tx, y + 24, MUTED);
        } else {
            fontRendererObj.drawString(info, tx, y + 12, MUTED);
        }

        int bx = x + colW - BTN_W - 4;
        int byy = y + (h - 16) / 2;
        int prog = p.progresso();
        if (prog >= 0) {
            drawRect(bx, byy, bx + BTN_W, byy + 16, CARD);
            drawRect(bx, byy, bx + BTN_W * prog / 100, byy + 16, 0x557FE7FF);
            GuiPulsarPanel.outline(bx, byy, bx + BTN_W, byy + 16, BEAM);
            drawCenteredString(fontRendererObj, prog + "%", bx + BTN_W / 2, byy + 4, TEXT);
        } else if (emUso) {
            botao("Em uso", bx, byy, BTN_W, mouseX, mouseY, VERDE);
        } else if (Texturas.baixado(p)) {
            botao("Usar", bx, byy, BTN_W, mouseX, mouseY, BEAM);
        } else {
            botao(p.erro() != null ? "Tentar" : "Baixar", bx, byy, BTN_W, mouseX, mouseY, p.erro() != null ? ERRO : cor);
        }
    }

    private void botao(String texto, int x, int y, int w, int mouseX, int mouseY, int cor) {
        boolean hover = inside(mouseX, mouseY, x, y, x + w, y + 16);
        drawRect(x, y, x + w, y + 16, hover ? (cor & 0x00FFFFFF) | 0x33000000 : CARD);
        GuiPulsarPanel.outline(x, y, x + w, y + 16, hover ? cor : (cor & 0x00FFFFFF) | 0x88000000);
        drawCenteredString(fontRendererObj, texto, x + w / 2, y + 4, hover ? TEXT : cor);
    }

    private static String tamanho(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        return mb < 1 ? Math.max(1, Math.round(bytes / 1024.0)) + " KB" : (mb < 10 ? String.format("%.1f MB", mb) : Math.round(mb) + " MB");
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button != 0) return;
        int by = py + ph - 22;
        if (inside(mouseX, mouseY, px + 10, by, px + 70, by + 16)) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (inside(mouseX, mouseY, px + pw - 100, by, px + pw - 10, by + 16)) {
            mc.displayGuiScreen(new GuiScreenResourcePacks(this));
            return;
        }
        clicarColuna(pvp, px + 10, mouseX, mouseY);
        clicarColuna(qualidade, px + 20 + colW, mouseX, mouseY);
    }

    private void clicarColuna(List<Texturas.Pacote> lista, int x, int mouseX, int mouseY) {
        int bx = x + colW - BTN_W - 4;
        for (int i = 0; i < lista.size(); i++) {
            int y = topo + 2 + i * linha;
            int h = linha - 3;
            int byy = y + (h - 16) / 2;
            if (!inside(mouseX, mouseY, bx, byy, bx + BTN_W, byy + 16)) continue;
            Texturas.Pacote p = lista.get(i);
            if (p.progresso() >= 0) return;
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new net.minecraft.util.ResourceLocation("gui.button.press"), 1f));
            if (!Texturas.baixado(p)) {
                Texturas.baixar(p);
                aviso = "";
            } else if (Texturas.emUso(p)) {
                if (Texturas.usar(p, false)) aviso = p.nome + " desligado.";
            } else if (Texturas.usar(p, true)) {
                aviso = "Usando " + p.nome + ".";
            }
            return;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static boolean inside(int mouseX, int mouseY, int l, int t, int r, int b) {
        return mouseX >= l && mouseX < r && mouseY >= t && mouseY < b;
    }
}
