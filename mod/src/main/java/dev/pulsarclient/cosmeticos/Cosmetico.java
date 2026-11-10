package dev.pulsarclient.cosmeticos;

/** Um item da loja do Pulsar: acessório (fica no corpo) ou emote (animação que a pessoa toca). */
public final class Cosmetico {
    public enum Tipo { ACESSORIO, EMOTE }

    public final String id;
    public final String nome;
    public final String descricao;
    public final Tipo tipo;
    public final boolean gratis;

    Cosmetico(String id, String nome, String descricao, Tipo tipo, boolean gratis) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.gratis = gratis;
    }
}
