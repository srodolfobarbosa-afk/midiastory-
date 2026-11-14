import { serve } from "https://deno.land/std@0.177.0/http/server.ts";

const CANONICAL_PACKAGE = "com.rdf112.midiastore";
const SUBS_PRODUCTS = new Set(["sweep_pro_monthly", "sweep_pro_annual"]);
const INAPP_PRODUCTS = new Set(["sweep_pro_lifetime"]);

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

serve(async (req: Request) => {
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

    if (SUBS_PRODUCTS.has(productId)) {
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
      const state = String(subData?.subscriptionState || "");
      const lineItems = Array.isArray(subData?.lineItems) ? subData.lineItems : [];
      const matchesProduct = lineItems.some((item: { productId?: string }) => item?.productId === productId);
      const isActive =
        matchesProduct &&
        (state === "SUBSCRIPTION_STATE_ACTIVE" || state === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD");

      return new Response(
        JSON.stringify({
          verified: isActive,
          entitlementActive: isActive,
          subscriptionState: state,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }

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
    const isPurchased = prodData?.purchaseState === 0;

    return new Response(
      JSON.stringify({
        verified: isPurchased,
        entitlementActive: isPurchased,
        purchaseState: prodData?.purchaseState ?? null,
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
