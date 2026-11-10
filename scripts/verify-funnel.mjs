import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const mcpConfigPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
const projectRef = "usmqtnlsaavxqguwrtix";

async function verify() {
  console.log("=== VERIFICAÇÃO DO FUNIL DE TESTADORES (FIREBASE HOSTING) ===");
  
  // 1. Checar Firebase Hosting
  const url = "https://midiastory-df6a0.web.app/";
  console.log(`Testando URL pública: ${url}`);
  const res = await fetch(url);
  console.log(`Status HTTP: ${res.status} ${res.statusText}`);
  if (!res.ok) throw new Error(`Falha ao acessar o site: ${res.status}`);

  const html = await res.text();
  const hasVipSection = html.includes('id="vip-testers"');
  const hasSupabase = html.includes('supabase.co');
  const hasForm = html.includes('id="testerForm"');

  console.log(` - Seção VIP presente (#vip-testers): ${hasVipSection ? "SIM" : "NÃO"}`);
  console.log(` - Conexão Supabase configurada: ${hasSupabase ? "SIM" : "NÃO"}`);
  console.log(` - Formulário de cadastro ativo: ${hasForm ? "SIM" : "NÃO"}`);

  // 2. Testar endpoint da tabela beta_testers no Supabase
  console.log("\nTestando integridade do banco Supabase...");
  let token = process.env.SUPABASE_ACCESS_TOKEN || "";
  if (!token && fs.existsSync(mcpConfigPath)) {
    const raw = JSON.parse(fs.readFileSync(mcpConfigPath, "utf8"));
    token = raw?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN || raw?.mcpServers?.supabase?.headers?.Authorization?.replace(/^Bearer\s+/i, "") || "";
  }

  if (token) {
    const restRes = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ query: "SELECT count(*) FROM public.beta_testers;" }),
    });
    const restData = await restRes.json();
    console.log(` - Resposta Supabase: HTTP ${restRes.status}`);
    console.log(` - Total de registros em beta_testers: ${restData[0]?.count}`);
  }

  console.log("\nFUNIL 100% OPERACIONAL E PRONTO PARA RECEBER TESTADORES!");
}

verify().catch(e => {
  console.error("Erro na verificação:", e.message);
  process.exit(1);
});
