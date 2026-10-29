package dev.pulsarclient.mods;

/** Campo de Colisão: mostra a hitbox das entidades (o mesmo do F3+B, num botão). */
public class CampoColisao extends PulsarMod {
    public CampoColisao() {
        super("campo_colisao", "Campo de Colisão", "Mostra a hitbox de jogadores e projéteis", Category.COMBATE, false);
    }

    @Override
    protected void onEnable() {
        mc.getRenderManager().setDebugBoundingBox(true);
    }

    @Override
    protected void onDisable() {
        mc.getRenderManager().setDebugBoundingBox(false);
    }

    @Override
    public void tick() {
        // O RenderManager só existe depois que o jogo termina de carregar.
        if (isEnabled() && mc.getRenderManager() != null && !mc.getRenderManager().isDebugBoundingBox()) onEnable();
    }
}
