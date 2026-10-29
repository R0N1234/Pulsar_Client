package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Blindagem: armadura e item na mão com a durabilidade de cada um. Equivale ao Armor Status. */
public class Blindagem extends HudMod {
    private static final int ROW = 17;
    private final BoolSetting held = add(new BoolSetting("mao", "Mostrar item na mão", true));
    private final ModeSetting mode = add(new ModeSetting("modo", "Durabilidade", 0, "Restante", "Porcentagem", "Restante/Total"));
    private static final ItemStack[] PREVIEW = {
            new ItemStack(Items.diamond_helmet), new ItemStack(Items.diamond_chestplate),
            new ItemStack(Items.iron_leggings), new ItemStack(Items.diamond_boots), new ItemStack(Items.diamond_sword)};

    public Blindagem() {
        super("blindagem", "Blindagem", "Durabilidade da armadura e do item na mão", Category.COMBATE, 10000, 5, false);
    }

    private List<ItemStack> items(boolean preview) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        if (mc.thePlayer == null) {
            if (!preview) return out;
            for (int i = 0; i < PREVIEW.length; i++) if (i < 4 || held.get()) out.add(PREVIEW[i]);
            return out;
        }
        // Do capacete para as botas.
        for (int i = 3; i >= 0; i--) {
            ItemStack s = mc.thePlayer.inventory.armorInventory[i];
            if (s != null) out.add(s);
        }
        ItemStack hand = mc.thePlayer.getHeldItem();
        if (held.get() && hand != null) out.add(hand);
        if (out.isEmpty() && preview) for (int i = 0; i < 4; i++) out.add(PREVIEW[i]);
        return out;
    }

    private String text(ItemStack s) {
        if (!s.isItemStackDamageable()) return s.stackSize > 1 ? String.valueOf(s.stackSize) : "";
        int max = s.getMaxDamage();
        int left = max - s.getItemDamage();
        if (mode.index() == 1) return (left * 100 / Math.max(1, max)) + "%";
        if (mode.index() == 2) return left + "/" + max;
        return String.valueOf(left);
    }

    private int textColor(ItemStack s) {
        if (!s.isItemStackDamageable()) return WHITE;
        float f = 1f - (float) s.getItemDamage() / Math.max(1, s.getMaxDamage());
        if (f > 0.5f) return accent();
        return f > 0.2f ? 0xFFFFD84A : 0xFFFF5A5A;
    }

    @Override
    public int width() {
        int w = 0;
        for (ItemStack s : items(true)) w = Math.max(w, font().getStringWidth(text(s)));
        return 18 + Math.max(w, font().getStringWidth("000"));
    }

    @Override
    public int height() {
        return Math.max(ROW, items(true).size() * ROW) - 1;
    }

    @Override
    public void render(boolean preview) {
        List<ItemStack> list = items(preview);
        if (list.isEmpty()) return;
        if (background.get()) net.minecraft.client.gui.Gui.drawRect(x - 2, y - 2, x + width() + 2, y + height() + 2, BACKGROUND);
        int ry = y;
        for (ItemStack s : list) {
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(s, x, ry);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            font().drawStringWithShadow(text(s), x + 18, ry + 4, textColor(s));
            ry += ROW;
        }
        GlStateManager.enableBlend();
    }
}
