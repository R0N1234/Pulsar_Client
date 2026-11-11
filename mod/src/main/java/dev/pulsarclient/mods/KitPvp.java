package dev.pulsarclient.mods;

import dev.pulsarclient.gui.GuiKitPvp;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Kit PvP: a pessoa monta no painel onde cada item de PvP fica no inventário. Com um baú (ou o inventário) aberto,
 * Shift + botão direito num item leva ele direto para o espaço dele; segurando os dois e passando o mouse por cima,
 * cada item que o mouse toca vai para o lugar.
 *
 * Um item por vez, sempre pela mão da pessoa: o mod só faz os cliques que ela faria pegando e soltando o item,
 * nunca esvazia o baú sozinho (isso é o "ChestStealer" dos clients de hack, que os anticheats banem).
 *
 * O item é reconhecido pelo tipo e pelo material (peitoral de diamante, espada de ferro), nunca pelo encantamento.
 * Poções vão pelo efeito (força, velocidade, cura...), separando as de beber das splash quando o layout tem as duas.
 */
public class KitPvp extends PulsarMod {
    public static final int ESPACOS = 36;
    private static final int SOMBRA = 0xA08B8B8B;

    private final ModeSetting kit = add(new ModeSetting("kit", "Kit em uso", 0, "Kit 1", "Kit 2", "Kit 3"));
    private final ActionSetting montar = add(new ActionSetting("montar", "Montar o layout do kit", new Runnable() {
        @Override public void run() { abrirEditor(); }
    }));
    private final BoolSetting sombra = add(new BoolSetting("sombra", "Sombra do item nos espaços vazios", true));
    private final BoolSetting arrastar = add(new BoolSetting("arrastar", "Segurar e passar o mouse organiza vários", true));
    private final TextSetting[] kits = {
            add(new TextSetting("kit1", "")),
            add(new TextSetting("kit2", "")),
            add(new TextSetting("kit3", "")),
    };

    private final ItemStack[][] cache = new ItemStack[3][];
    private final String[] cacheDe = new String[3];

    private Field theSlot, guiLeft, guiTop;
    private boolean reflexaoFalhou;
    private boolean segurando;
    private Slot ultimoSlot;
    private static int movidos;

    public KitPvp() {
        super("kit_pvp", "Kit PvP", "Monte o inventário do PvP e pegue do baú com Shift + botão direito, cada item no seu lugar", Category.COMBATE, true);
    }

    /** Quantos itens o mod já levou para o lugar (teste automático). */
    public static int movidos() {
        return movidos;
    }

    private void abrirEditor() {
        mc.displayGuiScreen(new GuiKitPvp(mc.currentScreen, this));
    }

    // ---------- layout ----------

    public int kitAtual() {
        return kit.index();
    }

    public void usarKit(int i) {
        kit.set(i);
    }

    /** O layout do kit: 36 espaços (0 a 8 a barra, 9 a 35 o inventário), null onde não tem nada. */
    public ItemStack[] layout(int k) {
        String raw = kits[k].get();
        if (cache[k] == null || !raw.equals(cacheDe[k])) {
            ItemStack[] l = new ItemStack[ESPACOS];
            for (String parte : raw.split(";")) {
                int igual = parte.indexOf('='), arroba = parte.lastIndexOf('@');
                if (igual < 0 || arroba < igual) continue;
                try {
                    int slot = Integer.parseInt(parte.substring(0, igual).trim());
                    Item item = Item.getByNameOrId(parte.substring(igual + 1, arroba));
                    int meta = Integer.parseInt(parte.substring(arroba + 1).trim());
                    if (slot >= 0 && slot < ESPACOS && item != null) l[slot] = new ItemStack(item, 1, meta);
                } catch (NumberFormatException ignored) {
                }
            }
            cache[k] = l;
            cacheDe[k] = raw;
        }
        return cache[k];
    }

    public ItemStack[] layout() {
        return layout(kitAtual());
    }

