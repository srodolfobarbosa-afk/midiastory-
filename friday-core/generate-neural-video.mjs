#!/usr/bin/env node
/**
 * FRIDAY Core — Gerador de Vídeo Neural Real via GPU (Multi-Space Failover LivePortrait)
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const AVATAR_DIR = path.join(PROJECT_ROOT, ".agents", "avatar");

const SPACES = [
  "https://innoai-liveportrait.hf.space",
  "https://han-123-liveportrait.hf.space",
  "https://klingteam-liveportrait.hf.space",
];

async function uploadImage(baseUrl, localPath) {
  const bytes = fs.readFileSync(localPath);
  const blob = new Blob([bytes], { type: "image/png" });
  const form = new FormData();
  form.append("files", blob, path.basename(localPath));

  const res = await fetch(`${baseUrl}/upload`, {
    method: "POST",
    body: form,
    signal: AbortSignal.timeout(25000),
  });
  if (!res.ok) throw new Error(`Upload HTTP ${res.status}`);
  const paths = await res.json();
  return paths[0];
}

async function discoverSpaceDrivingVideos(baseUrl) {
  const cfg = await fetch(`${baseUrl}/config`, { signal: AbortSignal.timeout(12000) }).then((r) => r.json());
  const videos = [];
  for (const comp of cfg.components || []) {
    if (comp.type === "dataset" && Array.isArray(comp.props?.samples)) {
      for (const row of comp.props.samples) {
        for (const cell of row) {
          const v = cell?.video;
          if (v?.path && v.path.endsWith(".mp4")) {
            videos.push(v);
          }
        }
      }
    }
  }
  const fnIdx = (cfg.dependencies || []).findIndex((d) => d.api_name === "gpu_wrapped_execute_video");
  const fnDep = cfg.dependencies?.[fnIdx];
  return { fnIdx, inputCount: fnDep?.inputs?.length || 5, videos };
}

async function runOnSpace(baseUrl, localImgPath) {
  console.log(`Tentando GPU Space: ${baseUrl}...`);
  const { fnIdx, inputCount, videos } = await discoverSpaceDrivingVideos(baseUrl);
  if (fnIdx === -1 || videos.length === 0) throw new Error("Space sem gpu_wrapped_execute_video ou vídeos");

  const uploadedImgPath = await uploadImage(baseUrl, localImgPath);
  console.log(`  Upload OK -> ${uploadedImgPath} (fn_index=${fnIdx}, inputs=${inputCount})`);

  const runVideo = async (vidObj) => {
    const sessionHash = Math.random().toString(36).slice(2, 12);
    const baseData = [
      {
        path: uploadedImgPath,
        url: `${baseUrl}/file=${uploadedImgPath}`,
        orig_name: "antigravity-avatar.png",
        meta: { _type: "gradio.FileData" },
      },
      {
        video: vidObj,
        subtitles: null,
      },
      true,
      true,
      true,
    ];
    while (baseData.length < inputCount) baseData.push(true);

    const joinRes = await fetch(`${baseUrl}/queue/join`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        data: baseData.slice(0, inputCount),
        event_data: null,
        fn_index: fnIdx,
        trigger_id: 24,
        session_hash: sessionHash,
      }),
      signal: AbortSignal.timeout(20000),
    });
    if (!joinRes.ok) throw new Error(`queue/join HTTP ${joinRes.status}`);

    const dataRes = await fetch(`${baseUrl}/queue/data?session_hash=${sessionHash}`, {
      headers: { Accept: "text/event-stream" },
      signal: AbortSignal.timeout(120000),
    });
    if (!dataRes.ok) throw new Error(`queue/data HTTP ${dataRes.status}`);

    const rawSse = await dataRes.text();
    for (const line of rawSse.split(/\r?\n/)) {
      if (!line.startsWith("data:")) continue;
      try {
        const evt = JSON.parse(line.slice(5).trim());
        if (evt.msg === "process_completed") {
          if (!evt.success) throw new Error(JSON.stringify(evt.output).slice(0, 180));
          const outData = evt.output?.data || [];
          const outVid = outData[1]?.video || outData[0]?.video || outData[1] || outData[0];
          const vUrl = outVid?.url || (outVid?.path ? `${baseUrl}/file=${outVid.path}` : "");
          if (!vUrl) throw new Error("Sem URL de vídeo");
          const vr = await fetch(vUrl, { signal: AbortSignal.timeout(45000) });
          return Buffer.from(await vr.arrayBuffer());
        }
      } catch (e) {
        if (e.message.includes("error") || e.message.includes("GPU")) throw e;
      }
    }
    throw new Error("SSE sem process_completed");
  };

  const talkBuf = await runVideo(videos[0]);
  fs.writeFileSync(path.join(AVATAR_DIR, "avatar-talk.mp4"), talkBuf);
  console.log(`  ✅ avatar-talk.mp4 NEURAL GERADO (${(talkBuf.length / 1024).toFixed(1)} KB)`);

  const idleVideo = videos[1] || videos[0];
  const idleBuf = await runVideo(idleVideo);
  fs.writeFileSync(path.join(AVATAR_DIR, "avatar-idle.mp4"), idleBuf);
  console.log(`  ✅ avatar-idle.mp4 NEURAL GERADO (${(idleBuf.length / 1024).toFixed(1)} KB)`);
  return true;
}

async function main() {
  const srcImg = path.join(AVATAR_DIR, "antigravity-avatar.png");
  for (const space of SPACES) {
    try {
      await runOnSpace(space, srcImg);
      process.exit(0);
    } catch (e) {
      console.warn(`  ⚠️ Falha em ${space}: ${e.message}`);
    }
  }
  process.exit(1);
}

main();
