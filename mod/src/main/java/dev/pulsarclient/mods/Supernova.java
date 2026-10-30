package dev.pulsarclient.mods;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Supernova: brilho máximo, enxerga em cavernas e à noite sem tocha.
 * O brilho sobe só enquanto a tela é desenhada, então o brilho salvo nas opções do jogo nunca muda.
 */
public class Supernova extends PulsarMod {
    private static final float FULL = 100f;
    private float saved = -1;

    public Supernova() {
        super("supernova", "Supernova", "Brilho total, enxerga no escuro", Category.VISUAL, false);
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (!isEnabled() || mc.theWorld == null) return;
            saved = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = FULL;
        } else if (saved >= 0) {
            mc.gameSettings.gammaSetting = saved;
            saved = -1;
        }
    }
}
