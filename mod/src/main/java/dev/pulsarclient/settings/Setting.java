package dev.pulsarclient.settings;

import com.google.gson.JsonElement;

/** Uma opção de um mod, mostrada no painel do Pulsar e salva em config.json. */
public abstract class Setting {
    private final String key;
    private final String label;

    protected Setting(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() { return key; }
    public String label() { return label; }

    /** false para dados guardados pelo mod que não aparecem no painel (ex.: slots travados). */
    public boolean visible() { return true; }

    /** null para opções que não são salvas (botões de ação). */
    public abstract JsonElement save();
    public abstract void load(JsonElement value);
}
