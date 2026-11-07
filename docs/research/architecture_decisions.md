# Decisões de arquitetura e engenharia

**Data:** 6 nov. 2026
**Base observada:** Kotlin, Android Views, módulos `:app` e `:domain`; namespace `com.rdf112.sweep`; IDs `com.rdf112.midiastore` e `.debug` preservados.

## ADR-01 — manter módulos e Android Views por ora

**Decisão:** preservar `:domain` sem dependência Android e `:app` como adaptador/apresentação. Não migrar toda a UI para Compose só para trocar tecnologia. A auditoria atual ainda precisa quebrar partes grandes de `MainActivity` em controllers/views menores, com estados explícitos, sem reescrita massiva.

**Motivo:** há fluxos de permissão, MediaStore, Billing e testes instrumentados existentes. Uma migração cruzada agora aumentaria risco sem benefício medido. Material 3 pode ser aplicado incrementalmente a componentes existentes; Material 3 Expressive deve ser avaliado por protótipos e acessibilidade, não adotado por moda.

## ADR-02 — APIs de armazenamento por menor privilégio

**Decisão:** MediaStore para mídia, Photo Picker para seleção pontual e SAF para árvore escolhida. Não declarar `MANAGE_EXTERNAL_STORAGE` como atalho para cleaners. Não prometer acesso ao cache privado de outros apps.

**Consequência:** toda consulta informa cobertura/volume/permissão; análise de árvore SAF é apenas da árvore escolhida e não representa o telefone inteiro.

