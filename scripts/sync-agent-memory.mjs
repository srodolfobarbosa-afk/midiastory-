#!/usr/bin/env node
/**
 * Sincronizador Universal de Memória Cognitiva (Antigravity + Codex + Roteadores -> Supabase)
 *
 * Uso:
 *   node scripts/sync-agent-memory.mjs read [consulta opcional]
 *   node scripts/sync-agent-memory.mjs write "Resumo validado" [--cat=semantic] [--origem=agent_sync] [--conf=0.92] [--supersedes=123]
 *   node scripts/sync-agent-memory.mjs backfill
 *   node scripts/sync-agent-memory.mjs metrics
 */
import {
  backfillCognitiveMemories,
  classifyMemoryCategory,
  computeCognitiveHealthMetrics,
  retrieveRelevantContext,
  runSupabaseSql,
  saveValidatedMemory,
  VALID_MEMORY_CATEGORIES,
} from "./lib/cognitive-memory.mjs";

function parseFlags(args) {
  const flags = {};
  const positional = [];
  for (const a of args) {
    if (a.startsWith("--")) {
      const eq = a.indexOf("=");
      if (eq !== -1) {
        flags[a.slice(2, eq)] = a.slice(eq + 1);
      } else {
        flags[a.slice(2)] = "true";
      }
    } else {
      positional.push(a);
    }
  }
  return { flags, positional };
}

async function handleRead(query) {
  const ctx = await retrieveRelevantContext({ query, limit: 8 });
  const latest = await runSupabaseSql(
    "SELECT id, categoria, versao, confianca, embedding_model, is_active, content, created_at FROM public.memories WHERE is_active = true ORDER BY id DESC LIMIT 5;"
  ).catch(() => []);

  console.log(`=== MEMORIA COGNITIVA MIDIASTORE (Fonte: ${ctx.source} | Emb: ${ctx.embeddingModel || "none"}) ===`);
  if (ctx.formattedContext) {
    console.log(ctx.formattedContext);
  }
  if (latest.length > 0) {
    console.log("\n=== ULTIMOS REGISTROS CRONOLOGICOS ATIVOS ===");
    for (const m of latest) {
      console.log(
        `\n[#${m.id} | ${m.categoria} v${m.versao} | conf:${m.confianca} | emb:${m.embedding_model} | ${m.created_at}]\n${m.content}`
      );
    }
  }
}

async function handleWrite(positional, flags) {
  const text = positional.join(" ").trim();
  if (!text) throw new Error("Informe o texto para gravar.");

  const rawCat = flags.cat || classifyMemoryCategory(text, "semantic");
  const categoria = VALID_MEMORY_CATEGORIES.has(rawCat) ? rawCat : "semantic";
  const origem = flags.origem || "agent_sync";
  const confianca = flags.conf ? Number(flags.conf) : 0.92;
  const supersedesId = flags.supersedes ? Number(flags.supersedes) : null;

  const res = await saveValidatedMemory({
    content: text,
    origem,
    categoria,
    confianca,
    supersedesId,
  });
  console.log("Resultado da gravacao validada:", JSON.stringify(res));
  if (!res.inserted && !res.deduplicated) {
    process.exitCode = 1;
  }
}

async function main() {
  const action = process.argv[2] || "read";
  const { flags, positional } = parseFlags(process.argv.slice(3));

  if (action === "read") {
    await handleRead(positional.join(" ").trim());
  } else if (action === "write") {
    await handleWrite(positional, flags);
  } else if (action === "backfill") {
    const res = await backfillCognitiveMemories({
      reclassifyAllLegacy: flags.reclassify === "true",
    });
    console.log("Backfill cognitivo concluido:", JSON.stringify(res));
  } else if (action === "metrics") {
    const metrics = await computeCognitiveHealthMetrics();
    console.log(JSON.stringify(metrics, null, 2));
  } else {
    console.log("Acoes validas: read [query] | write <texto> [--cat=...] [--origem=...] [--conf=...] [--supersedes=id] | backfill | metrics");
    process.exit(1);
  }
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
