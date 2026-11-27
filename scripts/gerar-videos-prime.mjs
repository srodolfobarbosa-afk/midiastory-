#!/usr/bin/env node
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const ASSETS = path.join(ROOT, "docs", "assets", "prime");
const OUTPUT_DIR = path.join(ROOT, "marketing", "criativos-v2.9.0", "videos");
const MODEL = process.env.HF_VIDEO_MODEL || "Wan-AI/Wan2.2-TI2V-5B";
const PROVIDER = process.env.HF_VIDEO_PROVIDER || "fal-ai";
const ROUTER = "https://router.huggingface.co";
const SCREENSHOT = path.join(ASSETS, "midiastore-tela-real-app-v290.png");
const ENDCARD = path.join(ASSETS, "midiastore-app-android-reels-9x16.png");
const FFMPEG = process.env.FFMPEG_PATH || (process.platform === "win32" ? "ffmpeg.exe" : "ffmpeg");

const ADS = [
  {
    slug: "prime-01-armazenamento-cheio",
    output: "prime-01-armazenamento-cheio-1080x1920.mp4",
    fallbackImage: "midiastore-app-android-limpar-celular-16x9.png",
    title: "Armazenamento cheio na hora errada",
    hookText: "Travou na hora errada?",
    appText: "Libere espaco com revisao visual",
    hookPrompt:
      "Vertical 9:16 cinematic commercial, golden hour at a Rio de Janeiro beach, Brazilian adult trying to record a joyful family moment on an Android phone, the phone briefly shows a generic low-storage warning with no readable brand or invented app interface, concerned expression turns hopeful, Sugarloaf Mountain softly blurred in background, natural handheld camera, realistic premium advertising, clean image, no logos, no text, no watermark.",
    screenSeconds: 7,
    endSeconds: 3,
    frames: 81,
  },
  {
    slug: "prime-02-whatsapp-fotos",
    output: "prime-02-whatsapp-fotos-1080x1920.mp4",
    fallbackImage: "midiastore-app-android-memoria-cheia-1x1.png",
    title: "WhatsApp, fotos e memoria cheia",
    hookText: "Celular lento e sem espaco?",
    appText: "Revise seus arquivos com controle",
    hookPrompt:
      "Vertical 9:16 bright premium technology commercial in a modern Brazilian apartment, friendly Brazilian woman in her late 20s holding an Android phone and looking directly at camera with a reassuring natural smile, subtle blue and white daylight, realistic person, modest movements, crisp commercial photography, no logos, no text, no watermark.",
    screenSeconds: 8,
    endSeconds: 3,
    frames: 81,
  },
  {
    slug: "prime-03-comprimir-sem-apagar",
    output: "prime-03-comprimir-sem-apagar-1080x1920.mp4",
    fallbackImage: "midiastore-app-android-telefone-pesado-4x5.png",
    title: "Preserve suas lembrancas",
    hookText: "Nao apague suas memorias",
    appText: "Conheca o Midiastore",
    hookPrompt:
      "Vertical 9:16 close-up cinematic commercial in a warm Brazilian family home, a person's hand hesitates above a smartphone gallery thumbnail showing a joyful family memory, the hand chooses to keep the video, gentle reassuring mood, natural light, shallow depth of field, premium realistic advertising, no visible brand, no legible text, no watermark.",
    screenSeconds: 10,
    endSeconds: 3,
    frames: 81,
  },
];

function loadDotEnv(filePath) {
  if (!fs.existsSync(filePath)) return {};
  const values = {};
  for (const line of fs.readFileSync(filePath, "utf8").split(/\r?\n/)) {
    const match = line.match(/^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (!match) continue;
    let value = match[2];
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    } else {
      value = value.replace(/\s+#.*$/, "");
    }
    values[match[1]] = value;
  }
  return values;
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { cwd: ROOT, windowsHide: true, stdio: ["ignore", "ignore", "pipe"], ...options });
    let stderr = "";
    child.stderr.setEncoding("utf8");
    child.stderr.on("data", (chunk) => {
      stderr = (stderr + chunk).slice(-12000);
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) return resolve();
      reject(new Error(`${path.basename(command)} exited with code ${code}: ${stderr.trim()}`));
    });
  });
}

