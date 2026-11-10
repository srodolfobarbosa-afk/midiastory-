#!/usr/bin/env node
/**
 * Equipe IA: Antigravity (Flash High) + OpenAI Codex CLI + Supabase 24/7
 * 
 * Divisão de Responsabilidades:
 * - Antigravity (Flash High): Orquestração, Supabase MCP, GitHub Actions 24h, Google Play API, Web Navigation.
 * - Codex CLI (codex.exe): Edição e geração pesada de código Kotlin/Android, refatorações complexas, Gradle local e testes instrumentados.
 * 
 * Uso:
 *   node scripts/equipe-ia.mjs list
 *   node scripts/equipe-ia.mjs add "Criar teste X" --target CODEX --cmd "instrucoes..."
 *   node scripts/equipe-ia.mjs codex-next
 *   node scripts/equipe-ia.mjs call-codex "Prompt direto para o codex.exe"
 */
import fs from "node:fs";
import { execSync, spawn } from "node:child_process";

const projectDir = "C:/Users/rodolfo/midiastory-";
const token = "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
const projectRef = "usmqtnlsaavxqguwrtix";
const codexPath = "C:\\Users\\rodolfo\\AppData\\Local\\Programs\\OpenAI\\Codex\\bin\\codex.exe";

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

async function listTasks() {
  console.log("=== FILA DE TAREFAS SINCRONIZADA (SUPABASE) ===");
  const tasks = await runSql(`
    SELECT id, state_id, title, target, status, created_at, updated_at
    FROM public.orchestrator_tasks
    ORDER BY id DESC
    LIMIT 15;
  `);

  if (!tasks || tasks.length === 0) {
    console.log("Nenhuma tarefa na fila.");
    return;
  }

  for (const t of tasks) {
    console.log(`[#${t.id}] [${t.target}] [${t.status}] ${t.title} (Atualizado: ${t.updated_at})`);
  }
}

