import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const LOCALES = {
  pt: {
    file: "docs/index.html",
    lang: "pt-BR",
    banner: "📱 Veio das redes? Libere espaço no seu Android em 2 toques — grátis, leve (7,2 MB) e 100% seguro!",
    playCta: "📱 Baixar Grátis na Google Play",
    waitlistCta: "Acesso Antecipado (Teste Fechado)",
    proofBefore: "99% cheio",
    proofAfter: "12 GB livres",
    proofSize: "7,2 MB",
  },
  en: {
    file: "docs/en/index.html",
    lang: "en-US",
    banner: "📱 Coming from social media? Free up Android storage in 2 taps — free, ultra-light (7.2 MB) & 100% on-device!",
    playCta: "📱 Free Download on Google Play",
    waitlistCta: "Early Access (Closed Test)",
    proofBefore: "99% full",
    proofAfter: "12 GB free",
    proofSize: "7.2 MB",
  },
  es: {
    file: "docs/es/index.html",
    lang: "es-ES",
    banner: "📱 ¿Vienes de redes sociales? Libera espacio en Android en 2 toques — gratis, ligero (7,2 MB) y 100% seguro.",
    playCta: "📱 Descargar Gratis en Google Play",
    waitlistCta: "Acceso Anticipado (Prueba Cerrada)",
    proofBefore: "99% lleno",
    proofAfter: "12 GB libres",
    proofSize: "7,2 MB",
  },
  ar: {
    file: "docs/ar/index.html",
    lang: "ar",
    banner: "📱 قادم من وسائل التواصل؟ وفّر مساحة أندرويد بنقرتين — مجاني وخفيف (7.2 MB) وآمن 100%!",
    playCta: "📱 تنزيل مجاني من Google Play",
    waitlistCta: "الوصول المبكر (الاختبار المغلق)",
    proofBefore: "99% ممتلئ",
    proofAfter: "12 GB متاحة",
    proofSize: "7.2 MB",
  },
  ru: {
    file: "docs/ru/index.html",
    lang: "ru-RU",
    banner: "📱 Пришли из соцсетей? Освободите память Android в 2 касания — бесплатно, 7,2 МБ и 100% локально!",
    playCta: "📱 Скачать бесплатно в Google Play",
    waitlistCta: "Ранний доступ (Закрытый тест)",
    proofBefore: "99% занято",
    proofAfter: "12 ГБ свободно",
    proofSize: "7,2 МБ",
  },
  hi: {
    file: "docs/hi/index.html",
    lang: "hi-IN",
    banner: "📱 सोशल मीडिया से आए हैं? 2 टैप में Android स्टोरेज खाली करें — मुफ्त, हल्का (7.2 MB) और 100% सुरक्षित!",
    playCta: "📱 Google Play से मुफ्त डाउनलोड करें",
    waitlistCta: "अर्ली एक्सेस (क्लोज्ड टेस्ट)",
    proofBefore: "99% भरा",
    proofAfter: "12 GB खाली",
    proofSize: "7.2 MB",
  },
  id: {
    file: "docs/id/index.html",
    lang: "id-ID",
    banner: "📱 Dari media sosial? Kosongkan memori Android dalam 2 ketukan — gratis, ringan (7,2 MB) & 100% aman!",
    playCta: "📱 Unduh Gratis di Google Play",
    waitlistCta: "Akses Awal (Tes Tertutup)",
    proofBefore: "99% penuh",
    proofAfter: "12 GB bebas",
    proofSize: "7,2 MB",
  },
  bn: {
    file: "docs/bn/index.html",
    lang: "bn-BD",
    banner: "📱 সোশ্যাল মিডিয়া থেকে এসেছেন? ২ ট্যাপে Android স্টোরেজ খালি করুন — ফ্রি, হালকা (7.2 MB) ও ১০০% নিরাপদ!",
    playCta: "📱 Google Play থেকে ফ্রি ডাউনলোড",
    waitlistCta: "আর্লি অ্যাক্সেস (ক্লোজড টেস্ট)",
    proofBefore: "৯৯% পূর্ণ",
    proofAfter: "12 GB খালি",
    proofSize: "7.2 MB",
  },
  ko: {
    file: "docs/ko/index.html",
    lang: "ko-KR",
    banner: "📱 소셜 미디어에서 오셨나요? 2번의 탭으로 Android 저장공간 확보 — 무료, 초경량(7.2 MB), 100% 기기 내 처리!",
    playCta: "📱 Google Play에서 무료 다운로드",
    waitlistCta: "얼리 액세스 (비공개 테스트)",
    proofBefore: "99% 사용",
    proofAfter: "12 GB 확보",
    proofSize: "7.2 MB",
  },
  de: {
    file: "docs/de/index.html",
    lang: "de-DE",
    banner: "📱 Aus Social Media hier? Android-Speicher in 2 Tipps freigeben — kostenlos, ultraleicht (7,2 MB) & 100% lokal!",
    playCta: "📱 Kostenlos bei Google Play laden",
    waitlistCta: "Früher Zugang (Geschlossener Test)",
    proofBefore: "99% voll",
    proofAfter: "12 GB frei",
    proofSize: "7,2 MB",
  },
  fr: {
    file: "docs/fr/index.html",
    lang: "fr-FR",
    banner: "📱 Venu des réseaux ? Libérez votre stockage Android en 2 gestes — gratuit, léger (7,2 Mo) et 100% local !",
    playCta: "📱 Télécharger gratuitement sur Google Play",
    waitlistCta: "Accès Anticipé (Test Fermé)",
    proofBefore: "99% plein",
    proofAfter: "12 Go libres",
    proofSize: "7,2 Mo",
  },
  ja: {
    file: "docs/ja/index.html",
    lang: "ja-JP",
    banner: "📱 SNSからお越しですか？2タップでAndroidの空き容量を確保 — 無料・超軽量(7.2 MB)・100%端末内処理！",
    playCta: "📱 Google Playで無料ダウンロード",
    waitlistCta: "先行アクセス（クローズドテスト）",
    proofBefore: "99%使用中",
    proofAfter: "12 GB空き",
    proofSize: "7.2 MB",
  },
  zh: {
    file: "docs/zh/index.html",
    lang: "zh-CN",
    banner: "📱 来自社交媒体？2步释放 Android 存储空间 — 免费、仅 7.2 MB、100% 本地安全！",
    playCta: "📱 在 Google Play 免费下载",
    waitlistCta: "抢先体验（封闭测试）",
    proofBefore: "已用 99%",
    proofAfter: "释放 12 GB",
    proofSize: "7.2 MB",
  },
};

