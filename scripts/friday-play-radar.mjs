#!/usr/bin/env node
/**
 * FRIDAY Play Store Competitor & ASO Radar v2 (Node.js — Zero Python)
 *
 * Monitora ao vivo:
 *  1) O próprio Midiastore (`com.rdf112.midiastore`) — posição, nota, versão pública e reviews
 *  2) Top 5 concorrentes em múltiplos mercados estratégicos (`pt/br`, `en/us`, `es/es`)
 *  3) Dores reais (reviews 1-2★) do #1 concorrente para converter em vantagem no Midiastore
 *  4) Persiste snapshot histórico em `docs/aso/play-radar-latest.json`
 *
 * Usage:
 *   node scripts/friday-play-radar.mjs [--open]
 *   node scripts/friday-play-radar.mjs "limpar armazenamento celular" [--open]
 */

import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const OWN_APP_ID = "com.rdf112.midiastore";

const MARKETS = [
  { key: "pt-BR", lang: "pt", country: "br", term: "limpador de armazenamento celular" },
  { key: "en-US", lang: "en", country: "us", term: "phone storage cleaner duplicate photos" },
  { key: "es-ES", lang: "es", country: "es", term: "limpiador almacenamiento celular" },
];

async function main() {
  const args = process.argv.slice(2);
  const openBoard = args.includes("--open");
  const customQuery = args.filter((a) => !a.startsWith("--")).join(" ").trim();

  const mod = await import("google-play-scraper");
  const gplay = mod.default || mod;

  const snapshot = {
    checkedAt: new Date().toISOString(),
    ownApp: null,
    markets: {},
  };

  // 1) Monitora o próprio Midiastore na Play Store pública
  try {
    const own = await gplay.app({ appId: OWN_APP_ID, lang: "pt", country: "br" });
    snapshot.ownApp = {
      published: true,
      title: own.title,
      score: own.score,
      ratings: own.ratings,
      installs: own.installs,
      version: own.version,
    };
  } catch {
    snapshot.ownApp = {
      published: false,
      note: "Ainda em Teste Fechado (previsão Produção: 26/11) — monitoramento público será ativado automaticamente após aprovação.",
    };
  }

  const lines = [
    `## 🛡️ Status Próprio (${OWN_APP_ID})`,
    snapshot.ownApp.published
      ? `- **Publicado:** ⭐ ${snapshot.ownApp.score} (${snapshot.ownApp.ratings} avaliações) | ${snapshot.ownApp.installs} instalações | v${snapshot.ownApp.version}`
      : `- **Pré-Lançamento:** ${snapshot.ownApp.note}`,
    "",
  ];

  const targets = customQuery
    ? [{ key: "pt-BR", lang: "pt", country: "br", term: customQuery }]
    : MARKETS;

  for (const m of targets) {
    try {
      const apps = await gplay.search({
        term: m.term,
        num: 5,
        lang: m.lang,
        country: m.country,
      });
      const ownRank = apps.findIndex((a) => a.appId === OWN_APP_ID);
      snapshot.markets[m.key] = {
        term: m.term,
        ownRank: ownRank >= 0 ? ownRank + 1 : null,
        top5: apps.map((a) => ({
          title: a.title,
          appId: a.appId,
          score: a.scoreText || (a.score ? a.score.toFixed(1) : "N/D"),
          developer: a.developer,
        })),
        painPoints: [],
      };

      lines.push(`## 🔍 Top 5 [${m.key}] — "${m.term}"`);
      for (const app of apps) {
        const score = app.scoreText || (app.score ? app.score.toFixed(1) : "N/D");
        lines.push(`- **${app.title}** (${app.appId}) — ⭐ ${score} | ${app.developer}`);
      }

      if (m.key === "pt-BR" && apps[0]?.appId) {
        const rev = await gplay.reviews({
          appId: apps[0].appId,
          sort: gplay.sort.NEWEST,
          num: 20,
          lang: m.lang,
          country: m.country,
        });
        const pains = (rev.data || [])
          .filter((r) => r.score <= 2 && r.text && r.text.length > 20)
          .slice(0, 4);
        snapshot.markets[m.key].painPoints = pains.map((p) => ({
          score: p.score,
          text: p.text.replace(/\r?\n/g, " ").slice(0, 180),
        }));
        if (pains.length > 0) {
          lines.push("", `## 🎯 Dores Reais (1-2★) do #1 (${apps[0].title}) para o Midiastore Vencer:`);
          for (const p of snapshot.markets[m.key].painPoints) {
            lines.push(`- **${p.score}★:** ${p.text}`);
          }
        }
      }
      lines.push("");
    } catch (err) {
      snapshot.markets[m.key] = { error: err.message };
    }
  }

  const asoDir = path.join(PROJECT_ROOT, "docs", "aso");
  fs.mkdirSync(asoDir, { recursive: true });
  const snapPath = path.join(asoDir, "play-radar-latest.json");
  fs.writeFileSync(snapPath, JSON.stringify(snapshot, null, 2), "utf8");

  const boardArgs = [
    path.join(PROJECT_ROOT, "scripts", "antigravity-board.mjs"),
    "--title",
    "FRIDAY Radar ASO v2 — Inteligência Multi-Mercado (BR / US / ES)",
    "--subtitle",
    `Snapshot salvo em docs/aso/play-radar-latest.json (${snapshot.checkedAt})`,
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
