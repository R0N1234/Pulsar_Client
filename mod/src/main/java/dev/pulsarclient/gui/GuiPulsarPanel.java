package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.Category;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.ColorSetting;
import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.Setting;
import dev.pulsarclient.settings.SliderSetting;
import dev.pulsarclient.settings.TeclaDoJogo;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel do Pulsar (Shift direito): categorias à esquerda, mods em cartões com ícone e as opções de cada mod.
 * Feito com caixas arredondadas, ícones de uma imagem só e texto: nada de sombra ou desfoque, para não pesar no FPS.
 */
public class GuiPulsarPanel extends GuiScreen {
    private static final int BG = 0xF20A0F1E;
    private static final int SIDEBAR = 0xF2060A14;
    private static final int CARD = 0xFF111A2D;
    private static final int CARD_HOVER = 0xFF182440;
    private static final int FIELD = 0xFF0B1222;
    private static final int BUTTON_OFF = 0xFF1C2540;
    private static final int LINE = 0x337FE7FF;
    private static final int BEAM = 0xFF7FE7FF;
    private static final int TEXT = 0xFFE6ECFF;
    private static final int MUTED = 0xFF8892B0;
    private static final int DIM = 0xFF4A5578;
    private static final int DARK = 0xFF0A1020;
    private static final int OFF = 0xFF353E5A;
    private static final int AMBER = 0xFFFFB347;
    private static final int GOLD = 0xFFFFD84A;
    private static final int CARD_H = 74;
    private static final int CARD_MIN_W = 92;
    private static final int ROW_H = 24;
    private static final int GAP = 6;

    private final GuiScreen parent;
    private Category category;
    private boolean favorites;
    private PulsarMod open;
    private GuiTextField search;
    private int scroll;
    // Barra de rolagem: segurando o mouse nela, a lista acompanha (-1 = não está arrastando).
    private int barraAgarrada = -1;
    private int ultimoMouseX, ultimoMouseY;
    private int contentHeight;
    private SliderSetting dragging;
    private int dragX, dragW;
    private ActionSetting flashed;
    private long flashUntil;
    private KeySetting listening;
    private TextSetting editing;
    private GuiTextField field;
    // Paleta de cores aberta: matiz, saturação e brilho sendo escolhidos, e a parte arrastada (1 = quadrado, 2 = barra).
    private ColorSetting picking;
    private float pickH, pickS, pickB;
    private int pickDrag;
    // Aberto direto nas opções de um mod (pelo editor de HUD): voltar fecha o painel.
    private boolean direct;

    // Painel calculado em initGui
    private int px, py, pw, ph, sideW;
    private int mx, mw, top, bottom;

    public GuiPulsarPanel(GuiScreen parent) {
        this(parent, null, null);
    }

    public GuiPulsarPanel(GuiScreen parent, Category category, PulsarMod open) {
        this.parent = parent;
        this.category = category;
        this.open = open;
    }

    /** Só as opções de um mod; "Voltar" e Esc voltam para a tela de antes. */
    public static GuiPulsarPanel options(GuiScreen parent, PulsarMod mod) {
        GuiPulsarPanel p = new GuiPulsarPanel(parent, mod.category(), mod);
        p.direct = true;
        return p;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(540, width - 16);
        ph = Math.min(320, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        sideW = pw >= 400 ? 104 : 90;
        mx = px + sideW + 10;
        mw = pw - sideW - 22;
        layout();
        String text = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRendererObj, px + pw - 134, py + 12, 118, 10);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(24);
        search.setText(text);
        String typing = field == null ? "" : field.getText();
        field = new GuiTextField(1, fontRendererObj, 0, 0, 162, 10);
        field.setEnableBackgroundDrawing(false);
        field.setMaxStringLength(editing == null ? 100 : editing.maxLength());
        field.setText(typing);
        field.setFocused(editing != null);
    }

    /** A lista de mods tem um rodapé com a descrição; as opções têm um cabeçalho mais alto. */
    private void layout() {
        top = open == null ? py + 32 : py + 46;
        bottom = open == null ? py + ph - 22 : py + ph - 8;
    }

    @Override
    public void updateScreen() {
        search.updateCursorCounter();
        field.updateCursorCounter();
    }

