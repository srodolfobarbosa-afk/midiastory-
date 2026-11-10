# AGENTS.md — Engineering rules for Midiastore

You are working on the Android app **Midiastore**. Read this file and `README.md`, then `docs/CONTINUATION_HANDOFF.md`, `docs/FEATURE_IMPLEMENTATION_MATRIX.md`, and `docs/RELEASE_BLOCKERS.md` before changing code.

## Project identity — do not guess

- Play listing / commercial app name: **Midiastore**.
- Confirmed applicationId supplied by the owner from the Play internal-testing listing: `com.rdf112.midiastore`.
- Internal Android namespace and Kotlin package are still `com.rdf112.sweep`; this is legacy implementation detail.
- Current build configuration: versionCode 17, versionName 2.2.1, minSdk 29, targetSdk 36, compileSdk 36, JDK 17, Gradle 8.13.
- VersionCode 17 is a source setting, not proof that 17 is greater than the maximum already published.
- Never change applicationId, signing certificate, versionCode or versionName without explicit owner approval.
- Never create a replacement upload keystore. Use only the legitimate Play App Signing upload key supplied through a secure owner-controlled process.
- The owner reported that the financial/receiving profile was enabled on 2026-11-03. This does not prove that any in-app product is configured, active, or tested.

## Equipe IA Híbrida: Auto-Evolução, Antigravity + Codex CLI

> [!IMPORTANT]
> **Princípio Fundamental de Auto-Evolução Contínua:** Nada nesta arquitetura ou fluxo de trabalho é estático ou fixo.
> Como agentes de IA, temos autonomia total e dever de iniciativa para atualizar, otimizar, refatorar e melhorar este fluxo.
> Em cada etapa, nos perguntamos: **"Como podemos melhorar?"** — seja adotando novos modelos de linguagem (LLMs), integrando bibliotecas mais modernas, adicionando novos servidores MCP ou otimizando custos e consumo de tokens.

### 1. Matriz de Agentes, Modelos e Failover de Inteligência

| Agente | Modelo / Runtime | Responsabilidade Principal | Gestão de Cotas & Failover |
| :--- | :--- | :--- | :--- |
| **Antigravity** | **Gemini 3.8 Flash (High)** / *Argon quando disponível* | **Orquestrador Central & Engenharia Forense**: Loop de decisão 24/7, Supabase MCP, GitHub Actions 24h, Google Play Console API, automação de navegador (Playwright/Chrome DevTools) e engenharia reversa via REA MCP (`morluto/rea`). | Quando a cota semanal do Argon atinge o teto, o **Flash (High)** assume com máxima velocidade e raciocínio profundo. Reseta semanalmente. |
| **Codex CLI** | **`codex.exe` local** (OpenAI / GPT-4o / o-series) | **Engenheiro de Código & Execução**: Refatoração profunda de Kotlin/Android, UI/Views, compilações pesadas no Windows (`./gradlew`) e testes instrumentados no emulador Android via ADB. | Opera pela assinatura/chaves locais OpenAI do usuário. Invocado sob demanda para não gastar tokens em tarefas triviais. |
| **IA Nativa Cloud** | **Supabase Edge Function** (`native-orchestrator`) + Gemini Flash / OpenRouter | **Guardiã do Loop 24/7 (Custo Zero)**: Monitoramento autônomo na nuvem mesmo com o PC desligado, acionando GitHub Actions e salvando memórias no Postgres. | Usa rota gratuita (OpenRouter / Gemini 2.0 Flash / Free Tier) com 500k invocações mensais no Supabase. Custo: R$ 0,00. |
| **Cloud Hands** | **GitHub Actions Runners** (Linux Ubuntu) | **Braço Executor na Nuvem**: Execução de scripts autônomos de crescimento orgânico, checagem de reviews da Play Store, builds e deploy do site. | Minutos gratuitos ilimitados por ser repositório público no GitHub. |

### 2. Estratégia de Expansão de Tokens e Poder Computacional
Para garantir que a equipe nunca fique sem poder de IA:
1. **Failover Multinível**: Se o modelo principal esgotar sua cota temporária, o orquestrador migra instantaneamente para o próximo nível sem parar o projeto:
   `Argon (High) ➔ Gemini 3.8 Flash (High) ➔ Codex CLI (o-series/GPT-4o) ➔ DeepSeek-R1 / Qwen 2.5 Coder (OpenRouter)`.
2. **OpenRouter Multi-Model Pool**: Já configurado com fallback para modelos gratuitos e de alta capacidade (como `deepseek/deepseek-r1:free` e `qwen/qwen-2.5-coder-32b-instruct:free`).
3. **Iniciativa Ativa de Pesquisa**: A IA pesquisa e propõe ativamente novos MCPs e ferramentas abertas no GitHub (como Playwright MCP, Chrome DevTools MCP e Agentic Skills) para expandir as capacidades do ambiente.

### 3. Protocolo Anti-Sobrescrita e Sincronização em Tempo Real
1. **Fila Centralizada no Supabase (`public.orchestrator_tasks`)**: Todas as tarefas são registradas e roteadas no Supabase (`target = 'CODEX'`, `'ANTIGRAVITY'` ou `'CLOUD_ACTIONS'`).
2. **Lock Atômico de Tarefas**: Antes de iniciar o trabalho em uma tarefa, o agente faz uma transição atômica de `status = 'PENDING'` para `status = 'IN_PROGRESS'`. Se a tarefa já tiver sido assumida, ela não é executada novamente.
3. **Memória Compartilhada (`public.memories`)**: Ambos os agentes registram resumos de suas entregas via `node scripts/sync-agent-memory.mjs write "..."`, garantindo continuidade cross-session.
4. **Git e Árvore de Arquivos**: Antes de qualquer commit ou edição estrutural, execute `git status` e verifique tarefas em andamento.

