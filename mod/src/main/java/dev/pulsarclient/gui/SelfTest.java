package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.Category;
import dev.pulsarclient.mods.HudMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayerMP;
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
            case 3: show("motor", new GuiPulsarPanel(menu, Category.MOTOR, PulsarClient.get().motor())); break;
            case 4: show("video", new GuiVideoSettings(menu, mc.gameSettings)); break;
            case 5: show("esc", new GuiIngameMenu()); break;
            case 6: show("hud", new GuiHudEditor(menu)); break;
            case 7: show("atalhos", new GuiPulsarPanel(menu, Category.SERVIDORES, PulsarClient.get().mod("atalhos_texto"))); break;
            case 8: show("travas", new GuiPulsarPanel(menu, Category.COMBATE, PulsarClient.get().inventario())); break;
            case 9: show("texturas", new GuiTexturas(menu)); break;
            case 10:
                mc.displayGuiScreen(menu);
                Conta.trocar();
                break;
            case 11: show("conta", menu); break;
            case 12:
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
            case 2: log("mundo"); break;
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
                mc.displayGuiScreen(null);
                finish(mc);
                break;
            default: break;
        }
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

    /** Liga todos os mods de HUD, dá itens e efeitos (só no cliente) para os prints mostrarem tudo. */
    private void prepareWorld(Minecraft mc) {
        for (HudMod m : PulsarClient.get().hudMods()) m.setEnabled(true);
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
