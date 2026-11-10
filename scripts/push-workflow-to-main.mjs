import fs from "node:fs";
import { execSync } from "node:child_process";

const workflowPath = "C:/Users/rodolfo/midiastory-/.github/workflows/orchestrator-cloud-hands.yml";
const content = fs.readFileSync(workflowPath, "utf8");
const b64 = Buffer.from(content).toString("base64");

async function pushFile() {
  const ghToken = execSync("gh auth token", { encoding: "utf8" }).trim();
  const repo = "srodolfobarbosa-afk/midiastory-";
  const path = ".github/workflows/orchestrator-cloud-hands.yml";

  let sha;
  const checkRes = await fetch(`https://api.github.com/repos/${repo}/contents/${path}?ref=main`, {
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
    message: "feat: add orchestrator-cloud-hands workflow 24/7",
    content: b64,
    branch: "main",
  };
  if (sha) body.sha = sha;

  const putRes = await fetch(`https://api.github.com/repos/${repo}/contents/${path}`, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${ghToken}`,
      Accept: "application/vnd.github.v3+json",
      "User-Agent": "Midiastory-Deployer",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  const resData = await putRes.json();
  console.log("GitHub API Status:", putRes.status);
  if (!putRes.ok) {
    throw new Error(`Erro ao publicar no GitHub: ${JSON.stringify(resData)}`);
  }
  console.log("Workflow orchestrator-cloud-hands.yml publicado diretamente na branch main do GitHub com sucesso!");
}

pushFile().catch((err) => {
  console.error(err);
  process.exit(1);
});
