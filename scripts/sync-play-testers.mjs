import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const mcpConfigPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
const packageName = "com.rdf112.midiastore";
const projectRef = "usmqtnlsaavxqguwrtix";

function getSupabaseToken() {
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
  const token = getSupabaseToken();
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  return res.json();
}

async function getPlayAccessToken() {
  const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: key.token_uri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");

  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const signature = sign.sign(key.private_key, "base64url");
  const jwt = `${header}.${payload}.${signature}`;

  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!data.access_token) throw new Error(JSON.stringify(data));
  return data.access_token;
}

async function main() {
  console.log("=== SINCRONIZADOR 24/7 DE TESTADORES (SUPABASE -> GOOGLE PLAY) ===");
  
  // 1. Obter testadores do Supabase
  const testers = await runSql(`
    SELECT id, email, status, created_at
    FROM public.beta_testers
    ORDER BY id ASC;
  `);

  console.log(`Testadores cadastrados no banco: ${testers.length}`);
  testers.forEach(t => console.log(` - [#${t.id}] ${t.email} (${t.status})`));

  // 2. Conectar com a Google Play Developer API
  const token = await getPlayAccessToken();

  const editRes = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: "{}",
  });
  const edit = await editRes.json();
  if (!editRes.ok) throw new Error(`Falha ao criar edit: ${JSON.stringify(edit)}`);

  const testersRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/testers/alpha`,
    { headers: { Authorization: `Bearer ${token}` } }
  );

  const currentTesters = await testersRes.json();
  console.log(`Status Testers Track Alpha (${testersRes.status}):`, JSON.stringify(currentTesters));

  // 3. Atualizar status no Supabase
  if (testers.length > 0) {
    await runSql(`
      UPDATE public.beta_testers
      SET status = 'READY_FOR_CONSOLE_INVITE', synced_to_play_at = NOW()
      WHERE status = 'PENDING_PLAY_INVITE';
    `);
    console.log("✅ Status atualizado no Supabase para READY_FOR_CONSOLE_INVITE!");
  }

  // 4. Salvar lista formatada pronta para importação no Console
  const emailsCsv = testers.map(t => t.email).join("\n");
  const exportPath = path.join(projectRoot, "docs", "campaigns", "lista-20-testadores-google-play.txt");
  fs.writeFileSync(exportPath, emailsCsv, "utf8");
  console.log(`📄 Lista de e-mails exportada para: ${exportPath}`);
}

main().catch(e => {
  console.error("Erro:", e.message);
  process.exit(1);
});
