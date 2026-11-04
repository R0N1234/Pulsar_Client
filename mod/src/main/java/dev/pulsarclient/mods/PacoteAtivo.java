package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;

/** Pacote Ativo: mostra o pacote de texturas em uso (o de cima da lista), com o ícone dele. */
public class PacoteAtivo extends HudMod {
    private final BoolSetting icone = add(new BoolSetting("icone", "Ícone do pacote", true));
    private final BoolSetting todos = add(new BoolSetting("todos", "Mostrar quantos pacotes estão ligados", false));
    private static long desenhos;

    public PacoteAtivo() {
        super("pacote_ativo", "Pacote Ativo", "Mostra o resource pack em uso", Category.HUD, 5, 184, false);
    }

    /** Quantas vezes o HUD foi desenhado (teste automático). */
    public static long desenhos() {
        return desenhos;
    }

    private ResourcePackRepository.Entry topo() {
        List<ResourcePackRepository.Entry> lista = mc.getResourcePackRepository().getRepositoryEntries();
        // O último da lista é o que fica por cima (o que vale quando dois pacotes mudam a mesma textura).
        return lista.isEmpty() ? null : lista.get(lista.size() - 1);
    }

    private String nome() {
        ResourcePackRepository.Entry e = topo();
        String n = e == null ? "Padrão" : EnumChatFormatting.getTextWithoutFormattingCodes(e.getResourcePackName());
        if (n.toLowerCase().endsWith(".zip")) n = n.substring(0, n.length() - 4);
        if (todos.get()) n += " (" + mc.getResourcePackRepository().getRepositoryEntries().size() + ")";
        return font().trimStringToWidth(n, 150);
    }

    private boolean comIcone() {
        return icone.get() && topo() != null;
    }

    @Override
    public int width() {
        return (comIcone() ? 20 : 0) + font().getStringWidth(nome());
    }

    @Override
    public int height() {
        return comIcone() ? 16 : 9;
    }

    @Override
    public void render(boolean preview) {
        desenhos++;
        if (background.get()) Gui.drawRect(x - 2, y - 2, x + width() + 2, y + height() + 2, BACKGROUND);
        int tx = x;
        ResourcePackRepository.Entry e = topo();
        if (comIcone() && e != null) {
            e.bindTexturePackIcon(mc.getTextureManager());
            GlStateManager.color(1f, 1f, 1f, 1f);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
            tx += 20;
        }
        font().drawStringWithShadow(nome(), tx, y + (height() - 8) / 2, accent());
    }
}