## Git workflow

- Current recovery branch: `fix/p0-sweep-permissions-missions-billing`.
- Review: https://github.com/srodolfobarbosa-afk/midiastory-/pull/3
- P0 issue: https://github.com/srodolfobarbosa-afk/midiastory-/issues/2
- Work in a feature branch and open/update a pull request.
- Do not merge to `main`, close the recovery issue, publish a release, or upload to Play Console unless the owner explicitly asks.
- Do not overwrite or reset owner commits. Fetch and inspect branch status before editing.

## Required engineering principles

1. A successful compile is not evidence that a feature works end-to-end.
2. Never report complete library coverage when permissions are partial, a provider/page failed, a generation is unverifiable, pagination is unsupported, or a safety limit was reached.
3. Exact duplicates require non-empty SHA-256 hashes that match. File size alone is never proof of identical content.
4. If global duplicate coverage is partial or any candidate hash cannot be verified, do not auto-select duplicate files for deletion.
5. Large/old-file missions are proposals only. The user reviews all candidates.
6. Never delete files automatically. Revalidate each MediaStore URI immediately before destructive operations and use Android's official consent flow where required.
7. Moving files to Android Trash is not freed storage. Credit bytes only after permanent deletion and post-operation absence are verified.
8. API 29 cleanup is currently a release blocker. Do not claim it works until a tested RecoverableSecurityException consent flow exists, or the owner approves an explicit minSdk change.
9. Do not claim Pro entitlement is production-verified while purchase tokens are not validated by a trusted backend/Google Play Developer API.
10. Do not invent product IDs, prices, offers, payment methods, device test results, Play Console status, or test outcomes.
11. Do not add marketing claims for visual similarity, blur detection, compression, generic document management, cache cleaning of other apps, RAM boost, cooling, or performance acceleration unless the feature is implemented and tested.
12. Keep media hashing and long-running global scan work off the UI thread. Use cancellable streaming I/O, bounded collections, explicit progress and safety limits.
13. Keep Portuguese and English resource keys synchronized. Do not leave clipped labels or long technical jargon in primary UI.

## Current technical shape

- Android Views app; preserve the current UI architecture unless a migration is explicitly planned.
- MediaStore repository for Images, Video and Audio by available volume.
- Bounded 100-item page UI, with paging/session revision checks.
- Recovery branch includes a cancellable scan across accessible kinds, volumes and pages.
- Global scanner safety bounds: 250,000 rows, 20,000 repeated-size duplicate candidate rows, 100,000 unique size buckets, 2 GiB maximum per duplicate file hash and 10 GiB total global hash I/O. Global missions disable page-level duplicate hashing and hash each candidate once. Normal Files-page duplicate hashing is capped at 512 MiB per file and 1 GiB per page.
- Global scanner uses streaming SHA-256 for exact duplicate groups and fail-closed duplicate selection under partial coverage.
- Android 10/API29 permanent deletion now uses RecoverableSecurityException and the official per-item system consent flow; this new path is not yet validated on an API29 emulator or physical device. Trash/restore remain API30+.
- Billing dependency is Google Play Billing KTX 8.0.0. Source product IDs are `sweep_pro_monthly` (SUBS), `sweep_pro_annual` (SUBS), and `sweep_pro_lifetime` (INAPP); these are not verified as active products in Play Console.
- The client currently checks purchase state and signature/JSON field presence but has no cryptographic purchase-token verification service.

## Build and test commands

Use JDK 17 and Android SDK 36 / Build Tools 35.0.0.

```bash
./gradlew --no-daemon \
  :domain:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

With a connected emulator or physical Android device:

```bash
./gradlew --no-daemon :app:connectedDebugAndroidTest
```

The GitHub Actions workflow also runs instrumented tests on an Android 35 emulator and uploads JUnit/XML reports. Review the exact commit SHA associated with each run; cancelled or stale runs are not evidence for the latest branch head.

In Codespaces, the `main` branch previously contained a machine-specific `org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64`. The recovery branch removes that path so the environment's `JAVA_HOME` is used. Switch to the recovery branch before running Gradle:

```bash
git fetch origin
git switch fix/p0-sweep-permissions-missions-billing
git pull --ff-only
```

On Windows, `adb: no devices/emulators found` means no device is attached. It is not an app failure or test result. Connect a device with USB debugging and authorize the computer, or start an Android Studio emulator, then confirm with `adb devices`.

## Required proof before a release

- Green CI on the exact latest PR commit: domain tests, app JVM tests, lint, Debug build and connected instrumented tests.
- MediaStore permissions, paging, partial access, cancellation, session freshness and global scan verified on Android API 29, 30, 33, 34, 35 and 36 and representative OEMs.
- Cleanup/restore verified for accept, cancel, partial approval and post-operation state; API29 explicitly resolved.
- Billing products/types/offers and availability confirmed in Play Console; license-tester purchase, pending, cancelled, restore, acknowledgement, refund and expiry flows tested.
- Purchase tokens validated in a trusted backend using Google Play Developer API before granting Pro entitlements.
- Correct package, versionCode and upload certificate confirmed against the production Play listing.
- Privacy policy, Data Safety, SDK declarations, manifest and final binary reviewed together.
- A correctly signed AAB inspected and installed in internal/closed testing before production.

**Stop and report accurately when a blocker requires owner-controlled Play Console access, credentials, payment configuration, or physical-device testing. Do not bypass those gates.**
