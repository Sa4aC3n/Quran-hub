import json
import os

locales_dir = "app/src/main/assets/locales"
os.makedirs(locales_dir, exist_ok=True)

# Base Arabic translations
ar = {
    # Nav & Main Tabs
    "nav_home": "الرئيسية",
    "nav_reciters": "القراء",
    "nav_surahs": "السور",
    "nav_mushaf": "المصحف",
    "nav_prayer": "الصلاة",
    "nav_settings": "الإعدادات",
    "nav_favorites": "المفضلة",
    "nav_downloads": "التنزيلات",
    "nav_search": "البحث",
    
    # App Title & Subtitle
    "app_title": "القرآن الكريم",
    "app_subtitle": "كتابك للهداية، صوتاً وتدبراً",
    "app_slogan": "تلاوات عطرة بأصوات كبار القراء في العالم الإسلامي",
    
    # Header & Search
    "search": "بحث",
    "search_placeholder": "ابحث عن سورة أو قارئ...",
    "search_reciters": "البحث في القراء...",
    "search_surahs": "البحث في السور...",
    "filter_all": "الكل",
    "filter_favorites": "المفضلة",
    "filter_downloaded": "المحملة",
    
    # Quick Actions
    "quick_actions_title": "الوصول السريع",
    "quick_reciters": "كبار القراء",
    "quick_surahs": "فهرس السور",
    "quick_mushaf": "تصفح المصحف",
    "quick_prayer": "مواقيت الصلاة",
    "quick_favorites": "قائمتي المفضلة",
    "quick_downloads": "التنزيلات الأوفلاين",
    "quick_random": "تلاوة عشوائية",
    "quick_daily": "ورد اليوم المبارك",
    
    # Continue Listening & Live Haram
    "continue_listening": "أكمل الاستماع",
    "now_playing": "قيد التشغيل الآن",
    "now_reading_haram": "الآن في الحرمين الشريفين",
    "makkah_haram": "المسجد الحرام بمكة",
    "madinah_haram": "المسجد النبوي الشريف",
    "live_stream": "بث مباشر",
    "listen_live": "استمع للبث",
    "haram_settings": "تنبيهات الحرمين",
    
    # Sections
    "popular_reciters": "استمع الآن - كبار القراء",
    "view_all": "عرض الكل",
    "quick_surahs_title": "السور المباركة",
    "quranic_reminder": "ألا بذكر الله تطمئن القلوب",
    "quranic_reminder_sub": "استمع إلى آيات الذكر الحكيم وتدبر معانيها العظيمة",
    
    # Reciter Details
    "reciter_profile": "الملف الشخصي للقارئ",
    "recitations": "التلاوات المتاحة",
    "riwayah": "الرواية",
    "audio_quality": "جودة الصوت",
    "standard_quality": "جودة قياسية",
    "high_quality": "جودة عالية (HQ)",
    "ultra_quality": "جودة فائقة (Lossless)",
    
    # Mushaf / Quran Reader
    "mushaf_reader": "المصحف الشريف",
    "surah": "سورة",
    "ayah": "آية",
    "juz": "الجزء",
    "hizb": "الحزب",
    "page": "الصفحة",
    "tafseer": "التفسير",
    "choose_tafseer": "اختر التفسير",
    "font_size": "حجم الخط",
    "theme_light": "فاتح",
    "theme_dark": "داكن",
    "theme_sepia": "مائل للصفرة",
    "bookmark": "إشارة مرجعية",
    "share_ayah": "مشاركة الآية",
    "copy_ayah": "نسخ الآية",
    
    # Prayer & Qibla
    "prayer_times": "مواقيت الصلاة",
    "qibla_compass": "بوصلة القبلة",
    "fajr": "الفجر",
    "sunrise": "الشروق",
    "dhuhr": "الظهر",
    "asr": "العصر",
    "maghrib": "المغرب",
    "isha": "العشاء",
    "next_prayer": "الصلاة القادمة",
    "time_remaining": "الوقت المتبقي",
    "qibla_direction": "اتجاه القبلة",
    "calibrate_compass": "قم بتدوير الهاتف لمعايرة البوصلة",
    
    # Settings & Preferences
    "settings_title": "الإعدادات والتفضيلات",
    "general_settings": "الإعدادات العامة",
    "language": "اللغة",
    "select_language": "اختر لغة التطبيق",
    "search_language": "ابحث عن لغة...",
    "app_theme": "مظهر التطبيق",
    "theme_system": "تلقائي حسب النظام",
    "theme_light_mode": "المظهر الفاتح",
    "theme_dark_mode": "المظهر الداكن الزمردي",
    "notifications": "الإشعارات والتنبيهات",
    "daily_reminder": "التذكير القرآني اليومي",
    "download_wifi_only": "التنزيل عبر Wi-Fi فقط",
    "storage_management": "إدارة مساحة التخزين",
    "about_app": "عن التطبيق",
    "app_version": "إصدار التطبيق",
    
    # Player Controls
    "play": "تشغيل",
    "pause": "إيقاف مؤقت",
    "stop": "إيقاف",
    "next": "التالي",
    "previous": "السابق",
    "shuffle": "عشوائي",
    "repeat": "تكرار",
    "playback_speed": "سرعة التشغيل",
    "sleep_timer": "مؤقت النوم",
    "download": "تنزيل",
    "downloading": "جاري التنزيل...",
    "download_complete": "تم التنزيل بنجاح",
    "delete": "حذف",
    "cancel": "إلغاء",
    "done": "تم",
    "save": "حفظ",
    "close": "إغلاق",
    "retry": "إعادة المحاولة",
    "offline_mode": "أنت غير متصل بالإنترنت، يتم عرض البيانات المحفوظة",
    "smartwatch_mode": "وضع الساعة الذكية"
}

