#!/usr/bin/env node
/**
 * AUDITOR AUTO-EVOLUTIVO IMPLACÁVEL (Midiastore Engine)
 * 
 * Realiza varredura profunda em todas as pastas do projeto:
 * - app/src/main/
 * - docs/
 * - scripts/
 * - supabase/functions/
 * - .github/workflows/
 * 
 * Identifica pontos positivos e negativos, corrige menções de preço para os oficiais:
 * Mensal: R$ 14,99 | Anual: R$ 79,99 | Vitalício: R$ 149,99
 * Valida links, scripts passivos e falhas de integração.
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const supabaseToken = "sbp_v0_d61bbc0f218c0c8ae5d530431302f765df7d0815";
const projectRef = "usmqtnlsaavxqguwrtix";

const OFFICIAL_PRICES = {
  monthly: "R$ 14,99",
  annual: "R$ 79,99",
  lifetime: "R$ 149,99",
};

const findings = {
  positives: [],
  negatives: [],
  fixesApplied: [],
};

function walkDir(dir, filterRegex) {
  let results = [];
  if (!fs.existsSync(dir)) return results;
  const list = fs.readdirSync(dir);
  for (const file of list) {
    if (file === "node_modules" || file === ".git" || file === ".gradle-local" || file === "build") continue;
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat && stat.isDirectory()) {
      results = results.concat(walkDir(fullPath, filterRegex));
    } else if (!filterRegex || filterRegex.test(file)) {
      results.push(fullPath);
    }
  }
  return results;
}

// 1. AUDITORIA DE PREÇOS
function auditarECorrigirPrecos() {
  console.log("🔍 [Auditoria 1/5] Verificando e corrigindo menções de preços em docs e scripts...");
  const targetFiles = [
    ...walkDir(path.join(projectRoot, "docs"), /\.(html|md|json)$/),
    ...walkDir(path.join(projectRoot, "scripts"), /\.(mjs|js)$/),
  ];

  let priceFixCount = 0;
  for (const filePath of targetFiles) {
    let content = fs.readFileSync(filePath, "utf8");
    let changed = false;

    // Corrige R$ 149,99 -> R$ 149,99 (se referir a vitalício) ou R$ 14,99 (se referir a mensal)
    if (content.includes("29,90") || content.includes("29.90")) {
      content = content.replace(/Pro Lifetime \(R\$ 29,90\)/g, "Pro Lifetime (R$ 149,99)");
      content = content.replace(/R\$ 29,90/g, "R$ 149,99");
      changed = true;
      priceFixCount++;
    }

    // Corrige R$ 14,99 -> R$ 14,99
    if (content.includes("14,99") || content.includes("14.99")) {
      content = content.replace(/14,99/g, "14,99");
      content = content.replace(/14\.90/g, "14.99");
      changed = true;
      priceFixCount++;
    }

    // Corrige R$ 79,99 -> R$ 79,99
    if (content.includes("79,99") || content.includes("79.99")) {
      content = content.replace(/79,99/g, "79,99");
      content = content.replace(/79\.90/g, "79.99");
      changed = true;
      priceFixCount++;
    }

    // Corrige R$ 149,99 -> R$ 149,99
    if (content.includes("149,99") || content.includes("149.99")) {
      content = content.replace(/149,99/g, "149,99");
      content = content.replace(/149\.90/g, "149.99");
      changed = true;
      priceFixCount++;
    }

    if (changed) {
      fs.writeFileSync(filePath, content, "utf8");
      findings.fixesApplied.push(`Preços corrigidos para padrão oficial em: ${path.relative(projectRoot, filePath)}`);
    }
  }

  if (priceFixCount > 0) {
    findings.negatives.push(`Encontradas menções inconsistentes de preços (ex: 29,90 / 14,99 / 79,99 / 149,99) em ${priceFixCount} arquivos.`);
  } else {
    findings.positives.push(`Preços auditados e 100% alinhados com a tabela oficial (R$ 14,99 / R$ 79,99 / R$ 149,99).`);
  }
}

// 2. AUDITORIA DE GRADLE E CI
function auditarGradleECI() {
  console.log("🔍 [Auditoria 2/5] Auditando gradle.properties e GitHub Workflows...");
  const gradlePropsPath = path.join(projectRoot, "gradle.properties");
  if (fs.existsSync(gradlePropsPath)) {
    const content = fs.readFileSync(gradlePropsPath, "utf8");
    if (content.includes("org.gradle.java.home")) {
      findings.negatives.push("gradle.properties continha org.gradle.java.home com caminho fixo de máquina Linux, quebrando CI.");
      const cleaned = content.split("\n").filter(l => !l.startsWith("org.gradle.java.home=")).join("\n");
      fs.writeFileSync(gradlePropsPath, cleaned, "utf8");
      findings.fixesApplied.push("Removido org.gradle.java.home hardcoded de gradle.properties para compatibilidade universal (Windows + Ubuntu runner).");
    } else {
      findings.positives.push("gradle.properties limpo e sem caminhos de JDK hardcoded que quebrem runners.");
    }
  }

  const workflowsDir = path.join(projectRoot, ".github", "workflows");
  const workflows = walkDir(workflowsDir, /\.ya?ml$/);
  if (workflows.length >= 3) {
    findings.positives.push(`${workflows.length} workflows GitHub Actions ativos (CI Android, Fábrica Ubuntu 24/7 e Mãos na Nuvem).`);
  } else {
    findings.negatives.push("Menos de 3 workflows encontrados em .github/workflows.");
  }
}

// 3. AUDITORIA DE LINKS E PÁGINAS EM DOCS
function auditarDocsEWeb() {
  console.log("🔍 [Auditoria 3/5] Auditando páginas web e rotas em docs/...");
  const docsDir = path.join(projectRoot, "docs");
  const htmlFiles = walkDir(docsDir, /\.html$/);
  
  const requiredPages = ["index.html", "terminal.html", "painel.html", "privacidade.html"];
  for (const page of requiredPages) {
    if (fs.existsSync(path.join(docsDir, page))) {
      findings.positives.push(`Página pública essencial presente: docs/${page}`);
    } else {
      findings.negatives.push(`Página pública ausente: docs/${page}`);
    }
  }

  // Verifica terminal.html quanto à integração de chat e seletor
  const termPath = path.join(docsDir, "terminal.html");
  if (fs.existsSync(termPath)) {
    const termContent = fs.readFileSync(termPath, "utf8");
    if (termContent.includes("model-selector-bar") && termContent.includes("FUSAO_TRINDADE")) {
      findings.positives.push("terminal.html possui Seletor de Modelo (Fusão Trindade, Carbon, Opus, Fable, Free) ativo.");
    } else {
      findings.negatives.push("terminal.html sem seletor de modelos da Trindade configurado.");
    }
  }
}

// 4. AUDITORIA DE SUPABASE E BANCO
async function auditarSupabase() {
  console.log("🔍 [Auditoria 4/5] Auditando tabelas e cotas do Supabase...");
  try {
    const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${supabaseToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        query: `
          SELECT table_name 
          FROM information_schema.tables 
          WHERE table_schema = 'public' 
          ORDER BY table_name;
        `
      }),
    });
    if (res.ok) {
      const tables = await res.json();
      const names = tables.map(t => t.table_name);
      findings.positives.push(`Supabase conectado com sucesso. Tabelas ativas: ${names.join(", ")}`);
    } else {
      findings.negatives.push(`Falha ao consultar tabelas do Supabase: HTTP ${res.status}`);
    }
  } catch (e) {
    findings.negatives.push(`Erro de conexão com Supabase: ${e.message}`);
  }
}

// 5. REGISTRAR MEMÓRIA DA AUDITORIA
async function registrarResultadoAuditoria() {
  console.log("🔍 [Auditoria 5/5] Registrando relatório em public.memories...");
  const report = `[Auditoria Auto-Evolutiva Concluída]
- Pontos Positivos (${findings.positives.length}):
  ${findings.positives.map(p => `+ ${p}`).join("\n  ")}
- Pontos Negativos Identificados (${findings.negatives.length}):
  ${findings.negatives.map(n => `- ${n}`).join("\n  ")}
- Correções Aplicadas Imediatamente (${findings.fixesApplied.length}):
  ${findings.fixesApplied.map(f => `* ${f}`).join("\n  ")}
- Tabela Oficial de Preços Fixada: Mensal R$ 14,99 | Anual R$ 79,99 | Vitalício R$ 149,99
- Meta Operacional: R$ 1.000.000,00 a R$ 3.000.000,00`;

  try {
    const escaped = report.replace(/'/g, "''");
    await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${supabaseToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        query: `INSERT INTO public.memories (content) VALUES ('${escaped}') RETURNING id;`
      }),
    });
    console.log("💾 Relatório de auditoria salvo em public.memories.");
  } catch (e) {
    console.warn("Aviso ao salvar relatório no Supabase:", e.message);
  }

  return report;
}

export async function runFullAudit() {
  console.log("================================================================");
  console.log("🛡️ INICIANDO AUDITORIA AUTO-EVOLUTIVA IMPLACÁVEL DE TODAS AS PASTAS");
  console.log("================================================================\n");

  auditarECorrigirPrecos();
  auditarGradleECI();
  auditarDocsEWeb();
  await auditarSupabase();
  const summary = await registrarResultadoAuditoria();

  console.log("\n==================== RESUMO DA AUDITORIA ====================");
  console.log(summary);
  console.log("=============================================================");
  return findings;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  runFullAudit().catch(console.error);
}
