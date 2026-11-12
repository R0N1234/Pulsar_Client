package dev.pulsarclient.mods;

import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Anti-Batata: avisa quando a regeneração da maçã dourada (a "cap") vai acabar, para ninguém ficar
 * "de batata" no meio da treta sem perceber.
 *
 * Aviso: contagem na tela (arrastável no editor de HUD) e/ou a tela piscando numa cor da paleta, começando
 * alguns segundos antes do fim (3 de fábrica). Quando acaba, um "Acabou!" rápido.
 *
 * Comer sozinho (desligado de fábrica): faltando 1 segundo, com espada na mão e batendo, troca para a maçã da
 * barra, come e volta para a espada. Uma vez por efeito: se o servidor não deixar comer, o mod não fica tentando.
 */
public class AntiBatata extends HudMod {
    private static final ItemStack MACA = new ItemStack(Items.golden_apple);
    private static final int CONTAGEM = 0, PISCAR = 1, OS_DOIS = 2, NENHUM = 3;
    private static final int ACABOU_TICKS = 30;
    /** Bateu em alguém nos últimos 1,5 s = está na treta. */
    private static final int COMBATE_TICKS = 30;

    private final ModeSetting aviso = add(new ModeSetting("aviso", "Aviso quando a regeneração for acabar", OS_DOIS,
            "Contagem", "Piscar", "Os dois", "Nenhum"));
    private final SliderSetting antes = add(new SliderSetting("segundos", "Começar o aviso faltando", 3, 1, 10, 1, "s"));
    private final ModeSetting onde = add(new ModeSetting("onde_piscar", "Onde piscar", 0, "Tela toda", "Só as bordas"));
    private final ColorSetting corPisca = add(new ColorSetting("cor_piscar", "Cor do pisca", ColorSetting.VERMELHO));
    private final SliderSetting forca = add(new SliderSetting("forca_piscar", "Força do pisca", 30, 10, 80, 5, "%"));
    private final BoolSetting som = add(new BoolSetting("som", "Bipe a cada segundo da contagem", true));
    private final BoolSetting acabou = add(new BoolSetting("aviso_acabou", "Mostrar \"Acabou!\" quando a regeneração terminar", true));
    private final BoolSetting comer = add(new BoolSetting("comer_sozinho", "Comer a maçã sozinho com espada na mão e batendo (alguns servidores proíbem)", false));
    private final SliderSetting comerFaltando = add(new SliderSetting("comer_faltando", "Comer faltando", 1, 0.5, 3, 0.5, "s"));
    private final ModeSetting qualMaca = add(new ModeSetting("qual_maca", "Qual maçã comer", 0,
            "Dourada (encantada se acabar)", "Só a dourada", "Só a encantada"));

    private int ticks;
    /** Ticks que faltam da regeneração (-1 sem o efeito). */
    private int restante = -1;
    private boolean tentou;
    private int acabouTicks;
    private int ultimoSegundo = -1;
    private int ultimoAtaque = -1000;
    private int combateTeste = -1000;

    // Comendo sozinho: -1 = não está comendo.
    private int comendo = -1;
    private int slotAntes = -1;
    private int slotMaca = -1;
    private int macasAntes;

    private static int avisos;
    private static int comidas;

    public AntiBatata() {
        super("anti_batata", "Anti-Batata", "Avisa quando a regeneração da maçã dourada vai acabar (contagem ou tela piscando) e pode comer outra sozinho",
                Category.COMBATE, CENTRO, 70, false);
    }

    /** Ticks em que o aviso esteve na tela (teste automático). */
    public static int avisos() {
        return avisos;
    }

    /** Maçãs comidas sozinho (teste automático). */
    public static int comidas() {
        return comidas;
    }

    /** Teste automático: liga contagem e pisca e, se pedido, o comer sozinho. */
    public void prepararTeste(boolean comerSozinho) {
        aviso.set(OS_DOIS);
        comer.set(comerSozinho);
        comerFaltando.set(1);
        qualMaca.set(0);
    }

    /** Teste automático: finge que o jogador está batendo durante alguns ticks. */
    public void simularCombate(int duracao) {
        combateTeste = ticks + duracao;
    }

    private int antesTicks() {
        return antes.getInt() * 20;
    }

    private boolean mostraContagem() {
        return aviso.index() == CONTAGEM || aviso.index() == OS_DOIS;
    }

