# Relatório de validação — Sweep v0.3

Data: 31/10/2026

## Evidências concluídas nesta sessão

| Verificação | Resultado | Escopo |
|---|---|---|
| Inspeção do ZIP-base e da árvore | Concluída | Comparação com a reconstrução v0.2 e a base Views |
| Compilação de fontes Kotlin puras `:domain` com `kotlinc` 1.9.0 | Aprovada | Sintaxe/tipos do domínio; não equivale a Gradle/JUnit nem valida KGP 2.3.21 |
| Smoke test Kotlin independente do domínio | Aprovado | Frescor de sessão, revisão desconhecida, mudança de volume, sobreposição, seleção por URI, limite e estados Billing; não é suíte JUnit |
| Revisão estática do contrato de página | Concluída | Limites, offsets, estados e invariantes do domínio |
| Revisão estática do adaptador MediaStore | Concluída | Ainda não executado em Android/ContentResolver real |
| Revisão estática de sessão/volume/geração | Concluída | Sem prova de estabilidade transacional |
| Revisão de UI/seleção | Concluída | Código Android ainda não compilado |
| Verificação de tipos de `MediaScanViewModel.kt` com stubs mínimos de Lifecycle e coroutines | Aprovada | Não substitui compilação contra Lifecycle Android real nem build Gradle |

## Testes presentes no repositório

- `:domain`: 46 métodos de teste declarados (modelos de mídia, paginação/sessões, permissões, guarda de sequência, seleção e política pura Billing).
- `app`: 5 testes JVM de ViewModel e 4 testes instrumentados (inicialização da Activity, paginação real de imagens/áudio e acesso visual parcial).

A existência de arquivos `@Test` não significa que foram executados. Nesta sessão não foi possível executar Gradle/JUnit porque não há Gradle 8.13 instalado em cache e o download do wrapper em `services.gradle.org` falhou por resolução DNS. O Wrapper JAR presente tem hash oficial da linha 9.8.2, enquanto a distribuição solicitada é 8.13; essa divergência está registrada em `SUPPLY_CHAIN_CHECKS.md` e precisa ser fechada regenerando o JAR com Gradle 8.13. Também não há Android SDK Platform 36 / `android.jar` disponível neste ambiente para compilar o módulo Android.

## Não executado / não aprovado

- `./gradlew :domain:test`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleDebugAndroidTest`
- Execução instrumentada em emulador ou telefone
- Query real em MediaStore e validação de `EXTRA_HONORED_ARGS`
- Android 29/30/33/34/35/36 e teste real de cartão SD
- Perfil de memória/bateria/tempo com biblioteca grande
- Billing, restauração de compras e exportação Pro
- Assinatura de release, AAB, instalação por cima do app publicado ou submissão à Play Console

## Observações técnicas pendentes

- O adaptador exige que o provedor declare honrados `LIMIT`, `OFFSET`, colunas/direção de sort e disponibilize `EXTRA_TOTAL_COUNT`. Não há SQL selection na consulta; `IS_PENDING` e `IS_TRASHED` são filtrados localmente após a leitura da página. Os offsets e o total usam contagem bruta do provider. Fabricantes/providers podem não fornecer o contrato; nesse caso a paginação será desabilitada para essa fonte.
- `MediaPageSequenceGuard` retém somente as últimas oito páginas por fonte para detectar sobreposição; não é prova de snapshot transacional.
- API 29 ou volumes com versão/geração indisponível permanecem `UNVERIFIABLE`.
- A permissão parcial é informada como cobertura parcial.
- Miniaturas são concorrentes limitadas e canceláveis, mas ainda precisam de medição em dispositivos lentos.

## Como executar a validação completa

Na raiz do projeto, com JDK 17+, Android SDK 36, Build Tools 35.0.0, rede disponível e pelo menos 4 GB livres:

```bash
bash scripts/prepare_and_test.sh
```

Depois execute `bash scripts/run_instrumented_tests.sh` em um emulador/aparelho por cada nível de API acordado. Anexe logs completos, modelo do dispositivo/API, commit/ZIP SHA-256 e resultados. Não marque gate como aprovado se a tarefa foi ignorada ou não executada.
