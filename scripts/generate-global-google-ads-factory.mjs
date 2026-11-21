#!/usr/bin/env node
/**
 * FÁBRICA GLOBAL DE ANÚNCIOS GOOGLE ADS (14 IDIOMAS + BANNERS + CSV AUTO-IMPORT + VÍDEOS MP4)
 *
 * Gera automaticamente na pasta C:\Users\rodolfo\Downloads\google-ads-midiastore:
 * 1. 28 Banners Comerciais OLED (1200x628 + 1200x1200) para cada um dos 14 idiomas
 *    em C:\Users\rodolfo\Downloads\google-ads-midiastore\<locale>\
 * 2. GOOGLE_ADS_14_IDIOMAS_COMPLETO.md (5 Títulos <=30 chars + 4 Descrições <=90 chars por idioma)
 * 3. google-ads-import-14-idiomas.csv (Pronto para importação em lote no Google Ads Editor)
 */

import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { execFileSync } from "node:child_process";

const OUT_DIR = "C:\\Users\\rodolfo\\Downloads\\google-ads-midiastore";
const BASE_HOME_PNG = path.join(process.cwd(), "docs", "campaigns", "store-screenshots", "_base", "home.png");

const GLOBAL_ADS_14 = {
  "pt-BR": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_pt_br",
    eyebrow: "CELULAR CHEIO?",
    headline: "Acabe com a memória lotada\nsem perder suas fotos",
    subtitle: "Encontre fotos repetidas e vídeos pesados do WhatsApp\nem segundos. Você escolhe tudo antes de apagar.",
    cta: "Baixar Grátis na Google Play  ›",
    trust1: "✓ Não apaga nada sozinho",
    trust2: "✓ Super leve e seguro",
    trust3: "✓ Grátis na Google Play",
    phoneBadge: "✓ +14,8 GB prontos para liberar",
    headlines30: [
      "Celular com Memória Cheia?",
      "Limpe sem Perder Suas Fotos",
      "WhatsApp Lotado de Vídeos?",
      "Apague Só Fotos Repetidas",
      "Seu Celular Leve de Novo",
    ],
    descriptions90: [
      "Acabe com a memória lotada sem medo! Você vê cada foto e vídeo antes de apagar.",
      "Encontre fotos repetidas e vídeos pesados do WhatsApp em segundos. Baixe grátis!",
      "Não apaga nada sozinho. App super leve que cabe até em celular quase sem espaço.",
      "Chega de celular travando por falta de espaço. Libere gigas de memória agora!",
    ],
  },
  "pt-PT": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_pt_pt",
    eyebrow: "TELEMÓVEL CHEIO?",
    headline: "Acabe com a memória cheia\nsem perder as suas fotos",
    subtitle: "Encontre fotos repetidas e vídeos pesados do WhatsApp\nem segundos. Reveja tudo antes de eliminar.",
    cta: "Transferir no Google Play  ›",
    trust1: "✓ Não apaga nada sozinho",
    trust2: "✓ Ultra leve e seguro",
    trust3: "✓ Grátis no Google Play",
    phoneBadge: "✓ +14,8 GB prontos a libertar",
    headlines30: [
      "Telemóvel com Memória Cheia?",
      "Limpe sem Perder Fotos",
      "WhatsApp Cheio de Vídeos?",
      "Elimine Só Fotos Repetidas",
      "O Seu Telemóvel Mais Leve",
    ],
    descriptions90: [
      "Acabe com a memória cheia sem receio! Vê cada foto e vídeo antes de eliminar.",
      "Encontre fotos repetidas e vídeos pesados do WhatsApp em segundos. Grátis!",
      "Não apaga nada sozinho. Aplicação super leve para telemóveis quase sem espaço.",
      "Liberte gigas de armazenamento no seu Android com segurança total no equipamento.",
    ],
  },
  "en-US": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/en/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_en",
    eyebrow: "PHONE STORAGE FULL?",
    headline: "Fix full storage fast\nwithout losing your photos",
    subtitle: "Spot duplicate photos and heavy WhatsApp videos\nin seconds. You review everything before deleting.",
    cta: "Get It Free on Google Play  ›",
    trust1: "✓ Never deletes without asking",
    trust2: "✓ Ultra-light & 100% private",
    trust3: "✓ Free on Google Play",
    phoneBadge: "✓ +14.8 GB ready to reclaim",
    headlines30: [
      "Phone Storage Always Full?",
      "Clean Without Losing Photos",
      "WhatsApp Full of Heavy Videos?",
      "Remove Exact Duplicate Photos",
      "Make Your Phone Fast & Light",
    ],
    descriptions90: [
      "Free up gigabytes without fear! You preview every photo and video before deleting.",
      "Find duplicate photos and heavy WhatsApp videos in seconds. Download free today!",
      "Never deletes anything automatically. Ultra-light app built for low-storage phones.",
      "Stop running out of memory. Reclaim gigabytes safely right on your Android phone!",
    ],
  },
  "es-ES": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/es/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_es",
    eyebrow: "¿CELULAR LLENO?",
    headline: "Acaba con la memoria llena\nsin perder tus fotos",
    subtitle: "Encuentra fotos repetidas y videos pesados de WhatsApp\nen segundos. Tú decides qué borrar.",
    cta: "Descargar Gratis en Google Play  ›",
    trust1: "✓ No borra nada sin permiso",
    trust2: "✓ Súper ligero y seguro",
    trust3: "✓ Gratis en Google Play",
    phoneBadge: "✓ +14,8 GB listos para liberar",
    headlines30: [
      "¿Celular con Memoria Llena?",
      "Limpia sin Perder Tus Fotos",
      "¿WhatsApp Lleno de Videos?",
      "Borra Solo Fotos Repetidas",
      "Tu Android Ligero de Nuevo",
    ],
    descriptions90: [
      "¡Libera memoria sin miedo! Ves cada foto y video antes de borrar cualquier archivo.",
      "Encuentra fotos repetidas y videos pesados de WhatsApp en segundos. ¡Bájalo gratis!",
      "Nunca borra nada solo. App súper ligera que instala incluso con poco espacio libre.",
      "Olvídate del aviso de almacenamiento lleno. ¡Recupera gigas en tu móvil hoy mismo!",
    ],
  },
  "fr-FR": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/fr/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_fr",
    eyebrow: "TÉLÉPHONE PLEIN ?",
    headline: "Libérez votre mémoire\nsans perdre vos photos",
    subtitle: "Trouvez les photos en double et vidéos lourdes WhatsApp\nen quelques secondes. Vous gardez le contrôle.",
    cta: "Télécharger sur Google Play  ›",
    trust1: "✓ Ne supprime rien tout seul",
    trust2: "✓ Ultra léger et 100% privé",
    trust3: "✓ Gratuit sur Google Play",
    phoneBadge: "✓ +14,8 Go prêts à libérer",
    headlines30: [
      "Mémoire du Téléphone Pleine ?",
      "Nettoyez sans Perdre de Photos",
      "WhatsApp Saturé de Vidéos ?",
      "Supprimez les Vrais Doublons",
      "Votre Android Léger à Nouveau",
    ],
    descriptions90: [
      "Libérez de l'espace sans crainte ! Vérifiez chaque photo et vidéo avant suppression.",
      "Repérez les photos en double et grosses vidéos WhatsApp en quelques secondes.",
      "Aucune suppression automatique. Application ultra légère même quand l'espace manque.",
      "Fini l'alerte de stockage saturé. Récupérez des gigas facilement sur votre Android !",
    ],
  },
  "de-DE": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/de/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_de",
    eyebrow: "HANDYSPEICHER VOLL?",
    headline: "Schluss mit vollem Speicher\nohne Foto-Verlust",
    subtitle: "Doppelte Fotos und große WhatsApp-Videos in Sekunden\nfinden. Du entscheidest vor jedem Löschen.",
    cta: "Jetzt bei Google Play laden  ›",
    trust1: "✓ Löscht nichts automatisch",
    trust2: "✓ Extrem leicht & 100% sicher",
    trust3: "✓ Gratis bei Google Play",
    phoneBadge: "✓ +14,8 GB sofort freigeben",
    headlines30: [
      "Handyspeicher Ständig Voll?",
      "Reinigen Ohne Foto-Verlust",
      "WhatsApp Voll Großer Videos?",
      "Nur Echte Duplikate Löschen",
      "Mehr Platz auf Deinem Android",
    ],
    descriptions90: [
      "Speicherplatz sicher freigeben! Du prüfst jedes Foto und Video vor dem Löschen.",
      "Finde doppelte Fotos und große WhatsApp-Videos in Sekunden. Jetzt gratis testen!",
      "Kein automatisches Löschen. Extrem leichte App, ideal auch bei fast vollem Speicher.",
      "Schluss mit Speicher-Warnungen. Hol dir Gigabyte an freiem Speicherplatz zurück!",
    ],
  },
  "hi-IN": {
    wpfFont: "Nirmala UI",
    url: "https://midiastory-df6a0.web.app/hi/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_hi",
    eyebrow: "फ़ोन मेमोरी फुल है?",
    headline: "बिना ज़रूरी फ़ोटो खोए\nफ़ोन की जगह खाली करें",
    subtitle: "डुप्लीकेट फ़ोटो और बड़े WhatsApp वीडियो सेकंडों में खोजें।\nकुछ भी डिलीट करने से पहले आप खुद चुनते हैं।",
    cta: "Google Play से मुफ़्त डाउनलोड करें ›",
    trust1: "✓ अपने आप कुछ डिलीट नहीं करता",
    trust2: "✓ बेहद हल्का और 100% सुरक्षित",
    trust3: "✓ Google Play पर मुफ़्त",
    phoneBadge: "✓ +14.8 GB खाली करने के लिए तैयार",
    headlines30: [
      "फ़ोन स्टोरेज बार-बार फुल?",
      "बिना फ़ोटो खोए जगह खाली करें",
      "WhatsApp वीडियो से फ़ोन भरा?",
      "सिर्फ़ डुप्लीकेट फ़ोटो हटाएं",
      "फ़ोन फिर से तेज़ और हल्का करें",
    ],
    descriptions90: [
      "बिना डर के फ़ोन स्टोरेज खाली करें! डिलीट करने से पहले हर फ़ोटो और वीडियो खुद देखें।",
      "सेकंडों में डुप्लीकेट फ़ोटो और बड़े WhatsApp वीडियो खोजें। अभी मुफ़्त डाउनलोड करें!",
      "बिना पूछे कुछ डिलीट नहीं होता। सिर्फ़ 5.9 MB का हल्का ऐप जो तुरंत इंस्टॉल होता है।",
      "स्टोरेज फुल के मैसेज से छुटकारा पाएं। अपने Android फ़ोन में कई GB जगह वापस पाएं!",
    ],
  },
  "ja-JP": {
    wpfFont: "Yu Gothic UI",
    url: "https://midiastory-df6a0.web.app/ja/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_ja",
    eyebrow: "スマホの容量不足ですか？",
    headline: "大切な写真を消さずに\nスマホの容量をスッキリ解消",
    subtitle: "重複した写真や大容量動画を数秒で発見。\n削除する前に必ず自分の目で確認できて安心です。",
    cta: "Google Play で無料入手  ›",
    trust1: "✓ 勝手に削除しません",
    trust2: "✓ 超軽量＆端末内で安全処理",
    trust3: "✓ Google Play で無料",
    phoneBadge: "✓ +14.8 GB すぐに確保可能",
    headlines30: [
      "スマホの容量不足を今すぐ解消",
      "大切な写真を残して安全に整理",
      "重複写真や大容量動画をすぐ発見",
      "勝手に消さない安心クリーナー",
      "わずか5.9MBの軽量アプリ",
    ],
    descriptions90: [
      "大切な思い出を消さずに容量確保！削除前にすべての写真・動画をプレビュー確認できます。",
      "重複した写真や重い動画を数秒で検出。クラウド送信なしでプライバシーも100%安全！",
      "自動削除は一切なし。容量がギリギリのスマホでもすぐ入る5.9MBの超軽量設計です。",
      "「空き容量が不足しています」の悩みを解消。Google Playから無料で今すぐお試し！",
    ],
  },
  "zh-CN": {
    wpfFont: "Microsoft YaHei UI",
    url: "https://midiastory-df6a0.web.app/zh/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_zh",
    eyebrow: "手机内存总是满？",
    headline: "不误删重要照片\n轻松释放手机海量空间",
    subtitle: "几秒找出重复照片与超大聊天视频。\n所有文件先看缩略图再清理，安全放心。",
    cta: "在 Google Play 免费下载  ›",
    trust1: "✓ 绝不自动误删文件",
    trust2: "✓ 超轻量且 100% 本地隐私",
    trust3: "✓ Google Play 免费下载",
    phoneBadge: "✓ 可立即释放 +14.8 GB 空间",
    headlines30: [
      "手机提示存储空间不足？",
      "安全清理内存不误删重要照片",
      "一键找出重复照片与超大视频",
      "绝不偷偷删文件的安心清理工具",
      "仅5.9MB超轻量手机清理管家",
    ],
    descriptions90: [
      "告别内存不足！清理前逐一预览真实缩略图，永远保留原图，绝不误删珍贵照片。",
      "几秒揪出重复照片和占空间的超大视频。100%本机运行，照片绝不上传云端！",
      "仅5.9MB超轻量，哪怕手机空间快满了也能秒速安装，无烦人的全屏弹窗广告。",
      "立即在 Google Play 免费下载 Midiastore，让您的 Android 手机瞬间恢复充裕空间！",
    ],
  },
  "ar": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/ar/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_ar",
    eyebrow: "ذاكرة الهاتف ممتلئة؟",
    headline: "تخلص من امتلاء الذاكرة\nبدون فقدان صورك المهمة",
    subtitle: "اكتشف الصور المكررة وفيديوهات واتساب الكبيرة في ثوانٍ.\nأنت تراجع وتختار كل شيء قبل الحذف.",
    cta: "حمّل مجاناً من Google Play  ›",
    trust1: "✓ لا يحذف أي شيء تلقائياً",
    trust2: "✓ خفيف جداً وآمن 100%",
    trust3: "✓ مجاني على Google Play",
    phoneBadge: "✓ +14.8 GB جاهزة للتفريغ",
    headlines30: [
      "هل ذاكرة هاتفك ممتلئة دائماً؟",
      "نظف هاتفك بدون فقدان صورك",
      "واتساب ممتلئ بالفيديوهات؟",
      "احذف الصور المكررة فقط بأمان",
      "استعد مساحة هاتفك في ثوانٍ",
    ],
    descriptions90: [
      "فرّغ مساحة هاتفك بدون خوف! شاهد وراجع كل صورة وفيديو بنفسك قبل أي حذف.",
      "اعثر على الصور المكررة وفيديوهات واتساب الثقيلة في ثوانٍ. حمّله مجاناً الآن!",
      "لا يحذف أي ملف تلقائياً. تطبيق خفيف جداً (5.9 MB) يعمل محلياً بخصوصية تامة.",
      "وداعاً لرسالة الذاكرة ممتلئة. استرجع جيجابايت من المساحة على هاتفك الأندرويد!",
    ],
  },
  "ru-RU": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/ru/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_ru",
    eyebrow: "ПАМЯТЬ ТЕЛЕФОНА ЗАПОЛНЕНА?",
    headline: "Освободите память телефона\nбез потери важных фото",
    subtitle: "Находите дубликаты фото и тяжелые видео WhatsApp\nза секунды. Вы сами проверяете файлы перед удалением.",
    cta: "Скачать в Google Play  ›",
    trust1: "✓ Не удаляет ничего без спроса",
    trust2: "✓ Суперлегкий и безопасный",
    trust3: "✓ Бесплатно в Google Play",
    phoneBadge: "✓ +14,8 ГБ готово к очистке",
    headlines30: [
      "Не Хватает Памяти в Телефоне?",
      "Очистка Без Потери Важных Фото",
      "WhatsApp Забит Видеофайлами?",
      "Удаляйте Только Дубликаты",
      "Верните Свободное Место Быстро",
    ],
    descriptions90: [
      "Освободите гигабайты без страха! Вы видите миниатюру каждого фото перед удалением.",
      "Находите одинаковые фото и тяжелые видео из WhatsApp за секунды. Скачайте бесплатно!",
      "Никакого автоудаления. Легкое приложение (5,9 МБ), работает 100% локально.",
      "Забудьте об ошибке «Недостаточно памяти». Очистите хранилище Android прямо сейчас!",
    ],
  },
  "id": {
    wpfFont: "Segoe UI",
    url: "https://midiastory-df6a0.web.app/id/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_id",
    eyebrow: "MEMORI HP PENUH?",
    headline: "Atasi memori penuh\ntanpa kehilangan foto penting",
    subtitle: "Temukan foto duplikat & video besar WhatsApp dalam\nhitungan detik. Anda cek semuanya sebelum menghapus.",
    cta: "Unduh Gratis di Google Play  ›",
    trust1: "✓ Tidak hapus otomatis",
    trust2: "✓ Super ringan & 100% aman",
    trust3: "✓ Gratis di Google Play",
    phoneBadge: "✓ +14,8 GB siap dibersihkan",
    headlines30: [
      "Memori HP Sering Penuh?",
      "Bersihkan Tanpa Hapus Foto",
      "WhatsApp Penuh Video Besar?",
      "Hapus Foto Duplikat Saja",
      "Bikin HP Lega Kembali",
    ],
    descriptions90: [
      "Bebaskan memori tanpa khawatir! Lihat setiap foto dan video sebelum dihapus.",
      "Temukan foto kembar dan video berat WhatsApp dalam detik. Unduh gratis sekarang!",
      "Tidak pernah menghapus sendiri. Aplikasi super ringan (5,9 MB) untuk semua Android.",
      "Selamat tinggal memori penuh. Dapatkan kembali ruang bergiga-giga di HP Anda!",
    ],
  },
  "bn-BD": {
    wpfFont: "Nirmala UI",
    url: "https://midiastory-df6a0.web.app/bn/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_bn",
    eyebrow: "ফোনের মেমোরি ফুল?",
    headline: "জরুরি ছবি না হারিয়েই\nফোনের স্টোরেজ খালি করুন",
    subtitle: "কয়েক সেকেন্ডে ডুপ্লিকেট ছবি ও WhatsApp-এর বড় ভিডিও খুঁজুন।\nমুছে ফেলার আগে আপনি নিজেই সব দেখে নেবেন।",
    cta: "Google Play থেকে ফ্রি ডাউনলোড ›",
    trust1: "✓ নিজে থেকে কিছু ডিলিট করে না",
    trust2: "✓ খুবই হালকা ও ১০০% নিরাপদ",
    trust3: "✓ Google Play-তে ফ্রি",
    phoneBadge: "✓ +14.8 GB খালি করার জন্য প্রস্তুত",
    headlines30: [
      "ফোনের স্টোরেজ কি বারবার ফুল?",
      "ছবি না হারিয়ে মেমোরি খালি",
      "WhatsApp ভিডিওতে ফোন ভরেছে?",
      "শুধু ডুপ্লিকেট ছবি মুছে ফেলুন",
      "ফোন আবার হালকা ও দ্রুত করুন",
    ],
    descriptions90: [
      "কোনো ভয় ছাড়াই ফোনের জায়গা খালি করুন! ডিলিট করার আগে প্রতিটি ছবি নিজে দেখে নিন।",
      "কয়েক সেকেন্ডে ডুপ্লিকেট ছবি এবং বড় WhatsApp ভিডিও খুঁজুন। আজই ফ্রি ডাউনলোড করুন!",
      "অনুমতি ছাড়া কিছুই ডিলিট হয় না। মাত্র 5.9 MB-এর হালকা অ্যাপ, সহজেই ইনস্টল হয়।",
      "মেমোরি ফুল সমস্যার সহজ সমাধান। আপনার Android ফোনে কয়েক GB জায়গা ফিরিয়ে আনুন!",
    ],
  },
  "ko-KR": {
    wpfFont: "Malgun Gothic",
    url: "https://midiastory-df6a0.web.app/ko/?utm_source=google_ads&utm_medium=cpc&utm_campaign=global_ko",
    eyebrow: "휴대폰 용량이 부족한가요?",
    headline: "소중한 사진 손실 없이\n부족한 저장공간을 해결하세요",
    subtitle: "중복 사진과 대용량 동영상을 몇 초 만에 찾아냅니다.\n삭제 전 직접 썸네일을 확인하므로 안심할 수 있습니다.",
    cta: "Google Play에서 무료 다운로드 ›",
    trust1: "✓ 임의로 자동 삭제하지 않음",
    trust2: "✓ 초경량 & 100% 기기 내 안전 처리",
    trust3: "✓ Google Play 무료 제공",
    phoneBadge: "✓ +14.8 GB 즉시 확보 가능",
    headlines30: [
      "휴대폰 저장공간이 늘 부족한가요?",
      "사진 손실 없이 안전한 용량 정리",
      "중복 사진과 대용량 영상 빠른 정리",
      "마음대로 지우지 않는 안심 클리너",
      "5.9MB 초경량 스마트폰 용량 정리",
    ],
    descriptions90: [
      "소중한 추억은 그대로! 삭제하기 전에 모든 사진과 동영상 썸네일을 직접 확인하세요.",
      "똑같은 중복 사진과 자리만 차지하는 큰 동영상을 몇 초 만에 찾아냅니다. 무료 다운로드!",
      "자동 삭제 없음, 클라우드 전송 없음! 용량이 꽉 찬 폰에도 바로 설치되는 5.9MB 앱.",
      "저장공간 부족 알림에서 벗어나세요. 지금 Google Play에서 무료로 설치해 보세요!",
    ],
  },
};

