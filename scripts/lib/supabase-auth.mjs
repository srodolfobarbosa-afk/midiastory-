import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..", "..");
const defaultMcpConfigPath = path.join(projectRoot, ".agents", "plugins", "supabase", "mcp_config.json");

export const SUPABASE_PROJECT_REF = process.env.SUPABASE_PROJECT_REF || "usmqtnlsaavxqguwrtix";

/**
 * Resolves the Supabase management access token from the environment or the local
 * untracked MCP configuration file, failing closed instead of embedding credentials.
 */
export function getSupabaseToken() {
  const fromEnv = process.env.SUPABASE_ACCESS_TOKEN?.trim();
  if (fromEnv) {
    return fromEnv;
  }

  if (fs.existsSync(defaultMcpConfigPath)) {
    const config = JSON.parse(fs.readFileSync(defaultMcpConfigPath, "utf8"));
    const envToken = config?.mcpServers?.supabase?.env?.SUPABASE_ACCESS_TOKEN?.trim();
    if (envToken) {
      return envToken;
    }
    const authHeader = config?.mcpServers?.supabase?.headers?.Authorization || "";
    const bearerToken = authHeader.replace(/^Bearer\s+/i, "").trim();
    if (bearerToken) {
      return bearerToken;
    }
  }

  throw new Error(
    "Token Supabase nao encontrado. Defina SUPABASE_ACCESS_TOKEN no ambiente ou em .agents/plugins/supabase/mcp_config.json."
  );
}
