#!/usr/bin/env node
/**
 * AUDITOR DE GROWTH, ANDROID VITALS, ASO, APPSEC & GOODHART (Módulo 4A + 4B + 4C)
 *
 * Consolida em um único relatório verídico (zero dados inventados):
 * 1. AppSec estático (app/proguard-rules.pro + AndroidManifest.xml sem MANAGE_EXTERNAL_STORAGE)
 * 2. Cobertura de idiomas ASO no app (app/src/main/res/values-*)
 * 3. Telemetria ao vivo do Supabase (banco, coortes v_funnel_unit_economics, beta_testers)
 * 4. Avaliação automática dos Guardrails da Lei de Goodhart
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";
import { evaluateGoodhartGuard } from "./lib/goodhart-guard.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

function auditAppSecStatic() {
  const proguardPath = path.join(projectRoot, "app", "proguard-rules.pro");
  const manifestPath = path.join(projectRoot, "app", "src", "main", "AndroidManifest.xml");

  const proguard = fs.existsSync(proguardPath) ? fs.readFileSync(proguardPath, "utf8") : "";
  const manifest = fs.existsSync(manifestPath) ? fs.readFileSync(manifestPath, "utf8") : "";

  const hasOverbroadKeep = /-keep\s+class\s+com\.rdf112\.sweep\.\*\*/.test(proguard);
  const hasForbiddenStoragePerm = /MANAGE_EXTERNAL_STORAGE/.test(manifest);

  return {
    scopedStorageClean: !hasForbiddenStoragePerm,
    r8ObfuscationHardened: !hasOverbroadKeep,
    findings: [
      ...(hasForbiddenStoragePerm
        ? ["CRITICAL: MANAGE_EXTERNAL_STORAGE detectado no AndroidManifest.xml!"]
        : []),
      ...(hasOverbroadKeep
        ? [
            "HIGH (AppSec v2.7.5): app/proguard-rules.pro contém '-keep class com.rdf112.sweep.** { *; }', desativando ofuscação R8 do app. Restringir apenas a classes serializadas/Entrypoints + integrar Google Play Integrity API no próximo release.",
          ]
        : []),
    ],
  };
}

function auditAsoLocales() {
  const resDir = path.join(projectRoot, "app", "src", "main", "res");
  if (!fs.existsSync(resDir)) return { localeCount: 0, locales: [] };

  const dirs = fs
    .readdirSync(resDir, { withFileTypes: true })
    .filter((d) => d.isDirectory() && (d.name === "values" || d.name.startsWith("values-")))
    .filter((d) => fs.existsSync(path.join(resDir, d.name, "strings.xml")))
    .map((d) => (d.name === "values" ? "en (default)" : d.name.replace("values-", "")));

  return {
    localeCount: dirs.length,
    locales: dirs.sort(),
  };
}

export async function runAsoVitalsAudit() {
  const appSec = auditAppSecStatic();
  const aso = auditAsoLocales();

  const [dbStats, funnelRows, testerStats] = await Promise.all([
    runSupabaseSql(`
      SELECT
        round((pg_database_size(current_database())::numeric / (1024 * 1024)), 2) AS db_size_mb,
        (SELECT count(*)::int FROM public.memories WHERE is_active = true) AS active_memories,
        (SELECT count(*)::int FROM public.kg_nodes) AS kg_nodes,
        (SELECT count(*)::int FROM public.kg_edges) AS kg_edges;
    `),
    runSupabaseSql(`SELECT * FROM public.v_funnel_unit_economics LIMIT 7;`),
    runSupabaseSql(`SELECT count(*)::int AS web_beta_testers FROM public.beta_testers;`),
  ]);

  const dbSizeMb = Number(dbStats?.[0]?.db_size_mb || 0);
  const goodhartDb = evaluateGoodhartGuard("DB_STORAGE_OPT", {
    dbSizeMb,
    deletedCriticalMemories: 0,
    ledgerMutations: 0,
  });

  return {
    auditedAt: new Date().toISOString(),
    appSec,
    aso,
    supabaseHealth: {
      dbSizeMb,
      activeMemories: dbStats?.[0]?.active_memories || 0,
      kgNodes: dbStats?.[0]?.kg_nodes || 0,
      kgEdges: dbStats?.[0]?.kg_edges || 0,
      webBetaTesters: testerStats?.[0]?.web_beta_testers || 0,
    },
    unitEconomicsLast7Days: funnelRows || [],
    goodhartChecks: {
      dbStorage: goodhartDb,
    },
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  runAsoVitalsAudit()
    .then((report) => {
      console.log("📈 AUDITORIA GROWTH / VITALS / ASO / APPSEC / GOODHART:");
      console.log(JSON.stringify(report, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro na auditoria:", err.message);
      process.exit(1);
    });
}
