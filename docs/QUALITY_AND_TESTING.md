# Quality and Testing — Midiastore

## Execução local final — 2026-11-06 — source não enviado, base SHA `16545a4277bcc515660a6516c32fb6c770a4e880`

| Verificação | Estado | Evidência |
|---|---|---|
| Domain/JVM/Lint/Debug/AndroidTest/helper | PASS | `./gradlew.bat --no-daemon :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug`; BUILD SUCCESSFUL, 114 tarefas (6 executadas). Lint task passa com 184 findings conhecidos e `abortOnError=false`. |
| Recompile AndroidTest/helper após mudanças finais | PASS | `./gradlew.bat --no-daemon -Pkotlin.compiler.execution.strategy=in-process :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug`; BUILD SUCCESSFUL, 81 tarefas (4 executadas). |
| AndroidJUnitRunner direto, MediaStorePagingInstrumentedTest | PASS | Motorola moto g84 5G, API35, serial `0085353748`; `OK (5 tests)`, incluindo partial visual access executado, áudio independente, página/sessão, SHA-256 de conteúdo e auditoria de cleanup. |
| AndroidJUnitRunner direto, MainActivityInstrumentedTest | PASS limitado | `OK (3 tests)` protocolo: 1 execução real (Home) e 2 casos API29 ignorados por assumption API; isso não valida API29. |
| `:app:connectedDebugAndroidTest`/UTP no mesmo APK final | FAIL | Código 1, `Failed to receive the UTP test results`; sem XML e sem casos coletados. Dispositivo ADB continuou online. `utp.0.log` registra fatal do result listener. Logcat pós execução sem `AndroidRuntime` fatal. Diagnóstico debug anterior mostra falha BouncyCastle/PEM RSA enquanto o listener gRPC local inicia; evidência de falha no caminho do coletor, ainda sem causa raiz definitiva. |
| API29 foreign-owner/consent | NOT RUN | Fixture auxiliar e cenários cancel/aceite compilam, mas exigem runtime API29. UID distinto no Motorola não valida OWNER_PACKAGE_NAME nem consentimento API29. AVD local não iniciou por falta de aceleração. |

APK SHA-256 finais: Debug `0E550D445D231F9B65EBACEA3AA18727764AFD05014294B1D49905B2AD6BA632`; AndroidTest `D6F1C7602F4AB9574413852FABFF9DD234886A92DFE8D339A80DDAC064DDC112`; helper `B00066BF7FF33116216619D728453D0E38E8525DA0C6F5FD28C590976F5632A1`. Evidências detalhadas, stdout do runner, exit codes, logcats, UIDs e checksums: `.local-test-results/2026-11-06/`. A saída direta é protocolo textual; não é XML. O UTP não produziu XML.

### Hash reempacotado e tentativa isolada

- Uma tentativa com `ANDROID_USER_HOME` alternativo criou outra debug keystore e falhou antes de executar testes com `INSTALL_FAILED_UPDATE_INCOMPATIBLE` em `mediaOwnerTest:installDebug`; não é resultado de UTP.
- Com o user home original, `:app:assembleDebug :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug` passou. O APK AndroidTest reempacotado SHA `15344F181D5AAE73BF2945D84BF850220FD30DADE45005AABE91C7223F79DCD1` foi executado pelo runner direto: MediaStore `OK (5)` e Main `OK (3)` (2 casos API29 ignorados). Esse hash ainda não foi testado pelo UTP. A comparação anterior que reproduziu UTP FAIL usou AndroidTest SHA `D6F1C760...`, também aprovado pelo runner direto no Motorola.
- Checksums atuais estão em `.local-test-results/2026-11-06/final-apk-sha256.txt`. Nenhuma chave de Release foi criada ou usada; manter o Android user home estável para futuras comparações de APK assinado em Debug.

Atualizado em 04/11/2026. Este documento é a situação de QA da branch de recuperação, não uma declaração de release aprovado.

## Identidade de build

- Listing / aplicação: Midiastore.
- applicationId: `com.rdf112.midiastore` (confirmado pelo proprietário).
- Namespace interno: `com.rdf112.sweep` (legado).
- versionCode: 17 no source atual; o maior versionCode já publicado ainda precisa de confirmação na Play Console.
- versionName: 2.2.1 no source atual.
- minSdk / targetSdk / compileSdk: 29 / 36 / 36.
- JDK: 17.
- Gradle Wrapper: 8.13.
- Android SDK Platform: 36.
- Build Tools: 35.0.0.
- Billing Library: 8.0.0.

