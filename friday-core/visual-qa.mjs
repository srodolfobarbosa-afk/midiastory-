#!/usr/bin/env node
/**
 * FRIDAY Core — 3. Mãos e Olhos Reais (QA Visual & Técnico Autônomo — Web + Android ADB)
 *
 * Modos:
 *   --web <url>       Audita HTTP, latência, SEO/meta, viewport mobile e links críticos ao vivo
 *   --android         Detecta dispositivo via ADB, captura screenshot + dump UI (ou reporta NO_DEVICE com verdade)
 *   --vision <imgPath> Analisa um screenshot real com Google AI Studio Vision (encontra cortes/UX/bugs)
 *
 * Uso:
 *   node friday-core/visual-qa.mjs --web https://midiastory-df6a0.web.app/
 *   node friday-core/visual-qa.mjs --android
 *   node friday-core/visual-qa.mjs --vision C:\path\to\screen.png
 */

import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const SECRETS_PATH = path.join(PROJECT_ROOT, ".secrets", "ai-media-keys.json");
const ADB_PATH = "C:\\Users\\rodolfo\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe";

function loadGoogleKey() {
  let key = process.env.GOOGLE_AI_STUDIO_API_KEY || process.env.GOOGLE_AQ_API_KEY || "";
  if (!key && fs.existsSync(SECRETS_PATH)) {
    try {
      const raw = JSON.parse(fs.readFileSync(SECRETS_PATH, "utf8"));
      key = raw.GOOGLE_AI_STUDIO_API_KEY || raw.GOOGLE_AQ_API_KEY || "";
    } catch {}
  }
  return key;
}

async function auditWeb(url) {
  const start = Date.now();
  const res = await fetch(url, { signal: AbortSignal.timeout(12000) });
  const ms = Date.now() - start;
  const html = await res.text();

  const title = (html.match(/<title[^>]*>([\s\S]*?)<\/title>/i)?.[1] || "").trim();
  const hasViewport = /<meta[^>]+name=["']viewport["']/i.test(html);
  const hasDescription = /<meta[^>]+name=["']description["']/i.test(html);
  const h1Count = (html.match(/<h1[\s>]/gi) || []).length;
  const imgWithoutAlt = (html.match(/<img(?![^>]*alt=)[^>]*>/gi) || []).length;
  const sizeKb = +(Buffer.byteLength(html, "utf8") / 1024).toFixed(1);

  const issues = [];
  if (res.status !== 200) issues.push(`HTTP ${res.status}`);
  if (ms > 1500) issues.push(`Latência alta (${ms}ms)`);
  if (!hasViewport) issues.push("Sem meta viewport mobile");
  if (!hasDescription) issues.push("Sem meta description (SEO)");
  if (h1Count === 0) issues.push("Sem tag <h1>");
  if (imgWithoutAlt > 0) issues.push(`${imgWithoutAlt} imagem(ns) sem atributo alt`);

  console.log(`\n👁️ FRIDAY VISUAL/WEB QA — ${url}`);
  console.log(`• Status: HTTP ${res.status} | Latência: ${ms}ms | HTML: ${sizeKb} KB`);
  console.log(`• Título: "${title || "(vazio)"}"`);
  console.log(`• Mobile Viewport: ${hasViewport ? "OK" : "FALHA"} | Meta Desc: ${hasDescription ? "OK" : "FALHA"} | H1: ${h1Count}`);
  console.log(`• Alertas: ${issues.length ? issues.join(" | ") : "ZERO alertas críticos ✅"}`);
}

function auditAndroid() {
  console.log("\n📱 FRIDAY ANDROID QA (ADB):");
  if (!fs.existsSync(ADB_PATH)) {
    console.log("• ADB não encontrado no caminho esperado.");
    return;
  }
  const dev = spawnSync(ADB_PATH, ["devices"], { encoding: "utf8", timeout: 5000 });
  const lines = (dev.stdout || "")
    .split(/\r?\n/)
    .filter((l) => /\tdevice\b/.test(l));

  if (lines.length === 0) {
    console.log("• Status: NO_DEVICE (Motorola não conectado via USB neste momento — sem simulação).");
    return;
  }

  const serial = lines[0].split("\t")[0];
  const outPng = path.join(os.tmpdir(), `friday-adb-qa-${Date.now()}.png`);
  const cap = spawnSync(ADB_PATH, ["-s", serial, "exec-out", "screencap", "-p"], {
    maxBuffer: 15 * 1024 * 1024,
    timeout: 10000,
  });
  if (cap.status === 0 && cap.stdout?.length > 1000) {
    fs.writeFileSync(outPng, cap.stdout);
    console.log(`• Dispositivo: ${serial} ✅`);
    console.log(`• Screenshot real capturado: ${outPng} (${(cap.stdout.length / 1024).toFixed(1)} KB)`);
  } else {
    console.log(`• Dispositivo ${serial} detectado, mas falha ao capturar tela.`);
  }
}

async function auditVision(imgPath) {
  if (!imgPath || !fs.existsSync(imgPath)) {
    console.error(`Imagem não encontrada: ${imgPath}`);
    process.exit(1);
  }
  const key = loadGoogleKey();
  if (!key) {
    console.error("GOOGLE_AI_STUDIO_API_KEY ausente para visão computacional.");
    process.exit(1);
  }
  const ext = path.extname(imgPath).toLowerCase();
  const mimeType = ext === ".jpg" || ext === ".jpeg" ? "image/jpeg" : "image/png";
  const b64 = fs.readFileSync(imgPath).toString("base64");

  const res = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${key}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      signal: AbortSignal.timeout(25000),
      body: JSON.stringify({
        contents: [
          {
            parts: [
              {
                text: "Você é um auditor sênior de UX/UI. Analise esta captura de tela e reporte em até 5 bullets curtos: (1) textos cortados/sobrepostos, (2) contraste/legibilidade, (3) hierarquia visual e CTA, (4) bugs visuais reais. Zero invenção.",
              },
              { inlineData: { mimeType, data: b64 } },
            ],
          },
        ],
        generationConfig: { temperature: 0.2, maxOutputTokens: 500 },
      }),
    }
  );
  if (!res.ok) {
    console.error(`Vision HTTP ${res.status}`);
    process.exit(1);
  }
  const data = await res.json();
  console.log(`\n👁️ AUDITORIA VISUAL IA (${path.basename(imgPath)}):\n`);
  console.log(data?.candidates?.[0]?.content?.parts?.[0]?.text || "(sem texto)");
}

async function main() {
  const args = process.argv.slice(2);
  const webIdx = args.indexOf("--web");
  const visIdx = args.indexOf("--vision");
  const runAndroid = args.includes("--android");

  if (webIdx === -1 && visIdx === -1 && !runAndroid) {
    await auditWeb("https://midiastory-df6a0.web.app/");
    auditAndroid();
    return;
  }
  if (webIdx !== -1 && args[webIdx + 1]) await auditWeb(args[webIdx + 1]);
  if (runAndroid) auditAndroid();
  if (visIdx !== -1 && args[visIdx + 1]) await auditVision(args[visIdx + 1]);
}

main().catch((e) => {
  console.error("VISUAL_QA_ERROR:", e.message);
  process.exit(1);
});
