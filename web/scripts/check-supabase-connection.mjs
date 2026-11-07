import { readFile } from "node:fs/promises";

const envText = await readFile(new URL("../../.env.local", import.meta.url), "utf8");
const env = Object.fromEntries(envText.split(/\r?\n/).filter((line) => line.includes("="))
  .map((line) => [line.slice(0, line.indexOf("=")), line.slice(line.indexOf("=") + 1)]));

if (!env.NEXT_PUBLIC_SUPABASE_URL || !env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY) {
  console.error("Faltam as configurações públicas do Supabase no .env.local.");
  process.exit(1);
}

try {
  const response = await fetch(`${env.NEXT_PUBLIC_SUPABASE_URL}/auth/v1/health`, {
    headers: { apikey: env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY },
    signal: AbortSignal.timeout(15000),
  });
  console.log(`Supabase REST respondeu com HTTP ${response.status}.`);
  process.exit(response.ok ? 0 : 1);
} catch {
  console.error("Não foi possível alcançar o endpoint REST do Supabase.");
  process.exit(1);
}
