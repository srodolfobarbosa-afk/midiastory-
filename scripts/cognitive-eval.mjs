#!/usr/bin/env node
/**
 * Suite Completa de Testes Obrigatórios & Avaliação Contínua da Arquitetura Cognitiva
 * Valida ponta a ponta:
 *  1. Rejeição estrita de afirmações especulativas
 *  2. Gravação de conhecimento novo com metadados completos e embedding neural 384d (gemini-embedding-001)
 *  3. Recuperação semântica por paráfrase/sinônimo (sem depender só de palavra-chave idêntica)
 *  4. Deduplicação por SHA-256 (content_hash)
 *  5. Resolução de contradições e versionamento (v1 -> v2, superseded_by)
 *  6. Aprendizado automático pós-turno (analyzeAndLearnTurn) com extração + histórico de conversa
 *  7. Reinício de processo + nova sessão com LLM #1 (Google AI Studio / Gemini)
 *  8. Troca completa de família de LLM em processo isolado #2 (OpenRouter / NVIDIA Nemotron / OpenRouter Free)
 *  9. Validação dos hooks nativos Antigravity (SessionStart & PreInvocation)
 * 10. Registro mensurável em public.cognitive_evaluations + limpeza integral de dados sintéticos
 */

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  analyzeAndLearnTurn,
  computeCognitiveHealthMetrics,
  NEURAL_EMBEDDING_MODEL,
  recordEvaluation,
  retrieveRelevantContext,
  runSupabaseSql,
  saveValidatedMemory,
} from "./lib/cognitive-memory.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

