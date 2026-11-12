import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

async function getAccessToken(scopes) {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: scopes.join(" "),
      aud: key.token_uri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");

  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const signature = sign.sign(key.private_key, "base64url");
  const jwt = `${header}.${payload}.${signature}`;

  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!data.access_token) throw new Error(JSON.stringify(data));
  return data.access_token;
}

async function main() {
  console.log("Testando permissoes de:", key.client_email);

  // 1. Search Console API
  try {
    const scToken = await getAccessToken(["https://www.googleapis.com/auth/webmasters"]);
    const scRes = await fetch("https://www.googleapis.com/webmasters/v3/sites", {
      headers: { Authorization: `Bearer ${scToken}` },
    });
    console.log("Search Console API:", scRes.status, await scRes.text());
  } catch (e) {
    console.log("Search Console API erro:", e.message);
  }

  // 2. Play Developer Reporting API (anomalies & crashRateMetricSet)
  try {
    const repToken = await getAccessToken(["https://www.googleapis.com/auth/playdeveloperreporting"]);
    const anomRes = await fetch(
      "https://playdeveloperreporting.googleapis.com/v1beta1/apps/com.rdf112.midiastore/anomalies",
      { headers: { Authorization: `Bearer ${repToken}` } }
    );
    console.log("Play Reporting API (anomalies):", anomRes.status, await anomRes.text());

    const repRes = await fetch(
      "https://playdeveloperreporting.googleapis.com/v1beta1/apps/com.rdf112.midiastore/crashRateMetricSet",
      { headers: { Authorization: `Bearer ${repToken}` } }
    );
    console.log("Play Reporting API (crashRateMetricSet):", repRes.status, await repRes.text());
  } catch (e) {
    console.log("Play Developer Reporting API erro:", e.message);
  }

  // 3. OpenRouter Multi-LLM API
  try {
    const aiKeys = JSON.parse(fs.readFileSync(path.join(projectRoot, ".secrets", "ai-media-keys.json"), "utf8"));
    const orRes = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${aiKeys.OPENROUTER_API_KEY}`,
        "HTTP-Referer": "https://srodolfobarbosa-afk.github.io/midiastory-/",
        "X-Title": "Midiastore Agent Hub",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "openai/gpt-4o-mini",
        max_tokens: 30,
        messages: [{ role: "user", content: "Responda em 5 palavras: Midiastore OpenRouter ativo!" }],
      }),
    });
    console.log("OpenRouter API:", orRes.status, await orRes.text());
  } catch (e) {
    console.log("OpenRouter erro:", e.message);
  }

  // 4. Meta / Facebook / Instagram Graph API
  const igToken = process.env.IG_TOKEN || "";
  if (igToken) {
    try {
      const meRes = await fetch(`https://graph.facebook.com/v21.0/me?fields=id,name&access_token=${igToken}`);
      console.log("Meta /me:", meRes.status, await meRes.text());

      const pagesRes = await fetch(
        `https://graph.facebook.com/v21.0/me/accounts?fields=id,name,instagram_business_account{id,username,followers_count,media_count}&access_token=${igToken}`
      );
      console.log("Meta /me/accounts (Pages + Instagram):", pagesRes.status, await pagesRes.text());

      const permsRes = await fetch(`https://graph.facebook.com/v21.0/me/permissions?access_token=${igToken}`);
      console.log("Meta /me/permissions:", permsRes.status, await permsRes.text());
    } catch (e) {
      console.log("Meta Graph API erro:", e.message);
    }
  }
}

main();
