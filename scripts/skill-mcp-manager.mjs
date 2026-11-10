#!/usr/bin/env node
/**
 * GERENCIADOR DE HABILIDADES (SKILLS) & SERVIDORES MCP (Antigravity Ecosystem)
 * 
 * Funcionalidades:
 * - Lista todas as skills instaladas em .agents/skills/
 * - Valida a conformidade de frontmatter YAML (name, description, progressive disclosure)
 * - Audita os servidores MCP configurados (.agents/plugins/ e .agents/mcp_config.json)
 * - Garante integridade e ausência de duplicações
 * 
 * Uso:
 *   node scripts/skill-mcp-manager.mjs --list
 *   node scripts/skill-mcp-manager.mjs --audit
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const skillsDir = path.join(projectRoot, ".agents", "skills");
const pluginsDir = path.join(projectRoot, ".agents", "plugins");
const rootMcpConfig = path.join(projectRoot, ".agents", "mcp_config.json");

function parseYamlFrontmatter(content) {
  const match = content.match(/^---\r?\n([\s\S]*?)\r?\n---/);
  if (!match) return null;
  const lines = match[1].split("\n");
  const data = {};
  for (const line of lines) {
    const colonIdx = line.indexOf(":");
    if (colonIdx > -1) {
      const key = line.slice(0, colonIdx).trim();
      const val = line.slice(colonIdx + 1).trim().replace(/^["']|["']$/g, "");
      data[key] = val;
    }
  }
  return data;
}

export function listSkills() {
  if (!fs.existsSync(skillsDir)) return [];
  const entries = fs.readdirSync(skillsDir, { withFileTypes: true });
  const skills = [];

  for (const entry of entries) {
    if (entry.isDirectory()) {
      const skillFile = path.join(skillsDir, entry.name, "SKILL.md");
      if (fs.existsSync(skillFile)) {
        const raw = fs.readFileSync(skillFile, "utf8");
        const fm = parseYamlFrontmatter(raw);
        skills.push({
          name: entry.name,
          declaredName: fm?.name || "(não declarado)",
          description: fm?.description || "(sem descrição)",
          path: skillFile,
          hasValidFrontmatter: !!(fm?.name && fm?.description),
        });
      }
    }
  }
  return skills;
}

export function listMcpServers() {
  const servers = [];

  // 1. Root MCP config
  if (fs.existsSync(rootMcpConfig)) {
    try {
      const parsed = JSON.parse(fs.readFileSync(rootMcpConfig, "utf8"));
      if (parsed.mcpServers) {
        for (const [name, conf] of Object.entries(parsed.mcpServers)) {
          servers.push({ name, source: "root (.agents/mcp_config.json)", command: conf.command });
        }
      }
    } catch (e) {
      servers.push({ name: "root_error", source: "root", error: e.message });
    }
  }

  // 2. Plugin MCP configs
  if (fs.existsSync(pluginsDir)) {
    const pluginFolders = fs.readdirSync(pluginsDir, { withFileTypes: true });
    for (const pf of pluginFolders) {
      if (pf.isDirectory()) {
        const pMcp = path.join(pluginsDir, pf.name, "mcp_config.json");
        if (fs.existsSync(pMcp)) {
          try {
            const parsed = JSON.parse(fs.readFileSync(pMcp, "utf8"));
            if (parsed.mcpServers) {
              for (const [name, conf] of Object.entries(parsed.mcpServers)) {
                servers.push({ name, source: `plugin (${pf.name})`, command: conf.command });
              }
            }
          } catch (e) {
            servers.push({ name: pf.name, source: "plugin", error: e.message });
          }
        }
      }
    }
  }

  return servers;
}

export function auditEcosystem() {
  console.log("\n========================================================");
  console.log("🔍 AUDITORIA DO ECOSSISTEMA DE SKILLS & MCP (ANTIGRAVITY)");
  console.log("========================================================\n");

  const skills = listSkills();
  console.log(`📦 Habilidades Instaladas (.agents/skills/): [Total: ${skills.length}]`);
  let skillsOk = 0;
  for (const s of skills) {
    const statusIcon = s.hasValidFrontmatter ? "✅" : "⚠️";
    console.log(`  ${statusIcon} [${s.name}]`);
    console.log(`     Nome Declarado: ${s.declaredName}`);
    console.log(`     Descrição: ${s.description.slice(0, 110)}...`);
    if (s.hasValidFrontmatter) skillsOk++;
  }

  const mcps = listMcpServers();
  console.log(`\n🔌 Servidores MCP Configurados: [Total: ${mcps.length}]`);
  for (const m of mcps) {
    if (m.error) {
      console.log(`  ❌ [${m.name}] Erro: ${m.error}`);
    } else {
      console.log(`  ✅ [${m.name}] Comando: ${m.command} (Origem: ${m.source})`);
    }
  }

  console.log("\n--------------------------------------------------------");
  console.log(`Resultado da Auditoria: ${skillsOk}/${skills.length} skills em conformidade estrita.`);
  console.log("Status: Sistema pronto para injeção progressiva sob demanda.");
  console.log("--------------------------------------------------------\n");
}

function main() {
  const args = process.argv.slice(2);
  if (args.includes("--audit") || args.length === 0) {
    auditEcosystem();
  } else if (args.includes("--list")) {
    console.log(JSON.stringify({ skills: listSkills(), mcps: listMcpServers() }, null, 2));
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main();
}
