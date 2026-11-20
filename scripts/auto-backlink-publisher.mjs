#!/usr/bin/env node
/**
 * MOTOR DE BACKLINKS VIA API — TELEGRAPH (telegra.ph) & DEV.TO (Módulo 8C)
 *
 * Publica artigos técnicos reais via API gratuita:
 * 1. Telegraph API (api.telegra.ph): cria conta programaticamente (zero cadastro manual)
 *    e publica artigo real com backlinks apontando para /guides/ e Google Play.
 * 2. Dev.to API (dev.to/api/articles): pronto para publicar automaticamente quando DEVTO_API_KEY existir.
 *
 * Uso:
 *   node scripts/auto-backlink-publisher.mjs --platform telegraph
 *   node scripts/auto-backlink-publisher.mjs --platform devto
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const tokenCachePath = path.join(projectRoot, ".tmp", "telegraph-account.json");

const SITE_GUIDES_URL = "https://midiastory-df6a0.web.app/guides/";
const PLAY_URL = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore&utm_source=telegraph&utm_medium=backlink";

async function getOrCreateTelegraphToken() {
  if (fs.existsSync(tokenCachePath)) {
    try {
      const cached = JSON.parse(fs.readFileSync(tokenCachePath, "utf8"));
      if (cached?.access_token) return cached.access_token;
    } catch {}
  }

  const res = await fetch("https://api.telegra.ph/createAccount", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      short_name: "MidiastoreEng",
      author_name: "Midiastore Engineering",
      author_url: SITE_GUIDES_URL,
    }),
    signal: AbortSignal.timeout(12000),
  });
  const data = await res.json();
  if (!data?.ok || !data?.result?.access_token) {
    throw new Error(`Telegraph createAccount falhou: ${JSON.stringify(data)}`);
  }

  fs.mkdirSync(path.dirname(tokenCachePath), { recursive: true });
  fs.writeFileSync(tokenCachePath, JSON.stringify(data.result, null, 2), "utf8");
  return data.result.access_token;
}

export async function publishToTelegraph() {
  const accessToken = await getOrCreateTelegraphToken();

  const contentNodes = [
    {
      tag: "p",
      children: [
        "Most Android users hit the dreaded 'Storage Space Running Out' alert not because of their favorite photos, but because of oversized forwarded videos (WhatsApp/Telegram), accumulated screenshots, and byte-exact duplicate files hidden across folders.",
      ],
    },
    { tag: "h3", children: ["Why Size-Only Duplicate Cleaners Are Risky"] },
    {
      tag: "p",
      children: [
        "Comparing files by file size or filename alone can produce false positives. A safer engineering approach uses Android's official Scoped Storage (MediaStore) combined with streaming SHA-256 cryptographic hashing — proving two files are 100% identical byte for byte while keeping the original untouched.",
      ],
    },
    { tag: "h3", children: ["Zero Bloatware: The 5.9 MB On-Device Architecture"] },
    {
      tag: "p",
      children: [
        "Instead of installing a 100+ MB cleaner packed with invasive ad SDKs and fake RAM/CPU boosters, ",
        { tag: "a", attrs: { href: PLAY_URL }, children: ["Midiastore on Google Play"] },
        " is R8-minified to 5.9 MB, runs 100% on-device with pure OLED dark UI, and never deletes any file without explicit user review and Android's system confirmation dialog.",
      ],
    },
    { tag: "h3", children: ["Device-Specific Android Storage Guides (Samsung, Motorola, Xiaomi & WhatsApp)"] },
    {
      tag: "ul",
      children: [
        {
          tag: "li",
          children: [
            { tag: "a", attrs: { href: SITE_GUIDES_URL }, children: ["Complete Android Storage Guides Hub (PT-BR, EN-US, ES-419, HI-IN)"] },
          ],
        },
        {
          tag: "li",
          children: [
            { tag: "a", attrs: { href: "https://midiastory-df6a0.web.app/guides/en-us/whatsapp-storage-full/" }, children: ["How to Free Up WhatsApp Storage Without Losing Photos"] },
          ],
        },
        {
          tag: "li",
          children: [
            { tag: "a", attrs: { href: "https://midiastory-df6a0.web.app/guides/en-us/remove-duplicate-photos-android/" }, children: ["Remove Duplicate Photos on Android Safely Using SHA-256"] },
          ],
        },
        {
          tag: "li",
          children: [
            { tag: "a", attrs: { href: "https://midiastory-df6a0.web.app/guides/en-us/samsung-galaxy-storage-full/" }, children: ["Samsung Galaxy Storage Full Fix"] },
          ],
        },
      ],
    },
  ];

  const res = await fetch("https://api.telegra.ph/createPage", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      access_token: accessToken,
      title: "How to Fix Android Storage Full Safely Using Streaming SHA-256 & Scoped Storage",
      author_name: "Midiastore Engineering",
      author_url: SITE_GUIDES_URL,
      content: contentNodes,
      return_content: false,
    }),
    signal: AbortSignal.timeout(15000),
  });
  const data = await res.json();
  if (!data?.ok || !data?.result?.url) {
    throw new Error(`Telegraph createPage falhou: ${JSON.stringify(data)}`);
  }
  return {
    platform: "telegraph",
    published: true,
    url: data.result.url,
    path: data.result.path,
    title: data.result.title,
  };
}

export async function publishToDevTo() {
  const apiKey = process.env.DEVTO_API_KEY?.trim();
  if (!apiKey) {
    return {
      platform: "devto",
      published: false,
      ready: true,
      reason: "Defina DEVTO_API_KEY para publicar automaticamente em dev.to/api/articles.",
    };
  }

  const bodyMarkdown = [
    "Most Android storage cleaners are 100 MB+ apps filled with fake RAM boosters. Here is how we engineered **Midiastore** (`5.9 MB`, R8-minified) using strictly Android **Scoped Storage (MediaStore)** and **streaming SHA-256** duplicate detection.",
    "",
    "### Key Engineering Decisions",
    "- **Zero `MANAGE_EXTERNAL_STORAGE`:** works cleanly across Android 10–16 via MediaStore.",
    "- **Streaming SHA-256:** hashes only repeated-size buckets off the UI thread with strict memory bounds.",
    "- **Transparent Trash:** never credits Trash moves as freed storage until permanent deletion is confirmed.",
    "",
    `Explore our full global Android storage guides: [Midiastore Guides Hub](${SITE_GUIDES_URL})`,
    `Try Midiastore on Google Play: [Midiastore (5.9 MB)](${PLAY_URL})`,
  ].join("\n");

  const res = await fetch("https://dev.to/api/articles", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "api-key": apiKey,
    },
    body: JSON.stringify({
      article: {
        title: "Engineering a 5.9 MB Android Duplicate Media Cleaner With Streaming SHA-256 & Scoped Storage",
        published: true,
        body_markdown: bodyMarkdown,
        tags: ["android", "kotlin", "privacy", "mobile"],
        canonical_url: "https://midiastory-df6a0.web.app/guides/en-us/remove-duplicate-photos-android/",
      },
    }),
    signal: AbortSignal.timeout(15000),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(`Dev.to API falhou (HTTP ${res.status}): ${JSON.stringify(data)}`);
  }
  return { platform: "devto", published: true, url: data.url, id: data.id };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const pIdx = process.argv.indexOf("--platform");
  const platform = pIdx > -1 ? process.argv[pIdx + 1] : "telegraph";
  const fn = platform === "devto" ? publishToDevTo : publishToTelegraph;

  fn()
    .then((res) => {
      console.log("🔗 AUTO BACKLINK PUBLISHER:");
      console.log(JSON.stringify(res, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro no auto-backlink-publisher:", err.message);
      process.exit(1);
    });
}
