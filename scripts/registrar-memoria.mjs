#!/usr/bin/env node
/**
 * REGISTRADOR DE MEMÓRIA & LINHA DO TEMPO AUTÔNOMA (Regra de Ouro Midiastore)
 *
 * Persiste sincronizadamente em:
 * 1. Supabase (public.memories via saveValidatedMemory com hash, categoria e embedding 384d)
 * 2. Repositório (docs/LINHA_DO_TEMPO_AUTONOMA.md)
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { classifyMemoryCategory, saveValidatedMemory, VALID_MEMORY_CATEGORIES } from "./lib/cognitive-memory.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const timelinePath = path.join(projectRoot, "docs", "LINHA_DO_TEMPO_AUTONOMA.md");

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
  const rawCategory = entry.category || classifyMemoryCategory(formattedMemoryText, "episodic");
  const categoria = VALID_MEMORY_CATEGORIES.has(rawCategory) ? rawCategory : "episodic";

  let supabaseMemoryId = null;
  try {
    const res = await saveValidatedMemory({
      content: formattedMemoryText,
      origem: entry.origem || "timeline_registrar",
      categoria,
      confianca: entry.confianca ?? (categoria === "correction" || categoria === "preference" ? 0.96 : 0.9),
      supersedesId: entry.supersedesId || null,
      allowSpeculative: true, // Preserva registros operacionais contendo campos descritivos de erro/tentativa
    });
    supabaseMemoryId = res.id || res.existingId || null;
    console.log(
      `✅ Memória gravada no Supabase (ID: #${supabaseMemoryId}, cat=${categoria}, emb=${res.embeddingModel || "cached"}).`
    );
  } catch (err) {
    console.warn("⚠️ Aviso ao gravar memória no Supabase:", err.message);
  }

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
    category: getArg("--category"),
    supersedesId: getArg("--supersedes") ? Number(getArg("--supersedes")) : null,
  };
}

async function main() {
  const parsed = parseCliArgs();
  if (!parsed.action) {
    console.log(
      "Uso: node scripts/registrar-memoria.mjs --action '...' --studied '...' --attempted '...' --errors '...' --fixes '...' --conclusions '...' [--category ...]"
    );
    process.exit(1);
  }
  await registrarMemoria(parsed);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch((e) => {
    console.error("Erro fatal:", e.message);
    process.exit(1);
  });
}
