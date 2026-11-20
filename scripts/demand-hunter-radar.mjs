#!/usr/bin/env node
/**
 * RADAR DE DOR EM TEMPO REAL — CAÇADOR DE CLIENTES (Módulo 8B)
 *
 * Consulta feeds públicos (Reddit JSON/RSS sem login) em r/Android, r/samsung,
 * r/whatsapp, r/techsupport e r/Xiaomi procurando pessoas reais relatando
 * dor de armazenamento cheio / WhatsApp lotado / duplicatas / "Outros" ocupando espaço.
 *
 * Salva oportunidades idempotentes em public.sales_leads com resposta útil (zero spam)
 * e link exato do guia pSEO correspondente.
 */

import { fileURLToPath } from "node:url";
import { escapeSqlString, runSupabaseSql } from "./lib/cognitive-memory.mjs";

const SITE_BASE = "https://midiastory-df6a0.web.app";
const SUBREDDITS = ["Android", "samsung", "whatsapp", "techsupport", "Xiaomi"];
const PAIN_REGEX =
  /(storage\s+full|out\s+of\s+storage|no\s+space|running\s+out\s+of\s+space|low\s+storage|whatsapp\s+storage|other\s+storage|system\s+storage|duplicate\s+photos|armazenamento\s+cheio|sem\s+espa[çc]o|mem[oó]ria\s+cheia|almacenamiento\s+lleno|sin\s+espacio)/i;

function detectPainAndGuide(title, text, sub) {
  const hay = `${title} ${text} ${sub}`.toLowerCase();
  const isPt = /(sem espa[çc]o|mem[oó]ria cheia|armazenamento cheio|celular)/i.test(hay);
  const isEs = /(almacenamiento lleno|sin espacio|memoria llena|celular)/i.test(hay);
  const locale = isPt ? "pt-BR" : isEs ? "es-419" : "en-US";
  const langSlug = isPt ? "pt-br" : isEs ? "es-419" : "en-us";

  let detectedPain = "android_storage_full";
  let slug = "android-storage-full-fix";

  if (/whatsapp/.test(hay)) {
    detectedPain = "whatsapp_storage_full";
    slug = "whatsapp-storage-full";
  } else if (/duplicate|duplicad/.test(hay)) {
    detectedPain = "duplicate_photos";
    slug = "remove-duplicate-photos-android";
  } else if (/samsung|galaxy/.test(hay)) {
    detectedPain = "samsung_storage_full";
    slug = "samsung-galaxy-storage-full";
  } else if (/xiaomi|redmi|poco/.test(hay)) {
    detectedPain = "xiaomi_other_storage";
    slug = "xiaomi-redmi-storage-full";
  } else if (/motorola|moto\s*g/.test(hay)) {
    detectedPain = "motorola_storage_full";
    slug = "motorola-storage-full-fix";
  }

  const guideUrl = `${SITE_BASE}/guides/${langSlug}/${slug}/?utm_source=reddit_radar&utm_medium=organic`;

  let draftReply = "";
  if (locale === "pt-BR") {
    draftReply = `Antes de apagar suas fotos importantes ou formatar o aparelho, vale checar duas coisas que mais travam a memória: vídeos grandes esquecidos/encaminhados no WhatsApp e cópias duplicadas exatas. Montei um passo a passo seguro (100% local, sem apagar nada automaticamente e usando verificação SHA-256) aqui: ${guideUrl}`;
  } else if (locale === "es-419") {
    draftReply = `Antes de formatear o borrar fotos importantes, revisa los videos grandes olvidados y copias duplicadas exactas (especialmente de WhatsApp). Aquí tienes una guía paso a paso segura (100% local, con verificación SHA-256 y sin borrar nada automáticamente): ${guideUrl}`;
  } else {
    draftReply = `Before uninstalling apps or factory resetting, check two silent culprits: oversized forwarded videos (especially WhatsApp) and byte-exact duplicate media. Also note that moving files to Android Trash doesn't free bytes until permanently emptied. Here's a quick, safe step-by-step guide (100% on-device, SHA-256 verification, zero auto-delete): ${guideUrl}`;
  }

  return { locale, detectedPain, guideUrl, draftReply };
}

async function fetchRedditRss(subreddit) {
  const url = `https://www.reddit.com/r/${subreddit}/new/.rss?limit=25`;
  try {
    const res = await fetch(url, {
      headers: {
        "User-Agent": "Mozilla/5.0 (compatible; MidiastoreDemandRadar/1.0; +https://midiastory-df6a0.web.app)",
        Accept: "application/atom+xml, application/xml, text/xml",
      },
      signal: AbortSignal.timeout(10000),
    });
    if (!res.ok) return [];
    const xml = await res.text();
    const entries = [...xml.matchAll(/<entry>([\s\S]*?)<\/entry>/g)].map((m) => m[1]);
    return entries.map((e) => {
      const title = (e.match(/<title>([\s\S]*?)<\/title>/)?.[1] || "").replace(/&amp;/g, "&").replace(/&quot;/g, '"');
      const link = e.match(/<link[^>]+href="([^"]+)"/)?.[1] || "";
      const updated = e.match(/<updated>([^<]+)<\/updated>/)?.[1] || "";
      const content = (e.match(/<content[^>]*>([\s\S]*?)<\/content>/)?.[1] || "").replace(/<[^>]+>/g, " ").slice(0, 600);
      return {
        source: `reddit:r/${subreddit}`,
        title: title.trim(),
        selftext: content,
        url: link,
        createdMs: updated ? Date.parse(updated) : Date.now(),
      };
    });
  } catch {
    return [];
  }
}

