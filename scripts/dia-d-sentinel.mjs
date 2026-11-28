#!/usr/bin/env node
/**
 * DIA D SENTINEL — Vigia de Abertura Pública na Google Play Store (100% Determinístico)
 * Pacote: com.rdf112.midiastore
 *
 * Operação:
 * - Verifica a cada 10 minutos (ou 1 vez com --once) se a URL pública anônima da Google Play Store
 *   retorna HTTP 200 com o pacote com.rdf112.midiastore visível.
 * - Assim que virar HTTP 200 pela 1ª vez:
 *   1) Dispara AUTOMATICAMENTE 1 ÚNICA VEZ (trava atômica em public.dia_d_sentinel_state no Supabase):
 *      `node scripts/play-aso-15-idiomas.mjs --commit` (que exige changesInReviewBehavior=ERROR_IF_IN_REVIEW).
 *   2) Registra o marco oficial no Supabase (`public.memories` + `public.dia_d_sentinel_state`).
 *   3) Avisa de forma destacada no terminal para ativar o `node scripts/ig-viral-dm-closer.mjs --live` e encerra.
 *
 * Uso:
 *   node scripts/dia-d-sentinel.mjs --once --dry-run   (teste único seguro sem commitar)
 *   node scripts/dia-d-sentinel.mjs                    (loop sentinela a cada 10 min)
 */

import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";
import { registrarMemoria } from "./registrar-memoria.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const ASO_SCRIPT_PATH = path.join(projectRoot, "scripts", "play-aso-15-idiomas.mjs");
const PUBLIC_PLAY_URL = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";
const DEFAULT_INTERVAL_MINUTES = 10;

function sqlLiteral(val) {
  if (val === null || val === undefined) return "NULL";
  return `'${String(val).replace(/'/g, "''")}'`;
}

async function execSql(query, retries = 3) {
  for (let i = 0; i < retries; i++) {
    try {
      return await runSupabaseSql(query);
    } catch (err) {
      const isTransient = /HTTP (?:502|503|504|429)|fetch failed|timeout/i.test(err.message || "");
      if (!isTransient || i === retries - 1) throw err;
      await new Promise((r) => setTimeout(r, 1500 * (i + 1)));
    }
  }
}

async function ensureSentinelStateTable() {
  await execSql(`
    CREATE TABLE IF NOT EXISTS public.dia_d_sentinel_state (
      id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
      aso_15_committed BOOLEAN NOT NULL DEFAULT false,
      last_http_status INTEGER,
      first_http200_at TIMESTAMPTZ,
      aso_committed_at TIMESTAMPTZ,
      last_checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      last_note TEXT
    );
    ALTER TABLE public.dia_d_sentinel_state ENABLE ROW LEVEL SECURITY;
    INSERT INTO public.dia_d_sentinel_state (id, aso_15_committed)
    VALUES (1, false)
    ON CONFLICT (id) DO NOTHING;
  `);
}

async function checkPublicPlayStoreHttp200() {
  try {
    const res = await fetch(PUBLIC_PLAY_URL, {
      method: "GET",
      redirect: "follow",
      credentials: "omit",
      headers: {
        "User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36",
        "Accept-Language": "pt-BR,pt;q=0.9,en-US;q=0.8",
      },
      signal: AbortSignal.timeout(12000),
    });
    const html = (await res.text()).slice(0, 45000);
    const hasMarker = html.includes("com.rdf112.midiastore") || html.includes("Midiastore");
    return {
      httpStatus: res.status,
      isLiveHttp200: res.status === 200 && hasMarker,
      url: PUBLIC_PLAY_URL,
    };
  } catch (err) {
    return {
      httpStatus: 0,
      isLiveHttp200: false,
      url: PUBLIC_PLAY_URL,
      error: err.message,
    };
  }
}

