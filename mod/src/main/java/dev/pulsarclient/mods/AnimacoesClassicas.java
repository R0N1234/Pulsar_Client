package dev.pulsarclient.mods;

import dev.pulsarclient.core.Ganchos;
import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Mouse;

/**
 * Animações Clássicas: o braço balança como na 1.7 quando você bate enquanto bloqueia com a espada (block-hit),
 * e também ao comer, beber e puxar o arco. É só visual: o balanço não é enviado ao servidor.
 */
public class AnimacoesClassicas extends PulsarMod implements Ganchos.Balanco {
    private final BoolSetting bloqueio = add(new BoolSetting("bloqueio", "Balanço ao bater bloqueando (block-hit)", true));
    private final BoolSetting usar = add(new BoolSetting("usar", "Balanço ao comer, beber e puxar o arco", true));

    public AnimacoesClassicas() {
        super("animacoes_classicas", "Animações Clássicas", "Bloqueio e balanço do braço no estilo da 1.7", Category.COMBATE, false);
        Ganchos.balanco = this;
    }

    @Override
    public boolean ativo() {
        if (!isEnabled() || mc.thePlayer == null) return false;
        ItemStack uso = mc.thePlayer.getItemInUse();
        if (uso == null) return false;
        return uso.getItemUseAction() == EnumAction.BLOCK ? bloqueio.get() : usar.get();
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent event) {
        if (!isEnabled() || !bloqueio.get() || mc.currentScreen != null || mc.thePlayer == null) return;
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() - 100 != mc.gameSettings.keyBindAttack.getKeyCode()) return;
        // Enquanto usa um item o jogo ignora o clique de ataque; aqui ele vira só o balanço do braço.
        if (!mc.thePlayer.isUsingItem()) return;
        EntityPlayerSP p = mc.thePlayer;
        if (!p.isSwingInProgress || p.swingProgressInt >= 3 || p.swingProgressInt < 0) {
            p.swingProgressInt = -1;
            p.isSwingInProgress = true;
        }
    }
}
