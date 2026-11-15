package dev.pulsarclient.mods;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.core.MotorFlags;
import dev.pulsarclient.core.MotorHooks;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityRainFX;
import net.minecraft.client.particle.EntitySpellParticleFX;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.opengl.Display;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pulsarclient.settings.Setting;
import net.minecraft.client.gui.GuiScreen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Motor Pulsar: o otimizador do próprio client.
 *
 * Parte dele roda por ganchos dentro do Minecraft (veja core/MotorTransformer): entidades atrás de paredes ou longe,
 * baús e placas longe, animações de textura e limite de partículas. O resto usa eventos do Forge:
 * FPS em segundo plano, suportes de armadura, distância dos nomes, céu, chuva, fogo na tela e os perfis prontos.
 *
 * O perfil padrão é o Desempenho: o jogo fica mais simples e limpo na tela, com o mínimo de peso no FPS.
 */
public class MotorPulsar extends PulsarMod {
    private static final String[] PROFILES = {"Desempenho", "Desempenho + Qualidade", "Qualidade+"};
    private static final int[] PARTICLE_LIMITS = {0, 300, 100, 30};

    /** Versão em que o perfil Desempenho passou a ser o padrão: quem vem de antes recebe ele uma vez. */
    private static final String PERFIL_PADRAO = "0.6";

    /** Começo do código de um perfil compartilhado (o resto é o perfil compactado em base64). */
    public static final String CODIGO = "PULSAR-PERFIL:";
    /** Opções que não vão no perfil compartilhado. */
    private static final Set<String> FORA_DO_PERFIL = new HashSet<String>(Arrays.asList(
            "perfil", "nome_perfil", "tecla_ligar", "favorito", "visto", "perfil_versao", "molduras_versao", "blocos_versao", "bonecos_versao"));

