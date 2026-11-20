package dev.pulsarclient.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.pulsarclient.mods.HudMod;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A parte "tudo" do perfil compartilhado do Motor Pulsar: quais mods estão ligados, a posição de cada
 * HUD, as opções de cada mod (cores, teclas, textos) e as opções gerais do jogo (FOV, brilho,
 * sensibilidade, escala da interface e as teclas do Minecraft).
 *
 * Para o código caber numa mensagem do Discord, só vai o que a pessoa mudou em relação ao padrão de
 * fábrica. Quem recebe volta para o padrão tudo o que não veio no código, então fica igual a quem mandou.
 *
 * Nada de conta vai no perfil: as contas ficam no launcher e não passam por aqui. Atalhos de Texto
 * que parecem login ("/login senha", "/register ...") também ficam de fora.
 */
public final class PerfilCompleto {
    /** Opções de cada mod que são da pessoa e não vão no perfil (estrela de favorito e a etiqueta NOVO). */
    private static final Set<String> FORA = new HashSet<String>(Arrays.asList("favorito", "visto"));
    /** Começos de comandos de login e senha: atalhos assim não vão no perfil. */
    private static final String[] LOGIN = {"/login", "/logar", "/l ", "/log ", "/register", "/registrar", "/reg ",
            "/senha", "/changepassword", "/changepass", "/trocarsenha", "/mudarsenha", "/2fa", "/auth"};

    /** Padrão de fábrica de cada mod, guardado antes de ler o config.json. */
    private static final Map<String, JsonObject> PADROES = new HashMap<String, JsonObject>();

    private PerfilCompleto() {}

    /** Chamado uma vez, com os mods recém-criados (antes do config.json): guarda o padrão de fábrica. */
    public static void guardarPadroes(List<PulsarMod> mods) {
        PADROES.clear();
        for (PulsarMod mod : mods) {
            try {
                PADROES.put(mod.id(), estado(mod, false));
            } catch (Exception e) {
                System.err.println("[Pulsar] Perfil: padrão de " + mod.id() + " não foi lido: " + e);
            }
        }
    }

    /** O estado completo de um mod: ligado, posição da HUD e todas as opções. */
    private static JsonObject estado(PulsarMod mod, boolean semOpcoes) {
        JsonObject o = new JsonObject();
        o.addProperty("ligado", mod.isEnabled());
        if (mod instanceof HudMod) {
            o.addProperty("x", ((HudMod) mod).x);
            o.addProperty("y", ((HudMod) mod).y);
        }
        if (semOpcoes) return o;
        JsonObject opcoes = new JsonObject();
        for (Setting s : mod.settings()) {
            if (FORA.contains(s.key()) || s instanceof ActionSetting) continue;
            JsonElement v = s.save();
            if (v == null) continue;
            if ("atalhos_texto".equals(mod.id()) && "atalhos".equals(s.key())) v = semLogin(v);
            opcoes.add(s.key(), v);
        }
        o.add("opcoes", opcoes);
        return o;
    }

    /** Tira da lista de Atalhos de Texto os que mandam login ou senha. */
    static JsonElement semLogin(JsonElement lista) {
        if (!lista.isJsonArray()) return lista;
        JsonArray out = new JsonArray();
        for (JsonElement e : lista.getAsJsonArray()) {
            if (e.isJsonObject() && e.getAsJsonObject().has("texto") && pareceLogin(e.getAsJsonObject().get("texto").getAsString())) continue;
            out.add(e);
        }
        return out;
    }

