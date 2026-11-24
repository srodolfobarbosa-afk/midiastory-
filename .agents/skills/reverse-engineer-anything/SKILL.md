---
name: reverse-engineer-anything
description: Metodologia oficial inspirada no morluto/rea (REA - Reverse Engineer Anything) adaptada para o FRIDAY: investigação baseada em evidências verificáveis, Evidence Ledger com coverage/limitations/unknowns, auditoria de APKs Android (R8/mapping.txt) e apps Web/JS sob demanda via `npx rea-agents`.
---

# 🔬 REA — Reverse Engineer Anything (Adaptado ao FRIDAY)

Baseado no repositório oficial [`morluto/rea`](https://github.com/morluto/rea) (`npx rea-agents`).

## Quando usar
- Quando uma afirmação depende de **artefato compilado** (APK/AAB release do Midiastore, `mapping.txt` do R8, binário nativo, bundle JS/Web ou comportamento em runtime) que não pode ser provado apenas lendo o código-fonte.
- **Não usar** para leitura normal de código-fonte já disponível no repositório (evita overhead e preserva os 3,93 GB de RAM do PC).

## 4 Regras de Ouro da Metodologia REA aplicadas no FRIDAY
1. **Evidence Ledger (`evidence_id`):** Nunca colar dumps gigantes na janela de contexto. Guardar o dado completo em evidência retida (`scripts/lib/friday-attention-gate.mjs` -> `createEvidenceRecord`) e trabalhar com resumos (`detail: "summary"`) citando o `evidence_id`.
2. **Proveniência + Cobertura + Limitações + Incógnitas:** Toda conclusão técnica do FRIDAY deve separar:
   - `observed` (o que foi realmente observado na ferramenta),
   - `coverage` (o que foi coberto vs. o que ficou de fora),
   - `limitations` (limites da ferramenta/ambiente),
   - `unknowns` (o que ainda não foi provado — **nunca completar lacunas com chute**).
3. **Android Release Audit (`APK` + `mapping.txt`):** Ao auditar builds R8/ProGuard do Midiastore, usar o caminho absoluto do `mapping.txt` para verificar símbolos `mapped` / `ambiguous` / `unmapped` e validar que nenhuma classe crítica (Billing, MediaStore) foi quebrada pelo R8.
4. **Conexão sob demanda (Zero RAM desperdiçada):** Não manter processos pesados do REA/JADX abertos em idle. Rodar CLI/MCP sob demanda via `npx rea-agents` apenas quando a missão exigir inspeção de binário/APK/JS.
