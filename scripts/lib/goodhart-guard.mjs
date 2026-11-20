/**
 * GUARDRAIL DA LEI DE GOODHART PARA AGENTES AUTÔNOMOS (Módulo 4C)
 *
 * "Quando uma medida se torna uma meta, ela deixa de ser uma boa medida."
 * Nenhuma otimização autônoma é aprovada sem respeitar suas métricas de contrapeso.
 */

export const GOODHART_POLICIES = {
  PAYWALL_CONVERSION: {
    goal: "Maximizar taxa de conversão do paywall Pro rumo a R$ 1.000.000,00",
    counterweights: [
      { metric: "chargebackRatePct", max: 0.5, label: "Chargeback/Estorno <= 0.50%" },
      { metric: "playRating", min: 4.5, label: "Nota na Play Store >= 4.5" },
      { metric: "crashFreePct", min: 99.5, label: "Crash-Free Rate >= 99.5%" },
    ],
  },
  DB_STORAGE_OPT: {
    goal: "Manter banco Supabase < 400 MB (teto gratuito 500 MB)",
    counterweights: [
      { metric: "dbSizeMb", max: 400, label: "Tamanho DB <= 400 MB" },
      { metric: "deletedCriticalMemories", max: 0, label: "Zero exclusões de memórias críticas" },
      { metric: "ledgerMutations", max: 0, label: "Zero mutações em financial_ledger" },
    ],
  },
  EDGE_LATENCY_OPT: {
    goal: "Reduzir latência p95 das Edge Functions (< 500 ms warm)",
    counterweights: [
      { metric: "rlsExposedTables", max: 0, label: "Zero tabelas expostas via anon key" },
      { metric: "failClosedBypassed", max: 0, label: "Zero relaxamento de validação fail-closed" },
    ],
  },
  ANDROID_GROWTH_SCALE: {
    goal: "Escalar instalações orgânicas e pagas na Google Play",
    counterweights: [
      { metric: "crashFreePct", min: 99.5, label: "Android Vitals Crash-Free >= 99.5%" },
      { metric: "anrRatePct", max: 0.47, label: "Android Vitals ANR <= 0.47%" },
    ],
  },
};

export function evaluateGoodhartGuard(policyName, metrics = {}) {
  const policy = GOODHART_POLICIES[policyName];
  if (!policy) {
    throw new Error(`Política Goodhart desconhecida: ${policyName}`);
  }

  const violations = [];
  for (const cw of policy.counterweights) {
    const val = metrics[cw.metric];
    if (val === undefined || val === null) continue;
    const num = Number(val);
    if (cw.max !== undefined && num > cw.max) {
      violations.push({
        metric: cw.metric,
        observed: num,
        limit: `<= ${cw.max}`,
        label: cw.label,
      });
    }
    if (cw.min !== undefined && num < cw.min) {
      violations.push({
        metric: cw.metric,
        observed: num,
        limit: `>= ${cw.min}`,
        label: cw.label,
      });
    }
  }

  return {
    policy: policyName,
    goal: policy.goal,
    approved: violations.length === 0,
    violations,
  };
}
