#!/usr/bin/env node
/**
 * IG VIRAL DM CLOSER — Versão Blindada Determinística (Zero LLM / Zero Spam)
 * Canal: @aconteceunobr -> Private Reply "LIMPAR" -> https://midiastory-df6a0.web.app/go/play
 *
 * 4 Travas Obrigatórias Invioláveis:
 *  (a) Checa HTTP 200 anônimo na URL pública da Play Store antes de qualquer disparo real.
 *  (b) Teto conservador de 60 DMs/hora + cooldown de 24h por usuário persistidos no Supabase (com RLS + índices).
 *  (c) Envia SOMENTE Private Reply (POST /{page_id}/messages recipient:{comment_id}) para a palavra-chave "LIMPAR",
 *      SEM spam de resposta pública em todo comentário.
 *  (d) Disjuntor imediato que pausa na hora em erros Meta 368 / 613 / 2534001 / 500 + modo --dry-run padrão
 *      + auditoria automática de permissão Standard vs Advanced Access (App Mode / Token Scopes).
 *
 * Uso:
 *   node scripts/ig-viral-dm-closer.mjs --dry-run
 *   node scripts/ig-viral-dm-closer.mjs --live      (só dispara se Play Store retornar HTTP 200 anônimo e disjuntor fechado)
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { runSupabaseSql } from "./lib/cognitive-memory.mjs";
import { buildGoPlayUrl, isLimparKeyword } from "./lib/gemini-closer.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const ENV_PATH = path.join(projectRoot, ".env");
const GRAPH_BASE = "https://graph.facebook.com/v27.0";
const PUBLIC_PLAY_URL = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";

const MAX_DMS_PER_HOUR = 60;
const USER_COOLDOWN_HOURS = 24;
const PRIVATE_REPLY_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000; // Janela oficial Meta Private Replies (7 dias)
const CIRCUIT_BREAKER_CODES = new Set([368, 613, 2534001, 2534041, 500]);

function loadDotEnv() {
  if (!fs.existsSync(ENV_PATH)) return;
  for (const line of fs.readFileSync(ENV_PATH, "utf8").split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    const eq = t.indexOf("=");
    if (eq <= 0) continue;
    const k = t.slice(0, eq).trim();
    const v = t.slice(eq + 1).trim().replace(/^["']|["']$/g, "");
    if (!process.env[k]) process.env[k] = v;
  }
}

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

/**
 * Garante tabelas de controle idempotente e disjuntor no Supabase com RLS ativo e índices compostos.
 */
async function ensureSupabaseSafetyTables() {
  await execSql(`
    CREATE TABLE IF NOT EXISTS public.ig_dm_closer_dispatches (
      ig_comment_id TEXT PRIMARY KEY,
      ig_user_id TEXT NOT NULL,
      ig_username TEXT,
      media_id TEXT NOT NULL,
      comment_text TEXT,
      status TEXT NOT NULL DEFAULT 'SENT',
      error_code INTEGER,
      sent_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
    ALTER TABLE public.ig_dm_closer_dispatches ENABLE ROW LEVEL SECURITY;
    CREATE INDEX IF NOT EXISTS idx_ig_dm_closer_user_sent
      ON public.ig_dm_closer_dispatches (ig_user_id, sent_at DESC);
    CREATE INDEX IF NOT EXISTS idx_ig_dm_closer_sent_at
      ON public.ig_dm_closer_dispatches (sent_at DESC);

    CREATE TABLE IF NOT EXISTS public.ig_dm_closer_circuit (
      id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
      is_paused BOOLEAN NOT NULL DEFAULT false,
      paused_reason TEXT,
      last_error_code INTEGER,
      paused_at TIMESTAMPTZ,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
    ALTER TABLE public.ig_dm_closer_circuit ENABLE ROW LEVEL SECURITY;
    INSERT INTO public.ig_dm_closer_circuit (id, is_paused)
    VALUES (1, false)
    ON CONFLICT (id) DO NOTHING;
  `);
}

