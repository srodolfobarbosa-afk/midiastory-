import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const packageName = "com.rdf112.midiastore";
const projectRef = "usmqtnlsaavxqguwrtix";

function resolveRepoFile(relPath) {
  const localPath = path.join(projectRoot, relPath);
  if (fs.existsSync(localPath)) return localPath;
  try {
    const gitCommonDir = execFileSync("git", ["rev-parse", "--git-common-dir"], {
      cwd: projectRoot,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
    if (gitCommonDir) {
      const mainRepoRoot = path.resolve(projectRoot, gitCommonDir, "..");
      const mainPath = path.join(mainRepoRoot, relPath);
      if (fs.existsSync(mainPath)) return mainPath;
    }
  } catch {
    // ignore
  }
  return localPath;
}

function getSupabaseToken() {
  if (process.env.SUPABASE_ACCESS_TOKEN) return process.env.SUPABASE_ACCESS_TOKEN.trim();
  const mcpConfigPath = resolveRepoFile(path.join(".agents", "plugins", "supabase", "mcp_config.json"));
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
  const keyPath = resolveRepoFile(path.join(".secrets", "play-service-account.json"));
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

function isSyntheticTestRow(row) {
  const email = (row.email || "").toLowerCase();
  const source = (row.source || "").toLowerCase();
  return email.startsWith("teste_") || source === "automated_verification";
}

async function main() {
  console.log("=== SINCRONIZADOR 24/7 DE TESTADORES (SUPABASE -> GOOGLE PLAY) ===");

  const testers = await runSql(`
    SELECT id, email, source, status, created_at
    FROM public.beta_testers
    ORDER BY id ASC;
  `);

  const realTesters = testers.filter(t => !isSyntheticTestRow(t));
  console.log(`Testadores cadastrados no banco: ${testers.length} total (${realTesters.length} reais, ${testers.length - realTesters.length} sintéticos)`);
  testers.forEach(t => console.log(` - [#${t.id}] ${t.email} [source=${t.source || "n/a"}] (${t.status})`));

  const token = await getPlayAccessToken();

  const editRes = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: "{}",
  });
  const edit = await editRes.json();
  if (!editRes.ok) throw new Error(`Falha ao criar edit: ${JSON.stringify(edit)}`);

  try {
    const testersRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/testers/alpha`,
      { headers: { Authorization: `Bearer ${token}` } }
    );

    const currentTesters = await testersRes.json();
    console.log(`Status Testers Track Alpha (${testersRes.status}):`, JSON.stringify(currentTesters));
  } finally {
    await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
  }

  if (testers.length > 0) {
    await runSql(`
      UPDATE public.beta_testers
      SET status = 'READY_FOR_CONSOLE_INVITE', synced_to_play_at = NOW()
      WHERE status = 'PENDING_PLAY_INVITE';
    `);
    console.log("✅ Status atualizado no Supabase para READY_FOR_CONSOLE_INVITE!");
  }

  const emailsCsv = realTesters.map(t => t.email).join("\n");
  const exportPath = path.join(projectRoot, "docs", "campaigns", "lista-20-testadores-google-play.txt");
  fs.mkdirSync(path.dirname(exportPath), { recursive: true });
  fs.writeFileSync(exportPath, emailsCsv, "utf8");
  console.log(`📄 Lista de e-mails reais exportada para: ${exportPath}`);
}

main().catch(e => {
  console.error("Erro:", e.message);
  process.exit(1);
});
