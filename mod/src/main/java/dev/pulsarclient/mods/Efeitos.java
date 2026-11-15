package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Efeitos: poções ativas com ícone e tempo restante.
 * Cada efeito do Minecraft tem uma opção para esconder (ex.: comer a maçã dourada e não querer ver a Resistência ao fogo).
 */
public class Efeitos extends HudMod {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");
    private static final int ROW = 20;
    private static final String[] ROMAN = {"", " II", " III", " IV", " V"};
    private final Gui gui = new Gui();

    /** Todos os efeitos do 1.8.9 (id, nome), os de PvP primeiro. */
    public static final Object[][] LISTA = {
            {5, "Força"}, {1, "Velocidade"}, {10, "Regeneração"}, {11, "Resistência"}, {12, "Resistência ao fogo"},
            {22, "Absorção"}, {21, "Vida extra"}, {19, "Veneno"}, {18, "Fraqueza"}, {2, "Lentidão"},
            {15, "Cegueira"}, {14, "Invisibilidade"}, {8, "Super pulo"}, {20, "Wither"}, {9, "Náusea"},
            {17, "Fome"}, {3, "Pressa"}, {4, "Fadiga de mineração"}, {16, "Visão noturna"},
            {13, "Respiração aquática"}, {23, "Saturação"}, {6, "Vida instantânea"}, {7, "Dano instantâneo"},
    };
    private final BoolSetting[] mostrar = new BoolSetting[Potion.potionTypes.length];
    private final BoolSetting outros;

    public Efeitos() {
        super("efeitos", "Efeitos", "Poções ativas com tempo restante", Category.COMBATE, 5, 245, false);
        for (Object[] e : LISTA) {
            int id = (Integer) e[0];
            mostrar[id] = add(new BoolSetting("mostrar_" + id, "Mostrar " + e[1], true));
        }
        outros = add(new BoolSetting("mostrar_outros", "Mostrar efeitos de servidor", true));
    }

    /** O efeito aparece na tela? (a pessoa pode esconder cada um nas opções) */
    public boolean mostra(int id) {
        if (id >= 0 && id < mostrar.length && mostrar[id] != null) return mostrar[id].get();
        return outros.get();
    }

    private List<PotionEffect> effects(boolean preview) {
        List<PotionEffect> out = new ArrayList<PotionEffect>();
        if (mc.thePlayer != null) {
            Collection<PotionEffect> active = mc.thePlayer.getActivePotionEffects();
            for (PotionEffect e : active) if (mostra(e.getPotionID())) out.add(e);
        }
        if (out.isEmpty() && preview) {
            // Exemplo no editor de HUD: os dois primeiros efeitos que a pessoa deixou aparecer.
            for (Object[] e : LISTA) {
                int id = (Integer) e[0];
                if (out.size() < 2 && mostra(id) && Potion.potionTypes[id] != null && Potion.potionTypes[id].hasStatusIcon())
                    out.add(new PotionEffect(id, out.isEmpty() ? 1800 : 900, out.isEmpty() ? 1 : 0));
            }
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
