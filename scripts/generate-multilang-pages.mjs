import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const docsDir = path.join(projectRoot, "docs");

const SUPABASE_URL = "https://usmqtnlsaavxqguwrtix.supabase.co";
// Public anon key (RLS-scoped to INSERT on public.beta_testers)
const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzbXF0bmxzYWF2eHFndXdydGl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNzcwNTQsImV4cCI6MjEwOTY1MzA1NH0.N7XIpn3lGJM7V8clfU8uKe-7v-N3qKPkIgv3sCKv2VU";

const PLAY_OPTIN_URL = "https://play.google.com/apps/testing/com.rdf112.midiastore";
const PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";

const LANGUAGES = {
  pt: {
    code: "pt-BR",
    title: "Midiastore — Veja o que ocupa espaço no seu Android",
    desc: "Encontre arquivos grandes e duplicatas exatas SHA-256 nas mídias acessíveis do seu Android. Garantir Acesso Antecipado Gratuito ao Teste Fechado na Google Play!",
    eyebrow: "Armazenamento cheio? Comece por uma revisão segura",
    h1: "Descubra o que ocupa espaço.<br><em>Você decide o que fazer.</em>",
    lead: "O Midiastore analisa as mídias acessíveis do seu Android para localizar vídeos pesados, áudios antigos e cópias exatas SHA-256 com confirmação nativa do sistema.",
    ctaHero: "Participar do Teste Fechado (Acesso Gratuito)",
    ctaHeroSub: "Abrir Convite Oficial Google Play",
    note: "A análise, a revisão e o Estúdio de Mídia são gratuitos. Qualquer remoção exige sua confirmação no diálogo oficial do Android.",
    vipBadge: "⭐ Teste Fechado Oficial Google Play (14 Dias • Meta: 12+ Testadores Ativos, 15+ Ideal)",
    vipTitle: "Acesso Antecipado Gratuito ao Teste Fechado em 3 Passos",
    vipDesc: "O Google Play exige pelo menos 12 testadores ativos por 14 dias contínuos (meta segura: 15+ testadores) antes do lançamento público. Cadastre seu Gmail da Google Play abaixo para receber <strong>Acesso Antecipado Gratuito ao Teste Fechado</strong>!",
    emailPlaceholder: "Seu e-mail da Google Play (ex: nome@gmail.com)",
    btnSubmit: "1. Autorizar Meu Gmail no Teste Fechado",
    step1Badge: "Passo 1 • Autorização",
    step1Title: "Cadastre seu Gmail da Google Play",
    step1Desc: "Envie seu e-mail no formulário acima para liberar sua conta Google na lista oficial da faixa fechada do Google Play Console.",
    step1Btn: "Preencher Meu Gmail Acima",
    step2Badge: "Passo 2 • Opt-in Oficial",
    step2Title: "Aceite o Convite Web do Google Play",
    step2Desc: "Abra a página oficial de testes do Google Play e toque no botão \"Tornar-se testador\" (Become a tester) para ativar o download na sua conta.",
    step2Btn: "Aceitar Convite (Opt-in)",
    step3Badge: "Passo 3 • Instalação (14 Dias)",
    step3Title: "Instale na Google Play Store",
    step3Desc: "Após aceitar o convite no Passo 2, baixe o Midiastore na Google Play e mantenha o app instalado por 14 dias contínuos.",
    step3Btn: "Baixar na Google Play",
    stepNote: "💡 Importante: Como o app está em Teste Fechado (14 dias), o link da Google Play (Passo 3) só abre depois que seu Gmail cadastrado no Passo 1 for autorizado na faixa fechada e você aceitar o convite web no Passo 2.",
    c1Tag: "Encontre",
    c1Title: "Arquivos grandes (>10 MB)",
    c1Desc: "Veja vídeos, áudios e imagens pesadas identificados na análise do armazenamento compartilhado acessível.",
    c2Tag: "Revisão de Mídia",
    c2Title: "Vídeos e Áudios Antigos (+30 dias)",
    c2Desc: "Localize mídias antigas de câmera e mensageiros indexadas no Android MediaStore para revisar com calma antes de remover.",
    c3Tag: "100% Seguro",
    c3Title: "Duplicatas Exatas SHA-256",
    c3Desc: "Verificação criptográfica byte a byte em streaming para garantir que apenas cópias idênticas sejam sugeridas, mantendo sempre 1 original protegido.",
    proEyebrow: "Estúdio de Mídia & Midiastore Pro",
    proTitle: "Estúdio completo de mídia e revisão avançada no seu Android.",
    proDesc: "Inclui Gerador de QR Code (PIX/Wi-Fi), Filtros de Fotos ao Vivo, Remoção de EXIF/GPS para privacidade, Conversor de Fotos para PDF A4, exportação do resumo da revisão e limpeza em lote com confirmação oficial do Android.",
    ctrlTitle: "Seus arquivos, sua decisão",
    ctrlDesc: "O Midiastore analisa apenas mídias acessíveis no dispositivo em total conformidade com o Scoped Storage do Android (zero MANAGE_EXTERNAL_STORAGE). Nenhuma foto ou vídeo sai do seu aparelho.",
    privacy: "Política de Privacidade",
    successMsg: "🎉 E-mail registrado com sucesso! Agora siga para o Passo 2 (Aceitar Convite Web) e o Passo 3 (Instalar na Google Play) abaixo.",
    duplicateMsg: "✅ Este e-mail já está registrado na lista do Teste Fechado! Siga para o Passo 2 (Opt-in) e Passo 3 (Google Play) abaixo.",
    errorMsg: "Não foi possível registrar agora. Verifique o endereço de e-mail digitado e tente novamente.",
    connErrorMsg: "Erro de conexão. Verifique sua internet e tente enviar seu Gmail novamente.",
    btnTools: "🎨 Ferramentas",
    btnWaTest: "📱 Diagnóstico",
    vipProNav: "Teste Fechado",
    calcBadge: "⚡ Simulador de Espaço Ocupado",
    calcTitle: "Quanto espaço você pode revisar hoje?",
    calcDesc: "Vídeos encaminhados, cópias repetidas e áudios antigos acumulam gigabytes ao longo dos meses. Faça uma estimativa:",
    calcTime: "Tempo de uso do aparelho:",
    calcOptT1: "6 meses",
    calcOptT2: "1 ano",
    calcOptT3: "2 anos",
    calcOptT4: "3 anos ou mais",
    calcGroups: "Grupos e conversas diárias:",
    calcOptG1: "Poucos (1 a 3 grupos)",
    calcOptG2: "Moderado (4 a 10 grupos)",
    calcOptG3: "Intenso (Mais de 10 grupos)",
    calcEst: "Volume de Mídia Estimado para Revisão:",
    calcSub: "Mídias duplicadas, vídeos grandes e áudios antigos ocupando seu armazenamento.",
    calcCta: "Quero Revisar Esse Espaço Grátis",
    liveTitle: "Movimento Pré-Lançamento em Tempo Real",
    liveVisitsLabel: "Visitas registradas:",
    liveWaitlistLabel: "Na fila de espera / testadores:",
    live24hLabel: "Visitas (24h):",
    shareBtn: "🚀 Convidar Amigos (Pré-Lançamento)",
    shareCopied: "🔗 Link de indicação copiado! Envie no WhatsApp ou Telegram.",
    shareText: "Estou testando o Midiastore no pré-lançamento para revisar arquivos grandes e duplicatas exatas SHA-256 no Android!"
  },
  en: {
    code: "en-US",
    title: "Midiastore — Review & Reclaim Your Android Storage Safely",
    desc: "Find large files and exact SHA-256 duplicates across accessible media on your Android device. Get Free Early Access to the Official Google Play Closed Test!",
    eyebrow: "Storage Full? Start With Complete Clarity",
    h1: "Discover what takes up space.<br><em>You decide what to keep.</em>",
    lead: "Midiastore scans accessible media on your Android device to pinpoint large videos, old audio files, and exact SHA-256 duplicates with native Android confirmation.",
    ctaHero: "Join Closed Testing (Free Early Access)",
    ctaHeroSub: "Open Google Play Opt-in",
    note: "Storage analysis, review, and Media Studio tools are free. File deletion always requires your native Android confirmation dialog.",
    vipBadge: "⭐ Official Google Play Closed Testing (14 Days • Goal: 12+ Active Testers, 15+ Ideal)",
    vipTitle: "Free Early Access to the Closed Test in 3 Easy Steps",
    vipDesc: "Google Play requires at least 12 active testers for 14 continuous days (15+ ideal target) before public launch. Enter your Google Play email below for <strong>Free Early Access to the Official Closed Test</strong>!",
    emailPlaceholder: "Your Google Play email (e.g. you@gmail.com)",
    btnSubmit: "1. Authorize My Google Play Email",
    step1Badge: "Step 1 • Authorization",
    step1Title: "Register Your Google Play Gmail",
    step1Desc: "Submit your Google Play email in the form above so your Google account is authorized on the official Play Console closed testing track.",
    step1Btn: "Enter My Gmail Above",
    step2Badge: "Step 2 • Official Opt-in",
    step2Title: "Accept the Google Play Web Invite",
    step2Desc: "Open the official Google Play testing page and tap \"Become a tester\" to enable the closed beta download for your account.",
    step2Btn: "Open Web Opt-in",
    step3Badge: "Step 3 • Install (14 Days)",
    step3Title: "Install from Google Play Store",
    step3Desc: "After accepting the invite in Step 2, install Midiastore from Google Play and keep it installed for 14 continuous days.",
    step3Btn: "Open Google Play Store",
    stepNote: "💡 Important: Because Midiastore is in Closed Testing (14 days), the Google Play Store link (Step 3) only opens after your submitted Gmail (Step 1) is authorized on the closed track and you accept the web opt-in (Step 2).",
    c1Tag: "Identify",
    c1Title: "Large Media Files (>10 MB)",
    c1Desc: "Quickly review large videos, audio recordings, and images taking up gigabytes in accessible shared storage.",
    c2Tag: "Media Review",
    c2Title: "Old Videos & Audio (+30 Days)",
    c2Desc: "Browse older camera and messaging media indexed in Android MediaStore so you can review candidates before removing anything.",
    c3Tag: "100% Safe",
    c3Title: "SHA-256 Exact Duplicates",
    c3Desc: "Streaming byte-by-byte SHA-256 verification ensures only identical copies are grouped, always keeping at least one original unselected.",
    proEyebrow: "Media Studio & Midiastore Pro",
    proTitle: "Complete Media Studio and Exportable Storage Reviews",
    proDesc: "Includes QR Code Generator, Live Photo Filters, EXIF/GPS privacy removal, Photos-to-PDF A4 converter, review summary export, and batch cleanup with native Android confirmation.",
    ctrlTitle: "Your Files, Your Absolute Decision",
    ctrlDesc: "Midiastore strictly respects Android Scoped Storage (zero MANAGE_EXTERNAL_STORAGE). All scanning happens 100% on-device.",
    privacy: "Privacy Policy",
    successMsg: "🎉 Email registered for Closed Testing! Now complete Step 2 (Accept Web Opt-in) and Step 3 (Install on Google Play) below.",
    duplicateMsg: "✅ This email is already registered on our Closed Testing list! Proceed to Step 2 (Web Opt-in) and Step 3 (Google Play) below.",
    errorMsg: "Could not register right now. Please check your email address and try again.",
    connErrorMsg: "Connection error. Please check your network and try submitting your email again.",
    btnTools: "🎨 Media Tools",
    btnWaTest: "📱 Storage Quiz",
    vipProNav: "Closed Test",
    calcBadge: "⚡ Media Storage Estimator",
    calcTitle: "How much media clutter can you review today?",
    calcDesc: "Messaging apps and cameras accumulate duplicate media, old voice notes, and large videos over time. Calculate your estimate:",
    calcTime: "How long have you used this phone?",
    calcOptT1: "6 months",
    calcOptT2: "1 year",
    calcOptT3: "2 years",
    calcOptT4: "3+ years",
    calcGroups: "Daily groups & chat activity:",
    calcOptG1: "Light (1 to 3 groups)",
    calcOptG2: "Moderate (4 to 10 groups)",
    calcOptG3: "Heavy (10+ active groups)",
    calcEst: "Estimated Media Volume to Review:",
    calcSub: "Duplicate files, old audio notes, and large videos occupying your storage.",
    calcCta: "Review This Storage for Free",
    liveTitle: "Live Pre-Launch Pulse",
    liveVisitsLabel: "Recorded visits:",
    liveWaitlistLabel: "In waitlist / testers:",
    live24hLabel: "Visits (24h):",
    shareBtn: "🚀 Invite Friends (Pre-Launch)",
    shareCopied: "🔗 Referral link copied! Share it on WhatsApp, Telegram, or X.",
    shareText: "Check out Midiastore in pre-launch: safely find large media and exact SHA-256 duplicates on Android!"
  },
  es: {
    code: "es-ES",
    title: "Midiastore — Revisa y libera espacio en tu Android de forma segura",
    desc: "Encuentra archivos grandes y duplicados exactos SHA-256 en los medios accesibles de tu Android. ¡Acceso Anticipado Gratuito a la Prueba Cerrada en Google Play!",
    eyebrow: "¿Almacenamiento lleno? Empieza con total claridad",
    h1: "Descubre qué ocupa espacio.<br><em>Tú decides qué conservar.</em>",
    lead: "Midiastore analiza los medios accesibles en tu Android para localizar vídeos pesados, audios antiguos y duplicados exactos SHA-256 con confirmación nativa de Android.",
    ctaHero: "Unirse a la Prueba Cerrada (Acceso Gratis)",
    ctaHeroSub: "Abrir Invitación de Google Play",
    note: "El análisis, la revisión y el Estudio de Medios son gratuitos. Cualquier eliminación requiere confirmación nativa de Android.",
    vipBadge: "⭐ Prueba Cerrada Oficial de Google Play (14 Días • Meta: 12+ Probadores Activos, 15+ Ideal)",
    vipTitle: "Acceso Anticipado Gratuito a la Prueba Cerrada en 3 Pasos",
    vipDesc: "Google Play exige al menos 12 probadores activos durante 14 días seguidos (meta ideal: 15+ probadores) antes del lanzamiento público. ¡Registra tu correo de Google Play abajo para obtener <strong>Acceso Anticipado Gratuito a la Prueba Cerrada</strong>!",
    emailPlaceholder: "Tu correo de Google Play (ej: nombre@gmail.com)",
    btnSubmit: "1. Autorizar mi correo en la Prueba Cerrada",
    step1Badge: "Paso 1 • Autorización",
    step1Title: "Registra tu Gmail de Google Play",
    step1Desc: "Envía tu correo de Google Play en el formulario superior para autorizar tu cuenta en la lista oficial de la prueba cerrada.",
    step1Btn: "Ingresar mi Gmail arriba",
    step2Badge: "Paso 2 • Opt-in Oficial",
    step2Title: "Acepta la Invitación Web de Google Play",
    step2Desc: "Abre la página oficial de pruebas de Google Play y pulsa en \"Convertirse en tester\" (Become a tester) para habilitar la descarga.",
    step2Btn: "Aceptar Invitación (Opt-in)",
    step3Badge: "Paso 3 • Instalación (14 Días)",
    step3Title: "Instala desde Google Play Store",
    step3Desc: "Tras aceptar la invitación en el Paso 2, descarga Midiastore en Google Play y mantenlo instalado durante 14 días seguidos.",
    step3Btn: "Abrir Google Play Store",
    stepNote: "💡 Importante: Como la app está en Prueba Cerrada (14 días), el enlace de Google Play (Paso 3) solo se abre después de autorizar tu cuenta (Paso 1) y aceptar la invitación web (Paso 2).",
    c1Tag: "Identifica",
    c1Title: "Archivos grandes (>10 MB)",
    c1Desc: "Localiza vídeos pesados, grabaciones y descargas grandes dentro del almacenamiento compartido accesible.",
    c2Tag: "Revisión de Medios",
    c2Title: "Vídeos y audios antiguos (+30 días)",
    c2Desc: "Revisa medios antiguos indexados en Android MediaStore con total calma antes de decidir qué borrar.",
    c3Tag: "100% Seguro",
    c3Title: "Duplicados exactos SHA-256",
    c3Desc: "Comprobación binaria SHA-256 byte a byte para agrupar solo copias idénticas, protegiendo siempre al menos un original.",
    proEyebrow: "Estudio de Medios y Midiastore Pro",
    proTitle: "Control claro y herramientas de medios en tu Android",
    proDesc: "Incluye generador de códigos QR, filtros de fotos en vivo, eliminación de metadatos EXIF/GPS, conversor de fotos a PDF A4, exportación de resumen y limpieza por lotes con confirmación nativa.",
    ctrlTitle: "Tus archivos, tu decisión absoluta",
    ctrlDesc: "Midiastore cumple estrictamente con Scoped Storage de Android (cero MANAGE_EXTERNAL_STORAGE). Todo el procesamiento es 100% local.",
    privacy: "Política de Privacidad",
    successMsg: "🎉 ¡Correo registrado en la Prueba Cerrada! Ahora completa el Paso 2 (Aceptar invitación web) y el Paso 3 (Instalar en Google Play) abajo.",
    duplicateMsg: "✅ ¡Este correo ya está registrado en la Prueba Cerrada! Continúa con el Paso 2 (Opt-in) y el Paso 3 (Google Play) abajo.",
    errorMsg: "No se pudo registrar ahora. Verifica tu dirección de correo e inténtalo de nuevo.",
    connErrorMsg: "Error de conexión. Revisa tu conexión a internet e inténtalo nuevamente.",
    btnTools: "🎨 Herramientas",
    btnWaTest: "📱 Diagnóstico",
    vipProNav: "Prueba Cerrada",
    calcBadge: "⚡ Simulador de Espacio Ocupado",
    calcTitle: "¿Cuánto espacio puedes revisar hoy?",
    calcDesc: "Las copias repetidas, audios antiguos y vídeos pesados acumulan gigabytes con el tiempo. Haz una estimación:",
    calcTime: "Tiempo de uso del móvil:",
    calcOptT1: "6 meses",
    calcOptT2: "1 año",
    calcOptT3: "2 años",
    calcOptT4: "3 años o más",
    calcGroups: "Grupos y chats diarios:",
    calcOptG1: "Pocos (1 a 3 grupos)",
    calcOptG2: "Moderado (4 a 10 grupos)",
    calcOptG3: "Intenso (Más de 10 grupos)",
    calcEst: "Volumen Estimado para Revisión:",
    calcSub: "Archivos duplicados, vídeos grandes y notas de voz antiguas en tu almacenamiento.",
    calcCta: "Revisar Este Espacio Gratis",
    liveTitle: "Pulso de Prelanzamiento en Tiempo Real",
    liveVisitsLabel: "Visitas registradas:",
    liveWaitlistLabel: "En lista de espera / testers:",
    live24hLabel: "Visitas (24h):",
    shareBtn: "🚀 Invitar Amigos (Prelanzamiento)",
    shareCopied: "🔗 ¡Enlace copiado! Compártelo por WhatsApp o Telegram.",
    shareText: "Estoy probando Midiastore en prelanzamiento para revisar vídeos grandes y duplicados exactos SHA-256 en Android."
  },
  de: {
    code: "de-DE",
    title: "Midiastore — Android-Speicher sicher prüfen und bereinigen",
    desc: "Finden Sie große Dateien und exakte SHA-256-Duplikate in den zugänglichen Medien Ihres Android-Geräts. Kostenloser Vorabzugang zum geschlossenen Google Play-Test!",
    eyebrow: "Speicher voll? Schaffen Sie endlich Klarheit",
    h1: "Sehen Sie, was Speicher belegt.<br><em>Sie entscheiden, was bleibt.</em>",
    lead: "Midiastore analysiert zugängliche Mediendateien auf Ihrem Android-Gerät, um große Videos, alte Audiodateien und exakte SHA-256-Duplikate sicher aufzuspüren.",
    ctaHero: "Am geschlossenen Test teilnehmen (Gratis Zugang)",
    ctaHeroSub: "Google Play Opt-in öffnen",
    note: "Analyse, Vorschau und Medienstudio sind kostenlos. Das Löschen erfordert stets Ihre offizielle Android-Systembestätigung.",
    vipBadge: "⭐ Offizieller geschlossener Google Play-Test (14 Tage • Ziel: 12+ aktive Tester, 15+ ideal)",
    vipTitle: "Kostenloser Vorabzugang zum geschlossenen Test in 3 Schritten",
    vipDesc: "Google Play erfordert mindestens 12 aktive Tester über 14 Tage (Ziel: 15+ Tester) vor der Veröffentlichung. Tragen Sie Ihre Google Play E-Mail ein für <strong>kostenlosen Vorabzugang zum geschlossenen Test</strong>!",
    emailPlaceholder: "Ihre Google Play E-Mail (z.B. name@gmail.com)",
    btnSubmit: "1. Meine Google Play E-Mail autorisieren",
    step1Badge: "Schritt 1 • Autorisierung",
    step1Title: "Google Play Gmail eintragen",
    step1Desc: "Senden Sie oben Ihre Google Play E-Mail-Adresse ab, damit Ihr Google-Konto für den geschlossenen Test freigeschaltet wird.",
    step1Btn: "Gmail oben eintragen",
    step2Badge: "Schritt 2 • Offizielles Opt-in",
    step2Title: "Google Play Web-Einladung annehmen",
    step2Desc: "Öffnen Sie die offizielle Google Play-Testseite und tippen Sie auf „Tester werden“ (Become a tester), um den Download freizuschalten.",
    step2Btn: "Web-Opt-in öffnen",
    step3Badge: "Schritt 3 • Installation (14 Tage)",
    step3Title: "Im Google Play Store installieren",
    step3Desc: "Laden Sie Midiastore nach Schritt 2 im Google Play Store herunter und behalten Sie die App für 14 Tage installiert.",
    step3Btn: "Im Google Play Store öffnen",
    stepNote: "💡 Wichtig: Da sich die App im geschlossenen Test (14 Tage) befindet, funktioniert der Play Store-Link (Schritt 3) erst nach Autorisierung Ihrer E-Mail (Schritt 1) und Annahme der Web-Einladung (Schritt 2).",
    c1Tag: "Erkennen",
    c1Title: "Große Dateien (>10 MB)",
    c1Desc: "Finden Sie schnell große Videos, Audiodateien und Bilder im zugänglichen gemeinsamen Speicher.",
    c2Tag: "Medienprüfung",
    c2Title: "Alte Videos & Audios (+30 Tage)",
    c2Desc: "Prüfen Sie ältere, im Android MediaStore indexierte Mediendateien in Ruhe vor jeder Bereinigung.",
    c3Tag: "100% Sicher",
    c3Title: "Exakte SHA-256-Duplikate",
    c3Desc: "Streaming-SHA-256-Prüfsummenabgleich stellt sicher, dass nur identische Dateien gruppiert werden und ein Original geschützt bleibt.",
    proEyebrow: "Medienstudio & Midiastore Pro",
    proTitle: "Volle Übersicht und Medienwerkzeuge auf Ihrem Android",
    proDesc: "Inklusive QR-Code-Generator, Live-Fotofilter, EXIF/GPS-Datenschutzbereinigung, Fotos-zu-PDF-Konverter (A4), Prüfbericht-Export und Stapellöschung mit Android-Bestätigung.",
    ctrlTitle: "Ihre Dateien, Ihre Entscheidung",
    ctrlDesc: "Midiastore respektiert zu 100% die Scoped-Storage-Vorgaben von Android (kein MANAGE_EXTERNAL_STORAGE). Alle Prüfungen erfolgen lokal auf dem Gerät.",
    privacy: "Datenschutzerklärung",
    successMsg: "🎉 E-Mail für den geschlossenen Test registriert! Bitte schließen Sie nun Schritt 2 (Web-Opt-in) und Schritt 3 (Google Play-Installation) unten ab.",
    duplicateMsg: "✅ Diese E-Mail ist bereits für den geschlossenen Test registriert! Fahren Sie unten mit Schritt 2 (Opt-in) und Schritt 3 (Google Play) fort.",
    errorMsg: "Registrierung momentan nicht möglich. Bitte prüfen Sie Ihre E-Mail-Adresse und versuchen Sie es erneut.",
    connErrorMsg: "Verbindungsfehler. Bitte prüfen Sie Ihre Internetverbindung und versuchen Sie es erneut.",
    btnTools: "🎨 Werkzeuge",
    btnWaTest: "📱 Speicher-Check",
    vipProNav: "Geschlossener Test",
    calcBadge: "⚡ Speicherplatz-Schätzer",
    calcTitle: "Wie viel Medienspeicher können Sie heute prüfen?",
    calcDesc: "Mit der Zeit sammeln sich doppelte Bilder, alte Sprachnotizen und große Videos an. Berechnen Sie Ihre Schätzung:",
    calcTime: "Nutzungsdauer des Smartphones:",
    calcOptT1: "6 Monate",
    calcOptT2: "1 Jahr",
    calcOptT3: "2 Jahre",
    calcOptT4: "3 Jahre oder länger",
    calcGroups: "Tägliche Gruppen & Chats:",
    calcOptG1: "Wenig (1 bis 3 Gruppen)",
    calcOptG2: "Moderat (4 bis 10 Gruppen)",
    calcOptG3: "Sehr aktiv (Mehr als 10 Gruppen)",
    calcEst: "Geschätztes Medienvolumen zur Prüfung:",
    calcSub: "Duplikate, große Videos und alte Audiodateien, die Ihren Speicher belegen.",
    calcCta: "Diesen Speicher kostenlos prüfen",
    liveTitle: "Live Pre-Launch Aktivität",
    liveVisitsLabel: "Erfasste Besuche:",
    liveWaitlistLabel: "Warteliste / Tester:",
    live24hLabel: "Besuche (24 Std.):",
    shareBtn: "🚀 Freunde einladen (Pre-Launch)",
    shareCopied: "🔗 Empfehlungslink kopiert! Teilen Sie ihn per WhatsApp oder Telegram.",
    shareText: "Ich teste Midiastore vorab — große Dateien und exakte SHA-256-Duplikate sicher auf Android prüfen!"
  },
  fr: {
    code: "fr-FR",
    title: "Midiastore — Vérifiez et libérez de l'espace Android en toute sécurité",
    desc: "Trouvez les fichiers volumineux et les doublons exacts SHA-256 parmi les médias accessibles de votre Android. Accès anticipé gratuit au test fermé Google Play !",
    eyebrow: "Mémoire saturée ? Retrouvez la clarté",
    h1: "Découvrez ce qui prend de la place.<br><em>Vous gardez le contrôle total.</em>",
    lead: "Midiastore analyse les médias accessibles de votre Android pour repérer les vidéos lourdes, les anciens fichiers audio et les doublons exacts SHA-256.",
    ctaHero: "Rejoindre le test fermé (Accès Gratuit)",
    ctaHeroSub: "Ouvrir l'invitation Google Play",
    note: "L'analyse, la vérification et le Studio Média sont gratuits. Toute suppression nécessite votre confirmation native Android.",
    vipBadge: "⭐ Test Fermé Officiel Google Play (14 Jours • Objectif : 12+ Testeurs Actifs, 15+ Idéal)",
    vipTitle: "Accès Anticipé Gratuit au Test Fermé en 3 Étapes",
    vipDesc: "Google Play exige au moins 12 testeurs actifs pendant 14 jours continus (objectif idéal : 15+ testeurs) avant le lancement public. Saisissez votre e-mail Google Play ci-dessous pour obtenir un <strong>Accès Anticipé Gratuit au Test Fermé</strong> !",
    emailPlaceholder: "Votre e-mail Google Play (ex: nom@gmail.com)",
    btnSubmit: "1. Autoriser mon e-mail Google Play",
    step1Badge: "Étape 1 • Autorisation",
    step1Title: "Enregistrez votre Gmail Google Play",
    step1Desc: "Saisissez votre e-mail Google Play dans le formulaire ci-dessus afin d'autoriser votre compte sur la liste officielle du test fermé.",
    step1Btn: "Renseigner mon Gmail ci-dessus",
    step2Badge: "Étape 2 • Opt-in Officiel",
    step2Title: "Acceptez l'invitation Web Google Play",
    step2Desc: "Ouvrez la page officielle de test Google Play et appuyez sur « Devenir testeur » (Become a tester) pour activer le téléchargement.",
    step2Btn: "Accepter l'invitation (Opt-in)",
    step3Badge: "Étape 3 • Installation (14 Jours)",
    step3Title: "Installez depuis Google Play Store",
    step3Desc: "Après avoir accepté l'invitation à l'étape 2, téléchargez Midiastore sur Google Play et conservez-le pendant 14 jours.",
    step3Btn: "Ouvrir Google Play Store",
    stepNote: "💡 Important : L'application étant en test fermé (14 jours), le lien Google Play (Étape 3) ne s'ouvre qu'après autorisation de votre e-mail (Étape 1) et validation de l'opt-in web (Étape 2).",
    c1Tag: "Détecter",
    c1Title: "Fichiers volumineux (>10 Mo)",
    c1Desc: "Repérez rapidement les vidéos lourdes, enregistrements audio et images occupant votre espace partagé accessible.",
    c2Tag: "Révision Média",
    c2Title: "Anciens audios et vidéos (+30 jours)",
    c2Desc: "Parcourez les anciens médias indexés dans Android MediaStore afin de vérifier chaque élément avant suppression.",
    c3Tag: "100% Sécurisé",
    c3Title: "Doublons exacts SHA-256",
    c3Desc: "Comparaison binaire SHA-256 octet par octet garantissant qu'un exemplaire original reste toujours protégé.",
    proEyebrow: "Studio Média & Midiastore Pro",
    proTitle: "Studio média complet et résumé d'analyse sur Android",
    proDesc: "Inclut générateur de QR Code, filtres photo en direct, suppression des métadonnées EXIF/GPS, convertisseur photo vers PDF A4, export du résumé et suppression par lot avec confirmation Android.",
    ctrlTitle: "Vos fichiers, votre décision souveraine",
    ctrlDesc: "Midiastore respecte scrupuleusement la politique Scoped Storage d'Android (zéro MANAGE_EXTERNAL_STORAGE). Tout reste 100% local sur votre appareil.",
    privacy: "Politique de confidentialité",
    successMsg: "🎉 E-mail enregistré pour le test fermé ! Passez maintenant à l'Étape 2 (Opt-in Web) et à l'Étape 3 (Installation Google Play) ci-dessous.",
    duplicateMsg: "✅ Cet e-mail figure déjà sur la liste du test fermé ! Passez directement aux Étapes 2 et 3 ci-dessous.",
    errorMsg: "Enregistrement indisponible pour l'instant. Vérifiez votre adresse e-mail et réessayez.",
    connErrorMsg: "Erreur de connexion. Vérifiez votre réseau et réessayez.",
    btnTools: "🎨 Outils Média",
    btnWaTest: "📱 Diagnostic",
    vipProNav: "Test Fermé",
    calcBadge: "⚡ Simulateur d'Espace Occupé",
    calcTitle: "Combien d'espace média pouvez-vous vérifier aujourd'hui ?",
    calcDesc: "Les doublons, notes vocales et vidéos lourdes s'accumulent au fil des mois. Faites une estimation :",
    calcTime: "Durée d'utilisation du téléphone :",
    calcOptT1: "6 mois",
    calcOptT2: "1 an",
    calcOptT3: "2 ans",
    calcOptT4: "3 ans ou plus",
    calcGroups: "Groupes et conversations quotidiennes :",
    calcOptG1: "Faible (1 à 3 groupes)",
    calcOptG2: "Modéré (4 à 10 groupes)",
    calcOptG3: "Intense (Plus de 10 groupes)",
    calcEst: "Volume Média Estimé à Vérifier :",
    calcSub: "Médias dupliqués, vidéos volumineuses et anciens fichiers audio sur votre appareil.",
    calcCta: "Vérifier cet espace gratuitement",
    liveTitle: "Activité Pré-Lancement en Direct",
    liveVisitsLabel: "Visites enregistrées :",
    liveWaitlistLabel: "Liste d'attente / testeurs :",
    live24hLabel: "Visites (24h) :",
    shareBtn: "🚀 Inviter des amis (Pré-Lancement)",
    shareCopied: "🔗 Lien copié ! Partagez-le sur WhatsApp ou Telegram.",
    shareText: "Découvrez Midiastore en pré-lancement pour repérer les gros fichiers et doublons exacts SHA-256 sur Android !"
  },
  hi: {
    code: "hi-IN",
    title: "Midiastore — अपने एंड्रॉइड स्टोरेज की सुरक्षित जांच और सफाई करें",
    desc: "अपने एंड्रॉइड फोन में बड़ी फाइलों और सटीक SHA-256 डुप्लिकेट मीडिया को पहचानें। Google Play क्लोज्ड टेस्टिंग में मुफ़्त अर्ली एक्सेस पाएं!",
    eyebrow: "स्टोरेज फुल हो गया है? सुरक्षित समीक्षा से शुरुआत करें",
    h1: "देखें कि क्या जगह ले रहा है।<br><em>क्या रखना है, निर्णय आपका है।</em>",
    lead: "Midiastore आपके एंड्रॉइड फोन में उपलब्ध बड़ी वीडियो फाइलों, पुराने ऑडियो और सटीक SHA-256 डुप्लिकेट फोटो को सुरक्षित रूप से ढूंढता है।",
    ctaHero: "क्लोज्ड टेस्ट में शामिल हों (मुफ़्त अर्ली एक्सेस)",
    ctaHeroSub: "Google Play ऑप्ट-इन खोलें",
    note: "विश्लेषण, समीक्षा और मीडिया स्टूडियो टूल मुफ़्त हैं। फ़ाइल हटाने के लिए हमेशा एंड्रॉइड सिस्टम की पुष्टि आवश्यक है।",
    vipBadge: "⭐ Google Play आधिकारिक क्लोज्ड टेस्टिंग (14 दिन • लक्ष्य: 12+ सक्रिय टेस्टर, 15+ आदर्श)",
    vipTitle: "3 आसान चरणों में क्लोज्ड टेस्ट का मुफ़्त अर्ली एक्सेस पाएं",
    vipDesc: "पब्लिक लॉन्च से पहले Google Play को 14 दिनों के लिए कम से कम 12 सक्रिय परीक्षकों (15+ आदर्श लक्ष्य) की आवश्यकता होती है। <strong>क्लोज्ड टेस्ट में मुफ़्त अर्ली एक्सेस</strong> के लिए नीचे अपना Google Play ईमेल दर्ज करें!",
    emailPlaceholder: "आपका Google Play ईमेल (उदा: name@gmail.com)",
    btnSubmit: "1. मेरा Google Play ईमेल अधिकृत करें",
    step1Badge: "चरण 1 • अनुमति",
    step1Title: "अपना Google Play Gmail दर्ज करें",
    step1Desc: "आधिकारिक क्लोज्ड टेस्टिंग ट्रैक में अपने खाते को अधिकृत करने के लिए ऊपर दिए गए फॉर्म में अपना Google Play ईमेल सबमिट करें।",
    step1Btn: "ऊपर अपना Gmail दर्ज करें",
    step2Badge: "चरण 2 • आधिकारिक ऑप्ट-इन",
    step2Title: "Google Play वेब आमंत्रण स्वीकार करें",
    step2Desc: "आधिकारिक Google Play परीक्षण पेज खोलें और डाउनलोड सक्रिय करने के लिए \"Become a tester\" पर टैप करें।",
    step2Btn: "वेब ऑप्ट-इन खोलें",
    step3Badge: "चरण 3 • इंस्टॉल करें (14 दिन)",
    step3Title: "Google Play Store से इंस्टॉल करें",
    step3Desc: "चरण 2 में आमंत्रण स्वीकार करने के बाद, Google Play से Midiastore इंस्टॉल करें और इसे 14 दिनों तक इंस्टॉल रखें।",
    step3Btn: "Google Play Store खोलें",
    stepNote: "💡 महत्वपूर्ण: चूंकि ऐप क्लोज्ड टेस्टिंग (14 दिन) में है, इसलिए Google Play लिंक (चरण 3) तभी खुलेगा जब आपका ईमेल अधिकृत हो (चरण 1) और आपने वेब ऑप्ट-इन (चरण 2) स्वीकार कर लिया हो।",
    c1Tag: "पहचानें",
    c1Title: "बड़ी मीडिया फाइलें (>10 MB)",
    c1Desc: "उपलब्ध स्टोरेज में जगह घेरने वाले बड़े वीडियो, ऑडियो और डाउनलोड की तुरंत पहचान करें।",
    c2Tag: "मीडिया समीक्षा",
    c2Title: "पुराने वीडियो और ऑडियो (+30 दिन)",
    c2Desc: "Android MediaStore में इंडेक्स की गई पुरानी मीडिया फाइलों की समीक्षा करें और तय करें कि क्या हटाना है।",
    c3Tag: "100% सुरक्षित",
    c3Title: "SHA-256 सटीक डुप्लिकेट",
    c3Desc: "बाइट-दर-बाइट SHA-256 जांच सुनिश्चित करती है कि केवल समान कॉपी ही चुनी जाएं और एक मूल कॉपी हमेशा सुरक्षित रहे।",
    proEyebrow: "मीडिया स्टूडियो और Midiastore Pro",
    proTitle: "संपूर्ण मीडिया स्टूडियो और सुरक्षित स्टोरेज समीक्षा",
    proDesc: "इसमें QR कोड जनरेटर, लाइव फोटो फिल्टर, EXIF/GPS गोपनीयता सुरक्षा, फोटो-टू-PDF A4 कनवर्टर, समीक्षा सारांश निर्यात और एंड्रॉइड पुष्टि के साथ बैच क्लीनअप शामिल है।",
    ctrlTitle: "आपकी फाइलें, आपका पूर्ण नियंत्रण",
    ctrlDesc: "Midiastore एंड्रॉइड Scoped Storage नीतियों का पूरी तरह पालन करता है (शून्य MANAGE_EXTERNAL_STORAGE)। सभी जांच आपके फोन पर ही होती है।",
    privacy: "गोपनीयता नीति",
    successMsg: "🎉 ईमेल पंजीकृत हो गया! अब नीचे चरण 2 (वेब ऑप्ट-इन) और चरण 3 (Google Play इंस्टॉल) पूरा करें।",
    duplicateMsg: "✅ यह ईमेल पहले से ही क्लोज्ड टेस्टिंग सूची में है! कृपया नीचे चरण 2 (ऑप्ट-इन) और चरण 3 (Google Play) पर जाएं।",
    errorMsg: "अभी पंजीकरण नहीं हो सका। कृपया अपना ईमेल पता जांचें और पुनः प्रयास करें।",
    connErrorMsg: "कनेक्शन त्रुटि। कृपया अपना इंटरनेट जांचें और पुनः प्रयास करें।",
    btnTools: "🎨 मीडिया टूल्स",
    btnWaTest: "📱 स्टोरेज जांच",
    vipProNav: "क्लोज्ड टेस्ट",
    calcBadge: "⚡ मीडिया स्टोरेज अनुमानक",
    calcTitle: "आज आप कितने मीडिया स्टोरेज की समीक्षा कर सकते हैं?",
    calcDesc: "समय के साथ डुप्लिकेट फोटो, पुराने वॉइस नोट्स और बड़े वीडियो जमा हो जाते हैं। अपना अनुमान लगाएं:",
    calcTime: "डिवाइस उपयोग का समय:",
    calcOptT1: "6 महीने",
    calcOptT2: "1 साल",
    calcOptT3: "2 साल",
    calcOptT4: "3 साल या अधिक",
    calcGroups: "दैनिक ग्रुप्स और चैट एक्टिविटी:",
    calcOptG1: "कम (1 से 3 ग्रुप्स)",
    calcOptG2: "मध्यम (4 से 10 ग्रुप्स)",
    calcOptG3: "अधिक (10+ ग्रुप्स)",
    calcEst: "समीक्षा के लिए अनुमानित मीडिया स्टोरेज:",
    calcSub: "पुरानी ऑडियो फाइलें, बड़े वीडियो और डुप्लिकेट मीडिया जो आपका स्टोरेज घेर रहे हैं।",
    calcCta: "मुफ्त में स्टोरेज की समीक्षा करें",
    liveTitle: "लाइव प्री-लॉन्च गतिविधि",
    liveVisitsLabel: "दर्ज विज़िट:",
    liveWaitlistLabel: "प्रतीक्षा सूची / टेस्टर्स:",
    live24hLabel: "24 घंटे में विज़िट:",
    shareBtn: "🚀 दोस्तों को आमंत्रित करें (प्री-लॉन्च)",
    shareCopied: "🔗 शेयर लिंक कॉपी हो गया! WhatsApp या Telegram पर भेजें।",
    shareText: "Android पर बड़ी फाइलों और SHA-256 डुप्लिकेट को सुरक्षित रूप से जांचने के लिए Midiastore प्री-लॉन्च देखें!"
  },
  ja: {
    code: "ja-JP",
    title: "Midiastore — Androidの空き容量を安全に確認・整理",
    desc: "Android端末内の大容量ファイルやSHA-256完全一致の重複メディアを安全に検出。Google Playクローズドテストの無料先行アクセスに参加しましょう！",
    eyebrow: "ストレージがいっぱい？ まずは安全な確認から",
    h1: "何が容量を圧迫しているか一目で把握。<br><em>削除の判断はあなた次第。</em>",
    lead: "MidiastoreはAndroid端末内のアクセス可能な大容量動画、古い音声ファイル、SHA-256完全一致の重複写真を検出し、Android標準の確認画面で安全に整理します。",
    ctaHero: "クローズドテストに参加（無料先行アクセス）",
    ctaHeroSub: "Google Play 参加ページを開く",
    note: "分析・確認およびメディアスタジオ機能は無料です。ファイルの削除には必ずAndroidシステムの確認画面が表示されます。",
    vipBadge: "⭐ Google Play 公式クローズドテスト（14日間 • 目標：12+ アクティブテスター、15+ 推奨）",
    vipTitle: "3つの簡単なステップでクローズドテストに無料参加",
    vipDesc: "Google Playの正式公開には最低12名以上（推奨目標15名以上）のテスターによる14日間の継続利用が必要です。下でGoogle Playのメールアドレスを登録し、<strong>公式クローズドテストの無料先行アクセス</strong>を受け取りましょう！",
    emailPlaceholder: "Google Playアカウントのメール (例: you@gmail.com)",
    btnSubmit: "1. Google Play メールアドレスを登録",
    step1Badge: "ステップ 1 • アカウント承認",
    step1Title: "Google Play の Gmail を登録",
    step1Desc: "上のフォームにGoogle Playのメールアドレスを送信して、公式クローズドテストトラックの承認リストに登録します。",
    step1Btn: "上のフォームに Gmail を入力",
    step2Badge: "ステップ 2 • 公式オプトイン",
    step2Title: "Google Play ウェブ招待を承認",
    step2Desc: "Google Play公式テストページを開き、「テスターになる (Become a tester)」をタップしてダウンロードを有効化します。",
    step2Btn: "ウェブ招待（オプトイン）を開く",
    step3Badge: "ステップ 3 • インストール（14日間）",
    step3Title: "Google Play ストアからインストール",
    step3Desc: "ステップ2の承認後、Google PlayからMidiastoreをインストールし、14日間インストールしたままにしてください。",
    step3Btn: "Google Play ストアを開く",
    stepNote: "💡 重要：クローズドテスト（14日間）のため、Google Playのリンク（ステップ3）は、ステップ1の承認とステップ2の「テスターになる」承認を完了した後に開けるようになります。",
    c1Tag: "検出",
    c1Title: "大容量メディアファイル (>10 MB)",
    c1Desc: "共有ストレージを圧迫している大容量の動画・音声・画像をすばやく一覧表示します。",
    c2Tag: "メディア整理",
    c2Title: "古い動画・音声ファイル (+30日)",
    c2Desc: "Android MediaStoreに記録された古いメディアを確認し、残すものと削除するものを自分で選べます。",
    c3Tag: "安全設計",
    c3Title: "SHA-256 完全一致の重複検出",
    c3Desc: "ストリーミングSHA-256ハッシュ比較により完全に同一のファイルのみを検出し、常に1つの元ファイルを保護します。",
    proEyebrow: "メディアスタジオ & Midiastore Pro",
    proTitle: "便利なメディアスタジオ機能と詳細な確認レポート",
    proDesc: "QRコード作成、ライブ写真フィルター、EXIF/GPS位置情報削除、写真からA4 PDFへの変換、確認レポート出力、Android標準確認付きの一括整理に対応。",
    ctrlTitle: "お客様のファイル、お客様の完全な選択権",
    ctrlDesc: "MidiastoreはAndroidのScoped Storageに完全準拠（MANAGE_EXTERNAL_STORAGE不使用）。すべての処理は端末内で完結します。",
    privacy: "プライバシーポリシー",
    successMsg: "🎉 クローズドテスト用にメールアドレスを登録しました！続いて下のステップ2（ウェブ招待の承認）とステップ3（Google Playでインストール）へ進んでください。",
    duplicateMsg: "✅ このメールアドレスは既にクローズドテストリストに登録済みです！下のステップ2とステップ3へ進んでください。",
    errorMsg: "現在登録できませんでした。メールアドレスを確認してもう一度お試しください。",
    connErrorMsg: "通信エラーが発生しました。インターネット接続を確認して再度お試しください。",
    btnTools: "🎨 ツール",
    btnWaTest: "📱 容量診断",
    vipProNav: "クローズドテスト",
    calcBadge: "⚡ メディア容量シミュレーター",
    calcTitle: "今すぐ見直せるメディア容量はどれくらい？",
    calcDesc: "重複した写真や古い音声ファイル、大きな動画は知らず知らずのうちに蓄積します。推定値をチェック：",
    calcTime: "端末のご利用期間：",
    calcOptT1: "6ヶ月",
    calcOptT2: "1年",
    calcOptT3: "2年",
    calcOptT4: "3年以上",
    calcGroups: "日々のグループ数・チャット頻度：",
    calcOptG1: "少ない (1〜3グループ)",
    calcOptG2: "標準 (4〜10グループ)",
    calcOptG3: "多い (10グループ以上)",
    calcEst: "確認対象の推定メディア容量：",
    calcSub: "重複ファイルや大容量動画、古い音声データが空き容量を占有しています。",
    calcCta: "この容量を今すぐ無料で確認する",
    liveTitle: "リアルタイム先行公開パルス",
    liveVisitsLabel: "累計訪問数:",
    liveWaitlistLabel: "待機リスト / テスター:",
    live24hLabel: "24時間の訪問:",
    shareBtn: "🚀 友達を招待（先行公開）",
    shareCopied: "🔗 紹介リンクをコピーしました！",
    shareText: "Androidの大容量メディアやSHA-256完全重複を安全に確認できるMidiastoreの先行公開をチェック！"
  },
  zh: {
    code: "zh-CN",
    title: "Midiastore — 安全检查与整理您的Android存储空间",
    desc: "在Android设备可访问媒体中精准定位大文件与SHA-256完全重复文件。立即获取Google Play封闭测试免费抢先体验资格！",
    eyebrow: "手机存储空间不足？从安全透明的检查开始",
    h1: "轻松找出占用空间的庞大文件。<br><em>删不删由您完全掌控。</em>",
    lead: "Midiastore扫描Android设备内可访问的媒体文件，帮助您定位大体积视频、陈旧音频及SHA-256完全重复照片，并通过系统原生弹窗确认操作。",
    ctaHero: "加入封闭测试（免费抢先体验）",
    ctaHeroSub: "打开 Google Play 测试邀请",
    note: "存储分析、预览与媒体工作室工具完全免费。任何删除操作均需经过Android系统原生弹窗确认。",
    vipBadge: "⭐ Google Play 官方封闭测试（14天 • 目标：12+ 活跃测试者，15+ 理想目标）",
    vipTitle: "只需 3 步即可免费加入封闭测试抢先体验",
    vipDesc: "Google Play全球正式发布前需要至少12名活跃测试者连续参与14天（理想目标：15名以上）。在下方输入您的Google Play邮箱，即可获得<strong>官方封闭测试免费抢先体验资格</strong>！",
    emailPlaceholder: "您的Google Play邮箱 (如: yourname@gmail.com)",
    btnSubmit: "1. 授权我的 Google Play 邮箱",
    step1Badge: "第 1 步 • 账号授权",
    step1Title: "登记您的 Google Play 邮箱",
    step1Desc: "在上方表单中提交您的 Google Play 邮箱，以便在官方封闭测试名单中为您开通访问权限。",
    step1Btn: "在上方填写我的 Gmail",
    step2Badge: "第 2 步 • 官方测试确认",
    step2Title: "接受 Google Play 网页测试邀请",
    step2Desc: "打开 Google Play 官方测试页面，点击“成为测试人员 (Become a tester)”激活您账号的下载权限。",
    step2Btn: "打开网页测试邀请 (Opt-in)",
    step3Badge: "第 3 步 • 安装并保留 14 天",
    step3Title: "在 Google Play 商店安装应用",
    step3Desc: "完成第 2 步确认后，前往 Google Play 下载安装 Midiastore，并连续保留安装 14 天。",
    step3Btn: "打开 Google Play 商店",
    stepNote: "💡 重要提示：由于应用处于封闭测试阶段（14天），必须先完成第 1 步（邮箱授权）和第 2 步（点击成为测试人员），Google Play 商店链接（第 3 步）才能正常打开。",
    c1Tag: "精准定位",
    c1Title: "超大媒体文件 (>10 MB)",
    c1Desc: "快速浏览可访问共享存储中占用大量空间的视频、音频与高清图片。",
    c2Tag: "媒体整理",
    c2Title: "陈旧视频与音频 (+30天)",
    c2Desc: "查看 Android MediaStore 索引的旧媒体文件，先核对预览再决定是否清理。",
    c3Tag: "百分百安全",
    c3Title: "SHA-256 精确查重验证",
    c3Desc: "采用流式逐字节 SHA-256 哈希比对，仅标记完全相同的文件，并始终保护至少一份原始文件不被选中。",
    proEyebrow: "媒体工作室 & Midiastore Pro",
    proTitle: "内置多功能媒体工作室与整理报告导出",
    proDesc: "包含二维码生成器、实时照片滤镜、EXIF/GPS隐私元数据清除、照片转A4 PDF文档、检查摘要导出，以及带有Android原生确认的批量清理功能。",
    ctrlTitle: "您的数据，您的最终决断",
    ctrlDesc: "Midiastore严格遵守Android Scoped Storage隐私规范（零 MANAGE_EXTERNAL_STORAGE 权限），所有分析均在本地设备完成。",
    privacy: "隐私政策",
    successMsg: "🎉 封闭测试邮箱登记成功！请继续完成下方第 2 步（接受网页邀请）和第 3 步（在 Google Play 安装）。",
    duplicateMsg: "✅ 该邮箱已在我们的封闭测试名单中！请直接前往下方第 2 步和第 3 步。",
    errorMsg: "暂时无法提交登记。请检查您的邮箱地址后重试。",
    connErrorMsg: "网络连接异常。请检查网络后重新提交。",
    btnTools: "🎨 媒体工具",
    btnWaTest: "📱 空间测算",
    vipProNav: "封闭测试",
    calcBadge: "⚡ 媒体空间测算模拟器",
    calcTitle: "今天您可以检查并整理多少存储空间？",
    calcDesc: "日常聊天和拍摄会不断累积重复照片、旧语音和大体积视频。立即测算：",
    calcTime: "手机使用时长：",
    calcOptT1: "6个月",
    calcOptT2: "1年",
    calcOptT3: "2年",
    calcOptT4: "3年以上",
    calcGroups: "日常群聊活跃度：",
    calcOptG1: "较少 (1至3个群组)",
    calcOptG2: "中等 (4至10个群组)",
    calcOptG3: "频繁 (10个以上群组)",
    calcEst: "预估可整理的媒体空间：",
    calcSub: "重复图片、大体积视频和旧音频文件正在占用您的存储空间。",
    calcCta: "立即免费检查此空间",
    liveTitle: "预发布实时动态",
    liveVisitsLabel: "累计访问：",
    liveWaitlistLabel: "候补队列 / 测试人数：",
    live24hLabel: "24小时访问：",
    shareBtn: "🚀 邀请好友（预发布抢先体验）",
    shareCopied: "🔗 邀请链接已复制，快分享到微信、Telegram 或 WhatsApp！",
    shareText: "我正在体验 Midiastore 预发布版：在 Android 上安全清理超大媒体与 SHA-256 完全重复文件！"
  },
  ar: {
    code: "ar",
    dir: "rtl",
    title: "Midiastore — منظف مساحة الأندرويد والاستوديو دون إنترنت",
    desc: "اعثر على الملفات الكبيرة والنسخ المكررة بدقة SHA-256 بدون إنترنت على أندرويد، مع ألعاب أوفلاين وساعة عالمية. احصل على وصول مبكر مجاني للاختبار المغلق!",
    eyebrow: "المساحة ممتلئة؟ ابدأ بمراجعة آمنة دون إنترنت",
    h1: "اكتشف ما يشغل مساحتك.<br><em>أنت تقرر ما تحذفه بأمان.</em>",
    lead: "يفحص Midiastore الوسائط المتاحة على جهازك محلياً 100% للعثور على الفيديوهات الضخمة والصوتيات القديمة والنسخ المكررة SHA-256 بتأكيد نظام أندرويد.",
    ctaHero: "انضم للاختبار المغلق (دخول مجاني)",
    ctaHeroSub: "فتح دعوة Google Play الرسمية",
    note: "الفحص والمراجعة واستوديو الوسائط والألعاب تعمل مجاناً ودون إنترنت. الحذف يتطلب تأكيد أندرويد الرسمي دائماً.",
    vipBadge: "⭐ الاختبار المغلق الرسمي Google Play (14 يوماً • الهدف: 12+ مختبر نشط)",
    vipTitle: "وصول مبكر مجاني للاختبار المغلق في 3 خطوات",
    vipDesc: "يتطلب Google Play 12 مختبراً نشطاً لمدة 14 يوماً. أدخل بريد Gmail الخاص بـ Google Play لتفعيل الوصول المبكر.",
    emailPlaceholder: "بريد Google Play (مثال: name@gmail.com)",
    btnSubmit: "1. تفعيل بريدي في الاختبار المغلق",
    step1Badge: "الخطوة 1 • التفعيل",
    step1Title: "سجّل بريد Google Play",
    step1Desc: "أرسل بريدك في النموذج أعلاه لإضافته لقائمة المختبرين الرسمية في Play Console.",
    step1Btn: "إدخال البريد أعلاه",
    step2Badge: "الخطوة 2 • الانضمام الرسمي",
    step2Title: "اقبل دعوة الويب في Google Play",
    step2Desc: "افتح صفحة الاختبار الرسمية واضغط \"Become a tester\" لتفعيل التحميل.",
    step2Btn: "قبول الدعوة (Opt-in)",
    step3Badge: "الخطوة 3 • التثبيت (14 يوماً)",
    step3Title: "ثبّت التطبيق من Google Play",
    step3Desc: "بعد الخطوة 2، حمّل Midiastore وأبقه مثبتاً 14 يوماً متواصلة.",
    step3Btn: "فتح متجر Google Play",
    stepNote: "💡 هام: رابط المتجر (الخطوة 3) يفتح بعد تسجيل بريدك (الخطوة 1) وقبول الدعوة (الخطوة 2).",
    c1Tag: "اكتشاف ذكي",
    c1Title: "ملفات كبيرة (>10 MB) وتنظيف أوفلاين",
    c1Desc: "راجع الفيديوهات والملفات الضخمة دون الحاجة لأي اتصال بالإنترنت.",
    c2Tag: "مراجعة الوسائط",
    c2Title: "فيديوهات وصوتيات قديمة (+30 يوماً)",
    c2Desc: "تصفح ملفات واتساب والكاميرا القديمة وقرر ما تحتفظ به.",
    c3Tag: "آمن 100%",
    c3Title: "مكررات SHA-256 دقيقة + وضع سهل",
    c3Desc: "تحقق بايت ببايت مع حماية نسخة أصلية واحدة دائماً، ووضع خطوط كبيرة لكبار السن.",
    proEyebrow: "استوديو الوسائط • ألعاب أوفلاين • Pro VIP",
    proTitle: "تنظيف الهاتف، ألعاب بدون نت، ساعة عالمية ومحول عملات.",
    proDesc: "يتضمن ألعاب الثعبان والكتل وبحث الكلمات Pro دون إنترنت، ساعة 16 دولة، QR احترافي ومفكرة مشفرة.",
    ctrlTitle: "ملفاتك، قرارك 100% محلياً",
    ctrlDesc: "يعمل Midiastore وفق Scoped Storage الصارم (صفر MANAGE_EXTERNAL_STORAGE). خصوصية كاملة على جهازك.",
    privacy: "سياسة الخصوصية",
    successMsg: "🎉 تم تسجيل بريدك! أكمل الخطوة 2 والخطوة 3 بالأسفل.",
    duplicateMsg: "✅ هذا البريد مسجل بالفعل! انتقل للخطوة 2 والخطوة 3 بالأسفل.",
    errorMsg: "تعذر التسجيل الآن. تحقق من البريد الإلكتروني وحاول مجدداً.",
    connErrorMsg: "خطأ في الاتصال. تحقق من الإنترنت وحاول مجدداً.",
    btnTools: "🎨 الأدوات",
    btnWaTest: "📱 التشخيص",
    vipProNav: "الاختبار المغلق",
    calcBadge: "⚡ حاسبة المساحة المشغولة",
    calcTitle: "كم مساحة يمكنك توفيرها اليوم؟",
    calcDesc: "احسب حجم الفيديوهات المكررة وملفات الدردشة المتراكمة:",
    calcTime: "مدة استخدام الهاتف:",
    calcOptT1: "6 أشهر",
    calcOptT2: "سنة واحدة",
    calcOptT3: "سنتان",
    calcOptT4: "3 سنوات فأكثر",
    calcGroups: "نشاط المجموعات اليومي:",
    calcOptG1: "قليل (1-3 مجموعات)",
    calcOptG2: "متوسط (4-10 مجموعات)",
    calcOptG3: "كثيف (+10 مجموعات)",
    calcEst: "المساحة المقدرة للمراجعة:",
    calcSub: "صور مكررة وفيديوهات كبيرة وملفات قديمة تشغل ذاكرتك.",
    calcCta: "مراجعة هذه المساحة مجاناً",
    liveTitle: "نبض الإطلاق المباشر",
    liveVisitsLabel: "الزيارات:",
    liveWaitlistLabel: "قائمة المختبرين:",
    live24hLabel: "زيارات (24 ساعة):",
    shareBtn: "🚀 دعوة الأصدقاء",
    shareCopied: "🔗 تم نسخ رابط الدعوة!",
    shareText: "جرّب Midiastore لتنظيف مساحة أندرويد واكتشاف المكررات SHA-256 بأمان ودون إنترنت!"
  },
  ru: {
    code: "ru",
    title: "Midiastore — Очистка памяти Android и офлайн-инструменты",
    desc: "Находите крупные файлы и точные дубликаты SHA-256 без интернета, а также играйте в офлайн-игры и используйте мировые часы.",
    eyebrow: "Память заполнена? Безопасная ревизия без интернета",
    h1: "Узнайте, что занимает память.<br><em>Вы сами решаете, что удалить.</em>",
    lead: "Midiastore 100% локально находит тяжёлые видео, старые аудио и точные копии SHA-256 с официальным подтверждением Android.",
    ctaHero: "Участвовать в закрытом тесте (бесплатно)",
    ctaHeroSub: "Открыть приглашение Google Play",
    note: "Анализ, Студия медиа и офлайн-игры бесплатны. Удаление всегда требует системного подтверждения Android.",
    vipBadge: "⭐ Закрытое тестирование Google Play (14 дней • Цель: 12+ активных тестеров)",
    vipTitle: "Бесплатный ранний доступ за 3 простых шага",
    vipDesc: "Укажите Gmail от Google Play ниже, чтобы получить ранний доступ к закрытому тестированию.",
    emailPlaceholder: "Ваш Gmail из Google Play (name@gmail.com)",
    btnSubmit: "1. Авторизовать мой Gmail",
    step1Badge: "Шаг 1 • Заявка",
    step1Title: "Укажите Gmail от Google Play",
    step1Desc: "Отправьте почту для добавления в официальный список тестеров.",
    step1Btn: "Ввести Gmail выше",
    step2Badge: "Шаг 2 • Подтверждение",
    step2Title: "Примите веб-приглашение Google Play",
    step2Desc: "Нажмите \"Become a tester\" на официальной странице Google Play.",
    step2Btn: "Принять приглашение (Opt-in)",
    step3Badge: "Шаг 3 • Установка (14 дней)",
    step3Title: "Установите из Google Play",
    step3Desc: "Скачайте Midiastore и оставьте приложение установленным 14 дней.",
    step3Btn: "Открыть Google Play",
    stepNote: "💡 Важно: ссылка магазина (Шаг 3) откроется после Шага 1 и Шага 2.",
    c1Tag: "Поиск",
    c1Title: "Крупные файлы (>10 МБ) офлайн",
    c1Desc: "Находите тяжёлые видео и записи без подключения к интернету.",
    c2Tag: "Ревизия",
    c2Title: "Старые видео и аудио (+30 дней)",
    c2Desc: "Спокойно просмотрите старые медиа из мессенджеров и камеры.",
    c3Tag: "100% защита",
    c3Title: "Точные дубликаты SHA-256",
    c3Desc: "Побайтовая проверка сохраняет один оригинал и предлагает лишние копии.",
    proEyebrow: "Медиа-студия • Офлайн-игры • Pro VIP",
    proTitle: "Очистка без сети, офлайн-игры и мировые утилиты.",
    proDesc: "Змейка, падающие блоки, поиск слов Pro без интернета, часы 16 стран, конвертер валют и QR-студия.",
    ctrlTitle: "Ваши файлы — ваше решение",
    ctrlDesc: "Строгое соответствие Android Scoped Storage (без MANAGE_EXTERNAL_STORAGE). 100% локально.",
    privacy: "Политика конфиденциальности",
    successMsg: "🎉 Почта добавлена! Переходите к Шагу 2 и Шагу 3 ниже.",
    duplicateMsg: "✅ Эта почта уже в списке! Выполните Шаг 2 и Шаг 3 ниже.",
    errorMsg: "Не удалось отправить. Проверьте формат email.",
    connErrorMsg: "Ошибка сети. Проверьте интернет и повторите.",
    btnTools: "🎨 Инструменты",
    btnWaTest: "📱 Диагностика",
    vipProNav: "Закрытый тест",
    calcBadge: "⚡ Калькулятор памяти",
    calcTitle: "Сколько гигабайт можно освободить?",
    calcDesc: "Оцените объём дубликатов и тяжёлых видео на вашем Android:",
    calcTime: "Срок использования телефона:",
    calcOptT1: "6 месяцев",
    calcOptT2: "1 год",
    calcOptT3: "2 года",
    calcOptT4: "3+ года",
    calcGroups: "Чаты и группы:",
    calcOptG1: "Мало (1–3)",
    calcOptG2: "Средне (4–10)",
    calcOptG3: "Много (10+)",
    calcEst: "Оценка памяти для проверки:",
    calcSub: "Дубликаты, старые аудио и тяжёлые видео занимают место.",
    calcCta: "Проверить бесплатно",
    liveTitle: "Статистика пре-запуска",
    liveVisitsLabel: "Визиты:",
    liveWaitlistLabel: "Тестеры:",
    live24hLabel: "За 24 часа:",
    shareBtn: "🚀 Пригласить друзей",
    shareCopied: "🔗 Ссылка скопирована!",
    shareText: "Тестирую Midiastore: офлайн-очистка Android и поиск точных дубликатов SHA-256!"
  },
  id: {
    code: "id",
    title: "Midiastore — Pembersih Penyimpanan Android & Game Offline",
    desc: "Temukan file besar dan duplikat persis SHA-256 tanpa internet di Android, plus game offline, jam dunia & alat Pro.",
    eyebrow: "Memori penuh? Mulai tinjau dengan aman & offline",
    h1: "Temukan apa yang memenuhi ruang.<br><em>Anda yang memutuskan apa yang dihapus.</em>",
    lead: "Midiastore memindai media Android 100% di perangkat secara offline untuk menemukan video besar dan duplikat persis SHA-256 dengan konfirmasi sistem.",
    ctaHero: "Ikut Tes Tertutup (Akses Gratis)",
    ctaHeroSub: "Buka Undangan Google Play",
    note: "Analisis, Studio Media, dan game offline gratis. Penghapusan selalu memakai dialog konfirmasi resmi Android.",
    vipBadge: "⭐ Tes Tertutup Resmi Google Play (14 Hari • Target: 12+ Penguji Aktif)",
    vipTitle: "Akses Awal Gratis Tes Tertutup dalam 3 Langkah",
    vipDesc: "Daftarkan email Google Play Anda untuk membuka undangan tes tertutup resmi selama 14 hari.",
    emailPlaceholder: "Email Google Play Anda (contoh@gmail.com)",
    btnSubmit: "1. Daftarkan Gmail Saya",
    step1Badge: "Langkah 1 • Pendaftaran",
    step1Title: "Kirim Gmail Google Play",
    step1Desc: "Masukkan alamat Gmail untuk diotorisasi pada daftar penguji tertutup.",
    step1Btn: "Isi Gmail di Atas",
    step2Badge: "Langkah 2 • Opt-in Resmi",
    step2Title: "Terima Undangan Web Google Play",
    step2Desc: "Klik \"Become a tester\" pada halaman pengujian resmi Google Play.",
    step2Btn: "Terima Undangan",
    step3Badge: "Langkah 3 • Pasang (14 Hari)",
    step3Title: "Instal dari Google Play Store",
    step3Desc: "Unduh Midiastore dan pertahankan terpasang selama 14 hari berturut-turut.",
    step3Btn: "Buka Google Play Store",
    stepNote: "💡 Penting: Tautan Play Store (Langkah 3) aktif setelah Langkah 1 & 2 selesai.",
    c1Tag: "Temukan",
    c1Title: "File Besar (>10 MB) Offline",
    c1Desc: "Lihat video dan file besar dengan cepat bahkan dalam mode pesawat.",
    c2Tag: "Tinjau Media",
    c2Title: "Video & Audio Lama (+30 Hari)",
    c2Desc: "Periksa file media lama dari kamera dan chat sebelum menghapus.",
    c3Tag: "100% Aman",
    c3Title: "Duplikat Persis SHA-256",
    c3Desc: "Verifikasi biner melindungi 1 file asli secara otomatis dan aman.",
    proEyebrow: "Studio Media • Game Offline • Pro VIP",
    proTitle: "Bersihkan HP, main game tanpa kuota, jam dunia & alat Pro.",
    proDesc: "Termasuk Snake retro, Falling Blocks, Cari Kata Pro offline, jam 16 negara, konverter mata uang & Mode Mudah Lansia.",
    ctrlTitle: "File Anda, Keputusan Anda",
    ctrlDesc: "Mematuhi Android Scoped Storage penuh (nol MANAGE_EXTERNAL_STORAGE). Semua proses 100% lokal.",
    privacy: "Kebijakan Privasi",
    successMsg: "🎉 Email terdaftar! Lanjutkan ke Langkah 2 dan Langkah 3 di bawah.",
    duplicateMsg: "✅ Email ini sudah terdaftar! Lanjut ke Langkah 2 & Langkah 3 di bawah.",
    errorMsg: "Gagal mendaftar. Periksa alamat email Anda.",
    connErrorMsg: "Gangguan koneksi. Periksa internet Anda.",
    btnTools: "🎨 Alat Media",
    btnWaTest: "📱 Diagnosa",
    vipProNav: "Tes Tertutup",
    calcBadge: "⚡ Kalkulator Penyimpanan",
    calcTitle: "Berapa GB ruang yang bisa Anda tinjau?",
    calcDesc: "Estimasi ukuran video besar dan duplikat dari obrolan harian:",
    calcTime: "Lama pemakaian HP:",
    calcOptT1: "6 bulan",
    calcOptT2: "1 tahun",
    calcOptT3: "2 tahun",
    calcOptT4: "3+ tahun",
    calcGroups: "Aktivitas grup harian:",
    calcOptG1: "Ringan (1–3 grup)",
    calcOptG2: "Sedang (4–10 grup)",
    calcOptG3: "Ramai (10+ grup)",
    calcEst: "Perkiraan Ruang Media:",
    calcSub: "Media duplikat dan video besar memenuhi memori Anda.",
    calcCta: "Tinjau Ruang Gratis",
    liveTitle: "Aktivitas Pra-Rilis Langsung",
    liveVisitsLabel: "Kunjungan:",
    liveWaitlistLabel: "Penguji:",
    live24hLabel: "24 jam terakhir:",
    shareBtn: "🚀 Ajak Teman",
    shareCopied: "🔗 Tautan disalin!",
    shareText: "Coba Midiastore untuk cek file besar dan duplikat SHA-256 Android secara offline & aman!"
  },
  bn: {
    code: "bn",
    title: "Midiastore — অফলাইন অ্যান্ড্রয়েড স্টোরেজ ক্লিনার ও গেমস",
    desc: "ইন্টারনেট ছাড়াই বড় ফাইল ও SHA-256 ডুপ্লিকেট খুঁজুন, সাথে অফলাইন গেম, বিশ্ব ঘড়ি ও মিডিয়া স্টুডিও।",
    eyebrow: "ফোন মেমরি ফুল? অফলাইনে নিরাপদ রিভিউ শুরু করুন",
    h1: "কোন ফাইলে জায়গা ভরেছে দেখুন।<br><em>ডিলিট করার সিদ্ধান্ত আপনার।</em>",
    lead: "Midiastore ১০০% আপনার ডিভাইসেই বড় ভিডিও, পুরনো অডিও এবং SHA-256 ডুপ্লিকেট শনাক্ত করে।",
    ctaHero: "ক্লোজড টেস্টে যোগ দিন (ফ্রি)",
    ctaHeroSub: "Google Play আমন্ত্রণ খুলুন",
    note: "স্ক্যান, মিডিয়া স্টুডিও ও অফলাইন গেম সম্পূর্ণ ফ্রি। ফাইল মুছতে Android সিস্টেম কনফার্মেশন লাগে।",
    vipBadge: "⭐ অফিসিয়াল Google Play ক্লোজড টেস্টিং (১৪ দিন • লক্ষ্য: ১২+ টেস্টার)",
    vipTitle: "৩ ধাপে ফ্রি ক্লোজড টেস্ট আর্লি অ্যাক্সেস",
    vipDesc: "১৪ দিনের ক্লোজড টেস্টের জন্য নিচে আপনার Google Play Gmail দিন।",
    emailPlaceholder: "আপনার Google Play Gmail (name@gmail.com)",
    btnSubmit: "১. আমার Gmail যুক্ত করুন",
    step1Badge: "ধাপ ১ • নিবন্ধন",
    step1Title: "Google Play Gmail দিন",
    step1Desc: "উপরের ফর্মে ইমেইল পাঠিয়ে টেস্টার তালিকায় অনুমোদন নিন।",
    step1Btn: "উপরে Gmail লিখুন",
    step2Badge: "ধাপ ২ • অফিসিয়াল সম্মতি",
    step2Title: "Google Play ওয়েব আমন্ত্রণ গ্রহণ করুন",
    step2Desc: "অফিসিয়াল টেস্টিং পেজে \"Become a tester\" বাটনে চাপুন।",
    step2Btn: "আমন্ত্রণ গ্রহণ (Opt-in)",
    step3Badge: "ধাপ ৩ • ইনস্টল (১৪ দিন)",
    step3Title: "Google Play থেকে ইনস্টল করুন",
    step3Desc: "ধাপ ২ শেষ করে অ্যাপটি ডাউনলোড করুন ও ১৪ দিন রাখুন।",
    step3Btn: "Google Play খুলুন",
    stepNote: "💡 দ্রষ্টব্য: ধাপ ১ ও ধাপ ২ শেষ হলে প্লে স্টোর লিংক কাজ করবে।",
    c1Tag: "শনাক্ত করুন",
    c1Title: "বড় ফাইল (>10 MB) অফলাইন",
    c1Desc: "ইন্টারনেট ছাড়াই বড় ভিডিও ও অডিও খুঁজে দেখুন।",
    c2Tag: "মিডিয়া রিভিউ",
    c2Title: "পুরনো ভিডিও ও অডিও (+৩০ দিন)",
    c2Desc: "ডিলিট করার আগে পুরনো মিডিয়া রিভিউ করুন।",
    c3Tag: "১০০% নিরাপদ",
    c3Title: "SHA-256 নির্ভুল ডুপ্লিকেট",
    c3Desc: "বাইট-বাই-বাইট যাচাই করে মূল ১টি ফাইল সবসময় সুরক্ষিত রাখে।",
    proEyebrow: "মিডিয়া স্টুডিও • অফলাইন গেম • Pro VIP",
    proTitle: "ফোন পরিষ্কার, অফলাইন গেম ও বিশ্ব ঘড়ি একসাথে।",
    proDesc: "অফলাইন স্নেক, ফলিং ব্লকস, Pro ওয়ার্ড সার্চ, ১৬ দেশের সময় ও মুদ্রা রূপান্তর।",
    ctrlTitle: "আপনার ফাইল, আপনার সিদ্ধান্ত",
    ctrlDesc: "Android Scoped Storage অনুসরণ করে (কোনো MANAGE_EXTERNAL_STORAGE নয়)।",
    privacy: "গোপনীয়তা নীতি",
    successMsg: "🎉 ইমেইল যুক্ত হয়েছে! নিচে ধাপ ২ ও ধাপ ৩ সম্পূর্ণ করুন।",
    duplicateMsg: "✅ এই ইমেইল আগে থেকেই তালিকাভুক্ত! ধাপ ২ ও ধাপ ৩-এ যান।",
    errorMsg: "সঠিক ইমেইল দিয়ে আবার চেষ্টা করুন।",
    connErrorMsg: "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।",
    btnTools: "🎨 টুলস",
    btnWaTest: "📱 ডায়াগনস্টিক",
    vipProNav: "ক্লোজড টেস্ট",
    calcBadge: "⚡ স্টোরেজ অনুমানকারী",
    calcTitle: "আজ কত জায়গা রিভিউ করতে পারবেন?",
    calcDesc: "দৈনন্দিন চ্যাটে জমা বড় ভিডিও ও ডুপ্লিকেটের আনুমানিক হিসাব:",
    calcTime: "ফোন ব্যবহারের সময়:",
    calcOptT1: "৬ মাস",
    calcOptT2: "১ বছর",
    calcOptT3: "২ বছর",
    calcOptT4: "৩+ বছর",
    calcGroups: "দৈনিক চ্যাট গ্রুপ:",
    calcOptG1: "কম (১–৩টি)",
    calcOptG2: "মাঝারি (৪–১০টি)",
    calcOptG3: "বেশি (১০+টি)",
    calcEst: "আনুমানিক মিডিয়া স্পেস:",
    calcSub: "ডুপ্লিকেট ছবি ও বড় ভিডিও আপনার জায়গা নিচ্ছে।",
    calcCta: "ফ্রিতে রিভিউ করুন",
    liveTitle: "লাইভ প্রি-লঞ্চ আপডেট",
    liveVisitsLabel: "ভিজিট:",
    liveWaitlistLabel: "টেস্টার তালিকা:",
    live24hLabel: "২৪ ঘণ্টায় ভিজিট:",
    shareBtn: "🚀 বন্ধুদের আমন্ত্রণ জানান",
    shareCopied: "🔗 লিংক কপি হয়েছে!",
    shareText: "অফলাইনে অ্যান্ড্রয়েডের বড় ফাইল ও SHA-256 ডুপ্লিকেট খুঁজতে Midiastore দেখুন!"
  },
  ko: {
    code: "ko",
    title: "Midiastore — 오프라인 안드로이드 저장공간 정리 & 스튜디오",
    desc: "인터넷 없이 대용량 파일과 SHA-256 완전 중복 파일 찾기, 오프라인 미니게임, 세계 시계 및 Pro 유틸리티.",
    eyebrow: "저장공간 부족? 오프라인에서 안전하게 점검하세요",
    h1: "용량을 차지하는 파일을 찾으세요.<br><em>삭제 여부는 직접 결정합니다.</em>",
    lead: "Midiastore는 100% 기기 내에서 대용량 동영상, 오래된 오디오, SHA-256 중복 파일을 분석하고 Android 공식 확인으로 보호합니다.",
    ctaHero: "비공개 테스트 참여 (무료 사전 체험)",
    ctaHeroSub: "Google Play 테스트 초대 열기",
    note: "분석, 미디어 스튜디오, 오프라인 게임은 무료이며 모든 삭제는 Android 시스템 확인이 필요합니다.",
    vipBadge: "⭐ Google Play 공식 비공개 테스트 (14일 • 목표: 12명+ 테스터)",
    vipTitle: "3단계로 무료 비공개 테스트 참여하기",
    vipDesc: "Google Play 계정의 Gmail을 등록하여 14일 비공개 테스트 얼리 액세스를 받으세요.",
    emailPlaceholder: "Google Play 이메일 (예: name@gmail.com)",
    btnSubmit: "1. 내 Gmail 테스트 등록",
    step1Badge: "1단계 • 등록",
    step1Title: "Google Play Gmail 등록",
    step1Desc: "위 양식에 이메일을 제출해 공식 테스터 목록에 추가하세요.",
    step1Btn: "위에 Gmail 입력",
    step2Badge: "2단계 • 공식 참여",
    step2Title: "Google Play 웹 초대 수락",
    step2Desc: "공식 테스트 페이지에서 \"Become a tester\" 버튼을 누르세요.",
    step2Btn: "초대 수락 (Opt-in)",
    step3Badge: "3단계 • 설치 (14일 유지)",
    step3Title: "Google Play 스토어에서 설치",
    step3Desc: "2단계 후 Midiastore를 설치하고 14일 동안 유지해 주세요.",
    step3Btn: "Google Play 열기",
    stepNote: "💡 안내: 1단계와 2단계를 완료하면 Play 스토어 링크(3단계)가 활성화됩니다.",
    c1Tag: "정밀 탐색",
    c1Title: "대용량 파일 (>10 MB) 오프라인",
    c1Desc: "비행기 모드에서도 용량이 큰 동영상과 녹음을 빠르게 확인합니다.",
    c2Tag: "미디어 점검",
    c2Title: "오래된 영상·오디오 (+30일)",
    c2Desc: "메신저와 카메라의 오래된 미디어를 삭제 전에 차분히 검토하세요.",
    c3Tag: "100% 안전",
    c3Title: "SHA-256 완전 중복 파일 찾기",
    c3Desc: "바이트 단위 검증으로 원본 1개를 항상 안전하게 보호합니다.",
    proEyebrow: "미디어 스튜디오 • 오프라인 게임 • Pro VIP",
    proTitle: "폰 정리, 와이파이 없는 게임, 세계 시계 & 간편 모드.",
    proDesc: "오프라인 스네이크, 블록 퍼즐, Pro 단어 찾기, 16개국 시계·환율, 시니어 큰 글씨 모드 지원.",
    ctrlTitle: "내 파일, 내 결정",
    ctrlDesc: "Android Scoped Storage 엄격 준수 (MANAGE_EXTERNAL_STORAGE 미사용). 100% 온디바이스.",
    privacy: "개인정보 처리방침",
    successMsg: "🎉 등록 완료! 아래 2단계와 3단계를 이어서 진행해 주세요.",
    duplicateMsg: "✅ 이미 등록된 이메일입니다! 아래 2단계·3단계로 이동하세요.",
    errorMsg: "등록할 수 없습니다. 이메일 주소를 확인해 주세요.",
    connErrorMsg: "네트워크 오류입니다. 연결을 확인 후 다시 시도하세요.",
    btnTools: "🎨 도구",
    btnWaTest: "📱 용량 진단",
    vipProNav: "비공개 테스트",
    calcBadge: "⚡ 예상 정리 용량 계산기",
    calcTitle: "오늘 얼마나 많은 용량을 점검할 수 있을까요?",
    calcDesc: "메신저와 카메라로 쌓인 대용량 영상 및 중복 파일을 계산해 보세요:",
    calcTime: "스마트폰 사용 기간:",
    calcOptT1: "6개월",
    calcOptT2: "1년",
    calcOptT3: "2년",
    calcOptT4: "3년 이상",
    calcGroups: "일일 단체 채팅:",
    calcOptG1: "적음 (1~3개)",
    calcOptG2: "보통 (4~10개)",
    calcOptG3: "많음 (10개 이상)",
    calcEst: "점검 예상 미디어 용량:",
    calcSub: "중복 파일과 대용량 동영상이 저장공간을 차지하고 있습니다.",
    calcCta: "무료로 점검하기",
    liveTitle: "실시간 사전 출시 현황",
    liveVisitsLabel: "누적 방문:",
    liveWaitlistLabel: "대기 / 테스터:",
    live24hLabel: "24시간 방문:",
    shareBtn: "🚀 친구 초대하기",
    shareCopied: "🔗 초대 링크가 복사되었습니다!",
    shareText: "오프라인 Android 대용량·SHA-256 중복 정리 앱 Midiastore 사전 테스트를 확인해보세요!"
  }
};

