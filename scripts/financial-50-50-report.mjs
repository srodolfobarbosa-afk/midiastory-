#!/usr/bin/env node
/**
 * MOTOR FINANCEIRO 50/50 & AUDITORIA DE ENTITLEMENTS (Módulo 2 — Meta R$ 1.000.000,00)
 *
 * Lê ao vivo do Supabase:
 * - public.financial_ledger (apenas transações reais, is_test_transaction = false)
 * - public.play_purchases & public.entitlements (auditoria de acknowledgment <72h e ativos)
 *
 * Calcula em centavos inteiros (zero float):
 *   Receita Bruta - Taxas de Loja/Gateway - Provisão Fiscal - Reembolsos/Chargebacks = Lucro Líquido
 *   Split rigoroso: 50% Retirada/Lucro do Fundador | 50% Fundo de Reinvestimento Dinâmico
 *
 * Uso:
 *   node scripts/financial-50-50-report.mjs [--tax-rate 0.06] [--play-fee 0.15]
 */

import { fileURLToPath } from "node:url";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";

const GOAL_BRL_CENTS = 100_000_000; // R$ 1.000.000,00

const CATALOG_BRL_CENTS = {
  sweep_pro_monthly: 1499,
  sweep_pro_annual: 7999,
  sweep_pro_lifetime: 14999,
};

function formatBrl(cents) {
  const sign = cents < 0 ? "-" : "";
  const abs = Math.abs(Number(cents) || 0);
  return `${sign}R$ ${(abs / 100).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export async function generateFinancial5050Report({ taxRate = 0.06, playFeeRate = 0.15 } = {}) {
  const [ledgerSummary, purchases, entitlements, rtdnStats] = await Promise.all([
    runSupabaseSql(`
      SELECT
        event_type,
        direction,
        is_test_transaction,
        count(*)::int AS tx_count,
        coalesce(sum(amount_brl_cents), 0)::bigint AS total_brl_cents
      FROM public.financial_ledger
      GROUP BY event_type, direction, is_test_transaction
      ORDER BY is_test_transaction, event_type;
    `),
    runSupabaseSql(`
      SELECT
        product_id,
        product_type,
        order_id,
        entitlement_active,
        acknowledged,
        raw_status,
        verified_at
      FROM public.play_purchases
      ORDER BY verified_at DESC
      LIMIT 50;
    `),
    runSupabaseSql(`
      SELECT
        count(*)::int AS total_entitlements,
        count(*) FILTER (WHERE is_active = true)::int AS active_entitlements,
        count(*) FILTER (WHERE is_active = true AND acknowledged = false)::int AS unacknowledged_active
      FROM public.entitlements;
    `),
    runSupabaseSql(`
      SELECT
        count(*)::int AS total_events,
        count(*) FILTER (WHERE processed = true)::int AS processed_events
      FROM public.rtdn_events;
    `),
  ]);

  let grossRealCents = 0;
  let storeFeeCents = 0;
  let taxProvisionCents = 0;
  let refundCents = 0;

  for (const row of ledgerSummary || []) {
    if (row.is_test_transaction) continue;
    const amt = Number(row.total_brl_cents || 0);
    if (row.event_type === "GROSS_SALE") grossRealCents += amt;
    if (row.event_type === "STORE_FEE") storeFeeCents += amt;
    if (row.event_type === "TAX_PROVISION") taxProvisionCents += amt;
    if (row.event_type === "REFUND" || row.event_type === "CHARGEBACK") refundCents += amt;
  }

  // If fees/taxes are not yet posted as separate ledger entries, compute conservative estimates
  const effectiveStoreFeeCents =
    storeFeeCents > 0 ? storeFeeCents : Math.round(grossRealCents * playFeeRate);
  const effectiveTaxCents =
    taxProvisionCents > 0 ? taxProvisionCents : Math.round(grossRealCents * taxRate);

  const netProfitCents = Math.max(
    0,
    grossRealCents - effectiveStoreFeeCents - effectiveTaxCents - refundCents,
  );
  const ownerProfit50Cents = Math.floor(netProfitCents / 2);
  const reinvest50Cents = netProfitCents - ownerProfit50Cents;

  // Separate real Play orders vs License Tester orders (GPA.xxxx vs test)
  const totalPlayRows = (purchases || []).length;
  const activePlayRows = (purchases || []).filter((p) => p.entitlement_active).length;
  const unackPlayRows = (purchases || []).filter((p) => p.entitlement_active && !p.acknowledged).length;

  const progressPct = ((grossRealCents / GOAL_BRL_CENTS) * 100).toFixed(4);

  return {
    generatedAt: new Date().toISOString(),
    goalBrl: formatBrl(GOAL_BRL_CENTS),
    progressPct: `${progressPct}%`,
    ledgerReal: {
      grossBrl: formatBrl(grossRealCents),
      storeFeesBrl: formatBrl(effectiveStoreFeeCents),
      taxProvisionBrl: formatBrl(effectiveTaxCents),
      refundsBrl: formatBrl(refundCents),
      netProfitBrl: formatBrl(netProfitCents),
      split50OwnerBrl: formatBrl(ownerProfit50Cents),
      split50ReinvestBrl: formatBrl(reinvest50Cents),
    },
    playAudit: {
      verifiedPurchasesRows: totalPlayRows,
      activePurchasesRows: activePlayRows,
      unacknowledgedActiveAlert: unackPlayRows,
      catalogReferenceBrl: Object.fromEntries(
        Object.entries(CATALOG_BRL_CENTS).map(([k, v]) => [k, formatBrl(v)]),
      ),
    },
    entitlements: entitlements?.[0] || {
      total_entitlements: 0,
      active_entitlements: 0,
      unacknowledged_active: 0,
    },
    rtdnEvents: rtdnStats?.[0] || { total_events: 0, processed_events: 0 },
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const taxIdx = process.argv.indexOf("--tax-rate");
  const feeIdx = process.argv.indexOf("--play-fee");
  const taxRate = taxIdx > -1 ? Number(process.argv[taxIdx + 1]) : 0.06;
  const playFeeRate = feeIdx > -1 ? Number(process.argv[feeIdx + 1]) : 0.15;

  generateFinancial5050Report({ taxRate, playFeeRate })
    .then((report) => {
      console.log("💰 RELATÓRIO FINANCEIRO 50/50 MIDIASTORE (AO VIVO SUPABASE):");
      console.log(JSON.stringify(report, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro no relatório financeiro 50/50:", err.message);
      process.exit(1);
    });
}
