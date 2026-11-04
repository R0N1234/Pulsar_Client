package dev.pulsarclient.mods;

import dev.pulsarclient.gui.Desenho;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Barra de Chefe: a barra de vida do chefe (e os avisos que os servidores colocam nela) vira um HUD:
 * dá para mover, mudar o tamanho, a cor, esconder o texto ou só a barra.
 */
public class BarraChefe extends HudMod {
    private static final int W = 182;
    private final BoolSetting texto = add(new BoolSetting("texto", "Mostrar o texto", true));
    private final BoolSetting barra = add(new BoolSetting("barra", "Mostrar a barra", true));
    private final BoolSetting porcentagem = add(new BoolSetting("porcentagem", "Porcentagem da vida", false));
    private final ColorSetting corBarra = add(new ColorSetting("cor_barra", "Cor da barra", 0xFF7AB6));
    private static BarraChefe instancia;
    private static long desenhos;

    public BarraChefe() {
        super("barra_chefe", "Barra de Chefe", "Move, muda o tamanho e a cor da barra de boss", Category.HUD, CENTRO, 22, false);
        instancia = this;
    }

    /** Ligada: a barra do jogo não aparece (a Barra Pulsar não precisa desviar dela). */
    public static boolean ativa() {
        return instancia != null && instancia.isEnabled();
    }

    /** Quantas vezes a barra foi desenhada com um chefe de verdade (teste automático). */
    public static long desenhos() {
        return desenhos;
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH && isEnabled()) event.setCanceled(true);
    }

    private boolean temChefe() {
        return BossStatus.bossName != null && BossStatus.statusBarTime > 0;
    }

    @Override
    public int width() {
        return W;
    }

    @Override
    public int height() {
        return (texto.get() ? 10 : 0) + (barra.get() ? 5 : 0);
    }

    @Override
    public void render(boolean preview) {
        boolean chefe = temChefe();
        if (!chefe && !preview) return;
        // O jogo conta o tempo da barra no próprio desenho dela; como ele não desenha mais, contamos aqui.
        if (chefe && !preview) {
            BossStatus.statusBarTime--;
            desenhos++;
        }
        String nome = chefe ? BossStatus.bossName : "Barra de Chefe";
        float vida = chefe ? BossStatus.healthScale : 0.75f;
        int ty = y;
        if (texto.get()) {
            String t = porcentagem.get() ? nome + " §7" + Math.round(vida * 100) + "%" : nome;
            font().drawStringWithShadow(t, x + (W - font().getStringWidth(t)) / 2f, ty, WHITE);
            ty += 10;
        }
        if (barra.get()) {
            Desenho.caixa(x, ty, x + W, ty + 5, 0xAA000000, 2);
            int fill = Math.round(W * Math.max(0f, Math.min(1f, vida)));
            if (fill > 0) Desenho.caixa(x, ty, x + fill, ty + 5, corBarra.argb(0xFF), 2);
        }
    }
}