const KEYWORDS_BY_LANG = {
  pt: "limpador de celular offline, limpar fotos duplicadas sha-256, liberar espaço android, limpador whatsapp, jogo da cobrinha offline, caça-palavras offline, relogio mundial, conversor de moedas offline, estudio de midia android, remover exif gps, qr code wifi pix, modo facil idoso, midiastore",
  en: "offline storage cleaner android, duplicate photo finder sha-256, free up space no internet, whatsapp media cleaner, offline snake game, offline word search pro, world clock currency converter, exif gps privacy cleaner, android media studio, midiastore",
  es: "limpiador de almacenamiento offline, fotos duplicadas sha-256 android, liberar espacio sin internet, limpiador whatsapp, juegos sin internet, reloj mundial conversor monedas, midiastore",
  ar: "منظف التخزين بدون نت, حذف الصور المكررة sha-256, توفير مساحة أندرويد, ألعاب أوفلاين بدون إنترنت, محول عملات وساعة عالمية, تنظيف الواتساب, midiastore",
  ru: "очистка памяти андроид без интернета, поиск дубликатов фото sha-256, офлайн игры, мировые часы конвертер валют, освободить место android, midiastore",
  hi: "offline storage cleaner android, duplicate photo remover sha-256, free up phone space without internet, offline games android, world clock currency converter, midiastore",
  id: "pembersih memori offline android, hapus foto duplikat sha-256, game offline tanpa kuota, jam dunia konverter mata uang, midiastore",
  bn: "অফলাইন স্টোরেজ ক্লিনার, ডুপ্লিকেট ফটো রিমুভার, ইন্টারনেট ছাড়া গেম, বিশ্ব ঘড়ি ও মুদ্রা রূপান্তর, midiastore",
  ko: "오프라인 저장공간 정리, 중복 사진 찾기 sha-256, 인터넷 없는 게임, 세계 시계 환율 계산기, 안드로이드 정리 앱, midiastore",
  ja: "オフライン ストレージ クリーナー, 重複写真 削除 SHA-256, オフライン ゲーム, 世界時計 通貨換算, Android 空き容量, midiastore",
  zh: "离线手机清理, Android 重复照片查找 SHA-256, 无需网络小游戏, 世界时钟 汇率换算, 释放存储空间, midiastore",
  de: "offline speicher reiniger android, doppelte fotos finden sha-256, speicherplatz freigeben ohne internet, offline spiele, weltzeituhr währungsrechner, midiastore",
  fr: "nettoyeur stockage hors ligne android, doublons photo sha-256, libérer espace sans internet, jeux hors ligne, horloge mondiale devises, midiastore"
};

