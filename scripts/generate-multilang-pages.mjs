import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const docsDir = path.join(projectRoot, "docs");

const LANGUAGES = {
  pt: {
    code: "pt-BR",
    title: "Midiastore — Veja o que ocupa espaço no seu Android",
    desc: "Encontre arquivos grandes nas mídias acessíveis do seu Android. Participe do programa VIP de testadores fechados e ganhe 3 meses de Midiastore Pro grátis!",
    eyebrow: "Armazenamento cheio? Comece por uma revisão",
    h1: "Descubra o que ocupa espaço.<br><em>Você decide o que fazer.</em>",
    lead: "O Midiastore ajuda a encontrar arquivos grandes nas mídias acessíveis do seu Android. Revise os itens com calma antes de qualquer ação.",
    ctaHero: "Participar do Teste Fechado VIP (Grátis)",
    note: "A análise, a revisão e a limpeza segura são gratuitas. A remoção exige sua confirmação pelo Android.",
    vipBadge: "⭐ Programa VIP Exclusivo Google Play",
    vipTitle: "Vagas Abertas para Testadores Fechados",
    vipDesc: "O Google Play exige 20 testadores voluntários antes da publicação mundial. Cadastre seu e-mail da Google Play Store abaixo e ganhe <strong>3 meses de Midiastore Pro grátis</strong> como agradecimento!",
    emailPlaceholder: "Seu e-mail da Google Play (ex: nome@gmail.com)",
    btnSubmit: "Quero Minha Vaga VIP",
    c1Tag: "Encontre", c1Title: "Arquivos grandes", c1Desc: "Veja arquivos de mídia grandes identificados na análise do armazenamento acessível.",
    c2Tag: "WhatsApp Cleaner", c2Title: "Mídias Ocultas", c2Desc: "Varra notas de voz antigas e vídeos encaminhados em pastas .nomedia que a galeria não mostra.",
    c3Tag: "100% Seguro", c3Title: "Duplicatas SHA-256", c3Desc: "Identificação binária exata para garantir que nenhuma memória única seja removida por engano.",
    proEyebrow: "Midiastore Pro", proTitle: "Leve o resumo da sua revisão com você.", proDesc: "O Pro adiciona exclusão em lote com 1 toque nativo, alertas de limpeza periódica e zero anúncios para sempre.",
    ctrlTitle: "Seus arquivos, sua decisão", ctrlDesc: "O Midiastore analisa mídias acessíveis no dispositivo em total conformidade com o Scoped Storage. A decisão é sempre sua.",
    privacy: "Privacidade",
    successMsg: "🎉 Vaga garantida! Seu e-mail foi adicionado à lista de testadores VIP. Em instantes você receberá o link da Google Play Store!",
    duplicateMsg: "✅ Este e-mail já está cadastrado na nossa lista VIP de testadores da Google Play!"
  },
  en: {
    code: "en-US",
    title: "Midiastore — Reclaim Your Android Storage Safely",
    desc: "Find large files and clean hidden WhatsApp media without risking your memories. Join the VIP Closed Tester Program and get 3 months of Midiastore Pro FREE!",
    eyebrow: "Storage Full? Start With Complete Clarity",
    h1: "Discover what eats up space.<br><em>You decide what to keep.</em>",
    lead: "Midiastore scans accessible media on your Android device to pinpoint massive files, duplicate photos, and forgotten WhatsApp data. Total privacy, zero accidental deletions.",
    ctaHero: "Join Closed Testing (Free VIP Pro)",
    note: "Analysis and review are 100% free. File deletion always requires your native Android confirmation.",
    vipBadge: "⭐ Google Play Official Closed Beta",
    vipTitle: "Free VIP Access for Beta Testers",
    vipDesc: "Google Play requires 20 active volunteer testers before our worldwide launch. Enter your Google Play account email below and unlock <strong>3 Months of Midiastore Pro FREE</strong>!",
    emailPlaceholder: "Your Google Play email (e.g. you@gmail.com)",
    btnSubmit: "Claim My VIP Spot",
    c1Tag: "Identify", c1Title: "Massive Media Files", c1Desc: "Instantly sort through forgotten videos, heavy downloads, and hidden files cluttering your phone.",
    c2Tag: "WhatsApp Cleaner", c2Title: "Hidden Audio & Memes", c2Desc: "Scan old voice notes, forwarded videos, and .nomedia files that standard photo galleries hide.",
    c3Tag: "100% Safe", c3Title: "SHA-256 Duplicates", c3Desc: "Byte-by-byte duplicate detection ensures your unique, cherished photos are never deleted by mistake.",
    proEyebrow: "Midiastore Pro", proTitle: "Supercharge Your Android Phone Today", proDesc: "Get batch deletion with a single system tap, recurring background cleaning reminders, and zero ads forever.",
    ctrlTitle: "Your Files, Your Absolute Decision", ctrlDesc: "Midiastore strictly respects Android Scoped Storage. No root required, no dangerous background permissions.",
    privacy: "Privacy Policy",
    successMsg: "🎉 VIP spot secured! You have been added to our official Google Play tester list. Check your email shortly!",
    duplicateMsg: "✅ This email is already registered on our Google Play VIP list!"
  },
  es: {
    code: "es-ES",
    title: "Midiastore — Libera espacio en tu Android de forma segura",
    desc: "Encuentra archivos grandes y limpia medios ocultos de WhatsApp en tu Android. ¡Únete al programa VIP de probadores y obtén 3 meses de Midiastore Pro GRATIS!",
    eyebrow: "¿Almacenamiento lleno? Empieza con total claridad",
    h1: "Descubre qué ocupa espacio.<br><em>Tú decides qué conservar.</em>",
    lead: "Midiastore escanea los medios accesibles en tu Android para localizar archivos pesados, duplicados exactos y datos olvidados de WhatsApp.",
    ctaHero: "Unirse a la Prueba Cerrada (VIP Gratis)",
    note: "El análisis y la revisión son 100% gratuitos. La eliminación siempre requiere confirmación nativa de Android.",
    vipBadge: "⭐ Programa VIP Oficial de Google Play",
    vipTitle: "Plazas Abiertas para Probadores VIP",
    vipDesc: "Google Play exige 20 probadores voluntarios antes del lanzamiento mundial. ¡Introduce tu correo de Google Play y desbloquea <strong>3 meses de Midiastore Pro GRATIS</strong>!",
    emailPlaceholder: "Tu correo de Google Play (ej: nombre@gmail.com)",
    btnSubmit: "Reclamar mi plaza VIP",
    c1Tag: "Identifica", c1Title: "Archivos gigantescos", c1Desc: "Localiza de inmediato vídeos olvidados y descargas pesadas que saturan tu memoria.",
    c2Tag: "Limpia WhatsApp", c2Title: "Audios y memes ocultos", c2Desc: "Escanea notas de voz antiguas y carpetas .nomedia que la galería estándar no muestra.",
    c3Tag: "100% Seguro", c3Title: "Duplicados SHA-256", c3Desc: "Comprobación binaria exacta para no borrar jamás tus fotos únicas por error.",
    proEyebrow: "Midiastore Pro", proTitle: "Potencia el almacenamiento de tu Android", proDesc: "Eliminación masiva con 1 solo toque nativo, recordatorios periódicos inteligentes y cero anuncios.",
    ctrlTitle: "Tus archivos, tu decisión absoluta", ctrlDesc: "Midiastore cumple estrictamente con Scoped Storage de Android. Sin permisos invasivos ni root.",
    privacy: "Política de Privacidad",
    successMsg: "🎉 ¡Plaza VIP asegurada! Has sido añadido a la lista oficial de Google Play. ¡Revisa tu correo!",
    duplicateMsg: "✅ ¡Este correo ya está registrado en nuestra lista VIP de Google Play!"
  },
  de: {
    code: "de-DE",
    title: "Midiastore — Android-Speicher sicher und smart bereinigen",
    desc: "Finden Sie große Dateien und versteckte WhatsApp-Medien auf Ihrem Android-Gerät. Werden Sie VIP-Tester und erhalten Sie 3 Monate Midiastore Pro GRATIS!",
    eyebrow: "Speicher voll? Schaffen Sie endlich Klarheit",
    h1: "Sehen Sie, was Speicher belegt.<br><em>Sie entscheiden, was bleibt.</em>",
    lead: "Midiastore analysiert Mediendateien auf Ihrem Android-Gerät, um riesige Videos, Duplikate und vergessene WhatsApp-Dateien sicher aufzuspüren.",
    ctaHero: "Am geschlossenen Betatest teilnehmen (Gratis)",
    note: "Analyse und Vorschau sind 100% kostenlos. Das Löschen erfordert stets Ihre Bestätigung in Android.",
    vipBadge: "⭐ Offizielles Google Play VIP-Testerprogramm",
    vipTitle: "Offene Plätze für geschlossene Tests",
    vipDesc: "Google Play verlangt 20 freiwillige Tester vor der weltweiten Freigabe. Tragen Sie Ihre Google Play E-Mail ein und erhalten Sie <strong>3 Monate Midiastore Pro GRATIS</strong>!",
    emailPlaceholder: "Ihre Google Play E-Mail (z.B. name@gmail.com)",
    btnSubmit: "Meinen VIP-Platz sichern",
    c1Tag: "Erkennen", c1Title: "Riesige Dateien", c1Desc: "Finden Sie sofort alte Downloads und 4K-Videos, die unnötig Gigabytes belegen.",
    c2Tag: "WhatsApp Cleaner", c2Title: "Versteckte Sprachmemos", c2Desc: "Bereinigen Sie alte Sprachnachrichten und weitergeleitete Videos in .nomedia-Ordnern.",
    c3Tag: "100% Sicher", c3Title: "SHA-256 Duplikate", c3Desc: "Exakter Prüfsummen-Abgleich schützt Ihre wertvollen Erinnerungen vor versehentlichem Löschen.",
    proEyebrow: "Midiastore Pro", proTitle: "Volle Kontrolle über Ihren Speicher", proDesc: "Stapelbereinigung mit nur einem System-Tap, automatische Bereinigungs-Erinnerungen und werbefrei.",
    ctrlTitle: "Ihre Dateien, Ihre Entscheidung", ctrlDesc: "Midiastore respektiert zu 100% die Scoped-Storage-Vorgaben von Android. Keine unnötigen Berechtigungen.",
    privacy: "Datenschutzerklärung",
    successMsg: "🎉 VIP-Platz gesichert! Sie wurden zur offiziellen Testerliste hinzugefügt. Prüfen Sie Ihr Postfach!",
    duplicateMsg: "✅ Diese E-Mail ist bereits in unserer VIP-Testerliste registriert!"
  },
  fr: {
    code: "fr-FR",
    title: "Midiastore — Libérez de l'espace Android en toute sécurité",
    desc: "Trouvez les fichiers volumineux et nettoyez les médias cachés de WhatsApp sur Android. Devenez testeur VIP et profitez de 3 mois de Midiastore Pro GRATUITS !",
    eyebrow: "Mémoire saturée ? Retrouvez la clarté",
    h1: "Découvrez ce qui prend de la place.<br><em>Vous gardez le contrôle total.</em>",
    lead: "Midiastore analyse les médias de votre Android pour cibler les vidéos lourdes, les doublons exacts et les données WhatsApp oubliées.",
    ctaHero: "Rejoindre le test fermé (VIP Gratuit)",
    note: "L'analyse et la vérification sont 100% gratuites. La suppression nécessite toujours votre confirmation native Android.",
    vipBadge: "⭐ Programme VIP Officiel Google Play",
    vipTitle: "Places Disponibles pour Testeurs Fermés",
    vipDesc: "Google Play exige 20 testeurs volontaires avant le lancement mondial. Saisissez votre e-mail Google Play et débloquez <strong>3 mois de Midiastore Pro GRATUITS</strong> !",
    emailPlaceholder: "Votre e-mail Google Play (ex: nom@gmail.com)",
    btnSubmit: "Obtenir ma place VIP",
    c1Tag: "Détecter", c1Title: "Fichiers massifs", c1Desc: "Repérez instantanément les vidéos oubliées et les téléchargements encombrants.",
    c2Tag: "Nettoyeur WhatsApp", c2Title: "Audios et vidéos cachés", c2Desc: "Scannez les notes vocales et fichiers .nomedia invisibles dans la galerie standard.",
    c3Tag: "100% Sécurisé", c3Title: "Doublons SHA-256", c3Desc: "Comparaison binaire exacte garantissant qu'aucune photo unique ne sera supprimée.",
    proEyebrow: "Midiastore Pro", proTitle: "Optimisez votre appareil Android", proDesc: "Suppression par lot en 1 touche native, rappels périodiques intelligents et zéro publicité.",
    ctrlTitle: "Vos fichiers, votre décision souveraine", ctrlDesc: "Midiastore respecte scrupuleusement la politique Scoped Storage d'Android. Vos données restent privées.",
    privacy: "Politique de confidentialité",
    successMsg: "🎉 Place VIP validée ! Votre e-mail a été ajouté à la liste officielle Google Play. À très vite !",
    duplicateMsg: "✅ Cet e-mail est déjà inscrit sur notre liste VIP Google Play !"
  },
  hi: {
    code: "hi-IN",
    title: "Midiastore — अपने एंड्रॉइड स्टोरेज को सुरक्षित रूप से खाली करें",
    desc: "भारी फाइलों और छिपे हुए व्हाट्सएप मीडिया को सुरक्षित रूप से साफ करें। वीआईपी क्लोज्ड टेस्टर बनें और 3 महीने का Midiastore Pro मुफ़्त पाएं!",
    eyebrow: "स्टोरेज फुल हो गया है? समझदारी से शुरुआत करें",
    h1: "देखें कि क्या जगह ले रहा है।<br><em>क्या रखना है, निर्णय आपका है।</em>",
    lead: "Midiastore आपके एंड्रॉइड फोन में बड़ी फाइलों, डुप्लिकेट फोटो और छिपे हुए व्हाट्सएप मीडिया को सुरक्षित रूप से ढूंढता है।",
    ctaHero: "क्लोज्ड टेस्ट में शामिल हों (मुफ़्त वीआईपी)",
    note: "विश्लेषण और समीक्षा 100% मुफ़्त है। फ़ाइल हटाने के लिए हमेशा एंड्रॉइड की पुष्टि आवश्यक है।",
    vipBadge: "⭐ Google Play आधिकारिक वीआईपी परीक्षक कार्यक्रम",
    vipTitle: "बीटा टेस्टर्स के लिए सीटें खुली हैं",
    vipDesc: "Google Play को लॉन्च से पहले 20 परीक्षकों की आवश्यकता है। नीचे अपना ईमेल दर्ज करें और <strong>3 महीने का Midiastore Pro मुफ़्त</strong> पाएं!",
    emailPlaceholder: "आपका Google Play ईमेल (उदा: name@gmail.com)",
    btnSubmit: "मेरा वीआईपी स्थान सुरक्षित करें",
    c1Tag: "पहचानें", c1Title: "भारी मीडिया फाइलें", c1Desc: "भूले हुए वीडियो और डाउनलोड की तुरंत पहचान करें जो स्टोरेज भर रहे हैं।",
    c2Tag: "WhatsApp क्लीनर", c2Title: "छिपे हुए ऑडियो और वीडियो", c2Desc: "पुराने वॉइस नोट्स और .nomedia फाइलों को साफ करें जो गैलरी में नहीं दिखतीं।",
    c3Tag: "100% सुरक्षित", c3Title: "SHA-256 डुप्लिकेट", c3Desc: "सटीक बाइनरी जांच ताकि आपकी अनमोल यादें कभी गलती से न हटें।",
    proEyebrow: "Midiastore Pro", proTitle: "अपने फोन को सुपरफास्ट बनाएं", proDesc: "सिंगल टैप बैच डिलीशन, ऑटोमैटिक रिमाइंडर और हमेशा के लिए विज्ञापन-मुक्त अनुभव।",
    ctrlTitle: "आपकी फाइलें, आपका पूर्ण नियंत्रण", ctrlDesc: "Midiastore एंड्रॉइड स्कोप्ड स्टोरेज नीतियों का पूरी तरह सम्मान करता है।",
    privacy: "गोपनीयता नीति",
    successMsg: "🎉 वीआईपी स्थान सुरक्षित! आपका ईमेल आधिकारिक Google Play सूची में जोड़ दिया गया है!",
    duplicateMsg: "✅ यह ईमेल पहले से ही हमारी VIP सूची में पंजीकृत है!"
  },
  ja: {
    code: "ja-JP",
    title: "Midiastore — Androidの空き容量を安全かつスマートに解放",
    desc: "大容量ファイルやWhatsAppの隠れた不要データを安全に検出。クローズドテスターに参加してMidiastore Proを3ヶ月間無料で手に入れましょう！",
    eyebrow: "ストレージがいっぱい？ まずは正確な診断から",
    h1: "何が容量を圧迫しているか一目で把握。<br><em>削除の判断はあなた次第。</em>",
    lead: "MidiastoreはAndroid端末内の大きなメディア、完全一致の重複写真、忘れられたWhatsAppの音声メモを安全に検出します。",
    ctaHero: "クローズドテストに参加（VIP Pro無料）",
    note: "スキャンと確認は完全無料です。ファイルの削除には必ずAndroidシステムの確認画面が表示されます。",
    vipBadge: "⭐ Google Play 公式クローズドテスター募集",
    vipTitle: "クローズドテスター限定20名募集",
    vipDesc: "Google Playの正式公開要件を満たすため20名のボランティアを募集中です。メールアドレスを登録して<strong>Midiastore Pro 3ヶ月分（無料）</strong>をGET！",
    emailPlaceholder: "Google Playアカウントのメール (例: you@gmail.com)",
    btnSubmit: "VIPテスターに無料で申し込む",
    c1Tag: "高速検出", c1Title: "大容量メディアファイル", c1Desc: "ギガバイト単位で容量を食いつぶしている巨大な動画やダウンロードファイルを即座に特定。",
    c2Tag: "WhatsAppクリーナー", c2Title: "隠れたボイスメモと動画", c2Desc: "通常の写真ギャラリーには表示されない.nomediaフォルダ内の不要データを安全に検出。",
    c3Tag: "完全安全保証", c3Title: "SHA-256 重複写真検出", c3Desc: "正確なバイナリハッシュ比較により、大切な思い出の写真を誤って消去する心配はありません。",
    proEyebrow: "Midiastore Pro", proTitle: "スマートフォンの空き容量を最大化", proDesc: "1タップ一括削除、定期的な空き容量通知、そして完全広告ゼロの快適な体験を提供。",
    ctrlTitle: "お客様のファイル、お客様の完全な選択権", ctrlDesc: "MidiastoreはAndroidのScoped Storageに完全準拠。安全でプライバシーを最優先した設計です。",
    privacy: "プライバシーポリシー",
    successMsg: "🎉 VIP参加登録が完了しました！Google Playテスターリストに追加されました。",
    duplicateMsg: "✅ このメールアドレスは既にVIPリストに登録されています。"
  },
  zh: {
    code: "zh-CN",
    title: "Midiastore — 安全智能释放您的Android存储空间",
    desc: "精准定位大文件及WhatsApp隐藏媒体，保护珍贵回忆绝不误删。立即加入封闭测试，免费领取3个月Midiastore Pro！",
    eyebrow: "手机内存不足？从全面深度分析开始",
    h1: "轻松找出占用空间的庞大文件。<br><em>删不删由您完全掌控。</em>",
    lead: "Midiastore深度扫描设备内可访问媒体，秒级定位海量视频、精确重复照片及WhatsApp隐藏音频数据。无隐私泄露，零误删风险。",
    ctaHero: "加入封闭测试（免费获赠Pro版）",
    note: "存储分析与预览完全免费。任何删除操作均需经过Android系统原生弹窗确认。",
    vipBadge: "⭐ Google Play 官方封闭测试计划",
    vipTitle: "封闭测试限额招募中",
    vipDesc: "根据Google Play全球发布要求，急需20名志愿测试者。输入您的Google Play账号邮箱，立即获赠<strong>3个月Midiastore Pro专业版</strong>！",
    emailPlaceholder: "您的Google Play邮箱 (如: yourname@gmail.com)",
    btnSubmit: "立即锁定VIP名额",
    c1Tag: "精准定位", c1Title: "超大媒体文件", c1Desc: "快速扫描占用几个G空间的陈旧视频与下载文件，迅速腾出宝贵内存。",
    c2Tag: "WhatsApp专清", c2Title: "隐藏语音与转发视频", c2Desc: "深度清理常规相册不会显示的.nomedia音频与无用转发表情包。",
    c3Tag: "百分百安全", c3Title: "SHA-256 查重验证", c3Desc: "通过逐字节哈希比对照片，确保独一无二的照片绝不会被误删。",
    proEyebrow: "Midiastore Pro", proTitle: "释放极致手机性能", proDesc: "原生系统单次确认批量删除、定期清理智能提醒、永久纯净无广告。",
    ctrlTitle: "您的数据，您的最终决断", ctrlDesc: "Midiastore严格遵守Android Scoped Storage隐私沙盒规范，无需Root，安全透明。",
    privacy: "隐私政策",
    successMsg: "🎉 恭喜！您已成功锁定VIP测试资格，安装链接稍后将发送至您的邮箱！",
    duplicateMsg: "✅ 该邮箱已在我们的Google Play VIP测试名单中！"
  }
};

