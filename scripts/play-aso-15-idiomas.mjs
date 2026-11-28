#!/usr/bin/env node
/**
 * PLAY ASO 15 IDIOMAS + REVIEWS DETERMINÍSTICOS — Versão Blindada (Zero LLM)
 * Pacote: com.rdf112.midiastore
 *
 * Regras Invioláveis:
 * 1. Padrão --dry-run (cria edit efêmero na Play API v3, valida tudo e executa DELETE /edits/{id} sem commitar).
 * 2. Quando chamado com --commit no futuro, usa OBRIGATORIAMENTE ?changesInReviewBehavior=ERROR_IF_IN_REVIEW.
 * 3. Linter estrito nos 15 idiomas (pt-BR mantido em 25 chars + 13 títulos expandidos <=30 chars + nova ficha it-IT + fullDescription v2.9.0):
 *    bloqueia termos proibidos ("Grátis/Free/Gratuito", "#1", "Melhor/Best", "RAM booster", "CPU cooler", "Estúdio/Studio", emojis em título/curta).
 * 4. Reviews determinísticos (zero LLM):
 *    - 4 e 5 estrelas -> templates nativos rotativos <= 350 chars reforçando < 7 MB e confirmação do usuário.
 *    - 1 a 3 estrelas -> NUNCA responde automático; grava na fila manual docs/aso/play-reviews-manual-queue.json.
 */

