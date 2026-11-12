package dev.pulsarclient.mods;

import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Sequências de Abate: a cada jogador eliminado, o nome da sequência aparece na tela em letras vermelhas com sangue
 * escorrendo, e a voz do Pulsar anuncia (FIRST BLOOD, DOUBLE KILL... GODLIKE). Abates dentro do intervalo
 * escolhido continuam a sequência; passou o tempo, volta para o primeiro.
 *
 * O abate é contado quando um jogador em quem você bateu há pouco morre. Os sons vêm no Pulsar e podem ser
 * trocados pelos .ogg da pasta pulsar/sons/sequencias/ (ver {@link SonsLocais}).
 */
public class Sequencias extends HudMod {
    /** Lista Pulsar (a do Rick): 1 a 12 abates, o último vale para 12 ou mais. */
    public static final String[] NOMES = {"first_blood", "double_kill", "triple_kill", "multi_kill", "mega_kill", "ultra_kill",
            "monster_kill", "killing_spree", "rampage", "dominating", "unstoppable", "godlike"};
    public static final String[] TEXTOS = {"FIRST BLOOD", "DOUBLE KILL", "TRIPLE KILL", "MULTI KILL", "MEGA KILL", "ULTRA KILL",
            "MONSTER KILL", "KILLING SPREE", "RAMPAGE", "DOMINATING", "UNSTOPPABLE", "GODLIKE"};
    /** Lista Arena (a dos shooters clássicos): 1 a 8 abates, o último vale para 8 ou mais. */
    public static final String[] NOMES_ARENA = {"first_blood", "double_kill", "multi_kill", "mega_kill", "ultra_kill",
            "monster_kill", "ludicrous_kill", "holy_shit"};
    public static final String[] TEXTOS_ARENA = {"FIRST BLOOD", "DOUBLE KILL", "MULTI KILL", "MEGA KILL", "ULTRA KILL",
            "MONSTER KILL", "LUDICROUS KILL", "HOLY SHIT"};
    private static final int VERMELHO = 0xFFE3141E, ESCURO = 0xFF8A0A12, CLARO = 0xFFFF5A5A;
    /** Depois de bater, por quanto tempo a morte do jogador conta como abate seu. */
    private static final int JANELA_ABATE = 60;
    private static final int GOTAS = 7;

    private final ModeSetting lista = add(new ModeSetting("lista", "Lista de sequências", 0, "Pulsar", "Arena"));
    private final SliderSetting intervalo = add(new SliderSetting("intervalo", "Tempo entre abates para seguir a sequência", 10, 3, 30, 1, "s"));
    private final SliderSetting duracao = add(new SliderSetting("duracao", "Tempo do texto na tela", 3, 1, 8, 0.5, "s"));
    private final BoolSetting sangue = add(new BoolSetting("sangue", "Sangue escorrendo nas letras", true));
    private final BoolSetting som = add(new BoolSetting("som", "Voz do Pulsar anunciando", true));
    private final SliderSetting volume = add(new SliderSetting("volume", "Volume da voz", 100, 0, 100, 5, "%"));
    private final ActionSetting testar = add(new ActionSetting("testar", "Testar a próxima sequência", new Runnable() {
        @Override public void run() { abate(); }
    }));
    private final ActionSetting recarregar = add(new ActionSetting("recarregar", "Recarregar os sons da pasta pulsar/sons", new Runnable() {
        @Override public void run() { mc.refreshResources(); }
    }));

    private int ticks;
    private final Map<EntityLivingBase, Integer> alvos = new HashMap<EntityLivingBase, Integer>();
    private int contagem;
    private int ultimoAbate = -100000;
    private int mostrarAte = -1;
    private long mostrouEm;
    private int indice;
    private long semente;

    private static int abates;
    private static int desenhos;

    public Sequencias() {
        super("sequencias", "Sequências de Abate", "FIRST BLOOD, DOUBLE KILL... GODLIKE na tela, com sangue escorrendo e a voz do Pulsar",
                Category.COMBATE, CENTRO, 80, false);
    }

    /** Teste automático. */
    public static int abates() { return abates; }
    public static int desenhos() { return desenhos; }

    public int contagem() { return contagem; }

    /** Teste automático: troca a lista (0 Pulsar, 1 Arena). */
    public void usarLista(int i) { lista.set(i); }

    private String[] nomes() { return lista.index() == 1 ? NOMES_ARENA : NOMES; }
    private String[] textos() { return lista.index() == 1 ? TEXTOS_ARENA : TEXTOS; }

    /** O texto da sequência atual (teste automático). */
    public String textoAtual() { return textos()[Math.min(indice, textos().length - 1)]; }