# English translations
en = {
    "nav_home": "Home",
    "nav_reciters": "Reciters",
    "nav_surahs": "Surahs",
    "nav_mushaf": "Mushaf",
    "nav_prayer": "Prayer",
    "nav_settings": "Settings",
    "nav_favorites": "Favorites",
    "nav_downloads": "Downloads",
    "nav_search": "Search",
    
    "app_title": "Holy Quran",
    "app_subtitle": "Your guide to listening & reflection",
    "app_slogan": "Beautiful recitations by the world's renowned reciters",
    
    "search": "Search",
    "search_placeholder": "Search surah or reciter...",
    "search_reciters": "Search reciters...",
    "search_surahs": "Search surahs...",
    "filter_all": "All",
    "filter_favorites": "Favorites",
    "filter_downloaded": "Downloaded",
    
    "quick_actions_title": "Quick Access",
    "quick_reciters": "Top Reciters",
    "quick_surahs": "Surah Index",
    "quick_mushaf": "Read Mushaf",
    "quick_prayer": "Prayer Times",
    "quick_favorites": "Favorites List",
    "quick_downloads": "Offline Downloads",
    "quick_random": "Random Recitation",
    "quick_daily": "Daily Portion",
    
    "continue_listening": "Continue Listening",
    "now_playing": "Now Playing",
    "now_reading_haram": "Live from the Two Holy Mosques",
    "makkah_haram": "Masjid Al-Haram, Makkah",
    "madinah_haram": "Masjid An-Nabawi, Madinah",
    "live_stream": "Live Stream",
    "listen_live": "Listen Live",
    "haram_settings": "Haram Alerts",
    
    "popular_reciters": "Listen Now - Top Reciters",
    "view_all": "View All",
    "quick_surahs_title": "Blessed Surahs",
    "quranic_reminder": "Verily in the remembrance of Allah do hearts find rest",
    "quranic_reminder_sub": "Listen to the holy verses and ponder their meanings",
    
    "reciter_profile": "Reciter Profile",
    "recitations": "Available Recitations",
    "riwayah": "Riwayah",
    "audio_quality": "Audio Quality",
    "standard_quality": "Standard Quality",
    "high_quality": "High Quality (HQ)",
    "ultra_quality": "Ultra Quality (Lossless)",
    
    "mushaf_reader": "Holy Quran Reader",
    "surah": "Surah",
    "ayah": "Ayah",
    "juz": "Juz",
    "hizb": "Hizb",
    "page": "Page",
    "tafseer": "Tafseer",
    "choose_tafseer": "Select Tafseer",
    "font_size": "Font Size",
    "theme_light": "Light",
    "theme_dark": "Dark",
    "theme_sepia": "Sepia",
    "bookmark": "Bookmark",
    "share_ayah": "Share Ayah",
    "copy_ayah": "Copy Ayah",
    
    "prayer_times": "Prayer Times",
    "qibla_compass": "Qibla Compass",
    "fajr": "Fajr",
    "sunrise": "Sunrise",
    "dhuhr": "Dhuhr",
    "asr": "Asr",
    "maghrib": "Maghrib",
    "isha": "Isha",
    "next_prayer": "Next Prayer",
    "time_remaining": "Time Remaining",
    "qibla_direction": "Qibla Direction",
    "calibrate_compass": "Rotate your phone to calibrate compass",
    
    "settings_title": "Settings & Preferences",
    "general_settings": "General Settings",
    "language": "Language",
    "select_language": "Select App Language",
    "search_language": "Search language...",
    "app_theme": "App Theme",
    "theme_system": "System Default",
    "theme_light_mode": "Light Theme",
    "theme_dark_mode": "Dark Emerald Theme",
    "notifications": "Notifications & Alerts",
    "daily_reminder": "Daily Quran Reminder",
    "download_wifi_only": "Download via Wi-Fi Only",
    "storage_management": "Storage Management",
    "about_app": "About App",
    "app_version": "App Version",
    
    "play": "Play",
    "pause": "Pause",
    "stop": "Stop",
    "next": "Next",
    "previous": "Previous",
    "shuffle": "Shuffle",
    "repeat": "Repeat",
    "playback_speed": "Playback Speed",
    "sleep_timer": "Sleep Timer",
    "download": "Download",
    "downloading": "Downloading...",
    "download_complete": "Download Complete",
    "delete": "Delete",
    "cancel": "Cancel",
    "done": "Done",
    "save": "Save",
    "close": "Close",
    "retry": "Retry",
    "offline_mode": "You are offline. Showing cached data.",
    "smartwatch_mode": "Smartwatch Mode"
}

