#!/usr/bin/env node
/**
 * Uploads localized 1080x1920 phoneScreenshots from docs/campaigns/store-screenshots/<lang>/
 * to Google Play Console for all 8 store locales without modifying text listings or tracks.
 */

import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const packageName = "com.rdf112.midiastore";
const locales = ["pt-BR", "en-US", "es-ES", "fr-FR", "de-DE", "hi-IN", "ja-JP", "zh-CN"];

function b64u(input) {
  return Buffer.from(input).toString("base64url");
}

async function getAccessToken() {
  const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));
  const now = Math.floor(Date.now() / 1000);
  const header = b64u(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = b64u(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: "https://oauth2.googleapis.com/token",
      exp: now + 3600,
      iat: now,
    })
  );
  const sig = crypto.createSign("RSA-SHA256").update(`${header}.${payload}`).sign(key.private_key, "base64url");
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${header}.${payload}.${sig}`,
  });
  const data = await res.json();
  if (!res.ok || !data.access_token) {
    throw new Error(`OAuth failed (${res.status}): ${JSON.stringify(data)}`);
  }
  return data.access_token;
}

async function main() {
  const token = await getAccessToken();
  const editRes = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: "{}",
  });
  const edit = await editRes.json();
  if (!editRes.ok || !edit.id) {
    throw new Error(`Create edit failed (${editRes.status}): ${JSON.stringify(edit)}`);
  }
  console.log(`Edit criado: ${edit.id}`);

  for (const lang of locales) {
    const dir = path.join(projectRoot, "docs", "campaigns", "store-screenshots", lang);
    if (!fs.existsSync(dir)) {
      console.warn(`Pasta ausente para ${lang}: ${dir}`);
      continue;
    }
    const pngs = fs.readdirSync(dir).filter((f) => f.toLowerCase().endsWith(".png")).sort();
    if (pngs.length === 0) {
      console.warn(`Nenhum PNG encontrado para ${lang}`);
      continue;
    }

    await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/phoneScreenshots`,
      { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
    );

    let uploaded = 0;
    for (const file of pngs) {
      const scRes = await fetch(
        `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/phoneScreenshots?uploadType=media`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}`, "Content-Type": "image/png" },
          body: fs.readFileSync(path.join(dir, file)),
        }
      );
      if (scRes.ok) {
        uploaded += 1;
      } else {
        console.warn(`Falha upload ${lang}/${file}: HTTP ${scRes.status} ${await scRes.text()}`);
      }
    }
    console.log(`${lang}: ${uploaded}/${pngs.length} phoneScreenshots enviados`);
  }

  const commitRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  const commitText = await commitRes.text();
  console.log(`Commit Play Console: HTTP ${commitRes.status} ${commitText}`);
  if (!commitRes.ok) {
    throw new Error(`Commit failed: ${commitText}`);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
