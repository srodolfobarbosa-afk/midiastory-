#!/usr/bin/env node
/**
 * DIAGNÓSTICO AUTOMÁTICO DE GARGALO DE VENDAS — CRO DOCTOR (Módulo 7B)
 *
 * Lê ao vivo do Supabase:
 * - public.product_events (funil completo: page_view -> play_store_click -> install -> scan -> paywall -> purchase)
 * - public.financial_ledger (receita real + estornos para checagem Goodhart)
 * - public.play_purchases (compras validadas na Play API)
 *
 * Calcula taxas de conversão etapa a etapa, identifica automaticamente o "Gargalo Número 1"
 * e sugere a próxima hipótese concreta de experimento A/B.
 */

import { fileURLToPath } from "node:url";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";
import { evaluateGoodhartGuard } from "./lib/goodhart-guard.mjs";

function pct(num, den) {
  if (!den || den <= 0) return 0;
  return Number(((num / den) * 100).toFixed(2));
}

export function diagnoseBottleneck(counts, refundRatePct) {
  const {
    pageViews,
    playClicks,
    installs,
    firstScans,
    paywallViews,
    purchases,
  } = counts;

  const ctr = pct(playClicks, pageViews);
  const scanActivation = pct(firstScans, installs);
  const paywallReach = pct(paywallViews, firstScans);
  const paywallToPurchase = pct(purchases, paywallViews);

  const goodhart = evaluateGoodhartGuard("PAYWALL_CONVERSION", {
    chargebackRatePct: refundRatePct,
  });

  if (!goodhart.approved) {
    return {
      bottleneckNumber1: "QUALIDADE / ESTORNO ACIMA DO LIMITE GOODHART",
      severity: "CRITICAL",
      diagnosis: `Taxa de estorno/chargeback (${refundRatePct}%) violou o teto de 0,50%. Pausar otimizações agressivas de paywall imediatamente.`,
      nextHypothesis: "Reforçar clareza de preço/renovação no paywall e auditar causa raiz dos estornos antes de escalar tráfego.",
      goodhart,
    };
  }

  if (pageViews < 50) {
    return {
      bottleneckNumber1: "TOPO DE FUNIL (TRÁFEGO INSUFICIENTE PARA SIGNIFICÂNCIA ESTATÍSTICA)",
      severity: "HIGH",
      diagnosis: `Apenas ${pageViews} page_views rastreados nas páginas pSEO. Ainda não há volume suficiente para otimizar micro-conversão — prioridade máxima é distribuição (Push + Pull).`,
      nextHypothesis: "Publicar os 2 Reels já prontos (PT-BR e EN-US) no TikTok/Reels/Shorts, enviar sitemap.xml no Google Search Console e executar o Backlink Blitz Pack (Product Hunt + AlternativeTo + Reddit) até atingir >= 200 visitas/semana.",
      goodhart,
    };
  }

  if (ctr < 8.0) {
    return {
      bottleneckNumber1: "LANDING PAGE -> GOOGLE PLAY (CTR BAIXO)",
      severity: "HIGH",
      diagnosis: `CTR de página para Play Store está em ${ctr}% (meta saudável >= 8%). Muita visita com pouco clique indica headline ou CTA fraco.`,
      nextHypothesis: "Ativar Variante B de landing_hero_v1 em docs/ab-config.json (foco em '5,9 MB que nunca apaga fotos sem permissão') e mover prova visual antes/depois para acima da dobra.",
      goodhart,
    };
  }

  if (installs >= 20 && scanActivation < 60.0) {
    return {
      bottleneckNumber1: "INSTALAÇÃO -> 1º SCAN (FRICÇÃO NO ONBOARDING / PERMISSÃO)",
      severity: "HIGH",
      diagnosis: `Apenas ${scanActivation}% dos usuários que abriram o app completaram o 1º scan (meta >= 60%).`,
      nextHypothesis: "Simplificar texto do pedido de permissão de mídia deixando claro que a análise é 100% local e nenhum arquivo é apagado automaticamente.",
      goodhart,
    };
  }

  if (paywallViews >= 20 && paywallToPurchase < 3.0) {
    return {
      bottleneckNumber1: "PAYWALL -> COMPRA (OBJEÇÃO DE PREÇO OU CONFIANÇA)",
      severity: "HIGH",
      diagnosis: `Conversão de Paywall para Compra está em ${paywallToPurchase}% (meta saudável >= 3%–5%).`,
      nextHypothesis: "Testar Variante B/C em app_paywall_v1 (destacar economia de 55% no Anual + âncora Vitalício + selo 'Cancele em 1 toque na Google Play') exibido imediatamente após o Momento Aha! do 1º scan.",
      goodhart,
    };
  }

  return {
    bottleneckNumber1: "ESCALA DE AQUISIÇÃO (FUNIL SAUDÁVEL)",
    severity: "LOW",
    diagnosis: "Nenhuma quebra crítica nas taxas atuais. Escalar canais vencedores de Push/Pull mantendo monitoramento Goodhart.",
    nextHypothesis: "Expandir páginas pSEO para DE-DE, FR-FR, ID e AR e aumentar cadência de Reels/Shorts nos ganchos de maior CTR.",
    goodhart,
  };
}

