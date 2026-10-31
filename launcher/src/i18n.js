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
    // Topo e navegação
    "Início": ["Home", "Главная", "Start"],
    Mods: ["Mods", "Моды", "Mods"],
    Texturas: ["Textures", "Текстуры", "Texturen"],
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
    "Versão 0.7.0:": ["Version 0.7.0:", "Версия 0.7.0:", "Version 0.7.0:"],
    "Editar HUD mostra só os mods ligados, com tamanho na roda do mouse e uma cruz com ímã no meio da tela. Paleta de cores, mira desenhada, atalhos sem limite, bonecos otimizados no Motor, Rastro de Cometa e idiomas no launcher.": [
      "Edit HUD only shows the mods that are on, with size on the mouse wheel and a magnetic cross in the middle of the screen. Color palette, drawn crosshair, unlimited shortcuts, optimized players in the Engine, Comet Trail and launcher languages.",
      "«Редактор HUD» показывает только включённые моды: размер меняется колесом мыши, а крест в центре экрана притягивает элементы. Палитра цветов, рисованный прицел, безлимитные быстрые фразы, оптимизированные игроки в Двигателе, Хвост кометы и языки в лаунчере.",
      "HUD bearbeiten zeigt nur eingeschaltete Mods, mit Größe per Mausrad und einem magnetischen Kreuz in der Bildschirmmitte. Farbpalette, gezeichnetes Fadenkreuz, unbegrenzte Kürzel, optimierte Spieler im Motor, Kometenschweif und Sprachen im Launcher.",
    ],
    "Registro do jogo": ["Game log", "Журнал игры", "Spielprotokoll"],
    console: ["console", "консоль", "Konsole"],
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
    "Zoom suave segurando uma tecla (padrão C), a roda do mouse ajusta": [
      "Smooth zoom while holding a key (default C), the mouse wheel adjusts it",
      "Плавный зум при удержании клавиши (по умолчанию C), колесо мыши регулирует",
      "Sanfter Zoom beim Halten einer Taste (Standard C), das Mausrad stellt ihn ein",
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
