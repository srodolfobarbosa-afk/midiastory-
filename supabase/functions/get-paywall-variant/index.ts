import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.49.1";

const CACHE_TTL_MS = 60_000;

interface VariantRow {
  experiment_key: string;
  variant_key: string;
  locale: string;
  headline: string;
  subheadline: string;
  highlighted_sku: string;
  cta_label: string;
  status: string;
  alpha: number;
  beta: number;
}

const variantCache = new Map<string, { rows: VariantRow[]; expiresAt: number }>();

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type, x-client-info, apikey",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

function normalSample(): number {
  const u1 = Math.max(1e-12, Math.random());
  const u2 = Math.random();
  return Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
}

function gammaSample(shape: number): number {
  if (shape < 1) {
    return gammaSample(shape + 1) * Math.pow(Math.random(), 1 / shape);
  }
  const d = shape - 1 / 3;
  const c = 1 / Math.sqrt(9 * d);
  while (true) {
    const x = normalSample();
    const v = Math.pow(1 + c * x, 3);
    if (v <= 0) continue;
    const u = Math.random();
    if (u < 1 - 0.0331 * x * x * x * x) return d * v;
    if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v))) return d * v;
  }
}

function betaSample(alpha: number, beta: number): number {
  const x = gammaSample(Math.max(0.5, alpha));
  const y = gammaSample(Math.max(0.5, beta));
  return x / (x + y);
}

function pickVariantThompson(rows: VariantRow[]): VariantRow {
  const winner = rows.find((r) => r.status === "WINNER");
  if (winner) return winner;

  let best = rows[0];
  let bestScore = -1;
  for (const r of rows) {
    const score = betaSample(Number(r.alpha) || 1, Number(r.beta) || 1);
    if (score > bestScore) {
      bestScore = score;
      best = r;
    }
  }
  return best;
}

Deno.serve(async (req: Request) => {
  const startedAt = performance.now();
  const requestId = crypto.randomUUID();

  if (req.method === "OPTIONS") {
    return new Response("ok", { status: 200, headers: corsHeaders });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !supabaseServiceKey) {
    return new Response(
      JSON.stringify({ ok: false, reason: "MISSING_SUPABASE_ENV", requestId }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }

  try {
    const url = new URL(req.url);
    let experimentKey = url.searchParams.get("experiment") || "paywall_v1";
    let locale = url.searchParams.get("locale") || "pt-BR";

    if (req.method === "POST") {
      try {
        const body = await req.json();
        if (body?.experiment) experimentKey = String(body.experiment);
        if (body?.locale) locale = String(body.locale);
      } catch {}
    }

    const normalizedLocale = locale.toLowerCase().startsWith("pt") ? "pt-BR" : "en-US";
    const cacheKey = `${experimentKey}:${normalizedLocale}`;
    let cacheHit = false;
    let rows: VariantRow[] = [];

    const cached = variantCache.get(cacheKey);
    if (cached && cached.expiresAt > Date.now()) {
      rows = cached.rows;
      cacheHit = true;
    } else {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);
      const { data, error } = await supabase
        .from("sales_experiments")
        .select("experiment_key,variant_key,locale,headline,subheadline,highlighted_sku,cta_label,status,alpha,beta")
        .eq("experiment_key", experimentKey)
        .eq("locale", normalizedLocale)
        .in("status", ["ACTIVE", "WINNER"]);
      if (error) throw new Error(error.message);
      rows = (data || []) as VariantRow[];
      if (rows.length > 0) {
        variantCache.set(cacheKey, { rows, expiresAt: Date.now() + CACHE_TTL_MS });
      }
    }

    if (rows.length === 0) {
      return new Response(
        JSON.stringify({ ok: false, reason: "NO_ACTIVE_VARIANTS", requestId }),
        { status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const selected = pickVariantThompson(rows);

    return new Response(
      JSON.stringify({
        ok: true,
        experiment: selected.experiment_key,
        variant: selected.variant_key,
        locale: selected.locale,
        headline: selected.headline,
        subheadline: selected.subheadline,
        highlightedSku: selected.highlighted_sku,
        ctaLabel: selected.cta_label,
        selectionPolicy: selected.status === "WINNER" ? "WINNER_LOCKED" : "THOMPSON_SAMPLING",
        cacheHit,
        requestId,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({
        ok: false,
        reason: err instanceof Error ? err.message : "PAYWALL_CONFIG_ERROR",
        requestId,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
