package dev.pulsarclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sons da pasta <code>.minecraft/pulsar/sons/</code> por cima dos que vêm no Pulsar.
 * O jogo abre o arquivo na hora de tocar, então trocar o .ogg vale no próximo abate, sem reiniciar.
 *
 * Para as Sequências de Abate (<code>sons/sequencias/</code>) o nome é flexível:
 * <ul>
 *   <li><code>double_kill.ogg</code> substitui a voz do Pulsar (maiúsculas, "_" e "-" não importam: <code>multikill.ogg</code> vale por <code>multi_kill</code>);</li>
 *   <li><code>double_kill_2.ogg</code>, <code>_3</code>... são variações da mesma fala, sorteadas a cada vez;</li>
 *   <li><code>MASCULINO_double_kill.ogg</code> ou a subpasta <code>MASCULINO/double_kill.ogg</code> fazem parte da voz "MASCULINO",
 *       que a pessoa escolhe no painel do mod. O que faltar numa voz continua com a voz do Pulsar.</li>
 * </ul>
 */
public final class SonsLocais implements IResourcePack {
    private static SonsLocais instalado;
    private static final Random SORTEIO = new Random();
    private final File pasta;
    /** Voz escolhida para as sequências ("" = os arquivos soltos, sem prefixo). */
    private volatile String voz = "";

    private SonsLocais(File pasta) {
        this.pasta = pasta;
    }

    public static File pasta() {
        return instalado == null ? new File(Minecraft.getMinecraft().mcDataDir, "pulsar/sons") : instalado.pasta;
    }

    /** Entra na lista de pacotes de fábrica do jogo, depois do jar do Pulsar (o que vem depois ganha). */
    @SuppressWarnings("unchecked")
    public static void instalar(File mcDataDir) {
        if (instalado != null) return;
        try {
            File pasta = new File(mcDataDir, "pulsar/sons");
            new File(pasta, "sequencias").mkdirs();
            List<IResourcePack> lista = (List<IResourcePack>) ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
            instalado = new SonsLocais(pasta);
            lista.add(instalado);
            System.out.println("[Pulsar] Sons locais: lendo de " + pasta);
        } catch (Throwable t) {
            System.err.println("[Pulsar] Sons locais indisponíveis: " + t);
        }
    }

    // ---------- vozes das sequências ----------

    public static void usarVoz(String nome) {
        if (instalado != null) instalado.voz = nome == null ? "" : nome;
    }

    public static String vozAtual() {
        return instalado == null ? "" : instalado.voz;
    }

    private static File pastaSequencias() {
        return new File(pasta(), "sequencias");
    }

