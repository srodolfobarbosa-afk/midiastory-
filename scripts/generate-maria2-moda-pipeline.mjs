#!/usr/bin/env node
/**
 * scripts/generate-maria2-moda-pipeline.mjs
 *
 * Official Maria 2.0 Photorealistic Neural Lip-Sync Pipeline (MoDA Diffusion + H.265/HEVC CDN).
 * - 100% Node.js + MsEdgeTTS (24kHz studio neural audio) + Gradio 5 MoDA API + FFmpeg libx265 (hvc1).
 * - Zero 3D / Zero 2D mouth-frame gambiarra (Strictly enforcesAGENTS.md Directive 12).
 * - Generates ~140-240 KB 512x512 H.265 micro-videos per clip into docs/cdn/maria2/<lang>/<clip>.mp4
 *   and updates docs/cdn/maria2/manifest.json with verified SHA-256 & byte sizes.
 * - Also builds app/src/main/res/raw/maria2_idle.mp4 (~26 KB) for instant 0ms offline startup.
 *
 * Usage:
 *   node scripts/generate-maria2-moda-pipeline.mjs                  # Default: idle + seed + render priority PT/EN clips
 *   node scripts/generate-maria2-moda-pipeline.mjs --locales pt,en  # Render specific locales
 *   node scripts/generate-maria2-moda-pipeline.mjs --all            # Incremental render across all 14 locales
 *   node scripts/generate-maria2-moda-pipeline.mjs --manifest-only  # Rebuild manifest.json from existing MP4s
 */

import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath, pathToFileURL } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const CDN_DIR = path.join(projectRoot, "docs", "cdn", "maria2");
const RAW_ANDROID_IDLE = path.join(projectRoot, "app", "src", "main", "res", "raw", "maria2_idle.mp4");
const PROTOTYPE_DIR = path.join(projectRoot, ".tmp", "prototipo-maria-2-humana");
const WORK_DIR = path.join(os.tmpdir(), "midiastore-maria2-moda-work");
const MASTER_PORTRAIT = fs.existsSync(path.join(PROTOTYPE_DIR, "maria2_sony_cinema_master.png"))
  ? path.join(PROTOTYPE_DIR, "maria2_sony_cinema_master.png")
  : path.join(projectRoot, ".agents", "avatar", "antigravity-avatar.png");

const FFMPEG_CANDIDATES = [
  "C:\\Users\\rodolfo\\tools\\ffmpeg\\bin\\ffmpeg.exe",
  "ffmpeg",
];
const FFMPEG = FFMPEG_CANDIDATES.find((bin) => {
  if (bin === "ffmpeg") return true;
  return fs.existsSync(bin);
});

const MODA_HOST = "https://multimodalart-moda-fast-talking-head.hf.space";

