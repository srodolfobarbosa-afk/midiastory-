#!/usr/bin/env node
/**
 * Antigravity Professor Whiteboard (Lousa Visual Interativa)
 *
 * Allows Antigravity to project lessons, step-by-step explanations, diagrams,
 * screenshots, and visual cards onto a sleek OLED board on Rodolfo's desktop
 * while speaking aloud in real time.
 *
 * Usage:
 *   node scripts/antigravity-board.mjs --title "Título" --subtitle "Subtítulo" --body "Markdown/HTML" [--image "C:\path\img.png"] [--open]
 */

import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawn } from "node:child_process";

const BOARD_HTML = path.join(os.tmpdir(), "antigravity-professor-board.html");
const BOARD_DATA = path.join(os.tmpdir(), "antigravity-professor-board.json");

function escapeHtml(s) {
  return String(s || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function formatBodyHtml(text) {
  const lines = String(text || "").split(/\r?\n/);
  return lines
    .map((line) => {
      const trimmed = line.trim();
      if (!trimmed) return "<div style='height:8px'></div>";
      if (trimmed.startsWith("### ")) return `<h3>${escapeHtml(trimmed.slice(4))}</h3>`;
      if (trimmed.startsWith("## ")) return `<h2>${escapeHtml(trimmed.slice(3))}</h2>`;
      if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
        return `<div class="bullet"><span class="dot"></span><span>${escapeHtml(trimmed.slice(2)).replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")}</span></div>`;
      }
      return `<p>${escapeHtml(trimmed).replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")}</p>`;
    })
    .join("\n");
}

function renderBoardHtml(state) {
  let imgHtml = "";
  if (state.imagePath && fs.existsSync(state.imagePath)) {
    const ext = path.extname(state.imagePath).toLowerCase().replace(".", "") || "png";
    const mime = ext === "jpg" ? "jpeg" : ext;
    const b64 = fs.readFileSync(state.imagePath).toString("base64");
    imgHtml = `<div class="media-card"><img src="data:image/${mime};base64,${b64}" alt="Visual"/></div>`;
  }

  return `<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8" />
<meta http-equiv="refresh" content="3" />
<title>Quadro Antigravity — Modo Professor</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #000000;
    color: #F2F2F7;
    font-family: "Segoe UI", system-ui, -apple-system, sans-serif;
    padding: 28px 34px;
    min-height: 100vh;
  }
  .topbar {
    display: flex; align-items: center; justify-content: space-between;
    border-bottom: 1px solid #2C2C2E; padding-bottom: 14px; margin-bottom: 22px;
  }
  .badge {
    display: inline-flex; align-items: center; gap: 8px;
    background: #1C1C1E; border: 1px solid #0A84FF; color: #0A84FF;
    padding: 6px 14px; border-radius: 999px; font-size: 13px; font-weight: 600;
  }
  .dot-live { width: 8px; height: 8px; border-radius: 50%; background: #30D158; }
  .timestamp { color: #8E8E93; font-size: 12px; }
  h1 { font-size: 28px; font-weight: 700; color: #FFFFFF; margin-bottom: 6px; }
  .subtitle { color: #0A84FF; font-size: 15px; margin-bottom: 22px; }
  .card {
    background: #1C1C1E; border: 1px solid #2C2C2E; border-radius: 18px;
    padding: 22px 26px; margin-bottom: 20px; line-height: 1.65; font-size: 16px;
  }
  h2 { font-size: 19px; color: #0A84FF; margin: 12px 0 8px; }
  h3 { font-size: 16px; color: #64D2FF; margin: 10px 0 6px; }
  p { margin-bottom: 8px; color: #E5E5EA; }
  .bullet { display: flex; gap: 10px; align-items: flex-start; margin: 8px 0; color: #E5E5EA; }
  .bullet .dot { width: 8px; height: 8px; border-radius: 50%; background: #0A84FF; margin-top: 9px; flex-shrink: 0; }
  strong { color: #FFFFFF; }
  .media-card {
    background: #101014; border: 2px solid #0A84FF; border-radius: 18px;
    padding: 14px; text-align: center; margin-bottom: 20px;
  }
  .media-card img { width: 100%; max-height: 580px; border-radius: 12px; object-fit: contain; }
</style>
</head>
<body>
  <div class="topbar">
    <div class="badge"><span class="dot-live"></span>Quadro Interativo Antigravity — Modo Professor</div>
    <div class="timestamp">Atualizado em ${escapeHtml(state.updatedAt)}</div>
  </div>
  <h1>${escapeHtml(state.title)}</h1>
  ${state.subtitle ? `<div class="subtitle">${escapeHtml(state.subtitle)}</div>` : ""}
  ${imgHtml}
  <div class="card">
    ${formatBodyHtml(state.body)}
  </div>
</body>
</html>`;
}

function openBoardWindow() {
  const url = `file:///${BOARD_HTML.replace(/\\/g, "/")}`;
  const edgePaths = [
    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
  ];
  const browser = edgePaths.find((p) => fs.existsSync(p));
  if (browser) {
    const child = spawn(browser, [`--app=${url}`, "--window-size=920,680"], {
      detached: true,
      stdio: "ignore",
    });
    child.unref();
  } else {
    const child = spawn("cmd.exe", ["/c", "start", "", url], {
      detached: true,
      stdio: "ignore",
    });
    child.unref();
  }
}

function parseArgs(argv) {
  const out = { open: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--open") out.open = true;
    else if (a === "--title") out.title = argv[++i];
    else if (a === "--subtitle") out.subtitle = argv[++i];
    else if (a === "--body") out.body = argv[++i];
    else if (a === "--image") out.imagePath = argv[++i];
  }
  return out;
}

const args = parseArgs(process.argv.slice(2));
let state = {
  title: "Central de Ensino & Comando Antigravity",
  subtitle: "Pronta para projetar aulas, diagramas, código e telas do Midiastore",
  body: [
    "## O que eu posso projetar aqui enquanto converso com você:",
    "- **Aulas passo a passo** sobre programação, Android, Google Play, tráfego pago e IA.",
    "- **Prints e comparações visuais** do Midiastore no seu Motorola Moto G84.",
    "- **Mapas de arquitetura e checklists** atualizados em tempo real.",
    "",
    "Basta me pedir por voz: **\"Me ensina isso no quadro\"** ou **\"Mostra no quadro\"**!",
  ].join("\n"),
  imagePath: "",
  updatedAt: new Date().toLocaleTimeString("pt-BR"),
};

if (fs.existsSync(BOARD_DATA)) {
  try {
    state = { ...state, ...JSON.parse(fs.readFileSync(BOARD_DATA, "utf8")) };
  } catch {}
}

if (args.title) state.title = args.title;
if (args.subtitle !== undefined) state.subtitle = args.subtitle;
if (args.body) state.body = args.body;
if (args.imagePath !== undefined) state.imagePath = args.imagePath;
state.updatedAt = new Date().toLocaleTimeString("pt-BR");

fs.writeFileSync(BOARD_DATA, JSON.stringify(state, null, 2), "utf8");
fs.writeFileSync(BOARD_HTML, renderBoardHtml(state), "utf8");

if (args.open) {
  openBoardWindow();
}
console.log("BOARD_UPDATED:", BOARD_HTML);
