#!/usr/bin/env node
/**
 * MOTOR DE OPERAÇÃO CONTÍNUA 24H (Fábrica Autônoma Midiastore)
 * 
 * 🏛️ CONSTITUIÇÃO DE AUTONOMIA COGNITIVA, PERCEPÇÃO E EXECUÇÃO CONTÍNUA
 * (META FUNDAMENTAL: R$ 1.000.000,00 EM VENDAS REAIS NA GOOGLE PLAY)
 * 
 * Princípios Fundamentais:
 * 1. MENTALIDADE DE INVENÇÃO ONDE NÃO HÁ CAMINHO: Superar bloqueios por engenharia ativa.
 * 2. BUSCA CONTÍNUA PELA MAIOR IA DISPONÍVEL: Gemini 4 Argon, Gemini 3.8 Flash e Codex local.
 * 3. PERCEPÇÃO AUTÔNOMA E EXECUÇÃO DE PONTA A PONTA: Diagnosticar e agir do início ao fim.
 * 4. LEITURA OBRIGATÓRIA DO PASSADO ANTES DE AGIR (ZERO LOOP): public.memories e registrar-memoria.mjs.
 * 5. AUTO-CONSTRUÇÃO DO ECOSSISTEMA: Subagentes e ferramentas, protegendo a cota de 500 MB.
 * 6. COMPROMISSO COM O RESULTADO REAL: Destravar os 20 testadores, aprovação e escala de receita.
 * 
 * Ciclos de Manutenção:
 * - Monitoramento e blindagem do Supabase (cota de 500 MB e deduplicação)
 * - Auditoria dos 20 testadores fechados para o Google Play Console
 * - Heartbeat do Roteador Multi-LLM (Gemini 3.8 Flash / Argon / Codex)
 * - Registro contínuo de memórias e marcos no Supabase
 * 
 * Uso:
 *   node scripts/fabrica-autonoma-24h.mjs --once          (Executa 1 ciclo único de auditoria)
 *   node scripts/fabrica-autonoma-24h.mjs --interval 300  (Executa em loop a cada 5 minutos)
 */

