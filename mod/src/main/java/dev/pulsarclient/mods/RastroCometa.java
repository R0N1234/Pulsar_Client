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
 * Rastro de Cometa: desfoque de movimento. Cada quadro do mundo é misturado com o anterior, então ao virar a câmera
 * a imagem deixa um rastro. Como nas Cores Vivas, roda antes da interface: HUD, chat e menus ficam nítidos.
 * São três passadas leves na tela inteira (misturar, guardar para o próximo quadro e devolver para a tela).
 */
public class RastroCometa extends PulsarMod {
    private static final ResourceLocation GRUPO = new ResourceLocation("pulsar", "shaders/post/rastro_cometa.json");

    private final SliderSetting forca = add(new SliderSetting("forca", "Força do rastro", 50, 10, 90, 5, "%"));

    private ShaderGroup grupo;
    private Shader rastro;
    private int largura;
    private int altura;
    // No primeiro quadro o quadro anterior ainda está vazio (preto): ele só é guardado, sem misturar.
    private boolean primeiro;
    private boolean falhou;

    public RastroCometa() {
        super("rastro_cometa", "Rastro de Cometa", "Desfoque de movimento: a imagem deixa um rastro quando você vira", Category.VISUAL, false);
    }

    @Override
    protected void onDisable() {
        apagar();
    }

    // Depois das Cores Vivas (que rodam em HIGHEST), para o rastro já sair com as cores novas.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !isEnabled() || falhou) return;
        if (!OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled()) return;
        try {
            Framebuffer principal = mc.getFramebuffer();
            if (grupo == null) criar(principal);
            if (principal.framebufferWidth != largura || principal.framebufferHeight != altura) {
                largura = principal.framebufferWidth;
                altura = principal.framebufferHeight;
                grupo.createBindFramebuffers(largura, altura);
                primeiro = true;
            }
            ShaderUniform u = rastro.getShaderManager().getShaderUniform("Rastro");
            if (u != null) u.set(primeiro ? 0f : (float) (forca.get() / 100.0));
            primeiro = false;
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
            System.err.println("[Pulsar] Rastro de Cometa desligado por erro no shader: " + t);
        }
    }

    private void criar(Framebuffer principal) throws Exception {
        grupo = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), principal, GRUPO);
        Framebuffer troca = grupo.getFramebufferRaw("swap");
        Framebuffer anterior = grupo.getFramebufferRaw("anterior");
        rastro = grupo.addShader("pulsar_rastro", principal, troca);
        rastro.addAuxFramebuffer("PrevSampler", anterior, anterior.framebufferWidth, anterior.framebufferHeight);
        grupo.addShader("blit", troca, anterior);
        grupo.addShader("blit", troca, principal);
        largura = principal.framebufferWidth;
        altura = principal.framebufferHeight;
        grupo.createBindFramebuffers(largura, altura);
        primeiro = true;
        System.out.println("[Pulsar] Rastro de Cometa: shader carregado");
    }

    private void apagar() {
        if (grupo != null) grupo.deleteShaderGroup();
        grupo = null;
        rastro = null;
    }
}
