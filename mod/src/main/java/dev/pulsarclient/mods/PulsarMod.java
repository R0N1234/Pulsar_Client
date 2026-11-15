package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base de todo mod do Pulsar. O id é o mesmo usado pelo launcher (launcher/src/mods.js).
 * Mods que desenham na tela estendem {@link HudMod}.
 */
public abstract class PulsarMod {
    protected final Minecraft mc = Minecraft.getMinecraft();
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<Setting>();
    // Todo mod tem a sua: a pessoa escolhe a tecla no painel do mod (no começo, nenhuma).
    private final KeySetting toggleKey = add(new KeySetting("tecla_ligar", "Tecla para ligar e desligar", 0));
    // Estrela no painel: favoritos aparecem primeiro. Guardado como opção escondida.
    private final BoolSetting favorite = add(new BoolSetting("favorito", "Favorito", false) {
        @Override public boolean visible() { return false; }
    });
    // Mods que acabaram de chegar mostram "NOVO" no painel até a pessoa abrir as opções ou ligar o mod.
    private final BoolSetting visto = add(new BoolSetting("visto", "Visto", false) {
        @Override public boolean visible() { return false; }
    });
    private boolean enabled;

    protected PulsarMod(String id, String name, String description, Category category, boolean enabledByDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabledByDefault;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Category category() { return category; }
    public List<Setting> settings() { return Collections.unmodifiableList(settings); }

    /** Só as opções que aparecem no painel. */
    public List<Setting> panelSettings() {
        List<Setting> out = new ArrayList<Setting>();
        for (Setting s : settings) if (s.visible()) out.add(s);
        return out;
    }
    public boolean isEnabled() { return enabled; }
    public KeySetting toggleKey() { return toggleKey; }
    public boolean favorite() { return favorite.get(); }
    public void toggleFavorite() { favorite.toggle(); }

    /** Chegou nesta versão (lista em {@link dev.pulsarclient.PulsarClient#NOVOS}) e a pessoa ainda não mexeu nele. */
    public boolean novo() {
        return !visto.get() && dev.pulsarclient.PulsarClient.NOVOS.contains(id);
    }

    /** A pessoa abriu as opções ou ligou o mod no painel: a etiqueta NOVO some. */
    public void marcarVisto() { visto.set(true); }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    public void toggle() { setEnabled(!enabled); }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    /** Aviso curto que o painel mostra em cima das opções do mod (ex.: "Código copiado!"). null = nada. */
    public String aviso() { return null; }

    protected void onEnable() {}
    protected void onDisable() {}

    /** Chamado a cada tick do cliente, ligado ou não. */
    public void tick() {}

    /** Chamado uma vez quando o mod dá erro e é pausado: desfaz o que ele mudou no jogo. */
    public void onBroken() {}
}
