package dev.pulsarclient.cosmeticos;

import dev.pulsarclient.core.Ganchos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Acessórios e emotes do Pulsar.
 *
 * - Acessórios são desenhados por uma camada a mais no boneco do jogador ({@link CamadaAcessorio}).
 * - Emotes mexem nas partes do boneco depois da pose normal do jogo, por um gancho no ModelBiped
 *   ({@link Ganchos#poseEmote}); a armadura e as camadas da skin seguem a mesma pose.
 *
 * Um acessório e um emote do Pulsar são grátis. Os pagos aparecem na loja trancados ("em breve")
 * até a venda abrir. Por enquanto cada um vê os próprios cosméticos; para os outros jogadores verem,
 * a rede do Pulsar vai contar o que cada um está usando.
 */
public final class Cosmeticos implements Ganchos.Emote {
    public static final List<Cosmetico> CATALOGO;
    static {
        List<Cosmetico> c = new ArrayList<Cosmetico>();
        c.add(new Cosmetico("orbita", "Órbita Pulsar", "Um pulsar girando sobre a cabeça, com as órbitas do logo.", Cosmetico.Tipo.ACESSORIO, true));
        c.add(new Cosmetico("coroa", "Coroa Estelar", "Coroa dourada com pontas de estrela e gema azul.", Cosmetico.Tipo.ACESSORIO, false));
        c.add(new Cosmetico("asas", "Asas de Nebulosa", "Asas de luz roxa e azul que batem devagar.", Cosmetico.Tipo.ACESSORIO, false));
        c.add(new Cosmetico("supernova", "Supernova", "Junta energia e explode em estrelas. O emote do Pulsar.", Cosmetico.Tipo.EMOTE, true));
        c.add(new Cosmetico("danca", "Dança Orbital", "Uma dancinha para comemorar o round.", Cosmetico.Tipo.EMOTE, false));
        c.add(new Cosmetico("facepalm", "Facepalm", "Mão na cara, para aquele erro.", Cosmetico.Tipo.EMOTE, false));
        CATALOGO = Collections.unmodifiableList(c);
    }

    private static Cosmeticos instancia;

    public final KeyBinding teclaEmotes = new KeyBinding("Emotes do Pulsar", Keyboard.KEY_B, "Pulsar Client");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final File arquivo;
    private String acessorio = "";
    private boolean camadasPostas;

    /** Emotes tocando agora, pelo id da entidade. */
    private final Map<Integer, Ativo> ativos = new HashMap<Integer, Ativo>();
    private boolean tudoLiberado;
    private static volatile int poses, desenhos;
    private int visaoAntes = -1;
    private int visaoNossa = -1;

    private static final class Ativo {
        final String id;
        final long inicio;
        boolean estourou;

        Ativo(String id, long inicio) {
            this.id = id;
            this.inicio = inicio;
        }
    }

    private Cosmeticos(File pastaDoJogo) {
        arquivo = new File(new File(pastaDoJogo, "pulsar"), "cosmeticos.properties");
        carregar();
    }

    public static Cosmeticos iniciar(File pastaDoJogo) {
        instancia = new Cosmeticos(pastaDoJogo);
        Ganchos.emote = instancia;
        return instancia;
    }

    public static Cosmeticos get() {
        return instancia;
    }

    public static Cosmetico item(String id) {
        for (Cosmetico c : CATALOGO) if (c.id.equals(id)) return c;
        return null;
    }

    // ---------- o que a pessoa tem e usa ----------

    /** Por enquanto só os grátis: a venda dos pagos ainda não abriu. */
    public boolean possui(Cosmetico c) {
        return c != null && (c.gratis || tudoLiberado);
    }

    /** Só no teste automático do CI: libera os pagos para conferir que todos aparecem. */
    public void liberarTudoParaTeste() {
        if (Boolean.getBoolean("pulsar.selftest")) tudoLiberado = true;
    }

    /** Quantas vezes um emote mexeu num boneco e quantos acessórios foram desenhados (teste automático). */
    public static int poses() {
        return poses;
    }

    public static int desenhos() {
        return desenhos;
    }

    static void desenhou() {
        desenhos++;
    }

    public String acessorio() {
        return acessorio;
    }

    public void usarAcessorio(String id) {
        Cosmetico c = item(id);
        acessorio = c != null && c.tipo == Cosmetico.Tipo.ACESSORIO && possui(c) ? id : "";
        salvar();
        System.out.println("[Pulsar] Cosméticos: acessório " + (acessorio.isEmpty() ? "nenhum" : acessorio));
    }

    /** Acessório que aparece nesse jogador (só o próprio, por enquanto). */
    String acessorioDe(AbstractClientPlayer p) {
        if (p == mc.thePlayer) return acessorio;
        return "";
    }

    private void carregar() {
        Properties p = new Properties();
        if (arquivo.isFile()) {
            try (InputStream in = new FileInputStream(arquivo)) {
                p.load(in);
            } catch (Exception e) {
                System.err.println("[Pulsar] Cosméticos: não deu para ler " + arquivo + ": " + e);
            }
        }
        Cosmetico c = item(p.getProperty("acessorio", "orbita"));
        acessorio = c != null && possui(c) ? c.id : "";
    }

    private void salvar() {
        Properties p = new Properties();
        p.setProperty("acessorio", acessorio.isEmpty() ? "nenhum" : acessorio);
        try {
            arquivo.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(arquivo)) {
                p.store(out, "Cosmeticos do Pulsar Client");
            }
        } catch (Exception e) {
            System.err.println("[Pulsar] Cosméticos: não deu para salvar: " + e);
        }
    }

    // ---------- emotes ----------

    /** Toca um emote no próprio jogador: a câmera vira de frente para a pessoa ver, e volta no fim. */
    public boolean tocar(String id) {
        Cosmetico c = item(id);
        EntityPlayerSP p = mc.thePlayer;
        if (c == null || c.tipo != Cosmetico.Tipo.EMOTE || !possui(c) || p == null) return false;
        ativos.put(p.getEntityId(), new Ativo(id, System.currentTimeMillis()));
        if (visaoNossa < 0) visaoAntes = mc.gameSettings.thirdPersonView;
        mc.gameSettings.thirdPersonView = 2;
        visaoNossa = 2;
        System.out.println("[Pulsar] Cosméticos: emote " + id);
        return true;
    }

    public boolean tocando() {
        return mc.thePlayer != null && ativos.containsKey(mc.thePlayer.getEntityId());
    }

    public void parar() {
        if (mc.thePlayer != null) ativos.remove(mc.thePlayer.getEntityId());
        if (visaoNossa >= 0 && mc.gameSettings.thirdPersonView == visaoNossa && visaoAntes >= 0) mc.gameSettings.thirdPersonView = visaoAntes;
        visaoNossa = -1;
        visaoAntes = -1;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.theWorld == null) {
            ativos.clear();
            if (visaoNossa >= 0 && visaoAntes >= 0) mc.gameSettings.thirdPersonView = visaoAntes;
            visaoNossa = -1;
            return;
        }
        Ativo a = ativos.get(p.getEntityId());
        if (a == null) return;
        float t = (System.currentTimeMillis() - a.inicio) / 1000f;
        // Andar, pular, agachar ou bater cancela, igual no Lunar.
        boolean mexeu = p.movementInput != null && (Math.abs(p.movementInput.moveForward) > 0.01f || Math.abs(p.movementInput.moveStrafe) > 0.01f
                || p.movementInput.jump || p.movementInput.sneak) || p.swingProgress > 0 || mc.gameSettings.thirdPersonView == 0;
        if (t > 0.25f && mexeu || t > Emotes.duracao(a.id)) {
            parar();
            return;
        }
        if (a.id.equals("supernova") && !a.estourou && t >= Emotes.ESTOURO) {
            a.estourou = true;
            estrelas(p);
        }
    }

    private void estrelas(EntityPlayer p) {
        double y = p.posY + 1.2;
        for (int i = 0; i < 48; i++) {
            double ang = i * Math.PI * 2 / 48;
            double sobe = (i % 3 - 1) * 0.12;
            mc.theWorld.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, p.posX, y, p.posZ,
                    Math.cos(ang) * 0.32, sobe + 0.05, Math.sin(ang) * 0.32);
        }
        for (int i = 0; i < 16; i++) {
            mc.theWorld.spawnParticle(EnumParticleTypes.REDSTONE, p.posX + (Math.random() - 0.5) * 1.2, y + Math.random() * 0.8,
                    p.posZ + (Math.random() - 0.5) * 1.2, 0.5, 0.9, 1.0);
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        if (teclaEmotes.isPressed()) mc.displayGuiScreen(new GuiCosmeticos(null, Cosmetico.Tipo.EMOTE));
    }

    @Override
    public void pose(ModelBiped modelo, Entity entidade) {
        if (ativos.isEmpty() || !(entidade instanceof EntityPlayer)) return;
        Ativo a = ativos.get(entidade.getEntityId());
        if (a == null) return;
        Emotes.pose(a.id, (System.currentTimeMillis() - a.inicio) / 1000f, modelo);
        poses++;
    }

    // ---------- camada dos acessórios ----------

    /** Na primeira volta do mundo, põe a camada dos acessórios nos dois bonecos (braço normal e fino). */
    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (camadasPostas) return;
        camadasPostas = true;
        int n = 0;
        for (RenderPlayer r : mc.getRenderManager().getSkinMap().values()) {
            r.addLayer(new CamadaAcessorio(r));
            n++;
        }
        System.out.println("[Pulsar] Cosméticos: camada de acessórios em " + n + " bonecos");
    }
}