export const LOCALE_VOICES = {
  pt: {
    voice: "pt-BR-FranciscaNeural",
    clips: {
      clean: "Olá! Eu sou a Maria! Já analisei seu celular para liberar espaço com total segurança.",
      files: "Suas fotos, vídeos, áudios e downloads estão organizados dos maiores para os menores.",
      pro: "Destrave o teste grátis no Plano Anual, compressor de vídeo na GPU e fotos parecidas com melhor foto protegida!",
      dup: "Encontrei cópias idênticas verificadas por hash SHA duzentos e cinquenta e seis, mantendo sempre o original protegido.",
      videos: "Estes são os vídeos mais pesados do seu celular. Você pode revisar ou comprimir na GPU sem perder qualidade.",
      whatsapp: "Aqui estão as mídias e downloads acumulados de conversas e redes sociais prontos para revisão.",
      screenshots: "Separei as capturas de tela antigas com mais de trinta dias para você ganhar espaço rápido.",
      nano: "A análise inteligente de nitidez e fotos parecidas roda cem por cento no seu aparelho, com privacidade total.",
    },
  },
  en: {
    voice: "en-US-AvaMultilingualNeural",
    clips: {
      clean: "Hi! I am Maria. I analyzed your phone storage so you can free up space safely.",
      files: "Your photos, videos, audio, and downloads are sorted from largest to smallest.",
      pro: "Start your free trial on the Annual Plan to unlock GPU video compression, unused apps, and audited reports!",
      dup: "Exact duplicate files verified by SHA two fifty-six, keeping your original safely protected.",
      videos: "Here are the largest videos on your device. Review them or shrink them locally on GPU.",
      whatsapp: "Media and downloads from your chats and social apps ready for quick review.",
      screenshots: "Old screenshots over thirty days ready to review and clean in one tap.",
      nano: "Smart blurry and similar photo detection runs one hundred percent privately on your device.",
    },
  },
  es: {
    voice: "es-MX-DaliaNeural",
    clips: {
      clean: "¡Hola! Soy Maria. Analicé tu teléfono para liberar espacio con total seguridad.",
      files: "Tus fotos, videos, audios y descargas ordenados de mayor a menor tamaño.",
      pro: "¡Prueba gratis el Plan Anual y desbloquea compresión H dos seis cinco, apps sin uso y fotos similares!",
    },
  },
  de: {
    voice: "de-DE-SeraphinaMultilingualNeural",
    clips: {
      clean: "Hallo! Ich bin Maria. Ihr Gerätespeicher wurde sicher zur Bereinigung analysiert.",
      files: "Ihre Fotos, Videos, Audios und Downloads sind nach Größe sortiert.",
      pro: "Testen Sie das Jahresabo kostenlos für lokale Videokomprimierung und unbenutzte Apps!",
    },
  },
  fr: {
    voice: "fr-FR-VivienneMultilingualNeural",
    clips: {
      clean: "Bonjour ! Je suis Maria. J'ai analysé votre téléphone pour libérer de l'espace en toute sécurité.",
      files: "Vos photos, vidéos, audios et téléchargements triés du plus grand au plus petit.",
      pro: "Essayez gratuitement l'abonnement Annuel pour débloquer la compression vidéo et les apps inutilisées !",
    },
  },
  hi: {
    voice: "hi-IN-SwaraNeural",
    clips: {
      clean: "नमस्ते! मैं मारिया हूँ। मैंने सुरक्षित रूप से स्पेस खाली करने के लिए आपके फ़ोन का विश्लेषण किया है।",
      files: "आपकी फ़ोटो, वीडियो और डाउनलोड आकार के अनुसार व्यवस्थित हैं।",
      pro: "वार्षिक प्लान पर मुफ़्त ट्रायल शुरू करें और वीडियो कंप्रेसर व प्रो टूल्स अनलॉक करें!",
    },
  },
  ja: {
    voice: "ja-JP-NanamiNeural",
    clips: {
      clean: "こんにちは！Mariaです。スマートフォンの空き容量を安全に増やす準備ができました。",
      files: "写真、動画、音声、ダウンロードをサイズ順に整理しました。",
      pro: "年間プランの無料トライアルで、高画質動画圧縮と未使用アプリ診断を解放しましょう！",
    },
  },
  zh: {
    voice: "zh-CN-XiaoxiaoNeural",
    clips: {
      clean: "您好！我是 Maria。已为您安全分析手机存储空间，随时可以清理。",
      files: "您的照片、视频、音频和下载已按从大到小排序。",
      pro: "开启年度计划免费试用，解锁本地视频压缩与闲置应用检测！",
    },
  },
  ar: {
    voice: "ar-SA-ZariyahNeural",
    clips: {
      clean: "مرحباً! أنا ماريا. قمت بتحليل ذاكرة هاتفك لتحرير المساحة بأمان تام.",
      files: "صورك وفيديوهاتك وتنزيلاتك مرتبة من الأكبر إلى الأصغر حجماً.",
      pro: "ابدأ التجربة المجانية للخطة السنوية لفتح ضغط الفيديو والتطبيقات غير المستخدمة!",
    },
  },
  ru: {
    voice: "ru-RU-SvetlanaNeural",
    clips: {
      clean: "Привет! Я Мария. Память вашего телефона проверена для безопасной очистки.",
      files: "Ваши фото, видео, аудио и загрузки отсортированы от больших к меньшим.",
      pro: "Попробуйте годовой план бесплатно: сжатие видео и поиск неиспользуемых приложений!",
    },
  },
  id: {
    voice: "id-ID-GadisNeural",
    clips: {
      clean: "Halo! Saya Maria. Penyimpanan ponsel Anda telah dianalisis untuk pembersihan yang aman.",
      files: "Foto, video, audio, dan unduhan Anda diurutkan dari ukuran terbesar.",
      pro: "Coba gratis Paket Tahunan untuk membuka kompresor video dan deteksi aplikasi tak terpakai!",
    },
  },
  bn: {
    voice: "bn-BD-NabanitaNeural",
    clips: {
      clean: "হ্যালো! আমি মারিয়া। নিরাপদে জায়গা খালি করার জন্য আপনার ফোনের স্টোরেজ বিশ্লেষণ করেছি।",
      files: "আপনার ছবি, ভিডিও, অডিও এবং ডাউনলোড আকার অনুযায়ী সাজানো হয়েছে।",
      pro: "বার্ষিক প্ল্যানে ফ্রি ট্রায়াল শুরু করুন এবং ভিডিও কমপ্রেসর আনলক করুন!",
    },
  },
  ko: {
    voice: "ko-KR-SunHiNeural",
    clips: {
      clean: "안녕하세요! Maria입니다. 안전하게 저장 공간을 확보할 수 있도록 분석을 마쳤습니다.",
      files: "사진, 동영상, 오디오 및 다운로드가 크기순으로 정리되었습니다.",
      pro: "연간 플랜 무료 체험으로 동영상 압축기와 미사용 앱 정리를 이용해 보세요!",
    },
  },
  it: {
    voice: "it-IT-IsabellaNeural",
    clips: {
      clean: "Ciao! Sono Maria. Ho analizzato la memoria del tuo telefono per liberare spazio in sicurezza.",
      files: "Foto, video, audio e download ordinati dal più grande al più piccolo.",
      pro: "Prova gratis il Piano Annuale per sbloccare compressione video e app inutilizzate!",
    },
  },
};

