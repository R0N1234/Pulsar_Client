package dev.pulsarclient;

import dev.pulsarclient.config.PulsarConfig;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.ClickTracker;
import dev.pulsarclient.mods.Propulsao;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.mods.SimpleHudMods;
import dev.pulsarclient.mods.TeclasPulse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod(modid = PulsarClient.MODID, name = "Pulsar Client", version = "0.1.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class PulsarClient {
    public static final String MODID = "pulsar";
    private static PulsarClient instance;

    private final List<PulsarMod> mods = new ArrayList<PulsarMod>();
    private final Propulsao propulsao = new Propulsao();
    private PulsarConfig config;

    private final KeyBinding keyHudEditor = new KeyBinding("Editor de HUD", Keyboard.KEY_RSHIFT, "Pulsar Client");
    private final KeyBinding keySprint = new KeyBinding("Propulsão (correr fixo)", Keyboard.KEY_R, "Pulsar Client");

    public static PulsarClient get() {
        return instance;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this;
        mods.add(new TeclasPulse());
        mods.add(new SimpleHudMods.FrequenciaCps());
        mods.add(new SimpleHudMods.Fps());
        mods.add(new SimpleHudMods.Latencia());
        mods.add(new SimpleHudMods.Coordenadas());
        mods.add(propulsao);

        config = new PulsarConfig(Minecraft.getMinecraft().mcDataDir);
        config.load(mods);

        ClientRegistry.registerKeyBinding(keyHudEditor);
        ClientRegistry.registerKeyBinding(keySprint);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public List<PulsarMod> mods() {
        return Collections.unmodifiableList(mods);
    }

    public void save() {
        config.save(mods);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.currentScreen instanceof GuiHudEditor) return;
        for (PulsarMod mod : mods) {
            if (mod.isEnabled()) mod.render(false);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (PulsarMod mod : mods) mod.tick();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (keyHudEditor.isPressed()) Minecraft.getMinecraft().displayGuiScreen(new GuiHudEditor());
        if (keySprint.isPressed()) propulsao.toggle();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.buttonstate) ClickTracker.click(event.button);
    }
}
