package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/** Mod que desenha um elemento na HUD, que pode ser arrastado no editor. */
public abstract class HudMod extends PulsarMod {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BACKGROUND = 0x66000000;

    public int x;
    public int y;
    protected final BoolSetting background = add(new BoolSetting("fundo", "Fundo escuro", true));
    protected final ColorSetting color = add(new ColorSetting("cor", "Cor do valor", 0));

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

    /** Desenha o mod com o canto superior esquerdo em (x, y). preview = editor de HUD aberto. */
    public abstract void render(boolean preview);

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x - 2 && mouseX <= x + width() + 2 && mouseY >= y - 2 && mouseY <= y + height() + 2;
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
