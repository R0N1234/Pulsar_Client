package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;

/**
 * Ícone Pulsar: o ícone PC aparece ao lado do nome de quem está usando o Pulsar, em cima da cabeça e no TAB.
 * "Aparecer para os outros" desligado: você continua vendo os ícones, mas o seu some para os outros.
 */
public class IconePulsar extends PulsarMod {
    private final BoolSetting cabeca = add(new BoolSetting("na_cabeca", "Ícone em cima da cabeça", true));
    private final BoolSetting tab = add(new BoolSetting("no_tab", "Ícone no TAB", true));
    private final BoolSetting aparecer = add(new BoolSetting("aparecer", "Aparecer para os outros", true));

    public IconePulsar() {
        super("icone_pulsar", "Ícone Pulsar", "Ícone PC no nome de quem usa o Pulsar", Category.SOCIAL, true);
    }

    public boolean naCabeca() { return isEnabled() && cabeca.get(); }
    public boolean noTab() { return isEnabled() && tab.get(); }
    public boolean aparecer() { return aparecer.get(); }
}
