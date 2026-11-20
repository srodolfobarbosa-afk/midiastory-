#!/usr/bin/env node
/**
 * PUBLICADOR AUTOMÁTICO DE YOUTUBE SHORTS (YouTube Data API v3 — Módulo 6)
 *
 * Faz upload resumable em Node.js nativo (zero dependências externas) de vídeos 1080x1920
 * da pasta marketing/videos/ para o canal do YouTube como Shorts (#Shorts).
 *
 * Requisitos (quando ativar publicação automática):
 *   YOUTUBE_OAUTH_ACCESS_TOKEN (OAuth2 token com escopo https://www.googleapis.com/auth/youtube.upload)
 *
 * Uso:
 *   node scripts/youtube-shorts-uploader.mjs --dry-run
 *   node scripts/youtube-shorts-uploader.mjs --file marketing/videos/midiastore-reel-ptbr-2026-11-19.mp4 --lang pt-BR
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const PLAY_URL = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore&utm_source=youtube_shorts&utm_medium=organic";

const METADATA_BY_LANG = {
  "pt-BR": {
    title: "Celular sem espaço? Libere GBs com apenas 5,9 MB! #Shorts",
    description:
      "Pare de apagar fotos importantes! O Midiastore pesa apenas 5,9 MB, roda 100% offline e usa verificação SHA-256 para achar duplicatas exatas e vídeos gigantes sem apagar nada sem sua permissão.\n\nBaixe grátis na Google Play: " +
      PLAY_URL +
      "\nGuia completo: https://midiastory-df6a0.web.app/guides/pt-br/whatsapp-storage-full/?utm_source=youtube_shorts\n\n#Shorts #Android #DicasDeCelular #MemoriaCheia #Midiastore",
    tags: ["android", "memoria cheia", "whatsapp", "limpar celular", "midiastore", "shorts"],
    defaultLanguage: "pt-BR",
  },
  "en-US": {
    title: "Android Storage Full? Reclaim GBs With Only 5.9 MB! #Shorts",
    description:
      "Stop uninstalling apps because of Storage Full! Midiastore is 5.9 MB, pure OLED dark, runs 100% on-device with exact SHA-256 duplicate detection and zero auto-delete bloatware.\n\nGet it free on Google Play: " +
      PLAY_URL +
      "\nFull guide: https://midiastory-df6a0.web.app/guides/en-us/android-storage-full-fix/?utm_source=youtube_shorts\n\n#Shorts #AndroidTips #StorageFull #TechTips #Midiastore",
    tags: ["android", "storage full", "phone cleaner", "duplicate photos", "midiastore", "shorts"],
    defaultLanguage: "en-US",
  },
};

export async function uploadShort({ filePath, lang = "pt-BR", privacyStatus = "public", dryRun = false }) {
  const absPath = path.isAbsolute(filePath) ? filePath : path.join(projectRoot, filePath);
  if (!fs.existsSync(absPath)) {
    throw new Error(`Vídeo não encontrado: ${absPath}`);
  }
  const stat = fs.statSync(absPath);
  const meta = METADATA_BY_LANG[lang] || METADATA_BY_LANG["pt-BR"];

  const snippet = {
    snippet: {
      title: meta.title,
      description: meta.description,
      tags: meta.tags,
      categoryId: "28", // Science & Technology
      defaultLanguage: meta.defaultLanguage,
    },
    status: {
      privacyStatus,
      selfDeclaredMadeForKids: false,
    },
  };

  if (dryRun) {
    return {
      dryRun: true,
      ready: true,
      file: absPath,
      sizeBytes: stat.size,
      endpoint: "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status",
      snippet,
    };
  }

  const accessToken = process.env.YOUTUBE_OAUTH_ACCESS_TOKEN?.trim();
  if (!accessToken) {
    throw new Error(
      "YOUTUBE_OAUTH_ACCESS_TOKEN não definido. Gere um token OAuth2 com escopo youtube.upload ou rode com --dry-run.",
    );
  }

  // Step 1: Initiate resumable upload session
  const initRes = await fetch(
    "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json; charset=UTF-8",
        "X-Upload-Content-Length": String(stat.size),
        "X-Upload-Content-Type": "video/mp4",
      },
      body: JSON.stringify(snippet),
      signal: AbortSignal.timeout(15000),
    },
  );
  if (!initRes.ok) {
    throw new Error(`YouTube init upload falhou (HTTP ${initRes.status}): ${await initRes.text()}`);
  }
  const uploadUrl = initRes.headers.get("location");
  if (!uploadUrl) {
    throw new Error("YouTube não retornou header Location para resumable upload.");
  }

  // Step 2: Upload MP4 binary
  const videoBytes = fs.readFileSync(absPath);
  const putRes = await fetch(uploadUrl, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "video/mp4",
      "Content-Length": String(stat.size),
    },
    body: videoBytes,
    signal: AbortSignal.timeout(120000),
  });
  const putText = await putRes.text();
  if (!putRes.ok) {
    throw new Error(`YouTube PUT falhou (HTTP ${putRes.status}): ${putText}`);
  }
  const uploaded = JSON.parse(putText);
  return {
    uploaded: true,
    videoId: uploaded.id,
    shortsUrl: `https://youtube.com/shorts/${uploaded.id}`,
    title: uploaded.snippet?.title,
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const args = process.argv.slice(2);
  const dryRun = args.includes("--dry-run");
  const fileIdx = args.indexOf("--file");
  const langIdx = args.indexOf("--lang");
  const defaultFile = "marketing/videos/midiastore-reel-ptbr-2026-11-19.mp4";
  const filePath = fileIdx > -1 ? args[fileIdx + 1] : defaultFile;
  const lang = langIdx > -1 ? args[langIdx + 1] : "pt-BR";

  uploadShort({ filePath, lang, dryRun })
    .then((res) => {
      console.log("📺 YOUTUBE SHORTS UPLOADER:");
      console.log(JSON.stringify(res, null, 2));
    })
    .catch((err) => {
      console.error("❌", err.message);
      process.exit(1);
    });
}
