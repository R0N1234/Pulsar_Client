package dev.pulsarclient.gui;

import dev.pulsarclient.mods.PlacaLeve;
import dev.pulsarclient.mods.AuraEncanto;
import dev.pulsarclient.mods.BarraChefe;
import dev.pulsarclient.mods.BarraPulsar;
import dev.pulsarclient.mods.PacoteAtivo;
import dev.pulsarclient.core.Ganchos;
import dev.pulsarclient.core.MotorHooks;
import net.minecraft.util.ChatComponentText;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockStandingSign;
import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.Category;
import dev.pulsarclient.mods.HudMod;
import dev.pulsarclient.mods.MiraPulsar;
import dev.pulsarclient.mods.GravidadeItens;
import dev.pulsarclient.mods.MolduraLeve;
import dev.pulsarclient.mods.Nebulosa;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.mods.Telescopio;
import dev.pulsarclient.mods.FocarJogador;
import dev.pulsarclient.mods.Sequencias;
import dev.pulsarclient.rede.Rede;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.Setting;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Blocks;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Só roda com -Dpulsar.selftest=true (teste do GitHub Actions).
 * Abre cada tela do Pulsar, troca de conta pela ponte do launcher, cria um mundo plano e testa os mods
 * dentro do jogo, avisando no log para o teste tirar um print de cada etapa.
 */
public class SelfTest {
    private static final int STEP = 60;
    private static final int WORLD_TIMEOUT = 20 * 150;

