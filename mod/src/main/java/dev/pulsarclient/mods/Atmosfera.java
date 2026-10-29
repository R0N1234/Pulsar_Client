package dev.pulsarclient.mods;

import dev.pulsarclient.settings.ModeSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Atmosfera: tira a chuva e fixa a hora do dia só para você. Sem chuva também sobra FPS,
 * porque o jogo para de criar as partículas e os sons de chuva.
 */
public class Atmosfera extends PulsarMod {
    private static final long[] TIMES = {-1, 6000, 12500, 18000};
    private final ModeSetting time = add(new ModeSetting("hora", "Hora do dia", 0, "Do servidor", "Dia", "Pôr do sol", "Noite"));
    private final ModeSetting weather = add(new ModeSetting("tempo", "Chuva e neve", 1, "Do servidor", "Sempre limpo"));

    public Atmosfera() {
        super("atmosfera", "Atmosfera", "Tira a chuva e fixa a hora do dia só para você", Category.VISUAL, false);
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !isEnabled() || mc.theWorld == null) return;
        if (weather.index() == 1) {
            mc.theWorld.setRainStrength(0f);
            mc.theWorld.setThunderStrength(0f);
        }
        long t = TIMES[time.index()];
        if (t >= 0) mc.theWorld.setWorldTime(t);
    }
}