const SEARCH_UI = {
  pt: {
    badge: "🔎 Busca Rápida no Site Midiastore",
    title: "Encontre funções, guias offline e ferramentas em segundos",
    placeholder: "Buscar: whatsapp, duplicatas, sha-256, jogo offline, caça-palavras, relógio, moeda, qr, pdf, idoso...",
    btn: "Buscar",
    googleBtn: "Buscar no Google (site:)",
    countLabel: "resultados encontrados",
    empty: "Nenhum tópico exato — tente termos como: offline, duplicatas, cobrinha, caça-palavras, whatsapp, qr, relógio ou moeda.",
    chips: ["offline", "duplicatas", "whatsapp", "jogo", "caça-palavras", "relógio", "moeda", "qr code", "pdf", "teste"]
  },
  en: {
    badge: "🔎 Search Midiastore Features & Guides",
    title: "Find offline tools, cleanup guides, games & utilities",
    placeholder: "Search: offline, duplicate sha-256, whatsapp, snake, word search, world clock, currency, qr, pdf...",
    btn: "Search",
    googleBtn: "Search Google (site:)",
    countLabel: "results found",
    empty: "No direct match — try: offline, duplicate, whatsapp, snake, word search, clock, currency, qr, pdf.",
    chips: ["offline", "duplicate", "whatsapp", "game", "word search", "clock", "currency", "qr", "pdf", "test"]
  },
  es: {
    badge: "🔎 Búsqueda Rápida en Midiastore",
    title: "Encuentra funciones offline, juegos, limpieza y guías",
    placeholder: "Buscar: offline, duplicados, whatsapp, juegos, sopa de letras, reloj, moneda, qr, pdf...",
    btn: "Buscar",
    googleBtn: "Buscar en Google (site:)",
    countLabel: "resultados encontrados",
    empty: "Sin coincidencias — prueba: offline, duplicados, whatsapp, juego, reloj, moneda, qr.",
    chips: ["offline", "duplicados", "whatsapp", "juego", "reloj", "moneda", "qr"]
  },
  ar: {
    badge: "🔎 بحث سريع داخل موقع Midiastore",
    title: "ابحث عن أدوات التنظيف والألعاب بدون نت والميزات",
    placeholder: "ابحث: أوفلاين، مكرر، واتساب، ألعاب، بحث كلمات، ساعة، عملات، QR...",
    btn: "بحث",
    googleBtn: "بحث Google (site:)",
    countLabel: "نتائج",
    empty: "لا توجد نتائج — جرّب: أوفلاين، مكرر، ألعاب، ساعة، عملات، QR.",
    chips: ["أوفلاين", "مكرر", "واتساب", "ألعاب", "ساعة", "عملات", "QR"]
  },
  ru: {
    badge: "🔎 Быстрый поиск по сайту Midiastore",
    title: "Найдите офлайн-очистку, игры и мировые утилиты",
    placeholder: "Поиск: офлайн, дубликаты, whatsapp, игры, слова, часы, валюта, qr...",
    btn: "Найти",
    googleBtn: "Искать в Google (site:)",
    countLabel: "результатов",
    empty: "Совпадений нет — попробуйте: офлайн, дубликаты, игры, часы, валюта, qr.",
    chips: ["офлайн", "дубликаты", "игры", "часы", "валюта", "qr"]
  },
  id: {
    badge: "🔎 Pencarian Cepat di Situs Midiastore",
    title: "Cari fitur offline, game, pembersih & panduan",
    placeholder: "Cari: offline, duplikat, whatsapp, game, cari kata, jam, mata uang, qr...",
    btn: "Cari",
    googleBtn: "Cari di Google (site:)",
    countLabel: "hasil ditemukan",
    empty: "Tidak ada hasil — coba: offline, duplikat, whatsapp, game, jam, qr.",
    chips: ["offline", "duplikat", "whatsapp", "game", "jam", "qr"]
  },
  bn: {
    badge: "🔎 Midiastore সাইট দ্রুত খুঁজুন",
    title: "অফলাইন ক্লিনার, গেম এবং টুলস মুহূর্তেই খুঁজে পান",
    placeholder: "খুঁজুন: অফলাইন, ডুপ্লিকেট, গেম, ঘড়ি, মুদ্রা, QR...",
    btn: "খুঁজুন",
    googleBtn: "Google-এ খুঁজুন (site:)",
    countLabel: "টি ফলাফল",
    empty: "কিছু পাওয়া যায়নি — চেষ্টা করুন: অফলাইন, ডুপ্লিকেট, গেম, ঘড়ি, QR।",
    chips: ["অফলাইন", "ডুপ্লিকেট", "গেম", "ঘড়ি", "QR"]
  },
  ko: {
    badge: "🔎 Midiastore 사이트 빠른 검색",
    title: "오프라인 정리, 게임, 세계 시계 및 도구를 바로 찾으세요",
    placeholder: "검색: 오프라인, 중복, 게임, 단어 찾기, 시계, 환율, QR...",
    btn: "검색",
    googleBtn: "Google 검색 (site:)",
    countLabel: "개 결과",
    empty: "일치하는 항목 없음 — 오프라인, 중복, 게임, 시계, QR 등을 입력해 보세요.",
    chips: ["오프라인", "중복", "게임", "시계", "환율", "QR"]
  }
};

