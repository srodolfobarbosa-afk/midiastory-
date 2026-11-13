import fs from "node:fs";
import { execSync } from "node:child_process";
import { getSupabaseToken } from "./lib/supabase-auth.mjs";

const projectDir = "C:/Users/rodolfo/midiastory-";
const repo = "srodolfobarbosa-afk/midiastory-";

function setSecret(name, value) {
  console.log(`Configurando secret ${name} no GitHub (${repo})...`);
  execSync(`gh secret set ${name} -R ${repo}`, {
    input: value,
    encoding: "utf8",
  });
}

function main() {
  const token = getSupabaseToken();
  const projectRef = "usmqtnlsaavxqguwrtix";
  const aiKeys = fs.readFileSync(`${projectDir}/.secrets/ai-media-keys.json`, "utf8");
  const playAccount = fs.readFileSync(`${projectDir}/.secrets/play-service-account.json`, "utf8");

  setSecret("SUPABASE_ACCESS_TOKEN", token);
  setSecret("SUPABASE_PROJECT_REF", projectRef);
  setSecret("SUPABASE_URL", `https://${projectRef}.supabase.co`);
  setSecret("AI_MEDIA_KEYS_JSON", aiKeys);
  setSecret("PLAY_SERVICE_ACCOUNT_JSON", playAccount);

  console.log("Todos os secrets foram configurados com sucesso no GitHub Actions!");
}

main();
