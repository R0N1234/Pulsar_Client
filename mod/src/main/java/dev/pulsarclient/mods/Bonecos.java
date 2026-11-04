package dev.pulsarclient.mods;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerArrow;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerDeadmau5Head;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Bonecos otimizados do Motor Pulsar: jogadores longe são desenhados com menos detalhes, sem perder a skin.
 * Sai a segunda camada da skin (chapéu, jaqueta, mangas e calça), a capa, as flechas presas no corpo,
 * as orelhas do deadmau5 e o brilho de encantamento da armadura. A skin, a armadura e o item na mão continuam.
 * No modo "só a skin" (muitos jogadores perto, como numa mina de PvP) saem também a armadura, o item na mão
 * e o bloco na cabeça: fica só o boneco com a skin, que é o que mais pesa menos.
 * Tudo volta ao normal logo depois de desenhar aquele jogador.
 */
final class Bonecos {
    private static Field layersField;
    private static Field glintField;
    private static boolean reflectionFailed;

    private RendererLivingEntity<?> renderer;
    private final List<Object> original = new ArrayList<Object>();
    private final List<LayerArmorBase<?>> noGlint = new ArrayList<LayerArmorBase<?>>();
    private long simplified;

    /** Quantos bonecos saíram simplificados desde o começo (para o log e o teste). */
    long count() {
        return simplified;
    }

    private long soPeles;

    /** Quantos saíram só com a skin (teste automático). */
    long skinOnlyCount() {
        return soPeles;
    }

    private static boolean reflect() {
        if (reflectionFailed) return false;
        if (layersField != null) return true;
        try {
            layersField = ReflectionHelper.findField(RendererLivingEntity.class, "layerRenderers", "field_177097_h");
            glintField = ReflectionHelper.findField(LayerArmorBase.class, "skipRenderGlint", "field_177193_i");
            return true;
        } catch (Throwable t) {
            reflectionFailed = true;
            System.err.println("[Pulsar] Motor: bonecos otimizados indisponíveis: " + t);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    void simplify(RendererLivingEntity<?> r, AbstractClientPlayer player) {
        simplify(r, player, false);
    }

    void simplify(RendererLivingEntity<?> r, AbstractClientPlayer player, boolean soPele) {
        if (!(r instanceof RenderPlayer) || !reflect()) return;
        ModelPlayer model = ((RenderPlayer) r).getMainModel();
        model.bipedHeadwear.showModel = false;
        model.bipedBodyWear.showModel = false;
        model.bipedLeftArmwear.showModel = false;
        model.bipedRightArmwear.showModel = false;
        model.bipedLeftLegwear.showModel = false;
        model.bipedRightLegwear.showModel = false;
        try {
            List<Object> layers = (List<Object>) layersField.get(r);
            renderer = r;
            original.clear();
            original.addAll(layers);
            List<Object> lighter = new ArrayList<Object>();
            for (Object layer : layers) {
                if (layer instanceof LayerCape || layer instanceof LayerArrow || layer instanceof LayerDeadmau5Head) continue;
                if (soPele && (layer instanceof LayerBipedArmor || layer instanceof LayerHeldItem || layer instanceof LayerCustomHead)) continue;
                if (layer instanceof LayerArmorBase && !glintField.getBoolean(layer)) {
                    glintField.setBoolean(layer, true);
                    noGlint.add((LayerArmorBase<?>) layer);
                }
                lighter.add(layer);
            }
            layers.clear();
            layers.addAll(lighter);
            simplified++;
            if (soPele) soPeles++;
        } catch (Throwable t) {
            reflectionFailed = true;
            restore();
            System.err.println("[Pulsar] Motor: bonecos otimizados desligados por erro: " + t);
        }
    }

    /** Devolve as camadas e o brilho como eram. Chamado depois de cada boneco e no fim de cada quadro. */
    @SuppressWarnings("unchecked")
    void restore() {
        if (renderer == null) return;
        try {
            List<Object> layers = (List<Object>) layersField.get(renderer);
            layers.clear();
            layers.addAll(original);
            for (LayerArmorBase<?> layer : noGlint) glintField.setBoolean(layer, false);
        } catch (Throwable ignored) {
        }
        renderer = null;
        original.clear();
        noGlint.clear();
    }
}