const MOBILE_CSS = `/* MOBILE-FIRST-FOLD-START */
    .social-welcome-banner{display:none;margin:0 auto 12px;max-width:640px;padding:10px 14px;border-radius:12px;background:linear-gradient(135deg,rgba(10,132,255,0.22),rgba(48,209,88,0.14));border:1px solid #0A84FF;font-size:0.86rem;font-weight:700;color:#fff;line-height:1.35}
    .hero-proof-pill{margin:12px auto 16px;padding:8px 14px;max-width:460px;border-radius:999px;background:#1C1C1E;border:1px solid #2C2C2E;display:inline-flex;align-items:center;justify-content:center;gap:8px;flex-wrap:wrap;font-size:0.84rem;font-weight:700;color:var(--text)}
    .hero-proof-pill .proof-before{color:#FF453A;font-weight:800}
    .hero-proof-pill .proof-arrow{color:var(--muted)}
    .hero-proof-pill .proof-after{color:#30D158;font-weight:900}
    .hero-proof-pill .proof-dot{color:var(--muted)}
    .hero-proof-pill .proof-size{color:var(--mint);font-weight:800}
    .hero-proof-bar{width:100%;max-width:280px;height:5px;border-radius:999px;background:#2C2C2E;overflow:hidden;margin:2px auto 0;display:flex}
    .hero-proof-bar span:first-child{width:38%;background:#0A84FF}
    .hero-proof-bar span:last-child{width:62%;background:#30D158;opacity:0.35}
    .hero-play-cta{font-size:1.05rem;padding:16px 30px;min-height:56px;box-shadow:0 10px 28px rgba(10,132,255,0.42)}
    @media (max-width: 640px){
      .wrap{width:calc(100% - 24px)}
      .nav{min-height:46px;padding:6px 0;gap:6px}
      .brand{font-size:1.1rem}
      .lang-nav{flex-wrap:nowrap;overflow-x:auto;max-width:100%;padding-bottom:2px;scrollbar-width:none;-webkit-overflow-scrolling:touch}
      .lang-nav::-webkit-scrollbar{display:none}
      .lang-nav a{font-size:0.72rem;padding:3px 6px;flex:0 0 auto}
      header .nav > div:last-child{display:none !important}
      .hero{padding:12px 0 10px}
      .social-welcome-banner{margin:0 auto 8px;padding:7px 10px;font-size:0.78rem}
      .eyebrow{font-size:0.74rem;margin-bottom:4px;letter-spacing:0.04em}
      h1{font-size:1.52rem;line-height:1.12;margin:0 0 8px}
      .lead{font-size:0.88rem;line-height:1.35;margin:0 auto 10px}
      .hero-proof-pill{margin:0 auto 10px;padding:7px 12px;font-size:0.78rem;gap:6px;width:100%;border-radius:14px}
      .actions{flex-direction:column;align-items:stretch;gap:8px}
      .hero-play-cta{width:100%;font-size:1rem !important;padding:14px 18px !important;min-height:52px !important}
      .actions .button.secondary{width:100%;min-height:42px;padding:9px 14px;font-size:0.86rem}
      .pulse-strip{margin:10px auto 0;padding:8px 10px;gap:8px;font-size:0.76rem;border-radius:12px}
      .note{font-size:0.74rem;margin-top:8px;line-height:1.35}
    }
    /* MOBILE-FIRST-FOLD-END */`;

