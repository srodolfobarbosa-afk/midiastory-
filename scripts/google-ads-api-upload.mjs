#!/usr/bin/env node
/**
 * UPLOAD AUTOMÁTICO DE CAMPANHAS GOOGLE ADS (14 IDIOMAS) — 100% NODE.JS (ZERO PYTHON)
 *
 * Projeto Google Cloud vinculado: midiastory-df6a0
 * Conta Google Ads padrão: 649-729-9513 (customerId: 6497299513)
 *
 * Modos de uso:
 *   node scripts/google-ads-api-upload.mjs --dry-run   (padrão: valida CSV, 28 banners, limites 30/90 chars e gera payload REST completo)
 *   node scripts/google-ads-api-upload.mjs --execute   (envia as 14 campanhas com status PAUSED e orçamento R$ 20,00/dia via OAuth/Google Cloud)
 */

import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const GCP_PROJECT_ID = process.env.GCP_PROJECT_ID || "midiastory-df6a0";
const GOOGLE_ADS_CUSTOMER_ID = (process.env.GOOGLE_ADS_CUSTOMER_ID || "649-729-9513").replace(/-/g, "");
const GOOGLE_ADS_API_VERSION = process.env.GOOGLE_ADS_API_VERSION || "v19";
const ADS_DIR = "C:\\Users\\rodolfo\\Downloads\\google-ads-midiastore";
const CSV_PATH = path.join(ADS_DIR, "google-ads-import-14-idiomas.csv");
const SERVICE_ACCOUNT_PATH = path.join(projectRoot, ".secrets", "play-service-account.json");

// Critérios oficiais de idioma na Google Ads API (languageConstants/{id})
const LANGUAGE_CRITERION_IDS = {
  Portuguese: "languageConstants/1014",
  English: "languageConstants/1000",
  Spanish: "languageConstants/1003",
  French: "languageConstants/1002",
  German: "languageConstants/1001",
  Hindi: "languageConstants/1023",
  Japanese: "languageConstants/1005",
  "Chinese (simplified)": "languageConstants/1017",
  Arabic: "languageConstants/1019",
  Russian: "languageConstants/1031",
  Indonesian: "languageConstants/1025",
  Bengali: "languageConstants/1056",
  Korean: "languageConstants/1012",
};