## Histórico de validações — não misturar commits

### Baseline anterior — 03/11/2026

Um build Debug em Codespaces Ubuntu 24.04 com JDK 17/Gradle 8.13 registrou `BUILD SUCCESSFUL` para domain tests, app JVM tests, Lint, Debug e AndroidTest APK. Foram registrados 58 testes de domínio e 7 testes JVM de app aprovados, com 76 warnings de Lint.

Esse resultado pertence à baseline VC14 anterior à recuperação. **Não prova que a branch atual compila**, porque desde então foram alterados scanner global, permissões, billing feedback, recursos, tema, scripts, testes e a cadeia de Release.

### Falhas intermediárias observadas durante a recuperação

- O primeiro workflow PR falhou por `org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64`, um caminho local inexistente no runner. A propriedade foi removida na branch de recuperação; use o `JAVA_HOME` configurado pelo ambiente.
- Um teste instrumentado MediaStore falhou no emulador Android 15. O primeiro workflow não apresentou uma stack trace útil; relatórios XML foram adicionados ao workflow para diagnóstico.
- Um teste JVM intermediário falhou porque ainda esperava a marca antiga “Midiastores”. O teste foi atualizado para “Midiastore”.
- Workflows de commits intermediários foram cancelados pela política de concorrência à medida que a branch recebia correções. Execuções canceladas ou de SHA antigo não são prova de aprovação do HEAD atual.

## Estado de validação do HEAD

Verifique sempre o workflow do **mesmo SHA** mostrado no topo da PR #3:

https://github.com/srodolfobarbosa-afk/midiastory-/pull/3

A verificação Debug do CI executa:

1. `bash -n scripts/build_signed_aab.sh` e guardas para confirmar `applicationId=com.rdf112.midiastore` e impedir mutações antigas do package pelos scripts Windows.
2. `:domain:test`.
3. `:app:testDebugUnitTest`.
4. `:app:lintDebug`.
5. `:app:assembleDebug`.
6. `:app:assembleDebugAndroidTest`.
7. `:app:connectedDebugAndroidTest` em emulador Android 35.
8. Upload de relatórios JVM/Android para diagnóstico quando disponíveis.

Um resultado de CI de SHA anterior, cancelado ou ainda em execução não é gate aprovado.

## Comandos locais no Codespaces

A branch `main` ainda pode conter a configuração antiga de JDK fixo. Use a branch de recuperação:

```bash
cd /workspaces/midiastory-
git fetch origin
git switch fix/p0-sweep-permissions-missions-billing
git pull --ff-only
```

Executar gates locais:

```bash
./gradlew --no-daemon \
  :domain:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

Com emulador ou aparelho autorizado:

```bash
./gradlew --no-daemon :app:connectedDebugAndroidTest
```

## Comandos Windows

Na raiz do projeto, abrir PowerShell e executar:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\COMPILAR_TUDO_WINDOWS.ps1
```

A rotina Windows atual executa testes/JVM, Lint, APK Debug e AndroidTest APK. Ela **não gera Release/AAB** e não altera `applicationId`, versionCode ou assinatura.

O APK Debug terá package `com.rdf112.midiastore.debug`; ele é um app de debug separado e não atualiza a instalação da Google Play.

Se `adb` responder `no devices/emulators found`, nenhum aparelho ou emulador está conectado. Ative Depuração USB, conecte o telefone, aceite o diálogo RSA e confirme com `adb devices`; ou inicie um emulador Android Studio. Isso não é falha do aplicativo nem resultado de teste.

## Scanner global — validações necessárias

A branch adiciona missão global cancelável:
- percorre imagens, vídeos e áudio com acesso FULL/PARTIAL;
- enumera volumes expostos por MediaStore;
- pagina em lotes de 100 registros;
- cancela consultas e hashing;
- calcula SHA-256 de candidatos em fluxo por dispatcher IO;
- aplica limites de 250.000 rows, 20.000 duplicate-candidate rows e 100.000 size buckets;
- identifica partial coverage se houver permissão parcial, falha provider, paginação não comprovada, revisão de sessão não verificável ou limite;
- bloqueia pré-seleção de duplicatas se a cobertura global for parcial.

Testes automatizados foram adicionados para:
- duplicatas idênticas atravessando limites de páginas;
- falha de provider em outra categoria;
- não selecionar duplicatas quando cobertura é parcial;
- remover um item de revisão global que não pertence à página visível;
- exigir SHA-256 não vazio e igual.

