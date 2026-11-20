package dev.pulsarclient.mods;

import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/**
 * Telescópio: o zoom do OptiFine. Segurando a tecla (C no começo) a visão aproxima 4 vezes na hora,
 * com a câmera cinematográfica ligada para a mira ficar suave; soltando, tudo volta como estava.
 *
 * A câmera cinematográfica é uma opção só do jogo (a do F8), e o zoom do OptiFine também mexe nela.
 * Antes o Telescópio guardava o valor no instante em que o zoom começava: se o zoom do OptiFine tinha
 * ligado a câmera um pouco antes (ele olha a tecla a cada quadro, o Minecraft só a cada tick), o valor
 * guardado já era "ligada" e, ao soltar, a câmera ficava lenta para sempre. Agora o Telescópio lembra
 * o que o jogador escolheu enquanto nenhum zoom está ativo e, ao soltar, volta para isso e confere por
 * mais um instante que nada ligou a câmera de novo.
 */
public class Telescopio extends PulsarMod {
    private static final float APROXIMA = 4f;
    /** Depois de soltar, por este tempo a câmera é conferida (um outro zoom pode devolver um valor velho). */
    private static final long CONFERE_MS = 500;

    public final KeyBinding key = new KeyBinding("Telescópio (zoom)", Keyboard.KEY_C, "Pulsar Client");
    private final TeclaDoJogo zoomKey = add(new TeclaDoJogo("tecla_zoom", "Tecla do zoom (segurar)", key));

    private boolean zooming;
    /** A câmera cinematográfica que o jogador escolheu (F8), vista com nenhum zoom ativo. */
    private boolean cinemaDoJogador;
    private boolean cinemaConhecida;
    private long conferirAte;
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
        tirarZoomDoOptifine(false);
        boolean agora = isEnabled() && mc.currentScreen == null && teclaSegurada();
        if (agora && !zooming) {
            tirarZoomDoOptifine(true);
            if (!cinemaConhecida) lembrarCinema();
            mc.gameSettings.smoothCamera = true;
        } else if (!agora && zooming) {
            voltarCinema();
        } else if (!zooming) {
            cuidarDepoisDoZoom();
        }
        zooming = agora;
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

    /** Fora do zoom: guarda a escolha do jogador e, logo depois de soltar, desfaz se algo religou a câmera. */
    private void cuidarDepoisDoZoom() {
        if (zoomDoOptifineSegurado()) return;
        boolean agora = mc.gameSettings.smoothCamera;
        if (System.currentTimeMillis() < conferirAte && agora != cinemaDoJogador) {
            if (teclaDoF8Segurada()) {
                lembrarCinema();
            } else {
                mc.gameSettings.smoothCamera = cinemaDoJogador;
                System.out.println("[Pulsar] Telescópio: câmera cinematográfica religada depois do zoom, voltando ao normal");
            }
            return;
        }
        lembrarCinema();
    }

    private void lembrarCinema() {
        cinemaDoJogador = mc.gameSettings.smoothCamera;
        cinemaConhecida = true;
    }

    private void voltarCinema() {
        mc.gameSettings.smoothCamera = cinemaConhecida && cinemaDoJogador;
        conferirAte = System.currentTimeMillis() + CONFERE_MS;
    }

    private boolean teclaDoF8Segurada() {
        KeyBinding f8 = mc.gameSettings.keyBindSmoothCamera;
        return f8 != null && f8.getKeyCode() != 0 && GameSettings.isKeyDown(f8);
    }

    @Override
    protected void onDisable() {
        if (zooming) voltarCinema();
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
