#!/usr/bin/env node
/**
 * FRIDAY Core CLI — Ponto Único de Comando da Construtora Autônoma
 *
 * Uso:
 *   node friday-core/cli.mjs <council|skills|qa|lab|night|audit|hud|quota|radar> [args...]
 */

import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");

const ROUTES = {
  council: "friday-core/council-adversarial.mjs",
  skills: "friday-core/skills-registry.mjs",
  qa: "friday-core/visual-qa.mjs",
  lab: "friday-core/money-lab.mjs",
  night: "friday-core/night-factory.mjs",
  immune: "friday-core/immune-memory.mjs",
  team: "friday-core/specialists-factory.mjs",
  audit: "scripts/friday-self-audit.mjs",
  hud: "scripts/friday-hud.mjs",
  quota: "scripts/friday-quota-guard.mjs",
  radar: "scripts/friday-play-radar.mjs",
  triage: "scripts/friday-triage-engine.mjs",
};

const [cmd, ...rest] = process.argv.slice(2);
if (!cmd || !ROUTES[cmd]) {
  console.log("🤖 FRIDAY Core v2.0 — Comandos disponíveis:");
  console.log("  " + Object.keys(ROUTES).join(" | "));
  process.exit(cmd ? 1 : 0);
}

const target = path.join(PROJECT_ROOT, ROUTES[cmd]);
const res = spawnSync(process.execPath, [target, ...rest], {
  cwd: PROJECT_ROOT,
  stdio: "inherit",
});
process.exit(res.status ?? 0);
