# 🏛️ Manifesto de Engenharia: A Arquitetura de IA Anti-Frágil do Midiastore
*(Como operamos 24/7 com inteligência de ponta a custo praticamente zero)*

---

## 🎯 Meta Fundamental: R$ 1.000.000,00 em Vendas Reais na Google Play

Esta arquitetura foi projetada para transformar a inteligência artificial de uma simples ferramenta de chat em um **Diretor de Tecnologia e Operações Autônomo** para o ecossistema Midiastore (`com.rdf112.midiastore`).

---

## 1. O Mistério do Erro 402 Desvendado (Por que o `max_tokens` salva o dia)

A maioria dos desenvolvedores comete o erro de chamar modelos como o Gemini 4 Argon ou Carbon sem definir o limite de saída (`max_tokens`). Veja o que acontecia nos bastidores:

1. **Sem limite definido:** O OpenRouter assume que o modelo pode gerar o teto máximo de saída (até 65.536 tokens).
2. **A Trava de Crédito:** O servidor calcula: *"Você tem saldo na conta para garantir o pagamento de 65.536 tokens agora caso o modelo use tudo?"*.
3. **O Aborto Preventivo:** Se você tiver apenas centavos ou uma conta com saldo baixo, o OpenRouter rejeita antes mesmo de iniciar a chamada com **HTTP 402 (Payment Required)**.
4. **A Nossa Correção:** Ao cravar `max_tokens: 1500` (ou `3000` em `scripts/equipe-ia.mjs:171`), a garantia de saldo exigida pela API despenca de dezenas de dólares para **frações de centavo ($0,002)**! A chamada passa instantaneamente e sem atrito.

### Implementação de Código em Produção:
* Em `scripts/equipe-ia.mjs`:
  ```javascript
  body: JSON.stringify({
    model: m,           // Rota para o Argon / Carbon
    max_tokens: 3000,   // Despenca a garantia exigida e passa liso
    messages: [{ role: "user", content: prompt }],
  })
  ```
* Em `scripts/ai-failover-router.mjs`:
  ```javascript
  body: JSON.stringify({
    model: modelId,
    max_tokens: maxTokens, // Calibrado em 1.500 com auto-adaptação dinâmica
    ...(modelId.includes("carbon") || modelId.includes("argon") ? { reasoning: { effort: "medium" } } : {}),
    messages: [{ role: "user", content: prompt }],
  })
  ```

---

## 2. Calibragem de Raciocínio no Gemini 4 Carbon (Reasoning Effort)

O Gemini 4 Carbon trouxe um dos recursos mais avançados de IA moderna: em vez de gastar processamento máximo em perguntas simples ou faltar raciocínio em bugs difíceis, nós calibramos o esforço em tempo de execução via API:

```javascript
// Implementado em scripts/ai-failover-router.mjs (linhas 72 e 99)
...(modelId.includes("carbon") || modelId.includes("argon")
    ? { reasoning: { effort: "medium" } }
    : {})
```

### Os 3 Níveis de Profundidade:
* 🟢 **`effort: "low"` (Baixo):** Ideal para checagens de rotina, formatação de JSON, queries SQL rápidas e consultas de status no Supabase. Custo mínimo e latência ultrabaixa (< 800ms).
* 🟡 **`effort: "medium"` (Nosso Padrão Atual):** O ponto de equilíbrio ideal. O modelo reflete sobre a arquitetura do Android (MediaStore, Scoped Storage, permissões) antes de gerar o código, sem desperdiçar tempo.
* 🔴 **`effort: "high"` (Extremo):** Raciocínio profundo para resolução de concorrência, auditorias criptográficas de billing e refatorações estruturais em Kotlin.

> *Essa precisão de parâmetros é o que permite operar com inteligência de ponta a custo praticamente zero.*

---

## 3. As Três Bases de Sustentação para a Meta de R$ 1.000.000,00

### Pilar 1: Coordenação de Agentes e `/swarm` no Antigravity
Um modelo generalista se perde quando vários agentes tentam editar código ao mesmo tempo. Com uma janela de contexto massiva (1 milhão de tokens) e raciocínio calibrado, ele assume o papel de **Coordenador Central**:
* 🤖 **Worker 1 (Android / Kotlin):** Focado em refatorar DataSources do MediaStore e manter conformidade estrita com Scoped Storage (zero `MANAGE_EXTERNAL_STORAGE`).
* 🤖 **Worker 2 (QA & CI):** Validando testes unitários e instrumentados (`./gradlew testDebugUnitTest`).
* 🤖 **Worker 3 (Growth & Funil):** Monitorando o cadastro dos 20 testadores no Supabase e publicando atualizações no Firebase Hosting.
* 🧠 **Memória Sem Perda de Fio da Meada:** Ingestão de todo o repositório, documentação do Google Play Console e histórico de memórias de uma só vez, **sem alucinação de escopo**.

### Pilar 2: Auditoria e Due Diligence Financeira (Blindagem contra Bloqueios)
No ecossistema da Google Play, qualquer erro sutil de faturamento ou permissão pode causar suspensão de conta. É aqui que a verificação rigorosa se paga:
* 🛡️ **Auditoria do Google Play Billing v7:** Garante que tokens de compra sejam validados criptograficamente antes de liberar o plano Pro (conforme exigido em `AGENTS.md:110`).
* 📋 **Due Diligence de Políticas da Google:** Cruzamento do manifesto Android com as diretrizes de privacidade e Data Safety do Google Play Console antes de gerar qualquer novo AAB de produção.
* 💰 **Proteção Orçamentária:** Zero desperdício de créditos, acionando raciocínio profundo apenas onde há risco financeiro ou de aprovação de app.

### Pilar 3: Criação de Entregáveis Profissionais e Governança Corporativa
Produz apresentações de slides, planilhas estruturadas e relatórios complexos prontos para uso corporativo, servindo como a camada de comunicação executiva do projeto:
* 📄 **Questionários e Relatórios Oficiais para a Play Store:** Geração automatizada das respostas de produção e conformidade em `docs/campaigns/respostas-formulario-producao-google-play.md`.
* 📊 **Matrizes de Release e Rastreabilidade:** Documentação técnica de blockers e features em `docs/RELEASE_BLOCKERS.md` e `docs/FEATURE_IMPLEMENTATION_MATRIX.md`.
* 📈 **Modelagem de Coorte dos 20 Testadores & LTV:** Estruturação de dados de retenção, taxas de conversão do funil web em 8 idiomas e acompanhamento da meta de R$ 1.000.000,00.

---

> **Veredito:** *Essa combinação transforma a IA de uma simples ferramenta de chat em um diretor de tecnologia e operações autônomo para o Midiastore.*
