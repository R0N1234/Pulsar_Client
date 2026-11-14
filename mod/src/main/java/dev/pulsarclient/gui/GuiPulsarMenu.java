package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.fml.client.GuiModList;

/** Menu principal do Pulsar: o universo girando de fundo, o logo "PC" animado no anel com órbitas e os botões com ícones. */
public class GuiPulsarMenu extends GuiScreen {
    private static final int SINGLE = 1;
    private static final int MULTI = 2;
    private static final int OPTIONS = 3;
    private static final int PANEL = 4;
    private static final int LANGUAGE = 5;
    private static final int MODS = 6;
    private static final int QUIT = 7;
    private static final int TEXTURES = 8;
    private static final int SUGESTOES = 9;
    private static final int COSMETICOS = 10;
    private static final int ACCOUNT_W = 132;
    private static final int ACCOUNT_H = 26;

    private float anel;
    private float logoY;
    private int tituloY;

    @Override
    public void initGui() {
        // O anel do logo ocupa uns 27% da altura; o resto da coluna (título, traço e botões) tem 122 de altura.
        anel = Math.max(40, Math.min(height * 0.27f, height - 150));
        float coluna = anel + 122;
        float topo = Math.max(30, (height - coluna) / 2f);
        logoY = topo + anel / 2;
        tituloY = Math.round(topo + anel + 8);
        int y = tituloY + 20;
        int cx = width / 2;
        buttonList.clear();
        // Cada fileira um pouco mais larga que a de cima, como no modelo; as de baixo cabem ícone e texto.
        buttonList.add(new MenuBotao(SINGLE, cx - 110, y, 220, 20, "Um jogador", Icones.CONTROLE));
        buttonList.add(new MenuBotao(MULTI, cx - 110, y + 24, 220, 20, "Multijogador", Icones.AMIGOS));
        buttonList.add(new MenuBotao(OPTIONS, cx - 116, y + 49, 114, 18, "Opções", Icones.ENGRENAGEM));
        buttonList.add(new MenuBotao(PANEL, cx + 2, y + 49, 114, 18, "Pulsar Client", Icones.CUBO));
        buttonList.add(new MenuBotao(LANGUAGE, cx - 128, y + 71, 61, 18, "Idioma", Icones.GLOBO));
        buttonList.add(new MenuBotao(TEXTURES, cx - 63, y + 71, 61, 18, "Texturas", Icones.CAMADAS));
        buttonList.add(new MenuBotao(MODS, cx + 2, y + 71, 61, 18, "Mods", Icones.PECA));
        buttonList.add(new MenuBotao(QUIT, cx + 67, y + 71, 61, 18, "Sair", Icones.SAIR));
        // Canto de cima, do outro lado da conta: cosméticos e sugestões.
        buttonList.add(new MenuBotao(SUGESTOES, width - 88, 6, 82, 20, "Sugestões", Icones.BRILHO));
        buttonList.add(new MenuBotao(COSMETICOS, width - 176, 6, 84, 20, "Cosméticos", Icones.CAMISA));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws java.io.IOException {
        if (button == 0 && mouseX >= 4 && mouseX < 4 + ACCOUNT_W && mouseY >= 4 && mouseY < 4 + ACCOUNT_H) {
            mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new net.minecraft.util.ResourceLocation("gui.button.press"), 1f));
            mc.displayGuiScreen(new GuiContas(this));
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    /** Caixinha da conta no canto de cima: cabeça, nick e o botão que abre as contas (original e offline). */
    private void drawAccount(int mouseX, int mouseY) {
        boolean hover = mouseX >= 4 && mouseX < 4 + ACCOUNT_W && mouseY >= 4 && mouseY < 4 + ACCOUNT_H;
        int a = hover ? 0xFF : 0xB0;
        Desenho.caixaBordaGradiente(4, 4, 4 + ACCOUNT_W, 4 + ACCOUNT_H, hover ? 0xE0152450 : 0xC80B1230,
                a << 24 | MenuBotao.CIANO & 0xFFFFFF, a << 24 | MenuBotao.VIOLETA & 0xFFFFFF, 6);
        Conta.drawHead(9, 9, 16);
        Desenho.caixa(30, 9, 31, 25, 0x60A9B8E8, 0);
        String name = fontRendererObj.trimStringToWidth(mc.getSession().getUsername(), ACCOUNT_W - 40);
        fontRendererObj.drawStringWithShadow(name, 35, 8, 0xFFFFFFFF);
        fontRendererObj.drawString(Conta.busy() ? "Fazendo login..." : "Trocar conta  >", 35, 18, hover ? 0xFF7FE7FF : 0xFF8892B0);
        String status = Conta.status();
        if (status.isEmpty()) return;
        java.util.List<String> lines = fontRendererObj.listFormattedStringToWidth(status, 200);
        for (int i = 0; i < lines.size(); i++) fontRendererObj.drawStringWithShadow(lines.get(i), 6, 34 + 10 * i, 0xFFB0BCD8);
    }

    /** "P U L S A R   C L I E N T" com os pontinhos de pixel dos lados e o traço ciano -> violeta embaixo. */
    private void drawTitulo() {
        String t = "P U L S A R   C L I E N T";
        int tw = fontRendererObj.getStringWidth(t);
        int cx = width / 2;
        fontRendererObj.drawStringWithShadow(t, cx - tw / 2f, tituloY, 0xFFDDF6FF);
        pontos(cx - tw / 2 - 16, tituloY + 1, MenuBotao.CIANO, false);
        pontos(cx + tw / 2 + 6, tituloY + 1, MenuBotao.VIOLETA, true);
        int lw = Math.min(120, tw);
        int ly = tituloY + 12;
        Desenho.caixaGradiente(cx - lw / 2f, ly, cx, ly + 1, 0x0035D6F5, 0xFF7FE7FF, 0);
        Desenho.caixaGradiente(cx, ly, cx + lw / 2f, ly + 1, 0xFFB28CFF, 0x009B6BFF, 0);
    }

    /** Três quadradinhos em escada, como pixels soltos no espaço. */
    private static void pontos(int x, int y, int cor, boolean espelho) {
        int[][] p = {{0, 0}, {4, 3}, {8, 6}, {4, 6}};
        for (int[] q : p) {
            int px = espelho ? x + 8 - q[0] : x + q[0];
            Desenho.caixa(px, y + q[1] - 1, px + 2, y + q[1] + 1, cor, 0);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case SINGLE: mc.displayGuiScreen(new GuiSelectWorld(this)); break;
            case MULTI: mc.displayGuiScreen(new GuiMultiplayer(this)); break;
            case OPTIONS: mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings)); break;
            case PANEL: mc.displayGuiScreen(new GuiPulsarPanel(this)); break;
            case LANGUAGE: mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager())); break;
            case MODS: mc.displayGuiScreen(new GuiModList(this)); break;
            case TEXTURES: mc.displayGuiScreen(new GuiTexturas(this)); break;
            case SUGESTOES: mc.displayGuiScreen(new GuiSugestoes(this)); break;
            case COSMETICOS: mc.displayGuiScreen(new dev.pulsarclient.cosmeticos.GuiCosmeticos(this, null)); break;
            case QUIT: mc.shutdown(); break;
            default: break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Universo.draw(width, height);
        Universo.logoMenu(width / 2f, logoY, anel / Universo.ANEL);
        drawAccount(mouseX, mouseY);
        drawTitulo();
        Desenho.caixa(4, height - 21, 5, height - 2, 0xFF35D6F5, 0);
        fontRendererObj.drawStringWithShadow("Pulsar Client " + PulsarClient.VERSION, 8, height - 20, 0xFFFFFFFF);
        fontRendererObj.drawStringWithShadow("Minecraft 1.8.9", 8, height - 10, 0xFF8892B0);
        String copy = "Copyright Mojang AB. Do not distribute!";
        fontRendererObj.drawStringWithShadow(copy, width - fontRendererObj.getStringWidth(copy) - 8, height - 10, 0xFF8892B0);
        Desenho.caixa(width - 5, height - 12, width - 4, height - 2, 0xFF9B6BFF, 0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
