package dev.pulsarclient.cosmeticos;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

/**
 * As animações dos emotes. Cada uma recebe o tempo desde o começo (em segundos) e mexe nos ângulos do boneco,
 * misturando com a pose normal do jogo no começo e no fim, para entrar e sair sem tranco.
 *
 * Ângulos do boneco (radianos): rotateAngleX negativo levanta o braço para a frente; no braço direito,
 * rotateAngleZ positivo abre para fora e rotateAngleY negativo puxa para o meio do corpo (no esquerdo, o contrário).
 */
final class Emotes {
    /** Momento da Supernova em que as estrelas explodem. */
    static final float ESTOURO = 0.75f;
    private static final float PI = (float) Math.PI;

    private Emotes() {}

    static float duracao(String id) {
        if (id.equals("supernova")) return 2.6f;
        if (id.equals("danca")) return 6f;
        if (id.equals("facepalm")) return 2.8f;
        return 0;
    }

    static void pose(String id, float t, ModelBiped m) {
        if (id.equals("supernova")) supernova(t, m);
        else if (id.equals("danca")) danca(t, m);
        else if (id.equals("facepalm")) facepalm(t, m);
        ModelBase.copyModelAngles(m.bipedHead, m.bipedHeadwear);
    }

    /** Junta energia com os braços na frente e explode com os braços para cima, em Y. */
    private static void supernova(float t, ModelBiped m) {
        float w = entrada(t, 0.2f, 2.6f, 0.4f);
        if (t < 0.7f) {
            float k = suave(t / 0.7f);
            float tremor = (float) Math.sin(t * 50) * 0.03f * k;
            alvo(m.bipedRightArm, -0.95f * k + tremor, -0.45f * k, 0.05f, w);
            alvo(m.bipedLeftArm, -0.95f * k - tremor, 0.45f * k, -0.05f, w);
            alvo(m.bipedHead, 0.35f * k, 0, 0, w);
        } else {
            float k = suave(Math.min(1, (t - 0.7f) / 0.18f));
            float tremor = t > 0.95f ? (float) Math.sin(t * 38) * 0.025f : 0;
            alvo(m.bipedRightArm, mix(-0.95f, -0.15f, k), mix(-0.45f, 0, k), mix(0.05f, 2.55f, k) + tremor, w);
            alvo(m.bipedLeftArm, mix(-0.95f, -0.15f, k), mix(0.45f, 0, k), mix(-0.05f, -2.55f, k) - tremor, w);
            alvo(m.bipedHead, mix(0.35f, -0.45f, k), 0, 0, w);
        }
    }

    /** Braços sobem alternados, corpo balança e as pernas marcam o passo. */
    private static void danca(float t, ModelBiped m) {
        float w = entrada(t, 0.25f, 6f, 0.35f);
        float p = t * 2 * PI / 1.2f;
        float s = (float) Math.sin(p);
        float c = (float) Math.cos(p);
        alvo(m.bipedRightArm, -0.3f * c, 0, 0.35f + 1.7f * Math.max(0, s), w);
        alvo(m.bipedLeftArm, 0.3f * c, 0, -(0.35f + 1.7f * Math.max(0, -s)), w);
        alvo(m.bipedBody, 0, 0.22f * s, 0, w);
        alvo(m.bipedHead, 0.08f * (float) Math.sin(p * 2), -0.3f * s, 0.12f * s, w);
        alvo(m.bipedRightLeg, 0.35f * s, 0, 0.04f, w);
        alvo(m.bipedLeftLeg, -0.35f * s, 0, -0.04f, w);
    }

    /** Mão direita na cara, cabeça baixa balançando de leve. */
    private static void facepalm(float t, ModelBiped m) {
        float w = entrada(t, 0.35f, 2.8f, 0.4f);
        float balanca = t > 0.9f && t < 2.3f ? (float) Math.sin((t - 0.9f) * 7) * 0.13f : 0;
        alvo(m.bipedHead, 0.5f, balanca, 0, w);
        alvo(m.bipedRightArm, -2.05f, -0.55f + balanca, 0.1f, w);
        alvo(m.bipedLeftArm, 0.1f, 0, -0.08f, w);
    }

    // ---------- utilidades ----------

    /** Peso do emote sobre a pose do jogo: sobe no começo, fica em 1 e desce no fim. */
    private static float entrada(float t, float sobe, float total, float desce) {
        float w = Math.min(1, t / sobe);
        w = Math.min(w, Math.max(0, (total - t) / desce));
        return suave(w);
    }

    private static void alvo(ModelRenderer parte, float x, float y, float z, float w) {
        parte.rotateAngleX = mix(parte.rotateAngleX, x, w);
        parte.rotateAngleY = mix(parte.rotateAngleY, y, w);
        parte.rotateAngleZ = mix(parte.rotateAngleZ, z, w);
    }

    private static float mix(float a, float b, float k) {
        return a + (b - a) * k;
    }

    private static float suave(float k) {
        k = Math.max(0, Math.min(1, k));
        return k * k * (3 - 2 * k);
    }
}
