#!/usr/bin/env node
/**
 * generate-play-screenshots.mjs
 *
 * Generates 56 Play Store phone screenshots (4 slides x 14 locales at 1080x1920 PNG)
 * PLUS 14 Play Store feature graphics (1024x500 PNG) wrapping clean v2.8.2 Midiastore
 * Android screens inside an OLED showcase frame via Windows WPF DirectWrite/Direct2D
 * hardware-independent rasterizer (scripts/render-screenshots.ps1).
 */

import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const OUT_ROOT = path.join(ROOT, "docs", "campaigns", "store-screenshots");
const BASE_DIR = path.join(OUT_ROOT, "_base");
const ICON_PATH = path.join(ROOT, "docs", "icon-512.png");
const PS_RENDERER = path.join(ROOT, "scripts", "render-screenshots.ps1");

const SLIDE_SPECS = [
  { id: "01_storage", baseFilePt: "home_pt.png", baseFileEn: "home_en.png", accent: "#0A84FF" },
  { id: "02_large_files", baseFilePt: "files_clean.png", baseFileEn: "files_clean.png", accent: "#38BDF8" },
  { id: "03_duplicates_sha256", baseFilePt: "review_clean.png", baseFileEn: "review_clean.png", accent: "#30D158" },
  { id: "04_pro_privacy", baseFilePt: "pro_pt.png", baseFileEn: "pro_en.png", accent: "#0A84FF" },
];