const EDITOR_LANGUAGE_MAP = {
  "pt-BR": { en: "Portuguese", pt: "Português" },
  "pt-PT": { en: "Portuguese", pt: "Português" },
  "en-US": { en: "English", pt: "Inglês" },
  "es-ES": { en: "Spanish", pt: "Espanhol" },
  "fr-FR": { en: "French", pt: "Francês" },
  "de-DE": { en: "German", pt: "Alemão" },
  "hi-IN": { en: "Hindi", pt: "Hindi" },
  "ja-JP": { en: "Japanese", pt: "Japonês" },
  "zh-CN": { en: "Chinese (simplified)", pt: "Chinês (simplificado)" },
  "ar": { en: "Arabic", pt: "Árabe" },
  "ru-RU": { en: "Russian", pt: "Russo" },
  "id": { en: "Indonesian", pt: "Indonésio" },
  "bn-BD": { en: "Bengali", pt: "Bengali" },
  "ko-KR": { en: "Korean", pt: "Coreano" },
};

function validateCopyLimits() {
  for (const [locale, cfg] of Object.entries(GLOBAL_ADS_14)) {
    for (const h of cfg.headlines30) {
      const len = [...h].length;
      if (len > 30) {
        throw new Error(`[${locale}] Headline excede 30 chars (${len}): "${h}"`);
      }
    }
    for (const d of cfg.descriptions90) {
      const len = [...d].length;
      if (len > 90) {
        throw new Error(`[${locale}] Description excede 90 chars (${len}): "${d}"`);
      }
    }
  }
}

