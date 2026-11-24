#!/usr/bin/env node
/**
 * Antigravity Voice Narrator + Floating Avatar Overlay (Google AI Studio TTS)
 *
 * Uses Google AI Studio official TTS models (gemini-3.8-flash-tts -> gemini-2.5-flash-preview-tts)
 * to speak assistant responses in natural Brazilian Portuguese with a lightweight floating avatar.
 *
 * CLI usage:
 *   node scripts/voice-narrator.mjs --speak "Texto para falar"
 *   node scripts/voice-narrator.mjs --on
 *   node scripts/voice-narrator.mjs --off
 *   node scripts/voice-narrator.mjs --voice Kore|Aoede|Orus|Charon
 *   node scripts/voice-narrator.mjs --status
 *   node scripts/voice-narrator.mjs --hook-stop   (called by .agents/hooks.json Stop hook)
 */

import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync, execSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const SECRETS_PATH = path.join(PROJECT_ROOT, ".secrets", "ai-media-keys.json");
const AVATAR_PATH = path.join(PROJECT_ROOT, ".agents", "avatar", "antigravity-avatar.png");
const OVERLAY_SCRIPT = path.join(PROJECT_ROOT, "scripts", "voice-avatar-overlay.ps1");
const CONFIG_PATH = path.join(os.tmpdir(), "midiastory-voice-config.json");
const PID_PATH = path.join(os.tmpdir(), "midiastory-voice-overlay.pid");
const LAST_SPOKEN_PATH = path.join(os.tmpdir(), "midiastory-voice-last-step.json");
const COMPANION_HEARTBEAT = path.join(os.tmpdir(), "antigravity-companion-heartbeat.txt");
const COMPANION_QUEUE_DIR = path.join(os.tmpdir(), "antigravity-voice-queue");
const TTS_COOLDOWN_FILE = path.join(os.tmpdir(), "antigravity-tts-429-until.txt");

function isCompanionAlive() {
  try {
    if (fs.existsSync(COMPANION_HEARTBEAT)) {
      const ts = Number(fs.readFileSync(COMPANION_HEARTBEAT, "utf8").trim());
      const nowSec = Math.floor(Date.now() / 1000);
      if (Number.isFinite(ts) && nowSec - ts <= 20) return true;
    }
  } catch {}
  // Fallback seguro: nunca abre uma 2ª janela se o processo antigravity-companion.ps1 já estiver ativo
  try {
    const out = execSync(
      'powershell.exe -NoProfile -Command "(Get-CimInstance Win32_Process -Filter \\"Name=\'powershell.exe\'\\" | Where-Object { $_.CommandLine -like \'*antigravity-companion.ps1*\' }).Count"',
      { encoding: "utf8", timeout: 4000, windowsHide: true, stdio: ["ignore", "pipe", "ignore"] }
    ).trim();
    return Number(out) > 0;
  } catch {
    return false;
  }
}

const TTS_CACHE_DIR = path.join(os.tmpdir(), "antigravity-tts-cache");
const NEURAL_VOICES_PT = [
  "pt-BR-FranciscaNeural",
  "pt-BR-ThalitaMultilingualNeural",
];

const DEFAULT_CONFIG = {
  enabled: true,
  voiceName: "pt-BR-FranciscaNeural", // Mesma voz neural de estúdio da Maria no Android (zero robô!)
  maxChars: 560,
  showAvatar: true,
};

function loadConfig() {
  try {
    if (fs.existsSync(CONFIG_PATH)) {
      return { ...DEFAULT_CONFIG, ...JSON.parse(fs.readFileSync(CONFIG_PATH, "utf8")) };
    }
  } catch {}
  return { ...DEFAULT_CONFIG };
}

function saveConfig(cfg) {
  fs.writeFileSync(CONFIG_PATH, JSON.stringify(cfg, null, 2), "utf8");
}

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

