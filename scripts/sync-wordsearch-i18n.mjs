import fs from "node:fs";
import path from "node:path";

const RES_DIR = path.resolve("app/src/main/res");

const LOCALE_TRANSLATIONS = {
  "values-pt-rBR": {
    pro_suite_wordsearch_btn: "🔤 Caça-Palavras Pro (100% Offline)",
    pro_ws_title: "🔤 Caça-Palavras Pro (100% Offline)",
    pro_ws_subtitle: "Funciona no ônibus ou avião sem internet! Toque na 1ª e na última letra de cada palavra. Modo Diário muda todo dia pela data do aparelho.",
    pro_ws_found_count: "Encontradas: %1$d / %2$d • Tema: %3$s (%4$s)",
    pro_ws_daily_tag: "Desafio do Dia %1$s",
    pro_ws_random_tag: "Jogo Livre Offline",
    pro_ws_completed_toast: "🏆 Parabéns! Você completou o Caça-Palavras Pro offline!",
    pro_ws_new_puzzle_btn: "🔀 Novo Tabuleiro",
    pro_ws_daily_btn: "📅 Desafio Diário",
    pro_ws_hint_btn: "💡 Dica",
    pro_ws_hint_toast: "💡 Dica: procure o início de \"%1$s\" destacado em dourado!",
    pro_ws_select_start: "Toque em outra ponta da palavra ou toque de novo para cancelar.",
    world_more_games_cta: "👑 Quer mais jogos offline? Conheça o Caça-Palavras Pro na aba Pro VIP"
  },
  values: {
    pro_suite_wordsearch_btn: "🔤 Pro Word Search (100% Offline)",
    pro_ws_title: "🔤 Pro Word Search (100% Offline)",
    pro_ws_subtitle: "Works in airplane mode or on the bus without internet! Tap the 1st and last letter of each hidden word.",
    pro_ws_found_count: "Found: %1$d / %2$d • Theme: %3$s (%4$s)",
    pro_ws_daily_tag: "Daily %1$s",
    pro_ws_random_tag: "Offline Free Play",
    pro_ws_completed_toast: "🏆 Awesome! You solved the entire offline Word Search!",
    pro_ws_new_puzzle_btn: "🔀 New Board",
    pro_ws_daily_btn: "📅 Daily Puzzle",
    pro_ws_hint_btn: "💡 Hint",
    pro_ws_hint_toast: "💡 Hint: look for the start of \"%1$s\" highlighted in gold!",
    pro_ws_select_start: "Now tap the other end of the word, or tap again to clear.",
    world_more_games_cta: "👑 Want more offline games? Try Pro Word Search in the Pro VIP tab"
  },
  "values-ar": {
    pro_suite_wordsearch_btn: "🔤 بحث الكلمات Pro (بدون نت 100%)",
    pro_ws_title: "🔤 بحث الكلمات Pro (بدون نت 100%)",
    pro_ws_subtitle: "يعمل بدون إنترنت في الحافلة أو الطائرة! اضغط أول حرف وآخر حرف لكل كلمة.",
    pro_ws_found_count: "المكتشف: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "تحدي %1$s",
    pro_ws_random_tag: "لعب أوفلاين",
    pro_ws_completed_toast: "🏆 أحسنت! أكملت جميع كلمات التحدي دون إنترنت!",
    pro_ws_new_puzzle_btn: "🔀 لوحة جديدة",
    pro_ws_daily_btn: "📅 التحدي اليومي",
    pro_ws_hint_btn: "💡 تلميح",
    pro_ws_hint_toast: "💡 تلميح: ابحث عن بداية \"%1$s\" باللون الذهبي!",
    pro_ws_select_start: "اضغط آخر حرف في الكلمة الآن.",
    world_more_games_cta: "👑 المزيد من الألعاب بدون نت؟ جرّب بحث الكلمات في تبويب Pro VIP"
  },
  "values-ru": {
    pro_suite_wordsearch_btn: "🔤 Поиск слов Pro (100% Офлайн)",
    pro_ws_title: "🔤 Поиск слов Pro (100% Офлайн)",
    pro_ws_subtitle: "Работает без интернета! Нажмите первую и последнюю букву каждого слова.",
    pro_ws_found_count: "Найдено: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "День %1$s",
    pro_ws_random_tag: "Офлайн-игра",
    pro_ws_completed_toast: "🏆 Браво! Вы нашли все слова без интернета!",
    pro_ws_new_puzzle_btn: "🔀 Новое поле",
    pro_ws_daily_btn: "📅 Ежедневный",
    pro_ws_hint_btn: "💡 Подсказка",
    pro_ws_hint_toast: "💡 Подсказка: начало слова «%1$s» подсвечено золотым!",
    pro_ws_select_start: "Нажмите последнюю букву слова.",
    world_more_games_cta: "👑 Больше офлайн-игр? Откройте Поиск слов во вкладке Pro VIP"
  },
  "values-hi": {
    pro_suite_wordsearch_btn: "🔤 शब्द खोज Pro (100% ऑफ़लाइन)",
    pro_ws_title: "🔤 शब्द खोज Pro (100% ऑफ़लाइन)",
    pro_ws_subtitle: "बिना इंटरनेट के बस या फ़्लाइट मोड में खेलें! शब्द के पहले और आखिरी अक्षर पर टैप करें।",
    pro_ws_found_count: "मिले: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "दैनिक %1$s",
    pro_ws_random_tag: "ऑफ़लाइन खेल",
    pro_ws_completed_toast: "🏆 शानदार! आपने बिना इंटरनेट सभी शब्द खोज लिए!",
    pro_ws_new_puzzle_btn: "🔀 नया बोर्ड",
    pro_ws_daily_btn: "📅 दैनिक चुनौती",
    pro_ws_hint_btn: "💡 संकेत",
    pro_ws_hint_toast: "💡 संकेत: सुनहरे अक्षर से \"%1$s\" शुरू होता है!",
    pro_ws_select_start: "अब शब्द का अंतिम अक्षर टैप करें।",
    world_more_games_cta: "👑 और ऑफ़लाइन गेम चाहिए? Pro VIP टैब में शब्द खोज Pro आज़माएं"
  },
  "values-in": {
    pro_suite_wordsearch_btn: "🔤 Cari Kata Pro (100% Offline)",
    pro_ws_title: "🔤 Cari Kata Pro (100% Offline)",
    pro_ws_subtitle: "Berjalan tanpa kuota internet! Ketuk huruf pertama dan terakhir dari setiap kata.",
    pro_ws_found_count: "Ditemukan: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "Harian %1$s",
    pro_ws_random_tag: "Main Offline",
    pro_ws_completed_toast: "🏆 Hebat! Semua kata ditemukan secara offline!",
    pro_ws_new_puzzle_btn: "🔀 Papan Baru",
    pro_ws_daily_btn: "📅 Tantangan Harian",
    pro_ws_hint_btn: "💡 Petunjuk",
    pro_ws_hint_toast: "💡 Petunjuk: awal kata \"%1$s\" ditandai warna emas!",
    pro_ws_select_start: "Ketuk ujung huruf terakhir dari kata.",
    world_more_games_cta: "👑 Ingin game offline lagi? Coba Cari Kata Pro di tab Pro VIP"
  },
  "values-bn": {
    pro_suite_wordsearch_btn: "🔤 ওয়ার্ড সার্চ Pro (১০০% অফলাইন)",
    pro_ws_title: "🔤 ওয়ার্ড সার্চ Pro (১০০% অফলাইন)",
    pro_ws_subtitle: "ইন্টারনেট ছাড়াই খেলুন! প্রতিটি শব্দের প্রথম এবং শেষ অক্ষরে ট্যাপ করুন।",
    pro_ws_found_count: "পাওয়া গেছে: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "দৈনিক %1$s",
    pro_ws_random_tag: "অফলাইন খেলা",
    pro_ws_completed_toast: "🏆 দারুণ! ইন্টারনেট ছাড়াই সব শব্দ খুঁজে পেয়েছেন!",
    pro_ws_new_puzzle_btn: "🔀 নতুন বোর্ড",
    pro_ws_daily_btn: "📅 দৈনিক চ্যালেঞ্জ",
    pro_ws_hint_btn: "💡 ইঙ্গিত",
    pro_ws_hint_toast: "💡 ইঙ্গিত: সোনালী রঙে \"%1$s\"-এর শুরু দেখানো হয়েছে!",
    pro_ws_select_start: "এবার শব্দের শেষ অক্ষরে ট্যাপ করুন।",
    world_more_games_cta: "👑 আরও অফলাইন গেম চান? Pro VIP ট্যাবে ওয়ার্ড সার্চ দেখুন"
  },
  "values-ko": {
    pro_suite_wordsearch_btn: "🔤 단어 찾기 Pro (100% 오프라인)",
    pro_ws_title: "🔤 단어 찾기 Pro (100% 오프라인)",
    pro_ws_subtitle: "인터넷 없이 비행기 모드에서도 작동! 각 단어의 첫 글자와 끝 글자를 누르세요.",
    pro_ws_found_count: "찾음: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "일일 %1$s",
    pro_ws_random_tag: "오프라인 자유",
    pro_ws_completed_toast: "🏆 축하합니다! 오프라인 단어 찾기를 완료했습니다!",
    pro_ws_new_puzzle_btn: "🔀 새 보드",
    pro_ws_daily_btn: "📅 오늘의 도전",
    pro_ws_hint_btn: "💡 힌트",
    pro_ws_hint_toast: "💡 힌트: \"%1$s\" 시작 글자가 금색으로 표시되었습니다!",
    pro_ws_select_start: "단어의 반대쪽 끝 글자를 누르세요.",
    world_more_games_cta: "👑 더 많은 오프라인 게임? Pro VIP 탭에서 단어 찾기 Pro를 확인하세요"
  },
  "values-es": {
    pro_suite_wordsearch_btn: "🔤 Sopa de Letras Pro (100% Offline)",
    pro_ws_title: "🔤 Sopa de Letras Pro (100% Offline)",
    pro_ws_subtitle: "¡Funciona sin internet en el autobús o avión! Toca la primera y última letra de cada palabra.",
    pro_ws_found_count: "Encontradas: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "Diario %1$s",
    pro_ws_random_tag: "Libre Offline",
    pro_ws_completed_toast: "🏆 ¡Genial! ¡Completaste la Sopa de Letras sin conexión!",
    pro_ws_new_puzzle_btn: "🔀 Nuevo Tablero",
    pro_ws_daily_btn: "📅 Reto Diario",
    pro_ws_hint_btn: "💡 Pista",
    pro_ws_hint_toast: "💡 Pista: busca el inicio de \"%1$s\" resaltado en dorado!",
    pro_ws_select_start: "Toca el otro extremo de la palabra.",
    world_more_games_cta: "👑 ¿Más juegos sin internet? Prueba Sopa de Letras Pro en la pestaña Pro VIP"
  },
  "values-fr": {
    pro_suite_wordsearch_btn: "🔤 Mots Mêlés Pro (100% Hors-ligne)",
    pro_ws_title: "🔤 Mots Mêlés Pro (100% Hors-ligne)",
    pro_ws_subtitle: "Fonctionne sans internet ! Touchez la première puis la dernière lettre de chaque mot.",
    pro_ws_found_count: "Trouvés : %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "Jour %1$s",
    pro_ws_random_tag: "Libre Hors-ligne",
    pro_ws_completed_toast: "🏆 Bravo ! Grille terminée 100 % hors-ligne !",
    pro_ws_new_puzzle_btn: "🔀 Nouvelle Grille",
    pro_ws_daily_btn: "📅 Défi du Jour",
    pro_ws_hint_btn: "💡 Indice",
    pro_ws_hint_toast: "💡 Indice : le début de « %1$s » est surligné en or !",
    pro_ws_select_start: "Touchez la dernière lettre du mot.",
    world_more_games_cta: "👑 Plus de jeux hors-ligne ? Découvrez Mots Mêlés Pro dans l'onglet Pro VIP"
  },
  "values-de": {
    pro_suite_wordsearch_btn: "🔤 Wortsuche Pro (100% Offline)",
    pro_ws_title: "🔤 Wortsuche Pro (100% Offline)",
    pro_ws_subtitle: "Funktioniert ohne Internet! Tippen Sie auf den ersten und letzten Buchstaben jedes Wortes.",
    pro_ws_found_count: "Gefunden: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "Täglich %1$s",
    pro_ws_random_tag: "Freies Offline-Spiel",
    pro_ws_completed_toast: "🏆 Super! Alle Wörter offline gelöst!",
    pro_ws_new_puzzle_btn: "🔀 Neues Rätsel",
    pro_ws_daily_btn: "📅 Tagesrätsel",
    pro_ws_hint_btn: "💡 Hinweis",
    pro_ws_hint_toast: "💡 Hinweis: Anfang von „%1$s“ ist golden markiert!",
    pro_ws_select_start: "Tippen Sie jetzt auf das Wortende.",
    world_more_games_cta: "👑 Mehr Offline-Spiele? Wortsuche Pro im Tab Pro VIP entdecken"
  },
  "values-ja": {
    pro_suite_wordsearch_btn: "🔤 単語探し Pro (100% オフライン)",
    pro_ws_title: "🔤 単語探し Pro (100% オフライン)",
    pro_ws_subtitle: "ネット接続なしで遊べます！各単語の最初と最後の文字をタップしてください。",
    pro_ws_found_count: "発見: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "毎日 %1$s",
    pro_ws_random_tag: "オフライン自由",
    pro_ws_completed_toast: "🏆 おめでとうございます！オフラインで全単語をクリアしました！",
    pro_ws_new_puzzle_btn: "🔀 新しい盤面",
    pro_ws_daily_btn: "📅 今日の問題",
    pro_ws_hint_btn: "💡 ヒント",
    pro_ws_hint_toast: "💡 ヒント：「%1$s」の先頭が金色で光っています！",
    pro_ws_select_start: "単語の終わりの文字をタップしてください。",
    world_more_games_cta: "👑 他のオフラインゲームは Pro VIP タブの単語探し Pro へ"
  },
  "values-zh-rCN": {
    pro_suite_wordsearch_btn: "🔤 寻字解谜 Pro (100% 离线)",
    pro_ws_title: "🔤 寻字解谜 Pro (100% 离线)",
    pro_ws_subtitle: "无需联网也能玩！依次点击每个隐藏单词的首字母和尾字母。",
    pro_ws_found_count: "已找到: %1$d / %2$d • %3$s (%4$s)",
    pro_ws_daily_tag: "每日 %1$s",
    pro_ws_random_tag: "离线畅玩",
    pro_ws_completed_toast: "🏆 恭喜！您已完全离线通关寻字解谜！",
    pro_ws_new_puzzle_btn: "🔀 新棋盘",
    pro_ws_daily_btn: "📅 每日挑战",
    pro_ws_hint_btn: "💡 提示",
    pro_ws_hint_toast: "💡 提示：“%1$s” 的首字母已用金色高亮！",
    pro_ws_select_start: "请再点击该单词的最后一个字母。",
    world_more_games_cta: "👑 更多离线游戏？前往 Pro VIP 标签页体验寻字解谜 Pro"
  }
};

function escapeXml(val) {
  return val
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "\\'");
}

const dirs = fs.readdirSync(RES_DIR).filter((d) => d === "values" || d.startsWith("values-"));
for (const dir of dirs) {
  const stringsPath = path.join(RES_DIR, dir, "strings.xml");
  if (!fs.existsSync(stringsPath)) continue;
  let xml = fs.readFileSync(stringsPath, "utf8");
  const pack = LOCALE_TRANSLATIONS[dir] || LOCALE_TRANSLATIONS.values;

  for (const [key, value] of Object.entries(pack)) {
    const escaped = escapeXml(value);
    const regex = new RegExp(`<string name="${key}"[^>]*>[\\s\\S]*?<\\/string>`, "g");
    const entry = `    <string name="${key}">${escaped}</string>`;
    if (regex.test(xml)) {
      xml = xml.replace(regex, entry);
    } else {
      xml = xml.replace("</resources>", `${entry}\n</resources>`);
    }
  }
  fs.writeFileSync(stringsPath, xml, "utf8");
  console.log(`Synced Word Search Pro keys -> ${dir}/strings.xml`);
}
