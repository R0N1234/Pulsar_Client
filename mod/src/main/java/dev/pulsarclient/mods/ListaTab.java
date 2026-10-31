package dev.pulsarclient.mods;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ModeSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lista do TAB do Pulsar: cada jogador com a cabeça da skin que está usando, em qualquer servidor
 * (o jogo só mostra as cabeças em servidor original), e o ping em número colorido.
 * Mesma ordem, colunas e textos de cima e de baixo que o servidor manda para a lista do jogo.
 */
public class ListaTab extends PulsarMod {
    private static final int LINHA = 10;
    private static final int MAX_JOGADORES = 80;
    private static final int MAX_POR_COLUNA = 20;
    private static final int FUNDO = 0x80000000;
    private static final int CELULA = 0x20FFFFFF;
    private static final int CELULA_VOCE = 0x349B7CFF;

    /** Mesma ordem do jogo: espectadores no fim, depois por time e por nome. */
    private static final Ordering<NetworkPlayerInfo> ORDEM = Ordering.from(new java.util.Comparator<NetworkPlayerInfo>() {
        @Override
        public int compare(NetworkPlayerInfo a, NetworkPlayerInfo b) {
            ScorePlayerTeam ta = a.getPlayerTeam();
            ScorePlayerTeam tb = b.getPlayerTeam();
            return ComparisonChain.start()
                    .compareTrueFirst(a.getGameType() != WorldSettings.GameType.SPECTATOR, b.getGameType() != WorldSettings.GameType.SPECTATOR)
                    .compare(ta != null ? ta.getRegisteredName() : "", tb != null ? tb.getRegisteredName() : "")
                    .compare(a.getGameProfile().getName(), b.getGameProfile().getName())
                    .result();
        }
    });

    private final ModeSetting ping = add(new ModeSetting("ping", "Ping", 0, "Número", "Barrinhas", "Esconder"));
    private final BoolSetting destacar = add(new BoolSetting("voce", "Destacar o seu nome", true));

    private Field campoTopo;
    private Field campoRodape;
    private boolean semCampos;

