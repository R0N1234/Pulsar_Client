# Pulsar Client

Client de PVP para Minecraft 1.8.9 com tema espacial. O projeto tem duas partes:

| Pasta | O que é | Tecnologia |
|---|---|---|
| `launcher/` | O programa que você abre no PC: login Microsoft, tela inicial, aba de mods, ajustes e o botão JOGAR | Electron (Node.js) |
| `mod/` | O que roda dentro do Minecraft: HUD, Teclas Pulse, CPS e os outros mods | Forge 1.8.9 (Java 8) |

O launcher instala o Forge 1.8.9 e o jar do mod numa pasta própria (`%APPDATA%\.pulsar` no Windows), separada da sua `.minecraft`.

## O que já funciona (versão 0.1)

Launcher:
- Tela inicial no estilo Núcleo (pulsar no centro, 1.8.9 e botão JOGAR)
- Aba Mods no estilo Painel, com os 58 mods do catálogo, busca, filtros e prévia da HUD
- Login com conta Microsoft (o token fica salvo criptografado pelo sistema)
- Baixa o Minecraft 1.8.9 e o Forge na primeira vez e abre o jogo
- Ajustes de memória, caminho do Java e pasta do jogo

Mod (6 prontos):
- **Teclas Pulse** (WASD, cliques com CPS e espaço)
- **Frequência CPS** (em Hz)
- **FPS**, **Latência**, **Coordenadas**
- **Propulsão** (correr fixo, tecla R)
- **Editor de HUD**: aperte **Shift direito** no jogo, arraste para mover, botão direito liga ou desliga

Ligar ou desligar um mod no launcher grava em `.pulsar/pulsar/config.json`, o mesmo arquivo que o mod lê ao abrir o jogo. As posições da HUD ficam no mesmo arquivo.

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
   O jar sai em `mod/build/libs/pulsar-client-0.1.0.jar`. A primeira compilação demora porque baixa o Minecraft e o Forge.

2. Abra o launcher:
   ```
   cd launcher
   npm install
   npm start
   ```

3. Entre com a Microsoft, confira em **Ajustes** o caminho do Java 8 (por exemplo `C:\Program Files\Eclipse Adoptium\jre-8.0.432.6-hotspot\bin\javaw.exe`) e clique em **JOGAR**.

Para gerar o instalador do Windows: `npm run dist` dentro de `launcher/` (compile o mod antes, o instalador leva o jar junto).

## Atualizações automáticas

O launcher procura versão nova ao abrir e a cada 30 minutos. Quando acha, baixa em segundo plano e mostra no topo o botão **Atualizar e reiniciar**. Se o jogador fechar o launcher sem clicar, a atualização é instalada na próxima vez que ele abrir.

Os instaladores ficam públicos em [R0N1234/Pulsar_Client-releases](https://github.com/R0N1234/Pulsar_Client-releases/releases). O código continua privado aqui.

### Para lançar uma versão
1. Aba **Actions** > **Lançar versão** > **Run workflow**.
2. Digite a versão nova, sempre maior que a anterior (ex.: `0.3.0`), e o que mudou.
3. Em alguns minutos a versão aparece em Releases e os launchers atualizam sozinhos.

### Configuração (uma vez só)
1. Crie o repositório **público** `Pulsar_Client-releases`, marcando "Add a README".
2. Em GitHub > Settings > Developer settings > Personal access tokens > **Fine-grained tokens**, gere um token com acesso só ao `Pulsar_Client-releases` e permissão **Contents: Read and write**.
3. Neste repositório, em Settings > Secrets and variables > Actions, crie o segredo `RELEASES_TOKEN` com esse token.

## Próximos passos sugeridos

- Mods de combate que faltam: Blindagem, Efeitos, Alcance, Combo, Animações Clássicas, Mira de Pulsar
- Telescópio (zoom) e Órbita Livre (freelook)
- Baixar o Java 8 automaticamente pelo launcher
- Cosméticos (Hangar), que precisam de um servidor próprio

## Observações

- Os nomes e a marca são do Pulsar. A coluna "equivale no Lunar" no catálogo é só referência de função; nada da marca ou dos arquivos do Lunar Client é usado.
- O login usa a biblioteca `msmc`, que usa o app de login padrão do Minecraft. Para lançar publicamente, o ideal é registrar um app próprio no Azure e pedir aprovação da Mojang para usar a API do Minecraft.
