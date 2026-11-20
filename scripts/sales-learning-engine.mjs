#!/usr/bin/env node
/**
 * MOTOR DE APRENDIZADO AUTÔNOMO DE VENDAS — THOMPSON SAMPLING + GOODHART GUARD (Módulo 7)
 *
 * 1. Lê variantes ativas em public.sales_experiments no Supabase.
 * 2. Calcula taxa de conversão, taxa de estorno e aplica obrigatoriamente o Guardrail da Lei de Goodhart
 *    (bloqueia/pausa variantes com refundRatePct > 0.50%).
 * 3. Roda 2.000 simulações Monte Carlo Beta(alpha, beta) (Thompson Sampling em Node.js puro)
 *    para estimar a probabilidade real de cada variante ser a campeã de vendas.
 * 4. Decompõe matematicamente a meta de R$ 1.000.000,00 em unidades reais (mix BR + Global Tier 1).
 */

import { fileURLToPath } from "node:url";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";
import { evaluateGoodhartGuard } from "./lib/goodhart-guard.mjs";

function normalSample() {
  const u1 = Math.max(1e-12, Math.random());
  const u2 = Math.random();
  return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
}

function gammaSample(shape) {
  if (shape < 1) return gammaSample(shape + 1) * Math.pow(Math.random(), 1 / shape);
  const d = shape - 1 / 3;
  const c = 1 / Math.sqrt(9 * d);
  while (true) {
    const x = normalSample();
    const v = Math.pow(1 + c * x, 3);
    if (v <= 0) continue;
    const u = Math.random();
    if (u < 1 - 0.0331 * x * x * x * x) return d * v;
    if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) return d * v;
  }
}

export function betaSample(alpha, beta) {
  const x = gammaSample(Math.max(0.5, alpha));
  const y = gammaSample(Math.max(0.5, beta));
  return x / (x + y);
}

export function computeThompsonWinProbs(variants, trials = 2000) {
  const wins = Object.fromEntries(variants.map((v) => [v.variant_key, 0]));
  for (let i = 0; i < trials; i++) {
    let bestKey = variants[0].variant_key;
    let bestVal = -1;
    for (const v of variants) {
      const sample = betaSample(Number(v.alpha) || 1, Number(v.beta) || 1);
      if (sample > bestVal) {
        bestVal = sample;
        bestKey = v.variant_key;
      }
    }
    wins[bestKey] += 1;
  }
  return Object.fromEntries(
    Object.entries(wins).map(([k, w]) => [k, Number(((w / trials) * 100).toFixed(2))]),
  );
}

export function computeGoalDecomposition() {
  const goalBrl = 1_000_000;
  // Google Play 15% tier -> 85% net before taxes
  return {
    goalGrossBrl: goalBrl,
    pureAnnualBrUnits: Math.ceil(goalBrl / 79.99),
    pureLifetimeBrUnits: Math.ceil(goalBrl / 149.99),
    realisticGlobalMix: {
      note: "Mix conservador: 40% Tier 1 (USD/EUR ticket médio R$ 180) + 60% BR/LATAM/IN (ticket médio R$ 85) -> blended R$ 123/venda",
      blendedTicketBrl: 123,
      totalPayingCustomersNeeded: Math.ceil(goalBrl / 123),
      at2PctInstallToPaidConversionInstallsNeeded: Math.ceil((goalBrl / 123) / 0.02),
    },
  };
}

export async function runSalesLearningCycle() {
  const rows = await runSupabaseSql(`
    SELECT experiment_key, variant_key, locale, highlighted_sku, status,
           impressions, clicks, purchases, refunds, alpha, beta
    FROM public.sales_experiments
    ORDER BY locale, variant_key;
  `);

  const byLocale = {};
  for (const r of rows || []) {
    byLocale[r.locale] = byLocale[r.locale] || [];
    const impressions = Number(r.impressions || 0);
    const purchases = Number(r.purchases || 0);
    const refunds = Number(r.refunds || 0);
    const convPct = impressions > 0 ? Number(((purchases / impressions) * 100).toFixed(2)) : 0;
    const refundRatePct = purchases > 0 ? Number(((refunds / purchases) * 100).toFixed(2)) : 0;

    const goodhart = evaluateGoodhartGuard("PAYWALL_CONVERSION", {
      chargebackRatePct: refundRatePct,
    });

    byLocale[r.locale].push({
      ...r,
      impressions,
      purchases,
      refunds,
      convPct,
      refundRatePct,
      goodhartApproved: goodhart.approved,
    });
  }

  const localeAnalysis = {};
  for (const [loc, list] of Object.entries(byLocale)) {
    const eligible = list.filter((v) => v.status !== "PAUSED" && v.goodhartApproved);
    const winProbPct = eligible.length > 0 ? computeThompsonWinProbs(eligible, 2000) : {};
    localeAnalysis[loc] = {
      variants: list.map((v) => ({
        variant: v.variant_key,
        sku: v.highlighted_sku,
        status: v.status,
        impressions: v.impressions,
        purchases: v.purchases,
        convPct: `${v.convPct}%`,
        refundRatePct: `${v.refundRatePct}%`,
        goodhartApproved: v.goodhartApproved,
        thompsonWinProbPct: `${winProbPct[v.variant_key] ?? 0}%`,
      })),
    };
  }

  return {
    evaluatedAt: new Date().toISOString(),
    goalMath: computeGoalDecomposition(),
    experiments: localeAnalysis,
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  runSalesLearningCycle()
    .then((report) => {
      console.log("🧠 MOTOR DE APRENDIZADO AUTÔNOMO DE VENDAS (THOMPSON SAMPLING + GOODHART):");
      console.log(JSON.stringify(report, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro no sales-learning-engine:", err.message);
      process.exit(1);
    });
}