async function fetchAndroidStackExchange() {
  const queries = ["storage full", "whatsapp storage", "duplicate photos"];
  const results = [];
  for (const q of queries) {
    try {
      const url = `https://api.stackexchange.com/2.3/search/advanced?order=desc&sort=creation&site=android&pagesize=10&q=${encodeURIComponent(q)}`;
      const res = await fetch(url, { signal: AbortSignal.timeout(10000) });
      if (!res.ok) continue;
      const data = await res.json();
      for (const item of data?.items || []) {
        results.push({
          source: "stackexchange:android",
          title: String(item.title || "").replace(/&#39;/g, "'").replace(/&quot;/g, '"').replace(/&amp;/g, "&"),
          selftext: (item.tags || []).join(" "),
          url: item.link || "",
          createdMs: item.creation_date ? item.creation_date * 1000 : Date.now(),
        });
      }
    } catch {}
  }
  return results;
}

export async function runDemandHunterRadar() {
  const leads = [];
  const seenUrls = new Set();

  const rawItems = [];
  for (const sub of SUBREDDITS) {
    rawItems.push(...(await fetchRedditRss(sub)));
  }
  rawItems.push(...(await fetchAndroidStackExchange()));

  for (const p of rawItems) {
    const title = String(p.title || "").trim();
    const selftext = String(p.selftext || "");
    const permalink = String(p.url || "").trim();
    if (!permalink || seenUrls.has(permalink)) continue;

    // Match strict pain regex on Reddit, or storage/whatsapp/duplicate on Android StackExchange
    const isMatch =
      p.source === "stackexchange:android"
        ? /(storage|space|whatsapp|duplicate|memory|full)/i.test(`${title} ${selftext}`)
        : PAIN_REGEX.test(`${title} ${selftext}`);
    if (!isMatch) continue;
    seenUrls.add(permalink);

    const createdMs = Number.isFinite(p.createdMs) ? p.createdMs : Date.now();
    const createdIso = new Date(createdMs).toISOString();
    const ageHours = (Date.now() - createdMs) / 3600000;
    const info = detectPainAndGuide(title, selftext, p.source);

    leads.push({
      source: p.source,
      postUrl: permalink,
      title,
      detectedPain: info.detectedPain,
      locale: info.locale,
      suggestedGuideUrl: info.guideUrl,
      draftReply: info.draftReply,
      postCreatedAt: createdIso,
      ageHours: Number(ageHours.toFixed(1)),
    });
  }

  // Sort freshest first (prioritize < 24h)
  leads.sort((a, b) => a.ageHours - b.ageHours);

  let persisted = 0;
  for (const lead of leads.slice(0, 25)) {
    await runSupabaseSql(`
      INSERT INTO public.sales_leads (
        source, post_url, title, detected_pain, locale, suggested_guide_url, draft_reply, post_created_at
      )
      VALUES (
        '${escapeSqlString(lead.source)}',
        '${escapeSqlString(lead.postUrl)}',
        '${escapeSqlString(lead.title)}',
        '${escapeSqlString(lead.detectedPain)}',
        '${escapeSqlString(lead.locale)}',
        '${escapeSqlString(lead.suggestedGuideUrl)}',
        '${escapeSqlString(lead.draftReply)}',
        ${lead.postCreatedAt ? `'${lead.postCreatedAt}'::timestamptz` : "NULL"}
      )
      ON CONFLICT (post_url) DO NOTHING;
    `);
    persisted += 1;
  }

  const totalDbLeads = await runSupabaseSql(`SELECT COUNT(*)::int AS total FROM public.sales_leads;`);

  return {
    scannedAt: new Date().toISOString(),
    subredditsScanned: SUBREDDITS,
    opportunitiesFoundNow: leads.length,
    upsertedNow: persisted,
    totalLeadsInSupabase: totalDbLeads?.[0]?.total || 0,
    top5LiveLeads: leads.slice(0, 5),
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  runDemandHunterRadar()
    .then((report) => {
      console.log("🎯 RADAR DE DOR EM TEMPO REAL (AO VIVO):");
      console.log(JSON.stringify(report, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro no demand-hunter-radar:", err.message);
      process.exit(1);
    });
}