function cleanTextForSpeech(markdown, maxChars = 750) {
  let text = String(markdown || "");
  // Remove fenced code blocks
  text = text.replace(/```[\s\S]*?```/g, " ");
  // Replace markdown links [label](url) with just label
  text = text.replace(/\[([^\]]+)\]\([^)]+\)/g, "$1");
  // Remove raw URLs
  text = text.replace(/https?:\/\/\S+/g, "");
  // Remove inline code backticks but keep readable short words
  text = text.replace(/`([^`]+)`/g, "$1");
  // Remove markdown headings, bold/italic markers, blockquote markers, table pipes
  text = text.replace(/^#{1,6}\s+/gm, "");
  text = text.replace(/^>\s*/gm, "");
  text = text.replace(/[*_~|]/g, " ");
  // Remove bullet dashes at line start
  text = text.replace(/^\s*[-+•]\s+/gm, "");
  // Sanitize arrows & XML-sensitive characters so MsEdgeTTS SSML never breaks
  text = text.replace(/->|=>/g, " para ");
  text = text.replace(/[<>&]/g, " ");
  // Collapse whitespace and newlines into natural pauses
  text = text.replace(/\r?\n+/g, ". ");
  text = text.replace(/\.\s*\./g, ".");
  text = text.replace(/\s{2,}/g, " ").trim();

  if (text.length > maxChars) {
    const sliced = text.slice(0, maxChars);
    const lastPunct = Math.max(sliced.lastIndexOf("."), sliced.lastIndexOf("!"), sliced.lastIndexOf("?"));
    text = (lastPunct > maxChars * 0.5 ? sliced.slice(0, lastPunct + 1) : sliced.trim() + "...");
  }
  return text;
}

function killPreviousOverlay() {
  try {
    if (fs.existsSync(PID_PATH)) {
      const pid = Number(fs.readFileSync(PID_PATH, "utf8").trim());
      if (Number.isInteger(pid) && pid > 0) {
        try {
          process.kill(pid);
        } catch {
          try {
            execSync(`taskkill /PID ${pid} /F /T`, { stdio: "ignore" });
          } catch {}
        }
      }
      fs.unlinkSync(PID_PATH);
    }
  } catch {}
}

function pcmToWav(pcmBuffer, sampleRate = 24000, numChannels = 1, bitsPerSample = 16) {
  const byteRate = (sampleRate * numChannels * bitsPerSample) / 8;
  const blockAlign = (numChannels * bitsPerSample) / 8;
  const header = Buffer.alloc(44);
  header.write("RIFF", 0);
  header.writeUInt32LE(36 + pcmBuffer.length, 4);
  header.write("WAVE", 8);
  header.write("fmt ", 12);
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(numChannels, 22);
  header.writeUInt32LE(sampleRate, 24);
  header.writeUInt32LE(byteRate, 28);
  header.writeUInt16LE(blockAlign, 32);
  header.writeUInt16LE(bitsPerSample, 34);
  header.write("data", 36);
  header.writeUInt32LE(pcmBuffer.length, 40);
  return Buffer.concat([header, pcmBuffer]);
}

const FFMPEG_PATH = "C:\\Users\\rodolfo\\tools\\ffmpeg\\bin\\ffmpeg.exe";
const MSEDGE_MODULE_URL = pathToFileURL(
  path.join(__dirname, "node_modules", "msedge-tts", "dist", "index.js")
).href;

function sleepMs(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function normalizeNeuralVoice(voiceName) {
  const v = String(voiceName || "").trim();
  if (v === "pt-BR-ThalitaMultilingualNeural" || v === "Thalita") {
    return "pt-BR-ThalitaMultilingualNeural";
  }
  return "pt-BR-FranciscaNeural";
}

function pruneTtsCache(maxFiles = 80) {
  try {
    if (!fs.existsSync(TTS_CACHE_DIR)) return;
    const entries = fs
      .readdirSync(TTS_CACHE_DIR)
      .filter((f) => f.endsWith(".wav"))
      .map((f) => {
        const full = path.join(TTS_CACHE_DIR, f);
        return { full, mtimeMs: fs.statSync(full).mtimeMs };
      })
      .sort((a, b) => b.mtimeMs - a.mtimeMs);
    for (let i = maxFiles; i < entries.length; i++) {
      try { fs.unlinkSync(entries[i].full); } catch {}
    }
  } catch {}
}

async function synthesizeEdgeNeuralOnce(text, targetVoice) {
  const { MsEdgeTTS, OUTPUT_FORMAT } = await import(MSEDGE_MODULE_URL);
  const tts = new MsEdgeTTS();
  await tts.setMetadata(targetVoice, OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3);
  const { audioStream } = tts.toStream(text);
  const chunks = [];
  for await (const chunk of audioStream) {
    chunks.push(chunk);
  }
  const mp3Buf = Buffer.concat(chunks);
  if (mp3Buf.length < 120) {
    throw new Error(`Edge Neural (${targetVoice}) retornou áudio vazio.`);
  }

  const stamp = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
  const tmpMp3 = path.join(os.tmpdir(), `antigravity-edge-${stamp}.mp3`);
  const tmpWav = path.join(os.tmpdir(), `antigravity-edge-${stamp}.wav`);
  fs.writeFileSync(tmpMp3, mp3Buf);
  const conv = spawnSync(
    FFMPEG_PATH,
    ["-y", "-i", tmpMp3, "-ar", "24000", "-ac", "1", "-c:a", "pcm_s16le", tmpWav, "-loglevel", "error"],
    { stdio: "ignore", windowsHide: true, timeout: 25000 }
  );
  try { fs.unlinkSync(tmpMp3); } catch {}
  if (conv.status === 0 && fs.existsSync(tmpWav)) {
    const wavBuf = fs.readFileSync(tmpWav);
    try { fs.unlinkSync(tmpWav); } catch {}
    return { buffer: wavBuf, model: `edge-neural-${targetVoice}` };
  }
  throw new Error(`Falha ao converter MP3 neural (${targetVoice}) para WAV.`);
}

/**
 * Síntese 100% travada em Voz Neural de Estúdio (FranciscaNeural -> ThalitaMultilingualNeural)
 * com cache em disco por SHA-1 e ZERO fallback SAPI5 robótico (banido em definitivo).
 */
async function synthesizeWav(text, requestedVoice) {
  const primaryVoice = normalizeNeuralVoice(requestedVoice);
  const candidateVoices = [
    primaryVoice,
    ...NEURAL_VOICES_PT.filter((v) => v !== primaryVoice),
  ];

  fs.mkdirSync(TTS_CACHE_DIR, { recursive: true });
  const cacheHash = crypto
    .createHash("sha1")
    .update(`${primaryVoice}|${text}`, "utf8")
    .digest("hex");
  const cacheFile = path.join(TTS_CACHE_DIR, `${cacheHash}.wav`);
  if (fs.existsSync(cacheFile)) {
    try {
      const cached = fs.readFileSync(cacheFile);
      if (cached.length > 200) {
        return { buffer: cached, model: `cache-neural-${primaryVoice}` };
      }
    } catch {}
  }

  let lastErr = null;
  const retryDelays = [0, 350, 800];
  for (let attempt = 0; attempt < retryDelays.length; attempt++) {
    if (retryDelays[attempt] > 0) await sleepMs(retryDelays[attempt]);
    const voice = candidateVoices[attempt % candidateVoices.length];
    try {
      const result = await synthesizeEdgeNeuralOnce(text, voice);
      try {
        fs.writeFileSync(cacheFile, result.buffer);
        pruneTtsCache(80);
      } catch {}
      return result;
    } catch (err) {
      lastErr = err;
    }
  }

  // Fallback SAPI5 robótico (Microsoft Maria Desktop) 100% REMOVIDO por decisão do fundador:
  // se a internet cair completamente, registra log silencioso em vez de falar com voz robótica.
  try {
    fs.appendFileSync(
      path.join(os.tmpdir(), "antigravity-voice-tts.log"),
      `${new Date().toISOString()} NEURAL_OFFLINE_SKIP: ${lastErr?.message || lastErr}\n`
    );
  } catch {}
  return { buffer: null, model: "neural-offline-muted" };
}

async function speakText(rawText, opts = {}) {
  const cfg = loadConfig();
  if (!cfg.enabled && !opts.force) return { skipped: true, reason: "disabled" };

  const cleaned = cleanTextForSpeech(rawText, cfg.maxChars);
  if (!cleaned || cleaned.length < 3) return { skipped: true, reason: "empty" };

  const voiceName = normalizeNeuralVoice(opts.voiceName || cfg.voiceName);
  if (cfg.voiceName !== voiceName) {
    cfg.voiceName = voiceName;
    try { saveConfig(cfg); } catch {}
  }
  const { buffer, model } = await synthesizeWav(cleaned, voiceName);
  if (!buffer || buffer.length < 100) {
    return { skipped: true, reason: "neural_offline_no_robot", model };
  }

  // If the persistent floating companion widget is open, drop the WAV into its queue and return immediately!
  if (isCompanionAlive()) {
    fs.mkdirSync(COMPANION_QUEUE_DIR, { recursive: true });
    const queuedWav = path.join(COMPANION_QUEUE_DIR, `voice-${Date.now()}.wav`);
    fs.writeFileSync(queuedWav, buffer);
    return { ok: true, mode: "companion_queue", model, voiceName, bytes: buffer.length };
  }

  const wavPath = path.join(os.tmpdir(), `antigravity-voice-${Date.now()}.wav`);
  fs.writeFileSync(wavPath, buffer);

  killPreviousOverlay();

  const args = [
    "-NoProfile",
    "-STA",
    "-ExecutionPolicy",
    "Bypass",
    "-File",
    OVERLAY_SCRIPT,
    "-WavPath",
    wavPath,
  ];
  if (cfg.showAvatar && fs.existsSync(AVATAR_PATH)) {
    args.push("-AvatarPath", AVATAR_PATH);
  }

  const errLogPath = path.join(os.tmpdir(), "antigravity-voice-error.log");
  // Run synchronously so Windows Job Object cleanup in CLI hooks does not kill the overlay/audio early.
  // Clicking the avatar closes it immediately and returns control.
  const res = spawnSync("powershell.exe", args, {
    stdio: ["ignore", "pipe", "pipe"],
    windowsHide: true,
    timeout: 110000,
  });

  const stderrText = String(res.stderr || "").trim();
  if (stderrText) {
    try { fs.writeFileSync(errLogPath, stderrText, "utf8"); } catch {}
  }

  return { ok: res.status === 0, model, voiceName, bytes: buffer.length, exitCode: res.status };
}

function readStdin() {
  return new Promise((resolve) => {
    let data = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (c) => (data += c));
    process.stdin.on("end", () => resolve(data));
    process.stdin.on("error", () => resolve(data));
  });
}

function readLastPlannerResponse(transcriptPath) {
  if (!transcriptPath || !fs.existsSync(transcriptPath)) return null;
  const rawLines = fs.readFileSync(transcriptPath, "utf8").split(/\r?\n/).filter(Boolean);
  const fullPath = transcriptPath.replace(/transcript\.jsonl$/i, "transcript_full.jsonl");
  let fullLines = null;

  const limit = Math.min(rawLines.length, 40);
  for (let offset = 0; offset < limit; offset++) {
    const line = rawLines[rawLines.length - 1 - offset];
    try {
      const obj = JSON.parse(line);
      if (obj.type === "PLANNER_RESPONSE" && obj.content && obj.content.trim()) {
        let content = obj.content;
        if (
          Array.isArray(obj.truncated_fields) &&
          obj.truncated_fields.includes("content") &&
          fs.existsSync(fullPath)
        ) {
          if (!fullLines) {
            fullLines = fs.readFileSync(fullPath, "utf8").split(/\r?\n/).filter(Boolean);
          }
          const fullObj = JSON.parse(fullLines[rawLines.length - 1 - offset] || "{}");
          if (fullObj?.content) content = fullObj.content;
        }
        return { stepIndex: obj.step_index, content };
      }
    } catch {}
  }
  return null;
}

async function handleHookStop() {
  try {
    const raw = await readStdin();
    const payload = raw.trim() ? JSON.parse(raw) : {};
    const cfg = loadConfig();
    if (!cfg.enabled) {
      process.stdout.write("{}");
      return;
    }
    const last = readLastPlannerResponse(payload.transcriptPath);
    if (!last) {
      process.stdout.write("{}");
      return;
    }

    const convId = String(payload.conversationId || "default");
    let state = {};
    try {
      if (fs.existsSync(LAST_SPOKEN_PATH)) {
        state = JSON.parse(fs.readFileSync(LAST_SPOKEN_PATH, "utf8"));
      }
    } catch {}

    if (state[convId] === last.stepIndex) {
      process.stdout.write("{}");
      return;
    }
    state[convId] = last.stepIndex;
    fs.writeFileSync(LAST_SPOKEN_PATH, JSON.stringify(state), "utf8");

    await speakText(last.content);
  } catch (err) {
    try {
      fs.appendFileSync(
        path.join(os.tmpdir(), "antigravity-voice-tts.log"),
        `${new Date().toISOString()} HOOK_ERR: ${err?.message || err}\n`
      );
    } catch {}
  }
  process.stdout.write("{}");
}

async function main() {
  const args = process.argv.slice(2);
  if (args.includes("--hook-stop")) {
    await handleHookStop();
    return;
  }
  if (args.includes("--on")) {
    const cfg = loadConfig();
    cfg.enabled = true;
    saveConfig(cfg);
    console.log("VOICE_ENABLED:", JSON.stringify(cfg));
    return;
  }
  if (args.includes("--off")) {
    const cfg = loadConfig();
    cfg.enabled = false;
    saveConfig(cfg);
    killPreviousOverlay();
    console.log("VOICE_DISABLED:", JSON.stringify(cfg));
    return;
  }
  if (args.includes("--status")) {
    console.log(JSON.stringify({ ...loadConfig(), avatarExists: fs.existsSync(AVATAR_PATH) }, null, 2));
    return;
  }
  const voiceIdx = args.indexOf("--voice");
  if (voiceIdx !== -1 && args[voiceIdx + 1]) {
    const cfg = loadConfig();
    cfg.voiceName = args[voiceIdx + 1];
    saveConfig(cfg);
    console.log("VOICE_UPDATED:", JSON.stringify(cfg));
    return;
  }
  const speakIdx = args.indexOf("--speak");
  if (speakIdx !== -1) {
    const text = args.slice(speakIdx + 1).join(" ");
    const res = await speakText(text, { force: true });
    console.log(JSON.stringify(res));
    return;
  }
  console.log("Uso: --speak <texto> | --on | --off | --voice <nome> | --status | --hook-stop");
}

main().catch((err) => {
  if (process.argv.includes("--hook-stop")) {
    process.stdout.write("{}");
  } else {
    console.error(err.message);
    process.exit(1);
  }
});
