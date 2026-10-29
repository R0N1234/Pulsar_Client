package dev.pulsarclient.mods;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Efeitos: poções ativas com ícone e tempo restante. */
public class Efeitos extends HudMod {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int ROW = 20;
    private static final String[] ROMAN = {"", " II", " III", " IV", " V"};
    private final Gui gui = new Gui();

    public Efeitos() {
        super("efeitos", "Efeitos", "Poções ativas com tempo restante", Category.COMBATE, 10000, 100, false);
    }

    private List<PotionEffect> effects(boolean preview) {
        List<PotionEffect> out = new ArrayList<PotionEffect>();
        if (mc.thePlayer != null) {
            Collection<PotionEffect> active = mc.thePlayer.getActivePotionEffects();
            out.addAll(active);
        }
        if (out.isEmpty() && preview) {
            out.add(new PotionEffect(Potion.moveSpeed.id, 1800, 1));
            out.add(new PotionEffect(Potion.damageBoost.id, 900, 0));
        }
        return out;
    }

    /** Alguns servidores mandam id ou nível fora do normal (nível 255 chega como -1), então tudo é conferido. */
    private static Potion potion(PotionEffect e) {
        int id = e.getPotionID();
        return id >= 0 && id < Potion.potionTypes.length ? Potion.potionTypes[id] : null;
    }

    private static String name(PotionEffect e) {
        Potion p = potion(e);
        int level = e.getAmplifier();
        String amp = level < 0 ? "" : level < ROMAN.length ? ROMAN[level] : " " + (level + 1);
        return (p == null ? "Efeito" : I18n.format(p.getName())) + amp;
    }

    @Override
    public int width() {
        int w = 60;
        for (PotionEffect e : effects(true)) w = Math.max(w, font().getStringWidth(name(e)));
        return 20 + w;
    }

    @Override
    public int height() {
        return Math.max(1, effects(true).size()) * ROW - 2;
    }

    @Override
    public void render(boolean preview) {
        List<PotionEffect> list = effects(preview);
        if (list.isEmpty()) return;
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + width() + 2, y + list.size() * ROW, BACKGROUND);
        int ry = y;
        for (PotionEffect e : list) {
            Potion p = potion(e);
            if (p != null && p.hasStatusIcon()) {
                int icon = p.getStatusIconIndex();
                GlStateManager.color(1f, 1f, 1f, 1f);
                mc.getTextureManager().bindTexture(INVENTORY);
                gui.drawTexturedModalRect(x, ry, icon % 8 * 18, 198 + icon / 8 * 18, 18, 18);
            }
            font().drawStringWithShadow(name(e), x + 20, ry + 1, WHITE);
            font().drawStringWithShadow(Potion.getDurationString(e), x + 20, ry + 10, accent());
            ry += ROW;
        }
    }
}