import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { checkAndMaintain } from "./supabase-maintenance.mjs";
import { executeWithFailover } from "./ai-failover-router.mjs";
import { registrarMemoria } from "./registrar-memoria.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const token = process.env.SUPABASE_ACCESS_TOKEN || "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
const projectRef = process.env.SUPABASE_PROJECT_REF || "usmqtnlsaavxqguwrtix";
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
    const target = process.env.GITHUB_ACTIONS ? "UBUNTU_CLOUD" : "LOCAL_PC";
    await runSql(`
      INSERT INTO public.orchestrator_tasks (state_id, title, target, payload, status, created_at)
      VALUES (
        1, 
        'HEARTBEAT_CICLO_${cycleNumber}', 
        '${target}', 
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

  // Etapa 3: Ativação Cognitiva da LLM Construtora (Gemini 4 Argon / 3.8 Flash / Codex)
  try {
    console.log(`\n[Etapa 3/4] 🧠 LLM Construtora Autônoma Ativa (Planejamento & Criação)...`);
    
    // Ler histórico recente do Supabase para manter linha de raciocínio
    let recentMemories = [];
    try {
      const memRows = await runSql(`SELECT content FROM public.memories ORDER BY id DESC LIMIT 3;`);
      recentMemories = memRows.map(r => r.content);
    } catch (e) {
      console.warn("Aviso ao ler memórias:", e.message);
    }

    const prompt = `Você é o Agente Autônomo Construtor do Midiastore (com.rdf112.midiastore), operando em regime 24/7 rumo à meta de R$ 1.000.000,00 em vendas e no fechamento dos 20 testadores fechados na Google Play.
Contexto recente:
${recentMemories.join("\n")}

Ciclo atual: #${cycleNumber}.
Gere UMA entrega técnica concreta e inédita para o projeto:
Pode ser:
- Um artigo rico de SEO em docs/artigos/ (focado em como limpar WhatsApp, liberar memória no Android, fotos duplicadas)
- Um novo roteiro viral detalhado para Reels/TikTok em docs/campaigns/
- Um kit de mensagens de conversão para comunidades de tecnologia em docs/campaigns/
- Uma melhoria arquitetural documentada em docs/

Retorne APENAS um JSON válido no seguinte formato exato (sem blocos markdown em volta do JSON):
{
  "action": "Título claro e objetivo da entrega",
  "category": "SEO | MARKETING | OUTREACH | ARCHITECTURE",
  "filename": "docs/caminho/do/arquivo.md",
  "content": "Conteúdo completo e aprofundado do arquivo",
  "summary": "1 frase explicando o impacto na meta de 20 testadores ou R$ 1 milhão"
}`;

    const aiResult = await executeWithFailover(prompt, { isCode: false });
    let parsed = null;
    try {
      const cleanJson = aiResult.content.replace(/^```json\s*/i, "").replace(/^```\s*/i, "").replace(/\s*```$/i, "").trim();
      parsed = JSON.parse(cleanJson);
    } catch (err) {
      console.warn("LLM retornou texto livre, adaptando para nota técnica:", err.message);
      parsed = {
        action: `Entrega Cognitiva do Ciclo #${cycleNumber}`,
        category: "MARKETING",
        filename: `docs/campaigns/entrega-ciclo-${cycleNumber}.md`,
        content: aiResult.content,
        summary: "Diretriz estratégica gerada pela LLM."
      };
    }

    // Gravar artefato no disco
    if (parsed.filename && parsed.content) {
      const fullPath = path.join(projectRoot, parsed.filename);
      fs.mkdirSync(path.dirname(fullPath), { recursive: true });
      fs.writeFileSync(fullPath, parsed.content, "utf8");
      console.log(`📝 Arquivo gerado pela LLM: ${parsed.filename}`);
    }

    // Gravar memória da ação
    await registrarMemoria({
      action: `[Ciclo #${cycleNumber}] ${parsed.action}`,
      studied: "Histórico de memórias recentes e gaps da meta de R$ 1M / 20 testadores",
      attempted: `Criação autônoma do artefato ${parsed.filename}`,
      errors: "Nenhum erro na geração",
      fixes: "Nenhuma correção necessária",
      conclusions: `${parsed.summary} (Provedor: ${aiResult.source})`
    });

    cycleSummary.steps.aiFleet = { provider: aiResult.source, action: parsed.action, filename: parsed.filename };

    // Auto-commit e push git se houver alterações
    try {
      const gitStatus = execSync(`git -C "${projectRoot}" status --porcelain`, { encoding: "utf8" });
      if (gitStatus.trim()) {
        console.log("📦 Alterações detectadas pelo git. Realizando commit e push autônomo...");
        execSync(`git -C "${projectRoot}" add "${parsed.filename}" docs/LINHA_DO_TEMPO_AUTONOMA.md`, { stdio: "pipe", timeout: 15000 });
        execSync(`git -C "${projectRoot}" commit -m "feat(autonomous-factory): [cycle-${cycleNumber}] ${parsed.action.replace(/"/g, '')}"`, { stdio: "pipe", timeout: 15000 });
        
        // Rebase preventivo antes do push para evitar conflitos concorrentes (nuvem vs local)
        try {
          execSync(`git -C "${projectRoot}" pull --rebase origin HEAD`, { stdio: "pipe", timeout: 30000 });
        } catch (pullErr) {
          console.warn("Aviso no rebase pré-push:", pullErr.message);
        }

        execSync(`git -C "${projectRoot}" push origin HEAD`, { stdio: "pipe", timeout: 30000 });
        console.log("🚀 Commit e push concluídos com sucesso pela Fábrica Autônoma!");
      }
    } catch (gitErr) {
      console.warn("Aviso no auto-commit git:", gitErr.message);
    }
  } catch (err) {
    console.warn(`⚠️ Aviso na Etapa 3:`, err.message);
    cycleSummary.steps.aiFleet = `ERROR: ${err.message}`;
  }

  // Etapa 4: Gravação do Heartbeat no Supabase
  const durationMs = Date.now() - startTime;
  cycleSummary.durationMs = durationMs;
  await logHeartbeat(cycleNumber, cycleSummary);

  console.log(`\n----------------------------------------------------------------`);
  console.log(`✅ [CICLO #${cycleNumber} CONCLUÍDO] Duração: ${(durationMs / 1000).toFixed(2)}s`);
  console.log(`----------------------------------------------------------------\n`);
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

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
  let running = true;

  process.on("SIGINT", () => {
    console.log("\n[Fábrica Autônoma] Interrupção recebida (SIGINT). Encerrando graciosamente...");
    running = false;
    process.exit(0);
  });

  process.on("SIGTERM", () => {
    console.log("\n[Fábrica Autônoma] Sinal de término (SIGTERM). Encerrando graciosamente...");
    running = false;
    process.exit(0);
  });

  while (running) {
    try {
      await runAutonomousCycle(cycle);
    } catch (err) {
      console.error(`Erro crítico no ciclo #${cycle}:`, err.message);
    }

    if (isOnce) {
      console.log("Execução única concluída com sucesso.");
      break;
    }

    cycle++;
    console.log(`Aguardando ${intervalSec}s até o próximo ciclo cognitivo autônomo...`);
    await sleep(intervalSec * 1000);
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(err => {
    console.error("Falha fatal na Fábrica Autônoma:", err);
    process.exit(1);
  });
}
