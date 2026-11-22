#!/usr/bin/env node
/**
 * FRIDAY Core — Jardineiro do Grafo de Conhecimento (EVO-KG-GARDENER)
 *
 * Lê as memórias reais em `public.memories` no Supabase, extrai entidades factuais
 * (arquivos, módulos, modelos, tabelas, versões, decisões, regras e tecnologias reais),
 * faz upsert em lote em `public.kg_nodes` e cria relações co-ocorrentes em `public.kg_edges`
 * com `source_memory_id` rastreável. Zero invenção, custo R$ 0,00.
 *
 * Uso:
 *   node friday-core/memory-gardener.mjs [--limit 450]
 */

import { getSupabaseToken, SUPABASE_PROJECT_REF } from "../scripts/lib/supabase-auth.mjs";

function esc(str) {
  return String(str ?? "").replace(/'/g, "''");
}

async function querySupabase(sql) {
  const token = getSupabaseToken();
  const res = await fetch(
    `https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      signal: AbortSignal.timeout(25000),
      body: JSON.stringify({ query: sql }),
    }
  );
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Supabase HTTP ${res.status}: ${txt.slice(0, 200)}`);
  }
  return await res.json();
}

function extractEntitiesFromMemory(mem) {
  const text = String(mem.content || "");
  const entities = new Map();

  const add = (key, label, type, salience = 0.72) => {
    const cleanKey = key.toLowerCase().replace(/[^a-z0-9:._-]/g, "_").slice(0, 80);
    if (!cleanKey || entities.has(cleanKey)) return;
    entities.set(cleanKey, {
      node_key: cleanKey,
      label: label.slice(0, 120),
      node_type: type,
      summary: text.slice(0, 180).replace(/\s+/g, " ").trim(),
      salience,
    });
  };

  // 1. Arquivos reais (.kt, .mjs, .ps1, .md, .yml, .json)
  const fileMatches = text.match(/\b[\w.-]+\.(?:kt|mjs|ps1|md|yml|json|xml|sql)\b/gi) || [];
  for (const f of fileMatches.slice(0, 6)) {
    add(`file:${f}`, f, "FILE", 0.75);
  }

  // 2. Modelos de IA reais
  const modelMatches = text.match(/\b(?:gemini-[\w.-]+|gpt-[\w.-]+|nemotron-[\w.-]+|gemma-[\w.-]+|claude-[\w.-]+)\b/gi) || [];
  for (const m of modelMatches.slice(0, 4)) {
    add(`model:${m}`, m, "SYSTEM", 0.82);
  }

  // 3. Versões do app (v2.x.x)
  const verMatches = text.match(/\bv\d+\.\d+\.\d+\b/gi) || [];
  for (const v of verMatches.slice(0, 3)) {
    add(`release:${v}`, `Midiastore ${v}`, "MILESTONE", 0.78);
  }

  // 4. Tabelas Supabase (public.*)
  const tblMatches = text.match(/\bpublic\.[a-z_][a-z0-9_]*\b/gi) || [];
  for (const t of tblMatches.slice(0, 4)) {
    add(`table:${t}`, t, "SYSTEM", 0.80);
  }

  // 5. Edge Functions e SKUs reais
  const skuMatches = text.match(/\b(?:sweep_pro_\w+|verify-play-purchase|ingest-product-event|get-paywall-variant|play-rtdn-webhook)\b/gi) || [];
  for (const s of skuMatches.slice(0, 4)) {
    add(`component:${s}`, s, "ENTITY", 0.85);
  }

  // 6. Memórias estratégicas e correções viram nós de objetivo/regra diretamente
  if (mem.categoria === "strategic" || mem.categoria === "correction") {
    const headline = text.split(/[\n|]/)[0].replace(/^\[[^\]]+\]\s*/, "").trim().slice(0, 90);
    if (headline.length >= 12) {
      add(
        `decision:mem_${mem.id}`,
        headline,
        mem.categoria === "correction" ? "RULE" : "GOAL",
        Math.min(0.98, Number(mem.confianca || 0.85))
      );
    }
  }

  return Array.from(entities.values());
}

