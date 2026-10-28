// Login com conta Microsoft (a forma oficial de entrar no Minecraft hoje).
// Usa a biblioteca msmc, que abre a janela de login da Microsoft dentro do Electron.
const { Auth } = require("msmc");

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

  async login() {
    const auth = new Auth("select_account");
    const xbox = await auth.launch("electron", { width: 520, height: 680, title: "Entrar com Microsoft" });
    this.mc = await xbox.getMinecraft();
    this.store.set({ msRefreshToken: xbox.save() });
    return this.profile();
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

module.exports = { Accounts };
