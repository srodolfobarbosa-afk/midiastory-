# Mapa Técnico Consolidado — Ecossistema Midiastore (`com.rdf112.midiastore`)

> **Última consolidação verificada:** 2026-11-15 | **Branch:** `codex/midiastore-release-audit`  
> **Meta Fundamental:** R$ 1.000.000,00 em vendas reais na Google Play Store (Regra Financeira 50/50 com alocação dinâmica).

---

## 1. Identidade, Versionamento e Stack Oficial

| Atributo | Valor Verificado no Código (`app/build.gradle.kts`) | Observação Operacional |
|---|---|---|
| **Nome Comercial** | Midiastore (Limpador & Estúdio) | Listing oficial Google Play |
| **Application ID** | `com.rdf112.midiastore` | Debug usa sufixo `.debug` |
| **Namespace Kotlin** | `com.rdf112.sweep` | Legado técnico preservado; não confundir com `applicationId` |
| **Versão no Source** | `versionCode = 22`, `versionName = "2.5.0"` | Play Console `internal`/`alpha` está em `vc 21` / `v2.4.0` (AAB R8 4,21 MB) |
| **SDK / Toolchain** | `minSdk = 29`, `targetSdk = 36`, `compileSdk = 36` | JDK 17, Gradle 8.13, AGP 8.13.2, Kotlin 2.3.21 |
| **UI & Bibliotecas** | Android Views + Material `1.14.0`, Coroutines `1.11.0`, ZXing `3.5.3` | `billing:8.3.0`, `review-ktx:2.0.2`, `core-ktx:1.17.0` |
| **Idiomas no App** | 14 diretórios (`values` + 13 locales, incl. `ar` RTL) | `pt-rBR`, `en`, `es`, `de`, `fr`, `hi`, `in`, `ja`, `ko`, `ru`, `zh-rCN`, `bn`, `ar` |
| **Funil Web** | Firebase Hosting (`https://midiastory-df6a0.web.app/`) | 13 idiomas + busca live + calculadora de lixo + telemetria |
| **Backend & Banco** | Supabase (`usmqtnlsaavxqguwrtix`) + Postgres/pgvector | Cota gratuita 500 MB (~13 MB em uso), RLS 9/9 ativo |

---

## 2. Mapa de Arquivos Críticos

