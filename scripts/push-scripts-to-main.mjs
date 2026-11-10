import fs from "node:fs";
import { execSync } from "node:child_process";

const projectDir = "C:/Users/rodolfo/midiastory-";
const ghToken = execSync("gh auth token", { encoding: "utf8" }).trim();
const repo = "srodolfobarbosa-afk/midiastory-";

const filesToPush = [
  "scripts/equipe-ia.mjs",
  "AGENTS.md",
  ".agents/mcp_config.example.json",
];

async function pushFile(filePath) {
  const fullPath = `${projectDir}/${filePath}`;
  const content = fs.readFileSync(fullPath, "utf8");
  const b64 = Buffer.from(content).toString("base64");

  let sha;
  const checkRes = await fetch(`https://api.github.com/repos/${repo}/contents/${filePath}?ref=main`, {
    headers: {
      Authorization: `Bearer ${ghToken}`,
      Accept: "application/vnd.github.v3+json",
      "User-Agent": "Midiastory-Deployer",
    },
  });
  if (checkRes.ok) {
    const data = await checkRes.json();
    sha = data.sha;
  }

  const body = {
    message: `feat: add ${filePath} to main`,
    content: b64,
    branch: "main",
  };
  if (sha) body.sha = sha;

  const putRes = await fetch(`https://api.github.com/repos/${repo}/contents/${filePath}`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${ghToken}`,
      Accept: "application/vnd.github.v3+json",
      "User-Agent": "Midiastory-Deployer",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  if (!putRes.ok) {
    throw new Error(`Falha ao enviar ${filePath}: ${await putRes.text()}`);
  }
  console.log(`Arquivo ${filePath} sincronizado no GitHub main com sucesso!`);
}

async function main() {
  for (const f of filesToPush) {
    await pushFile(f);
  }
  console.log("Todos os scripts sincronizados no main!");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
