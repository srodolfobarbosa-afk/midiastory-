#!/usr/bin/env node
/**
 * FRIDAY Core — Motor de Auto-Evolução (A "Chama" / Heartbeat Decisório)
 *
 * Em vez de esperar o fundador dizer o próximo passo, este motor:
 *   1. Audita o estado real da própria FRIDAY (self-audit + testes nativos + sonda Supabase/Google + git)
 *   2. Detecta automaticamente os gargalos reais medidos (zero achismo)
 *   3. Ranqueia o próximo salto de maior impacto em `friday-core/memory/evolution-backlog.json`
 *   4. Entrega a Próxima Missão Autônoma pronta para execução imediata (ou disparo via /goal / /schedule).
 *
 * Uso:
 *   node friday-core/evolution-loop.mjs [--board]
 */

import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const BACKLOG_PATH = path.join(__dirname, "memory", "evolution-backlog.json");

function runNode(args, timeout = 25000) {
  const res = spawnSync(process.execPath, args, {
    cwd: PROJECT_ROOT,
    encoding: "utf8",
    timeout,
  });
  return {
    ok: res.status === 0,
    stdout: (res.stdout || "").trim(),
    stderr: (res.stderr || "").trim(),
  };
}

function main() {
  const audit = runNode(["scripts/friday-self-audit.mjs"]);
  const tests = runNode(["--test", "friday-core/friday.test.mjs"]);
  const probe = runNode(["friday-core/capabilities-probe.mjs"], 20000);

  // Extrai métricas reais da sonda Supabase
  let kgNodes = null;
  let memoriesCount = null;
  const mKg = probe.stdout.match(/"kg_nodes":\s*(\d+)/);
  const mMem = probe.stdout.match(/"memories_count":\s*(\d+)/);
  if (mKg) kgNodes = Number(mKg[1]);
  if (mMem) memoriesCount = Number(mMem[1]);

  const candidates = [];

  if (!audit.ok) {
    candidates.push({
      priority: 100,
      id: "EVO-HEALTH",
      title: "Corrigir falha de saúde detectada no Self-Audit da FRIDAY",
      reason: audit.stdout.slice(0, 200),
      impact: "CRÍTICO",
    });
  }

  if (!tests.ok) {
    candidates.push({
      priority: 98,
      id: "EVO-TESTS",
      title: "Corrigir testes nativos quebrados em friday-core/friday.test.mjs",
      reason: (tests.stderr || tests.stdout).slice(0, 200),
      impact: "CRÍTICO",
    });
  }

  if (kgNodes !== null && memoriesCount !== null && kgNodes < Math.floor(memoriesCount * 0.25)) {
    candidates.push({
      priority: 92,
      id: "EVO-KG-GARDENER",
      title: `Construir Jardineiro do Grafo de Conhecimento (kg_nodes=${kgNodes} para ${memoriesCount} memórias)`,
      reason: `Apenas ${kgNodes} nós no grafo para ${memoriesCount} memórias; enriquecer relações multiplica precisão de contexto.`,
      impact: "ALTO",
    });
  }

  // Checar se o SDK @google/genai já está sendo usado no roteador central
  const routerSrc = fs.readFileSync(path.join(PROJECT_ROOT, "scripts", "ai-failover-router.mjs"), "utf8");
  if (!routerSrc.includes("@google/genai")) {
    candidates.push({
      priority: 85,
      id: "EVO-GENAI-SDK",
      title: "Migrar chamadas Google do roteador central para o SDK oficial @google/genai + valibot",
      reason: "SDK já instalado em scripts/package.json mas ainda não conectado ao motor central.",
      impact: "ALTO",
    });
  }

  // Checar se a Memória Imune já tem ponte de sincronização com Supabase
  const immuneSrc = fs.readFileSync(path.join(PROJECT_ROOT, "friday-core", "immune-memory.mjs"), "utf8");
  if (!immuneSrc.includes("supabase-auth.mjs")) {
    candidates.push({
      priority: 80,
      id: "EVO-IMMUNE-SYNC",
      title: "Sincronizar Memória de Erros Imunes com public.memories no Supabase",
      reason: "Hoje as 6 vacinas vivem só em JSON local; sincronizar protege qualquer agente em qualquer máquina.",
      impact: "MÉDIO-ALTO",
    });
  }

  candidates.sort((a, b) => b.priority - a.priority);
  const nextMission = candidates[0] || {
    priority: 50,
    id: "EVO-MIDI-REVENUE",
    title: "FRIDAY 100% calibrada — direcionar força total para caixa do Midiastore (Teste Fechado / Produção)",
    reason: "Todos os indicadores técnicos da Construtora estão verdes.",
    impact: "ESTRATÉGICO",
  };

  const snapshot = {
    evaluatedAt: new Date().toISOString(),
    healthOk: audit.ok,
    testsOk: tests.ok,
    supabaseStats: { memoriesCount, kgNodes },
    nextMission,
    backlog: candidates,
  };

  fs.mkdirSync(path.dirname(BACKLOG_PATH), { recursive: true });
  fs.writeFileSync(BACKLOG_PATH, JSON.stringify(snapshot, null, 2), "utf8");

  console.log("\n🔥 FRIDAY EVOLUTION LOOP — PRÓXIMA AUTO-EVOLUÇÃO DECIDIDA:");
  console.log(`• Saúde: ${audit.ok ? "100% OK" : "ALERTA"} | Testes: ${tests.ok ? "OK" : "FALHA"} | Supabase: ${memoriesCount ?? "?"} memórias / ${kgNodes ?? "?"} nós KG`);
  console.log(`• 🎯 MISSÃO #1 (Prioridade ${nextMission.priority} — ${nextMission.impact}): ${nextMission.title}`);
  console.log(`  Motivo real: ${nextMission.reason}`);
  if (candidates.length > 1) {
    console.log("• Fila seguinte:");
    for (const c of candidates.slice(1, 4)) {
      console.log(`  - [p=${c.priority}] ${c.title}`);
    }
  }

  if (process.argv.includes("--board")) {
    const boardScript = path.join(PROJECT_ROOT, "scripts", "antigravity-board.mjs");
    if (fs.existsSync(boardScript)) {
      const body = [
        `MISSÃO #1 ESCOLHIDA PELA PRÓPRIA FRIDAY (p=${nextMission.priority}):`,
        nextMission.title,
        `Motivo: ${nextMission.reason}`,
        "",
        "FILA AUTÔNOMA:",
        ...candidates.map((c, i) => `${i + 1}. [p=${c.priority}] ${c.title}`),
      ].join("\n");
      spawnSync(
        process.execPath,
        [boardScript, "--title", "🔥 FRIDAY Evolution Loop — Próximo Salto", "--body", body, "--open"],
        { stdio: "ignore", timeout: 10000 }
      );
    }
  }
}

main();
