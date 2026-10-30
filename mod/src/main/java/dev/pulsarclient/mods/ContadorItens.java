package dev.pulsarclient.mods;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;

/** Contador de Itens: flechas, pérolas, maçãs douradas e poções de arremesso no inventário. */
public class ContadorItens extends HudMod {
    private static final int ROW = 17;
    private static final ItemStack ARROW = new ItemStack(Items.arrow);
    private static final ItemStack PEARL = new ItemStack(Items.ender_pearl);
    private static final ItemStack APPLE = new ItemStack(Items.golden_apple);
    // 16421 = poção de cura II de arremesso, só para o ícone.
    private static final ItemStack POTION = new ItemStack(Items.potionitem, 1, 16421);
    private static final ItemStack[] ICONS = {ARROW, PEARL, APPLE, POTION};

    public ContadorItens() {
        super("contador_itens", "Contador de Itens", "Flechas, pérolas, maçãs e poções no inventário", Category.COMBATE, 10000, 230, false);
    }

    private int[] counts() {
        int[] c = new int[4];
        if (mc.thePlayer == null) return c;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s == null) continue;
            Item item = s.getItem();
            if (item == Items.arrow) c[0] += s.stackSize;
            else if (item == Items.ender_pearl) c[1] += s.stackSize;
            else if (item == Items.golden_apple) c[2] += s.stackSize;
            else if (item == Items.potionitem && ItemPotion.isSplash(s.getMetadata())) c[3] += s.stackSize;
        }
        return c;
    }

    @Override public int width() { return 18 + font().getStringWidth("000"); }
    @Override public int height() { return ICONS.length * ROW - 1; }

    @Override
    public void render(boolean preview) {
        int[] c = counts();
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + width() + 2, y + height() + 2, BACKGROUND);
        int ry = y;
        for (int i = 0; i < ICONS.length; i++) {
            int n = preview && c[i] == 0 ? 16 : c[i];
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(ICONS[i], x, ry);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            font().drawStringWithShadow(String.valueOf(n), x + 18, ry + 4, n == 0 ? 0xFF6B7490 : accent());
            ry += ROW;
        }
        GlStateManager.enableBlend();
    }
}
