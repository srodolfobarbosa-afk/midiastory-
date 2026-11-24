# Midiastore — relatório diário cumulativo

## 2026-11-06

Nota de continuidade: `docs/RELATORIO_MIDIASTORE_RETOMADA_2026-11-06.md` não existe no workspace e não foi localizada cópia equivalente fora dele; usei as instruções do proprietário e os documentos de continuidade existentes, sem sobrescrever/criar relatório duplicado. Nesta sessão, consulta read-only final confirmou PR #3 OPEN/Draft no SHA remoto `16545a4277bcc515660a6516c32fb6c770a4e880`, Issue #2 OPEN e run `42080940375` concluído com failure nesse mesmo SHA.

### Retomada de validação no Motorola — atualização final desta sessão

- Estado confirmado: branch local `codex/midiastore-release-audit`, HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`, igual ao HEAD da branch remota de recuperação conferido anteriormente. PR #3 segue aberta/Draft e Issue #2 aberta. Nenhuma operação remota de escrita foi feita.
- Dispositivo reidentificado: Motorola moto g84 5G, Android 15/API35, serial `0085353748`; fingerprint `motorola/bangkk_g/bangkk:15/V1TCS35H.88-16-9/2edd8c-be328e:user/release-keys`. UIDs instalados: Debug 10475, `mediaOwnerTest` 10476, instrumentação 10477 e auxiliar `.codex.test` 10478. Os UIDs são distintos; isso confirma isolamento de processo no Motorola, mas não substitui prova do `OWNER_PACKAGE_NAME` nem validação API29.
- **PASS — build completo local:** `./gradlew.bat --no-daemon :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug`, `BUILD SUCCESSFUL` (114 tarefas; 6 executadas, 108 up-to-date). Lint ainda relata 184 findings conhecidos (90 `MissingTranslation`, 94 warnings); `abortOnError=false` permite sucesso da task.
- **PASS — recompilação após adicionar consentimento API29 cancel/aceitar e auditoria de limpeza de fixtures:** `./gradlew.bat --no-daemon -Pkotlin.compiler.execution.strategy=in-process :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug`, `BUILD SUCCESSFUL` (81 tarefas; 4 executadas, 77 up-to-date).
- **PASS — AndroidJUnitRunner direto, APKs finais, API35:** `MediaStorePagingInstrumentedTest` terminou `OK (5 tests)`, incluindo Selected Photos partial access executado, áudio independente, página de imagem/session, SHA-256 real para bytes iguais/diferentes e auditoria de remoção das fixtures. `MainActivityInstrumentedTest` terminou `OK (3 tests)` no protocolo do runner, com Home aprovado e os dois casos limitados a API29 ignorados por assumption API; portanto somente 1 caso da segunda classe foi efetivamente executado. Resultados brutos e exit codes `0` estão em `.local-test-results/2026-11-06/`.
- Checksums SHA-256: Debug APK `0E550D445D231F9B65EBACEA3AA18727764AFD05014294B1D49905B2AD6BA632`; AndroidTest APK `D6F1C7602F4AB9574413852FABFF9DD234886A92DFE8D339A80DDAC064DDC112`; `mediaOwnerTest` APK `B00066BF7FF33116216619D728453D0E38E8525DA0C6F5FD28C590976F5632A1`. Lista em `.local-test-results/2026-11-06/final-apk-sha256.txt`.
- **FAIL — UTP/Gradle connected no mesmo APK AndroidTest final:** `./gradlew.bat --no-daemon :app:connectedDebugAndroidTest`, código 1 após 1m57s, `Failed to receive the UTP test results`. O UTP produziu somente `utp.0.log` com construção do runner e erro fatal; não produziu XML/relatório de casos. ADB permaneceu conectado. Nenhum crash `AndroidRuntime` foi capturado no logcat pós-execução. Isso é falha de coleta UTP, distinta do runner direto; não é aprovação do UTP nem evidência de falha de assertion do app.
- A depuração anterior de UTP (`.local-test-results/2026-11-06/utp-debug.log`) registrou `BouncyCastlePemReader Unable to extract private key` / `PEMException: malformed sequence in RSA private key` na inicialização do servidor gRPC do listener local de resultados do UTP, seguida por fatal em `UtpTestResultListenerServerRunner` e `Failed to receive...`. Essa é a evidência proximate de falha do host/coletor, sem afirmar causa raiz definitiva. A opção antiga `android.experimental.androidTest.useUnifiedTestPlatform=false` foi rejeitada pelo AGP: UTP é o único runner nesta versão.
- Artefatos/evidências adicionados nesta sessão incluem saídas e exitcodes dos runners diretos, logcat pós-UTP, UIDs e checksums de APK/log em `.local-test-results/2026-11-06/`. A saída direta `am instrument` é protocolo textual, não JUnit XML. O diretório do UTP contém somente `utp.0.log`/lock; XML está ausente.
- **NOT RUN — API29:** A fixture agora é criada pelo app auxiliar `com.rdf112.midiastore.testmediaowner`; teste exige pacote e UID distintos, `OWNER_PACKAGE_NAME` efetivamente igual ao helper, solicitação de consentimento, cancelamento sem exclusão, consentimento renovado, aprovação e confirmação de URI ausente. Tudo compila, mas nenhum runtime API29 foi executado. O Motorola API35 não valida API29; AVD API29 continua impedido por aceleração indisponível neste Windows.
- **NOT RUN — pipeline Linux para as alterações locais:** workflow `42080940375` refere-se ao SHA anterior `16545a4`. As alterações locais não foram enviadas. A execução API29/API35 sobre estas alterações exige autorização específica para preparar/publicar um commit na branch associada à PR.
- `git diff --check` passou; apenas avisos de conversão LF/CRLF em arquivos Windows. Não foram removidas fixtures sintéticas pessoais: auditoria de MediaStore passou para os diretórios próprios de teste.

### Continuação após investigação do consentimento — 2026-11-06

- **PASS — build local do estado atual:** `./gradlew.bat --no-daemon -Pkotlin.compiler.execution.strategy=in-process :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug`; `BUILD SUCCESSFUL`, 114 tarefas (11 executadas, 103 up-to-date), 4m38s. Lint continua com 184 findings conhecidos e `abortOnError=false`.
- Identificado o fator ambiental da falha intermitente no Motorola: `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` contém `com.truedevelopersstudio.automatictap.autoclicker/...WorkerService`. Durante consentimento, `MediaProvider` registrou “User allowed grant” ~0,9 s após abrir o diálogo; `UiAutomator` esperava `android:id/button2` até timeout e não o encontrava. O estado do dispositivo confirma que esse teste não controla o toque. Nenhuma configuração global foi alterada.
- Alteração no harness: os testes API30+ e API29 que dependem de decisão manual do consentimento agora usam assumption explícita se detectar serviço externo de auto-click. A assertion positiva/negativa continua intacta; nessas condições o caso é SKIP, não PASS.
- **PASS — AndroidJUnitRunner direto, APK AndroidTest atual, Main, API35:** `OK (4 tests)` no protocolo, exit code 0; um teste funcional de Home executado. Três casos foram interrompidos por assumptions de nível API ou pelo auto-clicker (logcat registra isso). Não conta como aprovação do consentimento.
- **PASS — AndroidJUnitRunner direto, APK AndroidTest atual, MediaStore, API35:** classe `OK (5 tests)` no protocolo; log detalhado mostrou que o teste partial visual foi assumption-skipped por permissões já concedidas no aparelho, e quatro métodos realmente executaram. Depois configurei temporariamente o pacote Debug via ADB para visual-selected=true e imagens/vídeo/áudio=false; o caso isolado `aSelectedVisualPermissionIsReportedAsPartialCoverage` passou (`OK (1 test)`, exit 0). Restaurei exatamente o estado original: visual-selected=true, images=true, audio=true, video=false. Os outros quatro métodos, incluindo auditoria de fixtures, passaram na classe.
- **FAIL — UTP atualizado, API35 Motorola:** `:app:connectedDebugAndroidTest` filtrado para `MediaStorePagingInstrumentedTest`, exit code 1 após cerca de 2 min. HTML informa 0 testes/0 falhas; `utp.0.log` contém somente “Constructing runner from config” e “Fatal error while executing main”. Nenhum XML foi produzido. O `utp-debug.log` anterior demonstra erro PEM RSA no `UtpTestResultListenerServer` (BouncyCastle `malformed sequence in RSA private key`) e class-not-found opcional de gRPC census; isso aponta para inicialização do coletor local, mas a causa raiz ainda não foi provada.
- Checksums dos APKs após build: Debug `EAE637CEF8F581B65B5D8BCDEEF97ADBC4BA8699D38716BBA4A818AD6045091E`; AndroidTest `55EAA999F40E3F0424A2271A5265AADA08167D3C23611A3F52ABDBD13C20E3CD`; helper `B00066BF7FF33116216619D728453D0E38E8525DA0C6F5FD28C590976F5632A1`.
- Logs/protocolos, códigos de saída e inventário permanecem em `.local-test-results/2026-11-06/`. UTP usou os mesmos APKs Debug/AndroidTest atualmente construídos, mas não coletou nenhum resultado de caso.
- API29 segue **NOT RUN**: o helper e a assertion de `OWNER_PACKAGE_NAME` real compilam; nenhum runtime API29 foi alcançado. O emulador local continua sem aceleração, portanto a próxima validação é no runner Linux API29/API35.
- Nenhuma fixture foi apagada fora de `Pictures/MidiastoreCodexTest/` e `Music/MidiastoreCodexTest/`; a auditoria sintética passou. Sem alterações em produção, Billing, signing, versioning ou dados pessoais.
- A primeira reconsulta GitHub dentro do sandbox falhou pelo proxy local `127.0.0.1:9`; repetição read-only autorizada fora do sandbox confirmou PR #3 OPEN/Draft, head `16545a4277bcc515660a6516c32fb6c770a4e880`, Issue #2 OPEN e workflow `42080940375` failure no SHA exato. Jobs: Windows script PASS; API29/API35 instrumentação FAIL; signed-release SKIPPED.

### Próxima ação

Revisar o diff combinado completo (staged, unstaged e novos) e montar um conjunto de sincronização excluindo caches/logs pessoais; então pedir autorização única para push da branch da PR para executar CI Linux/API29. Em paralelo, resolver UTP no host/toolchain e obter consentimento em ambiente sem auto-clicker. PR #3 Draft e Issue #2 aberta.

### Próxima ação

Revisar o diff combinado staged/unstaged e escolher um conjunto de commit que não inclua artefatos locais/segredos; então pedir autorização única e explícita para enviar o commit de código/documentação à branch da PR a fim de executar a matriz Linux API29/API35. Até lá, prosseguir apenas em trabalho local independente; não declarar P0 fechado. PR #3 permanece Draft, Issue #2 aberta.

### Diagnóstico de user home/assinatura Debug e rerun — 2026-11-06

- Uma tentativa de isolar UTP mudando `ANDROID_USER_HOME` não alcançou UTP: criou outro `debug.keystore` (SHA-256 `86163381EA6E1CEBB003237D8501B7F16776496CA31345E12E8F59ADD9A13BD6`, comparado ao original local `F8C24286C11B3734452D373C18B04EC08042A0734D50A0D3D5EA256BD998C7BF`) e `:mediaOwnerTest:installDebug` falhou `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Essa execução é FAIL de instalação e não conta como teste UTP.
- Retornado ao user home Android original e reconstruídos APKs sem instalar variante incompatível. `:app:assembleDebug :app:assembleDebugAndroidTest :mediaOwnerTest:assembleDebug` terminou `BUILD SUCCESSFUL` (98 tarefas; 3 executadas, 95 up-to-date). Debug/helper recuperaram hashes validados; AndroidTest foi reempacotado e agora tem SHA `15344F181D5AAE73BF2945D84BF850220FD30DADE45005AABE91C7223F79DCD1`.
- Runner direto repetido nesse AndroidTest APK: `MediaStorePagingInstrumentedTest OK (5 tests)` e `MainActivityInstrumentedTest OK (3 tests)` no protocolo, com os dois casos API29 ignorados; ambos exit code `0`. O UTP anterior que falhou rodou sobre hash `D6F1C760...`, que também havia passado no runner direto. Assim, cada comparação direta/UTP usa APK idêntico no seu próprio par; o hash mais novo ainda não foi submetido ao UTP.
- Keystores são somente de desenvolvimento; nenhum Release foi assinado ou alterado. Checksums atuais completos em `final-apk-sha256.txt`.