# French
fr = dict(en)
fr.update({
    "nav_home": "Accueil", "nav_reciters": "Récitateurs", "nav_surahs": "Sourates", "nav_mushaf": "Coran", "nav_prayer": "Prière", "nav_settings": "Paramètres", "nav_favorites": "Favoris", "nav_downloads": "Téléchargements", "nav_search": "Recherche",
    "app_title": "Saint Coran", "app_subtitle": "Votre guide d'écoute et de méditation",
    "search": "Rechercher", "search_placeholder": "Rechercher une sourate ou un récitateur...", "search_reciters": "Rechercher un récitateur...", "search_surahs": "Rechercher une sourate...",
    "filter_all": "Tous", "filter_favorites": "Favoris", "filter_downloaded": "Téléchargés",
    "quick_actions_title": "Accès Rapide", "quick_reciters": "Grands Récitateurs", "quick_surahs": "Index des Sourates", "quick_mushaf": "Lire le Coran", "quick_prayer": "Heures de Prière", "quick_favorites": "Mes Favoris", "quick_downloads": "Téléchargements Hors-ligne", "quick_random": "Récitation Aléatoire", "quick_daily": "Lecture Quotidienne",
    "continue_listening": "Reprendre l'écoute", "now_playing": "En cours de lecture", "now_reading_haram": "En direct des Deux Saintes Mosquées",
    "popular_reciters": "Écouter Maintenant - Grands Récitateurs", "view_all": "Voir Tout", "quick_surahs_title": "Sourates Bénies",
    "quranic_reminder": "N'est-ce point par l'évocation d'Allah que les cœurs s'apaisent ?",
    "mushaf_reader": "Lecture du Coran", "surah": "Sourate", "ayah": "Verset", "juz": "Juz", "hizb": "Hizb", "page": "Page", "tafseer": "Tafsir", "choose_tafseer": "Choisir le Tafsir",
    "prayer_times": "Heures de Prière", "qibla_compass": "Boussole Qibla", "fajr": "Fajr", "sunrise": "Lever du soleil", "dhuhr": "Dhuhr", "asr": "Asr", "maghrib": "Maghrib", "isha": "Isha", "next_prayer": "Prochaine Prière", "time_remaining": "Temps restant",
    "settings_title": "Paramètres & Préférences", "language": "Langue", "select_language": "Choisir la langue", "app_theme": "Thème de l'application",
    "play": "Lecture", "pause": "Pause", "stop": "Arrêter", "next": "Suivant", "previous": "Précédent", "download": "Télécharger", "delete": "Supprimer", "cancel": "Annuler", "done": "Terminé", "save": "Enregistrer", "close": "Fermer"
})

