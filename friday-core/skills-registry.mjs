#!/usr/bin/env node
/**
 * FRIDAY Core — 2. Biblioteca Viva de Poderes (Auto-Evolução estilo Voyager)
 *
 * Mantém um catálogo vivo, versionado e verificável de todos os poderes/ferramentas
 * construídos pela FRIDAY, permitindo buscar, registrar novos poderes e executá-los em 1 comando.
 *
 * Uso:
 *   node friday-core/skills-registry.mjs list
 *   node friday-core/skills-registry.mjs search "<termo>"
 *   node friday-core/skills-registry.mjs run <id> [args...]
 *   node friday-core/skills-registry.mjs register --id <id> --name "<nome>" --cmd "<comando>" --tags "a,b"
 */

import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const CATALOG_PATH = path.join(__dirname, "skills-catalog.json");

const DEFAULT_SKILLS = [
  {
    id: "triage",
    name: "Motor de Triagem & Peso por Subagente",
    pillar: "FRIDAY",
    cmd: "node scripts/friday-triage-engine.mjs --test",
    tags: ["subagentes", "peso", "triagem", "hook"],
    verified: true,
  },
  {
    id: "self-audit",
    name: "Auto-Auditoria 100% da FRIDAY",
    pillar: "FRIDAY",
    cmd: "node scripts/friday-self-audit.mjs",
    tags: ["saude", "auditoria", "hooks", "ram"],
    verified: true,
  },
  {
    id: "hud",
    name: "Mission Control HUD Ao Vivo",
    pillar: "HIBRIDO",
    cmd: "node scripts/friday-hud.mjs",
    tags: ["hud", "telemetria", "supabase", "ci", "quadro"],
    verified: true,
  },
  {
    id: "quota",
    name: "Guardião de Cota da Sessão",
    pillar: "FRIDAY",
    cmd: "node scripts/friday-quota-guard.mjs",
    tags: ["cota", "tokens", "compact", "semaforo"],
    verified: true,
  },
  {
    id: "play-radar",
    name: "Radar de Concorrentes & Dores 1-Estrela Play Store",
    pillar: "MIDIASTORE",
    cmd: "node scripts/friday-play-radar.mjs",
    tags: ["aso", "play-store", "concorrentes", "mercado"],
    verified: true,
  },
  {
    id: "council",
    name: "Conselho Adversarial (Gemini vs Codex/OpenRouter)",
    pillar: "FRIDAY",
    cmd: "node friday-core/council-adversarial.mjs",
    tags: ["debate", "arquitetura", "codex", "gemini", "decisao"],
    verified: true,
  },
  {
    id: "visual-qa",
    name: "Mãos e Olhos Reais (QA Web + Android ADB)",
    pillar: "HIBRIDO",
    cmd: "node friday-core/visual-qa.mjs",
    tags: ["qa", "web", "android", "adb", "visao"],
    verified: true,
  },
  {
    id: "money-lab",
    name: "Motor Científico de Dinheiro (Hipóteses & ROI)",
    pillar: "MIDIASTORE",
    cmd: "node friday-core/money-lab.mjs",
    tags: ["dinheiro", "vendas", "experimento", "roi", "supabase"],
    verified: true,
  },
  {
    id: "night-factory",
    name: "Fábrica Noturna Autônoma (Sandbox & Relatório)",
    pillar: "FRIDAY",
    cmd: "node friday-core/night-factory.mjs",
    tags: ["noturno", "autonomo", "branch", "pr"],
    verified: true,
  },
];

function loadCatalog() {
  if (!fs.existsSync(CATALOG_PATH)) {
    const initial = { updatedAt: new Date().toISOString(), skills: DEFAULT_SKILLS };
    fs.writeFileSync(CATALOG_PATH, JSON.stringify(initial, null, 2), "utf8");
    return initial;
  }
  return JSON.parse(fs.readFileSync(CATALOG_PATH, "utf8"));
}

function saveCatalog(catalog) {
  catalog.updatedAt = new Date().toISOString();
  fs.writeFileSync(CATALOG_PATH, JSON.stringify(catalog, null, 2), "utf8");
}

function main() {
  const [cmd, ...rest] = process.argv.slice(2);
  const catalog = loadCatalog();

  if (!cmd || cmd === "list") {
    console.log(`\n🧬 BIBLIOTECA VIVA DE PODERES FRIDAY (${catalog.skills.length} poderes verificados):\n`);
    for (const s of catalog.skills) {
      console.log(`  • [${s.id}] (${s.pillar}) ${s.name}`);
      console.log(`    cmd: ${s.cmd} | tags: ${(s.tags || []).join(", ")}`);
    }
    return;
  }

  if (cmd === "search") {
    const q = rest.join(" ").toLowerCase();
    const hits = catalog.skills.filter(
      (s) =>
        s.id.toLowerCase().includes(q) ||
        s.name.toLowerCase().includes(q) ||
        (s.tags || []).some((t) => t.toLowerCase().includes(q))
    );
    console.log(`Resultados para "${q}": ${hits.length}`);
    for (const s of hits) {
      console.log(`  • [${s.id}] ${s.name} -> ${s.cmd}`);
    }
    return;
  }

  if (cmd === "register") {
    const getArg = (flag) => {
      const i = rest.indexOf(flag);
      return i !== -1 ? rest[i + 1] : "";
    };
    const id = getArg("--id");
    const name = getArg("--name");
    const runCmd = getArg("--cmd");
    const pillar = getArg("--pillar") || "FRIDAY";
    const tags = (getArg("--tags") || "").split(",").map((t) => t.trim()).filter(Boolean);
    if (!id || !name || !runCmd) {
      console.error("Uso: register --id <id> --name \"<nome>\" --cmd \"<cmd>\" [--pillar FRIDAY] [--tags a,b]");
      process.exit(1);
    }
    const existingIdx = catalog.skills.findIndex((s) => s.id === id);
    const entry = { id, name, pillar, cmd: runCmd, tags, verified: true, addedAt: new Date().toISOString() };
    if (existingIdx >= 0) catalog.skills[existingIdx] = entry;
    else catalog.skills.push(entry);
    saveCatalog(catalog);
    console.log(`✅ Poder [${id}] registrado na Biblioteca Viva FRIDAY.`);
    return;
  }

  if (cmd === "run") {
    const id = rest[0];
    const extraArgs = rest.slice(1);
    const skill = catalog.skills.find((s) => s.id === id);
    if (!skill) {
      console.error(`Poder "${id}" não encontrado.`);
      process.exit(1);
    }
    const parts = skill.cmd.split(" ");
    const bin = parts[0] === "node" ? process.execPath : parts[0];
    const res = spawnSync(bin, [...parts.slice(1), ...extraArgs], {
      cwd: PROJECT_ROOT,
      stdio: "inherit",
    });
    process.exit(res.status ?? 0);
  }
}

main();
