#!/usr/bin/env node
/**
 * FRIDAY 3D Presenter Studio Server (60 FPS WebGL Three.js + TalkingHead + ARKit 52 Blendshapes + Braços/Mãos)
 *
 * Zero Python | Custo R$ 0,00 | Roda leve no Edge/Chrome local.
 * Endpoints:
 *   GET /                 -> Estúdio 3D Interativo
 *   GET /assets/*         -> Modelo .glb e módulos TalkingHead locais
 *   POST /api/speak       -> Gera áudio PT-BR neural via msedge-tts (FranciscaNeural) + timestamps aproximados
 */

import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { fileURLToPath } from "node:url";
import { MsEdgeTTS, OUTPUT_FORMAT } from "../../scripts/node_modules/msedge-tts/dist/index.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ASSETS_DIR = path.join(__dirname, "assets");
const PORT = 4789;

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".mjs": "application/javascript; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".glb": "model/gltf-binary",
  ".mp3": "audio/mpeg",
  ".wav": "audio/wav",
  ".json": "application/json; charset=utf-8",
};

async function synthesizeEdgeMp3(text, voice = "pt-BR-FranciscaNeural") {
  const tts = new MsEdgeTTS();
  await tts.setMetadata(voice, OUTPUT_FORMAT.AUDIO_24KHZ_48KBITRATE_MONO_MP3);
  const tmpDir = path.join(os.tmpdir(), `friday3d-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`);
  fs.mkdirSync(tmpDir, { recursive: true });
  try {
    const res = await tts.toFile(tmpDir, text);
    const mp3Path = typeof res === "string" ? res : res?.audioFilePath || path.join(tmpDir, "audio.mp3");
    return fs.readFileSync(mp3Path);
  } finally {
    try { fs.rmSync(tmpDir, { recursive: true, force: true }); } catch {}
  }
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url || "/", `http://127.0.0.1:${PORT}`);

    if (url.pathname === "/api/speak" && req.method === "POST") {
      let body = "";
      for await (const chunk of req) body += chunk;
      const { text, voice } = JSON.parse(body || "{}");
      const clean = String(text || "").trim().slice(0, 800);
      if (!clean) {
        res.writeHead(400, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Texto vazio" }));
        return;
      }
      const mp3 = await synthesizeEdgeMp3(clean, voice || "pt-BR-FranciscaNeural");
      res.writeHead(200, {
        "Content-Type": "audio/mpeg",
        "Content-Length": mp3.length,
        "Cache-Control": "no-store",
      });
      res.end(mp3);
      return;
    }

    let filePath =
      url.pathname === "/"
        ? path.join(__dirname, "index.html")
        : path.join(__dirname, url.pathname.replace(/^\/+/, ""));

    if (!filePath.startsWith(__dirname) || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
      res.writeHead(404);
      res.end("Not found");
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, {
      "Content-Type": MIME[ext] || "application/octet-stream",
      "Cache-Control": ext === ".glb" ? "public, max-age=86400" : "no-store",
    });
    fs.createReadStream(filePath).pipe(res);
  } catch (e) {
    res.writeHead(500, { "Content-Type": "text/plain" });
    res.end(String(e.message || e));
  }
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`FRIDAY_3D_STUDIO_READY: http://127.0.0.1:${PORT}`);
});