# Italian
it = dict(en)
it.update({
    "nav_home": "Home", "nav_reciters": "Recitatori", "nav_surahs": "Sure", "nav_mushaf": "Corano", "nav_prayer": "Preghiera", "nav_settings": "Impostazioni", "nav_favorites": "Preferiti", "nav_downloads": "Download", "nav_search": "Cerca",
    "app_title": "Sacro Corano", "app_subtitle": "La tua guida all'ascolto e alla riflessione",
    "search": "Cerca", "search_placeholder": "Cerca sura o recitatore...", "filter_all": "Tutti", "filter_favorites": "Preferiti", "filter_downloaded": "Scaricati",
    "quick_actions_title": "Accesso Rapido", "quick_reciters": "Grandi Recitatori", "quick_surahs": "Indice Sure", "quick_mushaf": "Leggi Corano", "quick_prayer": "Orari Preghiera", "quick_favorites": "I Miei Preferiti", "quick_downloads": "Download Offline",
    "continue_listening": "Continua ad ascoltare", "now_playing": "In riproduzione", "popular_reciters": "Ascolta Ora - Grandi Recitatori", "view_all": "Mostra Tutto",
    "prayer_times": "Orari di Preghiera", "qibla_compass": "Bussola Qibla", "fajr": "Fajr", "sunrise": "Alba", "dhuhr": "Dhuhr", "asr": "Asr", "maghrib": "Maghrib", "isha": "Isha",
    "settings_title": "Impostazioni & Preferenze", "language": "Lingua", "select_language": "Seleziona Lingua",
    "play": "Riproduci", "pause": "Pausa", "download": "Scarica", "delete": "Elimina", "cancel": "Annulla", "done": "Fatto", "save": "Salva", "close": "Chiudi"
})

