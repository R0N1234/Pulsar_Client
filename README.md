# Pulsar Client

Client de PVP para Minecraft 1.8.9 com tema espacial. O projeto tem duas partes:

| Pasta | O que é | Tecnologia |
|---|---|---|
| `launcher/` | O programa que você abre no PC: login Microsoft, tela inicial, aba de mods, ajustes e o botão JOGAR | Electron (Node.js) |
| `mod/` | O que roda dentro do Minecraft: HUD, Teclas Pulse, CPS e os outros mods | Forge 1.8.9 (Java 8) |

O launcher instala o Forge 1.8.9 e o jar do mod numa pasta própria (`%APPDATA%\.pulsar` no Windows), separada da sua `.minecraft`.

## O que já funciona (versão 0.5)

Launcher:
- Tela inicial no estilo Núcleo (pulsar no centro, 1.8.9 e botão JOGAR) e o logo PC com um pulsar atrás
- Aba Mods no estilo Painel, com o catálogo, busca, filtros e prévia da HUD
- Login com conta Microsoft em português (o token fica salvo criptografado pelo sistema e é renovado antes de jogar)
- Usa a `.minecraft` com uma instância própria (`.minecraft/pulsar/instancia-1.8.9`), baixa o Java 8 sozinho
- Modos Forge + Pulsar, OptiFine + Forge + Pulsar e Só OptiFine (o jar do OptiFine é escolhido pelo jogador)
- **Console do Pulsar**: janela própria com o registro do jogo, cores, filtro e copiar (abre ao clicar em Jogar; dá para desligar em Ajustes)
- Ponte local (`lib/ponte.js`, só 127.0.0.1 e com chave nova a cada jogo) para trocar de conta Microsoft pelo menu do Minecraft
- Atualização automática pelos Releases

Mod (30 prontos):
- HUD: **Teclas Pulse**, **Frequência CPS**, **FPS**, **Latência**, **Coordenadas**, **Relógio**, **Memória**, **Bússola**, **Servidor**, **Saturação**, **Cronômetro** (tecla P)
- Combate: **Propulsão** (correr fixo, tecla R), **Mira de Pulsar**, **Blindagem**, **Efeitos**, **Combo**, **Alcance**, **Tremor de Dano**, **Campo de Colisão**, **Contador de Itens**, **Contador TNT**, **Inventário** (trava itens com L para não dropar nem tirar do lugar; inventário centralizado com efeitos)
- Visual: **Telescópio** (zoom no C), **Órbita Livre** (segurando Alt, a câmera gira 360° em volta do personagem), **Meu Nick** (seu nick no F5), **Supernova**, **Contorno**, **Atmosfera**, **Poeira Estelar**
- **Motor Pulsar**: otimizador próprio com perfis **Desempenho**, **Desempenho + Qualidade** e **Qualidade+**. Tem ganchos dentro do jogo (`core/MotorTransformer`): não desenha entidades escondidas atrás de paredes ou longe (nem a sombra e o nome delas), baús/placas/cabeças longe, pode desligar a animação de água/lava/fogo e limitar partículas novas. Também segura o FPS em segundo plano e esconde suportes de armadura (mantém hologramas)

Menu principal: caixinha no canto de cima com a sua conta. Clicar nela abre o login da Microsoft por cima do jogo e troca de conta sem fechar o Minecraft.

Tela de carregamento e menu: o jar também é um coremod (`dev.pulsarclient.core`) que troca a tela de carregamento do Forge pela do Pulsar (universo de fundo, "PC" no círculo e uma barra). Se der qualquer erro, a tela normal do Forge aparece. O menu principal do jogo é trocado pelo do Pulsar, com o mesmo fundo. A arte sai de `tools/arte_espaco.py`.

Painel do Pulsar: aperte **Shift direito** no jogo. Categorias à esquerda, busca, cartões dos mods (clique liga ou desliga, as três barras ou o botão direito abrem as opções) e o botão **Editar HUD**. O painel também abre pelo botão **Pulsar Client** no menu do Esc, pelo botão **Motor Pulsar** em Opções > Vídeo e pelo botão **Pulsar** no menu inicial.

Tudo fica em `.minecraft/pulsar/instancia-1.8.9/pulsar/config.json`: o launcher grava se cada mod está ligado, e o jogo grava as posições da HUD e as opções de cada mod.

## Como rodar no seu PC

Você precisa de:
- **Java 21** (para compilar) e **Java 8** (para jogar a 1.8.9). Recomendo o Temurin: https://adoptium.net
- **Node.js 22**: https://nodejs.org

1. Compile o mod:
   ```
   cd mod
   gradlew build        (no Windows)
   ./gradlew build      (Linux/Mac)
   ```
   O jar sai em `mod/build/libs/pulsar-client-0.3.0.jar`. A primeira compilação demora porque baixa o Minecraft e o Forge.

2. Abra o launcher:
   ```
   cd launcher
   npm install
   npm start
   ```

3. Entre com a Microsoft, confira em **Ajustes** o caminho do Java 8 (por exemplo `C:\Program Files\Eclipse Adoptium\jre-8.0.432.6-hotspot\bin\javaw.exe`) e clique em **JOGAR**.

Para gerar o instalador do Windows: `npm run dist` dentro de `launcher/` (compile o mod antes, o instalador leva o jar junto).

## Atualizações automáticas

As versões saem nos **Releases** deste repositório, que continua privado. O launcher procura versão nova ao abrir e a cada 30 minutos, baixa em segundo plano e mostra no topo o botão **Atualizar e reiniciar**.

Como o repositório é privado, cada launcher precisa de uma **chave de leitura** colada em Ajustes. A chave fica só naquele computador, criptografada; nenhuma chave vai dentro do instalador.

### Para lançar uma versão
1. Aba **Actions** > **Lançar versão** > **Run workflow**.
2. Digite a versão nova, sempre maior que a anterior (ex.: `0.3.0`), e o que mudou.
3. Em alguns minutos a versão aparece em Releases e os launchers com chave atualizam sozinhos.

### Criar a chave de leitura (uma vez só)
GitHub > foto > Settings > Developer settings > Personal access tokens > **Fine-grained tokens** > Generate new token. Em Repository access escolha só o `Pulsar_Client`; em Permissions, **Contents: Read-only**. Copie a chave e cole no launcher em Ajustes > Chave de atualização.

## Próximos passos sugeridos

- Animações Clássicas (bloqueio estilo 1.7), que precisa de mais ganchos no jogo
- Motor Pulsar: carregamento de chunks mais rápido e culling de partículas fora da tela
- Cosméticos (Hangar), que precisam de um servidor próprio

## Observações

- Os nomes e a marca são do Pulsar. A coluna "equivale no Lunar" no catálogo é só referência de função; nada da marca ou dos arquivos do Lunar Client é usado.
- O login usa a biblioteca `msmc`, que usa o app de login padrão do Minecraft. Para lançar publicamente, o ideal é registrar um app próprio no Azure e pedir aprovação da Mojang para usar a API do Minecraft.
