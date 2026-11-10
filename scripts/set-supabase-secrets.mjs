import fs from "node:fs";
import { execSync } from "node:child_process";

const token = "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
const projectRef = "usmqtnlsaavxqguwrtix";

async function main() {
  const projectDir = "C:/Users/rodolfo/midiastory-";
  const aiKeys = JSON.parse(fs.readFileSync(`${projectDir}/.secrets/ai-media-keys.json`, "utf8"));
  let ghToken = "";
  try {
    ghToken = execSync("gh auth token", { encoding: "utf8" }).trim();
  } catch (e) {
    console.warn("Could not get gh auth token:", e.message);
  }

  const secretsToSet = [
    { name: "OPENROUTER_API_KEY", value: aiKeys.OPENROUTER_API_KEY },
    { name: "OPENROUTER_BASE_URL", value: aiKeys.OPENROUTER_BASE_URL },
    { name: "GOOGLE_AQ_API_KEY", value: aiKeys.GOOGLE_AQ_API_KEY || "" },
  ];

  if (ghToken) {
    secretsToSet.push({ name: "GH_TOKEN", value: ghToken });
    secretsToSet.push({ name: "GITHUB_TOKEN", value: ghToken });
  }

  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/secrets`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(secretsToSet),
  });

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  }

  console.log("Supabase secrets successfully updated with AI keys and GitHub Token!");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
