const { contextBridge, ipcRenderer } = require("electron");

// Janela escondida que grava o jogo (src/gravador.html). Só fala com o main.js.
contextBridge.exposeInMainWorld("gravador", {
  onComando: (fn) => ipcRenderer.on("gravar:comando", (_e, c) => fn(c)),
  responder: (r) => ipcRenderer.send("gravar:resposta", r),
  pedaco: (p) => ipcRenderer.send("gravar:pedaco", p),
  log: (texto) => ipcRenderer.send("gravar:log", String(texto)),
});
