# 🧠 CHECKLIST MESTRA DE EVOLUÇÃO CARBON — MIDIASTORE & MARIA 2.0
> 🧭 **Índice Central:** [**`docs/INDICE_MESTRE.md`**](INDICE_MESTRE.md) · [**`AGENTS.md`**](../AGENTS.md) · [**`CHECKLIST_PRODUCAO_VIVA.md`**](CHECKLIST_PRODUCAO_VIVA.md) · [**`CONTINUATION_HANDOFF.md`**](CONTINUATION_HANDOFF.md) · [**`PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`**](PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md) · [**`LINHA_DO_TEMPO_AUTONOMA.md`**](LINHA_DO_TEMPO_AUTONOMA.md)
**Criada em:** `2026-11-24` | **Motor:** Gemini 4 Carbon | **Ordem Oficial Aprovada:** **Fase 0 (`26/11`) ➔ Frente B (`feat/v2.9.0-motor-app`: Fotos Parecidas IA + SQLite Instantâneo + Compressor Vídeo Pro) ➔ Frente A (Maria 2.0 Neural/3D)** | **Regra Suprema:** Protocolo Catraca (Zero Regressão)

---

## 🛡️ PROTOCOLO CATRACA CARBON (ZERO REGRESSÃO)
1. **Baseline Travada no Git (`tag v2.8.2-carbon-baseline` -> commit `cd0a661`):** o AAB assinado `v2.8.2 (vc 34)` (`7,26 MB`, SHA-256 `5151aa51ac753efb78427d90591d6faaa13d7bdc16038642cda2704f017634f1`) é o piso mínimo imutável de qualidade.
2. **Isolamento Total por Branch:** toda melhoria nova nasce em branch dedicada (`feat/similar-photos`, `feat/sqlite-hash-cache`, `feat/maria-2.0`), nunca misturada na build de abertura de Produção (`26/11`).
3. **Validação Lado a Lado no Moto G84 (`0085353748`):** testar sempre `.debug` lado a lado com o pacote oficial `com.rdf112.midiastore` antes de subir `versionCode`.
4. **4 Portões Obrigatórios de Merge:** (1) Testes JVM + CI API 29/35 verdes, (2) `bundletool` sem permissões proibidas e `< 10 MB`, (3) `14/14` idiomas 100% sincronizados, (4) Aprovação visual do Rodolfo.

---

## 🏁 FASE 0 — PORTÃO DE PRODUÇÃO & CAIXA IMEDIATO (`24/11` a `26/11`)
- [x] App `v2.8.2 (vc 34)` auditado e validado no Moto G84 (zero `MANAGE_EXTERNAL_STORAGE`, zero `AD_ID`, zero `ACTION_SEND`, `631/631` strings em 14 idiomas).
- [x] Edge Functions `verify-play-purchase` (`v9`) + `play-rtdn-webhook` (`v5`) ativas no Supabase.
- [x] Relógio do Teste Fechado corrigido em `public.play_console_sync_state` + `get_prelaunch_stats()` (`23 testadores`, `Dia 12 de 14` dinâmico).
- [x] Políticas `/privacidade.html` e `/privacy.html` sincronizadas (zero menção a Estúdio + e-mail `srodolfobarbosa@gmail.com`) no Firebase Hosting (`Release 1795515742674000`, `HTTP 200`).
- [x] Sincronização final das 14 fichas da Play Store (`Edit 15816757970827593747`, `HTTP 200`: `< 8 MB`, correção `bn-BD`, 56 screenshots `v2.8.2` `1080x1920` + 14 `featureGraphic` `1024x500` verificados ao vivo na API v3).
- [x] Faxina documental + criação de [`docs/INDICE_MESTRE.md`](INDICE_MESTRE.md) (`167` documentos históricos movidos para `docs/archive/` com bloqueio no deploy Firebase).
- [ ] **Quinta `26/11`:** Solicitar acesso à Produção na Google Play Console com respostas auditadas de `docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`.
- [ ] **Dia 1 de Produção:** Ligar funil orgânico `@aconteceunobr` + Google Ads (`pt-BR` / `en-US`) e confirmar 1ª venda real de Produção em `public.play_purchases`.

