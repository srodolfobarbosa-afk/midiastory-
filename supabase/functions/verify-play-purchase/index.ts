import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.49.1";
import {
  CANONICAL_PACKAGE,
  INAPP_PRODUCTS,
  SUBS_PRODUCTS,
  getCachedGoogleAccessToken,
  isSubscriptionEntitlementActive,
  sha256Hex,
  signEntitlementLease,
} from "../_shared/play-auth.ts";

const EXTERNAL_TIMEOUT_MS = 8000;
const SHORT_CACHE_TTL_MS = 45_000;
const MAX_CACHE_ENTRIES = 250;
const OFFLINE_LEASE_MS = 72 * 60 * 60 * 1000; // 72h offline grace lease

interface CachedVerification {
  responsePayload: Record<string, unknown>;
  expiresAtMs: number;
}

const verificationCache = new Map<string, CachedVerification>();

function putVerificationCache(key: string, responsePayload: Record<string, unknown>) {
  if (verificationCache.size >= MAX_CACHE_ENTRIES) {
    const oldestKey = verificationCache.keys().next().value;
    if (oldestKey) verificationCache.delete(oldestKey);
  }
  verificationCache.set(key, {
    responsePayload,
    expiresAtMs: Date.now() + SHORT_CACHE_TTL_MS,
  });
}

function computeLeaseValidUntil(expiryTime: string | null): string {
  const maxLeaseMs = Date.now() + OFFLINE_LEASE_MS;
  if (!expiryTime) {
    return new Date(maxLeaseMs).toISOString();
  }
  const parsedExpiry = Date.parse(expiryTime);
  if (Number.isNaN(parsedExpiry)) {
    return new Date(maxLeaseMs).toISOString();
  }
  return new Date(Math.min(maxLeaseMs, parsedExpiry)).toISOString();
}

