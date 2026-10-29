package dev.pulsarclient.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/**
 * Parte do Pulsar que carrega antes do Minecraft. Por enquanto só troca a tela de carregamento do Forge
 * pela do Pulsar (veja {@link SplashTransformer}).
 */
@IFMLLoadingPlugin.Name("Pulsar Client")
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.TransformerExclusions({"dev.pulsarclient.core."})
public class PulsarCore implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{SplashTransformer.class.getName()};
    }

    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}
