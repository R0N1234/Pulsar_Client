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
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.TreeSet;

/**
 * Inventário: trava itens para não dropar nem tirar do lugar sem querer (passe o mouse no item e aperte L),
 * e ajustes da tela do inventário.
 */
public class Inventario extends PulsarMod {
    private static final int LOCK_COLOR = 0xFFFF6B6B;

    public final KeyBinding lockKey = new KeyBinding("Travar item (no inventário)", Keyboard.KEY_L, "Pulsar Client");

    private final BoolSetting locking = add(new BoolSetting("travar", "Travar itens com a tecla L", true));
    private final BoolSetting noShift = add(new BoolSetting("centralizar", "Inventário no meio mesmo com efeitos", true));
    private final BoolSetting hideEffects = add(new BoolSetting("sem_efeitos", "Esconder os efeitos ao lado do inventário", false));
    private final TextSetting lockedSlots = add(new TextSetting("slots", ""));

    private final TreeSet<Integer> locked = new TreeSet<Integer>();
    private String loadedFrom;
    private Field theSlot;
    private Field guiLeft;
    private Field guiTop;
    private Field xSize;
    private Field hasEffects;
    private boolean reflectionFailed;
    private long warnUntil;

    public Inventario() {
        super("inventario", "Inventário", "Trava itens para não dropar (tecla L) e ajusta a tela do inventário", Category.COMBATE, true);
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


    /** Índice no inventário do jogador do slot embaixo do mouse, ou -1. */
    private int hoveredIndex(GuiContainer gui) {
        try {
            Slot slot = (Slot) theSlot.get(gui);
            if (slot == null || mc.thePlayer == null || slot.inventory != mc.thePlayer.inventory) return -1;
            return slot.getSlotIndex();
        } catch (Exception e) {
            return -1;
        }
    }

    private void warn() {
        if (System.currentTimeMillis() < warnUntil) return;
        warnUntil = System.currentTimeMillis() + 1500;
        mc.ingameGUI.setRecordPlaying("Item travado (aperte L no inventário para destravar)", false);
    }

    // ---------- dentro do inventário ----------

    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!isEnabled() || !locking.get() || !(event.gui instanceof GuiContainer) || !Keyboard.getEventKeyState() || !reflect()) return;
        GuiContainer gui = (GuiContainer) event.gui;
        if (gui instanceof GuiContainerCreative) return;
        int key = Keyboard.getEventKey();
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

    @SubscribeEvent
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!isEnabled() || !locking.get() || !(event.gui instanceof GuiContainer) || !reflect()) return;
        if (event.gui instanceof GuiContainerCreative || !Mouse.getEventButtonState() || Mouse.getEventButton() < 0) return;
        int hovered = hoveredIndex((GuiContainer) event.gui);
        if (hovered >= 0 && isLocked(hovered)) {
            event.setCanceled(true);
            warn();
        }
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!isEnabled() || !(event.gui instanceof GuiInventory) || !reflect() || hasEffects == null) return;
        try {
            GuiContainer gui = (GuiContainer) event.gui;
            if (noShift.get() && hasEffects.getBoolean(gui)) {
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
        // Antes do jogo ler a tecla de dropar: com o item da mão travado, a tecla é consumida aqui.
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.currentScreen != null) return;
        if (!isLocked(mc.thePlayer.inventory.currentItem) || mc.thePlayer.inventory.getCurrentItem() == null) return;
        boolean consumed = false;
        while (mc.gameSettings.keyBindDrop.isPressed()) consumed = true;
        if (consumed) warn();
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
