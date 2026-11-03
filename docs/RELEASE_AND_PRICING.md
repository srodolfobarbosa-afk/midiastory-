# Sweep — release e monetização

## Estado da base v0.3

- `applicationId`: `com.rdf112.sweep`
- `versionCode`: `9` candidato, ainda sujeito à confirmação da Play Console
- `versionName`: `1.1.1` candidato
- `minSdk`: 29
- `targetSdk`: 36
- BillingClient / compras / entitlement: não integrado
- Exportação de resumo Pro: não implementada nesta reconstrução
- Lixeira/restore: fora da fatia v0.3; não há ação destrutiva nesta interface
- AAB Release: bloqueado até validar código, Billing, testes e JKS correto

## Continuidade dos usuários existentes

A auditoria do beta identificou Billing Library 8.0.0, novas vendas descritas como desabilitadas e exportação de resumo como benefício Pro. Os IDs reais de produto, purchase type, entitlement, compra/restore e estado da assinatura ainda precisam ser recuperados da fonte histórica ou Play Console. Os IDs usados nos testes da política são exemplos fictícios e nunca devem ser cadastrados no Play Console.

O possível bug de estado registrado na auditoria precisa ser comparado com o código original: `PURCHASED=1`, `PENDING=2` e `UNSPECIFIED=0`. Estados desconhecidos e falha de verificação não concedem novas compras. Antes de qualquer novo paywall, reimplementar e testar compra, reconhecimento, consulta em reconexão/onResume e restauração do Pro existente.

## Ordem de entrega

1. Concluir v0.3 somente leitura: build, JUnit, Lint, MediaStore instrumentado e dispositivos.
2. Recuperar a especificação do Billing e exportação Pro existentes; preservar entitlement já comprado.
3. Implementar Lixeira e recuperação em fatia independente após a validação da seleção/revalidação.
4. Validar assinatura de upload, versionCode, manifesto/AAB e Play Console.
5. Somente depois desenhar planos Free/Pro e experimentar preços localizados.

## Preço

Não há preço ou catálogo aprovado/codificado nesta revisão. Qualquer faixa de preço discutida em planejamento é uma hipótese comercial, não configuração ativa do app.
