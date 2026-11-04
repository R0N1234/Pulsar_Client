package dev.pulsarclient.mods;

import dev.pulsarclient.core.Ganchos;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.SliderSetting;

/**
 * Aura de Encanto: troca a cor roxa do brilho de encantamento dos itens (e da armadura, se quiser).
 * Entra pelos ganchos do jogo (core/GanchosTransformer), então não custa nada além do brilho que já existia.
 */
public class AuraEncanto extends PulsarMod implements Ganchos.Encanto {
    private final ColorSetting cor = add(new ColorSetting("cor", "Cor do brilho", ColorSetting.CIANO));
    private final BoolSetting armadura = add(new BoolSetting("armadura", "Também na armadura", true));
    private final BoolSetting arcoIris = add(new BoolSetting("arco_iris", "Arco-íris (a cor vai mudando)", false));
    private final SliderSetting velocidade = add(new SliderSetting("velocidade", "Velocidade do arco-íris", 5, 1, 10, 1, ""));

    public AuraEncanto() {
        super("aura_encanto", "Aura de Encanto", "Muda a cor do brilho de encantamento", Category.VISUAL, false);
        Ganchos.encanto = this;
    }

    @Override
    public int cor(boolean naArmadura) {
        if (!isEnabled() || naArmadura && !armadura.get()) return Ganchos.NAO;
        if (!arcoIris.get()) return cor.rgb();
        float h = (System.currentTimeMillis() % 100000L) / 1000f * (float) velocidade.get() * 0.04f;
        return ColorSetting.hsb(h - (float) Math.floor(h), 0.75f, 1f);
    }

    /** Ganchos instalados (teste automático e log). */
    public static String ganchos() {
        return "itens=" + Ganchos.ganchoEncantoItem + " armadura=" + Ganchos.ganchoEncantoArmadura;
    }
}
