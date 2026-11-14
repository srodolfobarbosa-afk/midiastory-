import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const key = JSON.parse(fs.readFileSync(path.join(projectRoot, ".secrets", "play-service-account.json"), "utf8"));
const packageName = "com.rdf112.midiastore";

// Mercado Tier-1 + dolarizados recebem conversao oficial da Google Play a partir dos novos precos em USD.
// Brasil (BR: R$ 14,99 / R$ 79,99 / R$ 149,99) e mercados emergentes preservam os valores localizados acessiveis.
const TIER1_USD_REGIONS = new Set([
  "US", "CA", "GB", "AU", "NZ", "IE",
  "CH", "NO", "SE", "DK", "FI", "IS", "LI",
  "DE", "FR", "NL", "BE", "AT", "LU", "IT", "ES", "PT",
  "JP", "KR", "SG", "HK", "TW",
  "AE", "SA", "QA", "KW", "BH", "OM", "IL",
  "PR", "VI", "GU", "AS", "MP"
]);

async function getAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: key.token_uri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");

  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const jwt = `${header}.${payload}.${sign.sign(key.private_key, "base64url")}`;

  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!data.access_token) throw new Error(JSON.stringify(data));
  return data.access_token;
}

async function convertUsdPrices(token, units, nanos) {
  const res = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/pricing:convertRegionPrices`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        price: { currencyCode: "USD", units: String(units), nanos },
      }),
    }
  );
  const text = await res.text();
  if (!res.ok) throw new Error(`convertRegionPrices (${res.status}): ${text}`);
  return JSON.parse(text);
}

function fmtPrice(p) {
  if (!p) return "N/A";
  const units = Number(p.units || 0);
  const nanos = Number(p.nanos || 0) / 1e9;
  return `${p.currencyCode} ${(units + nanos).toFixed(2)}`;
}

async function updateSubscriptionUsd(token, productId, basePlanId, targetUsdUnits, targetUsdNanos) {
  console.log(`\n=== Atualizando assinatura ${productId} (${basePlanId}) para USD ${targetUsdUnits}.${Math.round(targetUsdNanos / 1e7)} ===`);

  const getRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/subscriptions/${productId}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!getRes.ok) throw new Error(`GET ${productId} failed: ${getRes.status} ${await getRes.text()}`);
  const sub = await getRes.json();

  const converted = await convertUsdPrices(token, targetUsdUnits, targetUsdNanos);
  const regionsVersion = converted.regionVersion?.version || "2026/01";

  const updatedBasePlans = (sub.basePlans || []).map((bp) => {
    if (bp.basePlanId !== basePlanId) return bp;

    const existingByRegion = new Map((bp.regionalConfigs || []).map((rc) => [rc.regionCode, rc]));
    for (const [rc, rp] of Object.entries(converted.convertedRegionPrices || {})) {
      if (TIER1_USD_REGIONS.has(rc)) {
        const prev = existingByRegion.get(rc);
        existingByRegion.set(rc, {
          regionCode: rc,
          newSubscriberAvailability: prev ? prev.newSubscriberAvailability : true,
          price: rp.price,
        });
      }
    }

    return {
      ...bp,
      regionalConfigs: Array.from(existingByRegion.values()),
      otherRegionsConfig: {
        ...(bp.otherRegionsConfig || {}),
        usdPrice: converted.convertedOtherRegionsPrice?.usdPrice || {
          currencyCode: "USD",
          units: String(targetUsdUnits),
          nanos: targetUsdNanos,
        },
        eurPrice: converted.convertedOtherRegionsPrice?.eurPrice || bp.otherRegionsConfig?.eurPrice,
        newSubscriberAvailability: true,
      },
    };
  });

  const patchRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/subscriptions/${productId}?regionsVersion.version=${encodeURIComponent(regionsVersion)}&updateMask=basePlans`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        packageName,
        productId,
        basePlans: updatedBasePlans,
      }),
    }
  );
  const patchText = await patchRes.text();
  if (!patchRes.ok) {
    throw new Error(`PATCH ${productId} failed (${patchRes.status}): ${patchText}`);
  }
  const saved = JSON.parse(patchText);
  const savedBp = (saved.basePlans || []).find((b) => b.basePlanId === basePlanId);
  const pick = (rc) => fmtPrice(savedBp?.regionalConfigs?.find((x) => x.regionCode === rc)?.price);
  console.log(`PATCH ${productId}: 200 OK | state=${savedBp?.state} | regions=${savedBp?.regionalConfigs?.length}`);
  console.log(`  BR=${pick("BR")} | US=${pick("US")} | GB=${pick("GB")} | DE=${pick("DE")} | CA=${pick("CA")} | AU=${pick("AU")} | JP=${pick("JP")} | IN=${pick("IN")}`);
  console.log(`  OTHER: USD=${fmtPrice(savedBp?.otherRegionsConfig?.usdPrice)} | EUR=${fmtPrice(savedBp?.otherRegionsConfig?.eurPrice)}`);
}

