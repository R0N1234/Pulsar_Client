package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.Map;
import java.util.Random;

/**
 * Gravidade de Itens: item no chão fica deitado, parado e virado para um lado qualquer, em vez de girar
 * e flutuar. Blocos ficam apoiados no chão. Pilhas viram um montinho (até 4 cópias).
 * Troca só o desenho do item no chão; com o mod desligado, o desenho do Minecraft volta na hora.
 */
public class GravidadeItens extends PulsarMod {
    private final BoolSetting girarNoAr = add(new BoolSetting("girar_no_ar", "Girar enquanto cai", true));
    private boolean falhou;
    private static long desenhados;

    public GravidadeItens() {
        super("gravidade_itens", "Gravidade de Itens", "Itens caem deitados no chão", Category.VISUAL, false);
    }

    /** Quantos itens já saíram deitados (teste automático). */
    public static long desenhados() {
        return desenhados;
    }

    private Map<Class<? extends Entity>, Render<? extends Entity>> mapa;
    private Desenho desenho;
    private int vezes;

    /** Confere a cada tick: o OptiFine (modelos de entidade) devolve o desenho original quando o pacote de texturas muda. */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onTick(TickEvent.ClientTickEvent event) {
        if (falhou || event.phase != TickEvent.Phase.END || mc.getRenderManager() == null || mc.getRenderItem() == null) return;
        try {
            if (mapa == null) mapa = ReflectionHelper.getPrivateValue(RenderManager.class, mc.getRenderManager(), "entityRenderMap", "field_78729_o");
            if (mapa.get(EntityItem.class) instanceof Desenho) return;
            if (desenho == null) desenho = new Desenho(mc.getRenderManager(), mc.getRenderItem());
            mapa.put(EntityItem.class, desenho);
            System.out.println("[Pulsar] Gravidade de Itens: " + (vezes++ == 0 ? "instalada" : "instalada de novo (o desenho original tinha voltado)"));
        } catch (Throwable t) {
            falhou = true;
            System.out.println("[Pulsar] Gravidade de Itens: não instalada (" + t + ")");
        }
    }

    private final class Desenho extends RenderEntityItem {
        private final RenderItem itens;
        private final Random sorte = new Random();

        Desenho(RenderManager rm, RenderItem itens) {
            super(rm, itens);
            this.itens = itens;
        }

        @Override
        public void doRender(EntityItem e, double x, double y, double z, float yaw, float partialTicks) {
            ItemStack stack = e.getEntityItem();
            if (!isEnabled() || stack == null || stack.getItem() == null) {
                super.doRender(e, x, y, z, yaw, partialTicks);
                return;
            }
            desenhados++;
            IBakedModel modelo = itens.getItemModelMesher().getItemModel(stack);
            boolean bloco = modelo.isGui3d();
            boolean textura = bindEntityTexture(e);
            ITextureObject atlas = renderManager.renderEngine.getTexture(TextureMap.locationBlocksTexture);
            if (textura) atlas.setBlurMipmap(false, false);
            GlStateManager.enableRescaleNormal();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x, (float) y, (float) z);
            // Cada item tem um lado sorteado quando nasce (hoverStart), então ele não fica girando.
            GlStateManager.rotate(e.hoverStart * 57.29578F, 0.0F, 1.0F, 0.0F);
            boolean noAr = !e.onGround && !e.isInWater();
            int copias = stack.stackSize > 48 ? 4 : stack.stackSize > 16 ? 3 : stack.stackSize > 1 ? 2 : 1;
            sorte.setSeed(e.getEntityId() * 31L + 187L);
            for (int i = 0; i < copias; i++) {
                GlStateManager.pushMatrix();
                float dx = i == 0 ? 0 : (sorte.nextFloat() - 0.5F) * 0.16F;
                float dz = i == 0 ? 0 : (sorte.nextFloat() - 0.5F) * 0.16F;
                if (bloco) {
                    GlStateManager.translate(dx, -0.125F + i * 0.125F, dz);
                    if (noAr && girarNoAr.get()) GlStateManager.rotate((e.getAge() + partialTicks) * 12F, 1.0F, 0.0F, 0.5F);
                    IBakedModel m = ForgeHooksClient.handleCameraTransforms(modelo, ItemCameraTransforms.TransformType.GROUND);
                    itens.renderItem(stack, m);
                } else {
                    GlStateManager.translate(dx, 0.01F + i * 0.02F, dz);
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    if (noAr && girarNoAr.get()) GlStateManager.rotate((e.getAge() + partialTicks) * 12F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.scale(0.5F, 0.5F, 0.5F);
                    itens.renderItem(stack, modelo);
                }
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
            bindEntityTexture(e);
            if (textura) atlas.restoreLastBlurMipmap();
            renderName(e, x, y, z);
        }
    }
}
