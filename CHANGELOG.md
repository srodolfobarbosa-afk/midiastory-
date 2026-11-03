# Changelog

## v0.3 — integração de paginação incremental (candidato de desenvolvimento para 1.1.1)

- Conecta contratos de página/sessão do módulo `:domain` a um adaptador Android `ContentResolver`.
- Usa `QUERY_ARG_LIMIT`, `QUERY_ARG_OFFSET`, ordenação estável, filtro fixo de itens pendentes/na Lixeira e `CancellationSignal`.
- Exige confirmação do provedor por `EXTRA_HONORED_ARGS` e `EXTRA_TOTAL_COUNT`; `-1` é aceito como total desconhecido, ausência do extra ou argumentos não honrados bloqueiam a paginação.
- Adiciona verificação limitada de páginas sobrepostas e offsets anteriormente vistos com conteúdo alterado.
- Revalida permissões, conjunto de volumes e versões/gerações antes e depois de publicar cada página. Revisão indisponível é tratada como não verificável.
- Sinaliza explicitamente acesso parcial e revisão não verificável.
- Adiciona `MediaScanViewModel` com `StateFlow`, navegação incremental e cancelamento sem permitir que uma requisição substituída sobrescreva o estado mais recente.
- Mantém no máximo 100 linhas na página visível, 500 URIs em revisão, 50 offsets no histórico e quatro decodificações de miniatura concorrentes.
- Mantém o fluxo somente leitura: remove do código ativo as antigas classes de agrupamento/Lixeira até que o scanner paginado e a seleção sejam validados conforme a planta v1.9.
- Adiciona política pura de interpretação Billing fail-closed e testes de unidade, sem conectar BillingClient nem inventar IDs do Play Console.
- Adiciona matriz de implementação v1.9, critérios de aceite, bloqueios de release e relatório de validação atualizado.

### Verificação executada nesta sessão

- Compilação do código puro Kotlin do módulo `:domain` com `kotlinc`: aprovada.
- Teste manual de smoke das regras de domínio: será reportado separadamente no relatório de validação.
- `bash -n` do script de verificação, análise XML e teste de integridade do ZIP: pendentes de fechamento.
- Gradle/JUnit, compilação Android, Lint, APK, teste instrumentado, MediaStore real, aparelho e AAB: não executados nesta sessão.

Este changelog descreve o código-fonte do incremento, não comprova release ou publicação.
