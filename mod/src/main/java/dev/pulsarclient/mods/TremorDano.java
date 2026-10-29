package dev.pulsarclient.mods;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Tremor de Dano: tira o balanço da câmera quando você leva dano.
 * O tempo de dano é zerado só enquanto a tela é desenhada e volta logo depois, então nada muda no jogo.
 */
public class TremorDano extends PulsarMod {
    private int saved = -1;

    public TremorDano() {
        super("tremor_dano", "Tremor de Dano", "Tira o tremor da câmera ao levar dano", Category.COMBATE, false);
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (mc.thePlayer == null) return;
        if (event.phase == TickEvent.Phase.START) {
            if (!isEnabled() || mc.thePlayer.hurtTime == 0) return;
            saved = mc.thePlayer.hurtTime;
            mc.thePlayer.hurtTime = 0;
        } else if (saved >= 0) {
            mc.thePlayer.hurtTime = saved;
            saved = -1;
        }
    }
}
