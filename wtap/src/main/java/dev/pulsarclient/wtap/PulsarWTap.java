package dev.pulsarclient.wtap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * W-Tap e Blockhit do Pulsar, como um mod Forge separado (funciona sozinho, com ou sem o Pulsar Client).
 * A cada hit que você dá:
 * - W-Tap solta o correr por alguns ticks, para o servidor ver o "parou e voltou a correr" e o próximo hit sair com o knockback de corrida;
 * - Blockhit levanta a espada por alguns ticks logo depois do hit (o mesmo que apertar o botão direito).
 *
 * Tudo é feito pelas teclas do próprio jogo (correr e usar item), então o cliente e o servidor ficam de acordo.
 * Teclas para ligar e desligar cada parte e para abrir as opções em Opções > Controles; as opções também ficam em Mods > Pulsar W-Tap > Config.
 */
@Mod(modid = PulsarWTap.MODID, name = "Pulsar W-Tap e Blockhit", version = PulsarWTap.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", guiFactory = "dev.pulsarclient.wtap.Fabrica")
public class PulsarWTap {
    public static final String MODID = "pulsarwtap";
    public static final String VERSION = "1.1.0";
    static final String CATEGORIA = "geral";

    static Configuration config;
    private static Property wtap, wtapTicks, blockhit, blockTicks, soEspada;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding wtapKey = new KeyBinding("Ligar/desligar W-Tap", Keyboard.KEY_NONE, "Pulsar W-Tap e Blockhit");
    private final KeyBinding blockhitKey = new KeyBinding("Ligar/desligar Blockhit", Keyboard.KEY_NONE, "Pulsar W-Tap e Blockhit");
    private final KeyBinding opcoesKey = new KeyBinding("Abrir opções (ticks)", Keyboard.KEY_NONE, "Pulsar W-Tap e Blockhit");

    // Ticks que faltam: -1 = parado; 0 = acabou agora e precisa devolver a tecla.
    private int semCorrer = -1;
    private int bloqueando = -1;
    // O hit acontece no meio do tick; o efeito começa no tick seguinte, antes do jogador se mover.
    private boolean wtapPedido;
    private boolean blockPedido;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        carregar();
    }

    static void carregar() {
        wtap = config.get(CATEGORIA, "wtap", true, "W-Tap: solta o correr a cada hit");
        wtapTicks = config.get(CATEGORIA, "wtap_ticks", 1, "Ticks sem correr depois do hit (1 a 5)", 1, 5);
        blockhit = config.get(CATEGORIA, "blockhit", true, "Blockhit: levanta a espada a cada hit");
        blockTicks = config.get(CATEGORIA, "blockhit_ticks", 1, "Ticks bloqueando depois do hit (1 a 5)", 1, 5);
        soEspada = config.get(CATEGORIA, "so_espada", true, "Blockhit só com espada na mão");
        // Barra de arrastar no lugar da caixa de texto: digitar "3" depois do "1" virava "13" e a tela recusava.
        wtapTicks.setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        blockTicks.setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        if (config.hasChanged()) config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(wtapKey);
        ClientRegistry.registerKeyBinding(blockhitKey);
        ClientRegistry.registerKeyBinding(opcoesKey);
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("[Pulsar W-Tap] carregado (W-Tap " + wtap.getBoolean() + ", Blockhit " + blockhit.getBoolean() + ")");
    }

    @SubscribeEvent
    public void onConfig(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!MODID.equals(event.modID)) return;
        config.save();
        carregar();
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer == null || event.entityPlayer != mc.thePlayer) return;
        if (wtap.getBoolean() && mc.thePlayer.isSprinting()) wtapPedido = true;
        if (blockhit.getBoolean() && podeBloquear()) blockPedido = true;
    }

    private boolean podeBloquear() {
        ItemStack mao = mc.thePlayer.getHeldItem();
        if (mao == null) return false;
        return !soEspada.getBoolean() || mao.getItem() instanceof ItemSword;
    }

    private static int ticks(Property p) {
        return Math.max(1, Math.min(5, p.getInt()));
    }

    /** Começo do tick: antes do jogador se mover, que é quando o jogo decide correr e manda isso ao servidor. */
    @SubscribeEvent
    public void onTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            parar();
            return;
        }

        if (wtapPedido) {
            wtapPedido = false;
            semCorrer = ticks(wtapTicks);
        }
        if (semCorrer > 0) {
            semCorrer--;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            mc.thePlayer.setSprinting(false);
        } else if (semCorrer == 0) {
            semCorrer = -1;
            devolverCorrer();
        }

        if (blockPedido) {
            blockPedido = false;
            if (podeBloquear()) {
                bloqueando = ticks(blockTicks);
                KeyBinding use = mc.gameSettings.keyBindUseItem;
                KeyBinding.setKeyBindState(use.getKeyCode(), true);
                KeyBinding.onTick(use.getKeyCode());
            }
        } else if (bloqueando > 0) {
            bloqueando--;
            if (bloqueando > 0) KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
        }
        if (bloqueando == 0) {
            bloqueando = -1;
            devolverUsar();
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (mc.thePlayer == null) return;
        if (wtapKey.isPressed()) alternar("W-Tap", wtap);
        if (blockhitKey.isPressed()) alternar("Blockhit", blockhit);
        // Abre as mesmas opções da lista de mods, sem sair do jogo; ao fechar, volta para o jogo.
        if (opcoesKey.isPressed() && mc.currentScreen == null) mc.displayGuiScreen(new Fabrica.Tela(null));
    }

    private void alternar(String nome, Property p) {
        p.set(!p.getBoolean());
        config.save();
        mc.thePlayer.addChatMessage(new ChatComponentText("§b[Pulsar] §f" + nome + ": " + (p.getBoolean() ? "§aligado" : "§cdesligado")));
    }

    private void parar() {
        wtapPedido = false;
        blockPedido = false;
        if (semCorrer >= 0) devolverCorrer();
        if (bloqueando >= 0) devolverUsar();
        semCorrer = -1;
        bloqueando = -1;
    }

    // Devolve às teclas o estado real delas, para não ficarem presas.
    private void devolverCorrer() {
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        KeyBinding.setKeyBindState(sprint.getKeyCode(), GameSettings.isKeyDown(sprint));
    }

    private void devolverUsar() {
        KeyBinding use = mc.gameSettings.keyBindUseItem;
        KeyBinding.setKeyBindState(use.getKeyCode(), GameSettings.isKeyDown(use));
    }
}
