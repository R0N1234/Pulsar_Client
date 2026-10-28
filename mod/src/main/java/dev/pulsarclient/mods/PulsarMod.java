package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/**
 * Base de todo mod do Pulsar que desenha algo na tela.
 * O id é o mesmo usado pelo launcher (launcher/src/mods.js).
 */
public abstract class PulsarMod {
    public static final int BEAM = 0xFF7FE7FF;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BACKGROUND = 0x66000000;

    protected final Minecraft mc = Minecraft.getMinecraft();
    private final String id;
    private final String name;
    private boolean enabled = true;
    public int x;
    public int y;

    protected PulsarMod(String id, String name, int defaultX, int defaultY) {
        this.id = id;
        this.name = name;
        this.x = defaultX;
        this.y = defaultY;
    }

    public String id() { return id; }
    public String name() { return name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public abstract int width();
    public abstract int height();

    /** Desenha o mod com o canto superior esquerdo em (x, y). preview = editor de HUD aberto. */
    public abstract void render(boolean preview);

    /** Chamado a cada tick do cliente, mesmo com a HUD escondida. */
    public void tick() {}

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x - 2 && mouseX <= x + width() + 2 && mouseY >= y - 2 && mouseY <= y + height() + 2;
    }

    protected FontRenderer font() {
        return mc.fontRendererObj;
    }

    /** Caixa de fundo com "rótulo valor", o valor na cor do feixe do pulsar. */
    protected void drawLabel(String label, String value) {
        Gui.drawRect(x - 2, y - 2, x + width() + 2, y + height() + 2, BACKGROUND);
        int lx = label.isEmpty() ? x : font().drawStringWithShadow(label + " ", x, y, WHITE);
        font().drawStringWithShadow(value, lx, y, BEAM);
    }

    protected int labelWidth(String label, String value) {
        return font().getStringWidth(label.isEmpty() ? value : label + " " + value);
    }
}
