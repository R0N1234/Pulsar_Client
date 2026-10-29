package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Telescópio: zoom segurando C, com a roda do mouse ajustando o quanto aproxima. */
public class Telescopio extends PulsarMod {
    public final KeyBinding key = new KeyBinding("Telescópio (zoom)", Keyboard.KEY_C, "Pulsar Client");

    private final SliderSetting amount = add(new SliderSetting("zoom", "Aproximação", 4, 2, 12, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("suave", "Zoom suave", true));
    private final BoolSetting scroll = add(new BoolSetting("roda", "Roda do mouse ajusta", true));
    private final BoolSetting cinematic = add(new BoolSetting("cinema", "Câmera cinematográfica", false));

    private double target = 1;
    private double current = 1;
    private float savedSensitivity = -1;
    private boolean savedSmooth;
    private boolean zooming;

    public Telescopio() {
        super("telescopio", "Telescópio", "Zoom segurando C", Category.VISUAL, true);
    }

    private boolean held() {
        return isEnabled() && mc.currentScreen == null && key.isKeyDown();
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        boolean nowZooming = held();
        if (nowZooming && !zooming) {
            target = amount.get();
            savedSensitivity = mc.gameSettings.mouseSensitivity;
            savedSmooth = mc.gameSettings.smoothCamera;
            if (cinematic.get()) mc.gameSettings.smoothCamera = true;
        } else if (!nowZooming && zooming) {
            target = 1;
            if (savedSensitivity >= 0) mc.gameSettings.mouseSensitivity = savedSensitivity;
            mc.gameSettings.smoothCamera = savedSmooth;
            savedSensitivity = -1;
        }
        zooming = nowZooming;
        current = smooth.get() ? current + (target - current) * 0.25 : target;
        if (Math.abs(current - target) < 0.01) current = target;
        // Mira mais lenta quanto maior o zoom, para não perder o alvo.
        if (zooming && savedSensitivity >= 0) mc.gameSettings.mouseSensitivity = (float) (savedSensitivity / Math.sqrt(current));
    }

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (current > 1.001) event.setFOV((float) (event.getFOV() / current));
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!zooming || !scroll.get() || event.dwheel == 0) return;
        target = Math.max(1.5, Math.min(30, target * (event.dwheel > 0 ? 1.25 : 0.8)));
        event.setCanceled(true);
    }
}