import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const KEY_PATH = path.join(projectRoot, ".secrets", "play-service-account.json");
const PACKAGE_NAME = "com.rdf112.midiastore";
const API_BASE = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}`;

const TITLE_MAX = 30;
const SHORT_MAX = 80;
const FULL_MAX = 4000;
const REVIEW_REPLY_MAX = 350;

// Linter estrito de conformidade Google Play Metadata Policy + Engenharia Midiastore (fronteiras Unicode reais \p{L}\p{N})
const FORBIDDEN_METADATA_PATTERNS = [
  {
    label: "PROMO_FREE",
    rx: /(?<![\p{L}\p{N}])(?:gr[aá]tis|free|gratuit[eo]?|kostenlos|무료|無料|免费|бесплатно|फ्री|मुफ़्त|مجاني|ফ্রি)(?![\p{L}\p{N}])/iu,
  },
  {
    label: "RANKING_SUPERLATIVE",
    rx: /(?:(?:#|n[º°o]\.?)\s*1(?![\p{L}\p{N}])|(?<![\p{L}\p{N}])(?:melhor|best|mejor|meilleur|beste|migliore|terbaik|лучший|최고|ナンバーワン)(?![\p{L}\p{N}]))/iu,
  },
  {
    label: "FAKE_BOOSTER_COOLER",
    rx: /(?<![\p{L}\p{N}])(?:ram\s*boost(?:er)?|cpu\s*cooler|memory\s*booster|speed\s*booster|acelerador\s+de\s+ram|esfriar\s+celular|antiv[ií]rus)(?![\p{L}\p{N}])/iu,
  },
  {
    label: "DEPRECATED_STUDIO",
    rx: /(?<![\p{L}\p{N}])(?:est[uú]dio\s+de\s+m[ií]dia|media\s+studio|qr\s*code|exif)(?![\p{L}\p{N}])/iu,
  },
];

const EMOJI_IN_HEADLINE_RX = /[\p{Extended_Pictographic}]/u;

/**
 * 15 Idiomas v2.9.0 (< 7 MB, 3 abas Limpar/Arquivos/Pro, SHA-256, Fotos Parecidas, Compressor H.265 Pro, Mensageiros, Scoped Storage).
 * - pt-BR preserva título aprovado (25 chars)
 * - 13 idiomas existentes recebem títulos ASO <= 30 chars (antes tinham apenas "Midiastore" com 10 chars)
 * - it-IT adicionado como 15º idioma oficial
 */
export const LISTINGS_15_V290 = {
  "pt-BR": {
    title: "Midiastore: Limpar Espaço",
    shortDescription: "Limpador leve (< 7 MB): duplicatas SHA-256, vídeos grandes e revisão segura.",
    fullDescription:
      "Midiastore é um limpador de armazenamento para Android ultraleve (menos de 7 MB), rápido e 100% transparente, com análise local no seu aparelho.\n\n" +
      "O QUE O MIDIASTORE FAZ DE VERDADE:\n" +
      "• Limpador Ultraleve (< 7 MB): instala rápido mesmo quando o armazenamento já está quase cheio.\n" +
      "• Vídeos e Arquivos Grandes: localize mídias pesadas em segundos e escolha o que manter.\n" +
      "• Duplicatas Exatas (SHA-256): encontra arquivos idênticos byte a byte protegendo uma cópia original.\n" +
      "• Limpeza de Mídias de Mensageiros: revise vídeos e arquivos repetidos de WhatsApp, Telegram e Download sem apagar suas conversas.\n" +
      "• Recursos Pro Opcionais: Fotos Parecidas (com proteção de nitidez) e Compressor de Vídeo H.265 por hardware para economizar gigabytes mantendo qualidade.\n\n" +
      "SEGURANÇA E PRIVACIDADE (SCOPED STORAGE):\n" +
      "• Nada é excluído sozinho: toda remoção exige sua revisão e confirmação no diálogo oficial do Android.\n" +
      "• Processamento 100% no dispositivo, sem envio das suas fotos ou vídeos para servidores externos.",
  },
  "en-US": {
    title: "Midiastore: Storage Cleaner",
    shortDescription: "Compact < 7 MB cleaner: SHA-256 duplicates, large videos & safe review.",
    fullDescription:
      "Midiastore is a compact (< 7 MB), privacy-first Android storage cleaner designed for fast, transparent on-device file cleanup.\n\n" +
      "KEY STORAGE CLEANUP FEATURES:\n" +
      "• Ultra-Light (< 7 MB): installs quickly even when your phone storage is nearly full.\n" +
      "• Large Videos & Files: spot heavy media in seconds and review before removing.\n" +
      "• Exact Duplicate Finder (SHA-256): detects true byte-for-byte duplicate files while keeping an original safe.\n" +
      "• Messenger Media Cleanup: review repeated videos and downloads from WhatsApp, Telegram, and LINE without deleting chat history.\n" +
      "• Optional Pro Tools: Similar/Burst Photo review and hardware H.265 Video Compression to shrink large recordings on-device.\n\n" +
      "OFFICIAL ANDROID SAFETY & PRIVACY:\n" +
      "• Zero automatic deletion: every cleanup requires your explicit confirmation via Android's official system prompt.\n" +
      "• 100% on-device MediaStore analysis—your personal media stays on your phone.",
  },
  "es-ES": {
    title: "Midiastore: Limpiar Espacio",
    shortDescription: "Limpiador ligero (< 7 MB): duplicados SHA-256, videos grandes y control.",
    fullDescription:
      "Midiastore es un limpiador de almacenamiento para Android ultraligero (menos de 7 MB) con procesamiento 100 % local en tu teléfono.\n\n" +
      "FUNCIONES REALES DE LIMPIEZA:\n" +
      "• App Ultraligera (< 7 MB): se instala rápido incluso cuando queda poco espacio.\n" +
      "• Videos y Archivos Grandes: identifica al instante qué archivos ocupan más memoria.\n" +
      "• Duplicados Exactos (SHA-256): detecta copias idénticas byte a byte conservando el archivo original.\n" +
      "• Multimedia de Mensajería: revisa archivos repetidos de WhatsApp, Telegram y Descargas sin borrar tus chats.\n" +
      "• Herramientas Pro Opcionales: detección de fotos similares y compresor de video H.265 por hardware.\n\n" +
      "SEGURIDAD OFICIAL DE ANDROID:\n" +
      "• Ningún archivo se borra sin tu confirmación expresa en el cuadro oficial de Android.",
  },
  "fr-FR": {
    title: "Midiastore: Nettoyer Espace",
    shortDescription: "Nettoyeur léger (< 7 Mo) : doublons SHA-256, grosses vidéos et contrôle.",
    fullDescription:
      "Midiastore est un nettoyeur de stockage Android compact (moins de 7 Mo), conçu pour libérer de l'espace localement et en toute transparence.\n\n" +
      "FONCTIONS PRINCIPALES :\n" +
      "• Application Légère (< 7 Mo) : installation rapide même lorsque la mémoire est saturée.\n" +
      "• Gros Fichiers et Vidéos : repérez immédiatement les médias volumineux.\n" +
      "• Doublons Exacts (SHA-256) : vérification cryptographique octet par octet tout en conservant un exemplaire.\n" +
      "• Médias des Messageries : triez les vidéos reçues sur WhatsApp et Telegram sans supprimer vos discussions.\n" +
      "• Outils Pro Optionnels : tri des photos similaires et compression vidéo matérielle H.265.\n\n" +
      "SÉCURITÉ ANDROID :\n" +
      "• Aucune suppression automatique : vous validez chaque action via la confirmation officielle d'Android.",
  },
  "de-DE": {
    title: "Midiastore: Speicher Reiniger",
    shortDescription: "Kompakt (< 7 MB): SHA-256-Duplikate, große Videos & sichere Prüfung.",
    fullDescription:
      "Midiastore ist ein kompakter Speicher-Reiniger für Android (unter 7 MB) mit 100 % lokaler Analyse auf Ihrem Smartphone.\n\n" +
      "KERNFUNKTIONEN:\n" +
      "• Ultraleicht (< 7 MB): schnelle Installation auch bei fast vollem Gerätespeicher.\n" +
      "• Große Videos & Dateien: finden Sie speicherintensive Medien in Sekunden.\n" +
      "• Exakte Duplikate (SHA-256): erkennt identische Kopien Byte für Byte und schützt ein Original.\n" +
      "• Messenger-Medien prüfen: wiederholte Dateien aus WhatsApp, Telegram und Downloads prüfen, ohne Chats zu löschen.\n" +
      "• Optionale Pro-Funktionen: ähnliche Serienfotos prüfen und H.265-Hardware-Videokomprimierung.\n\n" +
      "SICHERHEIT & DATENSCHUTZ:\n" +
      "• Kein automatisches Löschen: jede Entfernung erfordert Ihre ausdrückliche Bestätigung im Android-Systemdialog.",
  },
  "it-IT": {
    title: "Midiastore: Pulizia Spazio",
    shortDescription: "Pulitore leggero (< 7 MB): duplicati SHA-256, video grandi e controllo.",
    fullDescription:
      "Midiastore è un pulitore di spazio di archiviazione per Android ultraleggero (meno di 7 MB), progettato con analisi 100% locale sul dispositivo.\n\n" +
      "FUNZIONI DI PULIZIA REALE:\n" +
      "• Ultraleggero (< 7 MB): si installa rapidamente anche quando la memoria del telefono è quasi piena.\n" +
      "• Video e File Grandi: individua subito i contenuti che occupano più spazio.\n" +
      "• Duplicati Esatti (SHA-256): trova copie identiche byte per byte proteggendo il file originale.\n" +
      "• Media delle Chat: controlla video e file ripetuti di WhatsApp, Telegram e Download senza cancellare le conversazioni.\n" +
      "• Strumenti Pro Opzionali: revisione foto simili e compressione video H.265 hardware per ridurre il peso dei video.\n\n" +
      "SICUREZZA UFFICIALE ANDROID:\n" +
      "• Nessuna cancellazione automatica: ogni eliminazione richiede sempre la tua conferma esplicita nel dialogo di sistema Android.",
  },
  "pt-PT": {
    title: "Midiastore: Limpar Espaço",
    shortDescription: "Limpeza leve (< 7 MB): duplicados SHA-256, vídeos grandes e revisão segura.",
    fullDescription:
      "Midiastore é um limpador de armazenamento Android compacto (menos de 7 MB) com processamento 100% local no telemóvel.\n\n" +
      "FUNCIONALIDADES PRINCIPAIS:\n" +
      "• Aplicação Leve (< 7 MB): instala facilmente mesmo com pouco armazenamento disponível.\n" +
      "• Vídeos e Ficheiros Grandes: identifique rapidamente o que ocupa mais espaço.\n" +
      "• Duplicados Exatos (SHA-256): deteta cópias idênticas byte a byte preservando o ficheiro original.\n" +
      "• Ficheiros de Mensagens: reveja vídeos repetidos do WhatsApp, Telegram e Transferências sem apagar conversas.\n" +
      "• Opções Pro: revisão de fotografias semelhantes e compressão de vídeo H.265 por hardware.\n\n" +
      "SEGURANÇA ANDROID:\n" +
      "• Nada é apagado automaticamente sem a sua confirmação explícita no sistema Android.",
  },
  "hi-IN": {
    title: "Midiastore: स्टोरेज क्लीनर",
    shortDescription: "< 7 MB हल्का क्लीनर: सटीक SHA-256 डुप्लीकेट और बड़े वीडियो सुरक्षित हटाएं।",
    fullDescription:
      "Midiastore Android के लिए एक बेहद हल्का (< 7 MB) और सुरक्षित स्टोरेज क्लीनर है, जो 100% आपके फ़ोन पर काम करता है।\n\n" +
      "मुख्य विशेषताएं:\n" +
      "• बेहद हल्का ऐप (< 7 MB): फ़ोन की मेमोरी भरी होने पर भी आसानी से इंस्टॉल होता है।\n" +
      "• बड़े वीडियो और फ़ाइलें: ज़्यादा जगह घेरने वाले वीडियो तुरंत पहचानें।\n" +
      "• सटीक डुप्लीकेट पहचान (SHA-256): असली कॉपी सुरक्षित रखते हुए एक-सी डुप्लीकेट फ़ाइलें ढूंढें।\n" +
      "• चैट मीडिया क्लीनअप: चैट डिलीट किए बिना WhatsApp और Telegram के बार-बार आए वीडियो रिव्यू करें।\n" +
      "• वैकल्पिक Pro टूल्स: मिलते-जुलते फ़ोटो की पहचान और H.265 हार्डवेयर वीडियो कम्प्रेशन।\n\n" +
      "सुरक्षा और गोपनीयता:\n" +
      "• आपकी अनुमति और कन्फ़र्मेशन के बिना कोई भी फ़ाइल अपने आप डिलीट नहीं होती।",
  },
  "ja-JP": {
    title: "Midiastore: 容量・重複ファイル整理",
    shortDescription: "軽量(< 7 MB)クリーナー：SHA-256重複検出と大容量動画を安全に整理。",
    fullDescription:
      "Midiastoreは、端末内100%ローカル処理で動作する軽量（7 MB未満）なAndroid向けストレージ整理アプリです。\n\n" +
      "主な機能：\n" +
      "• 超軽量設計（7 MB未満）：空き容量が残り少ない端末でも素早くインストール可能。\n" +
      "• 大容量動画・ファイルの検出：容量を圧迫している動画を一覧で確認。\n" +
      "• 完全一致の重複検出（SHA-256）：元ファイルを保護しながら同一コピーを正確に検出。\n" +
      "• メッセージアプリのメディア整理：トーク履歴を消さずにLINE・Telegram・WhatsAppの重複動画を安全に確認。\n" +
      "• Pro機能（任意）：類似写真の整理およびH.265ハードウェア動画圧縮。\n\n" +
      "安心のAndroid公式確認：\n" +
      "• 自動削除は一切行いません。すべての削除はユーザーご自身の確認後に行われます。",
  },
  "zh-CN": {
    title: "Midiastore：存储空间与重复清理",
    shortDescription: "< 7 MB轻量清理：SHA-256精准查重与大视频整理，100%本地安全可控。",
    fullDescription:
      "Midiastore 是一款体积小巧（小于 7 MB）、100% 在手机本地运行的 Android 存储空间整理工具。\n\n" +
      "核心清理功能：\n" +
      "• 超轻量安装包（< 7 MB）：即使手机存储接近满载也能快速安装。\n" +
      "• 大视频与大文件查找：快速定位占用空间最多的视频与下载文件。\n" +
      "• SHA-256 精确查重：逐字节校验重复文件并自动保留一份原件。\n" +
      "• 社交缓存媒体整理：在不删除聊天记录的前提下，安全预览微信、Telegram、WhatsApp 的重复媒体。\n" +
      "• 可选 Pro 工具：相似连拍照片筛选与 H.265 硬件视频压缩。\n\n" +
      "安全与隐私保护：\n" +
      "• 绝不自动删除任何文件，每次清理均需您通过 Android 官方弹窗确认。",
  },
  "ar": {
    title: "Midiastore: منظف التخزين",
    shortDescription: "منظف خفيف (< 7 MB): كشف التكرار SHA-256 والفيديوهات الكبيرة بأمان تام.",
    fullDescription:
      "Midiastore هو تطبيق خفيف للغاية (أقل من 7 ميجابايت) لتنظيم وتنظيف مساحة التخزين على أندرويد مع معالجة محلية 100% على جهازك.\n\n" +
      "المزايا الأساسية:\n" +
      "• حجم خفيف جداً (< 7 MB): يُثبّت بسرعة حتى عندما تكون ذاكرة الهاتف شبه ممتلئة.\n" +
      "• الفيديوهات والملفات الكبيرة: اكتشف الملفات التي تستهلك المساحة خلال ثوانٍ.\n" +
      "• الملفات المكررة بدقة (SHA-256): فحص دقيق مع حماية النسخة الأصلية.\n" +
      "• وسائط تطبيقات المراسلة: مراجعة الفيديوهات المكررة في WhatsApp وTelegram دون حذف المحادثات.\n" +
      "• أدوات Pro الاختيارية: فرز الصور المتشابهة وضغط الفيديو بترميز H.265.\n\n" +
      "الأمان والخصوصية:\n" +
      "• لا يتم حذف أي ملف تلقائياً أبداً؛ كل عملية تتطلب تأكيدك الصريح عبر نظام أندرويد.",
  },
  "ru-RU": {
    title: "Midiastore: Очистка Памяти",
    shortDescription: "Легкий клинер (< 7 МБ): точные дубликаты SHA-256 и большие видео.",
    fullDescription:
      "Midiastore — компактное приложение для очистки памяти Android (менее 7 МБ) со 100% локальной обработкой файлов на устройстве.\n\n" +
      "ОСНОВНЫЕ ВОЗМОЖНОСТИ:\n" +
      "• Компактный размер (< 7 МБ): быстрая установка даже при почти заполненной памяти.\n" +
      "• Большие видео и файлы: мгновенный поиск тяжелых медиафайлов.\n" +
      "• Точные дубликаты (SHA-256): побайтовая проверка копий с сохранением оригинала.\n" +
      "• Медиа из мессенджеров: проверка повторяющихся видео из Telegram и WhatsApp без удаления чатов.\n" +
      "• Опции Pro: поиск похожих фото и аппаратное сжатие видео H.265.\n\n" +
      "БЕЗОПАСНОСТЬ ANDROID:\n" +
      "• Никакого автоматического удаления: каждое действие выполняется только после вашего подтверждения.",
  },
  "id": {
    title: "Midiastore: Pembersih Ruang",
    shortDescription: "Pembersih ringan < 7 MB: duplikat SHA-256 akurat & video besar aman.",
    fullDescription:
      "Midiastore adalah pembersih penyimpanan Android super ringan (di bawah 7 MB) dengan analisis 100% lokal di ponsel Anda.\n\n" +
      "FITUR UTAMA:\n" +
      "• Ukuran Ringan (< 7 MB): tetap mudah dipasang saat memori ponsel hampir penuh.\n" +
      "• Video & File Besar: temukan file berukuran besar dalam hitungan detik.\n" +
      "• Duplikat Akurat (SHA-256): mendeteksi salinan identik sambil menjaga satu file asli tetap aman.\n" +
      "• Media Pesan: tinjau kiriman video berulang dari WhatsApp & Telegram tanpa menghapus obrolan.\n" +
      "• Fitur Pro Opsional: tinjauan foto serupa dan kompresi video H.265 berbasis perangkat keras.\n\n" +
      "KEAMANAN RESMI ANDROID:\n" +
      "• Tidak ada penghapusan otomatis: semua file hanya dihapus setelah konfirmasi langsung dari Anda.",
  },
  "bn-BD": {
    title: "Midiastore: স্টোরেজ ক্লিনার",
    shortDescription: "< 7 MB হালকা ক্লিনার: সঠিক SHA-256 ডুপ্লিকেট ও বড় ভিডিও নিরাপদে পরিষ্কার।",
    fullDescription:
      "Midiastore হলো ৭ মেগাবাইটের কম আকারের (< 7 MB) একটি নিরাপদ Android স্টোরেজ ক্লিনার, যা সম্পূর্ণ আপনার ফোনের ভেতরেই কাজ করে।\n\n" +
      "মূল সুবিধাসমূহ:\n" +
      "• অত্যন্ত হালকা (< 7 MB): ফোনের স্টোরেজ প্রায় পূর্ণ থাকলেও দ্রুত ইনস্টল হয়।\n" +
      "• বড় ভিডিও ও ফাইল: বেশি জায়গা নেওয়া ফাইলগুলো সহজেই খুঁজে বের করুন।\n" +
      "• নিখুঁত ডুপ্লিকেট শনাক্তকরণ (SHA-256): মূল ফাইল নিরাপদ রেখে হুবহু কপি শনাক্ত করে।\n" +
      "• মেসেঞ্জার মিডিয়া রিভিউ: চ্যাট মুছে না ফেলে WhatsApp ও Telegram-এর রিপিট ভিডিও পরিষ্কার করুন।\n" +
      "• ঐচ্ছিক Pro টুলস: একই রকম ছবি বাছাই এবং H.265 হার্ডওয়্যার ভিডিও কম্প্রেশন।\n\n" +
      "নিরাপত্তা ও গোপনীয়তা:\n" +
      "• আপনার স্পষ্ট অনুমতি ছাড়া কোনো ফাইল কখনোই একা ডিলিট হয় না।",
  },
  "ko-KR": {
    title: "Midiastore: 저장공간 및 중복 정리",
    shortDescription: "< 7 MB 초경량 클리너: 정확한 SHA-256 중복 파일 및 대용량 동영상 안전 정리.",
    fullDescription:
      "Midiastore는 기기 내 100% 로컬 분석으로 작동하는 7 MB 미만의 초경량 Android 저장공간 정리 앱입니다.\n\n" +
      "주요 정리 기능:\n" +
      "• 초경량 앱 (< 7 MB): 저장공간이 거의 가득 찬 상태에서도 빠르게 설치됩니다.\n" +
      "• 대용량 동영상 및 파일 찾기: 용량을 많이 차지하는 미디어를 즉시 확인합니다.\n" +
      "• 정확한 중복 파일 검사 (SHA-256): 원본을 안전하게 보호하면서 동일한 복사본만 찾아냅니다.\n" +
      "• 메신저 미디어 정리: 대화 내용을 지우지 않고 KakaoTalk, LINE, Telegram, WhatsApp 중복 미디어를 검토합니다.\n" +
      "• 선택형 Pro 기능: 유사 사진 정리 및 H.265 하드웨어 동영상 압축.\n\n" +
      "안전한 사용자 확인:\n" +
      "• 자동 삭제 없음: 모든 파일 삭제는 Android 공식 확인 창을 통해 사용자의 승인을 거칩니다.",
  },
};

// Templates determinísticos rotativos (<= 350 chars) APENAS para reviews 4 e 5 estrelas
const REVIEW_TEMPLATES_4_5 = {
  "pt-BR": [
    "Muito obrigado pela sua avaliação! Ficamos felizes em ajudar a organizar seu Android. Mantemos o Midiastore sempre leve (< 7 MB) e seguro, apagando arquivos apenas com a sua confirmação.",
    "Obrigado pelo apoio! Nosso foco é entregar limpeza real e rápida em menos de 7 MB, sempre com transparência e confirmação explícita do usuário antes de qualquer exclusão.",
  ],
  "en-US": [
    "Thank you for your review! We're glad Midiastore helped free up space. We keep the app compact (< 7 MB) and safe—nothing is ever deleted without your explicit confirmation.",
    "Thanks for the positive feedback! Midiastore stays ultra-light (< 7 MB) and 100% under your control with Android confirmation before removing any file.",
  ],
  "es-ES": [
    "¡Muchas gracias por tu reseña! Nos alegra ayudarte a liberar espacio manteniendo Midiastore ligero (< 7 MB) y seguro, borrando archivos solo tras tu confirmación.",
  ],
  "fr-FR": [
    "Merci beaucoup pour votre avis ! Midiastore reste léger (< 7 Mo) et 100 % sécurisé : aucun fichier n'est supprimé sans votre confirmation explicite.",
  ],
  "de-DE": [
    "Vielen Dank für Ihre Bewertung! Wir halten Midiastore bewusst kompakt (< 7 MB) und sicher – keine Datei wird jemals ohne Ihre ausdrückliche Bestätigung gelöscht.",
  ],
  "it-IT": [
    "Grazie mille per la recensione! Siamo felici di aiutarti a liberare spazio: Midiastore resta leggero (< 7 MB) e sicuro, eliminando i file solo dopo la tua conferma.",
  ],
  "pt-PT": [
    "Muito obrigado pela avaliação! O Midiastore mantém-se leve (< 7 MB) e seguro, eliminando ficheiros apenas após a sua confirmação explícita.",
  ],
  "hi-IN": [
    "आपकी समीक्षा के लिए बहुत धन्यवाद! Midiastore हमेशा हल्का (< 7 MB) और सुरक्षित रहता है — आपकी पुष्टि के बिना कोई भी फ़ाइल कभी डिलीट नहीं की जाती।",
  ],
  "ja-JP": [
    "レビューありがとうございます！Midiastoreは7MB未満の軽量設計を維持し、お客様ご自身の確認なしにファイルを削除しない安全な設計を徹底しております。",
  ],
  "zh-CN": [
    "非常感谢您的好评！Midiastore 始终保持轻量（< 7 MB）与透明安全，任何文件都只会在您明确确认后才会被清理。",
  ],
  "ar": [
    "شكراً جزيلاً لتقييمك! نحرص دائماً على بقاء Midiastore خفيفاً (< 7 MB) وآمناً تماماً، حيث لا يُحذف أي ملف دون تأكيدك الصريح.",
  ],
  "ru-RU": [
    "Большое спасибо за отзыв! Мы сохраняем Midiastore компактным (< 7 МБ) и безопасным: ни один файл не удаляется без вашего подтверждения.",
  ],
  "id": [
    "Terima kasih banyak atas ulasannya! Midiastore tetap ringan (< 7 MB) dan aman—tidak ada file yang dihapus tanpa konfirmasi langsung dari Anda.",
  ],
  "bn-BD": [
    "আপনার সুন্দর রিভিউয়ের জন্য ধন্যবাদ! Midiastore সবসময় হালকা (< 7 MB) এবং নিরাপদ থাকে—আপনার অনুমতি ছাড়া কোনো ফাইল মুছে ফেলা হয় না।",
  ],
  "ko-KR": [
    "소중한 리뷰 감사합니다! Midiastore는 언제나 가볍고(< 7 MB) 안전하게 유지되며, 사용자의 확인 없이는 어떤 파일도 삭제하지 않습니다.",
  ],
};

export function lintAllListings(listingsMap) {
  const report = [];
  let violationsCount = 0;

  for (const [lang, item] of Object.entries(listingsMap)) {
    const titleLen = [...item.title].length;
    const shortLen = [...item.shortDescription].length;
    const fullLen = [...item.fullDescription].length;
    const issues = [];

    if (titleLen > TITLE_MAX) issues.push(`TITLE_TOO_LONG (${titleLen}/${TITLE_MAX})`);
    if (shortLen > SHORT_MAX) issues.push(`SHORT_TOO_LONG (${shortLen}/${SHORT_MAX})`);
    if (fullLen > FULL_MAX) issues.push(`FULL_TOO_LONG (${fullLen}/${FULL_MAX})`);

    if (EMOJI_IN_HEADLINE_RX.test(item.title) || EMOJI_IN_HEADLINE_RX.test(item.shortDescription)) {
      issues.push("EMOJI_IN_TITLE_OR_SHORT_DESC");
    }

    const combined = `${item.title}\n${item.shortDescription}\n${item.fullDescription}`;
    for (const rule of FORBIDDEN_METADATA_PATTERNS) {
      const m = combined.match(rule.rx);
      if (m) issues.push(`${rule.label}: "${m[0]}"`);
    }

    violationsCount += issues.length;
    report.push({
      lang,
      title: item.title,
      titleChars: titleLen,
      shortChars: shortLen,
      fullChars: fullLen,
      ok: issues.length === 0,
      issues,
    });
  }

  return {
    totalLanguages: report.length,
    allPassed: violationsCount === 0 && report.length === 15,
    violationsCount,
    locales: report,
  };
}

async function getPlayAccessToken() {
  const key = JSON.parse(fs.readFileSync(KEY_PATH, "utf8"));
  const now = Math.floor(Date.now() / 1000);
  const hdr = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const pld = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: key.token_uri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");
  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${hdr}.${pld}`);
  const jwt = `${hdr}.${pld}.${sign.sign(key.private_key, "base64url")}`;

  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
    signal: AbortSignal.timeout(15000),
  });
  const data = await res.json();
  if (!data.access_token) throw new Error(`OAuth Play falhou: ${JSON.stringify(data)}`);
  return data.access_token;
}

