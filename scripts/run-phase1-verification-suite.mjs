#!/usr/bin/env node
/**
 * SUITE UNIFICADA DE VERIFICAÇÃO — FASE 1 + MÓDULO 4 (Midiastore & NEXO)
 *
 * Executa em um único comando todos os checks reais de produção e segurança:
 * 1. PenTest RLS (anon key bloqueada nas 8 tabelas sensíveis)
 * 2. Google Play Voided Purchases API (anti-fraude / estornos)
 * 3. Consolidador Cognitivo & Knowledge Graph (RRF + 2-hop + DB < 400 MB)
 * 4. Relatório Financeiro 50/50 (ledger real vs teste + unacknowledged alert)
 * 5. Auditoria Growth / ASO / AppSec / Lei de Goodhart
 */

import { runRlsPentest } from "./security-rls-pentest.mjs";
import { runVoidedPurchasesGuard } from "./play-voided-purchases-guard.mjs";
import { runMemoryConsolidation } from "./memory-consolidator.mjs";
import { generateFinancial5050Report } from "./financial-50-50-report.mjs";
import { runAsoVitalsAudit } from "./aso-vitals-audit.mjs";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";

async function main() {
  const startedAt = Date.now();
  const [rls, voided, memory, finance, growth, betaTesters] = await Promise.all([
    runRlsPentest(),
    runVoidedPurchasesGuard({ days: 28 }),
    runMemoryConsolidation({ limit: 80 }),
    generateFinancial5050Report(),
    runAsoVitalsAudit(),
    runSupabaseSql(`SELECT email, created_at FROM public.beta_testers ORDER BY created_at DESC LIMIT 20;`),
  ]);

  const summary = {
    verifiedAt: new Date().toISOString(),
    durationMs: Date.now() - startedAt,
    rlsPentest: { allPassed: rls.allPassed, totalTables: rls.totalTables, failures: rls.failures },
    voidedPurchases: { found: voided.voidedPurchasesFound, revoked: voided.revokedAndLogged },
    knowledgeGraph: {
      activeMemories: memory.stats.active_memories,
      nodes: memory.stats.kg_nodes,
      edges: memory.stats.kg_edges,
      dbSize: memory.stats.db_size,
      pctOf500mb: memory.stats.pct_of_500mb,
    },
    finance5050: {
      grossRealBrl: finance.ledgerReal.grossBrl,
      verifiedPlayRows: finance.playAudit.verifiedPurchasesRows,
      unacknowledgedAlert: finance.playAudit.unacknowledgedActiveAlert,
    },
    growthAndAppSec: {
      scopedStorageClean: growth.appSec.scopedStorageClean,
      r8ObfuscationHardened: growth.appSec.r8ObfuscationHardened,
      asoLocaleCount: growth.aso.localeCount,
      goodhartDbApproved: growth.goodhartChecks.dbStorage.approved,
    },
    webBetaTesters: (betaTesters || []).map((r) => ({ email: r.email, created_at: r.created_at })),
  };

  console.log("🚀 VERIFICAÇÃO UNIFICADA MIDIASTORE / NEXO:");
  console.log(JSON.stringify(summary, null, 2));

  if (!rls.allPassed || !growth.appSec.scopedStorageClean || !growth.goodhartChecks.dbStorage.approved) {
    console.error("❌ Falha em um ou mais guardrails críticos!");
    process.exit(1);
  }
  console.log("✅ TODOS OS GUARDRAILS CRÍTICOS APROVADOS!");
}

main().catch((err) => {
  console.error("❌ Erro na suite unificada:", err.message);
  process.exit(1);
});
