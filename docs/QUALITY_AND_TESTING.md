# Quality and Testing — Midiastore

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
