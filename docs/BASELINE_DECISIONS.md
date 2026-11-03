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

## Identidade de atualização

- `applicationId`: `com.rdf112.sweep`
- `versionCode`: `9` candidato, baseado no último valor `8` informado pelo proprietário
- `versionName`: `1.1.1` candidato

Antes de qualquer envio, conferir novamente pacote, maior versionCode e certificado na Play Console.

## Funcionalidades deliberadamente fora desta entrega

Google Play Billing, entitlement, restauração de compra, assinaturas, imagens visualmente semelhantes, compressão, análise de documentos/Downloads, banco de histórico e tarefas agendadas não são simulados nem apresentados como implementados.


## Desvio transparente da ordenação inicial v1.8

O estudo v1.8 sugeriu `DATE_MODIFIED DESC` com `_ID ASC`. A API estruturada `QUERY_ARG_SORT_COLUMNS`/`QUERY_ARG_SORT_DIRECTION` oferece uma única direção para a lista de colunas inteira; por isso este adaptador usa `DATE_MODIFIED DESC, _ID DESC`. A ordem permanece determinística e usa um desempate único para paginação, mas não reproduz a direção ascendente sugerida para o `_ID`. A alternativa seria usar `QUERY_ARG_SQL_SORT_ORDER` com cláusula constante, mas isso abandona a forma estruturada preferida pela documentação; não foi adotada sem evidência de provider real. O comportamento deve ser confirmado nos testes instrumentados.
