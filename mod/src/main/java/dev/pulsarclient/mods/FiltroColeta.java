package dev.pulsarclient.mods;

import dev.pulsarclient.PulsarClient;
import dev.pulsarclient.gui.GuiFiltroColeta;
import dev.pulsarclient.settings.ActionSetting;
import dev.pulsarclient.settings.BoolSetting;
import dev.pulsarclient.settings.TextSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filtro de Coleta: no PvP o chão fica cheio de armadura, espada, maçã e poção de quem morreu, e o inventário enche de
 * coisa que você não quer. A pessoa escolhe no painel quais itens de PvP quer pegar (verde) e quais descartar (vermelho).
 *
 * O Minecraft não deixa o cliente recusar um item do chão (quem decide é o servidor), então o mod faz o que a pessoa
 * faria: assim que um item vermelho entra no inventário vindo do chão, ele é jogado fora de novo (como o Q).
 * Só conta como "do chão" quando um item igual sumiu do chão perto do jogador no mesmo instante; assim item de
 * /kit, de baú, da loja ou o que já estava no inventário nunca é jogado fora.
 *
 * Blocos, minérios e qualquer item fora da lista de PvP sempre são pegos. Slots travados no mod Inventário também.
 */
public class FiltroColeta extends PulsarMod {
    /** Até quantos ticks entre o item sumir do chão e aparecer no inventário. */
    private static final int JANELA = 20;
    private static final int COMBATE_TICKS = 20 * 15;
    private static final int CLIQUES_POR_TICK = 8;

    private final ActionSetting escolher = add(new ActionSetting("escolher", "Escolher os itens (verde pega, vermelho descarta)", new Runnable() {
        @Override public void run() { abrirEditor(); }
    }));
    private final BoolSetting soCombate = add(new BoolSetting("so_combate", "Só descartar em combate (até 15 s depois de bater ou apanhar)", false));
    private final BoolSetting avisar = add(new BoolSetting("avisar", "Mostrar na tela o que foi descartado", true));
    private final TextSetting descartar = add(new TextSetting("descartar", ""));

    private Set<String> vermelhos;
    private String vermelhosDe;

    private int ticks;
    private EntityPlayerSP jogador;
    private boolean pronto;
    private final String[] antesChave = new String[36];
    private final int[] antesQtd = new int[36];
    /** Itens no chão perto do jogador no tick anterior: id da entidade e chave do item. */
    private final Map<Integer, String> perto = new HashMap<Integer, String>();
    /** Último tick em que um item de cada chave sumiu do chão perto do jogador. */
    private final Map<String, Integer> sumiu = new HashMap<String, Integer>();
    private final List<Entrada> entradas = new ArrayList<Entrada>();
    private int ultimoCombate = -100000;

    private static int descartados;

    /** Um item vermelho que entrou no inventário e ainda não foi casado com um item que sumiu do chão. */
    private static final class Entrada {
        final int espaco;
        final String chave;
        final int qtd;
        final int tick;
        boolean confirmada;

        Entrada(int espaco, String chave, int qtd, int tick) {
            this.espaco = espaco;
            this.chave = chave;
            this.qtd = qtd;
            this.tick = tick;
        }
    }

    public FiltroColeta() {
        super("filtro_coleta", "Filtro de Coleta", "Escolha os itens de PvP que você pega do chão: os marcados em vermelho são descartados na hora",
                Category.COMBATE, false);
    }

    /** Quantos itens o mod já jogou fora (teste automático). */
    public static int descartados() {
        return descartados;
    }

    private void abrirEditor() {
        mc.displayGuiScreen(new GuiFiltroColeta(mc.currentScreen, this));
    }

    // ---------- lista ----------

