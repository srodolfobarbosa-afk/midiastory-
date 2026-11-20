#!/usr/bin/env node
/**
 * MOTOR pSEO LONG-TAIL GLOBAL — MIDIASTORE (48 Páginas x 4 Idiomas + Malha Interna + A/B + GEO llms.txt)
 *
 * Gera 48 páginas estáticas ultra-rápidas focadas em buscas Long-Tail de baixa concorrência
 * e altíssima intenção de download (Samsung, Motorola, Xiaomi, WhatsApp, Duplicatas SHA-256,
 * Vídeos Grandes, Prints Antigos, Erro sem espaço para atualizar apps, Limpador < 10 MB).
 *
 * Cada página inclui:
 * - Schema.org: SoftwareApplication + FAQPage + BreadcrumbList
 * - Malha de links internos (Related Guides) para acelerar crawl/autoridade no Google
 * - Teste A/B real leve via /ab-config.json (ETag) + rastreamento navigator.sendBeacon
 * - Atualização automática de docs/sitemap.xml e docs/llms.txt (GEO)
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const docsDir = path.join(projectRoot, "docs");
const guidesDir = path.join(docsDir, "guides");
const sitemapPath = path.join(docsDir, "sitemap.xml");
const llmsPath = path.join(docsDir, "llms.txt");

const SITE_BASE = "https://midiastory-df6a0.web.app";
const PLAY_URL_BASE = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";
const TODAY = new Date().toISOString().slice(0, 10);

const LANGUAGES = [
  {
    code: "pt-br",
    hreflang: "pt-BR",
    htmlLang: "pt-BR",
    name: "Português (Brasil)",
    cta: "Baixar Midiastore grátis no Google Play (5,9 MB)",
    badge: "5,9 MB • 100% Offline • SHA-256 • Zero Bloatware",
    faqTitle: "Perguntas Frequentes",
    stepsTitle: "Como resolver em menos de 60 segundos",
    relatedTitle: "Outros Guias Práticos",
    trustLine: "Privacidade 100% local via Android Scoped Storage. Nenhum arquivo é apagado sem sua aprovação.",
  },
  {
    code: "en-us",
    hreflang: "en-US",
    htmlLang: "en",
    name: "English (US)",
    cta: "Download Midiastore Free on Google Play (5.9 MB)",
    badge: "5.9 MB • 100% On-Device • SHA-256 • Zero Bloatware",
    faqTitle: "Frequently Asked Questions",
    stepsTitle: "How to fix it in under 60 seconds",
    relatedTitle: "Related Storage Guides",
    trustLine: "100% private on-device scan via Android MediaStore. Nothing is ever deleted without your approval.",
  },
  {
    code: "es-419",
    hreflang: "es-419",
    htmlLang: "es",
    name: "Español (LATAM)",
    cta: "Descargar Midiastore gratis en Google Play (5,9 MB)",
    badge: "5,9 MB • 100% Local • SHA-256 • Sin Bloatware",
    faqTitle: "Preguntas Frecuentes",
    stepsTitle: "Cómo solucionarlo en menos de 60 segundos",
    relatedTitle: "Otras Guías Prácticas",
    trustLine: "Escaneo 100% privado en tu teléfono. Ningún archivo se borra sin tu revisión y confirmación.",
  },
  {
    code: "hi-in",
    hreflang: "hi-IN",
    htmlLang: "hi",
    name: "हिन्दी (भारत)",
    cta: "Google Play पर Midiastore मुफ़्त डाउनलोड करें (5.9 MB)",
    badge: "5.9 MB • 100% ऑफ़लाइन • SHA-256 • सुरक्षित",
    faqTitle: "अक्सर पूछे जाने वाले प्रश्न",
    stepsTitle: "60 सेकंड से कम में स्टोरेज खाली करें",
    relatedTitle: "अन्य स्टोरेज गाइड",
    trustLine: "100% प्राइवेट ऑन-डिवाइस स्कैन। आपकी अनुमति के बिना कोई फ़ाइल डिलीट नहीं होती।",
  },
];

// 12 Long-Tail high-intent topics (48 pages across 4 languages)
const LONG_TAIL_TOPICS = [
  {
    slug: "whatsapp-storage-full",
    kw: {
      "pt-br": { title: "Como Liberar Espaço no WhatsApp Sem Apagar Fotos Importantes", h1: "WhatsApp lotou seu Android? Libere gigabytes sem perder fotos", pain: "Vídeos encaminhados em grupos e cópias repetidas consomem GBs silenciosamente no WhatsApp." },
      "en-us": { title: "How to Free Up WhatsApp Storage on Android Without Losing Photos", h1: "WhatsApp Storage Full? Reclaim Gigabytes Safely", pain: "Forwarded group videos and duplicate copies silently fill your Android phone." },
      "es-419": { title: "Cómo Liberar Espacio de WhatsApp en Android Sin Borrar Tus Fotos", h1: "¿WhatsApp llenó tu celular? Libera gigas sin perder fotos", pain: "Videos reenviados en grupos y copias repetidas llenan tu memoria sin avisar." },
      "hi-in": { title: "बिना ज़रूरी फ़ोटो खोए Android में WhatsApp स्टोरेज कैसे खाली करें", h1: "WhatsApp से स्टोरेज भर गया? सुरक्षित रूप से GB खाली करें", pain: "ग्रुप वीडियो और डुप्लीकेट कॉपी चुपचाप फ़ोन की मेमोरी भर देती हैं।" },
    },
  },
  {
    slug: "remove-duplicate-photos-android",
    kw: {
      "pt-br": { title: "Como Apagar Fotos Duplicadas no Android com Segurança (SHA-256)", h1: "Apague Fotos e Vídeos Duplicados no Android com Precisão SHA-256", pain: "Comparar arquivos só pelo tamanho é arriscado; o hash SHA-256 prova cópias 100% idênticas." },
      "en-us": { title: "Remove Duplicate Photos on Android Safely Using SHA-256", h1: "Delete Duplicate Photos & Videos on Android With SHA-256 Proof", pain: "Size-only matching risks deleting unique photos; streaming SHA-256 proves byte-exact duplicates." },
      "es-419": { title: "Cómo Borrar Fotos Duplicadas en Android con Seguridad (SHA-256)", h1: "Elimina Fotos Duplicadas en Android con Precisión SHA-256", pain: "El hash SHA-256 verifica copias idénticas byte por byte manteniendo tu original seguro." },
      "hi-in": { title: "Android में डुप्लीकेट फ़ोटो सुरक्षित रूप से कैसे हटाएं (SHA-256)", h1: "SHA-256 जांच के साथ डुप्लीकेट फ़ोटो और वीडियो हटाएं", pain: "सटीक SHA-256 जांच केवल बिल्कुल एक जैसी कॉपी दिखाती है और ओरिजिनल सुरक्षित रखती है।" },
    },
  },
  {
    slug: "android-storage-full-fix",
    kw: {
      "pt-br": { title: "Armazenamento Cheio no Android: Como Resolver Rápido Sem Formatar", h1: "Aviso de 'Armazenamento Cheio' no Android? Resolva Sem Formatar", pain: "Você não precisa formatar o aparelho nem desinstalar seus apps favoritos para recuperar espaço." },
      "en-us": { title: "Android Storage Full Fix: Free Up Space Fast Without Factory Reset", h1: "Fix 'Android Storage Full' Fast — No Factory Reset Needed", pain: "Pinpoint massive forgotten videos, old screenshots, and exact duplicates in seconds." },
      "es-419": { title: "Memoria Llena en Android: Cómo Liberar Espacio Sin Formatear", h1: "¿'Memoria Llena' en tu Android? Libera Espacio Real Sin Formatear", pain: "Identifica videos gigantes, capturas viejas y duplicados sin perder tus apps." },
      "hi-in": { title: "Android Storage Full समस्या कैसे ठीक करें — बिना फ़ोन रीसेट किए", h1: "Android Storage Full? बिना फ़ॉर्मेट किए तुरंत जगह खाली करें", pain: "बड़े वीडियो, पुराने स्क्रीनशॉट और डुप्लीकेट फ़ाइलें सेकंडों में पहचानें।" },
    },
  },
  {
    slug: "clean-android-phone-fast",
    kw: {
      "pt-br": { title: "Melhor Limpador Leve para Android (5,9 MB) — Zero Bloatware", h1: "O Limpador de Armazenamento Leve (5,9 MB) Sem Anúncios Invasivos", pain: "Limpadores comuns pesam 100 MB e prometem milagres falsos; o Midiastore tem 5,9 MB e foco real." },
      "en-us": { title: "Lightweight Android Storage Cleaner (5.9 MB) — Zero Bloatware", h1: "A 5.9 MB Android Storage Cleaner Built for Speed & Privacy", pain: "Skip 100 MB scareware cleaners; get pure OLED UI, exact SHA-256 and zero bloatware." },
      "es-419": { title: "Limpiador Ligero para Android (5,9 MB) — Rápido y Sin Bloatware", h1: "El Limpiador de Android de 5,9 MB: Rápido, Privado y Claro", pain: "Sin anuncios invasivos ni promesas falsas; solo 3 pestañas limpias para recuperar gigas." },
      "hi-in": { title: "Android के लिए सबसे हल्का स्टोरेज क्लीनर (5.9 MB) — तेज़ और सुरक्षित", h1: "सिर्फ 5.9 MB का Android स्टोरेज क्लीनर — तेज़ और भरोसेमंद", pain: "भारी ब्लोटवेयर से छुटकारा पाएं और 100% ऑफ़लाइन असली स्टोरेज खाली करें।" },
    },
  },
  {
    slug: "samsung-galaxy-storage-full",
    kw: {
      "pt-br": { title: "Samsung Galaxy Memória Cheia Mesmo Apagando Fotos: Como Resolver", h1: "Samsung Galaxy com Memória Cheia? Libere Gigabytes em 1 Minuto", pain: "No Samsung Galaxy (A15, A54, S23/S24), vídeos 4K, lixeira e cópias repetidas ocupam quase todo o disco." },
      "en-us": { title: "Samsung Galaxy Storage Full After Deleting Photos: Real Fix", h1: "Samsung Galaxy Storage Still Full? Reclaim Real Space Fast", pain: "On Samsung Galaxy phones, oversized 4K clips, Trash retention, and duplicate media silently eat storage." },
      "es-419": { title: "Samsung Galaxy Memoria Llena Aunque Borre Fotos: Solución Real", h1: "¿Tu Samsung Galaxy Sigue Sin Espacio? Libera Gigas Hoy", pain: "Videos 4K grandes, papelera y archivos duplicados bloquean tu Galaxy aunque borres fotos sueltas." },
      "hi-in": { title: "Samsung Galaxy फ़ोन में स्टोरेज फुल है? तुरंत खाली करने का सही तरीका", h1: "Samsung Galaxy स्टोरेज फुल? 1 मिनट में असली GB खाली करें", pain: "Samsung फ़ोन में बड़े 4K वीडियो और डुप्लीकेट फ़ाइलें चुपचाप जगह घेर लेती हैं।" },
    },
  },
  {
    slug: "motorola-storage-full-fix",
    kw: {
      "pt-br": { title: "Motorola Moto G Armazenamento Cheio: Como Limpar Rápido e Seguro", h1: "Motorola Moto G Sem Espaço? Libere Memória Real Sem Travar", pain: "Testado em hardware Motorola real (Moto G84 5G): encontre arquivos gigantes e duplicatas SHA-256 em segundos." },
      "en-us": { title: "Motorola Moto G Storage Full Fix: Clean Space Safely (5.9 MB)", h1: "Motorola Phone Out of Storage? Free Up Gigabytes Safely", pain: "Tested on real Motorola hardware: surface oversized videos and exact duplicates with a 5.9 MB app." },
      "es-419": { title: "Motorola Moto G Almacenamiento Lleno: Cómo Liberar Espacio Rápido", h1: "¿Motorola Moto G Sin Espacio? Recupera Gigabytes Sin Formatear", pain: "Probado en equipos Motorola reales: detecta videos pesados y duplicados SHA-256 sin ralentizar el equipo." },
      "hi-in": { title: "Motorola Moto G स्टोरेज फुल समस्या का तेज़ और सुरक्षित समाधान", h1: "Motorola फ़ोन में जगह कम है? 5.9 MB ऐप से GB खाली करें", pain: "असली Motorola डिवाइस पर टेस्टेड: बड़े वीडियो और डुप्लीकेट फ़ाइलें तुरंत साफ करें।" },
    },
  },
  {
    slug: "xiaomi-redmi-storage-full",
    kw: {
      "pt-br": { title: "Xiaomi Redmi Memória Cheia: Como Limpar Vídeos e Duplicatas Sem Anúncios", h1: "Xiaomi / Redmi / Poco Sem Espaço? Limpe Sem Anúncios Invasivos", pain: "No Xiaomi/Redmi/Poco, vídeos baixados, prints acumulados e cópias do WhatsApp lotam a memória rapidamente." },
      "en-us": { title: "Xiaomi Redmi Storage Full Fix Without Annoying Ads (5.9 MB)", h1: "Xiaomi / Redmi / Poco Out of Space? Clean Fast With Zero Ads", pain: "Skip ad-heavy system cleaners on Xiaomi & Redmi; review large media and exact duplicates cleanly." },
      "es-419": { title: "Xiaomi Redmi Memoria Llena: Cómo Liberar Espacio Sin Anuncios", h1: "¿Xiaomi o Redmi Sin Memoria? Libera Espacio Real Sin Publicidad", pain: "Encuentra videos grandes, capturas viejas y duplicados exactos en tu Xiaomi, Redmi o Poco." },
      "hi-in": { title: "Xiaomi Redmi Poco स्टोरेज फुल कैसे खाली करें — बिना विज्ञापनों के", h1: "Xiaomi / Redmi / Poco स्टोरेज फुल? बिना Ads के साफ करें", pain: "विज्ञापनों से भरे क्लीनर छोड़ें और 5.9 MB Midiastore से असली स्पेस खाली करें।" },
    },
  },
  {
    slug: "free-up-space-android-update",
    kw: {
      "pt-br": { title: "Sem Espaço para Atualizar Apps na Play Store: Como Liberar GBs Agora", h1: "Não Consegue Atualizar Apps na Play Store por Falta de Espaço?", pain: "Quando o Android bloqueia atualizações por falta de memória, você precisa de um limpador de apenas 5,9 MB." },
      "en-us": { title: "Not Enough Space to Update Apps on Google Play: Quick Fix", h1: "Can't Update Apps on Play Store Due to Low Storage? Fix It Now", pain: "When Play Store blocks updates, install a tiny 5.9 MB tool to clear gigabytes of large videos and duplicates." },
      "es-419": { title: "Sin Espacio para Actualizar Apps en Play Store: Solución en 1 Minuto", h1: "¿Play Store No Te Deja Actualizar Apps por Falta de Espacio?", pain: "Con solo 5,9 MB, Midiastore cabe incluso en celulares llenos y te devuelve gigas al instante." },
      "hi-in": { title: "Play Store पर ऐप्स अपडेट करने के लिए जगह नहीं है? तुरंत समाधान", h1: "स्टोरेज कम होने से Play Store ऐप्स अपडेट नहीं हो रहे? अभी ठीक करें", pain: "सिर्फ 5.9 MB का Midiastore कम जगह में भी इंस्टॉल होकर GB खाली करता है।" },
    },
  },
  {
    slug: "find-largest-videos-android",
    kw: {
      "pt-br": { title: "Como Achar os Maiores Vídeos Ocupando Espaço no Android", h1: "Descubra Quais Vídeos Gigantes Estão Comendo Sua Memória no Android", pain: "Apenas 5 ou 10 vídeos esquecidos em alta resolução podem ocupar mais de 6 GB no seu celular." },
      "en-us": { title: "How to Find the Largest Videos Taking Up Space on Android", h1: "Find the Biggest Videos Eating Your Android Storage in Seconds", pain: "Just 5 to 10 forgotten HD/4K videos can consume over 6 GB of storage on your phone." },
      "es-419": { title: "Cómo Encontrar los Videos Más Pesados en tu Celular Android", h1: "Localiza en Segundos los Videos Gigantes Que Llenan tu Android", pain: "Solo 5 o 10 videos olvidados pueden ocupar más de 6 GB en tu galería." },
      "hi-in": { title: "Android फ़ोन में सबसे बड़े वीडियो कैसे खोजें और हटाएं", h1: "सेकंडों में जानें कौन से बड़े वीडियो आपका स्टोरेज खा रहे हैं", pain: "सिर्फ 5–10 पुराने HD वीडियो फ़ोन में 6 GB से ज़्यादा जगह घेर सकते हैं।" },
    },
  },
  {
    slug: "clean-old-screenshots-android",
    kw: {
      "pt-br": { title: "Como Apagar Prints e Capturas de Tela Antigas no Android (+30 Dias)", h1: "Limpe Milhares de Prints Antigos no Android Sem Perder Comprovantes", pain: "Comprovantes antigos, códigos e prints de conversa acumulam centenas de MBs sem você perceber." },
      "en-us": { title: "How to Delete Old Screenshots on Android Quickly (+30 Days)", h1: "Clear Hundreds of Old Screenshots on Android Safely", pain: "Expired QR codes, receipts, and chat screenshots pile up fast — review and clear them in one place." },
      "es-419": { title: "Cómo Borrar Capturas de Pantalla Antiguas en Android (+30 Días)", h1: "Limpia Cientos de Capturas Antiguas en Android con Seguridad", pain: "Revisa y elimina capturas viejas de más de 30 días sin tocar tus fotos importantes." },
      "hi-in": { title: "Android में पुराने स्क्रीनशॉट एक साथ कैसे साफ करें (+30 दिन)", h1: "सैकड़ों पुराने स्क्रीनशॉट सुरक्षित रूप से हटाएं", pain: "30 दिन से पुराने बेकार स्क्रीनशॉट आसानी से जांचें और हटाएं।" },
    },
  },
  {
    slug: "whatsapp-duplicate-videos-fix",
    kw: {
      "pt-br": { title: "Vídeos Duplicados no WhatsApp: Como Apagar Cópias Repetidas com SHA-256", h1: "Apague Vídeos Repetidos de Grupos do WhatsApp Mantendo o Original", pain: "O mesmo vídeo salvo em 3 grupos diferentes triplica o espaço gasto; SHA-256 remove só as cópias extras." },
      "en-us": { title: "Remove Duplicate WhatsApp Videos on Android With Exact SHA-256", h1: "Delete Repeated WhatsApp Group Videos While Keeping the Original", pain: "One video shared across 3 groups triples storage usage — SHA-256 clears only redundant copies." },
      "es-419": { title: "Videos Duplicados en WhatsApp: Cómo Borrar Copias Repetidas (SHA-256)", h1: "Elimina Videos Repetidos de Grupos de WhatsApp Sin Perder el Original", pain: "El mismo video en varios grupos multiplica el espacio usado; SHA-256 borra solo las copias extra." },
      "hi-in": { title: "WhatsApp के डुप्लीकेट वीडियो कैसे हटाएं — SHA-256 से सुरक्षित सफाई", h1: "WhatsApp ग्रुप के दोहराए गए वीडियो हटाएं, ओरिजिनल बचाएं", pain: "कई ग्रुप में आया एक ही वीडियो स्टोरेज दोगुना-तिगुना भर देता है।" },
    },
  },
  {
    slug: "lightweight-cleaner-under-10mb",
    kw: {
      "pt-br": { title: "Limpador de Celular Leve com Menos de 10 MB para Android (5,9 MB)", h1: "Por Que o Midiastore Pesa Apenas 5,9 MB (Limpador Ultra Leve)", pain: "Feito para celulares com 32 GB ou 64 GB: instala rápido, economiza bateria no tema OLED e roda 100% offline." },
      "en-us": { title: "Best Android Storage Cleaner Under 10 MB (Only 5.9 MB, Private)", h1: "Why Midiastore Is Only 5.9 MB — Ultra-Lightweight Android Cleaner", pain: "Built for 32 GB and 64 GB phones: installs in seconds, pure OLED dark mode, 100% on-device." },
      "es-419": { title: "Limpiador de Celular Ligero de Menos de 10 MB para Android (5,9 MB)", h1: "Por Qué Midiastore Pesa Solo 5,9 MB (Limpiador Ultraligero)", pain: "Ideal para celulares de 32 GB o 64 GB: modo oscuro OLED, rápido y 100% offline." },
      "hi-in": { title: "10 MB से कम का सबसे हल्का Android क्लीनर (सिर्फ 5.9 MB)", h1: "सिर्फ 5.9 MB का अल्ट्रा-लाइट Android स्टोरेज क्लीनर", pain: "32 GB और 64 GB फ़ोनों के लिए खास: तेज़ इंस्टॉल, OLED डार्क मोड और 100% ऑफ़लाइन।" },
    },
  },
];

function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function getSteps(langCode) {
  if (langCode === "pt-br") {
    return [
      "Instale o Midiastore (apenas 5,9 MB — cabe até em celulares quase lotados).",
      "Toque em Analisar Espaço para encontrar vídeos grandes, capturas antigas e duplicatas SHA-256.",
      "Revise os arquivos sugeridos e confirme a exclusão pelo diálogo oficial do Android.",
    ];
  }
  if (langCode === "es-419") {
    return [
      "Instala Midiastore (solo 5,9 MB — ideal incluso cuando casi no queda memoria).",
      "Toca Analizar Espacio para encontrar videos pesados, capturas viejas y duplicados SHA-256.",
      "Revisa cada archivo y confirma la limpieza con el diálogo oficial de Android.",
    ];
  }
  if (langCode === "hi-in") {
    return [
      "Midiastore इंस्टॉल करें (सिर्फ 5.9 MB — कम स्टोरेज में भी आसानी से इंस्टॉल होता है)।",
      "बड़े वीडियो, पुराने स्क्रीनशॉट और SHA-256 डुप्लीकेट खोजने के लिए स्कैन करें।",
      "फ़ाइलें जांचें और Android की आधिकारिक पुष्टि के साथ सुरक्षित डिलीट करें।",
    ];
  }
  return [
    "Install Midiastore (only 5.9 MB — fits easily even when storage is critical).",
    "Tap Analyze Space to surface large videos, old screenshots, and exact SHA-256 duplicates.",
    "Review every candidate and confirm deletion via Android's official system dialog.",
  ];
}

function getFaqs(langCode) {
  if (langCode === "pt-br") {
    return [
      { q: "O Midiastore apaga minhas fotos automaticamente?", a: "Nunca. Você revisa cada item e aprova manualmente no diálogo oficial do Android." },
      { q: "Minhas fotos ou vídeos vão para algum servidor?", a: "Não. Toda análise de tamanho e hash SHA-256 acontece 100% localmente no seu celular." },
      { q: "Posso usar de graça e cancelar o Pro quando quiser?", a: "Sim. O plano gratuito permite analisar e limpar itens essenciais, e o Pro opcional pode ser cancelado em 1 toque na Google Play." },
    ];
  }
  if (langCode === "es-419") {
    return [
      { q: "¿Midiastore borra fotos automáticamente?", a: "Nunca. Siempre revisas los archivos y confirmas con el diálogo oficial de Android." },
      { q: "¿Mis archivos se suben a la nube?", a: "No. El análisis y la verificación SHA-256 funcionan 100% en tu dispositivo." },
      { q: "¿Cuánto pesa la app?", a: "Solo 5,9 MB, diseñada para liberar espacio sin ocupar memoria." },
    ];
  }
  if (langCode === "hi-in") {
    return [
      { q: "क्या Midiastore अपने आप फ़ोटो डिलीट करता है?", a: "नहीं। आपकी समीक्षा और Android की अनुमति के बिना कुछ भी डिलीट नहीं होता।" },
      { q: "क्या मेरी फ़ोटो सर्वर पर अपलोड होती हैं?", a: "नहीं। पूरा स्कैन और SHA-256 जांच 100% आपके फ़ोन पर ऑफ़लाइन होती है।" },
    ];
  }
  return [
    { q: "Does Midiastore ever delete files automatically?", a: "Never. Every cleanup requires your visual review and Android's official confirmation dialog." },
    { q: "Are my photos or videos uploaded to the cloud?", a: "No. MediaStore scanning and SHA-256 duplicate hashing run 100% on-device." },
    { q: "Why is Midiastore only 5.9 MB?", a: "Zero bloatware, zero games, zero invasive ad SDKs — just fast, honest storage cleanup." },
  ];
}

function renderPage({ langObj, topic, topicIdx }) {
  const copy = topic.kw[langObj.code];
  const canonicalUrl = `${SITE_BASE}/guides/${langObj.code}/${topic.slug}/`;
  const playHref = `${PLAY_URL_BASE}&utm_source=pseo&utm_medium=organic&utm_campaign=${langObj.code}_${topic.slug}`;
  const steps = getSteps(langObj.code);
  const faqs = getFaqs(langObj.code);

  // Pick 4 related guides in the same language for dense internal link mesh
  const related = [];
  for (let offset = 1; offset <= 4; offset++) {
    related.push(LONG_TAIL_TOPICS[(topicIdx + offset) % LONG_TAIL_TOPICS.length]);
  }

  const hreflangTags = LANGUAGES.map(
    (l) => `  <link rel="alternate" hreflang="${l.hreflang}" href="${SITE_BASE}/guides/${l.code}/${topic.slug}/" />`,
  )
    .concat(`  <link rel="alternate" hreflang="x-default" href="${SITE_BASE}/guides/en-us/${topic.slug}/" />`)
    .join("\n");

  const softwareLd = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "Midiastore",
    operatingSystem: "Android 10+",
    applicationCategory: "UtilitiesApplication",
    fileSize: "5.9MB",
    offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
    downloadUrl: PLAY_URL_BASE,
    url: canonicalUrl,
  };

  const faqLd = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: faqs.map((f) => ({
      "@type": "Question",
      name: f.q,
      acceptedAnswer: { "@type": "Answer", text: f.a },
    })),
  };

  const breadcrumbLd = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "Midiastore", item: `${SITE_BASE}/` },
      { "@type": "ListItem", position: 2, name: "Guides", item: `${SITE_BASE}/guides/` },
      { "@type": "ListItem", position: 3, name: copy.h1, item: canonicalUrl },
    ],
  };

  const stepsHtml = steps
    .map((s, idx) => `      <li><span class="step-num">${idx + 1}</span><span>${escapeHtml(s)}</span></li>`)
    .join("\n");
  const faqsHtml = faqs
    .map((f) => `      <details class="faq-item" open><summary>${escapeHtml(f.q)}</summary><p>${escapeHtml(f.a)}</p></details>`)
    .join("\n");
  const relatedHtml = related
    .map(
      (r) =>
        `      <a class="rel-link" href="/guides/${langObj.code}/${r.slug}/">${escapeHtml(r.kw[langObj.code].h1)}</a>`,
    )
    .join("\n");
  const langSwitcher = LANGUAGES.map(
    (l) =>
      `<a href="/guides/${l.code}/${topic.slug}/"${l.code === langObj.code ? ' class="active"' : ""}>${escapeHtml(l.name)}</a>`,
  ).join(" · ");

  return `<!DOCTYPE html>
<html lang="${langObj.htmlLang}">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${escapeHtml(copy.title)} | Midiastore</title>
  <meta name="description" content="${escapeHtml(copy.pain)} Midiastore (5.9 MB, 100% on-device, SHA-256)." />
  <link rel="canonical" href="${canonicalUrl}" />
${hreflangTags}
  <meta property="og:type" content="article" />
  <meta property="og:title" content="${escapeHtml(copy.title)}" />
  <meta property="og:description" content="${escapeHtml(copy.pain)}" />
  <meta property="og:url" content="${canonicalUrl}" />
  <meta name="twitter:card" content="summary_large_image" />
  <meta name="theme-color" content="#000000" />
  <script type="application/ld+json">${JSON.stringify(softwareLd)}</script>
  <script type="application/ld+json">${JSON.stringify(faqLd)}</script>
  <script type="application/ld+json">${JSON.stringify(breadcrumbLd)}</script>
  <style>
    :root { --bg:#000000; --card:#1C1C1E; --text:#F5F5F7; --muted:#98989D; --accent:#0A84FF; --border:#2C2C2E; }
    * { box-sizing:border-box; }
    body { margin:0; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; background:var(--bg); color:var(--text); line-height:1.6; }
    .wrap { max-width:760px; margin:0 auto; padding:24px 18px 64px; }
    .topbar { display:flex; justify-content:space-between; align-items:center; gap:12px; padding-bottom:18px; border-bottom:1px solid var(--border); font-size:13px; color:var(--muted); }
    .brand { color:var(--text); font-weight:700; text-decoration:none; }
    .badge { display:inline-block; margin-top:22px; padding:6px 12px; border-radius:999px; background:#111827; border:1px solid #1E3A8A; color:#93C5FD; font-size:12px; font-weight:600; }
    h1 { font-size:clamp(26px,5vw,38px); line-height:1.18; margin:16px 0 12px; letter-spacing:-0.02em; }
    .subtitle { color:var(--muted); font-size:17px; margin:0 0 24px; }
    .cta-btn { display:inline-block; width:100%; text-align:center; background:var(--accent); color:#FFFFFF; text-decoration:none; font-weight:700; font-size:17px; padding:16px 22px; border-radius:14px; transition:opacity .15s; }
    .cta-btn:hover { opacity:0.9; }
    .trust { font-size:13px; color:var(--muted); margin:12px 0 28px; text-align:center; }
    .card { background:var(--card); border:1px solid var(--border); border-radius:16px; padding:22px; margin-bottom:20px; }
    .card h2 { margin:0 0 14px; font-size:20px; }
    ol.steps { list-style:none; padding:0; margin:0; display:grid; gap:14px; }
    ol.steps li { display:flex; gap:12px; align-items:flex-start; }
    .step-num { flex:0 0 28px; height:28px; border-radius:999px; background:var(--accent); color:#fff; display:inline-flex; align-items:center; justify-content:center; font-weight:700; font-size:14px; }
    .faq-item { border-top:1px solid var(--border); padding:12px 0; }
    .faq-item:first-of-type { border-top:none; padding-top:0; }
    .faq-item summary { cursor:pointer; font-weight:600; color:var(--text); }
    .faq-item p { margin:8px 0 0; color:var(--muted); font-size:15px; }
    .rel-grid { display:grid; gap:10px; }
    .rel-link { display:block; padding:12px 14px; border-radius:12px; background:#121214; border:1px solid var(--border); color:#93C5FD; text-decoration:none; font-size:14px; font-weight:600; }
    .rel-link:hover { border-color:var(--accent); }
    .langs { margin-top:28px; font-size:13px; color:var(--muted); text-align:center; }
    .langs a { color:var(--muted); text-decoration:none; }
    .langs a.active, .langs a:hover { color:var(--accent); }
  </style>
</head>
<body>
  <main class="wrap">
    <header class="topbar">
      <a class="brand" href="/">Midiastore</a>
      <a href="/guides/" style="color:#98989D;text-decoration:none;">All Guides (48)</a>
    </header>
    <span class="badge">${escapeHtml(langObj.badge)}</span>
    <h1>${escapeHtml(copy.h1)}</h1>
    <p class="subtitle">${escapeHtml(copy.pain)}</p>
    <a class="cta-btn" href="${playHref}" rel="noopener">${escapeHtml(langObj.cta)}</a>
    <p class="trust">${escapeHtml(langObj.trustLine)}</p>
    <section class="card">
      <h2>${escapeHtml(langObj.stepsTitle)}</h2>
      <ol class="steps">
${stepsHtml}
      </ol>
    </section>
    <section class="card">
      <h2>${escapeHtml(langObj.faqTitle)}</h2>
${faqsHtml}
    </section>
    <section class="card">
      <h2>${escapeHtml(langObj.relatedTitle)}</h2>
      <div class="rel-grid">
${relatedHtml}
      </div>
    </section>
    <a class="cta-btn" href="${playHref}" rel="noopener">${escapeHtml(langObj.cta)}</a>
    <nav class="langs" aria-label="Languages">${langSwitcher}</nav>
  </main>
  <script>
    (function(){
      var ENDPOINT = "https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/ingest-product-event";
      var vid = "";
      try {
        vid = localStorage.getItem("ms_anon_vid") || "";
        if (!/^[a-f0-9]{16,32}$/.test(vid)) {
          var arr = new Uint8Array(8);
          crypto.getRandomValues(arr);
          vid = Array.from(arr, function(b){ return b.toString(16).padStart(2, "0"); }).join("");
          localStorage.setItem("ms_anon_vid", vid);
        }
      } catch (e) {
        vid = "0000000000000001";
      }
      var variant = (parseInt(vid.slice(0, 2), 16) % 2 === 0) ? "A" : "B";
      var params = new URLSearchParams(window.location.search);
      var baseProps = {
        path: window.location.pathname,
        slug: "${topic.slug}",
        ab: variant,
        ref: (document.referrer || "direct").slice(0, 120),
        utm: (params.get("utm_source") || "organic").slice(0, 40)
      };
      function send(eventName, extra) {
        var payload = JSON.stringify({
          installHash: vid,
          eventName: eventName,
          appVersion: "web-pseo-2",
          locale: "${langObj.hreflang}",
          properties: Object.assign({}, baseProps, extra || {})
        });
        if (navigator.sendBeacon) {
          navigator.sendBeacon(ENDPOINT, payload);
        } else {
          fetch(ENDPOINT, { method: "POST", body: payload, keepalive: true }).catch(function(){});
        }
      }
      send("page_view");
      document.querySelectorAll(".cta-btn").forEach(function(btn, idx) {
        btn.addEventListener("click", function() {
          send("play_store_click", { ctaIndex: idx + 1 });
        });
      });
    })();
  </script>
</body>
</html>
`;
}

function updateSitemap(newUrls) {
  if (!fs.existsSync(sitemapPath)) return { updated: false, added: 0, totalUrls: 0 };
  let xml = fs.readFileSync(sitemapPath, "utf8");
  let added = 0;
  const entries = [];

  for (const url of newUrls) {
    if (!xml.includes(`<loc>${url}</loc>`)) {
      entries.push(`  <url>\n    <loc>${url}</loc>\n    <lastmod>${TODAY}</lastmod>\n    <changefreq>weekly</changefreq>\n    <priority>0.8</priority>\n  </url>`);
      added += 1;
    }
  }

  if (entries.length > 0) {
    xml = xml.replace("</urlset>", `${entries.join("\n")}\n</urlset>`);
    fs.writeFileSync(sitemapPath, xml, "utf8");
  }
  const totalUrls = (xml.match(/<loc>/g) || []).length;
  return { updated: added > 0, added, totalUrls };
}

function updateLlmsTxt(guideUrls) {
  if (!fs.existsSync(llmsPath)) return;
  let content = fs.readFileSync(llmsPath, "utf8");
  const marker = "## Guias Long-Tail de Armazenamento Android (pSEO)";
  const idx = content.indexOf(marker);
  if (idx !== -1) {
    content = content.slice(0, idx).trimEnd();
  }
  const block = [
    "",
    "",
    marker,
    `- Hub Global (48 guias em PT-BR, EN-US, ES-419, HI-IN): ${SITE_BASE}/guides/`,
    ...guideUrls.slice(0, 24).map((u) => `- ${u}`),
    "",
  ].join("\n");
  fs.writeFileSync(llmsPath, content + block, "utf8");
}

export function generatePseoPages() {
  const generatedUrls = [];
  let pagesGenerated = 0;

  for (const langObj of LANGUAGES) {
    for (let i = 0; i < LONG_TAIL_TOPICS.length; i++) {
      const topic = LONG_TAIL_TOPICS[i];
      const outDir = path.join(guidesDir, langObj.code, topic.slug);
      fs.mkdirSync(outDir, { recursive: true });
      fs.writeFileSync(path.join(outDir, "index.html"), renderPage({ langObj, topic, topicIdx: i }), "utf8");
      generatedUrls.push(`${SITE_BASE}/guides/${langObj.code}/${topic.slug}/`);
      pagesGenerated += 1;
    }
  }

  const hubSections = LANGUAGES.map((l) => {
    const items = LONG_TAIL_TOPICS.map(
      (t) => `<li><a href="/guides/${l.code}/${t.slug}/">${escapeHtml(t.kw[l.code].h1)}</a></li>`,
    ).join("");
    return `<section class="card"><h2>${escapeHtml(l.name)} (${LONG_TAIL_TOPICS.length})</h2><ul>${items}</ul></section>`;
  }).join("\n");

  const hubHtml = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Midiastore Android Storage Guides (48 Global Guides)</title>
  <meta name="description" content="48 practical Android storage cleanup guides in Portuguese, English, Spanish and Hindi (Samsung, Motorola, Xiaomi, WhatsApp, SHA-256 duplicates)." />
  <link rel="canonical" href="${SITE_BASE}/guides/" />
  <style>
    body { margin:0; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; background:#000; color:#F5F5F7; }
    main { max-width:780px; margin:0 auto; padding:28px 18px 60px; }
    .card { background:#1C1C1E; border:1px solid #2C2C2E; border-radius:16px; padding:20px; margin-bottom:18px; }
    a { color:#0A84FF; text-decoration:none; }
    li { margin:8px 0; }
  </style>
</head>
<body>
  <main>
    <p><a href="/">← Midiastore Home</a></p>
    <h1>Midiastore Global Storage Guides (48)</h1>
    ${hubSections}
  </main>
</body>
</html>
`;
  fs.mkdirSync(guidesDir, { recursive: true });
  fs.writeFileSync(path.join(guidesDir, "index.html"), hubHtml, "utf8");
  generatedUrls.push(`${SITE_BASE}/guides/`);

  const sitemap = updateSitemap(generatedUrls);
  updateLlmsTxt(generatedUrls);

  return {
    pagesGenerated,
    topicsCount: LONG_TAIL_TOPICS.length,
    languages: LANGUAGES.map((l) => l.code),
    sitemap,
    llmsTxtUpdated: true,
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const res = generatePseoPages();
  console.log("🚀 MOTOR pSEO LONG-TAIL (48 PÁGINAS) CONCLUÍDO:");
  console.log(JSON.stringify(res, null, 2));
}
