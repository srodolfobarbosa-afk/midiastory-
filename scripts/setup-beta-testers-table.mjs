import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const mcpConfigPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
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
  const sql = `
CREATE TABLE IF NOT EXISTS public.beta_testers (
  id BIGSERIAL PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  device_model TEXT,
  source TEXT DEFAULT 'web_funnel',
  status TEXT DEFAULT 'PENDING_PLAY_INVITE',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  synced_to_play_at TIMESTAMPTZ
);

ALTER TABLE public.beta_testers ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE tablename = 'beta_testers' AND policyname = 'Allow public insert beta_testers'
  ) THEN
    CREATE POLICY "Allow public insert beta_testers"
      ON public.beta_testers FOR INSERT
      WITH CHECK (true);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_policies WHERE tablename = 'beta_testers' AND policyname = 'Allow service read beta_testers'
  ) THEN
    CREATE POLICY "Allow service read beta_testers"
      ON public.beta_testers FOR SELECT
      USING (true);
  END IF;
END $$;
  `;

  const res = await runSql(sql);
  console.log("✅ Tabela public.beta_testers configurada com sucesso no Supabase!");
}

main().catch(err => {
  console.error("Erro:", err.message);
  process.exit(1);
});