/**
 * Trava (a): Verifica HTTP 200 anônimo na ficha pública da Google Play Store.
 */
async function checkPublicPlayStoreLive() {
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
    const htmlSnippet = (await res.text()).slice(0, 40000);
    const hasPackageMarker = htmlSnippet.includes("com.rdf112.midiastore") || htmlSnippet.includes("Midiastore");
    return {
      httpStatus: res.status,
      isPublicLive: res.status === 200 && hasPackageMarker,
      url: PUBLIC_PLAY_URL,
    };
  } catch (err) {
    return {
      httpStatus: 0,
      isPublicLive: false,
      url: PUBLIC_PLAY_URL,
      error: err.message,
    };
  }
}

/**
 * Auditoria de Permissão Meta: Token Scopes + App Mode (Development/Standard vs Live/Advanced Access).
 */
async function checkMetaPermissionsAndAccessTier() {
  const userToken = process.env.META_ACCESS_TOKEN?.trim();
  const pageToken = process.env.FACEBOOK_PAGE_ACCESS_TOKEN?.trim() || userToken;
  const appId = process.env.META_APP_ID?.trim();
  const appSecret = process.env.META_APP_SECRET?.trim();
  const pageId = process.env.FACEBOOK_PAGE_ID?.trim();
  const igId = process.env.INSTAGRAM_USER_ID?.trim();

  if (!userToken || !igId || !pageId) {
    throw new Error("META_ACCESS_TOKEN, FACEBOOK_PAGE_ID ou INSTAGRAM_USER_ID ausentes no .env.");
  }

  const permsUrl = new URL(`${GRAPH_BASE}/me/permissions`);
  permsUrl.searchParams.set("access_token", userToken);
  const permsRes = await fetch(permsUrl, { signal: AbortSignal.timeout(12000) });
  const permsJson = await permsRes.json();
  const granted = new Set(
    (permsJson.data || []).filter((p) => p.status === "granted").map((p) => p.permission)
  );

  const requiredScopes = ["instagram_basic", "instagram_manage_comments", "instagram_manage_messages", "pages_messaging"];
  const missingScopes = requiredScopes.filter((s) => !granted.has(s));

  let debugInfo = null;
  let appInfo = null;
  if (appId && appSecret) {
    const appAccessToken = `${appId}|${appSecret}`;
    const dbgUrl = new URL(`${GRAPH_BASE}/debug_token`);
    dbgUrl.searchParams.set("input_token", pageToken);
    dbgUrl.searchParams.set("access_token", appAccessToken);
    const [dbgRes, appRes] = await Promise.all([
      fetch(dbgUrl, { signal: AbortSignal.timeout(12000) }),
      fetch(`${GRAPH_BASE}/${appId}?fields=id,name,category&access_token=${encodeURIComponent(appAccessToken)}`, {
        signal: AbortSignal.timeout(12000),
      }),
    ]);
    const dbgData = (await dbgRes.json())?.data || {};
    appInfo = await appRes.json();
    debugInfo = {
      tokenType: dbgData.type || null,
      isValid: Boolean(dbgData.is_valid),
      expiresAt: dbgData.expires_at === 0 ? "NEVER" : dbgData.expires_at || null,
      granularScopes: (dbgData.granular_scopes || []).map((g) => ({
        scope: g.scope,
        targetIdsCount: Array.isArray(g.target_ids) ? g.target_ids.length : "ALL",
      })),
    };
  }

  // Teste não-destrutivo na endpoint de conversas da Página/IG para medir capacidade de mensageria
  let messagingEndpointProbe = null;
  try {
    const convUrl = new URL(`${GRAPH_BASE}/${pageId}/conversations`);
    convUrl.searchParams.set("platform", "instagram");
    convUrl.searchParams.set("limit", "1");
    convUrl.searchParams.set("access_token", pageToken);
    const convRes = await fetch(convUrl, { signal: AbortSignal.timeout(12000) });
    const convJson = await convRes.json();
    messagingEndpointProbe = {
      httpStatus: convRes.status,
      ok: convRes.ok && !convJson.error,
      errorCode: convJson.error?.code || null,
      errorSubcode: convJson.error?.error_subcode || null,
      note: convRes.ok
        ? "Endpoint /conversations?platform=instagram acessível. Atenção: se o App Meta estiver em Standard Access (sem App Review concluído para instagram_manage_messages), Private Replies para seguidores externos retornarão erro 2534001/#10 e acionarão o disjuntor imediatamente."
        : convJson.error?.message?.slice(0, 160) || "Erro ao consultar /conversations",
    };
  } catch (err) {
    messagingEndpointProbe = { ok: false, error: err.message };
  }

  return {
    appName: appInfo?.name || null,
    appId: appId || null,
    pageId,
    igId,
    requiredScopesGranted: missingScopes.length === 0,
    missingScopes,
    debugToken: debugInfo,
    messagingEndpointProbe,
  };
}

