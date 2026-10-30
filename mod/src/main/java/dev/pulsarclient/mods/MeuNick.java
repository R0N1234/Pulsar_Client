package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Meu Nick: mostra o seu próprio nome em cima do personagem no F5 e na Órbita Livre. */
public class MeuNick extends PulsarMod {
    private final BoolSetting background = add(new BoolSetting("fundo", "Fundo escuro atrás do nome", true));
    private final BoolSetting shadow = add(new BoolSetting("sombra", "Sombra no texto", true));

    public MeuNick() {
        super("meu_nick", "Meu Nick", "Mostra o seu nick em cima do personagem no F5", Category.VISUAL, true);
    }

    @SubscribeEvent
    public void onName(RenderLivingEvent.Specials.Post event) {
        if (!isEnabled() || event.entity != mc.thePlayer || mc.gameSettings.thirdPersonView == 0) return;
        if (!Minecraft.isGuiEnabled() || mc.thePlayer.isInvisible()) return;
        String text = mc.thePlayer.getDisplayName().getFormattedText();
        float up = mc.thePlayer.height + 0.5f - (mc.thePlayer.isSneaking() ? 0.25f : 0f);
        Etiqueta.draw(text, event.x, event.y + up, event.z, -1, background.get(), shadow.get());
    }
}
