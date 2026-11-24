import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.49.1";
import {
  CANONICAL_PACKAGE,
  INAPP_PRODUCTS,
  SUBS_PRODUCTS,
  getCachedGoogleAccessToken,
  isSubscriptionEntitlementActive,
  sha256Hex,
  timingSafeEqualStr,
} from "../_shared/play-auth.ts";

const EXTERNAL_TIMEOUT_MS = 8000;

const CATALOG_BRL_CENTS: Record<string, number> = {
  sweep_pro_monthly: 1499,
  sweep_pro_annual: 7999,
  sweep_pro_lifetime: 14999,
};

interface PubSubPushBody {
  message?: {
    messageId?: string;
    message_id?: string;
    data?: string;
    publishTime?: string;
  };
  subscription?: string;
}

interface RtdnPayload {
  version?: string;
  packageName?: string;
  eventTimeMillis?: string;
  subscriptionNotification?: {
    version?: string;
    notificationType?: number;
    purchaseToken?: string;
    subscriptionId?: string;
  };
  oneTimeProductNotification?: {
    version?: string;
    notificationType?: number;
    purchaseToken?: string;
    sku?: string;
  };
  voidedPurchaseNotification?: {
    purchaseToken?: string;
    orderId?: string;
    productType?: number; // 1 = PRODUCT_TYPE_SUBSCRIPTION, 2 = PRODUCT_TYPE_ONE_TIME
    refundType?: number;  // 1 = REFUND_TYPE_FULL_REFUND, 2 = REFUND_TYPE_QUANTITY_BASED_PARTIAL_REFUND
  };
  testNotification?: {
    version?: string;
  };
}

function decodeBase64UrlBytes(b64: string): Uint8Array {
  const normalized = b64.replace(/-/g, "+").replace(/_/g, "/");
  const pad = normalized.length % 4 === 0 ? "" : "=".repeat(4 - (normalized.length % 4));
  const bin = atob(normalized + pad);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

function decodeBase64UrlJson(b64: string): Record<string, unknown> {
  return JSON.parse(new TextDecoder().decode(decodeBase64UrlBytes(b64)));
}

function decodeBase64Json(b64: string): RtdnPayload {
  return decodeBase64UrlJson(b64) as unknown as RtdnPayload;
}

interface GoogleJwk extends JsonWebKey {
  kid?: string;
}

let cachedGoogleJwks: { keys: Map<string, CryptoKey>; expiresAtMs: number } | null = null;

async function getGoogleCertCryptoKey(kid: string): Promise<CryptoKey | null> {
  const now = Date.now();
  if (cachedGoogleJwks && cachedGoogleJwks.expiresAtMs > now) {
    const hit = cachedGoogleJwks.keys.get(kid);
    if (hit) return hit;
  }

  const res = await fetch("https://www.googleapis.com/oauth2/v3/certs", {
    signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
  });
  if (!res.ok) return null;
  const body = (await res.json()) as { keys?: GoogleJwk[] };
  const keyMap = new Map<string, CryptoKey>();
  for (const jwk of body.keys || []) {
    if (!jwk.kid || jwk.kty !== "RSA") continue;
    const imported = await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"],
    );
    keyMap.set(jwk.kid, imported);
  }
  cachedGoogleJwks = { keys: keyMap, expiresAtMs: now + 60 * 60 * 1000 };
  return keyMap.get(kid) || null;
}

/**
 * Verifies Google Cloud Pub/Sub OIDC JWT Bearer token claims + cryptographic RS256 signature against Google JWKS.
 */
