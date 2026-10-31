package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Órbita Livre (o "Perspective Mod"): segurando a tecla, a câmera vai para a terceira pessoa e gira 360° em volta
 * do personagem com o mouse, sem virar o personagem. Ao soltar, volta para a visão de antes.
 *
 * Como funciona: o jogo aplica o movimento do mouse no personagem e só depois monta a câmera.
 * Na hora de montar a câmera, o Pulsar pega esse movimento, passa para a câmera e devolve o personagem
 * para onde ele estava. Para o servidor, o personagem nunca virou.
 */
public class OrbitaLivre extends PulsarMod {
    public final KeyBinding key = new KeyBinding("Órbita Livre (girar a câmera)", Keyboard.KEY_LMENU, "Pulsar Client");

    private final TeclaDoJogo orbitKey = add(new TeclaDoJogo("tecla_orbita", "Tecla da órbita", key));
    private final ModeSetting mode = add(new ModeSetting("modo", "Jeito da tecla", 0, "Segurar", "Apertar liga e desliga"));
    private final BoolSetting invert = add(new BoolSetting("inverter", "Inverter o mouse para cima e para baixo", false));
    private final BoolSetting front = add(new BoolSetting("frente", "Começar olhando de frente", false));

    private boolean active;
    private boolean toggled;
    private boolean forced;
    private int savedView;
    private float camYaw;
    private float camPitch;
    private boolean captured;
    private float yaw0;
    private float pitch0;
    private float prevYaw0;
    private float prevPitch0;
    private float drawnYaw = Float.NaN;
    private float drawnPitch = Float.NaN;

    public OrbitaLivre() {
        super("orbita_livre", "Órbita Livre", "Gira a câmera 360° em volta do personagem com uma tecla", Category.VISUAL, true);
    }

    public boolean active() { return active; }

    /** Usado pelo teste automático: liga a órbita sem tecla, com a câmera girada yawOffset graus. */
    public void forceView(boolean on, float yawOffset) {
        forced = on;
        if (on && !active) start();
        if (on) camYaw = mc.thePlayer.rotationYaw + yawOffset;
        if (!on && active) stop();
    }

    @Override
    protected void onDisable() {
        if (active) stop();
        toggled = false;
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (isEnabled() && mode.index() == 1 && key.isPressed()) toggled = !toggled;
    }

    private void start() {
        active = true;
        savedView = mc.gameSettings.thirdPersonView;
        mc.gameSettings.thirdPersonView = 1;
        camYaw = mc.thePlayer.rotationYaw + (front.get() ? 180f : 0f);
        camPitch = mc.thePlayer.rotationPitch;
    }

    private void stop() {
        active = false;
        mc.gameSettings.thirdPersonView = savedView;
        // A lista de pedaços do mundo ainda é a da câmera girada: refaz para a visão normal.
        redrawWorld();
        drawnYaw = Float.NaN;
    }

    /**
     * O jogo só refaz a lista de pedaços do mundo que aparecem na tela quando o personagem anda ou vira.
     * Na órbita o personagem não vira, então sem isto o que fica atrás dele aparecia preto até andar.
     */
    private void redrawWorld() {
        if (mc.renderGlobal != null) mc.renderGlobal.setDisplayListEntitiesDirty();
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (mc.thePlayer == null) {
            if (active) stop();
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            boolean want = forced || isEnabled() && (mode.index() == 0 ? mc.currentScreen == null && key.isKeyDown() : toggled);
            if (want && !active) start();
            else if (!want && active) stop();
            if (!active) return;
            yaw0 = mc.thePlayer.rotationYaw;
            pitch0 = mc.thePlayer.rotationPitch;
            prevYaw0 = mc.thePlayer.prevRotationYaw;
            prevPitch0 = mc.thePlayer.prevRotationPitch;
            captured = true;
        } else if (captured) {
            restorePlayer();
        }
    }

    private void restorePlayer() {
        mc.thePlayer.rotationYaw = yaw0;
        mc.thePlayer.rotationPitch = pitch0;
        mc.thePlayer.prevRotationYaw = prevYaw0;
        mc.thePlayer.prevRotationPitch = prevPitch0;
        captured = false;
    }

    @SubscribeEvent
    public void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (!active || event.entity != mc.thePlayer) return;
        if (captured) {
            // O que o mouse mexeu no personagem neste quadro vai para a câmera.
            float dYaw = mc.thePlayer.rotationYaw - yaw0;
            float dPitch = mc.thePlayer.rotationPitch - pitch0;
            camYaw += dYaw;
            camPitch = MathHelper.clamp_float(camPitch + (invert.get() ? -dPitch : dPitch), -90f, 90f);
            restorePlayer();
        }
        event.yaw = camYaw + 180f;
        event.pitch = camPitch;
        if (camYaw != drawnYaw || camPitch != drawnPitch) {
            redrawWorld();
            drawnYaw = camYaw;
            drawnPitch = camPitch;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLiving(RenderLivingEvent.Pre event) {
        // O jogo vira os nomes (e o Meu Nick) para onde o personagem olha; na órbita eles têm que virar para a câmera.
        if (!active) return;
        RenderManager rm = mc.getRenderManager();
        rm.playerViewY = camYaw;
        rm.playerViewX = camPitch;
    }
}