    private final ModeSetting profile = add(new ModeSetting("perfil", "Perfil", 0, PROFILES));
    private final ActionSetting applyProfile = add(new ActionSetting("aplicar", "Aplicar o perfil escolhido", new Runnable() {
        @Override public void run() { aplicarEscolhido(); }
    }));
    // Perfis da pessoa: salvos em .minecraft/pulsar/perfis e compartilhados por um código (copiar e colar).
    private final TextSetting nomePerfil = add(new TextSetting("nome_perfil", "Nome do perfil", "Meu perfil", 24));
    private final ActionSetting salvarPerfil = add(new ActionSetting("perfil_salvar", "Salvar o meu perfil", new Runnable() {
        @Override public void run() { salvarMeuPerfil(); }
    }));
    private final ActionSetting copiarPerfil = add(new ActionSetting("perfil_copiar", "Copiar código para mandar a alguém", new Runnable() {
        @Override public void run() { copiarCodigo(); }
    }));
    private final ActionSetting colarPerfil = add(new ActionSetting("perfil_colar", "Colar perfil recebido (aplica na hora)", new Runnable() {
        @Override public void run() { colarCodigo(); }
    }));
    private final ActionSetting apagarPerfil = add(new ActionSetting("perfil_apagar", "Apagar o perfil escolhido", new Runnable() {
        @Override public void run() { apagarEscolhido(); }
    }) {
        @Override public boolean visible() { return profile.index() >= PROFILES.length; }
    });
    private final BoolSetting background = add(new BoolSetting("fundo", "Poupar PC em segundo plano", true));
    private final SliderSetting backgroundFps = add(new SliderSetting("fundo_fps", "FPS em segundo plano", 15, 5, 60, 5, ""));
    private final BoolSetting occlusion = add(new BoolSetting("oclusao", "Não desenhar o que está atrás de paredes", true));
    private final BoolSetting armorStands = add(new BoolSetting("suportes", "Esconder suportes de armadura", true));
    private final BoolSetting cullMobs = add(new BoolSetting("mobs", "Limitar distância dos mobs", true));
    private final SliderSetting mobDistance = add(new SliderSetting("mobs_dist", "Distância dos mobs", 32, 16, 128, 8, " blocos"));
    private final SliderSetting itemDistance = add(new SliderSetting("itens_dist", "Itens no chão, flechas e XP até", 24, 8, 64, 4, " blocos"));
    private final SliderSetting tileDistance = add(new SliderSetting("blocos_dist", "Baús, placas e cabeças até", 32, 16, 64, 4, " blocos"));
    // Blocos: o que sai da montagem dos chunks e o que deixa de ser desenhado a cada quadro.
    private final BoolSetting plants = add(new BoolSetting("plantas", "Grama alta e flores", false));
    private final BoolSetting tileOcclusion = add(new BoolSetting("blocos_oclusao", "Baús e cabeças atrás de paredes não são desenhados", true));
    private final SliderSetting signText = add(new SliderSetting("placas_texto", "Texto das placas até", 16, 8, 64, 4, " blocos"));
    // Molduras (item frames) com itens pesam muito: no modo leve a moldura vira uma lista pronta e o item sai sem brilho nem animação.
    private final BoolSetting framesLeves = add(new BoolSetting("molduras", "Molduras leves (itens sem brilho e sem animação)", true));
    private final SliderSetting framesDistance = add(new SliderSetting("molduras_dist", "Molduras e itens nelas até", 24, 8, 64, 4, " blocos"));
    private final BoolSetting players = add(new BoolSetting("bonecos", "Bonecos otimizados (a skin continua)", true));
    private final SliderSetting playerDetail = add(new SliderSetting("bonecos_dist", "Bonecos com todos os detalhes até", 8, 0, 64, 4, " blocos"));
    // Muita gente perto (mina de PvP com 40 jogadores): só os mais perto saem completos; longe, só a skin.
    private final SliderSetting playerCrowd = add(new SliderSetting("bonecos_perto", "Com muita gente, detalhe completo só nos", 6, 2, 20, 1, " mais perto"));
    private final SliderSetting playerGear = add(new SliderSetting("bonecos_equip", "Armadura e item na mão dos bonecos até", 16, 8, 64, 4, " blocos"));
    private final BoolSetting limitNames = add(new BoolSetting("nomes", "Limitar distância dos nomes", false));
    private final SliderSetting nameDistance = add(new SliderSetting("nomes_dist", "Distância dos nomes", 32, 8, 64, 4, " blocos"));
    private final ModeSetting particles = add(new ModeSetting("particulas", "Partículas", 2, "Todas", "Menos", "Mínimas"));
    private final ModeSetting particleLimit = add(new ModeSetting("particulas_limite", "Limite de partículas novas", 2,
            "Sem limite", "300 por tick", "100 por tick", "30 por tick"));
    private final BoolSetting animations = add(new BoolSetting("animacoes", "Animação de água, lava e fogo", false));
    private final BoolSetting cleanSky = add(new BoolSetting("ceu", "Céu limpo (sem sol, lua e estrelas)", true));
    private final BoolSetting weather = add(new BoolSetting("clima", "Mostrar chuva e neve", false));
    private final ModeSetting fire = add(new ModeSetting("fogo", "Fogo na tela quando queima", 1, "Normal", "Baixo", "Bem baixo"));
    private final BoolSetting ownPotions = add(new BoolSetting("pocoes", "Esconder partículas das suas poções", true));
    private final TextSetting profileVersion = add(new TextSetting("perfil_versao", ""));
    private final TextSetting framesVersion = add(new TextSetting("molduras_versao", ""));
    private final TextSetting blocksVersion = add(new TextSetting("blocos_versao", ""));
    private final TextSetting crowdVersion = add(new TextSetting("bonecos_versao", ""));
    private final ActionSetting freeMemory = add(new ActionSetting("memoria", "Liberar memória agora", new Runnable() {
        @Override public void run() { System.gc(); }
    }));

    private final Oclusao oclusao = new Oclusao();
    private final Bonecos bonecos = new Bonecos();
    private int appliedParticles = -1;
    /** Lido pelas threads que montam os chunks. */
    private volatile boolean hidePlants;
    private Boolean appliedPlants;
    private World lastWorld;

    /** Céu e clima vazios: no lugar do céu fica só a cor do horizonte, sem sol, lua, estrelas nem chuva. */
    private static final IRenderHandler NADA = new IRenderHandler() {
        @Override public void render(float partialTicks, WorldClient world, Minecraft mc) {}
    };

    public MotorPulsar() {
        super("motor_pulsar", "Motor Pulsar", "Otimizador do Pulsar: mais FPS e menos travadas", Category.MOTOR, true);
        MotorHooks.entities = new MotorHooks.EntityFilter() {
            @Override public boolean shouldRender(Entity entity) { return shouldDraw(entity); }
        };
        MotorHooks.tiles = new MotorHooks.TileFilter() {
            @Override public boolean shouldRender(TileEntity tile) { return shouldDraw(tile); }
        };
        MotorHooks.particles = new MotorHooks.ParticleFilter() {
            @Override public boolean allow(EntityFX particle) { return allowParticle(particle); }
        };
        MotorHooks.blocks = new MotorHooks.BlockFilter() {
            @Override public boolean shouldRender(IBlockState state) { return shouldDraw(state); }
        };
        PlacaLeve.usar(new PlacaLeve.Regra() {
            @Override public int distanciaTexto() { return isEnabled() ? signText.getInt() : 0; }
        });
        MolduraLeve.usar(new MolduraLeve.Regra() {
            @Override public boolean leve() { return isEnabled() && framesLeves.get(); }
        });
        atualizarPerfis();
    }

