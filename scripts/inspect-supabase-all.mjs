import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

async function runSql(query) {
  const token = getSupabaseToken();
  const res = await fetch(`https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  return res.json();
}

async function main() {
  const tables = await runSql(`
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    ORDER BY table_name;
  `);

  console.log("=== CENSO COMPLETO DO SUPABASE (BANCO: " + SUPABASE_PROJECT_REF + ") ===");
  for (const t of tables) {
    const name = t.table_name;
    const countRes = await runSql(`SELECT count(*) as c FROM public."${name}";`);
    console.log(`• public.${name}: ${countRes[0]?.c} registros`);
  }

  console.log("\n=== ÚLTIMAS 5 MEMÓRIAS SALVAS ===");
  const memories = await runSql(`SELECT id, content, created_at FROM public.memories ORDER BY id DESC LIMIT 5;`);
  for (const m of memories) {
    console.log(`[#${m.id} | ${m.created_at}]\n${m.content}\n`);
  }

  console.log("=== DOCUMENTOS REGISTRADOS ===");
  const docs = await runSql(`SELECT id, name, created_at FROM public.documents ORDER BY id DESC LIMIT 10;`).catch(async () => {
    return await runSql(`SELECT * FROM public.documents ORDER BY id DESC LIMIT 10;`);
  });
  for (const d of docs) {
    console.log(`[Doc #${d.id}]: ${d.name || d.filename || d.title || JSON.stringify(d).slice(0, 80)}`);
  }
}

main().catch(console.error);
