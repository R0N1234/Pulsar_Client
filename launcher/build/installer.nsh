; Antes de desinstalar a versão antiga, fecha o Pulsar que ainda estiver aberto (launcher, ícone perto do relógio
; ou partes do Electron). Com ele aberto, o desinstalador antigo para com
; "Falha ao desinstalar os arquivos do aplicativo antigo".
!macro customInit
  nsExec::Exec 'taskkill /F /T /IM "Pulsar Client.exe"'
  Pop $0
  Sleep 1500
!macroend
