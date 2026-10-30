package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Recargas: muitos servidores de PvP dão um tempo de espera depois de jogar pérola ou comer maçã dourada.
 * O mod percebe quando você usou o item (o número dele no inventário cai logo depois do uso)
 * e mostra quanto falta, com o tempo de cada item ajustável.
 */
public class Recargas extends HudMod {
    private static final ItemStack PEROLA = new ItemStack(Items.ender_pearl);
    private static final ItemStack MACA = new ItemStack(Items.golden_apple);
    private static final ItemStack MACA_ENC = new ItemStack(Items.golden_apple, 1, 1);
    private static final ItemStack[] ICONES = {PEROLA, MACA, MACA_ENC};
    private static final int ROW = 17;

    private final SliderSetting[] tempos = {
            add(new SliderSetting("perola", "Espera da pérola de Ender (0 = não mostrar)", 15, 0, 30, 1, "s")),
            add(new SliderSetting("maca", "Espera da maçã dourada (0 = não mostrar)", 0, 0, 60, 1, "s")),
            add(new SliderSetting("maca_encantada", "Espera da maçã encantada (0 = não mostrar)", 0, 0, 120, 5, "s")),
    };
    private final BoolSetting som = add(new BoolSetting("som", "Tocar um som quando a espera acabar", true));

    private final long[] fim = new long[3];
    private final int[] antes = {-1, -1, -1};
    private final long[] usouAte = new long[3];

    public Recargas() {
        super("recargas", "Recargas", "Tempo de espera da pérola e das maçãs douradas", Category.COMBATE, 10000, 150, false);
    }

    @Override
    public void tick() {
        if (mc.thePlayer == null) {
            antes[0] = antes[1] = antes[2] = -1;
            return;
        }
        long agora = System.currentTimeMillis();
        ItemStack mao = mc.thePlayer.getHeldItem();
        // Pérola: jogada com o botão de usar; maçã: comida até o fim (o número cai quando termina de comer).
        if (mao != null && mao.getItem() == Items.ender_pearl && mc.gameSettings.keyBindUseItem.isKeyDown()) usouAte[0] = agora + 1000;
        ItemStack uso = mc.thePlayer.getItemInUse();
        if (uso != null && uso.getItem() == Items.golden_apple) usouAte[uso.getMetadata() == 0 ? 1 : 2] = agora + 1500;

        int[] agoraTem = {contar(Items.ender_pearl, -1), contar(Items.golden_apple, 0), contar(Items.golden_apple, 1)};
        for (int i = 0; i < 3; i++) {
            if (antes[i] >= 0 && agoraTem[i] < antes[i] && agora < usouAte[i] && mc.currentScreen == null && tempos[i].getInt() > 0) {
                fim[i] = agora + tempos[i].getInt() * 1000L;
            }
            antes[i] = agoraTem[i];
            if (fim[i] != 0 && agora >= fim[i]) {
                fim[i] = 0;
                if (isEnabled() && som.get()) mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("random.orb"), 1.4f));
            }
        }
    }

    private int contar(Object item, int meta) {
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s != null && s.getItem() == item && (meta < 0 || s.getMetadata() == meta)) n += s.stackSize;
        }
        return n;
    }

    private int linhas(boolean preview) {
        int n = 0;
        long agora = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) if (preview ? tempos[i].getInt() > 0 : fim[i] > agora) n++;
        return n;
    }

    @Override public int width() { return 18 + font().getStringWidth("00.0s") + 2; }
    @Override public int height() { return Math.max(1, linhas(true)) * ROW - 1; }

    @Override
    public void render(boolean preview) {
        int n = linhas(preview);
        if (n == 0) return;
        long agora = System.currentTimeMillis();
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + width() + 2, y + n * ROW + 1, BACKGROUND);
        int ry = y;
        for (int i = 0; i < 3; i++) {
            float falta = preview ? tempos[i].getInt() * 0.6f : (fim[i] - agora) / 1000f;
            if (preview ? tempos[i].getInt() <= 0 : fim[i] <= agora) continue;
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(ICONES[i], x, ry);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            font().drawStringWithShadow(String.format("%.1fs", falta), x + 18, ry + 2, accent());
            // Barra do tempo que ainda falta.
            float total = Math.max(1, tempos[i].getInt());
            int bw = width() - 18;
            Gui.drawRect(x + 18, ry + 12, x + 18 + bw, ry + 13, 0x55FFFFFF);
            Gui.drawRect(x + 18, ry + 12, x + 18 + Math.round(bw * Math.min(1f, falta / total)), ry + 13, accent());
            ry += ROW;
        }
        GlStateManager.enableBlend();
    }
}