    /** Só letras e números, minúsculas: "Multi_Kill" e "multikill" ficam iguais. */
    private static String chave(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) if (Character.isLetterOrDigit(c)) b.append(c);
        return b.toString();
    }

    /** Tira a extensão e a variação do fim: "holy_shit_2.ogg" vira "holy_shit". */
    private static String semVariacao(String arquivo) {
        String base = arquivo;
        int ponto = base.lastIndexOf('.');
        if (ponto > 0) base = base.substring(0, ponto);
        return base.replaceFirst("[ _-]+\\d{1,2}$", "");
    }

    private static boolean ehOgg(File f) {
        return f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".ogg");
    }

    /**
     * Confere se o arquivo é a fala pedida: o nome inteiro, ou "VOZ" + separador + nome.
     * Devolve a voz do prefixo ("" sem prefixo) ou null se não for essa fala.
     */
    private static String vozDoArquivo(String arquivo, String nome) {
        String base = semVariacao(arquivo);
        String alvo = chave(nome);
        if (chave(base).equals(alvo)) return "";
        for (int i = 1; i < base.length() - 1; i++) {
            char c = base.charAt(i);
            if (c != '_' && c != '-' && c != ' ' && c != '.') continue;
            if (chave(base.substring(i + 1)).equals(alvo)) return base.substring(0, i);
        }
        return null;
    }

    /** Todos os nomes de fala que o Pulsar conhece (as duas listas). */
    private static Set<String> nomesConhecidos() {
        Set<String> s = new TreeSet<String>();
        Collections.addAll(s, Sequencias.NOMES);
        Collections.addAll(s, Sequencias.NOMES_ARENA);
        return s;
    }

    /** As vozes achadas na pasta: subpastas com .ogg dentro e prefixos dos arquivos soltos. Em ordem alfabética. */
    public static List<String> vozes() {
        Set<String> achadas = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        File[] arquivos = pastaSequencias().listFiles();
        if (arquivos == null) return new ArrayList<String>();
        Set<String> nomes = nomesConhecidos();
        for (File f : arquivos) {
            if (f.isDirectory()) {
                File[] dentro = f.listFiles();
                if (dentro == null) continue;
                for (File d : dentro) if (ehOgg(d)) { achadas.add(f.getName()); break; }
            } else if (ehOgg(f)) {
                for (String n : nomes) {
                    String v = vozDoArquivo(f.getName(), n);
                    if (v != null && !v.isEmpty()) achadas.add(v);
                }
            }
        }
        return new ArrayList<String>(achadas);
    }

    /** Os arquivos que servem para a fala "nome" na voz pedida (todas as variações). */
    public static List<File> arquivosDe(String voz, String nome) {
        List<File> out = new ArrayList<File>();
        File raiz = pastaSequencias();
        File[] soltos = raiz.listFiles();
        if (soltos != null) {
            for (File f : soltos) {
                if (!ehOgg(f)) continue;
                String v = vozDoArquivo(f.getName(), nome);
                if (v != null && v.equalsIgnoreCase(voz)) out.add(f);
            }
        }
        if (!voz.isEmpty()) {
            File[] dentro = new File(raiz, voz).listFiles();
            if (dentro != null) {
                for (File f : dentro) if (ehOgg(f) && "".equals(vozDoArquivo(f.getName(), nome))) out.add(f);
            }
        }
        Collections.sort(out);
        return out;
    }

    /** O arquivo que toca agora para a fala: uma variação sorteada da voz escolhida, ou null (fica a voz do Pulsar). */
    public static File escolher(String nome) {
        List<File> lista = arquivosDe(vozAtual(), nome);
        if (lista.isEmpty()) return null;
        return lista.get(lista.size() == 1 ? 0 : SORTEIO.nextInt(lista.size()));
    }

    /** Quantas falas da lista têm arquivo na voz (para o painel e o teste). */
    public static int falasNaVoz(String voz, String[] nomes) {
        int n = 0;
        for (String nome : nomes) if (!arquivosDe(voz, nome).isEmpty()) n++;
        return n;
    }

    // ---------- pacote de recursos ----------

    private File arquivo(ResourceLocation l) {
        if (!"pulsar".equals(l.getResourceDomain()) || !l.getResourcePath().startsWith("sounds/")) return null;
        String resto = l.getResourcePath().substring(7);
        if (resto.contains("..")) return null;
        if (resto.startsWith("sequencias/") && resto.endsWith(".ogg")) {
            File escolhido = escolher(resto.substring(11, resto.length() - 4));
            if (escolhido != null) return escolhido;
        }
        File f = new File(pasta, resto);
        return f.isFile() ? f : null;
    }

    @Override
    public InputStream getInputStream(ResourceLocation l) throws IOException {
        File f = arquivo(l);
        if (f == null) throw new FileNotFoundException(l.toString());
        return new FileInputStream(f);
    }

    @Override
    public boolean resourceExists(ResourceLocation l) {
        return arquivo(l) != null;
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton("pulsar");
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer serializer, String name) throws IOException {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("sem imagem");
    }

    @Override
    public String getPackName() {
        return "Sons do Pulsar";
    }
}