async function requestJson(url, options, description) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`${description} failed (HTTP ${response.status}): ${body.slice(0, 1200)}`);
  }
  return response.json();
}

async function generateHookVideo(prompt, frames, token, destination) {
  const mappingUrl = new URL(`/api/models/${MODEL}?expand[]=inferenceProviderMapping`, "https://huggingface.co");
  const mapping = await requestJson(mappingUrl, {
    headers: { Authorization: `Bearer ${token}` },
  }, "Hugging Face model lookup");
  const providerModel = mapping.inferenceProviderMapping?.[PROVIDER]?.providerId;
  if (!providerModel) throw new Error(`Model ${MODEL} is not currently served by ${PROVIDER}.`);

  const submitUrl = new URL(`/${PROVIDER}/${providerModel}`, ROUTER);
  submitUrl.searchParams.set("_subdomain", "queue");
  const queued = await requestJson(submitUrl, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      prompt,
      negative_prompt: "text, subtitles, logos, watermark, distorted face, extra fingers, deformed phone",
      aspect_ratio: "9:16",
      resolution: "480p",
      num_frames: frames,
      frames_per_second: 16,
      guidance_scale: 4,
      enable_safety_checker: true,
    }),
    signal: AbortSignal.timeout(120_000),
  }, "Hugging Face video generation request");
  if (!queued.request_id || !queued.response_url) throw new Error("Hugging Face returned an invalid queue response.");

  const responsePath = new URL(queued.response_url).pathname;
  const query = "?_subdomain=queue";
  const statusUrl = `${ROUTER}/${PROVIDER}${responsePath}/status${query}`;
  const resultUrl = `${ROUTER}/${PROVIDER}${responsePath}${query}`;
  let status = queued.status;
  const deadline = Date.now() + 30 * 60_000;
  while (status !== "COMPLETED") {
    if (status === "FAILED") throw new Error(`Hugging Face generation failed for request ${queued.request_id}.`);
    if (Date.now() >= deadline) throw new Error(`Hugging Face generation timed out for request ${queued.request_id}.`);
    await new Promise((resolve) => setTimeout(resolve, 2500));
    const statusResponse = await requestJson(statusUrl, {
      headers: { Authorization: `Bearer ${token}` },
      signal: AbortSignal.timeout(60_000),
    }, "Hugging Face video status");
    status = statusResponse.status;
    process.stdout.write(`  ${status || "WAITING"}\n`);
  }

  const result = await requestJson(resultUrl, {
    headers: { Authorization: `Bearer ${token}` },
    signal: AbortSignal.timeout(60_000),
  }, "Hugging Face video result");
  const videoUrl = result.video?.url;
  if (typeof videoUrl !== "string") throw new Error("Hugging Face response did not include a video URL.");
  const video = await fetch(videoUrl, { signal: AbortSignal.timeout(180_000) });
  if (!video.ok) throw new Error(`Generated video download failed (HTTP ${video.status}).`);
  const bytes = Buffer.from(await video.arrayBuffer());
  if (bytes.length < 100_000) throw new Error("Generated video is unexpectedly small; refusing to encode it.");
  fs.writeFileSync(destination, bytes);
}

