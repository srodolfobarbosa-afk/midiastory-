#!/usr/bin/env node
/**
 * ATAQUE DE CAUDA LONGA POR MODELO DE CELULAR (Módulo 8A — +72 Páginas Ultra-Específicas)
 *
 * Combina 8 modelos campeões de venda:
 *   Samsung Galaxy A54, A15, S23 | Motorola Moto G84, G54 | Xiaomi Redmi Note 12, Note 13 | Poco X6
 * x 3 dores ultra-específicas:
 *   - "armazenamento 'Outros' ocupando espaço" (other-storage-full)
 *   - "WhatsApp cheio mesmo apagando" (whatsapp-full-after-deleting)
 *   - "system storage full fix" (system-storage-full-fix)
 * x 3 idiomas (pt-br, en-us, es-419) = 72 novas páginas pSEO!
 *
 * Cada página traz: comparação direta com o problema do aparelho, FAQ Schema.org FAQPage,
 * SoftwareApplication + BreadcrumbList, CTA acima da dobra e navigator.sendBeacon.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const docsDir = path.join(projectRoot, "docs");
const guidesDir = path.join(docsDir, "guides");
const sitemapPath = path.join(docsDir, "sitemap.xml");

const SITE_BASE = "https://midiastory-df6a0.web.app";
const PLAY_URL_BASE = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";
const TODAY = new Date().toISOString().slice(0, 10);

const DEVICES = [
  { slug: "galaxy-a54", name: "Samsung Galaxy A54", ui: "One UI", typicalIssue: "4K camera clips, Samsung Trash 30-day hold & WhatsApp media" },
  { slug: "galaxy-a15", name: "Samsung Galaxy A15", ui: "One UI Core", typicalIssue: "128 GB filling quickly with forwarded videos & duplicate photos" },
  { slug: "galaxy-s23", name: "Samsung Galaxy S23", ui: "One UI", typicalIssue: "High-bitrate 4K/8K videos & hidden duplicate copies" },
  { slug: "moto-g84", name: "Motorola Moto G84", ui: "Hello UI / Android 15", typicalIssue: "Large videos, old screenshots & WhatsApp group duplicates" },
  { slug: "moto-g54", name: "Motorola Moto G54", ui: "MyUX / Android", typicalIssue: "Accumulated WhatsApp videos & repeated gallery copies" },
  { slug: "redmi-note-12", name: "Xiaomi Redmi Note 12", ui: "MIUI / HyperOS", typicalIssue: "'Other / System' bloat perception caused by large media & duplicates" },
  { slug: "redmi-note-13", name: "Xiaomi Redmi Note 13", ui: "HyperOS", typicalIssue: "Ad-heavy built-in cleaner + large videos & duplicate WhatsApp files" },
  { slug: "poco-x6", name: "Poco X6", ui: "HyperOS", typicalIssue: "Heavy media files, screenshots & duplicate clips consuming storage" },
];

const PAINS = [
  {
    slug: "other-storage-full",
    copy: {
      "pt-br": {
        titleTpl: "{device}: Armazenamento 'Outros' Ocupando Espaço — Como Resolver",
        h1Tpl: "{device} com 'Outros' Lotando a Memória? Libere GBs com Segurança",
        subTpl: "No {device} ({ui}), arquivos grandes escondidos, lixeira e cópias repetidas inflam a barra de armazenamento. Veja o que realmente ocupa espaço sem formatar.",
        deviceVs: "Limpador nativo genérico / Apps de 120 MB: anúncios invasivos e exclusão arriscada sem SHA-256.",
        midiastoreVs: "Midiastore (5,9 MB): 100% offline no {device}, hash SHA-256 exato e confirmação oficial Android.",
      },
      "en-us": {
        titleTpl: "{device} 'Other' Storage Full Fix: Reclaim Real Gigabytes Safely",
        h1Tpl: "{device} 'Other' Storage Taking Up Space? Fix It in 60 Seconds",
        subTpl: "On {device} ({ui}), oversized videos, Trash retention, and exact duplicates silently eat storage. Review every file safely on-device.",
        deviceVs: "Bloated 100+ MB cleaners: scare tactics, fake RAM boosts, and risky auto-delete.",
        midiastoreVs: "Midiastore (5.9 MB): 100% on-device on {device}, exact SHA-256 duplicates, zero auto-delete.",
      },
      "es-419": {
        titleTpl: "{device}: Almacenamiento 'Otros' Ocupando Espacio — Solución Real",
        h1Tpl: "¿{device} con 'Otros' Llenando tu Memoria? Libera Gigas Hoy",
        subTpl: "En tu {device} ({ui}), videos pesados, papelera y duplicados llenan el espacio sin avisar. Revisa todo localmente antes de borrar.",
        deviceVs: "Limpiadores pesados con anuncios: prometen milagros falsos y borran sin control.",
        midiastoreVs: "Midiastore (5,9 MB): 100% local en {device}, verificación SHA-256 y control total.",
      },
    },
  },
  {
    slug: "whatsapp-full-after-deleting",
    copy: {
      "pt-br": {
        titleTpl: "{device}: WhatsApp Cheio Mesmo Apagando Fotos — Como Liberar Espaço",
        h1Tpl: "WhatsApp Cheio Mesmo Apagando no {device}? Resolva Agora",
        subTpl: "Mesmo apagando conversas no {device}, vídeos gigantes e cópias duplicadas continuam na galeria e na Lixeira. O Midiastore (5,9 MB) encontra tudo com SHA-256.",
        deviceVs: "Apagar conversas manualmente no {device}: demorado e ainda deixa cópias repetidas na mídia.",
        midiastoreVs: "Midiastore (5,9 MB): agrupa duplicatas idênticas (SHA-256) e vídeos gigantes em segundos.",
      },
      "en-us": {
        titleTpl: "{device} WhatsApp Storage Still Full After Deleting Chats: Fix",
        h1Tpl: "WhatsApp Storage Still Full on {device} After Deleting? Real Fix",
        subTpl: "Even after clearing chats on {device}, forwarded videos and exact duplicate copies stay in local storage or Trash. Clear them safely with SHA-256.",
        deviceVs: "Deleting chats one by one on {device}: slow and misses duplicate media files.",
        midiastoreVs: "Midiastore (5.9 MB): finds byte-exact SHA-256 duplicates & large videos 100% on-device.",
      },
      "es-419": {
        titleTpl: "{device}: WhatsApp Lleno Aunque Borre Fotos — Cómo Liberar Espacio",
        h1Tpl: "¿WhatsApp Sigue Lleno Aunque Borres en tu {device}? Solución Rápida",
        subTpl: "En tu {device}, videos reenviados y copias repetidas siguen ocupando GBs. Midiastore (5,9 MB) los detecta con SHA-256 sin borrar nada sin permiso.",
        deviceVs: "Borrar chats a mano en {device}: lento y deja copias repetidas en tu galería.",
        midiastoreVs: "Midiastore (5,9 MB): detecta duplicados exactos SHA-256 y videos gigantes en segundos.",
      },
    },
  },
  {
    slug: "system-storage-full-fix",
    copy: {
      "pt-br": {
        titleTpl: "{device}: Erro 'Armazenamento Quase Cheio' (System Storage Fix)",
        h1Tpl: "{device} Travando com 'Armazenamento Quase Cheio'? Resolva Sem Formatar",
        subTpl: "Não formate seu {device}! Com apenas 5,9 MB, o Midiastore cabe mesmo com a memória no limite e mostra vídeos grandes, prints antigos e duplicatas exatas.",
        deviceVs: "Formatar o {device} ou desinstalar apps bancários: dor de cabeça desnecessária.",
        midiastoreVs: "Midiastore (5,9 MB): libera gigabytes reais mantendo seus apps e fotos originais intactos.",
      },
      "en-us": {
        titleTpl: "{device} System Storage Full Fix: Free Up Space Without Reset",
        h1Tpl: "{device} System Storage Full? Reclaim Gigabytes Without Resetting",
        subTpl: "Don't factory reset your {device}. At just 5.9 MB, Midiastore installs even when space is critical and surfaces huge videos, old screenshots, and exact duplicates.",
        deviceVs: "Factory resetting your {device} or deleting favorite apps: unnecessary friction.",
        midiastoreVs: "Midiastore (5.9 MB): pure OLED cleaner using Scoped Storage + SHA-256 verification.",
      },
      "es-419": {
        titleTpl: "{device}: Error 'Almacenamiento Lleno' — Cómo Liberar Sin Formatear",
        h1Tpl: "¿Tu {device} Dice 'Almacenamiento Lleno'? Libera Espacio Sin Formatear",
        subTpl: "No reinicies de fábrica tu {device}. Con solo 5,9 MB, Midiastore encuentra videos pesados, capturas viejas y duplicados SHA-256 en menos de 1 minuto.",
        deviceVs: "Formatear tu {device} o desinstalar apps importantes: pérdida de tiempo.",
        midiastoreVs: "Midiastore (5,9 MB): limpieza real 100% local con confirmación oficial de Android.",
      },
    },
  },
];

const LANGS = [
  { code: "pt-br", hreflang: "pt-BR", htmlLang: "pt-BR", cta: "Baixar Midiastore grátis no Google Play (5,9 MB)", compareTitle: "Comparação Direta", faqTitle: "Perguntas Frequentes" },
  { code: "en-us", hreflang: "en-US", htmlLang: "en", cta: "Download Midiastore Free on Google Play (5.9 MB)", compareTitle: "Direct Comparison", faqTitle: "Frequently Asked Questions" },
  { code: "es-419", hreflang: "es-419", htmlLang: "es", cta: "Descargar Midiastore gratis en Google Play (5,9 MB)", compareTitle: "Comparación Directa", faqTitle: "Preguntas Frecuentes" },
];

function esc(s) {
  return String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function fill(tpl, device) {
  return tpl.replace(/\{device\}/g, device.name).replace(/\{ui\}/g, device.ui);
}

export function generateDevicePseoPages() {
  const urls = [];
  let count = 0;

  for (const lang of LANGS) {
    for (const device of DEVICES) {
      for (const pain of PAINS) {
        const slug = `${device.slug}-${pain.slug}`;
        const raw = pain.copy[lang.code];
        const title = fill(raw.titleTpl, device);
        const h1 = fill(raw.h1Tpl, device);
        const sub = fill(raw.subTpl, device);
        const deviceVs = fill(raw.deviceVs, device);
        const midiastoreVs = fill(raw.midiastoreVs, device);
        const canonical = `${SITE_BASE}/guides/${lang.code}/${slug}/`;
        const playHref = `${PLAY_URL_BASE}&utm_source=pseo_device&utm_medium=organic&utm_campaign=${lang.code}_${slug}`;

        const faqs =
          lang.code === "pt-br"
            ? [
                { q: `O Midiastore funciona bem no ${device.name}?`, a: `Sim! É otimizado para Android 10–16 (${device.ui}), pesa apenas 5,9 MB e roda 100% no aparelho.` },
                { q: `Apagar arquivos no ${device.name} vai direto para a Lixeira?`, a: `No Android 11+, você pode escolher Lixeira ou exclusão permanente. O Midiastore só conta espaço como liberado após a exclusão permanente confirmada.` },
              ]
            : lang.code === "es-419"
              ? [
                  { q: `¿Midiastore funciona bien en el ${device.name}?`, a: `Sí, pesa solo 5,9 MB, funciona 100% local en ${device.ui} y nunca borra nada sin tu confirmación.` },
                  { q: `¿Por qué sigue lleno mi ${device.name} después de borrar fotos?`, a: `Mover fotos a la Papelera no libera espacio hasta vaciarla permanentemente; además, videos grandes y duplicados suelen ocupar varios GB.` },
                ]
              : [
                  { q: `Is Midiastore safe on ${device.name}?`, a: `Yes. It weighs only 5.9 MB, runs 100% on-device on ${device.ui} via Android Scoped Storage, and never deletes files without your confirmation.` },
                  { q: `Why is my ${device.name} still full after deleting photos?`, a: `Moving files to Trash does not free bytes until permanently deleted, and oversized videos or exact SHA-256 duplicates often take up gigabytes.` },
                ];

        const faqLd = {
          "@context": "https://schema.org",
          "@type": "FAQPage",
          mainEntity: faqs.map((f) => ({
            "@type": "Question",
            name: f.q,
            acceptedAnswer: { "@type": "Answer", text: f.a },
          })),
        };
        const swLd = {
          "@context": "https://schema.org",
          "@type": "SoftwareApplication",
          name: "Midiastore",
          operatingSystem: "Android 10+",
          applicationCategory: "UtilitiesApplication",
          fileSize: "5.9MB",
          offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
          downloadUrl: PLAY_URL_BASE,
          url: canonical,
        };
        const bcLd = {
          "@context": "https://schema.org",
          "@type": "BreadcrumbList",
          itemListElement: [
            { "@type": "ListItem", position: 1, name: "Midiastore", item: `${SITE_BASE}/` },
            { "@type": "ListItem", position: 2, name: "Guides", item: `${SITE_BASE}/guides/` },
            { "@type": "ListItem", position: 3, name: h1, item: canonical },
          ],
        };

        const hreflangs = LANGS.map(
          (l) => `  <link rel="alternate" hreflang="${l.hreflang}" href="${SITE_BASE}/guides/${l.code}/${slug}/" />`,
        )
          .concat(`  <link rel="alternate" hreflang="x-default" href="${SITE_BASE}/guides/en-us/${slug}/" />`)
          .join("\n");

        const html = `<!DOCTYPE html>
<html lang="${lang.htmlLang}">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${esc(title)} | Midiastore</title>
  <meta name="description" content="${esc(sub)}" />
  <link rel="canonical" href="${canonical}" />
${hreflangs}
  <meta name="theme-color" content="#000000" />
  <script type="application/ld+json">${JSON.stringify(swLd)}</script>
  <script type="application/ld+json">${JSON.stringify(faqLd)}</script>
  <script type="application/ld+json">${JSON.stringify(bcLd)}</script>
  <style>
    :root { --bg:#000; --card:#1C1C1E; --text:#F5F5F7; --muted:#98989D; --accent:#0A84FF; --border:#2C2C2E; }
    * { box-sizing:border-box; }
    body { margin:0; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; background:var(--bg); color:var(--text); line-height:1.6; }
    .wrap { max-width:760px; margin:0 auto; padding:24px 18px 60px; }
    .top { display:flex; justify-content:space-between; padding-bottom:16px; border-bottom:1px solid var(--border); font-size:13px; }
    .top a { color:var(--muted); text-decoration:none; }
    .badge { display:inline-block; margin-top:20px; padding:6px 12px; border-radius:999px; background:#111827; border:1px solid #1E3A8A; color:#93C5FD; font-size:12px; font-weight:600; }
    h1 { font-size:clamp(25px,5vw,36px); line-height:1.18; margin:14px 0 12px; }
    .sub { color:var(--muted); font-size:16px; margin:0 0 22px; }
    .cta-btn { display:block; width:100%; text-align:center; background:var(--accent); color:#fff; text-decoration:none; font-weight:700; font-size:17px; padding:16px 20px; border-radius:14px; }
    .card { background:var(--card); border:1px solid var(--border); border-radius:16px; padding:20px; margin:20px 0; }
    .cmp { display:grid; gap:12px; }
    .cmp-box { padding:14px; border-radius:12px; background:#121214; border:1px solid var(--border); font-size:14px; }
    .cmp-box.win { border-color:var(--accent); }
    details { border-top:1px solid var(--border); padding:10px 0; }
    details:first-of-type { border-top:none; padding-top:0; }
    summary { font-weight:600; cursor:pointer; }
    details p { color:var(--muted); margin:6px 0 0; font-size:14px; }
  </style>
</head>
<body>
  <main class="wrap">
    <header class="top"><a href="/" style="color:#fff;font-weight:700;">Midiastore</a><a href="/guides/">All Guides</a></header>
    <span class="badge">${esc(device.name)} • ${esc(device.ui)} • 5.9 MB</span>
    <h1>${esc(h1)}</h1>
    <p class="sub">${esc(sub)}</p>
    <a class="cta-btn" href="${playHref}" rel="noopener">${esc(lang.cta)}</a>
    <section class="card">
      <h2 style="margin:0 0 12px;font-size:19px;">${esc(lang.compareTitle)}</h2>
      <div class="cmp">
        <div class="cmp-box">❌ ${esc(deviceVs)}</div>
        <div class="cmp-box win">✅ ${esc(midiastoreVs)}</div>
      </div>
    </section>
    <section class="card">
      <h2 style="margin:0 0 12px;font-size:19px;">${esc(lang.faqTitle)}</h2>
      ${faqs.map((f) => `<details open><summary>${esc(f.q)}</summary><p>${esc(f.a)}</p></details>`).join("\n      ")}
    </section>
    <a class="cta-btn" href="${playHref}" rel="noopener">${esc(lang.cta)}</a>
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
      } catch (e) { vid = "0000000000000001"; }
      function send(ev, extra) {
        var payload = JSON.stringify({ installHash: vid, eventName: ev, appVersion: "web-pseo-device-1", locale: "${lang.hreflang}", properties: Object.assign({ path: window.location.pathname, device: "${device.slug}", pain: "${pain.slug}" }, extra || {}) });
        if (navigator.sendBeacon) navigator.sendBeacon(ENDPOINT, payload);
      }
      send("page_view");
      document.querySelectorAll(".cta-btn").forEach(function(b, i){ b.addEventListener("click", function(){ send("play_store_click", { ctaIndex: i + 1 }); }); });
    })();
  </script>
</body>
</html>
`;
        const outDir = path.join(guidesDir, lang.code, slug);
        fs.mkdirSync(outDir, { recursive: true });
        fs.writeFileSync(path.join(outDir, "index.html"), html, "utf8");
        urls.push(canonical);
        count += 1;
      }
    }
  }

  // Append new device URLs to sitemap.xml idempotently
  let xml = fs.readFileSync(sitemapPath, "utf8");
  let added = 0;
  const entries = [];
  for (const u of urls) {
    if (!xml.includes(`<loc>${u}</loc>`)) {
      entries.push(`  <url>\n    <loc>${u}</loc>\n    <lastmod>${TODAY}</lastmod>\n    <changefreq>weekly</changefreq>\n    <priority>0.8</priority>\n  </url>`);
      added += 1;
    }
  }
  if (entries.length > 0) {
    xml = xml.replace("</urlset>", `${entries.join("\n")}\n</urlset>`);
    fs.writeFileSync(sitemapPath, xml, "utf8");
  }
  const totalUrls = (xml.match(/<loc>/g) || []).length;
  return { devicePagesGenerated: count, sitemapAdded: added, sitemapTotalUrls: totalUrls };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  console.log("📱 GERADOR DE 72 PÁGINAS LONG-TAIL POR MODELO DE CELULAR:");
  console.log(JSON.stringify(generateDevicePseoPages(), null, 2));
}
