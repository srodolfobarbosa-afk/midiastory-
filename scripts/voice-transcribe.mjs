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
  { name: "gemini-3-flash-preview", thinkingZero: true },
  { name: "gemini-3.6-flash", thinkingZero: false },
  { name: "gemini-3.5-flash", thinkingZero: false },
  { name: "gemini-3.8-flash", thinkingZero: false },
  { name: "gemini-2.5-flash", thinkingZero: true },
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
        maxOutputTokens: 1536,
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
                    text: "Transcreva fielmente a fala em português do Brasil. Retorne SOMENTE o texto falado, sem aspas, sem comentários e sem corrigir a gramática informal do falante. Ignore ruído de fundo/ventilador. Glossário fonético obrigatório do falante (use SEMPRE a grafia exata quando soar parecido): Maria (nunca Hari), Rodolfo, Midiastore (nunca MIDStory, Medstoria ou media story), FRIDAY (nunca Firefly), Antigravity (nunca Antigravit), agy (comando CLI, nunca GI), Gemini 4 Carbon / Carbon (nunca carpo ou carbom), Gemini 4 Neon, Gemini 4 Argon, Gemini 4 Hydrogen, Codex, OpenRouter, Supabase (nunca super base), Edge Function, Play Console, Google Play Billing, MediaStore, Scoped Storage, SHA-256, Kotlin, Gradle, AAB, APK, ADB, Moto G84, scrcpy, MCP, HUD (nunca UG), REA (morluto/rea), The Diary Of A CEO, subagentes, swarm, CMD, R$ 1 milhão / R$ 1.000.000,00 (nunca R1 milhão), regra 50/50, 100% (quando disser cem por cento, nunca 10%). Se não houver voz humana clara, retorne exatamente: [SEM_FALA]",
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
