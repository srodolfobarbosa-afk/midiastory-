#!/usr/bin/env node
/**
 * FRIDAY YOUTUBE KNOWLEDGE INGESTOR (100% Node.js, Zero Python)
 *
 * Extrai episódios de um canal do YouTube (ex.: @TheDiaryOfACEO),
 * captura metadados, capítulos e legendas/transcrição quando disponíveis,
 * salva a base estruturada em docs/knowledge/<slug>/ e grava conhecimento
 * destilado na memória vetorial do Supabase (public.memories, protegendo 500 MB).
 *
 * Uso:
 *   node scripts/ingest-youtube-channel.mjs --channel https://www.youtube.com/@TheDiaryOfACEO --limit 15 --save-supabase
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { saveValidatedMemory } from "./lib/cognitive-memory.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

function parseArgs() {
  const args = process.argv.slice(2);
  const get = (flag, def = "") => {
    const idx = args.indexOf(flag);
    return idx > -1 && args[idx + 1] ? args[idx + 1] : def;
  };
  return {
    channel: get("--channel", "https://www.youtube.com/@TheDiaryOfACEO"),
    limit: Math.max(1, Math.min(50, Number(get("--limit", "15")) || 15)),
    saveSupabase: args.includes("--save-supabase"),
  };
}

function decodeXmlEntities(str) {
  return String(str || "")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&#x([0-9a-f]+);/gi, (_, h) => String.fromCharCode(parseInt(h, 16)))
    .replace(/&#(\d+);/g, (_, d) => String.fromCharCode(Number(d)));
}

function slugify(str) {
  return String(str || "channel")
    .replace(/^https?:\/\/(www\.)?youtube\.com\/@?/i, "")
    .replace(/[^a-z0-9]+/gi, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();
}

async function resolveChannelId(channelInput) {
  if (/^UC[a-zA-Z0-9_-]{20,}$/.test(channelInput)) return channelInput;
  const url = channelInput.startsWith("http")
    ? channelInput
    : `https://www.youtube.com/${channelInput.startsWith("@") ? channelInput : "@" + channelInput}`;
  const res = await fetch(url, {
    headers: { "User-Agent": UA, "Accept-Language": "en-US,en;q=0.9" },
    signal: AbortSignal.timeout(15000),
  });
  const html = await res.text();
  const match =
    html.match(/"externalId":"(UC[a-zA-Z0-9_-]+)"/) ||
    html.match(/channel_id=(UC[a-zA-Z0-9_-]+)/) ||
    html.match(/"browseId":"(UC[a-zA-Z0-9_-]+)"/);
  if (!match) throw new Error(`Não foi possível resolver channel_id de ${url}`);
  return match[1];
}

async function fetchRssVideos(channelId) {
  const rssUrl = `https://www.youtube.com/feeds/videos.xml?channel_id=${channelId}`;
  const res = await fetch(rssUrl, {
    headers: { "User-Agent": UA },
    signal: AbortSignal.timeout(15000),
  });
  if (!res.ok) throw new Error(`RSS HTTP ${res.status}`);
  const xml = await res.text();
  const entries = [];
  for (const m of xml.matchAll(/<entry>([\s\S]*?)<\/entry>/g)) {
    const b = m[1];
    const id = b.match(/<yt:videoId>(.*?)<\/yt:videoId>/)?.[1];
    const title = decodeXmlEntities(b.match(/<title>([\s\S]*?)<\/title>/)?.[1] || "");
    const published = b.match(/<published>(.*?)<\/published>/)?.[1] || "";
    const views = Number(b.match(/views="(\d+)"/)?.[1] || 0);
    if (id) entries.push({ id, title, published, views });
  }
  return entries;
}

function extractChapters(description) {
  const lines = String(description || "").split(/\r?\n/);
  const chapters = [];
  for (const line of lines) {
    const m = line.trim().match(/^(\d{1,2}:\d{2}(?::\d{2})?)\s*[-–—|]?\s*(.+)$/);
    if (m && m[2].length >= 3) {
      chapters.push(`${m[1]} ${m[2].trim()}`);
    }
  }
  return chapters;
}

async function fetchVideoDetailsAndCaptions(videoId) {
  const watchUrl = `https://www.youtube.com/watch?v=${videoId}`;
  const res = await fetch(watchUrl, {
    headers: { "User-Agent": UA, "Accept-Language": "en-US,en;q=0.9" },
    signal: AbortSignal.timeout(15000),
  });
  const html = await res.text();

  const apiKey = html.match(/"INNERTUBE_API_KEY":"([^"]+)"/)?.[1] || "";
  let vd = {};
  let tracks = [];

  const prMatch = html.match(/var ytInitialPlayerResponse\s*=\s*(\{.+?\});\s*(?:var|<\/script>)/s);
  if (prMatch) {
    try {
      const pr = JSON.parse(prMatch[1]);
      vd = pr.videoDetails || {};
      tracks = pr.captions?.playerCaptionsTracklistRenderer?.captionTracks || [];
    } catch {}
  }

  // Fallback via Innertube ANDROID client (costuma liberar legendas quando web vem vazio)
  if (apiKey) {
    try {
      const itRes = await fetch(`https://www.youtube.com/youtubei/v1/player?key=${apiKey}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "User-Agent": "com.google.android.youtube/19.29.37 (Linux; U; Android 14)" },
        signal: AbortSignal.timeout(12000),
        body: JSON.stringify({
          videoId,
          context: {
            client: {
              clientName: "ANDROID",
              clientVersion: "19.29.37",
              androidSdkVersion: 34,
              hl: "en",
              gl: "US",
            },
          },
        }),
      });
      if (itRes.ok) {
        const itJson = await itRes.json();
        if (!vd.title && itJson.videoDetails) vd = itJson.videoDetails;
        const itTracks = itJson.captions?.playerCaptionsTracklistRenderer?.captionTracks || [];
        if (itTracks.length > 0) tracks = itTracks;
      }
    } catch {}
  }

  let transcript = "";
  let transcriptLang = "";
  if (tracks.length > 0) {
    const preferred =
      tracks.find((t) => t.languageCode === "pt" || t.languageCode === "pt-BR") ||
      tracks.find((t) => t.languageCode === "en" && t.kind !== "asr") ||
      tracks.find((t) => t.languageCode?.startsWith("en")) ||
      tracks[0];
    if (preferred?.baseUrl) {
      transcriptLang = preferred.languageCode || "";
      try {
        const capRes = await fetch(preferred.baseUrl, {
          headers: { "User-Agent": UA },
          signal: AbortSignal.timeout(12000),
        });
        const capXml = await capRes.text();
        if (capXml.length > 50) {
          const segs = [...capXml.matchAll(/<text[^>]*>([\s\S]*?)<\/text>/g)]
            .map((m) => decodeXmlEntities(m[1]).replace(/\s+/g, " ").trim())
            .filter(Boolean);
          if (segs.length > 0) {
            transcript = segs.join(" ");
          }
        }
      } catch {}
    }
  }

  const description = vd.shortDescription || "";
  return {
    id: videoId,
    title: vd.title || "",
    author: vd.author || "",
    lengthSeconds: Number(vd.lengthSeconds || 0),
    description,
    chapters: extractChapters(description),
    transcript,
    transcriptLang,
  };
}

function buildCompactMemorySummary(ep) {
  const mins = ep.lengthSeconds ? `${Math.round(ep.lengthSeconds / 60)}min` : "";
  const chaps = ep.chapters.slice(0, 12).join(" | ");
  const descClean = ep.description
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l && !/^https?:\/\//i.test(l) && !/sponsor|discount|code|subscribe|follow/i.test(l))
    .slice(0, 6)
    .join(" ");
  const transcriptSnippet = ep.transcript ? ep.transcript.slice(0, 900) : "";

  const parts = [
    `[Podcast The Diary Of A CEO | ${ep.published.slice(0, 10)} | ${mins}] ${ep.title} (https://www.youtube.com/watch?v=${ep.id})`,
  ];
  if (chaps) parts.push(`Capítulos-chave: ${chaps}`);
  if (descClean) parts.push(`Resumo: ${descClean.slice(0, 600)}`);
  if (transcriptSnippet) parts.push(`Trecho da conversa (${ep.transcriptLang}): ${transcriptSnippet}`);
  return parts.join(" || ").slice(0, 2200);
}

async function main() {
  const opts = parseArgs();
  const slug = slugify(opts.channel);
  const outDir = path.join(projectRoot, "docs", "knowledge", slug);
  fs.mkdirSync(outDir, { recursive: true });

  console.log(`🔎 Resolvendo canal: ${opts.channel}`);
  const channelId = await resolveChannelId(opts.channel);
  console.log(`✅ Channel ID: ${channelId}`);

  const rssVideos = await fetchRssVideos(channelId);
  const batch = rssVideos.slice(0, opts.limit);
  console.log(`📡 Encontrados ${rssVideos.length} vídeos no RSS. Processando lote de ${batch.length}...`);

  const indexRows = [];
  let savedMemories = 0;
  let transcriptsOk = 0;

  for (let i = 0; i < batch.length; i++) {
    const item = batch[i];
    try {
      const details = await fetchVideoDetailsAndCaptions(item.id);
      const ep = { ...item, ...details, title: details.title || item.title };
      if (ep.transcript) transcriptsOk++;

      const mdPath = path.join(outDir, `${ep.id}.md`);
      const md = [
        `# ${ep.title}`,
        ``,
        `- **Canal:** ${ep.author || slug}`,
        `- **URL:** https://www.youtube.com/watch?v=${ep.id}`,
        `- **Publicado em:** ${ep.published}`,
        `- **Duração:** ${Math.round((ep.lengthSeconds || 0) / 60)} min | **Views (RSS):** ${ep.views}`,
        `- **Legenda/Transcrição extraída:** ${ep.transcript ? `Sim (${ep.transcriptLang}, ${ep.transcript.length} chars)` : "Não disponível via API pública (salvos capítulos + descrição)"}`,
        ``,
        `## Capítulos / Tópicos`,
        ep.chapters.length ? ep.chapters.map((c) => `- ${c}`).join("\n") : "_Sem capítulos marcados na descrição._",
        ``,
        `## Descrição / Notas`,
        ep.description.slice(0, 3500),
        ``,
        ep.transcript ? `## Transcrição da Conversa\n\n${ep.transcript.slice(0, 40000)}` : "",
      ].join("\n");
      fs.writeFileSync(mdPath, md, "utf8");

      let memId = null;
      if (opts.saveSupabase) {
        const memText = buildCompactMemorySummary(ep);
        const saved = await saveValidatedMemory({
          content: memText,
          origem: `youtube_knowledge_${slug}`,
          categoria: "semantic",
          confianca: 0.9,
          allowSpeculative: true,
        });
        memId = saved.id || saved.existingId || null;
        if (memId) savedMemories++;
      }

      indexRows.push({
        id: ep.id,
        date: ep.published.slice(0, 10),
        title: ep.title,
        chapters: ep.chapters.length,
        transcriptChars: ep.transcript.length,
        memId,
      });
      console.log(
        `  [${i + 1}/${batch.length}] ${ep.id} | cap=${ep.chapters.length} | txt=${ep.transcript.length}c | mem=#${memId || "-"} | ${ep.title.slice(0, 65)}`
      );
    } catch (err) {
      console.warn(`  ⚠️ Erro em ${item.id}: ${err.message}`);
    }
  }

  const indexMd = [
    `# 📚 Base de Conhecimento — ${opts.channel}`,
    ``,
    `- **Channel ID:** \`${channelId}\``,
    `- **Última atualização:** ${new Date().toISOString()}`,
    `- **Episódios indexados neste lote:** ${indexRows.length} (transcrições completas: ${transcriptsOk}, memórias no Supabase: ${savedMemories})`,
    ``,
    `| Data | Episódio | Capítulos | Transcrição | Memória Supabase | Arquivo |`,
    `| :--- | :--- | :---: | :---: | :---: | :--- |`,
    ...indexRows.map(
      (r) =>
        `| ${r.date} | ${r.title.replace(/\|/g, "-")} | ${r.chapters} | ${r.transcriptChars > 0 ? `${r.transcriptChars} chars` : "Capítulos+Desc"} | ${r.memId ? `#${r.memId}` : "—"} | [${r.id}.md](${r.id}.md) |`
    ),
    ``,
  ].join("\n");
  fs.writeFileSync(path.join(outDir, "INDEX.md"), indexMd, "utf8");
  console.log(`\n✅ Concluído! Índice salvo em: ${path.join(outDir, "INDEX.md")}`);
}

main().catch((err) => {
  console.error("❌ Erro fatal:", err.message);
  process.exit(1);
});