    // ---------- desenho ----------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ultimoMouseX = mouseX;
        ultimoMouseY = mouseY;
        layout();
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x66000000);
        Desenho.caixaBorda(px, py, px + pw, py + ph, BG, LINE, 8);
        drawSidebar(mouseX, mouseY);

        if (open == null) drawGrid(mouseX, mouseY);
        else drawSettings(mouseX, mouseY);
        if (picking != null) drawPicker();
    }

    private void drawSidebar(int mouseX, int mouseY) {
        Desenho.caixa(px + 4, py + 4, px + sideW, py + ph - 4, SIDEBAR, 6);
        Universo.logo(px + 9, py + 8, 26);
        fontRendererObj.drawString("Pulsar", px + 38, py + 11, TEXT);
        fontRendererObj.drawString("client 1.8.9", px + 38, py + 21, MUTED);

        boolean naLista = open == null;
        int y = py + 42;
        y = sideItem("Todos", Icones.GRADE, naLista && category == null && !favorites, y, mouseX, mouseY);
        y = sideItem("Favoritos", Icones.ESTRELA, naLista && favorites, y, mouseX, mouseY);
        for (Category c : Category.values()) {
            y = sideItem(c.label, Icones.daCategoria(c), naLista && !favorites && category == c, y, mouseX, mouseY);
        }
        drawRect(px + 12, y + 1, px + sideW - 8, y + 2, LINE);
        int fim = sideItem("Texturas", Icones.QUADRO, false, y + 4, mouseX, mouseY);

        int by = py + ph - 26;
        // Em telas baixas a contagem sai para não encostar na lista.
        if (fim + 2 <= by - 12) {
            int on = 0;
            for (PulsarMod m : PulsarClient.get().mods()) if (m.isEnabled()) on++;
            drawCenteredString(fontRendererObj, on + " ligados", px + 4 + (sideW - 4) / 2, by - 12, MUTED);
        }
        boolean hover = inside(mouseX, mouseY, px + 10, by, px + sideW - 6, by + 17);
        Desenho.caixaBorda(px + 10, by, px + sideW - 6, by + 17, hover ? 0xFF1C2A48 : CARD, hover ? BEAM : LINE, 4);
        drawCenteredString(fontRendererObj, "Editar HUD", px + 2 + sideW / 2, by + 5, TEXT);
    }

    /** Altura de cada item da barra lateral: menor em telas baixas, para caber tudo. */
    private int sideStep() {
        return ph >= 270 ? 17 : 15;
    }

    private int sideItem(String label, int icon, boolean selected, int y, int mouseX, int mouseY) {
        int h = sideStep() - 2;
        boolean hover = inside(mouseX, mouseY, px + 8, y, px + sideW - 4, y + h);
        if (selected) Desenho.caixa(px + 8, y, px + sideW - 4, y + h, 0x2E7FE7FF, 4);
        else if (hover) Desenho.caixa(px + 8, y, px + sideW - 4, y + h, 0x14FFFFFF, 4);
        int c = selected ? BEAM : hover ? TEXT : MUTED;
        Icones.desenhar(icon, px + 13, y + (h - 9) / 2f, 9, c);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(label, sideW - 32), px + 27, y + (h - 8) / 2 + 1, c);
        return y + sideStep();
    }

    private List<PulsarMod> visibleMods() {
        String q = search.getText().trim().toLowerCase();
        List<PulsarMod> fav = new ArrayList<PulsarMod>();
        List<PulsarMod> out = new ArrayList<PulsarMod>();
        for (PulsarMod m : PulsarClient.get().mods()) {
            if (favorites && !m.favorite()) continue;
            if (!favorites && category != null && m.category() != category) continue;
            if (!q.isEmpty() && !(m.name() + " " + m.description()).toLowerCase().contains(q)) continue;
            (m.favorite() ? fav : out).add(m);
        }
        // Favoritos sempre primeiro.
        fav.addAll(out);
        return fav;
    }

    private int columns() {
        return Math.max(2, Math.min(5, (mw + GAP) / (CARD_MIN_W + GAP)));
    }

    private int cardWidth() {
        return (mw - GAP * (columns() - 1)) / columns();
    }

    private String gridTitle() {
        return favorites ? "Favoritos" : category == null ? "Todos os mods" : category.label;
    }

    private void drawGrid(int mouseX, int mouseY) {
        fontRendererObj.drawString(gridTitle(), mx, py + 12, TEXT);
        int sx = px + pw - 150;
        Desenho.caixaBorda(sx, py + 8, px + pw - 12, py + 24, FIELD, search.isFocused() ? BEAM : LINE, 5);
        Icones.desenhar(Icones.LUPA, sx + 5, py + 11.5f, 9, search.isFocused() ? BEAM : MUTED);
        if (search.getText().isEmpty() && !search.isFocused()) fontRendererObj.drawString("Buscar mod...", sx + 16, py + 12, MUTED);
        search.drawTextBox();

        List<PulsarMod> mods = visibleMods();
        int cols = columns();
        int cw = cardWidth();
        int rows = (mods.size() + cols - 1) / cols;
        contentHeight = rows * (CARD_H + GAP) - GAP;
        clampScroll();

        PulsarMod hovered = null;
        if (mods.isEmpty()) {
            String vazio = favorites ? "Nenhum favorito ainda: clique na estrela de um mod" : "Nenhum mod encontrado";
            drawCenteredString(fontRendererObj, vazio, mx + mw / 2, top + 30, MUTED);
        } else {
            boolean inView = mouseY >= top && mouseY < bottom;
            scissor(mx, top, mw, bottom - top);
            for (int i = 0; i < mods.size(); i++) {
                int x = mx + (i % cols) * (cw + GAP);
                int y = top + (i / cols) * (CARD_H + GAP) - scroll;
                if (y + CARD_H < top || y > bottom) continue;
                boolean hover = inView && inside(mouseX, mouseY, x, y, x + cw, y + CARD_H);
                if (hover) hovered = mods.get(i);
                drawCard(mods.get(i), x, y, cw, hover, mouseX, mouseY);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            drawScrollbar();
        }
        // Rodapé: a descrição do mod embaixo do mouse.
        String rodape;
        if (hovered != null) {
            rodape = hovered.description();
        } else {
            int on = 0;
            for (PulsarMod m : mods) if (m.isEnabled()) on++;
            rodape = on + " de " + mods.size() + " ligados  ·  botão direito ou engrenagem abre as opções";
        }
        drawRect(mx, py + ph - 20, mx + mw, py + ph - 19, LINE);
        fontRendererObj.drawString(caber(rodape, mw), mx, py + ph - 14, hovered != null ? TEXT : MUTED);
    }

    private void drawCard(PulsarMod mod, int x, int y, int w, boolean hover, int mouseX, int mouseY) {
        boolean on = mod.isEnabled();
        if (on) Desenho.caixaBorda(x, y, x + w, y + CARD_H, hover ? CARD_HOVER : CARD, 0x557FE7FF, 5);
        else Desenho.caixa(x, y, x + w, y + CARD_H, hover ? CARD_HOVER : CARD, 5);
        boolean star = hover && inside(mouseX, mouseY, x, y, x + 18, y + 18);
        Icones.desenhar(mod.favorite() ? Icones.ESTRELA : Icones.ESTRELA_VAZIA, x + 5, y + 5, 9,
                mod.favorite() ? GOLD : star ? TEXT : hover ? MUTED : DIM);
        boolean gear = hover && inside(mouseX, mouseY, x + w - 18, y, x + w, y + 18);
        Icones.desenhar(Icones.ENGRENAGEM, x + w - 14, y + 5, 9, gear ? BEAM : hover ? MUTED : DIM);
        Icones.desenhar(Icones.doMod(mod), x + w / 2f - 11, y + 8, 22, on ? categoryColor(mod.category()) : MUTED);
        String nome = caber(mod.name(), w - 8);
        fontRendererObj.drawString(nome, x + (w - fontRendererObj.getStringWidth(nome)) / 2, y + 36, on ? TEXT : MUTED);
        int by = y + CARD_H - 21;
        boolean bh = hover && inside(mouseX, mouseY, x + 5, by, x + w - 5, by + 15);
        Desenho.caixa(x + 5, by, x + w - 5, by + 15, on ? (bh ? 0xFFA6F0FF : BEAM) : bh ? 0xFF26304F : BUTTON_OFF, 4);
        String t = on ? "Ligado" : "Desligado";
        fontRendererObj.drawString(t, x + (w - fontRendererObj.getStringWidth(t)) / 2, by + 4, on ? DARK : MUTED);
    }

    /** Opções do mod mostradas nas linhas (a tecla de ligar fica no cabeçalho). */
    private List<Setting> rows() {
        List<Setting> out = new ArrayList<Setting>();
        for (Setting s : open.panelSettings()) if (s != open.toggleKey()) out.add(s);
        return out;
    }

    private boolean wide(Setting s) {
        // Em telas estreitas tudo fica numa coluna só, para os nomes das opções caberem.
        return s instanceof SliderSetting || s instanceof TextSetting || s instanceof ActionSetting || mw < 360;
    }

    /** Linha de aviso em cima das opções (tecla do cabeçalho esperando ou repetida), ou null. */
    private String headerNote() {
        if (listening == open.toggleKey()) return "Aperte a tecla para ligar e desligar. Esc cancela, Backspace deixa sem tecla";
        String other = conflict(open.toggleKey());
        return other == null ? null : "A tecla de ligar e desligar já é usada em: " + other;
    }

    /** Posição de cada opção: {x, y, largura} relativos ao começo da lista. Pequenas em duas colunas, sliders inteiros. */
    private List<int[]> rowRects(List<Setting> list) {
        List<int[]> out = new ArrayList<int[]>();
        int half = (mw - GAP) / 2;
        int y = headerNote() != null ? 14 : 0;
        int col = 0;
        for (Setting s : list) {
            if (wide(s)) {
                if (col == 1) {
                    y += ROW_H + GAP;
                    col = 0;
                }
                out.add(new int[]{0, y, mw});
                y += ROW_H + GAP;
            } else {
                out.add(new int[]{col * (half + GAP), y, half});
                if (col == 1) {
                    col = 0;
                    y += ROW_H + GAP;
                } else {
                    col = 1;
                }
            }
        }
        if (col == 1) y += ROW_H + GAP;
        contentHeight = Math.max(0, y - GAP);
        return out;
    }

    private void drawSettings(int mouseX, int mouseY) {
        // Cabeçalho: voltar, ícone, nome e descrição, a tecla de ligar e o botão Ligado/Desligado.
        boolean backHover = inside(mouseX, mouseY, mx, py + 10, mx + 20, py + 30);
        Desenho.caixa(mx, py + 10, mx + 20, py + 30, backHover ? CARD_HOVER : CARD, 5);
        Icones.desenhar(Icones.VOLTAR, mx + 5, py + 15, 10, backHover ? BEAM : TEXT);
        Desenho.caixa(mx + 26, py + 8, mx + 50, py + 32, CARD, 5);
        Icones.desenhar(Icones.doMod(open), mx + 30, py + 12, 16, categoryColor(open.category()));
        int kx = mx + mw - 138;
        fontRendererObj.drawString(caber(open.name(), kx - mx - 60), mx + 56, py + 10, TEXT);
        fontRendererObj.drawString(caber(open.description(), kx - mx - 60), mx + 56, py + 22, MUTED);

        KeySetting tecla = open.toggleKey();
        boolean waiting = listening == tecla;
        boolean keyHover = inside(mouseX, mouseY, kx, py + 11, kx + 62, py + 29);
        Desenho.caixaBorda(kx, py + 11, kx + 62, py + 29, keyHover ? CARD_HOVER : FIELD, waiting ? GOLD : conflict(tecla) != null ? AMBER : keyHover ? BEAM : LINE, 5);
        String kt = waiting ? "..." : tecla.get() == 0 ? "NENHUMA" : tecla.display().toUpperCase();
        kt = caber(kt, 56);
        fontRendererObj.drawString(kt, kx + 31 - fontRendererObj.getStringWidth(kt) / 2, py + 16, waiting ? GOLD : tecla.get() == 0 ? MUTED : BEAM);
        int ex = mx + mw - 70;
        boolean on = open.isEnabled();
        boolean enHover = inside(mouseX, mouseY, ex, py + 11, ex + 70, py + 29);
        Desenho.caixa(ex, py + 11, ex + 70, py + 29, on ? (enHover ? 0xFFA6F0FF : BEAM) : enHover ? 0xFF26304F : BUTTON_OFF, 5);
        String et = on ? "Ligado" : "Desligado";
        fontRendererObj.drawString(et, ex + 35 - fontRendererObj.getStringWidth(et) / 2, py + 16, on ? DARK : MUTED);
        drawRect(mx, py + 38, mx + mw, py + 39, LINE);

        List<Setting> settings = rows();
        List<int[]> rects = rowRects(settings);
        clampScroll();
        scissor(mx, top, mw, bottom - top);
        String note = headerNote();
        if (note != null) fontRendererObj.drawString(caber(note, mw), mx, top - scroll + 2, waiting ? GOLD : AMBER);
        if (settings.isEmpty()) {
            fontRendererObj.drawString("Este mod não tem outras opções.", mx, top - scroll + (note != null ? 20 : 6), MUTED);
            fontRendererObj.drawString("Use os botões acima para ligar, desligar ou escolher uma tecla.", mx, top - scroll + (note != null ? 32 : 18), MUTED);
        }
        for (int i = 0; i < settings.size(); i++) {
            int[] r = rects.get(i);
            int y = top - scroll + r[1];
            if (y + ROW_H < top || y > bottom) continue;
            drawRow(settings.get(i), mx + r[0], y, r[2], mouseX, mouseY);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        drawScrollbar();
    }

    private int sliderX(int x, int w) { return x + Math.max(90, w * 2 / 5); }
    private int sliderW(int x, int w) { return x + w - 54 - sliderX(x, w); }

    private void drawRow(Setting s, int x, int y, int w, int mouseX, int mouseY) {
        boolean hover = mouseY >= top && mouseY < bottom && inside(mouseX, mouseY, x, y, x + w, y + ROW_H);
        int cy = y + 8;
        if (s instanceof ActionSetting) {
            boolean done = flashed == s && System.currentTimeMillis() < flashUntil;
            Desenho.caixaBorda(x, y, x + w, y + ROW_H, hover ? CARD_HOVER : CARD, hover ? BEAM : LINE, 5);
            drawCenteredString(fontRendererObj, done ? "Feito!" : s.label(), x + w / 2, cy, done ? BEAM : TEXT);
            return;
        }
        Desenho.caixa(x, y, x + w, y + ROW_H, hover ? CARD_HOVER : CARD, 5);
        int room = s instanceof BoolSetting ? w - 42 : s instanceof TextSetting ? w - Math.min(180, w / 2) - 16
                : s instanceof KeySetting ? w - 76 : s instanceof ColorSetting ? w - 44
                : s instanceof SliderSetting ? sliderX(x, w) - x - 14 : w - Math.min(100, w / 2) - 14;
        // Teclas ganham uma segunda linha: como cancelar enquanto espera, ou onde mais a tecla já é usada.
        String note = null;
        int noteColor = MUTED;
        if (listening == s) {
            note = "Esc cancela, Backspace limpa";
        } else if (s instanceof KeySetting) {
            String other = conflict((KeySetting) s);
            if (other != null) {
                note = "Já usada em: " + other;
                noteColor = AMBER;
            }
        }
        fontRendererObj.drawString(caber(s.label(), room), x + 8, note == null ? cy : y + 3, TEXT);
        if (note != null) fontRendererObj.drawString(caber(note, room), x + 8, y + 13, noteColor);
        if (s instanceof BoolSetting) {
            Desenho.chave(x + w - 30, y + 7, 22, 10, ((BoolSetting) s).get(), hover ? 1f : 0f, BEAM);
        } else if (s instanceof SliderSetting) {
            SliderSetting sl = (SliderSetting) s;
            int tx = sliderX(x, w);
            int tw = sliderW(x, w);
            Desenho.caixa(tx, y + 11, tx + tw, y + 13, OFF, 1);
            int kx = tx + (int) Math.round(sl.fraction() * tw);
            Desenho.caixa(tx, y + 11, kx, y + 13, BEAM, 1);
            Desenho.caixa(kx - 4, y + 8, kx + 4, y + 16, TEXT, 4);
            int vx = x + w - 46;
            Desenho.caixa(vx, y + 5, vx + 40, y + ROW_H - 5, FIELD, 4);
            String v = caber(sl.display(), 38);
            fontRendererObj.drawString(v, vx + 20 - fontRendererObj.getStringWidth(v) / 2, cy, BEAM);
        } else if (s instanceof ColorSetting) {
            // Quadradinho com a cor; o clique abre a paleta.
            ColorSetting c = (ColorSetting) s;
            int sx = x + w - 36;
            Desenho.caixa(sx, y + 6, sx + 28, y + ROW_H - 6, picking == s ? BEAM : FIELD, 3);
            Desenho.caixa(sx + 2, y + 8, sx + 26, y + ROW_H - 8, c.argb(0xFF), 2);
        } else if (s instanceof ModeSetting) {
            int bw = Math.min(100, w / 2);
            int bx = x + w - 6 - bw;
            Desenho.caixa(bx, y + 4, bx + bw, y + ROW_H - 4, FIELD, 4);
            String v = caber(((ModeSetting) s).get(), bw - 20);
            drawCenteredString(fontRendererObj, v, bx + bw / 2, cy, BEAM);
            fontRendererObj.drawString("<", bx + 4, cy, MUTED);
            fontRendererObj.drawString(">", bx + bw - 9, cy, MUTED);
        } else if (s instanceof KeySetting) {
            boolean waiting = listening == s;
            int kx = x + w - 66;
            if (waiting) Desenho.caixaBorda(kx, y + 4, kx + 60, y + ROW_H - 4, FIELD, GOLD, 4);
            else Desenho.caixa(kx, y + 4, kx + 60, y + ROW_H - 4, FIELD, 4);
            String kt = caber(waiting ? "..." : ((KeySetting) s).display(), 56);
            drawCenteredString(fontRendererObj, kt, kx + 30, cy, waiting ? GOLD : BEAM);
        } else if (s instanceof TextSetting) {
            int fw = Math.min(180, w / 2);
            int fx = x + w - 6 - fw;
            Desenho.caixaBorda(fx, y + 4, fx + fw, y + ROW_H - 4, FIELD, editing == s ? BEAM : LINE, 4);
            if (editing == s) {
                field.xPosition = fx + 4;
                field.yPosition = cy;
                field.drawTextBox();
            } else {
                String t = ((TextSetting) s).get();
                if (t.isEmpty()) fontRendererObj.drawString(caber("Clique para escrever", fw - 8), fx + 4, cy, MUTED);
                else fontRendererObj.drawString(fontRendererObj.trimStringToWidth(t, fw - 8), fx + 4, cy, TEXT);
            }
        }
    }

    /** Onde mais a tecla é usada (nos controles do jogo ou em outro mod do Pulsar), ou null. */
    private String conflict(KeySetting k) {
        int code = k.get();
        if (code == 0) return null;
        KeyBinding own = k instanceof TeclaDoJogo ? ((TeclaDoJogo) k).bind() : null;
        for (KeyBinding kb : mc.gameSettings.keyBindings) {
            // As teclas de transmissão da Twitch do 1.8.9 não fazem mais nada.
            if (kb == own || kb.getKeyCode() != code || "key.categories.stream".equals(kb.getKeyCategory())) continue;
            return I18n.format(kb.getKeyDescription());
        }
        for (PulsarMod m : PulsarClient.get().mods()) {
            // As opções do painel, para pegar também as teclas criadas na hora (Atalhos de Texto).
            for (Setting s : m.panelSettings()) {
                if (s == k || !(s instanceof KeySetting) || s instanceof TeclaDoJogo || ((KeySetting) s).get() != code) continue;
                // Na mesma tela, o nome do mod não ajuda: diz qual das teclas é.
                return s == m.toggleKey() && m.panelSettings().contains(k) ? "ligar e desligar" : m.name();
            }
        }
        return null;
    }

    // ---------- paleta de cores ----------

    private static final int PICK_W = 184;
    private static final int PICK_H = 122;
    private static final int SQ_W = 120;
    private static final int SQ_H = 64;

    private int pickX() { return mx + (mw - PICK_W) / 2; }
    private int pickY() { return Math.max(top, top + (bottom - top - PICK_H) / 2); }

    /** Abre a paleta de uma cor (teste automático). */
    public void showPicker(ColorSetting c) {
        openPicker(c);
    }

    private void openPicker(ColorSetting c) {
        picking = c;
        float[] hsb = ColorSetting.hsb(c.rgb());
        pickH = hsb[0];
        pickS = hsb[1];
        pickB = hsb[2];
    }

    private void drawPicker() {
        int x = pickX(), y = pickY();
        Desenho.caixaBorda(x, y, x + PICK_W, y + PICK_H, 0xFA0B1222, BEAM, 6);
        fontRendererObj.drawString(caber(picking.label(), PICK_W - 60), x + 8, y + 7, TEXT);
        fontRendererObj.drawString(picking.hex(), x + PICK_W - 8 - fontRendererObj.getStringWidth(picking.hex()), y + 7, BEAM);

        // Quadrado: da esquerda (sem cor) para a direita (cor pura), de cima (claro) para baixo (escuro).
        int sx = x + 8, sy = y + 20;
        int hue = 0xFF000000 | ColorSetting.hsb(pickH, 1f, 1f);
        gradient(sx, sy, sx + SQ_W, sy + SQ_H, 0xFFFFFFFF, hue, 0xFFFFFFFF, hue);
        gradient(sx, sy, sx + SQ_W, sy + SQ_H, 0x00000000, 0x00000000, 0xFF000000, 0xFF000000);
        int kx = sx + Math.round(pickS * SQ_W), ky = sy + Math.round((1 - pickB) * SQ_H);
        outline(kx - 2, ky - 2, kx + 3, ky + 3, pickB > 0.6f ? 0xFF000000 : 0xFFFFFFFF);

        // Barra das cores (matiz), de cima para baixo.
        int bx = sx + SQ_W + 6;
        for (int i = 0; i < 6; i++) {
            int a = 0xFF000000 | ColorSetting.hsb(i / 6f, 1f, 1f);
            int b = 0xFF000000 | ColorSetting.hsb((i + 1) / 6f, 1f, 1f);
            int t = sy + i * SQ_H / 6, bt = sy + (i + 1) * SQ_H / 6;
            gradient(bx, t, bx + 10, bt, a, a, b, b);
        }
        int hy = sy + Math.round(pickH * SQ_H);
        drawRect(bx - 1, hy - 1, bx + 11, hy + 1, 0xFFFFFFFF);

        // Cor escolhida.
        int px0 = bx + 16;
        drawRect(px0, sy, x + PICK_W - 8, sy + SQ_H, 0xFF000000 | picking.rgb());
        outline(px0, sy, x + PICK_W - 8, sy + SQ_H, LINE);

        // Cores prontas.
        int ry = sy + SQ_H + 8;
        for (int i = 0; i < ColorSetting.PRONTAS.length; i++) {
            int cx0 = sx + i * 11;
            drawRect(cx0, ry, cx0 + 9, ry + 9, 0xFF000000 | ColorSetting.PRONTAS[i]);
            if (ColorSetting.PRONTAS[i] == picking.rgb()) outline(cx0 - 1, ry - 1, cx0 + 10, ry + 10, 0xFFFFFFFF);
        }
        fontRendererObj.drawString("Clique fora da paleta para fechar", sx, ry + 15, MUTED);
    }

    /** true se o clique foi dentro da paleta. */
    private boolean clickPicker(int mouseX, int mouseY) {
        int x = pickX(), y = pickY();
        if (!inside(mouseX, mouseY, x, y, x + PICK_W, y + PICK_H)) return false;
        int sx = x + 8, sy = y + 20, bx = sx + SQ_W + 6;
        if (inside(mouseX, mouseY, sx, sy, sx + SQ_W + 1, sy + SQ_H + 1)) pickDrag = 1;
        else if (inside(mouseX, mouseY, bx - 2, sy, bx + 12, sy + SQ_H + 1)) pickDrag = 2;
        else {
            int ry = sy + SQ_H + 8;
            for (int i = 0; i < ColorSetting.PRONTAS.length; i++) {
                int cx0 = sx + i * 11;
                if (!inside(mouseX, mouseY, cx0, ry, cx0 + 10, ry + 10)) continue;
                picking.set(ColorSetting.PRONTAS[i]);
                openPicker(picking);
            }
            return true;
        }
        dragPicker(mouseX, mouseY);
        return true;
    }

    private void dragPicker(int mouseX, int mouseY) {
        int sx = pickX() + 8, sy = pickY() + 20;
        float fy = Math.max(0f, Math.min(1f, (mouseY - sy) / (float) SQ_H));
        if (pickDrag == 1) {
            pickS = Math.max(0f, Math.min(1f, (mouseX - sx) / (float) SQ_W));
            pickB = 1 - fy;
        } else if (pickDrag == 2) {
            pickH = Math.min(0.999f, fy);
        } else {
            return;
        }
        picking.set(ColorSetting.hsb(pickH, pickS, pickB));
    }

    /** Retângulo com uma cor em cada canto (a cor muda suave de um canto para o outro). */
    private static void gradient(int l, int t, int r, int b, int tl, int tr, int bl, int br) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        vertex(wr, r, t, tr);
        vertex(wr, l, t, tl);
        vertex(wr, l, b, bl);
        vertex(wr, r, b, br);
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static void vertex(WorldRenderer wr, int x, int y, int argb) {
        wr.pos(x, y, 0).color(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24).endVertex();
    }

    private int barraAltura() {
        int view = bottom - top;
        return Math.max(16, view * view / Math.max(1, contentHeight));
    }

    private int barraTopo() {
        int view = bottom - top;
        return top + (view - barraAltura()) * scroll / Math.max(1, contentHeight - view);
    }

    /** Área da barra (um pouco mais larga que o desenho, para ficar fácil de pegar). */
    private boolean naBarra(int mouseX, int mouseY) {
        return contentHeight > bottom - top && mouseX >= px + pw - 10 && mouseX < px + pw && mouseY >= top && mouseY < bottom;
    }

    /** Só para o teste: segura a barra, arrasta até embaixo e depois volta até em cima. */
    String testarBarra() {
        if (contentHeight <= bottom - top) return "sem-barra";
        int x = px + pw - 5, max = contentHeight - (bottom - top);
        try {
            mouseClicked(x, barraTopo() + 2, 0);
            mouseClickMove(x, bottom + 40, 0, 100);
            int embaixo = scroll;
            mouseReleased(x, bottom + 40, 0);
            mouseClicked(x, barraTopo() + 2, 0);
            mouseClickMove(x, top - 40, 0, 100);
            int emCima = scroll;
            mouseReleased(x, top - 40, 0);
            System.out.println("[Pulsar] Barra de rolagem: embaixo " + embaixo + " de " + max + ", em cima " + emCima);
            return embaixo == max && emCima == 0 ? "ok" : "falhou";
        } catch (IOException e) {
            return "falhou";
        }
    }

    private void drawScrollbar() {
        int view = bottom - top;
        if (contentHeight <= view) return;
        int bh = barraAltura();
        int by = barraTopo();
        boolean sobre = barraAgarrada >= 0 || naBarra(ultimoMouseX, ultimoMouseY);
        Desenho.caixa(px + pw - 8, top, px + pw - 4, bottom, sobre ? 0x227FE7FF : 0x117FE7FF, 2);
        Desenho.caixa(px + pw - 8, by, px + pw - 4, by + bh, barraAgarrada >= 0 ? 0xFF7FE7FF : sobre ? 0xCC7FE7FF : 0x887FE7FF, 2);
    }

    /** Move a lista para a barra ficar com o ponto agarrado embaixo do mouse. */
    private void arrastarBarra(int mouseY) {
        int view = bottom - top;
        int livre = view - barraAltura();
        if (livre <= 0) return;
        int novoTopo = mouseY - barraAgarrada - top;
        scroll = Math.round((float) novoTopo * (contentHeight - view) / livre);
        clampScroll();
    }

    private static int categoryColor(Category c) {
        switch (c) {
            case COMBATE: return 0xFFFF8A3D;
            case VISUAL: return 0xFF9B7CFF;
            case MOTOR: return 0xFFE8C547;
            case SERVIDORES: return 0xFF6EE7A8;
            case SOCIAL: return 0xFFFF7AB6;
            default: return BEAM;
        }
    }

    // ---------- entrada ----------

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (picking != null) {
            if (!clickPicker(mouseX, mouseY)) picking = null;
            return;
        }
        if (listening != null) {
            // Botão do meio ou laterais do mouse viram a tecla; clique esquerdo ou direito cancela.
            if (button >= 2) listening.set(button - 100);
            listening = null;
            return;
        }
        if (editing != null) commitText();
        layout();
        if (button == 0 && naBarra(mouseX, mouseY)) {
            int by = barraTopo();
            // Clicou fora da parte clara: ela pula para o mouse e já fica agarrada no meio.
            barraAgarrada = mouseY >= by && mouseY < by + barraAltura() ? mouseY - by : barraAltura() / 2;
            arrastarBarra(mouseY);
            return;
        }
        if (open == null) search.mouseClicked(mouseX, mouseY, button);
        if (clickSidebar(mouseX, mouseY)) return;
        if (mouseY < top) {
            if (open != null && button == 0) {
                int kx = mx + mw - 138, ex = mx + mw - 70;
                if (inside(mouseX, mouseY, mx, py + 8, mx + 22, py + 32)) back();
                else if (inside(mouseX, mouseY, kx, py + 11, kx + 62, py + 29)) listening = open.toggleKey();
                else if (inside(mouseX, mouseY, ex, py + 11, ex + 70, py + 29)) open.toggle();
            }
            return;
        }
        if (mouseY >= bottom || mouseX < mx || mouseX >= mx + mw) return;
        if (open == null) clickGrid(mouseX, mouseY, button);
        else clickSettings(mouseX, mouseY, button);
    }

    private boolean clickSidebar(int mouseX, int mouseY) {
        if (mouseX < px || mouseX >= px + sideW || mouseY < py || mouseY >= py + ph) return false;
        int y = py + 42;
        int step = sideStep();
        if (mouseY >= y && mouseY < y + step) { select(null, false); return true; }
        y += step;
        if (mouseY >= y && mouseY < y + step) { select(null, true); return true; }
        for (Category c : Category.values()) {
            y += step;
            if (mouseY >= y && mouseY < y + step) { select(c, false); return true; }
        }
        y += step + 4;
        if (mouseY >= y && mouseY < y + step) {
            commitText();
            mc.displayGuiScreen(new GuiTexturas(this));
            return true;
        }
        int by = py + ph - 26;
        if (inside(mouseX, mouseY, px + 10, by, px + sideW - 6, by + 17)) {
            mc.displayGuiScreen(new GuiHudEditor(this));
        }
        return true;
    }

    private void select(Category c, boolean fav) {
        commitText();
        favorites = fav;
        listening = null;
        picking = null;
        direct = false;
        category = c;
        open = null;
        scroll = 0;
    }

    private void clickGrid(int mouseX, int mouseY, int button) {
        List<PulsarMod> mods = visibleMods();
        int cols = columns();
        int cw = cardWidth();
        for (int i = 0; i < mods.size(); i++) {
            int x = mx + (i % cols) * (cw + GAP);
            int y = top + (i / cols) * (CARD_H + GAP) - scroll;
            if (!inside(mouseX, mouseY, x, y, x + cw, y + CARD_H)) continue;
            PulsarMod mod = mods.get(i);
            if (button == 0 && inside(mouseX, mouseY, x, y, x + 18, y + 18)) {
                mod.toggleFavorite();
            } else if (button == 1 || inside(mouseX, mouseY, x + cw - 18, y, x + cw, y + 18)) {
                open = mod;
                scroll = 0;
                layout();
            } else if (button == 0) {
                mod.toggle();
            }
            return;
        }
    }

    private void clickSettings(int mouseX, int mouseY, int button) {
        List<Setting> settings = rows();
        List<int[]> rects = rowRects(settings);
        for (int i = 0; i < settings.size(); i++) {
            int[] r = rects.get(i);
            int x = mx + r[0], y = top - scroll + r[1];
            if (!inside(mouseX, mouseY, x, y, x + r[2], y + ROW_H)) continue;
            dragX = sliderX(x, r[2]);
            dragW = sliderW(x, r[2]);
            clickSetting(settings.get(i), mouseX, button);
            return;
        }
    }

    private void clickSetting(Setting s, int mouseX, int button) {
        if (s instanceof BoolSetting) {
            ((BoolSetting) s).toggle();
        } else if (s instanceof ModeSetting) {
            if (button == 1) ((ModeSetting) s).previous();
            else ((ModeSetting) s).next();
        } else if (s instanceof ActionSetting) {
            ((ActionSetting) s).run();
            flashed = (ActionSetting) s;
            flashUntil = System.currentTimeMillis() + 1500;
        } else if (s instanceof SliderSetting) {
            dragging = (SliderSetting) s;
            drag(mouseX);
        } else if (s instanceof KeySetting) {
            listening = (KeySetting) s;
        } else if (s instanceof ColorSetting) {
            openPicker((ColorSetting) s);
        } else if (s instanceof TextSetting) {
            editing = (TextSetting) s;
            field.setMaxStringLength(editing.maxLength());
            field.setText(editing.get());
            field.setFocused(true);
            field.setCursorPositionEnd();
        }
    }

    private void commitText() {
        if (editing != null) editing.set(field.getText());
        editing = null;
        field.setFocused(false);
    }

    private void drag(int mouseX) {
        dragging.setFraction(Math.max(0, Math.min(1, (mouseX - dragX) / (double) Math.max(1, dragW))));
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long time) {
        if (barraAgarrada >= 0) {
            arrastarBarra(mouseY);
            return;
        }
        if (dragging != null) drag(mouseX);
        if (picking != null) dragPicker(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        barraAgarrada = -1;
        dragging = null;
        pickDrag = 0;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll += wheel > 0 ? -24 : 24;
            clampScroll();
        }
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (bottom - top))));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (picking != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) picking = null;
            return;
        }
        if (listening != null) {
            if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) listening.set(0);
            else if (keyCode != Keyboard.KEY_ESCAPE && keyCode != 0) listening.set(keyCode);
            if (keyCode != 0) listening = null;
            return;
        }
        if (editing != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                editing = null;
                field.setFocused(false);
            } else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                commitText();
            } else {
                field.textboxKeyTyped(typedChar, keyCode);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (open != null) back();
            else mc.displayGuiScreen(parent);
            return;
        }
        if (open == null && search.textboxKeyTyped(typedChar, keyCode)) {
            scroll = 0;
            return;
        }
        int panelKey = PulsarClient.get().panelKey().getKeyCode();
        if (keyCode != 0 && keyCode == panelKey) mc.displayGuiScreen(parent);
    }

    private void back() {
        commitText();
        listening = null;
        picking = null;
        if (direct) {
            mc.displayGuiScreen(parent);
            return;
        }
        open = null;
        scroll = 0;
        layout();
    }

    @Override
    public void onGuiClosed() {
        commitText();
        Keyboard.enableRepeatEvents(false);
        PulsarClient.get().save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ---------- utilidades ----------

    private void scissor(int x, int y, int w, int h) {
        int f = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, w * f, h * f);
    }

    /** Corta o texto com "..." quando não cabe. */
    private String caber(String s, int w) {
        if (fontRendererObj.getStringWidth(s) <= w) return s;
        return fontRendererObj.trimStringToWidth(s, w - fontRendererObj.getStringWidth("...")).trim() + "...";
    }

    private static boolean inside(int mouseX, int mouseY, int l, int t, int r, int b) {
        return mouseX >= l && mouseX < r && mouseY >= t && mouseY < b;
    }

    static void outline(int l, int t, int r, int b, int color) {
        drawRect(l, t, r, t + 1, color);
        drawRect(l, b - 1, r, b, color);
        drawRect(l, t, l + 1, b, color);
        drawRect(r - 1, t, r, b, color);
    }
}
