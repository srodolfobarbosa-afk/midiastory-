#!/usr/bin/env node
/**
 * SEO SEARCH CONSOLE MONITOR (14 Idiomas — 100% Node.js, Zero Python)
 *
 * Consulta a Google Search Console API (`searchconsole.googleapis.com`)
 * usando `.secrets/play-service-account.json` para acompanhar:
 * - Propriedades acessiveis pela conta de servico
 * - Sitemaps submetidos
 * - Impressoes, cliques, CTR e posicao media (ultimos 28 dias) agregados e por idioma (14 idiomas)
 *
 * Salva snapshot em: docs/aso/seo-search-console-latest.json
 */

import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const outPath = path.join(projectRoot, "docs", "aso", "seo-search-console-latest.json");

const SITE_URL = "https://midiastory-df6a0.web.app/";
const API_BASE = "https://searchconsole.googleapis.com/webmasters/v3";

// 14 idiomas do ecossistema Midiastore
const LANGUAGES_14 = [
  "pt", "en", "es", "de", "fr", "hi", "ja",
  "zh", "ar", "ru", "id", "bn", "ko", "it",
];

function detectLanguageFromUrl(pageUrl) {
  try {
    const u = new URL(pageUrl);
    const seg = u.pathname.replace(/^\/+/, "").split("/")[0].toLowerCase();
    if (LANGUAGES_14.includes(seg) && seg !== "pt") return seg;
    return "pt";
  } catch {
    return "pt";
  }
}

async function getAccessToken(key) {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/webmasters.readonly",
      aud: key.token_uri || "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");

  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const signature = sign.sign(key.private_key, "base64url");
  const jwt = `${header}.${payload}.${signature}`;

  const res = await fetch(key.token_uri || "https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
    signal: AbortSignal.timeout(15000),
  });
  const data = await res.json();
  if (!res.ok || !data.access_token) {
    throw new Error(`OAuth token falhou (HTTP ${res.status}): ${JSON.stringify(data)}`);
  }
  return data.access_token;
}

async function fetchJson(url, token, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    },
    signal: AbortSignal.timeout(20000),
  });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = { raw: text.slice(0, 500) };
  }
  return { status: res.status, ok: res.ok, data: json };
}

function initLanguageBuckets() {
  const buckets = {};
  for (const lang of LANGUAGES_14) {
    buckets[lang] = {
      clicks: 0,
      impressions: 0,
      ctr: 0,
      avgPosition: null,
      _weightedPos: 0,
      pagesWithTraffic: 0,
    };
  }
  return buckets;
}

