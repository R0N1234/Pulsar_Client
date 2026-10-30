package dev.pulsarclient.gui;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.GameSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pacotes de textura escolhidos pelo Pulsar (assets/pulsar/texturas.json, o mesmo catálogo do launcher):
 * 5 para PvP e FPS e 5 de qualidade, todos feitos para a 1.8.9 e baixados do Modrinth.
 * Baixa para a pasta resourcepacks (conferindo o arquivo) e liga ou desliga o pacote sem sair do jogo.
 */
public final class Texturas {
    /** Um pacote do catálogo. Os campos vêm do JSON. */
    public static final class Pacote {
        public String id;
        public String nome;
        public String tipo;
        public String desc;
        public String resolucao;
        public String arquivo;
        public String url;
        public String sha1;
        public long tamanho;

        /** -1 parado; 0 a 100 baixando. */
        transient volatile int progresso = -1;
        transient volatile String erro;
        transient long conferidoEm;
        transient boolean baixadoAntes;

        public boolean pvp() { return "pvp".equals(tipo); }
        public int progresso() { return progresso; }
        public String erro() { return erro; }
    }

    private static List<Pacote> catalogo;

    private Texturas() {}

    public static synchronized List<Pacote> catalogo() {
        if (catalogo != null) return catalogo;
        try {
            InputStream in = Texturas.class.getResourceAsStream("/assets/pulsar/texturas.json");
            try {
                Pacote[] lidos = new Gson().fromJson(new InputStreamReader(in, "UTF-8"), Pacote[].class);
                catalogo = Collections.unmodifiableList(new ArrayList<Pacote>(Arrays.asList(lidos)));
            } finally {
                in.close();
            }
        } catch (Exception e) {
            System.err.println("[Pulsar] Texturas: catálogo não carregou: " + e);
            catalogo = Collections.emptyList();
        }
        return catalogo;
    }

    private static ResourcePackRepository repo() {
        return Minecraft.getMinecraft().getResourcePackRepository();
    }

    private static File arquivo(Pacote p) {
        return new File(repo().getDirResourcepacks(), p.arquivo);
    }

    /** O arquivo está na pasta resourcepacks (conferido no máximo duas vezes por segundo, a tela chama a cada quadro). */
    public static boolean baixado(Pacote p) {
        long agora = System.currentTimeMillis();
        if (agora - p.conferidoEm > 500) {
            File f = arquivo(p);
            p.baixadoAntes = f.isFile() && f.length() == p.tamanho;
            p.conferidoEm = agora;
        }
        return p.baixadoAntes;
    }

    private static ResourcePackRepository.Entry entrada(List<ResourcePackRepository.Entry> lista, Pacote p) {
        for (ResourcePackRepository.Entry e : lista) if (e.getResourcePackName().equals(p.arquivo)) return e;
        return null;
    }

    public static boolean emUso(Pacote p) {
        return entrada(repo().getRepositoryEntries(), p) != null;
    }

    /** Pacote baixado e lido pelo jogo (para o ícone). Chame {@link #atualizarLista()} antes. */
    public static ResourcePackRepository.Entry entrada(Pacote p) {
        return entrada(repo().getRepositoryEntriesAll(), p);
    }

    /** Relê a pasta resourcepacks (só abre os pacotes novos). */
    public static void atualizarLista() {
        repo().updateRepositoryEntriesAll();
    }

    /** Baixa em segundo plano; o progresso fica no próprio pacote. */
    public static void baixar(final Pacote p) {
        if (p.progresso >= 0) return;
        p.erro = null;
        p.progresso = 0;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    baixarAgora(p);
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override public void run() { atualizarLista(); }
                    });
                    System.out.println("[Pulsar] Texturas: " + p.nome + " baixado");
                } catch (Exception e) {
                    p.erro = "Não deu para baixar " + p.nome + ": " + e.getMessage();
                    System.err.println("[Pulsar] Texturas: " + p.erro);
                } finally {
                    p.progresso = -1;
                }
            }
        }, "Pulsar - baixando " + p.id);
        t.setDaemon(true);
        t.start();
    }

    private static void baixarAgora(Pacote p) throws Exception {
        File destino = arquivo(p);
        File parte = new File(destino.getParentFile(), destino.getName() + ".part");
        destino.getParentFile().mkdirs();
        HttpURLConnection c = (HttpURLConnection) new URL(p.url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "PulsarClient (texturas)");
        if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        InputStream in = c.getInputStream();
        OutputStream out = new FileOutputStream(parte);
        long lidos = 0;
        try {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                sha1.update(buf, 0, n);
                lidos += n;
                p.progresso = (int) Math.min(99, lidos * 100 / Math.max(1, p.tamanho));
            }
        } finally {
            out.close();
            in.close();
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : sha1.digest()) hex.append(String.format("%02x", b));
        if (!hex.toString().equalsIgnoreCase(p.sha1)) {
            parte.delete();
            throw new IllegalStateException("o arquivo chegou diferente do esperado, tente de novo");
        }
        if (destino.exists() && !destino.delete()) throw new IllegalStateException("o arquivo antigo está em uso");
        if (!parte.renameTo(destino)) throw new IllegalStateException("não deu para salvar na pasta resourcepacks");
        p.conferidoEm = 0;
    }

    /**
     * Liga o pacote por cima dos outros pacotes do jogador (ou desliga), tirando os outros do catálogo.
     * Recarrega as texturas na hora, como a tela de pacotes do jogo.
     */
    public static boolean usar(Pacote p, boolean ligar) {
        Minecraft mc = Minecraft.getMinecraft();
        ResourcePackRepository repo = repo();
        repo.updateRepositoryEntriesAll();
        List<ResourcePackRepository.Entry> ativos = new ArrayList<ResourcePackRepository.Entry>(repo.getRepositoryEntries());
        for (Pacote outro : catalogo()) {
            ResourcePackRepository.Entry e = entrada(ativos, outro);
            if (e != null) ativos.remove(e);
        }
        if (ligar) {
            ResourcePackRepository.Entry e = entrada(repo.getRepositoryEntriesAll(), p);
            if (e == null) {
                p.erro = "O jogo não conseguiu ler " + p.nome + ". Baixe de novo.";
                return false;
            }
            // O último da lista fica por cima dos outros.
            ativos.add(e);
        }
        repo.setRepositories(ativos);
        GameSettings gs = mc.gameSettings;
        gs.resourcePacks.clear();
        gs.incompatibleResourcePacks.clear();
        for (ResourcePackRepository.Entry e : ativos) {
            gs.resourcePacks.add(e.getResourcePackName());
            if (e.func_183027_f() != 1) gs.incompatibleResourcePacks.add(e.getResourcePackName());
        }
        gs.saveOptions();
        mc.refreshResources();
        System.out.println("[Pulsar] Texturas: " + (ligar ? "usando " : "deixou de usar ") + p.nome);
        return true;
    }
}