# Japanese
ja = dict(en)
ja.update({
    "nav_home": "ホーム", "nav_reciters": "朗読者", "nav_surahs": "章（スーラ）", "nav_mushaf": "クルアーン", "nav_prayer": "礼拝", "nav_settings": "設定", "nav_favorites": "お気に入り", "nav_downloads": "ダウンロード", "nav_search": "検索",
    "app_title": "聖クルアーン", "app_subtitle": "傾聴と省察のためのあなたの導き",
    "search": "検索", "search_placeholder": "スーラまたは朗読者を検索...", "filter_all": "すべて", "filter_favorites": "お気に入り", "filter_downloaded": "ダウンロード済み",
    "quick_actions_title": "クイックアクセス", "quick_reciters": "著名な朗読者", "quick_surahs": "スーラ一覧", "quick_mushaf": "クルアーンを読む", "quick_prayer": "礼拝時刻", "quick_favorites": "お気に入り", "quick_downloads": "オフライン保存", "quick_random": "ランダム再生", "quick_daily": "本日の朗読",
    "continue_listening": "続きを聴く", "now_playing": "再生中", "popular_reciters": "今すぐ聴く - 著名な朗読者", "view_all": "すべて表示",
    "prayer_times": "礼拝時刻", "qibla_compass": "キブラコンパス", "fajr": "ファジュル", "sunrise": "日の出", "dhuhr": "ズフル", "asr": "アスル", "maghrib": "マグリブ", "isha": "イシャー",
    "settings_title": "設定と環境設定", "language": "言語", "select_language": "言語を選択",
    "play": "再生", "pause": "一時停止", "download": "ダウンロード", "delete": "削除", "cancel": "キャンセル", "done": "完了", "save": "保存", "close": "閉じる"
})

# German
de = dict(en)
de.update({
    "nav_home": "Startseite", "nav_reciters": "Rezitoren", "nav_surahs": "Suren", "nav_mushaf": "Koran", "nav_prayer": "Gebet", "nav_settings": "Einstellungen", "nav_favorites": "Favoriten", "nav_downloads": "Downloads", "nav_search": "Suche",
    "app_title": "Heiliger Koran", "app_subtitle": "Ihr Begleiter zum Hören & Nachdenken",
    "search": "Suchen", "search_placeholder": "Sure oder Rezitator suchen...", "filter_all": "Alle", "filter_favorites": "Favoriten", "filter_downloaded": "Heruntergeladen",
    "quick_actions_title": "Schnellzugriff", "quick_reciters": "Top-Rezitoren", "quick_surahs": "Suren-Verzeichnis", "quick_mushaf": "Koran Lesen", "quick_prayer": "Gebetszeiten", "quick_favorites": "Favoritenliste", "quick_downloads": "Offline-Downloads",
    "continue_listening": "Weiterhören", "now_playing": "Wird abgespielt", "popular_reciters": "Jetzt anhören - Bekannte Rezitoren", "view_all": "Alle anzeigen",
    "prayer_times": "Gebetszeiten", "qibla_compass": "Qibla-Kompass", "fajr": "Fadschr", "sunrise": "Sonnenaufgang", "dhuhr": "Zuhr", "asr": "Asr", "maghrib": "Maghrib", "isha": "Ischa",
    "settings_title": "Einstellungen", "language": "Sprache", "select_language": "Sprache wählen",
    "play": "Abspielen", "pause": "Pause", "download": "Herunterladen", "delete": "Löschen", "cancel": "Abbrechen", "done": "Fertig", "save": "Speichern", "close": "Schließen"
})

# Russian
ru = dict(en)
ru.update({
    "nav_home": "Главная", "nav_reciters": "Чтецы", "nav_surahs": "Суры", "nav_mushaf": "Коран", "nav_prayer": "Намаз", "nav_settings": "Настройки", "nav_favorites": "Избранное", "nav_downloads": "Загрузки", "nav_search": "Поиск",
    "app_title": "Священный Коран", "app_subtitle": "Ваше руководство к слушанию и размышлению",
    "search": "Поиск", "search_placeholder": "Поиск суры или чтеца...", "filter_all": "Все", "filter_favorites": "Избранное", "filter_downloaded": "Загруженные",
    "quick_actions_title": "Быстрый доступ", "quick_reciters": "Лучшие чтецы", "quick_surahs": "Список сур", "quick_mushaf": "Чтение Корана", "quick_prayer": "Время намаза", "quick_favorites": "Мои избранные", "quick_downloads": "Офлайн загрузки",
    "continue_listening": "Продолжить прослушивание", "now_playing": "Сейчас играет", "popular_reciters": "Слушать сейчас - Известные чтецы", "view_all": "Смотреть все",
    "prayer_times": "Время намаза", "qibla_compass": "Компас Киблы", "fajr": "Фаджр", "sunrise": "Восход", "dhuhr": "Зухр", "asr": "Аср", "maghrib": "Магриб", "isha": "Иша",
    "settings_title": "Настройки и параметры", "language": "Язык", "select_language": "Выберите язык",
    "play": "Воспроизвести", "pause": "Пауза", "download": "Скачать", "delete": "Удалить", "cancel": "Отмена", "done": "Готово", "save": "Сохранить", "close": "Закрыть"
})

