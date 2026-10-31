package dev.pulsarclient.rede;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.gui.Universo;
import dev.pulsarclient.mods.Amigos;
import dev.pulsarclient.mods.IconePulsar;
import dev.pulsarclient.mods.Nivel;
import dev.pulsarclient.mods.PulsarMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;

/**
 * Ícone PC e nível ao lado do nome de quem usa o Pulsar, em cima da cabeça.
 * Só desenha quando o próprio Minecraft desenha o nome (mesma regra de distância, time e invisibilidade).
 */
public class Selos {
    public static final int COR_NIVEL = 0xFF7FE7FF;
    private static Method podeNome;
    private static boolean procurou;
    private int ticks;

    private static PulsarMod mod(String id) {
        return PulsarClient.get().mod(id);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks % 20 != 0) return;
        IconePulsar icone = (IconePulsar) mod("icone_pulsar");
        Nivel nivel = (Nivel) mod("nivel");
        Amigos amigos = (Amigos) mod("amigos");
        boolean ativa = icone != null && icone.isEnabled() || nivel != null && nivel.isEnabled() || amigos != null && amigos.isEnabled();
        Rede.get().configurar(ativa, icone != null && !icone.aparecer(), amigos == null || amigos.mostrarServidor());
    }

    @SubscribeEvent
    public void onNome(RenderLivingEvent.Specials.Post event) {
        if (!(event.entity instanceof EntityPlayer)) return;
        IconePulsar icone = (IconePulsar) mod("icone_pulsar");
        Nivel nivelMod = (Nivel) mod("nivel");
        boolean comIcone = icone != null && icone.naCabeca();
        boolean comNivel = nivelMod != null && nivelMod.naCabeca();
        if (!comIcone && !comNivel) return;
        EntityPlayer p = (EntityPlayer) event.entity;
        int nivel = Rede.get().nivelDe(p);
        if (nivel <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (p == mc.thePlayer) {
            // O seu nome só aparece no F5 com o Meu Nick ligado.
            PulsarMod meuNick = mod("meu_nick");
            if (mc.gameSettings.thirdPersonView == 0 || meuNick == null || !meuNick.isEnabled() || !Minecraft.isGuiEnabled() || p.isInvisible()) return;
        } else {
            if (!nomeVisivel(event.renderer, p)) return;
            double longe = p.isSneaking() ? 32 : 64;
            if (p.getDistanceSqToEntity(mc.getRenderViewEntity()) >= longe * longe) return;
        }
        double y = event.y + p.height + 0.5 - (p.isSneaking() ? 0.25 : 0);
        // Com o placar embaixo do nome (vida, pontos), o nome sobe uma linha.
        ScoreObjective abaixo = p.getWorldScoreboard().getObjectiveInDisplaySlot(2);
        if (abaixo != null && p != mc.thePlayer && p.getDistanceSqToEntity(mc.getRenderViewEntity()) < 100) y += mc.fontRendererObj.FONT_HEIGHT * 1.15f * 0.026666668f;
        desenhar(p.getDisplayName().getFormattedText(), event.x, y, event.z, comIcone, comNivel ? nivel : 0);
    }

    private static boolean nomeVisivel(Render<?> renderer, Entity e) {
        if (!procurou) {
            procurou = true;
            try {
                podeNome = ReflectionHelper.findMethod(Render.class, null, new String[] {"canRenderName", "func_177070_b"}, Entity.class);
            } catch (Throwable t) {
                System.out.println("[Pulsar] Rede: regra do nome indisponível, usando a distância");
            }
        }
        if (podeNome == null) return Minecraft.isGuiEnabled() && !e.isInvisible();
        try {
            return (Boolean) podeNome.invoke(renderer, e);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Ícone à esquerda do nome e "Nv N" à direita, no mesmo lugar e tamanho do nome do Minecraft. */
    private static void desenhar(String nome, double x, double y, double z, boolean icone, int nivel) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        FontRenderer font = mc.fontRendererObj;
        float escala = 0.016666668f * 1.6f;
        int metade = font.getStringWidth(nome) / 2;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(rm.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-escala, -escala, escala);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        if (icone) {
            mc.getTextureManager().bindTexture(Universo.ICONE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GlStateManager.color(1f, 1f, 1f, 1f);
            float l = -metade - 11, t = -1, r = -metade - 2, b = 8;
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(l, t, 0).tex(0, 0).endVertex();
            wr.pos(l, b, 0).tex(0, 1).endVertex();
            wr.pos(r, b, 0).tex(1, 1).endVertex();
            wr.pos(r, t, 0).tex(1, 0).endVertex();
            tess.draw();
        }
        if (nivel > 0) font.drawStringWithShadow("Nv " + nivel, metade + 3, 0, COR_NIVEL);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}
