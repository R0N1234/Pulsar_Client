package dev.pulsarclient;

import dev.pulsarclient.config.PulsarConfig;
import dev.pulsarclient.gui.GuiPulsarPanel;
import dev.pulsarclient.gui.Janela;
import dev.pulsarclient.gui.MenuButtons;
import dev.pulsarclient.gui.SelfTest;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.AnimacoesClassicas;
import dev.pulsarclient.mods.AtalhosTexto;
import dev.pulsarclient.mods.Atmosfera;
import dev.pulsarclient.mods.Blindagem;
import dev.pulsarclient.mods.CampoColisao;
import dev.pulsarclient.mods.ClickTracker;
import dev.pulsarclient.mods.CombatHud;
import dev.pulsarclient.mods.CombatTracker;
import dev.pulsarclient.mods.ContadorItens;
import dev.pulsarclient.mods.ContadorTnt;
import dev.pulsarclient.mods.Contorno;
import dev.pulsarclient.mods.CoresVivas;
import dev.pulsarclient.mods.Cronometro;
import dev.pulsarclient.mods.FontePulsar;
import dev.pulsarclient.mods.GgAutomatico;
import dev.pulsarclient.mods.Impacto;
import dev.pulsarclient.mods.Inventario;
import dev.pulsarclient.mods.ListaTab;
import dev.pulsarclient.mods.MeuNick;
import dev.pulsarclient.mods.OrbitaLivre;
import dev.pulsarclient.mods.Placar;
import dev.pulsarclient.mods.PoeiraEstelar;
import dev.pulsarclient.mods.Efeitos;
import dev.pulsarclient.mods.HudMod;
import dev.pulsarclient.mods.MiraPulsar;
import dev.pulsarclient.mods.MotorPulsar;
import dev.pulsarclient.mods.Propulsao;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.mods.Recargas;
import dev.pulsarclient.mods.SimpleHudMods;
import dev.pulsarclient.mods.Supernova;
import dev.pulsarclient.mods.TeclasPulse;
import dev.pulsarclient.mods.Telescopio;
import dev.pulsarclient.mods.TremorDano;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod(modid = PulsarClient.MODID, name = "Pulsar Client", version = PulsarClient.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class PulsarClient {
    public static final String MODID = "pulsar";
    public static final String VERSION = "0.6.0";
    private static PulsarClient instance;

    private final List<PulsarMod> mods = new ArrayList<PulsarMod>();
    private final List<HudMod> hudMods = new ArrayList<HudMod>();
    private final Set<PulsarMod> broken = new HashSet<PulsarMod>();
    private final Propulsao propulsao = new Propulsao();
    private final Telescopio telescopio = new Telescopio();
    private final MotorPulsar motor = new MotorPulsar();
    private final OrbitaLivre orbita = new OrbitaLivre();
    private final Inventario inventario = new Inventario();
    private final Cronometro cronometro = new Cronometro();
    private final PoeiraEstelar poeira = new PoeiraEstelar();
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
        add(new SimpleHudMods.Relogio());
        add(new SimpleHudMods.Memoria());
        add(new SimpleHudMods.Bussola());
        add(new SimpleHudMods.Servidor());
        add(new SimpleHudMods.Saturacao());
        add(cronometro);
        add(propulsao);
        add(new MiraPulsar());
        add(new Blindagem());
        add(new Efeitos());
        add(new CombatHud.Combo());
        add(new CombatHud.Alcance());
        add(new TremorDano());
        add(new CampoColisao());
        add(new ContadorItens());
        add(new ContadorTnt());
        add(new Recargas());
        add(new AnimacoesClassicas());
        add(new Impacto());
        add(inventario);
        add(telescopio);
        add(orbita);
        add(new MeuNick());
        add(poeira);
        add(new Supernova());
        add(new Contorno());
        add(new Atmosfera());
        add(new Placar());
        add(new ListaTab());
        add(new FontePulsar());
        add(new CoresVivas());
        add(new AtalhosTexto());
        add(new GgAutomatico());
        add(motor);

        config = new PulsarConfig(Minecraft.getMinecraft().mcDataDir);
        config.load(mods);

        ClientRegistry.registerKeyBinding(keyPanel);
        ClientRegistry.registerKeyBinding(keySprint);
        ClientRegistry.registerKeyBinding(telescopio.key);
        ClientRegistry.registerKeyBinding(orbita.key);
        ClientRegistry.registerKeyBinding(inventario.lockKey);
        ClientRegistry.registerKeyBinding(cronometro.key);
        CombatTracker.listen(poeira);
        Janela.aplicar(VERSION);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MenuButtons());
        MinecraftForge.EVENT_BUS.register(new CombatTracker());
        if (Boolean.getBoolean("pulsar.selftest")) MinecraftForge.EVENT_BUS.register(new SelfTest());
        System.out.println("[Pulsar] Pulsar Client " + VERSION + " iniciado com " + mods.size() + " mods");
        System.out.println("[Pulsar] Motor: ganchos ativos " + MotorPulsar.hooksSummary());
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

    public OrbitaLivre orbita() {
        return orbita;
    }

    public Inventario inventario() {
        return inventario;
    }

    /** Mod pelo id (o mesmo do launcher), ou null. */
    public PulsarMod mod(String id) {
        for (PulsarMod m : mods) if (m.id().equals(id)) return m;
        return null;
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
            if (!mod.isEnabled() || broken.contains(mod)) continue;
            // A posição salva não muda: o mod só é desenhado puxado para dentro da tela.
            // Assim, com a janela minimizada ou pequena por um instante, o layout não se perde.
            int savedX = mod.x;
            int savedY = mod.y;
            try {
                keepOnScreen(mod, event.resolution);
                mod.render(false);
            } catch (Throwable t) {
                fail(mod, t);
            } finally {
                mod.x = savedX;
                mod.y = savedY;
            }
        }
    }

    /**
     * Um erro num mod nunca pode fechar o jogo: o mod para de rodar até reiniciar o jogo
     * e o erro vai para o log, para ser corrigido.
     */
    public void fail(PulsarMod mod, Throwable t) {
        if (!broken.add(mod)) return;
        System.err.println("[Pulsar] O mod " + mod.name() + " deu erro e foi pausado até reiniciar o jogo:");
        t.printStackTrace();
        try {
            mod.onBroken();
        } catch (Throwable ignored) {
        }
    }

    /** Mantém o mod dentro da tela, mesmo depois de mudar o tamanho da janela ou a escala da interface. */
    public static void keepOnScreen(HudMod mod, ScaledResolution res) {
        mod.x = Math.max(2, Math.min(mod.x, res.getScaledWidth() - mod.width() - 2));
        mod.y = Math.max(2, Math.min(mod.y, res.getScaledHeight() - mod.height() - 2));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (PulsarMod mod : mods) {
            if (broken.contains(mod)) continue;
            try {
                mod.tick();
            } catch (Throwable t) {
                fail(mod, t);
            }
        }
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