export async function runSalesFunnelDoctor() {
  const [eventRows, ledgerRows, playRows] = await Promise.all([
    runSupabaseSql(`
      SELECT event_name, COUNT(*)::int AS total_events, COUNT(DISTINCT install_hash)::int AS unique_users
      FROM public.product_events
      GROUP BY event_name;
    `),
    runSupabaseSql(`
      SELECT event_type, COUNT(*)::int AS cnt, COALESCE(SUM(amount_brl_cents), 0)::bigint AS total_brl_cents
      FROM public.financial_ledger
      WHERE is_test_transaction = false
      GROUP BY event_type;
    `),
    runSupabaseSql(`
      SELECT COUNT(*)::int AS verified_rows, COUNT(*) FILTER (WHERE entitlement_active = true)::int AS active_rows
      FROM public.play_purchases;
    `),
  ]);

  const byEvent = Object.fromEntries((eventRows || []).map((r) => [r.event_name, Number(r.total_events || 0)]));
  const counts = {
    pageViews: byEvent.page_view || 0,
    playClicks: byEvent.play_store_click || 0,
    installs: byEvent.install_first_open || 0,
    firstScans: byEvent.first_scan_completed || 0,
    paywallViews: byEvent.paywall_viewed || 0,
    purchases: byEvent.purchase_completed || 0,
  };

  let realSalesCount = 0;
  let realRefundsCount = 0;
  let realGrossCents = 0;
  for (const r of ledgerRows || []) {
    if (r.event_type === "GROSS_SALE") {
      realSalesCount += Number(r.cnt || 0);
      realGrossCents += Number(r.total_brl_cents || 0);
    }
    if (r.event_type === "REFUND" || r.event_type === "CHARGEBACK") {
      realRefundsCount += Number(r.cnt || 0);
    }
  }
  const refundRatePct = pct(realRefundsCount, realSalesCount);

  const rates = {
    pageToPlayClickPct: `${pct(counts.playClicks, counts.pageViews)}%`,
    installToFirstScanPct: `${pct(counts.firstScans, counts.installs)}%`,
    scanToPaywallPct: `${pct(counts.paywallViews, counts.firstScans)}%`,
    paywallToPurchasePct: `${pct(counts.purchases, counts.paywallViews)}%`,
    refundRatePct: `${refundRatePct}%`,
  };

  const verdict = diagnoseBottleneck(counts, refundRatePct);

  return {
    diagnosedAt: new Date().toISOString(),
    funnelCounts: counts,
    conversionRates: rates,
    realFinance: {
      realSalesCount,
      realGrossBrl: `R$ ${(realGrossCents / 100).toLocaleString("pt-BR", { minimumFractionDigits: 2 })}`,
      playVerifiedRowsIncludingTest: playRows?.[0]?.verified_rows || 0,
    },
    ...verdict,
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  runSalesFunnelDoctor()
    .then((report) => {
      console.log("🩺 SALES FUNNEL DOCTOR — DIAGNÓSTICO AUTOMÁTICO DE GARGALO:");
      console.log(JSON.stringify(report, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro no sales-funnel-doctor:", err.message);
      process.exit(1);
    });
}
