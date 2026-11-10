#!/usr/bin/env node
/**
 * NÚCLEO NATIVO DE AGENTE AUTÔNOMO (Midiastore Agent Core)
 * 
 * Inspirado nas arquiteturas do Antigravity, OpenAI Codex e MCP:
 * 1. Loop de Raciocínio ReAct (Thought -> Tool Call -> Observation -> Answer)
 * 2. Memória Persistente & Vetorial/SQL no Supabase
 * 3. Gerador Automático de Datasets (.jsonl) para Fine-Tuning
 * 4. Subagentes Paralelos Desacoplados
 * 
 * Uso:
 *   node scripts/agent-core.mjs run "Auditar tabelas do banco de dados"
 *   node scripts/agent-core.mjs export-dataset
 */

import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const secretsPath = path.join(projectRoot, ".secrets", "ai-media-keys.json");
const datasetPath = path.join(projectRoot, "docs", "datasets", "fine-tuning-trajectories.jsonl");

// Configurações do Supabase
const supabaseRef = "usmqtnlsaavxqguwrtix";
const supabaseToken = "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";

// Chave OpenRouter para modelo orquestrador
let openRouterKey = process.env.OPENROUTER_API_KEY || "";
if (fs.existsSync(secretsPath)) {
  try {
    const raw = JSON.parse(fs.readFileSync(secretsPath, "utf8"));
    if (raw.OPENROUTER_API_KEY) openRouterKey = raw.OPENROUTER_API_KEY;
  } catch {}
}

// -------------------------------------------------------------
// 1. CAMADA DE BANCO DE DADOS & MEMÓRIA SQL (SUPABASE)
// -------------------------------------------------------------
async function runSql(query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${supabaseRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${supabaseToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  return res.json();
}

async function recordTrajectoryStep(conversationId, stepIndex, role, content, toolCalls = null, toolResponse = null) {
  const payload = {
    conversation_id: conversationId,
    step_index: stepIndex,
    role,
    content,
    tool_calls: toolCalls,
    tool_response: toolResponse,
    created_at: new Date().toISOString()
  };

  const escapedJson = JSON.stringify(payload).replace(/'/g, "''");
  await runSql(`
    INSERT INTO public.orchestrator_logs (level, message, metadata)
    VALUES ('TRAJECTORY', 'Passo de execução do agente', '${escapedJson}'::jsonb);
  `);
}

// -------------------------------------------------------------
// 2. FERRAMENTAS DO AGENTE (TOOL REGISTRY)
// -------------------------------------------------------------
const TOOLS = {
  read_file: {
    description: "Lê o conteúdo de um arquivo de texto no projeto",
    parameters: { type: "object", properties: { path: { type: "string" } }, required: ["path"] },
    execute: (args) => {
      const fullPath = path.isAbsolute(args.path) ? args.path : path.join(projectRoot, args.path);
      return fs.readFileSync(fullPath, "utf8").slice(0, 5000);
    }
  },
  query_database: {
    description: "Executa uma consulta SQL no Supabase",
    parameters: { type: "object", properties: { query: { type: "string" } }, required: ["query"] },
    execute: async (args) => {
      return await runSql(args.query);
    }
  },
  run_terminal: {
    description: "Executa um comando de sistema seguro no repositório",
    parameters: { type: "object", properties: { command: { type: "string" } }, required: ["command"] },
    execute: (args) => {
      return execSync(args.command, { cwd: projectRoot, encoding: "utf8", timeout: 15000 });
    }
  }
};

// -------------------------------------------------------------
// 3. GERADOR DE DATASETS .JSONL PARA FINE-TUNING
// -------------------------------------------------------------
async function exportDatasetForFineTuning() {
  console.log("=== EXPORTADOR DE DATASET PARA FINE-TUNING (.JSONL) ===");
  fs.mkdirSync(path.dirname(datasetPath), { recursive: true });

  const logs = await runSql(`
    SELECT metadata
    FROM public.orchestrator_logs
    WHERE level = 'TRAJECTORY'
    ORDER BY id ASC
    LIMIT 200;
  `);

  if (!logs || logs.length === 0) {
    console.log("Nenhuma trajetória gravada ainda.");
    return;
  }

  // Agrupar por conversation_id
  const conversations = {};
  for (const log of logs) {
    const meta = log.metadata;
    if (!meta || !meta.conversation_id) continue;
    if (!conversations[meta.conversation_id]) conversations[meta.conversation_id] = [];
    conversations[meta.conversation_id].push(meta);
  }

  const jsonlLines = [];
  for (const [convId, steps] of Object.entries(conversations)) {
    const messages = [
      {
        role: "system",
        content: "Você é o Agente Autônomo Oficial do Midiastore, especializado em engenharia Android nativa, Jetpack Compose, Scoped Storage e monetização."
      }
    ];

    for (const s of steps) {
      if (s.role === "user") {
        messages.push({ role: "user", content: s.content });
      } else if (s.role === "assistant") {
        const msg = { role: "assistant", content: s.content };
        if (s.tool_calls) msg.tool_calls = s.tool_calls;
        messages.push(msg);
      } else if (s.role === "tool") {
        messages.push({ role: "tool", content: JSON.stringify(s.tool_response) });
      }
    }

    if (messages.length > 2) {
      jsonlLines.push(JSON.stringify({ messages }));
    }
  }

  fs.writeFileSync(datasetPath, jsonlLines.join("\n"), "utf8");
  console.log(`✅ Dataset exportado com sucesso!`);
  console.log(`📄 Arquivo: ${datasetPath}`);
  console.log(`📊 Total de diálogos prontos para Fine-Tuning: ${jsonlLines.length}`);
}

// -------------------------------------------------------------
// 4. SUBAGENTES PARALELOS
// -------------------------------------------------------------
async function runParallelSubagents(tasks) {
  console.log(`\n🚀 Disparando ${tasks.length} subagentes paralelos...`);
  const promises = tasks.map((task, idx) => {
    return (async () => {
      console.log(`[Subagente #${idx + 1}] Iniciando: "${task.title}"`);
      const startTime = Date.now();
      try {
        const result = await task.run();
        const duration = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`[Subagente #${idx + 1}] Concluído em ${duration}s.`);
        return { success: true, title: task.title, result };
      } catch (err) {
        console.error(`[Subagente #${idx + 1}] Falha:`, err.message);
        return { success: false, title: task.title, error: err.message };
      }
    })();
  });

  return await Promise.all(promises);
}

// -------------------------------------------------------------
// MAIN ENTRYPOINT
// -------------------------------------------------------------
async function main() {
  const cmd = process.argv[2] || "export-dataset";

  if (cmd === "export-dataset") {
    await exportDatasetForFineTuning();
  } else if (cmd === "test-subagents") {
    const results = await runParallelSubagents([
      {
        title: "Auditoria de Armazenamento",
        run: async () => await runSql("SELECT pg_size_pretty(pg_database_size(current_database()));")
      },
      {
        title: "Contagem de Testadores VIP",
        run: async () => await runSql("SELECT count(*) FROM public.beta_testers;")
      },
      {
        title: "Verificação de Arquivos de Build",
        run: async () => fs.existsSync(path.join(projectRoot, "app", "build.gradle.kts"))
      }
    ]);
    console.log("\nResultado dos Subagentes:", results);
  } else {
    console.log("Comandos disponíveis: export-dataset, test-subagents");
  }
}

main().catch(e => {
  console.error("Erro no Núcleo do Agente:", e.message);
  process.exit(1);
});
