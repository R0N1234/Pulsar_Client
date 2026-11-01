package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Molduras leves do Motor Pulsar (item frames). Com o Motor e a opção ligados:
 * - a moldura de madeira vira uma lista pronta na placa de vídeo (1 chamada, em vez de 7 desenhos por moldura);
 * - o item dentro não cria uma entidade nova a cada quadro, não tem o brilho de encantamento
 *   e a bússola não reenvia a textura dela a cada moldura (era o que mais travava);
 * - sem o "pushAttrib" do OpenGL a cada item.
 * Mapas e quem estiver com a opção desligada continuam com o desenho do Minecraft.
 */
public class MolduraLeve extends RenderItemFrame {
    private static final ModelResourceLocation MODELO = new ModelResourceLocation("item_frame", "normal");

    /** Quem decide se a moldura sai leve (o Motor). */
    public interface Regra {
        boolean leve();
    }

    private static Regra regra;
    private static long desenhadas;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final RenderItem itens;
    private IBakedModel modeloDaLista;
    private int lista = -1;
    // Cópia do item sem o encantamento (só para o desenho), guardada enquanto o item da moldura for o mesmo.
    private final Map<ItemStack, ItemStack> semBrilho = new WeakHashMap<ItemStack, ItemStack>();

    public MolduraLeve(RenderManager rm, RenderItem itens) {
        super(rm, itens);
        this.itens = itens;
    }

    public static void usar(Regra r) {
        regra = r;
    }

    /** Quantas molduras já saíram no modo leve (teste automático). */
    public static long desenhadas() {
        return desenhadas;
    }

    @Override
    public void doRender(EntityItemFrame frame, double x, double y, double z, float yaw, float partialTicks) {
        ItemStack item = frame.getDisplayedItem();
        if (regra == null || !regra.leve() || (item != null && item.getItem() instanceof ItemMap)) {
            super.doRender(frame, x, y, z, yaw, partialTicks);
            return;
        }
        desenhadas++;
        GlStateManager.pushMatrix();
        BlockPos pos = frame.getHangingPosition();
        GlStateManager.translate(pos.getX() - frame.posX + x + 0.5, pos.getY() - frame.posY + y + 0.5, pos.getZ() - frame.posZ + z + 0.5);
        GlStateManager.rotate(180.0F - frame.rotationYaw, 0.0F, 1.0F, 0.0F);
        renderManager.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        desenharMoldura();
        GlStateManager.translate(0.0F, 0.0F, 0.4375F);
        if (item != null) desenharItem(frame, item);
        GlStateManager.popMatrix();
        renderName(frame, x + frame.facingDirection.getFrontOffsetX() * 0.3F, y - 0.25D, z + frame.facingDirection.getFrontOffsetZ() * 0.3F);
    }

    /** A madeira da moldura: gravada uma vez numa lista do OpenGL e refeita só quando o pacote de texturas muda. */
    private void desenharMoldura() {
        IBakedModel modelo = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getModel(MODELO);
        if (modelo != modeloDaLista || lista < 0) {
            if (lista < 0) lista = GLAllocation.generateDisplayLists(1);
            modeloDaLista = modelo;
            GL11.glNewList(lista, GL11.GL_COMPILE);
            GlStateManager.pushMatrix();
            GlStateManager.translate(-0.5F, -0.5F, -0.5F);
            mc.getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(modelo, 1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
            GL11.glEndList();
        }
        GlStateManager.callList(lista);
    }

    private void desenharItem(EntityItemFrame frame, ItemStack item) {
        ItemStack desenho = semBrilho.get(item);
        if (desenho == null) {
            desenho = item.copy();
            desenho.stackSize = 1;
            if (desenho.hasTagCompound()) desenho.getTagCompound().removeTag("ench");
            semBrilho.put(item, desenho);
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.rotate(frame.getRotation() * 360.0F / 8.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        if (!itens.shouldRenderItemIn3D(desenho) || desenho.getItem() instanceof ItemSkull) GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        itens.renderItem(desenho, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