async function verifyPubSubBearerJwt(
  authHeader: string,
  expectedAudience: string | undefined,
  expectedSaEmail?: string | undefined,
): Promise<{ valid: boolean; reason?: string }> {
  const match = authHeader.match(/^Bearer\s+([A-Za-z0-9\-_]+)\.([A-Za-z0-9\-_]+)\.([A-Za-z0-9\-_]+)$/i);
  if (!match) return { valid: false, reason: "INVALID_BEARER_FORMAT" };

  const [, headerB64, payloadB64, sigB64] = match;
  try {
    const header = decodeBase64UrlJson(headerB64);
    const payload = decodeBase64UrlJson(payloadB64);
    const kid = String(header.kid || "");
    const iss = String(payload.iss || "");
    const exp = Number(payload.exp || 0);
    const aud = String(payload.aud || "");
    const email = String(payload.email || "").trim().toLowerCase();
    const nowSec = Math.floor(Date.now() / 1000);

    if (header.alg !== "RS256" || !kid) return { valid: false, reason: "UNSUPPORTED_JWT_ALG" };
    if (iss !== "https://accounts.google.com" && iss !== "accounts.google.com") {
      return { valid: false, reason: "INVALID_JWT_ISSUER" };
    }
    if (!exp || exp < nowSec - 60) {
      return { valid: false, reason: "EXPIRED_JWT" };
    }
    if (!aud || (expectedAudience && aud !== expectedAudience)) {
      return { valid: false, reason: "INVALID_JWT_AUDIENCE" };
    }
    if (payload.email_verified !== true) {
      return { valid: false, reason: "UNVERIFIED_JWT_EMAIL" };
    }
    if (expectedSaEmail && email !== expectedSaEmail.trim().toLowerCase()) {
      return { valid: false, reason: "UNEXPECTED_JWT_SA_EMAIL" };
    }

    const cryptoKey = await getGoogleCertCryptoKey(kid);
    if (!cryptoKey) {
      return { valid: false, reason: "UNKNOWN_GOOGLE_JWK_KID" };
    }
    const signedData = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
    const sigBytes = decodeBase64UrlBytes(sigB64);
    const sigValid = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      cryptoKey,
      sigBytes,
      signedData,
    );
    if (!sigValid) {
      return { valid: false, reason: "INVALID_JWT_SIGNATURE" };
    }

    return { valid: true };
  } catch {
    return { valid: false, reason: "MALFORMED_JWT" };
  }
}

function classifyLedgerEvent(
  isSubs: boolean,
  notificationType: number | null,
): { eventType: "GROSS_SALE" | "REFUND"; direction: "CREDIT" | "DEBIT" } | null {
  if (notificationType === null) return null;
  if (isSubs) {
    // 2 = SUBSCRIPTION_RENEWED, 4 = SUBSCRIPTION_PURCHASED, 1 = SUBSCRIPTION_RECOVERED
    if (notificationType === 2 || notificationType === 4 || notificationType === 1) {
      return { eventType: "GROSS_SALE", direction: "CREDIT" };
    }
    // 12 = SUBSCRIPTION_REVOKED
    if (notificationType === 12) {
      return { eventType: "REFUND", direction: "DEBIT" };
    }
    return null;
  }
  // One-time product: 1 = ONE_TIME_PRODUCT_PURCHASED, 2 = ONE_TIME_PRODUCT_CANCELED
  if (notificationType === 1) return { eventType: "GROSS_SALE", direction: "CREDIT" };
  if (notificationType === 2) return { eventType: "REFUND", direction: "DEBIT" };
  return null;
}

