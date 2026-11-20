#!/usr/bin/env node
/**
 * DEPLOY NATIVO DE SUPABASE EDGE FUNCTIONS VIA MANAGEMENT API (Zero Docker / Zero Bun / Zero SSE4.2)
 *
 * Envia arquivos .ts (incluindo ../_shared/*) em multipart/form-data direto para:
 *   POST https://api.supabase.com/v1/projects/{ref}/functions/deploy?slug={slug}
 *
 * Uso:
 *   node scripts/deploy-supabase-edge.mjs verify-play-purchase [--no-verify-jwt]
 *   node scripts/deploy-supabase-edge.mjs play-rtdn-webhook --no-verify-jwt
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

async function deployEdgeFunction(slug, { verifyJwt = false } = {}) {
  const fnDir = path.join(projectRoot, "supabase", "functions", slug);
  const sharedDir = path.join(projectRoot, "supabase", "functions", "_shared");
  const entryPath = path.join(fnDir, "index.ts");

  if (!fs.existsSync(entryPath)) {
    throw new Error(`Entrypoint não encontrado: ${entryPath}`);
  }

  const form = new FormData();
  const metadata = {
    entrypoint_path: `${slug}/index.ts`,
    name: slug,
    verify_jwt: verifyJwt,
  };
  form.append("metadata", JSON.stringify(metadata));

  // Add function files
  for (const file of fs.readdirSync(fnDir)) {
    const full = path.join(fnDir, file);
    if (!fs.statSync(full).isFile()) continue;
    const content = fs.readFileSync(full);
    const relPath = `${slug}/${file}`;
    form.append("file", new Blob([content], { type: "application/typescript" }), relPath);
  }

  // Add _shared files if present
  if (fs.existsSync(sharedDir)) {
    for (const file of fs.readdirSync(sharedDir)) {
      const full = path.join(sharedDir, file);
      if (!fs.statSync(full).isFile()) continue;
      const content = fs.readFileSync(full);
      const relPath = `_shared/${file}`;
      form.append("file", new Blob([content], { type: "application/typescript" }), relPath);
    }
  }

  const token = getSupabaseToken();
  const url = `https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/functions/deploy?slug=${encodeURIComponent(slug)}`;

  const res = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: form,
    signal: AbortSignal.timeout(45000),
  });

  const text = await res.text();
  if (!res.ok) {
    throw new Error(`Deploy ${slug} falhou (HTTP ${res.status}): ${text}`);
  }
  const parsed = JSON.parse(text);
  return {
    slug: parsed.slug || slug,
    version: parsed.version,
    status: parsed.status,
    verify_jwt: parsed.verify_jwt,
    updated_at: parsed.updated_at,
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const args = process.argv.slice(2);
  const slug = args.find((a) => !a.startsWith("--"));
  const verifyJwt = args.includes("--verify-jwt") && !args.includes("--no-verify-jwt");

  if (!slug) {
    console.error("Uso: node scripts/deploy-supabase-edge.mjs <slug> [--no-verify-jwt|--verify-jwt]");
    process.exit(1);
  }

  deployEdgeFunction(slug, { verifyJwt })
    .then((info) => {
      console.log(`✅ Edge Function '${info.slug}' publicada com sucesso:`, JSON.stringify(info));
    })
    .catch((err) => {
      console.error("❌", err.message);
      process.exit(1);
    });
}
