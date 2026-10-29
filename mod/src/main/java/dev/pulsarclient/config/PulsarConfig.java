package dev.pulsarclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pulsarclient.mods.HudMod;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.settings.Setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Lê e grava pulsar/config.json dentro da pasta do jogo.
 * O launcher escreve "enabled" de cada mod; o jogo escreve "x" e "y" da HUD e as opções em "settings".
 * Campos que o jogo não conhece são preservados.
 */
public class PulsarConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File file;

    public PulsarConfig(File gameDir) {
        this.file = new File(new File(gameDir, "pulsar"), "config.json");
    }

    private JsonObject read() {
        if (!file.isFile()) return new JsonObject();
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement el = new JsonParser().parse(r);
            return el != null && el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public void load(List<PulsarMod> mods) {
        JsonObject all = object(read(), "mods");
        for (PulsarMod mod : mods) {
            if (!all.has(mod.id()) || !all.get(mod.id()).isJsonObject()) continue;
            JsonObject m = all.getAsJsonObject(mod.id());
            try {
                if (m.has("enabled")) mod.setEnabled(m.get("enabled").getAsBoolean());
                if (mod instanceof HudMod) {
                    HudMod hud = (HudMod) mod;
                    if (m.has("x")) hud.x = m.get("x").getAsInt();
                    if (m.has("y")) hud.y = m.get("y").getAsInt();
                }
                JsonObject values = object(m, "settings");
                for (Setting s : mod.settings()) {
                    if (values.has(s.key())) s.load(values.get(s.key()));
                }
            } catch (Exception e) {
                System.err.println("[Pulsar] Config inválida para " + mod.id() + ": " + e);
            }
        }
    }

    public void save(List<PulsarMod> mods) {
        JsonObject root = read();
        JsonObject all = object(root, "mods");
        for (PulsarMod mod : mods) {
            JsonObject m = object(all, mod.id());
            m.addProperty("enabled", mod.isEnabled());
            if (mod instanceof HudMod) {
                m.addProperty("x", ((HudMod) mod).x);
                m.addProperty("y", ((HudMod) mod).y);
            }
            JsonObject values = object(m, "settings");
            for (Setting s : mod.settings()) {
                JsonElement v = s.save();
                if (v != null) values.add(s.key(), v);
            }
            if (values.entrySet().size() > 0) m.add("settings", values);
            all.add(mod.id(), m);
        }
        root.add("mods", all);
        file.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            System.err.println("[Pulsar] Não foi possível salvar " + file + ": " + e);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();
    }
}