function getSearchUi(langKey) {
  return SEARCH_UI[langKey] || SEARCH_UI.en;
}

function getSearchCatalog(langKey) {
  const isPt = langKey === "pt";
  return [
    {
      title: isPt ? "Limpeza Real em 1 Toque & Duplicatas SHA-256 (100% Offline)" : "1-Tap Storage Review & Exact SHA-256 Duplicates (100% Offline)",
      desc: isPt ? "Analisa vídeos >10 MB, mídias antigas +30d, áudios e cópias exatas SHA-256 sem internet, mantendo sempre 1 original seguro." : "Scans >10 MB videos, 30d+ old media, and byte-exact SHA-256 duplicates without internet while always keeping 1 original safe.",
      url: "#calculator",
      tags: "offline duplicatas duplicate sha-256 sha256 espaço storage cleaner limpeza limpar arquivos grandes memoria phone cleaner"
    },
    {
      title: isPt ? "Guia Completo: Como Liberar Espaço no WhatsApp Android" : "WhatsApp Media Review & Large Video Clutter Guide",
      desc: isPt ? "Aprenda a localizar vídeos pesados, áudios antigos e mídias encaminhadas repetidas com segurança." : "How to review forwarded WhatsApp videos, voice notes, and duplicate copies safely on Android.",
      url: "/artigos/como-liberar-espaco-whatsapp.html",
      tags: "whatsapp limpar limpeza videos audios antigos duplicadas guide artigo memoria"
    },
    {
      title: isPt ? "Jogos 100% Offline: Cobrinha Retrô, Blocos que Caem & Caça-Palavras Pro" : "100% Offline Games: Retro Snake, Falling Blocks & Pro Word Search",
      desc: isPt ? "Divirta-se no ônibus ou em modo avião sem internet! Inclui Desafio Diário do Caça-Palavras Pro gerado pela data local." : "Play on the bus or airplane without internet! Includes Retro Snake, Falling Blocks puzzle, and date-seeded Daily Pro Word Search.",
      url: "#vip-testers",
      tags: "jogo jogos offline cobrinha snake blocos puzzle tetris caca-palavras caça-palavras word search game no internet pro"
    },
    {
      title: isPt ? "Estúdio de Mídia: Filtros, Privacidade EXIF/GPS, QR Studio & PDF" : "Media Studio: Live Photo Filters, EXIF/GPS Privacy, Designer QR & PDF",
      desc: isPt ? "Aplique filtros, apague localização GPS de fotos antes de enviar, crie QR Code de Wi-Fi/PIX e converta fotos em PDF A4." : "Apply photo filters, strip hidden GPS EXIF metadata before sharing, generate Wi-Fi/PIX QR codes, and combine photos to A4 PDF.",
      url: "/ferramentas.html",
      tags: "estudio studio filtros foto photo exif gps privacidade qr code wifi pix pdf ferramentas tools"
    },
    {
      title: isPt ? "Aba Mundo Offline: Relógio de 16 Países, Conversor de Moedas & Clima" : "Global World Hub: 16-Country Clock, Offline Currency Converter & Travel",
      desc: isPt ? "Veja o horário real no Brasil, Índia, Japão, EUA, Mundo Árabe e Europa offline, converta 16 moedas e explore pontos turísticos." : "Check live world time across 16 countries offline, convert 16 currencies offline or live, check weather, and explore travel spots.",
      url: "#vip-testers",
      tags: "relogio relógio clock horario mundo world moeda currency conversor cambio viagem clima travel offline india arabia"
    },
    {
      title: isPt ? "Salão Secreto de Mágica (21 Cartas + Dado Mágico + Charadas) & Modo Idoso Aa+" : "Curiosity Magic Vault (21-Card Trick, Dice, Riddles) & Senior Easy Mode Aa+",
      desc: isPt ? "Desbloqueie segredos com 5 toques no logo + charadas e ative letras/botões grandes em 1 toque para qualquer idade." : "Unlock hidden math magic tricks & riddles via 5 logo taps, plus 1-tap Senior Easy Mode (Aa+) with large readable typography.",
      url: "#vip-testers",
      tags: "magica mágica magic cartas card dado dice charada riddle segredo idoso senior aa+ acessibilidade facil"
    },
    {
      title: isPt ? "Diagnóstico Gratuito de Armazenamento & Teste Fechado Google Play" : "Free Storage Estimator & Official Google Play Closed Test",
      desc: isPt ? "Calcule quantos gigabytes estão parados no aparelho e participe em 3 passos do pré-lançamento oficial." : "Estimate gigabytes of clutter on your phone and join the official 14-day Google Play Closed Test in 3 steps.",
      url: "/diagnostico.html",
      tags: "diagnostico diagnóstico teste fechado closed test google play opt-in android privacy privacidade scoped storage"
    },
    {
      title: isPt ? "Privacidade 100% On-Device (Zero MANAGE_EXTERNAL_STORAGE)" : "100% On-Device Privacy & Scoped Storage Compliance",
      desc: isPt ? "Seus arquivos nunca saem do aparelho e nada é apagado sem sua confirmação na lixeira oficial do Android." : "Photos, videos, and audio never leave your phone; zero hidden deletions and zero MANAGE_EXTERNAL_STORAGE permission.",
      url: "/privacidade.html",
      tags: "privacidade privacy scoped storage seguranca security permissions google play lixeira trash"
    },
    {
      title: isPt ? "Série de E-books Play Books: Celular Lotado Nunca Mais (PT & EN)" : "Official Play Books Guide: Android Storage Mastery (EN & PT-BR)",
      desc: isPt ? "Guias práticos completos sobre deduplicação SHA-256, privacidade EXIF/GPS e limpeza segura Android 10–16." : "Comprehensive guide to byte-exact SHA-256 deduplication, EXIF/GPS privacy, and Android 10–16 Scoped Storage mastery.",
      url: "/livros.html",
      tags: "ebook livro book play books google play books celular lotado android storage mastery guia manual"
    }
  ];
}

