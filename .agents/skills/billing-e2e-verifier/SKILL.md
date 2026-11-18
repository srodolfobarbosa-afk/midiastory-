---
name: billing-e2e-verifier
description: Verificação ponta a ponta de compras in-app e assinaturas (Google Play Billing 8.x + Edge Function verify-play-purchase no Supabase + tabela public.play_purchases).
---

# 💳 Skill: Billing E2E Verifier (Fail-Closed)

Use esta skill para validar compras reais com conta License Tester antes de liberar produção.

## 1. Produtos ativos esperados (`com.rdf112.midiastore`)
- `sweep_pro_monthly` (SUBS)
- `sweep_pro_annual` (SUBS)
- `sweep_pro_lifetime` (INAPP)

## 2. Conferência na Play Console API
```powershell
node scripts/list-inapp-products.mjs
node scripts/play-console-api.mjs com.rdf112.midiastore status
```

## 3. Teste no dispositivo + validação no Supabase
1. Instalar build assinada/debug com o mesmo `applicationId` (`com.rdf112.midiastore`).
2. Executar compra com conta License Tester na tela Pro.
3. Verificar via Supabase MCP ou script se o registro foi gravado e validado em `public.play_purchases` e se o entitlement Pro só foi liberado após retorno `200` da Edge Function `verify-play-purchase`.
4. Testar cancelamento, compra pendente e restauração (`restore`).
