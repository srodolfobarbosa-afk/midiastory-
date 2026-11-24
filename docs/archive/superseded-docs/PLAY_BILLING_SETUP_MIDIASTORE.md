# Configuração e teste de monetização — Midiastore

Atualizado: 04/11/2026

Este roteiro separa o perfil financeiro do Google Play dos produtos de faturamento dentro do aplicativo.

**O proprietário informou que habilitou o perfil financeiro/recebimento em 03/11/2026.** Isso não comprova que os produtos in-app abaixo existam, estejam ativos, tenham ofertas válidas ou tenham sido comprados com sucesso.

## Identidade obrigatória

- Aplicativo no Play Console: Midiastore.
- Package: `com.rdf112.midiastore`.
- O package deve corresponder ao `applicationId` no AAB. Não trocar por `com.rdf112.sweep`.
- O source atual configura versionCode 17/versionName 2.2.1; verificar o maior versionCode já publicado antes do envio.

## Catálogo atualmente esperado pelo código

| Nome comercial sugerido | Product ID | Tipo no Billing | Observação |
|---|---|---|---|
| Midiastore Pro Mensal | `sweep_pro_monthly` | Subscription / SUBS | Criar e ativar um base plan mensal |
| Midiastore Pro Anual | `sweep_pro_annual` | Subscription / SUBS | Criar e ativar um base plan anual |
| Midiastore Pro Vitalício | `sweep_pro_lifetime` | One-time product / INAPP | Criar e ativar produto de compra única |

Esses IDs são lidos pelo source. A existência e ativação no Play Console ainda não foram verificadas. Não inventar novos IDs nem trocar purchase type sem atualizar o código, testes e migração de compradores existentes.

## Checklist no Play Console

1. Abrir o app **Midiastore** com package `com.rdf112.midiastore`.
2. Entrar em **Monetize with Play / Monetização** e abrir a área de produtos.
3. Conferir se cada Product ID da tabela existe exatamente, sem diferença de maiúsculas, underscores ou caracteres.
4. Para mensal e anual, conferir o tipo Subscription, base plan correspondente, billing period, países/regions, preço e status Active.
5. Para vitalício, conferir One-time product, disponibilidade, preço e status Active.
6. Conferir que os produtos não estão em rascunho/inativos e que estão disponíveis no país da conta de teste.
7. Conferir as ofertas e eligibility. O cliente atual escolhe o primeiro offerToken retornado; se houver várias ofertas, isso precisa ser revisado antes de lançamento.
8. Não preencher preço sugerido de benchmark automaticamente. Definir preços e moeda diretamente no Play Console; o app deve exibir o `formattedPrice` retornado pelo Google Play.
9. Adicionar e-mails de teste em License testing e garantir que a conta de teste participa da faixa de teste interno/fechado.
10. Publicar a alteração do catálogo na faixa de teste necessária e aguardar a propagação antes do teste.

## Teste em telefone

O teste de compra deve usar o aplicativo instalado pela Google Play na faixa de teste, com o package e a assinatura aceitos. Um APK Debug instalado por ADB não prova que os produtos reais do listing estão disponíveis.

1. Entrar na Play Store do aparelho com a conta de license tester.
2. Instalar ou atualizar Midiastore pelo link de teste interno.
3. Abrir Planos Pro e aguardar a consulta de ProductDetails.
4. Confirmar que os três produtos aparecem com preços reais e corretos para o país/moeda.
5. Executar compra de teste mensal, anual e vitalícia em contas apropriadas.
6. Registrar cada estado: comprado, pendente, cancelado, já possuído, consulta indisponível e produto não encontrado.
7. Reabrir o app, usar Restaurar compras e testar reconexão.
8. Para subscriptions, validar renovação, cancelamento, expiração, reembolso e troca de plano com ferramentas oficiais de teste.
9. Confirmar que o entitlement só é liberado após validação e que compras pendentes não liberam Pro.

## Segurança necessária antes de venda em produção

O código atual consulta purchases e verifica que os campos de assinatura/JSON existem, mas isso não equivale a validação criptográfica do `purchaseToken` nem a uma fonte de verdade segura para entitlements.

Antes de vender Pro em produção:

- Implementar um backend confiável.
- Enviar o purchaseToken do cliente ao backend por HTTPS.
- Validar o token com a Google Play Developer API: `purchases.products:get` para compra única e `purchases.subscriptionsv2:get` para assinaturas.
- Persistir purchaseTokens já processados para impedir replay/duplicação.
- Tratar estado `PENDING` como sem entitlement.
- Fazer acknowledgement de forma idempotente e no fluxo recomendado pelo Google.
- Reconciliar renovação, cancelamento, expiração, reembolso e revogação.
- Não colocar service-account JSON, private key ou credenciais Google dentro do APK, GitHub, ZIP ou strings do aplicativo.

Documentação oficial:
- Verificar compras e fraude: https://developer.android.com/google/play/billing/security
- Testar Play Billing: https://developer.android.com/google/play/billing/test
- API Google Play Developer: https://developers.google.com/android-publisher/api-ref/rest

## Critério de aceite

O Billing só pode ser marcado como pronto quando houver:
- IDs/tipos/ofertas confirmados no Play Console;
- preços reais retornados pelo Google Play;
- compra de teste concluída em listing interno/fechado;
- pending/cancel/restore/acknowledgement e ciclo de assinatura verificados;
- token validado por backend confiável;
- entitlement preservado para compradores existentes;
- evidência (logs/prints) ligada ao commit e à versão testada.

A ativação do perfil financeiro, sozinha, não fecha nenhum desses critérios.