async function getQuotaAndCircuitStatus() {
  const [circuitRows, hourRows] = await Promise.all([
    execSql(`SELECT is_paused, paused_reason, last_error_code, paused_at FROM public.ig_dm_closer_circuit WHERE id = 1;`),
    execSql(`
      SELECT COUNT(*)::int AS sent_last_hour
      FROM public.ig_dm_closer_dispatches
      WHERE status = 'SENT' AND sent_at >= now() - INTERVAL '1 hour';
    `),
  ]);
  return {
    circuit: circuitRows?.[0] || { is_paused: false },
    sentLastHour: Number(hourRows?.[0]?.sent_last_hour || 0),
    hourlyCap: MAX_DMS_PER_HOUR,
  };
}

async function tripCircuitBreaker(errorCode, reason) {
  await execSql(`
    UPDATE public.ig_dm_closer_circuit
    SET is_paused = true,
        last_error_code = ${Number(errorCode) || 0},
        paused_reason = ${sqlLiteral(reason)},
        paused_at = now(),
        updated_at = now()
    WHERE id = 1;
  `);
}

function buildDeterministicPrivateReply(mediaId) {
  const link = buildGoPlayUrl("ig_dm", `reel_${mediaId}`);
  return (
    `Aqui está o link oficial do Midiastore na Google Play:\n${link}\n\n` +
    `Ele pesa menos de 7 MB, encontra vídeos pesados e duplicatas exatas (SHA-256) sem apagar suas conversas e só remove o que você confirmar.`
  );
}

async function fetchLimparCandidates(igId, userToken) {
  const mediaUrl = new URL(`${GRAPH_BASE}/${igId}/media`);
  mediaUrl.searchParams.set("fields", "id,media_product_type,media_type,timestamp,comments_count,permalink");
  mediaUrl.searchParams.set("limit", "15");
  mediaUrl.searchParams.set("access_token", userToken);

  const mediaRes = await fetch(mediaUrl, { signal: AbortSignal.timeout(15000) });
  const mediaJson = await mediaRes.json();
  if (!mediaRes.ok || mediaJson.error) {
    throw new Error(`Falha ao listar mídias IG: ${JSON.stringify(mediaJson.error || mediaJson)}`);
  }

  const posts = (mediaJson.data || []).filter((m) => (m.comments_count || 0) > 0);
  const candidates = [];
  const now = Date.now();

  for (const post of posts) {
    const cUrl = new URL(`${GRAPH_BASE}/${post.id}/comments`);
    cUrl.searchParams.set("fields", "id,text,timestamp,username,from");
    cUrl.searchParams.set("limit", "50");
    cUrl.searchParams.set("access_token", userToken);

    const cRes = await fetch(cUrl, { signal: AbortSignal.timeout(12000) });
    const cJson = await cRes.json();
    if (!cRes.ok || cJson.error) continue;

    for (const c of cJson.data || []) {
      const username = String(c.username || c.from?.username || "").toLowerCase();
      if (!username || username === "aconteceunobr") continue;
      if (!isLimparKeyword(c.text || "")) continue;

      const commentTs = c.timestamp ? new Date(c.timestamp).getTime() : 0;
      if (!commentTs || now - commentTs > PRIVATE_REPLY_MAX_AGE_MS) continue;

      const igUserId = String(c.from?.id || username);
      candidates.push({
        commentId: String(c.id),
        igUserId,
        username,
        mediaId: String(post.id),
        permalink: post.permalink || null,
        commentText: String(c.text || "").slice(0, 140),
        timestamp: c.timestamp,
      });
    }
  }

  return { postsScanned: posts.length, candidates };
}