    public static boolean pareceLogin(String texto) {
        String t = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT) + " ";
        for (String p : LOGIN) {
            if (p.endsWith(" ") ? t.startsWith(p) : t.startsWith(p + " ") || t.startsWith(p + "\t")) return true;
        }
        return false;
    }

    /**
     * Os mods de agora, só com o que difere do padrão. O Motor Pulsar vai só com "ligado": as opções
     * dele já estão no bloco "motor" do perfil (que versões antigas do Pulsar também entendem).
     */
    public static JsonObject exportarMods(List<PulsarMod> mods, String idMotor) {
        JsonObject todos = new JsonObject();
        for (PulsarMod mod : mods) {
            try {
                boolean motor = mod.id().equals(idMotor);
                JsonObject agora = estado(mod, motor);
                JsonObject padrao = PADROES.get(mod.id());
                JsonObject dif = new JsonObject();
                dif.addProperty("ligado", mod.isEnabled());
                if (padrao == null) {
                    dif = agora;
                } else {
                    for (String k : new String[]{"x", "y"}) {
                        if (agora.has(k) && !agora.get(k).equals(padrao.get(k))) dif.add(k, agora.get(k));
                    }
                    if (agora.has("opcoes")) {
                        JsonObject op = new JsonObject();
                        JsonObject padraoOp = padrao.has("opcoes") ? padrao.getAsJsonObject("opcoes") : new JsonObject();
                        for (Map.Entry<String, JsonElement> e : agora.getAsJsonObject("opcoes").entrySet()) {
                            if (!e.getValue().equals(padraoOp.get(e.getKey()))) op.add(e.getKey(), e.getValue());
                        }
                        if (op.entrySet().size() > 0) dif.add("opcoes", op);
                    }
                }
                todos.add(mod.id(), dif);
            } catch (Exception e) {
                System.err.println("[Pulsar] Perfil: " + mod.id() + " ficou de fora: " + e);
            }
        }
        return todos;
    }

    /**
     * Aplica os mods de um perfil. Mod que veio no código fica igual ao de quem mandou (o que não veio
     * volta ao padrão). Mod que não veio (versão diferente do Pulsar) fica como está.
     */
    public static void aplicarMods(List<PulsarMod> mods, JsonObject todos, String idMotor) {
        for (PulsarMod mod : mods) {
            if (!todos.has(mod.id()) || !todos.get(mod.id()).isJsonObject()) continue;
            JsonObject m = todos.getAsJsonObject(mod.id());
            JsonObject padrao = PADROES.containsKey(mod.id()) ? PADROES.get(mod.id()) : new JsonObject();
            try {
                if (mod instanceof HudMod) {
                    HudMod hud = (HudMod) mod;
                    JsonElement x = m.has("x") ? m.get("x") : padrao.get("x");
                    JsonElement y = m.has("y") ? m.get("y") : padrao.get("y");
                    if (x != null) hud.x = x.getAsInt();
                    if (y != null) hud.y = y.getAsInt();
                }
                if (!mod.id().equals(idMotor)) {
                    JsonObject op = m.has("opcoes") && m.get("opcoes").isJsonObject() ? m.getAsJsonObject("opcoes") : new JsonObject();
                    JsonObject padraoOp = padrao.has("opcoes") ? padrao.getAsJsonObject("opcoes") : new JsonObject();
                    for (Setting s : mod.settings()) {
                        if (FORA.contains(s.key()) || s instanceof ActionSetting) continue;
                        JsonElement v = op.has(s.key()) ? op.get(s.key()) : padraoOp.get(s.key());
                        if (v == null) continue;
                        if ("atalhos_texto".equals(mod.id()) && "atalhos".equals(s.key())) v = semLogin(v);
                        try {
                            s.load(v);
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (m.has("ligado")) mod.setEnabled(m.get("ligado").getAsBoolean());
            } catch (Exception e) {
                System.err.println("[Pulsar] Perfil: " + mod.id() + " não foi aplicado: " + e);
            }
        }
    }

    /** Opções gerais do jogo que não são gráficos: FOV, brilho, sensibilidade, interface e teclas mudadas. */
    public static void exportarGeral(GameSettings gs, JsonObject jogo) {
        jogo.addProperty("fov", gs.fovSetting);
        jogo.addProperty("brilho", gs.gammaSetting);
        jogo.addProperty("sensibilidade", gs.mouseSensitivity);
        jogo.addProperty("escala_interface", gs.guiScale);
        jogo.addProperty("balanco_camera", gs.viewBobbing);
        JsonObject teclas = new JsonObject();
        for (KeyBinding kb : gs.keyBindings) {
            if (kb.getKeyCode() != kb.getKeyCodeDefault()) teclas.addProperty(kb.getKeyDescription(), kb.getKeyCode());
        }
        jogo.add("teclas", teclas);
    }

    public static void aplicarGeral(Minecraft mc, JsonObject jogo) {
        GameSettings gs = mc.gameSettings;
        try {
            if (jogo.has("fov")) gs.fovSetting = Math.max(30f, Math.min(110f, jogo.get("fov").getAsFloat()));
            if (jogo.has("brilho")) gs.gammaSetting = Math.max(0f, Math.min(1f, jogo.get("brilho").getAsFloat()));
            if (jogo.has("sensibilidade")) gs.mouseSensitivity = Math.max(0f, Math.min(1f, jogo.get("sensibilidade").getAsFloat()));
            if (jogo.has("balanco_camera")) gs.viewBobbing = jogo.get("balanco_camera").getAsBoolean();
            if (jogo.has("escala_interface")) {
                int escala = Math.max(0, Math.min(3, jogo.get("escala_interface").getAsInt()));
                if (gs.guiScale != escala) {
                    gs.guiScale = escala;
                    if (mc.currentScreen != null) {
                        ScaledResolution res = new ScaledResolution(mc);
                        mc.currentScreen.setWorldAndResolution(mc, res.getScaledWidth(), res.getScaledHeight());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Pulsar] Perfil: parte das opções do jogo não foi lida: " + e);
        }
        if (jogo.has("teclas") && jogo.get("teclas").isJsonObject()) {
            JsonObject teclas = jogo.getAsJsonObject("teclas");
            for (KeyBinding kb : gs.keyBindings) {
                try {
                    int codigo = teclas.has(kb.getKeyDescription()) ? teclas.get(kb.getKeyDescription()).getAsInt() : kb.getKeyCodeDefault();
                    if (codigo >= -100 && codigo < 256) kb.setKeyCode(codigo);
                } catch (Exception ignored) {
                }
            }
            KeyBinding.resetKeyBindingArrayAndHash();
        }
    }
}
