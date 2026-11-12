package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

/**
 * Focar Jogador: quando a treta começa (você bate em alguém ou leva hit de alguém), aparece um aviso com a cabeça
 * da skin, o nick e a tecla para focar. Com o foco ligado, o nick dele fica na cor escolhida e uma caixa aparece
 * em volta dele enquanto está à vista. Atrás de parede fica só o nick (o jogo já mostra o nome através de blocos,
 * menos agachado). Depois de um tempo sem ver o jogador (30 s de fábrica), o foco sai sozinho.
 */
public class FocarJogador extends HudMod {
    private static final int AVISO_TICKS = 20 * 6;
    /** Até quantos blocos o jogador que te bateu pode estar. */
    private static final double ALCANCE_HIT = 6.5;

    private final KeySetting tecla = add(new KeySetting("tecla_focar", "Tecla para focar o jogador", Keyboard.KEY_V));
    private final ColorSetting destaque = add(new ColorSetting("destaque", "Cor do destaque", ColorSetting.VERMELHO));
    private final SliderSetting semVer = add(new SliderSetting("sem_ver", "Tirar o foco depois de", 30, 5, 120, 5, "s"));
    private final BoolSetting caixa = add(new BoolSetting("caixa", "Caixa em volta do jogador à vista", true));
    private final BoolSetting avisoBater = add(new BoolSetting("aviso_bater", "Perguntar quando você bate em alguém", true));
    private final BoolSetting avisoLevar = add(new BoolSetting("aviso_levar", "Perguntar quando alguém te bate", true));
    private final BoolSetting somAviso = add(new BoolSetting("som", "Som ao focar e ao tirar o foco", true));

    private int ticks;
    private int hurtAntes;

    // Quem o aviso pergunta para focar.
    private UUID candidato;
    private String candidatoNome = "";
    private int avisoAte = -1;

    // Quem está focado.
    private UUID foco;
    private String focoNome = "";
    private int vistoEm;

    private static int desenhosCaixa;
    private static int desenhosNome;

    public FocarJogador() {
        super("focar_jogador", "Focar Jogador", "Na treta, pergunta se quer focar o jogador: o nick fica destacado e uma caixa aparece em volta dele",
                Category.COMBATE, CENTRO, 40, false);
    }

    /** Teste automático. */
    public static int desenhosCaixa() { return desenhosCaixa; }
    public static int desenhosNome() { return desenhosNome; }

    public boolean focado() { return foco != null; }
    public String focadoNome() { return focoNome; }

    public boolean avisando() { return avisoAte >= ticks && candidato != null; }

    /** Teste automático e tecla: foca (ou tira o foco) do jogador. */
    public void focar(EntityPlayer p) {
        if (p == null) {
            if (foco != null) {
                System.out.println("[Pulsar] Focar Jogador: foco tirado de " + focoNome);
                foco = null;
                focoNome = "";
                if (somAviso.get()) tocar(0.7f);
            }
            return;
        }
        foco = p.getUniqueID();
        focoNome = p.getName();
        vistoEm = ticks;
        avisoAte = -1;
        candidato = null;
        if (somAviso.get()) tocar(1.4f);
        System.out.println("[Pulsar] Focar Jogador: focado em " + focoNome);
    }

