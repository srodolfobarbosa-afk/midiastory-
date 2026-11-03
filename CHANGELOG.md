# Changelog

## v2.1.0 — auditoria VC13 (NÃO PRONTO PARA PUBLICAÇÃO)

- Conecta contratos de página/sessão do módulo `:domain` a um adaptador Android `ContentResolver`.
- Usa `QUERY_ARG_LIMIT`, `QUERY_ARG_OFFSET`, ordenação estável, filtro fixo de itens pendentes/na Lixeira e `CancellationSignal`.
- Confere limite, offset e ordenação em `EXTRA_HONORED_ARGS`; se não houver confirmação completa, consulta sem paginação e aplica offsets localmente.
- Adiciona verificação limitada de páginas sobrepostas e offsets anteriormente vistos com conteúdo alterado.
- Revalida permissões, conjunto de volumes e versões/gerações antes e depois de publicar cada página. Revisão indisponível é tratada como não verificável.
- Sinaliza explicitamente acesso parcial e revisão não verificável.
- Adiciona `MediaScanViewModel` com `StateFlow`, navegação incremental e cancelamento sem permitir que uma requisição substituída sobrescreva o estado mais recente.
- Mantém no máximo 100 linhas na página visível, 500 URIs em revisão, 50 offsets no histórico e quatro decodificações de miniatura concorrentes.
- Exige SHA-256 igual e não vazio para confirmar duplicatas; falha de hashing não promove arquivos a duplicatas.
- Recalcula hashes em fluxo e de forma cancelável, além de revalidar conteúdo antes de exclusão quando o registro tem hash.
- Mantém operações via consentimento oficial do Android e não contabiliza itens na Lixeira como espaço liberado.
- Só contabiliza bytes autorizados após aceite do diálogo; só credita exclusão após aceite e confirmação de ausência dos itens.
- Integra BillingClient e SKUs configurados; estado inicial e falha de consulta são exibidos como reconciliação, não como plano Gratuito.
- Corrige estados fictícios no medidor de armazenamento e renomeia o comando que apenas inicia análise.

### Verificação executada em 03/11/2026

- Baseline `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` e `:app:assembleDebugAndroidTest`: `BUILD SUCCESSFUL`, JDK 17, Gradle 8.13, SDK 36, 82 tarefas.
- 58 testes de domínio e 7 testes JVM do app passaram; Lint completou com avisos; APK Debug e APK instrumentado foram gerados.
- O rótulo e textos comerciais visíveis foram atualizados para Midiastore/Midiastore Pro mantendo applicationId/namespace e SKUs legados. A marca e a substituição dos símbolos de permissões foram alteradas depois do build; precisam de nova compilação.
- Release/AAB, `connectedDebugAndroidTest`, compra real, MediaStore em aparelhos e hash de artefatos continuam não verificados.
- Backup pré-alteração: `/workspaces/midiastore-pre-2027-audit-2026-11-03.tar.gz`, SHA-256 `3447fb294a9748297c918438dc7c0d328a4f904a27141867afa4c0d5bb11dfdb`.

Este changelog descreve o código-fonte e as verificações desta sessão; não comprova compatibilidade OEM nem aprovação/publicação na Google Play.