    // ---------- contar os abates ----------

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.entityPlayer != mc.thePlayer) return;
        if (event.target instanceof EntityPlayer && event.target != mc.thePlayer) alvos.put((EntityLivingBase) event.target, ticks);
    }

    private static boolean morreu(EntityLivingBase e) {
        return e.isDead || e.getHealth() <= 0 || e.deathTime > 0;
    }

    @Override
    public void tick() {
        ticks++;
        if (mc.thePlayer == null || mc.theWorld == null) {
            alvos.clear();
            contagem = 0;
            return;
        }
        if (!isEnabled()) {
            alvos.clear();
            return;
        }
        Iterator<Map.Entry<EntityLivingBase, Integer>> it = alvos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<EntityLivingBase, Integer> e = it.next();
            if (ticks - e.getValue() > JANELA_ABATE) {
                it.remove();
            } else if (morreu(e.getKey())) {
                it.remove();
                System.out.println("[Pulsar] Sequências: " + e.getKey().getName() + " eliminado");
                abate();
            }
        }
        // Você morreu: a sequência acaba.
        if (mc.thePlayer.getHealth() <= 0 && contagem > 0) {
            contagem = 0;
            ultimoAbate = -100000;
        }
    }

    /** Mais um abate: segue a sequência se foi dentro do intervalo, senão começa de novo. */
    public void abate() {
        contagem = ticks - ultimoAbate <= intervalo.getInt() * 20 ? contagem + 1 : 1;
        ultimoAbate = ticks;
        indice = Math.min(contagem, nomes().length) - 1;
        mostrarAte = ticks + (int) Math.round(duracao.get() * 20);
        mostrouEm = System.currentTimeMillis();
        semente = mostrouEm;
        abates++;
        System.out.println("[Pulsar] Sequências: " + textos()[indice] + " (" + contagem + ")");
        if (som.get() && volume.get() > 0) {
            mc.getSoundHandler().playSound(new Voz(new ResourceLocation("pulsar", "sequencias." + nomes()[indice]), (float) (volume.get() / 100.0)));
        }
    }

    /** Som na categoria do jogador, sem distância, no volume escolhido. */
    private static final class Voz extends PositionedSound {
        Voz(ResourceLocation som, float vol) {
            super(som);
            this.volume = vol;
            this.pitch = 1f;
            this.attenuationType = ISound.AttenuationType.NONE;
        }
    }

    // ---------- desenho ----------

    private boolean mostrando() {
        return mostrarAte >= ticks;
    }

    @Override public int width() { return font().getStringWidth("LUDICROUS KILL") * 2 + 8; }
    @Override public int height() { return 34; }

    @Override
    public void render(boolean preview) {
        if (!preview && !mostrando()) return;
        String texto = preview ? textos()[3] : textoAtual();
        long agora = System.currentTimeMillis();
        float t = preview ? 1.5f : (agora - mostrouEm) / 1000f;
        float fim = (float) duracao.get();
        // Entra "batendo" (maior e encolhe) e some apagando no fim.
        float pop = t < 0.15f ? 1.35f - 0.35f * (t / 0.15f) : 1f;
        float alfa = preview ? 1f : t > fim - 0.5f ? Math.max(0f, (fim - t) / 0.5f) : 1f;
        if (alfa <= 0.02f) return;
        desenhos++;
        int a = Math.round(alfa * 255);
        int w = width();
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + w + 2, y + height() + 2, (a / 2) << 24);
        int tw = font().getStringWidth(texto) * 2;
        int cx = x + w / 2, cy = y + 9;
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 0);
        GlStateManager.scale(2f * pop, 2f * pop, 1f);
        GlStateManager.enableBlend();
        int cor = (a << 24) | (VERMELHO & 0xFFFFFF);
        int sombra = (a << 24) | (ESCURO & 0xFFFFFF);
        font().drawString(texto, -tw / 4 + 1, -4 + 1, sombra);
        font().drawString(texto, -tw / 4, -4, cor);
        GlStateManager.popMatrix();
        if (sangue.get()) gotas(cx - tw / 2, cy + 6, tw, t, a);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Gotas de sangue escorrendo de baixo das letras, cada uma no seu tempo. */
    private void gotas(int esquerda, int topo, int largura, float t, int alfaTexto) {
        Random r = new Random(semente);
        for (int i = 0; i < GOTAS; i++) {
            int gx = esquerda + 4 + r.nextInt(Math.max(1, largura - 8));
            float atraso = r.nextFloat() * 0.9f;
            float vel = 6 + r.nextFloat() * 8;
            int gros = 1 + r.nextInt(2);
            float vida = t - atraso;
            if (vida <= 0) continue;
            // Desce acelerando, até o fim da caixa; o rastro vai ficando mais escuro.
            float queda = Math.min(height() - 12, vel * vida * (1 + vida * 0.6f));
            int fimY = topo + Math.round(queda);
            int a = Math.min(alfaTexto, Math.round(255 * Math.max(0f, 1f - vida / 2.6f)));
            if (a <= 0) continue;
            Gui.drawRect(gx, topo - 1, gx + gros, fimY, (a << 24) | (ESCURO & 0xFFFFFF));
            Gui.drawRect(gx, topo - 1, gx + gros, topo + Math.round(queda * 0.35f), ((a * 3 / 4) << 24) | (VERMELHO & 0xFFFFFF));
            // A gota na ponta, com um brilho.
            Gui.drawRect(gx - 1, fimY - 1, gx + gros + 1, fimY + 2, (a << 24) | (VERMELHO & 0xFFFFFF));
            Gui.drawRect(gx, fimY, gx + 1, fimY + 1, (a << 24) | (CLARO & 0xFFFFFF));
            // Pingo que se soltou da gota e cai mais rápido.
            if (vida > 1.2f) {
                int py = fimY + 3 + Math.round((vida - 1.2f) * 26);
                if (py < y + height() + 2) Gui.drawRect(gx, py, gx + gros, py + 2, (a << 24) | (VERMELHO & 0xFFFFFF));
            }
        }
    }
}