const LOCALES = {
  "pt-BR": {
    wpfFont: "Segoe UI",
    titleFontSize: 52,
    titleLineHeight: 60,
    featureTitleFontSize: 38,
    featureTitleLineHeight: 46,
    featureChips: ["100% No aparelho", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "RAIO-X DO APARELHO",
        title: "Veja onde está seu\nespaço em segundos",
        subtitle: "Mapa real de fotos, vídeos e áudios direto no aparelho, sem enviar nada à nuvem.",
        chips: ["100% No aparelho", "Scoped Storage", "Menos de 8 MB"],
      },
      {
        badge: "LIMPEZA EM 2 TOQUES",
        title: "Ache vídeos gigantes e\npesados para revisar",
        subtitle: "Você confere cada miniatura proposta e decide o que manter antes de qualquer ação.",
        chips: ["Arquivos grandes", "Itens +30 dias", "Controle total"],
      },
      {
        badge: "SHA-256 + LIXEIRA",
        title: "Cópias idênticas e\nexclusão segura",
        subtitle: "Hash SHA-256 exato + confirmação nativa do Android com suporte à lixeira.",
        chips: ["Hash SHA-256", "Lixeira Android 11+", "Sem exclusão oculta"],
      },
      {
        badge: "100% PRIVADO & PRO",
        title: "Limpador OLED (< 8 MB)\ne varredura global Pro",
        subtitle: "Menos de 8 MB, zero exclusão automática, 100% local e planos Google Play.",
        chips: ["Preto OLED", "Zero Nuvem", "Planos Google Play"],
      },
    ],
  },
  "pt-PT": {
    wpfFont: "Segoe UI",
    titleFontSize: 52,
    titleLineHeight: 60,
    featureTitleFontSize: 38,
    featureTitleLineHeight: 46,
    featureChips: ["100% Local", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "MAPA NO TELEMÓVEL",
        title: "Veja o que ocupa o seu\nespaço em segundos",
        subtitle: "Análise real de fotos, vídeos e áudios no equipamento, sem enviar nada para a nuvem.",
        chips: ["100% Local", "Scoped Storage", "Menos de 8 MB"],
      },
      {
        badge: "LIMPEZA EM 2 TOQUES",
        title: "Encontre vídeos grandes\ne antigos para rever",
        subtitle: "Reveja cada ficheiro com miniatura antes de mover para o lixo ou eliminar.",
        chips: ["Ficheiros grandes", "Mais de 30 dias", "Controlo total"],
      },
      {
        badge: "SHA-256 + LIXO ANDROID",
        title: "Duplicados exatos e\nremoção protegida",
        subtitle: "Verificação SHA-256 byte a byte e confirmação oficial do sistema Android.",
        chips: ["Hash SHA-256", "Lixo Android 11+", "Zero auto-exclusão"],
      },
      {
        badge: "PRIVACIDADE & PRO",
        title: "Limpeza OLED (< 8 MB)\ne análise global Pro",
        subtitle: "Menos de 8 MB, zero exclusão automática, 100% local e planos Google Play.",
        chips: ["Design OLED", "Sem Nuvem", "Google Play Billing"],
      },
    ],
  },
  "en-US": {
    wpfFont: "Segoe UI",
    titleFontSize: 52,
    titleLineHeight: 60,
    featureTitleFontSize: 38,
    featureTitleLineHeight: 46,
    featureChips: ["100% On-Device", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "ON-DEVICE STORAGE MAP",
        title: "See what fills your\nstorage in seconds",
        subtitle: "Clear breakdown of photos, videos and audio processed privately on your phone.",
        chips: ["100% On-Device", "Scoped Storage", "Under 8 MB"],
      },
      {
        badge: "2-TAP REVIEW CLEANUP",
        title: "Spot heavy & old\nvideos fast",
        subtitle: "Review real thumbnails yourself before moving or deleting anything.",
        chips: ["Large Files", "Old Media (+30d)", "You Stay in Control"],
      },
      {
        badge: "SHA-256 + TRASH",
        title: "Exact SHA-256 copies\n& safe removal",
        subtitle: "Byte-exact SHA-256 matching plus Android system confirmation and Trash restore.",
        chips: ["Exact SHA-256", "Android Trash", "Zero Auto-Delete"],
      },
      {
        badge: "PURE OLED CLEANER PRO",
        title: "Ultra-light (< 8 MB) &\nfull-library Pro scan",
        subtitle: "Under 8 MB, zero auto-delete, 100% local OLED UI & official Google Play plans.",
        chips: ["Pure OLED UI", "No Cloud Upload", "Google Play Billing"],
      },
    ],
  },
  "es-ES": {
    wpfFont: "Segoe UI",
    titleFontSize: 52,
    titleLineHeight: 60,
    featureTitleFontSize: 38,
    featureTitleLineHeight: 46,
    featureChips: ["100% Local", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "MAPA LOCAL EN EL TELÉFONO",
        title: "Mira qué ocupa tu\nespacio al instante",
        subtitle: "Analiza fotos, videos y audios en tu dispositivo sin subir archivos a la nube.",
        chips: ["100% En el móvil", "Scoped Storage", "Menos de 8 MB"],
      },
      {
        badge: "LIMPIEZA EN 2 TOQUES",
        title: "Detecta videos pesados\ny antiguos primero",
        subtitle: "Tú revisas cada miniatura sugerida y eliges qué conservar antes de borrar.",
        chips: ["Archivos grandes", "Antiguos (+30d)", "Control total"],
      },
      {
        badge: "SHA-256 + PAPELERA",
        title: "Duplicados exactos y\nborrado protegido",
        subtitle: "Verificación exacta SHA-256 y confirmación oficial de Android con papelera.",
        chips: ["Hash SHA-256", "Papelera Android", "Sin borrado oculto"],
      },
      {
        badge: "PRIVACIDAD OLED & PRO",
        title: "Limpiador ligero (< 8 MB)\ny escaneo global Pro",
        subtitle: "Menos de 8 MB, cero borrado automático, 100% local y planes Google Play.",
        chips: ["Diseño OLED", "100% Privado", "Google Play Billing"],
      },
    ],
  },
  "fr-FR": {
    wpfFont: "Segoe UI",
    titleFontSize: 50,
    titleLineHeight: 58,
    featureTitleFontSize: 35,
    featureTitleLineHeight: 43,
    featureChips: ["100% Hors ligne", "SHA-256", "< 8 Mo"],
    slides: [
      {
        badge: "ANALYSE LOCALE",
        title: "Visualisez votre espace\nen quelques secondes",
        subtitle: "Cartographie privée des photos, vidéos et audios directement sur votre téléphone.",
        chips: ["100% Hors ligne", "Scoped Storage", "< 8 Mo"],
      },
      {
        badge: "NETTOYAGE EN 2 GESTES",
        title: "Repérez les vidéos\nlourdes à vérifier",
        subtitle: "Examinez chaque miniature proposée avant toute action dans votre galerie.",
        chips: ["Gros fichiers", "Médias anciens", "Votre choix"],
      },
      {
        badge: "SHA-256 + CORBEILLE",
        title: "Doublons exacts et\ncorbeille Android",
        subtitle: "Empreinte SHA-256 exacte et confirmation système Android avec restauration.",
        chips: ["Exact SHA-256", "Corbeille Android", "Sûr par défaut"],
      },
      {
        badge: "CONFIDENTIALITÉ & PRO",
        title: "Nettoyeur OLED (< 8 Mo)\n& analyse globale Pro",
        subtitle: "Moins de 8 Mo, zéro suppression auto, 100 % local et plans Google Play.",
        chips: ["Noir OLED", "Zéro Cloud", "Google Play Billing"],
      },
    ],
  },
  "de-DE": {
    wpfFont: "Segoe UI",
    titleFontSize: 50,
    titleLineHeight: 58,
    featureTitleFontSize: 35,
    featureTitleLineHeight: 43,
    featureChips: ["100% Lokal", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "LOKALE SPEICHERKARTE",
        title: "Sieh sofort, was deinen\nSpeicher füllt",
        subtitle: "Privater Überblick über Fotos, Videos und Audio direkt auf deinem Gerät.",
        chips: ["100% Lokal", "Scoped Storage", "Unter 8 MB"],
      },
      {
        badge: "PRÜFUNG IN 2 TIPPS",
        title: "Große & alte Videos\nsicher prüfen",
        subtitle: "Du kontrollierst jede Vorschau selbst, bevor etwas verschoben oder gelöscht wird.",
        chips: ["Große Dateien", "Alte Medien", "Volle Kontrolle"],
      },
      {
        badge: "SHA-256 + PAPIERKORB",
        title: "Exakte Duplikate &\nAndroid-Papierkorb",
        subtitle: "Bitgenauer SHA-256-Hash mit nativer Android-Bestätigung und Wiederherstellung.",
        chips: ["Exaktes SHA-256", "Papierkorb", "Kein Auto-Löschen"],
      },
      {
        badge: "DATENSCHUTZ & PRO",
        title: "Schlank (< 8 MB) OLED\n& globaler Pro-Scan",
        subtitle: "Unter 8 MB, kein Auto-Löschen, 100 % lokal auf dem Gerät & Google Play.",
        chips: ["OLED-Design", "Ohne Cloud", "Google Play Billing"],
      },
    ],
  },
  "hi-IN": {
    wpfFont: "Nirmala UI",
    titleFontSize: 48,
    titleLineHeight: 60,
    featureTitleFontSize: 34,
    featureTitleLineHeight: 46,
    featureChips: ["100% ऑफ़लाइन", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "ऑन-डिवाइस स्टोरेज मैप",
        title: "सेकंडों में देखें फ़ोन में\nजगह कहाँ भरी है",
        subtitle: "फ़ोटो, वीडियो और ऑडियो की सुरक्षित जांच — सीधे आपके डिवाइस पर, बिना क्लाउड अपलोड।",
        chips: ["100% ऑफ़लाइन", "Scoped Storage", "< 8 MB"],
      },
      {
        badge: "2 टैप में सुरक्षित सफाई",
        title: "बड़े और पुराने वीडियो\nआसानी से जांचें",
        subtitle: "किसी भी फ़ाइल को हटाने से पहले असली थंबनेल की समीक्षा आप स्वयं करते हैं।",
        chips: ["बड़ी फ़ाइलें", "पुराना मीडिया", "पूरा नियंत्रण"],
      },
      {
        badge: "SHA-256 + ट्रैश",
        title: "सटीक SHA-256 कॉपी\nऔर सुरक्षित ट्रैश",
        subtitle: "बाइट-सटीक SHA-256 मिलान और Android सिस्टम की आधिकारिक पुष्टि व ट्रैश सुरक्षा।",
        chips: ["सटीक SHA-256", "Android ट्रैश", "बिना ऑटो-डिलीट"],
      },
      {
        badge: "100% प्राइवेट और PRO",
        title: "हल्का (< 8 MB) OLED और\nग्लोबल Pro स्टोरेज स्कैन",
        subtitle: "< 8 MB आकार, बिना ऑटो-डिलीट, 100% लोकल प्राइवेसी और Google Play प्लान।",
        chips: ["OLED डार्क UI", "ज़ीरो क्लाउड", "Google Play बिलिंग"],
      },
    ],
  },
  "ja-JP": {
    wpfFont: "Yu Gothic UI",
    titleFontSize: 48,
    titleLineHeight: 58,
    featureTitleFontSize: 34,
    featureTitleLineHeight: 44,
    featureChips: ["100% 端末内処理", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "端末内ストレージ解析",
        title: "容量の使い道を数秒で\nわかりやすく表示",
        subtitle: "写真・動画・音声の内訳を端末内だけで解析。クラウドへの送信はありません。",
        chips: ["100% 端末内処理", "Scoped Storage", "軽量 < 8 MB"],
      },
      {
        badge: "2タップで確認＆整理",
        title: "容量の大きい動画や\n古い項目をすぐ確認",
        subtitle: "実サムネイルを自分で確認してから移動・削除を選択できます。",
        chips: ["大容量ファイル", "古いメディア", "安全な確認フロー"],
      },
      {
        badge: "SHA-256 + ゴミ箱",
        title: "完全一致SHA-256と\nAndroidゴミ箱保護",
        subtitle: "バイト単位のSHA-256一致判定とAndroid公式の削除確認・ゴミ箱復元に対応。",
        chips: ["厳密SHA-256", "Androidゴミ箱", "自動削除なし"],
      },
      {
        badge: "高プライバシー & PRO",
        title: "軽量 (< 8 MB) OLED設計と\n全体Proスキャン",
        subtitle: "8 MB未満、自動削除ゼロ、100%端末内処理と公式Google Playプラン。",
        chips: ["OLEDダークUI", "完全ローカル", "Google Play決済"],
      },
    ],
  },
  "zh-CN": {
    wpfFont: "Microsoft YaHei UI",
    titleFontSize: 50,
    titleLineHeight: 60,
    featureTitleFontSize: 36,
    featureTitleLineHeight: 46,
    featureChips: ["100% 本地处理", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "本机存储全景分析",
        title: "几秒看清手机空间\n到底被什么占用",
        subtitle: "纯本地扫描照片、视频与音频占用，无需联网或上传云端，隐私安全。",
        chips: ["100% 本地处理", "Scoped Storage", "< 8 MB"],
      },
      {
        badge: "2步快速核对清理",
        title: "快速找出大视频与\n长期闲置媒体",
        subtitle: "所有待清理文件均带真实缩略图由您亲自核对，绝不擅自删改。",
        chips: ["超大文件", "陈旧媒体", "完全自主掌控"],
      },
      {
        badge: "SHA-256 + 回收站",
        title: "精准 SHA-256 查重与\n系统级回收站保护",
        subtitle: "采用字节级 SHA-256 哈希核验，搭配 Android 官方弹窗确认与回收站恢复。",
        chips: ["精准 SHA-256", "系统回收站", "绝不静默删除"],
      },
      {
        badge: "纯粹清理与 PRO",
        title: "< 8 MB 轻量 OLED 与\n全盘 Pro 深度查重",
        subtitle: "小于 8 MB、绝不自动删除、100% 本地隐私与 Google Play 官方订阅。",
        chips: ["OLED 暗黑界面", "零云端上传", "Google Play 计费"],
      },
    ],
  },
  "ar": {
    wpfFont: "Segoe UI",
    titleFontSize: 48,
    titleLineHeight: 58,
    featureTitleFontSize: 36,
    featureTitleLineHeight: 46,
    featureChips: ["100% على الجهاز", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "تحليل التخزين على الجهاز",
        title: "اعرف ما يملأ مساحة\nهاتفك في ثوانٍ",
        subtitle: "تحليل واضح للصور والفيديوهات والصوتيات محلياً على هاتفك بدون رفع للسحابة.",
        chips: ["100% على الجهاز", "Scoped Storage", "أقل من 8 MB"],
      },
      {
        badge: "تنظيف آمن بخطوتين",
        title: "اكتشف الفيديوهات الكبيرة\nوالقديمة للمراجعة",
        subtitle: "راجع الصور المصغرة بنفسك واختر ما تريد الاحتفاظ به قبل أي حذف.",
        chips: ["ملفات كبيرة", "وسائط قديمة", "تحكم كامل"],
      },
      {
        badge: "SHA-256 + سلة المحذوفات",
        title: "نسخ مكررة متطابقة\nوحذف آمن ومؤكد",
        subtitle: "فحص SHA-256 دقيق مع نافذة تأكيد أندرويد الرسمية ودعم سلة المحذوفات.",
        chips: ["تطابق SHA-256", "سلة أندرويد", "بدون حذف تلقائي"],
      },
      {
        badge: "خصوصية تامة & PRO",
        title: "تصميم OLED خفيف (< 8 MB)\nوفحص شامل Pro",
        subtitle: "أقل من 8 MB، بدون حذف تلقائي، خصوصية محلية 100% وخطط Google Play.",
        chips: ["واجهة OLED", "خصوصية 100%", "Google Play"],
      },
    ],
  },
  "ru-RU": {
    wpfFont: "Segoe UI",
    titleFontSize: 48,
    titleLineHeight: 56,
    featureTitleFontSize: 35,
    featureTitleLineHeight: 43,
    featureChips: ["100% Локально", "SHA-256", "< 8 МБ"],
    slides: [
      {
        badge: "АНАЛИЗ ПАМЯТИ НА УСТРОЙСТВЕ",
        title: "Узнайте, что занимает\nместо, за секунды",
        subtitle: "Наглядная карта фото, видео и аудио локально на телефоне без загрузки в облако.",
        chips: ["100% Локально", "Scoped Storage", "< 8 МБ"],
      },
      {
        badge: "ОЧИСТКА В 2 КАСАНИЯ",
        title: "Находите тяжелые и\nстарые видео быстро",
        subtitle: "Проверяйте миниатюры файлов лично перед перемещением в корзину или удалением.",
        chips: ["Крупные файлы", "Старые медиа", "Полный контроль"],
      },
      {
        badge: "SHA-256 + КОРЗИНА",
        title: "Точные дубликаты и\nбезопасное удаление",
        subtitle: "Побайтовая проверка SHA-256 и официальное подтверждение Android с корзиной.",
        chips: ["Точный SHA-256", "Корзина Android", "Без автоудаления"],
      },
      {
        badge: "ПРИВАТНОСТЬ OLED & PRO",
        title: "Легкий (< 8 МБ) OLED и\nполное сканирование Pro",
        subtitle: "Менее 8 МБ, без автоудаления, 100% локально и планы Google Play.",
        chips: ["Дизайн OLED", "Без облака", "Google Play Billing"],
      },
    ],
  },
  "id": {
    wpfFont: "Segoe UI",
    titleFontSize: 50,
    titleLineHeight: 58,
    featureTitleFontSize: 35,
    featureTitleLineHeight: 43,
    featureChips: ["100% Lokal", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "PETA PENYIMPANAN LOKAL",
        title: "Lihat apa yang memenuhi\nmemori dalam detik",
        subtitle: "Rincian foto, video, dan audio diproses 100% di ponsel tanpa unggah ke cloud.",
        chips: ["100% Di Perangkat", "Scoped Storage", "< 8 MB"],
      },
      {
        badge: "BERSIHKAN DALAM 2 KETUKAN",
        title: "Temukan video besar &\nlama untuk ditinjau",
        subtitle: "Tinjau thumbnail asli setiap file sebelum memindahkan atau menghapus apa pun.",
        chips: ["File Besar", "Media Lama", "Kendali Penuh"],
      },
      {
        badge: "SHA-256 + SAMPAH ANDROID",
        title: "Duplikat SHA-256 akurat\n& penghapusan aman",
        subtitle: "Pencocokan byte SHA-256 ditambah konfirmasi resmi Android dan pemulihan Sampah.",
        chips: ["SHA-256 Akurat", "Sampah Android", "Tanpa Hapus Otomatis"],
      },
      {
        badge: "PEMBERSIH OLED & PRO",
        title: "Ringan (< 8 MB) OLED &\npemindaian penuh Pro",
        subtitle: "Di bawah 8 MB, tanpa hapus otomatis, 100% lokal, dan paket Google Play.",
        chips: ["Layar OLED", "Tanpa Cloud", "Google Play Billing"],
      },
    ],
  },
  "bn-BD": {
    wpfFont: "Nirmala UI",
    titleFontSize: 48,
    titleLineHeight: 60,
    featureTitleFontSize: 34,
    featureTitleLineHeight: 46,
    featureChips: ["100% অফলাইন", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "অন-ডিভাইস স্টোরেজ ম্যাপ",
        title: "কয়েক সেকেন্ডে দেখুন ফোনে\nকোথায় জায়গা ভরছে",
        subtitle: "ফটো, ভিডিও ও অডিওর নিরাপদ বিশ্লেষণ — সরাসরি আপনার ফোনে, কোনো ক্লাউড আপলোড ছাড়া।",
        chips: ["100% অফলাইন", "Scoped Storage", "< 8 MB"],
      },
      {
        badge: "২ ট্যাপে নিরাপদ ক্লিনআপ",
        title: "বড় এবং পুরনো ভিডিও\nসহজেই যাচাই করুন",
        subtitle: "যেকোনো ফাইল মুছে ফেলার আগে আসল থাম্বনেইল দেখে আপনি নিজেই সিদ্ধান্ত নিন।",
        chips: ["বড় ফাইল", "পুরনো মিডিয়া", "সম্পূর্ণ নিয়ন্ত্রণ"],
      },
      {
        badge: "SHA-256 + ট্র্যাশ সুরক্ষা",
        title: "সঠিক SHA-256 ডুপ্লিকেট\nও নিরাপদ মুছে ফেলা",
        subtitle: "বাইট-সঠিক SHA-256 যাচাই এবং Android-এর অফিসিয়াল কনফার্মেশন ও ট্র্যাশ রিস্টোর।",
        chips: ["সঠিক SHA-256", "Android ট্র্যাশ", "অটো-ডিলিট নেই"],
      },
      {
        badge: "প্রাইভেসি ও PRO স্ক্যান",
        title: "হালকা (< 8 MB) OLED এবং\nসম্পূর্ণ Pro স্টোরেজ স্ক্যান",
        subtitle: "< 8 MB আকার, অটো-ডিলিট নেই, 100% লোকাল প্রাইভেসি ও Google Play প্ল্যান।",
        chips: ["OLED ডার্ক UI", "ক্লাউড ছাড়া", "Google Play বিলিং"],
      },
    ],
  },
  "ko-KR": {
    wpfFont: "Malgun Gothic",
    titleFontSize: 48,
    titleLineHeight: 58,
    featureTitleFontSize: 35,
    featureTitleLineHeight: 45,
    featureChips: ["100% 기기 내 처리", "SHA-256", "< 8 MB"],
    slides: [
      {
        badge: "기기 내 저장공간 분석",
        title: "몇 초 만에 저장공간\n사용 내역을 한눈에",
        subtitle: "사진, 동영상, 오디오 용량을 클라우드 전송 없이 기기 내부에서 안전하게 분석합니다.",
        chips: ["100% 기기 내 처리", "Scoped Storage", "초경량 < 8 MB"],
      },
      {
        badge: "2번 탭으로 빠른 검토 정리",
        title: "대용량 동영상과 오래된\n파일을 빠르게 확인",
        subtitle: "실제 썸네일을 직접 확인하고 삭제할 항목을 스스로 선택할 수 있습니다.",
        chips: ["대용량 파일", "오래된 미디어", "완전한 제어"],
      },
      {
        badge: "SHA-256 + 휴지통 보호",
        title: "정확한 SHA-256 중복 검사\n및 안전한 삭제 확인",
        subtitle: "바이트 단위 SHA-256 해시 검증과 Android 공식 삭제 확인 및 휴지통 복원을 지원합니다.",
        chips: ["정밀 SHA-256", "Android 휴지통", "자동 삭제 없음"],
      },
      {
        badge: "완벽한 프라이버시 & PRO",
        title: "< 8 MB 초경량 OLED 및\n전체 Pro 정밀 스캔",
        subtitle: "8 MB 미만, 자동 삭제 없음, 100% 기기 내 처리 및 공식 Google Play 플랜.",
        chips: ["OLED 다크 UI", "클라우드 없음", "Google Play 결제"],
      },
    ],
  },
};

