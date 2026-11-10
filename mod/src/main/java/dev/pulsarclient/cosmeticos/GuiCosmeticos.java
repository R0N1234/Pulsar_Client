package dev.pulsarclient.cosmeticos;

import dev.pulsarclient.gui.Desenho;
import dev.pulsarclient.gui.PulsarButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loja do Pulsar: acessórios e emotes. À esquerda o próprio boneco girando com o acessório; à direita os itens.
 * Os grátis já vêm liberados. Os pagos ficam trancados ("em breve") até a venda abrir.
 */
public class GuiCosmeticos extends GuiScreen {
    private static final int ACESSORIOS = 1, EMOTES = 2, FECHAR = 3, ITEM = 100;
    private static final int LINHA = 42;
    private static final int TEXTO = 0xFFE6ECFF, CINZA = 0xFF8892B0, CIANO = 0xFF7FE7FF, OURO = 0xFFF2C744;

    private final GuiScreen parent;
    private Cosmetico.Tipo aba;
    private final List<Cosmetico> itens = new ArrayList<Cosmetico>();
    private int px, py, pw, ph, listaX;

    public GuiCosmeticos(GuiScreen parent, Cosmetico.Tipo aba) {
        this.parent = parent;
        this.aba = aba == null ? Cosmetico.Tipo.ACESSORIO : aba;
    }

    @Override
    public void initGui() {
        pw = Math.min(400, width - 16);
        ph = Math.min(250, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        listaX = px + 128;
        itens.clear();
        for (Cosmetico c : Cosmeticos.CATALOGO) if (c.tipo == aba) itens.add(c);

        buttonList.clear();
        int abaW = (pw - 140) / 2;
        GuiButton a = new PulsarButton(ACESSORIOS, listaX, py + 24, abaW, 18, "Acessórios");
        GuiButton e = new PulsarButton(EMOTES, listaX + abaW + 4, py + 24, abaW, 18, "Emotes");
        a.enabled = aba != Cosmetico.Tipo.ACESSORIO;
        e.enabled = aba != Cosmetico.Tipo.EMOTE;
        buttonList.add(a);
        buttonList.add(e);
        Cosmeticos cos = Cosmeticos.get();
        for (int i = 0; i < itens.size(); i++) {
            Cosmetico c = itens.get(i);
            int y = py + 48 + i * LINHA;
            String rotulo;
            boolean pode = cos != null && cos.possui(c);
            if (!pode) rotulo = "Em breve";
            else if (c.tipo == Cosmetico.Tipo.EMOTE) rotulo = mc.thePlayer == null ? "No jogo" : "Tocar";
            else rotulo = c.id.equals(cos.acessorio()) ? "Tirar" : "Usar";
            GuiButton b = new PulsarButton(ITEM + i, px + pw - 74, y + 11, 64, 18, rotulo);
            b.enabled = pode && (c.tipo == Cosmetico.Tipo.ACESSORIO || mc.thePlayer != null);
            buttonList.add(b);
        }
        buttonList.add(new PulsarButton(FECHAR, px + 10, py + ph - 26, 108, 18, "Fechar"));
    }

    @Override
    protected void actionPerformed(GuiButton b) {
        Cosmeticos cos = Cosmeticos.get();
        if (b.id == ACESSORIOS) aba = Cosmetico.Tipo.ACESSORIO;
        else if (b.id == EMOTES) aba = Cosmetico.Tipo.EMOTE;
        else if (b.id == FECHAR) {
            mc.displayGuiScreen(parent);
            return;
        } else if (b.id >= ITEM && cos != null) {
            Cosmetico c = itens.get(b.id - ITEM);
            if (c.tipo == Cosmetico.Tipo.ACESSORIO) {
                cos.usarAcessorio(c.id.equals(cos.acessorio()) ? "" : c.id);
            } else {
                mc.displayGuiScreen(null);
                cos.tocar(c.id);
                return;
            }
        }
        initGui();
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        Desenho.caixaBorda(px, py, px + pw, py + ph, 0xE60A0F1E, 0xFF223A5E, 6);
        fontRendererObj.drawStringWithShadow("Cosméticos do Pulsar", px + 10, py + 8, CIANO);

        // Boneco à esquerda, olhando para o mouse.
        Desenho.caixaBorda(px + 8, py + 22, px + 120, py + ph - 32, 0xB0060A16, 0xFF1A2B48, 4);
        if (mc.thePlayer != null) {
            int cx = px + 64, cy = py + ph - 46;
            GuiInventory.drawEntityOnScreen(cx, cy, 52, cx - mouseX, cy - 90 - mouseY, mc.thePlayer);
        } else {
            List<String> l = fontRendererObj.listFormattedStringToWidth("Entre num mundo ou servidor para ver o seu boneco aqui.", 100);
            for (int i = 0; i < l.size(); i++) drawCenteredString(fontRendererObj, l.get(i), px + 64, py + 80 + i * 10, CINZA);
        }

        Cosmeticos cos = Cosmeticos.get();
        for (int i = 0; i < itens.size(); i++) {
            Cosmetico c = itens.get(i);
            int y = py + 48 + i * LINHA;
            boolean tem = cos != null && cos.possui(c);
            boolean usando = cos != null && c.id.equals(cos.acessorio());
            Desenho.caixaBorda(listaX, y, px + pw - 8, y + LINHA - 4, usando ? 0xC0142540 : 0xA00C1424, usando ? 0xFF7FE7FF : 0xFF1A2B48, 4);
            fontRendererObj.drawStringWithShadow(c.nome, listaX + 6, y + 5, tem ? TEXTO : CINZA);
            String selo = c.gratis ? "GRÁTIS" : "EM BREVE";
            fontRendererObj.drawStringWithShadow(selo, listaX + 10 + fontRendererObj.getStringWidth(c.nome), y + 5, c.gratis ? CIANO : OURO);
            int largura = px + pw - 84 - listaX - 6;
            List<String> d = fontRendererObj.listFormattedStringToWidth(c.descricao, largura);
            for (int k = 0; k < Math.min(2, d.size()); k++) fontRendererObj.drawString(d.get(k), listaX + 6, y + 16 + k * 10, CINZA);
        }
        String dica = aba == Cosmetico.Tipo.EMOTE ? "No jogo, aperte B para abrir os emotes. Andar cancela." : "Os outros jogadores do Pulsar vão ver quando a rede estiver no ar.";
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(dica, pw - 136), listaX, py + ph - 21, CINZA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
