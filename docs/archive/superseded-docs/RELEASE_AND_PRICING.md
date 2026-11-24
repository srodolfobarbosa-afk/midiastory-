# Midiastore — release e monetização

## Identidade no código atual

- Nome comercial: Midiastore.
- `applicationId`: `com.rdf112.midiastore` (confirmado pelo proprietário na ficha do teste interno).
- Namespace Kotlin/Android resources: `com.rdf112.sweep` (legado técnico).
- `versionCode=17`, `versionName=2.2.1` no build atual; confirmar o maior versionCode publicado na Play Console.
- `minSdk=29`, `targetSdk=36`, `compileSdk=36`.
- Billing Library: `8.0.0`.

## Estado de venda

O proprietário informa que a configuração financeira/recebimento da conta foi habilitada em 03/11/2026. Isso é distinto de configurar e testar produtos de assinatura/compra única dentro da Play Console.

O código consulta estes IDs:

- `sweep_pro_monthly` — subscription (`SUBS`)
- `sweep_pro_annual` — subscription (`SUBS`)
- `sweep_pro_lifetime` — one-time (`INAPP`)

**Não confirmado:** existência/ativação dos três produtos, ofertas-base, offer tokens, preços reais, disponibilidade no Brasil, conta de license tester, compra real, restore e ciclo de assinatura.

O app atualmente verifica a presença de campos de assinatura/JSON do objeto de compra. Isso não é validação criptográfica do purchaseToken por um backend confiável. Não conceder benefícios de produção com base em uma suposição de entitlement local.

## Estado técnico

A branch `fix/p0-sweep-permissions-missions-billing` (PR #3) inclui correções para permissões parciais, retomada de missão após a primeira leitura, feedback de Billing, proteção SHA-256, accounting de lixeira/exclusão, marca Midiastore e missão global paginada por tipos/volumes.

A missão global tem limites de segurança e deve apresentar cobertura parcial quando algum provider, permissão, revisão de sessão ou limite impedir uma prova completa. Duplicatas não são pré-selecionadas se a cobertura não for completa.

## Ordem de entrega

1. Obter CI verde no commit mais recente, incluindo testes unitários e instrumentados.
2. Executar MediaStore, operações de limpeza e permissões em aparelhos físicos.
3. Resolver API 29 ou reavaliar minSdk.
4. Confirmar os SKUs/ofertas na Play Console e implementar validação confiável de purchaseToken.
5. Testar compra, pending, cancelamento, restauração, acknowledgement, reembolso e expiração com license testers.
6. Confirmar chave de upload legítima, maior versionCode e manifesto.
7. Validar privacidade, Data Safety, acessibilidade, desempenho e UX.
8. Gerar AAB assinado, inspecionar e distribuir em teste interno/fechado.
9. Só depois avaliar rollout de produção e aquisição de usuários.

## Preço

Os documentos de benchmark são hipóteses comerciais, não preço ativo. O aplicativo deve exibir apenas valores retornados dinamicamente por ProductDetails da Google Play. Não hardcode preços, descontos, moeda ou meios de pagamento.