# Chinese Simplified
zh = dict(en)
zh.update({
    "nav_home": "首页", "nav_reciters": "诵读家", "nav_surahs": "古兰经章节", "nav_mushaf": "古兰经", "nav_prayer": "礼拜", "nav_settings": "设置", "nav_favorites": "收藏", "nav_downloads": "下载", "nav_search": "搜索",
    "app_title": "神圣古兰经", "app_subtitle": "聆听与领悟的精神指引",
    "search": "搜索", "search_placeholder": "搜索章节或诵读家...", "filter_all": "全部", "filter_favorites": "收藏", "filter_downloaded": "已下载",
    "quick_actions_title": "快捷入口", "quick_reciters": "著名诵读家", "quick_surahs": "章节目录", "quick_mushaf": "阅读古兰经", "quick_prayer": "礼拜时间", "quick_favorites": "我的收藏", "quick_downloads": "离线下载",
    "continue_listening": "继续播放", "now_playing": "正在播放", "popular_reciters": "立即聆听 - 著名诵读家", "view_all": "查看全部",
    "prayer_times": "礼拜时间", "qibla_compass": "朝向罗盘", "fajr": "晨礼", "sunrise": "日出", "dhuhr": "晌礼", "asr": "晡礼", "maghrib": "昏礼", "isha": "宵礼",
    "settings_title": "设置与偏好", "language": "语言", "select_language": "选择语言",
    "play": "播放", "pause": "暂停", "download": "下载", "delete": "删除", "cancel": "取消", "done": "完成", "save": "保存", "close": "关闭"
})

# Persian (Farsi - RTL)
fa = dict(ar)
fa.update({
    "nav_home": "خانه", "nav_reciters": "قاریان", "nav_surahs": "سوره‌ها", "nav_mushaf": "مصحف شریف", "nav_prayer": "اوقات نماز", "nav_settings": "تنظیمات", "nav_favorites": "علاقه‌مندی‌ها", "nav_downloads": "دانلودها", "nav_search": "جستجو",
    "app_title": "قرآن کریم", "app_subtitle": "راهنمای شنیدن و تدبر در آیات الهی", "app_slogan": "تلاوت‌های دلنشین با صدای برترین قاریان جهان اسلام",
    "search": "جستجو", "search_placeholder": "جستجوی سوره یا قاری...", "search_reciters": "جستجو در قاریان...", "search_surahs": "جستجو در سوره‌ها...",
    "filter_all": "همه", "filter_favorites": "علاقه‌مندی‌ها", "filter_downloaded": "دانلود شده",
    "quick_actions_title": "دسترسی سریع", "quick_reciters": "قاریان برتر", "quick_surahs": "فهرست سوره‌ها", "quick_mushaf": "مطالعه مصحف", "quick_prayer": "اوقات شرعی", "quick_favorites": "علاقه‌مندی‌های من", "quick_downloads": "دانلودهای آفلاین", "quick_random": "تلاوت تصادفی", "quick_daily": "جزء روزانه",
    "continue_listening": "ادامه استماع", "now_playing": "در حال پخش", "now_reading_haram": "پخش زنده از حرمین شریفین",
    "popular_reciters": "بشنوید - قاریان برجسته", "view_all": "نمایش همه", "quick_surahs_title": "سوره‌های مبارکه",
    "quranic_reminder": "ألا بذكر الله تطمئن القلوب", "quranic_reminder_sub": "به آیات کلام‌الله گوش جان بسپارید و در معانی آن تدبر کنید",
    "mushaf_reader": "قرائت قرآن کریم", "surah": "سوره", "ayah": "آیه", "juz": "جزء", "hizb": "حزب", "page": "صفحه", "tafseer": "تفسیر", "choose_tafseer": "انتخاب تفسیر",
    "prayer_times": "اوقات شرعی", "qibla_compass": "قبله‌نما", "fajr": "اذان صبح", "sunrise": "طلوع آفتاب", "dhuhr": "اذان ظهر", "asr": "اذان عصر", "maghrib": "اذان مغرب", "isha": "اذان عشاء", "next_prayer": "نماز بعدی", "time_remaining": "زمان باقی‌مانده",
    "settings_title": "تنظیمات و سفارشی‌سازی", "language": "زبان", "select_language": "انتخاب زبان برنامه",
    "play": "پخش", "pause": "توقف موقت", "download": "دانلود", "delete": "حذف", "cancel": "انصراف", "done": "انجام شد", "save": "ذخیره", "close": "بستن"
})