    private boolean molduraFalhou, placaFalhou;
    private Map<Class<? extends Entity>, Render<? extends Entity>> mapaEntidades;
    private MolduraLeve moldura;
    private PlacaLeve placa;
    private int vezesMoldura, vezesPlaca;

    /**
     * Troca o desenho das molduras e das placas pelo do Motor (que usa o do Minecraft quando a opção está desligada).
     * Confere a cada tick: o OptiFine (modelos de entidade) devolve o desenho original quando o pacote de texturas muda.
     */
    @SuppressWarnings("unchecked")
    private void instalarMolduras() {
        if (mc.getRenderManager() == null || mc.getRenderItem() == null) return;
        if (!molduraFalhou) {
            try {
                if (mapaEntidades == null) mapaEntidades = ReflectionHelper.getPrivateValue(RenderManager.class, mc.getRenderManager(), "entityRenderMap", "field_78729_o");
                if (!(mapaEntidades.get(EntityItemFrame.class) instanceof MolduraLeve)) {
                    if (moldura == null) moldura = new MolduraLeve(mc.getRenderManager(), mc.getRenderItem());
                    mapaEntidades.put(EntityItemFrame.class, moldura);
                    System.out.println("[Pulsar] Motor: molduras leves " + (vezesMoldura++ == 0 ? "instaladas" : "instaladas de novo (o desenho original tinha voltado)"));
                }
            } catch (Throwable t) {
                molduraFalhou = true;
                System.out.println("[Pulsar] Motor: molduras leves indisponíveis (" + t + ")");
            }
        }
        if (!placaFalhou) {
            try {
                Map<Class<? extends TileEntity>, TileEntitySpecialRenderer<? extends TileEntity>> mapa = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
                if (!(mapa.get(TileEntitySign.class) instanceof PlacaLeve)) {
                    if (placa == null) placa = new PlacaLeve();
                    placa.setRendererDispatcher(TileEntityRendererDispatcher.instance);
                    mapa.put(TileEntitySign.class, placa);
                    System.out.println("[Pulsar] Motor: placas leves " + (vezesPlaca++ == 0 ? "instaladas" : "instaladas de novo (o desenho original tinha voltado)"));
                }
            } catch (Throwable t) {
                placaFalhou = true;
                System.out.println("[Pulsar] Motor: placas leves indisponíveis (" + t + ")");
            }
        }
    }

    /** Estado dos ganchos, para o painel e o log. */
    public static String hooksSummary() {
        return "entidades=" + MotorFlags.entities + " baús=" + MotorFlags.tiles + " blocos=" + MotorFlags.blocks
                + " animações=" + MotorFlags.animations + " partículas=" + MotorFlags.particles;
    }

    private static java.lang.reflect.Field conferenciaGl;
    private static boolean conferenciaProcurada;
    private Boolean conferenciaAplicada;

    /**
     * O Minecraft pergunta ao driver "deu erro?" (glGetError) várias vezes em cada quadro. Cada pergunta faz a placa
     * de vídeo parar e responder, e isso custa FPS. O OptiFine (e o CheatBreaker, que vem com ele) desliga essa
     * conferência; o Motor faz o mesmo enquanto estiver ligado.
     */
    private void semConferenciaGl(boolean desligar) {
        if (conferenciaAplicada != null && conferenciaAplicada == desligar) return;
        if (!conferenciaProcurada) {
            conferenciaProcurada = true;
            try {
                conferenciaGl = ReflectionHelper.findField(Minecraft.class, "enableGLErrorChecking", "field_175619_R");
            } catch (Throwable t) {
                System.err.println("[Pulsar] Motor: conferência de erros do OpenGL não achada: " + t);
            }
        }
        conferenciaAplicada = desligar;
        if (conferenciaGl == null) return;
        try {
            conferenciaGl.setBoolean(mc, !desligar);
            System.out.println("[Pulsar] Motor: conferência de erros do OpenGL " + (desligar ? "desligada" : "ligada"));
        } catch (Throwable t) {
            System.err.println("[Pulsar] Motor: não deu para mudar a conferência de erros do OpenGL: " + t);
        }
    }

