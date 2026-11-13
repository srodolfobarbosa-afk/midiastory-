#!/usr/bin/env node
/**
 * REGISTRADOR DE APRENDIZADO IA (Supabase: public.aprendizado_ia)
 * 
 * Registra diagnósticos cruzando:
 * - Tipo (ERRO_BUILD, TELA_APP, METRICA_PLAY, CONVERSAO, etc.)
 * - Contexto & Diagnóstico
 * - Peso da Ação (1.00, 0.95, 0.90, 0.85)
 * - Impacto na Meta de R$ 1.000.000,00
 * - Ação Executada & Resultado
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./lib/supabase-auth.mjs";

const projectRef = SUPABASE_PROJECT_REF;

function getToken() {
  return getSupabaseToken();
}

export async function registrarAprendizado({
  tipo,
  contexto,
  diagnostico,
  pesoAcao = 1.00,
  impactoMeta,
  acaoExecutada,
  resultado = "RESOLVIDO",
}) {
  const query = `
    INSERT INTO public.aprendizado_ia (tipo, contexto, diagnostico, peso_acao, impacto_meta, acao_executada, resultado)
    VALUES (
      '${(tipo || "GERAL").replace(/'/g, "''")}',
      '${(contexto || "").replace(/'/g, "''")}',
      '${(diagnostico || "").replace(/'/g, "''")}',
      ${parseFloat(pesoAcao) || 1.00},
      '${(impactoMeta || "").replace(/'/g, "''")}',
      '${(acaoExecutada || "").replace(/'/g, "''")}',
      '${(resultado || "").replace(/'/g, "''")}'
    )
    RETURNING id, created_at;
  `;

  const res = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${getToken()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });

  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text()}`);
  const data = await res.json();
  console.log(`✅ Registro salvo em public.aprendizado_ia (ID: #${data[0]?.id})`);
  return data[0];
}

// Execução via CLI se chamado diretamente
if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const args = process.argv.slice(2);
  const getArg = (flag) => {
    const idx = args.indexOf(flag);
    return idx !== -1 ? args[idx + 1] : null;
  };

  const tipo = getArg("--tipo") || "METRICA_PLAY";
  const contexto = getArg("--contexto") || "Auditoria de Closed Testing";
  const diagnostico = getArg("--diagnostico") || "Necessidade de 16 testadores";
  const pesoAcao = parseFloat(getArg("--peso")) || 1.00;
  const impactoMeta = getArg("--impacto") || "Destravar homologação para liberar faturamento";
  const acaoExecutada = getArg("--acao") || "Ativação do CONTEXTO_MESTRE.md e templates de captação";
  const resultado = getArg("--resultado") || "EM_ANDAMENTO";

  registrarAprendizado({ tipo, contexto, diagnostico, pesoAcao, impactoMeta, acaoExecutada, resultado })
    .catch((err) => {
      console.error("❌ Erro ao registrar:", err.message);
      process.exit(1);
    });
}
