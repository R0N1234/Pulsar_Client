<p align="center">
  <img src="launcher/build/icon.png" width="140" alt="Logo do Pulsar Client">
</p>

<h1 align="center">Pulsar Client</h1>

<p align="center">
  <b>O client de PvP para Minecraft 1.8.9 feito para PC fraco e para o FullPvP.</b><br>
  Mais FPS no meio da treta, HUD limpa e mods pensados para quem vive de combo, pote e caixa.
</p>

<p align="center">
  <a href="https://github.com/R0N1234/Pulsar_Client/releases/latest"><b>⬇️ Baixar para Windows</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/R0N1234/Pulsar_Client/issues/new?template=sugestao.yml">💡 Mandar sugestão</a>
  &nbsp;·&nbsp;
  <a href="https://github.com/R0N1234/Pulsar_Client/issues/new?template=problema.yml">🐞 Avisar de um problema</a>
</p>

<p align="center">
  <img src="docs/menu.png" width="49%" alt="Menu do Pulsar com o universo girando atrás">
  <img src="docs/painel.png" width="49%" alt="Painel de mods do Pulsar (Shift direito)">
</p>

---

## 🚀 Por que o Pulsar?

**Feito para PC fraco.** O **Motor Pulsar** é um otimizador próprio que entra dentro do jogo: não desenha o que está atrás de parede, deixa leve quem está longe, corta grama alta, flores e partículas em excesso e segura o FPS quando o jogo fica em segundo plano. Numa mina de FullPvP com 40 jogadores, só os mais perto de você ficam com todos os detalhes. Os de longe continuam com a skin, mas sem o peso da armadura e do item na mão.

**Feito para o FullPvP.** CPS, combo, alcance, durabilidade da armadura, contador de potes e pérolas, tempo de recarga das maçãs, TNT, zoom, mira personalizada e a Barra Pulsar no topo da tela com FPS, ping e hora. Tudo arrastável no editor de HUD.

**Bonito sem pesar.** Tema espacial do começo ao fim: menu com um universo 360° girando devagar (Andrômeda, os planetas e um pulsar), painel com ícones e favoritos, fonte própria. Tudo desenhado para custar quase nada de FPS.

**Fácil de usar.** Um launcher só: entra com a conta Microsoft, baixa o Java e o Forge sozinho, atualiza sozinho e funciona com ou sem OptiFine.

<p align="center">
  <img src="docs/hud.png" width="49%" alt="HUD do Pulsar dentro do jogo">
  <img src="docs/opcoes.png" width="49%" alt="Opções do Motor Pulsar">
</p>

## 🧩 Mods

**HUD**
- **Barra Pulsar**: mini painel no topo com o client, FPS, ms, hora e a música que está tocando no Spotify (com capa)
- **Teclas Pulse** e **Frequência CPS**: WASD, cliques e CPS na tela
- **FPS**, **Latência**, **Coordenadas**, **Bússola**, **Relógio**, **Memória**, **Servidor**, **Saturação** e **Cronômetro**
- **Placar** (sem os números vermelhos), **Lista do TAB** (cabeça da skin e ping em número), **Barra de Chefe** e **Pacote Ativo**

**Combate**
- **Combo**, **Alcance**, **Blindagem**, **Efeitos**, **Contador de Itens**, **Recargas** e **Contador TNT**
- **Mira de Pulsar** (formato, cor, tamanho e contorno), **Impacto**, **Campo de Colisão** e **Tremor de Dano**
- **Animações Clássicas** (bloqueio e swing da 1.7), **Propulsão** (correr fixo) e **Inventário** (trava itens contra drop)

**Visual**
- **Telescópio** (zoom), **Órbita Livre** (câmera 360° em volta do personagem), **Fonte do Pulsar** e **Cores Vivas**
- **Aura de Encanto** (cor do brilho de encantamento, até arco-íris), **Contorno**, **Supernova**, **Atmosfera**, **Poeira Estelar**, **Rastro de Cometa**, **Gravidade de Itens**, **Nebulosa** e **Meu Nick**

**Servidores e social**
- **Atalhos de Texto** (uma tecla manda /lobby, /home...), **GG Automático**, **Sinal Discord** e **Capturas**

**Motor Pulsar**
- Perfis **Desempenho**, **Desempenho + Qualidade** e **Qualidade+**, com cada parte ajustável

## 📥 Como instalar

1. Baixe o **Pulsar-Client-Instalador** na [página de lançamentos](https://github.com/R0N1234/Pulsar_Client/releases/latest).
2. Abra o instalador. Ele instala e abre o launcher sozinho.
3. Entre com a sua conta Microsoft (a que tem o Minecraft) e clique em **JOGAR**.

No jogo, aperte **Shift direito** para abrir o painel de mods. O launcher atualiza sozinho quando sai versão nova.

## ❓ Perguntas rápidas

**Posso usar em qualquer servidor?** O Pulsar é feito de HUD, visual e desempenho, como os clients conhecidos. Mesmo assim, cada servidor tem as suas regras: na dúvida, confira quais mods são permitidos lá.

**Funciona com OptiFine?** Sim. No launcher, escolha o modo **Pulsar + OptiFine**.

**A Barra Pulsar mostra a música de que jeito?** Ela lê a música pelos controles de mídia do Windows, sem login e sem pedir senha do Spotify. A capa vem da busca pública do iTunes, e dá para desligar a capa nas opções da barra.

**Achei um problema ou tenho uma ideia.** Use o botão **Sugestões** no menu do jogo ou abra uma [sugestão](https://github.com/R0N1234/Pulsar_Client/issues/new?template=sugestao.yml) ou um [problema](https://github.com/R0N1234/Pulsar_Client/issues/new?template=problema.yml) aqui no GitHub.

---

<details>
<summary><b>Para quem quer mexer no código</b></summary>

| Pasta | O que é | Tecnologia |
|---|---|---|
| `launcher/` | O programa que abre no PC: login, mods, ajustes e o botão JOGAR | Electron (Node.js) |
| `mod/` | O que roda dentro do Minecraft: HUD, mods, painel e o Motor Pulsar | Forge 1.8.9 (Java 8) |
| `wtap/` | W-Tap e Blockhit como um mod Forge separado | Forge 1.8.9 (Java 8) |
| `tools/` | Geradores da arte (universo, planetas, ícones e logo) | Python + Pillow |

Compilar o mod: `cd mod && ./gradlew build` (precisa do Java 21 para o Gradle e do Java 8 para o jogo).
Abrir o launcher: `cd launcher && npm install && npm start`.

Cada push roda o jogo de verdade no CI (Linux, Windows e com OptiFine), passa por todas as telas e mods e confere o log.
Para lançar uma versão: **Actions** > **Lançar versão** > **Run workflow**, com o número da versão e o que mudou.
</details>

<p align="center"><sub>Pulsar Client não é afiliado à Mojang nem à Microsoft. Minecraft é marca da Mojang AB.</sub></p>
