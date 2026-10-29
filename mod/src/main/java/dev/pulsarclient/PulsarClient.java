package dev.pulsarclient;

import dev.pulsarclient.config.PulsarConfig;
import dev.pulsarclient.gui.GuiPulsarPanel;
import dev.pulsarclient.gui.MenuButtons;
import dev.pulsarclient.gui.SelfTest;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.ClickTracker;
import dev.pulsarclient.mods.Contorno;
import dev.pulsarclient.mods.HudMod;
import dev.pulsarclient.mods.MiraPulsar;
import dev.pulsarclient.mods.MotorPulsar;
import dev.pulsarclient.mods.Propulsao;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.mods.SimpleHudMods;
import dev.pulsarclient.mods.Supernova;
import dev.pulsarclient.mods.TeclasPulse;
import dev.pulsarclient.mods.Telescopio;
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

@Mod(modid = PulsarClient.MODID, name = "Pulsar Client", version = PulsarClient.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class PulsarClient {
    public static final String MODID = "pulsar";
    public static final String VERSION = "0.3.0";
    private static PulsarClient instance;

    private final List<PulsarMod> mods = new ArrayList<PulsarMod>();
    private final List<HudMod> hudMods = new ArrayList<HudMod>();
    private final Propulsao propulsao = new Propulsao();
    private final Telescopio telescopio = new Telescopio();
    private final MotorPulsar motor = new MotorPulsar();
    private PulsarConfig config;

    private final KeyBinding keyPanel = new KeyBinding("Painel do Pulsar", Keyboard.KEY_RSHIFT, "Pulsar Client");
    private final KeyBinding keySprint = new KeyBinding("Propulsão (correr fixo)", Keyboard.KEY_R, "Pulsar Client");

    public static PulsarClient get() {
        return instance;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        instance = this;
        add(new TeclasPulse());
        add(new SimpleHudMods.FrequenciaCps());
        add(new SimpleHudMods.Fps());
        add(new SimpleHudMods.Latencia());
        add(new SimpleHudMods.Coordenadas());
        add(propulsao);
        add(new MiraPulsar());
        add(telescopio);
        add(new Supernova());
        add(new Contorno());
        add(motor);

        config = new PulsarConfig(Minecraft.getMinecraft().mcDataDir);
        config.load(mods);

        ClientRegistry.registerKeyBinding(keyPanel);
        ClientRegistry.registerKeyBinding(keySprint);
        ClientRegistry.registerKeyBinding(telescopio.key);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MenuButtons());
        if (Boolean.getBoolean("pulsar.selftest")) MinecraftForge.EVENT_BUS.register(new SelfTest());
        System.out.println("[Pulsar] Pulsar Client " + VERSION + " iniciado com " + mods.size() + " mods");
    }

    private void add(PulsarMod mod) {
        mods.add(mod);
        if (mod instanceof HudMod) hudMods.add((HudMod) mod);
        MinecraftForge.EVENT_BUS.register(mod);
    }

    public List<PulsarMod> mods() {
        return Collections.unmodifiableList(mods);
    }

    public List<HudMod> hudMods() {
        return Collections.unmodifiableList(hudMods);
    }

    public MotorPulsar motor() {
        return motor;
    }

    public KeyBinding panelKey() {
        return keyPanel;
    }

    public void save() {
        config.save(mods);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo || mc.currentScreen instanceof GuiHudEditor) return;
        for (HudMod mod : hudMods) {
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
        if (keyPanel.isPressed()) Minecraft.getMinecraft().displayGuiScreen(new GuiPulsarPanel(null));
        if (keySprint.isPressed() && propulsao.isEnabled()) propulsao.toggleActive();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.buttonstate) ClickTracker.click(event.button);
    }
}
