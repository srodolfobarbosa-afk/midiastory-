# Midiastore — CONTINUIDADE DO PROJETO

## Direção de produto confirmada pelo proprietário — 2026-11-06

- Rodolfo confirmou que o objetivo é um phone cleaner simples e confiável, inspirado na facilidade de uso do Files do Google: ajudar pessoas com o telefone cheio de conteúdo a encontrar o que ocupa espaço e liberar armazenamento sem perda acidental.
- Rodolfo autorizou explicitamente publicar no GitHub as atualizações do workspace; publicar após revisar e reconciliar os três commits remotos mais recentes, mantendo a PR em Draft e sem fazer merge ou release.
- Modelo pretendido: um plano gratuito e três opções pagas. O código-fonte existente nomeia as opções Pro mensal, anual e vitalícia; IDs e preços ainda dependem de confirmação no Play Console. Não inventar preços ou prometer recursos não implementados.
- Referências públicas pesquisadas: [Android Now in Android](https://github.com/android/nowinandroid) para arquitetura e testes; [SpaceSweep](https://github.com/Dhiva-Labs/SpaceSweep) e [Quarry](https://github.com/tanvirr007/quarry-app) para ideias de apresentação de espaço e revisão de limpeza. São referências de leitura, não dependências nem validação das alegações de seus autores. Manter a arquitetura Android Views existente e adotar somente padrões compatíveis com as permissões e limites do Midiastore.
- Prioridades até a publicação: confiabilidade da varredura e cobertura parcial explícita; seleção e exclusão sempre revisadas pelo usuário; acessibilidade e português/inglês; CI e testes reais; verificação de compra via backend e configuração Play. Não anunciar limpeza de cache de outros apps, fotos semelhantes, acelerador ou outras funções sem implementação e validação.

### Verificação executada nesta retomada

- `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` e `:mediaOwnerTest:assembleDebug`: **BUILD SUCCESSFUL** em 7m43s; 114 tarefas (1 executada, 113 up-to-date). Gradle e Android caches foram direcionados para diretórios locais ignorados do repositório.
- `:app:connectedDebugAndroidTest`: **não executado**; falhou em `:mediaOwnerTest:installDebug` com `No connected devices!`. Isso não é falha dos testes do app e também não é evidência de aprovação instrumentada.
- `git diff --check` e `git diff --cached --check`: sem erros de whitespace.
- Commit local `a8e0335` contém o diff de app, testes e documentos intencionais. O merge local está reconciliando a branch remota atual `c546b76`; os commits remotos e o trabalho local estão preservados. Ainda não houve push.

### Atualização após novo avanço da branch — 2026-11-06

- A branch remota avançou mais uma vez até `c546b76` enquanto o trabalho local era preparado, com 14 commits adicionais. Eles incluem um dashboard de limpeza com hierarquia de ações inspirado no Files do Google e melhorias para MediaStore/API29/API35. Nenhum avanço remoto foi sobrescrito.
- O merge desses commits está em andamento na branch local. Para os conflitos, foram mantidos os testes locais mais completos de proprietário/UID, cancelamento e aceite de consentimento, os testes de cobertura/paginação e o preparo isolado de Selected Photos; a configuração Kotlin/JVM e o manifesto mais recentes do app auxiliar vieram da branch da PR.
- A suíte completa foi repetida sobre o código combinado: **BUILD SUCCESSFUL** em 8m24s; 117 tarefas (31 executadas, 2 do cache, 84 up-to-date). Incluiu Domain, JVM, Lint, APK Debug, APK AndroidTest e helper.
- ADB continua sem dispositivo (`adb devices -l` vazio) mesmo após `adb start-server`; o Motorola não está acessível ao host. Não marcar instrumentação física como executada. O CI remoto é necessário para validar API29/API35 enquanto o aparelho não aparece no ADB.
- Rodolfo autorizou compilar o AAB e enviar ao Play Console quando os critérios estiverem satisfeitos. `:app:bundleRelease` executou compilação, recursos e R8, mas terminou **FAILED** pela proteção do projeto: falta a chave oficial e `keystore.properties`. Foi deixado um AAB local de saída do Gradle, ainda sem verificação de assinatura e proibido para upload; a menção a AAB no Gemini não o disponibiliza neste workspace. Não gerar chave substituta.
- Play continua bloqueado pelos gates registrados em `docs/RELEASE_BLOCKERS.md`: consentimento/API29 executado, CI atual, validação Billing no Play Console e verificação server-side do purchaseToken, versionCode/certificado e revisão final de privacidade. A autorização do proprietário não substitui essas provas.

## Checkpoint confirmado — 2026-11-06 (continuação)

- Estado Git: `codex/midiastore-release-audit`, HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`, remoto de recuperação ainda no mesmo SHA conforme leitura anterior. Há mudanças staged, unstaged e untracked; preservadas. Não houve commit, push, reset, limpeza ou merge.
- GitHub: PR #3 aberta como Draft; Issue #2 aberta. O workflow `42080940375` está associado ao SHA base anterior e não testa estas mudanças locais.
- Dispositivo: Motorola moto g84 5G / Android 15 API35 / `0085353748`, reidentificado e online. Debug/helper/teste têm UIDs separados (10475/10476/10477; o auxiliar `.codex.test` ficou em 10478). Prova de UID no aparelho não é prova de API29 nem de owner MediaStore para a fixture estrangeira.
- Builds locais: suite completa Domain, JVM, Lint, Debug, AndroidTest e helper passou; recompilação final dos testes também passou. Ver comandos/checksums/resultados detalhados no final de `docs/QUALITY_AND_TESTING.md` e `docs/DAILY_STATUS.md`.
- AndroidJUnitRunner direto com APKs finais: MediaStore `OK (5 tests)`, incluindo acesso visual parcial, áudio independente, paginação/session, hash de conteúdo e ausência de fixtures residuais; classe Main `OK (3 tests)` no protocolo, mas 2 casos API29 foram ignorados, restando 1 caso executado. Isso valida apenas API35.
- Gradle connected/UTP no mesmo AndroidTest APK falhou novamente sem XML/zero casos coletados (`Failed to receive the UTP test results`), enquanto ADB continuou online e o logcat pós-execução não mostra `AndroidRuntime` fatal. Log debug anterior aponta falha ao carregar chave PEM RSA no Netty/BouncyCastle do listener local gRPC do UTP; hipótese proximate de collector/host, não causa raiz comprovada. Resultado UTP permanece FAIL, runner direto PASS.
- API29 consent estrangeiro permanece NOT RUN. Fixture auxiliar, confirmação MediaStore de owner, cancelamento, novo consentimento, aceite e confirmação pós-exclusão estão implementados e compilados; requer runner API29 Linux/host com virtualização existente. Não deduzir sucesso da compilação/UID/API35.
- Artefatos desta sessão: `.local-test-results/2026-11-06/`; nenhuma mudança de assinatura, identidade, versão, pacote produção, compra ou dados pessoais.
- O relatório `docs/RELATORIO_MIDIASTORE_RETOMADA_2026-11-06.md` não está presente localmente e não havia cópia equivalente encontrada; a missão e handoff existentes forneceram continuidade, sem criar documento duplicado. Consulta GitHub read-only final repetiu PR #3 OPEN/Draft em SHA `16545a4277bcc515660a6516c32fb6c770a4e880`, Issue #2 OPEN e run `42080940375` failure no SHA exato.

### Próxima tarefa concreta

Completar revisão integral e segura do diff combinado, preparar commit contendo apenas código/testes/docs intencionais (excluindo caches e capturas locais), e pedir uma autorização única de push para a branch da PR executar a matriz Linux API29/API35. Até essa autorização, continuar validação e correções locais sem push. Manter issue e PR como estão; P0 API29 e UTP continuam abertos.

Nota de validação posterior: teste isolado com `ANDROID_USER_HOME` novo falhou na instalação do helper por keystore Debug diferente, antes da instrumentação. User home original restaurado; APK Debug/helper mantiveram hashes e AndroidTest reempacotado SHA `15344F181D5AAE73BF2945D84BF850220FD30DADE45005AABE91C7223F79DCD1` passou no runner direto (MediaStore 5 testes; Main protocolar 3, dois API29 ignorados). UTP FAIL anterior foi no AndroidTest SHA `D6F1C760...`, igualmente aprovado diretamente; não confundir com a tentativa de instalação incompatível. API29 segue NOT RUN.

## Continuidade atualizada — 2026-11-06, após retomada

- Diretório `C:\Users\rodolfo\midiastory-`; branch `codex/midiastore-release-audit`; HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`. Remote `origin` correto. Árvore continua com alterações staged, unstaged e untracked; nenhuma foi descartada. O AndroidTest atual recebeu uma alteração adicional de harness para detectar serviço de auto-click externo.
- O teste API35 de exclusão/consentimento falhou intermitentemente porque um serviço externo habilitado (`com.truedevelopersstudio.automatictap.autoclicker`) toca na UI do sistema antes do UiAutomator. O log do MediaProvider registrou concessão automática; não é prova de cancelamento controlado. Os testes de consentimento agora ficam SKIP por assumption nesse ambiente, mantendo todas as assertions. Não alterar configuração global do aparelho.
- Build completo atual PASS: Domain, app JVM, Lint, Debug, AndroidTest e helper, 114 tarefas/11 executadas. APKs: Debug SHA-256 `EAE637CEF8F581B65B5D8BCDEEF97ADBC4BA8699D38716BBA4A818AD6045091E`; AndroidTest `55EAA999F40E3F0424A2271A5265AADA08167D3C23611A3F52ABDBD13C20E3CD`; helper `B00066BF7FF33116216619D728453D0E38E8525DA0C6F5FD28C590976F5632A1`.
- Runner direto atual no Motorola API35: classe MediaStore `OK (5 tests)` no protocolo, quatro métodos executados e o caso partial visual skipped por permissões preexistentes; executado novamente isolado após preparar via ADB visual-selected=true e image/video/audio=false, `OK (1 test)`. O estado original de permissões foi restaurado. Main `OK (4 tests)` no protocolo, com 1 caso funcional executado e 3 assumptions/skips. Não contar consentimento nem API29 como aprovados.
- UTP mais recente, filtrado para MediaStore e usando APKs atuais, FAIL antes de coletar testes: HTML report `0 tests`, sem XML; `utp.0.log` fatal na inicialização. Investigação anterior de `--debug` registra erro de leitura de RSA PEM no listener gRPC local do AGP/UTP; hipótese de falha do coletor/host, causa raiz não confirmada.
- API29 permanece NOT RUN. Emulador local não tem aceleração disponível; teste auxiliar precisa de API29 Linux para confirmar UID/owner no MediaStore, RecoverableSecurityException, cancelar, aprovar e verificar estado posterior.
- Logs atualizados: `.local-test-results/2026-11-06/`. PR #3 permanece OPEN/Draft e Issue #2 OPEN; nenhum push/commit/merge nesta retomada.
- A primeira consulta `gh` dentro do sandbox falhou pelo proxy local `127.0.0.1:9`; consulta read-only autorizada fora do sandbox confirmou novamente PR #3 OPEN/Draft, head `16545a4277bcc515660a6516c32fb6c770a4e880`, Issue #2 OPEN e workflow `42080940375` failure nesse SHA. Windows-script PASS; API29 e API35 FAIL na etapa instrumentada; signed-release SKIPPED.

### Próxima tarefa concreta

Revisar integralmente o diff combinado e separar somente alterações de projeto intencionais dos artefatos locais; depois solicitar autorização específica para push à branch da PR a fim de executar CI Linux/API29. Continuar independentemente a investigação da falha UTP e não fechar os gates de consentimento/API29 antes de resultados executados.

Atualizado: 04/11/2026

## Identidade correta

- Nome comercial e listing: **Midiastore**.
- Package/Application ID: `com.rdf112.midiastore`.
- O proprietário confirmou o package olhando a ficha de teste interno da Play Console.
- Namespace e pastas Kotlin ainda usam `com.rdf112.sweep`; isso é legado técnico e não altera o Application ID.
- `versionCode=17` e `versionName=2.2.1` são os valores atualmente configurados no código-fonte. O maior versionCode publicado precisa ser consultado; o script de Release agora exige que o source seja estritamente maior.
- minSdk 29, targetSdk 36, compileSdk 36, JDK 17 e Gradle 8.13.
- O proprietário informa que habilitou o perfil financeiro/recebimento em 03/11/2026. Isso não confirma cadastro ou ativação dos produtos de Billing.

## Branch e revisão de trabalho

- Branch de recuperação: `fix/p0-sweep-permissions-missions-billing`.
- PR em rascunho: https://github.com/srodolfobarbosa-afk/midiastory-/pull/3
- Issue de recuperação: https://github.com/srodolfobarbosa-afk/midiastory-/issues/2
- Base da PR: `main`.
- Não fazer merge nem publicar sem autorização do proprietário.
- Não alterar Application ID, versionCode ou certificado de assinatura sem confirmação explícita.

## Trabalho já realizado nesta branch

1. Removido o caminho fixo `org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64`, permitindo que Gradle utilize o JDK definido pelo ambiente (JAVA_HOME).
2. Alinhada a marca visível e o ícone interno com Midiastore.
3. Ajustado o teste instrumentado para o Application ID atual `com.rdf112.midiastore.debug`.
4. Criada ação de revisão de permissões parciais Android 14+ também na aba Arquivos.
5. Missão clicada antes da primeira leitura passa a ser retomada depois que a primeira página carrega.
6. Os botões de plano exibem mensagem quando o Billing não consegue iniciar o fluxo.
7. O estado de reconciliação do Billing não é apresentado como plano gratuito.
8. Filtros com texto cortado tiveram altura aumentada; vários rótulos técnicos foram simplificados.
9. Arquivos do mesmo tamanho sem SHA-256 confirmado não são classificados como duplicatas.
10. Resposta nula do MediaStore não é interpretada como prova de exclusão.
11. Arquivos movidos para a Lixeira não são contabilizados como espaço liberado; exclusão só credita bytes após confirmação.
12. Adicionada uma varredura global cancelável por categorias, volumes e páginas. Duplicatas entre páginas são comparadas via SHA-256 em streaming. Limites: 2 GiB por arquivo e 10 GiB de hash por missão; hashing normal de página: 512 MiB por arquivo e 1 GiB por página. Se qualquer limite ou fonte não verificada for atingido, a cobertura é parcial e duplicatas não são pré-selecionadas para exclusão.
13. Acrescentados testes de domínio/VM para duplicatas entre páginas e para bloquear seleção automática em cobertura incompleta.
14. CI agora tenta executar testes instrumentados em emulador Android 35 e guardar relatórios JVM/Android para diagnóstico.
15. Implementado caminho de exclusão permanente no Android 10/API29 usando RecoverableSecurityException e consentimento oficial do sistema por URI. A verificação pós-exclusão consulta cada URI; bytes só são creditados para itens comprovadamente ausentes. Lixeira/restauração continuam API30+.

## Limites que continuam abertos

- A varredura global e as novas alterações ainda precisam de CI verde no commit mais recente.
- O primeiro teste instrumentado do MediaStore falhou no emulador Android 15 e necessita de diagnóstico; não assumir que é apenas instabilidade do emulador.
- O fluxo de exclusão permanente API29 foi implementado, mas ainda não foi validado em emulador API29 nem aparelho físico; Lixeira/restauração continuam indisponíveis nessa API.
- O BillingClient está integrado no código, mas os SKUs e ofertas não foram validados na Play Console.
- O cliente não valida criptograficamente o purchaseToken através de backend confiável. Não anunciar o entitlement como verificação de produção.
- Nenhum teste de compra real, assinatura, restauração, reembolso ou expiração foi concluído.
- Não há validação física em Samsung, Xiaomi, Motorola ou cartão SD.
- Histórico persistente de varreduras, Downloads/documentos via SAF, similaridade visual e compressão não estão implementados.
- Certificado de upload e maior versionCode publicado ainda precisam de confirmação da Play Console.
- Nenhum AAB desta branch está aprovado para publicação.

## Artefatos de teste

- O workflow exporta `midiastore-debug-apk-api29` e `midiastore-debug-apk-api35` com SHA-256, quando os jobs chegam ao upload.
- O APK Debug usa o package `com.rdf112.midiastore.debug` por causa de `applicationIdSuffix = ".debug"`. Ele serve para validar interface e funções locais e pode coexistir com a instalação do Play.
- O APK Debug não valida compras Google Play e não substitui a instalação assinada do listing `com.rdf112.midiastore`. Para testar Billing, é necessário AAB assinado com a chave de upload oficial, enviado à faixa de teste interno/fechado.
- Não pedir ao testador para repetir a avaliação do link antigo antes de instalar uma build nova desta branch.

## Como validar no Codespaces

Confirme que está na branch de recuperação:

```bash
git status --short --branch
git switch fix/p0-sweep-permissions-missions-billing
git pull --ff-only
```

O comando de build:

```bash
./gradlew --no-daemon :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Com emulador/telefone conectado:

```bash
./gradlew --no-daemon :app:connectedDebugAndroidTest
```

Se o Codespace estiver em `main`, o `gradle.properties` antigo pode continuar apontando para um JDK inexistente. A correção está nesta branch, não na main.

No computador Windows, `adb: no devices/emulators found` indica que nenhum Android está conectado. Ative Depuração USB e autorize o computador, ou inicie um emulador no Android Studio. Não é evidência de falha do aplicativo.

## Próximas prioridades

1. Concluir CI e resolver falhas reais de teste.
2. Executar teste MediaStore em APIs 29, 30, 33, 34, 35 e 36 e dispositivos de fabricantes diferentes.
3. Corrigir limpeza/API29 ou reavaliar minSdk mediante impacto nos usuários existentes.
4. Validar o catálogo de Billing, purchase tokens e entitlement seguro.
5. Adicionar histórico local persistente de análises e reconciliação.
6. Reestruturar a interface para fluxo de uma ação principal e navegação clara.
7. Revisar acessibilidade, escalas de fonte, TalkBack, orientação e tablets.
8. Confirmar package, versionCode e upload certificate no Play Console.
9. Só então gerar Release/AAB assinado pela chave legítima e publicar primeiro em teste interno.

## Regra de trabalho

Não começar do zero, não descartar código já existente e não confundir documentação, código compilável e funcionalidade comprovada. Cada funcionalidade precisa de teste e evidência. Nunca dizer que a biblioteca foi limpa, que o espaço foi liberado ou que uma compra foi validada sem confirmação técnica do resultado.

## Continuidade — 04/11/2026

- Posicionamento confirmado: Phone Cleaner / limpador inteligente de celular.
- Modelo comercial: Freemium com benefício Pro de exportação de relatório, único já implementado no cliente.
- Medidor: armazenamento compartilhado primário, não espaço privado /data; SD/USB não somados automaticamente.
- Testes novos: quatro casos para StorageUsageCalculator, aguardando CI.
- Localização: PT-BR/EN/fallback 152 chaves; outros seis idiomas 63/152 cada (534 faltantes).
- Pesquisa de mercado e tecnologia atualizada; nenhum código de terceiros foi incorporado.
- Manter PR #3 em Draft; não mergear, publicar ou gerar release assinado sem gates concluídos.

## Estado exato do HEAD — 04/11/2026, 16:22 BRT

- Branch `fix/p0-sweep-permissions-missions-billing`, HEAD `d60ff8f8c0852ab7c33ce638d662053c028d4916`.
- PR #3: aberta, Draft, base `main`, não merged.
- `main` permanece no SHA `3759e176c48f340fa576a06455579277e514d9d9`.
- GitHub Actions run [41866189110](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/41866189110), associado exatamente ao HEAD acima, está **queued**. Jobs API29, API35 e Windows também estão queued; não há resultado de build/teste para esse SHA.
- O último ajuste funcional liga o botão principal da home à missão global de arquivos grandes e retoma a tela de progresso após autorização de mídia. Foi adicionado teste instrumentado da existência/clickabilidade da ação principal; não foi executado ainda.
- Não declarar compilação, Lint, testes JVM, instrumentação ou APK gerado neste HEAD até obter logs do run exato.

## Jornada principal Phone Cleaner — atualização de 04/11/2026

A ação principal da Home agora executa a missão global `FREE_MAX_SPACE_NOW`, que percorre as categorias, volumes e páginas MediaStore acessíveis para propor arquivos grandes. Ela não exclui nada automaticamente. O pedido inicial de permissões preserva a missão pendente e, após autorização, retorna à aba Limpeza para exibir progresso. A navegação de mídia paginada é separada como “Abrir arquivos / Browse files”.

A melhoria está no source do HEAD `d60ff8f8c0852ab7c33ce638d662053c028d4916`. Há um teste instrumentado da ação principal, ainda sem execução por estar o CI queued. O botão principal não deve ser interpretado como uma análise sem limites: acesso parcial, provider e limites de segurança continuam visíveis na cobertura global.

## Operating model for AI-assisted execution — 04/11/2026

The owner wants the assistant to act as the technical manager of the Midiastore recovery, not to make the owner manually coordinate multiple agents or repeatedly run setup commands.

### Execution contract

- Use the connected GitHub repository tools to inspect, edit and verify files directly on `fix/p0-sweep-permissions-missions-billing` whenever the change is safe and within scope.
- Read the current source, CI logs, test reports and release blockers before proposing or making a change. Do not restart the project or repeat completed setup steps.
- Use the specialist roles (architecture, QA, security/privacy, performance, growth/revenue and implementation) as review responsibilities. Do not claim six independent agents are running unless an actual agent runtime confirms that.
- The local Codex extension and `.codex/agents/*.toml` profiles are developer tools; they do not provide unattended background workers by themselves.
- A GitHub Actions Codex agent can run unattended only after an API provider credential is configured. The official `openai/codex-action@v1` requires an `OPENAI_API_KEY` GitHub Actions secret and uses API billing. Do not add a workflow that requires paid credentials or ask the owner to create/buy them without explicit approval.
- GitHub Actions Android CI is the persistent automatic build/test monitor for pushes and PR updates. It does not itself autonomously repair Kotlin source.
- Do not ask the owner to run local commands unless access to their device, Play Console, physical handset, or a local artifact is genuinely necessary.
- Never promise or imply guaranteed revenue. Optimize for a stable, compliant release and measurable monetization validation.

### Latest verified CI investigation

- Branch HEAD before the CI adjustment: `7dae227c2ff0e60a12e3ecff205847e158725f2a`.
- Run `41917665474`: Windows script parser passed; Gradle domain/JVM tests, lint, Debug APK and AndroidTest APK build completed before instrumentation.
- API29 instrumentation did not start: ADB reported the emulator offline / unknown API level.
- API35 instrumentation process was killed with SIGKILL while the Google APIs image was starting Google Play Services/setup components. The failing report did not contain a Kotlin assertion stack trace. Do not classify the MediaStore adapter as correct or incorrect from this result alone.

### CI adjustment applied

- Commit: `6e193284a8e0914b1f649cb9e415f7b275b46c9b`.
- File: `.github/workflows/android.yml`.
- Changed the emulator image from `google_apis` to AOSP `default` and explicitly disabled animations. The MediaStore instrumentation tests remain enabled; no test was removed or weakened.
- New Android workflow run: `41957550437`, queued when this handoff was updated. Await its exact result before claiming success.
- A diagnostic note was posted to PR #3. PR remains Draft; main, signing configuration, application identity, version values and Issue #2 were not changed.

### Immediate work queue

1. Verify run `41957550437` against exact SHA `6e193284a8e0914b1f649cb9e415f7b275b46c9b`.
2. If instrumentation still fails, inspect the new API29/API35 XML and logcat artifacts; repair the actual failure without skipping tests.
3. Validate global MediaStore scanning, Android 10 RecoverableSecurityException deletion, partial permissions, cancellation and post-delete byte accounting on supported API levels.
4. Implement and security-test trusted purchase-token verification before enabling Pro purchases; confirm SKUs, offers and prices in Play Console.
5. Confirm highest published versionCode, upload certificate, privacy/Data Safety and final store listing.
6. Keep the PR in Draft and do not merge, generate a production release AAB, or publish until release gates pass and the owner authorizes it.

This section is the durable continuity checkpoint for future conversations. Continue from these exact facts and do not make the owner repeat them.

## Retomada local — 2026-11-06

- Workspace preservado na branch `codex/midiastore-release-audit`, HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`, igual ao commit remoto confirmado. Existem alterações staged, unstaged e arquivos locais não rastreados da sessão anterior; nenhum foi descartado. PR #3 permanece Draft e Issue #2 não foi alterada.
- As alterações anteriores de testes, fixture `mediaOwnerTest`, documentação de pesquisa e atualizações de dependências permanecem locais. A alteração unstaged para Core KTX 1.19.1 impedia o build: exige compileSdk 37 e AGP 9.1, incompatíveis com o SDK 36/AGP 8.13.2 deste projeto. Core KTX foi fixado em 1.17.0, mantendo as demais dependências e a configuração de identidade/versão.
- Build local Windows passou: `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` e `:mediaOwnerTest:assembleDebug`. Lint concluiu com issues existentes; build não significa relatório limpo.
- No Motorola moto g84 5G (API 35, serial `0085353748`), `:app:connectedDebugAndroidTest` falhou no UTP sem receber resultados e gerou relatório com zero testes. Executando os APKs instalados pelo `AndroidJUnitRunner` via ADB, a suíte concluiu `OK (7 tests)`.
- A primeira execução direta observou a geração MediaStore mudar durante consultas e o adapter retornar `SESSION_STALE` com `SOURCE_CHANGED` confirmado, sem registros ambíguos. Os testes de paginação/áudio agora reconhecem esse caso apenas quando a revisão realmente prova a mudança da fonte; resultados inesperados continuam falhando.
- API29 ainda não foi validado: não há AVD API29 neste ambiente. O teste foi preparado para executar o consentimento com mídia criada por um app auxiliar de UID distinto e conferir a atribuição reportada pelo próprio MediaStore, mas requer execução real em API29. Não inferir resultado API29 a partir do moto g84/API35.

## Continuação da validação — 2026-11-06

- Branch `codex/midiastore-release-audit`, HEAD local e base remota `16545a4277bcc515660a6516c32fb6c770a4e880`. Árvore segue com mudanças staged/unstaged e arquivos novos; nada foi descartado ou enviado.
- Consultas read-only confirmaram PR #3 aberta e Draft, Issue #2 aberta, workflow `42080940375` falho no SHA `16545a4`. Os artefatos locais `.local-ci-42080940375/` mostram que API29 falhou porque MediaStore atribuiu a fixture ao UID do app alvo; API35 falhou ao assumir `external_primary` antes de haver volume disponível e encerrou processo ao revogar permissões durante o teste.
- Fixture API29 agora vem do provider de `:mediaOwnerTest`, pacote/UID separado, e a assertion consulta o owner real do MediaStore. Ainda falta executar em API29.
- Testes API35 consultam volumes externos realmente anunciados pelo MediaStore. O teste de acesso parcial roda primeiro e exige permissões visuais/áudio negadas antes de conceder Selected Photos Access; não revoga permissão no processo.
- Corrigida a assertion anterior: `SESSION_STALE` por si só não passa paginação. Imagem/áudio só terminam com sucesso após sessão estável; até três sessões são tentadas, cada invalidação precisa corresponder a `SOURCE_CHANGED`, e falha se todas permanecerem stale. Um resultado estável ainda precisa ser executado no aparelho.
- `:app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest` concluiu `BUILD SUCCESSFUL` após essa alteração (49 tarefas: 4 executadas, 45 up-to-date). O Kotlin daemon recebeu `AccessDeniedException` ao tentar escrever em `AppData\\Local\\kotlin\\daemon`; compilação usou fallback e terminou com sucesso.
- Repetição `:app:connectedDebugAndroidTest -x :mediaOwnerTest:installDebug` reproduziu `Failed to receive the UTP test results`, com zero testes coletados. A conexão `adb` do serial `0085353748` desapareceu após a execução. O aparelho precisa ser reconectado para repetir o AndroidJUnitRunner direto e separar runner de coleta.
- A tentativa local de emulador API29 chegou à falha explícita de aceleração x86_64: o driver de hypervisor não está instalado. Não instalar driver nem alterar proteções globais do Windows; API29 permanece bloqueado por ambiente.
- Próxima tarefa: quando o Motorola retornar ao ADB, confirmar modelo/API/serial, reinstalar/verificar os APKs recém-compilados, limpar/conceder somente permissões do pacote Debug e executar primeiro o caso Selected Photos isolado, depois a suíte direta completa. Obter XML/logcat/códigos de saída do UTP em nova execução; retomar API29 em CI ou máquina com virtualização já habilitada.

## Auditoria de recuperação — 2026-11-05

- Branch local própria: `codex/midiastore-release-audit`, criada do `origin/fix/p0-sweep-permissions-missions-billing` em `16545a4277bcc515660a6516c32fb6c770a4e880`. A árvore estava limpa antes da auditoria. `main` segue em `3759e176c48f340fa576a06455579277e514d9d9`.
- PR #3 permanece aberta como Draft, base `main`, não integrada. O trabalho local não foi enviado.
- O último CI inspecionado, [run 42080940375](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/42080940375), passou JVM, Lint task, builds APK Debug/AndroidTest e parsing PowerShell. Instrumentação API29 e API35 falhou.
- API29 criava a mídia pelo contexto que tinha o mesmo package/UID do Midiastore. Foi criado `:mediaOwnerTest` com package/UID separado, provider de teste protegido por permissão signature, conferência do owner retornado pelo MediaStore, consentimento Android, retry pós-`RESULT_OK` e verificação de ausência. O código compila, mas precisa rodar em API29.
- API35 consultava `external_primary` enquanto o sistema ainda informava o volume como desmontável e o processo instrumentado acabou encerrado. Os testes e o runner CI agora aguardam a montagem real do volume, com timeout e diagnóstico. A correção ainda precisa de CI/API35 real.
- Dependências atualizadas para versões estáveis: Core 1.17, AppCompat 1.8, Activity 1.13, Lifecycle 2.11, Coroutines 1.11, Material 1.14, AndroidX Test 1.7 e Billing 8.3. `billing-ktx` virou `billing`, pois o código usa API callback e não extensões KTX. AGP 8.13.2, Kotlin 2.3.21, Gradle 8.13, identidade e versão do app foram preservados.
- Validação local Windows concluiu com sucesso: `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:mediaOwnerTest:assembleDebug`. O relatório Lint contém 90 erros `MissingTranslation` e 94 warnings; o build segue porque `abortOnError=false`.
- Não há imagens/AVDs API29 e API35 instaladas localmente e `adb devices` está vazio. Nenhum teste instrumentado foi executado após a correção.
- Não há `keystore.properties` nem `sweep-upload.jks` local. O Release não foi executado e nenhuma chave foi gerada. A política de privacidade segue como draft sem URL pública; catálogo Billing, versão publicada máxima e certificado ainda dependem da Play Console.
- Não fazer push, merge, despacho de workflow externo, build AAB de produção ou publicação sem autorização. PR continua Draft e release bloqueado até CI atualizado e gates de aparelho/Play Console.
## Continuidade remota — 2026-11-06

### Alteração visual aplicada diretamente no GitHub

- A branch de recuperação avançou de `16545a4277bcc515660a6516c32fb6c770a4e880` para `ca9ebd04fa11bfba966830ce7f977ab081954ce1`, por fast-forward, sem force-push.
- Paleta atualizada para identidade premium escura: fundo midnight, superfícies azul-petróleo, ação primária jade/ciano e acento âmbar.
- Tema Material 3 alinhado à paleta com cores explícitas de superfície, texto, ação e navegação.
- Arquivos: `app/src/main/res/values/colors.xml` e `app/src/main/res/values/styles.xml`.
- PR #3 permanece aberta em Draft; `main` não foi alterada e nenhuma publicação foi feita.
- O código visual foi gravado e relido pelo GitHub. Ainda não há execução de CI associada ao novo SHA; portanto, não declarar compilação ou teste aprovados por causa dessa alteração.

### Workspace local do Codex — reconciliação obrigatória

- A última leitura local mostrou a branch `codex/midiastore-release-audit` no SHA base `16545a4277bcc515660a6516c32fb6c770a4e880`, com alterações staged, unstaged e arquivos novos.
- Essas alterações locais incluem correções de testes/fixture API29, descoberta dinâmica de volumes MediaStore API35, documentação e pesquisa. Elas ainda não estão incluídas no SHA remoto acima.
- Evidência local relatada: AndroidJUnitRunner direto no Motorola API35 executou os testes de mídia; os cenários exclusivos de API29 foram ignorados corretamente nesse aparelho. Isso não valida API29 e não substitui CI.
- O Gradle UTP no Windows falhou antes de coletar resultados, com erro do listener local/Netty; não classificar como falha de assertion do app nem como aprovação.
- Antes de continuar alterações que toquem os mesmos arquivos, reconciliar cuidadosamente o workspace local com a branch remota. Não descartar, limpar ou sobrescrever o trabalho local.

### Próximas prioridades remotas

1. Integrar de forma auditável as correções locais de fixture com UID separado, volume MediaStore dinâmico e isolamento do teste de permissões.
2. Obter CI verde no SHA atual, incluindo API29 e API35; não aceitar testes ignorados como substitutos de cobertura.
3. Validar visualmente em aparelho real a home, cards, filtros, navegação, fonte ampliada e TalkBack.
4. Resolver os findings de localização; a última auditoria local relatou 90 erros MissingTranslation e 94 avisos.
5. Manter API29, Billing seguro, Play Console, assinatura e testes OEM como gates explícitos de release.