### Estado e continuidade

- Diretório: `C:\Users\rodolfo\midiastory-`; branch `codex/midiastore-release-audit`.
- HEAD local: `16545a4277bcc515660a6516c32fb6c770a4e880`, igual ao commit remoto confirmado da branch `fix/p0-sweep-permissions-missions-billing`.
- GitHub (consulta read-only): PR #3 aberta como Draft; Issue #2 aberta. Workflow `42080940375`, SHA `16545a4`, falhou nos jobs instrumentados API29/API35. PR/Issue não foram alteradas e nada foi enviado.
- Árvore contém staged, unstaged e untracked preexistentes e artefatos de diagnóstico. Tudo foi preservado; sem reset/clean/checkout destrutivo.

### Investigação e alterações

- Inspecionados `docs/CONTINUATION_HANDOFF.md`, `docs/QUALITY_AND_TESTING.md`, `docs/RELEASE_BLOCKERS.md` e seis documentos em `docs/research/`, além dos artefatos de CI baixados em `.local-ci-42080940375/`.
- Artefato API29 confirma que a fixture antiga tinha `OWNER_PACKAGE_NAME=com.rdf112.midiastore.debug`, igual ao alvo, em vez do pacote instrumentador. A fixture local agora é inserida por `:mediaOwnerTest`, com UID distinto, e o teste consulta a propriedade reportada pelo MediaStore sem falsificar owner.
- Artefatos API35 confirmam consultas prematuras ao volume fixo `external_primary` e encerramento do processo ao revogar permissão durante instrumentação. Os testes agora esperam volumes externos descobertos pelo MediaStore e verificam as pré-condições de acesso parcial sem revogação em processo.
- Revisada a assertion `SESSION_STALE`: o teste não aprova mais somente porque a fonte mudou. Ele confirma `SOURCE_CHANGED`, reinicia uma sessão limitada e exige que uma sessão estável conclua leitura/rejeição explícita; caso contrário falha.
- Atualizado ADR-03/ADR-05 para remover afirmação de prova estável que não se aplica ao novo APK ainda não executado.
- A alteração Core KTX 1.19.1 não compilava com compileSdk 36/AGP 8.13.2; a árvore mantém Core KTX 1.17.0. Build completo de domínio/JVM/Lint/Debug/AndroidTest/fixture já havia passado na sessão anterior; Lint reporta 90 `MissingTranslation` e 94 warnings, embora a task termine com sucesso.