async function updateOneTimeProductUsd(token, productId, purchaseOptionId, targetUsdUnits, targetUsdNanos) {
  console.log(`\n=== Atualizando produto vitalicio ${productId} (${purchaseOptionId}) para USD ${targetUsdUnits}.${Math.round(targetUsdNanos / 1e7)} ===`);

  const getRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/oneTimeProducts/${productId}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!getRes.ok) throw new Error(`GET ${productId} failed: ${getRes.status} ${await getRes.text()}`);
  const otp = await getRes.json();

  const converted = await convertUsdPrices(token, targetUsdUnits, targetUsdNanos);
  const regionsVersion = converted.regionVersion?.version || otp.regionsVersion?.version || "2026/01";

  const updatedOptions = (otp.purchaseOptions || []).map((po) => {
    if (po.purchaseOptionId !== purchaseOptionId) return po;
    const existingByRegion = new Map(
      (po.regionalPricingAndAvailabilityConfigs || []).map((rc) => [rc.regionCode, rc])
    );
    for (const [rc, rp] of Object.entries(converted.convertedRegionPrices || {})) {
      if (TIER1_USD_REGIONS.has(rc)) {
        const prev = existingByRegion.get(rc);
        existingByRegion.set(rc, {
          regionCode: rc,
          price: rp.price,
          availability: prev?.availability || "AVAILABLE",
        });
      }
    }
    return {
      ...po,
      regionalPricingAndAvailabilityConfigs: Array.from(existingByRegion.values()),
    };
  });

  const patchRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/onetimeproducts/${productId}?regionsVersion.version=${encodeURIComponent(regionsVersion)}&updateMask=purchaseOptions`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        packageName,
        productId,
        purchaseOptions: updatedOptions,
      }),
    }
  );
  const patchText = await patchRes.text();
  if (!patchRes.ok) {
    throw new Error(`PATCH ${productId} failed (${patchRes.status}): ${patchText}`);
  }
  const saved = JSON.parse(patchText);
  const savedPo = (saved.purchaseOptions || []).find((p) => p.purchaseOptionId === purchaseOptionId);
  const list = savedPo?.regionalPricingAndAvailabilityConfigs || [];
  const pick = (rc) => fmtPrice(list.find((x) => x.regionCode === rc)?.price);
  console.log(`PATCH ${productId}: 200 OK | state=${savedPo?.state} | regions=${list.length}`);
  console.log(`  BR=${pick("BR")} | US=${pick("US")} | GB=${pick("GB")} | DE=${pick("DE")} | CA=${pick("CA")} | AU=${pick("AU")} | JP=${pick("JP")} | IN=${pick("IN")}`);
}

async function main() {
  const token = await getAccessToken();
  await updateSubscriptionUsd(token, "sweep_pro_monthly", "monthly-base", 4, 990000000);
  await updateSubscriptionUsd(token, "sweep_pro_annual", "annual-base", 29, 990000000);
  await updateOneTimeProductUsd(token, "sweep_pro_lifetime", "lifetime-buy", 49, 990000000);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
