// Login com conta Microsoft (a forma oficial de entrar no Minecraft hoje).
// Usa a biblioteca msmc, que abre a janela de login da Microsoft dentro do Electron.
const { Auth, lexicon } = require("msmc");

// Tela de login da Microsoft e mensagens de erro em português.
Object.assign(lexicon.lexicon, {
  "gui.market": "pt-BR",
  error: "Aconteceu um erro desconhecido",
  "error.auth": "Não foi possível entrar",
  "error.auth.microsoft": "Não foi possível entrar na conta Microsoft",
  "error.auth.xboxLive": "Não foi possível entrar no Xbox Live",
  "error.auth.xsts.userNotFound": "Essa conta Microsoft ainda não tem perfil Xbox. Entre uma vez em xbox.com e tente de novo",
  "error.auth.xsts.bannedCountry": "O Xbox Live não está disponível no país dessa conta",
  "error.auth.xsts.child": "Conta de menor de 18 anos: ela precisa estar num grupo familiar Microsoft com um adulto",
  "error.auth.minecraft.profile": "Essa conta não tem o Minecraft Java. Compre o jogo ou entre com outra conta",
  "error.gui.closed": "A janela de login foi fechada",
});

// Os erros do msmc chegam como chaves do lexicon ("error.gui.closed"); vira a frase em português.
function describeError(err) {
  const raw = err && err.message ? err.message : err;
  const key = typeof raw === "string" ? raw : raw && raw.ts;
  return (key && lexicon.lexicon[key]) || String(raw || "Erro desconhecido");
}

class Accounts {
  constructor(store) {
    // store: objeto com get()/set() para guardar o refresh token entre aberturas do launcher
    this.store = store;
    this.mc = null;
  }

  profile() {
    if (!this.mc) return null;
    return { name: this.mc.profile.name, uuid: this.mc.profile.id };
  }

  // onTop: pedido de dentro do jogo, então a janela de login precisa aparecer por cima do Minecraft.
  async login({ onTop = false, icon } = {}) {
    const auth = new Auth("select_account");
    const xbox = await auth.launch("electron", { width: 520, height: 680, title: "Entrar com Microsoft", alwaysOnTop: onTop, icon });
    this.mc = await xbox.getMinecraft();
    this.store.set({ msRefreshToken: xbox.save() });
    return this.profile();
  }

  // O token do Minecraft vale cerca de 24 horas. Com o launcher aberto há muito tempo, renova antes de jogar.
  async fresh() {
    if (!this.mc) throw new Error("Entre com sua conta Microsoft antes de jogar.");
    if (!this.mc.validate()) {
      await this.mc.refresh(true);
      if (this.mc.parent && typeof this.mc.parent.save === "function") this.store.set({ msRefreshToken: this.mc.parent.save() });
    }
    return this.mc;
  }

  // Sessão para o jogo trocar de conta sem fechar.
  session() {
    if (!this.mc) return null;
    return { name: this.mc.profile.name, uuid: this.mc.profile.id, token: this.mc.mcToken };
  }

  // Tenta entrar sozinho com o token salvo da última vez.
  async restore() {
    const token = this.store.get().msRefreshToken;
    if (!token) return null;
    try {
      const auth = new Auth("select_account");
      const xbox = await auth.refresh(token);
      this.mc = await xbox.getMinecraft();
      this.store.set({ msRefreshToken: xbox.save() });
      return this.profile();
    } catch {
      this.store.set({ msRefreshToken: "" });
      return null;
    }
  }

  logout() {
    this.mc = null;
    this.store.set({ msRefreshToken: "" });
  }

  // Objeto de autorização no formato que o minecraft-launcher-core espera.
  mclc() {
    if (!this.mc) throw new Error("Entre com sua conta Microsoft antes de jogar.");
    return this.mc.mclc();
  }
}

module.exports = { Accounts, describeError };