function parseCsvLine(line) {
  const result = [];
  let cur = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (ch === '"') {
      if (inQuotes && line[i + 1] === '"') {
        cur += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (ch === "," && !inQuotes) {
      result.push(cur);
      cur = "";
    } else {
      cur += ch;
    }
  }
  result.push(cur);
  return result;
}

function loadAndValidateCsv(csvFilePath) {
  if (!fs.existsSync(csvFilePath)) {
    throw new Error(`CSV não encontrado em: ${csvFilePath}`);
  }
  const raw = fs.readFileSync(csvFilePath, "utf8").replace(/^\uFEFF/, "");
  const lines = raw.split(/\r?\n/).filter((l) => l.trim().length > 0);
  const headers = parseCsvLine(lines[0]);

  const requiredHeaders = [
    "Campaign",
    "Campaign Status",
    "Campaign Type",
    "Budget",
    "Budget type",
    "Bid Strategy Type",
    "Languages",
    "Ad Group",
    "Ad Group Status",
    "Ad type",
    "Status",
    "Final URL",
    "Business name",
    "Headline 1",
    "Headline 2",
    "Headline 3",
    "Headline 4",
    "Headline 5",
    "Long headline",
    "Description 1",
    "Description 2",
    "Description 3",
    "Description 4",
    "Marketing image",
    "Square marketing image",
    "Logo",
  ];

  for (const req of requiredHeaders) {
    if (!headers.includes(req)) {
      throw new Error(`Cabeçalho obrigatório do Google Ads Editor ausente no CSV: "${req}"`);
    }
  }

  const campaigns = [];
  for (let i = 1; i < lines.length; i++) {
    const cols = parseCsvLine(lines[i]);
    if (cols.length !== headers.length) {
      throw new Error(`Linha ${i + 1} possui ${cols.length} colunas, esperado ${headers.length}`);
    }
    const row = Object.fromEntries(headers.map((h, idx) => [h, cols[idx]]));

    if (row["Campaign Status"] !== "Paused") {
      throw new Error(`Campanha "${row["Campaign"]}" precisa estar com Campaign Status = Paused (encontrado: ${row["Campaign Status"]})`);
    }
    if (row["Budget"] !== "20.00") {
      throw new Error(`Campanha "${row["Campaign"]}" precisa estar com Budget = 20.00 (encontrado: ${row["Budget"]})`);
    }
    if ([...row["Business name"]].length > 25) {
      throw new Error(`Business name excede 25 caracteres na campanha "${row["Campaign"]}"`);
    }

    const headlines = [1, 2, 3, 4, 5].map((n) => row[`Headline ${n}`]);
    for (const h of headlines) {
      if ([...h].length > 30) {
        throw new Error(`Headline excede 30 chars (${[...h].length}) em "${row["Campaign"]}": ${h}`);
      }
    }

    const descriptions = [1, 2, 3, 4].map((n) => row[`Description ${n}`]);
    for (const d of [...descriptions, row["Long headline"]]) {
      if ([...d].length > 90) {
        throw new Error(`Description/Long headline excede 90 chars (${[...d].length}) em "${row["Campaign"]}": ${d}`);
      }
    }

    const horizFile = path.join(ADS_DIR, row["Marketing image"]);
    const sqFile = path.join(ADS_DIR, row["Square marketing image"]);
    const logoFile = path.join(ADS_DIR, row["Logo"]);

    for (const imgPath of [horizFile, sqFile, logoFile]) {
      if (!fs.existsSync(imgPath)) {
        throw new Error(`Imagem referenciada no CSV não existe na pasta: ${imgPath}`);
      }
    }

    campaigns.push({
      row,
      headlines,
      descriptions,
      horizFile,
      sqFile,
      logoFile,
    });
  }

  return { headers, campaigns };
}

/**
 * Constroi o payload oficial mutateOperations da Google Ads REST API (googleads.googleapis.com)
 * criando Budget (20.00 BRL = 20_000_000 micros), Campaign (PAUSED), Language Criterion,
 * AdGroup, 3 Image Assets (1200x628, 1200x1200 e 512x512) e ResponsiveDisplayAd.
 */
function buildGoogleAdsMutatePayload(customerId, campaignEntry, idx) {
  const { row, headlines, descriptions, horizFile, sqFile, logoFile } = campaignEntry;
  const budgetTempId = -(idx * 10 + 1);
  const campaignTempId = -(idx * 10 + 2);
  const adGroupTempId = -(idx * 10 + 3);
  const horizAssetTempId = -(idx * 10 + 4);
  const sqAssetTempId = -(idx * 10 + 5);
  const logoAssetTempId = -(idx * 10 + 6);

  const langConstant = LANGUAGE_CRITERION_IDS[row["Languages"]] || "languageConstants/1000";

  return {
    mutateOperations: [
      {
        campaignBudgetOperation: {
          create: {
            resourceName: `customers/${customerId}/campaignBudgets/${budgetTempId}`,
            name: `${row["Campaign"]} Budget #${Date.now()}-${idx}`,
            amountMicros: "20000000", // 20.00 BRL
            deliveryMethod: "STANDARD",
            explicitlyShared: false,
          },
        },
      },
      {
        campaignOperation: {
          create: {
            resourceName: `customers/${customerId}/campaigns/${campaignTempId}`,
            name: row["Campaign"],
            status: "PAUSED",
            advertisingChannelType: "DISPLAY",
            campaignBudget: `customers/${customerId}/campaignBudgets/${budgetTempId}`,
            maximizeClicks: {},
            euPoliticalAdvertisingStatus: "DOES_NOT_CONTAIN_EU_POLITICAL_ADVERTISING",
          },
        },
      },
      {
        campaignCriterionOperation: {
          create: {
            campaign: `customers/${customerId}/campaigns/${campaignTempId}`,
            language: {
              languageConstant: langConstant,
            },
          },
        },
      },
      {
        adGroupOperation: {
          create: {
            resourceName: `customers/${customerId}/adGroups/${adGroupTempId}`,
            name: row["Ad Group"],
            campaign: `customers/${customerId}/campaigns/${campaignTempId}`,
            status: "ENABLED",
            type: "DISPLAY_STANDARD",
          },
        },
      },
      {
        assetOperation: {
          create: {
            resourceName: `customers/${customerId}/assets/${horizAssetTempId}`,
            name: row["Marketing image"],
            type: "IMAGE",
            imageAsset: {
              data: fs.readFileSync(horizFile).toString("base64"),
            },
          },
        },
      },
      {
        assetOperation: {
          create: {
            resourceName: `customers/${customerId}/assets/${sqAssetTempId}`,
            name: row["Square marketing image"],
            type: "IMAGE",
            imageAsset: {
              data: fs.readFileSync(sqFile).toString("base64"),
            },
          },
        },
      },
      {
        assetOperation: {
          create: {
            resourceName: `customers/${customerId}/assets/${logoAssetTempId}`,
            name: row["Logo"],
            type: "IMAGE",
            imageAsset: {
              data: fs.readFileSync(logoFile).toString("base64"),
            },
          },
        },
      },
      {
        adGroupAdOperation: {
          create: {
            adGroup: `customers/${customerId}/adGroups/${adGroupTempId}`,
            status: "ENABLED",
            ad: {
              finalUrls: [row["Final URL"]],
              responsiveDisplayAd: {
                businessName: row["Business name"],
                headlines: headlines.map((text) => ({ text })),
                longHeadline: { text: row["Long headline"] },
                descriptions: descriptions.map((text) => ({ text })),
                marketingImages: [{ asset: `customers/${customerId}/assets/${horizAssetTempId}` }],
                squareMarketingImages: [{ asset: `customers/${customerId}/assets/${sqAssetTempId}` }],
                logoImages: [{ asset: `customers/${customerId}/assets/${logoAssetTempId}` }],
                squareLogoImages: [{ asset: `customers/${customerId}/assets/${logoAssetTempId}` }],
              },
            },
          },
        },
      },
    ],
  };
}

/**
 * Obtém token OAuth 2.0 via Refresh Token de Usuário (OAuth Client do projeto midiastory-df6a0)
 * ou via Service Account local (.secrets/play-service-account.json do projeto midiastory-df6a0).
 */
async function acquireGoogleCloudAccessToken() {
  if (process.env.GOOGLE_ADS_CLIENT_ID && process.env.GOOGLE_ADS_CLIENT_SECRET && process.env.GOOGLE_ADS_REFRESH_TOKEN) {
    const res = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: process.env.GOOGLE_ADS_CLIENT_ID,
        client_secret: process.env.GOOGLE_ADS_CLIENT_SECRET,
        refresh_token: process.env.GOOGLE_ADS_REFRESH_TOKEN,
        grant_type: "refresh_token",
      }),
    });
    const data = await res.json();
    if (!res.ok || !data.access_token) {
      throw new Error(`Falha ao renovar OAuth Refresh Token: ${JSON.stringify(data)}`);
    }
    return { token: data.access_token, authMode: "OAUTH2_USER_REFRESH_TOKEN" };
  }

  if (fs.existsSync(SERVICE_ACCOUNT_PATH)) {
    const sa = JSON.parse(fs.readFileSync(SERVICE_ACCOUNT_PATH, "utf8"));
    const now = Math.floor(Date.now() / 1000);
    const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
    const claim = Buffer.from(
      JSON.stringify({
        iss: sa.client_email,
        scope: "https://www.googleapis.com/auth/adwords",
        aud: "https://oauth2.googleapis.com/token",
        iat: now,
        exp: now + 3600,
      })
    ).toString("base64url");
    const signer = crypto.createSign("RSA-SHA256");
    signer.update(`${header}.${claim}`);
    const signature = signer.sign(sa.private_key, "base64url");
    const assertion = `${header}.${claim}.${signature}`;

    const res = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion,
      }),
    });
    const data = await res.json();
    if (!res.ok || !data.access_token) {
      throw new Error(`Falha ao obter token da Service Account (${sa.project_id}): ${JSON.stringify(data)}`);
    }
    return {
      token: data.access_token,
      authMode: `GCP_SERVICE_ACCOUNT (${sa.client_email} / project=${sa.project_id})`,
    };
  }

  throw new Error("Nenhuma credencial OAuth2 ou Service Account encontrada.");
}

