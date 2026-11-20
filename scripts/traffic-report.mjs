#!/usr/bin/env node
/**
 * RELATÓRIO DE TRÁFEGO REAL pSEO & CLIQUES NA PLAY STORE (Módulo 6)
 *
 * Lê ao vivo do Supabase (public.v_pseo_traffic_report) quantas visitas reais,
 * visitantes únicos e cliques no botão da Google Play cada página /guides/ recebeu.
 */

import { fileURLToPath } from "node:url";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";

export async function getTrafficReport() {
  const [rows, totals] = await Promise.all([
    runSupabaseSql(`SELECT * FROM public.v_pseo_traffic_report LIMIT 30;`),
    runSupabaseSql(`
      SELECT
        COUNT(*) FILTER (WHERE event_name = 'page_view')::int AS total_page_views,
        COUNT(DISTINCT install_hash) FILTER (WHERE event_name = 'page_view')::int AS total_unique_visitors,
        COUNT(*) FILTER (WHERE event_name = 'play_store_click')::int AS total_play_clicks
      FROM public.product_events
      WHERE event_name IN ('page_view', 'play_store_click');
    `),
  ]);

  const t = totals?.[0] || { total_page_views: 0, total_unique_visitors: 0, total_play_clicks: 0 };
  const overallCtr =
    t.total_page_views > 0 ? `${((t.total_play_clicks / t.total_page_views) * 100).toFixed(2)}%` : "0.00%";

  return {
    generatedAt: new Date().toISOString(),
    totals: { ...t, overallCtr },
    byPage: rows || [],
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  getTrafficReport()
    .then((report) => {
      console.log("📊 RELATÓRIO DE TRÁFEGO REAL pSEO (AO VIVO SUPABASE):");
      console.log(JSON.stringify(report, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro no traffic-report:", err.message);
      process.exit(1);
    });
}
