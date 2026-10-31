package dev.pulsarclient.gui;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.hud.GuiHudEditor;
import dev.pulsarclient.mods.Category;
import dev.pulsarclient.mods.PulsarMod;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.KeySetting;
import dev.pulsarclient.settings.ModeSetting;
import dev.pulsarclient.settings.Setting;
import dev.pulsarclient.settings.SliderSetting;
import dev.pulsarclient.settings.TeclaDoJogo;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel do Pulsar (Shift direito): categorias à esquerda, mods em cartões e as opções de cada mod.
 * Feito só com retângulos e texto, sem texturas nem sombras, para não pesar no FPS.
 */
public class GuiPulsarPanel extends GuiScreen {
    private static final int BG = 0xF20A0F1E;
    private static final int SIDEBAR = 0xF2060A14;
    private static final int CARD = 0xFF111A2D;
    private static final int CARD_HOVER = 0xFF18233C;
    private static final int LINE = 0x337FE7FF;
    private static final int BEAM = 0xFF7FE7FF;
    private static final int TEXT = 0xFFE6ECFF;
    private static final int MUTED = 0xFF8892B0;
    private static final int OFF = 0xFF353E5A;
    private static final int AMBER = 0xFFFFB347;
    private static final int CARD_H = 44;
    private static final int ROW_H = 22;
    private static final int GAP = 6;
    private static final int SIDE_W = 96;

    private final GuiScreen parent;
    private Category category;
    private PulsarMod open;
    private GuiTextField search;
    private int scroll;
    private int contentHeight;
    private SliderSetting dragging;
    private ActionSetting flashed;
    private long flashUntil;
    private KeySetting listening;
    private TextSetting editing;
    private GuiTextField field;

    // Painel calculado em initGui
    private int px, py, pw, ph;
    private int mx, mw, top, bottom;

    public GuiPulsarPanel(GuiScreen parent) {
        this(parent, null, null);
    }