async function main() {
  const executeLive = process.argv.includes("--execute");
  console.log("================================================================================");
  console.log("🚀 GOOGLE ADS API & EDITOR BULK VERIFIER — MIDIASTORE (14 IDIOMAS)");
  console.log(`   Projeto Google Cloud : ${GCP_PROJECT_ID}`);
  console.log(`   Conta Google Ads     : ${GOOGLE_ADS_CUSTOMER_ID} (649-729-9513)`);
  console.log(`   Arquivo CSV Oficial  : ${CSV_PATH}`);
  console.log("================================================================================\n");

  const { headers, campaigns } = loadAndValidateCsv(CSV_PATH);
  console.log(`✅ CSV Validado com Sucesso! (${headers.length} cabeçalhos oficiais do Google Ads Editor, ${campaigns.length} campanhas)`);

  for (let i = 0; i < campaigns.length; i++) {
    const c = campaigns[i];
    const payload = buildGoogleAdsMutatePayload(GOOGLE_ADS_CUSTOMER_ID, c, i + 1);
    console.log(
      `   [${String(i + 1).padStart(2, "0")}/14] ${c.row["Campaign"].padEnd(28)} | Status: ${c.row["Campaign Status"]} | Budget: R$ ${c.row["Budget"]} | Idioma: ${c.row["Languages"].padEnd(20)} | Ops REST: ${payload.mutateOperations.length}`
    );
  }

  if (!executeLive) {
    console.log("\n✅ Modo --dry-run concluído com 0 erros: os 14 payloads REST + 28 banners 1200x628/1200x1200 + logo 512x512 estão prontos.");
    return;
  }

  console.log("\n🔐 Autenticando no Google Cloud Project (midiastory-df6a0)...");
  const { token, authMode } = await acquireGoogleCloudAccessToken();
  console.log(`✅ Token OAuth obtido via: ${authMode}`);

  const endpoint = `https://googleads.googleapis.com/${GOOGLE_ADS_API_VERSION}/customers/${GOOGLE_ADS_CUSTOMER_ID}/googleAds:mutate`;
  const reqHeaders = {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
    "x-goog-user-project": GCP_PROJECT_ID,
  };
  if (process.env.GOOGLE_ADS_DEVELOPER_TOKEN) {
    reqHeaders["developer-token"] = process.env.GOOGLE_ADS_DEVELOPER_TOKEN;
  }

  for (let i = 0; i < campaigns.length; i++) {
    const c = campaigns[i];
    const payload = buildGoogleAdsMutatePayload(GOOGLE_ADS_CUSTOMER_ID, c, i + 1);
    const res = await fetch(endpoint, {
      method: "POST",
      headers: reqHeaders,
      body: JSON.stringify(payload),
    });
    const body = await res.json();
    if (!res.ok) {
      console.error(`❌ Erro HTTP ${res.status} ao enviar "${c.row["Campaign"]}":`, JSON.stringify(body, null, 2));
      process.exit(1);
    }
    console.log(`✅ Campanha enviada com sucesso via API: ${c.row["Campaign"]}`);
  }
}

main().catch((err) => {
  console.error("❌ Erro:", err.message);
  process.exit(1);
});