function sha256File(filePath) {
  return crypto.createHash("sha256").update(fs.readFileSync(filePath)).digest("hex");
}

async function synthStudioAudio(voice, text, outMp3, outWav) {
  const msedgeModuleUrl = pathToFileURL(
    path.join(projectRoot, "scripts", "node_modules", "msedge-tts", "dist", "index.js")
  ).href;
  const { MsEdgeTTS, OUTPUT_FORMAT } = await import(msedgeModuleUrl);
  const tts = new MsEdgeTTS();
  await tts.setMetadata(voice, OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3);
  const { audioStream } = tts.toStream(text);
  const chunks = [];
  for await (const c of audioStream) chunks.push(c);
  const buf = Buffer.concat(chunks);
  if (buf.length < 1024) throw new Error(`TTS returned empty buffer (${buf.length}B)`);
  fs.writeFileSync(outMp3, buf);

  const wavRes = spawnSync(
    FFMPEG,
    ["-y", "-i", outMp3, "-ar", "16000", "-ac", "1", "-c:a", "pcm_s16le", outWav],
    { stdio: "ignore", windowsHide: true }
  );
  if (wavRes.status !== 0 || !fs.existsSync(outWav)) {
    throw new Error("FFmpeg MP3 -> WAV conversion failed");
  }
}

async function gradioUpload(baseUrl, localFilePath, mimeType) {
  const buf = fs.readFileSync(localFilePath);
  const form = new FormData();
  form.append("files", new Blob([buf], { type: mimeType }), path.basename(localFilePath));
  const r = await fetch(`${baseUrl}/gradio_api/upload`, {
    method: "POST",
    body: form,
    signal: AbortSignal.timeout(30000),
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

async function renderModaTalkingHead(audioWavPath, outRawMp4) {
  const imgFile = await gradioUpload(MODA_HOST, MASTER_PORTRAIT, "image/png");
  const audFile = await gradioUpload(MODA_HOST, audioWavPath, "audio/wav");
  const callRes = await fetch(`${MODA_HOST}/gradio_api/call/generate_motion`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ data: [imgFile, audFile, "None", 1.2] }),
    signal: AbortSignal.timeout(30000),
  });
  if (!callRes.ok) throw new Error(`MoDA call HTTP ${callRes.status}`);
  const { event_id: eventId } = await callRes.json();

  const sseRes = await fetch(`${MODA_HOST}/gradio_api/call/generate_motion/${eventId}`, {
    signal: AbortSignal.timeout(120000),
  });
  const sseText = await sseRes.text();
  let lastEvent = "";
  let completeData = null;
  for (const line of sseText.split(/\r?\n/)) {
    if (line.startsWith("event:")) lastEvent = line.slice(6).trim();
    else if (line.startsWith("data:")) {
      const rawJson = line.slice(5).trim();
      if (lastEvent === "error") throw new Error(`MoDA SSE error: ${rawJson}`);
      if (lastEvent === "complete") completeData = JSON.parse(rawJson);
    }
  }
  const first = completeData?.[0];
  const fileObj = first?.video || first;
  const downloadUrl =
    fileObj?.url || (fileObj?.path ? `${MODA_HOST}/gradio_api/file=${fileObj.path}` : null);
  if (!downloadUrl) throw new Error("MoDA response did not contain video URL");

  const vidRes = await fetch(downloadUrl, { signal: AbortSignal.timeout(60000) });
  if (!vidRes.ok) throw new Error(`MoDA download HTTP ${vidRes.status}`);
  const vidBuf = Buffer.from(await vidRes.arrayBuffer());
  if (vidBuf.length < 20_000) throw new Error(`MoDA video unexpectedly small (${vidBuf.length}B)`);
  fs.writeFileSync(outRawMp4, vidBuf);
}

