package dev.pulsarclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pulsarclient.mods.PulsarMod;

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
 * O launcher escreve "enabled" de cada mod; o jogo escreve "x" e "y" da HUD.
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
        JsonObject all = mods(read());
        for (PulsarMod mod : mods) {
            if (!all.has(mod.id()) || !all.get(mod.id()).isJsonObject()) continue;
            JsonObject m = all.getAsJsonObject(mod.id());
            if (m.has("enabled")) mod.setEnabled(m.get("enabled").getAsBoolean());
            if (m.has("x")) mod.x = m.get("x").getAsInt();
            if (m.has("y")) mod.y = m.get("y").getAsInt();
        }
    }

    public void save(List<PulsarMod> mods) {
        JsonObject root = read();
        JsonObject all = mods(root);
        for (PulsarMod mod : mods) {
            JsonObject m = all.has(mod.id()) && all.get(mod.id()).isJsonObject() ? all.getAsJsonObject(mod.id()) : new JsonObject();
            m.addProperty("enabled", mod.isEnabled());
            m.addProperty("x", mod.x);
            m.addProperty("y", mod.y);
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

    private static JsonObject mods(JsonObject root) {
        return root.has("mods") && root.get("mods").isJsonObject() ? root.getAsJsonObject("mods") : new JsonObject();
    }
}
