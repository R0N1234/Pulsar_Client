package dev.pulsarclient.mods;

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
    public boolean isEnabled() { return enabled; }

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

    protected void onEnable() {}
    protected void onDisable() {}

    /** Chamado a cada tick do cliente, ligado ou não. */
    public void tick() {}
}
