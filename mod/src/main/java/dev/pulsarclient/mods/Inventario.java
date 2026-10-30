package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.TreeSet;

/**
 * Inventário: trava itens para não dropar nem tirar do lugar sem querer (passe o mouse no item e aperte L),
 * impede de dropar os tipos de item escolhidos (espada, arco, picareta...) e ajusta a tela do inventário.
 *
 * Os tipos escolhidos continuam livres para mexer no inventário; só não saem dele: a tecla de dropar,
 * clicar fora da janela com o item no mouse e fechar a janela com o item no mouse ficam bloqueados.
 */
public class Inventario extends PulsarMod {
    private static final int LOCK_COLOR = 0xFFFF6B6B;

    public final KeyBinding lockKey = new KeyBinding("Travar item (no inventário)", Keyboard.KEY_L, "Pulsar Client");

    private final BoolSetting locking = add(new BoolSetting("travar", "Travar itens com a tecla L", true));
    private final BoolSetting noShift = add(new BoolSetting("centralizar", "Inventário no meio mesmo com efeitos", true));
    private final BoolSetting hideEffects = add(new BoolSetting("sem_efeitos", "Esconder os efeitos ao lado do inventário", false));
    private final TextSetting lockedSlots = add(new TextSetting("slots", ""));

    // Tipos de item que não podem ser dropados.
    private final BoolSetting swords = add(new BoolSetting("nao_dropar_espadas", "Não dropar espadas", true));
    private final BoolSetting bows = add(new BoolSetting("nao_dropar_arcos", "Não dropar arcos", false));
    private final BoolSetting rods = add(new BoolSetting("nao_dropar_varas", "Não dropar varas de pesca", false));
    private final BoolSetting axes = add(new BoolSetting("nao_dropar_machados", "Não dropar machados", false));
    private final BoolSetting pickaxes = add(new BoolSetting("nao_dropar_picaretas", "Não dropar picaretas", false));
    private final BoolSetting shovels = add(new BoolSetting("nao_dropar_pas", "Não dropar pás", false));
    private final BoolSetting hoes = add(new BoolSetting("nao_dropar_enxadas", "Não dropar enxadas", false));
    private final BoolSetting apples = add(new BoolSetting("nao_dropar_macas", "Não dropar maçãs douradas", false));
    private final BoolSetting pearls = add(new BoolSetting("nao_dropar_perolas", "Não dropar pérolas do fim", false));
    private final BoolSetting potions = add(new BoolSetting("nao_dropar_pocoes", "Não dropar poções", false));
    private final BoolSetting buckets = add(new BoolSetting("nao_dropar_baldes", "Não dropar baldes de água e lava", false));

    private final TreeSet<Integer> locked = new TreeSet<Integer>();
    private String loadedFrom;
    private Field theSlot;
    private Field guiLeft;
    private Field guiTop;
    private Field xSize;
    private Field ySize;
    private Field hasEffects;
    private boolean reflectionFailed;
    private long warnUntil;

    public Inventario() {
        super("inventario", "Inventário", "Protege a espada e outros itens contra drop, trava slots (tecla L) e ajusta o inventário", Category.COMBATE, true);
    }

    // ---------- slots travados ----------