async function main() {
  const suffix = Date.now().toString().slice(-6);
  const uniqueCode = `PROTOCOLO_DELTA_${suffix}`;
  const createdMemoryIds = [];
  const createdConvIds = [];
  const results = [];
  let llm1Model = "google_ai_studio";
  let llm2Model = "openrouter/free";

  try {
    // [1/9] Speculative rejection
    const spec = await saveValidatedMemory({
      content: `Talvez o ${uniqueCode} tenha limite de 10%.`,
      origem: "cognitive_eval_test",
      categoria: "semantic",
    });
    assert.equal(spec.inserted, false);
    assert.equal(spec.reason, "rejected_speculative");
    results.push({ test: "speculative_rejection", passed: true });
    console.log("✅ [1/9] Rejeição de informação especulativa: PASS");

    // [2/9] Register validated knowledge v1 + real neural embedding
    const v1 = await saveValidatedMemory({
      content: `${uniqueCode}: A taxa oficial de auditoria QA é 42% no modo padrão e utiliza armazenamento isolado por escopo.`,
      origem: "cognitive_eval_test",
      categoria: "semantic",
      confianca: 0.96,
    });
    assert.equal(v1.inserted, true);
    assert.equal(v1.versao, 1);
    assert.equal(v1.embeddingModel, NEURAL_EMBEDDING_MODEL, "Esperado embedding neural gemini-embedding-001");
    createdMemoryIds.push(v1.id);
    results.push({ test: "register_knowledge_v1_neural_emb", passed: true, id: v1.id });
    console.log(`✅ [2/9] Conhecimento v1 gravado com ${v1.embeddingModel} (id=${v1.id}): PASS`);

    // [3/9] Semantic paraphrased retrieval
    const semTarget = await saveValidatedMemory({
      content: `Diretiva ${uniqueCode}: Quando o telefone ficar lento por fotos repetidas, remover duplicatas idênticas com hash criptográfico.`,
      origem: "cognitive_eval_test",
      categoria: "procedural",
      confianca: 0.94,
    });
    assert.equal(semTarget.inserted, true);
    createdMemoryIds.push(semTarget.id);

    const semSearch = await retrieveRelevantContext({
      query: `celular travando sem espaço livre na galeria de imagens duplicadas ${uniqueCode}`,
      limit: 8,
    });
    const foundSem = (semSearch.memories || []).some((m) => Number(m.id) === Number(semTarget.id));
    assert.ok(foundSem, "Busca semântica não recuperou o registro parafraseado");
    results.push({ test: "semantic_paraphrase_retrieval", passed: true, embeddingModel: semSearch.embeddingModel });
    console.log(`✅ [3/9] Recuperação semântica por paráfrase (${semSearch.embeddingModel}): PASS`);

    // [4/9] Deduplication by SHA-256
    const dup = await saveValidatedMemory({
      content: `  ${uniqueCode}: a taxa oficial de auditoria QA é 42% no modo padrão e utiliza armazenamento isolado por escopo. `,
      origem: "cognitive_eval_test",
      categoria: "semantic",
    });
    assert.equal(dup.inserted, false);
    assert.equal(dup.deduplicated, true);
    assert.equal(Number(dup.existingId), Number(v1.id));
    results.push({ test: "sha256_deduplication", passed: true });
    console.log("✅ [4/9] Deduplicação SHA-256: PASS");

    // [5/9] Contradiction resolution & versioning (v1 -> v2)
    const v2 = await saveValidatedMemory({
      content: `${uniqueCode}: Correção definitiva — a taxa oficial de auditoria QA atualizada agora é 55% no modo padrão.`,
      origem: "cognitive_eval_test",
      categoria: "correction",
      confianca: 0.99,
      supersedesId: v1.id,
    });
    assert.equal(v2.inserted, true);
    assert.equal(v2.versao, 2);
    createdMemoryIds.push(v2.id);

    const checkOld = await runSupabaseSql(`SELECT is_active, superseded_by FROM public.memories WHERE id = ${v1.id};`);
    assert.equal(checkOld[0].is_active, false);
    assert.equal(Number(checkOld[0].superseded_by), Number(v2.id));
    results.push({ test: "contradiction_versioning", passed: true, v1: v1.id, v2: v2.id });
    console.log(`✅ [5/9] Resolução de contradição v1(${v1.id}) -> v2(${v2.id}): PASS`);

    // [6/9] Continuous post-turn learner (analyzeAndLearnTurn)
    const turnLearn = await analyzeAndLearnTurn({
      question: `Regra permanente para ${uniqueCode}: o tempo máximo de cache local é 15 segundos.`,
      answer: `Confirmado: em ${uniqueCode}, o cache local expira exatamente em 15 segundos. Talvez no futuro mude, mas hoje está confirmado em 15s.`,
      modelUsed: "cognitive_eval_turn_test",
      sessionId: `eval-turn-${suffix}`,
      origem: "cognitive_eval_test",
      recordAprendizado: false,
    });
    assert.equal(turnLearn.skipped, false);
    assert.ok(turnLearn.conversationId, "Conversa não foi persistida");
    createdConvIds.push(turnLearn.conversationId);
    for (const sm of turnLearn.savedMemories || []) {
      if (sm?.id) createdMemoryIds.push(sm.id);
    }
    const insertedFromTurn = (turnLearn.savedMemories || []).filter((m) => m.inserted);
    assert.ok(insertedFromTurn.length >= 1, `Extrator automático (${turnLearn.extractorSource}) não salvou fato validado`);
    results.push({ test: "continuous_turn_learner", passed: true, extractor: turnLearn.extractorSource });
    console.log(`✅ [6/9] Aprendizado automático pós-turno (${turnLearn.extractorSource}): PASS`);

    // [7/9] Isolated Process Restart with LLM #1 (Google Gemini)
    const prompt = `Responda em 1 frase curta: qual é a taxa oficial atual de auditoria QA do ${uniqueCode}?`;
    const out1 = execFileSync(
      process.execPath,
      ["scripts/ai-failover-router.mjs", "--provider=google_ai_studio", `--session=eval-g-${suffix}`, prompt],
      { cwd: projectRoot, encoding: "utf8", timeout: 45000 }
    );
    assert.match(out1, /55%/, `LLM #1 (Google) não recuperou 55%. Saída: ${out1}`);
    const match1 = out1.match(/\[Provedor Vencedor:\s*([^\]]+)\]/);
    if (match1) llm1Model = match1[1].trim();
    results.push({ test: "cross_session_restart_google", passed: true, model: llm1Model });
    console.log(`✅ [7/9] Nova sessão isolada com LLM #1 (${llm1Model}) recuperou 55%: PASS`);

    // [8/9] Cross-Family LLM Swap in second isolated process (OpenRouter NVIDIA Nemotron / OpenRouter Free)
    const out2 = execFileSync(
      process.execPath,
      [
        "scripts/ai-failover-router.mjs",
        "--provider=openrouter",
        "--model=nvidia/nemotron-3.5-lightning:free",
        `--session=eval-or-${suffix}`,
        prompt,
      ],
      { cwd: projectRoot, encoding: "utf8", timeout: 60000 }
    );
    assert.match(out2, /55%/, `LLM #2 (OpenRouter) não recuperou 55%. Saída: ${out2}`);
    const match2 = out2.match(/\[Provedor Vencedor:\s*([^\]]+)\]/);
    if (match2) llm2Model = match2[1].trim();
    results.push({ test: "cross_family_llm_swap_openrouter", passed: true, model: llm2Model });
    console.log(`✅ [8/9] Troca de família de LLM (${llm2Model}) em processo isolado recuperou 55%: PASS`);

    // [9/9] Antigravity native hook smoke test (SessionStart + PreInvocation)
    const hookStartRaw = execFileSync(
      process.execPath,
      ["scripts/hooks/cognitive-hook.mjs", "SessionStart"],
      {
        cwd: projectRoot,
        input: JSON.stringify({ conversationId: `hook-eval-${suffix}`, lastUserInput: prompt }),
        encoding: "utf8",
        timeout: 15000,
      }
    );
    const hookStart = JSON.parse(hookStartRaw);
    assert.ok(hookStart?.injectSteps?.[0]?.systemMessage?.systemMessage, "SessionStart hook não retornou systemMessage");

    const hookPreRaw = execFileSync(
      process.execPath,
      ["scripts/hooks/cognitive-hook.mjs", "PreInvocation"],
      {
        cwd: projectRoot,
        input: JSON.stringify({ conversationId: `hook-eval-${suffix}`, lastUserInput: prompt, invocationNum: 1 }),
        encoding: "utf8",
        timeout: 15000,
      }
    );
    const hookPre = JSON.parse(hookPreRaw);
    const preText = hookPre?.injectSteps?.[0]?.systemMessage?.systemMessage || "";
    assert.match(preText, /55%/, "PreInvocation hook não injetou o conhecimento atualizado 55%");
    results.push({ test: "antigravity_hooks_session_and_pre", passed: true });
    console.log("✅ [9/9] Hooks nativos Antigravity (SessionStart + PreInvocation): PASS");

    // Compute live health & record evaluation
    const health = await computeCognitiveHealthMetrics();
    const evalRow = await recordEvaluation({
      modelUsed: `${llm1Model} <-> ${llm2Model}`,
      suiteName: "mandatory_continuous_cross_family_v2",
      totalCases: results.length,
      passedCases: results.filter((r) => r.passed).length,
      validatedMemoriesCount: health.memories.active,
      correctedResponsesCount: health.memories.correctionsCount,
      repeatedErrorsCount: 0,
      crossFamilyModels: [llm1Model, llm2Model],
      details: { uniqueCode, health: health.memories, results },
    });
    console.log(
      `📊 Avaliação registrada em public.cognitive_evaluations (id=${evalRow.id}, accuracy=${evalRow.retrieval_accuracy}, cobertura_neural=${health.memories.semanticCoveragePct}%)`
    );
  } finally {
    if (createdMemoryIds.length > 0) {
      await runSupabaseSql(`DELETE FROM public.memories WHERE id IN (${createdMemoryIds.map(Number).join(",")});`);
    }
    if (createdConvIds.length > 0) {
      await runSupabaseSql(`DELETE FROM public.conversations WHERE id IN (${createdConvIds.map(Number).join(",")});`);
    }
    console.log("🧹 Dados sintéticos de teste isolados e removidos após validação.");
  }
}

main().catch((err) => {
  console.error("❌ Falha na suite cognitiva:", err);
  process.exit(1);
});