# Urdu (RTL)
ur = dict(ar)
ur.update({
    "nav_home": "ہوم", "nav_reciters": "قراء", "nav_surahs": "سورتیں", "nav_mushaf": "مصحف", "nav_prayer": "نماز", "nav_settings": "سیٹنگز", "nav_favorites": "پسندیدہ", "nav_downloads": "ڈاؤن لوڈز", "nav_search": "تلاش",
    "app_title": "قرآن کریم", "app_subtitle": "تلاوت، سماعت اور تدبر کا حسین مجموعہ",
    "search": "تلاش کریں", "search_placeholder": "سورت یا قاری تلاش کریں...", "filter_all": "سب", "filter_favorites": "پسندیدہ", "filter_downloaded": "ڈاؤن لوڈ شدہ",
    "quick_actions_title": "فوری رسائی", "quick_reciters": "مشہور قراء", "quick_surahs": "فہرست سورتیں", "quick_mushaf": "قرآن پڑھیں", "quick_prayer": "اوقات نماز", "quick_favorites": "پسندیدہ تلاوتیں", "quick_downloads": "آف لائن تلاوتیں", "quick_random": "بے ترتیب تلاوت", "quick_daily": "روزانہ کا حصہ",
    "continue_listening": "سماعت جاری رکھیں", "now_playing": "اب چل رہا ہے", "popular_reciters": "ابھی سنیں - مشہور قراء", "view_all": "سب دیکھیں",
    "prayer_times": "اوقات نماز", "qibla_compass": "قبلہ نما", "fajr": "فجر", "sunrise": "طلوع آفتاب", "dhuhr": "ظہر", "asr": "عصر", "maghrib": "مغرب", "isha": "عشاء",
    "settings_title": "ترتیبات", "language": "زبان", "select_language": "زبان منتخب کریں",
    "play": "چلائیں", "pause": "روکیں", "download": "ڈاؤن لوڈ کریں", "delete": "حذف کریں", "cancel": "منسوخ", "done": "مکمل", "save": "محفوظ کریں", "close": "بند کریں"
})

