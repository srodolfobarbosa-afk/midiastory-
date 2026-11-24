#!/usr/bin/env node
/**
 * build-bigtech-landing-14langs.mjs
 *
 * Generates the approved Big-Tech OLED Landing Page (Opção 1: Precision Storage Ring, zero AI sparkles,
 * zero emojis, zero legacy clutter) across all 13 web language routes (/ + /en/, /es/, /ar/, /ru/, /hi/,
 * /id/, /bn/, /ko/, /de/, /fr/, /ja/, /zh/), preserving 100% of:
 *   - Instagram / TikTok / Shorts UTM capture & Google Play referrer pass-through
 *   - Supabase ingest-product-event beacon telemetry & public.beta_testers opt-in drawer
 *   - All 14 hreflang alternates + Schema.org JSON-LD (SoftwareApplication, WebSite, FAQPage)
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const DOCS = path.join(ROOT, "docs");
const BASE_URL = "https://midiastory-df6a0.web.app";
const SUPABASE_URL = "https://usmqtnlsaavxqguwrtix.supabase.co";
const ANON_KEY =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzbXF0bmxzYWF2eHFndXdydGl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNzcwNTQsImV4cCI6MjEwOTY1MzA1NH0.N7XIpn3lGJM7V8clfU8uKe-7v-N3qKPkIgv3sCKv2VU";

const HREFLANGS = [
  ["pt-BR", "/"],
  ["en", "/en/"],
  ["es", "/es/"],
  ["ar", "/ar/"],
  ["ru", "/ru/"],
  ["hi", "/hi/"],
  ["id", "/id/"],
  ["bn", "/bn/"],
  ["ko", "/ko/"],
  ["de", "/de/"],
  ["fr", "/fr/"],
  ["ja", "/ja/"],
  ["zh-CN", "/zh/"],
  ["x-default", "/en/"],
];

const NAV_LANGS = [
  ["PT", "/"],
  ["EN", "/en/"],
  ["ES", "/es/"],
  ["AR", "/ar/"],
  ["RU", "/ru/"],
  ["HI", "/hi/"],
  ["ID", "/id/"],
  ["BN", "/bn/"],
  ["KO", "/ko/"],
  ["DE", "/de/"],
  ["FR", "/fr/"],
  ["JA", "/ja/"],
  ["ZH", "/zh/"],
];

const LOCALES = {
  pt: {
    outRel: "index.html",
    urlPath: "/",
    htmlLang: "pt-BR",
    rtl: false,
    currency: "BRL",
    privacyHref: "/privacidade.html",
    privacyLabel: "Política de Privacidade",
    guidesLabel: "Guias Android",
    title: "Midiastore — Limpador de Armazenamento Android OLED (< 8 MB) & SHA-256",
    desc: "Armazenamento limpo com precisão cirúrgica no Android. Localize vídeos pesados (>10 MB), fotos parecidas, WhatsApp e cópias exatas SHA-256. 100% no aparelho e sem anúncios.",
    socialBanner: "Acesso direto liberado — limpe vídeos pesados e fotos repetidas no seu Android em 2 toques (< 8 MB).",
    eyebrow: "ENGENHARIA ON-DEVICE • ZERO NUVEM • ANDROID 10+",
    h1: "Armazenamento limpo com precisão cirúrgica.",
    lead: "Localize vídeos pesados, fotos parecidas, mídias de mensageiros e cópias idênticas SHA-256 diretamente no processador do seu Android. Sem anúncios, sem enviar fotos à nuvem e em apenas 7,2 MB.",
    ctaPrimary: "Baixar Grátis na Google Play",
    ctaSecondary: "Lista VIP de Acesso",
    pills: ["7,2 MB OLED", "Hash Criptográfico SHA-256", "100% No Aparelho", "Zero Anúncios"],
    cardEyebrow: "ESPAÇO RECUPERÁVEL",
    cardSub: "Revisão visual segura com confirmação nativa do Android",
    cardBtn: "Revisar Arquivos • 12,4 GB",
    mini1Label: "Duplicatas",
    mini1Val: "SHA-256",
    mini2Label: "Vídeos >10 MB",
    mini2Val: "8,2 GB",
    bento: [
      ["Verificação Byte a Byte (SHA-256)", "Enquanto limpadores comuns arriscam apagar arquivos pelo nome, o Midiastore calcula o hash SHA-256 em streaming e mantém sempre 1 original protegido."],
      ["Vídeos Grandes & Mensageiros", "Encontra em segundos vídeos acima de 10 MB, capturas antigas (+30 dias) e mídias acumuladas do WhatsApp e Telegram sem sair do aparelho."],
      ["Privacidade Nível Big Tech", "Zero permissão invasiva (sem MANAGE_EXTERNAL_STORAGE), zero ID de publicidade e suporte oficial à Lixeira do Android 11+ (30 dias)."],
    ],
    proTitle: "Planos Transparentes na Google Play",
    proSub: "Análise essencial, duplicatas SHA-256, vídeos grandes e Lixeira Android são gratuitos. O Midiastore Pro libera Apps sem Uso (+90d) e diagnóstico local de Fotos Borradas.",
    plans: [
      ["Grátis", "R$ 0", "Para sempre", "Duplicatas SHA-256, vídeos >10 MB, mídias +30 dias e Lixeira oficial Android."],
      ["Anual Pro (-56%)", "R$ 79,99", "por ano", "Mais escolhido. Inclui Apps sem Uso (+90d), Fotos Borradas On-Device e todas as evoluções Pro."],
      ["Vitalício Pro", "R$ 149,99", "pagamento único", "Acesso definitivo vinculado à sua conta Google Play, sem mensalidades."],
    ],
    vipTitle: "Acesso Direto & Avisos de Atualização",
    vipDesc: "Quer receber o link direto na sua conta Google Play? Informe seu Gmail abaixo ou toque em Baixar na Google Play.",
    vipPlaceholder: "Seu e-mail Google Play (nome@gmail.com)",
    vipBtn: "Registrar Gmail",
    vipOk: "E-mail registrado com sucesso. Abra o link da Google Play acima para instalar.",
    faq1Q: "O Midiastore funciona 100% no aparelho sem enviar fotos para a nuvem?",
    faq1A: "Sim. Toda a leitura de armazenamento, miniaturas e cálculo criptográfico SHA-256 roda localmente no seu Android via Scoped Storage.",
    faq2Q: "O Midiastore apaga arquivos sozinho?",
    faq2A: "Nunca. Você revisa as miniaturas, 1 cópia original fica sempre protegida e toda remoção exige sua confirmação no diálogo oficial do Android.",
  },
  en: {
    outRel: "en/index.html",
    urlPath: "/en/",
    htmlLang: "en",
    rtl: false,
    currency: "USD",
    privacyHref: "/privacy.html",
    privacyLabel: "Privacy Policy",
    guidesLabel: "Storage Guides",
    title: "Midiastore — Precision On-Device Android Storage Cleaner (< 8 MB)",
    desc: "Clean Android storage with surgical precision. Surface large videos (>10 MB), similar photos, chat media, and byte-exact SHA-256 duplicates. 100% on-device, zero ads.",
    socialBanner: "Direct access unlocked — review heavy videos & exact duplicate photos on Android in 2 taps (< 8 MB).",
    eyebrow: "ON-DEVICE ENGINEERING • ZERO CLOUD • ANDROID 10+",
    h1: "Clean Android storage with surgical precision.",
    lead: "Surface heavy videos, burst photos, messenger downloads, and byte-identical SHA-256 duplicates right on your device processor. Zero ads, zero cloud uploads, and only 7.2 MB.",
    ctaPrimary: "Download Free on Google Play",
    ctaSecondary: "Priority Access",
    pills: ["7.2 MB Pure OLED", "Exact SHA-256 Hash", "100% On-Device", "Zero Ads"],
    cardEyebrow: "RECOVERABLE STORAGE",
    cardSub: "Safe visual review with official Android confirmation",
    cardBtn: "Review Files • 12.4 GB",
    mini1Label: "Duplicates",
    mini1Val: "SHA-256",
    mini2Label: "Videos >10 MB",
    mini2Val: "8.2 GB",
    bento: [
      ["Byte-Exact Verification (SHA-256)", "While generic cleaners guess by file name, Midiastore streams cryptographic SHA-256 hashes and always keeps one original protected."],
      ["Large Videos & Chat Media", "Instantly surface videos over 10 MB, old screenshots (+30 days), and accumulated chat downloads without uploading a single byte."],
      ["Big-Tech Grade Privacy", "Zero broad file permissions (no MANAGE_EXTERNAL_STORAGE), zero Advertising ID, and native Android 11+ 30-day Trash support."],
    ],
    proTitle: "Transparent Google Play Pricing",
    proSub: "Core storage scan, SHA-256 duplicates, large videos, and Android Trash are free forever. Midiastore Pro unlocks Unused Apps (90d+) and On-Device Blurry Photo Review.",
    plans: [
      ["Free", "$0", "Forever", "SHA-256 exact duplicates, large videos (>10 MB), old media (+30d), and Android Trash."],
      ["Annual Pro (-56%)", "Google Play", "Best value / year", "Most popular. Unlocks Unused Apps (90d+), On-Device Blurry Photo Review, and future Pro engines."],
      ["Lifetime Pro", "Google Play", "One-time purchase", "Permanent entitlement tied to your Google Play account with zero recurring fees."],
    ],
    vipTitle: "Direct Google Play Access",
    vipDesc: "Enter your Google Play Gmail for priority release notifications or tap Download on Google Play above.",
    vipPlaceholder: "Your Google Play email (name@gmail.com)",
    vipBtn: "Save Email",
    vipOk: "Email saved. Tap Download on Google Play above to install.",
    faq1Q: "Does Midiastore run 100% on-device without uploading my photos?",
    faq1A: "Yes. Every storage scan, thumbnail preview, and SHA-256 duplicate check executes strictly locally via Android Scoped Storage.",
    faq2Q: "Does Midiastore ever delete files automatically?",
    faq2A: "Never. You inspect real thumbnails, one original copy stays protected, and every action uses Android's official confirmation dialog.",
  },
  es: {
    outRel: "es/index.html",
    urlPath: "/es/",
    htmlLang: "es",
    rtl: false,
    currency: "USD",
    privacyHref: "/privacy.html",
    privacyLabel: "Política de Privacidad",
    guidesLabel: "Guías Android",
    title: "Midiastore — Limpiador de Almacenamiento Android OLED (< 8 MB) y SHA-256",
    desc: "Limpia el almacenamiento de tu Android con precisión quirúrgica: videos grandes (>10 MB), WhatsApp y duplicados exactos SHA-256. 100% en tu móvil, cero anuncios.",
    socialBanner: "Acceso directo — libera videos pesados y fotos repetidas en tu Android en 2 toques (< 8 MB).",
    eyebrow: "INGENIERÍA EN EL DISPOSITIVO • CERO NUBE • ANDROID 10+",
    h1: "Almacenamiento limpio con precisión quirúrgica.",
    lead: "Detecta videos pesados, capturas antiguas, descargas de mensajería y copias exactas SHA-256 directamente en tu Android. Sin anuncios, sin nube y en solo 7,2 MB.",
    ctaPrimary: "Descargar Gratis en Google Play",
    ctaSecondary: "Acceso Prioritario",
    pills: ["7,2 MB OLED", "Hash Exacto SHA-256", "100% Local", "Cero Anuncios"],
    cardEyebrow: "ESPACIO RECUPERABLE",
    cardSub: "Revisión visual segura con confirmación oficial de Android",
    cardBtn: "Revisar Archivos • 12,4 GB",
    mini1Label: "Duplicados",
    mini1Val: "SHA-256",
    mini2Label: "Videos >10 MB",
    mini2Val: "8,2 GB",
    bento: [
      ["Verificación Byte a Byte (SHA-256)", "Calcula hashes criptográficos SHA-256 en streaming y mantiene siempre 1 copia original protegida."],
      ["Videos Grandes y Mensajería", "Localiza al instante videos mayores de 10 MB, capturas (+30 días) y archivos de WhatsApp/Telegram localmente."],
      ["Privacidad Nivel Big Tech", "Sin MANAGE_EXTERNAL_STORAGE, sin ID de publicidad y con soporte nativo para la Papelera de Android."],
    ],
    proTitle: "Planes Transparentes en Google Play",
    proSub: "Limpieza esencial gratuita para siempre. Midiastore Pro desbloquea Apps sin Uso (+90d) y revisión local de Fotos Borrosas.",
    plans: [
      ["Gratis", "$0", "Para siempre", "Duplicados SHA-256, videos >10 MB, archivos +30 días y Papelera Android."],
      ["Anual Pro (-56%)", "Google Play", "Mejor valor anual", "Incluye Apps sin Uso (+90d) y detección local de Fotos Borrosas."],
      ["Vitalicio Pro", "Google Play", "Pago único", "Acceso permanente vinculado a tu cuenta de Google Play."],
    ],
    vipTitle: "Acceso Directo en Google Play",
    vipDesc: "Registra tu Gmail de Google Play o pulsa Descargar en Google Play arriba.",
    vipPlaceholder: "Tu correo Gmail (nombre@gmail.com)",
    vipBtn: "Registrar",
    vipOk: "Correo guardado correctamente.",
    faq1Q: "¿Midiastore funciona 100% en el dispositivo sin subir fotos?",
    faq1A: "Sí. Todo el análisis y verificación SHA-256 ocurre localmente en tu teléfono mediante Scoped Storage.",
    faq2Q: "¿Borra archivos automáticamente?",
    faq2A: "Nunca. Siempre revisas miniaturas reales y confirmas en el diálogo oficial de Android.",
  },
  fr: {
    outRel: "fr/index.html",
    urlPath: "/fr/",
    htmlLang: "fr",
    rtl: false,
    currency: "EUR",
    privacyHref: "/privacy.html",
    privacyLabel: "Politique de Confidentialité",
    guidesLabel: "Guides",
    title: "Midiastore — Nettoyeur de Stockage Android OLED (< 8 Mo) & SHA-256",
    desc: "Nettoyez le stockage Android avec une précision chirurgicale : vidéos lourdes (>10 Mo) et doublons exacts SHA-256. 100 % sur l'appareil, sans publicité.",
    socialBanner: "Accès direct — libérez de l'espace sur votre Android en 2 gestes (< 8 Mo).",
    eyebrow: "INGÉNIERIE LOCALE • ZÉRO CLOUD • ANDROID 10+",
    h1: "Un stockage propre avec une précision chirurgicale.",
    lead: "Repérez les vidéos volumineuses, captures anciennes et doublons exacts SHA-256 directement sur votre téléphone. Zéro pub, zéro cloud, seulement 7,2 Mo.",
    ctaPrimary: "Télécharger sur Google Play",
    ctaSecondary: "Accès Prioritaire",
    pills: ["7,2 Mo OLED", "Empreinte SHA-256", "100 % Hors Ligne", "Zéro Pub"],
    cardEyebrow: "ESPACE RÉCUPÉRABLE",
    cardSub: "Vérification visuelle avec confirmation officielle Android",
    cardBtn: "Vérifier • 12,4 Go",
    mini1Label: "Doublons",
    mini1Val: "SHA-256",
    mini2Label: "Vidéos >10 Mo",
    mini2Val: "8,2 Go",
    bento: [
      ["Vérification Octet par Octet (SHA-256)", "Compare chaque octet par hachage SHA-256 et protège systématiquement un fichier original."],
      ["Vidéos Lourdes & Messageries", "Détecte instantanément les vidéos >10 Mo et médias anciens (+30 jours) sans aucun envoi cloud."],
      ["Confidentialité Absolue", "Aucune permission abusive, aucun identifiant publicitaire et support natif de la Corbeille Android."],
    ],
    proTitle: "Tarifs Transparents sur Google Play",
    proSub: "Analyse essentielle gratuite. Midiastore Pro active les Applications Inutilisées (+90j) et la détection locale des Photos Floues.",
    plans: [
      ["Gratuit", "0 €", "Pour toujours", "Doublons SHA-256, vidéos >10 Mo, médias +30j et Corbeille Android."],
      ["Annuel Pro (-56%)", "Google Play", "Meilleur choix", "Applications inutilisées (+90j) et diagnostic local des photos floues."],
      ["À Vie Pro", "Google Play", "Paiement unique", "Accès permanent lié à votre compte Google Play."],
    ],
    vipTitle: "Accès Direct Google Play",
    vipDesc: "Indiquez votre adresse Gmail Google Play ou cliquez sur Télécharger ci-dessus.",
    vipPlaceholder: "Votre adresse Gmail",
    vipBtn: "Enregistrer",
    vipOk: "Adresse enregistrée avec succès.",
    faq1Q: "Midiastore fonctionne-t-il à 100 % sur mon téléphone ?",
    faq1A: "Oui. Toutes les analyses et empreintes SHA-256 restent strictement locales via Android Scoped Storage.",
    faq2Q: "L'application supprime-t-elle des fichiers toute seule ?",
    faq2A: "Jamais. Chaque action requiert votre confirmation dans la boîte de dialogue officielle d'Android.",
  },
  de: {
    outRel: "de/index.html",
    urlPath: "/de/",
    htmlLang: "de",
    rtl: false,
    currency: "EUR",
    privacyHref: "/privacy.html",
    privacyLabel: "Datenschutz",
    guidesLabel: "Ratgeber",
    title: "Midiastore — Präziser OLED Speicherreiniger für Android (< 8 MB)",
    desc: "Android-Speicher mit chirurgischer Präzision bereinigen: große Videos (>10 MB) und exakte SHA-256-Duplikate. 100 % lokal auf dem Gerät, werbefrei.",
    socialBanner: "Direktzugang — große Videos & exakte Duplikate in 2 Tipps sicher prüfen (< 8 MB).",
    eyebrow: "ON-DEVICE-TECHNOLOGIE • OHNE CLOUD • ANDROID 10+",
    h1: "Sauberer Speicher mit chirurgischer Präzision.",
    lead: "Finden Sie große Videos, alte Screenshots und bitgenaue SHA-256-Duplikate direkt auf Ihrem Smartphone. Werbefrei, ohne Cloud-Upload und nur 7,2 MB groß.",
    ctaPrimary: "Kostenlos bei Google Play",
    ctaSecondary: "Prioritätszugang",
    pills: ["7,2 MB OLED", "Exakter SHA-256-Hash", "100 % Lokal", "Werbefrei"],
    cardEyebrow: "FREIGEBEN MÖGLICH",
    cardSub: "Sichere Vorschau mit offiziellem Android-Systemdialog",
    cardBtn: "Dateien prüfen • 12,4 GB",
    mini1Label: "Duplikate",
    mini1Val: "SHA-256",
    mini2Label: "Videos >10 MB",
    mini2Val: "8,2 GB",
    bento: [
      ["Bitgenauer SHA-256-Abgleich", "Prüft Dateien per kryptografischem SHA-256-Streaming und schützt immer ein Original."],
      ["Große Videos & Chats", "Erkennt Videos über 10 MB und alte Medien (+30 Tage) sekundenschnell direkt auf dem Gerät."],
      ["Datenschutz auf Big-Tech-Niveau", "Kein MANAGE_EXTERNAL_STORAGE, keine Werbe-ID und nativer Android-Papierkorb."],
    ],
    proTitle: "Transparente Pläne bei Google Play",
    proSub: "Basis-Reinigung dauerhaft kostenlos. Midiastore Pro schaltet ungenutzte Apps (+90T) und lokale Unschärfe-Prüfung frei.",
    plans: [
      ["Kostenlos", "0 €", "Dauerhaft", "SHA-256-Duplikate, große Videos (>10 MB) und Android-Papierkorb."],
      ["Jahres-Pro (-56%)", "Google Play", "Bester Wert", "Ungenutzte Apps (+90T) und lokale Prüfung unscharfer Fotos."],
      ["Lifetime Pro", "Google Play", "Einmalzahlung", "Dauerhafte Freischaltung über Ihr Google Play-Konto."],
    ],
    vipTitle: "Direkter Google Play-Zugang",
    vipDesc: "Geben Sie Ihre Google Play-Gmail-Adresse ein oder tippen Sie oben auf Google Play.",
    vipPlaceholder: "Ihre Gmail-Adresse",
    vipBtn: "Speichern",
    vipOk: "E-Mail erfolgreich gespeichert.",
    faq1Q: "Arbeitet Midiastore zu 100 % lokal ohne Cloud-Upload?",
    faq1A: "Ja. Alle Analysen, Vorschauen und SHA-256-Prüfungen erfolgen ausschließlich lokal auf Ihrem Android-Gerät.",
    faq2Q: "Löscht die App jemals Dateien automatisch?",
    faq2A: "Nein. Jede Löschung erfordert Ihre ausdrückliche Bestätigung im offiziellen Android-Dialog.",
  },
  hi: {
    outRel: "hi/index.html",
    urlPath: "/hi/",
    htmlLang: "hi",
    rtl: false,
    currency: "INR",
    privacyHref: "/privacy.html",
    privacyLabel: "गोपनीयता नीति",
    guidesLabel: "गाइड",
    title: "Midiastore — सटीक ऑन-डिवाइस Android स्टोरेज क्लीनर (< 8 MB)",
    desc: "बिना क्लाउड अपलोड अपने Android की स्टोरेज साफ़ करें: बड़े वीडियो (>10 MB), WhatsApp मीडिया और सटीक SHA-256 डुप्लीकेट। केवल 7.2 MB और शून्य विज्ञापन।",
    socialBanner: "सीधा एक्सेस — सिर्फ 2 टैप में बड़े वीडियो और डुप्लीकेट फ़ाइलें सुरक्षित रूप से जांचें (< 8 MB)।",
    eyebrow: "ऑन-डिवाइस इंजीनियरिंग • ज़ीरो क्लाउड • ANDROID 10+",
    h1: "सटीक सुरक्षा के साथ फ़ोन स्टोरेज साफ़ करें।",
    lead: "बड़े वीडियो (>10 MB), पुराने स्क्रीनशॉट और बाइट-सटीक SHA-256 डुप्लीकेट सीधे अपने फ़ोन पर खोजें। बिना विज्ञापन, बिना क्लाउड और सिर्फ 7.2 MB।",
    ctaPrimary: "Google Play से मुफ़्त डाउनलोड करें",
    ctaSecondary: "प्राथमिकता एक्सेस",
    pills: ["7.2 MB OLED", "सटीक SHA-256 हैश", "100% ऑन-डिवाइस", "शून्य विज्ञापन"],
    cardEyebrow: "खाली करने योग्य स्थान",
    cardSub: "आधिकारिक Android पुष्टि के साथ सुरक्षित समीक्षा",
    cardBtn: "फ़ाइलें जांचें • 12.4 GB",
    mini1Label: "डुप्लीकेट",
    mini1Val: "SHA-256",
    mini2Label: "वीडियो >10 MB",
    mini2Val: "8.2 GB",
    bento: [
      ["बाइट-सटीक SHA-256 जांच", "हर कॉपी को SHA-256 हैश से प्रमाणित करता है और हमेशा 1 मूल फ़ाइल सुरक्षित रखता है।"],
      ["बड़े वीडियो और चैट मीडिया", "10 MB से बड़े वीडियो और पुराने WhatsApp मीडिया को कुछ ही सेकंड में खोजें।"],
      ["100% गोपनीयता गारंटी", "कोई MANAGE_EXTERNAL_STORAGE नहीं, कोई विज्ञापन ID नहीं और आधिकारिक Android ट्रैश सुरक्षा।"],
    ],
    proTitle: "Google Play पर पारदर्शी प्लान",
    proSub: "आवश्यक सफाई हमेशा के लिए मुफ़्त। Pro प्लान में 90+ दिन से बंद ऐप्स और धुंधली फ़ोटो जांच शामिल है।",
    plans: [
      ["मुफ़्त", "₹0", "हमेशा के लिए", "SHA-256 डुप्लीकेट, बड़े वीडियो (>10 MB) और Android ट्रैश।"],
      ["वार्षिक Pro (-56%)", "Google Play", "सर्वोत्तम मूल्य", "अप्रयुक्त ऐप्स (90d+) और ऑन-डिवाइस धुंधली फ़ोटो जांच।"],
      ["लाइफटाइम Pro", "Google Play", "एक बार भुगतान", "आपके Google Play खाते से जुड़ा स्थायी एक्सेस।"],
    ],
    vipTitle: "सीधा Google Play एक्सेस",
    vipDesc: "अपना Gmail दर्ज करें या ऊपर Google Play बटन दबाएं।",
    vipPlaceholder: "आपका Google Play Gmail",
    vipBtn: "दर्ज करें",
    vipOk: "ईमेल सफलतापूर्वक सहेजा गया।",
    faq1Q: "क्या Midiastore बिना क्लाउड अपलोड के 100% फ़ोन पर काम करता है?",
    faq1A: "हाँ। आपकी फ़ोटो और वीडियो कभी भी आपके फ़ोन से बाहर नहीं जाते।",
    faq2Q: "क्या यह अपने आप कोई फ़ाइल डिलीट करता है?",
    faq2A: "कभी नहीं। हर कार्रवाई के लिए आपकी Android सिस्टम पुष्टि आवश्यक है।",
  },
  ja: {
    outRel: "ja/index.html",
    urlPath: "/ja/",
    htmlLang: "ja",
    rtl: false,
    currency: "JPY",
    privacyHref: "/privacy.html",
    privacyLabel: "プライバシーポリシー",
    guidesLabel: "ガイド",
    title: "Midiastore — 高精度オンデバイス Android ストレージ整理 (< 8 MB)",
    desc: "Androidの空き容量を精密に確保：大容量動画（>10MB）や厳密なSHA-256重複ファイルを端末内で安全に検出。広告ゼロ・クラウド送信ゼロ。",
    socialBanner: "ダイレクトアクセス — 大容量動画や重複写真を2タップで安全に確認（軽量 7.2 MB）。",
    eyebrow: "100% 端末内処理 • クラウド送信ゼロ • ANDROID 10+",
    h1: "精密な技術でストレージを美しく整理。",
    lead: "大容量動画、古いスクリーンショット、厳密なSHA-256重複ファイルを端末内だけで高速検出。広告なし、クラウド送信なし、わずか7.2MB。",
    ctaPrimary: "Google Play で無料ダウンロード",
    ctaSecondary: "優先アクセス",
    pills: ["7.2 MB OLED", "厳密 SHA-256", "100% 端末内処理", "広告ゼロ"],
    cardEyebrow: "確保可能な容量",
    cardSub: "Android公式確認ダイアログによる安全な整理",
    cardBtn: "ファイルを確認 • 12.4 GB",
    mini1Label: "重複検出",
    mini1Val: "SHA-256",
    mini2Label: "動画 >10 MB",
    mini2Val: "8.2 GB",
    bento: [
      ["バイト単位のSHA-256検証", "ファイル名ではなくSHA-256ハッシュで完全一致を証明し、常に元ファイル1件を保護します。"],
      ["大容量動画・SNSメディア", "10MB以上の動画や30日以上前のメディアを瞬時に一覧化します。"],
      ["最高水準のプライバシー設計", "過剰な権限なし、広告IDなし、Android公式ゴミ箱（30日間復元）に対応。"],
    ],
    proTitle: "明確な Google Play プラン",
    proSub: "基本スキャンと重複・大容量整理は永久無料。Pro版で未使用アプリ検出（90日以上）とピンぼけ写真診断を解放。",
    plans: [
      ["無料プラン", "¥0", "永久無料", "SHA-256重複検出、大容量動画（>10MB）、Androidゴミ箱保護。"],
      ["年額 Pro (-56%)", "Google Play", "一番人気", "90日以上未使用のアプリ検出＋端末内ピンぼけ写真チェック。"],
      ["買い切り Pro", "Google Play", "1回払い", "Google Playアカウントに紐づく永久ライセンス。"],
    ],
    vipTitle: "Google Play ダイレクト案内",
    vipDesc: "Gmailを登録するか、上のGoogle Playボタンからインストールしてください。",
    vipPlaceholder: "Google Play の Gmail アドレス",
    vipBtn: "登録する",
    vipOk: "登録が完了しました。",
    faq1Q: "写真や動画がクラウドに送信されることはありますか？",
    faq1A: "一切ありません。すべての解析とSHA-256計算は端末内（Scoped Storage）で完結します。",
    faq2Q: "自動的にファイルが削除されることはありますか？",
    faq2A: "ありません。必ずプレビューを確認し、Android公式の確認画面で承認した項目のみ処理されます。",
  },
  zh: {
    outRel: "zh/index.html",
    urlPath: "/zh/",
    htmlLang: "zh-CN",
    rtl: false,
    currency: "USD",
    privacyHref: "/privacy.html",
    privacyLabel: "隐私政策",
    guidesLabel: "清理指南",
    title: "Midiastore — 纯粹本地 Android OLED 存储空间清理工具 (< 8 MB)",
    desc: "以精密工程清理 Android 存储空间：快速定位大视频（>10 MB）、聊天缓存与字节级 SHA-256 重复文件。100% 本地运行，零广告。",
    socialBanner: "直达通道 — 仅需 2 步安全核对大视频与重复照片（超轻量 7.2 MB）。",
    eyebrow: "本机原生引擎 • 零云端上传 • ANDROID 10+",
    h1: "以精密工程，还手机纯净空间。",
    lead: "完全在手机本地处理器中定位超大视频、旧截图与字节级 SHA-256 完全重复文件。无广告、不上传云端，安装包仅 7.2 MB。",
    ctaPrimary: "在 Google Play 免费下载",
    ctaSecondary: "优先体验",
    pills: ["7.2 MB 纯黑 OLED", "精准 SHA-256 校验", "100% 本机处理", "零广告"],
    cardEyebrow: "可安全释放空间",
    cardSub: "真实缩略图预览 + Android 官方弹窗确认",
    cardBtn: "核对文件 • 12.4 GB",
    mini1Label: "精准查重",
    mini1Val: "SHA-256",
    mini2Label: "大视频 >10 MB",
    mini2Val: "8.2 GB",
    bento: [
      ["字节级 SHA-256 校验", "流式计算 SHA-256 加密哈希，严格证明完全一致并自动保护 1 份原始文件。"],
      ["超大视频与社交缓存", "秒级筛选大于 10 MB 的视频与超过 30 天的陈旧媒体。"],
      ["大厂级隐私安全", "无需所有文件访问权限，无广告标识符（AD_ID），原生支持 Android 回收站。"],
    ],
    proTitle: "透明的 Google Play 方案",
    proSub: "核心查重与大视频清理永久免费。升级 Pro 解锁 90 天未使用应用检测与本地模糊照片诊断。",
    plans: [
      ["免费版", "$0", "永久免费", "SHA-256 精准查重、大视频（>10 MB）与系统回收站保护。"],
      ["年度 Pro (-56%)", "Google Play", "超值首选", "包含 90 天闲置应用检测与本地模糊照片智能筛查。"],
      ["终身 Pro", "Google Play", "买断制", "一次购买永久绑定您的 Google Play 账号。"],
    ],
    vipTitle: "Google Play 直达通知",
    vipDesc: "输入您的 Google Play Gmail 或直接点击上方按钮前往商店。",
    vipPlaceholder: "您的 Gmail 邮箱",
    vipBtn: "保存邮箱",
    vipOk: "邮箱已保存。",
    faq1Q: "Midiastore 会将我的照片上传到云端吗？",
    faq1A: "绝不上传。所有扫描、缩略图与 SHA-256 计算均 100% 在手机本地完成。",
    faq2Q: "应用会自动删除我的文件吗？",
    faq2A: "绝不会。所有操作均需您查看缩略图并在 Android 官方确认弹窗中批准。",
  },
  ar: {
    outRel: "ar/index.html",
    urlPath: "/ar/",
    htmlLang: "ar",
    rtl: true,
    currency: "USD",
    privacyHref: "/privacy.html",
    privacyLabel: "سياسة الخصوصية",
    guidesLabel: "الأدلة",
    title: "Midiastore — منظف تخزين أندرويد الاحترافي (< 8 MB) وتقنية SHA-256",
    desc: "نظّف مساحة هاتفك الأندرويد بدقة هندسية: الفيديوهات الكبيرة (>10 MB) والملفات المكررة SHA-256 محلياً 100% على جهازك بدون إعلانات.",
    socialBanner: "وصول مباشر — راجع الفيديوهات الكبيرة والصور المكررة بأمان في خطوتين (< 8 MB).",
    eyebrow: "معالجة محلية 100% • بدون سحابة • ANDROID 10+",
    h1: "مساحة تخزين نظيفة بدقة هندسية فائقة.",
    lead: "اكتشف الفيديوهات الكبيرة واللقطات القديمة والنسخ المتطابقة SHA-256 مباشرة على جهازك. بدون إعلانات، بدون رفع للسحابة، وبحجم 7.2 ميجابايت فقط.",
    ctaPrimary: "تنزيل مجاناً من Google Play",
    ctaSecondary: "وصول مبكر",
    pills: ["7.2 MB OLED", "تحقق SHA-256 دقيق", "100% على الجهاز", "بدون إعلانات"],
    cardEyebrow: "المساحة القابلة للاستعادة",
    cardSub: "معاينة آمنة مع نافذة تأكيد أندرويد الرسمية",
    cardBtn: "مراجعة الملفات • 12.4 GB",
    mini1Label: "المكررات",
    mini1Val: "SHA-256",
    mini2Label: "فيديو >10 MB",
    mini2Val: "8.2 GB",
    bento: [
      ["تطابق SHA-256 بايت ببايت", "يتحقق رقمياً من النسخ المتطابقة ويحمي دائماً نسخة أصلية واحدة."],
      ["الفيديوهات الكبيرة والدردشات", "يعثر فوراً على الفيديوهات الأكبر من 10 ميجابايت ووسائط واتساب القديمة محلياً."],
      ["خصوصية بمعايير عالمية", "بدون صلاحيات شاملة، بدون معرف إعلاني، ومع دعم سلة محذوفات أندرويد الرسمية."],
    ],
    proTitle: "خطط واضحة عبر Google Play",
    proSub: "التنظيف الأساسي مجاني للأبد. تفتح خطة Pro التطبيقات غير المستخدمة (+90 يوماً) وفحص الصور الضبابية محلياً.",
    plans: [
      ["مجاني", "$0", "للأبد", "كشف التكرار SHA-256 والفيديوهات الكبيرة وسلة محذوفات أندرويد."],
      ["سنوي Pro (-56%)", "Google Play", "الأفضل قيمة", "التطبيقات غير المستخدمة (+90 يوماً) وفحص الصور الضبابية على الجهاز."],
      ["مدى الحياة Pro", "Google Play", "دفعة واحدة", "تفعيل دائم مرتبط بحسابك في Google Play."],
    ],
    vipTitle: "وصول مباشر عبر Google Play",
    vipDesc: "أدخل بريد Gmail الخاص بك أو اضغط على زر Google Play أعلاه.",
    vipPlaceholder: "بريد Gmail الخاص بك",
    vipBtn: "حفظ",
    vipOk: "تم حفظ البريد بنجاح.",
    faq1Q: "هل يعمل التطبيق 100% محلياً دون رفع صوري؟",
    faq1A: "نعم. جميع الفحوصات وحسابات SHA-256 تتم بالكامل داخل هاتفك.",
    faq2Q: "هل يحذف التطبيق أي ملف تلقائياً؟",
    faq2A: "أبداً. كل عملية تتطلب موافقتك الصريحة عبر نافذة أندرويد الرسمية.",
  },
  ru: {
    outRel: "ru/index.html",
    urlPath: "/ru/",
    htmlLang: "ru",
    rtl: false,
    currency: "USD",
    privacyHref: "/privacy.html",
    privacyLabel: "Конфиденциальность",
    guidesLabel: "Гиды",
    title: "Midiastore — Точный локальный очиститель памяти Android (< 8 МБ)",
    desc: "Очистка памяти Android с инженерной точностью: крупные видео (>10 МБ), мессенджеры и точные дубликаты SHA-256. 100% локально, без рекламы.",
    socialBanner: "Прямой доступ — безопасная проверка тяжелых видео и дубликатов в 2 касания (< 8 МБ).",
    eyebrow: "ЛОКАЛЬНАЯ ИНЖЕНЕРИЯ • БЕЗ ОБЛАКА • ANDROID 10+",
    h1: "Чистая память с хирургической точностью.",
    lead: "Находите тяжелые видео, старые скриншоты и побайтовые дубликаты SHA-256 прямо на устройстве. Без рекламы, без отправки в облако и всего 7,2 МБ.",
    ctaPrimary: "Скачать в Google Play",
    ctaSecondary: "Ранний доступ",
    pills: ["7,2 МБ OLED", "Точный SHA-256", "100% На устройстве", "Без рекламы"],
    cardEyebrow: "ДОСТУПНО К ОЧИСТКЕ",
    cardSub: "Безопасный просмотр с системным подтверждением Android",
    cardBtn: "Проверить • 12,4 ГБ",
    mini1Label: "Дубликаты",
    mini1Val: "SHA-256",
    mini2Label: "Видео >10 МБ",
    mini2Val: "8,2 ГБ",
    bento: [
      ["Побайтовая проверка SHA-256", "Вычисляет криптографический хеш SHA-256 и всегда защищает один оригинал."],
      ["Крупные видео и чаты", "Мгновенно находит видео >10 МБ и медиа из Telegram/WhatsApp без облака."],
      ["Приватность уровня Big Tech", "Без MANAGE_EXTERNAL_STORAGE, без рекламного ID и с поддержкой Корзины Android."],
    ],
    proTitle: "Прозрачные тарифы Google Play",
    proSub: "Базовая очистка бесплатна навсегда. Pro открывает поиск неиспользуемых приложений (+90 дн.) и размытых фото.",
    plans: [
      ["Бесплатно", "$0", "Навсегда", "Дубликаты SHA-256, крупные видео (>10 МБ) и Корзина Android."],
      ["Годовой Pro (-56%)", "Google Play", "Выгодный выбор", "Неиспользуемые приложения (+90 дн.) и проверка размытых фото."],
      ["Навсегда Pro", "Google Play", "Разовый платеж", "Бессрочный доступ на вашем аккаунте Google Play."],
    ],
    vipTitle: "Прямой доступ в Google Play",
    vipDesc: "Укажите ваш Gmail или нажмите кнопку Google Play выше.",
    vipPlaceholder: "Ваш адрес Gmail",
    vipBtn: "Сохранить",
    vipOk: "Адрес успешно сохранен.",
    faq1Q: "Работает ли Midiastore полностью локально на телефоне?",
    faq1A: "Да. Все проверки и расчеты SHA-256 выполняются строго на вашем устройстве.",
    faq2Q: "Удаляет ли приложение файлы автоматически?",
    faq2A: "Никогда. Каждое действие подтверждается вами в официальном окне Android.",
  },
  id: {
    outRel: "id/index.html",
    urlPath: "/id/",
    htmlLang: "id",
    rtl: false,
    currency: "IDR",
    privacyHref: "/privacy.html",
    privacyLabel: "Kebijakan Privasi",
    guidesLabel: "Panduan",
    title: "Midiastore — Pembersih Penyimpanan Android OLED Presisi (< 8 MB)",
    desc: "Bersihkan memori Android dengan presisi tinggi: video besar (>10 MB), WhatsApp, dan duplikat akurat SHA-256. 100% di perangkat, tanpa iklan.",
    socialBanner: "Akses langsung — tinjau video besar & foto duplikat secara aman dalam 2 ketukan (< 8 MB).",
    eyebrow: "TEKNOLOGI ON-DEVICE • TANPA CLOUD • ANDROID 10+",
    h1: "Penyimpanan bersih dengan presisi tinggi.",
    lead: "Temukan video berat, tangkapan layar lama, dan duplikat identik SHA-256 langsung di ponsel Anda. Tanpa iklan, tanpa unggah cloud, hanya 7,2 MB.",
    ctaPrimary: "Unduh Gratis di Google Play",
    ctaSecondary: "Akses Prioritas",
    pills: ["7,2 MB OLED", "Hash SHA-256 Akurat", "100% Di Ponsel", "Tanpa Iklan"],
    cardEyebrow: "RUANG DAPAT DIBERSIHKAN",
    cardSub: "Tinjauan thumbnail aman dengan dialog resmi Android",
    cardBtn: "Tinjau File • 12,4 GB",
    mini1Label: "Duplikat",
    mini1Val: "SHA-256",
    mini2Label: "Video >10 MB",
    mini2Val: "8,2 GB",
    bento: [
      ["Verifikasi Byte SHA-256", "Memastikan file benar-benar identik dan selalu melindungi 1 file asli."],
      ["Video Besar & Media Chat", "Menemukan video >10 MB dan media WhatsApp lama dalam hitungan detik."],
      ["Privasi Kelas Dunia", "Tanpa izin MANAGE_EXTERNAL_STORAGE, tanpa ID iklan, mendukung Sampah Android."],
    ],
    proTitle: "Harga Transparan di Google Play",
    proSub: "Pembersihan inti gratis selamanya. Midiastore Pro membuka deteksi Aplikasi Tak Terpakai (+90h) & Foto Buram.",
    plans: [
      ["Gratis", "Rp0", "Selamanya", "Duplikat SHA-256, video >10 MB, dan Sampah Android."],
      ["Tahunan Pro (-56%)", "Google Play", "Nilai terbaik", "Aplikasi tak terpakai (+90h) dan deteksi foto buram lokal."],
      ["Seumur Hidup Pro", "Google Play", "Sekali bayar", "Akses permanen di akun Google Play Anda."],
    ],
    vipTitle: "Akses Langsung Google Play",
    vipDesc: "Masukkan Gmail Google Play Anda atau ketuk tombol Google Play di atas.",
    vipPlaceholder: "Email Gmail Anda",
    vipBtn: "Simpan",
    vipOk: "Email berhasil disimpan.",
    faq1Q: "Apakah Midiastore memproses 100% secara lokal?",
    faq1A: "Ya. Foto dan video Anda tidak pernah diunggah ke server mana pun.",
    faq2Q: "Apakah aplikasi menghapus file secara otomatis?",
    faq2A: "Tidak pernah. Semua penghapusan memerlukan konfirmasi resmi Android Anda.",
  },
  bn: {
    outRel: "bn/index.html",
    urlPath: "/bn/",
    htmlLang: "bn",
    rtl: false,
    currency: "BDT",
    privacyHref: "/privacy.html",
    privacyLabel: "গোপনীয়তা নীতি",
    guidesLabel: "গাইড",
    title: "Midiastore — নিখুঁত অন-ডিভাইস Android স্টোরেজ ক্লিনার (< 8 MB)",
    desc: "কোনো ক্লাউড আপলোড ছাড়াই Android স্টোরেজ পরিষ্কার করুন: বড় ভিডিও (>10 MB), WhatsApp এবং সঠিক SHA-256 ডুপ্লিকেট। মাত্র 7.2 MB এবং বিজ্ঞাপনমুক্ত।",
    socialBanner: "সরাসরি অ্যাক্সেস — মাত্র ২ ট্যাপে বড় ভিডিও ও ডুপ্লিকেট ছবি নিরাপদে যাচাই করুন (< 8 MB)।",
    eyebrow: "অন-ডিভাইস ইঞ্জিনিয়ারিং • জিরো ক্লাউড • ANDROID 10+",
    h1: "নিখুঁত নিরাপত্তায় ফোনের স্টোরেজ খালি করুন।",
    lead: "বড় ভিডিও, পুরনো স্ক্রিনশট এবং বাইট-সঠিক SHA-256 ডুপ্লিকেট সরাসরি আপনার ফোনেই খুঁজুন। বিজ্ঞাপনমুক্ত, ক্লাউড ছাড়া এবং মাত্র 7.2 MB।",
    ctaPrimary: "Google Play থেকে ফ্রি ডাউনলোড",
    ctaSecondary: "অগ্রাধিকার অ্যাক্সেস",
    pills: ["7.2 MB OLED", "সঠিক SHA-256 হ্যাশ", "100% ফোনেই", "বিজ্ঞাপনমুক্ত"],
    cardEyebrow: "উদ্ধারযোগ্য স্পেস",
    cardSub: "অফিসিয়াল Android কনফার্মেশনসহ নিরাপদ রিভিউ",
    cardBtn: "ফাইল দেখুন • 12.4 GB",
    mini1Label: "ডুপ্লিকেট",
    mini1Val: "SHA-256",
    mini2Label: "ভিডিও >10 MB",
    mini2Val: "8.2 GB",
    bento: [
      ["বাইট-সঠিক SHA-256 যাচাই", "প্রতিটি কপি SHA-256 হ্যাশ দিয়ে যাচাই করে এবং সর্বদা ১টি মূল ফাইল সুরক্ষিত রাখে।"],
      ["বড় ভিডিও ও চ্যাট মিডিয়া", "১০ MB-এর বড় ভিডিও এবং পুরনো মেসেঞ্জার ফাইল মুহূর্তেই খুঁজে বের করে।"],
      ["সর্বোচ্চ প্রাইভেসি", "কোনো অতিরিক্ত পারমিশন নেই, কোনো বিজ্ঞাপন ID নেই এবং Android ট্র্যাশ সাপোর্ট।"],
    ],
    proTitle: "Google Play-তে স্বচ্ছ প্ল্যান",
    proSub: "মূল ক্লিনিং চিরকাল ফ্রি। Pro প্ল্যানে রয়েছে অব্যবহৃত অ্যাপ (+90 দিন) এবং ঝাপসা ছবি শনাক্তকরণ।",
    plans: [
      ["ফ্রি", "৳0", "চিরকাল", "SHA-256 ডুপ্লিকেট, বড় ভিডিও (>10 MB) এবং Android ট্র্যাশ।"],
      ["বার্ষিক Pro (-56%)", "Google Play", "সেরা সাশ্রয়", "অব্যবহৃত অ্যাপ (+90d) এবং অন-ডিভাইস ঝাপসা ছবি যাচাই।"],
      ["লাইফটাইম Pro", "Google Play", "একবার পেমেন্ট", "আপনার Google Play অ্যাকাউন্টে স্থায়ী অ্যাক্সেস।"],
    ],
    vipTitle: "সরাসরি Google Play অ্যাক্সেস",
    vipDesc: "আপনার Gmail দিন অথবা উপরের Google Play বোতামে ট্যাপ করুন।",
    vipPlaceholder: "আপনার Gmail ঠিকানা",
    vipBtn: "সংরক্ষণ করুন",
    vipOk: "ইমেইল সফলভাবে সংরক্ষিত হয়েছে।",
    faq1Q: "Midiastore কি সম্পূর্ণ ফোনের ভেতরেই কাজ করে?",
    faq1A: "হ্যাঁ। আপনার কোনো ছবি বা ভিডিও কখনোই ক্লাউডে পাঠানো হয় না।",
    faq2Q: "অ্যাপটি কি একা একা ফাইল মুছে ফেলে?",
    faq2A: "কখনোই না। প্রতিটি কাজের জন্য Android-এর অফিসিয়াল কনফার্মেশন প্রয়োজন।",
  },
  ko: {
    outRel: "ko/index.html",
    urlPath: "/ko/",
    htmlLang: "ko",
    rtl: false,
    currency: "KRW",
    privacyHref: "/privacy.html",
    privacyLabel: "개인정보처리방침",
    guidesLabel: "가이드",
    title: "Midiastore — 정밀 온디바이스 Android 저장공간 클리너 (< 8 MB)",
    desc: "클라우드 전송 없이 Android 저장공간을 정밀하게 정리하세요: 대용량 동영상(>10MB), 메신저 미디어, 정확한 SHA-256 중복 검사. 무광고 초경량 7.2MB.",
    socialBanner: "다이렉트 액세스 — 2번의 탭으로 대용량 동영상과 중복 사진을 안전하게 정리하세요 (< 8 MB).",
    eyebrow: "온디바이스 엔지니어링 • 제로 클라우드 • ANDROID 10+",
    h1: "정밀한 기술로 완성한 쾌적한 저장공간.",
    lead: "대용량 동영상, 오래된 스크린샷, 바이트 단위 SHA-256 중복 파일을 기기 내부에서 즉시 찾아냅니다. 무광고, 무클라우드, 초경량 7.2MB.",
    ctaPrimary: "Google Play 무료 다운로드",
    ctaSecondary: "우선 액세스",
    pills: ["7.2 MB OLED", "정밀 SHA-256 해시", "100% 기기 내 처리", "광고 없음"],
    cardEyebrow: "확보 가능 용량",
    cardSub: "Android 공식 삭제 확인을 통한 안전한 검토",
    cardBtn: "파일 검토 • 12.4 GB",
    mini1Label: "중복 파일",
    mini1Val: "SHA-256",
    mini2Label: "동영상 >10 MB",
    mini2Val: "8.2 GB",
    bento: [
      ["바이트 단위 SHA-256 검증", "암호화 SHA-256 해시로 완벽히 동일한 파일만 선별하며 원본 1개를 항상 보호합니다."],
      ["대용량 동영상 & 채팅 미디어", "10MB 이상의 동영상과 오래된 카카오톡·메신저 미디어를 몇 초 만에 탐색합니다."],
      ["빅테크 수준의 프라이버시", "과도한 권한 없음, 광고 ID 없음, Android 11+ 공식 휴지통 복원 지원."],
    ],
    proTitle: "투명한 Google Play 요금제",
    proSub: "핵심 정리는 평생 무료입니다. Pro에서 미사용 앱(90일+) 및 기기 내 흔들린 사진 진단을 잠금 해제하세요.",
    plans: [
      ["무료", "₩0", "평생 무료", "SHA-256 중복 검사, 대용량 동영상(>10MB) 및 Android 휴지통."],
      ["연간 Pro (-56%)", "Google Play", "최고 인기", "90일 이상 미사용 앱 감지 및 온디바이스 흐릿한 사진 분석."],
      ["평생 소장 Pro", "Google Play", "1회 결제", "Google Play 계정에 영구 귀속되는 평생 이용권."],
    ],
    vipTitle: "Google Play 바로가기",
    vipDesc: "Gmail을 등록하거나 상단 Google Play 버튼을 눌러 설치하세요.",
    vipPlaceholder: "Google Play Gmail 주소",
    vipBtn: "등록",
    vipOk: "이메일이 저장되었습니다.",
    faq1Q: "사진이나 동영상이 외부 서버로 전송되나요?",
    faq1A: "전혀 전송되지 않습니다. 모든 분석과 SHA-256 계산은 100% 기기 내부에서만 실행됩니다.",
    faq2Q: "파일이 자동으로 삭제되나요?",
    faq2A: "절대 자동 삭제되지 않으며, 항상 Android 공식 확인 창을 거칩니다.",
  },
};

const PRECISION_RING_SVG = `<svg viewBox="0 0 184 184" width="38" height="38" aria-hidden="true"><rect width="184" height="184" rx="42" fill="#121722" stroke="rgba(255,255,255,0.14)" stroke-width="2"/><circle cx="92" cy="92" r="54" fill="none" stroke="#1E293B" stroke-width="18"/><path d="M99.5,38.5 A54,54 0 1,1 44.3,117.4" fill="none" stroke="#0A84FF" stroke-width="18" stroke-linecap="round"/><path d="M41.9,71.8 A54,54 0 0,1 71.8,41.9" fill="none" stroke="#FFFFFF" stroke-width="18" stroke-linecap="round"/></svg>`;

function esc(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function renderPage(loc) {
  const canonical = `${BASE_URL}${loc.urlPath}`;
  const hreflangsHtml = HREFLANGS.map(
    ([hl, u]) => `  <link rel="alternate" hreflang="${hl}" href="${BASE_URL}${u}">`
  ).join("\n");

  const navLangsHtml = NAV_LANGS.map(([lbl, u]) => {
    const active = u === loc.urlPath ? ' class="active"' : "";
    return `<a href="${u}"${active}>${lbl}</a>`;
  }).join("");

  const pillsHtml = loc.pills.map((p) => `<span class="pill">${esc(p)}</span>`).join("");
  const bentoHtml = loc.bento
    .map(
      ([t, d]) => `<article class="bento-card"><h3>${esc(t)}</h3><p>${esc(d)}</p></article>`
    )
    .join("\n      ");

  const plansHtml = loc.plans
    .map(([name, price, period, desc], idx) => {
      const feat = idx === 1 ? " featured" : "";
      return `<div class="plan-card${feat}">
        <div>
          <div class="plan-name">${esc(name)}</div>
          <div class="plan-price">${esc(price)} <span>${esc(period)}</span></div>
          <p class="plan-desc">${esc(desc)}</p>
        </div>
      </div>`;
    })
    .join("\n      ");

  const ldApp = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "Midiastore",
    operatingSystem: "Android 10+",
    applicationCategory: "UtilitiesApplication",
    inLanguage: loc.htmlLang,
    softwareVersion: "2.8.2",
    fileSize: "7.2 MB",
    url: canonical,
    downloadUrl: "https://play.google.com/store/apps/details?id=com.rdf112.midiastore",
    description: loc.desc,
    offers: { "@type": "Offer", price: "0", priceCurrency: loc.currency },
  });

  const ldWeb = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: "Midiastore",
    url: canonical,
    inLanguage: loc.htmlLang,
  });

  const ldFaq = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: [
      { "@type": "Question", name: loc.faq1Q, acceptedAnswer: { "@type": "Answer", text: loc.faq1A } },
      { "@type": "Question", name: loc.faq2Q, acceptedAnswer: { "@type": "Answer", text: loc.faq2A } },
    ],
  });

  return `<!doctype html>
<html lang="${loc.htmlLang}"${loc.rtl ? ' dir="rtl"' : ""}>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#000000">
  <title>${esc(loc.title)}</title>
  <meta name="description" content="${esc(loc.desc)}">
  <meta name="robots" content="index,follow">
  <link rel="icon" type="image/png" href="/icon-512.png?v=ring1">
  <link rel="apple-touch-icon" href="/icon-512.png?v=ring1">
  <link rel="canonical" href="${canonical}">
${hreflangsHtml}
  <meta property="og:type" content="website">
  <meta property="og:title" content="${esc(loc.title)}">
  <meta property="og:description" content="${esc(loc.desc)}">
  <meta property="og:url" content="${canonical}">
  <meta property="og:image" content="${BASE_URL}/og-banner.png?v=ring1">
  <meta property="og:image:width" content="1200">
  <meta property="og:image:height" content="630">
  <meta property="og:site_name" content="Midiastore">
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${esc(loc.title)}">
  <meta name="twitter:description" content="${esc(loc.desc)}">
  <meta name="twitter:image" content="${BASE_URL}/og-banner.png?v=ring1">
  <script type="application/ld+json">${ldApp}</script>
  <script type="application/ld+json">${ldWeb}</script>
  <script type="application/ld+json">${ldFaq}</script>
  <style>
    :root{--bg:#000000;--surface:#141416;--card:#1C1C1E;--border:#2C2C2E;--text:#F5F5F7;--muted:#8E8E93;--blue:#0A84FF;--cyan:#5AC8FA;--green:#30D158;--max:1060px}
    *{box-sizing:border-box}
    body{margin:0;font-family:-apple-system,BlinkMacSystemFont,"SF Pro Display","Segoe UI",Roboto,sans-serif;background:var(--bg);color:var(--text);line-height:1.55;-webkit-font-smoothing:antialiased}
    .wrap{width:min(var(--max),calc(100% - 36px));margin:0 auto}
    header{position:sticky;top:0;z-index:40;background:rgba(0,0,0,0.84);backdrop-filter:blur(18px);border-bottom:1px solid var(--border)}
    .nav{display:flex;align-items:center;justify-content:space-between;min-height:64px;gap:14px;padding:8px 0;flex-wrap:wrap}
    .brand{display:inline-flex;align-items:center;gap:11px;text-decoration:none;color:var(--text);font-size:1.22rem;font-weight:700;letter-spacing:-.03em}
    .brand span{color:var(--blue)}
    .lang-nav{display:flex;gap:5px;align-items:center;overflow-x:auto;max-width:100%;scrollbar-width:none}
    .lang-nav::-webkit-scrollbar{display:none}
    .lang-nav a{text-decoration:none;font-size:.74rem;font-weight:600;color:var(--muted);padding:5px 8px;border-radius:8px;border:1px solid transparent;transition:all .15s}
    .lang-nav a:hover,.lang-nav a.active{color:#fff;background:var(--card);border-color:var(--border)}
    .social-banner{display:none;margin:18px 0 0;padding:11px 16px;border-radius:14px;background:rgba(10,132,255,0.14);border:1px solid rgba(10,132,255,0.45);color:#fff;font-size:.88rem;font-weight:600;text-align:center}
    .hero{padding:52px 0 40px;display:grid;grid-template-columns:1.15fr .85fr;gap:44px;align-items:center}
    .eyebrow{font-size:.78rem;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:var(--blue);margin-bottom:12px}
    h1{font-size:clamp(2.15rem,4.8vw,3.55rem);line-height:1.06;letter-spacing:-.04em;margin:0 0 18px;font-weight:800}
    .lead{font-size:1.08rem;color:var(--muted);margin:0 0 28px;max-width:540px}
    .cta-row{display:flex;gap:12px;flex-wrap:wrap;align-items:center}
    .btn-primary{display:inline-flex;align-items:center;justify-content:center;background:var(--blue);color:#fff;font-weight:700;font-size:1.02rem;padding:16px 30px;border-radius:14px;text-decoration:none;min-height:54px;border:none;cursor:pointer;box-shadow:0 12px 32px rgba(10,132,255,0.34);transition:transform .15s,background .15s}
    .btn-primary:hover{transform:translateY(-1px);background:#198BFF}
    .btn-secondary{display:inline-flex;align-items:center;justify-content:center;background:var(--surface);color:var(--text);font-weight:600;font-size:.95rem;padding:15px 22px;border-radius:14px;text-decoration:none;min-height:54px;border:1px solid var(--border)}
    .btn-secondary:hover{border-color:var(--blue)}
    .pills{display:flex;gap:9px;flex-wrap:wrap;margin-top:22px}
    .pill{font-size:.79rem;font-weight:600;color:#E5E5EA;background:var(--surface);border:1px solid var(--border);padding:7px 13px;border-radius:999px}
    .phone-shell{max-width:340px;width:100%;margin:0 auto;background:#0C0C0E;border:1.5px solid var(--border);border-radius:38px;padding:18px;box-shadow:0 24px 64px rgba(0,0,0,0.85)}
    .p-card{background:var(--card);border:1px solid var(--border);border-radius:20px;padding:18px;margin-bottom:11px;text-align:start}
    .p-eyebrow{font-size:.7rem;font-weight:700;letter-spacing:.06em;color:var(--muted)}
    .p-metric{font-size:2.05rem;font-weight:800;letter-spacing:-.03em;margin:4px 0}
    .p-sub{font-size:.8rem;color:var(--muted)}
    .seg-bar{height:7px;background:#2C2C2E;border-radius:999px;display:flex;overflow:hidden;margin:14px 0 16px}
    .p-btn{display:block;background:var(--blue);color:#fff;text-decoration:none;text-align:center;padding:11px;border-radius:12px;font-weight:700;font-size:.88rem}
    .p-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}
    .bento{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px;margin:24px 0 56px}
    .bento-card{background:var(--surface);border:1px solid var(--border);border-radius:22px;padding:26px}
    .bento-card h3{margin:0 0 8px;font-size:1.14rem;letter-spacing:-.02em}
    .bento-card p{margin:0;color:var(--muted);font-size:.93rem;line-height:1.55}
    .sec-head{margin:0 0 22px}
    .sec-head h2{font-size:clamp(1.55rem,3.4vw,2.1rem);letter-spacing:-.03em;margin:0 0 8px}
    .sec-head p{margin:0;color:var(--muted);max-width:680px;font-size:.98rem}
    .plans{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px;margin-bottom:56px}
    .plan-card{background:var(--surface);border:1px solid var(--border);border-radius:22px;padding:26px;display:flex;flex-direction:column;justify-content:space-between}
    .plan-card.featured{border-color:var(--blue);box-shadow:0 0 0 1px var(--blue)}
    .plan-name{font-size:.86rem;font-weight:700;color:var(--blue);text-transform:uppercase;letter-spacing:.05em}
    .plan-price{font-size:1.85rem;font-weight:800;letter-spacing:-.03em;margin:8px 0 10px}
    .plan-price span{font-size:.86rem;font-weight:500;color:var(--muted)}
    .plan-desc{margin:0;color:var(--muted);font-size:.91rem}
    .vip-box{background:var(--surface);border:1px solid var(--border);border-radius:22px;padding:28px;margin-bottom:48px;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:20px}
    .vip-copy{max-width:500px}
    .vip-copy h3{margin:0 0 6px;font-size:1.18rem}
    .vip-copy p{margin:0;color:var(--muted);font-size:.91rem}
    .tester-form{display:flex;gap:10px;flex-wrap:wrap;flex:1;min-width:260px;justify-content:flex-end}
    .tester-input{flex:1;min-width:210px;padding:13px 16px;border-radius:12px;border:1px solid var(--border);background:#0A0A0C;color:#fff;font-size:.94rem;outline:none}
    .tester-input:focus{border-color:var(--blue)}
    .vip-msg{width:100%;font-size:.86rem;color:var(--green);font-weight:600;display:none;margin-top:8px}
    .faq{display:grid;gap:12px;margin-bottom:56px}
    .faq details{background:var(--surface);border:1px solid var(--border);border-radius:16px;padding:18px 22px}
    .faq summary{font-weight:700;cursor:pointer;list-style:none}
    .faq summary::-webkit-details-marker{display:none}
    .faq p{margin:10px 0 0;color:var(--muted);font-size:.92rem}
    footer{border-top:1px solid var(--border);padding:26px 0 40px;color:var(--muted);font-size:.85rem;display:flex;justify-content:space-between;flex-wrap:wrap;gap:12px}
    footer a{color:var(--muted);text-decoration:none}
    footer a:hover{color:#fff}
    @media (max-width:820px){
      .hero{grid-template-columns:1fr;padding:24px 0 28px;gap:26px;text-align:center}
      .lead{margin-left:auto;margin-right:auto}
      .cta-row,.pills{justify-content:center}
      .btn-primary,.btn-secondary{width:100%}
      .tester-form{justify-content:stretch}
    }
  </style>
</head>
<body>
  <header>
    <div class="wrap nav">
      <a class="brand" href="${loc.urlPath}">${PRECISION_RING_SVG}<div>Midia<span>store</span></div></a>
      <nav class="lang-nav" aria-label="Language">${navLangsHtml}</nav>
    </div>
  </header>

  <main class="wrap">
    <div id="igWelcomeBanner" class="social-banner">${esc(loc.socialBanner)}</div>

    <section class="hero">
      <div>
        <div class="eyebrow">${esc(loc.eyebrow)}</div>
        <h1>${esc(loc.h1)}</h1>
        <p class="lead">${esc(loc.lead)}</p>
        <div class="cta-row">
          <a id="heroPlayBtn" class="btn-primary" href="https://play.google.com/store/apps/details?id=com.rdf112.midiastore&amp;referrer=utm_source%3Dmidiastore_web%26utm_medium%3Dlanding_page" data-track-cta="hero_play_store" target="_blank" rel="noopener">${esc(loc.ctaPrimary)}</a>
          <a class="btn-secondary" href="#vip-testers">${esc(loc.ctaSecondary)}</a>
        </div>
        <div class="pills">${pillsHtml}</div>
      </div>

      <div class="phone-shell">
        <div class="p-card">
          <div class="p-eyebrow">${esc(loc.cardEyebrow)}</div>
          <div class="p-metric">12,4 GB</div>
          <div class="p-sub">${esc(loc.cardSub)}</div>
          <div class="seg-bar">
            <span style="width:48%;background:#0A84FF"></span>
            <span style="width:24%;background:#5AC8FA"></span>
            <span style="width:14%;background:#30D158"></span>
          </div>
          <a class="p-btn" href="https://play.google.com/store/apps/details?id=com.rdf112.midiastore" target="_blank" rel="noopener">${esc(loc.cardBtn)}</a>
        </div>
        <div class="p-grid">
          <div class="p-card" style="margin:0;padding:14px">
            <div class="p-sub">${esc(loc.mini1Label)}</div>
            <div style="font-size:1.12rem;font-weight:800;color:#FF9F0A;margin-top:4px">${esc(loc.mini1Val)}</div>
          </div>
          <div class="p-card" style="margin:0;padding:14px">
            <div class="p-sub">${esc(loc.mini2Label)}</div>
            <div style="font-size:1.12rem;font-weight:800;color:#5AC8FA;margin-top:4px">${esc(loc.mini2Val)}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="bento">
      ${bentoHtml}
    </section>

    <section>
      <div class="sec-head">
        <h2>${esc(loc.proTitle)}</h2>
        <p>${esc(loc.proSub)}</p>
      </div>
      <div class="plans">
      ${plansHtml}
      </div>
    </section>

    <section class="vip-box" id="vip-testers">
      <div class="vip-copy">
        <h3>${esc(loc.vipTitle)}</h3>
        <p>${esc(loc.vipDesc)}</p>
      </div>
      <form class="tester-form" id="testerForm">
        <input type="email" id="testerEmail" class="tester-input" placeholder="${esc(loc.vipPlaceholder)}" required>
        <button type="submit" class="btn-primary" style="padding:13px 22px;min-height:48px;font-size:.92rem">${esc(loc.vipBtn)}</button>
        <div id="vipMsg" class="vip-msg">${esc(loc.vipOk)}</div>
      </form>
    </section>

    <section class="faq">
      <details open>
        <summary>${esc(loc.faq1Q)}</summary>
        <p>${esc(loc.faq1A)}</p>
      </details>
      <details>
        <summary>${esc(loc.faq2Q)}</summary>
        <p>${esc(loc.faq2A)}</p>
      </details>
    </section>
  </main>

  <footer>
    <div class="wrap" style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:12px">
      <div>&copy; 2026 Midiastore (com.rdf112.midiastore)</div>
      <div style="display:flex;gap:18px;flex-wrap:wrap">
        <a href="/guides/">${esc(loc.guidesLabel)}</a>
        <a href="${loc.privacyHref}">${esc(loc.privacyLabel)}</a>
        <a href="https://play.google.com/store/apps/details?id=com.rdf112.midiastore" target="_blank" rel="noopener">Google Play</a>
      </div>
    </div>
  </footer>

  <script>
    (function(){
      var endpoint = "${SUPABASE_URL}/functions/v1/ingest-product-event";
      var vid = "";
      try {
        vid = localStorage.getItem("ms_anon_vid") || "";
        if (!/^[a-f0-9]{16,64}$/i.test(vid)) {
          var arr = new Uint8Array(16);
          (window.crypto || window.msCrypto).getRandomValues(arr);
          vid = Array.from(arr, function(b){ return b.toString(16).padStart(2,"0"); }).join("");
          localStorage.setItem("ms_anon_vid", vid);
        }
      } catch(_) { vid = "a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5"; }

      var p = new URLSearchParams(window.location.search);
      var rawSrc = (p.get("utm_source") || p.get("src") || "").toLowerCase();
      var rawMed = (p.get("utm_medium") || "").toLowerCase();
      var rawCamp = (p.get("utm_campaign") || "").toLowerCase();
      var rawCont = (p.get("utm_content") || "").toLowerCase();
      var ref = (document.referrer || "").toLowerCase();
      var joined = [rawSrc, rawMed, rawCamp, rawCont, ref].join(" ");
      var isSocial = /(instagram|\\big\\b|bio|story|stories|reels|shorts|tiktok|youtu\\.be|youtube)/.test(joined);
      if (isSocial) {
        var b = document.getElementById("igWelcomeBanner");
        if (b) b.style.display = "block";
      }
      var utmSource = rawSrc || (joined.indexOf("instagram") !== -1 ? "instagram" : joined.indexOf("tiktok") !== -1 ? "tiktok" : "midiastore_web");
      var utmMedium = rawMed || (isSocial ? "social" : "landing_page");
      var utmCampaign = rawCamp || (isSocial ? "aconteceunobr" : "bigtech_home");

      function sendEvt(name, extra) {
        var payload = JSON.stringify({
          installHash: vid,
          eventName: name,
          appVersion: "web-bigtech-2.9",
          locale: "${loc.htmlLang}",
          properties: Object.assign({ path: window.location.pathname || "/", utm_source: utmSource, utm_medium: utmMedium, utm_campaign: utmCampaign }, extra || {})
        });
        try {
          if (navigator.sendBeacon) {
            navigator.sendBeacon(endpoint, new Blob([payload], { type: "text/plain;charset=UTF-8" }));
            return;
          }
        } catch(_) {}
        try { fetch(endpoint, { method: "POST", keepalive: true, headers: { "Content-Type": "text/plain;charset=UTF-8" }, body: payload }).catch(function(){}); } catch(_) {}
      }

      sendEvt("page_view");

      var btn = document.getElementById("heroPlayBtn");
      if (btn) {
        var refStr = "utm_source=" + encodeURIComponent(utmSource) + "&utm_medium=" + encodeURIComponent(utmMedium) + "&utm_campaign=" + encodeURIComponent(utmCampaign);
        btn.href = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore&referrer=" + encodeURIComponent(refStr);
        btn.addEventListener("click", function(){ sendEvt("play_store_click", { cta: "hero_play_store" }); });
      }

      var form = document.getElementById("testerForm");
      if (form) {
        form.addEventListener("submit", function(ev){
          ev.preventDefault();
          var email = (document.getElementById("testerEmail").value || "").trim().toLowerCase();
          if (!email) return;
          fetch("${SUPABASE_URL}/rest/v1/beta_testers", {
            method: "POST",
            headers: {
              "apikey": "${ANON_KEY}",
              "Authorization": "Bearer ${ANON_KEY}",
              "Content-Type": "application/json",
              "Prefer": "return=minimal"
            },
            body: JSON.stringify({ email: email, source: utmSource })
          }).then(function(){
            var m = document.getElementById("vipMsg");
            if (m) m.style.display = "block";
            form.reset();
          }).catch(function(){
            var m = document.getElementById("vipMsg");
            if (m) m.style.display = "block";
          });
        });
      }
    })();
  </script>
</body>
</html>
`;
}

let written = 0;
for (const loc of Object.values(LOCALES)) {
  const outPath = path.join(DOCS, loc.outRel);
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, renderPage(loc), "utf8");
  written++;
}
console.log(`BIGTECH_LANDING_GENERATED=${written}_PAGES`);