function renderLangNav(currentLang) {
  const links = [
    { id: "pt", label: "PT", url: "/" },
    { id: "en", label: "EN", url: "/en/" },
    { id: "es", label: "ES", url: "/es/" },
    { id: "ar", label: "AR", url: "/ar/" },
    { id: "ru", label: "RU", url: "/ru/" },
    { id: "hi", label: "HI", url: "/hi/" },
    { id: "id", label: "ID", url: "/id/" },
    { id: "bn", label: "BN", url: "/bn/" },
    { id: "ko", label: "KO", url: "/ko/" },
    { id: "de", label: "DE", url: "/de/" },
    { id: "fr", label: "FR", url: "/fr/" },
    { id: "ja", label: "JA", url: "/ja/" },
    { id: "zh", label: "ZH", url: "/zh/" },
  ];

  return `<div class="lang-nav">${links.map(l => `<a href="${l.url}" class="${l.id === currentLang ? "active" : ""}">${l.label}</a>`).join("")}</div>`;
}

function renderHreflang() {
  const routes = [
    { hreflang: "pt-BR", path: "/" },
    { hreflang: "en", path: "/en/" },
    { hreflang: "es", path: "/es/" },
    { hreflang: "ar", path: "/ar/" },
    { hreflang: "ru", path: "/ru/" },
    { hreflang: "hi", path: "/hi/" },
    { hreflang: "id", path: "/id/" },
    { hreflang: "bn", path: "/bn/" },
    { hreflang: "ko", path: "/ko/" },
    { hreflang: "de", path: "/de/" },
    { hreflang: "fr", path: "/fr/" },
    { hreflang: "ja", path: "/ja/" },
    { hreflang: "zh-CN", path: "/zh/" },
    { hreflang: "x-default", path: "/en/" },
  ];
  return routes
    .map((r) => `  <link rel="alternate" hreflang="${r.hreflang}" href="https://midiastory-df6a0.web.app${r.path}">`)
    .join("\n");
}

