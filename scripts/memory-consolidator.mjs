#!/usr/bin/env node
/**
 * CONSOLIDADOR NOTURNO DE MEMÓRIA & KNOWLEDGE GRAPH (Sleep-Time Consolidation — Módulo 3)
 *
 * Inspirado em Hipocampo -> Neocórtex + MemGPT:
 * 1. Lê memórias ativas recentes de public.memories
 * 2. Extrai entidades/conceitos/sistemas canônicos e relações causais (kg_nodes + kg_edges)
 * 3. Aplica salience decay leve em nós antigos não acessados e reforço em nós ativos
 * 4. Monitora tamanho real do banco Supabase vs teto gratuito de 500 MB
 *
 * Uso:
 *   node scripts/memory-consolidator.mjs [--limit 80] [--dry-run]
 */

import { fileURLToPath } from "node:url";
import { escapeSqlString, runSupabaseSql } from "./lib/cognitive-memory.mjs";

const CANONICAL_NODES = [
  { key: "goal:r1m", label: "Meta R$ 1.000.000,00 Midiastore", type: "GOAL", patterns: [/1\.000\.000|r\$\s*1m/i] },
  { key: "rule:50_50", label: "Regra Financeira 50/50", type: "RULE", patterns: [/50\/50|reinvestimento/i] },
  { key: "rule:custo_zero", label: "Blindagem Financeira Custo Zero", type: "RULE", patterns: [/custo zero|500\s*mb|supabase free/i] },
  { key: "rule:scoped_storage", label: "Scoped Storage (Zero MANAGE_EXTERNAL_STORAGE)", type: "RULE", patterns: [/scoped storage|manage_external_storage/i] },
  { key: "system:midiastore_app", label: "Midiastore Android App (com.rdf112.midiastore)", type: "SYSTEM", patterns: [/midiastore|com\.rdf112\.midiastore|v2\.7\.4|vc\s*29/i] },
  { key: "system:nexo", label: "Ecossistema de Agentes NEXO", type: "SYSTEM", patterns: [/\bnexo\b|mente sint[eé]tica/i] },
  { key: "entity:verify_play_purchase", label: "Edge Function verify-play-purchase", type: "FILE", patterns: [/verify-play-purchase/i] },
  { key: "entity:play_rtdn_webhook", label: "Edge Function play-rtdn-webhook (RTDN)", type: "FILE", patterns: [/play-rtdn-webhook|\brtdn\b/i] },
  { key: "entity:ai_failover_router", label: "Roteador Multi-LLM (ai-failover-router.mjs)", type: "FILE", patterns: [/ai-failover-router|circuit breaker|openrouter/i] },
  { key: "concept:hybrid_rrf", label: "Hybrid Search (BM25 + pgvector + RRF)", type: "CONCEPT", patterns: [/\brrf\b|hybrid_memory_search|bm25/i] },
  { key: "concept:knowledge_graph", label: "Knowledge Graph & GNNs (kg_nodes/kg_edges)", type: "CONCEPT", patterns: [/knowledge graph|\bgnn\b|kg_nodes|memgpt/i] },
  { key: "concept:eda_mqtt_grpc", label: "Event-Driven Architecture (MQTT / gRPC / Outbox)", type: "CONCEPT", patterns: [/\bmqtt\b|\bgrpc\b|event-driven|outbox|skip locked/i] },
  { key: "concept:global_payments_mor", label: "Pagamentos Globais (Play Billing 15% / MoR / Ledger)", type: "CONCEPT", patterns: [/merchant of record|\bmor\b|financial_ledger|stripe|voided purchases/i] },
  { key: "milestone:play_production", label: "Aprovação de Produção Google Play (14 dias)", type: "MILESTONE", patterns: [/produ[çc][aã]o|14 dias|testadores/i] },
];

const STRUCTURAL_EDGES = [
  ["entity:verify_play_purchase", "concept:global_payments_mor", "IMPLEMENTS", 0.95],
  ["entity:play_rtdn_webhook", "concept:global_payments_mor", "IMPLEMENTS", 0.95],
  ["entity:play_rtdn_webhook", "concept:eda_mqtt_grpc", "IMPLEMENTS", 0.9],
  ["concept:global_payments_mor", "goal:r1m", "UNLOCKS", 0.98],
  ["milestone:play_production", "goal:r1m", "UNLOCKS", 0.98],
  ["system:midiastore_app", "rule:scoped_storage", "DEPENDS_ON", 0.95],
  ["rule:50_50", "system:nexo", "UNLOCKS", 0.92],
  ["concept:hybrid_rrf", "system:nexo", "PART_OF", 0.94],
  ["concept:knowledge_graph", "system:nexo", "PART_OF", 0.95],
  ["concept:eda_mqtt_grpc", "system:nexo", "PART_OF", 0.92],
  ["entity:ai_failover_router", "rule:custo_zero", "IMPLEMENTS", 0.9],
];