    private TreeSet<Integer> locked() {
        // Os slots ficam salvos como "0,3,8" em config.json.
        String raw = lockedSlots.get();
        if (!raw.equals(loadedFrom)) {
            loadedFrom = raw;
            locked.clear();
            for (String p : raw.split(",")) {
                try {
                    if (!p.trim().isEmpty()) locked.add(Integer.parseInt(p.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return locked;
    }

    public boolean isLocked(int inventoryIndex) {
        return isEnabled() && locking.get() && locked().contains(inventoryIndex);
    }

    public void toggleLock(int inventoryIndex) {
        TreeSet<Integer> set = locked();
        if (!set.remove(inventoryIndex)) set.add(inventoryIndex);
        StringBuilder sb = new StringBuilder();
        for (int i : set) sb.append(sb.length() == 0 ? "" : ",").append(i);
        lockedSlots.set(sb.toString());
        loadedFrom = lockedSlots.get();
    }

    private boolean reflect() {
        if (theSlot != null) return true;
        if (reflectionFailed) return false;
        try {
            theSlot = ReflectionHelper.findField(GuiContainer.class, "theSlot", "field_147006_u");
            guiLeft = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i");
            guiTop = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
            xSize = ReflectionHelper.findField(GuiContainer.class, "xSize", "field_146999_f");
            ySize = ReflectionHelper.findField(GuiContainer.class, "ySize", "field_147000_g");
        } catch (Throwable t) {
            theSlot = null;
            reflectionFailed = true;
            System.err.println("[Pulsar] Inventário: não achei os campos da tela de inventário: " + t);
            return false;
        }
        try {
            hasEffects = ReflectionHelper.findField(InventoryEffectRenderer.class, "hasActivePotionEffects", "field_147045_u");
        } catch (Throwable t) {
            // Só os ajustes de efeitos deixam de funcionar; o travamento de itens continua.
            System.err.println("[Pulsar] Inventário: ajuste dos efeitos indisponível: " + t);
        }
        return true;
    }


    private Slot hoveredSlot(GuiContainer gui) {
        try {
            return (Slot) theSlot.get(gui);
        } catch (Exception e) {
            return null;
        }
    }

    /** Índice no inventário do jogador do slot embaixo do mouse, ou -1. */
    private int hoveredIndex(GuiContainer gui) {
        Slot slot = hoveredSlot(gui);
        if (slot == null || mc.thePlayer == null || slot.inventory != mc.thePlayer.inventory) return -1;
        return slot.getSlotIndex();
    }

    /** Item de um tipo que o jogador escolheu não dropar. */
    public boolean isProtectedType(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.getItem() == null) return false;
        Item item = stack.getItem();
        if (item instanceof ItemSword) return swords.get();
        if (item instanceof ItemBow) return bows.get();
        if (item instanceof ItemFishingRod) return rods.get();
        if (item instanceof ItemAxe) return axes.get();
        if (item instanceof ItemPickaxe) return pickaxes.get();
        if (item instanceof ItemSpade) return shovels.get();
        if (item instanceof ItemHoe) return hoes.get();
        if (item instanceof ItemPotion) return potions.get();
        if (item == Items.golden_apple) return apples.get();
        if (item == Items.ender_pearl) return pearls.get();
        if (item == Items.water_bucket || item == Items.lava_bucket) return buckets.get();
        return false;
    }

    /** Item da mão que não pode ser dropado: slot travado com L ou tipo protegido. */
    private boolean heldProtected() {
        if (mc.thePlayer == null) return false;
        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        return held != null && (isLocked(mc.thePlayer.inventory.currentItem) || isProtectedType(held));
    }

    private void warn() {
        warn("Item travado (aperte L no inventário para destravar)");
    }

    private void warnType() {
        warn("Esse item está protegido contra drop (mude no painel do Pulsar)");
    }

    private void warn(String message) {
        if (System.currentTimeMillis() < warnUntil) return;
        warnUntil = System.currentTimeMillis() + 1500;
        mc.ingameGUI.setRecordPlaying(message, false);
    }

    /** Item preso no mouse dentro de uma janela de inventário. */
    private ItemStack cursorStack() {
        return mc.thePlayer == null ? null : mc.thePlayer.inventory.getItemStack();
    }

    /** Mouse fora da janela: clicar ali com um item no mouse joga o item no chão. */
    private boolean mouseOutside(GuiContainer gui) {
        try {
            int mx = Mouse.getEventX() * gui.width / mc.displayWidth;
            int my = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
            int left = guiLeft.getInt(gui);
            int top = guiTop.getInt(gui);
            return mx < left || my < top || mx >= left + xSize.getInt(gui) || my >= top + ySize.getInt(gui);
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- dentro do inventário ----------

    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer) || !Keyboard.getEventKeyState() || !reflect()) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (gui instanceof GuiContainerCreative) return;
        int key = Keyboard.getEventKey();
        // Fechar a janela com um item protegido no mouse jogaria o item no chão.
        if ((key == Keyboard.KEY_ESCAPE || key == mc.gameSettings.keyBindInventory.getKeyCode()) && isProtectedType(cursorStack())) {
            event.setCanceled(true);
            warn("Coloque o item protegido num espaço antes de fechar");
            return;
        }
        Slot slot = hoveredSlot(gui);
        if (key == mc.gameSettings.keyBindDrop.getKeyCode() && slot != null && slot.getHasStack() && isProtectedType(slot.getStack())) {
            event.setCanceled(true);
            warnType();
            return;
        }
        if (!locking.get()) return;
        int hovered = hoveredIndex(gui);
        if (key == lockKey.getKeyCode() && key != 0) {
            if (hovered >= 0) toggleLock(hovered);
            event.setCanceled(true);
            return;
        }
        boolean blocked = false;
        if (key == mc.gameSettings.keyBindDrop.getKeyCode()) blocked = hovered >= 0 && isLocked(hovered);
        for (int i = 0; i < 9; i++) {
            // Teclas 1 a 9 trocam o item do mouse com a barra: bloqueia se um dos dois estiver travado.
            if (key == mc.gameSettings.keyBindsHotbar[i].getKeyCode() && hovered >= 0) blocked = isLocked(hovered) || isLocked(i);
        }
        if (blocked) {
            event.setCanceled(true);
            warn();
        }
    }

    /**
     * Clique e soltar do mouse. Os dois são barrados: com um item no mouse, é ao soltar o botão
     * que o jogo coloca o item no slot (ou joga no chão, se for fora da janela).
     */
    @SubscribeEvent
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isEnabled() || !(event.gui instanceof GuiContainer) || !reflect()) return;
        if (event.gui instanceof GuiContainerCreative || Mouse.getEventButton() < 0) return;
        GuiContainer gui = (GuiContainer) event.gui;
        boolean press = Mouse.getEventButtonState();
        if (locking.get()) {
            int hovered = hoveredIndex(gui);
            if (hovered >= 0 && isLocked(hovered)) {
                event.setCanceled(true);
                if (press) warn();
                return;
            }
        }
        if (isProtectedType(cursorStack()) && hoveredSlot(gui) == null && mouseOutside(gui)) {
            event.setCanceled(true);
            if (press) warnType();
        }
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        adjustEffects(event.gui);
    }

    /** Tira o empurrão para a direita que o jogo dá no inventário quando há efeitos, e/ou esconde os efeitos. */
    private void adjustEffects(GuiScreen screen) {
        if (!isEnabled() || !(screen instanceof GuiInventory) || !reflect() || hasEffects == null) return;
        try {
            GuiContainer gui = (GuiContainer) screen;
            if (hasEffects.getBoolean(gui) && (noShift.get() || hideEffects.get())) {
                guiLeft.setInt(gui, (gui.width - xSize.getInt(gui)) / 2);
            }
            if (hideEffects.get()) hasEffects.setBoolean(gui, false);
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!isEnabled() || !locking.get() || !(event.gui instanceof GuiContainer) || !reflect()) return;
        GuiScreen screen = event.gui;
        if (screen instanceof GuiContainerCreative) return;
        try {
            GuiContainer gui = (GuiContainer) screen;
            int left = guiLeft.getInt(gui);
            int top = guiTop.getInt(gui);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            for (Slot slot : gui.inventorySlots.inventorySlots) {
                if (slot.inventory != mc.thePlayer.inventory || !isLocked(slot.getSlotIndex())) continue;
                drawLock(left + slot.xDisplayPosition, top + slot.yDisplayPosition);
            }
            GlStateManager.enableDepth();
        } catch (Exception ignored) {
        }
    }

    // ---------- fora do inventário ----------

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // O inventário recalcula a posição a cada tick por causa dos efeitos, então o ajuste é refeito aqui.
            adjustEffects(mc.currentScreen);
            return;
        }
        consumeDrop();
    }

    // O jogo lê a tecla de dropar logo depois destes eventos, no mesmo tick: com o item da mão protegido,
    // o aperto é consumido aqui e o item fica.
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        consumeDrop();
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        consumeDrop();
    }

    private void consumeDrop() {
        if (mc.thePlayer == null || mc.currentScreen != null || !heldProtected()) return;
        boolean consumed = false;
        while (mc.gameSettings.keyBindDrop.isPressed()) consumed = true;
        if (!consumed) return;
        if (isLocked(mc.thePlayer.inventory.currentItem)) warn();
        else warnType();
    }

    @SubscribeEvent
    public void onHotbar(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR || !isEnabled() || !locking.get() || mc.thePlayer == null) return;
        int x0 = event.resolution.getScaledWidth() / 2 - 91 + 3;
        int y0 = event.resolution.getScaledHeight() - 19;
        for (int i = 0; i < 9; i++) {
            if (isLocked(i)) drawLock(x0 + i * 20, y0);
        }
    }

    /** Borda vermelha e um cadeadinho no canto do slot (16x16). */
    private static void drawLock(int x, int y) {
        Gui.drawRect(x, y, x + 16, y + 1, LOCK_COLOR);
        Gui.drawRect(x, y + 15, x + 16, y + 16, LOCK_COLOR);
        Gui.drawRect(x, y, x + 1, y + 16, LOCK_COLOR);
        Gui.drawRect(x + 15, y, x + 16, y + 16, LOCK_COLOR);
        // Cadeado: arco em cima e corpo embaixo, no canto superior direito.
        Gui.drawRect(x + 10, y + 1, x + 14, y + 2, LOCK_COLOR);
        Gui.drawRect(x + 10, y + 1, x + 11, y + 4, LOCK_COLOR);
        Gui.drawRect(x + 13, y + 1, x + 14, y + 4, LOCK_COLOR);
        Gui.drawRect(x + 9, y + 4, x + 15, y + 8, LOCK_COLOR);
    }
}