async function runSingleCheck({ isDryRun }) {
  await ensureSentinelStateTable();

  const [probe, stateRows] = await Promise.all([
    checkPublicPlayStoreHttp200(),
    execSql(`SELECT aso_15_committed, first_http200_at, aso_committed_at FROM public.dia_d_sentinel_state WHERE id = 1;`),
  ]);

  const state = stateRows?.[0] || { aso_15_committed: false };
  const nowIso = new Date().toISOString();

  await execSql(`
    UPDATE public.dia_d_sentinel_state
    SET last_http_status = ${Number(probe.httpStatus) || 0},
        first_http200_at = CASE
          WHEN ${probe.isLiveHttp200 ? "true" : "false"} AND first_http200_at IS NULL THEN now()
          ELSE first_http200_at
        END,
        last_checked_at = now()
    WHERE id = 1;
  `);

  if (!probe.isLiveHttp200) {
    const statusReport = {
      checkedAt: nowIso,
      mode: isDryRun ? "DRY_RUN" : "SENTINEL_ACTIVE",
      playStoreStatus: probe.httpStatus,
      isPublicLive: false,
      aso15AlreadyCommitted: Boolean(state.aso_15_committed),
      actionTaken: "WAITING_FOR_HTTP_200",
      nextStepWhenHttp200: "Executar 1x `node scripts/play-aso-15-idiomas.mjs --commit` (ERROR_IF_IN_REVIEW) + registrar no Supabase + avisar para ligar `ig-viral-dm-closer.mjs --live`.",
    };
    console.log(JSON.stringify(statusReport, null, 2));
    return { done: false, report: statusReport };
  }

  if (state.aso_15_committed) {
    const alreadyDoneReport = {
      checkedAt: nowIso,
      mode: isDryRun ? "DRY_RUN" : "SENTINEL_ACTIVE",
      playStoreStatus: probe.httpStatus,
      isPublicLive: true,
      aso15AlreadyCommitted: true,
      asoCommittedAt: state.aso_committed_at || null,
      actionTaken: "SKIP_ALREADY_COMMITTED_ONCE",
      nextOperatorAction: "Ligue agora: node scripts/ig-viral-dm-closer.mjs --live",
    };
    console.log(JSON.stringify(alreadyDoneReport, null, 2));
    return { done: true, report: alreadyDoneReport };
  }

  if (isDryRun) {
    const dryReport = {
      checkedAt: nowIso,
      mode: "DRY_RUN",
      playStoreStatus: probe.httpStatus,
      isPublicLive: true,
      aso15AlreadyCommitted: false,
      actionTaken: "DRY_RUN_WOULD_TRIGGER_ASO_COMMIT_ONCE",
      commandWouldRun: "node scripts/play-aso-15-idiomas.mjs --commit",
    };
    console.log(JSON.stringify(dryReport, null, 2));
    return { done: true, report: dryReport };
  }

  // Lock atômico no Supabase ANTES de disparar para garantir disparo único absoluto
  const lockRows = await execSql(`
    UPDATE public.dia_d_sentinel_state
    SET aso_15_committed = true,
        aso_committed_at = now(),
        last_note = 'Disparo unico automatico ao detectar HTTP 200 na Play Store'
    WHERE id = 1 AND aso_15_committed = false
    RETURNING id;
  `);

  if (!lockRows || lockRows.length === 0) {
    return { done: true, report: { checkedAt: nowIso, actionTaken: "LOCK_ALREADY_CLAIMED" } };
  }

  const runAso = spawnSync(process.execPath, [ASO_SCRIPT_PATH, "--commit"], {
    cwd: projectRoot,
    encoding: "utf8",
    timeout: 90_000,
  });

  if (runAso.status !== 0) {
    const errMsg = (runAso.stderr || runAso.stdout || "erro desconhecido").slice(0, 400);
    // Reverte lock caso ERROR_IF_IN_REVIEW bloqueie temporariamente, permitindo nova tentativa segura
    await execSql(`
      UPDATE public.dia_d_sentinel_state
      SET aso_15_committed = false,
          aso_committed_at = NULL,
          last_note = ${sqlLiteral(`Tentativa bloqueada (${errMsg})`)}
      WHERE id = 1;
    `);
    throw new Error(`ASO --commit retornou codigo ${runAso.status}: ${errMsg}`);
  }

  await registrarMemoria({
    category: "episodic",
    action: "MARCO DIA D: Play Store HTTP 200 Detectado + ASO 15 Idiomas Aplicado Automaticamente (1x)",
    studied: "Monitoramento autonomo scripts/dia-d-sentinel.mjs na URL publica da Google Play",
    attempted: "Disparo unico de node scripts/play-aso-15-idiomas.mjs --commit com ERROR_IF_IN_REVIEW",
    errors: "Nenhum",
    fixes: "Nenhuma",
    conclusions: "Midiastore publico (HTTP 200) na Google Play e 15 fichas ASO v2.9.0 publicadas. Pronto para ativar ig-viral-dm-closer.mjs --live.",
  });

  console.log("\n================================================================================");
  console.log("🚀 DIA D ATIVO: PLAY STORE RETORNOU HTTP 200 E ASO 15 IDIOMAS FOI PUBLICADO (1x)!");
  console.log("👉 PRÓXIMO PASSO IMEDIATO: ligue o closer com:");
  console.log("   node scripts/ig-viral-dm-closer.mjs --live");
  console.log("================================================================================\n");

  return { done: true, report: { checkedAt: nowIso, actionTaken: "ASO_15_COMMITTED_AND_LOGGED" } };
}

async function main() {
  const args = process.argv.slice(2);
  const isOnce = args.includes("--once");
  const isDryRun = args.includes("--dry-run");
  const minArg = args.find((a) => a.startsWith("--interval-min="));
  const intervalMinutes = Math.max(1, Number(minArg ? minArg.split("=")[1] : DEFAULT_INTERVAL_MINUTES) || DEFAULT_INTERVAL_MINUTES);

  const first = await runSingleCheck({ isDryRun });
  if (isOnce || first.done) return;

  console.log(`[DIA-D-SENTINEL] Próxima checagem automática em ${intervalMinutes} minutos...`);
  const timer = setInterval(async () => {
    try {
      const res = await runSingleCheck({ isDryRun });
      if (res.done) clearInterval(timer);
    } catch (err) {
      console.error("[DIA-D-SENTINEL WARN]:", err.message);
    }
  }, intervalMinutes * 60_000);
}

main().catch((err) => {
  console.error("DIA_D_SENTINEL_ERR:", err.message);
  process.exit(1);
});
