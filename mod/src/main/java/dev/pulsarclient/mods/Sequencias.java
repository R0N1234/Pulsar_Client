package dev.pulsarclient.mods;

import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sequências de Abate: a cada jogador eliminado, o nome da sequência aparece na tela em letras vermelhas com sangue
 * escorrendo, e a voz do Pulsar anuncia (FIRST BLOOD, DOUBLE KILL... GODLIKE). Abates dentro do intervalo
 * escolhido continuam a sequência; passou o tempo, volta para o primeiro.
 *
 * Só conta quando VOCÊ elimina o jogador, do mesmo jeito que o servidor decide de quem é o abate:
 * - Pelo jogo: o seu golpe foi confirmado (o alvo piscou de dano logo depois do seu clique), o jogador morreu de
 *   verdade (vida zerada ou animação de morte) até 5 s depois, e ninguém mais bateu nele depois de você. Jogador que
 *   só saiu de perto, fugiu para longe ou sumiu do mundo não conta; dano de queda, fogo ou veneno depois do seu golpe
 *   continua sendo seu abate, e o golpe de outro jogador depois do seu (assistência) não conta.
 * - Pelo chat do servidor: avisos como "Você matou Fulano", "Fulano foi morto por Você" ou "Rick matou Fulano" contam
 *   na hora; "Fulano foi morto por Outro" cancela o abate que o jogo ia contar. Mensagens de jogadores não valem.
 * O mesmo jogador nunca conta duas vezes (jogo e chat juntos).
 *
 * Os sons vêm no Pulsar e podem ser trocados pelos .ogg da pasta pulsar/sons/sequencias/, em vozes e variações
 * (ver {@link SonsLocais}).
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
    /** Depois do seu último golpe, por quanto tempo a morte do jogador conta como abate seu (o servidor usa 5 s). */
    private static final int JANELA_ABATE = 100;
    /** Até quantos ticks depois do clique o dano no alvo é do seu golpe. */
    private static final int CONFIRMAR = 6;
    /** Quanto o abate visto no jogo espera um aviso do chat dizendo que foi de outro jogador. */
    private static final int ESPERA_CHAT = 6;
    /** Outro jogador a esta distância do alvo, batendo, quando ele levou dano: o golpe foi dele. */
    private static final double PERTO_OUTRO = 6.0;
    private static final int GOTAS = 7;

    private final ModeSetting lista = add(new ModeSetting("lista", "Lista de sequências", 0, "Pulsar", "Arena"));
    private final ModeSetting deteccao = add(new ModeSetting("deteccao", "Como reconhecer o abate", 0, "Jogo e chat do servidor", "Só o chat do servidor", "Só o jogo"));
    private final SliderSetting intervalo = add(new SliderSetting("intervalo", "Tempo entre abates para seguir a sequência", 10, 3, 30, 1, "s"));
    private final SliderSetting duracao = add(new SliderSetting("duracao", "Tempo do texto na tela", 3, 1, 8, 0.5, "s"));
    private final BoolSetting sangue = add(new BoolSetting("sangue", "Sangue escorrendo nas letras", true));
    private final BoolSetting som = add(new BoolSetting("som", "Voz do Pulsar anunciando", true));
    private final SliderSetting volume = add(new SliderSetting("volume", "Volume da voz", 100, 0, 100, 5, "%"));
    /** Voz dos sons locais: a padrão (arquivos soltos) ou uma pasta/prefixo achado em pulsar/sons/sequencias. */
    private final ModeSetting voz = add(new ModeSetting("voz", "Voz (pasta pulsar/sons/sequencias)", 0, VOZ_PADRAO));
    private final ActionSetting testar = add(new ActionSetting("testar", "Testar a próxima sequência", new Runnable() {
        @Override public void run() { abate(); }
    }));
    private final ActionSetting testarTodos = add(new ActionSetting("testar_todos", "Testar todos os sons, um por um", new Runnable() {
        @Override public void run() { testarTodos(); }
    }));
    private final ActionSetting recarregar = add(new ActionSetting("recarregar", "Procurar sons novos na pasta", new Runnable() {
        @Override public void run() { procurarVozes(true); }
    }));
    public static final String VOZ_PADRAO = "Padrão";

    private int ticks;
    private final Map<EntityLivingBase, Alvo> alvos = new HashMap<EntityLivingBase, Alvo>();
    /** Abates vistos no jogo esperando o chat (nome em minúsculas -> tick em que morreu). */
    private final Map<String, Integer> pendentes = new HashMap<String, Integer>();
    /** Jogadores que já contaram (nome em minúsculas -> tick), para jogo e chat não contarem o mesmo abate. */
    private final Map<String, Integer> contados = new HashMap<String, Integer>();
    private Object mundo;
    private int contagem;
    private int ultimoAbate = -100000;
    private int mostrarAte = -1;
    private long mostrouEm;
    private int indice;
    private long semente;

    private static int abates;
    private static int desenhos;
    /** Teste de todos os sons: a próxima sequência a tocar e quando (em ticks), ou -1 parado. */
    private int fila = -1;
    private int proximoTeste;
    private int vozesEm = -1;

    public Sequencias() {
        super("sequencias", "Sequências de Abate", "FIRST BLOOD, DOUBLE KILL... GODLIKE na tela, com sangue escorrendo e a voz do Pulsar",
                Category.COMBATE, CENTRO, 80, false);
        // As vozes da pasta entram na lista já aqui, para a voz salva no config ser reconhecida ao carregar.
        try { procurarVozes(false); } catch (Throwable ignored) {}
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

    /** Quantos sons ainda vão tocar no teste de todos (teste automático). */
    public int naFila() { return fila < 0 ? 0 : nomes().length - fila; }

    public String vozEscolhida() { return voz.index() == 0 ? "" : voz.get(); }

    /** Teste automático: escolhe uma voz pelo nome (ou a padrão). */
    public boolean usarVoz(String nome) {
        procurarVozes(false);
        String[] ops = voz.options();
        for (int i = 0; i < ops.length; i++) {
            if (ops[i].equalsIgnoreCase(nome) || (nome.isEmpty() && i == 0)) {
                voz.set(i);
                SonsLocais.usarVoz(vozEscolhida());
                return true;
            }
        }
        return false;
    }

    // ---------- vozes ----------

    /** Lê a pasta de sons e atualiza a lista de vozes do painel; avisa no log quando pedido ou quando mudou. */
    private void procurarVozes(boolean avisar) {
        List<String> achadas = SonsLocais.vozes();
        String[] ops = new String[achadas.size() + 1];
        ops[0] = VOZ_PADRAO;
        for (int i = 0; i < achadas.size(); i++) ops[i + 1] = achadas.get(i);
        boolean mudou = !Arrays.equals(ops, voz.options());
        if (mudou) voz.setOptions(ops);
        SonsLocais.usarVoz(vozEscolhida());
        if (avisar || mudou) {
            StringBuilder b = new StringBuilder("[Pulsar] Sequências: vozes na pasta: ");
            if (achadas.isEmpty()) b.append("nenhuma (só os arquivos soltos)");
            for (String v : achadas) b.append(v).append(" (").append(SonsLocais.falasNaVoz(v, nomes())).append(" de ").append(nomes().length).append(") ");
            System.out.println(b.toString().trim());
        }
    }

    /** Toca cada sequência da lista, uma atrás da outra, com o texto na tela: para conferir os sons. */
    public void testarTodos() {
        procurarVozes(false);
        fila = 0;
        proximoTeste = ticks;
        contagem = 0;
        ultimoAbate = -100000;
    }

    // ---------- contar os abates ----------

    /** Um jogador em quem você bateu: quando clicou, quando o golpe foi confirmado e quando outro jogador bateu. */
    private static final class Alvo {
        int clique = -100000;
        int golpe = -100000;
        int outro = -100000;
        int dano;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.entityPlayer != mc.thePlayer) return;
        if (!(event.target instanceof EntityPlayer) || event.target == mc.thePlayer) return;
        EntityLivingBase alvo = (EntityLivingBase) event.target;
        Alvo a = alvos.get(alvo);
        if (a == null) {
            a = new Alvo();
            a.dano = alvo.hurtTime;
            alvos.put(alvo, a);
        }
        a.clique = ticks;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.message == null || deteccao.index() == 2) return;
        String linha = EnumChatFormatting.getTextWithoutFormattingCodes(event.message.getUnformattedText());
        if (linha == null) return;
        String[] r = lerChat(linha, mc.thePlayer.getName(), online());
        if (r == null) return;
        String vitima = r[0].toLowerCase(Locale.ROOT);
        if (r[1] == null) {
            // O servidor disse que o abate foi de outro jogador: o que o jogo ia contar não vale.
            if (pendentes.remove(vitima) != null) System.out.println("[Pulsar] Sequências: " + r[0] + " foi de outro jogador (chat)");
            return;
        }
        pendentes.remove(vitima);
        contar(r[0], "chat");
    }

    /** Os nicks do TAB do servidor. */
    private Collection<String> online() {
        List<String> nomes = new ArrayList<String>();
        if (mc.getNetHandler() == null) return nomes;
        for (NetworkPlayerInfo i : mc.getNetHandler().getPlayerInfoMap()) {
            if (i.getGameProfile() != null && i.getGameProfile().getName() != null) nomes.add(i.getGameProfile().getName());
        }
        return nomes;
    }

    /** Morreu de verdade: vida zerada ou animação de morte. Sumir do mundo (isDead) sozinho não é morrer. */
    private static boolean morreu(EntityLivingBase e) {
        return e.getHealth() <= 0 || e.deathTime > 0;
    }

    @Override
    public void tick() {
        ticks++;
        // Vozes novas na pasta aparecem no painel sem reiniciar (a pasta é lida a cada 10 s).
        if (isEnabled() && (vozesEm < 0 || ticks - vozesEm >= 200)) {
            vozesEm = ticks;
            procurarVozes(false);
        }
        if (fila >= 0 && ticks >= proximoTeste) {
            if (fila >= nomes().length || !isEnabled()) {
                fila = -1;
            } else {
                mostrar(fila++, 0);
                // Cada fala espera o texto sumir (ou pelo menos 2,5 s) antes da próxima.
                proximoTeste = ticks + Math.max(50, (int) Math.round(duracao.get() * 20));
            }
        }
        if (mc.thePlayer == null || mc.theWorld == null || mc.theWorld != mundo) {
            // Saiu do servidor ou trocou de mundo: os alvos antigos não existem mais.
            mundo = mc.theWorld;
            alvos.clear();
            pendentes.clear();
            if (mc.thePlayer == null || mc.theWorld == null) contagem = 0;
            return;
        }
        if (!isEnabled()) {
            alvos.clear();
            pendentes.clear();
            return;
        }
        Iterator<Map.Entry<EntityLivingBase, Alvo>> it = alvos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<EntityLivingBase, Alvo> e = it.next();
            EntityLivingBase alvo = e.getKey();
            Alvo a = e.getValue();
            if (alvo.hurtTime > a.dano) {
                // Levou dano agora: do seu golpe (logo depois do clique) ou de outro jogador batendo nele.
                if (ticks - a.clique <= CONFIRMAR) a.golpe = ticks;
                else if (outroBatendo(alvo)) a.outro = ticks;
            }
            a.dano = alvo.hurtTime;
            if (morreu(alvo)) {
                it.remove();
                boolean meu = ticks - a.golpe <= JANELA_ABATE && a.golpe >= a.outro;
                if (meu && deteccao.index() != 1) {
                    pendentes.put(alvo.getName().toLowerCase(Locale.ROOT), ticks);
                } else if (!meu && a.golpe > -100000) {
                    System.out.println("[Pulsar] Sequências: " + alvo.getName() + " morreu, mas o último golpe não foi seu");
                }
            } else if (alvo.isDead || ticks - Math.max(a.clique, a.golpe) > JANELA_ABATE) {
                // Saiu de perto, sumiu do mundo ou a luta acabou sem morte: não é abate.
                it.remove();
            }
        }
        Iterator<Map.Entry<String, Integer>> p = pendentes.entrySet().iterator();
        while (p.hasNext()) {
            Map.Entry<String, Integer> e = p.next();
            if (ticks - e.getValue() < ESPERA_CHAT) continue;
            p.remove();
            contar(e.getKey(), "jogo");
        }
        Iterator<Map.Entry<String, Integer>> c = contados.entrySet().iterator();
        while (c.hasNext()) if (ticks - c.next().getValue() > JANELA_ABATE) c.remove();
        // Você morreu: a sequência acaba.
        if (mc.thePlayer.getHealth() <= 0 && contagem > 0) {
            contagem = 0;
            ultimoAbate = -100000;
        }
    }

    /** Outro jogador (nem você nem o alvo) perto do alvo e no meio de um golpe. */
    private boolean outroBatendo(EntityLivingBase alvo) {
        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer || p == alvo || p.isDead) continue;
            if (p.getDistanceSqToEntity(alvo) <= PERTO_OUTRO * PERTO_OUTRO && (p.isSwingInProgress || p.swingProgressInt > 0)) return true;
        }
        return false;
    }

    /** Conta o abate de um jogador, se ele ainda não contou há pouco (pelo jogo ou pelo chat). */
    private void contar(String nome, String fonte) {
        String chave = nome.toLowerCase(Locale.ROOT);
        Integer antes = contados.get(chave);
        if (antes != null && ticks - antes <= JANELA_ABATE) return;
        contados.put(chave, ticks);
        System.out.println("[Pulsar] Sequências: " + nome + " eliminado (" + fonte + ")");
        abate();
    }

    // ---------- abates no chat do servidor ----------

    private static final String NOME = "([A-Za-z0-9_]{1,16})";
    private static final String VERBO = "(?:matou|eliminou|abateu|assassinou|derrubou|finalizou|killed|slew|eliminated|murdered)";
    private static final String MORTO = "(?:foi |was )?(?:morto|morta|eliminad[oa]|abatid[oa]|assassinad[oa]|derrubad[oa]|finalizad[oa]|slain|killed|eliminated|murdered)";
    /** "Você matou Fulano", "You killed Fulano". */
    private static final Pattern VOCE_MATOU = Pattern.compile("(?i)\\b(?:voc[eê]|vc|you)\\s+" + VERBO + "\\s+(?:o jogador\\s+|a jogadora\\s+|the player\\s+)?" + NOME + "\\b");
    /** "Fulano foi morto por Rick", "Fulano was slain by Rick", "Fulano morreu para Rick" (Rick pode ser qualquer um). */
    private static final Pattern MORTO_POR = Pattern.compile("(?i)\\b(?!(?:foi|was)\\b)" + NOME + "(?:\\s*\\([^)]*\\))?\\s+(?:" + MORTO + "|morreu)\\s+(?:por|pelo|pela|para|pra|by)\\s+(?:o jogador\\s+|the player\\s+)?(voc[eê]|[A-Za-z0-9_]{1,16})(?![\\p{L}\\p{N}_])");
    /** "Rick matou Fulano" (o primeiro nome é quem matou). */
    private static final Pattern MATOU = Pattern.compile("(?i)\\b" + NOME + "\\s+" + VERBO + "\\s+(?:o jogador\\s+|a jogadora\\s+|the player\\s+)?" + NOME + "\\b");
    /** Mensagem de jogador: o texto antes do aviso termina com um nick e ":", "»", ">" ou "->". */
    private static final Pattern FALA = Pattern.compile("([A-Za-z0-9_]{3,16})\\s*(?::|»|>|->)");

    /**
     * Lê um aviso de abate do chat. Devolve {vítima, quem matou} com quem matou = seu nick quando foi você,
     * {vítima, null} quando foi outro jogador, ou null quando a linha não é um aviso de abate.
     * online: os nicks do servidor (TAB), para reconhecer mensagens de jogadores; null trata qualquer nick como jogador.
     */
    public static String[] lerChat(String linha, String eu, Collection<String> online) {
        Matcher m = VOCE_MATOU.matcher(linha);
        if (m.find() && !fala(linha, m.start(), online)) return vitima(m.group(1), eu, eu);
        m = MORTO_POR.matcher(linha);
        if (m.find() && !fala(linha, m.start(), online)) {
            String quem = m.group(2);
            boolean fui = quem.equalsIgnoreCase(eu) || quem.matches("(?i)voc[eê]|vc|you");
            return vitima(m.group(1), fui ? eu : null, eu);
        }
        m = MATOU.matcher(linha);
        if (m.find() && !fala(linha, m.start(), online)) {
            String quem = m.group(1);
            if (quem.matches("(?i)voc[eê]|vc|you|que|quem|who")) return null;
            return vitima(m.group(2), quem.equalsIgnoreCase(eu) ? eu : null, eu);
        }
        return null;
    }

    private static String[] vitima(String nome, String matador, String eu) {
        if (nome == null || nome.equalsIgnoreCase(eu) || nome.matches("(?i)voc[eê]|vc|you|o|a|um|uma|the|por|by")) return null;
        return new String[]{nome, matador};
    }

    /** O aviso está dentro de uma mensagem que um jogador mandou no chat ("Nick: ...", "<Nick> ...", "Nick » ..."). */
    private static boolean fala(String linha, int inicio, Collection<String> online) {
        Matcher f = FALA.matcher(linha.substring(0, inicio));
        while (f.find()) {
            if (online == null) return true;
            for (String n : online) if (n.equalsIgnoreCase(f.group(1))) return true;
        }
        return false;
    }

    /** Mais um abate: segue a sequência se foi dentro do intervalo, senão começa de novo. */
    public void abate() {
        contagem = ticks - ultimoAbate <= intervalo.getInt() * 20 ? contagem + 1 : 1;
        ultimoAbate = ticks;
        abates++;
        mostrar(Math.min(contagem, nomes().length) - 1, contagem);
    }

    /** Mostra a sequência "i" na tela e toca a voz dela (contagem só para o log). */
    private void mostrar(int i, int contagemLog) {
        indice = i;
        mostrarAte = ticks + (int) Math.round(duracao.get() * 20);
        mostrouEm = System.currentTimeMillis();
        semente = mostrouEm;
        java.io.File local = SonsLocais.escolher(nomes()[indice]);
        System.out.println("[Pulsar] Sequências: " + textos()[indice] + (contagemLog > 0 ? " (" + contagemLog + ")" : " (teste)")
                + (local != null ? " · som local " + local.getName() : " · voz do Pulsar"));
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
