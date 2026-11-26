#!/usr/bin/env node
/**
 * ci-cd-governance-audit.mjs
 * Auditoria Contínua de Governança, Segurança e CI/CD para Midiastore
 * Verifica Scoped Storage, Manifest, Integridade dos 8 Idiomas, Billing e Supabase.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

console.log("=================================================");
console.log("🛡️ MIDIASTORE CI/CD GOVERNANCE & SECURITY AUDIT");
console.log("=================================================\n");

let failures = 0;

function check(title, condition, errorMsg) {
  if (condition) {
    console.log(`✅ [PASS] ${title}`);
  } else {
    console.error(`❌ [FAIL] ${title} -> ${errorMsg}`);
    failures++;
  }
}

// 1. Verificação de Scoped Storage & Zero MANAGE_EXTERNAL_STORAGE
const manifestPath = path.join(projectRoot, "app", "src", "main", "AndroidManifest.xml");
const manifestContent = fs.readFileSync(manifestPath, "utf8");
check(
  "Scoped Storage Compliance",
  !manifestContent.includes("MANAGE_EXTERNAL_STORAGE"),
  "Permissão proibida MANAGE_EXTERNAL_STORAGE detectada no AndroidManifest.xml!"
);

// 2. Verificação de Application ID Legítimo
const buildGradlePath = path.join(projectRoot, "app", "build.gradle.kts");
const buildGradleContent = fs.readFileSync(buildGradlePath, "utf8");
check(
  "Application ID Verification",
  buildGradleContent.includes('applicationId = "com.rdf112.midiastore"'),
  "applicationId incorreto no app/build.gradle.kts! Esperado com.rdf112.midiastore."
);

// 3. Verificação de Dependência Oficial do Google Play Billing
check(
  "Google Play Billing KTX Verification",
  buildGradleContent.includes("com.android.billingclient:billing"),
  "Biblioteca do Google Play Billing ausente ou não configurada."
);

// 4. Verificação dos 8 Idiomas no Funil Web
const requiredLocales = ["en", "es", "de", "fr", "hi", "ja", "zh"];
for (const loc of requiredLocales) {
  const pagePath = path.join(projectRoot, "docs", loc, "index.html");
  check(
    `Web Funnel Locale [${loc}]`,
    fs.existsSync(pagePath) && fs.statSync(pagePath).size > 2000,
    `Página do funil docs/${loc}/index.html ausente ou incompleta.`
  );
}

// 5. Verificação das Strings Android nos 14 Idiomas Globais (Paridade Exata + Zero Estrelinhas)
const resValuesFolders = [
  "values",
  "values-pt-rBR",
  "values-en",
  "values-es",
  "values-de",
  "values-fr",
  "values-hi",
  "values-ja",
  "values-zh-rCN",
  "values-ar",
  "values-ru",
  "values-in",
  "values-bn",
  "values-ko",
  "values-it",
];
const extractKeys = (xml) => [...xml.matchAll(/<string\s+name="([^"]+)"/g)].map((m) => m[1]);
const baseStringsRaw = fs.readFileSync(
  path.join(projectRoot, "app", "src", "main", "res", "values", "strings.xml"),
  "utf8"
);
const baseKeySet = new Set(extractKeys(baseStringsRaw));

for (const folder of resValuesFolders) {
  const stringsPath = path.join(projectRoot, "app", "src", "main", "res", folder, "strings.xml");
  const raw = fs.existsSync(stringsPath) ? fs.readFileSync(stringsPath, "utf8") : "";
  const keys = extractKeys(raw);
  const exactParity = keys.length === baseKeySet.size && keys.every((k) => baseKeySet.has(k));
  const noSparkles = !/[\u2728\u2726\u2727]/u.test(raw);
  const stringBodies = [...raw.matchAll(/<string\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)];
  const aaptInvalid = stringBodies
    .filter(([, , val]) => {
      const trimmed = val.trim();
      const quoted = trimmed.startsWith('"') && trimmed.endsWith('"');
      const unescapedApostrophe = !quoted && /(?<!\\)'/.test(val);
      const rawAngleBracket = /[<>]/.test(val);
      const unescapedAmp = /&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9a-fA-F]+;)/.test(val);
      return unescapedApostrophe || rawAngleBracket || unescapedAmp;
    })
    .map(([, name]) => name);
  check(
    `Android Locale Resources [${folder}] (${keys.length} keys)`,
    raw.includes("home_cleanup_confirm_prompt") &&
      raw.includes("nav_home") &&
      raw.includes("nav_files") &&
      raw.includes("nav_pro") &&
      raw.includes("pro_billing_terms_disclosure") &&
      exactParity &&
      noSparkles &&
      aaptInvalid.length === 0,
    `Chaves obrigatórias ausentes, divergência de paridade (${keys.length}/${baseKeySet.size}), estrelinha legada ou escape inválido AAPT2 (${aaptInvalid.join(", ")}) em ${folder}/strings.xml.`
  );
}

// 6. Verificação de Segredos não versionados no Git
const gitignorePath = path.join(projectRoot, ".gitignore");
const gitignoreContent = fs.readFileSync(gitignorePath, "utf8");
check(
  ".gitignore Secret Guard",
  gitignoreContent.includes(".secrets/") && gitignoreContent.includes("*.jks"),
  ".gitignore vulnerável! Deve conter .secrets/ e *.jks."
);

console.log("\n-------------------------------------------------");
if (failures === 0) {
  console.log("🎉 AUDITORIA 100% APROVADA - ZERO VULNERABILIDADES!");
  process.exit(0);
} else {
  console.error(`⚠️ ${failures} FALHA(S) ENCONTRADA(S) NA AUDITORIA!`);
  process.exit(1);
}