    @Override
    public void tick() {
        instalarMolduras();
        semConferenciaGl(isEnabled());
        MotorHooks.newTick();
        oclusao.tick();
        if (mc.theWorld != lastWorld) {
            lastWorld = mc.theWorld;
            oclusao.clear();
        }
        MotorHooks.animations = !isEnabled() || animations.get();
        MotorHooks.particleLimit = isEnabled() ? PARTICLE_LIMITS[particleLimit.index()] : 0;
        if (!PERFIL_PADRAO.equals(profileVersion.get())) {
            // Primeira vez nesta versão: o Motor entra no perfil Desempenho, como pedido para o client.
            profileVersion.set(PERFIL_PADRAO);
            profile.set(0);
            if (isEnabled()) applyProfile(0);
            else PulsarClient.get().save();
        }
        if (framesVersion.get().isEmpty()) {
            // Primeira vez com as molduras leves (0.7.2): seguem o perfil que a pessoa já usa.
            framesVersion.set("0.7.2");
            int p = profile.index();
            framesLeves.set(p < 2);
            framesDistance.set(p == 0 ? 16 : p == 1 ? 32 : 64);
            PulsarClient.get().save();
        }
        if (blocksVersion.get().isEmpty()) {
            // Primeira vez com as opções de blocos (0.7.3): seguem o perfil que a pessoa já usa.
            blocksVersion.set("0.7.3");
            int p = profile.index();
            plants.set(p > 0);
            tileOcclusion.set(true);
            signText.set(p == 0 ? 16 : p == 1 ? 24 : 48);
            PulsarClient.get().save();
        }
        if (crowdVersion.get().isEmpty()) {
            // Primeira vez com os bonecos para muita gente (0.8.0): seguem o perfil que a pessoa já usa.
            crowdVersion.set("0.8.0");
            int p = profile.index();
            playerCrowd.set(p == 0 ? 6 : p == 1 ? 10 : 20);
            playerGear.set(p == 0 ? 16 : p == 1 ? 32 : 64);
            PulsarClient.get().save();
        }
        contarJogadores();
        updatePlants();
        updateSky();
        if (!isEnabled()) return;
        // Só aplica quando a opção do Motor muda, para não brigar com a opção de partículas do jogo.
        if (particles.index() != appliedParticles) {
            appliedParticles = particles.index();
            mc.gameSettings.particleSetting = appliedParticles;
        }
    }

    // ---------- blocos ----------

    /** Grama alta e flores: mudou a opção, os chunks são montados de novo (uma vez só). */
    private void updatePlants() {
        boolean hide = isEnabled() && !plants.get();
        hidePlants = hide;
        if (appliedPlants == null) {
            appliedPlants = hide;
            if (hide) System.out.println("[Pulsar] Motor: grama alta e flores fora dos chunks");
            return;
        }
        if (appliedPlants == hide) return;
        appliedPlants = hide;
        System.out.println("[Pulsar] Motor: grama alta e flores " + (hide ? "fora dos chunks" : "de volta"));
        if (mc.renderGlobal != null && mc.theWorld != null) mc.renderGlobal.loadRenderers();
    }

    /** Roda nas threads que montam os chunks: só olha o tipo do bloco. */
    private boolean shouldDraw(IBlockState state) {
        if (!hidePlants) return true;
        Block b = state.getBlock();
        return !(b instanceof BlockTallGrass || b instanceof BlockDoublePlant || b instanceof BlockFlower || b instanceof BlockDeadBush);
    }

    // ---------- céu e clima ----------

    /** Troca o céu e a chuva do mundo atual pelo vazio (e devolve quando a opção é desligada). Só no mundo normal. */
    private void updateSky() {
        WorldProvider wp = mc.theWorld != null ? mc.theWorld.provider : null;
        if (wp == null) return;
        boolean overworld = wp.getDimensionId() == 0;
        boolean noSky = isEnabled() && cleanSky.get() && overworld;
        boolean noWeather = isEnabled() && !weather.get() && overworld;
        if (noSky && wp.getSkyRenderer() == null) wp.setSkyRenderer(NADA);
        else if (!noSky && wp.getSkyRenderer() == NADA) wp.setSkyRenderer(null);
        if (noWeather && wp.getWeatherRenderer() == null) wp.setWeatherRenderer(NADA);
        else if (!noWeather && wp.getWeatherRenderer() == NADA) wp.setWeatherRenderer(null);
    }

    // ---------- partículas ----------

