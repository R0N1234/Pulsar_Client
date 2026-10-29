package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Mira de Pulsar: troca a mira do jogo por uma configurável. */
public class MiraPulsar extends PulsarMod {
    private final ModeSetting shape = add(new ModeSetting("forma", "Formato", 0, "Cruz", "Cruz + ponto", "Ponto", "Quadrado"));
    private final ColorSetting color = add(new ColorSetting("cor", "Cor", 0));
    private final SliderSetting size = add(new SliderSetting("tamanho", "Tamanho", 5, 1, 12, 1, ""));
    private final SliderSetting gap = add(new SliderSetting("espaco", "Espaço no meio", 2, 0, 8, 1, ""));
    private final SliderSetting thick = add(new SliderSetting("espessura", "Espessura", 1, 1, 3, 1, ""));
    private final BoolSetting outline = add(new BoolSetting("contorno", "Contorno preto", true));

    public MiraPulsar() {
        super("mira_pulsar", "Mira de Pulsar", "Mira com formato, cor e tamanho", Category.COMBATE, false);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (!isEnabled() || event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (mc.gameSettings.thirdPersonView != 0) return;
        event.setCanceled(true);
        int cx = event.resolution.getScaledWidth() / 2;
        int cy = event.resolution.getScaledHeight() / 2;
        int s = size.getInt();
        int g = gap.getInt();
        int t = thick.getInt();
        int c = color.argb(0xFF);
        String mode = shape.get();
        if (mode.startsWith("Cruz")) {
            bar(cx - g - s, cy - t / 2, cx - g, cy - t / 2 + t, c);
            bar(cx + g + 1, cy - t / 2, cx + g + 1 + s, cy - t / 2 + t, c);
            bar(cx - t / 2, cy - g - s, cx - t / 2 + t, cy - g, c);
            bar(cx - t / 2, cy + g + 1, cx - t / 2 + t, cy + g + 1 + s, c);
        }
        if (mode.equals("Cruz + ponto") || mode.equals("Ponto")) bar(cx - t / 2, cy - t / 2, cx - t / 2 + t, cy - t / 2 + t, c);
        if (mode.equals("Quadrado")) {
            bar(cx - s, cy - s, cx + s + 1, cy - s + t, c);
            bar(cx - s, cy + s + 1 - t, cx + s + 1, cy + s + 1, c);
            bar(cx - s, cy - s, cx - s + t, cy + s + 1, c);
            bar(cx + s + 1 - t, cy - s, cx + s + 1, cy + s + 1, c);
        }
    }

    private void bar(int l, int t, int r, int b, int c) {
        if (outline.get()) Gui.drawRect(l - 1, t - 1, r + 1, b + 1, 0xAA000000);
        Gui.drawRect(l, t, r, b, c);
    }
}
