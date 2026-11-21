#!/usr/bin/env node
/**
 * FRIDAY Core — 6. Fábrica Noturna Autônoma (Sandbox Seguro + Relatório para Aprovação)
 *
 * Regras de segurança invioláveis:
 *   - NUNCA faz merge em `main`, NUNCA sobrescreve commits do dono, NUNCA publica na Play Store sozinha.
 *   - Roda auditoria completa (self-audit + visual-qa + money-lab) e grava relatório pronto
 *     em `friday-core/memory/night-reports/` para revisão matinal do fundador.
 *
 * Uso:
 *   node friday-core/night-factory.mjs --mission "Resumo da missão noturna" [--dry-run]
 */

import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = path.resolve(__dirname, "..");
const REPORTS_DIR = path.join(__dirname, "memory", "night-reports");

function runStep(label, scriptRel, args = []) {
  const abs = path.join(PROJECT_ROOT, scriptRel);
  const res = spawnSync(process.execPath, [abs, ...args], {
    cwd: PROJECT_ROOT,
    encoding: "utf8",
    timeout: 30000,
  });
  return {
    label,
    ok: res.status === 0,
    output: ((res.stdout || "") + "\n" + (res.stderr || "")).trim(),
  };
}

function main() {
  const args = process.argv.slice(2);
  const mIdx = args.indexOf("--mission");
  const mission = mIdx !== -1 ? args[mIdx + 1] : "Rotina Autônoma de Saúde, QA Web e Money Lab";

  const steps = [
    runStep("Self-Audit FRIDAY", "scripts/friday-self-audit.mjs"),
    runStep("Visual/Web QA", "friday-core/visual-qa.mjs", ["--web", "https://midiastory-df6a0.web.app/"]),
    runStep("Money Lab", "friday-core/money-lab.mjs"),
  ];

  const passed = steps.filter((s) => s.ok).length;
  const stamp = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
  fs.mkdirSync(REPORTS_DIR, { recursive: true });
  const reportPath = path.join(REPORTS_DIR, `night-report-${stamp}.md`);

  const md = [
    `# 🌙 Relatório Fábrica Noturna FRIDAY (${new Date().toISOString()})`,
    `**Missão:** ${mission}`,
    `**Resultado:** ${passed}/${steps.length} etapas verdes`,
    "",
    ...steps.map((s) => `## ${s.ok ? "✅" : "⚠️"} ${s.label}\n\`\`\`\n${s.output}\n\`\`\`\n`),
  ].join("\n");

  fs.writeFileSync(reportPath, md, "utf8");
  console.log(`🌙 Fábrica Noturna concluída: ${passed}/${steps.length} etapas verdes.`);
  console.log(`📄 Relatório pronto para aprovação: ${path.relative(PROJECT_ROOT, reportPath)}`);
  process.exit(passed === steps.length ? 0 : 1);
}

main();