function encodeAndroidHevcClip(rawVideoMp4, studioMp3, outHevcMp4) {
  fs.mkdirSync(path.dirname(outHevcMp4), { recursive: true });
  const res = spawnSync(
    FFMPEG,
    [
      "-y",
      "-i", rawVideoMp4,
      "-i", studioMp3,
      "-map", "0:v:0",
      "-map", "1:a:0",
      "-vf", "scale=512:512,format=yuv420p",
      "-c:v", "libx265",
      "-crf", "27",
      "-preset", "medium",
      "-tag:v", "hvc1",
      "-movflags", "+faststart",
      "-c:a", "aac",
      "-b:a", "64k",
      "-shortest",
      outHevcMp4,
    ],
    { stdio: "ignore", windowsHide: true }
  );
  if (res.status !== 0 || !fs.existsSync(outHevcMp4)) {
    throw new Error(`FFmpeg H.265 remux failed for ${outHevcMp4}`);
  }
}

function ensureIdleHevcVideos() {
  fs.mkdirSync(CDN_DIR, { recursive: true });
  fs.mkdirSync(path.dirname(RAW_ANDROID_IDLE), { recursive: true });
  const cdnIdle = path.join(CDN_DIR, "idle.mp4");
  const protoIdle = path.join(PROTOTYPE_DIR, "maria2_idle_android_hevc.mp4");

  if (fs.existsSync(protoIdle) && fs.statSync(protoIdle).size > 5000) {
    fs.copyFileSync(protoIdle, cdnIdle);
    fs.copyFileSync(protoIdle, RAW_ANDROID_IDLE);
    console.log(`✅ Seeded idle H.265 video (${(fs.statSync(cdnIdle).size / 1024).toFixed(1)} KB) -> res/raw/maria2_idle.mp4 & CDN`);
    return;
  }

  const res = spawnSync(
    FFMPEG,
    [
      "-y",
      "-loop", "1",
      "-i", MASTER_PORTRAIT,
      "-t", "4",
      "-vf", "scale=560:560,zoompan=z='1.015+0.012*sin(2*PI*on/96)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=512x512:fps=24,format=yuv420p",
      "-c:v", "libx265",
      "-crf", "28",
      "-preset", "medium",
      "-tag:v", "hvc1",
      "-movflags", "+faststart",
      "-an",
      cdnIdle,
    ],
    { stdio: "ignore", windowsHide: true }
  );
  if (res.status !== 0) throw new Error("Failed to encode idle.mp4");
  fs.copyFileSync(cdnIdle, RAW_ANDROID_IDLE);
  console.log(`✅ Encoded idle H.265 video (${(fs.statSync(cdnIdle).size / 1024).toFixed(1)} KB)`);
}

function seedApprovedPrototypeClip() {
  const protoPtHevc = path.join(PROTOTYPE_DIR, "maria2_neural_pt_android_hevc.mp4");
  const protoPtRaw = path.join(PROTOTYPE_DIR, "maria2_neural_pt_raw.mp4");
  const protoPtMp3 = path.join(PROTOTYPE_DIR, "maria2_talk_pt.mp3");
  const targetPtClean = path.join(CDN_DIR, "pt", "clean.mp4");

  if (fs.existsSync(targetPtClean) && fs.statSync(targetPtClean).size > 20_000) {
    return;
  }
  fs.mkdirSync(path.dirname(targetPtClean), { recursive: true });
  if (fs.existsSync(protoPtRaw) && fs.existsSync(protoPtMp3)) {
    encodeAndroidHevcClip(protoPtRaw, protoPtMp3, targetPtClean);
    console.log(`✅ Seeded faststart approved PT neural clip -> docs/cdn/maria2/pt/clean.mp4 (${(fs.statSync(targetPtClean).size / 1024).toFixed(1)} KB)`);
  } else if (fs.existsSync(protoPtHevc)) {
    fs.copyFileSync(protoPtHevc, targetPtClean);
    console.log(`✅ Copied approved PT neural clip -> docs/cdn/maria2/pt/clean.mp4`);
  }
}

