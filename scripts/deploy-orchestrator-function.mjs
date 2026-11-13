import fs from "node:fs";
import { getSupabaseToken } from "./lib/supabase-auth.mjs";

const token = getSupabaseToken();
const projectRef = "usmqtnlsaavxqguwrtix";
const slug = "native-orchestrator";
const filePath = "C:/Users/rodolfo/midiastory-/supabase/functions/native-orchestrator/index.ts";

async function main() {
  console.log(`Lendo código fonte de ${filePath}...`);
  const code = fs.readFileSync(filePath, "utf8");

  const form = new FormData();
  form.append(
    "metadata",
    JSON.stringify({
      entrypoint_path: "index.ts",
      name: "IA Nativa Orquestradora",
      verify_jwt: false,
    })
  );

  const blob = new Blob([code], { type: "application/typescript" });
  form.append("file", blob, "index.ts");

  console.log(`Publicando Edge Function '${slug}' no Supabase (${projectRef})...`);
  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/functions/deploy?slug=${slug}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: form,
  });

  const text = await res.text();
  if (!res.ok) {
    throw new Error(`Falha no deploy HTTP ${res.status}: ${text}`);
  }

  console.log("Edge Function publicada com sucesso no Supabase!");
  console.log("Resposta:", text);
}

main().catch((err) => {
  console.error("Erro no deploy:", err.message);
  process.exit(1);
});