Fontes: [MediaStore](https://developer.android.com/training/data-storage/shared/media), [Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker), [SAF](https://developer.android.com/training/data-storage/shared/documents-files), [política All files](https://support.google.com/googleplay/android-developer/answer/10467955), consultadas 6 nov. 2026.

## ADR-03 — revisão de sessão usa version/generation como sinal, não snapshot transacional

`MediaScanSession` guarda `MediaStore.getVersion()` e `getGeneration()` por volume. Mudança de acesso/volume invalida página; mudança da geração devolve `SESSION_STALE`. Isso é fail-closed: não combinar páginas de bibliotecas que mudaram silenciosamente.

**Evidência Motorola API35:** a sessão anterior executou o AndroidJUnitRunner direto com `OK (7 tests)`, mas isso antecede o ajuste recente que exige paginação completa em sessão estável. A estabilidade após fixture é apenas uma condição do teste; não prova que o provider sempre ficará estável. Os testes reiniciam até três sessões após `SOURCE_CHANGED` confirmado e falham se não obtiverem uma sessão estável; somente essa sessão pode provar leitura ou rejeição explícita de paginação. O novo APK AndroidTest compilou localmente, mas o UTP perdeu a conexão ADB antes de coletar resultados, e o runner direto ainda não foi repetido nessa versão. A lógica de produção não foi relaxada. Se a biblioteca mudar durante uso real, a UI deve oferecer novo scan e descartar seleção/páginas da sessão anterior.

API 36 muda semântica de `getVersion()` para valor opaco por app e exige ressincronização ao mudar; não compartilhar ou interpretar o valor. [Mudanças Android 16](https://developer.android.com/about/versions/16/behavior-changes-16), consultadas 6 nov. 2026.

## ADR-04 — exclusão e restauração

- Android 10: mídia criada por outro pacote demanda `RecoverableSecurityException`/consentimento aplicável; teste real precisa owner app com UID distinto.
- Android 11+: usar `MediaStore.createDeleteRequest()`/`createTrashRequest()` e confirmação do sistema.
- Revalidar URI/seleção e registrar apenas o retorno efetivo; verificar MediaStore depois da operação.
- `IS_TRASHED` não equivale a espaço recuperado. Restauração depende da permanência do item e confirmação do provider.

Fontes: [gerenciar arquivos de mídia](https://developer.android.com/training/data-storage/shared/media), [Android 10 privacy](https://developer.android.com/about/versions/10/privacy/changes), consultadas 6 nov. 2026.

## ADR-05 — testes instrumentados não mudam permissão do pacote durante o processo

Android encerrou o pacote alvo ao `UiAutomation.revokeRuntimePermission()` durante teste API35 (`ActivityManager: ... reason: permissions revoked`). O teste de acesso parcial não revoga mais permissões dentro do processo: verifica que imagem, vídeo e áudio estão negados antes de conceder `READ_MEDIA_VISUAL_USER_SELECTED`. Se a pré-condição estiver contaminada, a assumption marca o caso como ignored. Essa pré-condição foi configurada via ADB e o método isolado passou anteriormente; a suíte completa executou esse teste antes dos casos que concedem acesso total. O runner direto deve ser repetido após a mudança mais recente de paginação. API29 usa assumption explícita para não aparecer como aprovado em API35.

## ADR-06 — dependências

Consulta oficial 6 nov. 2026: AndroidX Core/KTX 1.19.1, Activity 1.13.0, AppCompat 1.8.0 e Lifecycle 2.11.0 estão estáveis; o projeto tem Core KTX 1.17.0 e os demais nessas versões estáveis. Billing 8.3.0 fica dentro do prazo publicado até 31 ago. 2027; Billing 9.1.0 existe, mas atualização só deve ocorrer com revisão de API/testes, não por número. Avaliar Core KTX 1.19.1 em mudança isolada com unit tests, lint e API29–35 antes de alteração. Não atualizar tudo em bloco.

Fontes: [Core](https://developer.android.com/jetpack/androidx/releases/core), [Activity](https://developer.android.com/jetpack/androidx/releases/activity), [AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle), [Billing releases](https://developer.android.com/google/play/billing/release-notes), [Billing deprecation](https://developer.android.com/google/play/billing/deprecation-faq). AGP 8.13.2/Kotlin 2.3.21/Gradle 8.13 compile localmente; manter until verified upgrade path.

## ADR-07 — Billing / entitlement

Billing 8.3.0 é atual o suficiente para o prazo declarado. Pending purchases não concedem Pro. Cliente reconcilia compras e acknowledge conforme documentação; produto comercial precisa confirmar modelo de verificação de tokens e provisionamento. Sem backend/Play Console de produção não declarar entitlement resistente a fraude como concluído.

Fontes: [Billing security](https://developer.android.com/google/play/billing/security), [integração](https://developer.android.com/google/play/billing/integrate), consultadas 6 nov. 2026.

## ADR-08 — imagem, vídeos e ML

- Thumbnail decodificada no tamanho de apresentação, com cache limitado e cancelamento.
- Duplicata exata: tamanho → hash candidate → SHA-256 integral; nunca remover só por hash amostrado.
- Semelhantes/baixa qualidade: não integrar modelo até existir conjunto de avaliação local consentido, métricas de falsos positivos/falsos negativos e orçamento CPU/RAM/energia em aparelho médio.
- Transcodificação: avaliar Media3 Transformer; manter original até usuário validar saída; estimar espaço temporário; cancelável.

## Dependências e versões registradas

Gradle Wrapper 8.13 (SHA-256 fixado), AGP 8.13.2, Kotlin 2.3.21, `compileSdk/targetSdk 36`, minSdk 29, Java 17, Billing 8.3.0. Build de teste compilou com Gradle em modo Kotlin in-process por erro de acesso a `C:\Users\rodolfo\AppData\Local\kotlin\daemon`; isso é problema de ambiente, não mudança de versão requerida.

## ADR-09 — consentimento interativo precisa de UI sem automação externa

Em teste físico API35, um serviço de acessibilidade de auto-click habilitado no dispositivo acionou a decisão do MediaStore antes do UiAutomator conseguir localizar os controles. O log registrou concessão automática; portanto aquela execução não prova cancelamento controlado. O harness mantém as assertions de cancelamento/aprovação, mas usa assumption explícita quando detecta serviço externo de auto-click. O cenário deve ser executado em CI/emulador ou dispositivo sem essa automação; não alterar configuração global do usuário para fazer o teste passar.

Evidência 06/11/2026: classe MediaStore reportou `OK (5 tests)`, mas quatro métodos executaram e o Selected Photos foi skipped por permissões totais preexistentes; após preparar o estado via ADB, esse caso passou isoladamente (`OK (1 test)`) e as permissões foram restauradas. Classe Main reportou `OK (4 tests)`, sendo 1 teste funcional e 3 assumptions por API/ambiente. API29 continua NOT RUN. UTP falhou antes de coletar resultados; o log debug anterior aponta erro PEM RSA no servidor local do listener, sem causa raiz confirmada. Checksums atuais e saídas em `.local-test-results/2026-11-06/`.
