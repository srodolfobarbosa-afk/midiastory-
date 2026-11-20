#!/usr/bin/env node
/**
 * CAPTURA DE DEMO REAL NO MOTO G84 VIA ADB + RENDERIZAÇÃO REELS/TIKTOK/SHORTS (FFmpeg)
 *
 * 1. Abre o Midiastore no dispositivo Android via ADB (sem apagar nenhum arquivo).
 * 2. Grava 14s de tela real (1080x2400) navegando pelas 3 abas OLED (Limpar -> Arquivos -> Pro -> Limpar).
 * 3. Puxa o MP4 bruto para marketing/videos/raw/ e remove o temporário do celular.
 * 4. Renderiza 2 vídeos verticais 1080x1920 (PT-BR e EN-US) prontos para Reels/TikTok/Shorts com FFmpeg.
 */

import fs from "node:fs";
import path from "node:path";
import { execFileSync, spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const SERIAL = process.env.ADB_SERIAL || "0085353748";
const FFMPEG = fs.existsSync("C:\\Users\\rodolfo\\tools\\ffmpeg\\bin\\ffmpeg.exe")
  ? "C:\\Users\\rodolfo\\tools\\ffmpeg\\bin\\ffmpeg.exe"
  : "ffmpeg";
const TODAY = new Date().toISOString().slice(0, 10);

const rawDir = path.join(projectRoot, "marketing", "videos", "raw");
const outDir = path.join(projectRoot, "marketing", "videos");
fs.mkdirSync(rawDir, { recursive: true });
fs.mkdirSync(outDir, { recursive: true });

const remotePath = "/sdcard/Download/midiastore_demo_raw.mp4";
const localRaw = path.join(rawDir, `midiastore-raw-${TODAY}.mp4`);

function adb(args) {
  return execFileSync("adb", ["-s", SERIAL, ...args], { encoding: "utf8", timeout: 20000 }).trim();
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function main() {
  const renderOnly = process.argv.includes("--render-only");

  if (!renderOnly) {
    // 1. Launch Midiastore cleanly
    adb(["shell", "monkey", "-p", "com.rdf112.midiastore", "-c", "android.intent.category.LAUNCHER", "1"]);
    await sleep(1800);

    // 2. Start 14s screenrecord in background
    console.log("🎥 Gravando 14s de demo real no Moto G84...");
    const rec = spawn("adb", [
      "-s", SERIAL, "shell", "screenrecord",
      "--bit-rate", "8000000",
      "--time-limit", "14",
      remotePath,
    ]);

    // Navigate gently across the 3 bottom tabs (1080x2400 -> bottom nav around y=2270)
    // Clean (~180, 2270), Files (~540, 2270), Pro (~900, 2270)
    await sleep(3000);
    adb(["shell", "input", "swipe", "540", "1400", "540", "850", "350"]); // gentle scroll on Home
    await sleep(2200);
    adb(["shell", "input", "tap", "540", "2270"]); // Tab Arquivos
    await sleep(3000);
    adb(["shell", "input", "tap", "900", "2270"]); // Tab Pro
    await sleep(2800);
    adb(["shell", "input", "tap", "180", "2270"]); // Back to Tab Limpar

    await new Promise((resolve, reject) => {
      rec.on("close", (code) => (code === 0 ? resolve() : reject(new Error(`screenrecord exit ${code}`))));
      rec.on("error", reject);
    });

    await sleep(600);
    adb(["pull", remotePath, localRaw]);
    adb(["shell", "rm", "-f", remotePath]);
  }
  const rawSizeMb = (fs.statSync(localRaw).size / (1024 * 1024)).toFixed(2);
  console.log(`✅ Vídeo bruto pronto (${rawSizeMb} MB): ${localRaw}`);

  // 3. Render vertical 1080x1920 Reels (PT-BR and EN-US) with crisp OLED framing + top/bottom hook bars
  const variants = [
    {
      lang: "ptbr",
      outFile: path.join(outDir, `midiastore-reel-ptbr-${TODAY}.mp4`),
      topText: "CELULAR SEM ESPACO? APENAS 5.9 MB",
      bottomText: "MIDIASTORE NO GOOGLE PLAY - 100% OFFLINE",
    },
    {
      lang: "enus",
      outFile: path.join(outDir, `midiastore-reel-enus-${TODAY}.mp4`),
      topText: "ANDROID STORAGE FULL? ONLY 5.9 MB",
      bottomText: "MIDIASTORE ON GOOGLE PLAY - 100% PRIVATE",
    },
  ];

  const fontFile = "C\\:/Windows/Fonts/arialbd.ttf";
  const rendered = [];

  for (const v of variants) {
    // Scale 1080x2400 phone video to fit cleanly centered inside 1080x1920 pure OLED black canvas with bold hook banners
    const filter = [
      "scale=-2:1560",
      "pad=1080:1920:(ow-iw)/2:(oh-ih)/2:color=black",
      `drawbox=x=0:y=0:w=1080:h=170:color=black@0.92:t=fill`,
      `drawbox=x=0:y=1750:w=1080:h=170:color=#0A84FF@0.95:t=fill`,
      `drawtext=fontfile='${fontFile}':expansion=none:text='${v.topText}':fontcolor=white:fontsize=44:x=(w-text_w)/2:y=68`,
      `drawtext=fontfile='${fontFile}':expansion=none:text='${v.bottomText}':fontcolor=white:fontsize=38:x=(w-text_w)/2:y=1818`,
    ].join(",");

    execFileSync(
      FFMPEG,
      [
        "-y",
        "-i", localRaw,
        "-vf", filter,
        "-c:v", "libx264",
        "-preset", "veryfast",
        "-crf", "22",
        "-pix_fmt", "yuv420p",
        "-movflags", "+faststart",
        "-an",
        v.outFile,
      ],
      { stdio: "pipe", timeout: 60000 },
    );
    const szMb = (fs.statSync(v.outFile).size / (1024 * 1024)).toFixed(2);
    rendered.push({ lang: v.lang, file: v.outFile, sizeMb: Number(szMb) });
    console.log(`🎬 Reel ${v.lang.toUpperCase()} renderizado (${szMb} MB): ${v.outFile}`);
  }

  console.log(JSON.stringify({ ok: true, localRaw, rawSizeMb: Number(rawSizeMb), rendered }, null, 2));
}

main().catch((err) => {
  console.error("❌ Erro na captura/renderização de Reel:", err.message);
  process.exit(1);
});
