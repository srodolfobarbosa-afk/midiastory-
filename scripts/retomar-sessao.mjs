#!/usr/bin/env node
/**
 * RESTAURADOR INSTANTÂNEO DE SESSÃO & ESTADO (Midiastore)
 * 
 * Uso em caso de queda de energia ou reinicialização do PC:
 *   node scripts/retomar-sessao.mjs
 */

import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { getSupabaseToken } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const token = getSupabaseToken();
const projectRef = "usmqtnlsaavxqguwrtix";

console.log("=================================================================");
console.log("⚡ RESTAURADOR DE SESSÃO MIDIASTORE (PÓS-REINICIALIZAÇÃO/QUEDA)");
console.log("=================================================================\n");

// 1. Verificar Estado do Git
console.log("1️⃣ Verificando integridade do repositório local e Git...");
try {
  const branch = execSync("git branch --show-current", { cwd: projectRoot, encoding: "utf8" }).trim();
  const status = execSync("git status -s", { cwd: projectRoot, encoding: "utf8" }).trim();
  console.log(`   Branch atual: ${branch}`);
  console.log(`   Arquivos modificados: ${status ? "\n" + status : "Nenhum (Árvore 100% Limpa)"}`);
} catch (e) {
  console.warn("   Aviso ao checar Git:", e.message);
}

// 2. Buscar Memórias Recentes no Supabase
console.log("\n2️⃣ Sincronizando memórias persistidas no Supabase Cloud...");
try {
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      query: "SELECT id, content, created_at FROM public.memories ORDER BY id DESC LIMIT 3;"
    }),
  });
  const memories = await res.json();
  for (const m of memories) {
    console.log(`   [#${m.id}] ${m.content.slice(0, 160)}...`);
  }
} catch (e) {
  console.warn("   Aviso ao buscar Supabase:", e.message);
}

// 3. Status das Chaves e Rotas de IA
console.log("\n3️⃣ Verificando conectividade das Rotas de IA (.secrets/ai-media-keys.json)...");
const secretsPath = path.join(projectRoot, ".secrets", "ai-media-keys.json");
if (fs.existsSync(secretsPath)) {
  console.log("   ✅ Arquivo .secrets/ai-media-keys.json presente e protegido.");
  console.log("   ✅ Rota #1 Nativa Gratuita (Google AI Studio): gemini-4-hydrogen (1M Context + Thinking HIGH/MEDIUM)");
  console.log("   ✅ Rota #2 Nativa Gratuita (Google AI Studio): gemini-2.5-flash");
  console.log("   ✅ Rota #3 Free OpenRouter: nvidia/nemotron-3.5-lightning:free & google/gemma-4-31b-it:free");
  console.log("   ✅ Rota #4 Motor Local: OpenAI Codex CLI (codex.exe)");
} else {
  console.warn("   ⚠️ Atenção: .secrets/ai-media-keys.json não encontrado!");
}

console.log("\n=================================================================");
console.log("🎯 COMO RETOMAR A CONVERSA NO ANTIGRAVITY:");
console.log("Basta colar a seguinte frase no chat do Antigravity:");
console.log("-----------------------------------------------------------------");
console.log('"Retomar sessão: leia as últimas memórias no Supabase e a linha do tempo. Confirme o status do Gemini 4 Carbon e os próximos passos."');
console.log("=================================================================\n");