    public void salvar(int k, ItemStack[] l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ESPACOS && i < l.length; i++) {
            if (l[i] == null) continue;
            ResourceLocation nome = Item.itemRegistry.getNameForObject(l[i].getItem());
            if (nome == null) continue;
            int meta = l[i].isItemStackDamageable() ? 0 : l[i].getMetadata();
            sb.append(sb.length() == 0 ? "" : ";").append(i).append('=').append(nome).append('@').append(meta);
        }
        kits[k].set(sb.toString());
        cache[k] = null;
        if (dev.pulsarclient.PulsarClient.get() != null) dev.pulsarclient.PulsarClient.get().save();
    }

    /** Uma cópia do item só com o que importa para o layout (tipo, material, efeito da poção). */
    public static ItemStack modelo(ItemStack s) {
        if (s == null) return null;
        return new ItemStack(s.getItem(), 1, s.isItemStackDamageable() ? 0 : s.getMetadata());
    }

    /** "minecraft:diamond_chestplate", "minecraft:golden_apple:1", "pocao:1:splash"... O encantamento não entra. */
    public static String chave(ItemStack s) {
        if (s == null || s.getItem() == null) return null;
        Item item = s.getItem();
        if (item instanceof ItemPotion) {
            List<PotionEffect> efeitos = ((ItemPotion) item).getEffects(s);
            String efeito = efeitos == null || efeitos.isEmpty() ? "agua" : String.valueOf(efeitos.get(0).getPotionID());
            return "pocao:" + efeito + (ItemPotion.isSplash(s.getMetadata()) ? ":splash" : "");
        }
        ResourceLocation nome = Item.itemRegistry.getNameForObject(item);
        String base = nome == null ? item.getUnlocalizedName() : nome.toString();
        return item.getHasSubtypes() && !s.isItemStackDamageable() ? base + ":" + s.getMetadata() : base;
    }

    /** A mesma chave sem separar poção de beber e splash. */
    private static String chaveSolta(String chave) {
        return chave != null && chave.endsWith(":splash") ? chave.substring(0, chave.length() - 7) : chave;
    }

    /** Os itens de PvP que aparecem no painel para montar o layout. */
    public static List<ItemStack> paleta() {
        List<ItemStack> p = new ArrayList<ItemStack>();
        Item[][] armaduras = {
                {Items.diamond_helmet, Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_boots},
                {Items.iron_helmet, Items.iron_chestplate, Items.iron_leggings, Items.iron_boots},
                {Items.chainmail_helmet, Items.chainmail_chestplate, Items.chainmail_leggings, Items.chainmail_boots},
                {Items.golden_helmet, Items.golden_chestplate, Items.golden_leggings, Items.golden_boots},
                {Items.leather_helmet, Items.leather_chestplate, Items.leather_leggings, Items.leather_boots},
        };
        for (Item[] a : armaduras) for (Item i : a) p.add(new ItemStack(i));
        Item[] armas = {Items.diamond_sword, Items.iron_sword, Items.stone_sword, Items.golden_sword, Items.wooden_sword,
                Items.diamond_axe, Items.iron_axe, Items.diamond_pickaxe, Items.iron_pickaxe, Items.bow, Items.arrow, Items.fishing_rod};
        for (Item i : armas) p.add(new ItemStack(i));
        p.add(new ItemStack(Items.golden_apple, 1, 0));
        p.add(new ItemStack(Items.golden_apple, 1, 1));
        p.add(new ItemStack(Items.ender_pearl));
        p.add(new ItemStack(Items.water_bucket));
        p.add(new ItemStack(Items.lava_bucket));
        p.add(new ItemStack(Items.flint_and_steel));
        // Poções: força II, velocidade II, cura II, resistência ao fogo e regeneração II, de beber e splash.
        int[] pocoes = {8233, 16425, 8226, 16418, 8229, 16421, 8227, 16419, 8225, 16417};
        for (int m : pocoes) p.add(new ItemStack(Items.potionitem, 1, m));
        p.add(new ItemStack(Items.cooked_beef));
        p.add(new ItemStack(Items.golden_carrot));
        p.add(new ItemStack(Items.mushroom_stew));
        p.add(new ItemStack(Items.snowball));
        p.add(new ItemStack(Items.egg));
        p.add(new ItemStack(Blocks.cobblestone));
        p.add(new ItemStack(Blocks.planks));
        p.add(new ItemStack(Blocks.web));
        p.add(new ItemStack(Blocks.tnt));
        return Collections.unmodifiableList(p);
    }

    // ---------- levar o item para o lugar ----------

    /** O espaço do inventário (0 a 35) de um slot da janela aberta, ou -1 se o slot não é do inventário do jogador. */
    private int espacoDe(Slot s) {
        if (s == null || mc.thePlayer == null || s.inventory != mc.thePlayer.inventory) return -1;
        int i = s.getSlotIndex();
        return i >= 0 && i < ESPACOS ? i : -1;
    }

    private Slot slotDoEspaco(GuiContainer gui, int espaco) {
        for (Slot s : gui.inventorySlots.inventorySlots) if (espacoDe(s) == espaco) return s;
        return null;
    }

    private boolean combina(String chaveLayout, String chaveItem, boolean solta) {
        if (chaveLayout == null || chaveItem == null) return false;
        return solta ? chaveSolta(chaveLayout).equals(chaveSolta(chaveItem)) : chaveLayout.equals(chaveItem);
    }

    /**
     * Para onde vai o item: o primeiro espaço do layout com o mesmo item que ainda não tem ele (ou tem e cabe mais).
     * Primeiro procura igualzinho; poção sem lugar exato aceita o lugar da mesma poção de beber ou splash.
     */
    private int destino(GuiContainer gui, ItemStack pilha, int origem) {
        ItemStack[] l = layout();
        String c = chave(pilha);
        InventoryPlayer inv = mc.thePlayer.inventory;
        for (int passo = 0; passo < 2; passo++) {
            boolean solta = passo == 1;
            int juntar = -1;
            for (int i = 0; i < ESPACOS; i++) {
                if (i == origem || !combina(chave(l[i]), c, solta)) continue;
                ItemStack la = inv.getStackInSlot(i);
                if (la == null || !combina(chave(l[i]), chave(la), solta)) return i;
                if (juntar < 0 && la.isStackable() && la.stackSize < la.getMaxStackSize() && ItemStack.areItemsEqual(la, pilha)
                        && ItemStack.areItemStackTagsEqual(la, pilha)) juntar = i;
            }
            if (juntar >= 0) return juntar;
        }
        return -1;
    }

    private void clicar(GuiContainer gui, Slot s) {
        mc.playerController.windowClick(gui.inventorySlots.windowId, s.slotNumber, 0, 0, mc.thePlayer);
    }

    /**
     * Leva o item do slot para o espaço dele no layout com os cliques normais de pegar e soltar.
     * O que estava no espaço vai para um espaço vazio do inventário fora do layout (ou volta para onde o item estava).
     * Devolve false quando o item não está no layout: aí o Shift + clique normal do jogo segue.
     */
    public boolean levar(GuiContainer gui, Slot de) {
        if (mc.thePlayer == null || de == null || !de.getHasStack() || mc.thePlayer.inventory.getItemStack() != null) return false;
        ItemStack pilha = de.getStack();
        int origem = espacoDe(de);
        String c = chave(pilha);
        ItemStack[] l = layout();
        if (origem >= 0 && combina(chave(l[origem]), c, true)) return true;
        int alvo = destino(gui, pilha, origem);
        if (alvo < 0) return false;
        Slot para = slotDoEspaco(gui, alvo);
        if (para == null) return false;
        clicar(gui, de);
        clicar(gui, para);
        if (mc.thePlayer.inventory.getItemStack() != null) {
            // Sobrou algo no mouse: o que estava no espaço (troca) ou o resto da pilha que não coube.
            Slot sobra = null;
            boolean mesmo = ItemStack.areItemsEqual(mc.thePlayer.inventory.getItemStack(), pilha);
            if (!mesmo) {
                for (int i = ESPACOS - 1; i >= 0 && sobra == null; i--) {
                    if (l[i] != null || mc.thePlayer.inventory.getStackInSlot(i) != null) continue;
                    sobra = slotDoEspaco(gui, i);
                }
            }
            clicar(gui, sobra != null ? sobra : de);
        }
        movidos++;
        return true;
    }

    // ---------- na tela do baú e do inventário ----------

    private boolean reflexao() {
        if (theSlot != null) return true;
        if (reflexaoFalhou) return false;
        try {
            theSlot = ReflectionHelper.findField(GuiContainer.class, "theSlot", "field_147006_u");
            guiLeft = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i");
            guiTop = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
        } catch (Throwable t) {
            theSlot = null;
            reflexaoFalhou = true;
            System.err.println("[Pulsar] Kit PvP: não achei os campos da tela do baú: " + t);
        }
        return theSlot != null;
    }

    private Slot slotNoMouse(GuiContainer gui) {
        try {
            return (Slot) theSlot.get(gui);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean vale(GuiScreen s) {
        return isEnabled() && s instanceof GuiContainer && !(s instanceof GuiContainerCreative) && mc.thePlayer != null && reflexao();
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        if (Mouse.getEventButton() != 1) return;
        if (!Mouse.getEventButtonState()) {
            // Soltou: o soltar do Shift + clique que o mod já tratou não vai para o jogo.
            if (segurando) e.setCanceled(true);
            segurando = false;
            ultimoSlot = null;
            return;
        }
        if (!vale(e.gui) || !GuiScreen.isShiftKeyDown()) return;
        GuiContainer gui = (GuiContainer) e.gui;
        Slot s = slotNoMouse(gui);
        if (levar(gui, s)) {
            e.setCanceled(true);
            segurando = true;
            ultimoSlot = s;
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (!vale(e.gui)) return;
        GuiContainer gui = (GuiContainer) e.gui;
        // Segurando Shift + botão direito: cada slot novo que o mouse toca vai para o lugar.
        if (segurando && arrastar.get()) {
            if (!Mouse.isButtonDown(1) || !GuiScreen.isShiftKeyDown()) {
                segurando = false;
            } else {
                Slot s = slotNoMouse(gui);
                if (s != null && s != ultimoSlot && s.getHasStack()) {
                    ultimoSlot = s;
                    levar(gui, s);
                }
            }
        }
        if (sombra.get()) desenharSombras(gui);
    }

    /** O item do layout, apagadinho, nos espaços vazios do inventário. */
    private void desenharSombras(GuiContainer gui) {
        ItemStack[] l = layout();
        int left, top;
        try {
            left = guiLeft.getInt(gui);
            top = guiTop.getInt(gui);
        } catch (Exception ex) {
            return;
        }
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        for (Slot s : gui.inventorySlots.inventorySlots) {
            int i = espacoDe(s);
            if (i < 0 || l[i] == null || s.getHasStack()) continue;
            int x = left + s.xDisplayPosition, y = top + s.yDisplayPosition;
            GlStateManager.enableDepth();
            mc.getRenderItem().renderItemAndEffectIntoGUI(l[i], x, y);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            Gui.drawRect(x, y, x + 16, y + 16, SOMBRA);
            RenderHelper.enableGUIStandardItemLighting();
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableDepth();
    }
}
