#!/usr/bin/env node
/**
 * scripts/render-single-moda-clip.mjs
 *
 * Renders a single clean commercial Maria 2.0 neural H.265 talking-head (or living idle) clip
 * with ZERO personal names ("Rodolfo") and ZERO brand hacks ("Mediatório").
 * Designed to run both locally and in parallel GitHub Actions matrix runners (each with fresh ZeroGPU IP).
 *
 * Usage:
 *   node scripts/render-single-moda-clip.mjs --target idle --out docs/cdn/maria2/idle.mp4
 *   node scripts/render-single-moda-clip.mjs --target pt/clean --out docs/cdn/maria2/pt/clean.mp4
 *   node scripts/render-single-moda-clip.mjs --regen-local-mp3s
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const PORTRAIT_PNG = path.join(projectRoot, ".agents", "avatar", "antigravity-avatar.png");
const FFMPEG_WIN = "C:\\Users\\rodolfo\\tools\\ffmpeg\\bin\\ffmpeg.exe";
const FFMPEG = process.platform === "win32" && fs.existsSync(FFMPEG_WIN) ? FFMPEG_WIN : "ffmpeg";

const MODA_HOST = "https://multimodalart-moda-fast-talking-head.hf.space";

// 100% Clean Global Commercial Speeches (ZERO "Rodolfo", ZERO "Mediatório", ZERO speed claims)
export const MARIA2_SPEECHES = {
  pt: {
    voice: "pt-BR-FranciscaNeural",
    clips: {
      clean: "Olá! Eu sou a Maria, do Mídia Store. Já separei itens repetidos e vídeos pesados para você liberar espaço em dois toques.",
      files: "Aqui estão suas fotos, vídeos, áudios e downloads organizados dos maiores para os menores.",
      pro: "No Mídia Store Pro você testa três dias grátis no Plano Anual, comprime vídeos pesados na GPU e encontra aplicativos sem uso.",
      dup: "Separei as cópias idênticas verificadas por código exato, mantendo seu arquivo original sempre protegido.",
      videos: "Estes são os vídeos grandes que mais ocupam espaço. Escolha quais deseja limpar ou comprimir sem perder nitidez.",
      whatsapp: "Aqui estão as mídias e downloads acumulados de conversas prontos para revisar e limpar agora.",
      screenshots: "Reuni as capturas de tela antigas com mais de trinta dias para liberar espaço rápido.",
      nano: "A análise inteligente de fotos parecidas e fotos borradas roda cem por cento no seu aparelho, mantendo a melhor foto protegida.",
    },
  },
  en: {
    voice: "en-US-AvaMultilingualNeural",
    clips: {
      clean: "Hi! I am Maria from Midiastore. Your duplicate files and heavy videos are ready to clean safely in two taps.",
      files: "Your photos, videos, audio, and downloads are sorted from largest to smallest.",
      pro: "Try Midiastore Pro free for three days on the Annual Plan to unlock GPU video compression and unused app detection.",
    },
  },
  es: {
    voice: "es-MX-DaliaNeural",
    clips: {
      clean: "¡Hola! Soy Maria de Midiastore. Tus archivos duplicados y videos pesados están listos para limpiar en dos toques.",
    },
  },
  fr: {
    voice: "fr-FR-VivienneMultilingualNeural",
    clips: {
      clean: "Bonjour ! Je suis Maria de Midiastore. Vos doublons et grandes vidéos sont prêts à être nettoyés en deux gestes.",
    },
  },
  de: {
    voice: "de-DE-SeraphinaMultilingualNeural",
    clips: {
      clean: "Hallo! Ich bin Maria von Midiastore. Duplikate und große Videos sind mit zwei Fingertipps sicher bereit zur Bereinigung.",
    },
  },
  it: {
    voice: "it-IT-IsabellaNeural",
    clips: {
      clean: "Ciao! Sono Maria di Midiastore. File duplicati e video pesanti sono pronti per liberare spazio in due tocchi.",
    },
  },
  ru: {
    voice: "ru-RU-SvetlanaNeural",
    clips: {
      clean: "Привет! Я Мария из Midiastore. Дубликаты и большие видео готовы к безопасной очистке в два касания.",
    },
  },
  zh: {
    voice: "zh-CN-XiaoxiaoNeural",
    clips: {
      clean: "您好！我是 Midiastore 的 Maria。重复文件和大视频已准备就绪，两步即可安全释放空间。",
    },
  },
  ja: {
    voice: "ja-JP-NanamiNeural",
    clips: {
      clean: "こんにちは！MidiastoreのMariaです。重複ファイルや大容量動画を2タップで安全に整理できます。",
    },
  },
  ko: {
    voice: "ko-KR-SunHiNeural",
    clips: {
      clean: "안녕하세요! Midiastore의 Maria입니다. 중복 파일과 대용량 동영상을 두 번의 탭으로 안전하게 정리해 보세요.",
    },
  },
  ar: {
    voice: "ar-SA-ZariyahNeural",
    clips: {
      clean: "مرحباً! أنا ماريا من Midiastore. الملفات المكررة والفيديوهات الكبيرة جاهزة للتنظيف بأمان بلمستين فقط.",
    },
  },
  hi: {
    voice: "hi-IN-SwaraNeural",
    clips: {
      clean: "नमस्ते! मैं Midiastore से मारिया हूँ। डुप्लिकेट फ़ाइलें और बड़े वीडियो बस दो टैप में सुरक्षित सफ़ाई के लिए तैयार हैं।",
    },
  },
  bn: {
    voice: "bn-BD-NabanitaNeural",
    clips: {
      clean: "হ্যালো! আমি Midiastore থেকে মারিয়া। ডুপ্লিকেট ফাইল এবং বড় ভিডিও মাত্র দুই ট্যাপে নিরাপদে পরিষ্কার করার জন্য প্রস্তুত।",
    },
  },
  id: {
    voice: "id-ID-GadisNeural",
    clips: {
      clean: "Halo! Saya Maria dari Midiastore. File duplikat dan video besar siap dibersihkan dengan aman dalam dua ketukan.",
    },
  },
};

async function importMsEdgeTts() {
  const localMod = path.join(projectRoot, "scripts", "node_modules", "msedge-tts", "dist", "index.js");
  if (fs.existsSync(localMod)) {
    return import(pathToFileURL(localMod).href);
  }
  return import("msedge-tts");
}

async function synthMp3(voice, text, outMp3) {
  if (/rodolfo|mediat[oó]rio/i.test(text)) {
    throw new Error(`Forbidden personal/gambiarra token detected in speech text: ${text}`);
  }
  const { MsEdgeTTS, OUTPUT_FORMAT } = await importMsEdgeTts();
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const tts = new MsEdgeTTS();
      await tts.setMetadata(voice, OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3);
      const { audioStream } = tts.toStream(text);
      const chunks = [];
      for await (const c of audioStream) chunks.push(c);
      const buf = Buffer.concat(chunks);
      if (buf.length < 1024) throw new Error(`Short TTS buffer: ${buf.length}`);
      fs.mkdirSync(path.dirname(outMp3), { recursive: true });
      fs.writeFileSync(outMp3, buf);
      return buf.length;
    } catch (e) {
      if (attempt === 3) throw e;
      await new Promise((r) => setTimeout(r, 1200 * attempt));
    }
  }
}

async function gradioUpload(baseUrl, localFilePath, mimeType) {
  const buf = fs.readFileSync(localFilePath);
  const form = new FormData();
  form.append("files", new Blob([buf], { type: mimeType }), path.basename(localFilePath));
  const r = await fetch(`${baseUrl}/gradio_api/upload`, {
    method: "POST",
    body: form,
    signal: AbortSignal.timeout(35000),
  });
  if (!r.ok) throw new Error(`Gradio upload HTTP ${r.status}`);
  const arr = await r.json();
  return {
    path: arr[0],
    orig_name: path.basename(localFilePath),
    mime_type: mimeType,
    meta: { _type: "gradio.FileData" },
  };
}

async function callModaQueue(audioWavPath, emotion = "None", cfgScale = 1.2) {
  const imgFile = await gradioUpload(MODA_HOST, PORTRAIT_PNG, "image/png");
  const audFile = await gradioUpload(MODA_HOST, audioWavPath, "audio/wav");
  const sessionHash = "m2" + Math.random().toString(36).slice(2, 11);

  const joinRes = await fetch(`${MODA_HOST}/gradio_api/queue/join`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      data: [imgFile, audFile, emotion, cfgScale],
      fn_index: 0,
      session_hash: sessionHash,
    }),
    signal: AbortSignal.timeout(35000),
  });
  if (!joinRes.ok) throw new Error(`MoDA queue/join HTTP ${joinRes.status}`);

  const dataRes = await fetch(`${MODA_HOST}/gradio_api/queue/data?session_hash=${sessionHash}`, {
    signal: AbortSignal.timeout(160000),
  });
  const sseText = await dataRes.text();
  let videoUrl = null;
  for (const line of sseText.split(/\r?\n/)) {
    if (!line.startsWith("data:")) continue;
    const payload = JSON.parse(line.slice(5).trim());
    if (payload.msg === "process_completed") {
      if (!payload.success) {
        throw new Error(payload?.output?.error || payload?.title || "MoDA process failed");
      }
      const first = payload?.output?.data?.[0];
      const fileObj = first?.video || first;
      videoUrl = fileObj?.url || (fileObj?.path ? `${MODA_HOST}/gradio_api/file=${fileObj.path}` : null);
    }
  }
  if (!videoUrl) throw new Error("No video URL returned from MoDA queue");
  const dl = await fetch(videoUrl, { signal: AbortSignal.timeout(60000) });
  if (!dl.ok) throw new Error(`Video download HTTP ${dl.status}`);
  return Buffer.from(await dl.arrayBuffer());
}

async function regenerateLocalBundledMp3s() {
  const rawDir = path.join(projectRoot, "app", "src", "main", "res", "raw");
  const ptSpec = MARIA2_SPEECHES.pt;
  const map = {
    clean: "maria_clean.mp3",
    files: "maria_files.mp3",
    pro: "maria_pro.mp3",
    dup: "maria_dup.mp3",
    videos: "maria_videos.mp3",
    whatsapp: "maria_whatsapp.mp3",
    screenshots: "maria_screenshots.mp3",
    nano: "maria_nano.mp3",
  };
  let totalBytes = 0;
  for (const [key, filename] of Object.entries(map)) {
    const out = path.join(rawDir, filename);
    const sz = await synthMp3(ptSpec.voice, ptSpec.clips[key], out);
    totalBytes += sz;
    console.log(`✅ Regenerated clean ${filename}: ${(sz / 1024).toFixed(1)} KB`);
  }
  console.log(`🎉 All 8 bundled PT-BR MP3s cleaned! Total size: ${(totalBytes / 1024).toFixed(1)} KB (zero Rodolfo, zero Mediatório)`);
}

async function renderTargetClip(target, outPath) {
  const workDir = fs.mkdtempSync(path.join(os.tmpdir(), "maria2-clip-"));
  const wavPath = path.join(workDir, "input.wav");
  const rawMp4 = path.join(workDir, "raw.mp4");
  fs.mkdirSync(path.dirname(outPath), { recursive: true });

  if (target === "idle") {
    // Generate 3.6s pure silence 16kHz WAV -> MoDA generates natural human blinking, breathing & subtle head motion!
    const sil = spawnSync(
      FFMPEG,
      ["-y", "-f", "lavfi", "-i", "anullsrc=r=16000:cl=mono", "-t", "3.6", "-c:a", "pcm_s16le", wavPath],
      { stdio: "ignore", windowsHide: true }
    );
    if (sil.status !== 0) throw new Error("Failed to generate silent WAV for idle");
    const vidBuf = await callModaQueue(wavPath, "Happiness", 1.1);
    fs.writeFileSync(rawMp4, vidBuf);
    // Seamless ping-pong loop (forward + reverse = 0 jump at loop boundary!) in 448x448 H.265 hvc1!
    const enc = spawnSync(
      FFMPEG,
      [
        "-y",
        "-i", rawMp4,
        "-filter_complex", "[0:v]scale=448:448,fps=24,format=yuv420p,split[f][r];[r]reverse[rr];[f][rr]concat=n=2:v=1:a=0[v]",
        "-map", "[v]",
        "-c:v", "libx265",
        "-crf", "28",
        "-preset", "medium",
        "-tag:v", "hvc1",
        "-movflags", "+faststart",
        "-an",
        outPath,
      ],
      { stdio: "ignore", windowsHide: true }
    );
    if (enc.status !== 0) throw new Error("FFmpeg ping-pong H.265 idle encode failed");
    console.log(`✅ Rendered living neural idle H.265 (${target}) -> ${outPath} (${(fs.statSync(outPath).size / 1024).toFixed(1)} KB)`);
    return;
  }

  const [lang, clipKey] = target.split("/");
  const spec = MARIA2_SPEECHES[lang];
  const text = spec?.clips?.[clipKey];
  if (!spec || !text) throw new Error(`Unknown target: ${target}`);

  const mp3Path = path.join(workDir, "speech.mp3");
  await synthMp3(spec.voice, text, mp3Path);
  const wRes = spawnSync(
    FFMPEG,
    ["-y", "-i", mp3Path, "-ar", "16000", "-ac", "1", "-c:a", "pcm_s16le", wavPath],
    { stdio: "ignore", windowsHide: true }
  );
  if (wRes.status !== 0) throw new Error("FFmpeg WAV decode failed");

  const vidBuf = await callModaQueue(wavPath, "None", 1.2);
  fs.writeFileSync(rawMp4, vidBuf);

  const enc = spawnSync(
    FFMPEG,
    [
      "-y",
      "-i", rawMp4,
      "-i", mp3Path,
      "-map", "0:v:0",
      "-map", "1:a:0",
      "-vf", "scale=480:480,format=yuv420p",
      "-c:v", "libx265",
      "-crf", "27",
      "-preset", "medium",
      "-tag:v", "hvc1",
      "-movflags", "+faststart",
      "-c:a", "aac",
      "-b:a", "56k",
      "-shortest",
      outPath,
    ],
    { stdio: "ignore", windowsHide: true }
  );
  if (enc.status !== 0) throw new Error(`FFmpeg H.265 encode failed for ${target}`);
  console.log(`✅ Rendered neural talking H.265 [${target}] -> ${outPath} (${(fs.statSync(outPath).size / 1024).toFixed(1)} KB)`);
}

async function main() {
  const args = process.argv.slice(2);
  if (args.includes("--regen-local-mp3s")) {
    await regenerateLocalBundledMp3s();
    return;
  }
  const targetIdx = args.indexOf("--target");
  const outIdx = args.indexOf("--out");
  if (targetIdx < 0 || outIdx < 0) {
    throw new Error("Usage: --target <idle|lang/clip> --out <path> | --regen-local-mp3s");
  }
  await renderTargetClip(args[targetIdx + 1], path.resolve(args[outIdx + 1]));
}

main().catch((err) => {
  console.error(`❌ [render-single-moda-clip] ${err?.message || err}`);
  process.exit(1);
});
