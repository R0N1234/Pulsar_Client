const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("pulsar", {
  getConfig: () => ipcRenderer.invoke("config:get"),
  setConfig: (patch) => ipcRenderer.invoke("config:set", patch),
  getMods: () => ipcRenderer.invoke("mods:get"),
  setMod: (id, enabled) => ipcRenderer.invoke("mods:set", id, enabled),
  restoreAccount: () => ipcRenderer.invoke("account:restore"),
  login: () => ipcRenderer.invoke("account:login"),
  logout: () => ipcRenderer.invoke("account:logout"),
  launch: () => ipcRenderer.invoke("game:launch"),
  onStatus: (fn) => ipcRenderer.on("game:status", (_e, s) => fn(s)),
  onLog: (fn) => ipcRenderer.on("game:log", (_e, l) => fn(l)),
  minimize: () => ipcRenderer.send("window:minimize"),
  close: () => ipcRenderer.send("window:close"),
});
