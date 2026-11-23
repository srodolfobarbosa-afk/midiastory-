#!/usr/bin/env node
/**
 * GROWTH LEARNING LOOP — Motor de Aprendizado de Conversão Orgânica
 * (@aconteceunobr -> Site midiastory-df6a0.web.app -> Google Play -> Assinaturas Pro)
 * 100% Node.js (Zero Python)
 *
 * Cruza ao vivo:
 *  1. Instagram Graph API v27.0 (@aconteceunobr): últimos 20 posts/Reels, engajamento por horário (BRT) e melhores ganchos
 *  2. Supabase `public.product_events`: page_view, play_store_click e origens UTM (instagram/bio/story/reels/shorts/tiktok)
 *  3. Supabase `public.play_purchases`: vendas reais verificadas na Edge Function verify-play-purchase
 *
 * Registra automaticamente o resumo estruturado em `public.aprendizado_ia` no Supabase
 * e salva o snapshot em `docs/aso/growth-learning-latest.json`.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";
import { registrarAprendizado } from "./registrar-aprendizado-ia.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const ENV_PATH = path.join(projectRoot, ".env");
const GRAPH_BASE = "https://graph.facebook.com/v27.0";

function loadDotEnv() {
  if (!fs.existsSync(ENV_PATH)) return;
  for (const line of fs.readFileSync(ENV_PATH, "utf8").split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const idx = t.indexOf("=");
    if (idx <= 0) continue;
    const k = t.slice(0, idx).trim();
    const v = t.slice(idx + 1).trim().replace(/^["']|["']$/g, "");
    if (!process.env[k]) process.env[k] = v;
  }
}

async function runSupabaseSql(query) {
  const token = getSupabaseToken();
  const res = await fetch(`https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
    signal: AbortSignal.timeout(15000),
  });
  if (!res.ok) throw new Error(`Supabase SQL HTTP ${res.status}: ${await res.text()}`);
  return res.json();
}

async function fetchInstagramMetrics() {
  const token = process.env.META_ACCESS_TOKEN?.trim();
  const igId = process.env.INSTAGRAM_USER_ID?.trim();
  if (!token || !igId) {
    return { ok: false, error: "META_ACCESS_TOKEN ou INSTAGRAM_USER_ID ausente no .env" };
  }

  const [profRes, mediaRes] = await Promise.all([
    fetch(`${GRAPH_BASE}/${igId}?fields=username,followers_count,media_count&access_token=${encodeURIComponent(token)}`, {
      signal: AbortSignal.timeout(15000),
    }),
    fetch(
      `${GRAPH_BASE}/${igId}/media?fields=id,caption,media_type,media_product_type,permalink,timestamp,like_count,comments_count&limit=20&access_token=${encodeURIComponent(token)}`,
      { signal: AbortSignal.timeout(15000) }
    ),
  ]);

  const profile = await profRes.json();
  const media = await mediaRes.json();
  const posts = (media.data || []).map((m) => {
    const dt = new Date(m.timestamp);
    // Converte para hora BRT (UTC-3)
    const brtHour = (dt.getUTCHours() + 21) % 24;
    const likes = m.like_count ?? 0;
    const comments = m.comments_count ?? 0;
    const score = likes + comments * 3;
    const hook = (m.caption || "").replace(/\s+/g, " ").trim().slice(0, 72);
    return {
      id: m.id,
      type: m.media_product_type || m.media_type,
      timestamp: m.timestamp,
      brtHour,
      likes,
      comments,
      engagementScore: score,
      hook,
      permalink: m.permalink,
    };
  });

  // Agrupa por faixa horária BRT
  const hourBuckets = {};
  for (const p of posts) {
    const bucket = `${String(p.brtHour).padStart(2, "0")}h BRT`;
    if (!hourBuckets[bucket]) hourBuckets[bucket] = { posts: 0, totalScore: 0 };
    hourBuckets[bucket].posts++;
    hourBuckets[bucket].totalScore += p.engagementScore;
  }

  const bestHours = Object.entries(hourBuckets)
    .map(([hour, d]) => ({ hour, posts: d.posts, avgScore: Number((d.totalScore / d.posts).toFixed(1)) }))
    .sort((a, b) => b.avgScore - a.avgScore)
    .slice(0, 3);

  const topHooks = [...posts].sort((a, b) => b.engagementScore - a.engagementScore).slice(0, 3);

  return {
    ok: true,
    username: profile.username || "aconteceunobr",
    followers: profile.followers_count ?? null,
    mediaCount: profile.media_count ?? null,
    postsAnalyzed: posts.length,
    bestHours,
    topHooks: topHooks.map((h) => ({
      id: h.id,
      type: h.type,
      brtHour: `${String(h.brtHour).padStart(2, "0")}h`,
      likes: h.likes,
      comments: h.comments,
      score: h.engagementScore,
      hook: h.hook,
    })),
  };
}

async function fetchWebEventsMetrics() {
  const byEvent = await runSupabaseSql(`
    SELECT event_name, COUNT(*)::int AS total
    FROM public.product_events
    WHERE created_at >= now() - INTERVAL '7 days'
    GROUP BY event_name
    ORDER BY total DESC;
  `);

  const bySource = await runSupabaseSql(`
    SELECT COALESCE(properties->>'utm_source', 'direct') AS utm_source,
           COALESCE(properties->>'utm_medium', 'none') AS utm_medium,
           COUNT(*)::int AS total
    FROM public.product_events
    WHERE created_at >= now() - INTERVAL '7 days'
    GROUP BY 1, 2
    ORDER BY total DESC
    LIMIT 10;
  `);

  const total7d = byEvent.reduce((acc, r) => acc + (r.total || 0), 0);
  const pageViews = byEvent.find((r) => r.event_name === "page_view")?.total || 0;
  const playClicks = byEvent.find((r) => r.event_name === "play_store_click")?.total || 0;
  const ctrPct = pageViews > 0 ? Number(((playClicks / pageViews) * 100).toFixed(1)) : 0;

  return { total7d, pageViews, playClicks, ctrPct, byEvent, bySource };
}

async function fetchPurchasesMetrics() {
  const rows = await runSupabaseSql(`
    SELECT COUNT(*)::int AS total_purchases,
           MAX(created_at) AS last_purchase_at
    FROM public.play_purchases;
  `);
  return rows[0] || { total_purchases: 0, last_purchase_at: null };
}

async function main() {
  loadDotEnv();
  const checkedAt = new Date().toISOString();

  const [instagram, webEvents, purchases] = await Promise.all([
    fetchInstagramMetrics(),
    fetchWebEventsMetrics(),
    fetchPurchasesMetrics(),
  ]);

  const report = { checkedAt, instagram, webEvents, purchases };

  const outDir = path.join(projectRoot, "docs", "aso");
  fs.mkdirSync(outDir, { recursive: true });
  const outPath = path.join(outDir, "growth-learning-latest.json");
  fs.writeFileSync(outPath, JSON.stringify(report, null, 2), "utf8");

  const topHourStr = instagram.ok && instagram.bestHours.length
    ? instagram.bestHours.map((h) => `${h.hour} (score ${h.avgScore})`).join(", ")
    : "N/D";
  const topHookStr = instagram.ok && instagram.topHooks.length
    ? instagram.topHooks[0].hook
    : "N/D";
  const topSourceStr = webEvents.bySource.length
    ? webEvents.bySource.map((s) => `${s.utm_source}/${s.utm_medium}=${s.total}`).join(", ")
    : "sem cliques UTM ainda";

  const saved = await registrarAprendizado({
    tipo: "CONVERSAO_ORGANICA",
    contexto: `Growth Learning Loop @${instagram.username || "aconteceunobr"} (${instagram.followers || "?"} seg) -> Web -> Play -> Pro`,
    diagnostico: `Melhores horarios BRT: ${topHourStr} | Top gancho: "${topHookStr}" | Site 7d: ${webEvents.pageViews} views, ${webEvents.playClicks} cliques Play (CTR ${webEvents.ctrPct}%), fontes: ${topSourceStr} | Vendas play_purchases: ${purchases.total_purchases}`,
    pesoAcao: 1.0,
    impactoMeta: "Converter 2M views/mes organicos em instalacoes e assinantes Pro rumo a R$ 1.000.000,00 (Regra 50/50)",
    acaoExecutada: "Execucao automatica scripts/growth-learning-loop.mjs cruzando Meta Graph API v27.0 + public.product_events + public.play_purchases",
    resultado: "ATIVO",
  });

  report.aprendizadoIaId = saved?.id ?? null;
  console.log(JSON.stringify(report, null, 2));
}

main().catch((err) => {
  console.error("GROWTH_LEARNING_ERR:", err.message);
  process.exit(1);
});