function buildHeroScript(langCode) {
  return `<!-- HERO-UTM-SCRIPT-START -->
      <script>
        (function(){
          try {
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
            var utmSource = rawSrc || (joined.indexOf("instagram") !== -1 ? "instagram" : joined.indexOf("tiktok") !== -1 ? "tiktok" : joined.indexOf("shorts") !== -1 || joined.indexOf("youtube") !== -1 ? "shorts" : "midiastore_web");
            var utmMedium = rawMed || (joined.indexOf("story") !== -1 ? "story" : joined.indexOf("reels") !== -1 ? "reels" : joined.indexOf("bio") !== -1 ? "bio" : isSocial ? "social" : "landing_page");
            var utmCampaign = rawCamp || (isSocial ? "aconteceunobr" : "home_hero");
            var refParts = [
              "utm_source=" + encodeURIComponent(utmSource),
              "utm_medium=" + encodeURIComponent(utmMedium),
              "utm_campaign=" + encodeURIComponent(utmCampaign)
            ];
            if (rawCont) refParts.push("utm_content=" + encodeURIComponent(rawCont));
            var playRef = refParts.join("&");
            var btn = document.getElementById("heroPlayBtn");
            if (btn) {
              btn.href = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore&referrer=" + encodeURIComponent(playRef);
              btn.addEventListener("click", function() {
                if (window.MidiastoreTelemetry && btn.dataset.msBound === "1") return;
                var vid = "";
                try { vid = localStorage.getItem("ms_anon_vid") || ""; } catch(_) {}
                if (!/^[a-f0-9]{16,64}$/i.test(vid)) {
                  vid = "a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5";
                }
                var bodyStr = JSON.stringify({
                  installHash: vid,
                  eventName: "play_store_click",
                  appVersion: "web-2.8.2",
                  locale: "${langCode}",
                  properties: {
                    path: window.location.pathname || "/",
                    cta: "hero_play_store",
                    utm_source: utmSource,
                    utm_medium: utmMedium,
                    utm_campaign: utmCampaign,
                    utm_content: rawCont || null
                  }
                });
                var endpoint = "https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/ingest-product-event";
                var sent = false;
                try {
                  if (navigator.sendBeacon) {
                    sent = navigator.sendBeacon(endpoint, new Blob([bodyStr], { type: "text/plain;charset=UTF-8" }));
                  }
                } catch(_) {}
                if (!sent) {
                  try {
                    fetch(endpoint, { method: "POST", keepalive: true, headers: { "Content-Type": "text/plain;charset=UTF-8" }, body: bodyStr }).catch(function(){});
                  } catch(_) {}
                }
              });
            }
          } catch(e) {}
        })();
      </script>
      <!-- HERO-UTM-SCRIPT-END -->`;
}