    /** Os itens de PvP que aparecem no painel. Blocos ficam de fora: sempre são pegos. */
    public static List<ItemStack> itens() {
        List<ItemStack> l = new ArrayList<ItemStack>();
        Item[][] armaduras = {
                {Items.diamond_helmet, Items.diamond_chestplate, Items.diamond_leggings, Items.diamond_boots},
                {Items.iron_helmet, Items.iron_chestplate, Items.iron_leggings, Items.iron_boots},
                {Items.chainmail_helmet, Items.chainmail_chestplate, Items.chainmail_leggings, Items.chainmail_boots},
                {Items.golden_helmet, Items.golden_chestplate, Items.golden_leggings, Items.golden_boots},
                {Items.leather_helmet, Items.leather_chestplate, Items.leather_leggings, Items.leather_boots},
        };
        for (Item[] a : armaduras) for (Item i : a) l.add(new ItemStack(i));
        Item[] armas = {Items.diamond_sword, Items.iron_sword, Items.stone_sword, Items.golden_sword, Items.wooden_sword,
                Items.diamond_axe, Items.iron_axe, Items.stone_axe, Items.golden_axe, Items.wooden_axe,
                Items.bow, Items.arrow, Items.fishing_rod, Items.flint_and_steel};
        for (Item i : armas) l.add(new ItemStack(i));
        l.add(new ItemStack(Items.golden_apple, 1, 0));
        l.add(new ItemStack(Items.golden_apple, 1, 1));
        l.add(new ItemStack(Items.ender_pearl));
        // Poções pelo efeito (o nível não importa), de beber e splash.
        int[] pocoes = {
                8233, 16425, // força
                8226, 16418, // velocidade
                8229, 16421, // cura
                8225, 16417, // regeneração
                8227, 16419, // resistência ao fogo
                8206, 16398, // invisibilidade
                16428, 16388, 16394, 16392, // dano, veneno, lentidão e fraqueza (splash)
        };
        for (int m : pocoes) l.add(new ItemStack(Items.potionitem, 1, m));
        Item[] resto = {Items.water_bucket, Items.lava_bucket, Items.bucket, Items.milk_bucket,
                Items.mushroom_stew, Items.bowl, Items.glass_bottle, Items.experience_bottle,
                Items.cooked_beef, Items.cooked_porkchop, Items.golden_carrot, Items.bread, Items.snowball, Items.egg};
        for (Item i : resto) l.add(new ItemStack(i));
        return Collections.unmodifiableList(l);
    }

    private Set<String> vermelhos() {
        String raw = descartar.get();
        if (vermelhos == null || !raw.equals(vermelhosDe)) {
            Set<String> s = new HashSet<String>();
            for (String p : raw.split(",")) if (!p.trim().isEmpty()) s.add(p.trim());
            vermelhos = s;
            vermelhosDe = raw;
        }
        return vermelhos;
    }

    /** O item está marcado em vermelho (é descartado quando vem do chão). */
    public boolean descarta(ItemStack s) {
        String c = KitPvp.chave(s);
        return c != null && vermelhos().contains(c);
    }

    private boolean descartaChave(String chave) {
        return chave != null && vermelhos().contains(chave);
    }

    /** Marca o item como verde (pega) ou vermelho (descarta). */
    public void marcar(ItemStack s, boolean descartarItem) {
        String c = KitPvp.chave(s);
        if (c == null || vermelhos().contains(c) == descartarItem) return;
        Set<String> novo = new HashSet<String>(vermelhos());
        if (descartarItem) novo.add(c);
        else novo.remove(c);
        salvar(novo);
    }

    /** Todos verdes ou todos vermelhos. */
    public void marcarTodos(boolean descartarTodos) {
        Set<String> novo = new HashSet<String>();
        if (descartarTodos) for (ItemStack s : itens()) novo.add(KitPvp.chave(s));
        salvar(novo);
    }

    private void salvar(Set<String> novo) {
        List<String> ordem = new ArrayList<String>(novo);
        Collections.sort(ordem);
        StringBuilder sb = new StringBuilder();
        for (String c : ordem) sb.append(sb.length() == 0 ? "" : ",").append(c);
        descartar.set(sb.toString());
        vermelhos = null;
        if (PulsarClient.get() != null) PulsarClient.get().save();
    }

    public int quantosVermelhos() {
        return vermelhos().size();
    }

