import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const mcpConfigPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
const projectRef = "usmqtnlsaavxqguwrtix";
const MAX_SAFE_MB = 400; // Limite de alerta (Free tier e 500 MB)

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

export async function checkAndMaintain() {
  const now = new Date().toLocaleTimeString("pt-BR");
  console.log(`\n[${now}] === SUPABASE STORAGE & HEALTH MONITOR ===`);

  // 1. Checar tamanho do banco de dados
  const dbSizeRes = await runSql(`
    SELECT 
      pg_size_pretty(pg_database_size(current_database())) as total_db_size,
      pg_database_size(current_database()) as total_bytes;
  `);
  const totalMb = (Number(dbSizeRes[0]?.total_bytes || 0) / (1024 * 1024)).toFixed(2);
  console.log(`Tamanho Total do Banco: ${dbSizeRes[0]?.total_db_size} (${totalMb} MB / 500 MB)`);

  if (Number(totalMb) > MAX_SAFE_MB) {
    console.warn(`⚠️ ALERTA: Armazenamento atingiu ${totalMb} MB! Iniciando limpeza profunda...`);
    // Limpeza de logs antigos
    await runSql(`
      DELETE FROM public.orchestrator_logs WHERE created_at < NOW() - INTERVAL '7 days';
      DELETE FROM public.orchestrator_tasks WHERE status = 'DONE' AND created_at < NOW() - INTERVAL '14 days';
    `);
    console.log("Limpeza de logs e tarefas antigas executada.");
  }

  // 2. Tabela de testadores
  const countRes = await runSql(`
    SELECT 
      count(*) as total_rows,
      count(DISTINCT lower(trim(email))) as unique_emails
    FROM public.beta_testers;
  `);
  console.log(`Testadores Cadastrados: ${countRes[0]?.total_rows} (${countRes[0]?.unique_emails} únicos)`);

  // 3. Deduplicação preventiva
  await runSql(`
    DELETE FROM public.beta_testers a
    USING public.beta_testers b
    WHERE a.id > b.id 
      AND lower(trim(a.email)) = lower(trim(b.email));
  `);

  console.log(`Status: Proteção ativa. Uso atual: ${((Number(totalMb) / 500) * 100).toFixed(1)}% da cota.`);
}

async function main() {
  const isWatch = process.argv.includes("--watch");
  if (isWatch) {
    console.log("Modo sentinela contínuo ativado (verificação a cada 60 minutos)...");
    await checkAndMaintain();
    setInterval(async () => {
      try {
        await checkAndMaintain();
      } catch (e) {
        console.error("Falha no monitoramento:", e.message);
      }
    }, 60 * 60 * 1000);
  } else {
    await checkAndMaintain();
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main().catch(err => {
    console.error("Erro na execucao:", err.message);
    process.exit(1);
  });
}
