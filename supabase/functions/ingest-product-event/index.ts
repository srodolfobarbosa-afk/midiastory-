import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2.49.1";

const ALLOWED_EVENTS = new Set([
  "install_first_open",
  "first_scan_completed",
  "paywall_viewed",
  "purchase_started",
  "purchase_completed",
  "cleanup_completed",
  "retention_ping",
  "page_view",
  "play_store_click",
]);

const HASH_REGEX = /^[a-f0-9]{16,64}$/i;
const MAX_BATCH_SIZE = 20;
const MAX_PROPS_BYTES = 1024;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type, x-client-info, apikey",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface RawEventInput {
  installHash?: unknown;
  eventName?: unknown;
  appVersion?: unknown;
  locale?: unknown;
  properties?: unknown;
}

function sanitizeEvent(raw: RawEventInput) {
  const installHash = String(raw?.installHash || "").trim().toLowerCase();
  const eventName = String(raw?.eventName || "").trim();
  const appVersion = String(raw?.appVersion || "").trim().slice(0, 32) || null;
  const locale = String(raw?.locale || "").trim().slice(0, 16) || null;

  if (!HASH_REGEX.test(installHash) || !ALLOWED_EVENTS.has(eventName)) {
    return null;
  }

  let properties: Record<string, unknown> = {};
  if (raw?.properties && typeof raw.properties === "object" && !Array.isArray(raw.properties)) {
    const serialized = JSON.stringify(raw.properties);
    if (serialized.length <= MAX_PROPS_BYTES) {
      properties = raw.properties as Record<string, unknown>;
    }
  }

  return {
    install_hash: installHash,
    event_name: eventName,
    app_version: appVersion,
    locale,
    properties,
  };
}

Deno.serve(async (req: Request) => {
  const startedAt = performance.now();
  const requestId = crypto.randomUUID();

  if (req.method === "OPTIONS") {
    return new Response("ok", { status: 200, headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ ok: false, reason: "METHOD_NOT_ALLOWED", requestId }),
      { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
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
    const rawBodyText = await req.text();
    const body = rawBodyText ? JSON.parse(rawBodyText) : {};
    const rawList: RawEventInput[] = Array.isArray(body?.events)
      ? body.events.slice(0, MAX_BATCH_SIZE)
      : [body];

    const validRows = rawList.map(sanitizeEvent).filter((row): row is NonNullable<typeof row> => row !== null);

    if (validRows.length === 0) {
      return new Response(
        JSON.stringify({
          ok: false,
          reason: "INVALID_EVENTS_PAYLOAD",
          accepted: 0,
          requestId,
          latencyMs: Math.round(performance.now() - startedAt),
        }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey);
    const { error } = await supabase.from("product_events").insert(validRows);
    if (error) {
      throw new Error(error.message);
    }

    return new Response(
      JSON.stringify({
        ok: true,
        accepted: validRows.length,
        dropped: rawList.length - validRows.length,
        requestId,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({
        ok: false,
        reason: err instanceof Error ? err.message : "INGEST_ERROR",
        requestId,
        latencyMs: Math.round(performance.now() - startedAt),
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
