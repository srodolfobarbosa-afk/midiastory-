import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const ghToken = execSync("gh auth token", { encoding: "utf8" }).trim();
const repo = "srodolfobarbosa-afk/midiastory-";

async function pushFileToMain(relPath, commitMsg) {
  const fullPath = path.join(projectRoot, relPath);
  const content = fs.readFileSync(fullPath, "utf8");
  const b64 = Buffer.from(content).toString("base64");

  let sha;
  const checkRes = await fetch(`https://api.github.com/repos/${repo}/contents/${relPath}?ref=main`, {
    headers: {
      Authorization: `Bearer ${ghToken}`,
      Accept: "application/vnd.github.v3+json",
      "User-Agent": "Midiastory-Sync",
    },
  });
  if (checkRes.ok) {
    const data = await checkRes.json();
    sha = data.sha;
  }

  const body = {
    message: commitMsg,
    content: b64,
    branch: "main",
  };
  if (sha) body.sha = sha;

  const putRes = await fetch(`https://api.github.com/repos/${repo}/contents/${relPath}`, {
    method: "POST",
    method: "PUT",
    headers: {
      Authorization: `Bearer ${ghToken}`,
      Accept: "application/vnd.github.v3+json",
      "User-Agent": "Midiastory-Sync",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const resData = await putRes.json();
  console.log(`[Push to main] ${relPath}: HTTP ${putRes.status}`);
  if (!putRes.ok) {
    console.warn(`Erro ao publicar ${relPath}:`, resData.message);
  }
}

async function main() {
  await pushFileToMain("scripts/supabase-maintenance.mjs", "feat(maintenance): sync supabase maintenance to main");
  await pushFileToMain(".github/workflows/fabrica-autonoma-ubuntu.yml", "ci(cloud): add autonomous factory workflow on main");
  await pushFileToMain("scripts/fabrica-autonoma-24h.mjs", "feat(engine): sync active builder engine to main");
  await pushFileToMain("scripts/ai-failover-router.mjs", "feat(ai): sync failover router with Gemini 3.8/Argon to main");
  await pushFileToMain("scripts/registrar-memoria.mjs", "feat(memory): sync memory registrar to main");
}

main().catch(console.error);
