#!/usr/bin/env node
/**
 * Sincronizador Universal de Memoria (Antigravity + Codex + Manus -> Supabase)
 * Uso:
 *   node scripts/sync-agent-memory.mjs read
 *   node scripts/sync-agent-memory.mjs write "Resumo do que foi feito"
 */
import fs from "node:fs";

const mcpConfigPath = ".agents/plugins/supabase/mcp_config.json";
const projectRef = "usmqtnlsaavxqguwrtix";

function getToken() {
  if (process.env.SUPABASE_ACCESS_TOKEN) return process.env.SUPABASE_ACCESS_TOKEN;
  if (fs.existsSync(mcpConfigPath)) {
    const raw = JSON.parse(fs.readFileSync(mcpConfigPath, "utf8"));
    const envToken = raw?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN;
    if (envToken) return envToken.trim();
    const auth = raw?.mcpServers?.supabase?.headers?.Authorization || "";
    return auth.replace(/^Bearer\s+/i, "").trim();
  }
  throw new Error("Token Supabase nao encontrado.");
}

async function runSql(query) {
  const token = getToken();
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  }
  return res.json();
}

async function main() {
  const action = process.argv[2] || "read";
  if (action === "read") {
    const memories = await runSql("SELECT id, content, created_at FROM public.memories ORDER BY id DESC LIMIT 5;");
    console.log("=== ULTIMAS MEMORIAS DO PROJETO MIDIASTORE (SUPABASE) ===");
    for (const m of memories) {
      console.log(`\n[#${m.id} - ${m.created_at}]\n${m.content}`);
    }
  } else if (action === "write") {
    const text = process.argv.slice(3).join(" ").trim();
    if (!text) throw new Error("Informe o texto para gravar.");
    const escaped = text.replace(/'/g, "''");
    await runSql(`INSERT INTO public.memories (content) VALUES ('${escaped}');`);
    console.log("Memoria gravada com sucesso no Supabase!");
  }
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
