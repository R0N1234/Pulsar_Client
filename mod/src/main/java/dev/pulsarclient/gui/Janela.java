package dev.pulsarclient.gui;

import org.lwjgl.opengl.Display;

/**
 * Nome da janela do jogo. O ícone continua o do próprio Minecraft, para o jogador reconhecer o jogo na barra de tarefas;
 * o logo do Pulsar fica no launcher, no instalador e nas telas do client.
 */
public final class Janela {
    private Janela() {}

    public static void aplicar(String version) {
        try {
            Display.setTitle("Minecraft 1.8.9 - Pulsar Client " + version);
        } catch (Throwable t) {
            System.err.println("[Pulsar] Não deu para trocar o nome da janela: " + t);
        }
    }
}
