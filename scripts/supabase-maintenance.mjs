import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

const projectRef = SUPABASE_PROJECT_REF;
const MAX_SAFE_MB = 400; // Limite de alerta (Free tier e 500 MB)

async function runSql(query) {
  const token = getSupabaseToken();
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
    // Limpeza de logs e telemetria antiga
    await runSql(`
      DELETE FROM public.orchestrator_logs WHERE created_at < NOW() - INTERVAL '7 days';
      DELETE FROM public.orchestrator_tasks WHERE status = 'DONE' AND created_at < NOW() - INTERVAL '14 days';
      DELETE FROM public.web_page_events WHERE created_at < NOW() - INTERVAL '30 days';
    `);
    console.log("Limpeza de logs, telemetria antiga (>30d) e tarefas antigas executada.");
  }

  // 2. Tabela de testadores e funil pre-lancamento
  const countRes = await runSql(`
    SELECT 
      count(*) as total_rows,
      count(DISTINCT lower(trim(email))) as unique_emails
    FROM public.beta_testers;
  `);
  console.log(`Testadores / Fila de Espera: ${countRes[0]?.total_rows} (${countRes[0]?.unique_emails} únicos)`);

  const statsRes = await runSql(`SELECT public.get_prelaunch_stats() AS stats;`).catch(() => []);
  if (statsRes[0]?.stats) {
    const s = statsRes[0].stats;
    console.log(`Telemetria Web: ${s.total_visits} visitas (${s.unique_visitors} únicos, ${s.visits_24h} em 24h, ${s.share_clicks} compartilhamentos)`);
  }

  // 3. Deduplicação preventiva
  await runSql(`
    DELETE FROM public.beta_testers a
    USING public.beta_testers b
    WHERE a.id > b.id 
      AND lower(trim(a.email)) = lower(trim(b.email));
  `);

  // 4. Auditoria de RLS em tabelas public.*
  const rlsMissing = await runSql(`
    SELECT tablename
    FROM pg_tables
    WHERE schemaname = 'public' AND rowsecurity = false
    ORDER BY tablename;
  `);
  if (rlsMissing.length > 0) {
    console.warn(
      `⚠️ ALERTA RLS: Tabelas public sem Row Level Security ativo: ${rlsMissing.map((r) => r.tablename).join(", ")}`
    );
  } else {
    console.log("RLS Check: Todas as tabelas public.* possuem Row Level Security ativo.");
  }

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

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main().catch(err => {
    console.error("Erro na execucao:", err.message);
    process.exit(1);
  });
}
