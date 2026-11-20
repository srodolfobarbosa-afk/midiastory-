#!/usr/bin/env node
/**
 * INDEXAÇÃO INSTANTÂNEA VIA INDEXNOW (Bing, DuckDuckGo, Yandex, Naver, Seznam & ChatGPT Search)
 *
 * Lê todas as URLs de docs/sitemap.xml e envia em lote para https://api.indexnow.org/indexnow
 * usando a chave verificada em https://midiastory-df6a0.web.app/a9f4c2e8b1d740369e5a8c2f1b4d7e90.txt.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const HOST = "midiastory-df6a0.web.app";
const KEY = "a9f4c2e8b1d740369e5a8c2f1b4d7e90";
const KEY_LOCATION = `https://${HOST}/${KEY}.txt`;

export async function pingIndexNow() {
  // 1. Verify key file is live on Firebase Hosting first
  const keyCheck = await fetch(KEY_LOCATION, { signal: AbortSignal.timeout(10000) });
  const keyText = (await keyCheck.text()).trim();
  if (!keyCheck.ok || keyText !== KEY) {
    throw new Error(`Chave IndexNow não encontrada ou inválida em ${KEY_LOCATION} (HTTP ${keyCheck.status})`);
  }

  // 2. Parse all URLs from docs/sitemap.xml
  const sitemapPath = path.join(projectRoot, "docs", "sitemap.xml");
  const xml = fs.readFileSync(sitemapPath, "utf8");
  const urlList = [...xml.matchAll(/<loc>(https:\/\/[^<]+)<\/loc>/g)].map((m) => m[1]);

  if (urlList.length === 0) {
    throw new Error("Nenhuma URL encontrada em docs/sitemap.xml");
  }

  // 3. Submit batch to IndexNow API
  const payload = {
    host: HOST,
    key: KEY,
    keyLocation: KEY_LOCATION,
    urlList,
  };

  const res = await fetch("https://api.indexnow.org/indexnow", {
    method: "POST",
    headers: { "Content-Type": "application/json; charset=utf-8" },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(15000),
  });

  const bodyText = await res.text();
  return {
    submittedAt: new Date().toISOString(),
    status: res.status,
    ok: res.status === 200 || res.status === 202,
    urlsSubmitted: urlList.length,
    keyLocation: KEY_LOCATION,
    responseBody: bodyText.slice(0, 200),
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  pingIndexNow()
    .then((res) => {
      console.log("⚡ INDEXNOW SUBMIT (BING / DUCKDUCKGO / YANDEX / CHATGPT SEARCH):");
      console.log(JSON.stringify(res, null, 2));
      if (!res.ok) process.exit(1);
    })
    .catch((err) => {
      console.error("❌ Erro no IndexNow:", err.message);
      process.exit(1);
    });
}