function assertValidPngDimensions(filePath, expectedW, expectedH) {
  const buf = fs.readFileSync(filePath);
  if (buf.length < 32) {
    throw new Error(`PNG too small: ${filePath} (${buf.length} bytes)`);
  }
  const pngSig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  if (!buf.subarray(0, 8).equals(pngSig)) {
    throw new Error(`Invalid PNG signature in ${filePath}`);
  }
  const ihdrType = buf.subarray(12, 16).toString("ascii");
  if (ihdrType !== "IHDR") {
    throw new Error(`Missing IHDR chunk in ${filePath}`);
  }
  const width = buf.readUInt32BE(16);
  const height = buf.readUInt32BE(20);
  if (width !== expectedW || height !== expectedH) {
    throw new Error(`Unexpected dimensions in ${filePath}: ${width}x${height} (expected ${expectedW}x${expectedH})`);
  }
  return { width, height, bytes: buf.length };
}

function main() {
  if (!fs.existsSync(PS_RENDERER)) {
    throw new Error(`Missing WPF renderer script: ${PS_RENDERER}`);
  }
  if (!fs.existsSync(ICON_PATH)) {
    throw new Error(`Missing app icon: ${ICON_PATH}`);
  }

  const localeArg = process.argv.find((arg) => arg.startsWith("--locale="));
  const filterLocale = localeArg ? localeArg.slice("--locale=".length).trim() : null;

  if (filterLocale && !LOCALES[filterLocale]) {
    throw new Error(
      `Unknown locale '${filterLocale}'. Valid locales: ${Object.keys(LOCALES).join(", ")}`
    );
  }

  const targetLocales = filterLocale
    ? [[filterLocale, LOCALES[filterLocale]]]
    : Object.entries(LOCALES);

  const manifest = [];
  for (const [localeCode, config] of targetLocales) {
    const isPortuguese = localeCode === "pt-BR" || localeCode === "pt-PT";
    const localeOutDir = path.join(OUT_ROOT, localeCode);

    // Clean up deprecated 04_privacy_studio.png if present
    const oldSlide4 = path.join(localeOutDir, "04_privacy_studio.png");
    if (fs.existsSync(oldSlide4)) {
      fs.unlinkSync(oldSlide4);
    }

    for (let i = 0; i < SLIDE_SPECS.length; i++) {
      const spec = SLIDE_SPECS[i];
      const slide = config.slides[i];
      const chosenBase = isPortuguese ? spec.baseFilePt : spec.baseFileEn;
      const baseImagePath = path.join(BASE_DIR, chosenBase);
      if (!fs.existsSync(baseImagePath)) {
        throw new Error(`Missing base screenshot: ${baseImagePath}`);
      }
      manifest.push({
        kind: "phone",
        expectedW: 1080,
        expectedH: 1920,
        locale: localeCode,
        id: spec.id,
        wpfFont: config.wpfFont,
        titleFontSize: config.titleFontSize,
        titleLineHeight: config.titleLineHeight,
        accent: spec.accent,
        badge: slide.badge,
        title: slide.title,
        subtitle: slide.subtitle,
        chips: slide.chips,
        baseImagePath,
        iconImagePath: ICON_PATH,
        outPngPath: path.join(localeOutDir, `${spec.id}.png`),
      });
    }

    // Add 1024x500 feature_graphic.png item
    const fgBasePath = path.join(BASE_DIR, isPortuguese ? "home_pt.png" : "home_en.png");
    manifest.push({
      kind: "feature",
      expectedW: 1024,
      expectedH: 500,
      locale: localeCode,
      id: "feature_graphic",
      wpfFont: config.wpfFont,
      featureTitleFontSize: config.featureTitleFontSize,
      featureTitleLineHeight: config.featureTitleLineHeight,
      accent: "#0A84FF",
      badge: config.slides[0].badge,
      title: config.slides[0].title,
      subtitle: "",
      chips: config.featureChips,
      baseImagePath: fgBasePath,
      iconImagePath: ICON_PATH,
      outPngPath: path.join(localeOutDir, "feature_graphic.png"),
    });
  }

  const tmpRoot = fs.mkdtempSync(path.join(os.tmpdir(), "midiastore-wpf-"));
  const manifestPath = path.join(tmpRoot, "manifest.json");
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), "utf8");

  try {
    const out = execFileSync(
      "powershell.exe",
      [
        "-Sta",
        "-NoProfile",
        "-Command",
        "& ([ScriptBlock]::Create([System.IO.File]::ReadAllText($env:MIDI_SHOTS_PS1, [System.Text.Encoding]::UTF8)))",
      ],
      {
        encoding: "utf8",
        timeout: 240000,
        env: {
          ...process.env,
          MIDI_SHOTS_MANIFEST: manifestPath,
          MIDI_SHOTS_PS1: PS_RENDERER,
        },
      }
    );
    if (out.trim()) console.log(out.trim());
  } finally {
    try {
      fs.rmSync(tmpRoot, { recursive: true, force: true });
    } catch {}
  }

  let totalBytes = 0;
  let phoneCount = 0;
  let featureCount = 0;
  for (const item of manifest) {
    const meta = assertValidPngDimensions(item.outPngPath, item.expectedW, item.expectedH);
    totalBytes += meta.bytes;
    if (item.kind === "feature") featureCount += 1;
    else phoneCount += 1;
  }

  console.log(
    `\nDONE: Generated & IHDR-verified ${phoneCount} phone screenshots (1080x1920) + ${featureCount} feature graphics (1024x500) across ${targetLocales.length} locales.`
  );
  console.log(`Total size: ${(totalBytes / 1024 / 1024).toFixed(2)} MB`);
}

main();
