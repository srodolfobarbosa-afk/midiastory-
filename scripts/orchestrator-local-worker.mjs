#!/usr/bin/env node
/**
 * Trabalhador Local ("Mãos no PC") da IA Nativa Orquestradora
 * Executa tarefas pendentes que necessitam do ambiente local (compilação Gradle, emulador, hardware)
 * e devolve o resultado para a IA Nativa Orquestradora no Supabase.
 */
import fs from "node:fs";
import { execSync } from "node:child_process";

const token = "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
const projectRef = "usmqtnlsaavxqguwrtix";
const edgeFunctionUrl = `https://${projectRef}.supabase.co/functions/v1/native-orchestrator`;

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

async function notifyCallback(taskId, stateId, status, result) {
  const res = await fetch(edgeFunctionUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      action: "TASK_CALLBACK",
      task_id: taskId,
      state_id: stateId,
      status,
      result,
    }),
  });
  if (res.ok) {
    console.log(`[Local Worker] Callback da tarefa #${taskId} enviado com sucesso (${status}).`);
  }
}

async function triggerNextStep() {
  await fetch(edgeFunctionUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ action: "STEP" }),
  });
}

async function checkAndRunLocalTasks() {
  console.log("[Local Worker] Verificando tarefas para PC Local no Supabase...");
  const tasks = await runSql(`
    SELECT id, state_id, title, payload, status 
    FROM public.orchestrator_tasks 
    WHERE target = 'LOCAL_PC' AND status = 'PENDING' 
    ORDER BY id ASC 
    LIMIT 1;
  `);

  if (!tasks || tasks.length === 0) {
    console.log("[Local Worker] Nenhuma tarefa local pendente.");
    return;
  }

  const task = tasks[0];
  console.log(`[Local Worker] Iniciando tarefa #${task.id}: "${task.title}"`);

  await runSql(`UPDATE public.orchestrator_tasks SET status = 'RUNNING', updated_at = NOW() WHERE id = ${task.id};`);

  try {
    const cmd = task.payload?.script_or_command || "echo 'Tarefa local concluida'";
    console.log(`[Local Worker] Executando comando: ${cmd}`);
    const output = execSync(cmd, { encoding: "utf8", stdio: "pipe" });

    await notifyCallback(task.id, task.state_id, "DONE", { output: output.trim() });
    await triggerNextStep();
    console.log(`[Local Worker] Tarefa #${task.id} concluída com sucesso!`);
  } catch (err) {
    console.error(`[Local Worker] Erro na execução da tarefa #${task.id}:`, err.message);
    await notifyCallback(task.id, task.state_id, "FAILED", { error: err.message });
    await triggerNextStep();
  }
}

async function main() {
  const mode = process.argv[2] || "once";
  if (mode === "loop") {
    console.log("[Local Worker] Modo contínuo ativado (verificação a cada 30 segundos). Pressione Ctrl+C para sair.");
    while (true) {
      await checkAndRunLocalTasks();
      await new Promise((r) => setTimeout(r, 30000));
    }
  } else {
    await checkAndRunLocalTasks();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