# Hausa (LTR)
ha = dict(en)
ha.update({
    "nav_home": "Gida", "nav_reciters": "Makarantan Alqur'ani", "nav_surahs": "Surori", "nav_mushaf": "Alqur'ani", "nav_prayer": "Sallah", "nav_settings": "Saituna", "nav_favorites": "Wadanda Aka Fi So", "nav_downloads": "Wadanda Aka Sauke", "nav_search": "Bincike",
    "app_title": "Alkur'ani Mai Girma", "app_subtitle": "Hanyar sauraro da bimbini a kan ayoyin Allah",
    "search": "Bincika", "search_placeholder": "Nemi sura ko makaranci...", "filter_all": "Duka", "filter_favorites": "Wadanda aka fi so", "filter_downloaded": "Wadanda aka sauke",
    "quick_actions_title": "Hanyar Sauri", "quick_reciters": "Manyan Makarantan", "quick_surahs": "Jerin Surori", "quick_mushaf": "Karanta Alqur'ani", "quick_prayer": "Lokutan Sallah", "quick_favorites": "Jerin Da Aka Fi So", "quick_downloads": "Sauke Zuwa Waya",
    "continue_listening": "Ci gaba da sauraro", "now_playing": "Yana tashi yanzu", "popular_reciters": "Saurara Yanzu - Manyan Makarantan", "view_all": "Duba Duka",
    "prayer_times": "Lokutan Sallah", "qibla_compass": "Alqibla", "fajr": "Asuba", "sunrise": "Fitowar Rana", "dhuhr": "Azahar", "asr": "La'asar", "maghrib": "Magariba", "isha": "Isha'i",
    "settings_title": "Saituna & Zabuka", "language": "Harshe", "select_language": "Zabi Harshe",
    "play": "Fara", "pause": "Dakatar", "download": "Sauke", "delete": "Goge", "cancel": "Soke", "done": "An gama", "save": "Ajiye", "close": "Rufe"
})

# Kyrgyz (LTR)
ky = dict(en)
ky.update({
    "nav_home": "Башкы бет", "nav_reciters": "Карылар", "nav_surahs": "Сүрөлөр", "nav_mushaf": "Ыйык Куран", "nav_prayer": "Намаз", "nav_settings": "Орнотуулар", "nav_favorites": "Тандалгандар", "nav_downloads": "Жүктөөлөр", "nav_search": "Издөө",
    "app_title": "Ыйык Куран", "app_subtitle": "Угуу жана ой жүгүртүү үчүн жол көрсөткүчүңүз",
    "search": "Издөө", "search_placeholder": "Сүрөнү же карыны издөө...", "filter_all": "Баары", "filter_favorites": "Тандалгандар", "filter_downloaded": "Жүктөлгөндөр",
    "quick_actions_title": "Тез өтүү", "quick_reciters": "Атактуу карылар", "quick_surahs": "Сүрөлөрдүн тизмеси", "quick_mushaf": "Куран окуу", "quick_prayer": "Намаз убактылары", "quick_favorites": "Менин тандалгандарым", "quick_downloads": "Офлайн жүктөөлөр",
    "continue_listening": "Угууну улантуу", "now_playing": "Азыр ойноп жатат", "popular_reciters": "Азыр угуу - Белгилүү карылар", "view_all": "Баарын көрүү",
    "prayer_times": "Намаз убактылары", "qibla_compass": "Кыбыла компасы", "fajr": "Багымдат", "sunrise": "Күн чыгышы", "dhuhr": "Бешим", "asr": "Аср", "maghrib": "Шам", "isha": "Куптан",
    "settings_title": "Орнотуулар", "language": "Тил", "select_language": "Тилди тандаңыз",
    "play": "Ойнотуу", "pause": "Тыныгуу", "download": "Жүктөө", "delete": "Өчүрүү", "cancel": "Жокко чыгаруу", "done": "Даяр", "save": "Сактоо", "close": "Жабуу"
})

all_locales = {
    "ar": ar, "en": en, "fr": fr, "it": it, "ja": ja, "de": de,
    "ru": ru, "zh": zh, "fa": fa, "ur": ur, "ha": ha, "ky": ky
}

for code, data in all_locales.items():
    path = os.path.join(locales_dir, f"{code}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"Written: {path} ({len(data)} keys)")

print("All 12 locales created successfully!")