Deno.serve(async (req: Request) => {
  const startedAt = performance.now();
  const requestId = crypto.randomUUID();

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ ok: false, reason: "METHOD_NOT_ALLOWED", requestId }), {
      status: 405,
      headers: { "Content-Type": "application/json" },
    });
  }

  // 1. Fail-closed auth: requires either RTDN_WEBHOOK_TOKEN or verified Google Pub/Sub OIDC JWT
  const expectedToken = Deno.env.get("RTDN_WEBHOOK_TOKEN");
  const expectedAud = Deno.env.get("RTDN_EXPECTED_AUDIENCE");
  const expectedSaEmail = Deno.env.get("RTDN_EXPECTED_SA_EMAIL");
  const requireJwt = Deno.env.get("RTDN_VERIFY_PUBSUB_JWT") === "true";

  if (!expectedToken && !requireJwt && !expectedAud) {
    return new Response(
      JSON.stringify({ ok: false, reason: "WEBHOOK_AUTH_NOT_CONFIGURED", requestId }),
      { status: 503, headers: { "Content-Type": "application/json" } },
    );
  }

  if (requireJwt || expectedAud) {
    const authHeader = req.headers.get("authorization") || "";
    const effectiveAudience = expectedAud || req.url.split("?")[0];
    const jwtCheck = await verifyPubSubBearerJwt(authHeader, effectiveAudience, expectedSaEmail);
    if (!jwtCheck.valid) {
      return new Response(
        JSON.stringify({ ok: false, reason: jwtCheck.reason || "UNAUTHORIZED_JWT", requestId }),
        { status: 401, headers: { "Content-Type": "application/json" } },
      );
    }
  }

  if (expectedToken) {
    const url = new URL(req.url);
    const suppliedToken =
      url.searchParams.get("token") || req.headers.get("x-rtdn-token") || "";
    if (!suppliedToken || !timingSafeEqualStr(suppliedToken, expectedToken)) {
      return new Response(JSON.stringify({ ok: false, reason: "UNAUTHORIZED", requestId }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      });
    }
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !supabaseServiceKey) {
    return new Response(JSON.stringify({ ok: false, reason: "MISSING_SUPABASE_ENV", requestId }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }

  try {
    const body = (await req.json()) as PubSubPushBody;
    const messageId = String(body?.message?.messageId || body?.message?.message_id || "").trim();
    const rawData = String(body?.message?.data || "").trim();

    if (!messageId || !rawData) {
      return new Response(
        JSON.stringify({ ok: false, reason: "INVALID_PUBSUB_ENVELOPE", requestId }),
        { status: 400, headers: { "Content-Type": "application/json" } },
      );
    }

    const rtdn = decodeBase64Json(rawData);
    const packageName = String(rtdn.packageName || "").trim();
    if (packageName !== CANONICAL_PACKAGE) {
      return new Response(
        JSON.stringify({ ok: true, ignored: true, reason: "FOREIGN_PACKAGE", requestId }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);
    const subNotif = rtdn.subscriptionNotification;
    const otpNotif = rtdn.oneTimeProductNotification;
    const voidedNotif = rtdn.voidedPurchaseNotification;
    const purchaseToken = String(
      subNotif?.purchaseToken || otpNotif?.purchaseToken || voidedNotif?.purchaseToken || "",
    ).trim();
    const productId = String(subNotif?.subscriptionId || otpNotif?.sku || "").trim() || null;
    const notificationType =
      subNotif?.notificationType ?? otpNotif?.notificationType ?? voidedNotif?.refundType ?? null;
    const tokenHash = purchaseToken ? await sha256Hex(purchaseToken) : null;

    // 2. Strict idempotency by message_id — only deduplicate when processed === true
    const { error: insertErr } = await supabase.from("rtdn_events").insert({
      message_id: messageId,
      notification_type: notificationType,
      package_name: packageName,
      product_id: productId,
      purchase_token_hash: tokenHash,
      event_time_millis: rtdn.eventTimeMillis ? Number(rtdn.eventTimeMillis) : null,
      processed: false,
      raw_payload: rtdn,
    });

    if (insertErr) {
      if (insertErr.code === "23505") {
        const { data: existingEvent, error: selectErr } = await supabase
          .from("rtdn_events")
          .select("processed")
          .eq("message_id", messageId)
          .maybeSingle();
        if (selectErr) {
          throw new Error(`RTDN_DEDUP_CHECK_FAILED: ${selectErr.message}`);
        }
        if (existingEvent?.processed === true) {
          return new Response(
            JSON.stringify({
              ok: true,
              deduplicated: true,
              messageId,
              requestId,
              latencyMs: Math.round(performance.now() - startedAt),
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          );
        }
        // processed === false: transient Play API or DB failure on prior attempt — continue reprocessing!
      } else {
        throw new Error(`RTDN_PERSIST_FAILED: ${insertErr.message}`);
      }
    }

    if (rtdn.testNotification) {
      const { error: testUpdateErr } = await supabase
        .from("rtdn_events")
        .update({ processed: true, processing_result: "TEST_NOTIFICATION_ACK" })
        .eq("message_id", messageId);
      if (testUpdateErr) {
        throw new Error(`RTDN_TEST_ACK_FAILED: ${testUpdateErr.message}`);
      }
      return new Response(
        JSON.stringify({
          ok: true,
          type: "testNotification",
          messageId,
          requestId,
          latencyMs: Math.round(performance.now() - startedAt),
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    if (!purchaseToken || !tokenHash) {
      await supabase
        .from("rtdn_events")
        .update({ processed: true, processing_result: "NO_PURCHASE_TOKEN" })
        .eq("message_id", messageId);
      return new Response(JSON.stringify({ ok: true, skipped: "NO_PURCHASE_TOKEN", requestId }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    const saRaw = Deno.env.get("PLAY_SERVICE_ACCOUNT_JSON");
    const nowIso = new Date().toISOString();

    let entitlementActive = false;
    let acknowledged = false;
    let orderId: string | null = null;
    let expiryTime: string | null = null;
    let rawStatus = "UNKNOWN";
    let resolvedProductId = productId || "unknown";
    let isTestPurchase = false;
    let productType: "SUBS" | "INAPP" = subNotif ? "SUBS" : "INAPP";
    let voidedLedgerAmountCents: number | null = null;
    let voidedLedgerBrlCents: number | null = null;

    if (voidedNotif) {
      entitlementActive = false;
      rawStatus = "VOIDED_PURCHASE";
      const voidedOrderId = String(voidedNotif.orderId || "").trim() || null;
      // Google Play VoidedPurchaseNotification.productType: 1 = SUBSCRIPTION, 2 = ONE_TIME
      productType = voidedNotif.productType === 1 ? "SUBS" : "INAPP";

      const [{ data: existingPurchase }, { data: existingEntitlement }, { data: existingSale }] =
        await Promise.all([
          supabase
            .from("play_purchases")
            .select("product_id, product_type, order_id, acknowledged, expiry_time")
            .eq("purchase_token_hash", tokenHash)
            .maybeSingle(),
          supabase
            .from("entitlements")
            .select("product_id, product_type, latest_order_id, acknowledged, expiry_time")
            .eq("subject_key", tokenHash)
            .maybeSingle(),
          voidedOrderId
            ? supabase
                .from("financial_ledger")
                .select("product_id, amount_cents, amount_brl_cents, is_test_transaction")
                .eq("order_id", voidedOrderId)
                .eq("event_type", "GROSS_SALE")
                .maybeSingle()
            : Promise.resolve({ data: null }),
        ]);

      if (voidedNotif.productType !== 1 && voidedNotif.productType !== 2) {
        if (existingPurchase?.product_type === "SUBS" || existingEntitlement?.product_type === "SUBS") {
          productType = "SUBS";
        }
      }

      orderId =
        voidedOrderId ||
        existingPurchase?.order_id ||
        existingEntitlement?.latest_order_id ||
        null;
      acknowledged = Boolean(
        existingPurchase?.acknowledged ?? existingEntitlement?.acknowledged ?? false,
      );
      expiryTime = existingPurchase?.expiry_time || existingEntitlement?.expiry_time || null;
      isTestPurchase = Boolean(existingSale?.is_test_transaction);

      if (typeof existingSale?.amount_cents === "number") {
        voidedLedgerAmountCents = existingSale.amount_cents;
      }
      if (typeof existingSale?.amount_brl_cents === "number") {
        voidedLedgerBrlCents = existingSale.amount_brl_cents;
      }

      const knownProduct =
        existingPurchase?.product_id ||
        existingEntitlement?.product_id ||
        existingSale?.product_id ||
        null;

      if (knownProduct && knownProduct !== "unknown") {
        resolvedProductId = String(knownProduct);
      } else if (productType === "SUBS" && saRaw) {
        try {
          const sa = JSON.parse(saRaw);
          const { accessToken } = await getCachedGoogleAccessToken(sa, EXTERNAL_TIMEOUT_MS);
          const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
            CANONICAL_PACKAGE,
          )}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
          const playRes = await fetch(url, {
            headers: { Authorization: `Bearer ${accessToken}` },
            signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
          });
          if (playRes.ok) {
            const subData = await playRes.json();
            isTestPurchase = Boolean(subData?.testPurchase);
            const lineItems = Array.isArray(subData?.lineItems) ? subData.lineItems : [];
            if (lineItems[0]?.productId) {
              resolvedProductId = String(lineItems[0].productId);
            }
            if (!expiryTime && lineItems[0]?.expiryTime) {
              expiryTime = String(lineItems[0].expiryTime);
            }
            if (!orderId && subData?.latestOrderId) {
              orderId = String(subData.latestOrderId);
            }
          }
        } catch {
          // Best-effort lookup only; proceed with fail-closed revocation
        }
      } else if (productType === "INAPP" && INAPP_PRODUCTS.size === 1) {
        resolvedProductId = Array.from(INAPP_PRODUCTS)[0];
      }
    } else {
      if (!saRaw) throw new Error("MISSING_SERVICE_ACCOUNT");
      const sa = JSON.parse(saRaw);
      const { accessToken } = await getCachedGoogleAccessToken(sa, EXTERNAL_TIMEOUT_MS);

      if (subNotif) {
        const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
          CANONICAL_PACKAGE,
        )}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
        const playRes = await fetch(url, {
          headers: { Authorization: `Bearer ${accessToken}` },
          signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
        });
        if (!playRes.ok) throw new Error(`PLAY_API_${playRes.status}`);

        const subData = await playRes.json();
        rawStatus = String(subData?.subscriptionState || "UNKNOWN");
        isTestPurchase = Boolean(subData?.testPurchase);
        const lineItems = Array.isArray(subData?.lineItems) ? subData.lineItems : [];
        const matchedItem = subNotif.subscriptionId
          ? lineItems.find(
              (item: { productId?: string; expiryTime?: string }) =>
                item?.productId === subNotif.subscriptionId,
            )
          : undefined;
        const primaryItem = matchedItem || lineItems[0];
        if (primaryItem?.productId) resolvedProductId = String(primaryItem.productId);
        expiryTime = primaryItem?.expiryTime || null;
        entitlementActive =
          SUBS_PRODUCTS.has(resolvedProductId) &&
          isSubscriptionEntitlementActive(rawStatus, expiryTime);
        acknowledged = subData?.acknowledgementState === "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED";
        orderId = subData?.latestOrderId || null;
      } else if (otpNotif && productId && INAPP_PRODUCTS.has(productId)) {
        const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
          CANONICAL_PACKAGE,
        )}/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}`;
        const playRes = await fetch(url, {
          headers: { Authorization: `Bearer ${accessToken}` },
          signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
        });
        if (!playRes.ok) throw new Error(`PLAY_API_${playRes.status}`);

        const prodData = await playRes.json();
        const purchaseState = typeof prodData?.purchaseState === "number" ? prodData.purchaseState : null;
        rawStatus = purchaseState !== null ? `PURCHASE_STATE_${purchaseState}` : "UNKNOWN";
        isTestPurchase = prodData?.purchaseType === 0; // 0 = Test purchase in Play API
        entitlementActive = purchaseState === 0;
        acknowledged = prodData?.acknowledgementState === 1;
        orderId = prodData?.orderId || null;
      }
    }

    const writes: Promise<{ error?: { message?: string } | null }>[] = [
      supabase.from("play_purchases").upsert(
        {
          package_name: CANONICAL_PACKAGE,
          product_id: resolvedProductId,
          product_type: productType,
          purchase_token_hash: tokenHash,
          order_id: orderId,
          entitlement_active: entitlementActive,
          acknowledged,
          expiry_time: expiryTime,
          raw_status: rawStatus,
          verified_at: nowIso,
        },
        { onConflict: "purchase_token_hash" },
      ),
      supabase.from("entitlements").upsert(
        {
          subject_key: tokenHash,
          source: "GOOGLE_PLAY",
          package_name: CANONICAL_PACKAGE,
          product_id: resolvedProductId,
          product_type: productType === "SUBS" ? "SUBS" : "LIFETIME",
          is_active: entitlementActive,
          acknowledged,
          status: rawStatus,
          expiry_time: expiryTime,
          latest_order_id: orderId,
          last_verified_at: nowIso,
          updated_at: nowIso,
        },
        { onConflict: "subject_key" },
      ),
    ];

    // 3. Append-only financial_ledger entry (idempotent)
    const ledgerClass = voidedNotif
      ? ({ eventType: "REFUND", direction: "DEBIT" } as const)
      : classifyLedgerEvent(Boolean(subNotif), notificationType);
    const catalogCents = CATALOG_BRL_CENTS[resolvedProductId] || 0;
    const ledgerAmountCents = voidedNotif
      ? (voidedLedgerAmountCents ?? catalogCents)
      : catalogCents;
    const ledgerBrlCents = voidedNotif
      ? (voidedLedgerBrlCents ?? ledgerAmountCents)
      : catalogCents;

    if (ledgerClass && (Boolean(voidedNotif) || ledgerAmountCents > 0)) {
      const idempotencyKey = voidedNotif
        ? `rtdn:voided:${orderId || tokenHash}`
        : `rtdn:${messageId}:${ledgerClass.eventType}`;
      writes.push(
        supabase.from("financial_ledger").upsert(
          {
            idempotency_key: idempotencyKey,
            event_type: ledgerClass.eventType,
            source: "GOOGLE_PLAY",
            order_id: orderId,
            product_id: resolvedProductId,
            direction: ledgerClass.direction,
            amount_cents: ledgerAmountCents,
            currency: "BRL",
            fx_rate_to_brl: 1.0,
            amount_brl_cents: ledgerBrlCents,
            is_test_transaction: isTestPurchase,
            occurred_at: nowIso,
            metadata: {
              messageId,
              notificationType,
              rawStatus,
              ...(voidedNotif
                ? {
                    voidedProductType: voidedNotif.productType ?? null,
                    refundType: voidedNotif.refundType ?? null,
                  }
                : {}),
            },
          },
          { onConflict: "idempotency_key", ignoreDuplicates: true },
        ),
      );
    }

    const writeResults = await Promise.all(writes);
    for (const r of writeResults) {
      if (r?.error) {
        throw new Error(`RTDN_DB_WRITE_FAILED: ${r.error.message || "unknown"}`);
      }
    }

    const { error: markErr } = await supabase
      .from("rtdn_events")
      .update({
        processed: true,
        processing_result: `SYNCED_${rawStatus}_ACTIVE_${entitlementActive}_LEDGER_${ledgerClass?.eventType || "NONE"}`,
      })
      .eq("message_id", messageId);
    if (markErr) {
      throw new Error(`RTDN_MARK_PROCESSED_FAILED: ${markErr.message}`);
    }

    return new Response(
      JSON.stringify({
        ok: true,
        messageId,
        productId: resolvedProductId,
        entitlementActive,
        status: rawStatus,
        ledgerEvent: ledgerClass?.eventType || null,
        isTestPurchase,
        requestId,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  } catch (err) {
    console.error("play-rtdn-webhook error:", err);
    return new Response(
      JSON.stringify({
        ok: false,
        reason: err instanceof Error ? err.message : "RTDN_ERROR",
        requestId,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }
});
