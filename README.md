# Sweep — v0.3 Paginação incremental (base de atualização 1.1.1)

**Estado:** incremento técnico de desenvolvimento, somente leitura. Este ZIP não é um Release Candidate e não deve ser enviado à Google Play.

## Identidade candidata

| Campo | Valor |
|---|---|
| `applicationId` | `com.rdf112.sweep` |
| `versionCode` | `9` (proposto com base na informação do proprietário de que o último publicado é 8) |
| `versionName` | `1.1.1` (proposto) |
| `minSdk` | `29` |
| `compileSdk` / `targetSdk` | `36` / `36` |
| Java/Kotlin JVM target | `17` |

Confirme pacote, versionCode mais alto e certificado de upload na Play Console antes de qualquer submissão. A configuração do código não comprova o estado atual da conta.

## Fidelidade à aplicação publicada

O fonte histórico que gerou o AAB de produção não foi recuperado. Este é um projeto reconstruído para desenvolvimento, usando o `applicationId` conhecido e os requisitos documentados. Não afirmar equivalência completa com o aplicativo já instalado.

O AAB beta previamente analisado indicava Billing Library 8.0.0. A planta técnica registra um possível erro no parser de estado de compra e um problema na recuperação de itens da Lixeira. Esta revisão ainda **não** contém BillingClient, catálogo de produtos, entitlement, fluxo Pro ou recuperação de compras. O código puro `BillingStatePolicy` serve apenas para testar a interpretação fail-closed dos estados 0/1/2/valores desconhecidos; não está conectado ao aplicativo e os IDs de produto nos testes são fixtures fictícias.

## O que o incremento v0.3 implementa

- Projeto Android Views; não migra a interface para Compose.
- Tela de análise e revisão em português e inglês.
- Leitura informativa de espaço do armazenamento interno via `StatFs`.
- Consulta somente de fotos, vídeos e áudio acessíveis pelo MediaStore.
- Enumeração de volumes externos expostos por MediaStore, incluindo armazenamento removível quando o provedor/dispositivo o disponibiliza.
- Consultas estruturadas e limitadas a 100 registros por página, com navegação incremental. O domínio permite pedidos de até 500, mas a interface usa 100.
- Ordenação determinística por data de modificação e ID como desempate (ambos descendentes, por limitação da direção única da API estruturada; desvio da sugestão `_ID ASC` documentado).
- Verificação de `EXTRA_HONORED_ARGS`, `EXTRA_TOTAL_COUNT` e tamanho real do cursor. Se o provedor não comprovar os argumentos de paginação/ordenação ou retornar uma página incoerente, o aplicativo interrompe a navegação dessa fonte em vez de apresentar uma lista como completa. `IS_PENDING` e `IS_TRASHED` são filtrados localmente após a consulta; offsets e total referem-se às linhas brutas do provider.
- Detecção limitada de páginas sobrepostas ou conteúdo alterado para offsets já vistos.
- Sessão com permissões, conjunto de volumes, versão e geração por volume. Mudança observada invalida a sessão; marcador ausente/indisponível é `UNVERIFIABLE`, não “estável”.
- `CancellationSignal` ligado ao cancelamento da consulta MediaStore.
- Acesso parcial no Android 14+ é mostrado como cobertura parcial para imagens/vídeos; áudio usa a permissão granular independente `READ_MEDIA_AUDIO` no Android 13+.
- Miniaturas carregadas sob demanda para a página atual, com no máximo quatro operações de leitura simultâneas e sinal de cancelamento.
- Seleção local de até 500 URIs apenas para revisão; o ledger é limitado e não oferece operação de exclusão.
- Histórico de navegação limitado às últimas 50 páginas.
- Nenhuma ação de Lixeira, exclusão permanente, limpeza de cache de terceiros ou Billing nesta etapa.

## Delimitação de produto

Esta fatia cumpre a direção da planta v1.9 para paginação incremental e revisão somente leitura. Ainda não implementa duplicatas SHA-256, imagens semelhantes, análise de arquivos Downloads/documentos via SAF, histórico persistente, exportação Pro ou tarefas agendadas.

Não afirmar que o telefone está “limpo”, que bytes foram liberados ou que uma página representa toda a biblioteca quando o acesso for parcial, a geração não puder ser verificada ou o provedor não confirmar a paginação. A tela informa somente o conteúdo da página e os avisos de cobertura.

## Ferramentas declaradas

| Componente | Versão configurada |
|---|---:|
| Android Gradle Plugin | 8.13.2 |
| Kotlin Gradle Plugin | 2.3.21 |
| Gradle Wrapper | 8.13 |
| JDK | 17 ou superior |
| Android SDK Platform | 36 |
| Android Build Tools | 35.0.0 requerido pelo script de validação |

A combinação foi escolhida dentro das faixas de compatibilidade publicadas para AGP, Gradle e Kotlin. Isso não substitui a execução do build sobre este ZIP.

## Estrutura principal

```text
Sweep/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/rdf112/sweep/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/media/AndroidMediaPageRepository.kt
│       │   │   └── presentation/scan/MediaScanViewModel.kt
│       │   ├── res/values/                 # Português
│       │   └── res/values-en/              # Inglês
│       └── androidTest/
├── domain/
│   └── src/
│       ├── main/kotlin/com/rdf112/sweep/domain/
│       │   ├── model/
│       │   ├── paging/
│       │   ├── repository/
│       │   ├── selection/
│       │   └── billing/                    # Política pura; não é Billing integrado
│       └── test/
├── docs/
├── scripts/prepare_and_test.sh
├── scripts/run_instrumented_tests.sh
├── gradle/wrapper/
├── gradlew
└── gradlew.bat
```

## Como validar em um ambiente Android

Requisitos: JDK 17+, pelo menos 4 GB livres no volume do projeto, Android SDK Platform 36 e Build Tools 35.0.0, licenças aceitas e conectividade para obter Gradle/dependências na primeira execução.

Na raiz extraída:

```bash
bash scripts/prepare_and_test.sh
```

O script executa `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` e `:app:assembleDebugAndroidTest`. Não apaga caches nem arquivos do usuário. O APK debug e o APK de testes não são artefatos de produção.

Com um emulador ou telefone autorizado conectado via `adb`, execute também `bash scripts/run_instrumented_tests.sh` para rodar `:app:connectedDebugAndroidTest`. Repita em cada API da matriz (29, 30, 33, 34, 35 e 36) e registre modelo/API e logs; um único aparelho não cobre a matriz.

Se o ambiente não tiver rede, Gradle Wrapper/cache ou SDK completo, a execução falhará antes de concluir o gate. Registre a saída completa; não interprete um passo ignorado como teste aprovado.

## Assinatura e publicação

`bundleRelease` e `assembleRelease` são bloqueados até que `keystore.properties` e o JKS correto estejam configurados localmente. O ZIP inclui somente `keystore.properties.example`, sem senha ou chave. Não crie outro JKS por tentativa. Compare o certificado com a chave de upload aceita pela Play Console.

Antes de um release real, ainda são necessários: código histórico/artefato publicado para comparação de regressão; recuperação e teste de Billing/Pro; testes Gradle e Lint aprovados; teste instrumentado no emulador/aparelho; testes de MediaStore nos níveis de API e fabricantes suportados; revisão de acessibilidade/performance; política de privacidade e Data safety alinhados; AAB assinado e inspecionado; teste interno/fechado e aprovação da conta.
