// Shared low-latency Google Play Developer API v3 OAuth2 & Crypto helpers
// Runs inside Deno Edge Isolates with in-memory token caching (50 min TTL)

export const CANONICAL_PACKAGE = "com.rdf112.midiastore";
export const SUBS_PRODUCTS = new Set(["sweep_pro_monthly", "sweep_pro_annual"]);
export const INAPP_PRODUCTS = new Set(["sweep_pro_lifetime"]);

export interface GoogleServiceAccount {
  client_email: string;
  private_key: string;
  token_uri?: string;
}

interface CachedToken {
  accessToken: string;
  expiresAtMs: number;
  clientEmail: string;
}

let cachedAccessToken: CachedToken | null = null;
let inFlightTokenPromise: Promise<string> | null = null;

export async function sha256Hex(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export function base64UrlEncode(data: Uint8Array | string): string {
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

/**
 * Returns a Google Play API OAuth2 access token with isolate-level caching (50 min TTL)
 * and Single-Flight coalescing to avoid redundant OAuth round-trips under burst traffic.
 */
export async function getCachedGoogleAccessToken(
  sa: GoogleServiceAccount,
  timeoutMs = 8000,
): Promise<{ accessToken: string; cacheHit: boolean }> {
  const nowMs = Date.now();
  if (
    cachedAccessToken &&
    cachedAccessToken.clientEmail === sa.client_email &&
    cachedAccessToken.expiresAtMs > nowMs
  ) {
    return { accessToken: cachedAccessToken.accessToken, cacheHit: true };
  }

  if (inFlightTokenPromise) {
    const token = await inFlightTokenPromise;
    return { accessToken: token, cacheHit: true };
  }

  inFlightTokenPromise = (async () => {
    const nowSec = Math.floor(Date.now() / 1000);
    const tokenUri = sa.token_uri || "https://oauth2.googleapis.com/token";
    const header = base64UrlEncode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
    const payload = base64UrlEncode(
      JSON.stringify({
        iss: sa.client_email,
        scope: "https://www.googleapis.com/auth/androidpublisher",
        aud: tokenUri,
        iat: nowSec,
        exp: nowSec + 3600,
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
      signal: AbortSignal.timeout(timeoutMs),
    });
    const data = await res.json();
    if (!res.ok || !data?.access_token) {
      throw new Error(`PLAY_OAUTH_FAILED_${res.status}`);
    }

    // Cache for 50 minutes (Google tokens live 60 minutes)
    cachedAccessToken = {
      accessToken: String(data.access_token),
      expiresAtMs: Date.now() + 50 * 60 * 1000,
      clientEmail: sa.client_email,
    };
    return cachedAccessToken.accessToken;
  })();

  try {
    const token = await inFlightTokenPromise;
    return { accessToken: token, cacheHit: false };
  } finally {
    inFlightTokenPromise = null;
  }
}

export async function signEntitlementLease(
  payload: Record<string, unknown>,
  secret: string,
): Promise<string> {
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(encodedPayload));
  return `${encodedPayload}.${base64UrlEncode(new Uint8Array(sig))}`;
}

export function timingSafeEqualStr(a: string, b: string): boolean {
  const encA = new TextEncoder().encode(a);
  const encB = new TextEncoder().encode(b);
  if (encA.length !== encB.length) return false;
  let diff = 0;
  for (let i = 0; i < encA.length; i++) {
    diff |= encA[i] ^ encB[i];
  }
  return diff === 0;
}

/**
 * Determines whether a Google Play subscription v2 state grants active Pro entitlement.
 * Supports active 3-day Free Trial (`annual-trial-3d` under `SUBSCRIPTION_STATE_ACTIVE`) and
 * `SUBSCRIPTION_STATE_CANCELED` while `expiryTime` is still in the future; strictly fail-closed
 * once `expiryTime` passes (`SUBSCRIPTION_STATE_EXPIRED`).
 */
export function isSubscriptionEntitlementActive(
  subscriptionState: string,
  expiryTime: string | null | undefined,
): boolean {
  const hasFutureExpiry = (() => {
    if (!expiryTime) return true;
    const parsedExpiry = Date.parse(expiryTime);
    return Number.isNaN(parsedExpiry) ? false : parsedExpiry > Date.now();
  })();

  if (
    subscriptionState === "SUBSCRIPTION_STATE_ACTIVE" ||
    subscriptionState === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"
  ) {
    return hasFutureExpiry;
  }
  if (subscriptionState === "SUBSCRIPTION_STATE_CANCELED" && expiryTime) {
    return hasFutureExpiry;
  }
  return false;
}
