# Midiastore — 2.1.1 (versionCode 14)

**Estado:** Debug e Release compilados/testados com assinatura temporária; não pronto para publicação até validar certificado e Play Console.

O nome técnico Sweep permanece em pacotes/namespace e recursos internos; a identidade Google Play e o certificado do app existente ainda não foram confirmados.

## Identidade candidata

| Campo | Valor |
|---|---|
| `applicationId` | `com.rdf112.sweep` |
| `versionCode` | `14` (candidato; não confirma o maior valor publicado) |
| `versionName` | `2.1.1` (candidato) |
| `minSdk` | `29` |
| `compileSdk` / `targetSdk` | `36` / `36` |
| Java/Kotlin JVM target | `17` |

Confirme pacote, versionCode mais alto e certificado de upload na Play Console antes de qualquer submissão. A configuração do código não comprova o estado atual da conta.

## Fidelidade à aplicação publicada

O fonte histórico que gerou o AAB de produção não foi recuperado. Este é um projeto reconstruído para desenvolvimento, usando o `applicationId` conhecido e os requisitos documentados. Não afirmar equivalência completa com o aplicativo já instalado.

O fonte histórico que gerou o AAB de produção não foi recuperado; equivalência com usuários existentes não está comprovada. O app contém BillingClient 8.0.0, consulta os SKUs `sweep_pro_monthly`, `sweep_pro_annual` e `sweep_pro_lifetime`, inicia compras e consulta compras existentes. Isso não comprova cadastro/disponibilidade dos produtos na Play Console nem valida criptograficamente tokens em backend. A política local mapeia os estados fail-closed, mas compra/restauração/reembolso/expiração ainda precisam de validação real.

## O que o projeto implementa

- Projeto Android Views; não migra a interface para Compose.
- Tela de análise e revisão em português e inglês.
- Leitura informativa de espaço do armazenamento interno via `StatFs`.
- Consulta somente de fotos, vídeos e áudio acessíveis pelo MediaStore.
- Enumeração de volumes externos expostos por MediaStore, incluindo armazenamento removível quando o provedor/dispositivo o disponibiliza.
- Consultas estruturadas e limitadas a 100 registros por página, com navegação incremental. O domínio permite pedidos de até 500, mas a interface usa 100.
- Ordenação determinística por data de modificação e ID como desempate (ambos descendentes, por limitação da direção única da API estruturada; desvio da sugestão `_ID ASC` documentado).
- Verificação dos argumentos de paginação/ordenação honrados. Sem confirmação do provider, consulta sem paginação com ordenação determinística e aplica o offset localmente; o custo pode crescer com páginas profundas. `IS_PENDING` e `IS_TRASHED` são filtrados localmente e offsets contam linhas brutas.
- Detecção limitada de páginas sobrepostas ou conteúdo alterado para offsets já vistos.
- Sessão com permissões, conjunto de volumes, versão e geração por volume. Mudança observada invalida a sessão; marcador ausente/indisponível é `UNVERIFIABLE`, não “estável”.
- `CancellationSignal` ligado ao cancelamento da consulta MediaStore.
- Acesso parcial no Android 14+ é mostrado como cobertura parcial para imagens/vídeos; áudio usa a permissão granular independente `READ_MEDIA_AUDIO` no Android 13+.
- Miniaturas carregadas sob demanda para a página atual, com no máximo quatro operações de leitura simultâneas e sinal de cancelamento.
- Seleção de até 500 URIs para revisão; a Lixeira e a exclusão permanente usam os diálogos oficiais de consentimento do Android 11+.
- Revalidação de tamanho, MIME, data e SHA-256 quando disponível antes da ação; a ausência de hash não confirma duplicatas.
- Espaço do dispositivo por `StatFs`; bytes da Lixeira não são contados como liberados e a exclusão só credita bytes após aceitação e confirmação dos URIs ausentes.
- Histórico de navegação limitado às últimas 50 páginas.
- Não limpa cache de terceiros nem executa ações destrutivas sem autorização do sistema Android.

## Delimitação de produto

Duplicatas são comparadas somente entre registros disponíveis na página atual, não em uma varredura global de toda a biblioteca. Ainda não há análise de Downloads/documentos via SAF nem tarefas agendadas. A exportação Pro existe, mas depende de entitlement validado. Páginas, seleção e espaço potencial são locais à página; não representam automaticamente a biblioteca inteira.

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
│       │   └── billing/                    # Política de entitlement
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

Antes de um release real, ainda são necessários: código/artefato publicado para comparação; JDK 17, Android SDK 36 e Build Tools 35.0.0; testes Gradle, Lint, builds Debug/Release e testes instrumentados aprovados; MediaStore em APIs/OEMs suportados; Billing e produtos validados na Play Console; verificação segura de compras; chave de upload conferida; política de privacidade e Data safety alinhados; AAB assinado e inspecionado; teste interno/fechado e aprovação da conta.
