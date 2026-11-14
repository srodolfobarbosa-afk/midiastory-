import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.49.1";

const CANONICAL_PACKAGE = "com.rdf112.midiastore";
const SUBS_PRODUCTS = new Set(["sweep_pro_monthly", "sweep_pro_annual"]);
const INAPP_PRODUCTS = new Set(["sweep_pro_lifetime"]);

async function sha256Hex(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function base64UrlEncode(data: Uint8Array | string): string {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : data;
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const b64 = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

async function getGoogleAccessToken(sa: {
  client_email: string;
  private_key: string;
  token_uri?: string;
}): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const tokenUri = sa.token_uri || "https://oauth2.googleapis.com/token";
  const header = base64UrlEncode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = base64UrlEncode(
    JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: tokenUri,
      iat: now,
      exp: now + 3600,
    }),
  );

  const signingInput = `${header}.${payload}`;
  const keyData = pemToArrayBuffer(sa.private_key);
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyData,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(signingInput),
  );
  const jwt = `${signingInput}.${base64UrlEncode(new Uint8Array(signature))}`;

  const res = await fetch(tokenUri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!data.access_token) {
    throw new Error("PLAY_OAUTH_FAILED");
  }
  return data.access_token;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ verified: false, entitlementActive: false, reason: "METHOD_NOT_ALLOWED" }),
      { status: 405, headers: { "Content-Type": "application/json" } },
    );
  }

  try {
    const body = await req.json();
    const packageName = String(body?.packageName || "").trim();
    const productId = String(body?.productId || "").trim();
    const purchaseToken = String(body?.purchaseToken || "").trim();

    if (packageName !== CANONICAL_PACKAGE || !productId || !purchaseToken) {
      return new Response(
        JSON.stringify({ verified: false, entitlementActive: false, reason: "INVALID_PARAMETERS" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    if (!SUBS_PRODUCTS.has(productId) && !INAPP_PRODUCTS.has(productId)) {
      return new Response(
        JSON.stringify({ verified: false, entitlementActive: false, reason: "UNSUPPORTED_PRODUCT" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const saRaw = Deno.env.get("PLAY_SERVICE_ACCOUNT_JSON");
    if (!saRaw) {
      return new Response(
        JSON.stringify({ verified: false, entitlementActive: false, reason: "MISSING_SERVICE_ACCOUNT" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

    const sa = JSON.parse(saRaw);
    const accessToken = await getGoogleAccessToken(sa);
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
      });
      if (!playRes.ok) {
        return new Response(
          JSON.stringify({
            verified: false,
            entitlementActive: false,
            reason: `PLAY_API_${playRes.status}`,
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
      entitlementActive =
        matchesProduct &&
        (rawStatus === "SUBSCRIPTION_STATE_ACTIVE" || rawStatus === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD");
      acknowledged = subData?.acknowledgementState === "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED";
      orderId = subData?.latestOrderId || null;
      expiryTime = matchedItem?.expiryTime || lineItems[0]?.expiryTime || null;
    } else {
      // One-time product (sweep_pro_lifetime)
      const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
        CANONICAL_PACKAGE,
      )}/purchases/products/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}`;
      const playRes = await fetch(url, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (!playRes.ok) {
        return new Response(
          JSON.stringify({
            verified: false,
            entitlementActive: false,
            reason: `PLAY_API_${playRes.status}`,
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      const prodData = await playRes.json();
      // purchaseState === 0 means Purchased in Google Play Developer API v3 purchases.products
      purchaseState = typeof prodData?.purchaseState === "number" ? prodData.purchaseState : null;
      rawStatus = purchaseState !== null ? `PURCHASE_STATE_${purchaseState}` : "UNKNOWN";
      entitlementActive = purchaseState === 0;
      acknowledged = prodData?.acknowledgementState === 1;
      orderId = prodData?.orderId || null;
    }

    const tokenHash = await sha256Hex(purchaseToken);
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (supabaseUrl && supabaseServiceKey) {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);
      const { error: dbError } = await supabase.from("play_purchases").upsert(
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
          verified_at: new Date().toISOString(),
        },
        { onConflict: "purchase_token_hash" },
      );
      if (dbError) {
        console.error("Failed to persist play_purchases audit row:", dbError.message);
      }
    }

    return new Response(
      JSON.stringify({
        verified: entitlementActive,
        entitlementActive,
        acknowledged,
        productId,
        productType,
        status: rawStatus,
        ...(productType === "SUBS" ? { subscriptionState: rawStatus } : { purchaseState }),
        expiryTime,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({
        verified: false,
        entitlementActive: false,
        reason: err instanceof Error ? err.message : "VERIFICATION_ERROR",
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }
});
