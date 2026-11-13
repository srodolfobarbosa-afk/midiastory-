#!/usr/bin/env node
/**
 * REGISTRADOR DE MEMÓRIA & LINHA DO TEMPO AUTÔNOMA (Regra de Ouro Midiastore)
 * 
 * Regra: Após qualquer ação relevante, anote imediatamente:
 * - O que leu e estudou
 * - O que tentou
 * - O que errou
 * - Como corrigiu
 * - O que concluiu
 * 
 * Persiste sincronizadamente em:
 * 1. Supabase (tabela public.memories e public.orchestrator_tasks)
 * 2. Repositório (docs/LINHA_DO_TEMPO_AUTONOMA.md)
 * 
 * Uso:
 *   node scripts/registrar-memoria.mjs \
 *     --action "Implementação X" \
 *     --studied "Doc Y, código Z" \
 *     --attempted "Tentativa de compilação" \
 *     --errors "Erro 404 em rota antiga" \
 *     --fixes "Atualizado para modelo ativo" \
 *     --conclusions "Sistema operando 100%"
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const projectRef = SUPABASE_PROJECT_REF;
const timelinePath = path.join(projectRoot, "docs", "LINHA_DO_TEMPO_AUTONOMA.md");

function getToken() {
  return getSupabaseToken();
}

async function runSql(query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${getToken()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  return res.json();
}

export async function registrarMemoria(entry) {
  const now = new Date();
  const timestampIso = now.toISOString();
  const timestampBr = now.toLocaleString("pt-BR");

  const record = {
    timestamp: timestampIso,
    action: entry.action || "Ação não especificada",
    studied: entry.studied || "N/A",
    attempted: entry.attempted || "N/A",
    errors: entry.errors || "Nenhum erro registrado",
    fixes: entry.fixes || "Nenhuma correção necessária",
    conclusions: entry.conclusions || "Concluído com sucesso",
  };

  const formattedMemoryText = `[${timestampBr}] ${record.action} | Estudou: ${record.studied} | Tentou: ${record.attempted} | Errou: ${record.errors} | Corrigiu: ${record.fixes} | Conclusão: ${record.conclusions}`;

  // 1. Gravar no Supabase (public.memories)
  let supabaseMemoryId = null;
  try {
    const escapedContent = formattedMemoryText.replace(/'/g, "''");
    const insertRes = await runSql(`
      INSERT INTO public.memories (content, created_at)
      VALUES ('${escapedContent}', NOW())
      RETURNING id;
    `);
    supabaseMemoryId = insertRes[0]?.id || null;
    console.log(`✅ Memória gravada no Supabase (ID: #${supabaseMemoryId}).`);
  } catch (err) {
    console.warn("⚠️ Aviso ao gravar memória no Supabase:", err.message);
  }

  // 2. Gravar no Repositório (docs/LINHA_DO_TEMPO_AUTONOMA.md)
  try {
    const mdEntry = `
### 📍 [${timestampBr}] ${record.action}
- **📖 Estudou / Leu:** ${record.studied}
- **⚙️ Tentou / Executou:** ${record.attempted}
- **⚠️ Erros Encontrados:** ${record.errors}
- **🔧 Como Corrigiu:** ${record.fixes}
- **🎯 Conclusão:** ${record.conclusions}
- *Registro Supabase:* Memory #${supabaseMemoryId || "Local"}

---
`;

    if (!fs.existsSync(timelinePath)) {
      const header = `# 📜 Linha do Tempo Autônoma do Ecossistema Midiastore\n\nEste documento registra a evolução contínua, os experimentos, erros e correções em conformidade com a Regra de Ouro.\n\n---\n`;
      fs.writeFileSync(timelinePath, header + mdEntry, "utf8");
    } else {
      fs.appendFileSync(timelinePath, mdEntry, "utf8");
    }
    console.log(`✅ Linha do tempo atualizada em: ${timelinePath}`);
  } catch (err) {
    console.error("❌ Erro ao atualizar arquivo de linha do tempo:", err.message);
  }

  return { supabaseMemoryId, record };
}

function parseCliArgs() {
  const args = process.argv.slice(2);
  const getArg = (flag) => {
    const idx = args.indexOf(flag);
    return idx > -1 && args[idx + 1] ? args[idx + 1] : "";
  };

  return {
    action: getArg("--action"),
    studied: getArg("--studied"),
    attempted: getArg("--attempted"),
    errors: getArg("--errors"),
    fixes: getArg("--fixes"),
    conclusions: getArg("--conclusions"),
  };
}

async function main() {
  const parsed = parseCliArgs();
  if (!parsed.action) {
    console.log("Uso: node scripts/registrar-memoria.mjs --action '...' --studied '...' --attempted '...' --errors '...' --fixes '...' --conclusions '...'");
    process.exit(1);
  }
  await registrarMemoria(parsed);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(e => {
    console.error("Erro fatal:", e.message);
    process.exit(1);
  });
}