### Android (`app/src/main/`)
- **`AndroidManifest.xml`**: Permissões granulares (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` `maxSdkVersion="32"`, `PACKAGE_USAGE_STATS`). **PROIBIDO** `MANAGE_EXTERNAL_STORAGE`.
- **`MainActivity.kt`**: Interface principal em abas (`Início`, `Limpeza`, `Arquivos`, `Estúdio`, `Mundo`, `Pro VIP`), botão hero de limpeza 1-toque (`homeDirectCleanRow`), paywall e integração de módulos.
- **`data/media/AndroidMediaPageRepository.kt`**: Paginação MediaStore (100 itens/página), suporte a múltiplos volumes, verificação de `SessionFreshness`, streaming SHA-256 e fallback sem `EXTRA_TOTAL_COUNT`/`EXTRA_HONORED_ARGS` para OEMs (Samsung, Motorola, Xiaomi, Pixel).
- **`data/media/AndroidTrashOperationCoordinator.kt`**: Revalidação item a item antes de exclusão, Lixeira oficial API 30+ (`createTrashRequest`), exclusão permanente API 30+ (`createDeleteRequest`) e API 29 (`RecoverableSecurityException` URI a URI).
- **`presentation/scan/MediaScanViewModel.kt`**: Scanner global cancelável com limites de segurança (`250.000` linhas, `20.000` candidatos duplicados, `100.000` buckets de tamanho, `2 GiB`/arquivo e `10 GiB` de I/O global; leitura normal `512 MiB`/arquivo e `1 GiB`/página).
- **`data/billing/AndroidPlayBillingGateway.kt`**: Billing 8.3.0 **fail-closed**; exige `PURCHASED = 1`, allowlist de SKUs, `purchaseToken` não vazio e validação server-side na Edge Function `verify-play-purchase` antes de liberar Pro e executar `acknowledgePurchase`.
- **`util/InAppReviewCoordinator.kt`**: Aciona Google Play In-App Review (`review-ktx:2.0.2`) após confirmação real de bytes liberados.
- **`presentation/studio/`**: `MediaStudioTools.kt` (filtros, extração M4A, scrubber EXIF/GPS, QR ZXing, PDF A4, downloader com cancelamento cooperativo), `GlobalHubTools.kt`, `MagicRiddlesTools.kt`, `ProWordSearchTools.kt`, `SnakeGameView.kt`, `FallingBlocksGameView.kt`.

### Backend, IA & Automação (`supabase/` e `scripts/`)
- **`supabase/functions/verify-play-purchase/index.ts`**: Edge Function (`v6`) que autentica via Service Account JWT na Google Play Developer API v3 (`subscriptionsv2` / `products`), valida o SKU e registra auditoria em `public.play_purchases` com SHA-256 do token.
- **`scripts/lib/rodolfo-brain-persona.mjs`**: Define `RODOLFO_BRAIN_SYSTEM_PROMPT` (15 diretrizes) e `RODOLFO_BRAIN_MODEL_PRIORITY` (`gemini-4-carbon`, `gemini-4-hydrogen`, `gemini-4-argon`, `gemini-3-flash-preview`, `gemini-2.5-flash`).
- **`scripts/ai-failover-router.mjs`**: Cascata de modelos (Google AI Studio -> OpenRouter com `max_tokens` dinâmico anti-402 e modelos `:free` -> `codex.exe` local).
- **`scripts/sync-agent-memory.mjs`**: Leitura e gravação em `public.memories` no Supabase via `scripts/lib/supabase-auth.mjs`.
- **`scripts/supabase-maintenance.mjs`**: Monitora e compacta dados para manter o Postgres bem abaixo de 400 MB (teto gratuito: 500 MB).
- **`scripts/firebase-deploy-site.mjs` & `scripts/verify-funnel.mjs`**: Publicação e verificação HTTP 200 de todas as rotas do funil web.

---

## 3. Regras de Negócio, Produto e Diretrizes do Fundador

1. **Qualidade antes de vender**: Compilar (`BUILD SUCCESSFUL`) não prova funcionamento. Nunca escalar tráfego pago antes de fechar os gates P0.
2. **Verdade Absoluta**: Zero simulações, zero métricas inventadas. Lixeira **não** conta como espaço liberado; bytes só são creditados após ausência confirmada pós-exclusão permanente.
3. **Regra Financeira 50/50 (Alocação Dinâmica)**: 50% lucro/retirada e 50% reinvestimento alocado dinamicamente onde houver maior retorno imediato (tráfego/ASO, APIs/nuvem ou hardware).
4. **Freemium com Valor Massivo Gratuito**: Limpeza MediaStore, revisão manual, proteção do original (fail-closed se cobertura global for parcial), Estúdio de Mídia local e Hub Mundial Offline permanecem gratuitos. Proibido anunciar recursos inexistentes (similaridade visual, blur, compressão, limpeza de cache de terceiros, RAM boost, resfriamento).
5. **Proibido Web Checkout**: Monetização 100% nativa via Google Play Billing.
6. **Arquitetura Agnóstica Frontier**: Personalidade e regras do fundador plugáveis nos modelos mais inteligentes disponíveis (priorizando `gemini-4-carbon` quando disponível, com fallback real para `gemini-4-hydrogen` / `gemini-3.8-flash` / OpenRouter / Codex CLI).
7. **Lei Cognitiva dos Verbos**: Toda IA extrai o verbo executável (`forma_base` no infinitivo) e age de ponta a ponta sem passividade.

---

## 4. Preços Reais e Matemática de R$ 1.000.000,00

| SKU (`AndroidPlayBillingGateway` & Play API v3) | Tipo | Preço Brasil (ATIVO na API v3) | Equivalente para R$ 1.000.000,00 |
|---|---|---|---|
| `sweep_pro_monthly` | `SUBS` | **R$ 14,99 / mês** | ~66.711 assinaturas mensais |
| `sweep_pro_annual` | `SUBS` | **R$ 79,99 / ano** | ~12.500 assinaturas anuais |
| `sweep_pro_lifetime` | `INAPP` | **R$ 149,99 único** | ~6.667 planos vitalícios |

*(Preços USD/Tier-1 também atualizados via Play API v3 em 174 regiões; rota correta de atualização para vitalício é `/onetimeproducts/{id}` em minúsculo).*

---

## 5. Bloqueios P0 Atuais (Antes de Produção Pública)

1. **Conformidade App Android**: Concluir Prominent Disclosure explícito para `PACKAGE_USAGE_STATS`, aviso claro de renovação/cancelamento de assinatura no paywall e revalidar após as edições pendentes.
2. **Testes Instrumentados & CI**: Executar e comprovar `RecoverableSecurityException` em **API 29** (emulador com KVM/Linux ou aparelho físico) e suíte em **API 35/36** sem interferência de auto-clicker de terceiros, com CI verde no commit candidato.
3. **Play Console & Testadores**: Atingir **12+ testadores humanos em opt-in contínuo por 14 dias** (regra oficial vigente; meta de segurança 15–20; atualmente 4 ativos confirmados -> faltam 8 para o mínimo oficial / 16 para folga de 20), alinhar declarações de `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`/`PACKAGE_USAGE_STATS` + Data Safety com o AAB final e subir `phoneScreenshots` para os 6 idiomas ainda zerados (`es-ES`, `fr-FR`, `de-DE`, `hi-IN`, `ja-JP`, `zh-CN`).
4. **Billing End-to-End**: Validar compra real com *License Testers* (compra, pendente, cancelamento, restauração, acknowledge, reembolso e expiração) passando pela Edge Function `verify-play-purchase`.

---

## 6. Lições Aprendidas (Zero Repetição de Erros)

- **Windows & Gradle**: Host local com 3 GB de RAM exige `--no-daemon` (single-JVM); se o cache `transforms` corromper, purgar a pasta corrompida. No PowerShell, usar `;` em vez de `&&`.
- **UTP vs `AndroidJUnitRunner`**: O coletor UTP do Gradle no Windows pode falhar por erro de PEM RSA no listener gRPC local (0 testes/sem XML); usar `AndroidJUnitRunner` direto via ADB para diagnóstico local e confiar no CI Linux para UTP/API 29.
- **Auto-clicker no Moto G84**: `com.truedevelopersstudio.automatictap.autoclicker` interfere em diálogos de consentimento do sistema; os testes instrumentados usam `assumeNoExternalAutoClicker` para não gerar falso positivo.
- **OpenRouter HTTP 402**: Nunca aceitar 402 passivamente — extrair o limite acessível do corpo do erro e ajustar `max_tokens` (ex.: `1500`) ou acionar Google AI Studio (`gemini-4-hydrogen` Free Tier com `thinkingLevel` MEDIUM/HIGH).
- **Play Developer API v3**: Título em espanhol não pode passar de 30 caracteres (`Midiastore: Limpiador Estudio`); rota de produto único é `/onetimeproducts/{id}` (minúsculo).
- **Firebase Hosting**: O workflow antigo `fabrica-autonoma-ubuntu.yml` sobrescrevia o site e causava HTTP 404 nas rotas de idioma — foi removido; sempre validar com `node scripts/verify-funnel.mjs` após deploy.
- **Segurança**: Nunca expor `sbp_`, tokens ou `.secrets/` em scripts ou commits; usar `scripts/lib/supabase-auth.mjs` e verificar `git status` antes de qualquer commit.


---

## 7. Post-Mortem Operacional: Fim do Ciclo Estéril (Feedback do Fundador — 2026-11-15)

- **Contexto observado:** Histórico de sessão em `Antigravity CLI 1.5.6` (`srodolfobarbosa@gmail.com`, `Google AI Pro`, `Gemini 3.8 Flash (High)`, `~/midiastory-`). *(Nota de Verdade Absoluta: o trecho colado encerrou no cabeçalho da sessão, sem os comandos subsequentes).*
- **Diagnóstico da falha recorrente apontada pelo fundador:**
  1. **Atividade sem resultado financeiro:** Múltiplas sessões rodando em círculos (gerando novos documentos, scripts paralelos e micro-refatorações) sem fechar os 3 gargalos que realmente colocam dinheiro na conta: **P0 do app -> 12+ testadores por 14 dias -> Produção & Billing validado**.
  2. **Limitação de modelo / Cota:** Sessões rodando em modelos mais fracos (`Gemini 3.8 Flash`) ou sem `Gemini 4 Carbon` ativo (que exige GCP billing/créditos AI Pro/Ultra além do Free Tier).
- **Regras Anti-Repetição Obrigatórias:**
  - **Filtro Caixa-Primeiro (Peso 1.00):** Antes de criar qualquer novo doc ou script, validar se a ação destrava diretamente: *(a)* fechamento de blocker P0, *(b)* teste real/CI, *(c)* aquisição/retenção dos 12+ testadores por 14 dias, ou *(d)* conversão paga no Google Play Billing. Se não destrava nenhum dos 4, **não priorizar**.
  - **Transparência de Modelo & Cota:** Nunca fingir que `Gemini 4 Carbon` está rodando se a API retornar `limit: 0` / `429`; usar o melhor modelo ativo (`Gemini 4 Hydrogen` / roteador) e orientar o caminho exato para habilitar `Carbon` quando necessário.
  - **Zero Invenção em Logs:** Se um log colado vier incompleto/truncado, analisar apenas o que está presente e pedir explicitamente o restante se precisar de stack trace.
  - **Chave presente != Cota ativa:** Nunca afirmar que `Gemini 4 Carbon` / `Argon` / OpenRouter está "pronto para uso" apenas porque `.secrets/ai-media-keys.json` existe; validar saldo/cota real (`limit > 0` / sem HTTP 402) antes de prometer acesso.
  - **Não confundir modelos homônimos:** `models/gemini-4-hydrogen` é o modelo oficial do Google AI Studio (acessível grátis via `GOOGLE_AI_STUDIO_API_KEY`), diferente de `deep-principle/Hydrogen-1.0` (open-weight no Hugging Face baseado em GLM-5.3, que exige hospedagem GPU própria/provedor externo).

---

## 8. Extrações Reais da Sessão `rodolfo madrgada!.txt` (Erros, Descartes e Decisões)

- **Descartes Definitivos no App (Não Repetir):**
  - **QR Code XOR/Hash customizado ->** substituído por `com.google.zxing:core:3.5.3` (ISO/IEC 18004, contraste >= 4:1) após falha de leitura em câmeras/apps bancários.
  - **Downloader salvando HTML como `.mp4` ->** substituído por validação `Content-Type: video/*` (rejeição explícita de HTML), streaming em blocos de 8 KB e cancelamento cooperativo.
  - **Botão de Lixeira na API 29 ->** Android 10 não possui Lixeira de sistema; ocultado na API 29, usando exclusão permanente com consentimento (`RecoverableSecurityException`).
  - **Alertas pequenos ->** substituídos por `BottomSheetDialog` com alvos de **48dp**, `WindowInsets`, botão `📋 COLAR` (`ClipboardManager` + regex `https?://[^\s]+`) e `HapticFeedbackConstants` (`KEYBOARD_TAP` / `CONFIRM`).
- **Build, R8 e ADB:**
  - `isMinifyEnabled = false` impedia redução do APK e geração de `mapping.txt`; ativar R8 estrito (`app/proguard-rules.pro` protegendo ZXing, Billing e ViewModels) reduziu o APK de release para **4,2 MB** (AAB `vc 21` = 5.861.444 bytes).
  - `adb install -r` falha com `signatures do not match` se houver build com outra assinatura no aparelho (ex.: debug vs `sweep-upload.jks`).
- **OpenRouter 402 & APIs Google:**
  - Omitir `max_tokens` no OpenRouter faz o servidor pré-reservar **65.536 tokens** e disparar HTTP 402; travar `max_tokens: 30` em healthcheck retornou `200 OK` a `$0.00000795` (e `1500`/`3000` em produção).
  - `check-google-apis.mjs` validou `200 OK` em Search Console API e Play Developer Reporting API (Anomalias + Crash Rate) com `midiastore-agent@cultivated-pen-469823-p3.iam.gserviceaccount.com`.
  - Recusa estrita de usar engenharia reversa/REA para burlar cotas ou controles de acesso de serviços Google (uso do REA `morluto/rea` restrito a auditoria forense legítima).
- **Tração Real na Play Console & Decisão Freemium:**
  - Instalações locais via `adb install` **não contam** nos gráficos da Play Console (platô `0` de 15/10 a 08/11/2026); apenas downloads pelo link oficial de opt-in (`https://play.google.com/apps/testing/com.rdf112.midiastore`) iniciam o relógio de 14 dias.
  - As primeiras **4 instalações reais** (1 em 09/11 + 3 em 10/11) vieram **100% do Brasil** (foco inicial no mercado brasileiro + ferramentas essenciais gratuitas para retenção dos 14 dias).
- **Infraestrutura Cognitiva Criada:**
  - `CONTEXTO_MESTRE.md` e `CONTEXTO_ESTRATEGICO.md` (linha de montagem Fábrica GitHub -> Cérebro/Cofre Supabase -> Vitrine/Caixa Google Play, com matriz de pesos `1.00 / 0.95 / 0.90 / 0.85`).
  - Tabela `public.aprendizado_ia` no Supabase + `scripts/registrar-aprendizado-ia.mjs` e Lei Cognitiva dos Verbos (`forma_base` no infinitivo + tolerância fonética a erros de digitação).

---

## 9. Mineração Total do Histórico (`C:\Users\rodolfo\Documents` — 13 Arquivos `.txt`, ~2,07 MB)

Extrações consolidadas dos logs completos de Codespaces, Play Console, Moto G84, terminal, Cloud 24/7 e roteamento multi-LLM:

- **Gradle, AGP, Kotlin DSL e Codespaces (`usar esse como exemplo...`):**
  - `java.io.IOException: java.io.tmpdir (/tmp/gtmp)` inexistente no container -> criar `/tmp/gtmp` antes do Gradle (`mkdir -p /tmp/gtmp`).
  - `Incorrect package="..." found in source AndroidManifest.xml` -> no AGP 8+, remover atributo `package` do `AndroidManifest.xml` e declarar `namespace` apenas em `app/build.gradle.kts`.
  - `Unresolved reference 'isCheckReleaseBuilds' / 'isAbortOnError'` no bloco `lint {}` em Kotlin DSL -> usar `checkReleaseBuilds = false` / `abortOnError = false` (sem prefixo `is`).
  - `Inconsistent JVM-target compatibility` (`compileReleaseJavaWithJavac` 1.8 vs `compileReleaseKotlin` 21) e crash do Kotlin Daemon no JDK 25 (`NotSerializableException: SmartList`) -> travar `JavaVersion.VERSION_17` + `jvmTarget = "17"` (e usar **JDK 17 + Gradle 8.13**).
  - `gh models run --system` falha com `unknown flag: --system` -> a flag correta no GitHub Models CLI é `--system-prompt`.
- **UI Android, Moto G84 & Play Console (`66666.txt` & `termi rodolfo dia 10`):**
  - Altura fixa `dp(52)` cortou botões de 2 linhas em ~2px no Moto G84 e `cardElevation = 2dp` aplicou surface tint esverdeado do Material 3 (`v19 -> v20`) -> usar `WRAP_CONTENT` com `minHeight` e controlar `cardBackgroundColor`/`elevation` explicitamente.
  - Aviso de política **Advertising ID** na Play Console bloqueou release até ser declarado/corrigido no painel; após ajuste, a faixa `20 (2.3.1)` atingiu `"status": "completed"`.
  - Editor de Fotos local corrigido para exportar em alta resolução até **4096px** com controle de saturação `0.0x–2.0x`.
- **Scripts Node.js, Supabase & Cota Zero (`termi rodolfo dia 10`, `pulo do gato`):**
  - Caminhos relativos em `scripts/*.mjs` quebram com `ENOENT` se executados fora da raiz (ex.: `system32` ou CI) -> resolver `projectRoot` via `path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..")` e ler `.secrets/ai-media-keys.json` (ou env vars `OPENROUTER_API_KEY` / `AI_MEDIA_KEYS_JSON` na nuvem).
  - Submissão duplicada de testador em `public.beta_testers` retorna HTTP `409 Conflict` -> tratar `409` graciosamente no frontend (`testerForm`) como já cadastrado.
  - Manutenção do Supabase (`scripts/supabase-maintenance.mjs`): teto preventivo de **400 MB** (plano free 500 MB) com purga de `orchestrator_logs > 7d` e `orchestrator_tasks > 14d`.
- **Failover Multi-LLM Real Testado (`rodolfosalavandomidiadia13`, `novo pulo do gato`, `ligarocarbomdeverdade`):**
  - Crédito promocional `$0.20` do OpenRouter consumido até `$0.199917639` -> modelos pagos (`google/gemini-4-carbon`, `google/gemini-4-argon`) devolvem HTTP `402` ("can only afford 1") até recarga, e `deepseek/deepseek-r1:free` foi descontinuado (`404`).
  - `Gemini 4 Carbon` direto via Google AI Studio API devolve `429` sem vínculo de faturamento GCP.
  - **Rotas de custo zero validadas (`200 OK`):** Google AI Studio direto (`gemini-4-hydrogen` / `gemini-2.5-flash` / `gemini-3-flash-preview`), OpenRouter `:free` ativos (`openrouter/free`, `nvidia/nemotron-3.5-lightning:free`, `google/gemma-4-31b-it:free` — 50 req/dia) e `codex.exe` local (logado via ChatGPT).
  - **Dívida técnica identificada:** `scripts/invisible-loop-bot.mjs` e `scripts/organic-growth-agent.mjs` ainda chamam `openai/gpt-4o-mini` direto no OpenRouter sem `max_tokens` nem failover.