    // ---------- no jogo ----------

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (mc.thePlayer != null && event.entityPlayer == mc.thePlayer) ultimoCombate = ticks;
    }

    private void recomecar() {
        pronto = false;
        perto.clear();
        sumiu.clear();
        entradas.clear();
    }

    @Override
    protected void onEnable() {
        recomecar();
    }

    @Override
    public void tick() {
        ticks++;
        EntityPlayerSP p = mc.thePlayer;
        if (!isEnabled() || p == null || mc.theWorld == null || p.isDead || p.capabilities.isCreativeMode) {
            recomecar();
            jogador = p;
            return;
        }
        // Renasceu ou trocou de mundo: o jogador é outro objeto e o inventário é novo.
        if (p != jogador) {
            jogador = p;
            recomecar();
        }
        if (p.hurtTime > 0) ultimoCombate = ticks;

        olharChao(p);
        olharInventario(p);
        jogarFora(p);
    }

    /** Anota os itens que sumiram do chão perto do jogador (pegos por alguém, talvez por ele). */
    private void olharChao(EntityPlayerSP p) {
        for (Map.Entry<Integer, String> e : perto.entrySet()) {
            Entity ent = mc.theWorld.getEntityByID(e.getKey());
            if (ent == null || ent.isDead) sumiu.put(e.getValue(), ticks);
        }
        perto.clear();
        for (Object o : mc.theWorld.getEntitiesWithinAABB(EntityItem.class, p.getEntityBoundingBox().expand(4, 3, 4))) {
            EntityItem ei = (EntityItem) o;
            ItemStack s = ei.getEntityItem();
            String c = s == null ? null : KitPvp.chave(s);
            if (c != null) perto.put(ei.getEntityId(), c);
        }
        Iterator<Map.Entry<String, Integer>> it = sumiu.entrySet().iterator();
        while (it.hasNext()) if (ticks - it.next().getValue() > JANELA) it.remove();
    }

    /** Compara com o tick anterior: item vermelho que aumentou num espaço vira uma entrada para conferir. */
    private void olharInventario(EntityPlayerSP p) {
        // Item no mouse (mexendo no inventário): a conta dos espaços muda sem nada vir do chão.
        boolean ver = pronto && p.inventory.getItemStack() == null && !(mc.currentScreen instanceof GuiContainer && !(mc.currentScreen instanceof GuiInventory));
        for (int i = 0; i < 36; i++) {
            ItemStack s = p.inventory.mainInventory[i];
            String c = KitPvp.chave(s);
            int q = s == null ? 0 : s.stackSize;
            if (ver && c != null) {
                int antes = c.equals(antesChave[i]) ? antesQtd[i] : 0;
                if (q > antes && descartaChave(c)) entradas.add(new Entrada(i, c, q - antes, ticks));
            }
            antesChave[i] = c;
            antesQtd[i] = q;
        }
        pronto = true;
        // Casa cada entrada com um item igual que sumiu do chão; o que não casar em meio segundo não veio do chão.
        Iterator<Entrada> it = entradas.iterator();
        while (it.hasNext()) {
            Entrada e = it.next();
            if (e.confirmada) continue;
            Integer t = sumiu.get(e.chave);
            if (t != null && Math.abs(t - e.tick) <= JANELA) e.confirmada = true;
            else if (ticks - e.tick > 10) it.remove();
        }
    }

    private boolean podeClicar(EntityPlayerSP p) {
        GuiScreen tela = mc.currentScreen;
        boolean telaOk = tela == null || tela instanceof GuiInventory || !(tela instanceof GuiContainer);
        return telaOk && p.openContainer == p.inventoryContainer && p.inventory.getItemStack() == null;
    }

    /** Joga fora o que veio do chão e está em vermelho, com o mesmo clique do Q no inventário. */
    private void jogarFora(EntityPlayerSP p) {
        // Ficou esperando muito (baú aberto, item no mouse): a pessoa pode ter mexido no inventário, então deixa quieto.
        Iterator<Entrada> velhas = entradas.iterator();
        while (velhas.hasNext()) if (ticks - velhas.next().tick > 100) velhas.remove();
        if (entradas.isEmpty() || !podeClicar(p)) return;
        boolean combate = ticks - ultimoCombate <= COMBATE_TICKS;
        int cliques = 0;
        List<Entrada> resto = new ArrayList<Entrada>();
        Iterator<Entrada> it = entradas.iterator();
        while (it.hasNext() && cliques < CLIQUES_POR_TICK) {
            Entrada e = it.next();
            if (!e.confirmada) continue;
            it.remove();
            if (soCombate.get() && !combate) continue;
            if (PulsarClient.get() != null && PulsarClient.get().inventario().isLocked(e.espaco)) continue;
            ItemStack s = p.inventory.mainInventory[e.espaco];
            if (s == null || !e.chave.equals(KitPvp.chave(s))) continue;
            int slot = e.espaco < 9 ? 36 + e.espaco : e.espaco;
            int qtd = Math.min(e.qtd, s.stackSize);
            String nome = s.getDisplayName();
            int feitos = 0;
            if (qtd >= s.stackSize) {
                // A pilha toda veio do chão: Ctrl + Q.
                mc.playerController.windowClick(p.inventoryContainer.windowId, slot, 1, 4, p);
                cliques++;
                feitos = qtd;
            } else {
                // Juntou numa pilha que já existia: joga fora só o que chegou, um por um.
                for (; feitos < qtd && cliques < CLIQUES_POR_TICK; feitos++, cliques++) {
                    mc.playerController.windowClick(p.inventoryContainer.windowId, slot, 0, 4, p);
                }
                // Acabaram os cliques deste tick: o que faltou sai no próximo (antes ficava no inventário).
                if (feitos < qtd) {
                    Entrada r = new Entrada(e.espaco, e.chave, qtd - feitos, ticks);
                    r.confirmada = true;
                    resto.add(r);
                }
            }
            qtd = feitos;
            descartados += qtd;
            ItemStack depois = p.inventory.mainInventory[e.espaco];
            antesChave[e.espaco] = KitPvp.chave(depois);
            antesQtd[e.espaco] = depois == null ? 0 : depois.stackSize;
            System.out.println("[Pulsar] Filtro de Coleta: descartou " + qtd + "x " + e.chave);
            if (avisar.get()) mc.ingameGUI.setRecordPlaying("§cDescartado: §f" + (qtd > 1 ? qtd + "x " : "") + nome, false);
        }
        entradas.addAll(resto);
    }

    @Override
    public void onBroken() {
        recomecar();
    }
}
