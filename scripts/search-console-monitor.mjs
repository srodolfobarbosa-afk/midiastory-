#!/usr/bin/env node
/**
 * MONITOR GOOGLE SEARCH CONSOLE (14 Idiomas — 100% Node.js, Zero Python)
 *
 * Usa a conta de serviço do projeto `midiastory-df6a0` (.secrets/play-service-account.json)
 * com a API `searchconsole.googleapis.com` para puxar ao vivo:
 * - Propriedades verificadas
 * - Top queries, cliques, impressões, CTR e posição média dos últimos 28 dias
 * - Sitemaps submetidos
 *
 * Salva snapshot em: docs/aso/search-console-latest.json
 */

import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const SITE_URL = "https://midiastory-df6a0.web.app/";

async function getAccessToken(key) {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/webmasters.readonly",
      aud: key.token_uri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");
  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const jwt = `${header}.${payload}.${sign.sign(key.private_key, "base64url")}`;

  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
    signal: AbortSignal.timeout(15000),
  });
  const data = await res.json();
  if (!data.access_token) throw new Error(`OAuth Search Console falhou: ${JSON.stringify(data)}`);
  return data.access_token;
}

async function main() {
  const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));
  const token = await getAccessToken(key);
  const checkedAt = new Date().toISOString();

  const sitesRes = await fetch("https://www.googleapis.com/webmasters/v3/sites", {
    headers: { Authorization: `Bearer ${token}` },
    signal: AbortSignal.timeout(15000),
  });
  const sitesData = await sitesRes.json();
  const siteEntries = sitesData.siteEntry || [];

  const result = {
    checkedAt,
    serviceAccountEmail: key.client_email,
    sitesCount: siteEntries.length,
    sites: siteEntries,
    analytics28d: null,
    actionNeeded: null,
  };

  const targetSite = siteEntries.find((s) => s.siteUrl.includes("midiastory-df6a0.web.app"))?.siteUrl;
  if (!targetSite) {
    result.actionNeeded = `Adicione ${key.client_email} como usuário (Acesso restrito/completo) na propriedade ${SITE_URL} no Google Search Console e envie o sitemap ${SITE_URL}sitemap.xml.`;
  } else {
    const endDate = new Date().toISOString().slice(0, 10);
    const startDate = new Date(Date.now() - 28 * 86400 * 1000).toISOString().slice(0, 10);
    const qRes = await fetch(
      `https://www.googleapis.com/webmasters/v3/sites/${encodeURIComponent(targetSite)}/searchAnalytics/query`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          startDate,
          endDate,
          dimensions: ["query", "page", "country"],
          rowLimit: 50,
        }),
        signal: AbortSignal.timeout(15000),
      }
    );
    if (qRes.ok) {
      result.analytics28d = await qRes.json();
    } else {
      result.analyticsError = `HTTP ${qRes.status}: ${await qRes.text()}`;
    }
  }

  const outDir = path.join(projectRoot, "docs", "aso");
  fs.mkdirSync(outDir, { recursive: true });
  const outPath = path.join(outDir, "search-console-latest.json");
  fs.writeFileSync(outPath, JSON.stringify(result, null, 2), "utf8");
  console.log(JSON.stringify(result, null, 2));
}

main().catch((err) => {
  console.error("SEARCH_CONSOLE_ERR:", err.message);
  process.exit(1);
});