export async function runMemoryConsolidation({ limit = 80, dryRun = false } = {}) {
  const safeLimit = Math.max(10, Math.min(200, Number(limit) || 80));

  const memories = await runSupabaseSql(`
    SELECT id, content, categoria, confianca, created_at
    FROM public.memories
    WHERE is_active = true
      AND content !~ '\\[Ciclo #[0-9]+\\]'
    ORDER BY id DESC
    LIMIT ${safeLimit};
  `);

  const nodeHits = new Map();
  const coOccurrenceEdges = new Map();

  for (const m of memories || []) {
    const matched = [];
    for (const def of CANONICAL_NODES) {
      if (def.patterns.some((re) => re.test(m.content || ""))) {
        matched.push(def);
        const curr = nodeHits.get(def.key) || { def, count: 0, lastMemoryId: m.id };
        curr.count += 1;
        if (m.id > curr.lastMemoryId) curr.lastMemoryId = m.id;
        nodeHits.set(def.key, curr);
      }
    }
    for (let i = 0; i < matched.length; i++) {
      for (let j = i + 1; j < matched.length; j++) {
        const pairKey = `${matched[i].key}|${matched[j].key}`;
        const existing = coOccurrenceEdges.get(pairKey) || {
          src: matched[i].key,
          tgt: matched[j].key,
          hits: 0,
          sourceMemoryId: m.id,
        };
        existing.hits += 1;
        coOccurrenceEdges.set(pairKey, existing);
      }
    }
  }

  if (!dryRun) {
    // Upsert canonical nodes with salience boosted by real mention frequency
    for (const def of CANONICAL_NODES) {
      const hit = nodeHits.get(def.key);
      const count = hit?.count || 0;
      const salience = Math.min(0.99, 0.55 + count * 0.04).toFixed(4);
      await runSupabaseSql(`
        INSERT INTO public.kg_nodes (node_key, label, node_type, salience, access_count, last_accessed_at, updated_at)
        VALUES (
          '${escapeSqlString(def.key)}',
          '${escapeSqlString(def.label)}',
          '${def.type}',
          ${salience},
          ${Math.max(1, count)},
          NOW(),
          NOW()
        )
        ON CONFLICT (node_key) DO UPDATE SET
          label = EXCLUDED.label,
          salience = GREATEST(public.kg_nodes.salience, EXCLUDED.salience),
          access_count = public.kg_nodes.access_count + ${count > 0 ? 1 : 0},
          last_accessed_at = CASE WHEN ${count > 0 ? "true" : "false"} THEN NOW() ELSE public.kg_nodes.last_accessed_at END,
          updated_at = NOW();
      `);
    }

    // Upsert structural edges
    for (const [src, tgt, rel, weight] of STRUCTURAL_EDGES) {
      await runSupabaseSql(`
        INSERT INTO public.kg_edges (source_node_id, target_node_id, relation, weight, updated_at)
        SELECT s.id, t.id, '${rel}', ${weight}, NOW()
        FROM public.kg_nodes s, public.kg_nodes t
        WHERE s.node_key = '${escapeSqlString(src)}'
          AND t.node_key = '${escapeSqlString(tgt)}'
        ON CONFLICT (source_node_id, target_node_id, relation) DO UPDATE SET
          weight = GREATEST(public.kg_edges.weight, EXCLUDED.weight),
          updated_at = NOW();
      `);
    }

    // Upsert strong co-occurrence edges (>= 2 shared memories)
    for (const edge of coOccurrenceEdges.values()) {
      if (edge.hits < 2) continue;
      const weight = Math.min(0.92, 0.55 + edge.hits * 0.05).toFixed(4);
      await runSupabaseSql(`
        INSERT INTO public.kg_edges (source_node_id, target_node_id, relation, weight, source_memory_id, updated_at)
        SELECT s.id, t.id, 'RELATED_TO', ${weight}, ${Number(edge.sourceMemoryId)}, NOW()
        FROM public.kg_nodes s, public.kg_nodes t
        WHERE s.node_key = '${escapeSqlString(edge.src)}'
          AND t.node_key = '${escapeSqlString(edge.tgt)}'
        ON CONFLICT (source_node_id, target_node_id, relation) DO UPDATE SET
          weight = GREATEST(public.kg_edges.weight, EXCLUDED.weight),
          updated_at = NOW();
      `);
    }
  }

  const stats = await runSupabaseSql(`
    SELECT
      (SELECT count(*)::int FROM public.memories WHERE is_active = true) AS active_memories,
      (SELECT count(*)::int FROM public.kg_nodes) AS kg_nodes,
      (SELECT count(*)::int FROM public.kg_edges) AS kg_edges,
      pg_size_pretty(pg_database_size(current_database())) AS db_size,
      round((pg_database_size(current_database())::numeric / (500 * 1024 * 1024)) * 100, 2) AS pct_of_500mb;
  `);

  const sampleTraversal = await runSupabaseSql(`
    SELECT hop, source_key, relation, target_key, weight
    FROM public.kg_traverse_2hop(ARRAY['entity:verify_play_purchase', 'concept:hybrid_rrf'], 8);
  `);

  return {
    scannedMemories: memories?.length || 0,
    matchedCanonicalNodes: nodeHits.size,
    dryRun,
    stats: stats?.[0] || {},
    sampleTraversal: sampleTraversal || [],
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const dryRun = process.argv.includes("--dry-run");
  const limitIdx = process.argv.indexOf("--limit");
  const limit = limitIdx > -1 ? Number(process.argv[limitIdx + 1]) : 80;

  runMemoryConsolidation({ limit, dryRun })
    .then((res) => {
      console.log("✅ Consolidação Cognitiva concluída:");
      console.log(JSON.stringify(res, null, 2));
    })
    .catch((err) => {
      console.error("❌ Erro na consolidação:", err.message);
      process.exit(1);
    });
}
