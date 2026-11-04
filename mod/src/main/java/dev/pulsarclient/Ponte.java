package dev.pulsarclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Avisos rápidos do jogo para o launcher (Sinal Discord, Rádio Pulsar), pela mesma ponte local da troca de conta.
 * Roda numa linha separada para nunca travar o jogo; se o launcher não responder, o aviso é perdido sem erro.
 */
public final class Ponte {
    private static final String PORT = System.getProperty("pulsar.ponte");
    private static final String KEY = System.getProperty("pulsar.chave");
    private static final ExecutorService FILA = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Pulsar - ponte");
            t.setDaemon(true);
            return t;
        }
    });

    private Ponte() {}

    /** O jogo foi aberto pelo launcher do Pulsar (e não por outro launcher). */
    public static boolean disponivel() {
        return PORT != null && KEY != null;
    }

    /** Resposta de um pedido ao launcher (chega na linha da ponte, não na do jogo). */
    public interface Resposta {
        void chegou(JsonObject resposta);
    }

    public static void avisar(final String caminho, final JsonObject corpo) {
        perguntar(caminho, corpo, null);
    }

    /** Pede algo ao launcher; a resposta (ou nada, se o launcher não responder) chega em "resposta". */
    public static void perguntar(final String caminho, final JsonObject corpo, final Resposta resposta) {
        if (!disponivel()) return;
        FILA.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + PORT + caminho).openConnection(Proxy.NO_PROXY);
                    c.setRequestMethod("POST");
                    c.setRequestProperty("X-Pulsar-Chave", KEY);
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    c.setConnectTimeout(2000);
                    c.setReadTimeout(5000);
                    c.setDoOutput(true);
                    OutputStream out = c.getOutputStream();
                    try {
                        out.write(corpo.toString().getBytes(StandardCharsets.UTF_8));
                    } finally {
                        out.close();
                    }
                    InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream();
                    if (in == null) return;
                    try {
                        if (resposta != null) {
                            JsonElement el = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
                            if (el != null && el.isJsonObject()) resposta.chegou(el.getAsJsonObject());
                        }
                    } finally {
                        in.close();
                    }
                } catch (Exception ignored) {
                    // Launcher fechado ou ocupado: o próximo aviso tenta de novo.
                }
            }
        });
    }
}