function buildCdnManifest() {
  const clips = {};
  if (fs.existsSync(CDN_DIR)) {
    for (const entry of fs.readdirSync(CDN_DIR, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const lang = entry.name;
      const langDir = path.join(CDN_DIR, lang);
      for (const file of fs.readdirSync(langDir)) {
        if (!file.endsWith(".mp4")) continue;
        const clipKey = path.basename(file, ".mp4");
        const full = path.join(langDir, file);
        const bytes = fs.statSync(full).size;
        if (bytes < 10_000) continue;
        if (!clips[lang]) clips[lang] = {};
        clips[lang][clipKey] = {
          url: `https://midiastory-df6a0.web.app/cdn/maria2/${lang}/${file}`,
          bytes,
          sha256: sha256File(full),
        };
      }
    }
  }
  const idlePath = path.join(CDN_DIR, "idle.mp4");
  const manifest = {
    version: "2.9.1-moda-hevc",
    updatedAt: new Date().toISOString(),
    codec: "hvc1",
    resolution: "512x512",
    idle: fs.existsSync(idlePath)
      ? {
          url: "https://midiastory-df6a0.web.app/cdn/maria2/idle.mp4",
          bytes: fs.statSync(idlePath).size,
          sha256: sha256File(idlePath),
        }
      : null,
    locales: clips,
  };
  const manifestPath = path.join(CDN_DIR, "manifest.json");
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2) + "\n", "utf8");
  return manifest;
}

async function main() {
  const args = process.argv.slice(2);
  const manifestOnly = args.includes("--manifest-only");
  const force = args.includes("--force");
  const renderAll = args.includes("--all");
  const localesArg = args.find((a) => a.startsWith("--locales="))?.split("=")[1]
    || (args.indexOf("--locales") >= 0 ? args[args.indexOf("--locales") + 1] : null);
  const clipsArg = args.find((a) => a.startsWith("--clips="))?.split("=")[1]
    || (args.indexOf("--clips") >= 0 ? args[args.indexOf("--clips") + 1] : null);

  fs.mkdirSync(WORK_DIR, { recursive: true });
  ensureIdleHevcVideos();
  seedApprovedPrototypeClip();

  if (!manifestOnly) {
    const targetLocales = renderAll
      ? Object.keys(LOCALE_VOICES)
      : (localesArg ? localesArg.split(",").map((s) => s.trim()) : ["pt", "en"]);
    const filterClips = clipsArg ? new Set(clipsArg.split(",").map((s) => s.trim())) : null;

    let renderedCount = 0;
    let quotaStopped = false;

    for (const lang of targetLocales) {
      if (quotaStopped) break;
      const spec = LOCALE_VOICES[lang];
      if (!spec) continue;

      for (const [clipKey, text] of Object.entries(spec.clips)) {
        if (filterClips && !filterClips.has(clipKey)) continue;
        const outHevc = path.join(CDN_DIR, lang, `${clipKey}.mp4`);
        if (!force && fs.existsSync(outHevc) && fs.statSync(outHevc).size > 20_000) {
          console.log(`⏭️  [SKIP] ${lang}/${clipKey}.mp4 already exists (${(fs.statSync(outHevc).size / 1024).toFixed(1)} KB)`);
          continue;
        }

        const mp3Path = path.join(WORK_DIR, `${lang}_${clipKey}.mp3`);
        const wavPath = path.join(WORK_DIR, `${lang}_${clipKey}.wav`);
        const rawMp4Path = path.join(WORK_DIR, `${lang}_${clipKey}_raw.mp4`);

        try {
          console.log(`🎙️  [${lang}/${clipKey}] Synthesizing neural studio voice (${spec.voice})...`);
          await synthStudioAudio(spec.voice, text, mp3Path, wavPath);
          if (renderedCount > 0) {
            await new Promise((r) => setTimeout(r, 2500));
          }
          console.log(`🎬 [${lang}/${clipKey}] Rendering MoDA Diffusion lip-sync...`);
          await renderModaTalkingHead(wavPath, rawMp4Path);
          encodeAndroidHevcClip(rawMp4Path, mp3Path, outHevc);
          renderedCount++;
          console.log(`✅ [${lang}/${clipKey}] Saved H.265 clip: ${(fs.statSync(outHevc).size / 1024).toFixed(1)} KB`);
        } catch (err) {
          const msg = String(err?.message || err);
          console.warn(`⚠️  [${lang}/${clipKey}] Skipped (${msg})`);
          if (/quota|ZeroGPU|exceeded|429/i.test(msg)) {
            console.warn("⏸️  ZeroGPU window quota reached — preserving all rendered clips and writing CDN manifest.");
            quotaStopped = true;
            break;
          }
        }
      }
    }
  }

  const manifest = buildCdnManifest();
  const totalClips = Object.values(manifest.locales).reduce((acc, m) => acc + Object.keys(m).length, 0);
  console.log(`\n📦 Maria 2.0 CDN Manifest updated: ${totalClips} verified neural H.265 talking clip(s) + idle.mp4`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
