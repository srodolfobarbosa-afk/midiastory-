# Relatório de validação — Midiastore 2.1.0 / VC13

Data: 03/11/2026

Backup pré-alterações: `/workspaces/midiastore-pre-2027-audit-2026-11-03.tar.gz` (595 entries); SHA-256 `3447fb294a9748297c918438dc7c0d328a4f904a27141867afa4c0d5bb11dfdb`.

## Evidências concluídas nesta sessão

| Verificação | Resultado | Escopo |
|---|---|---|
| `:domain:test` | Aprovada | 58 testes; 0 falhas/erros/ignorados |
| `:app:testDebugUnitTest` | Aprovada | 7 testes; 0 falhas/erros |
| `:app:lintDebug` | Concluída com warnings | Referências inlined e versões de dependências sinalizadas; não bloqueou o build |
| `:app:assembleDebug` | Aprovada | APK Debug produzido |
| `:app:assembleDebugAndroidTest` | Aprovada | APK instrumentado produzido; não significa execução em dispositivo |
| Diagnósticos do editor nos arquivos tocados | Nenhum reportado | Não substitui build/execução Android |

## Testes presentes no repositório

- `:domain`: 58 testes executados e aprovados.
- `app`: 7 testes JVM executados e aprovados; 4 testes instrumentados compilados, mas não executados em dispositivo.

Baseline executada em Codespace Ubuntu 24.04 em 03/11/2026 com Gradle 8.13, AGP 8.13.2, Kotlin 2.3.21, JDK 17.0.17, Android SDK Platform 36 e Build Tools 35.0.0. O SDK foi instalado em `/tmp/tmp.Pnk13LDOXN/android-sdk`; o command-line tools baixado foi verificado com SHA-1 publicado. O build completo terminou `BUILD SUCCESSFUL` em 5m20s, 82 tarefas. Foi emitido warning de SDK XML v4 não reconhecido integralmente pelo parser utilizado.

## Não executado / não aprovado

- `:app:compileReleaseKotlin`, `:app:lintRelease`, `:app:assembleRelease` e `:app:bundleRelease`.
- `:app:connectedDebugAndroidTest`; nenhum emulador/aparelho foi provisionado nesta sessão.
- Query real em MediaStore e validação de `EXTRA_HONORED_ARGS`
- Android 29/30/33/34/35/36 e teste real de cartão SD
- Perfil de memória/bateria/tempo com biblioteca grande
- Billing, restauração de compras e exportação Pro
- Assinatura de release, instalação sobre a versão publicada ou submissão à Play Console

Artefatos existentes: `app/build/outputs/apk/debug/app-debug.apk` e `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Não há APK Release nem AAB. As tarefas Release e a assinatura legítima ainda precisam ser avaliadas; nenhuma chave local está disponível.

O código Android, os novos documentos e o arquivo ZIP original aparecem como não rastreados no único commit Git (`42dae12`, que contém o ZIP). Não foi criada branch, pois não há baseline de código rastreada para isolar uma branch útil. Nenhuma operação destrutiva de Git foi executada.

## Observações técnicas pendentes

- Se o provider não confirmar `LIMIT`, `OFFSET` e ordenação em `EXTRA_HONORED_ARGS`, o adaptador consulta sem paginação e aplica offsets localmente. Isso evita depender dos extras para avançar, mas precisa de validação real nos providers/OEMs e pode reler muitos registros em páginas profundas. `IS_PENDING` e `IS_TRASHED` são filtrados localmente.
- O agrupamento SHA-256 é limitado aos registros de uma única página visível; não é uma detecção global de duplicatas entre páginas/volumes.
- A consulta de compra usa BillingClient local; SKUs e entitlement não foram verificados contra Play Console nem validados em backend.
- `MediaPageSequenceGuard` retém somente as últimas oito páginas por fonte para detectar sobreposição; não é prova de snapshot transacional.
- API 29 ou volumes com versão/geração indisponível permanecem `UNVERIFIABLE`.
- A permissão parcial é informada como cobertura parcial.
- Miniaturas são concorrentes limitadas e canceláveis, mas ainda precisam de medição em dispositivos lentos.

## Como executar a validação completa

Na raiz do projeto, com JDK 17+, Android SDK 36, Build Tools 35.0.0, licenças aceitas, rede disponível e pelo menos 4 GB livres:

```bash
bash scripts/prepare_and_test.sh
```

Depois execute `bash scripts/run_instrumented_tests.sh` em um emulador/aparelho por cada nível de API acordado. Para Release, configure localmente a chave de upload que já corresponde à Play Console; não crie uma nova chave. Anexe logs completos, modelo/API, commit/ZIP SHA-256 e resultados. Não marque gate como aprovado se a tarefa foi ignorada ou não executada.
