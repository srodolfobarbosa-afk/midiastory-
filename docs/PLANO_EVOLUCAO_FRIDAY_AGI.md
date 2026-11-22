# 🧠 PLANO MESTRE DE AUTO-EVOLUÇÃO DA FRIDAY CORE (RUMO À AGI OPERACIONAL)

**Proprietário:** Rodolfo  
**Arquitetura:** Agnóstica de Modelo | Stack 100% Node.js/TypeScript + Kotlin + CLIs Nativas (**Zero Python**) | Blindagem Financeira (Custo Extra R$ 0 até Faturar -> Regra 50/50)  
**Última Sonda Ao Vivo:** 2026-11-21 (`node friday-core/cli.mjs probe`)

---

## 1. Diagnóstico Real Ao Vivo (Onde Estamos Hoje)

- **Google AI Studio (65 modelos na chave):**
  - ✅ **Ativos GRÁTIS (`HTTP 200` testado):** `gemini-4-hydrogen` (1M contexto), `gemini-3.5-transcribe` (STT dedicado), `gemini-2.5-flash`, `gemini-3.8-flash`, `gemini-embedding-001`, `gemini-embedding-2` (8.192 tokens = 4x contexto).
  - 🔒 **Exigem Billing Pago (`HTTP 429` na chave grátis):** `gemini-4-carbon`, `gemini-4-neon`, `gemini-4-argon`, `deep-research-preview-04-2026`, `google_search` grounding via API. *(Nota: no app Antigravity Pro usamos Gemini 4 Neon/Carbon conforme disponível no plano.)*
- **Memória Cognitiva Supabase (`19 MB / 500 MB`):**
  - `446` memórias com `0` embeddings faltantes (`100%` indexadas).
  - **Gargalo real encontrado:** o Grafo de Conhecimento tem apenas `14` nós (`kg_nodes`) e `56` arestas (`kg_edges`) para `446` memórias — subaproveitado!
- **FRIDAY Core v2.0:** `18/18` motores JS válidos, `5` hooks nativos, `12` poderes na Biblioteca Viva, `6` vacinas anti-regressão, `4` especialistas fixos.

---

## 2. Roadmap de Evolução em 4 Fases (Autônomo e Executável)

### ✅ FASE 1 — Turbo Imediato Custo Zero (CONCLUÍDA HOJE)
1. **Sonda Viva de Modelos & Memória** (`friday-core/capabilities-probe.mjs`) — monitora ao vivo Google AI Studio + Supabase.
2. **Conselho Adversarial promovido para `gemini-4-carbon -> gemini-4-hydrogen` (1M ctx)** — assim que houver billing, sobe automaticamente para Carbon sem mexer em 1 linha de código.
3. **STT promovido para `gemini-3.5-transcribe`** (modelo dedicado de voz do Google, `HTTP 200` grátis) + Glossário Fonético do Fundador.

### 🚀 FASE 2 — Salto de Memória, Bibliotecas & Código (Próxima Rodada — Custo R$ 0)
*(Baseado na auditoria cirúrgica do subagente `research` em `2026-11-21`)*
1. **SDK Oficial `@google/genai` + `valibot` + `postgres` (Node.js leves, zero Python):** substituir `fetch` manuais dispersos e `parseJsonSafe` artesanal por cliente tipado, adicionando `AbortSignal.timeout(20000)` no `callOpenRouter` (`scripts/ai-failover-router.mjs:166`) e unificando tudo atrás do `ProviderCircuitBreaker`.
2. **Turbo na Memória Supabase (`scripts/lib/cognitive-memory.mjs`):**
   - Trocar Management API (`api.supabase.com`, que dá 429) por PostgREST/pooler no runtime;
   - Adicionar **Cache LRU local de embeddings** (últimos 200 hashes -> elimina chamadas repetidas no `PreInvocation` + `Stop`) e **batching de embeddings** (`computeBatchEmbeddings`) no `Stop` hook (corte de ~50-60% na latência);
   - Enriquecer o Grafo de Conhecimento (`kg_nodes` 14 → 300+) e limpar código morto (`linhas 369-391`).
3. **Proteção Extrema de RAM (3,93 GB) nos Hooks (`cognitive-hook.mjs:62` & `friday-quota-guard.mjs:36`):** trocar `readFileSync` integral do `transcript.jsonl` por **Tail-Reader de buffer fixo (< 256 KB)** + escrita atômica de estado (`tmp` + `rename`).
4. **Sincronizar Memória Imune (`friday-core/immune-memory.mjs`) com Supabase** + paralelizar `friday-hud.mjs` e `friday-self-audit.mjs` com `Promise.allSettled` + suíte nativa `node --test`.

### ⚡ FASE 3 — Tempo Real & Olhos/Mãos Avançados (Custo R$ 0)
1. **Voz Bidirecional Ultra-Baixa Latência (`gemini-3.8-live` / `gemini-3.5-transcribe-live`):** estudar migração do fluxo gravar-MP3 → transcrever → falar para WebSocket `bidiGenerateContent` (latência < 500 ms, estilo conversa humana real).
2. **QA Visual Autônomo Completo (Playwright + ADB + `gemini-2.5-computer-use`):** abrir o app no Motorola ou site no Chrome headless, navegar clicando sozinha e gerar relatório de defeitos antes do humano tocar.

### 💎 FASE 4 — Modo Superinteligência com Caixa Real (Regra 50/50 do Midiastore)
*Gatilho: primeiros R$ 5.000 de lucro líquido do Midiastore (50% = R$ 2.500 para reinvestimento).*
1. **Ativar Billing no Google AI Studio:** destrava imediatamente na API `gemini-4-carbon`, `deep-research-max-preview-04-2026` e `google_search` grounding nativo (código já preparado com failover automático!).
2. **VPS Linux 24/7 (8–16 GB RAM):** tira todo o peso do notebook de 3,93 GB de RAM e deixa a FRIDAY rodando ciclos autônomos de engenharia, ASO, monitoramento e vendas 24 horas por dia.
