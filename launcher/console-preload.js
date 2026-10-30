const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("pulsarConsole", {
  lines: () => ipcRenderer.invoke("console:lines"),
  onLines: (fn) => ipcRenderer.on("console:lines", (_e, batch) => fn(batch)),
});
