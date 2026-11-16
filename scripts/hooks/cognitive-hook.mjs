#!/usr/bin/env node
/**
 * Antigravity Native Lifecycle Hook for Continuous Learning & Cross-Session Retrieval
 *
 * Events:
 *   - SessionStart: retrieves core rules, pending tasks, and recent conversations from Supabase
 *   - PreInvocation: performs query-specific semantic+FTS retrieval once per new user prompt
 *   - Stop: analyzes the finished turn from transcriptPath and persists validated knowledge & conversations
 */

import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { analyzeAndLearnTurn, retrieveRelevantContext } from "../lib/cognitive-memory.mjs";

const STATE_FILE = path.join(os.tmpdir(), "midiastory-cognitive-hook-state.json");

function readStdin() {
  return new Promise((resolve) => {
    let data = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => {
      data += chunk;
    });
    process.stdin.on("end", () => resolve(data));
    process.stdin.on("error", () => resolve(data));
  });
}

function loadState() {
  try {
    if (fs.existsSync(STATE_FILE)) {
      return JSON.parse(fs.readFileSync(STATE_FILE, "utf8"));
    }
  } catch {}
  return { preByConv: {}, stopByConv: {} };
}

function saveState(state) {
  try {
    const trimObj = (obj) => {
      const entries = Object.entries(obj || {});
      return Object.fromEntries(entries.slice(-30));
    };
    const next = {
      preByConv: trimObj(state.preByConv),
      stopByConv: trimObj(state.stopByConv),
    };
    fs.writeFileSync(STATE_FILE, JSON.stringify(next), "utf8");
  } catch {}
}

function stripUserRequestTags(text) {
  const s = String(text || "");
  const match = s.match(/<USER_REQUEST>\s*([\s\S]*?)\s*<\/USER_REQUEST>/i);
  return (match ? match[1] : s).trim();
}

function readTranscriptTailExchange(transcriptPath) {
  if (!transcriptPath || !fs.existsSync(transcriptPath)) return null;
  const rawLines = fs.readFileSync(transcriptPath, "utf8").split(/\r?\n/).filter(Boolean);
  if (rawLines.length === 0) return null;

  const fullPath = transcriptPath.replace(/transcript\.jsonl$/i, "transcript_full.jsonl");
  let fullLines = null;

  const getUnTruncatedContent = (entry, idxFromEndOffset, totalLen) => {
    if (
      Array.isArray(entry.truncated_fields) &&
      entry.truncated_fields.includes("content") &&
      fs.existsSync(fullPath)
    ) {
      if (!fullLines) {
        fullLines = fs.readFileSync(fullPath, "utf8").split(/\r?\n/).filter(Boolean);
      }
      const lineIdx = totalLen - 1 - idxFromEndOffset;
      const fullRaw = fullLines[lineIdx];
      if (fullRaw) {
        try {
          const fullObj = JSON.parse(fullRaw);
          if (fullObj?.content) return fullObj.content;
        } catch {}
      }
    }
    return entry.content || "";
  };

  let lastUser = null;
  let lastPlanner = null;
  const scanLimit = Math.min(rawLines.length, 60);

  for (let offset = 0; offset < scanLimit; offset++) {
    const line = rawLines[rawLines.length - 1 - offset];
    try {
      const obj = JSON.parse(line);
      if (!lastPlanner && obj.type === "PLANNER_RESPONSE" && obj.content && obj.content.trim()) {
        lastPlanner = {
          stepIndex: obj.step_index,
          content: getUnTruncatedContent(obj, offset, rawLines.length),
        };
      } else if (lastPlanner && !lastUser && obj.type === "USER_INPUT" && obj.content) {
        const cleaned = stripUserRequestTags(getUnTruncatedContent(obj, offset, rawLines.length));
        if (cleaned) {
          lastUser = { stepIndex: obj.step_index, content: cleaned };
          break;
        }
      }
    } catch {}
  }

  if (!lastUser || !lastPlanner) return null;
  return { user: lastUser, planner: lastPlanner };
}

async function handleSessionStart(payload) {
  const ctx = await retrieveRelevantContext({
    query: "",
    sessionId: payload.conversationId || null,
    limit: 6,
    includeCore: true,
    includeDocs: true,
    includeConversations: true,
    includeTasksAndLearnings: true,
  });
  if (!ctx.formattedContext) return {};
  return {
    injectSteps: [
      {
        systemMessage: {
          systemMessage: `[MEMÓRIA COGNITIVA BOOT (${ctx.source} | ${ctx.embeddingModel || "none"})]\n${ctx.formattedContext.slice(0, 3200)}`,
        },
      },
    ],
  };
}

async function handlePreInvocation(payload) {
  const convId = String(payload.conversationId || "default");
  const cleanPrompt = stripUserRequestTags(payload.lastUserInput || "");
  if (!cleanPrompt || cleanPrompt.length < 4) return {};

  const promptHash = crypto.createHash("sha256").update(cleanPrompt).digest("hex").slice(0, 16);
  const state = loadState();
  if (state.preByConv[convId] === promptHash) {
    return {};
  }
  state.preByConv[convId] = promptHash;
  saveState(state);

  const ctx = await retrieveRelevantContext({
    query: cleanPrompt,
    sessionId: convId,
    limit: 6,
    includeCore: false,
    includeDocs: false,
    includeTasksAndLearnings: false,
    includeConversations: true,
  });
  if (!ctx.formattedContext) return {};

  return {
    injectSteps: [
      {
        systemMessage: {
          systemMessage: `[CONTEXTO SEMÂNTICO RECUPERADO (${ctx.source} | ${ctx.embeddingModel || "none"})]\n${ctx.formattedContext.slice(0, 3200)}`,
        },
      },
    ],
  };
}

async function handleStop(payload) {
  if (payload.terminationReason && payload.terminationReason !== "NO_TOOL_CALL") {
    return {};
  }
  if (payload.continuationActive) {
    return {};
  }

  const convId = String(payload.conversationId || "default");
  const exchange = readTranscriptTailExchange(payload.transcriptPath);
  if (!exchange) return {};

  const state = loadState();
  if (state.stopByConv[convId] === exchange.planner.stepIndex) {
    return {};
  }

  await analyzeAndLearnTurn({
    question: exchange.user.content,
    answer: exchange.planner.content,
    modelUsed: payload.modelName || "antigravity_agent",
    sessionId: convId,
    origem: "antigravity_stop_hook",
  });

  state.stopByConv[convId] = exchange.planner.stepIndex;
  saveState(state);

  return {};
}

async function main() {
  const event = process.argv[2] || "";
  const rawInput = await readStdin();
  let payload = {};
  try {
    payload = rawInput ? JSON.parse(rawInput) : {};
  } catch {}

  let output = {};
  try {
    if (event === "SessionStart") {
      output = await handleSessionStart(payload);
    } else if (event === "PreInvocation") {
      output = await handlePreInvocation(payload);
    } else if (event === "Stop") {
      output = await handleStop(payload);
    }
  } catch {
    output = {};
  }

  process.stdout.write(JSON.stringify(output));
}

main().catch(() => {
  process.stdout.write("{}");
});