function routeReviewsDeterministically(reviewsList) {
  const autoRepliesReady = [];
  const manualDraftQueue = [];

  for (let i = 0; i < reviewsList.length; i++) {
    const rev = reviewsList[i];
    const comments = rev.comments || [];
    const userComment = comments.find((c) => c.userComment)?.userComment;
    const hasDevReply = comments.some((c) => c.developerComment);
    if (!userComment || hasDevReply) continue;

    const stars = Number(userComment.starRating || 0);
    const revLang = String(userComment.reviewerLanguage || "en-US");
    const matchedLocale =
      Object.keys(REVIEW_TEMPLATES_4_5).find((k) => k.toLowerCase() === revLang.toLowerCase()) ||
      Object.keys(REVIEW_TEMPLATES_4_5).find((k) => k.split("-")[0] === revLang.split(/[-_]/)[0]) ||
      "en-US";

    if (stars >= 4) {
      const pool = REVIEW_TEMPLATES_4_5[matchedLocale] || REVIEW_TEMPLATES_4_5["en-US"];
      const replyText = pool[i % pool.length].slice(0, REVIEW_REPLY_MAX);
      autoRepliesReady.push({
        reviewId: rev.reviewId,
        authorName: rev.authorName || "",
        stars,
        reviewerLanguage: revLang,
        matchedLocale,
        charCount: [...replyText].length,
        replyText,
      });
    } else {
      manualDraftQueue.push({
        reviewId: rev.reviewId,
        authorName: rev.authorName || "",
        stars,
        reviewerLanguage: revLang,
        userText: userComment.text || "",
        status: "DRAFT_NEEDS_OWNER_APPROVAL",
      });
    }
  }

  return { autoRepliesReady, manualDraftQueue };
}

