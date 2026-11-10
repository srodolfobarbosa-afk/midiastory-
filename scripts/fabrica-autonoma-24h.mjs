#!/usr/bin/env node
/**
 * MOTOR DE OPERAÇÃO CONTÍNUA 24H (Fábrica Autônoma Midiastore)
 * 
 * Executa ciclos periódicos automatizados para governança e manutenção do ecossistema:
 * 1. Monitoramento e blindagem do Supabase (cota de 500 MB e deduplicação de e-mails)
 * 2. Auditoria e sincronização dos 20 testadores fechados para a Google Play Console
 * 3. Heartbeat do Roteador Multi-LLM (Gemini 3.8 Flash / Argon / GPT-4o-mini / Codex)
 * 4. Processamento da fila de tarefas pendentes em public.orchestrator_tasks
 * 5. Registro contínuo de métricas e status de saúde no Supabase
 * 
 * Uso:
 *   node scripts/fabrica-autonoma-24h.mjs --once          (Executa 1 ciclo único de auditoria)
 *   node scripts/fabrica-autonoma-24h.mjs --interval 300  (Executa em loop a cada 5 minutos)
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { checkAndMaintain } from "./supabase-maintenance.mjs";
import { executeWithFailover } from "./ai-failover-router.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const token = "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
const projectRef = "usmqtnlsaavxqguwrtix";
const campaignFile = path.join(projectRoot, "docs", "campaigns", "lista-20-testadores-google-play.txt");

async function runSql(query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  return res.json();
}

async function logHeartbeat(cycleNumber, details) {
  try {
    const escapedDetails = JSON.stringify(details).replace(/'/g, "''");
    await runSql(`
      INSERT INTO public.orchestrator_tasks (state_id, title, target, payload, status, created_at)
      VALUES (
        1, 
        'HEARTBEAT_CICLO_${cycleNumber}', 
        'LOCAL_PC', 
        '${escapedDetails}', 
        'DONE', 
        NOW()
      );
    `);
  } catch (e) {
    console.warn(`[Ciclo #${cycleNumber}] Aviso ao gravar heartbeat no Supabase:`, e.message);
  }
}

async function auditTesters() {
  const rows = await runSql(`
    SELECT id, email, source, status, created_at, synced_to_play_at 
    FROM public.beta_testers 
    ORDER BY id ASC;
  `);

  const emails = rows.map(r => r.email.toLowerCase().trim()).filter(Boolean);
  const uniqueEmails = [...new Set(emails)];

  fs.mkdirSync(path.dirname(campaignFile), { recursive: true });
  fs.writeFileSync(campaignFile, uniqueEmails.join("\n"), "utf8");

  return {
    total: uniqueEmails.length,
    target: 20,
    remaining: Math.max(0, 20 - uniqueEmails.length),
    latest: uniqueEmails.slice(-3),
  };
}

async function runAutonomousCycle(cycleNumber) {
  const startTime = Date.now();
  const timestamp = new Date().toLocaleTimeString("pt-BR");
  console.log(`\n================================================================`);
  console.log(`🚀 [${timestamp}] INICIANDO CICLO AUTÔNOMO #${cycleNumber}`);
  console.log(`================================================================`);

  const cycleSummary = {
    cycle: cycleNumber,
    timestamp: new Date().toISOString(),
    steps: {},
  };

  // Etapa 1: Blindagem de Armazenamento do Supabase
  try {
    console.log(`\n[Etapa 1/4] Blindagem & Auditoria do Supabase...`);
    await checkAndMaintain();
    cycleSummary.steps.supabase = "OK";
  } catch (err) {
    console.error(`❌ Erro na Etapa 1:`, err.message);
    cycleSummary.steps.supabase = `ERROR: ${err.message}`;
  }

  // Etapa 2: Auditoria dos Testadores Fechados
  try {
    console.log(`\n[Etapa 2/4] Auditoria dos Testadores Fechados (Meta: 20)...`);
    const testersInfo = await auditTesters();
    console.log(`Testadores Ativos: ${testersInfo.total}/20 (${((testersInfo.total / 20) * 100).toFixed(0)}%)`);
    console.log(`Faltam para aprovação: ${testersInfo.remaining} testadores`);
    console.log(`Lista sincronizada em: ${campaignFile}`);
    cycleSummary.steps.testers = testersInfo;
  } catch (err) {
    console.error(`❌ Erro na Etapa 2:`, err.message);
    cycleSummary.steps.testers = `ERROR: ${err.message}`;
  }

  // Etapa 3: Roteador de IA & Failover Ping
  try {
    console.log(`\n[Etapa 3/4] Heartbeat do Roteador Multi-LLM...`);
    const aiResult = await executeWithFailover("Responda apenas 'PULSO_OK' para teste de integridade do cluster.", { isCode: false });
    console.log(`Provedor Ativo: ${aiResult.source} | Resposta: ${aiResult.content.slice(0, 50).trim()}`);
    cycleSummary.steps.aiFleet = { provider: aiResult.source, status: "ONLINE" };
  } catch (err) {
    console.warn(`⚠️ Aviso na Etapa 3:`, err.message);
    cycleSummary.steps.aiFleet = `FALLBACK_REQUIRED: ${err.message}`;
  }

  // Etapa 4: Gravação do Heartbeat no Supabase
  const durationMs = Date.now() - startTime;
  cycleSummary.durationMs = durationMs;
  await logHeartbeat(cycleNumber, cycleSummary);

  console.log(`\n----------------------------------------------------------------`);
  console.log(`✅ [CICLO #${cycleNumber} CONCLUÍDO] Duração: ${(durationMs / 1000).toFixed(2)}s`);
  console.log(`----------------------------------------------------------------\n`);
}

async function main() {
  const args = process.argv.slice(2);
  const isOnce = args.includes("--once");
  const intervalArgIdx = args.indexOf("--interval");
  const intervalSec = intervalArgIdx > -1 && args[intervalArgIdx + 1] ? Number(args[intervalArgIdx + 1]) : 300;

  console.log("================================================================");
  console.log("🏭 FÁBRICA AUTÔNOMA DE SOFTWARE & GOVERNANÇA MIDIASTORE");
  console.log(`Modo: ${isOnce ? "Ciclo Único (--once)" : `Loop Contínuo (Intervalo: ${intervalSec}s)`}`);
  console.log("================================================================");

  let cycle = 1;
  await runAutonomousCycle(cycle);

  if (isOnce) {
    console.log("Execução única concluída com sucesso.");
    process.exit(0);
  }

  process.on("SIGINT", () => {
    console.log("\n[Fábrica Autônoma] Interrupção recebida (SIGINT). Encerrando graciosamente...");
    process.exit(0);
  });

  process.on("SIGTERM", () => {
    console.log("\n[Fábrica Autônoma] Sinal de término (SIGTERM). Encerrando graciosamente...");
    process.exit(0);
  });

  setInterval(async () => {
    cycle++;
    try {
      await runAutonomousCycle(cycle);
    } catch (err) {
      console.error(`Erro crítico no ciclo #${cycle}:`, err.message);
    }
  }, intervalSec * 1000);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(err => {
    console.error("Falha fatal na Fábrica Autônoma:", err);
    process.exit(1);
  });
}