async function main() {
  if (!fs.existsSync(keyPath)) {
    throw new Error(`Service account ausente em ${keyPath}`);
  }

  const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));
  const checkedAt = new Date().toISOString();
  const token = await getAccessToken(key);

  const sitesResp = await fetchJson(`${API_BASE}/sites`, token);
  const siteEntries = sitesResp.ok && Array.isArray(sitesResp.data?.siteEntry)
    ? sitesResp.data.siteEntry
    : [];

  const targetSite =
    siteEntries.find((s) => s.siteUrl === SITE_URL)?.siteUrl ||
    siteEntries.find((s) => s.siteUrl?.includes("midiastory-df6a0.web.app"))?.siteUrl ||
    null;

  const endDate = new Date().toISOString().slice(0, 10);
  const startDate = new Date(Date.now() - 28 * 86400 * 1000).toISOString().slice(0, 10);

  const snapshot = {
    checkedAt,
    apiEndpoint: API_BASE,
    targetSiteUrl: SITE_URL,
    serviceAccountEmail: key.client_email,
    sitesApiHttpStatus: sitesResp.status,
    sitesCount: siteEntries.length,
    sites: siteEntries,
    period: { startDate, endDate, days: 28 },
    summary: null,
    byLanguage14: initLanguageBuckets(),
    topQueries: [],
    topPages: [],
    sitemaps: null,
    status: "UNKNOWN",
    actionNeeded: null,
  };

  if (!sitesResp.ok) {
    snapshot.status = "API_ERROR";
    snapshot.apiError = sitesResp.data;
    snapshot.actionNeeded = `Verifique se a Google Search Console API (searchconsole.googleapis.com) esta habilitada no projeto GCP da conta ${key.client_email}.`;
  } else if (!targetSite) {
    // Also probe direct query to confirm exact 403/permission message
    const probe = await fetchJson(
      `${API_BASE}/sites/${encodeURIComponent(SITE_URL)}/searchAnalytics/query`,
      token,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ startDate, endDate, dimensions: ["page"], rowLimit: 5 }),
      }
    );
    snapshot.directProbeHttpStatus = probe.status;
    snapshot.directProbeError = probe.ok ? null : probe.data?.error?.message || probe.data;
    snapshot.status = "PENDING_GSC_PERMISSION";
    snapshot.actionNeeded = `Adicione ${key.client_email} como usuario ou proprietario na propriedade ${SITE_URL} no Google Search Console (Configuracoes -> Usuarios e permissoes) e submeta ${SITE_URL}sitemap.xml.`;
  } else {
    snapshot.resolvedSiteUrl = targetSite;

    // 1. Page-level breakdown for 14 languages
    const pageResp = await fetchJson(
      `${API_BASE}/sites/${encodeURIComponent(targetSite)}/searchAnalytics/query`,
      token,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          startDate,
          endDate,
          dimensions: ["page"],
          rowLimit: 500,
        }),
      }
    );

    // 2. Top queries
    const queryResp = await fetchJson(
      `${API_BASE}/sites/${encodeURIComponent(targetSite)}/searchAnalytics/query`,
      token,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          startDate,
          endDate,
          dimensions: ["query", "country"],
          rowLimit: 100,
        }),
      }
    );

    // 3. Sitemaps
    const sitemapsResp = await fetchJson(
      `${API_BASE}/sites/${encodeURIComponent(targetSite)}/sitemaps`,
      token
    );

    if (sitemapsResp.ok) {
      snapshot.sitemaps = sitemapsResp.data?.sitemap || [];
    } else {
      snapshot.sitemapsError = { status: sitemapsResp.status, data: sitemapsResp.data };
    }

    if (pageResp.ok) {
      const rows = pageResp.data?.rows || [];
      let totalClicks = 0;
      let totalImpressions = 0;
      let totalWeightedPos = 0;

      for (const row of rows) {
        const page = row.keys?.[0] || "";
        const lang = detectLanguageFromUrl(page);
        const clicks = row.clicks || 0;
        const impressions = row.impressions || 0;
        const position = row.position || 0;

        totalClicks += clicks;
        totalImpressions += impressions;
        totalWeightedPos += position * impressions;

        const b = snapshot.byLanguage14[lang];
        if (b) {
          b.clicks += clicks;
          b.impressions += impressions;
          b._weightedPos += position * impressions;
          b.pagesWithTraffic += 1;
        }
      }

      for (const lang of LANGUAGES_14) {
        const b = snapshot.byLanguage14[lang];
        b.ctr = b.impressions > 0 ? Number((b.clicks / b.impressions).toFixed(4)) : 0;
        b.avgPosition = b.impressions > 0 ? Number((b._weightedPos / b.impressions).toFixed(2)) : null;
        delete b._weightedPos;
      }

      snapshot.summary = {
        clicks: totalClicks,
        impressions: totalImpressions,
        ctr: totalImpressions > 0 ? Number((totalClicks / totalImpressions).toFixed(4)) : 0,
        avgPosition: totalImpressions > 0 ? Number((totalWeightedPos / totalImpressions).toFixed(2)) : null,
        indexedPagesWithTraffic: rows.length,
      };

      snapshot.topPages = rows.slice(0, 30).map((r) => ({
        page: r.keys?.[0],
        lang: detectLanguageFromUrl(r.keys?.[0] || ""),
        clicks: r.clicks,
        impressions: r.impressions,
        ctr: Number((r.ctr || 0).toFixed(4)),
        position: Number((r.position || 0).toFixed(2)),
      }));
      snapshot.status = "OK";
    } else {
      snapshot.status = "QUERY_ERROR";
      snapshot.pageQueryError = { status: pageResp.status, data: pageResp.data };
    }

    if (queryResp.ok) {
      snapshot.topQueries = (queryResp.data?.rows || []).slice(0, 50).map((r) => ({
        query: r.keys?.[0],
        country: r.keys?.[1],
        clicks: r.clicks,
        impressions: r.impressions,
        ctr: Number((r.ctr || 0).toFixed(4)),
        position: Number((r.position || 0).toFixed(2)),
      }));
    }
  }

  // Clean internal helper fields if still present (when !targetSite)
  for (const lang of LANGUAGES_14) {
    if (snapshot.byLanguage14[lang] && "_weightedPos" in snapshot.byLanguage14[lang]) {
      delete snapshot.byLanguage14[lang]._weightedPos;
    }
  }

  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, JSON.stringify(snapshot, null, 2), "utf8");
  console.log(JSON.stringify(snapshot, null, 2));
}

main().catch((err) => {
  console.error("SEO_SEARCH_CONSOLE_ERROR:", err.message);
  process.exit(1);
});
