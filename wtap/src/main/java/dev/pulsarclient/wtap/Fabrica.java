package dev.pulsarclient.wtap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

import java.util.Set;

/** Botão "Config" na lista de mods: as opções do W-Tap e do Blockhit numa tela do próprio Forge. */
public class Fabrica implements IModGuiFactory {
    @Override
    public void initialize(Minecraft mc) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return Tela.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class Tela extends GuiConfig {
        public Tela(GuiScreen parent) {
            super(parent, new ConfigElement(PulsarWTap.config.getCategory(PulsarWTap.CATEGORIA)).getChildElements(),
                    PulsarWTap.MODID, false, false, "Pulsar W-Tap e Blockhit");
        }
    }
}