async function addTask(title, target = "CODEX", payload = {}) {
  const escapedTitle = title.replace(/'/g, "''");
  const payloadJson = JSON.stringify(payload).replace(/'/g, "''");

  const query = `
    INSERT INTO public.orchestrator_tasks (title, target, payload, status)
    VALUES ('${escapedTitle}', '${target}', '${payloadJson}'::jsonb, 'PENDING')
    RETURNING id, title, target, status;
  `;
  const result = await runSql(query);
  console.log(`Tarefa criada com sucesso:`, result[0]);
  return result[0];
}

async function runCodexTask(task) {
  console.log(`\n🤖 [EQUIPE IA] Delegando tarefa #${task.id} para o Codex CLI...`);
  console.log(`Título: ${task.title}`);

  // Lock atômico para evitar que outro agente pegue a mesma tarefa
  const locked = await runSql(`
    UPDATE public.orchestrator_tasks
    SET status = 'IN_PROGRESS', updated_at = NOW()
    WHERE id = ${task.id} AND status = 'PENDING'
    RETURNING id;
  `);

  if (!locked || locked.length === 0) {
    console.log(`⚠️ Tarefa #${task.id} já foi assumida por outro processo. Abortando.`);
    return;
  }

  const prompt = task.payload?.prompt || task.payload?.script_or_command || task.title;
  console.log(`Prompt para o Codex:\n${prompt}\n`);

  try {
    const output = execSync(
      `"${codexPath}" exec --cd "${projectDir}" --dangerously-bypass-approvals-and-sandbox "${prompt.replace(/"/g, '\\"')}"`,
      { encoding: "utf8", stdio: "pipe", maxBuffer: 10 * 1024 * 1024 }
    );

    console.log(`\n✅ Codex concluiu a tarefa #${task.id} com sucesso!`);
    const escapedResult = JSON.stringify({ output: output.trim().slice(-1000) }).replace(/'/g, "''");

    await runSql(`
      UPDATE public.orchestrator_tasks
      SET status = 'DONE', result = '${escapedResult}'::jsonb, updated_at = NOW()
      WHERE id = ${task.id};
    `);

    await runSql(`
      INSERT INTO public.memories (content)
      VALUES ('[Equipe IA - Codex] Tarefa #${task.id} ("${task.title.replace(/'/g, "''")}") concluída com sucesso pelo codex.exe.');
    `);
  } catch (err) {
    console.error(`❌ Erro na execução do Codex:`, err.message);
    const escapedErr = JSON.stringify({ error: err.message }).replace(/'/g, "''");
    await runSql(`
      UPDATE public.orchestrator_tasks
      SET status = 'FAILED', result = '${escapedErr}'::jsonb, updated_at = NOW()
      WHERE id = ${task.id};
    `);
  }
}

async function processNextCodex() {
  const pending = await runSql(`
    SELECT id, state_id, title, target, payload, status
    FROM public.orchestrator_tasks
    WHERE target IN ('CODEX', 'LOCAL_PC') AND status = 'PENDING'
    ORDER BY id ASC
    LIMIT 1;
  `);

  if (!pending || pending.length === 0) {
    console.log("Nenhuma tarefa pendente para o Codex no momento.");
    return;
  }

  await runCodexTask(pending[0]);
}

async function callOpenRouterLlm(prompt, model = "google/gemini-4-argon") {
  const aiKeys = JSON.parse(fs.readFileSync(`${projectDir}/.secrets/ai-media-keys.json`, "utf8"));
  const modelAliases = {
    argon: "google/gemini-4-argon",
    o3: "openai/o3",
    "o3-pro": "openai/o3-pro",
    r1: "deepseek/deepseek-r1-0528",
    gpt4o: "openai/gpt-4o-2024-11-20",
    "gpt4o-mini": "openai/gpt-4o-mini",
  };
  if (modelAliases[model]) model = modelAliases[model];
  console.log(`\n🧠 [EQUIPE IA] Consultando Modelo Remoto (${model}) via OpenRouter...`);
  
  const modelsToTry = [model, "google/gemini-4-argon", "openai/gpt-4o-mini", "openrouter/free"];
  for (const m of modelsToTry) {
    try {
      const res = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${aiKeys.OPENROUTER_API_KEY}`,
          "Content-Type": "application/json",
          "HTTP-Referer": "https://midiastory-df6a0.web.app/",
          "X-Title": "Midiastore Equipe IA",
        },
        body: JSON.stringify({
          model: m,
          max_tokens: 3000,
          messages: [{ role: "user", content: prompt }],
        }),
      });
      if (!res.ok) {
        const errText = await res.text();
        console.warn(`Modelo ${m} retornou ${res.status}: ${errText.slice(0, 150)}. Tentando próximo...`);
        continue;
      }
      const data = await res.json();
      const reply = data.choices?.[0]?.message?.content || "";
      console.log(`\n--- RESPOSTA (${m}) ---\n${reply}\n`);
      return reply;
    } catch (e) {
      console.warn(`Erro no modelo ${m}:`, e.message);
    }
  }
  throw new Error("Nenhum modelo do OpenRouter respondeu com sucesso.");
}

async function callCodexDirect(prompt) {
  console.log(`\n🚀 Invocando Codex CLI diretamente...`);
  console.log(`Prompt: "${prompt}"`);

  try {
    const child = spawn(
      codexPath,
      ["exec", "--cd", projectDir, "--dangerously-bypass-approvals-and-sandbox", prompt],
      { stdio: "inherit" }
    );

    child.on("close", (code) => {
      console.log(`\nCodex finalizado com código ${code}`);
    });
  } catch (err) {
    console.error("Falha ao invocar Codex:", err.message);
  }
}

async function main() {
  const action = process.argv[2] || "list";

  if (action === "list") {
    await listTasks();
  } else if (action === "add") {
    const title = process.argv[3];
    if (!title) {
      console.error("Uso: node scripts/equipe-ia.mjs add 'Título da tarefa' [--target CODEX|ANTIGRAVITY]");
      process.exit(1);
    }
    const targetIdx = process.argv.indexOf("--target");
    const target = targetIdx !== -1 ? process.argv[targetIdx + 1] : "CODEX";
    const promptIdx = process.argv.indexOf("--prompt");
    const prompt = promptIdx !== -1 ? process.argv[promptIdx + 1] : title;

    await addTask(title, target, { prompt });
  } else if (action === "codex-next") {
    await processNextCodex();
  } else if (action === "call-codex") {
    const prompt = process.argv.slice(3).join(" ");
    if (!prompt) {
      console.error("Uso: node scripts/equipe-ia.mjs call-codex 'seu prompt aqui'");
      process.exit(1);
    }
    await callCodexDirect(prompt);
  } else if (action === "call-llm") {
    const modelIdx = process.argv.indexOf("--model");
    let model = "openrouter/free";
    let promptArgs = process.argv.slice(3);
    if (modelIdx !== -1) {
      model = process.argv[modelIdx + 1] || model;
      promptArgs = process.argv.slice(3, modelIdx);
    }
    const prompt = promptArgs.join(" ");
    if (!prompt) {
      console.error("Uso: node scripts/equipe-ia.mjs call-llm 'seu prompt' [--model deepseek/deepseek-r1:free]");
      process.exit(1);
    }
    await callOpenRouterLlm(prompt, model);
  } else {
    console.log("Comandos disponíveis: list, add, codex-next, call-codex, call-llm");
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
