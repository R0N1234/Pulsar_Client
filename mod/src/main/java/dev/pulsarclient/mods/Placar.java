package dev.pulsarclient.mods;

import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.SliderSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.GuiIngameForge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Placar: o placar lateral dos servidores, que pode ser arrastado, mudar de tamanho e esconder os números vermelhos.
 * Com o mod ligado o placar do jogo é escondido e este é desenhado no lugar, com a mesma regra de quais linhas mostrar.
 */
public class Placar extends HudMod {
    private static final int LINHA = 9;
    private static final int MAX_LINHAS = 15;

    private final BoolSetting numeros = add(new BoolSetting("numeros", "Mostrar os números vermelhos", false));
    private final SliderSetting escala = add(new SliderSetting("escala", "Tamanho", 100, 50, 150, 5, "%"));
    private final SliderSetting opacidade = add(new SliderSetting("opacidade", "Fundo", 30, 0, 80, 5, "%"));

    private boolean escondeuDoJogo;

    /** Linhas prontas para desenhar: nome (com as cores do time) e pontos. */
    private static final class Linha {
        final String nome;
        final String pontos;
        Linha(String nome, String pontos) { this.nome = nome; this.pontos = pontos; }
    }

    private String titulo = "";
    private final List<Linha> linhas = new ArrayList<Linha>();
    // O placar é montado no máximo uma vez por tick (20 vezes por segundo), não a cada quadro.
    private int ticks;
    private int montadoEm = -1;
    private boolean montadoExemplo;
    private boolean temPlacar;
    private int largura;

    public Placar() {
        super("placar", "Placar", "Placar lateral do servidor: mover, mudar o tamanho e tirar os números", Category.HUD, 10000, 60, false);
        color.set(5);
    }

    @Override
    public void tick() {
        ticks++;
        boolean esconder = isEnabled();
        if (esconder != escondeuDoJogo) {
            escondeuDoJogo = esconder;
            GuiIngameForge.renderObjective = !esconder;
        }
    }

    @Override
    public void onBroken() {
        // Com erro, o placar do próprio jogo volta, para o jogador não ficar sem placar.
        GuiIngameForge.renderObjective = true;
        escondeuDoJogo = false;
    }

    /** O mesmo objetivo que o jogo mostraria: o da cor do time do jogador, ou o da lateral. */
    private ScoreObjective objetivo() {
        if (mc.theWorld == null || mc.thePlayer == null) return null;
        Scoreboard placar = mc.theWorld.getScoreboard();
        ScoreObjective objetivo = null;
        ScorePlayerTeam time = placar.getPlayersTeam(mc.thePlayer.getName());
        if (time != null) {
            int slot = time.getChatFormat().getColorIndex();
            if (slot >= 0) objetivo = placar.getObjectiveInDisplaySlot(3 + slot);
        }
        return objetivo != null ? objetivo : placar.getObjectiveInDisplaySlot(1);
    }

    private boolean montar(boolean preview) {
        if (montadoEm == ticks && montadoExemplo == preview) return temPlacar;
        montadoEm = ticks;
        montadoExemplo = preview;
        temPlacar = montarAgora(preview);
        largura = temPlacar ? medir() : 0;
        return temPlacar;
    }

    private boolean montarAgora(boolean preview) {
        linhas.clear();
        ScoreObjective objetivo = objetivo();
        if (objetivo == null) {
            if (!preview) return false;
            titulo = "§d§lPULSAR";
            linhas.add(new Linha("§7Mapa: §fÓrbita", "5"));
            linhas.add(new Linha("§7Abates: §a12", "4"));
            linhas.add(new Linha(" ", "3"));
            linhas.add(new Linha("§7Jogadores: §f8", "2"));
            linhas.add(new Linha("§bpulsarclient.dev", "1"));
            return true;
        }
        Scoreboard placar = objetivo.getScoreboard();
        List<Score> validos = new ArrayList<Score>();
        for (Score s : placar.getSortedScores(objetivo)) {
            if (s.getPlayerName() != null && !s.getPlayerName().startsWith("#")) validos.add(s);
        }
        // O jogo mostra as 15 maiores pontuações; a lista vem da menor para a maior.
        Collection<Score> mostrar = validos.size() > MAX_LINHAS ? validos.subList(validos.size() - MAX_LINHAS, validos.size()) : validos;
        for (Score s : mostrar) {
            ScorePlayerTeam time = placar.getPlayersTeam(s.getPlayerName());
            linhas.add(0, new Linha(ScorePlayerTeam.formatPlayerName(time, s.getPlayerName()), String.valueOf(s.getScorePoints())));
        }
        titulo = objetivo.getDisplayName();
        return true;
    }

    private int medir() {
        int w = font().getStringWidth(titulo);
        for (Linha l : linhas) {
            int lw = font().getStringWidth(l.nome);
            if (numeros.get()) lw += font().getStringWidth(": " + l.pontos);
            w = Math.max(w, lw);
        }
        return w + 4;
    }

    private int alturaBase() {
        return (linhas.size() + 1) * LINHA + 1;
    }

    private float escala() {
        return (float) (escala.get() / 100.0);
    }

    private boolean noEditor() {
        return mc.currentScreen instanceof GuiHudEditor;
    }

    @Override
    public int width() {
        montar(noEditor());
        return Math.max(1, Math.round(largura * escala()));
    }

    @Override
    public int height() {
        montar(noEditor());
        return Math.max(1, Math.round(alturaBase() * escala()));
    }

    @Override
    public void render(boolean preview) {
        if (!montar(preview)) return;
        int w = largura;
        int fundo = (int) Math.round(opacidade.get() * 2.55) << 24;
        int fundoTitulo = Math.min(255, (int) Math.round(opacidade.get() * 2.55) + 16) << 24;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(escala(), escala(), 1f);
        if (background.get() && opacidade.get() > 0) {
            Gui.drawRect(0, 0, w, LINHA + 1, fundoTitulo);
            Gui.drawRect(0, LINHA + 1, w, alturaBase(), fundo);
        }
        font().drawString(titulo, (w - font().getStringWidth(titulo)) / 2, 1, WHITE);
        int ly = LINHA + 1;
        for (Linha l : linhas) {
            font().drawString(l.nome, 2, ly, WHITE);
            if (numeros.get()) font().drawString(l.pontos, w - 2 - font().getStringWidth(l.pontos), ly, accent());
            ly += LINHA;
        }
        GlStateManager.popMatrix();
    }
}