    public ListaTab() {
        super("lista_tab", "Lista do TAB", "Jogadores do TAB com a cabeça da skin e o ping em número", Category.HUD, true);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST || !isEnabled()) return;
        if (mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null) return;
        event.setCanceled(true);
        Scoreboard placar = mc.theWorld.getScoreboard();
        desenhar(event.resolution.getScaledWidth(), mc.getNetHandler().getPlayerInfoMap(), placar, placar.getObjectiveInDisplaySlot(0));
    }

    /** Desenha a lista no topo da tela. Público para o autoteste, que não tem outros jogadores no mundo. */
    public void desenhar(int largura, Collection<NetworkPlayerInfo> todos, Scoreboard placar, ScoreObjective objetivo) {
        FontRenderer fr = mc.fontRendererObj;
        GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
        List<NetworkPlayerInfo> jogadores = ORDEM.sortedCopy(todos);
        if (jogadores.size() > MAX_JOGADORES) jogadores = jogadores.subList(0, MAX_JOGADORES);
        int total = jogadores.size();

        // Largura de cada pedaço da célula: nome, pontos do placar do TAB e ping.
        int nomeMax = 0;
        int pontosMax = 0;
        int pingMax = 0;
        List<String> nomes = new ArrayList<String>(total);
        // Rede do Pulsar: ícone PC antes do nome e o nível depois, para quem usa o Pulsar.
        dev.pulsarclient.mods.IconePulsar icone = (dev.pulsarclient.mods.IconePulsar) dev.pulsarclient.PulsarClient.get().mod("icone_pulsar");
        dev.pulsarclient.mods.Nivel nivelMod = (dev.pulsarclient.mods.Nivel) dev.pulsarclient.PulsarClient.get().mod("nivel");
        boolean comIcone = icone != null && icone.noTab();
        boolean comNivel = nivelMod != null && nivelMod.noTab();
        int[] niveis = new int[total];
        for (int i = 0; i < total; i++) niveis[i] = comIcone || comNivel ? dev.pulsarclient.rede.Rede.get().nivelDe(jogadores.get(i).getGameProfile()) : 0;
        for (int i = 0; i < total; i++) {
            NetworkPlayerInfo j = jogadores.get(i);
            String nome = tab.getPlayerName(j);
            if (j.getGameType() == WorldSettings.GameType.SPECTATOR) nome = EnumChatFormatting.ITALIC + nome;
            nomes.add(nome);
            int extra = niveis[i] > 0 ? (comIcone ? 10 : 0) + (comNivel ? fr.getStringWidth(" " + niveis[i]) : 0) : 0;
            nomeMax = Math.max(nomeMax, fr.getStringWidth(nome) + extra);
            if (objetivo != null) pontosMax = Math.max(pontosMax, fr.getStringWidth(" " + pontos(placar, objetivo, j)));
            if (ping.index() == 0) pingMax = Math.max(pingMax, fr.getStringWidth(textoPing(j.getResponseTime())));
        }
        if (ping.index() == 1) pingMax = 10;

        int linhas = total;
        int colunas = 1;
        while (linhas > MAX_POR_COLUNA) {
            colunas++;
            linhas = (total + colunas - 1) / colunas;
        }
        int celula = Math.min(colunas * (11 + nomeMax + pontosMax + (pingMax > 0 ? pingMax + 4 : 0) + 3), largura - 50) / colunas;
        int conteudo = celula * colunas + (colunas - 1) * 5;
        int largo = conteudo;

        List<String> topo = linhas(tab, true, largura - 50);
        List<String> rodape = linhas(tab, false, largura - 50);
        for (String s : topo) largo = Math.max(largo, fr.getStringWidth(s));
        for (String s : rodape) largo = Math.max(largo, fr.getStringWidth(s));

        int meio = largura / 2;
        int y = 10;
        if (!topo.isEmpty()) {
            Gui.drawRect(meio - largo / 2 - 3, y - 3, meio + largo / 2 + 3, y + topo.size() * fr.FONT_HEIGHT + 1, FUNDO);
            for (String s : topo) {
                fr.drawStringWithShadow(s, meio - fr.getStringWidth(s) / 2, y, 0xFFFFFFFF);
                y += fr.FONT_HEIGHT;
            }
            y += 3;
        }

        int x0 = meio - conteudo / 2;
        Gui.drawRect(meio - largo / 2 - 3, y - 2, meio + largo / 2 + 3, y + linhas * LINHA + 1, FUNDO);
        for (int i = 0; i < total; i++) {
            NetworkPlayerInfo j = jogadores.get(i);
            int cx = x0 + (i / linhas) * (celula + 5);
            int cy = y + (i % linhas) * LINHA;
            boolean voce = destacar.get() && j.getGameProfile().getId() != null && j.getGameProfile().getId().equals(mc.thePlayer.getUniqueID());
            Gui.drawRect(cx, cy, cx + celula, cy + LINHA - 1, voce ? CELULA_VOCE : CELULA);
            cabeca(j, cx + 1, cy);
            boolean espectador = j.getGameType() == WorldSettings.GameType.SPECTATOR;
            int nx = cx + 11;
            if (niveis[i] > 0 && comIcone) {
                dev.pulsarclient.gui.Universo.icone(nx, cy, 8);
                nx += 10;
            }
            nx = fr.drawStringWithShadow(nomes.get(i), nx, cy + 1, espectador ? 0x90FFFFFF : 0xFFFFFFFF);
            if (niveis[i] > 0 && comNivel) fr.drawStringWithShadow(String.valueOf(niveis[i]), nx + 1, cy + 1, dev.pulsarclient.rede.Selos.COR_NIVEL);
            int direita = cx + celula - 2;
            if (ping.index() == 0) {
                String p = textoPing(j.getResponseTime());
                fr.drawStringWithShadow(p, direita - fr.getStringWidth(p), cy + 1, corPing(j.getResponseTime()));
                direita -= pingMax + 4;
            } else if (ping.index() == 1) {
                barrinhas(direita - 10, cy, j.getResponseTime());
                direita -= pingMax + 4;
            }
            if (objetivo != null && !espectador) {
                String s = String.valueOf(pontos(placar, objetivo, j));
                fr.drawStringWithShadow(s, direita - fr.getStringWidth(s), cy + 1, 0xFFFFE066);
            }
        }
        y += linhas * LINHA + 1;

        if (!rodape.isEmpty()) {
            y += 3;
            Gui.drawRect(meio - largo / 2 - 3, y - 2, meio + largo / 2 + 3, y + rodape.size() * fr.FONT_HEIGHT + 2, FUNDO);
            for (String s : rodape) {
                fr.drawStringWithShadow(s, meio - fr.getStringWidth(s) / 2, y, 0xFFFFFFFF);
                y += fr.FONT_HEIGHT;
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Cabeça 8x8 da skin, com a camada do chapéu por cima. */
    private void cabeca(NetworkPlayerInfo j, int x, int y) {
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getTextureManager().bindTexture(j.getLocationSkin());
        Gui.drawScaledCustomSizeModalRect(x, y + 1, 8f, 8f, 8, 8, 8, 8, 64f, 64f);
        EntityPlayer p = j.getGameProfile().getId() == null ? null : mc.theWorld.getPlayerEntityByUUID(j.getGameProfile().getId());
        if (p == null || p.isWearing(EnumPlayerModelParts.HAT)) {
            Gui.drawScaledCustomSizeModalRect(x, y + 1, 40f, 8f, 8, 8, 8, 8, 64f, 64f);
        }
    }

    private void barrinhas(int x, int y, int ms) {
        int cheias = ms < 0 ? 0 : ms < 150 ? 5 : ms < 300 ? 4 : ms < 600 ? 3 : ms < 1000 ? 2 : 1;
        int cor = corPing(ms);
        for (int i = 0; i < 5; i++) {
            int h = 2 + i;
            Gui.drawRect(x + i * 2, y + 8 - h, x + i * 2 + 1, y + 8, i < cheias ? cor : 0x60FFFFFF);
        }
    }

    private static String textoPing(int ms) {
        return ms < 0 ? "?" : ms > 9999 ? "9999+" : ms + "ms";
    }

    private static int corPing(int ms) {
        if (ms < 0) return 0xFFAAAAAA;
        if (ms < 80) return 0xFF6EE7A8;
        if (ms < 150) return 0xFFC8E86E;
        if (ms < 300) return 0xFFFFD84A;
        if (ms < 600) return 0xFFFF8A3D;
        return 0xFFFF5A5A;
    }

    private static int pontos(Scoreboard placar, ScoreObjective objetivo, NetworkPlayerInfo j) {
        // Só lê: getValueFromObjective sozinho criaria uma linha nova no placar para quem não tem pontos.
        String nome = j.getGameProfile().getName();
        return placar.entityHasObjective(nome, objetivo) ? placar.getValueFromObjective(nome, objetivo).getScorePoints() : 0;
    }

    /** Textos de cima e de baixo da lista, que o servidor manda (ficam guardados na lista do jogo). */
    private List<String> linhas(GuiPlayerTabOverlay tab, boolean topo, int largura) {
        List<String> out = new ArrayList<String>();
        if (semCampos) return out;
        try {
            if (campoTopo == null) {
                campoTopo = ReflectionHelper.findField(GuiPlayerTabOverlay.class, "header", "field_175256_i");
                campoRodape = ReflectionHelper.findField(GuiPlayerTabOverlay.class, "footer", "field_175255_h");
            }
            IChatComponent texto = (IChatComponent) (topo ? campoTopo : campoRodape).get(tab);
            if (texto != null) out.addAll(mc.fontRendererObj.listFormattedStringToWidth(texto.getFormattedText(), largura));
        } catch (Throwable t) {
            semCampos = true;
            System.err.println("[Pulsar] Lista do TAB: textos do servidor indisponíveis: " + t);
        }
        return out;
    }
}
