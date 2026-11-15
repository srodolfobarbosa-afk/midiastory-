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
