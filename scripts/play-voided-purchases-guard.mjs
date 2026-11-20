#!/usr/bin/env node
/**
 * GUARDA ANTI-FRAUDE: GOOGLE PLAY VOIDED PURCHASES API (Módulo 2 + 4A)
 *
 * Consulta ao vivo a Google Play Developer API v3 (`purchases.voidedpurchases.list`, type=1:
 * inclui compras únicas + assinaturas estornadas/chargebacks), revoga automaticamente qualquer
 * entitlement comprometido em `public.entitlements` e `public.play_purchases`, e registra evento
 * append-only de REFUND/CHARGEBACK em `public.financial_ledger`.
 *
 * Uso:
 *   node scripts/play-voided-purchases-guard.mjs [--days 30]
 */

import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { escapeSqlString, runSupabaseSql } from "./lib/cognitive-memory.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const PACKAGE_NAME = "com.rdf112.midiastore";

function loadPlayServiceAccount() {
  if (process.env.PLAY_SERVICE_ACCOUNT_JSON) {
    return JSON.parse(process.env.PLAY_SERVICE_ACCOUNT_JSON);
  }
  const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
  if (fs.existsSync(keyPath)) {
    return JSON.parse(fs.readFileSync(keyPath, "utf8"));
  }
  throw new Error("Service Account da Play Store não encontrada (.secrets/play-service-account.json ou PLAY_SERVICE_ACCOUNT_JSON).");
}

async function getPlayAccessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  const tokenUri = sa.token_uri || "https://oauth2.googleapis.com/token";
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: tokenUri,
      iat: now,
      exp: now + 3600,
    }),
  ).toString("base64url");

  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const sig = sign.sign(sa.private_key, "base64url");
  const jwt = `${header}.${payload}.${sig}`;

  const res = await fetch(tokenUri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
    signal: AbortSignal.timeout(10000),
  });
  const data = await res.json();
  if (!res.ok || !data?.access_token) {
    throw new Error(`Falha OAuth Play API (HTTP ${res.status}): ${JSON.stringify(data)}`);
  }
  return data.access_token;
}

export async function runVoidedPurchasesGuard({ days = 28 } = {}) {
  const sa = loadPlayServiceAccount();
  const accessToken = await getPlayAccessToken(sa);

  const safeDays = Math.max(1, Math.min(29, Number(days) || 28));
  const startTimeMs = Date.now() - safeDays * 24 * 60 * 60 * 1000;
  const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
    PACKAGE_NAME,
  )}/purchases/voidedpurchases?type=1&startTime=${startTimeMs}`;

  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}` },
    signal: AbortSignal.timeout(12000),
  });
  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Voided Purchases API falhou (HTTP ${res.status}): ${errText}`);
  }

  const data = await res.json();
  const voidedList = Array.isArray(data?.voidedPurchases) ? data.voidedPurchases : [];

  let revokedCount = 0;
  for (const vp of voidedList) {
    const purchaseToken = String(vp.purchaseToken || "").trim();
    const orderId = String(vp.orderId || "").trim();
    if (!purchaseToken) continue;

    const tokenHash = crypto.createHash("sha256").update(purchaseToken).digest("hex");
    const voidedReason = vp.voidedReason ?? null;
    const voidedSource = vp.voidedSource ?? null;
    const eventType = voidedReason === 2 ? "CHARGEBACK" : "REFUND";

    await runSupabaseSql(`
      UPDATE public.entitlements
      SET is_active = false,
          status = 'VOIDED_${eventType}',
          updated_at = NOW()
      WHERE subject_key = '${escapeSqlString(tokenHash)}';

      UPDATE public.play_purchases
      SET entitlement_active = false,
          raw_status = 'VOIDED_${eventType}',
          verified_at = NOW()
      WHERE purchase_token_hash = '${escapeSqlString(tokenHash)}';

      INSERT INTO public.financial_ledger (
        idempotency_key, event_type, source, order_id, direction,
        amount_cents, currency, fx_rate_to_brl, amount_brl_cents,
        is_test_transaction, metadata
      )
      VALUES (
        'voided:${escapeSqlString(orderId || tokenHash)}',
        '${eventType}',
        'GOOGLE_PLAY',
        ${orderId ? `'${escapeSqlString(orderId)}'` : "NULL"},
        'DEBIT',
        0,
        'BRL',
        1.0,
        0,
        false,
        '${escapeSqlString(JSON.stringify({ voidedReason, voidedSource, voidedTimeMillis: vp.voidedTimeMillis || null }))}'::jsonb
      )
      ON CONFLICT (idempotency_key) DO NOTHING;
    `);
    revokedCount += 1;
  }

  return {
    checkedAt: new Date().toISOString(),
    packageName: PACKAGE_NAME,
    lookbackDays: days,
    voidedPurchasesFound: voidedList.length,
    revokedAndLogged: revokedCount,
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const daysIdx = process.argv.indexOf("--days");
  const days = daysIdx > -1 ? Number(process.argv[daysIdx + 1]) : 28;

  runVoidedPurchasesGuard({ days })
    .then((report) => {
      console.log("🛡️ AUDITORIA GOOGLE PLAY VOIDED PURCHASES (AO VIVO):");
      console.log(JSON.stringify(report, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro na Voided Purchases Guard:", err.message);
      process.exit(1);
    });
}
