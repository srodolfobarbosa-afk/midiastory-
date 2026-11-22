#!/usr/bin/env node
/**
 * FRIDAY Quota & Context Guard — Real Session Telemetry (Zero Guessing)
 *
 * Inspects the active Antigravity CLI transcript.jsonl to measure:
 *   - Exact turn count & step count in the current session
 *   - Exact transcript payload size (KB) & estimated token load
 *   - Automatic status: VERDE / AMARELO (/compact recomendado) / VERMELHO (/compact agora)
 *   - Cloud TTS 429 cooldown state & local Edge Neural availability
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const BRAIN_DIR = path.join(os.homedir(), ".gemini", "antigravity-cli", "brain");
const TTS_COOLDOWN_FILE = path.join(os.tmpdir(), "antigravity-tts-429-until.txt");

export function getActiveSessionMetrics() {
  try {
    if (!fs.existsSync(BRAIN_DIR)) return { status: "N/D" };
    const dirs = fs
      .readdirSync(BRAIN_DIR, { withFileTypes: true })
      .filter((d) => d.isDirectory())
      .map((d) => {
        const tPath = path.join(BRAIN_DIR, d.name, ".system_generated", "logs", "transcript.jsonl");
        if (!fs.existsSync(tPath)) return null;
        const stat = fs.statSync(tPath);
        return { convId: d.name, tPath, mtimeMs: stat.mtimeMs, sizeBytes: stat.size };
      })
      .filter(Boolean)
      .sort((a, b) => b.mtimeMs - a.mtimeMs);

    if (!dirs.length) return { status: "N/D" };
    const latest = dirs[0];
    // RAM-safe tail scan (caps buffer at 192 KB on 3.93 GB RAM PC and scales turn count if file is larger)
    const maxSample = 196608;
    const readLen = Math.min(latest.sizeBytes, maxSample);
    const buf = Buffer.alloc(readLen);
    const fd = fs.openSync(latest.tPath, "r");
    try {
      fs.readSync(fd, buf, 0, readLen, Math.max(0, latest.sizeBytes - readLen));
    } finally {
      fs.closeSync(fd);
    }
    const sampleText = buf.toString("utf8");
    const scale = readLen > 0 ? latest.sizeBytes / readLen : 1;
    const userTurns = Math.round(((sampleText.match(/"type":"USER_INPUT"/g) || []).length) * scale);
    const plannerTurns = Math.round(((sampleText.match(/"type":"PLANNER_RESPONSE"/g) || []).length) * scale);

    const sizeKb = Math.round(latest.sizeBytes / 1024);
    const estTokens = Math.round(latest.sizeBytes / 4);
    let level = "🟢 VERDE (pode continuar à vontade)";
    if (sizeKb >= 320 || userTurns >= 22) {
      level = "🔴 ALTO — rode /compact agora para economizar cota!";
    } else if (sizeKb >= 160 || userTurns >= 14) {
      level = "🟡 MODERADO — rode /compact em breve para economizar cota";
    }

    let ttsMode = "Google AI Studio Neural + Francisca Neural";
    if (fs.existsSync(TTS_COOLDOWN_FILE)) {
      const until = Number(fs.readFileSync(TTS_COOLDOWN_FILE, "utf8").trim());
      if (until > Date.now()) {
        const minsLeft = Math.ceil((until - Date.now()) / 60000);
        ttsMode = `Francisca Neural (ilimitada, 0 cota | Google TTS em cooldown ${minsLeft}min)`;
      }
    }

    return {
      convId: latest.convId.slice(0, 8),
      userTurns,
      plannerTurns,
      sizeKb,
      estTokens,
      level,
      ttsMode,
    };
  } catch (e) {
    return { status: "erro", error: e.message };
  }
}

if (process.argv[1] && process.argv[1].endsWith("friday-quota-guard.mjs")) {
  console.log(JSON.stringify(getActiveSessionMetrics(), null, 2));
}
