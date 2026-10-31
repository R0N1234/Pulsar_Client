package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;

/**
 * Nível: o nível de quem usa o Pulsar ao lado do nome. Sobe com o tempo jogando com o Pulsar
 * (15 min → 2, 1 h → 3, 4 h → 5, 15 h → 8, 100 h → 21). O seu aparece na tela de Amigos.
 */
public class Nivel extends PulsarMod {
    private final BoolSetting cabeca = add(new BoolSetting("na_cabeca", "Nível em cima da cabeça", true));
    private final BoolSetting tab = add(new BoolSetting("no_tab", "Nível no TAB", true));

    public Nivel() {
        super("nivel", "Nível", "Mostra o nível do jogador no nome", Category.SOCIAL, true);
    }

    public boolean naCabeca() { return isEnabled() && cabeca.get(); }
    public boolean noTab() { return isEnabled() && tab.get(); }
}
