package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/** Mod que desenha um elemento na HUD, que pode ser arrastado no editor. */
public abstract class HudMod extends PulsarMod {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BACKGROUND = 0x66000000;
    /** Posição x de fábrica "no meio da tela" (vira um número de verdade quando a pessoa arrasta). */
    public static final int CENTRO = -10000;

    public int x;
    public int y;
    // Tamanho de cada HUD: muda no painel ou com a roda do mouse no editor de HUD.
    private final SliderSetting size = add(new SliderSetting("escala", "Tamanho", 100, 50, 200, 5, "%"));
    protected final BoolSetting background = add(new BoolSetting("fundo", "Fundo escuro", true));
    protected final ColorSetting color = add(new ColorSetting("cor", "Cor do valor", ColorSetting.CIANO));

    protected HudMod(String id, String name, String description, Category category, int defaultX, int defaultY) {
        this(id, name, description, category, defaultX, defaultY, true);
    }

    protected HudMod(String id, String name, String description, Category category, int defaultX, int defaultY, boolean enabledByDefault) {
        super(id, name, description, category, enabledByDefault);
        this.x = defaultX;
        this.y = defaultY;
    }

    public abstract int width();
    public abstract int height();

    /** Desenha o mod com o canto superior esquerdo em (x, y), no tamanho 100%. preview = editor de HUD aberto. */
    public abstract void render(boolean preview);

    public float scale() {
        return (float) (size.get() / 100.0);
    }

    /** Muda o tamanho em passos de 10% (roda do mouse no editor). */
    public void resize(int steps) {
        size.set(size.get() + steps * 10);
    }

    public String sizeText() {
        return size.display();
    }

    /** Largura e altura na tela, já com o tamanho escolhido. */
    public int boxWidth() {
        return Math.max(1, Math.round(width() * scale()));
    }

    public int boxHeight() {
        return Math.max(1, Math.round(height() * scale()));
    }

    /** Desenha no tamanho escolhido: o mod desenha em (0, 0) e a tela é esticada a partir de (x, y). */
    public void draw(boolean preview) {
        float s = scale();
        if (s == 1f) {
            render(preview);
            return;
        }
        int sx = x;
        int sy = y;
        GlStateManager.pushMatrix();
        GlStateManager.translate(sx, sy, 0);
        GlStateManager.scale(s, s, 1f);
        x = 0;
        y = 0;
        try {
            render(preview);
        } finally {
            x = sx;
            y = sy;
            GlStateManager.popMatrix();
        }
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x - 2 && mouseX <= x + boxWidth() + 2 && mouseY >= y - 2 && mouseY <= y + boxHeight() + 2;
    }

    protected FontRenderer font() {
        return mc.fontRendererObj;
    }

    protected int accent() {
        return color.argb(0xFF);
    }

    /** Caixa com "rótulo valor", o valor na cor escolhida. */
    protected void drawLabel(String label, String value) {
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + width() + 2, y + height() + 2, BACKGROUND);
        int lx = label.isEmpty() ? x : font().drawStringWithShadow(label + " ", x, y, WHITE);
        font().drawStringWithShadow(value, lx, y, accent());
    }

    protected int labelWidth(String label, String value) {
        return font().getStringWidth(label.isEmpty() ? value : label + " " + value);
    }
}
