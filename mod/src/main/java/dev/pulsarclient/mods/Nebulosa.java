package dev.pulsarclient.mods;

import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Nebulosa: desfoque no fundo dos menus dentro do jogo (ESC, inventário, painel do Pulsar...).
 * O mundo e a HUD ficam borrados atrás do menu; no chat e no Editar HUD nada muda.
 * São duas passadas do desfoque do próprio Minecraft na tela inteira, só enquanto um menu está aberto.
 */
public class Nebulosa extends PulsarMod {
    private static final ResourceLocation GRUPO = new ResourceLocation("pulsar", "shaders/post/nebulosa.json");

    private final SliderSetting forca = add(new SliderSetting("forca", "Força do desfoque", 8, 2, 20, 1, ""));
    private final BoolSetting inventario = add(new BoolSetting("inventario", "Também no inventário e nos baús", true));
    private final BoolSetting suave = add(new BoolSetting("suave", "Entrar aos poucos", true));

    private ShaderGroup grupo;
    private Shader horizontal, vertical;
    private int largura, altura;
    private boolean falhou;
    private GuiScreen telaAnterior;
    private long abriu;
    private static long vezes;

    public Nebulosa() {
        super("nebulosa", "Nebulosa", "Desfoque no fundo dos menus", Category.VISUAL, false);
    }

    /** Quantas vezes o desfoque já foi desenhado (teste automático). */
    public static long vezes() {
        return vezes;
    }

    @Override
    protected void onDisable() {
        apagar();
    }

    private boolean desfocar(GuiScreen tela) {
        if (mc.theWorld == null || tela == null || tela instanceof GuiChat || tela instanceof GuiHudEditor) return false;
        return inventario.get() || !(tela instanceof GuiContainer);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!isEnabled() || falhou || !desfocar(event.gui)) return;
        if (!OpenGlHelper.shadersSupported || !OpenGlHelper.isFramebufferEnabled()) return;
        if (event.gui != telaAnterior) {
            telaAnterior = event.gui;
            abriu = System.currentTimeMillis();
        }
        float raio = (float) forca.get();
        if (suave.get()) raio *= Math.min(1f, (System.currentTimeMillis() - abriu) / 180f);
        if (raio < 0.5f) return;
        try {
            Framebuffer principal = mc.getFramebuffer();
            if (grupo == null) criar(principal);
            if (principal.framebufferWidth != largura || principal.framebufferHeight != altura) {
                largura = principal.framebufferWidth;
                altura = principal.framebufferHeight;
                grupo.createBindFramebuffers(largura, altura);
            }
            uniforme(horizontal, "Radius", raio);
            uniforme(vertical, "Radius", raio);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            grupo.loadShaderGroup(event.renderPartialTicks);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            principal.bindFramebuffer(true);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1f);
            vezes++;
        } catch (Throwable t) {
            falhou = true;
            apagar();
            mc.getFramebuffer().bindFramebuffer(true);
            System.err.println("[Pulsar] Nebulosa desligada por erro no shader: " + t);
        }
    }

    private void criar(Framebuffer principal) throws Exception {
        grupo = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), principal, GRUPO);
        Framebuffer troca = grupo.getFramebufferRaw("swap");
        horizontal = grupo.addShader("blur", principal, troca);
        vertical = grupo.addShader("blur", troca, principal);
        horizontal.getShaderManager().getShaderUniform("BlurDir").set(1f, 0f);
        vertical.getShaderManager().getShaderUniform("BlurDir").set(0f, 1f);
        largura = principal.framebufferWidth;
        altura = principal.framebufferHeight;
        grupo.createBindFramebuffers(largura, altura);
        System.out.println("[Pulsar] Nebulosa: shader carregado");
    }

    private static void uniforme(Shader s, String nome, float valor) {
        ShaderUniform u = s.getShaderManager().getShaderUniform(nome);
        if (u != null) u.set(valor);
    }

    private void apagar() {
        if (grupo != null) grupo.deleteShaderGroup();
        grupo = null;
        horizontal = vertical = null;
        telaAnterior = null;
    }
}
