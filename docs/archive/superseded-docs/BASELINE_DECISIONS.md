# Decisões da base de desenvolvimento

## Origem e limite de fidelidade

O código-fonte histórico que gerou o AAB publicado não foi fornecido. Os arquivos disponíveis até aqui incluem AABs e reconstruções independentes. Este projeto é uma reconstrução funcional para desenvolvimento, não uma recuperação byte a byte do aplicativo de produção.

Foram comparadas duas bases reconstruídas disponíveis:

- A base `Sweep_Android_Foundation_API36` usa Android Views e contém integração MediaStore, análise por hash e fluxo inicial de Lixeira.
- A reconstrução `sweep-source-rebuild-v0(1)` contém módulo de domínio, contratos de paginação e testes JVM reportados, mas usa Compose e seu próprio relatório registra que a paginação ainda não estava ligada ao scanner/UI e que não havia duplicatas, Lixeira ou Billing implementados.

Como o AAB auditado indica uma Activity baseada em Views, esta edição conserva Views e aproveita as decisões técnicas úteis das auditorias sem afirmar que o código é o source histórico.

## Configuração da ferramenta

- AGP 8.13.2
- Kotlin Gradle Plugin 2.3.21
- Gradle 8.13
- JDK 17+
- compileSdk 36 / targetSdk 36
- minSdk 29, alinhado ao AAB auditado

A matriz oficial do Kotlin informa que o KGP 2.3.21 é plenamente compatível com Gradle 8.13 e AGP 8.13.2. O release notes do AGP 8.13.2 confirma R8 8.13.19, compatível com Kotlin 2.3, API máxima 36.1, Gradle 8.13 e JDK 17. A combinação foi selecionada por compatibilidade declarada, mas o build do projeto ainda não foi executado neste ambiente.

## Identidade de atualização (04/11/2026)

- Nome comercial da ficha: `Midiastore`.
- `applicationId`: `com.rdf112.midiastore` (confirmado pelo proprietário na ficha de teste interno da Google Play).
- Namespace de código: `com.rdf112.sweep` (legado técnico; não substituir sem migração planejada).
- `versionCode`: `17` no build.gradle.kts atual; ainda não confirmado como maior versionCode publicado.
- `versionName`: `2.2.1` no build.gradle.kts atual.
- `minSdk=29`, `targetSdk=36`, `compileSdk=36`.

O proprietário confirmou que a ficha do aplicativo de teste interno usa `com.rdf112.midiastore`. Antes de enviar AAB, verificar o maior versionCode efetivamente publicado, o certificado de upload aceito e a identidade do listing. Não gerar chave substituta.

## Escopo atual e limites de implementação

- BillingClient 8.0.0 está integrado no app, mas SKUs/ofertas reais, compra, restauração, assinatura e reembolso ainda não foram validados na Play Console.
- O cliente verifica presença de assinatura/JSON de compra, mas não valida criptograficamente o purchaseToken via backend confiável. O estado Pro não deve ser tratado como entitlement de produção.
- A branch `fix/p0-sweep-permissions-missions-billing` implementa varredura global cancelável por tipos, volumes e páginas, com limite de segurança; CI e testes completos ainda estão pendentes.
- Duplicatas só são confirmadas por SHA-256 não vazio e igual. Cobertura parcial impede pré-seleção automática de duplicatas.
- Limpeza/Lixeira usa as APIs MediaStore oficiais em Android 11+; o fluxo API 29 continua bloqueado.
- Similaridade visual, compressão, análise geral de documentos/Downloads via SAF, histórico persistente e tarefas agendadas não estão implementados.
- Não declarar equivalência binária com o AAB publicado: o código histórico de produção não foi recuperado.


## Desvio transparente da ordenação inicial v1.8

O estudo v1.8 sugeriu `DATE_MODIFIED DESC` com `_ID ASC`. A API estruturada `QUERY_ARG_SORT_COLUMNS`/`QUERY_ARG_SORT_DIRECTION` oferece uma única direção para a lista de colunas inteira; por isso este adaptador usa `DATE_MODIFIED DESC, _ID DESC`. A ordem permanece determinística e usa um desempate único para paginação, mas não reproduz a direção ascendente sugerida para o `_ID`. A alternativa seria usar `QUERY_ARG_SQL_SORT_ORDER` com cláusula constante, mas isso abandona a forma estruturada preferida pela documentação; não foi adotada sem evidência de provider real. O comportamento deve ser confirmado nos testes instrumentados.
