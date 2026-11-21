#!/usr/bin/env node
/**
 * FRIDAY Play Store Competitor & ASO Radar (Node.js)
 *
 * Searches Google Play live for competitor storage/cleaner apps, extracts ratings,
 * installs, and 1-star user pain points so Midiastore beats them on product & ASO.
 * Projects the live report straight onto the Antigravity Professor Board.
 *
 * Usage:
 *   node scripts/friday-play-radar.mjs "limpar armazenamento celular" [--open]
 */

import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");

async function main() {
  const args = process.argv.slice(2);
  const openBoard = args.includes("--open");
  const query = args.filter((a) => !a.startsWith("--")).join(" ") || "limpador de armazenamento celular";

  const mod = await import("google-play-scraper");
  const gplay = mod.default || mod;

  const apps = await gplay.search({
    term: query,
    num: 5,
    lang: "pt",
    country: "br",
  });

  const lines = [
    `## 🔍 Top 5 Concorrentes Ao Vivo na Google Play ("${query}")`,
  ];

  for (const app of apps) {
    const score = app.scoreText || (app.score ? app.score.toFixed(1) : "N/D");
    lines.push(`- **${app.title}** (${app.appId}) — ⭐ ${score} | ${app.developer}`);
  }

  // Mine 1-2 star pain points from top #1 competitor
  if (apps[0]?.appId) {
    try {
      const rev = await gplay.reviews({
        appId: apps[0].appId,
        sort: gplay.sort.NEWEST,
        num: 20,
        lang: "pt",
        country: "br",
      });
      const pains = (rev.data || []).filter((r) => r.score <= 2 && r.text && r.text.length > 20).slice(0, 4);
      if (pains.length > 0) {
        lines.push("", `## 🎯 Dores Reais (Reviews 1-2★) do #1 (${apps[0].title}) para o Midiastore Superar:`);
        for (const p of pains) {
          lines.push(`- **${p.score}★:** ${p.text.replace(/\r?\n/g, " ").slice(0, 180)}`);
        }
      }
    } catch {}
  }

  const boardArgs = [
    path.join(PROJECT_ROOT, "scripts", "antigravity-board.mjs"),
    "--title",
    "FRIDAY Radar ASO — Inteligência Competitiva Google Play",
    "--subtitle",
    `Busca ao vivo no Brasil: "${query}"`,
    "--body",
    lines.join("\n"),
    "--image",
    "",
  ];
  if (openBoard) boardArgs.push("--open");
  spawnSync(process.execPath, boardArgs, { cwd: PROJECT_ROOT, stdio: "inherit" });
  console.log(lines.join("\n"));
}

main().catch((e) => {
  console.error("RADAR_ERR:", e.message);
  process.exit(1);
});
