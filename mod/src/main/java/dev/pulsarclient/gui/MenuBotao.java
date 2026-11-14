package dev.pulsarclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Botão do menu principal: vidro escuro com cantos arredondados, borda ciano -> violeta, ícone, um traço e o texto.
 * Com o mouse em cima, acende devagar (a borda e o fundo clareiam e aparece um brilho em volta).
 */
public class MenuBotao extends GuiButton {
    public static final int CIANO = 0xFF35D6F5;
    public static final int VIOLETA = 0xFF9B6BFF;
    private static final int FUNDO = 0xC80B1230;
    private static final int FUNDO_HOVER = 0xE0152450;

    private final int icone;
    private float acesa;
    private long antes;

    /** icone: um dos ícones de {@link Icones}, ou -1 sem ícone. */
    public MenuBotao(int id, int x, int y, int width, int height, String text, int icone) {
        super(id, x, y, width, height, text);
        this.icone = icone;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        long agora = Minecraft.getSystemTime();
        float passo = antes == 0 ? 1f : Math.min(1f, (agora - antes) / 120f);
        antes = agora;
        acesa += ((hovered && enabled ? 1f : 0f) - acesa) * passo;
        int l = xPosition, t = yPosition, r = xPosition + width, b = yPosition + height;
        int raio = Math.min(5, height / 3);
        if (acesa > 0.02f) {
            int a = Math.round(acesa * 0x50);
            Desenho.caixaGradiente(l - 2, t - 2, r + 2, b + 2, a << 24 | CIANO & 0xFFFFFF, a << 24 | VIOLETA & 0xFFFFFF, raio + 2);
        }
        int bordaA = Math.round(0x90 + acesa * 0x6F);
        Desenho.caixaBordaGradiente(l, t, r, b, Desenho.misturar(FUNDO, FUNDO_HOVER, acesa),
                bordaA << 24 | CIANO & 0xFFFFFF, bordaA << 24 | VIOLETA & 0xFFFFFF, raio);
        // Reflexo do vidro na metade de cima.
        Desenho.caixa(l + 2, t + 1, r - 2, t + height / 2, 0x0CFFFFFF, Math.max(0, raio - 2));

        int cor = !enabled ? 0xFF6B7490 : Desenho.misturar(0xFFE6ECFF, 0xFFBFF6FF, acesa);
        int tw = mc.fontRendererObj.getStringWidth(displayString);
        int tam = Math.min(12, height - 8);
        boolean traco = icone >= 0 && width >= 90;
        int conteudo = icone >= 0 ? tam + (traco ? 15 : 5) + tw : tw;
        int x = l + (width - conteudo) / 2;
        int meio = t + height / 2;
        if (icone >= 0) {
            Icones.desenhar(icone, x, meio - tam / 2f, tam, Desenho.misturar(0xFF7FE7FF, 0xFFFFFFFF, acesa * 0.5f));
            x += tam + 5;
            if (traco) {
                Desenho.caixa(x + 2, meio - 5, x + 3, meio + 5, 0x60A9B8E8, 0);
                x += 10;
            }
        }
        mc.fontRendererObj.drawStringWithShadow(displayString, x, meio - 4, cor);
    }
}