async function main() {
  loadDotEnv();
  const args = new Set(process.argv.slice(2));
  const isLiveMode = args.has("--live") && !args.has("--dry-run");
  const mode = isLiveMode ? "LIVE" : "DRY_RUN";

  if (args.has("--reset-circuit")) {
    await ensureSupabaseSafetyTables();
    await execSql(`
      UPDATE public.ig_dm_closer_circuit
      SET is_paused = false, paused_reason = NULL, last_error_code = NULL, paused_at = NULL, updated_at = now()
      WHERE id = 1;
    `);
  }

  await ensureSupabaseSafetyTables();

  const [playCheck, accessAudit, quotaState] = await Promise.all([
    checkPublicPlayStoreLive(),
    checkMetaPermissionsAndAccessTier(),
    getQuotaAndCircuitStatus(),
  ]);

  const userToken = process.env.META_ACCESS_TOKEN.trim();
  const pageToken = (process.env.FACEBOOK_PAGE_ACCESS_TOKEN || userToken).trim();
  const pageId = process.env.FACEBOOK_PAGE_ID.trim();
  const igId = process.env.INSTAGRAM_USER_ID.trim();

  const scan = await fetchLimparCandidates(igId, userToken);

  const gates = {
    playStorePublicHttp200: playCheck.isPublicLive,
    circuitClosed: !quotaState.circuit.is_paused,
    withinHourlyCap: quotaState.sentLastHour < MAX_DMS_PER_HOUR,
    requiredMetaScopesGranted: accessAudit.requiredScopesGranted,
  };
  const allGatesGreen = Object.values(gates).every(Boolean);

  const actions = [];
  let sentCountThisRun = 0;
  let remainingHourlyBudget = Math.max(0, MAX_DMS_PER_HOUR - quotaState.sentLastHour);

  for (const cand of scan.candidates) {
    const existing = await execSql(`
      SELECT
        EXISTS(SELECT 1 FROM public.ig_dm_closer_dispatches WHERE ig_comment_id = ${sqlLiteral(cand.commentId)}) AS comment_done,
        EXISTS(
          SELECT 1 FROM public.ig_dm_closer_dispatches
          WHERE ig_user_id = ${sqlLiteral(cand.igUserId)}
            AND status = 'SENT'
            AND sent_at >= now() - INTERVAL '${USER_COOLDOWN_HOURS} hours'
        ) AS user_in_cooldown;
    `);
    const row = existing?.[0] || {};
    if (row.comment_done) {
      actions.push({ ...cand, decision: "SKIP_ALREADY_REPLIED_COMMENT" });
      continue;
    }
    if (row.user_in_cooldown) {
      actions.push({ ...cand, decision: "SKIP_USER_COOLDOWN_24H" });
      continue;
    }
    if (remainingHourlyBudget <= 0) {
      actions.push({ ...cand, decision: "STOP_HOURLY_CAP_60_REACHED" });
      break;
    }

    const dmMessage = buildDeterministicPrivateReply(cand.mediaId);

    if (!isLiveMode || !allGatesGreen) {
      actions.push({
        ...cand,
        decision: !isLiveMode ? "DRY_RUN_WOULD_PRIVATE_REPLY" : "BLOCKED_BY_SAFETY_GATE",
        publicCommentReply: "NONE (anti-spam estrito)",
        messagePreview: dmMessage,
      });
      continue;
    }

    // Disparo real SOMENTE via Private Reply (zero comentário público)
    const sendRes = await fetch(`${GRAPH_BASE}/${pageId}/messages`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${pageToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        recipient: { comment_id: cand.commentId },
        message: { text: dmMessage },
      }),
      signal: AbortSignal.timeout(15000),
    });

    const sendJson = await sendRes.json();
    const errCode = Number(sendJson?.error?.code || 0);
    const errSubcode = Number(sendJson?.error?.error_subcode || 0);

    if (!sendRes.ok || sendJson.error) {
      const triggerCode = CIRCUIT_BREAKER_CODES.has(errCode)
        ? errCode
        : CIRCUIT_BREAKER_CODES.has(errSubcode)
          ? errSubcode
          : sendRes.status >= 500
            ? 500
            : errCode || sendRes.status;

      await runSupabaseSql(`
        INSERT INTO public.ig_dm_closer_dispatches (ig_comment_id, ig_user_id, ig_username, media_id, comment_text, status, error_code)
        VALUES (
          ${sqlLiteral(cand.commentId)},
          ${sqlLiteral(cand.igUserId)},
          ${sqlLiteral(cand.username)},
          ${sqlLiteral(cand.mediaId)},
          ${sqlLiteral(cand.commentText)},
          'FAILED',
          ${Number(triggerCode) || 0}
        )
        ON CONFLICT (ig_comment_id) DO NOTHING;
      `);

      if (CIRCUIT_BREAKER_CODES.has(triggerCode) || sendRes.status === 429) {
        const reason = `Disjuntor acionado por HTTP ${sendRes.status} / Meta code=${errCode} subcode=${errSubcode}: ${sendJson?.error?.message || "erro"}`;
        await tripCircuitBreaker(triggerCode || 500, reason);
        actions.push({ ...cand, decision: "CIRCUIT_TRIPPED_ABORT", errorCode: triggerCode, reason });
        break;
      }

      actions.push({
        ...cand,
        decision: "FAILED_NON_CRITICAL",
        errorCode: triggerCode,
        errorMessage: sendJson?.error?.message?.slice(0, 140) || null,
      });
      continue;
    }

    await runSupabaseSql(`
      INSERT INTO public.ig_dm_closer_dispatches (ig_comment_id, ig_user_id, ig_username, media_id, comment_text, status)
      VALUES (
        ${sqlLiteral(cand.commentId)},
        ${sqlLiteral(cand.igUserId)},
        ${sqlLiteral(cand.username)},
        ${sqlLiteral(cand.mediaId)},
        ${sqlLiteral(cand.commentText)},
        'SENT'
      )
      ON CONFLICT (ig_comment_id) DO NOTHING;
    `);

    remainingHourlyBudget--;
    sentCountThisRun++;
    actions.push({ ...cand, decision: "PRIVATE_REPLY_SENT", messageId: sendJson.message_id || null });
  }

  console.log(
    JSON.stringify(
      {
        checkedAt: new Date().toISOString(),
        mode,
        llmUsed: false,
        publicPlayStoreCheck: playCheck,
        safetyGates: gates,
        allGatesGreen,
        quotaAndCircuit: quotaState,
        metaAccessAudit: accessAudit,
        scanSummary: {
          postsWithCommentsScanned: scan.postsScanned,
          limparCommentsMatched: scan.candidates.length,
          sentCountThisRun,
        },
        sampleActions: actions.slice(0, 10),
      },
      null,
      2
    )
  );
}

main().catch((err) => {
  console.error("IG_VIRAL_DM_CLOSER_ERR:", err.message);
  process.exit(1);
});