async function main() {
  const args = new Set(process.argv.slice(2));
  const shouldCommit = args.has("--commit") && !args.has("--dry-run");
  const mode = shouldCommit ? "COMMIT_ERROR_IF_IN_REVIEW" : "DRY_RUN";

  // 1. Linter fail-closed antes de tocar na rede
  const lintReport = lintAllListings(LISTINGS_15_V290);
  if (!lintReport.allPassed) {
    console.error(JSON.stringify({ abortedByLinter: true, lintReport }, null, 2));
    process.exit(1);
  }

  const token = await getPlayAccessToken();

  // 2. Abre edit na Play API
  const editRes = await fetch(`${API_BASE}/edits`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: "{}",
    signal: AbortSignal.timeout(15000),
  });
  const edit = await editRes.json();
  if (!editRes.ok) throw new Error(`Falha ao criar edit Play: ${JSON.stringify(edit)}`);

  let editCommitted = false;
  let editDiscarded = false;
  let commitResponse = null;

  try {
    const liveRes = await fetch(`${API_BASE}/edits/${edit.id}/listings`, {
      headers: { Authorization: `Bearer ${token}` },
      signal: AbortSignal.timeout(15000),
    });
    const liveData = await liveRes.json();
    const liveMap = new Map((liveData.listings || []).map((l) => [l.language, l]));

    const diffs = [];
    for (const [lang, target] of Object.entries(LISTINGS_15_V290)) {
      const current = liveMap.get(lang);
      const actionType = !current
        ? "CREATE_NEW_LOCALE"
        : current.title !== target.title
          ? "EXPAND_TITLE_AND_DESC_V290"
          : "UPDATE_DESC_V290_KEEP_TITLE";

      diffs.push({
        lang,
        actionType,
        oldTitle: current?.title || null,
        newTitle: target.title,
        newTitleChars: [...target.title].length,
        newShortChars: [...target.shortDescription].length,
        newFullChars: [...target.fullDescription].length,
      });

      if (shouldCommit) {
        const putRes = await fetch(`${API_BASE}/edits/${edit.id}/listings/${lang}`, {
          method: "PUT",
          headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
          body: JSON.stringify({
            language: lang,
            title: target.title,
            shortDescription: target.shortDescription,
            fullDescription: target.fullDescription,
          }),
          signal: AbortSignal.timeout(15000),
        });
        if (!putRes.ok) {
          throw new Error(`PUT listing ${lang} falhou (${putRes.status}): ${await putRes.text()}`);
        }
      }
    }

    if (shouldCommit) {
      const cUrl = `${API_BASE}/edits/${edit.id}:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW`;
      const cRes = await fetch(cUrl, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        signal: AbortSignal.timeout(20000),
      });
      const cJson = await cRes.json();
      if (!cRes.ok) {
        throw new Error(`Commit bloqueado (ERROR_IF_IN_REVIEW / HTTP ${cRes.status}): ${JSON.stringify(cJson)}`);
      }
      editCommitted = true;
      commitResponse = cJson;
    } else {
      const delRes = await fetch(`${API_BASE}/edits/${edit.id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
        signal: AbortSignal.timeout(15000),
      });
      editDiscarded = delRes.ok || delRes.status === 204;
    }

    // 3. Consulta reviews ao vivo (somente leitura em --dry-run)
    const revRes = await fetch(`${API_BASE}/reviews?maxResults=50`, {
      headers: { Authorization: `Bearer ${token}` },
      signal: AbortSignal.timeout(15000),
    });
    const revData = revRes.ok ? await revRes.json() : { reviews: [] };
    const routedReviews = routeReviewsDeterministically(revData.reviews || []);

    const outDir = path.join(projectRoot, "docs", "aso");
    fs.mkdirSync(outDir, { recursive: true });
    const queuePath = path.join(outDir, "play-reviews-manual-queue.json");
    fs.writeFileSync(
      queuePath,
      JSON.stringify(
        {
          updatedAt: new Date().toISOString(),
          rule: "1-3 estrelas apenas fila manual; 4-5 estrelas templates deterministicos <=350 chars",
          pendingManualCount: routedReviews.manualDraftQueue.length,
          items: routedReviews.manualDraftQueue,
        },
        null,
        2
      ) + "\n",
      "utf8"
    );

    const expandedTitlesCount = diffs.filter((d) => d.actionType === "EXPAND_TITLE_AND_DESC_V290").length;
    const createdLocales = diffs.filter((d) => d.actionType === "CREATE_NEW_LOCALE").map((d) => d.lang);

    console.log(
      JSON.stringify(
        {
          checkedAt: new Date().toISOString(),
          mode,
          changesInReviewBehavior: "ERROR_IF_IN_REVIEW",
          llmUsed: false,
          editId: edit.id,
          editCommitted,
          editDiscarded,
          commitResponse,
          linterSummary: {
            totalLanguages: lintReport.totalLanguages,
            allPassed: lintReport.allPassed,
            violationsCount: lintReport.violationsCount,
          },
          changesSummary: {
            liveLocalesBefore: liveMap.size,
            targetLocalesAfter: Object.keys(LISTINGS_15_V290).length,
            keptPtBrTitleCount: diffs.filter((d) => d.actionType === "UPDATE_DESC_V290_KEEP_TITLE").length,
            expandedTitlesCount,
            createdLocales,
          },
          diffs,
          reviewsSummary: {
            totalFetched: (revData.reviews || []).length,
            eligibleAutoReply45Stars: routedReviews.autoRepliesReady.length,
            queuedManual13Stars: routedReviews.manualDraftQueue.length,
            manualQueueFile: "docs/aso/play-reviews-manual-queue.json",
          },
        },
        null,
        2
      )
    );
  } catch (err) {
    if (!editCommitted && !editDiscarded) {
      try {
        await fetch(`${API_BASE}/edits/${edit.id}`, {
          method: "DELETE",
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch {}
    }
    throw err;
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((err) => {
    console.error("PLAY_ASO_15_ERR:", err.message);
    process.exit(1);
  });
}