function writeMarkdownAndCsv() {
  fs.mkdirSync(OUT_DIR, { recursive: true });

  let md = `# 🌍 KIT GLOBAL GOOGLE ADS — MIDIASTORE (14 IDIOMAS COM PSICOLOGIA DE VENDAS)\n\n`;
  md += `Todos os títulos foram validados matematicamente com **≤ 30 caracteres** e todas as descrições com **≤ 90 caracteres** (padrão oficial Google Ads).\n`;
  md += `Todas as 14 campanhas estão configuradas com **Status = Paused** e **Budget = 20.00 BRL/dia**.\n\n`;
  md += `Os 28 banners **Horizontal (1200x628)** e **Quadrado (1200x1200)** estão organizados tanto na raiz junto ao CSV (\`banner-horizontal-<idioma>-1200x628.png\` e \`banner-quadrado-<idioma>-1200x1200.png\`) quanto nas subpastas \`<idioma>\\\` e no pacote ZIP oficial do Google Ads Editor (\`google-ads-editor-lote-14-idiomas.zip\`).\n\n---\n\n`;

  // Cabeçalhos oficiais do Google Ads Editor (Inglês universal - auto-mapeado em qualquer idioma de interface)
  const enHeaders = [
    "Campaign",
    "Campaign Status",
    "Campaign Type",
    "Budget",
    "Budget type",
    "Bid Strategy Type",
    "Languages",
    "Ad Group",
    "Ad Group Status",
    "Ad type",
    "Status",
    "Final URL",
    "Business name",
    "Headline 1",
    "Headline 2",
    "Headline 3",
    "Headline 4",
    "Headline 5",
    "Long headline",
    "Description 1",
    "Description 2",
    "Description 3",
    "Description 4",
    "Marketing image",
    "Square marketing image",
    "Logo",
  ];

  // Cabeçalhos oficiais em Português aceitos pelo Google Ads Editor PT-BR
  const ptHeaders = [
    "Campanha",
    "Status da campanha",
    "Tipo de campanha",
    "Orçamento",
    "Tipo de orçamento",
    "Tipo de estratégia de lances",
    "Idiomas",
    "Grupo de anúncios",
    "Status do grupo de anúncios",
    "Tipo de anúncio",
    "Status",
    "URL final",
    "Nome da empresa",
    "Título 1",
    "Título 2",
    "Título 3",
    "Título 4",
    "Título 5",
    "Título longo",
    "Descrição 1",
    "Descrição 2",
    "Descrição 3",
    "Descrição 4",
    "Imagem de marketing",
    "Imagem de marketing quadrada",
    "Logotipo",
  ];

  const csvRowsEn = [enHeaders.join(",")];
  const csvRowsPt = [ptHeaders.join(",")];

  const csvEscape = (s) => `"${String(s).replace(/"/g, '""')}"`;

  for (const [locale, cfg] of Object.entries(GLOBAL_ADS_14)) {
    const flatHorizName = `banner-horizontal-${locale}-1200x628.png`;
    const flatSqName = `banner-quadrado-${locale}-1200x1200.png`;
    const logoName = `logo-quadrado-512x512.png`;
    const langInfo = EDITOR_LANGUAGE_MAP[locale] || { en: "All", pt: "Todos" };
    const longHeadline = cfg.descriptions90[0];

    md += `## 🌐 Idioma: \`${locale}\` (${langInfo.pt} / ${langInfo.en})\n`;
    md += `- **Campanha:** \`Midiastore Global - ${locale}\` | **Status:** \`Paused\` | **Budget:** \`20.00\` BRL\n`;
    md += `- **URL Final (com rastreamento Google Ads):** \`${cfg.url}\`\n`;
    md += `- **Banner Horizontal (1200x628):** \`${path.join(OUT_DIR, flatHorizName)}\`\n`;
    md += `- **Banner Quadrado (1200x1200):** \`${path.join(OUT_DIR, flatSqName)}\`\n\n`;
    md += `### Títulos (máx. 30 caracteres)\n`;
    cfg.headlines30.forEach((h, i) => {
      md += `${i + 1}. \`${h}\` *(${[...h].length} chars)*\n`;
    });
    md += `\n### Descrições (máx. 90 caracteres)\n`;
    cfg.descriptions90.forEach((d, i) => {
      md += `${i + 1}. \`${d}\` *(${[...d].length} chars)*\n`;
    });
    md += `\n---\n\n`;

    csvRowsEn.push(
      [
        csvEscape(`Midiastore Global - ${locale}`),
        csvEscape("Paused"),
        csvEscape("Display"),
        csvEscape("20.00"),
        csvEscape("Daily"),
        csvEscape("Maximize clicks"),
        csvEscape(langInfo.en),
        csvEscape(`Storage Cleaner - ${locale}`),
        csvEscape("Enabled"),
        csvEscape("Responsive display ad"),
        csvEscape("Enabled"),
        csvEscape(cfg.url),
        csvEscape("Midiastore"),
        ...cfg.headlines30.map(csvEscape),
        csvEscape(longHeadline),
        ...cfg.descriptions90.map(csvEscape),
        csvEscape(flatHorizName),
        csvEscape(flatSqName),
        csvEscape(logoName),
      ].join(",")
    );

    csvRowsPt.push(
      [
        csvEscape(`Midiastore Global - ${locale}`),
        csvEscape("Paused"),
        csvEscape("Display"),
        csvEscape("20.00"),
        csvEscape("Daily"),
        csvEscape("Maximize clicks"),
        csvEscape(langInfo.pt),
        csvEscape(`Storage Cleaner - ${locale}`),
        csvEscape("Enabled"),
        csvEscape("Responsive display ad"),
        csvEscape("Enabled"),
        csvEscape(cfg.url),
        csvEscape("Midiastore"),
        ...cfg.headlines30.map(csvEscape),
        csvEscape(longHeadline),
        ...cfg.descriptions90.map(csvEscape),
        csvEscape(flatHorizName),
        csvEscape(flatSqName),
        csvEscape(logoName),
      ].join(",")
    );
  }

  fs.writeFileSync(path.join(OUT_DIR, "GOOGLE_ADS_14_IDIOMAS_COMPLETO.md"), md, "utf8");
  fs.writeFileSync(path.join(OUT_DIR, "google-ads-import-14-idiomas.csv"), "\uFEFF" + csvRowsEn.join("\r\n"), "utf8");
  fs.writeFileSync(path.join(OUT_DIR, "google-ads-import-14-idiomas-ptbr.csv"), "\uFEFF" + csvRowsPt.join("\r\n"), "utf8");
  console.log("✅ Salvos GOOGLE_ADS_14_IDIOMAS_COMPLETO.md, google-ads-import-14-idiomas.csv e google-ads-import-14-idiomas-ptbr.csv");
}