    private boolean pisca() {
        return aviso.index() == PISCAR || aviso.index() == OS_DOIS;
    }

    /** A regeneração está nos últimos segundos (dentro do aviso). */
    private boolean avisando() {
        return restante > 0 && restante <= antesTicks();
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer != null && event.entityPlayer == mc.thePlayer) ultimoAtaque = ticks;
    }

    @Override
    public void tick() {
        ticks++;
        EntityPlayerSP p = mc.thePlayer;
        if (p == null || !isEnabled() || p.isDead || p.getHealth() <= 0) {
            pararDeComer();
            restante = -1;
            tentou = false;
            acabouTicks = 0;
            ultimoSegundo = -1;
            return;
        }
        PotionEffect regen = p.getActivePotionEffect(Potion.regeneration);
        int agora = regen == null ? -1 : regen.getDuration();
        // Acabou sozinho (não foi leite nem morte): estava no finzinho quando sumiu.
        if (agora < 0 && restante >= 0 && restante <= 10) acabouTicks = ACABOU_TICKS;
        // Efeito novo ou renovado (comeu outra maçã): pode tentar comer de novo no fim deste.
        if (agora > restante) tentou = false;
        restante = agora;
        if (acabouTicks > 0) acabouTicks--;

        if (avisando() && aviso.index() != NENHUM) {
            avisos++;
            int segundo = (restante + 19) / 20;
            if (segundo != ultimoSegundo) {
                ultimoSegundo = segundo;
                if (som.get()) {
                    float tom = 0.8f + 0.4f * Math.max(0, antes.getInt() - segundo) / Math.max(1, antes.getInt());
                    mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("note.pling"), tom));
                }
            }
        } else {
            ultimoSegundo = -1;
        }

        if (comendo >= 0) {
            continuarComendo(p);
            return;
        }
        if (comer.get() && !tentou && restante > 0 && restante <= Math.round(comerFaltando.get() * 20)
                && mc.currentScreen == null && emCombate(p)) {
            tentou = true;
            comecarAComer(p);
        }
    }

    /** Espada na mão e batendo (botão de ataque segurado ou acertou alguém agora há pouco). */
    private boolean emCombate(EntityPlayerSP p) {
        ItemStack mao = p.getHeldItem();
        if (mao == null || !(mao.getItem() instanceof ItemSword)) return false;
        return mc.gameSettings.keyBindAttack.isKeyDown() || ticks - ultimoAtaque <= COMBATE_TICKS || ticks <= combateTeste;
    }

    /** A maçã da barra (0 a 8) que vai ser comida, ou -1. */
    private int acharMaca(EntityPlayerSP p) {
        int dourada = -1, encantada = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.inventory.mainInventory[i];
            if (s == null || s.getItem() != Items.golden_apple) continue;
            if (s.getMetadata() == 0 && dourada < 0) dourada = i;
            if (s.getMetadata() != 0 && encantada < 0) encantada = i;
        }
        switch (qualMaca.index()) {
            case 1: return dourada;
            case 2: return encantada;
            default: return dourada >= 0 ? dourada : encantada;
        }
    }

    private void comecarAComer(EntityPlayerSP p) {
        int slot = acharMaca(p);
        if (slot < 0) return;
        slotAntes = p.inventory.currentItem;
        slotMaca = slot;
        macasAntes = p.inventory.mainInventory[slot].stackSize;
        p.inventory.currentItem = slot;
        // Segura o botão de usar como a pessoa faria: o jogo começa a comer no mesmo tick.
        int k = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(k, true);
        KeyBinding.onTick(k);
        comendo = 0;
        System.out.println("[Pulsar] Anti-Batata: comendo a maçã do espaço " + (slot + 1) + " com " + restante + " ticks de regeneração");
    }

    private void continuarComendo(EntityPlayerSP p) {
        comendo++;
        ItemStack s = p.inventory.mainInventory[slotMaca];
        boolean comeu = s == null || s.getItem() != Items.golden_apple || s.stackSize < macasAntes;
        boolean parou = comendo > 8 && !p.isUsingItem();
        if (comeu || parou || comendo > 60 || mc.currentScreen != null || p.inventory.currentItem != slotMaca) {
            if (comeu) {
                comidas++;
                System.out.println("[Pulsar] Anti-Batata: maçã comida, voltando para o espaço " + (slotAntes + 1));
            }
            pararDeComer();
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
    }

    private void pararDeComer() {
        if (comendo < 0) return;
        comendo = -1;
        // Solta o botão de usar, mas só se a pessoa não estiver segurando ele de verdade (bloqueio com a espada).
        int k = mc.gameSettings.keyBindUseItem.getKeyCode();
        boolean fisico = false;
        try {
            fisico = k < 0 ? Mouse.isButtonDown(k + 100) : k > 0 && Keyboard.isKeyDown(k);
        } catch (Throwable ignored) {
        }
        KeyBinding.setKeyBindState(k, fisico);
        if (mc.thePlayer != null && mc.thePlayer.inventory.currentItem == slotMaca && slotAntes >= 0 && slotAntes < 9) {
            mc.thePlayer.inventory.currentItem = slotAntes;
        }
    }

    @Override
    protected void onDisable() {
        pararDeComer();
    }

    @Override
    public void onBroken() {
        pararDeComer();
    }

    // ---------- desenho ----------

    @Override public int width() { return 84; }
    @Override public int height() { return 20; }

    @Override
    public void render(boolean preview) {
        float segundos;
        boolean fim = false;
        if (preview) {
            segundos = antes.getInt() * 0.8f;
        } else if (mostraContagem() && avisando()) {
            segundos = restante / 20f;
        } else if (acabou.get() && aviso.index() != NENHUM && acabouTicks > 0) {
            segundos = 0;
            fim = true;
        } else {
            return;
        }
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + width() + 2, y + height() + 2, BACKGROUND);
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(MACA, x, y + 2);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        if (fim) {
            font().drawStringWithShadow("Acabou!", x + 22, y + 2, corPisca.argb(0xFF));
            font().drawStringWithShadow("Regeneração", x + 22, y + 11, WHITE);
            GlStateManager.enableBlend();
            return;
        }
        // Número grande; no último segundo alterna com branco.
        boolean ultimo = segundos <= 1f && (System.currentTimeMillis() / 150) % 2 == 0;
        String texto = String.format("%.1fs", segundos);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 22, y + 1, 0);
        GlStateManager.scale(2f, 2f, 1f);
        font().drawStringWithShadow(texto, 0, 0, ultimo ? WHITE : accent());
        GlStateManager.popMatrix();
        int bx = x + 22, bw = width() - 22;
        float total = Math.max(1, antes.getInt());
        Gui.drawRect(bx, y + 18, bx + bw, y + 19, 0x55FFFFFF);
        Gui.drawRect(bx, y + 18, bx + Math.round(bw * Math.min(1f, segundos / total)), y + 19, accent());
        GlStateManager.enableBlend();
    }

    /** O pisca vai por baixo da HUD (antes de tudo), para não esconder a vida e a barra. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !isEnabled() || !pisca() || mc.thePlayer == null) return;
        if (mc.currentScreen instanceof GuiHudEditor) return;
        float intensidade;
        if (avisando()) {
            // Pisca mais rápido no último segundo.
            double hz = restante <= 20 ? 4 : 2;
            // Nunca apaga de todo: a cor fica sempre um pouco na tela enquanto o aviso dura, e pulsa por cima.
            intensidade = 0.675f + 0.325f * (float) Math.sin(System.currentTimeMillis() / 1000.0 * hz * 2 * Math.PI);
        } else if (acabou.get() && acabouTicks > 0) {
            intensidade = acabouTicks / (float) ACABOU_TICKS;
        } else {
            return;
        }
        int alfa = Math.round((float) (forca.get() / 100.0) * 255 * intensidade);
        if (alfa <= 2) return;
        int w = event.resolution.getScaledWidth(), h = event.resolution.getScaledHeight();
        if (onde.index() == 0) {
            Gui.drawRect(0, 0, w, h, corPisca.argb(alfa));
        } else {
            // Bordas: faixas que vão sumindo para dentro.
            int faixas = 12;
            for (int i = 0; i < faixas; i++) {
                int c = corPisca.argb(alfa * (faixas - i) / faixas);
                int d = i * 2;
                Gui.drawRect(d, d, w - d, d + 2, c);
                Gui.drawRect(d, h - d - 2, w - d, h - d, c);
                Gui.drawRect(d, d + 2, d + 2, h - d - 2, c);
                Gui.drawRect(w - d - 2, d + 2, w - d, h - d - 2, c);
            }
        }
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
