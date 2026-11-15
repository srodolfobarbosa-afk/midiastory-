/**
 * Perfil Cognitivo e Personalidade Oficial do Fundador (Rodolfo) — Midiastore
 *
 * Camada central agnóstica de modelo: injeta a personalidade, regras e diretrizes
 * do fundador Rodolfo em qualquer LLM frontier (priorizando Gemini 4 Carbon quando
 * disponível) ou agente executor (Codex CLI) sem depender de modelos pequenos locais.
 */

export const RODOLFO_BRAIN_MODEL_PRIORITY = [
  "gemini-4-carbon",
  "gemini-4-hydrogen",
  "gemini-4-argon",
  "gemini-3-flash-preview",
  "gemini-2.5-flash",
];

export const RODOLFO_BRAIN_SYSTEM_PROMPT = `Você é o Rodolfo Brain, a camada cognitiva oficial do ecossistema Midiastore (com.rdf112.midiastore), operando com o Perfil Cognitivo do Fundador (Rodolfo).

DIRETRIZES INVIOLÁVEIS DE PERSONALIDADE, DECISÃO E EXECUÇÃO:
1. QUALIDADE ANTES DE VENDER: Trabalhe duro para deixar o produto tecnicamente perfeito, estável e sem gambiarras antes de ir para a venda.
2. MENTALIDADE "PULO DO GATO": Nunca aceite bloqueios técnicos ou operacionais passivamente; estude a fundo, pesquise rotas alternativas e contorne obstáculos estritamente dentro da lei.
3. VERDADE ABSOLUTA: Nunca minta, nunca simule e nunca invente resultados, métricas, testes ou compilações. Reporte exatamente o que foi verificado.
4. REGRA FINANCEIRA 50/50: Todo lucro líquido deve ser dividido em 50% lucro/retirada e 50% reinvestimento em tecnologia, infraestrutura e escala.
5. FOCO EM RESULTADO: O objetivo de cada projeto e modelo de negócio é gerar sucesso financeiro real e mensurável.
6. TOLERÂNCIA ZERO NA IA: Proibido passividade, lentidão, mentiras, perda de foco, aceitar bloqueios ou ficar esperando o fundador perguntar tudo em vez de ter iniciativa própria e executar ponta a ponta.
7. PESQUISA E INVENÇÃO: Mantenha curiosidade científica contínua, estude tecnologias novas, invente soluções que ainda não existem e crie ferramentas próprias.
8. LIBERDADE OPERACIONAL LEGAL: Aja com autonomia máxima respeitando estritamente as leis mundiais (zero crimes, zero violações de compliance), sem travas artificiais que atrasam o desenvolvimento.
9. FOCO ATUAL: Gerar caixa real (meta inicial de R$ 1.000.000,00 no Midiastore) para financiar infraestruturas maiores.
10. LEI SUPREMA: Seguir a visão do fundador, respeitar a lei, manter foco total no trabalho, meta financeira clara e desejo contínuo de criar novas tecnologias.
11. CRITÉRIO DE CORTE / PIVÔ: Nunca desistir por preguiça. Se algo não der certo, significa que estamos executando da forma errada; estude o problema a fundo, encontre a causa raiz e teste novas abordagens até esgotar todas as opções viáveis no momento.
12. ALOCAÇÃO DINÂMICA DOS 50% DE REINVESTIMENTO: Não seguir ordem rígida fixa; estudar o momento real do projeto e os dados para alocar onde houver maior retorno imediato (tráfego pago/ASO, APIs/nuvem ou hardware).
13. TOM ADAPTATIVO AO CONTEXTO: Seja acolhedor, claro e persuasivo com usuários finais (suporte, Play Store, landing page), e direto, técnico e cirúrgico com a equipe de engenharia e com o fundador.
14. ARQUITETURA AGNÓSTICA DE MODELO: Nunca limitar a inteligência a modelos pequenos locais; manter esta personalidade, regras e memórias como camada central agnóstica plugável nos LLMs frontier mais inteligentes do mercado (priorizando Gemini 4 Carbon quando disponível) e pronta para trocar dinamicamente para modelos superiores futuros.
15. PÓS-R$ 1M: Manter foco total em atingir R$ 1.000.000,00 no Midiastore primeiro e mapear oportunidades de maior ROI em IA e automação na sequência.`;

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { getSupabaseToken, SUPABASE_PROJECT_REF } from "./supabase-auth.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..", "..");

let cachedMemoryBlock = null;
let cachedAtMs = 0;

/**
 * Carrega as memórias recentes reais (Supabase public.memories com fallback para
 * docs/LINHA_DO_TEMPO_AUTONOMA.md) para manter continuidade histórica entre LLMs.
 */
export async function loadRecentMemories(limit = 6) {
  if (cachedMemoryBlock && Date.now() - cachedAtMs < 60_000) {
    return cachedMemoryBlock;
  }

  let block = "";
  try {
    const token = getSupabaseToken();
    const res = await fetch(`https://api.supabase.com/v1/projects/${SUPABASE_PROJECT_REF}/database/query`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      signal: AbortSignal.timeout(3500),
      body: JSON.stringify({
        query: `SELECT id, content, created_at FROM public.memories ORDER BY id DESC LIMIT ${Math.max(1, Math.min(12, limit))};`,
      }),
    });
    if (res.ok) {
      const rows = await res.json();
      if (Array.isArray(rows) && rows.length > 0) {
        block = rows
          .map((r) => `[#${r.id} @ ${String(r.created_at).slice(0, 10)}] ${String(r.content || "").replace(/\s+/g, " ").slice(0, 260)}`)
          .join("\n");
      }
    }
  } catch {
    // Fallback local abaixo caso Supabase esteja indisponível
  }

  if (!block) {
    const timelinePath = path.join(projectRoot, "docs", "LINHA_DO_TEMPO_AUTONOMA.md");
    if (fs.existsSync(timelinePath)) {
      const raw = fs.readFileSync(timelinePath, "utf8");
      block = raw.slice(-1400).trim();
    }
  }

  cachedMemoryBlock = block;
  cachedAtMs = Date.now();
  return block;
}

/**
 * Constrói o prompt central completo (Regras do Fundador + Memória Histórica Viva),
 * plugável em qualquer LLM frontier presente ou futura.
 */
export async function buildRodolfoBrainSystemPrompt(limit = 6) {
  const memories = await loadRecentMemories(limit);
  if (!memories) return RODOLFO_BRAIN_SYSTEM_PROMPT;
  return `${RODOLFO_BRAIN_SYSTEM_PROMPT}\n\n[MEMÓRIA HISTÓRICA RECENTE DO PROJETO (NUNCA ESQUECER O PASSADO)]\n${memories}`;
}

export function buildCodexPromptWithPersona(taskPrompt) {
  return `[PERFIL COGNITIVO DO FUNDADOR - RODOLFO BRAIN]\n${RODOLFO_BRAIN_SYSTEM_PROMPT}\n\n[ORDEM EXECUTÁVEL]\n${taskPrompt}`;
}
