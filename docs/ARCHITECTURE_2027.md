# Arquitetura 2027

## Estado encontrado

O projeto contém dois módulos Gradle (`:app` Android e `:domain` Kotlin/JVM), UI Android Views com quatro seções em `MainActivity`, `MediaScanViewModel` com `StateFlow`, regras puras para paginação/seleção/agrupamento/OaaS/Billing e adaptadores Android para MediaStore, operações de mídia, telemetria local e Google Play Billing. A Activity ainda é proprietária da navegação e de grande parte da composição/renderização; não é uma arquitetura de features isoladas.

A baseline Debug executou com Gradle 8.13, AGP 8.13.2, Kotlin 2.3.21, JDK 17 e Android SDK 36. Não há evidência de build Release assinado ou teste em dispositivo. O source ZIP contém um único commit rastreado; os módulos extraídos são arquivos não rastreados.

## Direção

```mermaid
flowchart LR
  UI[Views / futuras features] --> VM[ViewModels e casos de uso]
  VM --> DOMAIN[Domain: contratos e políticas puras]
  DATA[Data Android] --> DOMAIN
  DATA --> PLATFORM[MediaStore, SAF, Billing, StatFs, WorkManager]
  PLATFORM --> OS[Android / Google Play]
```

A regra de dependência é `UI -> domain contracts`; adaptadores implementam contratos e dependem de APIs Android. O domínio não importa Android, Billing, Views ou SDKs de IA.

## Limites propostos

- **Storage:** snapshot de `StatFs`, amostras locais e previsão com incerteza explícita.
- **Media catalog:** MediaStore por volume/tipo, paginação estável, permissões e cobertura.
- **Media intelligence:** filtros e hash exato; mais tarde perceptual hash em fluxo de revisão separado.
- **Safe cleanup:** planos imutáveis, revalidação, consentimento oficial, reconciliação pós-operação e accounting.
- **Files:** MediaStore para mídia; SAF para documentos/Downloads selecionados.
- **Billing:** catálogo/flow client e estado de entitlement; validação confiável não pode depender de SharedPreferences.
- **Automation:** WorkManager só para análises/relatórios opt-in, nunca remoção automática.

Não criar módulos Gradle vazios agora. Primeiro estabilizar contratos, testes e release. Considerar `:feature:storage`, `:feature:media`, `:feature:files` ou `:feature:billing` apenas quando houver código independente, dono/testes próprios e dependências unidirecionais que justifiquem o custo.

## Revisão de contratos

- `MediaPageRepository`: mantém página limitada, sessão e erros; documentar ordenação do provider e fallback local.
- `MediaOperationContract`: separar plano preparado, consentimento e resultado observado; não oferecer API que permita executar sem confirmação do Android.
- `StorageSampleRepository`: guardar amostras por volume/capacidade; cota, retenção e exportação controladas.
- `BillingGateway`: expor catálogo carregando/disponível/indisponível/erro e não usar detalhes em cache após produto não encontrado.
- Toda operação longa recebe cancelamento; streams e cursores são fechados em `use`; UI coleta estado com lifecycle.

## Decisões de plataforma

- Preservar `applicationId`/namespace `com.rdf112.sweep`; nome visível pode ser Midiastore. Confirmar histórico/certificado na Play Console antes de release.
- `minSdk 29`, `targetSdk 36`, `compileSdk 36`, Java 17; consultar Play target policy a cada release e testar API 37 antes da migração.
- Nada de root, ADB, Shizuku, `MANAGE_EXTERNAL_STORAGE` ou acesso privado de terceiros como requisito do MVP.
- Não adicionar Room, WorkManager, LiteRT ou MediaPipe sem um caso de uso aprovado e benchmark.

## Gatilhos de modularização

Extrair feature do `:app` quando pelo menos dois sinais ocorrerem: equipe independente, ciclo de release separado, API de domínio estável, redução demonstrável do grafo de dependências/tempo de build ou testes de feature isoláveis. Manter no domínio puro aquilo que pode ser testado sem Android.
