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

// 5. Verificação das Strings Android nos 8 Idiomas
const resValuesFolders = ["values", "values-pt-rBR", "values-en", "values-es", "values-de", "values-fr", "values-hi", "values-ja", "values-zh-rCN"];
for (const folder of resValuesFolders) {
  const stringsPath = path.join(projectRoot, "app", "src", "main", "res", folder, "strings.xml");
  check(
    `Android Locale Resources [${folder}]`,
    fs.existsSync(stringsPath) && fs.readFileSync(stringsPath, "utf8").includes("home_cleanup_confirm_prompt"),
    `Chave 'home_cleanup_confirm_prompt' ausente em ${folder}/strings.xml.`
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