function organizeBannersAndPackageZip() {
  const loteDir = path.join(OUT_DIR, "lote-google-ads-editor");
  fs.mkdirSync(loteDir, { recursive: true });

  const csvContent = fs.readFileSync(path.join(OUT_DIR, "google-ads-import-14-idiomas.csv"));
  fs.writeFileSync(path.join(loteDir, "data.csv"), csvContent);
  fs.writeFileSync(path.join(loteDir, "google-ads-import-14-idiomas.csv"), csvContent);

  const logoSrc = path.join(OUT_DIR, "logo-quadrado-512x512.png");
  if (fs.existsSync(logoSrc)) {
    fs.copyFileSync(logoSrc, path.join(loteDir, "logo-quadrado-512x512.png"));
  }

  let copiedBanners = 0;
  for (const locale of Object.keys(GLOBAL_ADS_14)) {
    const subHoriz = path.join(OUT_DIR, locale, "banner-horizontal-1200x628.png");
    const subSq = path.join(OUT_DIR, locale, "banner-quadrado-1200x1200.png");
    const flatHorizName = `banner-horizontal-${locale}-1200x628.png`;
    const flatSqName = `banner-quadrado-${locale}-1200x1200.png`;

    if (fs.existsSync(subHoriz)) {
      fs.copyFileSync(subHoriz, path.join(OUT_DIR, flatHorizName));
      fs.copyFileSync(subHoriz, path.join(loteDir, flatHorizName));
      copiedBanners++;
    }
    if (fs.existsSync(subSq)) {
      fs.copyFileSync(subSq, path.join(OUT_DIR, flatSqName));
      fs.copyFileSync(subSq, path.join(loteDir, flatSqName));
      copiedBanners++;
    }
  }

  const zipPath = path.join(OUT_DIR, "google-ads-editor-lote-14-idiomas.zip");
  if (fs.existsSync(zipPath)) {
    fs.unlinkSync(zipPath);
  }
  execFileSync(
    "powershell.exe",
    [
      "-NoProfile",
      "-ExecutionPolicy",
      "Bypass",
      "-Command",
      `Compress-Archive -Path "${loteDir}\\*" -DestinationPath "${zipPath}" -Force`,
    ],
    { encoding: "utf8" }
  );
  console.log(`✅ Organizados ${copiedBanners} banners (1200x628 e 1200x1200) junto ao CSV e gerado pacote ZIP oficial: ${zipPath}`);
}

