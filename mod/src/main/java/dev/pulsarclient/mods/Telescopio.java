package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.SliderSetting;
import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/** Telescópio: zoom segurando a tecla escolhida (C no começo), com a roda do mouse ajustando o quanto aproxima. */
public class Telescopio extends PulsarMod {
    public final KeyBinding key = new KeyBinding("Telescópio (zoom)", Keyboard.KEY_C, "Pulsar Client");
    private final TeclaDoJogo zoomKey = add(new TeclaDoJogo("tecla_zoom", "Tecla do zoom (segurar)", key));

    private final SliderSetting amount = add(new SliderSetting("zoom", "Aproximação", 4, 2, 12, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("suave", "Zoom suave", true));
    private final BoolSetting scroll = add(new BoolSetting("roda", "Roda do mouse ajusta", true));
    private final BoolSetting cinematic = add(new BoolSetting("cinema", "Câmera cinematográfica", false));

    private final MiraLenta mouse = new MiraLenta();
    private double target = 1;
    private double current = 1;
    private long lastNanos;
    private boolean savedSmooth;
    private boolean zooming;

    public Telescopio() {
        super("telescopio", "Telescópio", "Zoom segurando uma tecla, a roda do mouse ajusta", Category.VISUAL, true);
    }

    private boolean held() {
        return isEnabled() && mc.currentScreen == null && key.isKeyDown();
    }

    /** Quanto o movimento do mouse encolhe agora (1 = normal). Usado pelo teste automático. */
    public double mouseScale() {
        return mc.mouseHelper == mouse ? mouse.scale : 1;
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        boolean nowZooming = held();
        if (nowZooming && !zooming) {
            target = amount.get();
            savedSmooth = mc.gameSettings.smoothCamera;
            if (cinematic.get()) mc.gameSettings.smoothCamera = true;
        } else if (!nowZooming && zooming) {
            target = 1;
            mc.gameSettings.smoothCamera = savedSmooth;
        }
        zooming = nowZooming;
        // Suavização pelo tempo, para o zoom ter a mesma velocidade com 30 ou 300 FPS.
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        current = smooth.get() ? current + (target - current) * (1 - Math.exp(-dt * 14)) : target;
        if (Math.abs(current - target) < 0.01) current = target;
        // Mira mais lenta quanto maior o zoom, para não perder o alvo. A sensibilidade do jogo não muda mais:
        // antes ela era baixada em cada quadro e, se não voltasse, o mouse ficava lento mesmo sem zoom.
        // Agora é o movimento do mouse que encolhe, com a mesma sensação de antes.
        double scale = 1;
        if (current > 1.001) {
            float s = mc.gameSettings.mouseSensitivity;
            scale = Math.pow((s / Math.sqrt(current) * 0.6 + 0.2) / (s * 0.6 + 0.2), 3);
            // Entra na frente do mouse do jogo uma vez só. Se outro mod trocar o mouse depois, o zoom continua sem a mira lenta.
            if (mouse.inner == null) {
                mouse.inner = mc.mouseHelper;
                mc.mouseHelper = mouse;
            }
        }
        mouse.scale = scale;
    }

    /** Mouse do jogo com o movimento encolhido durante o zoom. Repassa tudo para o mouse que já existia. */
    private static class MiraLenta extends MouseHelper {
        private MouseHelper inner;
        private double scale = 1;
        private double restX;
        private double restY;

        @Override
        public void grabMouseCursor() {
            inner.grabMouseCursor();
            deltaX = 0;
            deltaY = 0;
        }

        @Override
        public void ungrabMouseCursor() {
            inner.ungrabMouseCursor();
        }

        @Override
        public void mouseXYChange() {
            inner.mouseXYChange();
            if (scale >= 0.999) {
                restX = 0;
                restY = 0;
                deltaX = inner.deltaX;
                deltaY = inner.deltaY;
                return;
            }
            // Guarda a fração que sobra, para movimentos pequenos não se perderem.
            restX += inner.deltaX * scale;
            restY += inner.deltaY * scale;
            deltaX = (int) restX;
            deltaY = (int) restY;
            restX -= deltaX;
            restY -= deltaY;
        }
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