let updatedCount = 0;

for (const [code, cfg] of Object.entries(LOCALES)) {
  const fullPath = path.join(projectRoot, cfg.file);
  if (!fs.existsSync(fullPath)) {
    console.error(`[MISSING] ${cfg.file}`);
    continue;
  }

  let html = fs.readFileSync(fullPath, "utf8");

  // 1. Inject or replace responsive mobile first-fold CSS
  const cssRegex = /\/\* MOBILE-FIRST-FOLD-START \*\/[\s\S]*?\/\* MOBILE-FIRST-FOLD-END \*\//;
  if (cssRegex.test(html)) {
    html = html.replace(cssRegex, MOBILE_CSS);
  } else {
    html = html.replace("</style>", `\n    ${MOBILE_CSS}\n  </style>`);
  }

  // 2. Replace/upgrade hero section content cleanly while preserving localized eyebrow, h1, lead, pulse-strip, note
  const heroMatch = html.match(/<section class="hero">([\s\S]*?)<\/section>/);
  if (!heroMatch) {
    console.error(`[NO HERO] ${cfg.file}`);
    continue;
  }

  let heroInner = heroMatch[1];

  // Remove old igWelcomeBanner, hero-proof-pill, old inline script
  heroInner = heroInner.replace(/<div id="igWelcomeBanner"[\s\S]*?<\/div>\s*/g, "");
  heroInner = heroInner.replace(/<div class="hero-proof-pill"[\s\S]*?<\/div>\s*/g, "");
  heroInner = heroInner.replace(/<!-- HERO-UTM-SCRIPT-START -->[\s\S]*?<!-- HERO-UTM-SCRIPT-END -->\s*/g, "");
  heroInner = heroInner.replace(/<script>[\s\S]*?igWelcomeBanner[\s\S]*?<\/script>\s*/g, "");

  // Inject banner at top of hero
  const bannerHtml = `<div id="igWelcomeBanner" class="social-welcome-banner">${cfg.banner}</div>\n      `;
  heroInner = `\n      ${bannerHtml}${heroInner.trimStart()}`;

  // Build proof pill + standardized actions block
  const proofPillHtml = `<div class="hero-proof-pill" aria-label="${cfg.proofBefore} -> ${cfg.proofAfter} • ${cfg.proofSize}">
        <span class="proof-before">${cfg.proofBefore}</span>
        <span class="proof-arrow">→</span>
        <span class="proof-after">${cfg.proofAfter}</span>
        <span class="proof-dot">•</span>
        <span class="proof-size">${cfg.proofSize}</span>
        <span class="hero-proof-bar" aria-hidden="true"><span></span><span></span></span>
      </div>`;

  const actionsHtml = `${proofPillHtml}
      <div class="actions">
        <a id="heroPlayBtn" class="button hero-play-cta" href="https://play.google.com/store/apps/details?id=com.rdf112.midiastore&amp;referrer=utm_source%3Dmidiastore_web%26utm_medium%3Dlanding_page" data-track-cta="hero_play_store" target="_blank" rel="noopener">${cfg.playCta}</a>
        <a class="button secondary" href="#vip-testers" data-track-cta="hero_waitlist" onclick="document.getElementById('testerEmail').focus()">${cfg.waitlistCta}</a>
      </div>`;

  heroInner = heroInner.replace(/<div class="actions">[\s\S]*?<\/div>/, actionsHtml);
  heroInner = `${heroInner.trimEnd()}\n      ${buildHeroScript(cfg.lang)}\n    `;

  html = html.replace(/<section class="hero">[\s\S]*?<\/section>/, `<section class="hero">${heroInner}</section>`);

  fs.writeFileSync(fullPath, html, "utf8");
  updatedCount++;
  console.log(`[OK] ${code.padEnd(2)} -> ${cfg.file}`);
}

console.log(`\nConcluido: ${updatedCount}/${Object.keys(LOCALES).length} paginas otimizadas.`);
