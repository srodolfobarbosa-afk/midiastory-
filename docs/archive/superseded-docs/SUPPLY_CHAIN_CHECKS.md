# Verificações de cadeia de suprimentos

- Distribuição configurada: Gradle 8.13 (`gradle/wrapper/gradle-wrapper.properties`).
- SHA-256 da distribuição `gradle-8.13-bin.zip`: `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`, configurado em `distributionSha256Sum`.
- SHA-256 do `gradle-wrapper.jar` incluído: `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`. Esse hash corresponde ao Wrapper publicado para Gradle 9.8.2, **não** ao Wrapper 8.13. Portanto, o arquivo JAR é reconhecido como uma distribuição oficial, mas a correspondência exata entre JAR e distribuição configurada ainda não foi fechada.
- Antes de considerar o ambiente reproduzível, regenere/atualize o Wrapper com Gradle 8.13 (`gradle wrapper --gradle-version 8.13 --distribution-type bin`) em um ambiente confiável e confirme o hash do JAR no catálogo oficial. Não trate a divergência como validada.
- Não são incluídos `keystore.properties`, JKS, tokens, service-account keys ou `local.properties`.
- Dependências Gradle são declaradas com versões fixas nos arquivos de build. A execução de build deve ser feita com repositórios oficiais e rede TLS válida.

Fontes oficiais: [Gradle checksums](https://gradle.org/release-checksums/) e [Gradle Wrapper security guidance](https://docs.gradle.org/current/userguide/gradle_wrapper.html).
