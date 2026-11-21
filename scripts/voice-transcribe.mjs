#!/usr/bin/env node
/**
 * Ultra-fast Brazilian Portuguese Voice-to-Text engine (Google AI Studio)
 * Uses thinkingBudget: 0 + compressed DSP audio (MP3/OGG/WAV) for < 1.2s latency.
 *
 * Usage:
 *   node scripts/voice-transcribe.mjs <audioPath> [outUtf8TxtPath]
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const SECRETS_PATH = path.join(PROJECT_ROOT, ".secrets", "ai-media-keys.json");

const STT_MODELS = [
  { name: "gemini-2.5-flash", thinkingZero: true },
  { name: "gemini-flash-lite-latest", thinkingZero: true },
  { name: "gemini-3.5-flash-lite", thinkingZero: false },
  { name: "gemini-3.8-flash", thinkingZero: false },
];

function loadGoogleApiKey() {
  let key = process.env.GOOGLE_AI_STUDIO_API_KEY || process.env.GOOGLE_AQ_API_KEY || process.env.GEMINI_API_KEY || "";
  if (!key && fs.existsSync(SECRETS_PATH)) {
    try {
      const raw = JSON.parse(fs.readFileSync(SECRETS_PATH, "utf8"));
      key = raw.GOOGLE_AI_STUDIO_API_KEY || raw.GOOGLE_AQ_API_KEY || "";
    } catch {}
  }
  return key;
}

function detectMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  if (ext === ".mp3") return "audio/mp3";
  if (ext === ".ogg" || ext === ".opus") return "audio/ogg";
  return "audio/wav";
}

async function main() {
  const audioPath = process.argv[2];
  const outTxtPath = process.argv[3] || "";
  if (!audioPath || !fs.existsSync(audioPath)) {
    process.exit(1);
  }
  const key = loadGoogleApiKey();
  if (!key) {
    process.exit(2);
  }

  const b64 = fs.readFileSync(audioPath).toString("base64");
  const mimeType = detectMimeType(audioPath);
  let lastErr = "";

  for (const model of STT_MODELS) {
    try {
      const generationConfig = {
        temperature: 0.0,
        maxOutputTokens: 512,
        ...(model.thinkingZero ? { thinkingConfig: { thinkingBudget: 0 } } : {}),
      };

      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model.name}:generateContent?key=${key}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          signal: AbortSignal.timeout(18000),
          body: JSON.stringify({
            contents: [
              {
                parts: [
                  {
                    text: "Transcreva exatamente a fala em português do Brasil. Retorne SOMENTE o texto falado, sem aspas nem comentários. Ignore ruído de fundo/ventilador. Glossário técnico do falante (use a grafia correta quando soar parecido): Midiastore (nunca MIDStory), Supabase (nunca super base), AGI (nunca GI), 100% (quando disser cem por cento, nunca 10%), Gemini 4, HUD (nunca UG), Antigravity, Play Console, Kotlin, ADB, MCP, subagentes. Se não houver voz humana clara, retorne exatamente: [SEM_FALA]",
                  },
                  {
                    inlineData: { mimeType, data: b64 },
                  },
                ],
              },
            ],
            generationConfig,
          }),
        }
      );

      if (!res.ok) {
        lastErr = `${model.name}:HTTP_${res.status}`;
        continue;
      }

      const data = await res.json();
      const text = String(data?.candidates?.[0]?.content?.parts?.[0]?.text || "").trim();
      if (!text || text.includes("[SEM_FALA]")) {
        process.exit(0);
      }
      if (outTxtPath) {
        fs.writeFileSync(outTxtPath, text, "utf8");
      } else {
        process.stdout.write(text);
      }
      process.exit(0);
    } catch (e) {
      lastErr = `${model.name}:${e.name || e.message}`;
    }
  }

  console.error(`STT_FAILED:${lastErr}`);
  process.exit(3);
}

main().catch(() => process.exit(4));
