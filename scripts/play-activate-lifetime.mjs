import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

function getToken() {
  if (process.env.SUPABASE_ACCESS_TOKEN) return process.env.SUPABASE_ACCESS_TOKEN;
  const mcpPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");
  if (fs.existsSync(mcpPath)) {
    const raw = JSON.parse(fs.readFileSync(mcpPath, "utf8"));
    return raw?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN || "";
  }
  return "";
}

const saPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const saJson = fs.readFileSync(saPath, "utf8");
const token = getToken();

const secretRes = await fetch("https://api.supabase.com/v1/projects/usmqtnlsaavxqguwrtix/secrets", {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  },
  body: JSON.stringify([{ name: "PLAY_SERVICE_ACCOUNT_JSON", value: saJson }]),
});
console.log("Set secret status:", secretRes.status, await secretRes.text());

const fnRes = await fetch("https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/verify-play-purchase", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    packageName: "com.rdf112.midiastore",
    productId: "sweep_pro_monthly",
    purchaseToken: "test-invalid-token-check",
  }),
});
console.log("Edge Function test status:", fnRes.status, await fnRes.text());
