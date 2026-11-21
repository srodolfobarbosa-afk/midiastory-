#!/usr/bin/env node
/**
 * FRIDAY Core — Fábrica de Especialistas Permanentes (Time Fixo da Construtora)
 *
 * Mantém o elenco fixo de 4 especialistas de elite prontos para despacho paralelo
 * (via subagentes Antigravity ou prompts estruturados para Codex/Swarm).
 *
 * Uso:
 *   node friday-core/specialists-factory.mjs list
 *   node friday-core/specialists-factory.mjs brief <id> "<missão>"
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const TEAM_PATH = path.join(__dirname, "specialists.json");

const DEFAULT_TEAM = [
  {
    id: "android-architect",
    name: "Arquiteto Android/Kotlin & Scoped Storage",
    recommendedModel: "inherit",
    domain: "Kotlin, Android Views/Compose OLED, MediaStore API 29-36, SHA-256 streaming, Gradle 8.13, AAB R8",
    rules: [
      "Zero MANAGE_EXTERNAL_STORAGE",
      "Lixeira API 30+ não conta como espaço liberado",
      "Nunca usar dp(52) fixo em botão de 2 linhas",
      "Compilar e testar antes de declarar pronto",
    ],
  },
  {
    id: "revenue-auditor",
    name: "Auditor de Receita, Billing 8.x & Play Store",
    recommendedModel: "pro",
    domain: "Google Play Billing 8.3.0, Edge Function fail-closed verify-play-purchase, Paywall, Data Safety, Teste Fechado 14 dias",
    rules: [
      "Nunca inventar métricas ou status de Play Console (verificar ao vivo)",
      "Todo entitlement Pro exige validação fail-closed no backend",
      "Regra financeira 50% lucro / 50% reinvestimento",
    ],
  },
  {
    id: "market-hunter",
    name: "Caçador de Mercado, ASO & Dores 1-Estrela",
    recommendedModel: "flash",
    domain: "Play Store scraping real, reviews 1-estrela de concorrentes, ASO 14 idiomas, funil Firebase Hosting",
    rules: [
      "Usar dados reais do friday-play-radar.mjs e Supabase",
      "Transformar dores reais em experimentos mensuráveis no Money Lab",
    ],
  },
  {
    id: "friday-evolver",
    name: "Engenheiro de Evolução da FRIDAY Core",
    recommendedModel: "inherit",
    domain: "Hooks Antigravity, voz neural, avatar lip-sync, MCPs leves, auto-auditoria, memória cognitiva Supabase",
    rules: [
      "Stack 100% Node.js/TS + CLIs nativas (Zero Python)",
      "Custo extra R$ 0,00 (Blindagem Financeira)",
      "Proteger PC de 3,93 GB de RAM (sem daemons pesados)",
    ],
  },
];

function loadTeam() {
  if (!fs.existsSync(TEAM_PATH)) {
    const init = { updatedAt: new Date().toISOString(), specialists: DEFAULT_TEAM };
    fs.writeFileSync(TEAM_PATH, JSON.stringify(init, null, 2), "utf8");
    return init;
  }
  return JSON.parse(fs.readFileSync(TEAM_PATH, "utf8"));
}

function main() {
  const [cmd, id, ...rest] = process.argv.slice(2);
  const team = loadTeam();

  if (!cmd || cmd === "list") {
    console.log(`\n🏭 TIME FIXO DE ESPECIALISTAS FRIDAY (${team.specialists.length} agentes prontos de fábrica):\n`);
    for (const s of team.specialists) {
      console.log(`  • [${s.id}] ${s.name} (modelo: ${s.recommendedModel})`);
      console.log(`    Domínio: ${s.domain}`);
    }
    return;
  }

  if (cmd === "brief") {
    const spec = team.specialists.find((s) => s.id === id);
    if (!spec) {
      console.error(`Especialista "${id}" não encontrado.`);
      process.exit(1);
    }
    const mission = rest.join(" ").trim() || "(definir missão)";
    console.log(
      [
        `=== BRIEFING ESPECIALISTA [${spec.id}] ===`,
        `Papel: ${spec.name} | Modelo: ${spec.recommendedModel}`,
        `Domínio: ${spec.domain}`,
        `Regras invioláveis:\n${spec.rules.map((r) => `  - ${r}`).join("\n")}`,
        `Missão: ${mission}`,
      ].join("\n")
    );
  }
}

main();