    private int ticks = -1;
    private int phase;
    private int loadTicks;
    private int inWorld;
    private float sensibilidade;
    private boolean cinemaAntes;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (ticks < 0) {
            if (!(mc.currentScreen instanceof GuiPulsarMenu || mc.currentScreen instanceof GuiMainMenu)) return;
            ticks = 0;
            // Na tela virtual do teste a janela pode não ter foco; sem isso o jogo pausaria sozinho.
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.difficulty = EnumDifficulty.PEACEFUL;
            return;
        }
        ticks++;
        if (phase == 1) {
            waitWorld(mc);
            return;
        }
        if (ticks % STEP != 0) return;
        GuiPulsarMenu menu = new GuiPulsarMenu();
        switch (ticks / STEP) {
            case 1: show("menu", menu); break;
            case 2: show("painel", new GuiPulsarPanel(menu)); break;
            case 3:
                // A barra de rolagem do painel tem que dar para segurar e arrastar com o mouse.
                if (mc.currentScreen instanceof GuiPulsarPanel) log("barra-" + ((GuiPulsarPanel) mc.currentScreen).testarBarra());
                show("motor", new GuiPulsarPanel(menu, Category.MOTOR, PulsarClient.get().motor())); break;
            case 4: show("video", new GuiVideoSettings(menu, mc.gameSettings)); break;
            case 5: show("esc", new GuiIngameMenu()); break;
            case 6: show("hud", new GuiHudEditor(menu)); break;
            case 7:
                // Atalhos sem limite: o botão de adicionar cria o terceiro atalho.
                for (Setting s : PulsarClient.get().mod("atalhos_texto").panelSettings()) {
                    if (s instanceof ActionSetting && s.key().equals("adicionar")) ((ActionSetting) s).run();
                }
                show("atalhos", new GuiPulsarPanel(menu, Category.SERVIDORES, PulsarClient.get().mod("atalhos_texto")));
                break;
            case 8: show("travas", new GuiPulsarPanel(menu, Category.COMBATE, PulsarClient.get().inventario())); break;
            case 9:
                // Tecla repetida de propósito (a mesma do zoom), para o print mostrar o aviso.
                PulsarClient.get().mod("telescopio").toggleKey().set(Keyboard.KEY_C);
                show("teclas", new GuiPulsarPanel(menu, Category.VISUAL, PulsarClient.get().mod("telescopio")));
                break;
            case 10: show("texturas", new GuiTexturas(menu)); break;
            case 11: show("mira", new GuiMiraEditor(menu, (MiraPulsar) PulsarClient.get().mod("mira_pulsar"))); break;
            case 12:
                // Paleta de cores aberta no Contorno.
                PulsarMod contorno = PulsarClient.get().mod("contorno");
                GuiPulsarPanel paleta = GuiPulsarPanel.options(menu, contorno);
                for (Setting s : contorno.settings()) if (s instanceof ColorSetting) paleta.showPicker((ColorSetting) s);
                show("paleta", paleta);
                break;
            case 13:
                mc.displayGuiScreen(menu);
                Conta.trocar();
                show("sugestoes", new GuiSugestoes(menu));
                break;
            case 14: show("conta", menu); break;
            case 15: mc.displayGuiScreen(new GuiContas(menu)); break;
            case 16: Conta.adicionarOffline("NickTeste"); break;
            case 17: log("contas"); break;
            case 18:
                log("criando-mundo");
                mc.launchIntegratedServer("pulsar-teste", "Pulsar Teste",
                        new WorldSettings(2026L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT));
                phase = 1;
                break;
            default: break;
        }
    }

    private void waitWorld(Minecraft mc) {
        if (inWorld == 0 && mc.currentScreen instanceof GuiIngameMenu) mc.displayGuiScreen(null);
        boolean ready = mc.thePlayer != null && mc.theWorld != null && (mc.currentScreen == null || inWorld > 0);
        if (!ready) {
            if (++loadTicks > WORLD_TIMEOUT) {
                log("mundo-falhou");
                finish(mc);
            }
            return;
        }
        // Conta os passos a partir de quando o mundo apareceu.
        int t = ++inWorld;
        if (t % STEP != 0) return;
        switch (t / STEP) {
            case 1: prepareWorld(mc); break;
            case 2:
                log("mundo");
                testarFov(mc);
                // Cedo, para dar tempo de os chunks serem montados de novo antes da conferência (passo 18).
                montarBlocos(mc);
                // Câmera um pouco fora do eixo: olhando exatamente para o sul, a conta do campo de visão do jogo
                // puro (caixa infinita x 0) dá NaN e as placas somem do quadro, um defeito do Minecraft.
                mc.thePlayer.rotationYaw += 4f;
                mc.thePlayer.rotationPitch = 3f;
                break;
            case 3:
                PulsarClient.get().orbita().forceView(true, 150f);
                log("orbita");
                break;
            case 4:
                PulsarClient.get().orbita().forceView(false, 0f);
                if (!PulsarClient.get().inventario().isLocked(0)) PulsarClient.get().inventario().toggleLock(0);
                show("inventario", new GuiInventory(mc.thePlayer));
                break;
            case 5:
                // TAB: no singleplayer a lista só aparece com um placar na lista (criado em prepareWorld).
                mc.displayGuiScreen(null);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindPlayerList.getKeyCode(), true);
                log("tab");
                break;
            case 6:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindPlayerList.getKeyCode(), false);
                PulsarClient.get().mod("cores_vivas").setEnabled(true);
                log("cores");
                break;
            case 7:
                PulsarClient.get().mod("cores_vivas").setEnabled(false);
                setOnFire(mc);
                log("fogo");
                break;
            case 8:
                // Baixa o pacote mais leve do catálogo de verdade (Modrinth) e liga no jogo.
                Texturas.baixar(pacoteTeste());
                log("baixando-textura");
                break;
            case 9:
            case 10:
                if (Texturas.baixado(pacoteTeste()) && pacoteTeste().progresso() < 0) {
                    if (!Texturas.emUso(pacoteTeste())) Texturas.usar(pacoteTeste(), true);
                } else if (t / STEP == 10) {
                    log("textura-falhou");
                }
                break;
            case 11:
                log("textura");
                break;
            case 12:
                show("texturasjogo", new GuiTexturas(null));
                break;
            case 13:
                // A tecla de ligar e desligar escolhida no painel: desliga o Telescópio e o aviso aparece na tela.
                mc.displayGuiScreen(null);
                PulsarClient.get().mod("telescopio").toggleKey().set(Keyboard.KEY_G);
                PulsarClient.get().pressToggleKey(Keyboard.KEY_G);
                log("tecla");
                break;
            case 14:
                // Zoom do Telescópio segurando a tecla; a sensibilidade do jogo não pode mudar em nenhum momento.
                PulsarClient.get().pressToggleKey(Keyboard.KEY_G);
                sensibilidade = mc.gameSettings.mouseSensitivity;
                cinemaAntes = mc.gameSettings.smoothCamera;
                KeyBinding.setKeyBindState(telescopio().key.getKeyCode(), true);
                break;
            case 15:
                // Igual ao OptiFine: com zoom, a câmera cinematográfica liga para a mira ficar suave.
                boolean lenta = telescopio().zoomAtivo() && mc.gameSettings.smoothCamera && mc.gameSettings.mouseSensitivity == sensibilidade;
                log(lenta ? "zoom" : "zoom-sem-mira-lenta");
                break;
            case 16:
                KeyBinding.setKeyBindState(telescopio().key.getKeyCode(), false);
                break;
            case 17:
                boolean voltou = !telescopio().zoomAtivo() && mc.gameSettings.smoothCamera == cinemaAntes && mc.gameSettings.mouseSensitivity == sensibilidade;
                log(voltou ? "zoom-ok" : "zoom-falhou");
                spawnBonecos(mc);
                spawnMolduras(mc);
                // Com a rede no ar: os bonecos viram usuários do Pulsar (ícone PC e nível no nome) e chegam amigos de mentira.
                if (Rede.noAr()) {
                    List<UUID> bonecos = new ArrayList<UUID>();
                    for (int i = 1; i <= 2; i++) bonecos.add(UUID.nameUUIDFromBytes(("Boneco" + i).getBytes()));
                    Rede.get().dadosDeTeste(bonecos);
                }
                break;
            case 18:
                // Um boneco perto (completo) e um longe (otimizado pelo Motor).
                System.out.println("[Pulsar] Molduras leves desenhadas: " + MolduraLeve.desenhadas());
                log(MolduraLeve.desenhadas() > 0 ? "molduras-ok" : "molduras-falhou");
                System.out.println("[Pulsar] Blocos fora dos chunks: " + MotorHooks.skippedBlocks + ", placas sem texto: " + PlacaLeve.semTexto());
                log(MotorHooks.skippedBlocks > 0 ? "plantas-ok" : "plantas-falhou");
                if (PlacaLeve.semTexto() == 0 && placaTeste != null) {
                    TileEntity te = mc.theWorld.getTileEntity(placaTeste);
                    System.out.println("[Pulsar] Placa de teste: bloco " + mc.theWorld.getBlockState(placaTeste) + ", te " + te
                            + ", desenho " + (te == null ? null : net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance.getSpecialRenderer(te))
                            + ", caixa " + (te == null ? null : te.getRenderBoundingBox()) + ", motor " + (te == null ? null : MotorHooks.shouldRenderTile(te))
                            + ", chamadas " + PlacaLeve.chamadas());
                }
                log(PlacaLeve.semTexto() > 0 ? "placas-ok" : "placas-falhou");
                PulsarClient.get().mod("rastro_cometa").setEnabled(true);
                // Rádio Pulsar: o jogo pede para o launcher tocar (o e2e confere o pedido na ponte).
                PulsarClient.get().mod("radio_pulsar").setEnabled(true);
                break;
            case 19:
                show("hudjogo", new GuiHudEditor(null));
                break;
            case 20:
                if (Rede.noAr()) show("amigos", new GuiAmigos(null).escolher("a1", "AnaPvP"));
                break;
            case 21:
                // Gravidade de Itens: itens jogados na frente do jogador, deitados no chão.
                mc.displayGuiScreen(null);
                PulsarClient.get().mod("gravidade_itens").setEnabled(true);
                spawnItens(mc);
                // Mods novos da 0.8.0: Aura de Encanto (espada encantada na mão e a armadura do boneco),
                // Pacote Ativo e Barra de Chefe (com um chefe de mentira). A Barra Pulsar já está ligada.
                PulsarClient.get().mod("aura_encanto").setEnabled(true);
                PulsarClient.get().mod("pacote_ativo").setEnabled(true);
                PulsarClient.get().mod("barra_chefe").setEnabled(true);
                ItemStack espada = new ItemStack(Items.diamond_sword);
                espada.addEnchantment(Enchantment.sharpness, 1);
                mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, espada);
                net.minecraft.entity.boss.BossStatus.bossName = "Chefe de Teste";
                net.minecraft.entity.boss.BossStatus.healthScale = 0.6f;
                net.minecraft.entity.boss.BossStatus.statusBarTime = 100000;
                log("hudnovo");
                break;
            case 22:
                log(GravidadeItens.desenhados() > 0 ? "itens" : "itens-falhou");
                // Bonecos conferidos só aqui: com o OptiFine o boneco longe às vezes demora alguns quadros para aparecer.
                System.out.println("[Pulsar] Bonecos: " + PulsarClient.get().motor().jogadoresVistos() + " desenhos de jogador, " + PulsarClient.get().motor().simplifiedPlayers() + " simplificados");
                if (PulsarClient.get().motor().simplifiedPlayers() == 0) {
                    net.minecraft.entity.Entity b2 = mc.theWorld.getEntityByID(-4243);
                    System.out.println("[Pulsar] Boneco2: " + b2 + (b2 == null ? "" : ", morto " + b2.isDead + ", no mundo " + mc.theWorld.loadedEntityList.contains(b2)
                            + ", distância " + b2.getDistanceToEntity(mc.thePlayer) + ", motor " + MotorHooks.shouldRenderEntity(b2)
                            + ", desenho " + mc.getRenderManager().getEntityRenderObject(b2) + ", olho " + mc.thePlayer.getPositionEyes(1f)));
                }
                log(PulsarClient.get().motor().simplifiedPlayers() > 0 ? "bonecos" : "bonecos-falhou");
                // O boneco a 20 blocos passa da distância da armadura no Desempenho (16): sai só com a skin.
                log(PulsarClient.get().motor().skinOnlyPlayers() > 0 ? "bonecos-pele" : "bonecos-pele-falhou");
                System.out.println("[Pulsar] Aura de Encanto: " + AuraEncanto.ganchos() + ", itens " + Ganchos.encantoItens + ", armaduras " + Ganchos.encantoArmaduras);
                log(Ganchos.encantoItens > 0 ? "encanto-ok" : "encanto-falhou");
                log(PacoteAtivo.desenhos() > 0 ? "pacote-ok" : "pacote-falhou");
                log(BarraChefe.desenhos() > 0 ? "chefe-ok" : "chefe-falhou");
                log(BarraPulsar.desenhosComMusica() > 0 ? "musica-ok" : "musica-falhou");
                log(Universo.quadros() > 0 ? "universo-ok" : "universo-falhou");
                net.minecraft.entity.boss.BossStatus.statusBarTime = 0;
                PulsarClient.get().mod("aura_encanto").setEnabled(false);
                PulsarClient.get().mod("pacote_ativo").setEnabled(false);
                PulsarClient.get().mod("barra_chefe").setEnabled(false);
                break;
            case 23:
                // Nebulosa: o menu do ESC com o mundo desfocado atrás.
                PulsarClient.get().mod("gravidade_itens").setEnabled(false);
                PulsarClient.get().mod("nebulosa").setEnabled(true);
                show("nebulosa", new GuiIngameMenu());
                break;
            case 24:
                log(Nebulosa.vezes() > 0 ? "nebulosa-ok" : "nebulosa-falhou");
                mc.displayGuiScreen(null);
                PulsarClient.get().mod("nebulosa").setEnabled(false);
                // Capturas: uma print como a do F2; o mod acha na pasta, copia e mostra os botões no chat.
                ScreenShotHelper.saveScreenshot(mc.mcDataDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                break;
            case 25:
                show("capturas", new GuiChat());
                break;
            case 26:
                // Cosméticos: a loja com o próprio boneco usando a Órbita Pulsar.
                dev.pulsarclient.cosmeticos.Cosmeticos.get().liberarTudoParaTeste();
                dev.pulsarclient.cosmeticos.Cosmeticos.get().usarAcessorio("orbita");
                show("cosmeticos", new dev.pulsarclient.cosmeticos.GuiCosmeticos(null, null));
                break;
            case 27:
                // Supernova: o print sai no meio do emote, com os braços para cima e as estrelas.
                mc.displayGuiScreen(null);
                log(dev.pulsarclient.cosmeticos.Cosmeticos.get().tocar("supernova") ? "emote" : "emote-falhou");
                break;
            case 28:
                dev.pulsarclient.cosmeticos.Cosmeticos.get().usarAcessorio("coroa");
                dev.pulsarclient.cosmeticos.Cosmeticos.get().tocar("facepalm");
                log("coroa");
                break;
            case 29:
                // As asas ficam nas costas: câmera por trás.
                dev.pulsarclient.cosmeticos.Cosmeticos.get().usarAcessorio("asas");
                dev.pulsarclient.cosmeticos.Cosmeticos.get().tocar("danca");
                mc.gameSettings.thirdPersonView = 1;
                log("asas");
                break;
            case 30:
                System.out.println("[Pulsar] Cosméticos: " + dev.pulsarclient.cosmeticos.Cosmeticos.poses() + " poses de emote, "
                        + dev.pulsarclient.cosmeticos.Cosmeticos.desenhos() + " acessórios desenhados, gancho " + Ganchos.ganchoEmote);
                log(Ganchos.ganchoEmote && dev.pulsarclient.cosmeticos.Cosmeticos.poses() > 0 ? "emote-ok" : "emote-falhou");
                log(dev.pulsarclient.cosmeticos.Cosmeticos.desenhos() > 0 ? "acessorio-ok" : "acessorio-falhou");
                dev.pulsarclient.cosmeticos.Cosmeticos.get().parar();
                dev.pulsarclient.cosmeticos.Cosmeticos.get().usarAcessorio("orbita");
                mc.gameSettings.thirdPersonView = 0;
                break;
            case 31:
                // Kit PvP: um baú com peitoral P4, poção splash de velocidade, maçãs e uma espada de ferro fora do layout.
                montarKit(mc);
                break;
            case 32:
                // O baú aberto com a sombra do layout nos espaços vazios do inventário.
                log(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiChest ? "kit" : "kit-sem-bau");
                break;
            case 33:
                // Shift + botão direito em cada item do baú, como a pessoa faria.
                if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
                    net.minecraft.client.gui.inventory.GuiContainer bau = (net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen;
                    int levados = 0;
                    for (Object o : new ArrayList<Object>(bau.inventorySlots.inventorySlots)) {
                        net.minecraft.inventory.Slot s = (net.minecraft.inventory.Slot) o;
                        if (s.inventory != mc.thePlayer.inventory && s.getHasStack() && kit().levar(bau, s)) levados++;
                    }
                    System.out.println("[Pulsar] Kit PvP: " + levados + " itens levados ao lugar");
                }
                log("organizado");
                break;
            case 34:
                conferirKit(mc);
                mc.thePlayer.closeScreen();
                break;
            case 35:
                show("kiteditor", new GuiKitPvp(null, kit()));
                break;
            case 36:
                // Anti-Batata: regeneração de 5 s; um passo (3 s) depois a contagem e o pisca estão na tela.
                mc.displayGuiScreen(null);
                antiBatata().setEnabled(true);
                antiBatata().prepararTeste(false);
                darRegeneracao(mc, 100, false);
                break;
            case 37:
                log("batata");
                log(dev.pulsarclient.mods.AntiBatata.avisos() > 0 ? "batata-aviso-ok" : "batata-aviso-falhou");
                log(dev.pulsarclient.mods.AntiBatata.piscadas() > 0 ? "batata-pisca-ok" : "batata-pisca-falhou");
                break;
            case 38:
                // Comer sozinho: espada na mão (espaço 1), 3 maçãs no espaço 2, batendo, regeneração de 1,5 s.
                antiBatata().prepararTeste(true);
                antiBatata().simularCombate(STEP);
                mc.thePlayer.inventory.currentItem = 0;
                darRegeneracao(mc, 30, true);
                break;
            case 40:
                conferirBatata(mc);
                antiBatata().prepararTeste(false);
                break;
            case 41:
                // Filtro de Coleta: tudo vermelho menos o kit de diamante (o exemplo do Rick); itens caem no pé do jogador.
                filtro().setEnabled(true);
                filtro().marcarTodos(true);
                for (Item i : new Item[] {Items.diamond_helmet, Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_boots}) {
                    filtro().marcar(new ItemStack(i), false);
                }
                soltarItensFiltro(mc);
                break;
            case 42:
                conferirFiltro(mc);
                break;
            case 43:
                show("filtro", new GuiFiltroColeta(null, filtro()));
                break;
            case 44:
                show("batatapainel", GuiPulsarPanel.options(null, antiBatata()));
                break;
            case 45:
                // Focar Jogador: você bate no Boneco1 (3 blocos à frente), o aviso aparece com a cabeça e a tecla.
                mc.displayGuiScreen(null);
                focar().setEnabled(true);
                if (boneco(mc) != null) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(mc.thePlayer, boneco(mc)));
                break;
            case 46:
                log(focar().avisando() ? "focar-aviso" : "focar-aviso-falhou");
                // A tecla: o boneco fica focado, com o nick na cor e a caixa em volta.
                focar().focar(boneco(mc));
                break;
            case 47:
                System.out.println("[Pulsar] Focar Jogador: caixa " + FocarJogador.desenhosCaixa() + ", nome " + FocarJogador.desenhosNome());
                log(focar().focado() && FocarJogador.desenhosCaixa() > 0 && FocarJogador.desenhosNome() > 0 ? "focar-ok" : "focar-falhou");
                // Sequências: bate no boneco e ele morre; a voz vem do jar e um .ogg da pasta substitui a próxima.
                focar().focar(null);
                sequencias().setEnabled(true);
                copiarSomLocal(mc);
                if (boneco(mc) != null) {
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(mc.thePlayer, boneco(mc)));
                    boneco(mc).setHealth(0f);
                }
                break;
            case 48:
                log("sequencia");
                boolean somJar = mc.getSoundHandler().getSound(new net.minecraft.util.ResourceLocation("pulsar", "sequencias.first_blood")) != null;
                boolean somLocal = false;
                try {
                    somLocal = "Sons do Pulsar".equals(mc.getResourceManager().getResource(new net.minecraft.util.ResourceLocation("pulsar", "sounds/sequencias/double_kill.ogg")).getResourcePackName());
                } catch (Exception e) {
                    System.out.println("[Pulsar] selftest: som local não achado: " + e);
                }
                System.out.println("[Pulsar] Sequências: abates " + Sequencias.abates() + ", desenhos " + Sequencias.desenhos() + ", som do jar " + somJar + ", som local " + somLocal);
                log(Sequencias.abates() >= 1 && Sequencias.desenhos() > 0 && somJar ? "sequencia-ok" : "sequencia-falhou");
                log(somLocal ? "sons-locais-ok" : "sons-locais-falhou");
                // Segundo abate dentro do intervalo: DOUBLE KILL (toca o .ogg da pasta).
                sequencias().abate();
                break;
            case 49:
                log(sequencias().contagem() == 2 ? "sequencia-dupla-ok" : "sequencia-dupla-falhou");
                sequencias().setEnabled(false);
                focar().setEnabled(false);
                break;
            case 50:
                // Desliga o rádio um passo antes do fim: o pedido de parar sai em até meio segundo.
                filtro().setEnabled(false);
                PulsarClient.get().mod("rastro_cometa").setEnabled(false);
                PulsarClient.get().mod("radio_pulsar").setEnabled(false);
                mc.displayGuiScreen(null);
                break;
            case 51:
                finish(mc);
                break;
            default: break;
        }
    }

    private static dev.pulsarclient.mods.AntiBatata antiBatata() {
        return (dev.pulsarclient.mods.AntiBatata) PulsarClient.get().mod("anti_batata");
    }

    private static dev.pulsarclient.mods.FiltroColeta filtro() {
        return (dev.pulsarclient.mods.FiltroColeta) PulsarClient.get().mod("filtro_coleta");
    }

    /** Regeneração II no jogador pelo servidor; com maçãs, também espada no espaço 1 e 3 maçãs no espaço 2 (barra limpa). */
    private void darRegeneracao(Minecraft mc, final int duracao, final boolean comMacas) {
        final IntegratedServer server = mc.getIntegratedServer();
        final UUID id = mc.thePlayer.getUniqueID();
        if (server == null) return;
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                EntityPlayerMP p = server.getConfigurationManager().getPlayerByUUID(id);
                if (p == null) return;
                if (comMacas) {
                    for (int i = 0; i < 36; i++) {
                        ItemStack s = p.inventory.getStackInSlot(i);
                        if (i < 9 || (s != null && s.getItem() == Items.golden_apple)) p.inventory.setInventorySlotContents(i, null);
                    }
                    p.inventory.setInventorySlotContents(0, new ItemStack(Items.diamond_sword));
                    p.inventory.setInventorySlotContents(1, new ItemStack(Items.golden_apple, 3));
                }
                p.removePotionEffect(Potion.regeneration.id);
                p.addPotionEffect(new PotionEffect(Potion.regeneration.id, duracao, 1));
            }
        });
    }

    /** Confere no servidor que uma maçã foi comida (sobraram 2), a regeneração foi renovada e a espada voltou para a mão. */
    private void conferirBatata(Minecraft mc) {
        IntegratedServer server = mc.getIntegratedServer();
        EntityPlayerMP p = server == null ? null : server.getConfigurationManager().getPlayerByUUID(mc.thePlayer.getUniqueID());
        if (p == null) {
            log("batata-falhou");
            return;
        }
        int macas = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == Items.golden_apple) macas += s.stackSize;
        }
        PotionEffect regen = p.getActivePotionEffect(Potion.regeneration);
        boolean espada = mc.thePlayer.inventory.currentItem == 0;
        System.out.println("[Pulsar] Anti-Batata no servidor: maçãs " + macas + ", regeneração " + (regen == null ? "nenhuma" : regen.getDuration())
                + ", espaço na mão " + (mc.thePlayer.inventory.currentItem + 1) + ", comidas " + dev.pulsarclient.mods.AntiBatata.comidas());
        log(macas == 2 && regen != null && espada && dev.pulsarclient.mods.AntiBatata.comidas() >= 1 ? "batata-ok" : "batata-falhou");
    }

    /** Inventário só com a espada e itens de PvP caindo no pé do jogador (pegos depois de 5 ticks). */
    private void soltarItensFiltro(Minecraft mc) {
        final IntegratedServer server = mc.getIntegratedServer();
        final UUID id = mc.thePlayer.getUniqueID();
        if (server == null) return;
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                EntityPlayerMP p = server.getConfigurationManager().getPlayerByUUID(id);
                if (p == null) return;
                for (int i = 0; i < 36; i++) p.inventory.setInventorySlotContents(i, null);
                // Espada vermelha que já estava no inventário: nunca é jogada fora.
                p.inventory.setInventorySlotContents(0, new ItemStack(Items.diamond_sword));
                ItemStack[] chao = {new ItemStack(Items.diamond_helmet), new ItemStack(Items.iron_chestplate), new ItemStack(Items.potionitem, 1, 16418),
                        new ItemStack(Items.golden_apple, 2), new ItemStack(Blocks.cobblestone, 16), new ItemStack(Items.bowl)};
                for (ItemStack s : chao) {
                    EntityItem e = new EntityItem(p.worldObj, p.posX, p.posY + 0.2, p.posZ, s);
                    e.motionX = e.motionY = e.motionZ = 0;
                    e.setPickupDelay(5);
                    p.worldObj.spawnEntityInWorld(e);
                }
            }
        });
    }

    /** Confere no servidor: capacete, pedra e espada ficaram; peitoral de ferro, poção, maçãs e tigela foram jogados fora. */
    private void conferirFiltro(Minecraft mc) {
        IntegratedServer server = mc.getIntegratedServer();
        EntityPlayerMP p = server == null ? null : server.getConfigurationManager().getPlayerByUUID(mc.thePlayer.getUniqueID());
        if (p == null) {
            log("filtro-falhou");
            return;
        }
        boolean capacete = false, pedra = false, espada = false, lixo = false;
        StringBuilder tem = new StringBuilder();
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s == null) continue;
            tem.append(s.stackSize).append("x ").append(s.getItem().getUnlocalizedName()).append("; ");
            if (s.getItem() == Items.diamond_helmet) capacete = true;
            else if (s.getItem() == Item.getItemFromBlock(Blocks.cobblestone) && s.stackSize == 16) pedra = true;
            else if (s.getItem() == Items.diamond_sword) espada = true;
            else if (s.getItem() == Items.iron_chestplate || s.getItem() == Items.potionitem || s.getItem() == Items.golden_apple || s.getItem() == Items.bowl) lixo = true;
        }
        System.out.println("[Pulsar] Filtro de Coleta no servidor: " + tem + "descartados " + dev.pulsarclient.mods.FiltroColeta.descartados());
        log(capacete && pedra && espada && !lixo && dev.pulsarclient.mods.FiltroColeta.descartados() >= 4 ? "filtro-ok" : "filtro-falhou");
    }

    private static FocarJogador focar() {
        return (FocarJogador) PulsarClient.get().mod("focar_jogador");
    }

    private static Sequencias sequencias() {
        return (Sequencias) PulsarClient.get().mod("sequencias");
    }

    /** O Boneco1 (jogador de mentira a 3 blocos), ou null. */
    private static net.minecraft.entity.player.EntityPlayer boneco(Minecraft mc) {
        net.minecraft.entity.Entity e = mc.theWorld.getEntityByID(-4242);
        return e instanceof net.minecraft.entity.player.EntityPlayer ? (net.minecraft.entity.player.EntityPlayer) e : null;
    }

    /** Copia a voz do jar para a pasta pulsar/sons/sequencias como double_kill.ogg: o jogo tem que ler a da pasta. */
    private void copiarSomLocal(Minecraft mc) {
        try {
            java.io.File destino = new java.io.File(dev.pulsarclient.mods.SonsLocais.pasta(), "sequencias/double_kill.ogg");
            destino.getParentFile().mkdirs();
            java.io.InputStream in = SelfTest.class.getResourceAsStream("/assets/pulsar/sounds/sequencias/godlike.ogg");
            java.io.OutputStream out = new java.io.FileOutputStream(destino);
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            in.close();
            out.close();
        } catch (Exception e) {
            System.out.println("[Pulsar] selftest: não copiei o som local: " + e);
        }
    }

    private static dev.pulsarclient.mods.KitPvp kit() {
        return (dev.pulsarclient.mods.KitPvp) PulsarClient.get().mod("kit_pvp");
    }

    /** Layout do teste: espada no 1, maçãs no 3, splash de velocidade no 9 e o kit de diamante na primeira fileira. */
    private void montarKit(Minecraft mc) {
        ItemStack[] l = new ItemStack[dev.pulsarclient.mods.KitPvp.ESPACOS];
        l[0] = new ItemStack(Items.diamond_sword);
        l[2] = new ItemStack(Items.golden_apple);
        l[8] = new ItemStack(Items.potionitem, 1, 16418);
        l[9] = new ItemStack(Items.diamond_helmet);
        l[10] = new ItemStack(Items.diamond_chestplate);
        l[11] = new ItemStack(Items.diamond_leggings);
        l[12] = new ItemStack(Items.diamond_boots);
        l[7] = new ItemStack(Items.potionitem, 1, 8233);
        kit().usarKit(0);
        kit().salvar(0, l);
        final IntegratedServer server = mc.getIntegratedServer();
        final UUID id = mc.thePlayer.getUniqueID();
        final BlockPos pos = new BlockPos((int) Math.floor(mc.thePlayer.posX) - 2, (int) Math.floor(mc.thePlayer.posY), (int) Math.floor(mc.thePlayer.posZ));
        if (server == null) return;
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                EntityPlayerMP p = server.getConfigurationManager().getPlayerByUUID(id);
                net.minecraft.world.WorldServer ws = server.worldServerForDimension(0);
                if (p == null) return;
                for (int i = 0; i < 36; i++) p.inventory.setInventorySlotContents(i, null);
                // O que já estava no lugar do peitoral vai ter que sair para outro espaço.
                p.inventory.setInventorySlotContents(10, new ItemStack(Blocks.cobblestone, 32));
                ws.setBlockState(pos, Blocks.chest.getDefaultState());
                TileEntity te = ws.getTileEntity(pos);
                if (!(te instanceof net.minecraft.tileentity.TileEntityChest)) {
                    System.out.println("[Pulsar] selftest: kit sem baú (" + te + ")");
                    return;
                }
                net.minecraft.tileentity.TileEntityChest bau = (net.minecraft.tileentity.TileEntityChest) te;
                ItemStack peito = new ItemStack(Items.diamond_chestplate);
                peito.addEnchantment(Enchantment.protection, 4);
                peito.addEnchantment(Enchantment.unbreaking, 3);
                bau.setInventorySlotContents(0, peito);
                bau.setInventorySlotContents(4, new ItemStack(Items.potionitem, 1, 16418));
                bau.setInventorySlotContents(13, new ItemStack(Items.golden_apple, 8));
                bau.setInventorySlotContents(20, new ItemStack(Items.iron_sword));
                p.displayGUIChest(bau);
            }
        });
    }

    /** Confere no servidor (não só na tela) que cada item foi para o espaço do layout e a espada de ferro ficou no baú. */
    private void conferirKit(Minecraft mc) {
        IntegratedServer server = mc.getIntegratedServer();
        EntityPlayerMP p = server == null ? null : server.getConfigurationManager().getPlayerByUUID(mc.thePlayer.getUniqueID());
        if (p == null) {
            log("kit-falhou");
            return;
        }
        ItemStack peito = p.inventory.getStackInSlot(10), pocao = p.inventory.getStackInSlot(8), macas = p.inventory.getStackInSlot(2);
        boolean pedra = false;
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == Item.getItemFromBlock(Blocks.cobblestone) && s.stackSize == 32) pedra = true;
        }
        boolean ok = peito != null && peito.getItem() == Items.diamond_chestplate && peito.isItemEnchanted()
                && pocao != null && pocao.getItem() == Items.potionitem && pocao.getMetadata() == 16418
                && macas != null && macas.getItem() == Items.golden_apple && macas.stackSize == 8
                && pedra && dev.pulsarclient.mods.KitPvp.movidos() >= 3;
        System.out.println("[Pulsar] Kit PvP no servidor: peitoral " + peito + ", poção " + pocao + ", maçãs " + macas + ", pedra guardada " + pedra
                + ", movidos " + dev.pulsarclient.mods.KitPvp.movidos());
        log(ok ? "kit-ok" : "kit-falhou");
    }

    /** Dois jogadores de mentira na frente da câmera: um a 3 blocos e outro a 20, com armadura encantada. */
    private void spawnBonecos(Minecraft mc) {
        float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
        double[] dist = {3, 20};
        for (int i = 0; i < dist.length; i++) {
            String nome = "Boneco" + (i + 1);
            EntityOtherPlayerMP p = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(UUID.nameUUIDFromBytes(nome.getBytes()), nome));
            double x = mc.thePlayer.posX - Math.sin(yaw) * dist[i] + (i == 0 ? 1.2 : -2);
            double z = mc.thePlayer.posZ + Math.cos(yaw) * dist[i];
            p.setPositionAndRotation(x, mc.thePlayer.posY, z, mc.thePlayer.rotationYaw + 180f, 0f);
            p.rotationYawHead = mc.thePlayer.rotationYaw + 180f;
            ItemStack peito = new ItemStack(Items.diamond_chestplate);
            peito.addEnchantment(Enchantment.protection, 1);
            p.setCurrentItemOrArmor(0, new ItemStack(Items.diamond_sword));
            p.setCurrentItemOrArmor(3, peito);
            mc.theWorld.addEntityToWorld(-4242 - i, p);
        }
    }

    /** Propulsão com "Tirar o zoom da tela ao correr": correndo, o FOV fica 1 (sem a opção, aumenta). */
    private void testarFov(Minecraft mc) {
        BoolSetting semZoom = null;
        for (Setting s : PulsarClient.get().mod("propulsao").settings()) if (s.key().equals("sem_zoom")) semZoom = (BoolSetting) s;
        if (semZoom == null) {
            log("fov-falhou");
            return;
        }
        boolean antes = semZoom.get();
        boolean correndo = mc.thePlayer.isSprinting();
        mc.thePlayer.setSprinting(true);
        semZoom.set(false);
        float normal = mc.thePlayer.getFovModifier();
        semZoom.set(true);
        float fixo = mc.thePlayer.getFovModifier();
        semZoom.set(antes);
        mc.thePlayer.setSprinting(correndo);
        System.out.println("[Pulsar] FOV correndo: normal " + normal + ", sem zoom " + fixo);
        log(normal > 1.01f && Math.abs(fixo - 1f) < 0.001f ? "fov-ok" : "fov-falhou");
    }

    /** Três molduras com itens (espada encantada, bússola e maçã dourada) à direita, para o Motor desenhar no modo leve. */
    private void spawnMolduras(Minecraft mc) {
        ItemStack espada = new ItemStack(Items.diamond_sword);
        espada.addEnchantment(Enchantment.sharpness, 2);
        ItemStack[] itens = {espada, new ItemStack(Items.compass), new ItemStack(Items.golden_apple)};
        int bx = (int) Math.floor(mc.thePlayer.posX), by = (int) Math.floor(mc.thePlayer.posY) + 1, bz = (int) Math.floor(mc.thePlayer.posZ);
        for (int i = 0; i < itens.length; i++) {
            // O jogador olha para o sul (+Z): a moldura fica de frente para ele, presa no bloco de trás.
            EntityItemFrame m = new EntityItemFrame(mc.theWorld, new BlockPos(bx - 2 - i, by, bz + 5), EnumFacing.NORTH);
            m.setDisplayedItem(itens[i]);
            mc.theWorld.addEntityToWorld(-5000 - i, m);
        }
    }

    /**
     * Blocos do Motor: grama alta e flores à direita (no Desempenho saem da montagem dos chunks)
     * e uma placa com texto a uns 30 blocos (longe demais para o texto, perto o bastante para a placa).
     * A grama só no mundo do cliente; a placa pelo servidor.
     */
    private BlockPos placaTeste;

    private void montarBlocos(Minecraft mc) {
        int bx = (int) Math.floor(mc.thePlayer.posX), by = (int) Math.floor(mc.thePlayer.posY), bz = (int) Math.floor(mc.thePlayer.posZ);
        mc.theWorld.setBlockState(new BlockPos(bx + 2, by, bz + 3), Blocks.tallgrass.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS));
        mc.theWorld.setBlockState(new BlockPos(bx + 3, by, bz + 3), Blocks.red_flower.getDefaultState());
        mc.theWorld.setBlockState(new BlockPos(bx + 4, by, bz + 3), Blocks.yellow_flower.getDefaultState());
        // Em outro chunk que o do boneco longe: o servidor reenviando o chunk apagaria o boneco de mentira do OptiFine.
        final BlockPos placa = new BlockPos(bx + 12, by, bz + 28);
        placaTeste = placa;
        // A placa vem do servidor, como num servidor de verdade (o bloco e o texto chegam por pacote).
        final net.minecraft.world.WorldServer ws = mc.getIntegratedServer().worldServerForDimension(0);
        mc.getIntegratedServer().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ws.setBlockState(placa, Blocks.standing_sign.getDefaultState().withProperty(BlockStandingSign.ROTATION, 8));
                TileEntity te = ws.getTileEntity(placa);
                if (te instanceof TileEntitySign) {
                    ((TileEntitySign) te).signText[0] = new ChatComponentText("Pulsar");
                    ((TileEntitySign) te).signText[1] = new ChatComponentText("Client");
                    te.markDirty();
                    ws.markBlockForUpdate(placa);
                } else {
                    System.out.println("[Pulsar] selftest: placa sem bloco de placa (" + te + ")");
                }
            }
        });
    }

    /** Uma pilha de flechas, uma espada e um bloco no chão, a 2 blocos do jogador. */
    private void spawnItens(Minecraft mc) {
        ItemStack[] itens = {new ItemStack(Items.arrow, 64), new ItemStack(Items.diamond_sword), new ItemStack(Blocks.gold_block, 5)};
        for (int i = 0; i < itens.length; i++) {
            EntityItem e = new EntityItem(mc.theWorld, mc.thePlayer.posX - 0.8 + i * 0.8, mc.thePlayer.posY, mc.thePlayer.posZ + 3.5, itens[i]);
            e.motionX = e.motionY = e.motionZ = 0;
            e.setInfinitePickupDelay();
            mc.theWorld.addEntityToWorld(-6000 - i, e);
        }
    }

    private static Telescopio telescopio() {
        return (Telescopio) PulsarClient.get().mod("telescopio");
    }

    private static Texturas.Pacote pacoteTeste() {
        for (Texturas.Pacote p : Texturas.catalogo()) if (p.id.equals("f8thful")) return p;
        return Texturas.catalogo().get(0);
    }

    /** Fogo no jogador pelo servidor do mundo (no cliente o fogo é apagado a cada tick), com resistência ao fogo. */
    private void setOnFire(Minecraft mc) {
        final IntegratedServer server = mc.getIntegratedServer();
        final UUID id = mc.thePlayer.getUniqueID();
        if (server == null) return;
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                EntityPlayerMP p = server.getConfigurationManager().getPlayerByUUID(id);
                if (p == null) return;
                p.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 600, 0));
                p.setFire(10);
            }
        });
    }

    /** Mundo do teste sem monstros: um slime matava o jogador no meio dos prints. (Sem dano não dá: o jogo apaga o fogo.) */
    private void protegerJogador(Minecraft mc) {
        final IntegratedServer server = mc.getIntegratedServer();
        if (server == null) return;
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                server.setDifficultyForAllWorlds(EnumDifficulty.PEACEFUL);
            }
        });
    }

    /** Liga todos os mods de HUD, dá itens e efeitos (só no cliente) para os prints mostrarem tudo. */
    private void prepareWorld(Minecraft mc) {
        protegerJogador(mc);
        for (HudMod m : PulsarClient.get().hudMods()) m.setEnabled(true);
        // HUD do FPS em 150% e a mira desenhada, para os prints.
        ((HudMod) PulsarClient.get().mod("fps")).resize(5);
        MiraPulsar mira = (MiraPulsar) PulsarClient.get().mod("mira_pulsar");
        mira.useDrawing();
        mira.setEnabled(true);
        mc.thePlayer.inventory.setInventorySlotContents(0, new ItemStack(Items.diamond_sword));
        mc.thePlayer.inventory.setInventorySlotContents(1, new ItemStack(Items.ender_pearl, 16));
        mc.thePlayer.inventory.setInventorySlotContents(2, new ItemStack(Items.golden_apple, 8));
        mc.thePlayer.inventory.setInventorySlotContents(3, new ItemStack(Items.arrow, 64));
        mc.thePlayer.inventory.armorInventory[3] = new ItemStack(Items.diamond_helmet);
        mc.thePlayer.inventory.armorInventory[2] = new ItemStack(Items.diamond_chestplate);
        mc.thePlayer.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 6000, 1));
        // Nível 255 chega como -1: era o que fechava o jogo na 0.4.0.
        mc.thePlayer.addPotionEffect(new PotionEffect(Potion.jump.id, 6000, -1));
        // Placar de servidor de mentira (só no cliente), para o Placar e a lista do TAB aparecerem.
        Scoreboard sb = mc.theWorld.getScoreboard();
        ScoreObjective o = sb.getObjective("pulsar");
        if (o == null) o = sb.addScoreObjective("pulsar", IScoreObjectiveCriteria.DUMMY);
        o.setDisplayName("§d§lPULSAR");
        sb.setObjectiveInDisplaySlot(0, o);
        sb.setObjectiveInDisplaySlot(1, o);
        String[] linhas = {"§7Mapa: §fÓrbita", "§7Abates: §a12", "§7Jogadores: §f8", "§bpulsarclient"};
        for (int i = 0; i < linhas.length; i++) sb.getValueFromObjective(linhas[i], o).setScorePoints(linhas.length - i);
        log("mundo-pronto");
    }

    private void finish(Minecraft mc) {
        phase = 2;
        log("fim");
    }

    private static void show(String name, net.minecraft.client.gui.GuiScreen screen) {
        Minecraft.getMinecraft().displayGuiScreen(screen);
        log(name);
    }

    private static void log(String step) {
        System.out.println("[Pulsar] selftest: " + step);
    }
}