function renderLangNav(currentLang) {
  const links = [
    { id: "pt", label: "PT", url: "/" },
    { id: "en", label: "EN", url: "/en/" },
    { id: "es", label: "ES", url: "/es/" },
    { id: "de", label: "DE", url: "/de/" },
    { id: "fr", label: "FR", url: "/fr/" },
    { id: "hi", label: "HI", url: "/hi/" },
    { id: "ja", label: "JA", url: "/ja/" },
    { id: "zh", label: "ZH", url: "/zh/" },
  ];

  return `<div class="lang-nav">${links.map(l => `<a href="${l.url}" class="${l.id === currentLang ? "active" : ""}">${l.label}</a>`).join("")}</div>`;
}

function generateHtml(langKey, t) {
  const isRoot = langKey === "pt";
  const canonicalUrl = isRoot ? "https://midiastory-df6a0.web.app/" : `https://midiastory-df6a0.web.app/${langKey}/`;

  return `<!doctype html>
<html lang="${t.code}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#07111f">
  <title>${t.title}</title>
  <meta name="description" content="${t.desc}">
  <meta name="robots" content="index,follow">
  <link rel="canonical" href="${canonicalUrl}">
  <meta property="og:type" content="website">
  <meta property="og:title" content="${t.title}">
  <meta property="og:description" content="${t.desc}">
  <meta property="og:url" content="${canonicalUrl}">
  <meta property="og:site_name" content="Midiastore">
  <style>
    :root{color-scheme:dark;--bg:#07111f;--panel:#101d2e;--line:#26364a;--mint:#5de3bf;--white:#f5f8fc;--muted:#b3c0d0}
    *{box-sizing:border-box}body{margin:0;background:radial-gradient(ellipse at 50% 0%,#123047 0,transparent 55%),var(--bg);color:var(--white);font:16px/1.6 system-ui,-apple-system,"Segoe UI",sans-serif}
    a{color:inherit}.wrap{width:min(100% - 36px,1040px);margin:auto}header{padding:16px 0;border-bottom:1px solid var(--line)}.nav{display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap}.brand{font-weight:850;font-size:1.3rem;text-decoration:none;letter-spacing:-.04em}.brand span{color:var(--mint)}
    .lang-nav{display:flex;gap:6px;font-size:0.85rem;color:var(--muted);align-items:center}.lang-nav a{text-decoration:none;padding:3px 6px;border-radius:6px;transition:0.15s}.lang-nav a:hover,.lang-nav a.active{color:var(--mint);font-weight:700;background:rgba(93,227,191,0.1)}
    .button{display:inline-flex;justify-content:center;align-items:center;padding:13px 20px;border-radius:12px;background:var(--mint);color:#07111f;text-decoration:none;font-weight:800;min-height:48px;border:none;cursor:pointer;font-size:1rem;transition:transform 0.15s, opacity 0.15s}.button:hover{opacity:0.9;transform:translateY(-1px)}.button.secondary{background:transparent;color:var(--mint);border:1px solid #388b78}
    .hero{text-align:center;padding:50px 0 30px}.eyebrow{color:var(--mint);font-size:.82rem;font-weight:800;letter-spacing:.09em;text-transform:uppercase}.hero h1{font-size:clamp(2.3rem,6.5vw,4.5rem);line-height:1.06;letter-spacing:-.055em;margin:14px auto 20px;max-width:850px}.hero h1 em{font-style:normal;color:var(--mint)}.lead{font-size:1.15rem;color:var(--muted);max-width:690px;margin:0 auto 28px}.actions{display:flex;flex-wrap:wrap;justify-content:center;gap:12px}.note{font-size:.87rem;color:var(--muted);margin-top:16px}
    .vip-testers{margin:36px auto;padding:36px;border:2px solid var(--mint);border-radius:24px;background:linear-gradient(135deg,#0a2736,#101e30 70%);text-align:center;box-shadow:0 15px 40px rgba(93,227,191,0.15)}
    .vip-testers h2{font-size:clamp(1.8rem,4.5vw,2.5rem);margin:10px 0 14px;letter-spacing:-.03em}
    .vip-testers p{color:var(--muted);max-width:620px;margin:0 auto 24px;font-size:1.05rem}
    .tester-form{display:flex;flex-wrap:wrap;justify-content:center;gap:10px;max-width:540px;margin:0 auto}
    .tester-input{flex:1;min-width:260px;padding:14px 18px;border-radius:12px;border:1px solid var(--line);background:#07111f;color:#fff;font-size:1rem;outline:none}
    .tester-input:focus{border-color:var(--mint)}
    .tester-badge{display:inline-block;background:rgba(93,227,191,0.15);color:var(--mint);padding:6px 14px;border-radius:20px;font-weight:700;font-size:0.8rem;text-transform:uppercase;letter-spacing:0.08em;margin-bottom:12px}
    .feedback-msg{margin-top:16px;font-weight:700;font-size:0.95rem;display:none}
    .feedback-msg.success{display:block;color:var(--mint)}
    .feedback-msg.error{display:block;color:#f87171}
    section{padding:30px 0}.section-title{text-align:center;font-size:clamp(1.6rem,4vw,2.3rem);letter-spacing:-.03em}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:16px;margin-top:25px}.card{background:var(--panel);border:1px solid var(--line);border-radius:18px;padding:23px}.card h3{margin:0 0 8px;font-size:1.12rem}.card p{color:var(--muted);margin:0}.tag{font-size:.78rem;font-weight:800;color:var(--mint);text-transform:uppercase;letter-spacing:.07em;margin-bottom:8px}
    .pro{margin:40px auto;padding:34px;border:1px solid #338d79;border-radius:22px;background:linear-gradient(130deg,#102b34,#101d2e 65%);text-align:center}.pro h2{font-size:clamp(1.7rem,4vw,2.4rem);margin:8px 0}.pro p{color:var(--muted);max-width:650px;margin:0 auto 20px}.control{margin:28px auto 50px;max-width:790px;padding:25px;background:#0b1725;border:1px solid var(--line);border-radius:18px}.control h2{margin:0 0 8px}.control p{margin:0;color:var(--muted)}footer{border-top:1px solid var(--line);padding:25px 0 34px;color:var(--muted);font-size:.9rem;text-align:center}
    @media(max-width:600px){.nav{flex-direction:column}.lang-nav{margin-top:8px}}
  </style>
</head>
<body>
  <header>
    <div class="wrap nav">
      <a class="brand" href="${canonicalUrl}">Midia<span>store</span></a>
      ${renderLangNav(langKey)}
      <a class="button" href="#vip-testers">VIP Pro</a>
    </div>
  </header>

  <main class="wrap">
    <section class="hero">
      <div class="eyebrow">${t.eyebrow}</div>
      <h1>${t.h1}</h1>
      <p class="lead">${t.lead}</p>
      <div class="actions"><a class="button" href="#vip-testers">${t.ctaHero}</a></div>
      <p class="note">${t.note}</p>
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
      <div id="feedbackMsg" class="feedback-msg"></div>
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

    <section class="pro">
      <div class="eyebrow">${t.proEyebrow}</div>
      <h2>${t.proTitle}</h2>
      <p>${t.proDesc}</p>
      <a class="button" href="#vip-testers">${t.btnSubmit}</a>
    </section>

    <aside class="control">
      <h2>${t.ctrlTitle}</h2>
      <p>${t.ctrlDesc}</p>
    </aside>
  </main>

  <footer>
    <div class="wrap">
      <p>© 2026 Midiastore · <a href="/privacidade.html">${t.privacy}</a></p>
      <p>Google Play and Android are registered trademarks of Google LLC.</p>
    </div>
  </footer>

  <script>
    const form = document.getElementById('testerForm');
    const emailInput = document.getElementById('testerEmail');
    const submitBtn = document.getElementById('testerSubmitBtn');
    const feedback = document.getElementById('feedbackMsg');
    const SUPABASE_URL = "https://usmqtnlsaavxqguwrtix.supabase.co";
    const ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVzbXF0bmxzYWF2eHFndXdydGl4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTQwNzcwNTQsImV4cCI6MjEwOTY1MzA1NH0.N7XIpn3lGJM7V8clfU8uKe-7v-N3qKPkIgv3sCKv2VU";

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = emailInput.value.trim().toLowerCase();
      if (!email || !email.includes('@')) return;

      submitBtn.disabled = true;
      submitBtn.innerText = "Enviando...";
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
          feedback.innerText = "${t.successMsg}";
          emailInput.value = "";
        } else if (res.status === 409) {
          feedback.className = "feedback-msg success";
          feedback.innerText = "${t.duplicateMsg}";
        } else {
          feedback.className = "feedback-msg error";
          feedback.innerText = "Error. Please try again.";
        }
      } catch (err) {
        feedback.className = "feedback-msg error";
        feedback.innerText = "Connection error. Please try again.";
      } finally {
        submitBtn.disabled = false;
        submitBtn.innerText = "${t.btnSubmit}";
        feedback.style.display = "block";
      }
    });
  </script>
</body>
</html>`;
}

async function buildAll() {
  console.log("=== COMPILANDO PÁGINAS EM 8 IDIOMAS (MIDIASTORE) ===");

  for (const [langKey, t] of Object.entries(LANGUAGES)) {
    const html = generateHtml(langKey, t);
    const targetDir = langKey === "pt" ? docsDir : path.join(docsDir, langKey);
    fs.mkdirSync(targetDir, { recursive: true });
    const targetPath = path.join(targetDir, "index.html");
    fs.writeFileSync(targetPath, html, "utf8");
    console.log(`✅ [${langKey.toUpperCase()}] Gerado: ${path.relative(projectRoot, targetPath)}`);
  }

  console.log("\n🎉 Todas as 8 páginas internacionais foram compiladas com sucesso!");
}

buildAll().catch(console.error);
