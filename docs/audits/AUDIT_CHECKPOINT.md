# Checkpoint da Auditoria (retomável por qualquer agente/sessão)

> Regra: antes de começar, leia este arquivo. Ao terminar cada item, marque `[x]`, anote evidência curta e salve.
> Se a cota acabar no meio, a próxima sessão continua do primeiro item `[ ]`.

Iniciado: 2026-11-11 15:12 BRT — Claude Opus 5.5 (Antigravity)
Fábrica 24h: PARADA durante a auditoria (fazia `git add docs/ && push` e deploy automático a cada ciclo).

## P0 — Segurança
- [ ] S1. Fechar cadeia de execução remota: chat público (terminal.html + anon key) → `orchestrator_tasks` → fábrica → `codex.exe --dangerously-bypass-approvals-and-sandbox`
- [ ] S2. RLS: remover leitura pública de `beta_testers` (e-mails expostos) e escrita anon em `orchestrator_*`
- [ ] S3. Remover token `sbp_...` hardcoded de todos os scripts (helper único lendo env/mcp_config)
- [ ] S4. DONO: revogar o token `sbp_v0_d61b...` no painel Supabase (está no histórico de um repo PÚBLICO) e gerar novo

## P1 — Correções
- [ ] C1. Trindade sem texto simulado (fallbacks fixos → erro explícito)
- [ ] C2. Chat real no terminal.html via Edge Function autenticada por chave do dono
- [ ] C3. Regra de 12 testadores (docs/scripts que dizem 20)
- [ ] C4. Preços oficiais R$ 14,99 / 79,99 / 149,99 — verificação final
- [ ] C5. CI Android verde no commit enviado
- [ ] C6. Fábrica: sem push/deploy automático por padrão (flags opt-in)

## P2
- [ ] I1. Inovação para atrair testadores/compradores reais (sem promessas falsas)
- [ ] M1. Registrar memória + conversa no Supabase
