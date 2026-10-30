package dev.pulsarclient.mods;

import dev.pulsarclient.core.Ganchos;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/** Impacto: escolhe a cor e a força do brilho de quando um jogador ou mob leva dano (no jogo é sempre vermelho). */
public class Impacto extends PulsarMod implements Ganchos.CorDano {
    private static final String[] NOMES = {"Vermelho", "Roxo Pulsar", "Ciano", "Laranja", "Verde", "Amarelo", "Branco", "Rosa"};
    private static final int[] CORES = {0xFF0000, 0x9B5CFF, 0x3FE0FF, 0xFF7A1A, 0x3CFF6E, 0xFFE03C, 0xFFFFFF, 0xFF4FD8};

    private final ModeSetting cor = add(new ModeSetting("cor", "Cor do dano", 1, NOMES));
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
        int c = CORES[cor.index()];
        rgba[0] = (c >> 16 & 255) / 255f;
        rgba[1] = (c >> 8 & 255) / 255f;
        rgba[2] = (c & 255) / 255f;
        rgba[3] = (float) forca.get() / 100f;
        return rgba;
    }
}
