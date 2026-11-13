import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const key = JSON.parse(fs.readFileSync(path.join(projectRoot, ".secrets", "play-service-account.json"), "utf8"));
const packageName = "com.rdf112.midiastore";

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

async function convertPrices(token, units, nanos) {
  const res = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/pricing:convertRegionPrices`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        price: {
          currencyCode: "BRL",
          units: String(units),
          nanos: nanos,
        },
      }),
    }
  );
  const text = await res.text();
  if (!res.ok) {
    throw new Error(`convertRegionPrices failed (${res.status}): ${text}`);
  }
  return JSON.parse(text);
}

async function createAndActivateSubscription(token, productId, basePlanId, billingPeriod, titlePt, descPt, titleEn, descEn, unitsBrl, nanosBrl) {
  console.log(`\n=== Configurando assinatura: ${productId} ===`);
  const converted = await convertPrices(token, unitsBrl, nanosBrl);
  const regionsVersion = converted.regionVersion?.version || "2026/01";
  const regionalConfigs = Object.values(converted.convertedRegionPrices || {}).map((rp) => ({
    regionCode: rp.regionCode,
    newSubscriberAvailability: true,
    price: rp.price,
  }));

  const subBody = {
    packageName,
    productId,
    listings: [
      {
        languageCode: "pt-BR",
        title: titlePt,
        description: descPt,
        benefits: [
          "Limpeza de duplicatas e antigos",
          "Exportar relatorio de limpeza",
          "Sugestoes de apps e arquivos",
        ],
      },
      {
        languageCode: "en-US",
        title: titleEn,
        description: descEn,
        benefits: [
          "Duplicate & old file cleanup",
          "Export full cleanup reports",
          "Unused app & file browser tools",
        ],
      },
    ],
    basePlans: [
      {
        basePlanId,
        autoRenewingBasePlanType: {
          billingPeriodDuration: billingPeriod,
          gracePeriodDuration: "P3D",
          resubscribeState: "RESUBSCRIBE_STATE_ACTIVE",
          prorationMode: "SUBSCRIPTION_PRORATION_MODE_CHARGE_ON_NEXT_BILLING_DATE",
          legacyCompatible: true,
        },
        regionalConfigs,
        otherRegionsConfig: {
          usdPrice: converted.convertedOtherRegionsPrice?.usdPrice || { currencyCode: "USD", units: "2", nanos: 990000000 },
          eurPrice: converted.convertedOtherRegionsPrice?.eurPrice || { currencyCode: "EUR", units: "2", nanos: 990000000 },
          newSubscriberAvailability: true,
        },
      },
    ],
  };

  const getRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/subscriptions/${productId}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );

  let createRes;
  if (getRes.status === 404) {
    createRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/subscriptions?productId=${productId}&regionsVersion.version=${encodeURIComponent(regionsVersion)}`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify(subBody),
      }
    );
  } else {
    createRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/subscriptions/${productId}?regionsVersion.version=${encodeURIComponent(regionsVersion)}&updateMask=listings,basePlans`,
      {
        method: "PATCH",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify(subBody),
      }
    );
  }

  const createText = await createRes.text();
  console.log(`Status criacao/atualizacao ${productId}:`, createRes.status);
  if (!createRes.ok) {
    console.log("Erro:", createText);
    return;
  }

  const actRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/subscriptions/${productId}/basePlans/${basePlanId}:activate`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: "{}",
    }
  );
  console.log(`Status ativacao basePlan (${basePlanId}):`, actRes.status, await actRes.text());
}

async function createAndActivateOneTimeProduct(token, productId, optionId, titlePt, descPt, titleEn, descEn, unitsBrl, nanosBrl) {
  console.log(`\n=== Configurando produto vitalicio (One-Time Product): ${productId} ===`);
  const converted = await convertPrices(token, unitsBrl, nanosBrl);
  const regionsVersion = converted.regionVersion?.version || "2026/01";

  const regionalPricingAndAvailabilityConfigs = Object.values(converted.convertedRegionPrices || {}).map((rp) => ({
    regionCode: rp.regionCode,
    price: rp.price,
    availability: "AVAILABLE",
  }));

  const otpBody = {
    packageName,
    productId,
    listings: [
      {
        languageCode: "pt-BR",
        title: titlePt,
        description: descPt,
      },
      {
        languageCode: "en-US",
        title: titleEn,
        description: descEn,
      },
    ],
    purchaseOptions: [
      {
        purchaseOptionId: optionId,
        buyOption: {
          legacyCompatible: true,
        },
        regionalPricingAndAvailabilityConfigs,
      },
    ],
  };

  const patchRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/oneTimeProducts/${productId}?regionsVersion.version=${encodeURIComponent(regionsVersion)}&allowMissing=true&updateMask=listings,purchaseOptions`,
    {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify(otpBody),
    }
  );
  const patchText = await patchRes.text();
  console.log(`Status criacao/atualizacao ${productId}:`, patchRes.status);
  if (!patchRes.ok) {
    console.log("Detalhes:", patchText);
    return;
  }

  const actRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/oneTimeProducts/${productId}/purchaseOptions:batchUpdateStates`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        requests: [
          {
            activatePurchaseOptionRequest: {
              packageName,
              productId,
              purchaseOptionId: optionId,
            },
          },
        ],
      }),
    }
  );
  console.log(`Status ativacao purchaseOption (${optionId}):`, actRes.status, await actRes.text());
}

async function main() {
  const token = await getAccessToken();

  await createAndActivateSubscription(
    token,
    "sweep_pro_monthly",
    "monthly-base",
    "P1M",
    "Midiastore Pro Mensal",
    "Acesso mensal completo as ferramentas Pro de limpeza e relatorios do Midiastore.",
    "Midiastore Pro Monthly",
    "Full monthly access to Midiastore Pro cleanup and report tools.",
    14,
    990000000
  );

  await createAndActivateSubscription(
    token,
    "sweep_pro_annual",
    "annual-base",
    "P1Y",
    "Midiastore Pro Anual",
    "Acesso anual completo as ferramentas Pro de limpeza e relatorios do Midiastore.",
    "Midiastore Pro Annual",
    "Full annual access to Midiastore Pro cleanup and report tools.",
    79,
    990000000
  );

  await createAndActivateOneTimeProduct(
    token,
    "sweep_pro_lifetime",
    "lifetime-buy",
    "Midiastore Pro Vitalicio",
    "Acesso vitalicio em pagamento unico as ferramentas Pro do Midiastore.",
    "Midiastore Pro Lifetime",
    "One-time purchase for lifetime access to Midiastore Pro tools.",
    149,
    990000000
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
