package dev.pulsarclient.mods;

import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/**
 * Telescópio: o zoom do OptiFine. Segurando a tecla (C no começo) a visão aproxima 4 vezes na hora,
 * com a câmera cinematográfica ligada para a mira ficar suave; soltando, tudo volta como estava.
 *
 * A câmera cinematográfica (a do F8) só fica ligada enquanto um zoom está ativo, o nosso ou o do OptiFine.
 * Fora do zoom ela é sempre desligada: antes, com o zoom do OptiFine na mesma tecla, ela podia ficar ligada
 * depois de soltar e a câmera continuava lenta. O atalho "Alternar Câmera Cinematográfica" também sai de
 * Opções > Controles, para ninguém ligar a câmera lenta sem querer.
 */
public class Telescopio extends PulsarMod {
    private static final float APROXIMA = 4f;

    public final KeyBinding key = new KeyBinding("Telescópio (zoom)", Keyboard.KEY_C, "Pulsar Client");
    private final TeclaDoJogo zoomKey = add(new TeclaDoJogo("tecla_zoom", "Tecla do zoom (segurar)", key));

    private boolean zooming;
    private boolean atalhoEscondido;
    private boolean segurandoTeste;
    private int teclaConferida = -1;

    public Telescopio() {
        super("telescopio", "Telescópio", "Zoom igual ao do OptiFine, segurando uma tecla", Category.VISUAL, false);
    }

    /** O zoom está ativo agora. Usado pelo teste automático. */
    public boolean zoomAtivo() {
        return zooming;
    }

    /** Teste automático: segura ou solta a tecla do zoom sem teclado de verdade. */
    public void segurarParaTeste(boolean segurando) {
        segurandoTeste = segurando;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || mc.gameSettings == null) return;
        esconderAtalhoDaCamera();
        tirarZoomDoOptifine(false);
        boolean agora = isEnabled() && mc.currentScreen == null && teclaSegurada();
        if (agora && !zooming) tirarZoomDoOptifine(true);
        zooming = agora;
        if (zooming) mc.gameSettings.smoothCamera = true;
        else if (mc.gameSettings.smoothCamera && !zoomDoOptifineSegurado()) mc.gameSettings.smoothCamera = false;
    }

    /**
     * A tecla é lida direto do teclado (ou mouse), igual ao OptiFine, e não pelo estado do Minecraft,
     * que só muda a cada tick: assim o zoom começa e termina no mesmo quadro que o do OptiFine.
     * Sem a janela em foco, a tecla conta como solta.
     */
    private boolean teclaSegurada() {
        if (segurandoTeste) return true;
        return key.getKeyCode() != 0 && Display.isActive() && GameSettings.isKeyDown(key);
    }

    @Override
    protected void onDisable() {
        if (zooming) mc.gameSettings.smoothCamera = false;
        zooming = false;
    }

    @Override
    public void onBroken() {
        onDisable();
    }

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier event) {
        if (zooming) event.setFOV(event.getFOV() / APROXIMA);
    }

    /** Tira "Alternar Câmera Cinematográfica" de Opções > Controles e da tecla (o jogo continua funcionando sem ele). */
    private void esconderAtalhoDaCamera() {
        if (atalhoEscondido || mc.gameSettings.keyBindings == null) return;
        atalhoEscondido = true;
        KeyBinding cinema = mc.gameSettings.keyBindSmoothCamera;
        if (cinema == null) return;
        cinema.setKeyCode(0);
        mc.gameSettings.keyBindings = ArrayUtils.removeElement(mc.gameSettings.keyBindings, cinema);
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    /** O zoom do OptiFine, se ele estiver no jogo e ainda tiver tecla. */
    private KeyBinding zoomDoOptifine() {
        if (mc.gameSettings.keyBindings == null) return null;
        for (KeyBinding k : mc.gameSettings.keyBindings) {
            if (k != null && k != key && "of.key.zoom".equals(k.getKeyDescription())) return k;
        }
        return null;
    }

    private boolean zoomDoOptifineSegurado() {
        KeyBinding of = zoomDoOptifine();
        return of != null && of.getKeyCode() != 0 && Display.isActive() && GameSettings.isKeyDown(of);
    }

    /**
     * Com OptiFine junto, o zoom dele usa a mesma tecla C: os dois somados davam 16x e um desfazia a câmera
     * do outro. O dele sai da tecla. Confere quando a nossa tecla muda e de novo a cada zoom, porque a tecla
     * do OptiFine pode voltar para a mesma (em Controles, ou pelo options.txt).
     */
    private void tirarZoomDoOptifine(boolean sempre) {
        int nossa = key.getKeyCode();
        // Com o Telescópio desligado, a tecla C fica com o zoom do OptiFine.
        if (!isEnabled()) {
            teclaConferida = -1;
            return;
        }
        if (!sempre && nossa == teclaConferida) return;
        teclaConferida = nossa;
        if (nossa == 0) return;
        KeyBinding of = zoomDoOptifine();
        if (of != null && of.getKeyCode() == nossa) {
            of.setKeyCode(0);
            KeyBinding.resetKeyBindingArrayAndHash();
            System.out.println("[Pulsar] Telescópio: zoom do OptiFine tirado da tecla " + GameSettings.getKeyDisplayString(nossa));
        }
    }
}