Ainda é necessário executar e aprovar esses testes no CI do HEAD atual e provar performance/memória em bibliotecas grandes.

## Limpeza e armazenamento

- API 30+ usa os pedidos de consentimento oficiais do MediaStore.
- API29 segue bloqueada para operações de Lixeira/exclusão, apesar de minSdk29.
- Mover para Lixeira não conta como bytes livres.
- Exclusão permanente só conta como liberada depois do consentimento e confirmação de URIs ausentes.
- Não dizer que a limpeza funcionou ou espaço foi liberado sem verificação real em aparelho.
- Testar aceitar, cancelar, aprovação parcial, provider error, restore e reconciliação em APIs/OEMs.

## Billing e Release — gates separados

O código consulta estes IDs:
- `sweep_pro_monthly` — SUBS.
- `sweep_pro_annual` — SUBS.
- `sweep_pro_lifetime` — INAPP.

Ainda não há evidência suficiente de que os produtos estão ativos na Play Console, nem de compra real, restore, refund, expiry, assinatura ou teste de licença. O perfil financeiro habilitado pelo proprietário não fecha esses gates.

O cliente atual verifica presença dos campos de assinatura/JSON da compra, mas não valida criptograficamente `purchaseToken` através de backend confiável. Antes de monetização de produção, implementar verificação no backend pela Google Play Developer API e reconciliação idempotente.

### AAB Release

`scripts/preparar_release.ps1` foi intencionalmente bloqueado: o script antigo alternava entre `com.rdf112.sweep` e `com.rdf112.midiastore` e poderia restaurar package incorreto.

O único caminho futuro de Release é o workflow protegido `play-release`, após autorização do proprietário e dos gates P0. O script `scripts/build_signed_aab.sh` exige:
- `MIDIASTORE_RELEASE_APPROVED=YES`;
- `MIDIASTORE_CONFIRMED_VERSION_CODE` igual ao versionCode confirmado como válido na Play Console;
- `MIDIASTORE_EXPECTED_CERT_SHA256` igual ao fingerprint de upload correto;
- `SWEEP_STORE_FILE`, `SWEEP_STORE_PASSWORD`, `SWEEP_KEY_ALIAS`, `SWEEP_KEY_PASSWORD`.

O script valida package e versionCode do source, fingerprint do JKS, assinatura do APK e do AAB e exporta artifacts mais SHA-256. Ele não publica na Play. Não criar ou compartilhar chaves privadas pelo chat.

## Matriz de dispositivo obrigatória antes de release

- Android API 29, 30, 33, 34, 35 e 36.
- Pixel/AOSP e ao menos Samsung, Motorola e Xiaomi/HyperOS.
- Armazenamento interno, cartão SD e volumes removíveis quando expostos pelo provider.
- Acesso completo, parcial, negado e revogado; áudio independente.
- Biblioteca pequena, grande e duplicatas entre categorias/volumes.
- Cancelamento em consulta MediaStore e no meio de hash.
- Lixeira, exclusão permanente, cancelamento, aprovação parcial e restauração.
- TalkBack, escala de fonte, contraste, rotações, tela pequena, tablet e dobrável.
- Billing licenciado: catalog, pending, cancel, retry, restore, ack, refund, expiry e network loss.

**Critério de aprovação:** evidência (logs/screenshots) ligada ao mesmo commit, modelo/API e artefato. Nenhum gate é aprovado pela presença de código ou por um build antigo.

## Atualização local de validação — 2026-11-06

