package dev.pulsarclient.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.Setting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Atalhos de Texto: cada tecla escolhida manda uma mensagem ou um comando no servidor
 * (por exemplo, F6 manda "/lobby"). Funciona com teclas do teclado e com os botões laterais do mouse.
 * Não tem limite: o painel tem um botão para adicionar atalhos e um para remover cada um.
 */
public class AtalhosTexto extends PulsarMod {
    private static final long ESPERA = 1000;
    // Versões até a 0.6 tinham 6 atalhos fixos, salvos como tecla_1..6 e texto_1..6.
    private static final int ANTIGOS = 6;

    /** Um atalho: a tecla, o texto e o botão de remover, com os nomes de acordo com a posição na lista. */
    private final class Atalho {
        final KeySetting tecla = new KeySetting("tecla", "", 0) {
            @Override public String label() { return "Atalho " + numero() + ": tecla"; }
        };
        final TextSetting texto = new TextSetting("texto", "", "", 100) {
            @Override public String label() { return "Atalho " + numero() + ": texto"; }
        };
        final ActionSetting remover = new ActionSetting("remover", "", new Runnable() {
            @Override public void run() { atalhos.remove(Atalho.this); }
        }) {
            @Override public String label() { return "Remover o atalho " + numero(); }
        };
        long ultimo;

        Atalho(int codigo, String t) {
            tecla.set(codigo);
            texto.set(t);
        }

        int numero() { return atalhos.indexOf(this) + 1; }
    }

    private final List<Atalho> atalhos = new ArrayList<Atalho>();
    private final ActionSetting adicionar = new ActionSetting("adicionar", "+ Adicionar atalho", new Runnable() {
        @Override public void run() { atalhos.add(new Atalho(0, "")); }
    });
    private boolean carregou;
    private final int[] teclasAntigas = new int[ANTIGOS];
    private final String[] textosAntigos = new String[ANTIGOS];
    private boolean temAntigos;

    public AtalhosTexto() {
        super("atalhos_texto", "Atalhos de Texto", "Uma tecla manda uma mensagem ou um comando no servidor", Category.SERVIDORES, true);
        atalhos.add(new Atalho(0, "/lobby"));
        atalhos.add(new Atalho(0, "gg"));
        add(new Lista());
        for (int i = 0; i < ANTIGOS; i++) {
            final int n = i;
            add(new Antigo("tecla_" + (i + 1)) {
                @Override public void load(JsonElement v) {
                    if (v.isJsonPrimitive()) teclasAntigas[n] = v.getAsInt();
                    temAntigos = true;
                }
            });
            add(new Antigo("texto_" + (i + 1)) {
                @Override public void load(JsonElement v) {
                    if (v.isJsonPrimitive()) textosAntigos[n] = v.getAsString();
                    temAntigos = true;
                }
            });
        }
    }

    /** Os atalhos guardados em config.json, como uma lista de {tecla, texto}. */
    private final class Lista extends Setting {
        Lista() { super("atalhos", "atalhos"); }

        @Override public boolean visible() { return false; }

        @Override
        public JsonElement save() {
            migrar();
            JsonArray lista = new JsonArray();
            for (Atalho a : atalhos) {
                JsonObject o = new JsonObject();
                o.add("tecla", new JsonPrimitive(a.tecla.get()));
                o.add("texto", new JsonPrimitive(a.texto.get()));
                lista.add(o);
            }
            return lista;
        }

        @Override
        public void load(JsonElement v) {
            if (!v.isJsonArray()) return;
            atalhos.clear();
            for (JsonElement e : v.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                int codigo = o.has("tecla") ? o.get("tecla").getAsInt() : 0;
                String t = o.has("texto") ? o.get("texto").getAsString() : "";
                atalhos.add(new Atalho(codigo, t));
            }
            carregou = true;
        }
    }

    /** Lê os atalhos fixos das versões antigas; não salva nada. */
    private abstract static class Antigo extends Setting {
        Antigo(String key) { super(key, key); }
        @Override public boolean visible() { return false; }
        @Override public JsonElement save() { return null; }
    }

    /** Config antiga sem a lista: os 6 atalhos fixos viram os primeiros da lista. */
    private void migrar() {
        if (carregou || !temAntigos) return;
        carregou = true;
        atalhos.clear();
        for (int i = 0; i < ANTIGOS; i++) {
            String t = textosAntigos[i] == null ? (i == 0 ? "/lobby" : i == 1 ? "gg" : "") : textosAntigos[i];
            if (teclasAntigas[i] != 0 || !t.trim().isEmpty()) atalhos.add(new Atalho(teclasAntigas[i], t));
        }
    }

    @Override
    public List<Setting> panelSettings() {
        migrar();
        List<Setting> out = super.panelSettings();
        for (Atalho a : atalhos) {
            out.add(a.tecla);
            out.add(a.texto);
            out.add(a.remover);
        }
        out.add(adicionar);
        return out;
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) apertou(Keyboard.getEventKey());
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() >= 0) apertou(Mouse.getEventButton() - 100);
    }

    private void apertou(int codigo) {
        if (!isEnabled() || codigo == 0 || mc.currentScreen != null || mc.thePlayer == null) return;
        migrar();
        long agora = System.currentTimeMillis();
        for (Atalho a : new ArrayList<Atalho>(atalhos)) {
            String texto = a.texto.get().trim();
            if (a.tecla.get() != codigo || texto.isEmpty()) continue;
            // Segurar a tecla não vira spam: no máximo uma mensagem por segundo em cada atalho.
            if (agora - a.ultimo < ESPERA) continue;
            a.ultimo = agora;
            mandar(texto);
        }
    }

    private void mandar(String texto) {
        mc.ingameGUI.getChatGUI().addToSentMessages(texto);
        // Comandos de outros mods (que rodam só no seu jogo) vão para o Forge, como no chat normal.
        if (texto.startsWith("/") && ClientCommandHandler.instance.executeCommand(mc.thePlayer, texto) != 0) return;
        mc.thePlayer.sendChatMessage(texto);
    }
}
