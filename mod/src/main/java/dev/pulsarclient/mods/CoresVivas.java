package dev.pulsarclient.mods;

import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Cores Vivas: contraste, saturação e brilho da imagem do mundo, para deixar o jogo mais vivo.
 *
 * Um shader de pós-processamento (assets/minecraft/shaders/program/pulsar_cores) passa por cima do mundo e da mão
 * logo antes da interface ser desenhada, então a HUD, o chat e os menus continuam com as cores normais.
 * São duas passadas na tela inteira, bem leves; com os controles no neutro o shader nem roda.
 */
public class CoresVivas extends PulsarMod {
    private static final ResourceLocation GRUPO = new ResourceLocation("pulsar", "shaders/post/cores_vivas.json");

    private final SliderSetting saturacao = add(new SliderSetting("saturacao", "Saturação", 140, 0, 200, 5, "%"));
    private final SliderSetting contraste = add(new SliderSetting("contraste", "Contraste", 115, 50, 150, 5, "%"));
    private final SliderSetting brilho = add(new SliderSetting("brilho", "Brilho", 0, -20, 20, 1, ""));

    private ShaderGroup grupo;
    private Shader cores;
    private int largura;
    private int altura;
    private boolean falhou;

    public CoresVivas() {
        super("cores_vivas", "Cores Vivas", "Contraste e saturação para deixar o jogo mais vivo", Category.VISUAL, false);
    }

    @Override
    protected void onDisable() {
        apagar();
    }

    /** Antes de tudo da interface: a imagem nesse momento é só o mundo e a mão. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !isEnabled() || falhou) return;
        if (!OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled()) return;
        float sat = (float) (saturacao.get() / 100.0);
        float con = (float) (contraste.get() / 100.0);
        float bri = (float) (brilho.get() / 100.0);
        if (sat == 1f && con == 1f && bri == 0f) return;
        try {
            Framebuffer principal = mc.getFramebuffer();
            if (grupo == null) criar(principal);
            if (principal.framebufferWidth != largura || principal.framebufferHeight != altura) {
                largura = principal.framebufferWidth;
                altura = principal.framebufferHeight;
                grupo.createBindFramebuffers(largura, altura);
            }
            uniforme("Saturacao", sat);
            uniforme("Contraste", con);
            uniforme("Brilho", bri);
            // Igual ao EntityRenderer quando desenha os shaders do próprio jogo.
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            grupo.loadShaderGroup(event.partialTicks);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            principal.bindFramebuffer(true);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1f);
        } catch (Throwable t) {
            falhou = true;
            apagar();
            mc.getFramebuffer().bindFramebuffer(true);
            System.err.println("[Pulsar] Cores Vivas desligado por erro no shader: " + t);
        }
    }

    private void criar(Framebuffer principal) throws Exception {
        grupo = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), principal, GRUPO);
        Framebuffer troca = grupo.getFramebufferRaw("swap");
        cores = grupo.addShader("pulsar_cores", principal, troca);
        grupo.addShader("blit", troca, principal);
        largura = principal.framebufferWidth;
        altura = principal.framebufferHeight;
        grupo.createBindFramebuffers(largura, altura);
        System.out.println("[Pulsar] Cores Vivas: shader carregado");
    }

    private void uniforme(String nome, float valor) {
        ShaderUniform u = cores.getShaderManager().getShaderUniform(nome);
        if (u != null) u.set(valor);
    }

    private void apagar() {
        if (grupo != null) grupo.deleteShaderGroup();
        grupo = null;
        cores = null;
    }
}
