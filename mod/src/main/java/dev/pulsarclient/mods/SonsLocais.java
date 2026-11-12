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
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sons da pasta <code>.minecraft/pulsar/sons/</code> por cima dos que vêm no Pulsar.
 * Um arquivo <code>pulsar/sons/sequencias/first_blood.ogg</code> substitui o som <code>pulsar:sequencias/first_blood</code>.
 * O jogo abre o arquivo na hora de tocar, então trocar o .ogg vale no próximo abate, sem reiniciar.
 */
public final class SonsLocais implements IResourcePack {
    private static SonsLocais instalado;
    private final File pasta;

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

    private File arquivo(ResourceLocation l) {
        if (!"pulsar".equals(l.getResourceDomain()) || !l.getResourcePath().startsWith("sounds/")) return null;
        String resto = l.getResourcePath().substring(7);
        if (resto.contains("..")) return null;
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
