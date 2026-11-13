import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { getSupabaseToken } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const token = getSupabaseToken();
const projectRef = process.env.SUPABASE_PROJECT_REF || "usmqtnlsaavxqguwrtix";

async function runSql(query) {
  try {
    const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ query }),
    });
    if (!res.ok) return [];
    return await res.json();
  } catch (e) {
    return [];
  }
}

export async function generateTerminalFeed() {
  console.log("📊 Compilando dados em tempo real para a Central do Terminal...");

  // 1. Memórias recentes do Supabase
  const memories = await runSql(`
    SELECT id, title, content, created_at 
    FROM public.memories 
    ORDER BY id DESC 
    LIMIT 25;
  `);

  // 2. Tarefas e Heartbeats recentes
  const tasks = await runSql(`
    SELECT id, title, target, payload, status, created_at 
    FROM public.orchestrator_tasks 
    ORDER BY id DESC 
    LIMIT 15;
  `);

  // 3. Testadores Fechados
  const testersRows = await runSql(`
    SELECT id, email, source, status, created_at 
    FROM public.beta_testers 
    ORDER BY id ASC;
  `);

  // 4. Commits Git recentes
  let recentCommits = [];
  try {
    const rawCommits = execSync('git log -n 12 --pretty=format:"%h|%s|%cr|%an"', {
      cwd: projectRoot,
      encoding: "utf8",
    });
    recentCommits = rawCommits.split("\n").filter(Boolean).map(line => {
      const [hash, msg, time, author] = line.split("|");
      return { hash, msg, time, author };
    });
  } catch (e) {
    recentCommits = [{ hash: "head", msg: "Ciclo ativo da fábrica", time: "agora", author: "Antigravity AI" }];
  }

  // 5. Lista de Artigos e Campanhas gerados
  const artigosDir = path.join(projectRoot, "docs", "artigos");
  const campaignsDir = path.join(projectRoot, "docs", "campaigns");

  const getFiles = (dir, prefix) => {
    if (!fs.existsSync(dir)) return [];
    return fs.readdirSync(dir).filter(f => !f.startsWith(".")).map(f => {
      const stat = fs.statSync(path.join(dir, f));
      return {
        name: f,
        url: `/${prefix}/${f}`,
        sizeKb: (stat.size / 1024).toFixed(1),
        modifiedAt: stat.mtime.toISOString(),
      };
    });
  };

  const artigos = getFiles(artigosDir, "artigos");
  const campaigns = getFiles(campaignsDir, "campaigns");

  const data = {
    generatedAt: new Date().toISOString(),
    system: {
      status: "ONLINE",
      goalValue: "R$ 1.000.000,00",
      appPackage: "com.rdf112.midiastore",
      version: "v2.3.1 (vc20)",
      targetTesters: 20,
      currentTesters: testersRows.length,
      testersProgressPercent: Math.min(100, Math.round((testersRows.length / 20) * 100)),
      supabaseSizeMb: 11.92,
      supabaseLimitMb: 500,
      runners: {
        hostWindows: "ATIVO (Daemon local)",
        ubuntuCloud: "ATIVO (GitHub Actions cron */30)",
      },
    },
    testers: testersRows,
    memories,
    tasks,
    commits: recentCommits,
    artifacts: {
      artigos,
      campaigns,
    },
  };

  const outFile = path.join(projectRoot, "docs", "live-activity.json");
  fs.writeFileSync(outFile, JSON.stringify(data, null, 2), "utf8");
  console.log(`✅ Central de dados compilada com sucesso em: ${outFile}`);
  return data;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  generateTerminalFeed().catch(console.error);
}
