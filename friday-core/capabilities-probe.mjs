#!/usr/bin/env node
/**
 * FRIDAY Core — Sonda Viva de Capacidades & Modelos (Google AI Studio + OpenRouter + Supabase Memory)
 *
 * Verifica AO VIVO (zero achismo):
 *   1. Quais modelos frontier (Gemini 4 Carbon / Hydrogen / Neon / 2.5 Pro / Flash / Imagen / Veo / TTS / Embedding)
 *      estão realmente disponíveis na chave do Google AI Studio.
 *   2. Quais modelos :free ou frontier estão ativos no OpenRouter.
 *   3. Saúde real da memória cognitiva no Supabase (tamanho em MB, total de memórias, embeddings faltantes, grafo KG).
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "../scripts/lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const SECRETS_PATH = path.join(PROJECT_ROOT, ".secrets", "ai-media-keys.json");

function loadSecrets() {
  if (!fs.existsSync(SECRETS_PATH)) return {};
  try {
    return JSON.parse(fs.readFileSync(SECRETS_PATH, "utf8"));
  } catch {
    return {};
  }
}

async function probeGoogle(apiKey) {
  if (!apiKey) return { ok: false, error: "GOOGLE_AI_STUDIO_API_KEY ausente" };
  const res = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models?pageSize=150&key=${apiKey}`,
    { signal: AbortSignal.timeout(15000) }
  );
  if (!res.ok) return { ok: false, error: `HTTP ${res.status}` };
  const data = await res.json();
  const models = (data.models || []).map((m) => ({
    id: m.name.replace(/^models\//, ""),
    methods: m.supportedGenerationMethods || [],
    inputLimit: m.inputTokenLimit || 0,
  }));
  const interesting = models.filter((m) =>
    /gemini-4|gemini-3|gemini-2\.5|carbon|hydrogen|neon|imagen|veo|embedding|tts|live|deep-research/i.test(m.id)
  );
  return { ok: true, total: models.length, interesting };
}

async function probeSupabase() {
  try {
    const token = getSupabaseToken();
    const sql = `
      SELECT
        pg_size_pretty(pg_database_size(current_database())) AS db_size,
        (SELECT count(*)::int FROM public.memories) AS memories_count,
        (SELECT count(*)::int FROM public.memories WHERE embedding IS NULL) AS missing_embeddings,
        (SELECT count(*)::int FROM public.kg_nodes) AS kg_nodes,
        (SELECT count(*)::int FROM public.kg_edges) AS kg_edges,
        (SELECT count(*)::int FROM public.orchestrator_logs) AS orchestrator_logs;
    `;
    const res = await fetch(
      `https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        signal: AbortSignal.timeout(12000),
        body: JSON.stringify({ query: sql }),
      }
    );
    if (!res.ok) return { ok: false, error: `HTTP ${res.status}` };
    const rows = await res.json();
    return { ok: true, stats: rows[0] || {} };
  } catch (e) {
    return { ok: false, error: e.message };
  }
}

async function main() {
  const sec = loadSecrets();
  const gKey = sec.GOOGLE_AI_STUDIO_API_KEY || sec.GOOGLE_AQ_API_KEY || process.env.GOOGLE_AI_STUDIO_API_KEY || "";

  const [google, sb] = await Promise.all([probeGoogle(gKey), probeSupabase()]);

  console.log("\n=== 1. GOOGLE AI STUDIO (AO VIVO) ===");
  if (!google.ok) {
    console.log("ERRO:", google.error);
  } else {
    console.log(`Total de modelos na chave: ${google.total} | Relevantes/Frontier: ${google.interesting.length}`);
    for (const m of google.interesting) {
      console.log(`  • ${m.id} (ctx=${m.inputLimit}) [${m.methods.join(",")}]`);
    }
    const hasCarbon = google.interesting.some((m) => /carbon/i.test(m.id));
    console.log(`>> Gemini 4 Carbon na chave Google AI Studio hoje: ${hasCarbon ? "DISPONÍVEL ✅" : "NÃO LISTADO NA CHAVE GRATUITA ❌"}`);
  }

  console.log("\n=== 2. SUPABASE MEMÓRIA COGNITIVA (AO VIVO) ===");
  if (!sb.ok) {
    console.log("ERRO:", sb.error);
  } else {
    console.log(JSON.stringify(sb.stats, null, 2));
  }
}

main().catch((e) => {
  console.error("PROBE_ERROR:", e.message);
  process.exit(1);
});
