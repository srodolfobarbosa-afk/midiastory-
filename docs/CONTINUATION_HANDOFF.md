# Midiastore — CONTINUIDADE DO PROJETO

Atualizado: 04/11/2026

## Identidade correta

- Nome comercial e listing: **Midiastore**.
- Package/Application ID: `com.rdf112.midiastore`.
- O proprietário confirmou o package olhando a ficha de teste interno da Play Console.
- Namespace e pastas Kotlin ainda usam `com.rdf112.sweep`; isso é legado técnico e não altera o Application ID.
- `versionCode=17` e `versionName=2.2.1` são os valores atualmente configurados no código-fonte. Ainda é obrigatório conferir o maior versionCode publicado na Play Console.
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
