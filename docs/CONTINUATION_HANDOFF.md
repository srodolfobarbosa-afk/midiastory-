# Midiastore — CONTINUIDADE DO PROJETO

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