    private boolean allowParticle(EntityFX p) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return true;
        // Gotas da chuva batendo no chão.
        if (!weather.get() && p instanceof EntityRainFX && mc.theWorld.getRainStrength(1f) > 0f) return false;
        // Redemoinho das suas poções em primeira pessoa: aparece em volta da câmera e só atrapalha a visão.
        if (ownPotions.get() && p instanceof EntitySpellParticleFX && mc.gameSettings.thirdPersonView == 0) {
            double dx = p.posX - mc.thePlayer.posX;
            double dz = p.posZ - mc.thePlayer.posZ;
            double dy = p.posY - mc.thePlayer.posY;
            if (dx * dx + dz * dz < 1.0 && dy > -0.5 && dy < 2.5) return false;
        }
        return true;
    }

    // ---------- fogo na tela ----------

    /** O fogo de quando o jogador queima cobre metade da tela; aqui ele é desenhado mais para baixo. */
    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (event.overlayType != RenderBlockOverlayEvent.OverlayType.FIRE || !isEnabled() || fire.index() == 0) return;
        event.setCanceled(true);
        drawFire(fire.index() == 1 ? 0.15f : 0.3f);
    }

    /** Mesmo desenho do Minecraft (ItemRenderer.renderFireInFirstPerson), descido um pouco. */
    private void drawFire(float down) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.color(1f, 1f, 1f, 0.9f);
        GlStateManager.depthFunc(519);
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/fire_layer_1");
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        for (int i = 0; i < 2; i++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(-(i * 2 - 1) * 0.24f, -0.3f - down, 0f);
            GlStateManager.rotate((i * 2 - 1) * 10f, 0f, 1f, 0f);
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(-0.5, -0.5, -0.5).tex(u1, v1).endVertex();
            wr.pos(0.5, -0.5, -0.5).tex(u0, v1).endVertex();
            wr.pos(0.5, 0.5, -0.5).tex(u0, v0).endVertex();
            wr.pos(-0.5, 0.5, -0.5).tex(u1, v0).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
    }

    // ---------- entidades ----------

    private boolean shouldDraw(Entity e) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return true;
        Entity view = mc.getRenderViewEntity();
        if (view == null || e == view || e == mc.thePlayer) return true;
        // Suporte com nome é holograma de servidor: continua visível.
        if (armorStands.get() && e instanceof EntityArmorStand && !e.hasCustomName()) return false;
        double d2 = e.getDistanceSqToEntity(view);
        if (e instanceof EntityItemFrame) {
            double max = framesDistance.get();
            if (d2 > max * max) return false;
        } else if (e instanceof EntityItem || e instanceof EntityXPOrb || e instanceof EntityArrow) {
            double max = itemDistance.get();
            if (d2 > max * max) return false;
        } else if (cullMobs.get() && e instanceof EntityLivingBase && !(e instanceof EntityPlayer)) {
            double max = mobDistance.get();
            if (d2 > max * max) return false;
        }
        // Em terceira pessoa a câmera fica atrás do jogador, então a conta pelos olhos dele não vale.
        return !(occlusion.get() && mc.gameSettings.thirdPersonView == 0 && oclusao.hidden(mc.theWorld, view.getPositionEyes(1f), e));
    }

    private boolean shouldDraw(TileEntity t) {
        if (!isEnabled()) return true;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return true;
        double max = tileDistance.get();
        if (t.getDistanceSq(view.posX, view.posY, view.posZ) > max * max) return false;
        return !(tileOcclusion.get() && mc.theWorld != null && mc.gameSettings.thirdPersonView == 0 && oclusao.hidden(mc.theWorld, view.getPositionEyes(1f), t));
    }

    /** Sem o gancho de entidades (por exemplo, com outro mod mexendo no mesmo lugar), o evento do Forge faz o serviço. */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        if (!MotorFlags.entities && !shouldDraw(event.entity)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderName(RenderLivingEvent.Specials.Pre event) {
        if (!isEnabled() || !limitNames.get() || mc.thePlayer == null || event.entity == mc.thePlayer) return;
        double max = nameDistance.get();
        if (event.entity.getDistanceSqToEntity(mc.thePlayer) > max * max) event.setCanceled(true);
    }

    // ---------- bonecos ----------

    /** Jogadores mais longe que a distância escolhida saem com menos detalhes (veja Bonecos). Você mesmo nunca. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlayerPre(RenderLivingEvent.Pre event) {
        bonecos.restore();
        if (event.entity instanceof AbstractClientPlayer) jogadoresVistos++;
        Entity view = mc.getRenderViewEntity();
        if (!isEnabled() || !players.get() || !(event.entity instanceof AbstractClientPlayer) || view == null || event.entity == view) return;
        double d2 = event.entity.getDistanceSqToEntity(view);
        double d = playerDetail.get();
        boolean perto = maisPerto == null || maisPerto.contains(event.entity.getEntityId());
        if (d2 <= d * d && perto) return;
        double g = playerGear.get();
        bonecos.simplify(event.renderer, (AbstractClientPlayer) event.entity, d2 > g * g);
    }

    /** Ids dos jogadores mais perto quando tem mais gente que o limite; null = poucos jogadores, todos contam. */
    private java.util.Set<Integer> maisPerto;

    /** Uma vez por tick: com muita gente em volta, separa os mais perto (os únicos que saem completos). */
    private void contarJogadores() {
        maisPerto = null;
        Entity view = mc.getRenderViewEntity();
        if (!isEnabled() || !players.get() || mc.theWorld == null || view == null) return;
        int limite = playerCrowd.getInt();
        java.util.List<EntityPlayer> todos = mc.theWorld.playerEntities;
        if (todos.size() - 1 <= limite) return;
        final Entity v = view;
        java.util.List<EntityPlayer> ordem = new java.util.ArrayList<EntityPlayer>(todos);
        ordem.remove(v);
        java.util.Collections.sort(ordem, new java.util.Comparator<EntityPlayer>() {
            @Override public int compare(EntityPlayer a, EntityPlayer b) {
                return Double.compare(a.getDistanceSqToEntity(v), b.getDistanceSqToEntity(v));
            }
        });
        java.util.Set<Integer> ids = new java.util.HashSet<Integer>();
        for (int i = 0; i < limite && i < ordem.size(); i++) ids.add(ordem.get(i).getEntityId());
        maisPerto = ids;
    }

    /** Quantos bonecos saíram só com a skin (teste automático). */
    public long skinOnlyPlayers() {
        return bonecos.skinOnlyCount();
    }

    @SubscribeEvent
    public void onPlayerPost(RenderLivingEvent.Post event) {
        bonecos.restore();
    }

    private long jogadoresVistos;

    /** Quantos jogadores passaram pelo evento de desenho (teste automático). */
    public long jogadoresVistos() {
        return jogadoresVistos;
    }

    /** Quantos bonecos já saíram simplificados (teste automático). */
    public long simplifiedPlayers() {
        return bonecos.count();
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        bonecos.restore();
        if (event.phase != TickEvent.Phase.END || !isEnabled() || !background.get()) return;
        if (!Display.isActive()) Display.sync(backgroundFps.getInt());
    }

    // ---------- perfis ----------

    /**
     * 0 = Desempenho: tudo no mínimo, para PC fraco ou para ter o máximo de FPS.
     * 1 = Desempenho + Qualidade: visual bonito no que pesa pouco, corta o que pesa muito.
     * 2 = Qualidade+: visual completo; o Motor só corta o que não aparece na tela.
     */
    private void applyProfile(int p) {
        GameSettings gs = mc.gameSettings;
        gs.fancyGraphics = p > 0;
        gs.clouds = p;
        gs.ambientOcclusion = p;
        gs.entityShadows = p == 2;
        gs.particleSetting = 2 - p;
        gs.useVbo = true;
        gs.fboEnable = true;
        gs.enableVsync = false;
        gs.limitFramerate = 260;
        if (p == 0 && gs.renderDistanceChunks > 8) gs.renderDistanceChunks = 8;
        Display.setVSyncEnabled(false);
        int mipmap = p == 0 ? 0 : p == 1 ? 2 : 4;
        if (gs.mipmapLevels != mipmap) gs.setOptionFloatValue(GameSettings.Options.MIPMAP_LEVELS, mipmap);

        particles.set(2 - p);
        appliedParticles = 2 - p;
        occlusion.set(true);
        armorStands.set(p < 2);
        cullMobs.set(true);
        mobDistance.set(p == 0 ? 32 : p == 1 ? 48 : 96);
        itemDistance.set(p == 0 ? 24 : p == 1 ? 32 : 64);
        tileDistance.set(p == 0 ? 32 : p == 1 ? 48 : 64);
        plants.set(p > 0);
        tileOcclusion.set(true);
        signText.set(p == 0 ? 16 : p == 1 ? 24 : 48);
        hidePlants = isEnabled() && !plants.get();
        appliedPlants = hidePlants;
        framesLeves.set(p < 2);
        framesDistance.set(p == 0 ? 16 : p == 1 ? 32 : 64);
        players.set(true);
        playerDetail.set(p == 0 ? 8 : p == 1 ? 24 : 48);
        playerCrowd.set(p == 0 ? 6 : p == 1 ? 10 : 20);
        playerGear.set(p == 0 ? 16 : p == 1 ? 32 : 64);
        limitNames.set(p == 0);
        nameDistance.set(32);
        particleLimit.set(p == 0 ? 2 : p == 1 ? 1 : 0);
        animations.set(p > 0);
        cleanSky.set(p == 0);
        weather.set(p > 0);
        fire.set(p == 2 ? 0 : 1);
        ownPotions.set(p < 2);
        updateSky();

        if (mc.renderGlobal != null && mc.theWorld != null) mc.renderGlobal.loadRenderers();
        gs.saveOptions();
        PulsarClient.get().save();
    }

    // ---------- perfis da pessoa (salvar, copiar e colar) ----------

    private String aviso;
    private long avisoAte;

    @Override
    public String aviso() {
        return System.currentTimeMillis() < avisoAte ? aviso : null;
    }

    private void avisar(String texto) {
        aviso = texto;
        avisoAte = System.currentTimeMillis() + 8000;
        System.out.println("[Pulsar] Motor: " + texto);
    }

    private static File pastaPerfis() {
        return new File(new File(Minecraft.getMinecraft().mcDataDir, "pulsar"), "perfis");
    }

    /** Nome que pode virar arquivo: sem barras nem caracteres que o Windows recusa. */
    static String nomeLimpo(String nome) {
        String n = nome == null ? "" : nome.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
        if (n.length() > 24) n = n.substring(0, 24).trim();
        for (String p : PROFILES) if (p.equalsIgnoreCase(n)) n = n + " (meu)";
        return n.isEmpty() ? "Meu perfil" : n;
    }

    /** Lista do "Perfil": os três prontos e depois os salvos na pasta perfis (arquivos .txt com o código). */
    private void atualizarPerfis() {
        List<String> nomes = new ArrayList<String>(Arrays.asList(PROFILES));
        File[] arquivos = pastaPerfis().listFiles();
        if (arquivos != null) {
            Arrays.sort(arquivos);
            for (File f : arquivos) {
                String n = f.getName();
                if (f.isFile() && n.toLowerCase().endsWith(".txt")) nomes.add(n.substring(0, n.length() - 4));
            }
        }
        profile.setOptions(nomes.toArray(new String[0]));
    }

    /** O perfil atual (opções do Motor e os gráficos do jogo) como texto JSON. */
    public JsonObject perfilAtual(String nome) {
        JsonObject motor = new JsonObject();
        for (Setting st : settings()) {
            if (FORA_DO_PERFIL.contains(st.key()) || st instanceof ActionSetting) continue;
            JsonElement v = st.save();
            if (v != null) motor.add(st.key(), v);
        }
        GameSettings gs = mc.gameSettings;
        JsonObject jogo = new JsonObject();
        jogo.addProperty("graficos_bonitos", gs.fancyGraphics);
        jogo.addProperty("nuvens", gs.clouds);
        jogo.addProperty("luz_suave", gs.ambientOcclusion);
        jogo.addProperty("sombras", gs.entityShadows);
        jogo.addProperty("distancia", gs.renderDistanceChunks);
        jogo.addProperty("mipmap", gs.mipmapLevels);
        jogo.addProperty("fps_max", gs.limitFramerate);
        jogo.addProperty("vsync", gs.enableVsync);
        jogo.addProperty("particulas", gs.particleSetting);
        JsonObject o = new JsonObject();
        o.addProperty("v", 1);
        o.addProperty("nome", nome);
        o.add("motor", motor);
        o.add("jogo", jogo);
        return o;
    }

    /** Perfil virando código: "PULSAR-PERFIL:" + JSON compactado em base64 (cabe numa mensagem do Discord). */
    public static String codificar(JsonObject perfil) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            GZIPOutputStream gz = new GZIPOutputStream(bytes);
            gz.write(perfil.toString().getBytes(StandardCharsets.UTF_8));
            gz.close();
            return CODIGO + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Lê um código (aceita espaços, quebras de linha e texto em volta, como numa mensagem colada). null se não for um perfil. */
    public static JsonObject decodificar(String texto) {
        if (texto == null) return null;
        int i = texto.indexOf(CODIGO);
        if (i < 0) return null;
        // O código pode vir quebrado em linhas e com texto depois ("... valeu!"): junta os pedaços em base64
        // e tenta do mais comprido para o mais curto até um deles abrir.
        String resto = texto.substring(i + CODIGO.length());
        List<String> partes = new ArrayList<String>();
        for (String p : resto.trim().split("\\s+")) {
            int fim = 0;
            while (fim < p.length() && BASE64.indexOf(p.charAt(fim)) >= 0) fim++;
            if (fim > 0) partes.add(p.substring(0, fim));
            if (fim < p.length() || p.endsWith("=")) break;
        }
        for (int k = partes.size(); k > 0; k--) {
            StringBuilder b = new StringBuilder();
            for (int n = 0; n < k; n++) b.append(partes.get(n));
            JsonObject o = abrir(b.toString());
            if (o != null) return o;
        }
        return null;
    }

    private static final String BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private static JsonObject abrir(String base64) {
        try {
            byte[] dados = java.util.Base64.getDecoder().decode(base64);
            GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(dados));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = gz.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 1 << 20) return null;
            }
            JsonElement el = new JsonParser().parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
            return el.isJsonObject() && el.getAsJsonObject().has("motor") && el.getAsJsonObject().get("motor").isJsonObject() ? el.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Aplica um perfil lido de um código ou arquivo. */
    void aplicarPerfil(JsonObject o) {
        JsonObject motor = o.getAsJsonObject("motor");
        for (Setting st : settings()) {
            if (FORA_DO_PERFIL.contains(st.key()) || st instanceof ActionSetting || !motor.has(st.key())) continue;
            try {
                st.load(motor.get(st.key()));
            } catch (Exception ignored) {
            }
        }
        GameSettings gs = mc.gameSettings;
        JsonObject jogo = o.has("jogo") && o.get("jogo").isJsonObject() ? o.getAsJsonObject("jogo") : new JsonObject();
        try {
            if (jogo.has("graficos_bonitos")) gs.fancyGraphics = jogo.get("graficos_bonitos").getAsBoolean();
            if (jogo.has("nuvens")) gs.clouds = clamp(jogo.get("nuvens").getAsInt(), 0, 2);
            if (jogo.has("luz_suave")) gs.ambientOcclusion = clamp(jogo.get("luz_suave").getAsInt(), 0, 2);
            if (jogo.has("sombras")) gs.entityShadows = jogo.get("sombras").getAsBoolean();
            if (jogo.has("distancia")) gs.renderDistanceChunks = clamp(jogo.get("distancia").getAsInt(), 2, 32);
            if (jogo.has("fps_max")) gs.limitFramerate = clamp(jogo.get("fps_max").getAsInt(), 10, 260);
            if (jogo.has("vsync")) {
                gs.enableVsync = jogo.get("vsync").getAsBoolean();
                Display.setVSyncEnabled(gs.enableVsync);
            }
            if (jogo.has("mipmap")) {
                int m = clamp(jogo.get("mipmap").getAsInt(), 0, 4);
                if (gs.mipmapLevels != m) gs.setOptionFloatValue(GameSettings.Options.MIPMAP_LEVELS, m);
            }
        } catch (Exception e) {
            System.err.println("[Pulsar] Motor: parte dos gráficos do perfil não foi lida: " + e);
        }
        appliedParticles = -1;
        hidePlants = isEnabled() && !plants.get();
        appliedPlants = hidePlants;
        updateSky();
        if (mc.renderGlobal != null && mc.theWorld != null) mc.renderGlobal.loadRenderers();
        gs.saveOptions();
        PulsarClient.get().save();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void aplicarEscolhido() {
        int i = profile.index();
        if (i < PROFILES.length) {
            applyProfile(i);
            avisar("Perfil " + profile.get() + " aplicado.");
            return;
        }
        File f = new File(pastaPerfis(), profile.get() + ".txt");
        JsonObject o = null;
        try {
            o = decodificar(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        if (o == null) {
            avisar("Não deu para ler o perfil " + profile.get() + ".");
            return;
        }
        aplicarPerfil(o);
        avisar("Perfil " + profile.get() + " aplicado.");
    }

    /** Salva num arquivo; devolve o nome usado. */
    private String guardar(String nome, JsonObject perfil) {
        String n = nomeLimpo(nome);
        perfil.addProperty("nome", n);
        try {
            File pasta = pastaPerfis();
            pasta.mkdirs();
            Files.write(new File(pasta, n + ".txt").toPath(), codificar(perfil).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            avisar("Não deu para salvar o perfil: " + e.getMessage());
            return null;
        }
        atualizarPerfis();
        for (int i = 0; i < profile.options().length; i++) if (profile.options()[i].equals(n)) profile.set(i);
        PulsarClient.get().save();
        return n;
    }

    private void salvarMeuPerfil() {
        String n = guardar(nomePerfil.get(), perfilAtual(nomePerfil.get()));
        if (n != null) avisar("Perfil " + n + " salvo. Ele aparece na lista \"Perfil\".");
    }

    private void copiarCodigo() {
        String nome = profile.index() >= PROFILES.length ? profile.get() : nomeLimpo(nomePerfil.get());
        GuiScreen.setClipboardString(codificar(perfilAtual(nome)));
        avisar("Código copiado! Cole no Discord para a pessoa, e ela usa \"Colar perfil recebido\".");
    }

    private void colarCodigo() {
        JsonObject o = decodificar(GuiScreen.getClipboardString());
        if (o == null) {
            avisar("Copie primeiro o código do perfil (começa com " + CODIGO + ").");
            return;
        }
        String nome = o.has("nome") ? o.get("nome").getAsString() : "Perfil recebido";
        aplicarPerfil(o);
        String n = guardar(nome, o);
        avisar("Perfil " + (n != null ? n : nome) + " aplicado e salvo na lista.");
    }

    private void apagarEscolhido() {
        if (profile.index() < PROFILES.length) return;
        String n = profile.get();
        if (new File(pastaPerfis(), n + ".txt").delete()) avisar("Perfil " + n + " apagado.");
        atualizarPerfis();
        PulsarClient.get().save();
    }
}
