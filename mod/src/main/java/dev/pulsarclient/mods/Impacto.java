package dev.pulsarclient.mods;

import dev.pulsarclient.core.Ganchos;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/** Impacto: escolhe a cor e a força do brilho de quando um jogador ou mob leva dano (no jogo é sempre vermelho). */
public class Impacto extends PulsarMod implements Ganchos.CorDano {
    private final ColorSetting cor = add(new ColorSetting("cor", "Cor do dano", 0x9B5CFF));
    private final SliderSetting forca = add(new SliderSetting("intensidade", "Intensidade", 30, 10, 70, 5, "%"));
    private final BoolSetting soJogadores = add(new BoolSetting("so_jogadores", "Só em jogadores", false));
    private final float[] rgba = new float[4];

    public Impacto() {
        super("impacto", "Impacto", "Muda a cor do inimigo quando ele leva dano", Category.COMBATE, false);
        Ganchos.corDano = this;
    }

    @Override
    public float[] cor(EntityLivingBase entidade) {
        if (!isEnabled() || soJogadores.get() && !(entidade instanceof EntityPlayer)) return null;
        int c = cor.rgb();
        rgba[0] = (c >> 16 & 255) / 255f;
        rgba[1] = (c >> 8 & 255) / 255f;
        rgba[2] = (c & 255) / 255f;
        rgba[3] = (float) forca.get() / 100f;
        return rgba;
    }
}