async function composeVideo(ad, hookFile, destination) {
  const duration = ad.screenSeconds + ad.endSeconds + 5;
  const inputs = [
    "-stream_loop", "-1", "-i", hookFile,
    "-loop", "1", "-framerate", "30", "-t", String(ad.screenSeconds), "-i", SCREENSHOT,
    "-loop", "1", "-framerate", "30", "-t", String(ad.endSeconds), "-i", ENDCARD,
  ];
  const filter = [
    "[0:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,fps=30,setsar=1,trim=duration=5,setpts=PTS-STARTPTS[v0]",
    `[1:v]scale=864:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:color=black,fps=30,setsar=1,trim=duration=${ad.screenSeconds},setpts=PTS-STARTPTS[v1]`,
    `[2:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,fps=30,setsar=1,trim=duration=${ad.endSeconds},setpts=PTS-STARTPTS[v2]`,
    "[v0][v1][v2]concat=n=3:v=1:a=0,format=yuv420p[outv]",
  ].join(";");
  await run(FFMPEG, [
    "-hide_banner", "-loglevel", "error", "-y", ...inputs,
    "-filter_complex", filter, "-map", "[outv]", "-an",
    "-c:v", "libx264", "-preset", "medium", "-crf", "22", "-r", "30",
    "-t", String(duration), "-movflags", "+faststart", destination,
  ]);
}

async function makeLocalHook(ad, tempDir) {
  const source = path.join(ASSETS, ad.fallbackImage);
  const output = path.join(tempDir, `${ad.slug}-local-hook.mp4`);
  const filter = [
    "[0:v]split=2[bg0][fg0]",
    "[bg0]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,boxblur=24:8,setsar=1[bg]",
    "[fg0]scale=1040:1760:force_original_aspect_ratio=decrease,setsar=1[fg]",
    "[bg][fg]overlay=(W-w)/2:(H-h)/2,fps=30,trim=duration=5,setpts=PTS-STARTPTS,format=yuv420p[outv]",
  ].join(";");
  await run(FFMPEG, [
    "-hide_banner", "-loglevel", "error", "-y", "-loop", "1", "-framerate", "30", "-t", "5", "-i", source,
    "-filter_complex", filter, "-map", "[outv]", "-an", "-c:v", "libx264", "-preset", "medium",
    "-crf", "22", "-r", "30", "-t", "5", "-movflags", "+faststart", output,
  ]);
  return output;
}

async function main() {
  const dotenv = loadDotEnv(path.join(ROOT, ".env"));
  const token = process.env.HF_TOKEN || process.env.HUGGINGFACE_TOKEN || dotenv.HF_TOKEN || dotenv.HUGGINGFACE_TOKEN;
  if (!token) throw new Error("Set HF_TOKEN or HUGGINGFACE_TOKEN in the environment or repository .env file.");
  for (const file of [SCREENSHOT, ENDCARD]) {
    if (!fs.existsSync(file)) throw new Error(`Required Prime PNG is missing: ${path.relative(ROOT, file)}`);
  }
  for (const ad of ADS) {
    if (!fs.existsSync(path.join(ASSETS, ad.fallbackImage))) {
      throw new Error(`Required fallback PNG is missing: ${ad.fallbackImage}`);
    }
  }
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "midiastore-prime-"));
  try {
    let useLocalFallback = false;
    for (const ad of ADS) {
      if (useLocalFallback) {
        process.stdout.write(`Generating ${ad.title} from supplied artwork with local FFmpeg.\n`);
      } else {
        process.stdout.write(`Generating ${ad.title} with ${MODEL} via ${PROVIDER}. This may use Hugging Face Inference Provider credits.\n`);
      }
      const hookFile = path.join(tempDir, `${ad.slug}-hook.mp4`);
      const output = path.join(OUTPUT_DIR, ad.output);
      if (!useLocalFallback) {
        try {
          await generateHookVideo(ad.hookPrompt, ad.frames, token, hookFile);
        } catch (error) {
          if (!String(error.message).includes("HTTP 402")) throw error;
          useLocalFallback = true;
          process.stdout.write("Hugging Face credits are unavailable; using the supplied campaign stills for local FFmpeg motion videos.\n");
        }
      }
      const hook = useLocalFallback ? await makeLocalHook(ad, tempDir) : hookFile;
      await composeVideo(ad, hook, output);
      process.stdout.write(`Created ${path.relative(ROOT, output)}\n`);
    }
  } finally {
    fs.rmSync(tempDir, { recursive: true, force: true });
  }
}

main().catch((error) => {
  process.stderr.write(`${error.message}\n`);
  process.exitCode = 1;
});