function generateHtml(langKey, t) {
  const isRoot = langKey === "pt";
  const canonicalUrl = isRoot ? "https://midiastory-df6a0.web.app/" : `https://midiastory-df6a0.web.app/${langKey}/`;
  const dirAttr = t.dir === "rtl" ? ' dir="rtl"' : "";
  const keywords = KEYWORDS_BY_LANG[langKey] || KEYWORDS_BY_LANG.en;
  const searchUi = getSearchUi(langKey);
  const searchCatalog = getSearchCatalog(langKey);

  const schemaAppJson = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "Midiastore",
    alternateName: "Midiastore Offline Cleaner, Studio & Games",
    operatingSystem: "Android 10+",
    applicationCategory: "UtilitiesApplication",
    inLanguage: t.code,
    url: canonicalUrl,
    downloadUrl: PLAY_OPTIN_URL,
    description: t.desc,
    keywords,
    featureList: "Offline SHA-256 Duplicate Finder, Large File Cleaner, WhatsApp Media Review, Live Photo Filters, EXIF Privacy Cleaner, Offline Retro Snake, Offline Falling Blocks, Offline Pro Word Search, 16-Country World Clock, Offline Currency Converter",
    offers: {
      "@type": "Offer",
      price: "0",
      priceCurrency: "USD",
    },
  });

  const schemaWebSiteJson = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: "Midiastore",
    url: "https://midiastory-df6a0.web.app/",
    inLanguage: t.code,
    potentialAction: {
      "@type": "SearchAction",
      target: {
        "@type": "EntryPoint",
        urlTemplate: `${canonicalUrl}?q={search_term_string}`,
      },
      "query-input": "required name=search_term_string",
    },
  });

  const schemaFaqJson = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: [
      {
        "@type": "Question",
        name: isRoot ? "O Midiastore funciona offline sem internet?" : "Does Midiastore work offline without internet?",
        acceptedAnswer: {
          "@type": "Answer",
          text: isRoot
            ? "Sim. A limpeza de arquivos, detecção exata SHA-256, Estúdio de Mídia, Relógio Mundial, conversor com taxas base, Cobrinha, Blocos, Caça-Palavras Pro e truques de mágica funcionam 100% offline no aparelho."
            : "Yes. Storage scanning, exact SHA-256 duplicate detection, Media Studio, World Clock, baseline currency conversion, Retro Snake, Falling Blocks, Pro Word Search, and magic tricks work 100% offline on-device.",
        },
      },
      {
        "@type": "Question",
        name: isRoot ? "O Midiastore apaga arquivos sozinho ou protege os originais?" : "Does Midiastore keep one original file safe?",
        acceptedAnswer: {
          "@type": "Answer",
          text: isRoot
            ? "Nunca apaga sem você confirmar no diálogo oficial do Android (MediaStore / Lixeira) e em duplicatas SHA-256 sempre mantém 1 cópia original desmarcada."
            : "Nothing is ever deleted automatically. Every removal requires the official Android confirmation sheet, and exact SHA-256 duplicate groups always keep at least one original unselected.",
        },
      },
    ],
  });

  return `<!doctype html>
<html lang="${t.code}"${dirAttr}>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#07111f">
  <title>${t.title}</title>
  <meta name="description" content="${t.desc}">
  <meta name="keywords" content="${keywords}">
  <meta name="robots" content="index,follow">
  <link rel="icon" type="image/png" href="/icon-512.png">
  <link rel="canonical" href="${canonicalUrl}">
${renderHreflang()}
  <meta property="og:type" content="website">
  <meta property="og:title" content="${t.title}">
  <meta property="og:description" content="${t.desc}">
  <meta property="og:url" content="${canonicalUrl}">
  <meta property="og:image" content="https://midiastory-df6a0.web.app/icon-512.png">
  <meta property="og:site_name" content="Midiastore">
  <meta name="twitter:card" content="summary">
  <meta name="twitter:title" content="${t.title}">
  <meta name="twitter:description" content="${t.desc}">
  <meta name="twitter:image" content="https://midiastory-df6a0.web.app/icon-512.png">
  <script type="application/ld+json">${schemaAppJson}</script>
  <script type="application/ld+json">${schemaWebSiteJson}</script>
  <script type="application/ld+json">${schemaFaqJson}</script>
  <style>
    :root{--bg:#07111f;--card:#0f1d31;--line:#223652;--text:#edf3fc;--muted:#9ab0ce;--mint:#5de3bf;--white:#fff;--max:980px}
    *{box-sizing:border-box}
    body{margin:0;font-family:system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;background:var(--bg);color:var(--text);line-height:1.55}
    .wrap{width:min(var(--max),calc(100% - 32px));margin:0 auto}
    header{border-bottom:1px solid var(--line);background:rgba(7,17,31,0.92);backdrop-filter:blur(8px);position:sticky;top:0;z-index:20}
    .nav{display:flex;align-items:center;justify-content:space-between;min-height:64px;gap:12px;flex-wrap:wrap;padding:8px 0}
    .brand{font-size:1.25rem;font-weight:900;letter-spacing:-.02em;text-decoration:none;color:var(--white)}
    .brand span{color:var(--mint)}
    .lang-nav{display:flex;gap:6px;align-items:center;flex-wrap:wrap}
    .lang-nav a{text-decoration:none;font-size:0.78rem;font-weight:700;color:var(--muted);padding:4px 8px;border-radius:6px;border:1px solid var(--line);transition:all .15s ease}
    .lang-nav a:hover,.lang-nav a.active{color:var(--white);background:#1a2b42;border-color:var(--mint)}
    .hero{padding:48px 0 24px;text-align:center}
    .eyebrow{color:var(--mint);font-weight:700;font-size:0.88rem;letter-spacing:0.06em;text-transform:uppercase;margin-bottom:8px}
    h1{font-size:clamp(1.9rem,4.8vw,2.9rem);line-height:1.15;letter-spacing:-.03em;margin:0 0 16px}
    h1 em{font-style:normal;color:var(--mint)}
    .lead{font-size:1.1rem;color:var(--muted);max-width:640px;margin:0 auto 24px}
    .actions{display:flex;justify-content:center;gap:12px;flex-wrap:wrap}
    .button{display:inline-flex;align-items:center;justify-content:center;background:var(--mint);color:#041019;font-weight:800;text-decoration:none;padding:12px 24px;border-radius:12px;min-height:48px;border:none;cursor:pointer;font-size:0.95rem;transition:transform .15s,box-shadow .15s}
    .button:hover{transform:translateY(-1px);box-shadow:0 8px 24px rgba(93,227,191,0.35)}
    .button.secondary{background:transparent;color:var(--text);border:1px solid var(--line)}
    .button.secondary:hover{border-color:var(--mint);color:var(--white)}
    .note{font-size:0.84rem;color:var(--muted);margin-top:10px}
    .pulse-strip{margin:20px auto 0;padding:12px 18px;max-width:740px;border:1px solid var(--line);border-radius:16px;background:rgba(15,29,49,0.78);display:flex;align-items:center;justify-content:center;gap:18px;flex-wrap:wrap;font-size:0.86rem}
    .pulse-item{display:flex;align-items:center;gap:6px;color:var(--muted)}
    .pulse-item strong{color:var(--mint);font-size:0.98rem;font-weight:900}
    .share-row{margin-top:14px;display:flex;justify-content:center;align-items:center;gap:10px;flex-wrap:wrap}
    .share-feedback{font-size:0.84rem;color:var(--mint);font-weight:700;display:none}
    
    .calculator-card{margin:36px auto;padding:32px 24px;border:1px solid #338d79;border-radius:24px;background:linear-gradient(135deg,#0c2331,#101e30 75%);text-align:center;max-width:680px;box-shadow:0 12px 36px rgba(0,0,0,0.4)}
    .calculator-card h2{font-size:clamp(1.5rem,3.8vw,2.1rem);margin:8px 0 12px;letter-spacing:-.03em}
    .calculator-card p{color:var(--muted);max-width:560px;margin:0 auto 20px}
    .calc-controls{display:flex;flex-wrap:wrap;justify-content:center;gap:16px;margin-bottom:20px}
    .calc-control-group{display:flex;flex-direction:column;align-items:flex-start;gap:6px;min-width:240px;flex:1}
    .calc-control-group label{font-size:0.86rem;color:var(--mint);font-weight:700}
    .calc-select{width:100%;padding:12px 14px;border-radius:10px;border:1px solid var(--line);background:#07111f;color:#fff;font-size:0.95rem;outline:none}
    .calc-select:focus{border-color:var(--mint)}
    .calc-result-box{background:rgba(93,227,191,0.06);padding:22px;border-radius:18px;border:1px dashed rgba(93,227,191,0.35);margin-top:10px}
    .calc-result-label{font-size:0.85rem;text-transform:uppercase;letter-spacing:0.08em;color:var(--mint);font-weight:800}
    .calc-result-value{font-size:clamp(2.4rem,6vw,3.2rem);font-weight:900;color:var(--white);margin:4px 0}
    .calc-result-value span{color:var(--mint)}
    .calc-subtext{font-size:0.88rem;color:var(--muted);margin:0 auto 16px;max-width:480px}

    .vip-testers{margin:40px auto 32px;padding:32px 24px;border:1px solid var(--mint);border-radius:24px;background:linear-gradient(135deg,#0c2331,#081729);text-align:center;max-width:860px;box-shadow:0 12px 40px rgba(93,227,191,0.15)}
    .tester-badge{display:inline-block;padding:6px 12px;background:rgba(93,227,191,0.12);border:1px solid var(--mint);border-radius:20px;font-size:0.82rem;font-weight:800;color:var(--mint);margin-bottom:12px}
    .vip-testers h2{font-size:clamp(1.4rem,3.6vw,1.9rem);margin:0 0 10px;letter-spacing:-.02em}
    .vip-testers p{color:var(--muted);max-width:620px;margin:0 auto 20px;font-size:0.96rem}
    .vip-testers p strong{color:var(--mint)}
    .tester-form{display:flex;gap:10px;justify-content:center;flex-wrap:wrap;max-width:540px;margin:0 auto}
    .tester-input{flex:1;min-width:240px;padding:14px 18px;border-radius:12px;border:1px solid var(--line);background:#07111f;color:#fff;font-size:0.95rem;outline:none}
    .tester-input:focus{border-color:var(--mint)}
    .feedback-msg{margin:14px auto 0;max-width:620px;padding:12px 16px;border-radius:12px;font-size:0.92rem;font-weight:700;display:none}
    .feedback-msg.success{color:var(--mint);background:rgba(93,227,191,0.1);border:1px solid rgba(93,227,191,0.4);display:block}
    .feedback-msg.error{color:#ff9b9b;background:rgba(255,117,117,0.1);border:1px solid rgba(255,117,117,0.35);display:block}

    .onboarding-steps{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:14px;margin-top:24px;text-align:left}
    .onboarding-steps.highlighted .step-card{border-color:var(--mint);box-shadow:0 0 24px rgba(93,227,191,0.18)}
    .step-card{background:rgba(7,17,31,0.78);border:1px solid var(--line);border-radius:16px;padding:18px;display:flex;flex-direction:column;justify-content:space-between;gap:12px;transition:border-color .2s,box-shadow .2s}
    .step-badge{display:inline-block;font-size:0.75rem;font-weight:800;text-transform:uppercase;letter-spacing:0.05em;color:var(--mint);background:rgba(93,227,191,0.1);padding:4px 10px;border-radius:999px;width:fit-content}
    .step-card h3{margin:6px 0 4px;font-size:1.05rem;color:var(--white)}
    .step-card p{margin:0;font-size:0.86rem;color:var(--muted);line-height:1.45}
    .step-btn{width:100%;min-height:42px;padding:10px 14px;font-size:0.86rem;border-radius:10px}
    .step-note{margin:18px auto 0;max-width:720px;font-size:0.83rem;color:var(--muted);background:rgba(7,17,31,0.65);border:1px dashed var(--line);padding:10px 14px;border-radius:12px}

    .section-title{font-size:1.6rem;letter-spacing:-.02em;margin:36px 0 16px}
    .cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px}
    .card{background:var(--card);border:1px solid var(--line);border-radius:18px;padding:24px}
    .card .tag{display:inline-block;color:var(--mint);font-weight:800;font-size:0.78rem;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px}
    .card h3{margin:0 0 8px;font-size:1.15rem}
    .card p{margin:0;color:var(--muted);font-size:0.92rem}
    .pro-box{margin:36px 0;border:1px solid var(--line);border-radius:20px;background:#0b192c;padding:32px;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:20px}
    .pro-copy h2{margin:0 0 8px;font-size:1.45rem}
    .pro-copy p{margin:0;color:var(--muted);max-width:560px}
    footer{border-top:1px solid var(--line);padding:24px 0;margin-top:48px;font-size:0.85rem;color:var(--muted);display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px}
    footer a{color:var(--muted);text-decoration:none}
    footer a:hover{color:var(--mint)}
    .search-card{margin:18px auto 28px;padding:24px;border:1px solid var(--line);border-radius:20px;background:rgba(15,29,49,0.88);max-width:860px}
    .search-card h2{font-size:1.3rem;margin:0 0 12px}
    .search-row{display:flex;gap:10px;flex-wrap:wrap;align-items:center}
    .search-input{flex:1;min-width:220px;padding:13px 16px;border-radius:12px;border:1px solid var(--line);background:#07111f;color:#fff;font-size:0.95rem;outline:none}
    .search-input:focus{border-color:var(--mint)}
    .search-chips{display:flex;gap:6px;flex-wrap:wrap;margin:12px 0}
    .search-chip{background:#0b182a;border:1px solid var(--line);color:var(--muted);padding:5px 11px;border-radius:999px;font-size:0.78rem;font-weight:700;cursor:pointer}
    .search-chip:hover,.search-chip.active{border-color:var(--mint);color:var(--mint)}
    .search-meta{font-size:0.82rem;color:var(--mint);font-weight:700;margin-bottom:10px}
    .search-results{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px}
    .search-result-card{display:block;text-decoration:none;background:#07111f;border:1px solid var(--line);border-radius:14px;padding:14px;color:var(--text);transition:border-color .15s}
    .search-result-card:hover{border-color:var(--mint)}
    .search-result-card strong{display:block;color:var(--mint);font-size:0.95rem;margin-bottom:4px}
    .search-result-card span{display:block;color:var(--muted);font-size:0.83rem;line-height:1.4}
  </style>
</head>
<body>
  <header>
    <div class="wrap nav">
      <a class="brand" href="https://midiastory-df6a0.web.app/">Midia<span>store</span></a>
      ${renderLangNav(langKey)}
      <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
        <a class="button secondary" href="#site-search" style="min-height: 40px; padding: 8px 14px; font-size: 0.88rem;">🔎 ${searchUi.btn}</a>
        <a class="button secondary" href="/ferramentas.html" style="min-height: 40px; padding: 8px 14px; font-size: 0.88rem;">${t.btnTools}</a>
        <a class="button secondary" href="/diagnostico.html" style="min-height: 40px; padding: 8px 14px; font-size: 0.88rem;">${t.btnWaTest}</a>
        <a class="button" href="#vip-testers" style="min-height: 40px; padding: 8px 14px; font-size: 0.88rem;">${t.vipProNav}</a>
      </div>
    </div>
  </header>

  <main class="wrap">
    <section class="hero">
      <div class="eyebrow">${t.eyebrow}</div>
      <h1>${t.h1}</h1>
      <p class="lead">${t.lead}</p>
      <div class="actions">
        <a class="button" href="#vip-testers" data-track-cta="hero_waitlist" onclick="document.getElementById('testerEmail').focus()">${t.ctaHero}</a>
        <a class="button secondary" href="${PLAY_OPTIN_URL}" data-track-cta="hero_optin" target="_blank" rel="noopener">${t.ctaHeroSub}</a>
      </div>
      <div class="pulse-strip" aria-label="${t.liveTitle}">
        <span class="pulse-item">👁️ ${t.liveVisitsLabel} <strong data-stat="total_visits">—</strong></span>
        <span class="pulse-item">👥 ${t.liveWaitlistLabel} <strong data-stat="waitlist_count">—</strong></span>
        <span class="pulse-item">🔥 ${t.live24hLabel} <strong data-stat="visits_24h">—</strong></span>
      </div>
      <p class="note">${t.note}</p>
    </section>

    <!-- INSTANT SITE SEARCH + GOOGLE SEARCHACTION -->
    <section class="search-card" id="site-search" role="search" aria-label="${searchUi.badge}">
      <div class="tester-badge">${searchUi.badge}</div>
      <h2>${searchUi.title}</h2>
      <form class="search-row" id="siteSearchForm">
        <input type="search" id="siteSearchInput" class="search-input" placeholder="${searchUi.placeholder}" autocomplete="off">
        <button type="submit" class="button" style="min-height:46px;padding:10px 18px;">${searchUi.btn}</button>
        <button type="button" id="googleSiteSearchBtn" class="button secondary" style="min-height:46px;padding:10px 16px;font-size:0.86rem;">${searchUi.googleBtn}</button>
      </form>
      <div class="search-chips" id="searchChips">
        ${searchUi.chips.map((c) => `<button type="button" class="search-chip" data-chip="${c}">${c}</button>`).join("")}
      </div>
      <div id="searchMeta" class="search-meta"></div>
      <div id="searchResults" class="search-results"></div>
    </section>

    <!-- INTERACTIVE CLUTTER ESTIMATOR -->
    <section class="calculator-card" id="calculator">
      <div class="tester-badge">${t.calcBadge}</div>
      <h2>${t.calcTitle}</h2>
      <p>${t.calcDesc}</p>

      <div class="calc-controls">
        <div class="calc-control-group">
          <label for="timeSelect">${t.calcTime}</label>
          <select id="timeSelect" class="calc-select">
            <option value="4.2">${t.calcOptT1}</option>
            <option value="8.8" selected>${t.calcOptT2}</option>
            <option value="16.5">${t.calcOptT3}</option>
            <option value="26.0">${t.calcOptT4}</option>
          </select>
        </div>
        <div class="calc-control-group">
          <label for="groupSelect">${t.calcGroups}</label>
          <select id="groupSelect" class="calc-select">
            <option value="1">${t.calcOptG1}</option>
            <option value="1.4" selected>${t.calcOptG2}</option>
            <option value="2.1">${t.calcOptG3}</option>
          </select>
        </div>
      </div>

      <div class="calc-result-box">
        <div class="calc-result-label">${t.calcEst}</div>
        <div class="calc-result-value"><span id="calcGb">12.3</span> GB</div>
        <p class="calc-subtext">${t.calcSub}</p>
        <a class="button" href="#vip-testers" onclick="document.getElementById('testerEmail').focus()">${t.calcCta}</a>
      </div>
    </section>

    <!-- VIP TESTER FUNNEL -->
    <section class="vip-testers" id="vip-testers">
      <div class="tester-badge">${t.vipBadge}</div>
      <h2>${t.vipTitle}</h2>
      <p>${t.vipDesc}</p>
      
      <form class="tester-form" id="testerForm">
        <input type="email" id="testerEmail" class="tester-input" placeholder="${t.emailPlaceholder}" required>
        <button type="submit" id="testerSubmitBtn" class="button">${t.btnSubmit}</button>
      </form>
      <div id="feedbackMsg" class="feedback-msg" role="status" aria-live="polite"></div>

      <div class="onboarding-steps" id="onboardingSteps">
        <div class="step-card">
          <div>
            <span class="step-badge">${t.step1Badge}</span>
            <h3>${t.step1Title}</h3>
            <p>${t.step1Desc}</p>
          </div>
          <a class="button secondary step-btn" href="#testerEmail" onclick="document.getElementById('testerEmail').focus(); return false;">${t.step1Btn}</a>
        </div>
        <div class="step-card">
          <div>
            <span class="step-badge">${t.step2Badge}</span>
            <h3>${t.step2Title}</h3>
            <p>${t.step2Desc}</p>
          </div>
          <a class="button step-btn" id="optInStepBtn" data-track-cta="step2_optin" href="${PLAY_OPTIN_URL}" target="_blank" rel="noopener">${t.step2Btn}</a>
        </div>
        <div class="step-card">
          <div>
            <span class="step-badge">${t.step3Badge}</span>
            <h3>${t.step3Title}</h3>
            <p>${t.step3Desc}</p>
          </div>
          <a class="button secondary step-btn" data-track-cta="step3_play" href="${PLAY_STORE_URL}" target="_blank" rel="noopener">${t.step3Btn}</a>
        </div>
      </div>
      <div class="share-row">
        <button type="button" class="button secondary" id="shareFunnelBtn" style="min-height:42px;padding:10px 18px;font-size:0.88rem;">${t.shareBtn}</button>
        <span id="shareFeedback" class="share-feedback">${t.shareCopied}</span>
      </div>
      <div class="step-note">${t.stepNote}</div>
    </section>

    <section>
      <h2 class="section-title">Midiastore</h2>
      <div class="cards">
        <article class="card">
          <div class="tag">${t.c1Tag}</div>
          <h3>${t.c1Title}</h3>
          <p>${t.c1Desc}</p>
        </article>
        <article class="card">
          <div class="tag">${t.c2Tag}</div>
          <h3>${t.c2Title}</h3>
          <p>${t.c2Desc}</p>
        </article>
        <article class="card">
          <div class="tag">${t.c3Tag}</div>
          <h3>${t.c3Title}</h3>
          <p>${t.c3Desc}</p>
        </article>
      </div>
    </section>

    <section class="pro-box">
      <div class="pro-copy">
        <div class="eyebrow">${t.proEyebrow}</div>
        <h2>${t.proTitle}</h2>
        <p>${t.proDesc}</p>
      </div>
      <a class="button" href="#vip-testers" onclick="document.getElementById('testerEmail').focus()">${t.btnSubmit}</a>
    </section>

    <section style="margin-top: 36px;">
      <h2 class="section-title">${t.ctrlTitle}</h2>
      <p style="color: var(--muted); max-width: 680px;">${t.ctrlDesc}</p>
    </section>
  </main>

  <footer class="wrap">
    <div>&copy; 2026 Midiastore (com.rdf112.midiastore). All rights reserved.</div>
    <div style="display:flex;gap:14px;flex-wrap:wrap;">
      <a href="/ferramentas.html">${t.btnTools}</a>
      <a href="/diagnostico.html">${t.btnWaTest}</a>
      <a href="/privacidade.html">${t.privacy}</a>
    </div>
  </footer>

  <script>
    const timeEl = document.getElementById('timeSelect');
    const groupEl = document.getElementById('groupSelect');
    const calcGbEl = document.getElementById('calcGb');

    function updateClutterEstimate() {
      const base = parseFloat(timeEl.value) || 8.8;
      const mult = parseFloat(groupEl.value) || 1.4;
      const total = (base * mult).toFixed(1);
      if (calcGbEl) calcGbEl.innerText = total;
    }

    if (timeEl && groupEl) {
      timeEl.addEventListener('change', updateClutterEstimate);
      groupEl.addEventListener('change', updateClutterEstimate);
      updateClutterEstimate();
    }

    const testerForm = document.getElementById("testerForm");
    const emailInput = document.getElementById("testerEmail");
    const submitBtn = document.getElementById("testerSubmitBtn");
    const feedback = document.getElementById("feedbackMsg");
    const onboardingSteps = document.getElementById("onboardingSteps");
    const SUPABASE_URL = "${SUPABASE_URL}";
    const ANON_KEY = "${SUPABASE_ANON_KEY}";

    testerForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = emailInput.value.trim().toLowerCase();
      if (!email || !email.includes("@")) return;

      submitBtn.disabled = true;
      submitBtn.innerText = "...";
      feedback.className = "feedback-msg";
      feedback.style.display = "none";

      try {
        const res = await fetch(\`\${SUPABASE_URL}/rest/v1/beta_testers\`, {
          method: "POST",
          headers: {
            "apikey": ANON_KEY,
            "Authorization": \`Bearer \${ANON_KEY}\`,
            "Content-Type": "application/json",
            "Prefer": "return=minimal"
          },
          body: JSON.stringify({ email: email, source: "landing_page_${langKey}" })
        });

        if (res.ok || res.status === 201 || res.status === 204) {
          feedback.className = "feedback-msg success";
          feedback.innerText = ${JSON.stringify(t.successMsg)};
          emailInput.value = "";
          if (onboardingSteps) onboardingSteps.classList.add("highlighted");
          if (window.MidiastoreTelemetry) {
            window.MidiastoreTelemetry.trackEvent("waitlist_submit", "landing_page_${langKey}");
            window.MidiastoreTelemetry.refreshPrelaunchStats();
          }
        } else if (res.status === 409) {
          feedback.className = "feedback-msg success";
          feedback.innerText = ${JSON.stringify(t.duplicateMsg)};
          if (onboardingSteps) onboardingSteps.classList.add("highlighted");
        } else {
          feedback.className = "feedback-msg error";
          feedback.innerText = ${JSON.stringify(t.errorMsg)};
        }
      } catch (err) {
        feedback.className = "feedback-msg error";
        feedback.innerText = ${JSON.stringify(t.connErrorMsg)};
      } finally {
        submitBtn.disabled = false;
        submitBtn.innerText = ${JSON.stringify(t.btnSubmit)};
        feedback.style.display = "block";
      }
    });

    const shareFunnelBtn = document.getElementById("shareFunnelBtn");
    const shareFeedback = document.getElementById("shareFeedback");
    if (shareFunnelBtn) {
      shareFunnelBtn.addEventListener("click", () => {
        if (window.MidiastoreTelemetry) {
          const nativeUsed = window.MidiastoreTelemetry.sharePage(${JSON.stringify(t.title)}, ${JSON.stringify(t.shareText)});
          if (!nativeUsed && shareFeedback) {
            shareFeedback.style.display = "inline-block";
            setTimeout(() => { shareFeedback.style.display = "none"; }, 4000);
          }
          window.MidiastoreTelemetry.refreshPrelaunchStats();
        }
      });
    }

    const searchCatalog = ${JSON.stringify(searchCatalog)};
    const searchInput = document.getElementById("siteSearchInput");
    const siteSearchForm = document.getElementById("siteSearchForm");
    const searchResultsEl = document.getElementById("searchResults");
    const searchMetaEl = document.getElementById("searchMeta");
    const googleBtn = document.getElementById("googleSiteSearchBtn");

    function runSiteSearch(rawQuery) {
      const q = (rawQuery || "").trim().toLowerCase();
      const terms = q ? q.split(/\\s+/).filter(Boolean) : [];
      const matched = terms.length === 0
        ? searchCatalog.slice(0, 6)
        : searchCatalog.filter((item) => {
            const hay = (item.title + " " + item.desc + " " + item.tags).toLowerCase();
            return terms.every((term) => hay.includes(term));
          });

      if (searchMetaEl) {
        searchMetaEl.textContent = matched.length > 0
          ? matched.length + " " + ${JSON.stringify(searchUi.countLabel)}
          : ${JSON.stringify(searchUi.empty)};
      }
      if (searchResultsEl) {
        searchResultsEl.innerHTML = matched.map((item) =>
          '<a class="search-result-card" href="' + item.url + '"><strong>' + item.title + '</strong><span>' + item.desc + '</span></a>'
        ).join("");
      }
    }

    if (searchInput) {
      const urlQ = new URLSearchParams(window.location.search).get("q") || "";
      if (urlQ) searchInput.value = urlQ;
      runSiteSearch(searchInput.value);
      searchInput.addEventListener("input", () => runSiteSearch(searchInput.value));
    }
    if (siteSearchForm && searchInput) {
      siteSearchForm.addEventListener("submit", (e) => {
        e.preventDefault();
        runSiteSearch(searchInput.value);
      });
    }
    document.querySelectorAll("#searchChips .search-chip").forEach((chip) => {
      chip.addEventListener("click", () => {
        if (!searchInput) return;
        const val = chip.getAttribute("data-chip") || "";
        searchInput.value = val;
        runSiteSearch(val);
      });
    });
    if (googleBtn) {
      googleBtn.addEventListener("click", () => {
        const q = (searchInput && searchInput.value ? searchInput.value.trim() : "Midiastore offline");
        window.open("https://www.google.com/search?q=" + encodeURIComponent("site:midiastory-df6a0.web.app " + q), "_blank", "noopener");
      });
    }
  </script>
  <script src="/telemetry.js" defer></script>
</body>
</html>`;
}

