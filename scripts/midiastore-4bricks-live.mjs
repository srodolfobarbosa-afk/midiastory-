#!/usr/bin/env node
/**
 * Midiastore — Raio-X Ao Vivo dos 4 Tijolos (usando as 3 novas bibliotecas de produção)
 *
 * 1. google-auth-library -> Google Play Developer API v3 (Tracks reais: internal, alpha, production)
 * 2. @supabase/supabase-js -> Cliente oficial Supabase (latência real sem HTTP 429 da Management API)
 * 3. cheerio -> Auditoria SEO/CTA/JSON-LD/hreflang ao vivo do funil https://midiastory-df6a0.web.app/
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { GoogleAuth } from "google-auth-library";
import { createClient } from "@supabase/supabase-js";
import * as cheerio from "cheerio";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const PACKAGE_NAME = "com.rdf112.midiastore";

async function checkPlayTracks() {
  const keyFile = path.join(PROJECT_ROOT, ".secrets", "play-service-account.json");
  if (!fs.existsSync(keyFile)) return { ok: false, error: "play-service-account.json ausente" };

  const auth = new GoogleAuth({
    keyFile,
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  const client = await auth.getClient();
  const { token } = await client.getAccessToken();

  const base = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}`;
  const editRes = await fetch(`${base}/edits`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({}),
  });
  if (!editRes.ok) {
    return { ok: false, error: `edits.insert HTTP ${editRes.status}` };
  }
  const { id: editId } = await editRes.json();

  try {
    const tracksRes = await fetch(`${base}/edits/${editId}/tracks`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const tracksData = await tracksRes.json();
    const summary = (tracksData.tracks || []).map((t) => ({
      track: t.track,
      releases: (t.releases || []).map((r) => ({
        name: r.name,
        status: r.status,
        versionCodes: r.versionCodes || [],
      })),
    }));
    return { ok: true, tracks: summary };
  } finally {
    await fetch(`${base}/edits/${editId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {});
  }
}

async function checkSupabaseSdk() {
  const cfgPath = path.join(PROJECT_ROOT, ".secrets", "supabase-anon.json");
  if (!fs.existsSync(cfgPath)) return { ok: false, error: "supabase-anon.json ausente" };
  const raw = JSON.parse(fs.readFileSync(cfgPath, "utf8"));
  const url = "https://usmqtnlsaavxqguwrtix.supabase.co";
  let key = "";
  if (Array.isArray(raw)) {
    const svc = raw.find((k) => k.name === "service_role" && k.api_key && !k.api_key.includes("·"));
    const anon = raw.find((k) => k.name === "anon" && k.api_key);
    key = svc?.api_key || anon?.api_key || "";
  } else {
    key = raw.serviceRoleKey || raw.anonKey || raw.SUPABASE_ANON_KEY || "";
  }
  if (!key) return { ok: false, error: "chave não encontrada em supabase-anon.json" };

  const t0 = Date.now();
  const sb = createClient(url, key, { auth: { persistSession: false } });
  const [testers, purchases] = await Promise.all([
    sb.from("beta_testers").select("id", { count: "exact", head: true }),
    sb.from("play_purchases").select("id", { count: "exact", head: true }),
  ]);
  const ms = Date.now() - t0;
  return {
    ok: !testers.error && !purchases.error,
    ms,
    status: `beta_testers=${testers.count ?? "?"} | play_purchases=${purchases.count ?? "?"} (zero 429)`,
  };
}

async function checkWebFunnelSeo() {
  const url = "https://midiastory-df6a0.web.app/";
  const t0 = Date.now();
  const res = await fetch(url, { signal: AbortSignal.timeout(12000) });
  const ms = Date.now() - t0;
  const html = await res.text();
  const $ = cheerio.load(html);

  const title = $("title").text().trim();
  const desc = $('meta[name="description"]').attr("content") || "";
  const hreflangs = $('link[rel="alternate"][hreflang]').length;
  const jsonLd = $('script[type="application/ld+json"]').length;
  const playLinks = $('a[href*="play.google.com"]').length;
  const h1 = $("h1").first().text().trim();

  return { ok: res.ok, status: res.status, ms, title, descLen: desc.length, hreflangs, jsonLd, playLinks, h1 };
}

async function main() {
  const [play, sb, web] = await Promise.all([checkPlayTracks(), checkSupabaseSdk(), checkWebFunnelSeo()]);

  console.log("\n🧱 RAIO-X AO VIVO DOS 4 TIJOLOS DO MIDIASTORE (3 NOVAS BIBLIOTECAS):\n");

  console.log("1️⃣ & 2️⃣ GOOGLE PLAY CONSOLE API v3 (via google-auth-library):");
  if (!play.ok) {
    console.log("  ❌", play.error);
  } else {
    for (const t of play.tracks) {
      const relStr = t.releases
        .map((r) => `${r.name || "?"} [${r.status}] vc=${r.versionCodes.join(",")}`)
        .join(" | ");
      console.log(`  • Track [${t.track}]: ${relStr || "(vazio)"}`);
    }
  }

  console.log("\n3️⃣ SUPABASE SDK OFICIAL (via @supabase/supabase-js):");
  console.log(`  • Conexão direta: ${sb.ok ? "OK ✅" : "ERRO ❌"} | Latência: ${sb.ms ?? "?"}ms | ${sb.status || sb.error}`);

  console.log("\n4️⃣ FUNIL WEB & SEO (via cheerio em https://midiastory-df6a0.web.app/):");
  console.log(`  • HTTP ${web.status} (${web.ms}ms) | Title: "${web.title}"`);
  console.log(`  • H1: "${web.h1}" | Meta Desc: ${web.descLen} chars`);
  console.log(`  • Idiomas (hreflang): ${web.hreflangs} | Schema JSON-LD: ${web.jsonLd} | CTAs Play Store: ${web.playLinks}`);
}

main().catch((e) => {
  console.error("4BRICKS_ERROR:", e.message);
  process.exit(1);
});