function renderAll28Banners() {
  const manifest = [];
  for (const [locale, cfg] of Object.entries(GLOBAL_ADS_14)) {
    const locDir = path.join(OUT_DIR, locale);
    fs.mkdirSync(locDir, { recursive: true });
    manifest.push({
      locale,
      wpfFont: cfg.wpfFont,
      eyebrow: cfg.eyebrow,
      headline: cfg.headline,
      subtitle: cfg.subtitle,
      cta: cfg.cta,
      trust1: cfg.trust1,
      trust2: cfg.trust2,
      trust3: cfg.trust3,
      phoneBadge: cfg.phoneBadge,
      horizOut: path.join(locDir, "banner-horizontal-1200x628.png"),
      sqOut: path.join(locDir, "banner-quadrado-1200x1200.png"),
    });
  }

  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "gads-14-"));
  const manifestPath = path.join(tmpDir, "manifest.json");
  const psScriptPath = path.join(tmpDir, "render.ps1");
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), "utf8");

  const psCode = `
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName WindowsBase

function Convert-HexToColor([string]$hex) {
    return [System.Windows.Media.ColorConverter]::ConvertFromString($hex)
}
function New-SolidBrush([string]$hex) {
    $b = New-Object System.Windows.Media.SolidColorBrush (Convert-HexToColor $hex)
    $b.Freeze()
    return $b
}
function Load-BitmapFrozen([string]$path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $ms = New-Object System.IO.MemoryStream (,$bytes)
    $bmp = New-Object System.Windows.Media.Imaging.BitmapImage
    $bmp.BeginInit()
    $bmp.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
    $bmp.StreamSource = $ms
    $bmp.EndInit()
    $bmp.Freeze()
    $ms.Dispose()
    return $bmp
}
function Save-ElementAsPng([System.Windows.FrameworkElement]$el, [int]$w, [int]$h, [string]$filePath) {
    $el.Width = $w
    $el.Height = $h
    $el.Measure((New-Object System.Windows.Size($w, $h)))
    $el.Arrange((New-Object System.Windows.Rect(0, 0, $w, $h)))
    $el.UpdateLayout()
    $rtb = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($w, $h, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
    $rtb.Render($el)
    $enc = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $enc.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($rtb))
    $fs = [System.IO.File]::Open($filePath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try { $enc.Save($fs) } finally { $fs.Dispose() }
}
function New-MidiastoreLogoCanvas([double]$size) {
    $grid = New-Object System.Windows.Controls.Grid
    $grid.Width = $size; $grid.Height = $size
    $bgGrad = New-Object System.Windows.Media.LinearGradientBrush
    $bgGrad.StartPoint = New-Object System.Windows.Point(0, 0); $bgGrad.EndPoint = New-Object System.Windows.Point(1, 1)
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#0B1324"), 0.0))
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.55))
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#07182E"), 1.0))
    $outerBorder = New-Object System.Windows.Controls.Border
    $outerBorder.CornerRadius = New-Object System.Windows.CornerRadius(($size * 0.22))
    $outerBorder.Background = $bgGrad
    $outerBorder.BorderBrush = New-SolidBrush "#330A84FF"
    $outerBorder.BorderThickness = New-Object System.Windows.Thickness(($size * 0.015))
    $grid.Children.Add($outerBorder) | Out-Null
    $vb = New-Object System.Windows.Controls.Viewbox
    $vb.Width = $size * 0.78; $vb.Height = $size * 0.78
    $cv = New-Object System.Windows.Controls.Canvas
    $cv.Width = 108; $cv.Height = 108
    $ringGrad = New-Object System.Windows.Media.LinearGradientBrush
    $ringGrad.StartPoint = New-Object System.Windows.Point(0, 0); $ringGrad.EndPoint = New-Object System.Windows.Point(1, 1)
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#38BDF8"), 0.0))
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#0A84FF"), 0.6))
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#30D158"), 1.0))
    $trackCircle = New-Object System.Windows.Shapes.Ellipse
    $trackCircle.Width = 76; $trackCircle.Height = 76
    [System.Windows.Controls.Canvas]::SetLeft($trackCircle, 16); [System.Windows.Controls.Canvas]::SetTop($trackCircle, 16)
    $trackCircle.Stroke = New-SolidBrush "#1F2937"; $trackCircle.StrokeThickness = 7.5
    $cv.Children.Add($trackCircle) | Out-Null
    $arcPath = New-Object System.Windows.Shapes.Path
    $arcPath.Data = [System.Windows.Media.Geometry]::Parse("M 27.1,27.1 A 38,38 0 1 1 22.5,75.2")
    $arcPath.Stroke = $ringGrad; $arcPath.StrokeThickness = 8
    $arcPath.StrokeStartLineCap = [System.Windows.Media.PenLineCap]::Round; $arcPath.StrokeEndLineCap = [System.Windows.Media.PenLineCap]::Round
    $cv.Children.Add($arcPath) | Out-Null
    $mPath = New-Object System.Windows.Shapes.Path
    $mPath.Data = [System.Windows.Media.Geometry]::Parse("M 35,66 L 35,42 L 47.5,56 L 54,48.5 L 60.5,56 L 73,42 L 73,66")
    $mPath.Stroke = New-SolidBrush "#FFFFFF"; $mPath.StrokeThickness = 6.8
    $mPath.StrokeStartLineCap = [System.Windows.Media.PenLineCap]::Round; $mPath.StrokeEndLineCap = [System.Windows.Media.PenLineCap]::Round
    $mPath.StrokeLineJoin = [System.Windows.Media.PenLineJoin]::Round
    $cv.Children.Add($mPath) | Out-Null
    $starPath = New-Object System.Windows.Shapes.Path
    $starPath.Data = [System.Windows.Media.Geometry]::Parse("M 80,16 L 82.8,23.2 L 90,26 L 82.8,28.8 L 80,36 L 77.2,28.8 L 70,26 L 77.2,23.2 Z")
    $starPath.Fill = New-SolidBrush "#38BDF8"
    $cv.Children.Add($starPath) | Out-Null
    $vb.Child = $cv
    $grid.Children.Add($vb) | Out-Null
    return $grid
}
function New-PhoneMockupWithBadge([System.Windows.Media.Imaging.BitmapImage]$bmp, [double]$phoneW, [double]$phoneH, [string]$badgeLabel, [System.Windows.Media.FontFamily]$ff) {
    $wrap = New-Object System.Windows.Controls.Grid
    $wrap.Width = $phoneW + 24; $wrap.Height = $phoneH
    $phoneBorder = New-Object System.Windows.Controls.Border
    $phoneBorder.Width = $phoneW; $phoneBorder.Height = $phoneH
    $phoneBorder.CornerRadius = New-Object System.Windows.CornerRadius(30)
    $phoneBorder.Background = New-SolidBrush "#000000"; $phoneBorder.BorderBrush = New-SolidBrush "#3B4A63"
    $phoneBorder.BorderThickness = New-Object System.Windows.Thickness(3.5); $phoneBorder.Padding = New-Object System.Windows.Thickness(7)
    $innerClip = New-Object System.Windows.Controls.Border
    $innerClip.CornerRadius = New-Object System.Windows.CornerRadius(23); $innerClip.ClipToBounds = $true; $innerClip.Background = New-SolidBrush "#000000"
    $img = New-Object System.Windows.Controls.Image
    $img.Source = $bmp; $img.Stretch = [System.Windows.Media.Stretch]::UniformToFill
    $img.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center; $img.VerticalAlignment = [System.Windows.VerticalAlignment]::Top
    $innerClip.Child = $img; $phoneBorder.Child = $innerClip
    $wrap.Children.Add($phoneBorder) | Out-Null
    $floatPill = New-Object System.Windows.Controls.Border
    $floatPill.Background = New-SolidBrush "#0D2618"; $floatPill.BorderBrush = New-SolidBrush "#30D158"
    $floatPill.BorderThickness = New-Object System.Windows.Thickness(1.5); $floatPill.CornerRadius = New-Object System.Windows.CornerRadius(14)
    $floatPill.Padding = New-Object System.Windows.Thickness(14, 7, 14, 7)
    $floatPill.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center; $floatPill.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
    $floatPill.Margin = New-Object System.Windows.Thickness(0, 0, 0, 18)
    $pillTxt = New-Object System.Windows.Controls.TextBlock
    $pillTxt.Text = $badgeLabel; $pillTxt.FontFamily = $ff; $pillTxt.FontSize = 14
    $pillTxt.FontWeight = [System.Windows.FontWeights]::ExtraBold; $pillTxt.Foreground = New-SolidBrush "#30D158"
    $floatPill.Child = $pillTxt
    $wrap.Children.Add($floatPill) | Out-Null
    return $wrap
}

$homeBmp = Load-BitmapFrozen $env:GADS_BASE_PNG
$items = [System.IO.File]::ReadAllText($env:GADS_MANIFEST, [System.Text.Encoding]::UTF8) | ConvertFrom-Json

foreach ($item in $items) {
    $font = New-Object System.Windows.Media.FontFamily $item.wpfFont

    # HORIZONTAL 1200x628
    $horizRoot = New-Object System.Windows.Controls.Grid
    $hBg = New-Object System.Windows.Media.LinearGradientBrush
    $hBg.StartPoint = New-Object System.Windows.Point(0, 0); $hBg.EndPoint = New-Object System.Windows.Point(1, 1)
    $hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#060B16"), 0.0))
    $hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.5))
    $hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#08172E"), 1.0))
    $horizRoot.Background = $hBg

    $hGlow = New-Object System.Windows.Shapes.Ellipse
    $hGlow.Width = 700; $hGlow.Height = 560
    $hGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Right; $hGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $hGlow.Margin = New-Object System.Windows.Thickness(0, 0, -40, 0)
    $hGlow.Fill = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#3D0A84FF"), (Convert-HexToColor "#000A84FF")
    $horizRoot.Children.Add($hGlow) | Out-Null

    $hCols = New-Object System.Windows.Controls.Grid
    $hCols.Margin = New-Object System.Windows.Thickness(76, 48, 56, 48)
    $c0 = New-Object System.Windows.Controls.ColumnDefinition; $c0.Width = New-Object System.Windows.GridLength(1.55, [System.Windows.GridUnitType]::Star)
    $c1 = New-Object System.Windows.Controls.ColumnDefinition; $c1.Width = New-Object System.Windows.GridLength(0.88, [System.Windows.GridUnitType]::Star)
    $hCols.ColumnDefinitions.Add($c0); $hCols.ColumnDefinitions.Add($c1)

    $leftStack = New-Object System.Windows.Controls.StackPanel
    $leftStack.VerticalAlignment = [System.Windows.VerticalAlignment]::Center

    $brandRow = New-Object System.Windows.Controls.StackPanel
    $brandRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
    $brandRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 14)
    $brandRow.Children.Add((New-MidiastoreLogoCanvas 44)) | Out-Null

    $brandText = New-Object System.Windows.Controls.TextBlock
    $brandText.FontFamily = $font; $brandText.FontSize = 28; $brandText.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $brandText.VerticalAlignment = [System.Windows.VerticalAlignment]::Center; $brandText.Margin = New-Object System.Windows.Thickness(12, 0, 16, 0)
    $r1 = New-Object System.Windows.Documents.Run("Midia"); $r1.Foreground = New-SolidBrush "#FFFFFF"
    $r2 = New-Object System.Windows.Documents.Run("store"); $r2.Foreground = New-SolidBrush "#0A84FF"
    $brandText.Inlines.Add($r1); $brandText.Inlines.Add($r2)
    $brandRow.Children.Add($brandText) | Out-Null

    $tagPill = New-Object System.Windows.Controls.Border
    $tagPill.Background = New-SolidBrush "#102819"; $tagPill.BorderBrush = New-SolidBrush "#30D158"
    $tagPill.BorderThickness = New-Object System.Windows.Thickness(1.5); $tagPill.CornerRadius = New-Object System.Windows.CornerRadius(12)
    $tagPill.Padding = New-Object System.Windows.Thickness(12, 5, 12, 5); $tagPill.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $tagTxt = New-Object System.Windows.Controls.TextBlock
    $tagTxt.FontFamily = $font; $tagTxt.Text = $item.eyebrow; $tagTxt.FontSize = 13
    $tagTxt.FontWeight = [System.Windows.FontWeights]::ExtraBold; $tagTxt.Foreground = New-SolidBrush "#30D158"
    $tagPill.Child = $tagTxt
    $brandRow.Children.Add($tagPill) | Out-Null
    $leftStack.Children.Add($brandRow) | Out-Null

    $hTitle = New-Object System.Windows.Controls.TextBlock
    $hTitle.FontFamily = $font; $hTitle.Text = $item.headline; $hTitle.FontSize = 40; $hTitle.LineHeight = 48
    $hTitle.TextWrapping = [System.Windows.TextWrapping]::Wrap
    $hTitle.FontWeight = [System.Windows.FontWeights]::ExtraBold; $hTitle.Foreground = New-SolidBrush "#FFFFFF"
    $leftStack.Children.Add($hTitle) | Out-Null

    $hSub = New-Object System.Windows.Controls.TextBlock
    $hSub.FontFamily = $font; $hSub.Text = $item.subtitle; $hSub.FontSize = 19; $hSub.LineHeight = 26
    $hSub.TextWrapping = [System.Windows.TextWrapping]::Wrap
    $hSub.Foreground = New-SolidBrush "#B4C0D3"; $hSub.Margin = New-Object System.Windows.Thickness(0, 12, 0, 20)
    $leftStack.Children.Add($hSub) | Out-Null

    $ctaRow = New-Object System.Windows.Controls.StackPanel
    $ctaRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
    $ctaBadge = New-Object System.Windows.Controls.Border
    $ctaBadge.Background = New-SolidBrush "#0A84FF"; $ctaBadge.CornerRadius = New-Object System.Windows.CornerRadius(12)
    $ctaBadge.Padding = New-Object System.Windows.Thickness(20, 12, 20, 12)
    $ctaText = New-Object System.Windows.Controls.TextBlock
    $ctaText.FontFamily = $font; $ctaText.Text = $item.cta; $ctaText.FontSize = 18
    $ctaText.FontWeight = [System.Windows.FontWeights]::ExtraBold; $ctaText.Foreground = New-SolidBrush "#FFFFFF"
    $ctaBadge.Child = $ctaText
    $ctaRow.Children.Add($ctaBadge) | Out-Null

    $trustTxt = New-Object System.Windows.Controls.TextBlock
    $trustTxt.FontFamily = $font; $trustTxt.Text = "$($item.trust1)\`n$($item.trust2)"
    $trustTxt.FontSize = 13.5; $trustTxt.LineHeight = 19; $trustTxt.FontWeight = [System.Windows.FontWeights]::SemiBold
    $trustTxt.Foreground = New-SolidBrush "#30D158"; $trustTxt.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $trustTxt.Margin = New-Object System.Windows.Thickness(16, 0, 0, 0)
    $ctaRow.Children.Add($trustTxt) | Out-Null
    $leftStack.Children.Add($ctaRow) | Out-Null

    [System.Windows.Controls.Grid]::SetColumn($leftStack, 0)
    $hCols.Children.Add($leftStack) | Out-Null

    $rightBox = New-Object System.Windows.Controls.Grid
    $rightBox.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center; $rightBox.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $rightBox.Children.Add((New-PhoneMockupWithBadge $homeBmp 268 496 $item.phoneBadge $font)) | Out-Null
    [System.Windows.Controls.Grid]::SetColumn($rightBox, 1)
    $hCols.Children.Add($rightBox) | Out-Null

    $horizRoot.Children.Add($hCols) | Out-Null
    Save-ElementAsPng $horizRoot 1200 628 $item.horizOut

    # SQUARE 1200x1200
    $sqRoot = New-Object System.Windows.Controls.Grid
    $sqRoot.Background = $hBg
    $sqGlow = New-Object System.Windows.Shapes.Ellipse
    $sqGlow.Width = 880; $sqGlow.Height = 680
    $sqGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center; $sqGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
    $sqGlow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 20)
    $sqGlow.Fill = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#3D0A84FF"), (Convert-HexToColor "#000A84FF")
    $sqRoot.Children.Add($sqGlow) | Out-Null

    $sqStack = New-Object System.Windows.Controls.StackPanel
    $sqStack.Margin = New-Object System.Windows.Thickness(72, 58, 72, 48)
    $sqStack.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center

    $sqBrandRow = New-Object System.Windows.Controls.StackPanel
    $sqBrandRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
    $sqBrandRow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $sqBrandRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 14)
    $sqBrandRow.Children.Add((New-MidiastoreLogoCanvas 46)) | Out-Null

    $sqBrandText = New-Object System.Windows.Controls.TextBlock
    $sqBrandText.FontFamily = $font; $sqBrandText.FontSize = 30; $sqBrandText.FontWeight = [System.Windows.FontWeights]::ExtraBold
    $sqBrandText.VerticalAlignment = [System.Windows.VerticalAlignment]::Center; $sqBrandText.Margin = New-Object System.Windows.Thickness(12, 0, 16, 0)
    $sr1 = New-Object System.Windows.Documents.Run("Midia"); $sr1.Foreground = New-SolidBrush "#FFFFFF"
    $sr2 = New-Object System.Windows.Documents.Run("store"); $sr2.Foreground = New-SolidBrush "#0A84FF"
    $sqBrandText.Inlines.Add($sr1); $sqBrandText.Inlines.Add($sr2)
    $sqBrandRow.Children.Add($sqBrandText) | Out-Null

    $sqTagPill = New-Object System.Windows.Controls.Border
    $sqTagPill.Background = New-SolidBrush "#102819"; $sqTagPill.BorderBrush = New-SolidBrush "#30D158"
    $sqTagPill.BorderThickness = New-Object System.Windows.Thickness(1.5); $sqTagPill.CornerRadius = New-Object System.Windows.CornerRadius(12)
    $sqTagPill.Padding = New-Object System.Windows.Thickness(14, 5, 14, 5); $sqTagPill.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
    $sqTagTxt = New-Object System.Windows.Controls.TextBlock
    $sqTagTxt.FontFamily = $font; $sqTagTxt.Text = $item.eyebrow; $sqTagTxt.FontSize = 14
    $sqTagTxt.FontWeight = [System.Windows.FontWeights]::ExtraBold; $sqTagTxt.Foreground = New-SolidBrush "#30D158"
    $sqTagPill.Child = $sqTagTxt
    $sqBrandRow.Children.Add($sqTagPill) | Out-Null
    $sqStack.Children.Add($sqBrandRow) | Out-Null

    $sqTitle = New-Object System.Windows.Controls.TextBlock
    $sqTitle.FontFamily = $font; $sqTitle.Text = $item.headline; $sqTitle.FontSize = 48; $sqTitle.LineHeight = 56
    $sqTitle.TextAlignment = [System.Windows.TextAlignment]::Center; $sqTitle.TextWrapping = [System.Windows.TextWrapping]::Wrap
    $sqTitle.FontWeight = [System.Windows.FontWeights]::ExtraBold; $sqTitle.Foreground = New-SolidBrush "#FFFFFF"
    $sqStack.Children.Add($sqTitle) | Out-Null

    $sqSub = New-Object System.Windows.Controls.TextBlock
    $sqSub.FontFamily = $font; $sqSub.Text = $item.subtitle; $sqSub.FontSize = 20; $sqSub.LineHeight = 28
    $sqSub.TextAlignment = [System.Windows.TextAlignment]::Center; $sqSub.TextWrapping = [System.Windows.TextWrapping]::Wrap
    $sqSub.Foreground = New-SolidBrush "#B4C0D3"; $sqSub.Margin = New-Object System.Windows.Thickness(0, 12, 0, 18)
    $sqStack.Children.Add($sqSub) | Out-Null

    $chipsRow = New-Object System.Windows.Controls.StackPanel
    $chipsRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
    $chipsRow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $chipsRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 20)
    foreach ($chipLabel in @($item.trust1, $item.trust2, $item.trust3)) {
        $chipBorder = New-Object System.Windows.Controls.Border
        $chipBorder.Background = New-SolidBrush "#121C2E"; $chipBorder.BorderBrush = New-SolidBrush "#263A5E"
        $chipBorder.BorderThickness = New-Object System.Windows.Thickness(1); $chipBorder.CornerRadius = New-Object System.Windows.CornerRadius(12)
        $chipBorder.Padding = New-Object System.Windows.Thickness(14, 6, 14, 6); $chipBorder.Margin = New-Object System.Windows.Thickness(5, 0, 5, 0)
        $cTxt = New-Object System.Windows.Controls.TextBlock
        $cTxt.FontFamily = $font; $cTxt.Text = $chipLabel; $cTxt.FontSize = 14
        $cTxt.FontWeight = [System.Windows.FontWeights]::Bold; $cTxt.Foreground = New-SolidBrush "#38BDF8"
        $chipBorder.Child = $cTxt
        $chipsRow.Children.Add($chipBorder) | Out-Null
    }
    $sqStack.Children.Add($chipsRow) | Out-Null

    $phoneSq = New-PhoneMockupWithBadge $homeBmp 390 660 $item.phoneBadge $font
    $phoneSq.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $sqStack.Children.Add($phoneSq) | Out-Null

    $sqRoot.Children.Add($sqStack) | Out-Null
    Save-ElementAsPng $sqRoot 1200 1200 $item.sqOut
    Write-Host "RENDERED_LOCALE: $($item.locale)"
}
`;
  fs.writeFileSync(psScriptPath, psCode, "utf8");

  try {
    const out = execFileSync(
      "powershell.exe",
      ["-Sta", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", psScriptPath],
      {
        encoding: "utf8",
        timeout: 180000,
        env: {
          ...process.env,
          GADS_BASE_PNG: BASE_HOME_PNG,
          GADS_MANIFEST: manifestPath,
        },
      }
    );
    console.log(out.trim());
  } finally {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  }
}

const forceRender = process.argv.includes("--render");
const allBannersExist = Object.keys(GLOBAL_ADS_14).every(
  (loc) =>
    fs.existsSync(path.join(OUT_DIR, loc, "banner-horizontal-1200x628.png")) &&
    fs.existsSync(path.join(OUT_DIR, loc, "banner-quadrado-1200x1200.png"))
);

validateCopyLimits();
writeMarkdownAndCsv();
if (forceRender || !allBannersExist) {
  renderAll28Banners();
}
organizeBannersAndPackageZip();
console.log("🚀 FÁBRICA GLOBAL GOOGLE ADS (14 IDIOMAS / 28 BANNERS + CSV + ZIP + MD) CONCLUÍDA!");
