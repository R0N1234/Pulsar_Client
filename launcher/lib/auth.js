// Login com conta Microsoft (a forma oficial de entrar no Minecraft hoje).
// Usa a biblioteca msmc, que abre a janela de login da Microsoft dentro do Electron.
const crypto = require("crypto");
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

// Nick de conta offline: as mesmas regras de nome do Minecraft.
const NICK = /^[A-Za-z0-9_]{3,16}$/;
const MAX_OFFLINE = 30;

// UUID de conta offline, igual ao que os servidores calculam ("OfflinePlayer:<nick>").
function uuidOffline(nick) {
  const h = crypto.createHash("md5").update("OfflinePlayer:" + nick, "utf8").digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  return h.toString("hex");
}

/**
 * Contas do launcher: a conta Microsoft (original) e os nicks offline.
 * Os nicks offline só ficam liberados depois que uma conta Microsoft com o Minecraft entrou neste PC,
 * como nos launchers abertos conhecidos. Eles servem para jogar sem internet, em LAN e em servidores
 * que aceitam conta offline.
 */
class Accounts {
  constructor(store, prefs) {
    // store: get()/set() do refresh token (criptografado); prefs: get()/set() das contas offline (launcher.json)
    this.store = store;
    this.prefs = prefs;
    this.mc = null;
  }

  profile() {
    if (!this.mc) return null;
    return { name: this.mc.profile.name, uuid: this.mc.profile.id };
  }

  offline() {
    const lista = this.prefs.get().contasOffline;
    return Array.isArray(lista) ? lista.filter((n) => NICK.test(n)) : [];
  }

  liberado() {
    return !!this.mc || !!this.prefs.get().originalVerificada;
  }

  // Conta que vai abrir o jogo: "microsoft" ou um nick offline.
  ativa() {
    const a = this.prefs.get().contaAtiva;
    if (a && a !== "microsoft" && this.liberado() && this.offline().includes(a)) return a;
    return "microsoft";
  }

  estado() {
    return { microsoft: this.profile(), offline: this.offline(), ativa: this.ativa(), liberado: this.liberado() };
  }

  lembrarOriginal() {
    if (this.mc) this.prefs.set({ originalVerificada: true });
  }

  // onTop: pedido de dentro do jogo, então a janela de login precisa aparecer por cima do Minecraft.
  async login({ onTop = false, icon } = {}) {
    const auth = new Auth("select_account");
    const xbox = await auth.launch("electron", { width: 520, height: 680, title: "Entrar com Microsoft", alwaysOnTop: onTop, icon });
    this.mc = await xbox.getMinecraft();
    this.store.set({ msRefreshToken: xbox.save() });
    this.lembrarOriginal();
    this.prefs.set({ contaAtiva: "microsoft" });
    return this.estado();
  }

  // O token do Minecraft vale cerca de 24 horas. Com o launcher aberto há muito tempo, renova antes de jogar.
  async fresh() {
    if (this.ativa() !== "microsoft") return;
    if (!this.mc) throw new Error("Entre com sua conta Microsoft antes de jogar.");
    if (!this.mc.validate()) {
      await this.mc.refresh(true);
      if (this.mc.parent && typeof this.mc.parent.save === "function") this.store.set({ msRefreshToken: this.mc.parent.save() });
    }
  }

  // Sessão para o jogo trocar de conta sem fechar.
  session() {
    const a = this.ativa();
    if (a !== "microsoft") return { name: a, uuid: uuidOffline(a), token: "0", offline: true };
    if (!this.mc) return null;
    return { name: this.mc.profile.name, uuid: this.mc.profile.id, token: this.mc.mcToken };
  }

  // Tenta entrar sozinho com o token salvo da última vez.
  async restore() {
    const token = this.store.get().msRefreshToken;
    if (token) {
      try {
        const auth = new Auth("select_account");
        const xbox = await auth.refresh(token);
        this.mc = await xbox.getMinecraft();
        this.store.set({ msRefreshToken: xbox.save() });
        this.lembrarOriginal();
      } catch {
        this.store.set({ msRefreshToken: "" });
      }
    }
    return this.estado();
  }

  logout() {
    this.mc = null;
    this.store.set({ msRefreshToken: "" });
    return this.estado();
  }

  usar(conta) {
    conta = String(conta || "");
    if (conta !== "microsoft") {
      if (!this.liberado()) throw new Error("Entre uma vez com uma conta Microsoft que tenha o Minecraft para liberar as contas offline.");
      if (!this.offline().includes(conta)) throw new Error("Essa conta offline não existe.");
    }
    this.prefs.set({ contaAtiva: conta });
    return this.estado();
  }

  adicionarOffline(nick) {
    nick = String(nick || "").trim();
    if (!this.liberado()) throw new Error("Entre uma vez com uma conta Microsoft que tenha o Minecraft para liberar as contas offline.");
    if (!NICK.test(nick)) throw new Error("O nick precisa ter de 3 a 16 letras, números ou _ (sem espaço nem acento).");
    const lista = this.offline();
    const igual = lista.find((n) => n.toLowerCase() === nick.toLowerCase());
    if (igual) return this.usar(igual);
    if (lista.length >= MAX_OFFLINE) throw new Error(`Dá para guardar até ${MAX_OFFLINE} nicks.`);
    this.prefs.set({ contasOffline: [...lista, nick], contaAtiva: nick });
    return this.estado();
  }

  removerOffline(nick) {
    const lista = this.offline().filter((n) => n !== nick);
    const patch = { contasOffline: lista };
    if (this.prefs.get().contaAtiva === nick) patch.contaAtiva = "microsoft";
    this.prefs.set(patch);
    return this.estado();
  }

  // Objeto de autorização no formato que o minecraft-launcher-core espera.
  mclc() {
    const a = this.ativa();
    if (a !== "microsoft") {
      return { access_token: "0", client_token: uuidOffline(a), uuid: uuidOffline(a), name: a, user_properties: "{}", meta: { type: "mojang" } };
    }
    if (!this.mc) throw new Error("Entre com sua conta Microsoft antes de jogar.");
    return this.mc.mclc();
  }
}

module.exports = { Accounts, describeError, uuidOffline };
