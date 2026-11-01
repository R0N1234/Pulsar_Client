package dev.pulsarclient.mods;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.SliderSetting;
import dev.pulsarclient.settings.TeclaDoJogo;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * W-Tap e Blockhit: a cada hit que você dá,
 * - W-Tap solta o correr por alguns ticks, para o servidor ver o "parou e voltou a correr" e o próximo hit sair com o knockback de corrida;
 * - Blockhit levanta a espada por alguns ticks logo depois do hit (o mesmo que apertar o botão direito).
 *
 * Tudo é feito pelas teclas do próprio jogo (correr e usar item), então o cliente e o servidor ficam de acordo
 * sobre quando você está correndo ou bloqueando. Cada parte tem a sua tecla (F5 e F6 no começo) e
 * pode ser trocada no painel do mod ou em Opções > Controles.
 */
public class WTapBlockhit extends PulsarMod {
    public final KeyBinding wtapKey = new KeyBinding("Ligar/desligar W-Tap", Keyboard.KEY_F5, "Pulsar Client");
    public final KeyBinding blockhitKey = new KeyBinding("Ligar/desligar Blockhit", Keyboard.KEY_F6, "Pulsar Client");

    private final BoolSetting wtap = add(new BoolSetting("wtap", "W-Tap", true));
    private final SliderSetting wtapTicks = add(new SliderSetting("wtap_ticks", "Tempo sem correr", 1, 1, 5, 1, " tick"));
    private final TeclaDoJogo wtapTecla = add(new TeclaDoJogo("tecla_wtap", "Tecla do W-Tap", wtapKey));
    private final BoolSetting blockhit = add(new BoolSetting("blockhit", "Blockhit", true));
    private final SliderSetting blockTicks = add(new SliderSetting("blockhit_ticks", "Tempo bloqueando", 1, 1, 5, 1, " tick"));
    private final BoolSetting soEspada = add(new BoolSetting("so_espada", "Blockhit só com espada na mão", true));
    private final TeclaDoJogo blockTecla = add(new TeclaDoJogo("tecla_blockhit", "Tecla do Blockhit", blockhitKey));

    // Ticks que faltam: -1 = parado; 0 = acabou agora e precisa devolver a tecla.
    private int semCorrer = -1;
    private int bloqueando = -1;
    // O hit acontece no meio do tick; o efeito começa no tick seguinte, antes do jogador se mover.
    private boolean wtapPedido;
    private boolean blockPedido;

    public WTapBlockhit() {
        super("wtap_blockhit", "W-Tap e Blockhit", "Solta o correr e levanta a espada a cada hit", Category.COMBATE, false);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.entityPlayer != mc.thePlayer) return;
        if (wtap.get() && mc.thePlayer.isSprinting()) wtapPedido = true;
        if (blockhit.get() && podeBloquear()) blockPedido = true;
    }

    private boolean podeBloquear() {
        ItemStack mao = mc.thePlayer.getHeldItem();
        if (mao == null) return false;
        return !soEspada.get() || mao.getItem() instanceof ItemSword;
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
            semCorrer = wtapTicks.getInt();
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
                bloqueando = blockTicks.getInt();
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
        if (!isEnabled() || mc.thePlayer == null) return;
        if (wtapKey.isPressed()) {
            wtap.set(!wtap.get());
            aviso("W-Tap", wtap.get());
        }
        if (blockhitKey.isPressed()) {
            blockhit.set(!blockhit.get());
            aviso("Blockhit", blockhit.get());
        }
    }

    private void aviso(String nome, boolean ligado) {
        mc.thePlayer.addChatMessage(new ChatComponentText("§d[Pulsar] §f" + nome + ": " + (ligado ? "§aligado" : "§cdesligado")));
        PulsarClient.get().save();
    }

    @Override
    protected void onDisable() {
        parar();
    }

    @Override
    public void onBroken() {
        parar();
    }

    private void parar() {
        wtapPedido = false;
        blockPedido = false;
        if (semCorrer >= 0) devolverCorrer();
        if (bloqueando >= 0) devolverUsar();
        semCorrer = -1;
        bloqueando = -1;
    }

    // Devolve às teclas o estado real delas, para não ficarem presas (a Propulsão volta a segurar o correr sozinha).
    private void devolverCorrer() {
        KeyBinding sprint = mc.gameSettings.keyBindSprint;
        KeyBinding.setKeyBindState(sprint.getKeyCode(), GameSettings.isKeyDown(sprint));
    }

    private void devolverUsar() {
        KeyBinding use = mc.gameSettings.keyBindUseItem;
        KeyBinding.setKeyBindState(use.getKeyCode(), GameSettings.isKeyDown(use));
    }
}
