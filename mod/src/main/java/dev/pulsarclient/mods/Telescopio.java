package dev.pulsarclient.mods;

import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Telescópio: o zoom do OptiFine. Segurando a tecla (C no começo) a visão aproxima 4 vezes na hora,
 * com a câmera cinematográfica ligada para a mira ficar suave; soltando, tudo volta como estava.
 */
public class Telescopio extends PulsarMod {
    private static final float APROXIMA = 4f;

    public final KeyBinding key = new KeyBinding("Telescópio (zoom)", Keyboard.KEY_C, "Pulsar Client");
    private final TeclaDoJogo zoomKey = add(new TeclaDoJogo("tecla_zoom", "Tecla do zoom (segurar)", key));

    private boolean zooming;
    private boolean savedSmooth;
    private int teclaConferida = -1;

    public Telescopio() {
        super("telescopio", "Telescópio", "Zoom igual ao do OptiFine, segurando uma tecla", Category.VISUAL, true);
    }

    /** O zoom está ativo agora. Usado pelo teste automático. */
    public boolean zoomAtivo() {
        return zooming;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.gameSettings == null) return;
        tirarZoomDoOptifine();
        boolean agora = isEnabled() && mc.currentScreen == null && key.isKeyDown();
        if (agora && !zooming) {
            savedSmooth = mc.gameSettings.smoothCamera;
            mc.gameSettings.smoothCamera = true;
        } else if (!agora && zooming) {
            mc.gameSettings.smoothCamera = savedSmooth;
        }
        zooming = agora;
    }

    @Override
    protected void onDisable() {
        if (zooming) mc.gameSettings.smoothCamera = savedSmooth;
        zooming = false;
    }

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (zooming) event.setFOV(event.getFOV() / APROXIMA);
    }

    /** Com OptiFine junto, o zoom dele usa a mesma tecla C: os dois somados davam 16x. O dele sai da tecla. */
    private void tirarZoomDoOptifine() {
        int nossa = key.getKeyCode();
        if (nossa == teclaConferida) return;
        teclaConferida = nossa;
        if (nossa == 0 || mc.gameSettings.keyBindings == null) return;
        for (KeyBinding k : mc.gameSettings.keyBindings) {
            if (k != key && "of.key.zoom".equals(k.getKeyDescription()) && k.getKeyCode() == nossa) {
                k.setKeyCode(0);
                KeyBinding.resetKeyBindingArrayAndHash();
                System.out.println("[Pulsar] Telescópio: zoom do OptiFine tirado da tecla " + Keyboard.getKeyName(nossa));
            }
        }
    }
}
