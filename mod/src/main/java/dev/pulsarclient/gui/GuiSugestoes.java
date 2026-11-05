package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

/**
 * Sugestões para o Pulsar: a pessoa escreve a ideia aqui e o botão abre a página de sugestões do projeto no GitHub
 * já preenchida (é só confirmar lá). Também tem o botão do grupo do Discord do Pulsar.
 * Nada é mandado sozinho e nenhuma senha passa pelo jogo: quem envia é a própria pessoa, no navegador.
 */
public class GuiSugestoes extends GuiScreen {
    public static final String REPO = "https://github.com/R0N1234/Pulsar_Client";
    /** Convite do grupo do Discord do Pulsar (vazio = ainda não tem; o botão aparece desligado). */
    public static final String DISCORD = "https://discord.gg/UUW5jTfDKH";
    private static final int ENVIAR = 1, DISCORD_BTN = 2, VOLTAR = 3;
    private static final int W = 320, H = 196;

    private final GuiScreen parent;
    private GuiTextField titulo;
    private GuiTextField ideia;
    private String aviso = "";
    private int px, py;

    public GuiSugestoes(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        px = (width - W) / 2;
        py = (height - H) / 2;
        String t = titulo == null ? "" : titulo.getText();
        String i = ideia == null ? "" : ideia.getText();
        titulo = new GuiTextField(0, fontRendererObj, px + 14, py + 56, W - 28, 16);
        titulo.setMaxStringLength(80);
        titulo.setText(t);
        titulo.setFocused(true);
        ideia = new GuiTextField(1, fontRendererObj, px + 14, py + 92, W - 28, 16);
        ideia.setMaxStringLength(600);
        ideia.setText(i);
        buttonList.clear();
        buttonList.add(new PulsarButton(ENVIAR, px + 14, py + H - 52, W - 28, 20, "Enviar sugestão (abre o GitHub)"));
        GuiButton dc = new PulsarButton(DISCORD_BTN, px + 14, py + H - 28, (W - 32) / 2, 20, DISCORD.isEmpty() ? "Discord (em breve)" : "Grupo do Discord");
        dc.enabled = !DISCORD.isEmpty();
        buttonList.add(dc);
        buttonList.add(new PulsarButton(VOLTAR, px + 18 + (W - 32) / 2, py + H - 28, (W - 32) / 2, 20, "Voltar"));
    }

    @Override
    public void updateScreen() {
        titulo.updateCursorCounter();
        ideia.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case ENVIAR: enviar(); break;
            case DISCORD_BTN: abrir(DISCORD); break;
            case VOLTAR: mc.displayGuiScreen(parent); break;
            default: break;
        }
    }

    private void enviar() {
        String t = titulo.getText().trim();
        String i = ideia.getText().trim();
        if (t.isEmpty() && i.isEmpty()) {
            aviso = "Escreva a sua ideia antes de enviar.";
            return;
        }
        try {
            String url = REPO + "/issues/new?template=sugestao.yml&labels=sugest%C3%A3o"
                    + "&title=" + URLEncoder.encode("[Sugestão] " + (t.isEmpty() ? cortar(i, 60) : t), "UTF-8")
                    + "&ideia=" + URLEncoder.encode(i.isEmpty() ? t : i, "UTF-8")
                    + "&versao=" + URLEncoder.encode(PulsarClient.VERSION, "UTF-8");
            if (abrir(url)) aviso = "Abrimos o GitHub no navegador: é só clicar em \"Create\" lá. Valeu pela ideia!";
        } catch (IOException e) {
            aviso = "Não deu para abrir o navegador.";
        }
    }

    private static String cortar(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 3) + "...";
    }

    private boolean abrir(String url) {
        if (url.isEmpty()) return false;
        try {
            java.awt.Desktop.getDesktop().browse(new URI(url));
            return true;
        } catch (Throwable t) {
            try {
                org.lwjgl.Sys.openURL(url);
                return true;
            } catch (Throwable t2) {
                aviso = "Não deu para abrir o navegador. Endereço: " + REPO;
                return false;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (key == Keyboard.KEY_TAB) {
            boolean t = titulo.isFocused();
            titulo.setFocused(!t);
            ideia.setFocused(t);
            return;
        }
        if (key == Keyboard.KEY_RETURN && ideia.isFocused()) {
            enviar();
            return;
        }
        titulo.textboxKeyTyped(c, key);
        ideia.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        titulo.mouseClicked(mouseX, mouseY, button);
        ideia.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x66000000);
        Desenho.caixaBorda(px, py, px + W, py + H, 0xF20A0F1E, 0x337FE7FF, 8);
        Icones.desenhar(Icones.BALAO, px + 14, py + 12, 16, 0xFF7FE7FF);
        fontRendererObj.drawString("Sugestões para o Pulsar", px + 36, py + 13, 0xFFE6ECFF);
        fontRendererObj.drawString("Ideias de mods, HUDs ou melhorias: a gente lê todas.", px + 36, py + 24, 0xFF8892B0);
        fontRendererObj.drawString("Título", px + 14, py + 45, 0xFF8892B0);
        titulo.drawTextBox();
        fontRendererObj.drawString("Sua ideia (Tab troca de campo, Enter envia)", px + 14, py + 81, 0xFF8892B0);
        ideia.drawTextBox();
        String rodape = aviso.isEmpty() ? "Precisa de uma conta do GitHub (grátis). Nada é enviado sem você confirmar." : aviso;
        List<String> linhas = fontRendererObj.listFormattedStringToWidth(rodape, W - 28);
        for (int i = 0; i < Math.min(2, linhas.size()); i++) {
            fontRendererObj.drawString(linhas.get(i), px + 14, py + 114 + i * 10, aviso.isEmpty() ? 0xFF8892B0 : 0xFF7FE7FF);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
