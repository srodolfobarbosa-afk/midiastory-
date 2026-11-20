#!/usr/bin/env node
/**
 * Codex CLI SessionStart hook: load compact project continuity from Supabase
 * and local handoff documents. It never prints memory contents to the console.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { retrieveRelevantContext } from "../lib/cognitive-memory.mjs";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

function readInput() {
  return new Promise((resolve) => {
    let input = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => { input += chunk; });
    process.stdin.on("end", () => resolve(input));
    process.stdin.on("error", () => resolve(input));
  });
}

function redact(text) {
  return String(text || "")
    .replace(/AQ\.[A-Za-z0-9_.-]+/g, "[REDACTED_KEY]")
    .replace(/(?:AIza|ghp_|github_pat_|sk-or-v1-|sbp_|sb_secret_)[A-Za-z0-9_\-]{12,}/g, "[REDACTED_SECRET]")
    .replace(/(SUPABASE_ACCESS_TOKEN\s*[=:]\s*)\S+/gi, "$1[REDACTED]")
    .replace(/(Authorization\s*:\s*Bearer\s+)\S+/gi, "$1[REDACTED]");
}

function readTail(relativePath, maxChars) {
  try {
    const content = fs.readFileSync(path.join(projectRoot, relativePath), "utf8");
    return redact(content.slice(-maxChars)).trim();
  } catch {
    return "";
  }
}

function readLatestContinuationUpdate(maxChars) {
  try {
    const docsDir = path.join(projectRoot, "docs");
    const candidates = fs.readdirSync(docsDir)
      .filter((name) => /^CONTINUATION_UPDATE_\d{4}-\d{2}-\d{2}\.md$/.test(name))
      .sort();
    const latest = candidates[candidates.length - 1];
    if (latest) return { name: latest, content: readTail(path.join("docs", latest), maxChars) };
  } catch {}
  return { name: "CONTINUATION_HANDOFF.md", content: readTail("docs/CONTINUATION_HANDOFF.md", maxChars) };
}

async function main() {
  let sessionId = null;
  try {
    const input = await readInput();
    sessionId = JSON.parse(input || "{}").session_id || null;
  } catch {}

  const parts = ["[MIDIASTORE — contexto automático de continuidade]"];
  try {
    const context = await retrieveRelevantContext({
      query: "",
      sessionId,
      limit: 5,
      includeCore: true,
      includeDocs: false,
      includeConversations: false,
      includeTasksAndLearnings: false,
    });
    if (context.formattedContext) {
      parts.push(`Memória Supabase (${context.source}; modelo de embedding ${context.embeddingModel || "none"}):\n${redact(context.formattedContext).slice(0, 2600)}`);
    } else {
      parts.push("Memória Supabase sem contexto retornado; consulte o handoff local.");
    }
  } catch (error) {
    parts.push(`Memória Supabase indisponível nesta abertura (${String(error?.message || "erro").slice(0, 160)}); consulte o handoff local.`);
  }

  const handoff = readLatestContinuationUpdate(3500);
  if (handoff.content) parts.push(`Snapshot local mais recente (${handoff.name}):\n${handoff.content}`);
  const roadmap = readTail("docs/ROADMAP_NEXT_UPDATE.md", 1800);
  if (roadmap) parts.push(`Plano atual:\n${roadmap}`);

  const additionalContext = parts.join("\n\n").slice(0, 8500);
  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: "SessionStart",
      additionalContext,
    },
  }));
}

main().catch(() => {
  const local = readLatestContinuationUpdate(3500).content;
  const context = ["[MIDIASTORE — fallback de continuidade local]", local].filter(Boolean).join("\n\n");
  process.stdout.write(JSON.stringify({
    hookSpecificOutput: { hookEventName: "SessionStart", additionalContext: context },
  }));
});