- Base de código: branch local `codex/midiastore-release-audit`, HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`; mudanças locais ainda não publicadas.
- `:app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest`: `BUILD SUCCESSFUL` após alterar o tratamento de `SESSION_STALE` para exigir sessão estável. Resultado é compilação, não execução dos testes.
- Kotlin daemon não pôde criar arquivo temporário em `%LOCALAPPDATA%\kotlin\daemon` (`AccessDeniedException`); fallback do compilador concluiu. Reavaliar daemon se voltar a ocorrer.
- UTP no Motorola moto g84 5G, serial `0085353748`, API35: `:app:connectedDebugAndroidTest -x :mediaOwnerTest:installDebug` falhou ao receber resultados e coletou zero testes. Depois da tentativa, `adb devices -l` ficou vazio. Não classificar como falha de assertion nem sucesso da aplicação; runner direto precisa ser repetido no novo APK.
- Execução AndroidJUnitRunner direta anterior: `OK (7 tests)` no mesmo aparelho/API35, mas anterior à nova assertion e aos APKs compilados nesta continuação. Dois testes API29 foram ignorados por condição de API; portanto são cinco casos efetivamente executados. O caso de acesso parcial passou com permissões preparadas previamente via ADB.
- API29: fixture de propriedade agora usa app auxiliar com UID distinto e valida `OWNER_PACKAGE_NAME` real. Sem execução: AVD/imagem API29 não iniciou porque aceleração de virtualização x86_64 não está habilitada/instalada neste host. Nenhum driver foi instalado.
- Diagnóstico remoto exato run `42080940375`, SHA `16545a4`: XML API29 confirmou owner igual ao app alvo; API35 confirmou `external_primary not found`; logcat registrou morte do UID após revogação de permissão durante a instrumentação. Artefatos locais: `.local-ci-42080940375/`.
- Próximo gate: reconectar e reidentificar o Motorola; executar teste Selected Photos isolado e suite direta atual, coletar `logcat`, XML e exit code. API29 deve rodar em CI ou host já habilitado para virtualização. Nenhum resultado local substitui CI verde no commit PR mais recente.

## Atualização de validação — 04/11/2026

- O último HEAD de código funcional, antes desta atualização documental, adicionou StorageUsageCalculator e quatro testes unitários.
- O workflow 41862544945 para o SHA d11c1b9 estava queued; não houve conclusão para aquele SHA.
- O novo commit de documentação/posicionamento será novamente submetido ao CI; não transferir resultados anteriores.
- O medidor usa o armazenamento compartilhado primário, não inclui automaticamente SD/USB.
- PT-BR/EN/fallback têm 152 chaves; DE/ES/FR/HI/JA/ZH-CN têm 63/152 cada. Faltam 89 por locale, 534 no total.
- Nenhum teste instrumentado/API29/API35 ou teste físico do HEAD atual foi aprovado.
- Estado permanece NÃO PRONTO PARA RELEASE.

## Mitigação do boot do emulador — 04/11/2026

O workflow foi ajustado para não chamar o serviço Android Settings durante o boot do emulador. A configuração anterior usava disable-animations e três comandos adb shell settings; o CI já havia registrado "Failure calling service settings: Broken pipe (32)" antes dos testes instrumentados. Agora a etapa aguarda sys.boot_completed por até 180 segundos, com repetição tolerante a falhas transitórias, e só então executa connectedDebugAndroidTest.

Esta mudança remove a dependência do serviço Settings no início do job; não é evidência de que a instrumentação passou. O próximo resultado precisa ser lido no SHA exato e deve incluir a execução real dos testes API29 e API35.

## Jornada principal do Phone Cleaner — ajuste de 04/11/2026

A ação circular principal da Home agora chama a missão global de encontrar arquivos grandes em todas as categorias/volumes acessíveis, em vez de abrir somente a primeira página de fotos na aba Arquivos. Quando falta permissão, a seleção da missão é preservada; após o callback de permissão, a navegação retorna à aba Limpeza para mostrar o progresso da missão.

A ação secundária foi renomeada para Abrir arquivos/Browse files, deixando claro que ela abre a navegação paginada por mídia. Um teste instrumentado verifica que a ação principal global está presente e clicável. A execução do teste continua dependente do workflow atual.

## Estado exato do HEAD — 04/11/2026, 16:22 BRT

- Branch `fix/p0-sweep-permissions-missions-billing`, HEAD `d60ff8f8c0852ab7c33ce638d662053c028d4916`.
- PR #3: aberta, Draft, base `main`, não merged.
- `main` permanece no SHA `3759e176c48f340fa576a06455579277e514d9d9`.
- GitHub Actions run [41866189110](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/41866189110), associado exatamente ao HEAD acima, está **queued**. Jobs API29, API35 e Windows também estão queued; não há resultado de build/teste para esse SHA.
- O último ajuste funcional liga o botão principal da home à missão global de arquivos grandes e retoma a tela de progresso após autorização de mídia. Foi adicionado teste instrumentado da existência/clickabilidade da ação principal; não foi executado ainda.
- Não declarar compilação, Lint, testes JVM, instrumentação ou APK gerado neste HEAD até obter logs do run exato.

## Auditoria local de recuperação — 2026-11-05

- Base da branch local `codex/midiastore-release-audit`: HEAD remoto de recuperação `16545a4277bcc515660a6516c32fb6c770a4e880`; nenhuma alteração enviada ao GitHub.
- Último workflow remoto examinado: [42080940375](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/42080940375). JVM, Lint task, APKs e parser PowerShell passaram; API29 e API35 instrumentados falharam por fixture com owner incorreto e corrida de montagem do volume, respectivamente.
- Correção local adiciona app de fixture independente e espera volume montado. `:app:compileDebugAndroidTestKotlin` e `:app:assembleDebugAndroidTest` passaram, mas não houve execução instrumentada após as mudanças.
- Com JDK 17.0.20.1, Gradle 8.13, SDK Platform 36 e Build Tools 35.0.0, a validação local terminou `BUILD SUCCESSFUL`: `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` e `:mediaOwnerTest:assembleDebug`.
- O Lint encontrou 184 issues: 90 erros `MissingTranslation` nos recursos de idiomas parciais e 94 warnings. A tarefa passou porque `abortOnError=false`; o relatório não está limpo.
- `adb devices -l` não listou dispositivo. Não há imagem/AVD local API29 ou API35; instrumentação e teste físico continuam pendentes.
- Release/AAB não foi validado: a chave legítima não está no workspace e os gates de Play Console estão abertos.

## Execução de continuidade — 2026-11-06 (APK atual)

| Validação | Estado | Evidência / limitação |
|---|---|---|
| Domain, JVM, Lint, Debug, AndroidTest e helper | PASS | `./gradlew.bat --no-daemon -Pkotlin.compiler.execution.strategy=in-process :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug`; BUILD SUCCESSFUL, 114 tarefas, 11 executadas. Lint ainda reporta 184 findings com `abortOnError=false`. |
| AndroidJUnitRunner direto — MediaStore, Motorola API35 | PASS com execução isolada parcial | Classe: protocolo `OK (5 tests)`, mas 1 método foi assumption-skipped por permissões preexistentes e 4 executaram. O caso Selected Photos foi preparado via ADB (visual-selected=true, imagens/vídeo/áudio=false), executado isoladamente e passou `OK (1 test)`, exit 0. Permissões Debug restauradas ao estado original. |
| AndroidJUnitRunner direto — Main, Motorola API35 | PASS limitado | `OK (4 tests)` no protocolo, exit 0; logcat registra 1 teste funcional executado e 3 assumptions por API/ambiente. O cenário de consentimento não foi validado nesta execução. |
| Consentimento UI API35 no Motorola | SKIP após guard / execução anterior FAIL | Serviço `com.truedevelopersstudio.automatictap.autoclicker` está habilitado. Logcat mostra `MediaProvider: User allowed grant` menos de 1 s após abrir o diálogo, sem interação controlada do teste. O guard de harness pula esse caso; não foi alterada configuração global. Validar em emulador/aparelho sem automação externa. |
| Gradle `connectedDebugAndroidTest` / UTP | FAIL antes da coleta | Execução filtrada para MediaStore terminou exit 1 com `Failed to receive the UTP test results`; HTML = 0 testes e nenhum XML. `utp.0.log` apenas informa construção do runner e erro fatal. Um `--debug` anterior mostra `BouncyCastlePemReader` incapaz de ler RSA PEM no `UtpTestResultListenerServer`; causa raiz não confirmada. |
| API29 owner/RecoverableSecurityException | NOT RUN | Nenhum runtime API29 local; AVD não iniciou por falta de aceleração permitida. Requer job Linux API29 após autorização de push. |

Device/build association: Motorola moto g84 5G, Android 15/API35, serial `0085353748`, fingerprint `motorola/bangkk_g/bangkk:15/V1TCS35H.88-16-9/2edd8c-be328e:user/release-keys`; source HEAD `16545a4277bcc515660a6516c32fb6c770a4e880` mais alterações locais não publicadas. APK SHA-256: Debug `EAE637CEF8F581B65B5D8BCDEEF97ADBC4BA8699D38716BBA4A818AD6045091E`, AndroidTest `55EAA999F40E3F0424A2271A5265AADA08167D3C23611A3F52ABDBD13C20E3CD`, helper `B00066BF7FF33116216619D728453D0E38E8525DA0C6F5FD28C590976F5632A1`. Logs/exitcodes: `.local-test-results/2026-11-06/`. Direct instrumentation protocol is not JUnit XML; UTP produced no XML.
