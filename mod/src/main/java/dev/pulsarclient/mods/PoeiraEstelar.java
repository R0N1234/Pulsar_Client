package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;

/** Poeira Estelar: mais partículas de acerto a cada hit confirmado, para sentir melhor o combo. */
public class PoeiraEstelar extends PulsarMod {
    private final SliderSetting multiplier = add(new SliderSetting("vezes", "Partículas extras por hit", 2, 1, 5, 1, "x"));
    private final BoolSetting sharp = add(new BoolSetting("afiado", "Sempre mostrar as de encantamento", true));
    private final BoolSetting crit = add(new BoolSetting("critico", "Sempre mostrar as de crítico", true));

    public PoeiraEstelar() {
        super("poeira_estelar", "Poeira Estelar", "Multiplica as partículas de hit", Category.VISUAL, false);
    }

    /** Chamado pelo CombatTracker quando o servidor confirma um hit seu. */
    public void onHit(Entity target) {
        if (!isEnabled() || mc.effectRenderer == null) return;
        for (int i = 0; i < multiplier.getInt(); i++) {
            if (crit.get()) mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT);
            if (sharp.get()) mc.effectRenderer.emitParticleAtEntity(target, EnumParticleTypes.CRIT_MAGIC);
        }
    }
}