### Testes e evidências

- Comando executado nesta continuação: `./gradlew.bat --no-daemon :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest`.
- Resultado: `BUILD SUCCESSFUL`, 49 tarefas (4 executadas, 45 up-to-date). Kotlin daemon recebeu `AccessDeniedException` em `%LOCALAPPDATA%\kotlin\daemon`; compilação fallback terminou com sucesso.
- Recompilação incremental após remover import não utilizado: `:app:compileDebugAndroidTestKotlin` também `BUILD SUCCESSFUL` (30 tarefas: 1 executada, 29 up-to-date).
- SHA-256 dos artefatos locais consultados: Debug APK `0E550D445D231F9B65EBACEA3AA18727764AFD05014294B1D49905B2AD6BA632`; AndroidTest APK `BEF4E449AED53347E405B625D183CB41CE3B8D8262B6F2F9C9B273881AB9424D`. O AndroidTest APK foi empacotado antes da remoção do import não utilizado; o código executável não mudou depois.
- Repetição Gradle `:app:connectedDebugAndroidTest -x :mediaOwnerTest:installDebug` voltou a falhar em `Failed to receive the UTP test results`, sem testes coletados. Após UTP, `adb devices -l` não listou o serial `0085353748`; o dispositivo desapareceu da conexão.
- Diagnóstico UTP local disponível é incompleto: `app/build/outputs/androidTest-results/connected/debug/moto g84 5G - 15/utp.0.log` registra apenas `Constructing runner from config` e `Fatal error while executing main` com caminhos dos proto-configs, sem stack/cause. O log Gradle confirma que o APK auxiliar foi instalado antes da perda de resultados. A conexão ADB não estava presente para coletar logcat/processo no instante seguinte.
- A sessão anterior registrou execução direta no Motorola API35 `OK (7 tests)` antes da nova assertion de estabilidade. Essa evidência não valida o APK AndroidTest compilado nesta continuação. Necessário reconectar e executar runner direto no APK atual.
- API29: AVD criado com imagem API29, mas não iniciou por falta de aceleração x86_64/hypervisor. Não instalado driver nem alteradas proteções do Windows. Sem resultado instrumentado API29.
- Testes ignorados/condicionais: os dois casos específicos de API29 eram condicionais quando a suíte direta foi executada em API35; contagem `OK (7)` incluía 2 ignorados por API. O caso parcial API35 foi executado anteriormente com permissões preparadas via ADB, mas precisa ser repetido com APK novo.

### Arquivos/documentação

- Alterados: teste instrumentado MediaStore, handoff, qualidade, release blockers e ADRs de sessão/permissões.
- Criado `docs/DAILY_STATUS.md` após confirmar que não existia.
- Novos artefatos baixados de CI permanecem em `.local-ci-42080940375/` para diagnóstico; não são fonte de estado remoto atualizado.

### Bloqueios e próxima ação

- Reconectar/desbloquear o Motorola e autorizar depuração USB; confirmar serial/modelo/API antes de qualquer novo teste. Em seguida instalar/verificar APKs atuais, executar caso Selected Photos isolado e a suíte AndroidJUnitRunner direta, capturando XML/logcat/código de saída; diagnosticar UTP separado.
- Executar correção de propriedade/RecoverableSecurityException em API29 num CI/API29 ou host com virtualização já habilitada.
- Não considerar P0 fechado, não fazer push/merge, manter PR #3 Draft e Issue #2 aberta. Billing/backend, Play Console, assinatura, política/Data Safety e release seguem gates abertos.
