// Idiomas do launcher. O texto é escrito em português no código e aqui fica a tradução de cada frase,
// na ordem [inglês, russo, alemão]. O inglês do Reino Unido usa o mesmo texto com a grafia de lá.
// Frases com {0}, {1}… são modelos: {0} pega qualquer texto (que também é traduzido) e {n0} só números.
// Funciona na janela (window.i18n) e no processo principal (require).
(function (raiz, criar) {
  if (typeof module === "object" && module.exports) module.exports = criar();
  else raiz.i18n = criar();
})(typeof self !== "undefined" ? self : this, function () {
  const IDIOMAS = [
    { id: "en-US", nome: "English", rotulo: "English (US)" },
    { id: "pt-BR", nome: "Português", rotulo: "Português" },
    { id: "ru", nome: "Русский", rotulo: "Русский" },
    { id: "en-GB", nome: "English", rotulo: "English (UK)" },
    { id: "de", nome: "Deutsch", rotulo: "Deutsch" },
  ];
  const COLUNA = { "en-US": 0, "en-GB": 0, ru: 1, de: 2 };

  // Bandeiras desenhadas aqui mesmo (SVG pequeno, sem baixar nada).
  const BANDEIRAS = {
    "en-US":
      '<svg viewBox="0 0 30 20" preserveAspectRatio="none"><rect width="30" height="20" fill="#B22234"/><path d="M0 2.3h30M0 5.4h30M0 8.5h30M0 11.5h30M0 14.6h30M0 17.7h30" stroke="#fff" stroke-width="1.54"/><rect width="13" height="10.8" fill="#3C3B6E"/><g fill="#fff"><circle cx="2" cy="2" r=".6"/><circle cx="5" cy="2" r=".6"/><circle cx="8" cy="2" r=".6"/><circle cx="11" cy="2" r=".6"/><circle cx="3.5" cy="4.2" r=".6"/><circle cx="6.5" cy="4.2" r=".6"/><circle cx="9.5" cy="4.2" r=".6"/><circle cx="2" cy="6.4" r=".6"/><circle cx="5" cy="6.4" r=".6"/><circle cx="8" cy="6.4" r=".6"/><circle cx="11" cy="6.4" r=".6"/><circle cx="3.5" cy="8.6" r=".6"/><circle cx="6.5" cy="8.6" r=".6"/><circle cx="9.5" cy="8.6" r=".6"/></g></svg>',
    "pt-BR":
      '<svg viewBox="0 0 30 20" preserveAspectRatio="none"><rect width="30" height="20" fill="#009C3B"/><path d="M15 2.2 27.6 10 15 17.8 2.4 10z" fill="#FFDF00"/><circle cx="15" cy="10" r="4.6" fill="#002776"/><path d="M10.5 9c3-1 6.6-.5 9 1.3" stroke="#fff" stroke-width=".9" fill="none"/></svg>',
    ru: '<svg viewBox="0 0 30 20" preserveAspectRatio="none"><rect width="30" height="20" fill="#D52B1E"/><rect width="30" height="13.3" fill="#0039A6"/><rect width="30" height="6.7" fill="#fff"/></svg>',
    "en-GB":
      '<svg viewBox="0 0 60 30" preserveAspectRatio="none"><rect width="60" height="30" fill="#012169"/><path d="M0 0 60 30M60 0 0 30" stroke="#fff" stroke-width="6"/><path d="M0 0 60 30M60 0 0 30" stroke="#C8102E" stroke-width="2"/><path d="M30 0v30M0 15h60" stroke="#fff" stroke-width="10"/><path d="M30 0v30M0 15h60" stroke="#C8102E" stroke-width="6"/></svg>',
    de: '<svg viewBox="0 0 30 20" preserveAspectRatio="none"><rect width="30" height="20" fill="#FFCE00"/><rect width="30" height="13.3" fill="#DD0000"/><rect width="30" height="6.7" fill="#000"/></svg>',
  };

  const T = {
    // Novidades da 1.0.0
    "Versão 1.0.0: o Pulsar Client agora tem o código aberto no GitHub, para todo mundo ver como ele funciona.": ["Version 1.0.0: Pulsar Client is now open source on GitHub, so anyone can see how it works.", "Версия 1.0.0: исходный код Pulsar Client теперь открыт на GitHub, любой может посмотреть, как он работает.", "Version 1.0.0: Pulsar Client ist jetzt Open Source auf GitHub, damit jeder sehen kann, wie es funktioniert."],
    "Sequências de Abate só contam quando você elimina o jogador: o seu golpe precisa ser o último antes da morte, e os avisos de abate do chat do servidor (\"Você matou Fulano\", \"Fulano foi morto por você\") também contam. Quem fugiu, saiu de perto ou morreu para outro jogador (assistência) não conta mais.": ["Kill Streaks only count when you take the kill: your hit has to be the last one before the death, and the server's kill messages in chat (\"You killed Someone\", \"Someone was slain by you\") count too. Players who ran away, left your sight or died to someone else (assists) no longer count.", "Серии убийств засчитываются, только когда убиваете вы: ваш удар должен быть последним перед смертью, и сообщения сервера об убийстве в чате («Вы убили Игрока», «Игрок убит вами») тоже засчитываются. Сбежавшие, ушедшие из вида или убитые другим игроком (ассисты) больше не считаются.", "Kill-Serien zählen nur, wenn du den Kill machst: dein Treffer muss der letzte vor dem Tod sein, und die Kill-Meldungen des Servers im Chat (\"Du hast Jemand getötet\", \"Jemand wurde von dir getötet\") zählen auch. Wer weggelaufen ist, außer Sicht ging oder durch jemand anderen starb (Assists), zählt nicht mehr."],
    "Chat inteiro: o jogo guarda todas as mensagens desde que você entrou no servidor, então dá para subir o chat até a primeira, como no Lunar e no Silent.": ["Full chat: the game keeps every message since you joined the server, so you can scroll the chat all the way to the first one, like Lunar and Silent.", "Весь чат: игра хранит все сообщения с момента входа на сервер, так что чат можно прокрутить до самого первого, как в Lunar и Silent.", "Ganzer Chat: das Spiel behält jede Nachricht seit du dem Server beigetreten bist, du kannst also bis zur ersten hochscrollen, wie bei Lunar und Silent."],
    "Menu principal novo: o logo PC animado no anel com órbitas e luas girando, botões com ícones e o universo girando atrás.": ["New main menu: the animated PC logo in a ring with orbits and moons going around, buttons with icons and the universe spinning behind.", "Новое главное меню: анимированный логотип PC в кольце с орбитами и вращающимися лунами, кнопки с иконками и вращающаяся вселенная на фоне.", "Neues Hauptmenü: das animierte PC-Logo im Ring mit Orbits und kreisenden Monden, Knöpfe mit Symbolen und das drehende Universum dahinter."],
    "Tela de carregamento do jogo e tela de atualização no visual novo, com a barra ciano e violeta. A atualização abre no meio da tela e com letras maiores em TV e monitor grande.": ["New look for the game loading screen and the update screen, with the cyan and violet bar. The update opens in the middle of the screen with bigger text on TVs and large monitors.", "Новый вид экрана загрузки игры и экрана обновления с голубо-фиолетовой полосой. Обновление открывается по центру экрана и с более крупным текстом на ТВ и больших мониторах.", "Neuer Look für den Ladebildschirm des Spiels und den Update-Bildschirm, mit dem cyan-violetten Balken. Das Update öffnet sich in der Bildschirmmitte und mit größerer Schrift auf Fernsehern und großen Monitoren."],
    "Console do Pulsar com visual novo, e no início do launcher o pulsar gira com o feixe de luz e as luas em órbita.": ["New look for the Pulsar Console, and on the launcher home the pulsar spins with its light beam and the moons in orbit.", "Новый вид консоли Pulsar, а на главной странице лаунчера пульсар вращается со своим лучом и лунами на орбите.", "Neuer Look für die Pulsar-Konsole, und auf der Startseite des Launchers dreht sich der Pulsar mit seinem Lichtstrahl und den Monden im Orbit."],

    // Novidades da 0.9.5
    "Launcher novo no modo Normal: painel de lançamento à direita, mods em cartões, texturas com prévia e ajustes por abas. Em Ajustes dá para voltar para o modo Lite (a tela leve, com as órbitas) na hora.": ["New launcher in Normal mode: launch panel on the right, mods in cards, textures with preview and tabbed settings. In Settings you can switch back to Lite mode (the light screen with the orbits) right away.", "Новый лаунчер в режиме Normal: панель запуска справа, моды в карточках, текстуры с превью и настройки по вкладкам. В настройках можно сразу вернуться к режиму Lite (лёгкий экран с орбитами).", "Neuer Launcher im Normal-Modus: Startpanel rechts, Mods in Karten, Texturen mit Vorschau und Einstellungen in Tabs. In den Einstellungen kannst du sofort zum Lite-Modus (der leichte Bildschirm mit den Orbits) zurück."],
    "Novo mod Gravação: F9 grava o jogo e F10 salva um clipe dos últimos segundos, como no Lunar. Qualidade, FPS, som do jogo e microfone configuráveis; os vídeos ficam em Vídeos/Pulsar Client, com REC na tela e os botões Abrir e Pasta no chat.": ["New Recording mod: F9 records the game and F10 saves a clip of the last seconds, like in Lunar. Quality, FPS, game sound and microphone are configurable; videos go to Videos/Pulsar Client, with REC on screen and Open and Folder buttons in chat.", "Новый мод Запись: F9 записывает игру, F10 сохраняет клип последних секунд, как в Lunar. Качество, FPS, звук игры и микрофон настраиваются; видео лежат в Видео/Pulsar Client, с REC на экране и кнопками Открыть и Папка в чате.", "Neuer Mod Aufnahme: F9 nimmt das Spiel auf und F10 speichert einen Clip der letzten Sekunden, wie bei Lunar. Qualität, FPS, Spielton und Mikrofon sind einstellbar; Videos landen in Videos/Pulsar Client, mit REC auf dem Bildschirm und den Knöpfen Öffnen und Ordner im Chat."],
    "Sequências de Abate com vozes: pastas ou prefixos (MASCULINO_double_kill.ogg, MASCULINO/double_kill.ogg) viram uma opção Voz no painel, com variações (_2, _3) sorteadas. Botão Testar todos os sons para ouvir a lista inteira e Procurar sons novos na pasta.": ["Kill Streaks with voices: folders or prefixes (MASCULINO_double_kill.ogg, MASCULINO/double_kill.ogg) become a Voice option in the panel, with random variations (_2, _3). A Test all sounds button plays the whole list, and Look for new sounds rescans the folder.", "Серии убийств с голосами: папки или префиксы (MASCULINO_double_kill.ogg, MASCULINO/double_kill.ogg) становятся опцией Голос в панели, с случайными вариациями (_2, _3). Кнопка Проверить все звуки проигрывает весь список, а Найти новые звуки перечитывает папку.", "Kill-Serien mit Stimmen: Ordner oder Präfixe (MASCULINO_double_kill.ogg, MASCULINO/double_kill.ogg) werden zur Option Stimme im Panel, mit zufälligen Varianten (_2, _3). Der Knopf Alle Sounds testen spielt die ganze Liste, und Neue Sounds suchen liest den Ordner neu."],
    "Etiqueta NOVO nos mods novos, no painel do jogo (Shift direito) e no launcher.": ["NEW tag on new mods, in the in-game panel (right Shift) and in the launcher.", "Метка НОВЫЙ на новых модах, в панели игры (правый Shift) и в лаунчере.", "NEU-Etikett an neuen Mods, im Spielpanel (rechte Shift) und im Launcher."],

    // Modo Normal do launcher (modelo completo)
    "Launcher Minecraft": ["Minecraft launcher", "Лаунчер Minecraft", "Minecraft-Launcher"],
    "Sistema Pulsar": ["Pulsar System", "Система Pulsar", "Pulsar-System"],
    "Novidades, sugestões e Discord giram em volta do pulsar. Clique em um planeta para abrir.": ["News, suggestions and Discord orbit the pulsar. Click a planet to open it.", "Новости, предложения и Discord вращаются вокруг пульсара. Нажмите на планету, чтобы открыть.", "Neuigkeiten, Vorschläge und Discord kreisen um den Pulsar. Klick auf einen Planeten, um ihn zu öffnen."],
    "Pulsar Core": ["Pulsar Core", "Ядро Pulsar", "Pulsar Core"],
    Pronto: ["Ready", "Готово", "Bereit"],
    Jogando: ["Playing", "В игре", "Im Spiel"],
    "Jogo aberto": ["Game open", "Игра открыта", "Spiel offen"],
    "Gravando pelo mod Gravação": ["Recording with the Recording mod", "Запись через мод «Запись»", "Aufnahme mit dem Aufnahme-Mod"],
    "Vídeo salvo": ["Video saved", "Видео сохранено", "Video gespeichert"],
    "Novidades do Pulsar": ["Pulsar news", "Новости Pulsar", "Pulsar-Neuigkeiten"],
    "{n0} versões": ["{0} versions", "{0} версий", "{0} Versionen"],
    Entrar: ["Sign in", "Войти", "Anmelden"],
    "conta Microsoft": ["Microsoft account", "аккаунт Microsoft", "Microsoft-Konto"],
    "conta original": ["original account", "оригинальный аккаунт", "Originalkonto"],
    offline: ["offline", "офлайн", "offline"],
    "Sem conta": ["No account", "Нет аккаунта", "Kein Konto"],
    "Entre para jogar": ["Sign in to play", "Войдите, чтобы играть", "Melde dich an, um zu spielen"],
    "Conta Microsoft": ["Microsoft account", "Аккаунт Microsoft", "Microsoft-Konto"],
    "Conta offline": ["Offline account", "Офлайн-аккаунт", "Offline-Konto"],
    "Conta Microsoft em uso": ["Microsoft account in use", "Используется аккаунт Microsoft", "Microsoft-Konto in Benutzung"],
    "Conta offline em uso": ["Offline account in use", "Используется офлайн-аккаунт", "Offline-Konto in Benutzung"],
    "Entre com a sua conta Microsoft para jogar": ["Sign in with your Microsoft account to play", "Войдите в аккаунт Microsoft, чтобы играть", "Melde dich mit deinem Microsoft-Konto an, um zu spielen"],
    "Usar a conta Microsoft": ["Use the Microsoft account", "Использовать аккаунт Microsoft", "Microsoft-Konto verwenden"],
    "Sair da conta": ["Sign out", "Выйти из аккаунта", "Abmelden"],
    "Gerenciar conta": ["Manage account", "Управление аккаунтом", "Konto verwalten"],
    "Conta conectada": ["Account connected", "Аккаунт подключён", "Konto verbunden"],
    Conta: ["Account", "Аккаунт", "Konto"],
    "Maximizar": ["Maximize", "Развернуть", "Maximieren"],
    "Biblioteca orbital": ["Orbital library", "Орбитальная библиотека", "Orbitale Bibliothek"],
    "{n0} ligados": ["{0} on", "{0} вкл.", "{0} an"],
    "Nenhum mod encontrado": ["No mod found", "Моды не найдены", "Kein Mod gefunden"],
    "Tente outro nome ou categoria.": ["Try another name or category.", "Попробуйте другое название или категорию.", "Versuch einen anderen Namen oder eine andere Kategorie."],
    novo: ["new", "новый", "neu"],
    "Minecraft 1.8.9": ["Minecraft 1.8.9", "Minecraft 1.8.9", "Minecraft 1.8.9"],
    "Biblioteca visual": ["Visual library", "Визуальная библиотека", "Visuelle Bibliothek"],
    "Textura padrão em uso": ["Default texture in use", "Используется стандартная текстура", "Standardtextur in Benutzung"],
    "{0} em uso": ["{0} in use", "Используется {0}", "{0} in Benutzung"],
    "Textura padrão": ["Default texture", "Стандартная текстура", "Standardtextur"],
    Todas: ["All", "Все", "Alle"],
    "PvP · mais FPS": ["PvP · more FPS", "PvP · больше FPS", "PvP · mehr FPS"],
    "Qualidade · mais bonito": ["Quality · prettier", "Качество · красивее", "Qualität · schöner"],
    Baixadas: ["Downloaded", "Скачанные", "Heruntergeladen"],
    Baixada: ["Downloaded", "Скачана", "Heruntergeladen"],
    Nuvem: ["Cloud", "Облако", "Cloud"],
    Qualidade: ["Quality", "Качество", "Qualität"],
    "{n0} de {n1} baixadas": ["{0} of {1} downloaded", "{0} из {1} скачано", "{0} von {1} heruntergeladen"],
    "Usar pack": ["Use pack", "Использовать пак", "Pack verwenden"],
    Desligar: ["Turn off", "Выключить", "Ausschalten"],
    "{0} baixada. Clique em Usar pack.": ["{0} downloaded. Click Use pack.", "{0} скачана. Нажмите «Использовать пак».", "{0} heruntergeladen. Klick auf „Pack verwenden“."],
    "Linha do tempo": ["Timeline", "Хронология", "Zeitleiste"],
    "Duas órbitas disponíveis": ["Two orbits available", "Доступны две орбиты", "Zwei Orbits verfügbar"],
    Launcher: ["Launcher", "Лаунчер", "Launcher"],
    "Órbita atual": ["Current orbit", "Текущая орбита", "Aktueller Orbit"],
    "Rota de retorno": ["Return route", "Путь назад", "Rückweg"],
    "Jogando com esta": ["Playing with this one", "Играете на этой", "Du spielst mit dieser"],
    Indisponível: ["Unavailable", "Недоступно", "Nicht verfügbar"],
    "Central de controle": ["Control center", "Центр управления", "Kontrollzentrum"],
    "Preferências locais": ["Local preferences", "Локальные настройки", "Lokale Einstellungen"],
    "Faça o Pulsar funcionar do seu jeito.": ["Make Pulsar work your way.", "Настройте Pulsar под себя.", "Richte Pulsar so ein, wie du willst."],
    "Salvar alterações": ["Save changes", "Сохранить изменения", "Änderungen speichern"],
    Geral: ["General", "Общие", "Allgemein"],
    Visual: ["Visual", "Вид", "Optik"],
    Atualização: ["Update", "Обновление", "Update"],
    "Idioma, visual e comportamento do Pulsar.": ["Language, look and behavior of Pulsar.", "Язык, вид и поведение Pulsar.", "Sprache, Aussehen und Verhalten von Pulsar."],
    "A troca vale na hora, sem precisar salvar.": ["The change applies right away, no need to save.", "Изменение применяется сразу, сохранять не нужно.", "Die Änderung gilt sofort, ohne Speichern."],
    "Visual do launcher": ["Launcher look", "Вид лаунчера", "Launcher-Optik"],
    "Normal é este modelo completo. Lite é a tela leve, com as órbitas.": ["Normal is this full layout. Lite is the light screen with the orbits.", "Normal — это полный вид. Lite — лёгкий экран с орбитами.", "Normal ist dieses komplette Layout. Lite ist der leichte Bildschirm mit den Orbits."],
    "Lite (esta tela leve)": ["Lite (this light screen)", "Lite (этот лёгкий экран)", "Lite (dieser leichte Bildschirm)"],
    "Normal (modelo completo)": ["Normal (full layout)", "Normal (полный вид)", "Normal (komplettes Layout)"],
    "O Normal tem o painel de lançamento à direita, a biblioteca de mods em cartões e os ajustes por abas. A troca vale na hora.": ["Normal has the launch panel on the right, the mod library in cards and tabbed settings. The change applies right away.", "В Normal панель запуска справа, библиотека модов в карточках и настройки по вкладкам. Изменение применяется сразу.", "Normal hat das Startpanel rechts, die Mod-Bibliothek in Karten und Einstellungen in Tabs. Die Änderung gilt sofort."],
    "Abrir o Console do Pulsar ao clicar em Jogar": ["Open the Pulsar Console when clicking Play", "Открывать консоль Pulsar при нажатии «Играть»", "Pulsar-Konsole beim Klick auf Spielen öffnen"],
    "Uma janela própria com o registro do Minecraft, com cores, filtro e botão de copiar.": ["A separate window with the Minecraft log, with colors, a filter and a copy button.", "Отдельное окно с журналом Minecraft: цвета, фильтр и кнопка копирования.", "Ein eigenes Fenster mit dem Minecraft-Protokoll, mit Farben, Filter und Kopierknopf."],
    "Instância do Minecraft": ["Minecraft instance", "Экземпляр Minecraft", "Minecraft-Instanz"],
    "Memória, Java, pasta e OptiFine.": ["Memory, Java, folder and OptiFine.", "Память, Java, папка и OptiFine.", "Speicher, Java, Ordner und OptiFine."],
    "reservados para o Minecraft.": ["reserved for Minecraft.", "выделено для Minecraft.", "für Minecraft reserviert."],
    "A mesma .minecraft do launcher oficial: seus mundos, packs, servidores e opções aparecem no Pulsar.": ["The same .minecraft as the official launcher: your worlds, packs, servers and options show up in Pulsar.", "Та же .minecraft, что и в официальном лаунчере: ваши миры, паки, серверы и настройки появятся в Pulsar.", "Dieselbe .minecraft wie beim offiziellen Launcher: deine Welten, Packs, Server und Optionen erscheinen in Pulsar."],
    "Baixe o": ["Download the", "Скачайте", "Lade das"],
    "no site oficial e escolha o arquivo aqui.": ["from the official site and choose the file here.", "на официальном сайте и выберите файл здесь.", "von der offiziellen Seite herunter und wähle die Datei hier."],
    "Aparência do launcher": ["Launcher appearance", "Внешний вид лаунчера", "Aussehen des Launchers"],
    "Controle brilho, movimento e efeitos. Vale só neste visual Normal.": ["Control glow, motion and effects. Applies only to this Normal look.", "Управляйте свечением, движением и эффектами. Только для вида Normal.", "Steuere Leuchten, Bewegung und Effekte. Gilt nur für diese Normal-Optik."],
    "Intensidade das animações": ["Animation intensity", "Интенсивность анимаций", "Animationsstärke"],
    "de movimento.": ["motion.", "движения.", "Bewegung."],
    "Brilho do núcleo": ["Core glow", "Свечение ядра", "Leuchten des Kerns"],
    "de luminosidade.": ["brightness.", "яркости.", "Helligkeit."],
    "Reduzir efeitos": ["Reduce effects", "Меньше эффектов", "Effekte reduzieren"],
    "Desliga a luz do mouse, a grade e as animações contínuas.": ["Turns off the mouse light, the grid and the continuous animations.", "Отключает свет от мыши, сетку и постоянные анимации.", "Schaltet das Mauslicht, das Raster und die Dauer-Animationen aus."],
    "Restaurar visual": ["Restore look", "Сбросить вид", "Optik zurücksetzen"],
    "Volta a aparência para o padrão do Pulsar.": ["Brings the appearance back to the Pulsar default.", "Возвращает вид к стандарту Pulsar.", "Setzt das Aussehen auf den Pulsar-Standard zurück."],
    Restaurar: ["Restore", "Сбросить", "Zurücksetzen"],
    "A aparência voltou ao padrão do Pulsar.": ["The appearance is back to the Pulsar default.", "Вид возвращён к стандарту Pulsar.", "Das Aussehen ist wieder der Pulsar-Standard."],
    "O launcher baixa sozinho as versões novas do repositório privado.": ["The launcher downloads new versions from the private repository by itself.", "Лаунчер сам скачивает новые версии из приватного репозитория.", "Der Launcher lädt neue Versionen aus dem privaten Repository selbst herunter."],
    "Versão instalada": ["Installed version", "Установленная версия", "Installierte Version"],
    "Procurar atualização": ["Check for update", "Проверить обновление", "Nach Update suchen"],
    "Procurando atualização…": ["Checking for update…", "Проверка обновления…", "Suche nach Update…"],
    "Ajustes salvos": ["Settings saved", "Настройки сохранены", "Einstellungen gespeichert"],
    "Suas preferências foram aplicadas.": ["Your preferences were applied.", "Ваши настройки применены.", "Deine Einstellungen wurden übernommen."],
    "Escolha a versão": ["Choose the version", "Выберите версию", "Version wählen"],
    MODO: ["MODE", "РЕЖИМ", "MODUS"],
    Loadout: ["Loadout", "Набор", "Loadout"],
    "Gerenciar mods": ["Manage mods", "Управление модами", "Mods verwalten"],
    "Registro do jogo": ["Game log", "Журнал игры", "Spielprotokoll"],
    "Transmissão Pulsar": ["Pulsar transmission", "Передача Pulsar", "Pulsar-Übertragung"],
    "Sinal recebido": ["Signal received", "Сигнал получен", "Signal empfangen"],
    Entendido: ["Got it", "Понятно", "Verstanden"],
    "Fechar aviso": ["Close notice", "Закрыть уведомление", "Hinweis schließen"],
    "O núcleo liga o launcher, os mods, as texturas e as suas configurações. Em volta dele giram as novidades, as sugestões e o Discord.": ["The core links the launcher, the mods, the textures and your settings. News, suggestions and Discord orbit around it.", "Ядро связывает лаунчер, моды, текстуры и ваши настройки. Вокруг него вращаются новости, предложения и Discord.", "Der Kern verbindet Launcher, Mods, Texturen und deine Einstellungen. Um ihn kreisen Neuigkeiten, Vorschläge und Discord."],
    "Clique em um planeta para abrir. Na aba Mods você monta o seu loadout; em Texturas troca o visual do jogo; em Versões escolhe a versão do Pulsar.": ["Click a planet to open it. In the Mods tab you build your loadout; in Textures you change the game's look; in Versions you choose the Pulsar version.", "Нажмите на планету, чтобы открыть. На вкладке «Моды» собирается набор; в «Текстурах» меняется вид игры; в «Версиях» выбирается версия Pulsar.", "Klick auf einen Planeten, um ihn zu öffnen. Im Tab Mods baust du dein Loadout; in Texturen änderst du die Optik des Spiels; in Versionen wählst du die Pulsar-Version."],
    "Novidades da atualização": ["What's new in the update", "Что нового в обновлении", "Neues im Update"],
    "Atualização pronta": ["Update ready", "Обновление готово", "Update bereit"],
    // Topo e navegação
    "Início": ["Home", "Главная", "Start"],
    Mods: ["Mods", "Моды", "Mods"],
    Texturas: ["Textures", "Текстуры", "Texturen"],
    Versões: ["Versions", "Версии", "Versionen"],
    "Versões do Pulsar": ["Pulsar versions", "Версии Pulsar", "Pulsar-Versionen"],
    "Versão do Pulsar": ["Pulsar version", "Версия Pulsar", "Pulsar-Version"],
    "Escolha com qual versão jogar. O Pulsar guarda a versão atual e só uma anterior.": [
      "Choose which version to play. Pulsar keeps the current version and just one previous one.",
      "Выберите, на какой версии играть. Pulsar хранит текущую версию и только одну предыдущую.",
      "Wähle, mit welcher Version du spielst. Pulsar behält die aktuelle Version und nur eine vorherige.",
    ],
    "Seus mods ligados, a HUD, as teclas e as texturas são os mesmos nas duas versões. Se algo da versão nova der problema, jogue na anterior até sair a correção.": [
      "Your enabled mods, HUD, keys and textures are the same in both versions. If something in the new version breaks, play the previous one until the fix comes out.",
      "Включённые моды, HUD, клавиши и текстуры одинаковы в обеих версиях. Если в новой версии что-то сломалось, играйте на предыдущей, пока не выйдет исправление.",
      "Deine aktiven Mods, HUD, Tasten und Texturen sind in beiden Versionen gleich. Wenn in der neuen Version etwas nicht klappt, spiel die vorherige, bis die Korrektur kommt.",
    ],
    Atual: ["Current", "Текущая", "Aktuell"],
    Anterior: ["Previous", "Предыдущая", "Vorherige"],
    "A versão mais nova, com tudo o que saiu agora.": ["The newest version, with everything that just came out.", "Самая новая версия со всеми последними изменениями.", "Die neueste Version mit allem, was gerade erschienen ist."],
    "A versão de antes da última atualização, do jeito que ela era.": ["The version from before the last update, exactly as it was.", "Версия до последнего обновления, такая, какой она была.", "Die Version vor dem letzten Update, genau so, wie sie war."],
    "Ainda não tem versão anterior. Ela aparece aqui depois da próxima atualização.": ["There's no previous version yet. It shows up here after the next update.", "Предыдущей версии пока нет. Она появится здесь после следующего обновления.", "Es gibt noch keine vorherige Version. Sie erscheint hier nach dem nächsten Update."],
    "✓ Jogando com esta": ["✓ Playing with this one", "✓ Играете на этой", "✓ Du spielst mit dieser"],
    "Jogar com esta versão": ["Play with this version", "Играть на этой версии", "Mit dieser Version spielen"],
    "Jogando com a versão anterior ({0})": ["Playing with the previous version ({0})", "Игра на предыдущей версии ({0})", "Du spielst mit der vorherigen Version ({0})"],
    "Feche o Minecraft antes de trocar de versão.": ["Close Minecraft before switching versions.", "Закройте Minecraft перед сменой версии.", "Schließe Minecraft, bevor du die Version wechselst."],
    Ajustes: ["Settings", "Настройки", "Einstellungen"],
    "Entrar com Microsoft": ["Sign in with Microsoft", "Войти через Microsoft", "Mit Microsoft anmelden"],
    Minimizar: ["Minimize", "Свернуть", "Minimieren"],
    Fechar: ["Close", "Закрыть", "Schließen"],

    // Início
    "VERSÃO · PVP": ["VERSION · PVP", "ВЕРСИЯ · PVP", "VERSION · PVP"],
    MODO: ["MODE", "РЕЖИМ", "MODUS"],
    "Só OptiFine": ["OptiFine only", "Только OptiFine", "Nur OptiFine"],
    JOGAR: ["PLAY", "ИГРАТЬ", "SPIELEN"],
    "Pronto para jogar": ["Ready to play", "Готово к игре", "Bereit zum Spielen"],
    "mods do Pulsar ligados": ["Pulsar mods on", "модов Pulsar включено", "Pulsar-Mods an"],
    "{n0} de {n1}": ["{0} of {1}", "{0} из {1}", "{0} von {1}"],
    Novidades: ["What's new", "Что нового", "Neuigkeiten"],
    // 0.9: Início no modelo Órbita, versões de PvP e janela de novidades
    "Sistema Pulsar": ["Pulsar System", "Система Pulsar", "Pulsar-System"],
    "Novidades, sugestões e Discord giram em volta do pulsar. Clique em um planeta para abrir.": ["News, suggestions and Discord orbit around the pulsar. Click a planet to open it.", "Новости, предложения и Discord вращаются вокруг пульсара. Нажмите на планету, чтобы открыть.", "Neuigkeiten, Vorschläge und Discord kreisen um den Pulsar. Klicke auf einen Planeten, um ihn zu öffnen."],
    "Órbitas": ["Orbits", "Орбиты", "Umlaufbahnen"],
    "Órbita": ["Orbit", "Орбита", "Umlaufbahn"],
    "Abas abertas": ["Open tabs", "Открытые вкладки", "Offene Tabs"],
    "Fechar aba": ["Close tab", "Закрыть вкладку", "Tab schließen"],
    "Sugestões": ["Suggestions", "Предложения", "Vorschläge"],
    "Discord": ["Discord", "Discord", "Discord"],
    "Escolha a versão": ["Choose the version", "Выберите версию", "Wähle die Version"],
    "Versão do Minecraft": ["Minecraft version", "Версия Minecraft", "Minecraft-Version"],
    "Modo": ["Mode", "Режим", "Modus"],
    "Contas": ["Accounts", "Аккаунты", "Konten"],
    "Entrar": ["Sign in", "Войти", "Anmelden"],
    "conta Microsoft": ["Microsoft account", "аккаунт Microsoft", "Microsoft-Konto"],
    "NOVIDADES DA ATUALIZAÇÃO": ["WHAT'S NEW IN THIS UPDATE", "ЧТО НОВОГО В ОБНОВЛЕНИИ", "NEU IN DIESEM UPDATE"],
    "BORA JOGAR": ["LET'S PLAY", "ИГРАТЬ", "LOS GEHT'S"],
    "Ver todas as novidades": ["See all news", "Все новости", "Alle Neuigkeiten"],
    "Versão {n0}": ["Version {0}", "Версия {0}", "Version {0}"],
    "Minecraft puro": ["Vanilla Minecraft", "Чистый Minecraft", "Vanilla-Minecraft"],
    "Java {n0}": ["Java {0}", "Java {0}", "Java {0}"],
    "Animações 1.7": ["1.7 animations", "Анимации 1.7", "1.7-Animationen"],
    "+ {n0} mods": ["+ {0} mods", "+ {0} модов", "+ {0} Mods"],
    "Minecraft {0} puro: sem os mods do Pulsar.": ["Vanilla Minecraft {0}: without the Pulsar mods.", "Чистый Minecraft {0}: без модов Pulsar.", "Vanilla-Minecraft {0}: ohne die Pulsar-Mods."],
    "Baixando o Java {n0} (só na primeira vez)": ["Downloading Java {0} (first time only)", "Загрузка Java {0} (только в первый раз)", "Java {0} wird geladen (nur beim ersten Mal)"],
    "Instalando o Java {n0}": ["Installing Java {0}", "Установка Java {0}", "Java {0} wird installiert"],
    "RECOMENDADA PVP": ["RECOMMENDED PVP", "РЕКОМЕНДУЕТСЯ ДЛЯ PVP", "EMPFOHLEN FÜR PVP"],
    "PVP CLÁSSICO": ["CLASSIC PVP", "КЛАССИЧЕСКОЕ PVP", "KLASSISCHES PVP"],
    "KITS E CRYSTAL": ["KITS AND CRYSTAL", "КИТЫ И КРИСТАЛЛЫ", "KITS UND CRYSTAL"],
    "PVP MODERNO": ["MODERN PVP", "СОВРЕМЕННОЕ PVP", "MODERNES PVP"],
    "Combate clássico, sem recarga de ataque. Com o Forge, o Motor Pulsar e todos os mods do Pulsar.": ["Classic combat, no attack cooldown. With Forge, the Pulsar Engine and every Pulsar mod.", "Классический бой без перезарядки атаки. С Forge, Движком Pulsar и всеми модами Pulsar.", "Klassischer Kampf ohne Angriffsabklingzeit. Mit Forge, der Pulsar-Engine und allen Pulsar-Mods."],
    "A versão das practice antigas: hit mais seco e o blockhit original. Abre no Minecraft puro, sem os mods do Pulsar.": ["The old practice-server version: snappier hits and the original blockhit. Opens in vanilla Minecraft, without the Pulsar mods.", "Версия старых practice-серверов: резкие удары и оригинальный блокхит. Открывается в чистом Minecraft, без модов Pulsar.", "Die Version der alten Practice-Server: knackigere Hits und das originale Blockhit. Startet in Vanilla-Minecraft, ohne die Pulsar-Mods."],
    "Combate novo com recarga, mundo atual e muitos servidores de kit. Abre no Minecraft puro, sem os mods do Pulsar.": ["New combat with cooldown, a modern world and lots of kit servers. Opens in vanilla Minecraft, without the Pulsar mods.", "Новый бой с перезарядкой, современный мир и много кит-серверов. Открывается в чистом Minecraft, без модов Pulsar.", "Neuer Kampf mit Abklingzeit, moderne Welt und viele Kit-Server. Startet in Vanilla-Minecraft, ohne die Pulsar-Mods."],
    "A versão dos tier tests de espada, machado e crystal. Abre no Minecraft puro, sem os mods do Pulsar. Usa o Java 21 (o Pulsar baixa sozinho).": ["The version used for sword, axe and crystal tier tests. Opens in vanilla Minecraft, without the Pulsar mods. Uses Java 21 (Pulsar downloads it for you).", "Версия для тир-тестов на мечах, топорах и кристаллах. Открывается в чистом Minecraft, без модов Pulsar. Использует Java 21 (Pulsar скачает сам).", "Die Version für Schwert-, Axt- und Crystal-Tiertests. Startet in Vanilla-Minecraft, ohne die Pulsar-Mods. Nutzt Java 21 (Pulsar lädt es selbst)."],
    "Launcher novo no modelo Órbita: Novidades, Sugestões e Discord giram em volta do pulsar e cada item abre a sua própria aba.": ["New launcher in the Orbit design: News, Suggestions and Discord orbit around the pulsar and every item opens in its own tab.", "Новый лаунчер в стиле «Орбита»: Новости, Предложения и Discord вращаются вокруг пульсара, и каждый пункт открывается в своей вкладке.", "Neuer Launcher im Orbit-Design: Neuigkeiten, Vorschläge und Discord kreisen um den Pulsar und jeder Eintrag öffnet sich in einem eigenen Tab."],
    "Ícone de contas no canto de cima, à direita: entre com a Microsoft, troque de conta ou use um nick offline.": ["Accounts icon in the top right corner: sign in with Microsoft, switch accounts or use an offline nick.", "Значок аккаунтов в правом верхнем углу: войдите через Microsoft, смените аккаунт или используйте офлайн-ник.", "Konten-Symbol oben rechts: melde dich mit Microsoft an, wechsle das Konto oder nutze einen Offline-Nick."],
    "Três versões de PvP novas além da 1.8.9: 1.7.10, 1.16.5 e 1.21.8, no Minecraft puro e com o Java certo baixado sozinho.": ["Three new PvP versions besides 1.8.9: 1.7.10, 1.16.5 and 1.21.8, in vanilla Minecraft with the right Java downloaded for you.", "Три новые PvP-версии помимо 1.8.9: 1.7.10, 1.16.5 и 1.21.8, в чистом Minecraft, нужная Java скачивается сама.", "Drei neue PvP-Versionen neben 1.8.9: 1.7.10, 1.16.5 und 1.21.8, in Vanilla-Minecraft, das passende Java wird automatisch geladen."],
    "Janela de novidades: toda vez que o Pulsar atualizar, ela abre no meio da tela com o que mudou.": ["What's new window: every time Pulsar updates, it opens in the middle of the screen with what changed.", "Окно новостей: после каждого обновления Pulsar оно открывается посреди экрана со списком изменений.", "Neuigkeiten-Fenster: Nach jedem Pulsar-Update öffnet es sich in der Bildschirmmitte und zeigt, was sich geändert hat."],
    "Novo mod Anti-Batata: avisa quando a regeneração da maçã dourada vai acabar, com contagem na tela (arrastável) e/ou a tela piscando na cor que você escolher. Pode até comer outra maçã sozinho faltando 1 segundo, só com a espada na mão e batendo.": ["New Anti-Potato mod: warns you when golden apple regeneration is about to end, with an on-screen countdown (draggable) and/or the screen flashing in the colour you choose. It can even eat another apple for you with 1 second left, only while holding a sword and hitting.", "Новый мод «Анти-картошка»: предупреждает, когда заканчивается регенерация золотого яблока, — отсчётом на экране (можно перетаскивать) и/или вспышкой экрана выбранного цвета. Может даже сам съесть ещё одно яблоко за 1 секунду до конца, только с мечом в руке и во время ударов.", "Neuer Mod Anti-Kartoffel: warnt, wenn die Regeneration des goldenen Apfels endet, mit Countdown auf dem Bildschirm (verschiebbar) und/oder einem Aufblitzen in deiner Wunschfarbe. Er kann sogar 1 Sekunde vorher selbst einen neuen Apfel essen, nur mit Schwert in der Hand und beim Schlagen."],
    "Novo mod Filtro de Coleta: escolha os itens de PvP que você pega do chão (botão esquerdo deixa verde, botão direito deixa vermelho). Os vermelhos são descartados na hora, e blocos e minérios sempre são pegos.": ["New Pickup Filter mod: choose which PvP items you pick up from the ground (left click makes it green, right click makes it red). Red ones are dropped right away, and blocks and ores are always picked up.", "Новый мод «Фильтр подбора»: выберите, какие PvP-предметы подбирать с земли (левый клик — зелёный, правый — красный). Красные сразу выбрасываются, а блоки и руды подбираются всегда.", "Neuer Mod Aufsammel-Filter: wähle, welche PvP-Items du vom Boden aufhebst (Linksklick macht sie grün, Rechtsklick rot). Rote werden sofort weggeworfen, Blöcke und Erze werden immer aufgehoben."],
    "Kit PvP: o nome do item sob o mouse não fica mais apagado atrás das sombras do layout no inventário e no baú.": ["Kit PvP: the name of the item under the mouse is no longer faded behind the layout shadows in the inventory and chest.", "Кит PvP: название предмета под курсором больше не тускнеет за тенями раскладки в инвентаре и сундуке.", "Kit PvP: Der Name des Items unter der Maus wird im Inventar und in der Truhe nicht mehr von den Layout-Schatten verdeckt."],
    // 0.9.1: Focar Jogador e Sequências de Abate
    "Novo mod Focar Jogador: ao bater ou apanhar de alguém, aparece um aviso com a cabeça da skin e o nick dele. Aperte a tecla (V) e o jogador fica focado: nick destacado e uma caixa em volta dele quando está à vista. Tecla, cor e o tempo sem ver são configuráveis.": ["New Focus Player mod: when you hit or get hit by someone, a prompt shows their skin head and nick. Press the key (V) and the player becomes focused: highlighted nick and a box around them while in sight. Key, colour and the out-of-sight time are configurable.", "Новый мод Фокус на игроке: когда вы бьёте кого-то или получаете удар, появляется подсказка с головой скина и ником. Нажмите клавишу (V), и игрок окажется в фокусе: выделенный ник и рамка вокруг него, пока он в поле зрения. Клавиша, цвет и время вне поля зрения настраиваются.", "Neuer Mod Spieler fokussieren: Wenn du jemanden triffst oder getroffen wirst, erscheint ein Hinweis mit Skin-Kopf und Nick. Drücke die Taste (V) und der Spieler wird fokussiert: hervorgehobener Nick und ein Rahmen um ihn, solange er in Sicht ist. Taste, Farbe und die Zeit außer Sicht sind einstellbar."],
    "Novo mod Sequências de Abate: FIRST BLOOD, DOUBLE KILL... GODLIKE em letras vermelhas com sangue escorrendo e a voz do Pulsar anunciando. Duas listas (Pulsar e Arena), tempo entre abates, duração, volume e som ligado ou desligado. Suas próprias vozes entram pela pasta pulsar/sons/sequencias.": ["New Kill Streaks mod: FIRST BLOOD, DOUBLE KILL... GODLIKE in red letters with dripping blood and the Pulsar voice announcing. Two lists (Pulsar and Arena), time between kills, duration, volume and sound on or off. Your own voices go in the pulsar/sons/sequencias folder.", "Новый мод Серии убийств: FIRST BLOOD, DOUBLE KILL... GODLIKE красными буквами со стекающей кровью и голосом Pulsar. Два списка (Pulsar и Arena), время между убийствами, длительность, громкость и звук вкл/выкл. Свои голоса кладите в папку pulsar/sons/sequencias.", "Neuer Mod Kill-Serien: FIRST BLOOD, DOUBLE KILL... GODLIKE in roten Buchstaben mit tropfendem Blut und der Pulsar-Stimme. Zwei Listen (Pulsar und Arena), Zeit zwischen Kills, Dauer, Lautstärke und Ton an oder aus. Eigene Stimmen kommen in den Ordner pulsar/sons/sequencias."],
    "Sugerir um mod": ["Suggest a mod", "Предложить мод", "Mod vorschlagen"],
    "Tem uma ideia de mod ou de mudança no Pulsar? Conte o que ele faz e em que momento do PvP ajuda.": ["Got an idea for a mod or a change in Pulsar? Tell us what it does and when it helps in PvP.", "Есть идея мода или изменения в Pulsar? Расскажите, что он делает и чем помогает в PvP.", "Hast du eine Idee für einen Mod oder eine Änderung in Pulsar? Erzähl, was er macht und wann er im PvP hilft."],
    "As sugestões ficam abertas para todo mundo ver e comentar, e as mais pedidas entram nas próximas versões.": ["Suggestions are public for everyone to see and comment on, and the most requested ones make it into the next versions.", "Предложения открыты для всех, их можно комментировать, а самые популярные попадают в следующие версии.", "Vorschläge sind für alle sichtbar und kommentierbar, und die meistgewünschten kommen in die nächsten Versionen."],
    "Escrever sugestão": ["Write a suggestion", "Написать предложение", "Vorschlag schreiben"],
    "Relatar um problema": ["Report a problem", "Сообщить о проблеме", "Problem melden"],
    "Algo deu errado? Conte o que aconteceu e cole o registro do jogo (botão copiar no Registro do jogo ou no Console do Pulsar).": ["Something went wrong? Tell us what happened and paste the game log (copy button in the Game log or in the Pulsar Console).", "Что-то пошло не так? Расскажите, что случилось, и вставьте журнал игры (кнопка «копировать» в журнале игры или в консоли Pulsar).", "Ist etwas schiefgelaufen? Erzähl, was passiert ist, und füge das Spielprotokoll ein (Kopieren-Button im Spielprotokoll oder in der Pulsar-Konsole)."],
    "Com o registro dá para achar o problema bem mais rápido.": ["With the log we can find the problem much faster.", "С журналом проблему можно найти гораздо быстрее.", "Mit dem Protokoll finden wir das Problem viel schneller."],
    "Relatar problema": ["Report problem", "Сообщить о проблеме", "Problem melden"],
    "Abrir o console": ["Open the console", "Открыть консоль", "Konsole öffnen"],
    "Ideias da galera": ["Community ideas", "Идеи игроков", "Ideen der Community"],
    "Veja o que os outros jogadores já pediram, comente e apoie as ideias que você também quer no Pulsar.": ["See what other players have asked for, comment and support the ideas you also want in Pulsar.", "Посмотрите, что уже просили другие игроки, комментируйте и поддерживайте идеи, которые хотите видеть в Pulsar.", "Sieh dir an, was andere Spieler sich gewünscht haben, kommentiere und unterstütze die Ideen, die du auch in Pulsar willst."],
    "Ver sugestões": ["See suggestions", "Смотреть предложения", "Vorschläge ansehen"],
    "Pelo jogo": ["In game", "В игре", "Im Spiel"],
    "Sugestões dentro do jogo": ["Suggestions in game", "Предложения из игры", "Vorschläge im Spiel"],
    "No menu do jogo (Esc) tem o botão Sugestões: ele abre a mesma página sem precisar sair do Minecraft.": ["The game menu (Esc) has a Suggestions button: it opens the same page without leaving Minecraft.", "В меню игры (Esc) есть кнопка «Предложения»: она открывает ту же страницу, не выходя из Minecraft.", "Im Spielmenü (Esc) gibt es den Button Vorschläge: Er öffnet dieselbe Seite, ohne Minecraft zu verlassen."],
    "Servidor do Pulsar": ["Pulsar server", "Сервер Pulsar", "Pulsar-Server"],
    "Servidor do Pulsar no Discord": ["Pulsar server on Discord", "Сервер Pulsar в Discord", "Pulsar-Server auf Discord"],
    "Entre no Discord do Pulsar para achar gente para treinar, ver as novidades antes de todo mundo e falar direto com quem faz o client.": ["Join the Pulsar Discord to find people to practise with, see the news before everyone else and talk directly to the people who make the client.", "Заходите в Discord Pulsar, чтобы найти с кем тренироваться, узнавать новости раньше всех и общаться напрямую с создателями клиента.", "Komm auf den Pulsar-Discord, um Trainingspartner zu finden, Neuigkeiten vor allen anderen zu sehen und direkt mit den Machern des Clients zu reden."],
    "Entrar no Discord": ["Join the Discord", "Войти в Discord", "Discord beitreten"],
    "Ajuda": ["Help", "Помощь", "Hilfe"],
    "Ajuda no Discord": ["Help on Discord", "Помощь в Discord", "Hilfe auf Discord"],
    "Travou, ficou sem som ou o jogo não abre? Mande a sua dúvida no Discord junto com o registro do jogo.": ["Game froze, no sound or won't open? Post your question on Discord along with the game log.", "Игра зависла, нет звука или не запускается? Напишите вопрос в Discord вместе с журналом игры.", "Spiel hängt, kein Ton oder startet nicht? Stell deine Frage auf Discord zusammen mit dem Spielprotokoll."],
    "Copie o registro no Console do Pulsar e cole na mensagem.": ["Copy the log in the Pulsar Console and paste it into your message.", "Скопируйте журнал в консоли Pulsar и вставьте в сообщение.", "Kopiere das Protokoll in der Pulsar-Konsole und füge es in deine Nachricht ein."],
    "Pedir ajuda": ["Ask for help", "Попросить помощи", "Um Hilfe bitten"],
    "Com o Discord aberto, o seu perfil mostra que você está jogando com o Pulsar, a versão do Minecraft e onde você está (menu, sozinho ou o servidor).": ["With Discord open, your profile shows that you're playing with Pulsar, the Minecraft version and where you are (menu, singleplayer or the server).", "Когда Discord открыт, ваш профиль показывает, что вы играете с Pulsar, версию Minecraft и где вы находитесь (меню, одиночная игра или сервер).", "Wenn Discord offen ist, zeigt dein Profil, dass du mit Pulsar spielst, die Minecraft-Version und wo du bist (Menü, Einzelspieler oder der Server)."],
    "Dá para desligar na aba Mods, no mod Sinal Discord.": ["You can turn it off in the Mods tab, in the Discord Signal mod.", "Это можно отключить на вкладке «Моды», в моде «Сигнал Discord».", "Du kannst es im Tab Mods im Mod Discord-Signal ausschalten."],
    "Abrir Mods": ["Open Mods", "Открыть моды", "Mods öffnen"],
    "Versão 0.8.6:": ["Version 0.8.6:", "Версия 0.8.6:", "Version 0.8.6:"],
    "Novo mod Kit PvP: monte no painel onde fica cada item do seu inventário e, no baú, Shift + botão direito leva cada item direto para o lugar (vale o tipo e o minério, não o encantamento).": [
      "New Kit PvP mod: set up in the panel where each item goes in your inventory, and in a chest Shift + right click sends each item straight to its place (by type and material, not enchantment).",
      "Новый мод «Кит PvP»: настройте в панели, где лежит каждый предмет инвентаря, а в сундуке Shift + правый клик отправит предмет прямо на его место (по типу и материалу, без учёта зачарований).",
      "Neuer Mod Kit PvP: lege im Panel fest, wo jedes Item im Inventar liegt, und in der Truhe bringt Shift + Rechtsklick jedes Item direkt an seinen Platz (nach Typ und Material, nicht nach Verzauberung).",
    ],
    "Versão 0.8.5:": ["Version 0.8.5:", "Версия 0.8.5:", "Version 0.8.5:"],
    "Cosméticos do Pulsar: a Órbita Pulsar (acessório) e a Supernova (emote) são grátis, e os outros chegam em breve. Aperte B no jogo para os emotes. O Telescópio virou um zoom igual ao do OptiFine (segure C). Mais FPS: a fonte e o Motor Pulsar param de esperar a placa de vídeo a cada quadro.": [
      "Pulsar cosmetics: the Pulsar Orbit (accessory) and Supernova (emote) are free, and the others are coming soon. Press B in game for emotes. The Telescope is now an OptiFine-style zoom (hold C). More FPS: the font and Pulsar Engine no longer wait on the graphics card every frame.",
      "Косметика Pulsar: «Орбита Pulsar» (аксессуар) и «Сверхновая» (эмоция) бесплатны, остальное скоро. Нажмите B в игре для эмоций. Телескоп теперь — зум как в OptiFine (удерживайте C). Больше FPS: шрифт и Движок Pulsar больше не ждут видеокарту каждый кадр.",
      "Pulsar-Kosmetik: der Pulsar-Orbit (Accessoire) und die Supernova (Emote) sind kostenlos, der Rest kommt bald. Drücke B im Spiel für Emotes. Das Teleskop ist jetzt ein Zoom wie bei OptiFine (C gedrückt halten). Mehr FPS: Schrift und Pulsar-Motor warten nicht mehr jedes Bild auf die Grafikkarte.",
    ],
    "Versão 0.8.4:": ["Version 0.8.4:", "Версия 0.8.4:", "Version 0.8.4:"],
    "Som no Windows com o driver de áudio novo (o mesmo do Minecraft atual), para quem ficava sem som com alguns fones e placas de som.": [
      "Sound on Windows with the new audio driver (the same as current Minecraft), for anyone who had no sound with some headsets and sound cards.",
      "Звук в Windows с новым аудиодрайвером (как в современном Minecraft) — для тех, у кого не было звука с некоторыми наушниками и звуковыми картами.",
      "Ton unter Windows mit dem neuen Audiotreiber (wie im aktuellen Minecraft), für alle, die mit manchen Headsets und Soundkarten keinen Ton hatten.",
    ],
    "Versão 0.8.3:": ["Version 0.8.3:", "Версия 0.8.3:", "Version 0.8.3:"],
    "Textos compridos do painel agora quebram em duas linhas e passam sozinhos com o mouse em cima. A Barra Pulsar lê a música do Spotify pelos controles de mídia do Windows, sem login. Novo botão Sugestões no menu do jogo. O W-Tap e Blockhit saiu do client e virou um mod separado.": [
      "Long texts in the panel now wrap into two lines and scroll when you hover them. The Pulsar Bar reads your Spotify song from the Windows media controls, no login needed. New Suggestions button in the game menu. W-Tap and Blockhit left the client and became a separate mod.",
      "Длинные тексты в панели теперь переносятся на две строки и прокручиваются при наведении мыши. Панель Pulsar читает песню из Spotify через медиа-управление Windows, без входа. Новая кнопка «Предложения» в меню игры. W-Tap и Blockhit убраны из клиента и стали отдельным модом.",
      "Lange Texte im Panel brechen jetzt in zwei Zeilen um und laufen durch, wenn die Maus darauf ist. Die Pulsar-Leiste liest deinen Spotify-Song über die Mediensteuerung von Windows, ohne Login. Neuer Vorschläge-Knopf im Spielmenü. W-Tap und Blockhit sind aus dem Client raus und jetzt ein eigener Mod.",
    ],
    "Versão 0.8.0:": ["Version 0.8.0:", "Версия 0.8.0:", "Version 0.8.0:"],
    "Barra Pulsar no topo da tela com FPS, ms, hora e a música do Spotify (com capa). O fundo do menu virou um universo 360° girando, com Andrômeda, os planetas e um pulsar. Painel do Shift direito e opções dos mods com visual novo, ícones e favoritos. No Motor Pulsar, com muita gente perto, só os mais próximos ficam com detalhe completo e os de longe ficam só com a skin. Novos mods: Pacote Ativo, Barra de Chefe e Aura de Encanto.": [
      "Pulsar Bar at the top of the screen with FPS, ms, time and your Spotify song (with cover). The menu background is now a slowly turning 360° universe with Andromeda, the planets and a pulsar. New look for the Right Shift panel and mod options, with icons and favorites. In the Pulsar Engine, with a crowd nearby, only the closest players keep full detail and far ones keep just the skin. New mods: Active Pack, Boss Bar and Enchant Aura.",
      "Панель Pulsar сверху экрана: FPS, пинг, время и песня из Spotify (с обложкой). Фон меню теперь — медленно вращающаяся вселенная 360° с Андромедой, планетами и пульсаром. Новый вид панели на правом Shift и настроек модов, с иконками и избранным. В Двигателе Pulsar при толпе рядом полную детализацию сохраняют только ближайшие игроки, а дальние остаются только со скином. Новые моды: Активный пак, Полоса босса и Аура зачарования.",
      "Pulsar-Leiste oben im Bild mit FPS, ms, Uhrzeit und deinem Spotify-Song (mit Cover). Der Menühintergrund ist jetzt ein langsam drehendes 360°-Universum mit Andromeda, den Planeten und einem Pulsar. Neues Aussehen für das Rechts-Shift-Panel und die Mod-Optionen, mit Symbolen und Favoriten. Im Pulsar-Motor behalten bei vielen Spielern in der Nähe nur die nächsten alle Details, entfernte nur den Skin. Neue Mods: Aktives Paket, Bossleiste und Verzauberungsaura.",
    ],
    "Registro do jogo": ["Game log", "Журнал игры", "Spielprotokoll"],
    console: ["console", "консоль", "Konsole"],
    "sugestões": ["suggestions", "предложения", "Vorschläge"],
    copiar: ["copy", "копировать", "kopieren"],
    copiado: ["copied", "скопировано", "kopiert"],
    "não deu para copiar": ["couldn't copy", "не удалось скопировать", "Kopieren fehlgeschlagen"],
    "Os logs do Minecraft aparecem aqui quando o jogo abrir.": [
      "Minecraft logs show up here when the game opens.",
      "Логи Minecraft появятся здесь, когда игра запустится.",
      "Die Minecraft-Logs erscheinen hier, sobald das Spiel startet.",
    ],

    // Mods
    "Mods · 1.8.9 PVP": ["Mods · 1.8.9 PVP", "Моды · 1.8.9 PVP", "Mods · 1.8.9 PVP"],
    "Mods do Pulsar": ["Pulsar mods", "Моды Pulsar", "Pulsar-Mods"],
    "Seus mods": ["Your mods", "Ваши моды", "Deine Mods"],
    "+ Adicionar mods": ["+ Add mods", "+ Добавить моды", "+ Mods hinzufügen"],
    "Abrir pasta": ["Open folder", "Открыть папку", "Ordner öffnen"],
    "Os mods do": ["Mods for", "Моды для", "Mods für"],
    "rodam junto com o Pulsar, para jogar solo ou com amigos em servidor privado. Em servidores grandes, use só mods permitidos.": [
      "run alongside Pulsar, for solo play or with friends on a private server. On big servers, only use allowed mods.",
      "работают вместе с Pulsar: для одиночной игры или с друзьями на приватном сервере. На больших серверах используйте только разрешённые моды.",
      "laufen zusammen mit Pulsar, für Solo-Spiele oder mit Freunden auf einem privaten Server. Auf großen Servern nur erlaubte Mods verwenden.",
    ],
    "Buscar mod…": ["Search mods…", "Поиск модов…", "Mod suchen…"],
    "só os prontos": ["ready only", "только готовые", "nur fertige"],
    "Prévia da HUD": ["HUD preview", "Предпросмотр HUD", "HUD-Vorschau"],
    "[Correndo (fixo)]": ["[Sprinting (toggled)]", "[Бег (закреплён)]", "[Sprinten (fest)]"],
    "No jogo, aperte Shift direito para arrastar cada elemento.": [
      "In game, press Right Shift to drag each element.",
      "В игре нажмите правый Shift, чтобы перетаскивать элементы.",
      "Drücke im Spiel die rechte Umschalttaste, um jedes Element zu verschieben.",
    ],
    "{n0} mods ligados. {n1} prontos e {n2} a caminho.": [
      "{0} mods on. {1} ready and {2} on the way.",
      "Включено модов: {0}. Готово: {1}, в пути: {2}.",
      "{0} Mods an. {1} fertig und {2} unterwegs.",
    ],
    Todos: ["All", "Все", "Alle"],
    pronto: ["ready", "готов", "fertig"],
    "em breve": ["soon", "скоро", "bald"],
    "Desligar {0}": ["Turn off {0}", "Выключить {0}", "{0} ausschalten"],
    "Ligar {0}": ["Turn on {0}", "Включить {0}", "{0} einschalten"],
    "Nenhum mod seu ainda.": ["No mods of yours yet.", "Пока нет ваших модов.", "Noch keine eigenen Mods."],
    "Clique em Adicionar mods ou arraste os arquivos .jar para cá.": [
      "Click Add mods or drag .jar files here.",
      "Нажмите «Добавить моды» или перетащите сюда файлы .jar.",
      "Klicke auf Mods hinzufügen oder zieh .jar-Dateien hierher.",
    ],
    remover: ["remove", "удалить", "entfernen"],
    "versão {0}": ["version {0}", "версия {0}", "Version {0}"],
    "{0} foi para a Lixeira.": ["{0} was moved to the Recycle Bin.", "{0} перемещён в корзину.", "{0} wurde in den Papierkorb verschoben."],
    "Não entraram: {0}": ["Not added: {0}", "Не добавлены: {0}", "Nicht hinzugefügt: {0}"],
    "Mods adicionados. Eles carregam na próxima vez que o jogo abrir.": [
      "Mods added. They load the next time the game opens.",
      "Моды добавлены. Они загрузятся при следующем запуске игры.",
      "Mods hinzugefügt. Sie werden beim nächsten Spielstart geladen.",
    ],
    "Mod não encontrado.": ["Mod not found.", "Мод не найден.", "Mod nicht gefunden."],
    "Feche o Minecraft antes de mudar os mods.": ["Close Minecraft before changing mods.", "Закройте Minecraft перед изменением модов.", "Schließe Minecraft, bevor du Mods änderst."],
    "Mod do Fabric: não funciona no Forge 1.8.9.": ["Fabric mod: doesn't work on Forge 1.8.9.", "Мод для Fabric: не работает на Forge 1.8.9.", "Fabric-Mod: funktioniert nicht mit Forge 1.8.9."],
    "Mod de uma versão nova do Minecraft: não funciona na 1.8.9.": [
      "Mod for a newer Minecraft version: doesn't work on 1.8.9.",
      "Мод для новой версии Minecraft: не работает на 1.8.9.",
      "Mod für eine neuere Minecraft-Version: funktioniert nicht mit 1.8.9.",
    ],
    "Arquivo danificado ou que não é um mod.": ["Damaged file, or not a mod.", "Файл повреждён или это не мод.", "Beschädigte Datei oder kein Mod."],
    "Feito para o Minecraft {0}, não para o 1.8.9.": ["Made for Minecraft {0}, not 1.8.9.", "Сделан для Minecraft {0}, а не для 1.8.9.", "Für Minecraft {0} gemacht, nicht für 1.8.9."],
    "{0}: {1}": ["{0}: {1}", "{0}: {1}", "{0}: {1}"],

    // Categorias
    Combate: ["Combat", "Бой", "Kampf"],
    HUD: ["HUD", "HUD", "HUD"],
    Visual: ["Visual", "Визуал", "Optik"],
    Servidores: ["Servers", "Серверы", "Server"],
    Social: ["Social", "Общение", "Sozial"],
    Desempenho: ["Performance", "Производительность", "Leistung"],

    // Nomes dos mods
    "Teclas Pulse": ["Pulse Keys", "Клавиши Pulse", "Pulse-Tasten"],
    "Frequência CPS": ["CPS Frequency", "Частота CPS", "CPS-Frequenz"],
    Alcance: ["Reach", "Дальность", "Reichweite"],
    Combo: ["Combo", "Комбо", "Combo"],
    "Animações Clássicas": ["Classic Animations", "Классические анимации", "Klassische Animationen"],
    "Mira de Pulsar": ["Pulsar Crosshair", "Прицел Pulsar", "Pulsar-Fadenkreuz"],
    Impacto: ["Impact", "Удар", "Treffer"],
    "Campo de Colisão": ["Collision Field", "Поле столкновений", "Kollisionsfeld"],
    Blindagem: ["Armor", "Броня", "Rüstung"],
    Efeitos: ["Effects", "Эффекты", "Effekte"],
    "Inventário": ["Inventory", "Инвентарь", "Inventar"],
    "Contador de Itens": ["Item Counter", "Счётчик предметов", "Item-Zähler"],
    "Propulsão": ["Propulsion", "Ускорение", "Antrieb"],
    Recargas: ["Cooldowns", "Перезарядки", "Abklingzeiten"],
    "Poções Brilhantes": ["Shiny Potions", "Сияющие зелья", "Glänzende Tränke"],
    "Tremor de Dano": ["Damage Shake", "Тряска от урона", "Schadenswackeln"],
    "Contador TNT": ["TNT Timer", "Таймер TNT", "TNT-Timer"],
    "Latência": ["Latency", "Задержка", "Latenz"],
    Coordenadas: ["Coordinates", "Координаты", "Koordinaten"],
    "Bússola": ["Compass", "Компас", "Kompass"],
    "Relógio": ["Clock", "Часы", "Uhr"],
    "Cronômetro": ["Stopwatch", "Секундомер", "Stoppuhr"],
    "Memória": ["Memory", "Память", "Speicher"],
    Servidor: ["Server", "Сервер", "Server"],
    Placar: ["Scoreboard", "Табло", "Anzeigetafel"],
    "Barra de Chefe": ["Boss Bar", "Полоса босса", "Bossleiste"],
    "Pacote Ativo": ["Active Pack", "Активный пак", "Aktives Paket"],
    "Saturação": ["Saturation", "Насыщение", "Sättigung"],
    "Lista do TAB": ["TAB List", "Список TAB", "TAB-Liste"],
    Etiquetas: ["Nametags", "Неймтеги", "Namensschilder"],
    "Fonte do Pulsar": ["Pulsar Font", "Шрифт Pulsar", "Pulsar-Schrift"],
    "Cores Vivas": ["Vivid Colors", "Яркие цвета", "Lebendige Farben"],
    "Telescópio": ["Telescope", "Телескоп", "Teleskop"],
    "Órbita Livre": ["Free Orbit", "Свободная орбита", "Freier Orbit"],
    "Rastro de Cometa": ["Comet Trail", "Хвост кометы", "Kometenschweif"],
    "Meu Nick": ["My Nickname", "Мой ник", "Mein Name"],
    Supernova: ["Supernova", "Сверхновая", "Supernova"],
    Contorno: ["Outline", "Контур", "Umriss"],
    "Aura de Encanto": ["Enchant Aura", "Аура зачарования", "Verzauberungsaura"],
    "Poeira Estelar": ["Stardust", "Звёздная пыль", "Sternenstaub"],
    Atmosfera: ["Atmosphere", "Атмосфера", "Atmosphäre"],
    "Gravidade de Itens": ["Item Gravity", "Гравитация предметов", "Item-Schwerkraft"],
    Nebulosa: ["Nebula", "Туманность", "Nebel"],
    "Carta Estelar": ["Star Chart", "Звёздная карта", "Sternkarte"],
    "Constelações": ["Constellations", "Созвездия", "Sternbilder"],
    "GG Automático": ["Auto GG", "Авто GG", "Auto-GG"],
    "Atalhos de Texto": ["Text Shortcuts", "Быстрые фразы", "Text-Kürzel"],
    "Nível": ["Level", "Уровень", "Level"],
    "Depois do F2, a print já vai copiada para colar no Discord, com botões no chat": [
      "After F2, the screenshot is already copied to paste on Discord, with buttons in chat",
      "После F2 скриншот уже скопирован, чтобы вставить в Discord, а в чате появляются кнопки",
      "Nach F2 ist der Screenshot schon kopiert, um ihn in Discord einzufügen, mit Buttons im Chat",
    ],
    "Ícone Pulsar": ["Pulsar Icon", "Значок Pulsar", "Pulsar-Symbol"],
    "Salto Rápido": ["Quick Jump", "Быстрый переход", "Schnellsprung"],
    "Modo Anônimo": ["Anonymous Mode", "Анонимный режим", "Anonymer Modus"],
    Hangar: ["Hangar", "Ангар", "Hangar"],
    Emotes: ["Emotes", "Эмоции", "Emotes"],
    Amigos: ["Friends", "Друзья", "Freunde"],
    "Sinal Discord": ["Discord Signal", "Сигнал Discord", "Discord-Signal"],
    "Rádio Pulsar": ["Pulsar Radio", "Радио Pulsar", "Pulsar-Radio"],
    Replay: ["Replay", "Повтор", "Replay"],
    Capturas: ["Captures", "Скриншоты", "Aufnahmen"],
    "Motor Pulsar": ["Pulsar Engine", "Двигатель Pulsar", "Pulsar-Motor"],
    "Organizador de Packs": ["Pack Organizer", "Органайзер паков", "Pack-Organizer"],

    // Descrições dos mods
    "Mostra WASD, cliques e pulo na tela": ["Shows WASD, clicks and jump on screen", "Показывает WASD, клики и прыжок на экране", "Zeigt WASD, Klicks und Sprung auf dem Bildschirm"],
    "Cliques por segundo, mostrados em Hz": ["Clicks per second, shown in Hz", "Клики в секунду, в герцах", "Klicks pro Sekunde, in Hz angezeigt"],
    "Distância de cada hit que você acerta": ["Distance of every hit you land", "Дистанция каждого вашего удара", "Distanz jedes Treffers, den du landest"],
    "Conta quantos hits seguidos você deu": ["Counts how many hits in a row you landed", "Считает удары подряд", "Zählt, wie viele Treffer du in Folge landest"],
    "Bloqueio e swing no estilo da 1.7": ["1.7-style blocking and swing", "Блок и взмах в стиле 1.7", "Blocken und Schwingen im Stil von 1.7"],
    "Mira com formato, cor, tamanho e contorno": ["Crosshair with shape, color, size and outline", "Прицел с формой, цветом, размером и контуром", "Fadenkreuz mit Form, Farbe, Größe und Umriss"],
    "Muda a cor do inimigo quando ele leva dano": ["Changes the enemy's color when they take damage", "Меняет цвет противника, когда он получает урон", "Ändert die Farbe des Gegners, wenn er Schaden nimmt"],
    "Mostra a hitbox de jogadores e projéteis": ["Shows the hitbox of players and projectiles", "Показывает хитбоксы игроков и снарядов", "Zeigt die Hitbox von Spielern und Geschossen"],
    "Durabilidade da armadura e do item na mão": ["Durability of your armor and held item", "Прочность брони и предмета в руке", "Haltbarkeit der Rüstung und des Items in der Hand"],
    "Poções ativas com tempo restante": ["Active potions with time left", "Активные зелья и оставшееся время", "Aktive Tränke mit Restzeit"],
    "Protege a espada e outros itens contra drop, trava slots (tecla padrão L) e centraliza o inventário": [
      "Protects your sword and other items from dropping, locks slots (default key L) and centers the inventory",
      "Защищает меч и другие предметы от выбрасывания, блокирует слоты (клавиша L по умолчанию) и центрирует инвентарь",
      "Schützt Schwert und andere Items vor dem Fallenlassen, sperrt Slots (Standardtaste L) und zentriert das Inventar",
    ],
    "Flechas, pérolas, maçãs e poções no inventário": ["Arrows, pearls, apples and potions in your inventory", "Стрелы, жемчуг, яблоки и зелья в инвентаре", "Pfeile, Perlen, Äpfel und Tränke im Inventar"],
    "Correr fica ligado sem segurar a tecla (tecla padrão R para fixo ou manual)": [
      "Sprint stays on without holding the key (default key R for toggled or manual)",
      "Бег включён без удержания клавиши (клавиша R по умолчанию: закреплённый или ручной)",
      "Sprinten bleibt an, ohne die Taste zu halten (Standardtaste R für fest oder manuell)",
    ],
    "Tempo de espera de pérolas e maçãs": ["Cooldown for pearls and apples", "Перезарядка жемчуга и яблок", "Abklingzeit von Perlen und Äpfeln"],
    "Brilho de encantamento nas poções": ["Enchantment glint on potions", "Блеск зачарования на зельях", "Verzauberungsglanz auf Tränken"],
    "Tira o tremor da câmera ao levar dano": ["Removes camera shake when taking damage", "Убирает тряску камеры при получении урона", "Entfernt das Kamerawackeln bei Schaden"],
    "Tempo até a TNT explodir": ["Time until TNT explodes", "Время до взрыва TNT", "Zeit bis zur TNT-Explosion"],
    "Quadros por segundo": ["Frames per second", "Кадры в секунду", "Bilder pro Sekunde"],
    "Seu ping no servidor": ["Your ping on the server", "Ваш пинг на сервере", "Dein Ping auf dem Server"],
    "Posição X, Y, Z e direção": ["X, Y, Z position and direction", "Позиция X, Y, Z и направление", "X-, Y-, Z-Position und Richtung"],
    "Direção para onde você está olhando": ["The direction you're looking at", "Направление взгляда", "Die Richtung, in die du schaust"],
    "Hora do seu computador": ["Your computer's time", "Время на вашем компьютере", "Die Uhrzeit deines Computers"],
    "Marca tempo de partidas e treinos": ["Times matches and practice", "Засекает время матчей и тренировок", "Misst die Zeit von Matches und Training"],
    "Uso de RAM do jogo": ["Game RAM usage", "Использование RAM игрой", "RAM-Nutzung des Spiels"],
    "Endereço do servidor atual": ["Current server address", "Адрес текущего сервера", "Adresse des aktuellen Servers"],
    "Move o placar do servidor, muda o tamanho e tira os números vermelhos": [
      "Moves the server scoreboard, changes its size and hides the red numbers",
      "Перемещает табло сервера, меняет размер и убирает красные числа",
      "Verschiebt die Anzeigetafel, ändert die Größe und entfernt die roten Zahlen",
    ],
    "Ajusta a barra de boss": ["Adjusts the boss bar", "Настраивает полосу босса", "Passt die Bossleiste an"],
    "Barra de boss arredondada, com cor e porcentagem": ["Rounded boss bar with color and percentage", "Скруглённая полоса босса с цветом и процентами", "Abgerundete Bossleiste mit Farbe und Prozent"],
    "Barra Pulsar": ["Pulsar Bar", "Панель Pulsar", "Pulsar-Leiste"],
    "Mini painel no topo com FPS, ms, hora e a música do Spotify": ["Mini panel at the top with FPS, ms, time and your Spotify song", "Мини-панель сверху: FPS, пинг, время и песня из Spotify", "Mini-Leiste oben mit FPS, ms, Uhrzeit und deinem Spotify-Song"],
    "Mostra o resource pack em uso": ["Shows the resource pack in use", "Показывает текущий ресурспак", "Zeigt das aktive Ressourcenpaket"],
    "Mostra a saturação da fome": ["Shows hunger saturation", "Показывает насыщение", "Zeigt die Hunger-Sättigung"],
    "Jogadores do TAB com a cabeça da skin e o ping em número": [
      "TAB players with their skin head and ping as a number",
      "Игроки в TAB с головой скина и пингом числом",
      "TAB-Spieler mit Skin-Kopf und Ping als Zahl",
    ],
    "Ajusta nomes acima dos jogadores": ["Adjusts names above players", "Настраивает ники над игроками", "Passt Namen über Spielern an"],
    "Chat com histórico, filtros e mensagens empilhadas": ["Chat with history, filters and stacked messages", "Чат с историей, фильтрами и сгруппированными сообщениями", "Chat mit Verlauf, Filtern und gestapelten Nachrichten"],
    "Troca a fonte de todo o jogo pela fonte do Pulsar (ou volta a do Minecraft)": [
      "Swaps the whole game's font for the Pulsar font (or back to Minecraft's)",
      "Меняет шрифт всей игры на шрифт Pulsar (или возвращает шрифт Minecraft)",
      "Ersetzt die Schrift im ganzen Spiel durch die Pulsar-Schrift (oder zurück zur Minecraft-Schrift)",
    ],
    "Contraste e saturação para deixar o jogo mais vivo": ["Contrast and saturation for a more vivid game", "Контраст и насыщенность для более яркой игры", "Kontrast und Sättigung für ein lebendigeres Spiel"],
    "Monte o inventário do PvP e pegue do baú com Shift + botão direito, cada item no seu lugar": [
      "Set up your PvP inventory and take items from chests with Shift + right click, each in its place",
      "Настройте инвентарь для PvP и берите вещи из сундука через Shift + правый клик — каждую на своё место",
      "Richte dein PvP-Inventar ein und nimm Items aus Truhen mit Shift + Rechtsklick, jedes an seinen Platz",
    ],
    "Zoom igual ao do OptiFine segurando uma tecla (padrão C)": [
      "OptiFine-style zoom while holding a key (default C)",
      "Зум как в OptiFine при удержании клавиши (по умолчанию C)",
      "Zoom wie bei OptiFine beim Halten einer Taste (Standard C)",
    ],
    "Segurando uma tecla (padrão Alt), a câmera gira 360° em volta do personagem (Perspective)": [
      "While holding a key (default Alt), the camera spins 360° around your character (Perspective)",
      "При удержании клавиши (по умолчанию Alt) камера вращается на 360° вокруг персонажа (Perspective)",
      "Beim Halten einer Taste (Standard Alt) dreht sich die Kamera 360° um deine Figur (Perspective)",
    ],
    "Desfoque de movimento: a imagem deixa um rastro quando você vira": [
      "Motion blur: the image leaves a trail when you turn",
      "Размытие в движении: изображение оставляет след при повороте",
      "Bewegungsunschärfe: das Bild zieht beim Drehen einen Schweif",
    ],
    "Mostra o seu nick em cima do personagem no F5": ["Shows your nickname above your character in F5", "Показывает ваш ник над персонажем в F5", "Zeigt deinen Namen über deiner Figur in F5"],
    "Brilho total, enxerga no escuro": ["Full brightness, see in the dark", "Полная яркость, видно в темноте", "Volle Helligkeit, sieh im Dunkeln"],
    "Cor e espessura do contorno do bloco": ["Block outline color and thickness", "Цвет и толщина контура блока", "Farbe und Dicke des Blockumrisses"],
    "Muda a cor do brilho de encantamento": ["Changes the enchantment glint color", "Меняет цвет блеска зачарования", "Ändert die Farbe des Verzauberungsglanzes"],
    "Muda a cor do brilho de encantamento (até arco-íris)": ["Changes the enchantment glint color (even rainbow)", "Меняет цвет блеска зачарования (даже радуга)", "Ändert die Farbe des Verzauberungsglanzes (sogar Regenbogen)"],
    "Mais partículas a cada hit que você acerta": ["More particles on every hit you land", "Больше частиц при каждом ударе", "Mehr Partikel bei jedem Treffer"],
    "Tira a chuva e fixa a hora do dia só para você": ["Removes rain and locks the time of day just for you", "Убирает дождь и фиксирует время суток только для вас", "Entfernt Regen und fixiert die Tageszeit nur für dich"],
    "Itens caem deitados no chão": ["Items lie flat on the ground", "Предметы лежат плашмя на земле", "Items liegen flach auf dem Boden"],
    "Desfoque no fundo dos menus": ["Blur behind menus", "Размытие фона в меню", "Unschärfe hinter Menüs"],
    "Minimapa no canto da tela": ["Minimap in the corner of the screen", "Мини-карта в углу экрана", "Minikarte in der Bildschirmecke"],
    "Marcadores de lugares no mundo": ["Waypoints in the world", "Метки мест в мире", "Wegpunkte in der Welt"],
    "Manda GG no fim da partida": ["Sends GG at the end of the match", "Пишет GG в конце матча", "Sendet GG am Ende des Matches"],
    "Uma tecla manda um comando ou frase no servidor (ex.: /lobby)": [
      "A key sends a command or phrase on the server (e.g. /lobby)",
      "Клавиша отправляет команду или фразу на сервер (напр.: /lobby)",
      "Eine Taste sendet einen Befehl oder Satz auf dem Server (z. B. /lobby)",
    ],
    "Mostra o nível do jogador no nome": ["Shows the player's level next to their name", "Показывает уровень игрока рядом с ником", "Zeigt das Level des Spielers am Namen"],
    "Ícone PC no nome de quem usa o Pulsar, em cima da cabeça e no TAB": ["PC icon next to the name of everyone using Pulsar, above their head and in TAB", "Значок ПК рядом с ником тех, кто играет с Pulsar: над головой и в TAB", "PC-Symbol am Namen aller, die Pulsar nutzen, über dem Kopf und in der TAB-Liste"],
    "Entra em modos de jogo com um clique": ["Joins game modes with one click", "Вход в режимы игры в один клик", "Betritt Spielmodi mit einem Klick"],
    "Esconde seu nick para gravar ou fazer live": ["Hides your nickname for recording or streaming", "Скрывает ваш ник для записи или стрима", "Versteckt deinen Namen für Aufnahmen oder Streams"],
    "Capas, asas, chapéus e outros cosméticos": ["Capes, wings, hats and other cosmetics", "Плащи, крылья, шляпы и другая косметика", "Umhänge, Flügel, Hüte und andere Kosmetik"],
    "Animações para o personagem": ["Animations for your character", "Анимации для персонажа", "Animationen für deine Figur"],
    "Lista de amigos e chat dentro do client": ["Friends list and chat inside the client", "Список друзей и чат внутри клиента", "Freundesliste und Chat im Client"],
    "Mostra no seu perfil do Discord que você está jogando com o Pulsar e em qual servidor": [
      "Shows on your Discord profile that you're playing with Pulsar and on which server",
      "Показывает в профиле Discord, что вы играете с Pulsar и на каком сервере",
      "Zeigt in deinem Discord-Profil, dass du mit Pulsar spielst und auf welchem Server",
    ],
    "Rádios da internet tocando enquanto você joga: liga, troca de estação e muda o volume no jogo": [
      "Internet radio while you play: turn it on, switch stations and change the volume in game",
      "Интернет-радио во время игры: включайте, меняйте станции и громкость прямо в игре",
      "Internetradio beim Spielen: im Spiel einschalten, Sender wechseln und Lautstärke ändern",
    ],
    "Grava partidas para rever depois": ["Records matches to watch later", "Записывает матчи для просмотра", "Nimmt Matches zum späteren Ansehen auf"],
    "Salva e envia prints com um clique": ["Saves and shares screenshots with one click", "Сохраняет и отправляет скриншоты в один клик", "Speichert und teilt Screenshots mit einem Klick"],
    "Otimizador do Pulsar: perfil Desempenho de fábrica, jogo mais limpo e mais FPS": [
      "Pulsar's optimizer: Performance profile by default, a cleaner game and more FPS",
      "Оптимизатор Pulsar: профиль «Производительность» по умолчанию, чище картинка и больше FPS",
      "Optimierer von Pulsar: standardmäßig Leistungsprofil, aufgeräumteres Spiel und mehr FPS",
    ],
    "Pastas e busca para resource packs": ["Folders and search for resource packs", "Папки и поиск для ресурспаков", "Ordner und Suche für Ressourcenpakete"],

    // Texturas
    "Texturas · 1.8.9": ["Textures · 1.8.9", "Текстуры · 1.8.9", "Texturen · 1.8.9"],
    "Escolhidas pelo Pulsar. Baixe uma vez e troque quando quiser, aqui ou na aba Texturas dentro do jogo.": [
      "Picked by Pulsar. Download once and switch whenever you like, here or in the Textures tab in game.",
      "Подобраны Pulsar. Скачайте один раз и меняйте когда угодно: здесь или во вкладке «Текстуры» в игре.",
      "Von Pulsar ausgewählt. Einmal herunterladen und jederzeit wechseln, hier oder im Tab Texturen im Spiel.",
    ],
    "Abrir pasta de texturas": ["Open textures folder", "Открыть папку текстур", "Texturordner öffnen"],
    "mais FPS": ["more FPS", "больше FPS", "mehr FPS"],
    Qualidade: ["Quality", "Качество", "Qualität"],
    "mais bonito": ["better looking", "красивее", "schöner"],
    "Em uso": ["In use", "Используется", "Aktiv"],
    "Clique para desligar": ["Click to turn off", "Нажмите, чтобы выключить", "Klicken zum Ausschalten"],
    "página": ["page", "страница", "Seite"],
    Usar: ["Use", "Включить", "Nutzen"],
    apagar: ["delete", "удалить", "löschen"],
    Baixar: ["Download", "Скачать", "Herunterladen"],
    "{0} desligada.": ["{0} turned off.", "{0}: выключена.", "{0} ausgeschaltet."],
    "{0} apagada.": ["{0} deleted.", "{0}: удалена.", "{0} gelöscht."],
    "Usando {0}. Ela aparece na próxima vez que o jogo abrir.": [
      "Using {0}. It shows up the next time the game opens.",
      "Выбрана {0}. Она появится при следующем запуске игры.",
      "{0} wird genutzt. Sie erscheint beim nächsten Spielstart.",
    ],
    "{0} baixada. Clique em Usar.": ["{0} downloaded. Click Use.", "{0}: скачана. Нажмите «Включить».", "{0} heruntergeladen. Klicke auf Nutzen."],
    "Não deu para baixar {0}: {1}": ["Couldn't download {0}: {1}", "Не удалось скачать {0}: {1}", "{0} konnte nicht heruntergeladen werden: {1}"],
    "Pacote desconhecido.": ["Unknown pack.", "Неизвестный пак.", "Unbekanntes Paket."],
    "O Modrinth respondeu {0}.": ["Modrinth answered {0}.", "Modrinth ответил {0}.", "Modrinth antwortete {0}."],
    "O arquivo chegou diferente do esperado. Tente de novo.": [
      "The file arrived different than expected. Try again.",
      "Файл пришёл не таким, как ожидалось. Попробуйте ещё раз.",
      "Die Datei kam anders an als erwartet. Versuch es noch einmal.",
    ],
    "O jogo está aberto: troque a textura pela aba Texturas dentro do jogo.": [
      "The game is open: switch textures in the Textures tab inside the game.",
      "Игра запущена: меняйте текстуры во вкладке «Текстуры» в игре.",
      "Das Spiel ist offen: Wechsle die Textur im Tab Texturen im Spiel.",
    ],
    "Feche o Minecraft antes de apagar uma textura.": ["Close Minecraft before deleting a texture.", "Закройте Minecraft перед удалением текстуры.", "Schließe Minecraft, bevor du eine Textur löschst."],
    "Baixe o pacote antes de usar.": ["Download the pack before using it.", "Скачайте пак перед использованием.", "Lade das Paket vor der Nutzung herunter."],
    "8x8: as texturas mais leves que existem, para o máximo de FPS": [
      "8x8: the lightest textures there are, for maximum FPS",
      "8x8: самые лёгкие текстуры для максимального FPS",
      "8x8: die leichtesten Texturen überhaupt, für maximale FPS",
    ],
    "Lisa e simples, no estilo dos trailers do Minecraft": ["Smooth and simple, in the style of the Minecraft trailers", "Гладкие и простые, в стиле трейлеров Minecraft", "Glatt und schlicht, im Stil der Minecraft-Trailer"],
    "Parecida com a original, com itens limpos para PvP": ["Close to vanilla, with clean items for PvP", "Похожи на оригинал, с чистыми предметами для PvP", "Nah am Original, mit klaren Items für PvP"],
    "Clara e limpa, com interface e itens repaginados": ["Bright and clean, with a refreshed interface and items", "Светлые и чистые, с обновлённым интерфейсом и предметами", "Hell und klar, mit überarbeiteter Oberfläche und Items"],
    "Cores frias e visual limpo para PvP": ["Cool colors and a clean look for PvP", "Холодные цвета и чистый вид для PvP", "Kühle Farben und klare Optik für PvP"],
    "A textura original com o dobro de resolução": ["The vanilla textures at double resolution", "Оригинальные текстуры в двойном разрешении", "Die Originaltexturen in doppelter Auflösung"],
    "As texturas novas do Minecraft trazidas para a 1.8.9": ["The new Minecraft textures brought to 1.8.9", "Новые текстуры Minecraft для 1.8.9", "Die neuen Minecraft-Texturen für 1.8.9"],
    "No estilo Faithful, com mais detalhes": ["Faithful style, with more detail", "В стиле Faithful, с большей детализацией", "Im Faithful-Stil, mit mehr Details"],
    "Faithful em 64x, bem detalhada": ["Faithful at 64x, highly detailed", "Faithful в 64x, очень детальные", "Faithful in 64x, sehr detailliert"],
    "Alta definição em 128x, para PC mais forte": ["High definition at 128x, for stronger PCs", "Высокая чёткость 128x для мощных ПК", "Hohe Auflösung in 128x, für stärkere PCs"],

    // Ajustes
    Idioma: ["Language", "Язык", "Sprache"],
    "Memória para o jogo": ["Game memory", "Память для игры", "Arbeitsspeicher für das Spiel"],
    "{n0} GB": ["{0} GB", "{0} ГБ", "{0} GB"],
    "Nenhum arquivo escolhido": ["No file chosen", "Файл не выбран", "Keine Datei ausgewählt"],
    "Escolher arquivo": ["Choose file", "Выбрать файл", "Datei auswählen"],
    "O OptiFine não pode ser distribuído junto com o Pulsar, então baixe o": [
      "OptiFine can't be shipped with Pulsar, so download",
      "OptiFine нельзя распространять вместе с Pulsar, поэтому скачайте",
      "OptiFine darf nicht mit Pulsar ausgeliefert werden, also lade",
    ],
    "no site oficial e escolha o arquivo aqui.": [
      "from the official site and choose the file here.",
      "с официального сайта и выберите файл здесь.",
      "von der offiziellen Seite herunter und wähle die Datei hier aus.",
    ],
    "Abrir optifine.net": ["Open optifine.net", "Открыть optifine.net", "optifine.net öffnen"],
    "Automático: o Pulsar baixa o Java 8 sozinho": ["Automatic: Pulsar downloads Java 8 for you", "Автоматически: Pulsar сам скачает Java 8", "Automatisch: Pulsar lädt Java 8 selbst herunter"],
    "Deixe vazio para usar o Java 8 do Pulsar. Para usar outro, cole o caminho do javaw.exe de um Java 8.": [
      "Leave empty to use Pulsar's Java 8. To use another one, paste the path to a Java 8 javaw.exe.",
      "Оставьте пустым, чтобы использовать Java 8 от Pulsar. Чтобы выбрать другую, вставьте путь к javaw.exe из Java 8.",
      "Leer lassen, um das Java 8 von Pulsar zu nutzen. Für ein anderes den Pfad zur javaw.exe eines Java 8 einfügen.",
    ],
    "Pasta do Minecraft": ["Minecraft folder", "Папка Minecraft", "Minecraft-Ordner"],
    "A mesma .minecraft do launcher oficial: seus mundos, packs, servidores e opções aparecem no Pulsar. O Pulsar tem uma pasta de mods só dele, então mods de outras versões não atrapalham.": [
      "The same .minecraft as the official launcher: your worlds, packs, servers and options show up in Pulsar. Pulsar has its own mods folder, so mods for other versions don't get in the way.",
      "Та же папка .minecraft, что и у официального лаунчера: ваши миры, паки, серверы и настройки появятся в Pulsar. У Pulsar своя папка модов, поэтому моды для других версий не мешают.",
      "Derselbe .minecraft-Ordner wie beim offiziellen Launcher: Deine Welten, Packs, Server und Optionen erscheinen in Pulsar. Pulsar hat einen eigenen Mod-Ordner, Mods anderer Versionen stören also nicht.",
    ],
    "Abrir o Console do Pulsar ao clicar em Jogar": ["Open the Pulsar Console when clicking Play", "Открывать консоль Pulsar при нажатии «Играть»", "Pulsar-Konsole beim Klick auf Spielen öffnen"],
    "Uma janela própria com o registro do Minecraft, com cores, filtro e botão de copiar. Ajuda a ver o que aconteceu se o jogo der erro.": [
      "A separate window with the Minecraft log, with colors, a filter and a copy button. Helps you see what happened if the game hits an error.",
      "Отдельное окно с журналом Minecraft: цвета, фильтр и кнопка копирования. Помогает понять, что случилось, если игра выдала ошибку.",
      "Ein eigenes Fenster mit dem Minecraft-Protokoll, mit Farben, Filter und Kopier-Knopf. Hilft zu sehen, was passiert ist, wenn das Spiel einen Fehler hat.",
    ],
    "Chave de atualização": ["Update key", "Ключ обновлений", "Update-Schlüssel"],
    "Cole a chave do GitHub (github_pat_…)": ["Paste the GitHub key (github_pat_…)", "Вставьте ключ GitHub (github_pat_…)", "GitHub-Schlüssel einfügen (github_pat_…)"],
    "Salvar chave": ["Save key", "Сохранить ключ", "Schlüssel speichern"],
    "Com a chave, o launcher baixa sozinho as versões novas do repositório privado. Ela fica guardada só neste computador.": [
      "With the key, the launcher downloads new versions from the private repository by itself. It's only stored on this computer.",
      "С ключом лаунчер сам скачивает новые версии из приватного репозитория. Ключ хранится только на этом компьютере.",
      "Mit dem Schlüssel lädt der Launcher neue Versionen aus dem privaten Repository selbst herunter. Er wird nur auf diesem Computer gespeichert.",
    ],
    "Chave salva. Cole outra para trocar.": ["Key saved. Paste another to replace it.", "Ключ сохранён. Вставьте другой, чтобы заменить.", "Schlüssel gespeichert. Füge einen anderen ein, um ihn zu ersetzen."],
    "Chave salva. Procurando atualização…": ["Key saved. Checking for updates…", "Ключ сохранён. Ищем обновление…", "Schlüssel gespeichert. Suche nach Updates…"],
    "Chave removida.": ["Key removed.", "Ключ удалён.", "Schlüssel entfernt."],
    Salvar: ["Save", "Сохранить", "Speichern"],
    "Salvo.": ["Saved.", "Сохранено.", "Gespeichert."],
    "Escolha o arquivo do OptiFine em Ajustes para usar esse modo.": [
      "Choose the OptiFine file in Settings to use this mode.",
      "Выберите файл OptiFine в настройках, чтобы использовать этот режим.",
      "Wähle die OptiFine-Datei in den Einstellungen, um diesen Modus zu nutzen.",
    ],
    "Só OptiFine: mais leve, mas sem os mods do Pulsar.": ["OptiFine only: lighter, but without Pulsar's mods.", "Только OptiFine: легче, но без модов Pulsar.", "Nur OptiFine: leichter, aber ohne die Pulsar-Mods."],
    "Escolha o OptiFine 1.8.9": ["Choose OptiFine 1.8.9", "Выберите OptiFine 1.8.9", "OptiFine 1.8.9 auswählen"],
    "Escolha mods Forge para o Minecraft 1.8.9": ["Choose Forge mods for Minecraft 1.8.9", "Выберите моды Forge для Minecraft 1.8.9", "Forge-Mods für Minecraft 1.8.9 auswählen"],

    // Contas
    "Sem conta": ["No account", "Нет аккаунта", "Kein Konto"],
    "● offline": ["● offline", "● офлайн", "● offline"],
    "● original": ["● premium", "● лицензия", "● Premium"],
    "entre para jogar": ["sign in to play", "войдите, чтобы играть", "zum Spielen anmelden"],
    "Conta original": ["Premium account", "Лицензионный аккаунт", "Premium-Konto"],
    "em uso": ["in use", "используется", "aktiv"],
    usar: ["use", "выбрать", "nutzen"],
    sair: ["sign out", "выйти", "abmelden"],
    "Contas offline": ["Offline accounts", "Офлайн-аккаунты", "Offline-Konten"],
    "Entre uma vez com uma conta Microsoft que tenha o Minecraft para liberar os nicks offline.": [
      "Sign in once with a Microsoft account that owns Minecraft to unlock offline nicknames.",
      "Войдите один раз через аккаунт Microsoft с купленным Minecraft, чтобы открыть офлайн-ники.",
      "Melde dich einmal mit einem Microsoft-Konto an, das Minecraft besitzt, um Offline-Namen freizuschalten.",
    ],
    "Apagar {0}": ["Delete {0}", "Удалить {0}", "{0} löschen"],
    "Novo nick": ["New nickname", "Новый ник", "Neuer Name"],
    Adicionar: ["Add", "Добавить", "Hinzufügen"],
    "Para servidores que aceitam conta offline (pirata), LAN e jogar sem internet. Dentro do jogo dá para trocar pelo botão da conta no menu.": [
      "For servers that accept offline (cracked) accounts, LAN and playing without internet. In game you can switch with the account button in the menu.",
      "Для серверов с офлайн-аккаунтами (пиратка), LAN и игры без интернета. В игре аккаунт меняется кнопкой в меню.",
      "Für Server, die Offline-Konten (cracked) erlauben, LAN und Spielen ohne Internet. Im Spiel wechselst du über den Konto-Knopf im Menü.",
    ],
    "Entre com sua conta Microsoft para jogar": ["Sign in with your Microsoft account to play", "Войдите через аккаунт Microsoft, чтобы играть", "Melde dich mit deinem Microsoft-Konto an, um zu spielen"],
    "Abrindo o login da Microsoft…": ["Opening Microsoft sign-in…", "Открываем вход Microsoft…", "Microsoft-Anmeldung wird geöffnet…"],
    "Não deu para entrar: {0}": ["Couldn't sign in: {0}", "Не удалось войти: {0}", "Anmeldung fehlgeschlagen: {0}"],
    "Conectando sua conta…": ["Connecting your account…", "Подключаем аккаунт…", "Konto wird verbunden…"],
    "Entre com a conta Microsoft no launcher primeiro.": ["Sign in with the Microsoft account in the launcher first.", "Сначала войдите в аккаунт Microsoft в лаунчере.", "Melde dich zuerst im Launcher mit dem Microsoft-Konto an."],
    "Entre com sua conta Microsoft antes de jogar.": ["Sign in with your Microsoft account before playing.", "Войдите через аккаунт Microsoft перед игрой.", "Melde dich vor dem Spielen mit deinem Microsoft-Konto an."],
    "Entre uma vez com uma conta Microsoft que tenha o Minecraft para liberar as contas offline.": [
      "Sign in once with a Microsoft account that owns Minecraft to unlock offline accounts.",
      "Войдите один раз через аккаунт Microsoft с купленным Minecraft, чтобы открыть офлайн-аккаунты.",
      "Melde dich einmal mit einem Microsoft-Konto an, das Minecraft besitzt, um Offline-Konten freizuschalten.",
    ],
    "Essa conta offline não existe.": ["That offline account doesn't exist.", "Такого офлайн-аккаунта нет.", "Dieses Offline-Konto gibt es nicht."],
    "O nick precisa ter de 3 a 16 letras, números ou _ (sem espaço nem acento).": [
      "The nickname needs 3 to 16 letters, numbers or _ (no spaces or accents).",
      "Ник должен содержать от 3 до 16 латинских букв, цифр или _ (без пробелов).",
      "Der Name braucht 3 bis 16 Buchstaben, Zahlen oder _ (ohne Leerzeichen oder Akzente).",
    ],
    "Dá para guardar até {n0} nicks.": ["You can keep up to {0} nicknames.", "Можно сохранить до {0} ников.", "Du kannst bis zu {0} Namen speichern."],
    "Aconteceu um erro desconhecido": ["An unknown error happened", "Произошла неизвестная ошибка", "Ein unbekannter Fehler ist aufgetreten"],
    "Erro desconhecido": ["Unknown error", "Неизвестная ошибка", "Unbekannter Fehler"],
    "Não foi possível entrar": ["Couldn't sign in", "Не удалось войти", "Anmeldung nicht möglich"],
    "Não foi possível entrar na conta Microsoft": ["Couldn't sign in to the Microsoft account", "Не удалось войти в аккаунт Microsoft", "Anmeldung beim Microsoft-Konto nicht möglich"],
    "Não foi possível entrar no Xbox Live": ["Couldn't sign in to Xbox Live", "Не удалось войти в Xbox Live", "Anmeldung bei Xbox Live nicht möglich"],
    "Essa conta Microsoft ainda não tem perfil Xbox. Entre uma vez em xbox.com e tente de novo": [
      "This Microsoft account has no Xbox profile yet. Sign in once at xbox.com and try again",
      "У этого аккаунта Microsoft ещё нет профиля Xbox. Войдите один раз на xbox.com и попробуйте снова",
      "Dieses Microsoft-Konto hat noch kein Xbox-Profil. Melde dich einmal auf xbox.com an und versuch es erneut",
    ],
    "O Xbox Live não está disponível no país dessa conta": ["Xbox Live isn't available in this account's country", "Xbox Live недоступен в стране этого аккаунта", "Xbox Live ist im Land dieses Kontos nicht verfügbar"],
    "Conta de menor de 18 anos: ela precisa estar num grupo familiar Microsoft com um adulto": [
      "Account of someone under 18: it needs to be in a Microsoft family group with an adult",
      "Аккаунт несовершеннолетнего: он должен быть в семейной группе Microsoft со взрослым",
      "Konto einer Person unter 18: Es muss in einer Microsoft-Familiengruppe mit einem Erwachsenen sein",
    ],
    "Essa conta não tem o Minecraft Java. Compre o jogo ou entre com outra conta": [
      "This account doesn't own Minecraft Java. Buy the game or sign in with another account",
      "На этом аккаунте нет Minecraft Java. Купите игру или войдите в другой аккаунт",
      "Dieses Konto besitzt kein Minecraft Java. Kauf das Spiel oder melde dich mit einem anderen Konto an",
    ],
    "A janela de login foi fechada": ["The sign-in window was closed", "Окно входа закрыто", "Das Anmeldefenster wurde geschlossen"],

    // Jogo abrindo (mensagens que vêm do launcher)
    "Preparando o Forge 1.8.9": ["Preparing Forge 1.8.9", "Подготовка Forge 1.8.9", "Forge 1.8.9 wird vorbereitet"],
    "Instalando o Pulsar": ["Installing Pulsar", "Установка Pulsar", "Pulsar wird installiert"],
    "Preparando o OptiFine": ["Preparing OptiFine", "Подготовка OptiFine", "OptiFine wird vorbereitet"],
    "Baixando {0}": ["Downloading {0}", "Загрузка: {0}", "Lade {0} herunter"],
    "Verificando os arquivos do Minecraft": ["Checking Minecraft files", "Проверка файлов Minecraft", "Minecraft-Dateien werden geprüft"],
    Jogando: ["Playing", "В игре", "Im Spiel"],
    "sons e texturas": ["sounds and textures", "звуки и текстуры", "Sounds und Texturen"],
    "arquivos do sistema": ["system files", "системные файлы", "Systemdateien"],
    bibliotecas: ["libraries", "библиотеки", "Bibliotheken"],
    "bibliotecas do Forge": ["Forge libraries", "библиотеки Forge", "Forge-Bibliotheken"],
    "Baixando arquivos do Minecraft": ["Downloading Minecraft files", "Загрузка файлов Minecraft", "Minecraft-Dateien werden geladen"],
    "Arquivos baixados": ["Files downloaded", "Файлы загружены", "Dateien heruntergeladen"],
    "Bibliotecas prontas": ["Libraries ready", "Библиотеки готовы", "Bibliotheken bereit"],
    "Preparando o Forge": ["Preparing Forge", "Подготовка Forge", "Forge wird vorbereitet"],
    "Abrindo o Minecraft": ["Opening Minecraft", "Запуск Minecraft", "Minecraft wird gestartet"],
    "Baixando o Java 8 (só na primeira vez)": ["Downloading Java 8 (first time only)", "Загрузка Java 8 (только в первый раз)", "Java 8 wird geladen (nur beim ersten Mal)"],
    "Instalando o Java 8": ["Installing Java 8", "Установка Java 8", "Java 8 wird installiert"],
    "O jogo fechou com erro (código {0})": ["The game closed with an error (code {0})", "Игра закрылась с ошибкой (код {0})", "Das Spiel wurde mit einem Fehler beendet (Code {0})"],
    "O Minecraft já está aberto.": ["Minecraft is already open.", "Minecraft уже запущен.", "Minecraft ist schon geöffnet."],
    "Feche o Minecraft antes de atualizar.": ["Close Minecraft before updating.", "Закройте Minecraft перед обновлением.", "Schließe Minecraft vor dem Update."],
    "O Java escolhido em Ajustes não existe: {0}": ["The Java chosen in Settings doesn't exist: {0}", "Java, выбранная в настройках, не найдена: {0}", "Das in den Einstellungen gewählte Java existiert nicht: {0}"],
    "Não consegui baixar o Java 8 (HTTP {0}). Escolha um Java 8 em Ajustes.": [
      "Couldn't download Java 8 (HTTP {0}). Choose a Java 8 in Settings.",
      "Не удалось скачать Java 8 (HTTP {0}). Выберите Java 8 в настройках.",
      "Java 8 konnte nicht geladen werden (HTTP {0}). Wähle ein Java 8 in den Einstellungen.",
    ],
    "O Java 8 foi baixado, mas não encontrei o executável. Escolha um Java 8 em Ajustes.": [
      "Java 8 was downloaded, but the executable wasn't found. Choose a Java 8 in Settings.",
      "Java 8 скачана, но исполняемый файл не найден. Выберите Java 8 в настройках.",
      "Java 8 wurde geladen, aber die ausführbare Datei fehlt. Wähle ein Java 8 in den Einstellungen.",
    ],
    "Não consegui baixar {0} (HTTP {1})": ["Couldn't download {0} (HTTP {1})", "Не удалось скачать {0} (HTTP {1})", "{0} konnte nicht geladen werden (HTTP {1})"],
    "Escolha o arquivo do OptiFine 1.8.9 em Ajustes antes de jogar nesse modo.": [
      "Choose the OptiFine 1.8.9 file in Settings before playing in this mode.",
      "Выберите файл OptiFine 1.8.9 в настройках перед игрой в этом режиме.",
      "Wähle vor dem Spielen in diesem Modus die OptiFine-1.8.9-Datei in den Einstellungen.",
    ],
    "O arquivo do OptiFine precisa ser da versão 1.8.9 (por exemplo OptiFine_1.8.9_HD_U_M5.jar).": [
      "The OptiFine file must be version 1.8.9 (for example OptiFine_1.8.9_HD_U_M5.jar).",
      "Файл OptiFine должен быть версии 1.8.9 (например, OptiFine_1.8.9_HD_U_M5.jar).",
      "Die OptiFine-Datei muss Version 1.8.9 sein (zum Beispiel OptiFine_1.8.9_HD_U_M5.jar).",
    ],
    "O preparo do OptiFine falhou (código {0}).": ["OptiFine setup failed (code {0}).", "Подготовка OptiFine не удалась (код {0}).", "Die OptiFine-Vorbereitung ist fehlgeschlagen (Code {0})."],
    "O Minecraft não abriu. Veja o registro do jogo para o motivo.": [
      "Minecraft didn't open. Check the game log for the reason.",
      "Minecraft не запустился. Причина есть в журнале игры.",
      "Minecraft ist nicht gestartet. Den Grund findest du im Spielprotokoll.",
    ],

    // Sinal Discord (aparece no perfil do Discord)
    "No menu principal": ["In the main menu", "В главном меню", "Im Hauptmenü"],
    "Jogando sozinho": ["Playing solo", "Играет в одиночку", "Spielt solo"],
    "Jogando em {0}": ["Playing on {0}", "Играет на {0}", "Spielt auf {0}"],
    "Jogando em um servidor": ["Playing on a server", "Играет на сервере", "Spielt auf einem Server"],

    // Atualização
    "Baixando atualização{0}… {n1}%": ["Downloading update{0}… {1}%", "Скачиваем обновление{0}… {1}%", "Update wird geladen{0}… {1}%"],
    "Atualizar para {0}": ["Update to {0}", "Обновить до {0}", "Auf {0} aktualisieren"],
    "ATUALIZAÇÃO PRONTA": ["UPDATE READY", "ОБНОВЛЕНИЕ ГОТОВО", "UPDATE BEREIT"],
    "ATUALIZAR AGORA": ["UPDATE NOW", "ОБНОВИТЬ СЕЙЧАС", "JETZT AKTUALISIEREN"],
    Depois: ["Later", "Позже", "Später"],
    "O Pulsar fecha, atualiza e abre de novo sozinho em alguns segundos.": [
      "Pulsar closes, updates and reopens by itself in a few seconds.",
      "Pulsar закроется, обновится и сам откроется снова через несколько секунд.",
      "Pulsar schließt sich, aktualisiert und startet in ein paar Sekunden selbst neu.",
    ],
    "Melhorias e correções para o seu PvP.": ["Improvements and fixes for your PvP.", "Улучшения и исправления для вашего PvP.", "Verbesserungen und Fehlerbehebungen für dein PvP."],
    "Não consegui procurar atualização. Confira a chave em Ajustes.": [
      "Couldn't check for updates. Check the key in Settings.",
      "Не удалось проверить обновления. Проверьте ключ в настройках.",
      "Update-Suche fehlgeschlagen. Prüfe den Schlüssel in den Einstellungen.",
    ],
    "Não consegui procurar atualização agora. Tento de novo daqui a pouco.": [
      "Couldn't check for updates right now. I'll try again soon.",
      "Сейчас не удалось проверить обновления. Попробую позже.",
      "Konnte gerade nicht nach Updates suchen. Ich versuche es gleich nochmal.",
    ],
    "A atualização falhou. O launcher tenta de novo daqui a pouco.": [
      "The update failed. The launcher will try again shortly.",
      "Обновление не удалось. Лаунчер скоро попробует снова.",
      "Das Update ist fehlgeschlagen. Der Launcher versucht es gleich noch einmal.",
    ],

    // Console do Pulsar
    "Console do Pulsar": ["Pulsar Console", "Консоль Pulsar", "Pulsar-Konsole"],
    "{n0} linhas": ["{0} lines", "Строк: {0}", "{0} Zeilen"],
    "Filtrar (ex.: erro, Pulsar, CHAT)": ["Filter (e.g. error, Pulsar, CHAT)", "Фильтр (напр.: error, Pulsar, CHAT)", "Filtern (z. B. error, Pulsar, CHAT)"],
    "só erros": ["errors only", "только ошибки", "nur Fehler"],
    "rolar sozinho": ["auto-scroll", "автопрокрутка", "automatisch scrollen"],
    "Copiar tudo": ["Copy all", "Копировать всё", "Alles kopieren"],
    Limpar: ["Clear", "Очистить", "Leeren"],
    Copiado: ["Copied", "Скопировано", "Kopiert"],
    "Não deu para copiar": ["Couldn't copy", "Не удалось скопировать", "Kopieren fehlgeschlagen"],
  };

  // Inglês do Reino Unido: o texto americano com a grafia britânica.
  const GRAFIA_UK = [
    [/\b([Cc])olor/g, "$1olour"],
    [/([Oo])ptimiz/g, "$1ptimis"],
    [/\b([Cc])enter/g, "$1entre"],
    [/([Oo])rganiz/g, "$1rganis"],
    [/\b([Aa])rmor/g, "$1rmour"],
    [/([Mm])inimiz/g, "$1inimis"],
  ];

  // Modelos com {0}: viram expressões, as de texto fixo mais longo primeiro (mais específicas).
  const MODELOS = Object.keys(T)
    .filter((k) => /\{n?\d\}/.test(k))
    .map((k) => {
      const ordem = [];
      const fonte = k
        .split(/(\{n?\d\})/)
        .map((parte) => {
          const m = parte.match(/^\{(n?)(\d)\}$/);
          if (!m) return parte.replace(/[.*+?^$()|[\]\\{}]/g, "\\$&");
          ordem.push({ indice: Number(m[2]), numero: !!m[1] });
          return m[1] ? "([\\d.,]+)" : "([\\s\\S]*?)";
        })
        .join("");
      return { chave: k, re: new RegExp("^" + fonte + "$"), ordem, fixo: k.replace(/\{n?\d\}/g, "").length };
    })
    .sort((a, b) => b.fixo - a.fixo);

  let atual = "pt-BR";
  const cache = {};

  function escolher(pedido) {
    const p = String(pedido || "").toLowerCase();
    if (!p) return "pt-BR";
    const exato = IDIOMAS.find((i) => i.id.toLowerCase() === p);
    if (exato) return exato.id;
    if (p.startsWith("ru")) return "ru";
    if (p.startsWith("de")) return "de";
    if (p === "en-uk" || p.startsWith("en-gb")) return "en-GB";
    if (p.startsWith("en")) return "en-US";
    return "pt-BR";
  }

  function emIdioma(linha, idioma) {
    if (!linha) return null;
    const s = linha[COLUNA[idioma]];
    if (s == null) return null;
    return idioma === "en-GB" ? GRAFIA_UK.reduce((acc, [re, troca]) => acc.replace(re, troca), s) : s;
  }

  // Tradução do miolo da frase; null quando não há tradução.
  function traduzir(texto, idioma) {
    const direta = emIdioma(T[texto], idioma);
    if (direta != null) return direta;
    for (const m of MODELOS) {
      const achou = texto.match(m.re);
      if (!achou) continue;
      const alvo = emIdioma(T[m.chave], idioma);
      if (alvo == null) return null;
      const args = [];
      m.ordem.forEach((o, i) => {
        const valor = achou[i + 1];
        args[o.indice] = o.numero || !valor ? valor : traduzir(valor, idioma) ?? valor;
      });
      return alvo.replace(/\{(\d)\}/g, (_, n) => args[Number(n)] ?? "");
    }
    return null;
  }

  // t("Pronto para jogar") → frase no idioma escolhido (ou a original, se não houver tradução).
  function t(texto, idioma = atual) {
    if (texto == null || idioma === "pt-BR" || !(idioma in COLUNA)) return texto;
    const s = String(texto);
    const c = (cache[idioma] = cache[idioma] || new Map());
    if (c.has(s)) return c.get(s);
    const partes = s.match(/^(\s*)([\s\S]*?)(\s*)$/);
    const miolo = partes[2] ? traduzir(partes[2], idioma) : null;
    const saida = miolo == null ? s : partes[1] + miolo + partes[3];
    if (c.size < 2000) c.set(s, saida);
    return saida;
  }

  // Traduz o HTML fixo da página. Guarda o texto original de cada pedaço para poder trocar de idioma de novo.
  const ATRIBUTOS = ["placeholder", "aria-label", "title"];
  const originais = typeof WeakMap === "function" ? new WeakMap() : null;

  function original(no, chave, agora) {
    const salvo = originais.get(no) || {};
    const r = salvo[chave];
    const pt = r && r.mostrado === agora ? r.pt : agora;
    return { salvo, pt };
  }

  function aplicar(raiz) {
    if (typeof document === "undefined") return;
    raiz = raiz || document.body;
    const andar = document.createTreeWalker(raiz, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT, {
      acceptNode: (n) =>
        n.nodeType === 1 && (n.matches("script,style,pre,#log,[data-sem-traducao]") || n.closest("[data-sem-traducao]"))
          ? NodeFilter.FILTER_REJECT
          : NodeFilter.FILTER_ACCEPT,
    });
    for (let n = andar.currentNode; n; n = andar.nextNode()) {
      if (n.nodeType === 3) {
        if (!n.nodeValue.trim()) continue;
        const { salvo, pt } = original(n, "texto", n.nodeValue);
        const novo = t(pt);
        if (novo !== n.nodeValue) n.nodeValue = novo;
        salvo.texto = { pt, mostrado: novo };
        originais.set(n, salvo);
      } else if (n.nodeType === 1) {
        for (const a of ATRIBUTOS) {
          if (!n.hasAttribute(a)) continue;
          const { salvo, pt } = original(n, a, n.getAttribute(a));
          const novo = t(pt);
          if (novo !== n.getAttribute(a)) n.setAttribute(a, novo);
          salvo[a] = { pt, mostrado: novo };
          originais.set(n, salvo);
        }
      }
    }
  }

  function definir(idioma) {
    atual = escolher(idioma);
    if (typeof document !== "undefined") {
      document.documentElement.lang = atual;
      try {
        localStorage.setItem("pulsar.idioma", atual);
      } catch {}
      aplicar(document.body);
      if (tituloPt == null) tituloPt = document.title;
      document.title = t(tituloPt);
    }
    return atual;
  }
  let tituloPt = null;

  return {
    IDIOMAS,
    t,
    definir,
    aplicar,
    escolher,
    bandeira: (id) => BANDEIRAS[id] || "",
    atual: () => atual,
    // Só para o teste: todas as frases conhecidas.
    _frases: () => Object.keys(T),
  };
});
