#!/usr/bin/env node
/**
 * MOTOR DE AUTO-APERFEIÇOAMENTO E SELF-HEALING (Antigravity + Codex + Supabase)
 * 
 * Ciclo de Autocorreção:
 * 1. Executa a validação (ex: testes de compilação ou lints)
 * 2. Se falhar: extrai o erro e gera a hipótese de correção
 * 3. Registra a lição aprendida no Supabase (public.memories / orchestrator_tasks)
 * 4. Aciona o OpenAI Codex CLI (codex.exe) para aplicar a correção no código
 * 5. Revalida a compilação. Repete até 3 tentativas controladas.
 */

import fs from "node:fs";
import path from "node:path";
import { execSync, spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { getSupabaseToken } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const codexPath = "C:\\Users\\rodolfo\\AppData\\Local\\Programs\\OpenAI\\Codex\\bin\\codex.exe";
const supabaseRef = "usmqtnlsaavxqguwrtix";
const supabaseToken = getSupabaseToken();

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

async function recordMemory(lesson) {
  const escaped = lesson.replace(/'/g, "''");
  await runSql(`
    INSERT INTO public.memories (content)
    VALUES ('[Self-Healing] ${escaped}');
  `);
}

function runVerificationCommand(cmd) {
  console.log(`\n🔍 [VERIFICAÇÃO] Executando: "${cmd}"...`);
  try {
    const output = execSync(cmd, { cwd: projectRoot, encoding: "utf8", stdio: "pipe" });
    return { success: true, output };
  } catch (err) {
    return { 
      success: false, 
      output: (err.stdout || "") + "\n" + (err.stderr || "") + "\n" + err.message 
    };
  }
}

async function triggerCodexFix(errorLog, attemptNumber) {
  console.log(`\n🤖 [SELF-HEALING TENTATIVA ${attemptNumber}] Acionando OpenAI Codex CLI para correção...`);
  
  const prompt = `Analise este erro de compilação/teste no projeto Android Midiastore e aplique a correção mínima necessária nos arquivos correspondentes:\n\n${errorLog.slice(-1500)}`;
  
  if (!fs.existsSync(codexPath)) {
    throw new Error(`Codex CLI não encontrado em: ${codexPath}`);
  }

  const result = spawnSync(
    codexPath,
    ["exec", "--cd", projectRoot, "--dangerously-bypass-approvals-and-sandbox", prompt],
    { encoding: "utf8", maxBuffer: 10 * 1024 * 1024 }
  );

  return result.stdout || result.stderr || "Executado";
}

export async function runSelfHealingCycle(validationCommand, maxAttempts = 3) {
  console.log("=== INICIANDO CICLO DE AUTO-APERFEIÇOAMENTO (SELF-HEALING) ===");
  console.log(`Comando alvo: ${validationCommand}`);
  console.log(`Limite máximo de tentativas: ${maxAttempts}\n`);

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const check = runVerificationCommand(validationCommand);

    if (check.success) {
      console.log(`\n🎉 SUCESSO! Validação aprovada 100% na tentativa ${attempt}!`);
      await recordMemory(`Comando "${validationCommand}" validado com sucesso na tentativa ${attempt}. Código íntegro.`);
      return { success: true, attempts: attempt };
    }

    console.warn(`\n⚠️ Falha detectada na tentativa ${attempt}/${maxAttempts}!`);
    const errorSnippet = check.output.trim().slice(-600);
    console.log("Trecho do erro:", errorSnippet);

    await recordMemory(`Falha na tentativa ${attempt} de "${validationCommand}": ${errorSnippet.slice(0, 180)}`);

    if (attempt < maxAttempts) {
      await triggerCodexFix(check.output, attempt);
    } else {
      console.error(`\n❌ Limite máximo de ${maxAttempts} tentativas atingido sem convergência.`);
      return { success: false, attempts: attempt, lastError: check.output };
    }
  }
}

async function main() {
  const cmd = process.argv.slice(2).join(" ") || "gradlew.bat testDebugUnitTest";
  await runSelfHealingCycle(cmd, 3);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(err => {
    console.error("Erro fatal no Self-Healing:", err.message);
    process.exit(1);
  });
}