    public GuiPulsarPanel(GuiScreen parent, Category category, PulsarMod open) {
        this.parent = parent;
        this.category = category;
        this.open = open;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        pw = Math.min(460, width - 16);
        ph = Math.min(280, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        mx = px + SIDE_W + 10;
        mw = pw - SIDE_W - 20;
        top = py + 30;
        bottom = py + ph - 8;
        String text = search == null ? "" : search.getText();
        search = new GuiTextField(0, fontRendererObj, px + pw - 118, py + 10, 104, 10);
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

    @Override
    public void updateScreen() {
        search.updateCursorCounter();
        field.updateCursorCounter();
    }

    // ---------- desenho ----------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (mc.theWorld == null) Universo.draw(width, height);
        drawRect(0, 0, width, height, 0x66000000);
        drawRect(px, py, px + pw, py + ph, BG);
        outline(px, py, px + pw, py + ph, LINE);
        drawSidebar(mouseX, mouseY);

        if (open == null) drawGrid(mouseX, mouseY);
        else drawSettings(mouseX, mouseY);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        drawRect(px + 1, py + 1, px + SIDE_W, py + ph - 1, SIDEBAR);
        drawRect(px + SIDE_W, py + 1, px + SIDE_W + 1, py + ph - 1, LINE);

        // Logo "PC" do client.
        int cx = px + SIDE_W / 2;
        Universo.logo(cx - 20, py + 4, 40);
        drawCenteredString(fontRendererObj, "PULSAR", cx, py + 46, TEXT);
        drawCenteredString(fontRendererObj, "client 1.8.9", cx, py + 56, MUTED);

        int y = py + 74;
        y = sideItem("Todos", category == null && open == null, y, mouseX, mouseY);
        for (Category c : Category.values()) {
            y = sideItem(c.label, category == c && open == null, y, mouseX, mouseY);
        }
        drawRect(px + 10, y + 1, px + SIDE_W - 10, y + 2, LINE);
        sideItem("Texturas  >", false, y + 4, mouseX, mouseY);

        int on = 0;
        for (PulsarMod m : PulsarClient.get().mods()) if (m.isEnabled()) on++;
        drawCenteredString(fontRendererObj, on + " ligados", cx, py + ph - 42, MUTED);
        int by = py + ph - 28;
        boolean hover = inside(mouseX, mouseY, px + 8, by, px + SIDE_W - 8, by + 18);
        drawRect(px + 8, by, px + SIDE_W - 8, by + 18, hover ? 0xFF1C2A48 : CARD);
        outline(px + 8, by, px + SIDE_W - 8, by + 18, hover ? BEAM : LINE);
        drawCenteredString(fontRendererObj, "Editar HUD", cx, by + 5, TEXT);
    }

    /** Altura de cada item da barra lateral: menor em telas baixas, para caber tudo. */
    private int sideStep() {
        return ph >= 260 ? 17 : 15;
    }

    private int sideItem(String label, boolean selected, int y, int mouseX, int mouseY) {
        int h = sideStep() - 1;
        boolean hover = inside(mouseX, mouseY, px + 1, y, px + SIDE_W, y + h);
        if (selected) {
            drawRect(px + 1, y, px + SIDE_W, y + h, 0x227FE7FF);
            drawRect(px + 1, y, px + 3, y + h, BEAM);
        } else if (hover) {
            drawRect(px + 1, y, px + SIDE_W, y + h, 0x11FFFFFF);
        }
        fontRendererObj.drawString(label, px + 10, y + (h - 8) / 2, selected ? BEAM : hover ? TEXT : MUTED);
        return y + sideStep();
    }

    private List<PulsarMod> visibleMods() {
        String q = search.getText().trim().toLowerCase();
        List<PulsarMod> out = new ArrayList<PulsarMod>();
        for (PulsarMod m : PulsarClient.get().mods()) {
            if (category != null && m.category() != category) continue;
            if (!q.isEmpty() && !(m.name() + " " + m.description()).toLowerCase().contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    private int columns() {
        return mw >= 300 ? 3 : 2;
    }

    private int cardWidth() {
        return (mw - GAP * (columns() - 1)) / columns();
    }

    private void drawGrid(int mouseX, int mouseY) {
        fontRendererObj.drawString(category == null ? "Todos os mods" : category.label, mx, py + 11, TEXT);
        drawRect(px + pw - 122, py + 7, px + pw - 10, py + 21, CARD);
        outline(px + pw - 122, py + 7, px + pw - 10, py + 21, search.isFocused() ? BEAM : LINE);
        if (search.getText().isEmpty() && !search.isFocused()) fontRendererObj.drawString("Buscar mod...", px + pw - 118, py + 10, MUTED);
        search.drawTextBox();

        List<PulsarMod> mods = visibleMods();
        int cols = columns();
        int cw = cardWidth();
        int rows = (mods.size() + cols - 1) / cols;
        contentHeight = rows * (CARD_H + GAP) - GAP;
        clampScroll();

        if (mods.isEmpty()) {
            drawCenteredString(fontRendererObj, "Nenhum mod encontrado", mx + mw / 2, top + 30, MUTED);
            return;
        }
        boolean inView = mouseY >= top && mouseY < bottom;
        scissor(mx, top, mw, bottom - top);
        for (int i = 0; i < mods.size(); i++) {
            int x = mx + (i % cols) * (cw + GAP);
            int y = top + (i / cols) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < top || y > bottom) continue;
            drawCard(mods.get(i), x, y, cw, inView && inside(mouseX, mouseY, x, y, x + cw, y + CARD_H), mouseX, mouseY);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        drawScrollbar();
    }

    private void drawCard(PulsarMod mod, int x, int y, int w, boolean hover, int mouseX, int mouseY) {
        drawRect(x, y, x + w, y + CARD_H, hover ? CARD_HOVER : CARD);
        drawRect(x, y, x + w, y + 1, categoryColor(mod.category()));
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(mod.name(), w - 12), x + 6, y + 6, TEXT);
        fontRendererObj.drawString(caber(mod.description(), w - 12), x + 6, y + 18, MUTED);
        toggle(x + 6, y + 31, mod.isEnabled());
        fontRendererObj.drawString(mod.isEnabled() ? "Ligado" : "Desligado", x + 32, y + 32, mod.isEnabled() ? BEAM : MUTED);
        if (!mod.panelSettings().isEmpty()) {
            boolean gear = hover && inside(mouseX, mouseY, x + w - 20, y + 28, x + w - 4, y + 42);
            int c = gear ? BEAM : MUTED;
            // Ícone de opções: três barras.
            drawRect(x + w - 16, y + 31, x + w - 6, y + 32, c);
            drawRect(x + w - 16, y + 34, x + w - 6, y + 35, c);
            drawRect(x + w - 16, y + 37, x + w - 6, y + 38, c);
        }
    }

    private void drawSettings(int mouseX, int mouseY) {
        boolean backHover = inside(mouseX, mouseY, mx, py + 8, mx + 50, py + 22);
        fontRendererObj.drawString("< Voltar", mx, py + 11, backHover ? BEAM : MUTED);
        fontRendererObj.drawString(open.name(), mx + 56, py + 11, TEXT);
        toggle(px + pw - 36, py + 10, open.isEnabled());

        List<Setting> settings = open.panelSettings();
        contentHeight = 14 + settings.size() * (ROW_H + 4);
        clampScroll();
        scissor(mx, top, mw, bottom - top);
        int y = top - scroll;
        fontRendererObj.drawString(caber(open.description(), mw), mx, y, MUTED);
        y += 14;
        if (settings.isEmpty()) {
            fontRendererObj.drawString("Este mod não tem opções. Use o botão acima para ligar ou desligar.", mx, y + 6, MUTED);
        }
        for (Setting s : settings) {
            drawRow(s, y, mouseX, mouseY);
            y += ROW_H + 4;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        drawScrollbar();
    }

    private void drawRow(Setting s, int y, int mouseX, int mouseY) {
        boolean hover = mouseY >= top && mouseY < bottom && inside(mouseX, mouseY, mx, y, mx + mw, y + ROW_H);
        drawRect(mx, y, mx + mw, y + ROW_H, hover ? CARD_HOVER : CARD);
        int cx = mx + mw - 116;
        int cy = y + 7;
        if (s instanceof ActionSetting) {
            boolean done = flashed == s && System.currentTimeMillis() < flashUntil;
            outline(mx, y, mx + mw, y + ROW_H, hover ? BEAM : LINE);
            drawCenteredString(fontRendererObj, done ? "Feito!" : s.label(), mx + mw / 2, cy, done ? BEAM : TEXT);
            return;
        }
        int room = s instanceof BoolSetting ? mw - 44 : s instanceof TextSetting ? mw - 186 : s instanceof KeySetting ? mw - 118 : mw - 130;
        // Teclas ganham uma segunda linha: como cancelar enquanto espera, ou onde mais a tecla já é usada.
        String note = null;
        int noteColor = MUTED;
        if (listening == s) {
            note = "Esc cancela, Backspace deixa sem tecla";
        } else if (s instanceof KeySetting) {
            String other = conflict((KeySetting) s);
            if (other != null) {
                note = "Já usada em: " + other;
                noteColor = AMBER;
            }
        }
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(s.label(), room), mx + 6, note == null ? cy : y + 3, TEXT);
        if (note != null) fontRendererObj.drawString(caber(note, room), mx + 6, y + 12, noteColor);
        if (s instanceof BoolSetting) {
            toggle(mx + mw - 28, y + 6, ((BoolSetting) s).get());
        } else if (s instanceof SliderSetting) {
            SliderSetting sl = (SliderSetting) s;
            int tx = cx + 40;
            int tw = 70;
            String v = sl.display();
            fontRendererObj.drawString(v, tx - 6 - fontRendererObj.getStringWidth(v), cy, BEAM);
            drawRect(tx, y + 10, tx + tw, y + 12, OFF);
            int kx = tx + (int) Math.round(sl.fraction() * tw);
            drawRect(tx, y + 10, kx, y + 12, BEAM);
            drawRect(kx - 2, y + 6, kx + 2, y + 16, TEXT);
        } else if (s instanceof ModeSetting) {
            String v = "< " + ((ModeSetting) s).get() + " >";
            drawRect(cx, y + 4, cx + 110, y + ROW_H - 4, 0xFF0B1222);
            drawCenteredString(fontRendererObj, v, cx + 55, cy, BEAM);
        } else if (s instanceof KeySetting) {
            // Caixa da tecla mais estreita, para sobrar espaço para o aviso embaixo do nome.
            boolean waiting = listening == s;
            int kx = mx + mw - 106;
            drawRect(kx, y + 4, kx + 100, y + ROW_H - 4, 0xFF0B1222);
            if (waiting) outline(kx, y + 4, kx + 100, y + ROW_H - 4, 0xFFFFD84A);
            drawCenteredString(fontRendererObj, waiting ? "Aperte uma tecla" : ((KeySetting) s).display(), kx + 50, cy, waiting ? 0xFFFFD84A : BEAM);
        } else if (s instanceof TextSetting) {
            int fx = mx + mw - 176;
            drawRect(fx, y + 4, mx + mw - 6, y + ROW_H - 4, 0xFF0B1222);
            outline(fx, y + 4, mx + mw - 6, y + ROW_H - 4, editing == s ? BEAM : LINE);
            if (editing == s) {
                field.xPosition = fx + 4;
                field.yPosition = cy;
                field.drawTextBox();
            } else {
                String t = ((TextSetting) s).get();
                if (t.isEmpty()) fontRendererObj.drawString("Clique para escrever", fx + 4, cy, MUTED);
                else fontRendererObj.drawString(fontRendererObj.trimStringToWidth(t, 160), fx + 4, cy, TEXT);
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
            for (Setting s : m.settings()) {
                if (s == k || !(s instanceof KeySetting) || s instanceof TeclaDoJogo || ((KeySetting) s).get() != code) continue;
                // Na mesma tela, o nome do mod não ajuda: diz qual das teclas é.
                return s == m.toggleKey() && m.settings().contains(k) ? "ligar e desligar" : m.name();
            }
        }
        return null;
    }

    private void toggle(int x, int y, boolean on) {
        drawRect(x, y, x + 20, y + 10, on ? 0xFF2D6F80 : OFF);
        drawRect(on ? x + 11 : x + 1, y + 1, on ? x + 19 : x + 9, y + 9, on ? BEAM : MUTED);
    }

    private void drawScrollbar() {
        int view = bottom - top;
        if (contentHeight <= view) return;
        int bh = Math.max(16, view * view / contentHeight);
        int by = top + (view - bh) * scroll / (contentHeight - view);
        drawRect(px + pw - 5, by, px + pw - 3, by + bh, 0x887FE7FF);
    }

    private static int categoryColor(Category c) {
        switch (c) {
            case COMBATE: return 0xFFFF8A3D;
            case VISUAL: return 0xFF9B7CFF;
            case MOTOR: return 0xFFE8C547;
            case SERVIDORES: return 0xFF6EE7A8;
            default: return BEAM;
        }
    }

    // ---------- entrada ----------

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (listening != null) {
            // Botão do meio ou laterais do mouse viram a tecla; clique esquerdo ou direito cancela.
            if (button >= 2) listening.set(button - 100);
            listening = null;
            return;
        }
        if (editing != null) commitText();
        if (open == null) search.mouseClicked(mouseX, mouseY, button);
        if (clickSidebar(mouseX, mouseY)) return;
        if (mouseY < py + 26) {
            if (open != null && button == 0) {
                if (inside(mouseX, mouseY, mx, py + 8, mx + 50, py + 22)) back();
                else if (inside(mouseX, mouseY, px + pw - 40, py + 6, px + pw - 10, py + 24)) open.toggle();
            }
            return;
        }
        if (mouseY < top || mouseY >= bottom || mouseX < mx || mouseX >= mx + mw) return;
        if (open == null) clickGrid(mouseX, mouseY, button);
        else clickSettings(mouseX, mouseY, button);
    }

    private boolean clickSidebar(int mouseX, int mouseY) {
        if (mouseX < px || mouseX >= px + SIDE_W || mouseY < py || mouseY >= py + ph) return false;
        int y = py + 74;
        int step = sideStep();
        if (mouseY >= y && mouseY < y + step) { select(null); return true; }
        for (Category c : Category.values()) {
            y += step;
            if (mouseY >= y && mouseY < y + step) { select(c); return true; }
        }
        y += step + 4;
        if (mouseY >= y && mouseY < y + step) {
            commitText();
            mc.displayGuiScreen(new GuiTexturas(this));
            return true;
        }
        int by = py + ph - 28;
        if (inside(mouseX, mouseY, px + 8, by, px + SIDE_W - 8, by + 18)) {
            mc.displayGuiScreen(new GuiHudEditor(this));
        }
        return true;
    }

    private void select(Category c) {
        commitText();
        listening = null;
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
            boolean gear = inside(mouseX, mouseY, x + cw - 20, y + 28, x + cw, y + CARD_H);
            if ((button == 1 || gear) && !mod.panelSettings().isEmpty()) {
                open = mod;
                scroll = 0;
            } else if (button == 0) {
                mod.toggle();
            }
            return;
        }
    }

    private void clickSettings(int mouseX, int mouseY, int button) {
        int y = top - scroll + 14;
        for (Setting s : open.panelSettings()) {
            if (mouseY >= y && mouseY < y + ROW_H) {
                clickSetting(s, mouseX, button);
                return;
            }
            y += ROW_H + 4;
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
        int tx = mx + mw - 116 + 40;
        dragging.setFraction(Math.max(0, Math.min(1, (mouseX - tx) / 70.0)));
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long time) {
        if (dragging != null) drag(mouseX);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = null;
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
        open = null;
        scroll = 0;
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