async function main() {
  const before = await querySupabase(
    "SELECT (SELECT count(*)::int FROM public.kg_nodes) AS nodes, (SELECT count(*)::int FROM public.kg_edges) AS edges;"
  );

  const memories = await querySupabase(`
    SELECT id, content, categoria, confianca
    FROM public.memories
    ORDER BY confianca DESC, id DESC
    LIMIT 450;
  `);

  const allNodes = new Map();
  const edgePairs = [];

  for (const mem of memories) {
    const ents = extractEntitiesFromMemory(mem);
    for (const e of ents) {
      if (!allNodes.has(e.node_key)) {
        allNodes.set(e.node_key, e);
      }
    }
    // Conecta pares de entidades co-ocorrentes na mesma memória (limitado a 3 pares/memória para manter grafo limpo)
    for (let i = 0; i < Math.min(ents.length, 3); i++) {
      for (let j = i + 1; j < Math.min(ents.length, 4); j++) {
        edgePairs.push({
          srcKey: ents[i].node_key,
          dstKey: ents[j].node_key,
          relation: "RELATED_TO",
          weight: Math.min(0.95, Number(mem.confianca || 0.75)),
          memoryId: mem.id,
        });
      }
    }
  }

  // Limita a 250 nós de maior relevância por ciclo para proteger cota de 500 MB do Supabase
  const nodeList = Array.from(allNodes.values())
    .sort((a, b) => b.salience - a.salience)
    .slice(0, 250);

  if (nodeList.length > 0) {
    const valuesSql = nodeList
      .map(
        (n) =>
          `('${esc(n.node_key)}', '${esc(n.label)}', '${esc(n.node_type)}', '${esc(n.summary)}', ${n.salience}, 1, NOW(), NOW(), NOW())`
      )
      .join(",\n");

    await querySupabase(`
      INSERT INTO public.kg_nodes (node_key, label, node_type, summary, salience, access_count, last_accessed_at, created_at, updated_at)
      VALUES ${valuesSql}
      ON CONFLICT (node_key) DO UPDATE SET
        summary = COALESCE(EXCLUDED.summary, public.kg_nodes.summary),
        salience = GREATEST(public.kg_nodes.salience, EXCLUDED.salience),
        access_count = public.kg_nodes.access_count + 1,
        last_accessed_at = NOW(),
        updated_at = NOW();
    `);
  }

  // Buscar mapa node_key -> id para inserir arestas válidas
  const dbNodes = await querySupabase("SELECT id, node_key FROM public.kg_nodes;");
  const keyToId = new Map(dbNodes.map((r) => [r.node_key, r.id]));

  const uniqueEdges = new Map();
  for (const ep of edgePairs) {
    const sId = keyToId.get(ep.srcKey);
    const tId = keyToId.get(ep.dstKey);
    if (!sId || !tId || sId === tId) continue;
    const dedup = `${Math.min(sId, tId)}-${Math.max(sId, tId)}-${ep.relation}`;
    if (!uniqueEdges.has(dedup)) {
      uniqueEdges.set(dedup, { sId, tId, relation: ep.relation, weight: ep.weight, memoryId: ep.memoryId });
    }
  }

  const edgeList = Array.from(uniqueEdges.values()).slice(0, 300);
  if (edgeList.length > 0) {
    const edgeValues = edgeList
      .map((e) => `(${e.sId}, ${e.tId}, '${esc(e.relation)}', ${e.weight}, ${e.memoryId}, NOW(), NOW())`)
      .join(",\n");

    // Insere apenas arestas que ainda não existem entre o mesmo par + relação
    await querySupabase(`
      INSERT INTO public.kg_edges (source_node_id, target_node_id, relation, weight, source_memory_id, created_at, updated_at)
      SELECT v.s, v.t, v.rel, v.w, v.mid, v.c, v.u
      FROM (VALUES ${edgeValues}) AS v(s, t, rel, w, mid, c, u)
      WHERE NOT EXISTS (
        SELECT 1 FROM public.kg_edges e
        WHERE e.source_node_id = v.s AND e.target_node_id = v.t AND e.relation = v.rel
      );
    `);
  }

  const after = await querySupabase(
    "SELECT (SELECT count(*)::int FROM public.kg_nodes) AS nodes, (SELECT count(*)::int FROM public.kg_edges) AS edges;"
  );

  console.log("\n🌿 FRIDAY MEMORY GARDENER — GRAFO DE CONHECIMENTO ENRIQUECIDO:");
  console.log(`• Memórias processadas: ${memories.length}`);
  console.log(`• Nós (kg_nodes): ${before[0].nodes} -> ${after[0].nodes}`);
  console.log(`• Arestas (kg_edges): ${before[0].edges} -> ${after[0].edges}`);
}

main().catch((e) => {
  console.error("GARDENER_ERROR:", e.message);
  process.exit(1);
});