---

## 🚀 FASE 1 (FRENTE B — PRIORIDADE #1 DE CONVERSÃO PRO) — MOTOR DO APP (`feat/v2.9.0-motor-app`)
- [x] **1.1 Núcleo Fotos Parecidas / Similares On-Device (`PerceptualImageHasher.kt` — `dHash 64-bit` + Burst Anti-Falso-Positivo):** agrupar fotos quase idênticas protegendo automaticamente a mais nítida (`isProtected = true, isSelectable = false`).
- [x] **1.2 Cache Incremental SQLite WAL de Hashes (`AndroidMediaFingerprintStore.kt` + `AndroidSimilarPhotoScanner.kt`):** reabertura instantânea sem reprocessar miniaturas intactas (`0 KB` de dependências externas).
- [ ] **1.3 Integração UI Fotos Parecidas + Preview Tela Cheia & Comparador Lado a Lado:** conectar no painel `Limpar`/`Pro` + zoom rápido antes de excluir + QA lado a lado no Moto G84.
- [ ] **1.4 Compressor de Vídeos Hardware-Accelerated (`Media3 Transformer H.265/HEVC` — Recurso Pro):** comprimir vídeos grandes sem perda visível (100% na GPU local, zero custo cloud) com segurança pela Lixeira Android.
- [ ] **1.5 Refatoração Modular da `MainActivity.kt` (`361 KB`) + Remoção de Código Morto (`~170 KB`):** eliminar `presentation/studio/*` (jogos/jornal/estúdio descontinuados) e `MariaLipSyncEnvelopes.kt`, extraindo `CleanViewModel`, `FilesViewModel` e `BillingViewModel`.
- [ ] **1.6 Histórico Acumulado de GB Liberados + `InAppReview` Pós-Limpeza:** prova de valor contínua na Home + pedido nativo de 5 estrelas no pico de satisfação.

---

## 👩‍💼 FASE 2 — MARIA 2.0 (EXPRESSÕES FACIAIS NEURAIS REAIS & VOZ 14 IDIOMAS)
- [ ] **2.1 Micro-Loops Neurais Reais no App (`LivePortrait / Hallo3 / Veo` em WebP/H.265 `< 1,5 MB`):** expressões faciais humanas naturais (olhar, sobrancelhas, respiração, sorriso e articulação labial real) substituindo qualquer recorte 2D de boca.
- [ ] **2.2 TTS Neural Dinâmico nos 14 Idiomas (`Android Neural TTS` + Visemas):** falar em todos os 14 idiomas com os números exatos de MB/GB encontrados no aparelho do usuário.
- [ ] **2.3 Avatar 3D/Neural 60 FPS no Desktop (`FRIDAY Companion 2.0`):** evoluir do warp 2D atual para rig facial completo de **52 blendshapes ARKit + Oculus Visemes** (`friday-core/avatar-3d/talkinghead.mjs`) ou renderização neural dedicada.
- [ ] **2.4 Consultora Local Híbrida (`Gemini Nano AICore` + Motor Determinístico MediaStore):** respostas contextuais inteligentes 100% privadas no aparelho.

---

## 💰 FASE 3 — ESCALA RUMO A R$ 1.000.000,00 & PILAR 2 (REGRA 50/50)
- [ ] **3.1 Otimização A/B de Paywall (`get-paywall-variant`):** maximizar LTV entre Anual (`R$ 79,99`) e Vitalício (`R$ 149,99`) por país.
- [ ] **3.2 Domínio `midiastore.app` + Escala pSEO Internacional (139 páginas):** financiado pelos 50% de reinvestimento das primeiras vendas.
- [ ] **3.3 Máquina Orgânica Multicanal (`@aconteceunobr` + Shorts + TikTok):** ativação pós-aprovação de Produção.
- [ ] **3.4 Fábrica Multiproduto FRIDAY (Pilar 2):** replicar a base blindada para novos apps utilitários de alta conversão após tração do Midiastore.