async function buildAll() {
  const langCount = Object.keys(LANGUAGES).length;
  console.log(`=== COMPILANDO PÁGINAS EM ${langCount} IDIOMAS (MIDIASTORE) ===`);

  const todayIso = new Date().toISOString().slice(0, 10);
  const sitemapUrls = [];

  for (const [langKey, t] of Object.entries(LANGUAGES)) {
    const html = generateHtml(langKey, t);
    const targetDir = langKey === "pt" ? docsDir : path.join(docsDir, langKey);
    fs.mkdirSync(targetDir, { recursive: true });
    const targetPath = path.join(targetDir, "index.html");
    fs.writeFileSync(targetPath, html, "utf8");
    const urlLoc = langKey === "pt"
      ? "https://midiastory-df6a0.web.app/"
      : `https://midiastory-df6a0.web.app/${langKey}/`;
    sitemapUrls.push({ loc: urlLoc, changefreq: langKey === "pt" || langKey === "en" ? "daily" : "weekly", priority: langKey === "pt" ? "1.0" : "0.9" });
    console.log(`✅ [${langKey.toUpperCase()}] Gerado: ${path.relative(projectRoot, targetPath)}`);
  }

  for (const extra of [
    { loc: "https://midiastory-df6a0.web.app/livros.html", changefreq: "weekly", priority: "0.9" },
    { loc: "https://midiastory-df6a0.web.app/diagnostico.html", changefreq: "weekly", priority: "0.9" },
    { loc: "https://midiastory-df6a0.web.app/ferramentas.html", changefreq: "weekly", priority: "0.9" },
    { loc: "https://midiastory-df6a0.web.app/privacidade.html", changefreq: "monthly", priority: "0.6" },
    { loc: "https://midiastory-df6a0.web.app/artigos/como-liberar-espaco-whatsapp.html", changefreq: "weekly", priority: "0.9" },
  ]) {
    sitemapUrls.push(extra);
  }

  const sitemapXml = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n` +
    sitemapUrls.map((u) => `  <url>\n    <loc>${u.loc}</loc>\n    <lastmod>${todayIso}</lastmod>\n    <changefreq>${u.changefreq}</changefreq>\n    <priority>${u.priority}</priority>\n  </url>`).join("\n") +
    `\n</urlset>\n`;
  fs.writeFileSync(path.join(docsDir, "sitemap.xml"), sitemapXml, "utf8");
  console.log(`✅ Sitemap XML atualizado com ${sitemapUrls.length} URLs em docs/sitemap.xml`);
  console.log(`\n🎉 Todas as ${langCount} páginas internacionais foram compiladas com busca interna + SEO!`);
}

buildAll().catch(console.error);
