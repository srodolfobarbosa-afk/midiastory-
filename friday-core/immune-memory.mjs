#!/usr/bin/env node
/**
 * FRIDAY Core — Memória de Erros Imunes (Anti-Regressão Automática)
 *
 * Grava assinaturas reais de erros + causa raiz + correção definitiva e bloqueia
 * qualquer agente ou script de repetir falhas já resolvidas.
 *
 * Uso:
 *   node friday-core/immune-memory.mjs list
 *   node friday-core/immune-memory.mjs check "<log de erro ou comando>"
 *   node friday-core/immune-memory.mjs record --sig "<erro>" --cause "<causa>" --fix "<correcao>"
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "../scripts/lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DB_PATH = path.join(__dirname, "memory", "immune-errors.json");

function escSql(s) {
  return String(s ?? "").replace(/'/g, "''");
}

async function runSupabaseSql(sql) {
  const token = getSupabaseToken();
  const res = await fetch(
    `https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      signal: AbortSignal.timeout(15000),
      body: JSON.stringify({ query: sql }),
    }
  );
  if (!res.ok) throw new Error(`Supabase HTTP ${res.status}`);
  return await res.json();
}

const SEED_ERRORS = [
  {
    id: "ERR-001",
    signature: "MANAGE_EXTERNAL_STORAGE",
    cause: "Permissão proibida pela política da Google Play para apps de limpeza comum.",
    fix: "Usar exclusivamente MediaStore Scoped Storage + MediaStore.createDeleteRequest (API 30+) e RecoverableSecurityException (API 29).",
  },
  {
    id: "ERR-002",
    signature: "npm.ps1 cannot be loaded",
    cause: "Windows PowerShell 5.1 ExecutionPolicy bloqueia scripts .ps1 do npm.",
    fix: "Sempre executar npm.cmd em vez de npm no PowerShell 5.1, e usar ';' em vez de '&&'.",
  },
  {
    id: "ERR-003",
    signature: "dp(52)",
    cause: "Altura fixa dp(52) em botão de 2 linhas corta o texto no Moto G84.",
    fix: "Usar minHeight/wrapContent com padding vertical adequado para textos multilíngues.",
  },
  {
    id: "ERR-004",
    signature: "package=.*AndroidManifest",
    cause: "AGP 8+ proíbe atributo package= no AndroidManifest.xml.",
    fix: "Definir namespace exclusivamente no app/build.gradle.kts.",
  },
  {
    id: "ERR-005",
    signature: "429.*generateContent.*tts",
    cause: "Cota diária do Google AI Studio TTS atingida.",
    fix: "Ativar cooldown de 30 min e migrar automaticamente para Tier 2 Microsoft Edge Neural (pt-BR-FranciscaNeural) ilimitado.",
  },
  {
    id: "ERR-006",
    signature: "deepseek-r1:free",
    cause: "Modelo removido no OpenRouter (HTTP 404).",
    fix: "Usar rotas :free validadas: openrouter/free, nvidia/nemotron-3.5-lightning:free, google/gemma-4-31b-it:free.",
  },
];

function loadDb() {
  if (!fs.existsSync(DB_PATH)) {
    fs.mkdirSync(path.dirname(DB_PATH), { recursive: true });
    const init = { updatedAt: new Date().toISOString(), items: SEED_ERRORS };
    fs.writeFileSync(DB_PATH, JSON.stringify(init, null, 2), "utf8");
    return init;
  }
  return JSON.parse(fs.readFileSync(DB_PATH, "utf8"));
}

function saveDb(db) {
  db.updatedAt = new Date().toISOString();
  fs.writeFileSync(DB_PATH, JSON.stringify(db, null, 2), "utf8");
}

async function syncWithSupabase(db) {
  let pushed = 0;
  for (const item of db.items) {
    const tag = `[VACINA_IMUNE ${item.id}] Assinatura: ${item.signature} | Causa: ${item.cause} | Correção: ${item.fix}`;
    const existing = await runSupabaseSql(
      `SELECT id FROM public.memories WHERE origem = 'friday_immune_memory' AND content LIKE '[VACINA_IMUNE ${escSql(item.id)}]%' LIMIT 1;`
    );
    if (!Array.isArray(existing) || existing.length === 0) {
      await runSupabaseSql(`
        INSERT INTO public.memories (content, origem, categoria, confianca, versao)
        VALUES ('${escSql(tag)}', 'friday_immune_memory', 'correction', 0.99, 1);
      `);
      pushed++;
    }
  }
  const remoteRows = await runSupabaseSql(
    `SELECT count(*)::int AS total FROM public.memories WHERE origem = 'friday_immune_memory';`
  );
  const totalRemote = remoteRows?.[0]?.total ?? 0;
  console.log(`☁️ Sincronização Supabase concluída: ${pushed} nova(s) vacina(s) enviada(s) | Total na nuvem: ${totalRemote}`);
}

async function main() {
  const [cmd, ...rest] = process.argv.slice(2);
  const db = loadDb();

  if (cmd === "sync") {
    await syncWithSupabase(db);
    return;
  }

  if (!cmd || cmd === "list") {
    console.log(`\n🛡️ MEMÓRIA DE ERROS IMUNES FRIDAY (${db.items.length} vacinas ativas):\n`);
    for (const e of db.items) {
      console.log(`  • [${e.id}] Assinatura: "${e.signature}"`);
      console.log(`    Correção: ${e.fix}`);
    }
    return;
  }

  if (cmd === "check") {
    const text = rest.join(" ");
    const hits = db.items.filter((e) => {
      try {
        return new RegExp(e.signature, "i").test(text);
      } catch {
        return text.toLowerCase().includes(e.signature.toLowerCase());
      }
    });
    if (hits.length === 0) {
      console.log("✅ Nenhum erro imune conhecido detectado.");
      return;
    }
    console.log(`🚨 BLOQUEIO ANTI-REGRESSÃO: ${hits.length} assinatura(s) conhecida(s) detectada(s)!`);
    for (const h of hits) {
      console.log(`  • [${h.id}] Causa: ${h.cause}`);
      console.log(`    ✅ Correção obrigatória: ${h.fix}`);
    }
    process.exit(2);
  }

  if (cmd === "record") {
    const getArg = (flag) => {
      const i = rest.indexOf(flag);
      return i !== -1 ? rest[i + 1] : "";
    };
    const signature = getArg("--sig");
    const cause = getArg("--cause");
    const fix = getArg("--fix");
    if (!signature || !fix) {
      console.error('Uso: record --sig "<erro>" --cause "<causa>" --fix "<correcao>"');
      process.exit(1);
    }
    const id = `ERR-${String(db.items.length + 1).padStart(3, "0")}`;
    db.items.push({ id, signature, cause: cause || "Registrado automaticamente", fix, recordedAt: new Date().toISOString() });
    saveDb(db);
    console.log(`🛡️ Vacina [${id}] gravada na Memória de Erros Imunes.`);
  }
}

main();
