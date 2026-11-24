# 🧠 Arquitetura Interna de uma IA Aplicada ao FRIDAY (Pilar 2)

> **Fonte estudada:** Vídeo *"O Que Acontece Dentro de uma Inteligência Artificial?"* (Canal Mundo da Ciência — `https://www.youtube.com/watch?v=s6FD7pLtvz0`) + repositórios open-source de referência no GitHub.
> **Aplicação direta:** Evolução do cérebro próprio **FRIDAY** (`Rodolfo Brain`), mantendo a arquitetura agnóstica de modelo (camada cognitiva própria conectada aos melhores LLMs frontier).

---

## 1. Como uma IA funciona por dentro → Como o FRIDAY aplica isso hoje

| Peça interna da IA (Vídeo) | O que acontece matematicamente | Onde isso já vive no nosso FRIDAY | Próxima evolução (Pilar 2) |
| :--- | :--- | :--- | :--- |
| **1. Tokens** (Cap. 3) | Texto é quebrado em subpalavras/IDs numéricos. | Orçamento de contexto e compactação de prompts nos hooks `.agents/hooks.json` e `scripts/lib/friday-attention-gate.mjs`. | Medir custo real de tokens por turno e manter o prompt sempre enxuto. |
| **2. Embeddings** (Cap. 3) | Cada token/frase vira um vetor de números em espaço semântico (ideias parecidas ficam próximas). | `scripts/lib/cognitive-memory.mjs` gera vetores reais de **384 dimensões** (`gemini-embedding-001`) e salva no PostgreSQL `pgvector` (`public.memories`). | Expandir cache LRU e cruzar embeddings de código + métricas de vendas. |
| **3. Atenção / Transformer** (Cap. 4) | `Attention(Q, K, V) = softmax((Q · Kᵀ) / √d) · V` — decide quais palavras/fatos têm mais peso para a resposta atual. | `scripts/lib/friday-attention-gate.mjs` aplica **Scaled Dot-Product Attention** + decaimento temporal + peso de autoridade (decisões do fundador) para escolher as memórias mais relevantes. | Integrar reranking de atenção diretamente no boot de memória de todas as sessões. |
| **4. Treinamento, Retropropagação e Descida do Gradiente** (Cap. 5) | Calcula o erro da previsão (`loss`) e ajusta milhões/bilhões de pesos passo a passo para diminuir o erro. | Como não treinamos pesos brutos do zero em GPU (blindagem financeira), aplicamos **Retropropagação Operacional**: cada correção do Rodolfo vira memória `categoria=correction` (confiança `0.96`) + regra no `AGENTS.md` + exemplo no `docs/datasets/rodolfo-brain-finetuning.jsonl`. | Quando o Midiastore bater a meta de caixa (50% reinvestimento), fazer fine-tuning real (LoRA) usando nosso dataset acumulado. |
| **5. Limites Reais: Alucinação, Janela de Contexto e Energia** (Cap. 6) | A IA prevê o próximo token provável (não "sabe" a verdade sozinha), tem memória de trabalho finita e consome muita GPU. | **Anti-Alucinação:** Lei 3 (Verdade Absoluta) + `SPECULATIVE_REGEX` + verificação obrigatória em ferramentas reais (ADB, Play API, Supabase). **Energia/Custo:** Roteamento por Peso (`flash` / `neon` / `codex.exe`). | Nunca responder métrica operacional sem checar fonte ao vivo (Regra Anti-Dado-Velho). |

---

## 2. Repositórios Reais no GitHub (Estudo e Base para Construção Futura)

Quando o Rodolfo quiser inspecionar ou construir partes de um modelo real do zero com código aberto (100% auditável):

1. **`karpathy/nanoGPT`** (`https://github.com/karpathy/nanoGPT`) — Implementação mínima e didática de um Transformer GPT completo (~300 linhas de treino + ~300 linhas de modelo).
2. **`karpathy/llm.c`** (`https://github.com/karpathy/llm.c`) — Treinamento de LLM em C/CUDA puro, mostrando a matemática real de atenção, backpropagation e gradiente sem caixas-pretas.
3. **`ggerganov/llama.cpp`** (`https://github.com/ggerganov/llama.cpp`) — Motor C++ leve para rodar modelos open-source localmente em CPU/GPU com quantização (ideal para futura independência).
4. **`huggingface/transformers`** (`https://github.com/huggingface/transformers`) — Biblioteca padrão da indústria com implementações oficiais de arquiteturas modernas.

5. **`morluto/rea`** (`https://github.com/morluto/rea`, `npx rea-agents`) — **REA: Reverse Engineer Anything** (475k+ stars, TypeScript, MIT). Padrão-ouro de **Evidence Ledger** (`retained-evidence` + `evidence_id` + `coverage` + `limitations` + `unknowns`) e inspeção de APKs Android (incluindo R8 `mapping.txt`) e apps JS/Web sem estourar a janela de contexto. Já integrado ao FRIDAY via skill `.agents/skills/reverse-engineer-anything/SKILL.md` e `createEvidenceRecord()` em `scripts/lib/friday-attention-gate.mjs`.

---

## 3. Princípio de Ouro da Arquitetura FRIDAY

> **O modelo frontier (Gemini 4, Codex, etc.) é o motor; o FRIDAY (memória vetorial `pgvector` + atenção contextual + regras do fundador + ferramentas reais + retropropagação de correções) é o cérebro permanente que nunca se perde quando trocamos de motor.**
