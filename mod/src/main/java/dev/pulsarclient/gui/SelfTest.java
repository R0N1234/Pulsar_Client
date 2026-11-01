package dev.pulsarclient.gui;

import dev.pulsarclient.mods.PlacaLeve;
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
                KeyBinding.setKeyBindState(telescopio().key.getKeyCode(), true);
                break;
            case 15:
                boolean lenta = telescopio().mouseScale() < 0.99 && mc.gameSettings.mouseSensitivity == sensibilidade;
                log(lenta ? "zoom" : "zoom-sem-mira-lenta");
                break;
            case 16:
                KeyBinding.setKeyBindState(telescopio().key.getKeyCode(), false);
                break;
            case 17:
                boolean voltou = telescopio().mouseScale() >= 0.999 && mc.gameSettings.mouseSensitivity == sensibilidade;
                log(voltou ? "zoom-ok" : "zoom-falhou");
                spawnBonecos(mc);
                spawnMolduras(mc);
                montarBlocos(mc);
                // Com a rede no ar: os bonecos viram usuários do Pulsar (ícone PC e nível no nome) e chegam amigos de mentira.
                if (Rede.noAr()) {
                    List<UUID> bonecos = new ArrayList<UUID>();
                    for (int i = 1; i <= 2; i++) bonecos.add(UUID.nameUUIDFromBytes(("Boneco" + i).getBytes()));
                    Rede.get().dadosDeTeste(bonecos);
                }
                break;
            case 18:
                // Um boneco perto (completo) e um longe (otimizado pelo Motor).
                System.out.println("[Pulsar] Bonecos: " + PulsarClient.get().motor().jogadoresVistos() + " desenhos de jogador, " + PulsarClient.get().motor().simplifiedPlayers() + " simplificados");
                log(PulsarClient.get().motor().simplifiedPlayers() > 0 ? "bonecos" : "bonecos-falhou");
                System.out.println("[Pulsar] Molduras leves desenhadas: " + MolduraLeve.desenhadas());
                log(MolduraLeve.desenhadas() > 0 ? "molduras-ok" : "molduras-falhou");
                System.out.println("[Pulsar] Blocos fora dos chunks: " + MotorHooks.skippedBlocks + ", placas sem texto: " + PlacaLeve.semTexto());
                log(MotorHooks.skippedBlocks > 0 ? "plantas-ok" : "plantas-falhou");
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
                break;
            case 22:
                log(GravidadeItens.desenhados() > 0 ? "itens" : "itens-falhou");
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
                // Desliga o rádio um passo antes do fim: o pedido de parar sai em até meio segundo.
                PulsarClient.get().mod("rastro_cometa").setEnabled(false);
                PulsarClient.get().mod("radio_pulsar").setEnabled(false);
                mc.displayGuiScreen(null);
                break;
            case 27:
                finish(mc);
                break;
            default: break;
        }
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
     * e uma placa com texto a 24 blocos (longe demais para o texto, perto o bastante para a placa).
     * Só no mundo do cliente, para não depender do servidor mandar a mudança.
     */
    private void montarBlocos(Minecraft mc) {
        int bx = (int) Math.floor(mc.thePlayer.posX), by = (int) Math.floor(mc.thePlayer.posY), bz = (int) Math.floor(mc.thePlayer.posZ);
        mc.theWorld.setBlockState(new BlockPos(bx + 2, by, bz + 3), Blocks.tallgrass.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS));
        mc.theWorld.setBlockState(new BlockPos(bx + 3, by, bz + 3), Blocks.red_flower.getDefaultState());
        mc.theWorld.setBlockState(new BlockPos(bx + 4, by, bz + 3), Blocks.yellow_flower.getDefaultState());
        BlockPos placa = new BlockPos(bx, by, bz + 24);
        mc.theWorld.setBlockState(placa, Blocks.standing_sign.getDefaultState().withProperty(BlockStandingSign.ROTATION, 8));
        TileEntity te = mc.theWorld.getTileEntity(placa);
        if (te instanceof TileEntitySign) {
            ((TileEntitySign) te).signText[0] = new ChatComponentText("Pulsar");
            ((TileEntitySign) te).signText[1] = new ChatComponentText("Client");
        } else {
            System.out.println("[Pulsar] selftest: placa sem bloco de placa (" + te + ")");
        }
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