    private void tocar(float tom) {
        mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new ResourceLocation("random.click"), tom));
    }

    private void perguntar(EntityPlayer p) {
        if (p == null || p == mc.thePlayer || p.getUniqueID().equals(foco)) return;
        candidato = p.getUniqueID();
        candidatoNome = p.getName();
        avisoAte = ticks + AVISO_TICKS;
    }

    // ---------- quem está na treta ----------

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.entityPlayer != mc.thePlayer) return;
        if (avisoBater.get() && event.target instanceof EntityPlayer) perguntar((EntityPlayer) event.target);
    }

    /** Levou dano: o jogador mais perto que está dando o golpe (ou só o mais perto, a até 6,5 blocos). */
    private EntityPlayer quemBateu(EntityPlayerSP eu) {
        EntityPlayer melhor = null;
        double melhorD = ALCANCE_HIT * ALCANCE_HIT;
        for (Object o : mc.theWorld.playerEntities) {
            EntityPlayer p = (EntityPlayer) o;
            if (p == eu || p.isDead) continue;
            double d = p.getDistanceSqToEntity(eu);
            if (p.isSwingInProgress || p.swingProgress > 0) d *= 0.25;
            if (d < melhorD) {
                melhorD = d;
                melhor = p;
            }
        }
        return melhor;
    }

    @Override
    public void tick() {
        ticks++;
        EntityPlayerSP eu = mc.thePlayer;
        if (eu == null || mc.theWorld == null) {
            foco = null;
            candidato = null;
            hurtAntes = 0;
            return;
        }
        if (!isEnabled()) {
            foco = null;
            candidato = null;
            return;
        }
        if (eu.hurtTime > hurtAntes && avisoLevar.get()) perguntar(quemBateu(eu));
        hurtAntes = eu.hurtTime;

        if (foco != null) {
            EntityPlayer p = mc.theWorld.getPlayerEntityByUUID(foco);
            if (p != null && !p.isDead && eu.canEntityBeSeen(p)) vistoEm = ticks;
            if (ticks - vistoEm > semVer.getInt() * 20) {
                System.out.println("[Pulsar] Focar Jogador: " + focoNome + " sumiu por " + semVer.getInt() + " s, foco tirado");
                foco = null;
                focoNome = "";
            }
        }
    }

    // ---------- tecla ----------

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState()) apertou(Keyboard.getEventKey());
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() >= 0) apertou(Mouse.getEventButton() - 100);
    }

    private void apertou(int codigo) {
        if (!isEnabled() || codigo == 0 || codigo != tecla.get() || mc.currentScreen != null || mc.thePlayer == null) return;
        if (avisando()) {
            focar(mc.theWorld.getPlayerEntityByUUID(candidato));
        } else if (foco != null) {
            focar(null);
        }
    }

    // ---------- destaque no mundo ----------

    private EntityPlayer focadoNoMundo() {
        if (foco == null || mc.theWorld == null) return null;
        EntityPlayer p = mc.theWorld.getPlayerEntityByUUID(foco);
        return p == null || p.isDead ? null : p;
    }

    /** As mesmas regras do jogo para mostrar o nome em cima de um jogador. */
    private boolean jogoMostraNome(EntityPlayer p) {
        if (!Minecraft.isGuiEnabled() || p == mc.getRenderViewEntity() || p.isInvisibleToPlayer(mc.thePlayer) || p.riddenByEntity != null) return false;
        Team dele = p.getTeam();
        Team meu = mc.thePlayer.getTeam();
        if (dele != null) {
            switch (dele.getNameTagVisibility()) {
                case NEVER: return false;
                case HIDE_FOR_OTHER_TEAMS: return meu == null || dele.isSameTeam(meu);
                case HIDE_FOR_OWN_TEAM: return meu == null || !dele.isSameTeam(meu);
                default: break;
            }
        }
        double max = p.isSneaking() ? 32 : 64;
        return p.getDistanceSqToEntity(mc.getRenderViewEntity()) < max * max;
    }

    /** No lugar do nome normal, o nick na cor do destaque (atrás de parede o jogo já mostra o nome, e aqui também). */
    @SubscribeEvent
    public void onName(RenderLivingEvent.Specials.Pre event) {
        if (!isEnabled() || foco == null || !(event.entity instanceof EntityPlayer) || !foco.equals(event.entity.getUniqueID())) return;
        EntityPlayer p = (EntityPlayer) event.entity;
        if (!jogoMostraNome(p)) return;
        event.setCanceled(true);
        desenhosNome++;
        String texto = "§l" + p.getDisplayName().getFormattedText();
        float up = p.height + 0.5f - (p.isSneaking() ? 0.25f : 0f);
        if (p.isSneaking()) {
            // Agachado: igual ao jogo, o nome não aparece através de parede.
            Etiqueta.drawComProfundidade(texto, event.x, event.y + up, event.z, destaque.argb(0xFF));
        } else {
            Etiqueta.draw(texto, event.x, event.y + up, event.z, destaque.argb(0xFF), true, true);
        }
    }

    /** Caixa em volta do jogador focado, só quando ele está à vista. */
    @SubscribeEvent
    public void onWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || !caixa.get()) return;
        EntityPlayer p = focadoNoMundo();
        if (p == null || mc.thePlayer == null || !mc.thePlayer.canEntityBeSeen(p) || p == mc.getRenderViewEntity()) return;
        float pt = event.partialTicks;
        RenderManager rm = mc.getRenderManager();
        double x = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt - rm.viewerPosX;
        double y = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt - rm.viewerPosY;
        double z = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt - rm.viewerPosZ;
        AxisAlignedBB bb = p.getEntityBoundingBox();
        double w = (bb.maxX - bb.minX) / 2 + 0.05, h = bb.maxY - bb.minY + 0.05;
        AxisAlignedBB caixaBb = new AxisAlignedBB(x - w, y - 0.05, z - w, x + w, y + h, z + w);
        int cor = destaque.rgb();
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GL11.glLineWidth(2.5f);
        RenderGlobal.drawOutlinedBoundingBox(caixaBb, cor >> 16 & 255, cor >> 8 & 255, cor & 255, 230);
        GL11.glLineWidth(1f);
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
        desenhosCaixa++;
    }

    // ---------- aviso na tela ----------

    @Override public int width() { return 130; }
    @Override public int height() { return 22; }

    private ResourceLocation skinDe(UUID id, String nome) {
        if (mc.theWorld != null) {
            EntityPlayer p = mc.theWorld.getPlayerEntityByUUID(id);
            if (p instanceof AbstractClientPlayer) return ((AbstractClientPlayer) p).getLocationSkin();
        }
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(id);
            if (info != null) return info.getLocationSkin();
        }
        return DefaultPlayerSkin.getDefaultSkin(id);
    }

    private void cabeca(ResourceLocation skin, boolean chapeu, int x, int y, int tam) {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(x, y, 8f, 8f, 8, 8, tam, tam, 64f, 64f);
        if (chapeu) Gui.drawScaledCustomSizeModalRect(x, y, 40f, 8f, 8, 8, tam, tam, 64f, 64f);
    }

    @Override
    public void render(boolean preview) {
        UUID id;
        String nome;
        boolean focadoAgora;
        if (preview) {
            id = mc.thePlayer != null ? mc.thePlayer.getUniqueID() : null;
            nome = mc.thePlayer != null ? mc.thePlayer.getName() : "Jogador";
            focadoAgora = false;
        } else if (avisando()) {
            id = candidato;
            nome = candidatoNome;
            focadoAgora = false;
        } else if (foco != null) {
            id = foco;
            nome = focoNome;
            focadoAgora = true;
        } else {
            return;
        }
        int w = width(), h = height();
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + w + 2, y + h + 2, BACKGROUND);
        int cor = focadoAgora ? destaque.argb(0xFF) : accent();
        Gui.drawRect(x, y, x + 2, y + h, cor);
        if (id != null) {
            EntityPlayer p = mc.theWorld == null ? null : mc.theWorld.getPlayerEntityByUUID(id);
            cabeca(skinDe(id, nome), p == null || p.isWearing(EnumPlayerModelParts.HAT), x + 6, y + 2, 18);
        }
        int tx = x + 28;
        font().drawStringWithShadow(nome, tx, y + 1, WHITE);
        if (focadoAgora) {
            font().drawStringWithShadow("§lFOCADO §r§7(" + tecla.display() + " tira)", tx, y + 12, cor);
        } else {
            String pergunta = "Focar jogador?";
            font().drawStringWithShadow(pergunta, tx, y + 12, 0xFFC8D2E8);
            String t = "[" + tecla.display() + "]";
            int tw = font().getStringWidth(t);
            Gui.drawRect(x + w - tw - 6, y + 10, x + w - 1, y + 21, 0x80000000);
            font().drawStringWithShadow(t, x + w - tw - 4, y + 12, cor);
        }
        GlStateManager.enableBlend();
    }
}
