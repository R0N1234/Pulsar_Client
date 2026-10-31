package dev.pulsarclient.mods;

import com.google.gson.JsonObject;
import dev.pulsarclient.Ponte;
import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Rádio Pulsar: rádios da internet tocando enquanto você joga. Ligar o mod liga o rádio.
 * Quem toca o som é o launcher (o Minecraft 1.8.9 não toca rádio da internet); o jogo só manda
 * a estação e o volume pela ponte. Na tela aparece a estação que está tocando.
 */
public class RadioPulsar extends HudMod {
    // Os endereços de cada estação ficam no launcher (launcher/lib/radio.js), pelo mesmo id.
    private static final String[] NOMES = {"Space Station", "Groove Salad", "DEF CON", "Fluid", "Nightride", "Chillsynth", "Radio Paradise", "Lofi"};
    private static final String[] IDS = {"space_station", "groove_salad", "def_con", "fluid", "nightride", "chillsynth", "radio_paradise", "lofi"};

    private final ModeSetting estacao = add(new ModeSetting("estacao", "Estação", 0, NOMES));
    private final SliderSetting volume = add(new SliderSetting("volume", "Volume", 40, 0, 100, 5, "%"));
    private final KeySetting proxima = add(new KeySetting("tecla_estacao", "Tecla para trocar de estação", 0));
    private final BoolSetting mostrar = add(new BoolSetting("mostrar", "Mostrar a estação na tela", true));

    private String enviado = "";
    private int ticks;

    public RadioPulsar() {
        super("radio_pulsar", "Rádio Pulsar", "Rádio da internet enquanto você joga", Category.SOCIAL, 10000, 10000, false);
    }

    private String texto() {
        return Ponte.disponivel() ? estacao.get() : "abra pelo launcher do Pulsar";
    }

    @Override public int width() { return labelWidth("Rádio", texto()); }
    @Override public int height() { return 8; }

    @Override
    public void render(boolean preview) {
        if (preview || mostrar.get()) drawLabel("Rádio", texto());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % 10 != 0) return;
        String estado = isEnabled() + "|" + estacao.index() + "|" + Math.round(volume.get());
        if (estado.equals(enviado)) return;
        // O primeiro aviso com o rádio desligado não precisa ir (o launcher começa calado).
        if (enviado.isEmpty() && !isEnabled()) {
            enviado = estado;
            return;
        }
        enviado = estado;
        JsonObject j = new JsonObject();
        j.addProperty("tocando", isEnabled());
        j.addProperty("estacao", IDS[estacao.index()]);
        j.addProperty("volume", (int) Math.round(volume.get()));
        Ponte.avisar("/radio", j);
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
        if (!isEnabled() || codigo == 0 || codigo != proxima.get() || mc.currentScreen != null || mc.thePlayer == null) return;
        estacao.next();
        mc.ingameGUI.setRecordPlaying("§fRádio Pulsar: §b" + estacao.get(), false);
        PulsarClient.get().save();
    }
}
