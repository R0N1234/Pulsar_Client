package dev.pulsarclient.settings;

import com.google.gson.JsonElement;

/** Um botão no painel que executa algo na hora. */
public class ActionSetting extends Setting {
    private final Runnable action;

    public ActionSetting(String key, String label, Runnable action) {
        super(key, label);
        this.action = action;
    }

    public void run() { action.run(); }

    @Override public JsonElement save() { return null; }
    @Override public void load(JsonElement v) {}
}