Deno.serve(async (req: Request) => {
  const startedAt = performance.now();
  const requestId = crypto.randomUUID();

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({
        verified: false,
        entitlementActive: false,
        reason: "METHOD_NOT_ALLOWED",
        requestId,
      }),
      { status: 405, headers: { "Content-Type": "application/json" } },
    );
  }

  try {
    const body = await req.json();
    const packageName = String(body?.packageName || "").trim();
    const productId = String(body?.productId || "").trim();
    const purchaseToken = String(body?.purchaseToken || "").trim();
    const bypassCache = Boolean(body?.forceRefresh);

    if (packageName !== CANONICAL_PACKAGE || !productId || !purchaseToken) {
      return new Response(
        JSON.stringify({
          verified: false,
          entitlementActive: false,
          reason: "INVALID_PARAMETERS",
          requestId,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    if (!SUBS_PRODUCTS.has(productId) && !INAPP_PRODUCTS.has(productId)) {
      return new Response(
        JSON.stringify({
          verified: false,
          entitlementActive: false,
          reason: "UNSUPPORTED_PRODUCT",
          requestId,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const tokenHash = await sha256Hex(purchaseToken);
    const cacheKey = `${productId}:${tokenHash}`;
    if (!bypassCache) {
      const cached = verificationCache.get(cacheKey);
      if (cached && cached.expiresAtMs > Date.now()) {
        return new Response(
          JSON.stringify({
            ...cached.responsePayload,
            requestId,
            verificationCacheHit: true,
            latencyMs: Math.round(performance.now() - startedAt),
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
    }

    const saRaw = Deno.env.get("PLAY_SERVICE_ACCOUNT_JSON");
    if (!saRaw) {
      return new Response(
        JSON.stringify({
          verified: false,
          entitlementActive: false,
          reason: "MISSING_SERVICE_ACCOUNT",
          requestId,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const sa = JSON.parse(saRaw);
    const { accessToken, cacheHit: oauthCacheHit } = await getCachedGoogleAccessToken(
      sa,
      EXTERNAL_TIMEOUT_MS,
    );
    const productType = SUBS_PRODUCTS.has(productId) ? "SUBS" : "INAPP";

    let entitlementActive = false;
    let acknowledged = false;
    let orderId: string | null = null;
    let expiryTime: string | null = null;
    let rawStatus = "UNKNOWN";
    let purchaseState: number | null = null;

    if (productType === "SUBS") {
      const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
        CANONICAL_PACKAGE,
      )}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
      const playRes = await fetch(url, {
        headers: { Authorization: `Bearer ${accessToken}` },
        signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
      });
      if (!playRes.ok) {
        return new Response(
          JSON.stringify({
            verified: false,
            entitlementActive: false,
            reason: `PLAY_API_${playRes.status}`,
            requestId,
            oauthCacheHit,
            latencyMs: Math.round(performance.now() - startedAt),
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const subData = await playRes.json();
      rawStatus = String(subData?.subscriptionState || "UNKNOWN");
      const lineItems = Array.isArray(subData?.lineItems) ? subData.lineItems : [];
      const matchedItem = lineItems.find(
        (item: { productId?: string; expiryTime?: string }) => item?.productId === productId,
      );
      const matchesProduct = Boolean(matchedItem);
      expiryTime = matchedItem?.expiryTime || lineItems[0]?.expiryTime || null;
      entitlementActive =
        matchesProduct && isSubscriptionEntitlementActive(rawStatus, expiryTime);
      acknowledged = subData?.acknowledgementState === "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED";
      orderId = subData?.latestOrderId || null;
    } else {
      const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
        CANONICAL_PACKAGE,
      )}/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}`;
      const playRes = await fetch(url, {
        headers: { Authorization: `Bearer ${accessToken}` },
        signal: AbortSignal.timeout(EXTERNAL_TIMEOUT_MS),
      });
      if (!playRes.ok) {
        return new Response(
          JSON.stringify({
            verified: false,
            entitlementActive: false,
            reason: `PLAY_API_${playRes.status}`,
            requestId,
            oauthCacheHit,
            latencyMs: Math.round(performance.now() - startedAt),
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const prodData = await playRes.json();
      purchaseState = typeof prodData?.purchaseState === "number" ? prodData.purchaseState : null;
      rawStatus = purchaseState !== null ? `PURCHASE_STATE_${purchaseState}` : "UNKNOWN";
      entitlementActive = purchaseState === 0;
      acknowledged = prodData?.acknowledgementState === 1;
      orderId = prodData?.orderId || null;
    }

    const nowIso = new Date().toISOString();
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (supabaseUrl && supabaseServiceKey) {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);
      const [purchaseWrite, entitlementWrite] = await Promise.all([
        supabase.from("play_purchases").upsert(
          {
            package_name: CANONICAL_PACKAGE,
            product_id: productId,
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
            product_id: productId,
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
      ]);
      if (purchaseWrite.error) {
        console.error("Failed to persist play_purchases audit row:", purchaseWrite.error.message);
      }
      if (entitlementWrite.error) {
        console.error("Failed to persist entitlements row:", entitlementWrite.error.message);
      }
    }

    let signedLease: string | null = null;
    const leaseSecret = Deno.env.get("ENTITLEMENT_LEASE_SECRET");
    if (entitlementActive && leaseSecret) {
      signedLease = await signEntitlementLease(
        {
          sub: tokenHash,
          pkg: CANONICAL_PACKAGE,
          pid: productId,
          active: true,
          iat: nowIso,
          validUntil: computeLeaseValidUntil(expiryTime),
        },
        leaseSecret,
      );
    }

    const baseResponse: Record<string, unknown> = {
      verified: entitlementActive,
      entitlementActive,
      acknowledged,
      productId,
      productType,
      status: rawStatus,
      ...(productType === "SUBS" ? { subscriptionState: rawStatus } : { purchaseState }),
      expiryTime,
      ...(signedLease ? { signedLease } : {}),
    };

    if (entitlementActive) {
      putVerificationCache(cacheKey, baseResponse);
    }

    return new Response(
      JSON.stringify({
        ...baseResponse,
        requestId,
        oauthCacheHit,
        verificationCacheHit: false,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({
        verified: false,
        entitlementActive: false,
        reason: err instanceof Error ? err.message : "VERIFICATION_ERROR",
        requestId,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }
});
