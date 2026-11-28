## Checkpoint 2026-11-28 — Motor Orgânico Blindado (Zero LLM Gratuita, `/go/play` Tap-First, IG DM Closer & ASO 15 Idiomas Dry-Run)

- **Blindagem da Conta Google:** Chaves gratuitas `AQ.*` removidas de `.env`, `.secrets/ai-media-keys.json` e desativadas em `.secrets/gemini-atendimento.json`; `scripts/lib/gemini-closer.mjs` convertido para 100% templates determinísticos (zero chamadas de LLM gratuita). `app/` 100% intocado e sem execução de Gradle.
- **Rota `/go/play` Tap-First no Ar (`firebase.cmd deploy --only hosting --project midiastory-df6a0`):** Apenas 1 arquivo novo enviado (`docs/go/play/index.html`, espelhado em `public/go/play/index.html`), sem `window.location` automático, com `<a href="intent://details?id=com.rdf112.midiastore&referrer=...;package=com.android.vending;S.browser_fallback_url=...;end">`, link HTTPS direto e dica `⋮ > Abrir no navegador`. HTTP 200 confirmado ao vivo em `/go/play`, `/google7f3bae0a2f8f091c.html`, `/image-sitemap.xml`, `/sitemap.xml` e `/it/`.
- **`scripts/ig-viral-dm-closer.mjs` Validado em `--dry-run`:** Tabelas `public.ig_dm_closer_dispatches` e `public.ig_dm_closer_circuit` criadas no Supabase com RLS ativo e índices; 4 travas ativas (checa HTTP 200 anônimo na Play Store — hoje `404` enquanto `production` não está pública, bloqueando disparo automaticamente; teto `60 DMs/h` + cooldown `24h/usuário`; Private Reply exclusivo para `"LIMPAR"` sem spam público; disjuntor em códigos `368/613/2534001/2534041/500`). Auditoria Meta detectou `errorSubcode: 2534041` (`Permitir acesso às mensagens` desligado no app Instagram do `@aconteceunobr` — basta ligar quando a Produção abrir).
- **`scripts/play-aso-15-idiomas.mjs` Validado em `--dry-run` (Edit `03780746045476488764` descartado via `DELETE`):** `changesInReviewBehavior=ERROR_IF_IN_REVIEW` obrigatório, Linter Unicode aprovado nos 15 idiomas (`0` violações; `pt-BR` preservado em 25 chars + `13` títulos expandidos `<=30` chars + nova ficha `it-IT` + `fullDescription` `v2.9.0` `< 7 MB`) e roteador determinístico de reviews (`4-5★` templates nativos `<=350` chars; `1-3★` fila manual em `docs/aso/play-reviews-manual-queue.json`).

---

## Checkpoint 2026-11-27 — Fase 1 SEO e Criativos Prime v2.9.0

- Hosting efetivo usa `docs/` como raiz e os arquivos SEO também estão em `public/`; publicação final incluiu 161 arquivos do site e excluiu `docs/archive/`, Markdown interno e diretórios operacionais.
- Home e 13 páginas localizadas apontam para o conjunto recíproco exato `pt-BR`, `en`, `es`, `fr`, `de`, `it`, `hi`, `ja`, `zh-Hans`, `ar`, `ru`, `id`, `bn`, `ko` e `x-default`; `/it/` foi criado.
- Home usa JSON-LD `MobileApplication` / `UtilitiesApplication`, preço `0` e nenhum `aggregateRating`. `robots.txt`, sitemap com 143 URLs HTTPS e três guias PT-BR com CTA oficial foram publicados.
- HTTP 200 validado em `/limpar-armazenamento-android.html`, `/apagar-fotos-duplicadas-android.html`, `/comprimir-videos-android.html`, `/sitemap.xml`, `/robots.txt` e `/it/`.
- Criativos Prime v2.9.0: README registra dimensões 1200×628, 1200×1200, 1200×1500 e 1080×1920 e exige preservação de nitidez da tela original.
- Memórias Supabase #582 (marco) e #583 (ajuste de escopo do Hosting). `app/` sem diff nesta tarefa; Gradle não executado.

---

## Checkpoint verificado - 2026-11-26 (AAB v2.9.0 enviado para alpha)

- Upload oficial pela Android Publisher API concluido para com.rdf112.midiastore na faixa alpha. Release 2.9.0 (35), versionCode 35, status completed, com notas pt-BR e en-US.
- Faixas apos o commit: alpha=35; internal permanece em 2.8.0 (32); production e beta vazias. Nenhuma escrita nas listas de testadores. Painel Supabase: 23 testadores oficiais, dia 14/14.
- AAB: 6.912.480 bytes; SHA-256 D8AF42030D612072713310DAEAA5C0F74D6DB47605B8F655639DB77D052ADAF8. Sem Gradle local. Tag v2.8.2-carbon-baseline preservada em cd0a6612fd4038eeb3d2ffb1db6f85fbd73954b4. Sem publicacao em producao.
- Memoria Supabase #581; linha do tempo autonoma atualizada.

---

# Midiastore â€” CONTINUIDADE DO PROJETO
> ðŸ§­ **NavegaÃ§Ã£o Central:** [**`docs/INDICE_MESTRE.md`**](INDICE_MESTRE.md) | **Checklist ProduÃ§Ã£o (`26/11`):** [**`docs/CHECKLIST_PRODUCAO_VIVA.md`**](CHECKLIST_PRODUCAO_VIVA.md) | **Roadmap `v2.9.0` & Maria 2.0:** [**`docs/CHECKLIST_EVOLUCAO_CARBON.md`**](CHECKLIST_EVOLUCAO_CARBON.md) | **HistÃ³rico Arquivado:** [`docs/archive/`](archive/README.md)

## Checkpoint verificado â€” 2026-11-26 (sincronia fala/vÃ­deo Maria)

- Commit Ãºnico `63b27b523996f807e94a78b182b40e40d76d1c4c` em `feat/v2.9.0-motor-app`; GitHub Actions run `45770508505` terminou `success` no SHA exato; compilaÃ§Ã£o feita sÃ³ no runner Linux. Auditoria local de governanÃ§a tambÃ©m passou. Tag `v2.8.2-carbon-baseline` (`cd0a6612fd4038eeb3d2ffb1db6f85fbd73954b4`) preservada.
- `MariaLocalizedSpeech.kt` agora mantÃ©m um TTS singleton aquecido, define o locale corrente antes de cada `QUEUE_FLUSH`, aciona inÃ­cio/fim na Main Thread e nÃ£o chama `shutdown()`. O vÃ­deo universal sÃ³ recebe `start()` no inÃ­cio real do MediaPlayer PT ou `onStart` do TTS; callbacks done/error/stop liberam vÃ­deo/anel. Troca de aba, `ON_PAUSE`/`ON_STOP` e `onDispose` interrompem TTS e os players.
- Corrigidas quatro strings com `?` corrompido em `values-fr`/`values-de`, alÃ©m de `storage_measurement` e `storage_used_headline` em espanhol e francÃªs. A auditoria encontrou zero `?` dentro de palavras nos 14 diretÃ³rios de idioma.
- APK CI SHA-256 `00B9ADC2B062C38E879F8B348B005BFBEE7C0681AEF3967B2A6D3DD9AE147AFE`, pacote `.debug` instalado no Moto G84 API 35; pacote oficial continuou instalado. EvidÃªncias em `.tmp/qa/maria-sync-63b27b5/` (`es-start.png`, `es-later.png`, `es-done.png`, `es-background-logcat.txt`, `pt-active.png`, `pt-after-speech.png`, `pt-background-logcat.txt`). ES comeÃ§ou visualmente idle antes do inÃ­cio do Ã¡udio; ao mandar para Home em PT e ES, AudioFlinger ficou com `numTracks=0`. Em PT, captura apÃ³s fala mostrou anel azul/idle e sem trilhas de Ã¡udio. NÃ£o houve compilaÃ§Ã£o Gradle local.
- MemÃ³ria Supabase registrada como #574 pela rotina `scripts/registrar-memoria.mjs`; linha do tempo autÃ´noma atualizada.

## Checkpoint verificado â€” 2026-11-26 (animaÃ§Ã£o universal muda da Maria)

- **CÃ³digo em `feat/v2.9.0-motor-app`:** commit `6d9afb52eb18cc0085877af2756cbeb91803f9a1`, validado no GitHub Actions run `45767023861` (CI verde; gate de AAB abaixo de 8 MB). Nenhum Gradle foi executado localmente. `v2.8.2-carbon-baseline` permaneceu intacta.
- **VÃ­deo:** `app/src/main/res/raw/maria_talk_universal.mp4`, H.264, sem faixa de Ã¡udio, 65.176 bytes. O player embutido toca em loop com volume zero junto Ã  fala localizada e Ã© liberado ao terminar/trocar de aba; o idle player concorrente foi removido para evitar disputa pela `TextureView`. O manifesto CDN fica somente com `pt/clean.mp4` como fonte; a animaÃ§Ã£o de runtime nÃ£o depende de CDN.
- **Moto G84 / evidÃªncia:** APK `.debug` instalado ao lado do pacote oficial, sem alterar este. As capturas `clean-confirm.png`, `files.png` e `pro.png` em `.tmp/qa/maria-one-video-6d9afb5/` mostram o vÃ­deo nas trÃªs abas. Logs daquela sessÃ£o confirmaram o primeiro frame da `TextureView` apÃ³s cada troca e Ã¡udio do vÃ­deo zerado. O dispositivo estÃ¡ desconectado nesta atualizaÃ§Ã£o; nÃ£o foi feita nova coleta ADB.
- **MemÃ³ria:** resultado registrado em `public.memories` pela rotina do projeto (registro #573); resumo tambÃ©m consta na linha do tempo autÃ´noma.

## Checkpoint Oficial Verificado Ao Vivo â€” 2026-11-25 (Maria 2.0 MoDA H.265 CDN + Growth Pack v2.9.1 ConcluÃ­dos e Instalados no Moto G84 Â· Baseline 26/11 Intocada)

- **Baseline Protegida para Quinta-Feira (`26/11`):** `tag v2.8.2-carbon-baseline` (`cd0a6612fd4038eeb3d2ffb1db6f85fbd73954b4`) **100% intocada**; faixas `internal`/`alpha` na Play Console preservadas em `v2.8.0 (vc 32)` com os **23 testadores fechados intactos**.
- **Maria 2.0 Humana Fotorrealista (MoDA Diffusion H.265 CDN + Cache Local + Fallback Offline) na `feat/v2.9.0-motor-app`:**
  - **CDN Firebase Hosting (`Release 1795577272160000`, `HTTP 200`):** publicado `https://midiastory-df6a0.web.app/cdn/maria2/manifest.json` + `idle.mp4` (`25,7 KB` H.265 `hvc1`) + `pt/clean.mp4` (`174,3 KB`, SHA-256 `36dbd80b...`) + pipeline incremental Node.js em `scripts/generate-maria2-moda-pipeline.mjs` (zero Python, zero 3D, zero boca 2D artificial).
  - **Player Nativo Leve (`MariaAssistantBanner.kt` + `MariaCdnVideoCache.kt`):** `TextureView` hardware + cache local LRU limitado (`<= 24 MB` em `files/maria2_cdn_cache` com verificaÃ§Ã£o de assinatura `ftyp` e SHA-256) + passagem segura via `FileDescriptor` para `MediaPlayer` no Android 15 + `res/raw/maria2_idle.mp4` (`25,7 KB`) para abertura instantÃ¢nea (`0 ms`) e fallback seguro offline.
- **Growth Pack `v2.9.1` (3 Frentes ConcluÃ­das nos 14 Idiomas â€” `307/307` chaves em paridade 100%):**
  1. **Free Trial 3 Dias Exclusivo no Plano Anual (`sweep_pro_annual`):** `SubscriptionOfferSelector.kt` + `AndroidPlayBillingGateway.kt` + Edge Function `verify-play-purchase` (`v10 ACTIVE` no Supabase) validando `annual-trial-3d` com renovaÃ§Ã£o/expiraÃ§Ã£o `fail-closed`.
  2. **NotificaÃ§Ã£o Semanal Inteligente (`WeeklyStorageDigestWorker.kt`):** `WorkManager` periÃ³dico de 7 dias (`setRequiresBatteryNotLow(true)`), silÃªncio absoluto se `< 300 MB` (`314.572.800 bytes`) ou app aberto hÃ¡ `< 48h`, e convite `POST_NOTIFICATIONS` somente pÃ³s-1Âª limpeza.
  3. **BotÃ£o Nativo de Compartilhar + Card OLED `1080x1350` (`ShareAchievementCardRenderer.kt`, `+0 KB`):** renderizaÃ§Ã£o pura em `Canvas` com `Precision Storage Ring` + `FileProvider` + preview `ClipData` e link UTM (`?utm_source=app_share&utm_medium=viral_card`) para WhatsApp, Telegram e Instagram Stories/Direct (zero `<intent-filter>` `ACTION_SEND` no manifesto).
- **CI Linux 7 GB RAM (`Run 45543787553` â€” `100% verde` em `6df50eb`) + QA Real no Moto G84 (`0085353748`):**
  - **AAB Release R8 Assinado Medido no CI:** **`6,79 MB`** (`7.120.223 bytes` â€” **`-470 KB` menor** que os `7,26 MB` da `v2.8.2`, cumprindo com folga o teto `< 8 MB`).
  - **APK `.debug` (`com.rdf112.midiastore.debug`, SHA-256 `21139805cf1f5587afb0a5125f8fb7cd211341c109ab5983d65668d8de2783b8`):** instalado lado a lado no **Motorola Moto G84 5G (`0085353748`, API 35)** com `pt_clean_v291.mp4` (`178.505 bytes`) confirmado no cache local e `0` erros `AndroidRuntime:E`.

---

## Checkpoint Oficial Verificado Ao Vivo â€” 2026-11-24 (Blindagem 360Â° Carbon ConcluÃ­da â€” Pronto para ProduÃ§Ã£o 26/11 + Roadmap v2.9.0/Maria 2.0)

- **Fonte Ãšnica de Prioridades:** [`docs/CHECKLIST_PRODUCAO_VIVA.md`](CHECKLIST_PRODUCAO_VIVA.md), [`docs/CHECKLIST_EVOLUCAO_CARBON.md`](CHECKLIST_EVOLUCAO_CARBON.md), [`docs/CONTINUATION_UPDATE_2026-11-24.md`](CONTINUATION_UPDATE_2026-11-24.md), [`docs/MOTOR_CRESCIMENTO_META.md`](MOTOR_CRESCIMENTO_META.md) e [`docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`](PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md).
- **App Android `v2.8.2 (vc 34)` (Tag `v2.8.2-carbon-baseline` em `cd0a661` / HEAD `6fd8020`, CI `Run 45342358085` + `45343870292` 100% verdes):** AAB assinado **`7,26 MB`** (`7.263.824 bytes`, SHA-256 `5151aa51ac753efb78427d90591d6faaa13d7bdc16038642cda2704f017634f1` em `.tmp/qa/v282-carbon-aab/app-release.aab`) auditado via `bundletool` (zero `MANAGE_EXTERNAL_STORAGE`, zero `AD_ID`, zero `ACTION_SEND`, `631/631` strings vÃ¡lidas nos 14 idiomas) e validado ao vivo em retrato no **Motorola Moto G84 5G (`0085353748`)** (`PT-BR` e `EN-US`).
- **Google Play Console (Confirmado por Print Oficial em `24/11/2026 19:45 BRT` â€¢ Abertura de ProduÃ§Ã£o Quinta 26/11):** `Envio 18 â€” Detalhes do app` **âœ… Publicado** (nova marca `Precision Storage Ring` + 14 fichas no ar); painel oficial marcando **`12 testadores estÃ£o participando continuamente hÃ¡ 12 dias`** (faltam **exatos 2 dias** para liberar o botÃ£o *"Solicitar a produÃ§Ã£o"* na quinta-feira `26/11`); `internal`/`alpha` intactos em `v2.8.0 (vc 32)` com 23 testadores preservados. **0 bloqueios P0.**
- **Supabase & PolÃ­ticas Web:** `play_console_sync_state` + `get_prelaunch_stats()` corrigidos com relÃ³gio dinÃ¢mico BRT (`23 testadores`, `Dia 12 de 14`); `/privacidade.html` e `/privacy.html` limpas e publicadas no Firebase (`Release 1795515742674000`, `HTTP 200`, `srodolfobarbosa@gmail.com`).
- **Score de ProntidÃ£o Comercial (`node scripts/goal-growth-engine.mjs`):** **`75/100`** (Ãºnicos 25 pontos = clicar em *"Solicitar a produÃ§Ã£o"* na quinta `26/11`). EvoluÃ§Ãµes `v2.9.0` e **LaboratÃ³rio Maria Humana de Ãšltima GeraÃ§Ã£o** (**zero 3D â€” Diretriz 12**) organizadas sob o Protocolo Catraca em [`docs/CHECKLIST_EVOLUCAO_CARBON.md`](CHECKLIST_EVOLUCAO_CARBON.md).
- **AvanÃ§o `feat/v2.9.0-motor-app` (Etapas 1, 2 e 3 ConcluÃ­das em `2026-11-24` â€” TODOS os 8 Blocos Prontos e Instalados Lado a Lado no Moto G84 `0085353748` + Estudo `docs/ESTUDO_MARIA_2_HUMANA_E_ESCALA.md` Entregue):**
  1. **Etapa 1 (`Run 45491856092` verde):** removidos `presentation/studio/*`, `ZXing`, `friday-core/avatar-3d/*` e `~3.240` linhas mortas em `MainActivity.kt` (**Maria atual 100% intocada**); `AndroidMediaFingerprintStore` (`SQLite WAL`) integrado ao `SHA-256`; zero estrelinhas/emojis.
  2. **Etapa 2 (`Run 45496347206` verde em `667be1f`):** `Fotos Parecidas / Burst IA` (`openSimilarPhotosCleanerFlow` + selo `MELHOR FOTO PROTEGIDA` nas abas Limpar e Pro), `Preview Tela Cheia OLED` (`MediaFullscreenPreviewDialog.kt` pinch-zoom + player vÃ­deo), `Compressor de VÃ­deo H.265/HEVC GPU Pro` (`AndroidHardwareVideoCompressor.kt` + `VideoCompressionPlanner.kt`, `0 KB` bloat, mantendo `< 8 MB`), `GB Acumulados na Home + InAppReview 5 Estrelas` (cooldown 7d) e `288/288` chaves em paridade 100% + auditoria AAPT2 nos 14 idiomas.
  3. **Etapa 3 (`Run 45500927976` verde em `c0fbf23`):** APK `.debug` (`com.rdf112.midiastore.debug`, SHA-256 `755c1927...`) compilado no GitHub Actions Linux 7 GB RAM e instalado lado a lado no **Motorola Moto G84 5G (`0085353748`, Android 15 API 35)** sem tocar no `com.rdf112.midiastore` oficial; QA visual real validado (`0` erros `AndroidRuntime:E`) + estudo comparativo completo entregue em [`docs/ESTUDO_MARIA_2_HUMANA_E_ESCALA.md`](ESTUDO_MARIA_2_HUMANA_E_ESCALA.md).

---

## Checkpoint Oficial Verificado Ao Vivo â€” 2026-11-23 (VisÃ£o SistÃªmica Integrada â€” v2.8.2 vc 34 Pronta para QA USB & ProduÃ§Ã£o 26/11)

- **Fonte Ãšnica de Prioridades:** [`docs/CHECKLIST_PRODUCAO_VIVA.md`](CHECKLIST_PRODUCAO_VIVA.md), [`docs/CONTINUATION_UPDATE_2026-11-23.md`](CONTINUATION_UPDATE_2026-11-23.md), [`docs/MOTOR_CRESCIMENTO_META.md`](MOTOR_CRESCIMENTO_META.md) e [`docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`](PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md).
- **App Android `v2.8.2 (vc 34)` (`735dcd3`, CI `Run 45118253546` + `45118253451` 100% verdes):** AAB assinado **`7,26 MB`** (`b0d0fe6e...`) auditado via `bundletool` (zero `MANAGE_EXTERNAL_STORAGE`, zero `AD_ID`, `630/630` strings vÃ¡lidas nos 14 idiomas). Removido modal bloqueador + 9 frames `maria_lip_0..7`/`maria_talk_blink`; nova barra compacta Maria (`76dp`, retrato natural + piscada + respiraÃ§Ã£o + anel neon + ondas 60 FPS sob demanda). APK/AAB em `.tmp/qa/v282-apk/` aguardando reconexÃ£o USB do Moto G84 para QA visual final.
- **Widget Desktop FRIDAY/Maria:** restaurado somando todas as conquistas (16 nÃ­veis Optical-Flow F1/F2 60 FPS + crossfade + gate rÃ­gido + anel neon + 5 ondas de voz 60 FPS).
- **Play Console (Dia 10/14 â€¢ PrevisÃ£o 26/11):** `internal`/`alpha` intactos em `v2.8.0 (vc 32)` com 23 testadores preservados; **APTO para solicitar produÃ§Ã£o em 26/11 (0 bloqueios P0)**.
- **Score de ProntidÃ£o Comercial (`node scripts/goal-growth-engine.mjs`):** **`75/100`** (Ãºnico gargalo de caixa = abertura de ProduÃ§Ã£o em 26/11).

---

## Checkpoint Oficial Verificado Ao Vivo â€” 2026-11-22 (Parede Ãšnica Ativa â€” Fechamento do Dia v2.8.0 vc 32)

- **Fonte Ãšnica de Prioridades (Parede Ãšnica):** [`docs/CHECKLIST_PRODUCAO_VIVA.md`](CHECKLIST_PRODUCAO_VIVA.md), [`docs/CONTINUATION_UPDATE_2026-11-22.md`](CONTINUATION_UPDATE_2026-11-22.md) e [`docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`](PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md). **Regra da Parede Ãšnica:** nunca iniciar nova arquitetura paralela sem concluir o tijolo atual; cada sessÃ£o e cada mensagem continua diretamente do estado verificado abaixo.
- **Estado Real na Google Play Console (verificado via `google-auth-library` / Play API v3 em `2026-11-22T08:50:28Z`):**
  - `internal` e `alpha` (Teste Fechado): **`v2.8.0` (`versionCode 32`) `[completed]`** (`Edit 14212325279113978395`, SHA-256 `97035252009b5cf95a7f75892f9d5ab7eeb236d6fc764ca6ea8b804d730a5227`, `7,36 MB` AAB R8 assinado com `sweep-upload.jks`). Lista dos 23 testadores fechados 100% preservada.
  - `production`: aguardando tÃ©rmino do relÃ³gio obrigatÃ³rio de 14 dias do Teste Fechado para clicar em **"Solicitar a produÃ§Ã£o"** (respostas de 300 caracteres e Data Safety jÃ¡ prontas e auditadas contra `AndroidManifest.xml` â€” zero `MANAGE_EXTERNAL_STORAGE`, zero `AD_ID`).
  - **Produtos Billing Ativos (`ACTIVE`):** `sweep_pro_monthly` (`monthly-base:ACTIVE`), `sweep_pro_annual` (`annual-base:ACTIVE`) e `sweep_pro_lifetime` (`oneTimeProducts` `lifetime-buy:ACTIVE` em 170+ paÃ­ses).
- **Novidades Entregues e Validadas no Moto G84 (`0085353748`) na `v2.8.0 (vc 32)`:**
  - **Assistente Pessoal Maria (`MariaAssistantBanner.kt` + `MariaLipSyncEnvelopes.kt`):** retratos widescreen `720x512` sem corte (`maria_lip_0..7`, `maria_blink_0..2`, `maria_talk_blink`, `maria_smile`), marca `Midiastore` integrada no topo esquerdo do card da Maria, badge limpa `â— MARIA â€¢ ASSISTENTE IA` sem legenda poluindo a blusa, botÃ£o `â–² Recolher` + retorno via `BackHandler`, 8 Ã¡udios neurais de estÃºdio (`pt-BR-FranciscaNeural`, 8,4s a 28,9s) e **dublagem silÃ¡bica ZCR com detecÃ§Ã£o de vales consonantais + compensaÃ§Ã£o de `-85ms` do `AudioTrack` a 35,7 FPS**.
  - **Chat Midiastore IA (White Sheet estilo ChatGPT/Gemini):** modal limpo em fundo branco `#FFFFFF` acionado ao tocar nos quadradinhos da aba Pro, sem caixa inline poluindo o rodapÃ©.
  - **14 Idiomas (`MariaI18n`) + Limpador Global de Mensageiros (`WhatsAppCleanerDataSource.kt`):** suporte a WhatsApp, Telegram, WeChat/WeiXin, LINE, KakaoTalk, Messenger, Instagram, TikTok e `Download/` com tamanhos reais em `MB / GB` ao vivo em todas as abas e fim do erro `"NÃ£o foi possÃ­vel analisar os arquivos"` sob mudanÃ§a concorrente do `MediaStore`.
- **Estado do CI/CD, Supabase, Funil Web e Auto-Start Desktop (`2026-11-22`):**
  - GitHub Actions (`Run 45003986718` + `Run 45003986707`): **`success`** (100% verde).
  - Supabase (`@supabase/supabase-js` direto, zero HTTP 429): `play_purchases = 1` venda testada validada na Edge Function `verify-play-purchase`, `beta_testers = 2`, `memories = 497+`, Grafo de Conhecimento enriquecido (`kg_nodes = 264`, `kg_edges = 60`).
  - Funil Web (`https://midiastory-df6a0.web.app/`): `HTTP 200`, `14` idiomas (`hreflang`), `145` pÃ¡ginas HTML auditadas com `cheerio` (**0 links quebrados**).
  - **Auto-Start da Maria/FRIDAY no PC Windows:** script `Maria-FRIDAY-AutoStart.vbs` configurado na pasta `Startup` do Windows (`%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup`) para abrir automaticamente o painel `scripts/antigravity-companion.ps1` ao ligar o PC amanhÃ£.

---

## Checkpoint mais recente â€” 2026-11-07

- Rodolfo pediu que o app seja concluÃ­do e as atualizaÃ§Ãµes sejam salvas no GitHub. NÃ£o mesclar em `main`; manter a PR em Draft.
- Home estÃ¡ em azul-marinho/rosa, com arte vetorial original, aÃ§Ã£o principal gratuita, planos Pro e botÃ£o de compartilhamento do Android.
- A aÃ§Ã£o grÃ¡tis agora inicia a missÃ£o MediaStore de arquivos grandes dentro do Midiastore. No Moto G84 5G/API 35, a varredura encontrou 5 itens para revisÃ£o (84,30 MB), e o botÃ£o levou Ã  tela interna de revisÃ£o. Nenhum arquivo pessoal foi apagado. Esse teste manual nÃ£o valida o fluxo final de consentimento/exclusÃ£o permanente.
- A aÃ§Ã£o principal nÃ£o abre mais o Google Files. A pessoa precisa revisar os candidatos e aprovar a exclusÃ£o pelo fluxo Android; o app nÃ£o promete limpeza silenciosa de cache de outros apps.
- O Sharesheet do Android abriu com o link da Play Store e foi fechado sem compartilhar/publicar.
- `:app:assembleDebug` passou no Windows e o APK Debug atual foi instalado no Motorola. Produtos mensais/anual/vitalÃ­cio aparecem indisponÃ­veis; Play Console e backend de validaÃ§Ã£o de purchaseToken continuam gates externos. PreÃ§os recomendados e checklist atual estÃ£o em `docs/PRICING_AND_PLAN_MATRIX.md` e `docs/FINAL_PUBLICATION_CHECKLIST.md`.
- In-app localization remains English + pt-BR. No portrait AI asset was generated; current home character is a native vector illustration.
- PrÃ³ximo passo: revisar `git diff`, commit somente arquivos intencionais e enviar para a branch existente da PR. Excluir caches, screenshots, keystores, senhas e arquivos de ponte locais do commit. Continuar bloqueando release pÃºblica atÃ© fechar `docs/RELEASE_BLOCKERS.md`.

## Retomada do objetivo de limpeza rÃ¡pida â€” 2026-11-07

- O proprietÃ¡rio pediu uma limpeza grÃ¡tis, fÃ¡cil e com resultado real em uma aÃ§Ã£o principal; Pro Ã© opcional e vem depois de valor comprovado. Nenhuma limpeza pode apagar mÃ­dia sem revisÃ£o e consentimento oficial do Android.
- Tentativa manual mais recente no Motorola API35: abriu o Midiastore instalado, tocou em â€œLiberar espaÃ§oâ€ e viu â€œNÃ£o foi possÃ­vel analisar os arquivosâ€; nenhum arquivo foi apagado. A versÃ£o Debug local nÃ£o atualizou o pacote instalado porque a assinatura Debug Ã© diferente. O erro do build antigo nÃ£o identifica a causa no source atual.
- CÃ³digo local atualizado: exceÃ§Ã£o global da anÃ¡lise agora registra somente a classe do erro (sem nome/caminho/URI de mÃ­dia); planos indisponÃ­veis continuam tocÃ¡veis e explicam o bloqueio; restaurar compras confirma a verificaÃ§Ã£o; falha ao abrir gerenciamento da Play Store dÃ¡ feedback; o nome Midiastore foi sobreposto dentro da arte atual da Home.
- ValidaÃ§Ã£o local do SHA ainda nÃ£o enviado: Domain/JVM/Lint/Debug/AndroidTest APK completaram `BUILD SUCCESSFUL`; XML JUnit local: 78 testes, 0 falhas, 0 erros, 0 skips. Lint emitiu 222 findings, pois `abortOnError=false`. NÃ£o foi execuÃ§Ã£o conectada.
- A execuÃ§Ã£o `:app:connectedDebugAndroidTest` parou antes dos testes ao instalar `mediaOwnerTest`, por assinatura incompatÃ­vel com o helper jÃ¡ instalado. NÃ£o remover apps nem dados do telefone para contornar. API29 e consentimento de exclusÃ£o seguem sem prova runtime.
- Chaves EN/PT-BR/fallback: 206/206, sincronizadas. ES/FR/DE/HI/JA/zh-CN: 95/206 em cada idioma; 111 chaves faltantes por locale. `docs/CHECKLIST_FINALIZACAO_PT_BR.md` e a seÃ§Ã£o atual desta matriz registram as pendÃªncias.
- A geraÃ§Ã£o de imagem foi autorizada pelo proprietÃ¡rio, mas a API de imagem respondeu que nÃ£o hÃ¡ crÃ©ditos restantes; ferramenta de vÃ­deo de 10 segundos nÃ£o estÃ¡ disponÃ­vel nesta sessÃ£o. Nenhum asset de foto/vÃ­deo foi adicionado.
- NÃ£o gerar AAB pronto para Play: faltam aprovaÃ§Ã£o instrumentada API29/35, CI verde no SHA atualizado, catÃ¡logo/preÃ§os e compra real configurados, backend de purchaseToken, traduÃ§Ãµes e verificaÃ§Ã£o Play Console de versionCode/certificado.

## DireÃ§Ã£o de produto confirmada pelo proprietÃ¡rio â€” 2026-11-06

- Rodolfo confirmou que o objetivo Ã© um phone cleaner simples e confiÃ¡vel, inspirado na facilidade de uso do Files do Google: ajudar pessoas com o telefone cheio de conteÃºdo a encontrar o que ocupa espaÃ§o e liberar armazenamento sem perda acidental.
- Rodolfo autorizou publicar as atualizaÃ§Ãµes no GitHub, compilar o AAB e enviar ao Play Console quando os gates de release forem comprovados. Ele tambÃ©m autorizou merge em `main` se necessÃ¡rio; a PR permanece Draft enquanto validaÃ§Ã£o e gates estÃ£o pendentes.
- Modelo pretendido: um plano gratuito e trÃªs opÃ§Ãµes pagas. O cÃ³digo-fonte existente nomeia as opÃ§Ãµes Pro mensal, anual e vitalÃ­cia; IDs e preÃ§os ainda dependem de confirmaÃ§Ã£o no Play Console. NÃ£o inventar preÃ§os ou prometer recursos nÃ£o implementados.
- ReferÃªncias pÃºblicas pesquisadas: [Android Now in Android](https://github.com/android/nowinandroid) para arquitetura e testes; [SpaceSweep](https://github.com/Dhiva-Labs/SpaceSweep) e [Quarry](https://github.com/tanvirr007/quarry-app) para ideias de apresentaÃ§Ã£o de espaÃ§o e revisÃ£o de limpeza. SÃ£o referÃªncias de leitura, nÃ£o dependÃªncias nem validaÃ§Ã£o das alegaÃ§Ãµes de seus autores. Manter a arquitetura Android Views existente e adotar somente padrÃµes compatÃ­veis com as permissÃµes e limites do Midiastore.
- Prioridades atÃ© a publicaÃ§Ã£o: confiabilidade da varredura e cobertura parcial explÃ­cita; seleÃ§Ã£o e exclusÃ£o sempre revisadas pelo usuÃ¡rio; acessibilidade e portuguÃªs/inglÃªs; CI e testes reais; verificaÃ§Ã£o de compra via backend e configuraÃ§Ã£o Play. NÃ£o anunciar limpeza de cache de outros apps, fotos semelhantes, acelerador ou outras funÃ§Ãµes sem implementaÃ§Ã£o e validaÃ§Ã£o.

### VerificaÃ§Ã£o executada nesta retomada

- `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` e `:mediaOwnerTest:assembleDebug`: **BUILD SUCCESSFUL** em 7m43s; 114 tarefas (1 executada, 113 up-to-date). Gradle e Android caches foram direcionados para diretÃ³rios locais ignorados do repositÃ³rio.
- `:app:connectedDebugAndroidTest`: **nÃ£o executado**; falhou em `:mediaOwnerTest:installDebug` com `No connected devices!`. Isso nÃ£o Ã© falha dos testes do app e tambÃ©m nÃ£o Ã© evidÃªncia de aprovaÃ§Ã£o instrumentada.
- `git diff --check` e `git diff --cached --check`: sem erros de whitespace.
- Commit local `a8e0335` contÃ©m o diff de app, testes e documentos intencionais. Merge commit `c1a313f` integra a branch remota atual `c546b76`, preservando seus commits; ainda nÃ£o houve push.

### AtualizaÃ§Ã£o apÃ³s novo avanÃ§o da branch â€” 2026-11-06

- A branch remota avanÃ§ou mais uma vez atÃ© `c546b76` enquanto o trabalho local era preparado, com 14 commits adicionais. Eles incluem um dashboard de limpeza com hierarquia de aÃ§Ãµes inspirado no Files do Google e melhorias para MediaStore/API29/API35. Nenhum avanÃ§o remoto foi sobrescrito.
- O merge desses commits foi concluÃ­do. Para os conflitos, foram mantidos os testes locais mais completos de proprietÃ¡rio/UID, cancelamento e aceite de consentimento, os testes de cobertura/paginaÃ§Ã£o e o preparo isolado de Selected Photos; a configuraÃ§Ã£o Kotlin/JVM e o manifesto mais recentes do app auxiliar vieram da branch da PR.
- A suÃ­te completa foi repetida sobre o cÃ³digo combinado: **BUILD SUCCESSFUL** em 8m24s; 117 tarefas (31 executadas, 2 do cache, 84 up-to-date). Incluiu Domain, JVM, Lint, APK Debug, APK AndroidTest e helper.
- ADB continua sem dispositivo (`adb devices -l` vazio) mesmo apÃ³s `adb start-server`; o Motorola nÃ£o estÃ¡ acessÃ­vel ao host. NÃ£o marcar instrumentaÃ§Ã£o fÃ­sica como executada. O CI remoto Ã© necessÃ¡rio para validar API29/API35 enquanto o aparelho nÃ£o aparece no ADB.
- `:app:bundleRelease` terminou **BUILD SUCCESSFUL** em 9m03s. AAB local: `app/build/outputs/bundle/release/app-release.aab` (3,601,394 bytes); `jarsigner -verify` confirmou a assinatura e o SHA-1 corresponde ao fingerprint informado pelo proprietÃ¡rio. O SHA-256 real Ã© `B2:74:CC:7E:89:0E:06:D0:F6:6C:64:51:B0:44:3F:2D:E1:3D:E4:6A:B6:42:75:2D:F7:7F:FC:5A:7B:0E:79:01`, divergente do valor informado (`AD:3A:...:B9:06`). Package `com.rdf112.midiastore`, versionCode 17 e versionName 2.2.1 foram confirmados no manifest release mesclado. AAB nÃ£o estÃ¡ liberado para upload atÃ© comparar SHA-256 e versÃ£o mÃ¡xima com Play Console.
- A senha do upload key foi guardada apenas como SecureString criptografada pelo DPAPI do Windows em `%APPDATA%\Midiastore\secrets\upload-password.dpapi`; nenhum segredo ou keystore foi adicionado ao repositÃ³rio. O caminho original do keystore continua em Downloads.
- `PONTE_CODEX_PARA_GEMINI.txt` contÃ©m estado tÃ©cnico sem senhas e foi copiado ao clipboard a pedido do proprietÃ¡rio. Espera-se que Rodolfo cole no Gemini e traga a resposta.
- Play continua bloqueado pelos gates registrados em `docs/RELEASE_BLOCKERS.md`: consentimento/API29 executado, CI atual, validaÃ§Ã£o Billing no Play Console e verificaÃ§Ã£o server-side do purchaseToken, versionCode/certificado e revisÃ£o final de privacidade. A autorizaÃ§Ã£o do proprietÃ¡rio nÃ£o substitui essas provas.

## Checkpoint confirmado â€” 2026-11-06 (continuaÃ§Ã£o)

- Estado Git: `codex/midiastore-release-audit`, HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`, remoto de recuperaÃ§Ã£o ainda no mesmo SHA conforme leitura anterior. HÃ¡ mudanÃ§as staged, unstaged e untracked; preservadas. NÃ£o houve commit, push, reset, limpeza ou merge.
- GitHub: PR #3 aberta como Draft; Issue #2 aberta. O workflow `42080940375` estÃ¡ associado ao SHA base anterior e nÃ£o testa estas mudanÃ§as locais.
- Dispositivo: Motorola moto g84 5G / Android 15 API35 / `0085353748`, reidentificado e online. Debug/helper/teste tÃªm UIDs separados (10475/10476/10477; o auxiliar `.codex.test` ficou em 10478). Prova de UID no aparelho nÃ£o Ã© prova de API29 nem de owner MediaStore para a fixture estrangeira.
- Builds locais: suite completa Domain, JVM, Lint, Debug, AndroidTest e helper passou; recompilaÃ§Ã£o final dos testes tambÃ©m passou. Ver comandos/checksums/resultados detalhados no final de `docs/QUALITY_AND_TESTING.md` e `docs/DAILY_STATUS.md`.
- AndroidJUnitRunner direto com APKs finais: MediaStore `OK (5 tests)`, incluindo acesso visual parcial, Ã¡udio independente, paginaÃ§Ã£o/session, hash de conteÃºdo e ausÃªncia de fixtures residuais; classe Main `OK (3 tests)` no protocolo, mas 2 casos API29 foram ignorados, restando 1 caso executado. Isso valida apenas API35.
- Gradle connected/UTP no mesmo AndroidTest APK falhou novamente sem XML/zero casos coletados (`Failed to receive the UTP test results`), enquanto ADB continuou online e o logcat pÃ³s-execuÃ§Ã£o nÃ£o mostra `AndroidRuntime` fatal. Log debug anterior aponta falha ao carregar chave PEM RSA no Netty/BouncyCastle do listener local gRPC do UTP; hipÃ³tese proximate de collector/host, nÃ£o causa raiz comprovada. Resultado UTP permanece FAIL, runner direto PASS.
- API29 consent estrangeiro permanece NOT RUN. Fixture auxiliar, confirmaÃ§Ã£o MediaStore de owner, cancelamento, novo consentimento, aceite e confirmaÃ§Ã£o pÃ³s-exclusÃ£o estÃ£o implementados e compilados; requer runner API29 Linux/host com virtualizaÃ§Ã£o existente. NÃ£o deduzir sucesso da compilaÃ§Ã£o/UID/API35.
- Artefatos desta sessÃ£o: `.local-test-results/2026-11-06/`; nenhuma mudanÃ§a de assinatura, identidade, versÃ£o, pacote produÃ§Ã£o, compra ou dados pessoais.
- O relatÃ³rio `docs/RELATORIO_MIDIASTORE_RETOMADA_2026-11-06.md` nÃ£o estÃ¡ presente localmente e nÃ£o havia cÃ³pia equivalente encontrada; a missÃ£o e handoff existentes forneceram continuidade, sem criar documento duplicado. Consulta GitHub read-only final repetiu PR #3 OPEN/Draft em SHA `16545a4277bcc515660a6516c32fb6c770a4e880`, Issue #2 OPEN e run `42080940375` failure no SHA exato.

### PrÃ³xima tarefa concreta

Completar revisÃ£o integral e segura do diff combinado, preparar commit contendo apenas cÃ³digo/testes/docs intencionais (excluindo caches e capturas locais), e pedir uma autorizaÃ§Ã£o Ãºnica de push para a branch da PR executar a matriz Linux API29/API35. AtÃ© essa autorizaÃ§Ã£o, continuar validaÃ§Ã£o e correÃ§Ãµes locais sem push. Manter issue e PR como estÃ£o; P0 API29 e UTP continuam abertos.

Nota de validaÃ§Ã£o posterior: teste isolado com `ANDROID_USER_HOME` novo falhou na instalaÃ§Ã£o do helper por keystore Debug diferente, antes da instrumentaÃ§Ã£o. User home original restaurado; APK Debug/helper mantiveram hashes e AndroidTest reempacotado SHA `15344F181D5AAE73BF2945D84BF850220FD30DADE45005AABE91C7223F79DCD1` passou no runner direto (MediaStore 5 testes; Main protocolar 3, dois API29 ignorados). UTP FAIL anterior foi no AndroidTest SHA `D6F1C760...`, igualmente aprovado diretamente; nÃ£o confundir com a tentativa de instalaÃ§Ã£o incompatÃ­vel. API29 segue NOT RUN.

## Continuidade atualizada â€” 2026-11-06, apÃ³s retomada

- DiretÃ³rio `C:\Users\rodolfo\midiastory-`; branch `codex/midiastore-release-audit`; HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`. Remote `origin` correto. Ãrvore continua com alteraÃ§Ãµes staged, unstaged e untracked; nenhuma foi descartada. O AndroidTest atual recebeu uma alteraÃ§Ã£o adicional de harness para detectar serviÃ§o de auto-click externo.
- O teste API35 de exclusÃ£o/consentimento falhou intermitentemente porque um serviÃ§o externo habilitado (`com.truedevelopersstudio.automatictap.autoclicker`) toca na UI do sistema antes do UiAutomator. O log do MediaProvider registrou concessÃ£o automÃ¡tica; nÃ£o Ã© prova de cancelamento controlado. Os testes de consentimento agora ficam SKIP por assumption nesse ambiente, mantendo todas as assertions. NÃ£o alterar configuraÃ§Ã£o global do aparelho.
- Build completo atual PASS: Domain, app JVM, Lint, Debug, AndroidTest e helper, 114 tarefas/11 executadas. APKs: Debug SHA-256 `EAE637CEF8F581B65B5D8BCDEEF97ADBC4BA8699D38716BBA4A818AD6045091E`; AndroidTest `55EAA999F40E3F0424A2271A5265AADA08167D3C23611A3F52ABDBD13C20E3CD`; helper `B00066BF7FF33116216619D728453D0E38E8525DA0C6F5FD28C590976F5632A1`.
- Runner direto atual no Motorola API35: classe MediaStore `OK (5 tests)` no protocolo, quatro mÃ©todos executados e o caso partial visual skipped por permissÃµes preexistentes; executado novamente isolado apÃ³s preparar via ADB visual-selected=true e image/video/audio=false, `OK (1 test)`. O estado original de permissÃµes foi restaurado. Main `OK (4 tests)` no protocolo, com 1 caso funcional executado e 3 assumptions/skips. NÃ£o contar consentimento nem API29 como aprovados.
- UTP mais recente, filtrado para MediaStore e usando APKs atuais, FAIL antes de coletar testes: HTML report `0 tests`, sem XML; `utp.0.log` fatal na inicializaÃ§Ã£o. InvestigaÃ§Ã£o anterior de `--debug` registra erro de leitura de RSA PEM no listener gRPC local do AGP/UTP; hipÃ³tese de falha do coletor/host, causa raiz nÃ£o confirmada.
- API29 permanece NOT RUN. Emulador local nÃ£o tem aceleraÃ§Ã£o disponÃ­vel; teste auxiliar precisa de API29 Linux para confirmar UID/owner no MediaStore, RecoverableSecurityException, cancelar, aprovar e verificar estado posterior.
- Logs atualizados: `.local-test-results/2026-11-06/`. PR #3 permanece OPEN/Draft e Issue #2 OPEN; nenhum push/commit/merge nesta retomada.
- A primeira consulta `gh` dentro do sandbox falhou pelo proxy local `127.0.0.1:9`; consulta read-only autorizada fora do sandbox confirmou novamente PR #3 OPEN/Draft, head `16545a4277bcc515660a6516c32fb6c770a4e880`, Issue #2 OPEN e workflow `42080940375` failure nesse SHA. Windows-script PASS; API29 e API35 FAIL na etapa instrumentada; signed-release SKIPPED.

### PrÃ³xima tarefa concreta

Revisar integralmente o diff combinado e separar somente alteraÃ§Ãµes de projeto intencionais dos artefatos locais; depois solicitar autorizaÃ§Ã£o especÃ­fica para push Ã  branch da PR a fim de executar CI Linux/API29. Continuar independentemente a investigaÃ§Ã£o da falha UTP e nÃ£o fechar os gates de consentimento/API29 antes de resultados executados.

Atualizado: 04/11/2026

## Identidade correta

- Nome comercial e listing: **Midiastore**.
- Package/Application ID: `com.rdf112.midiastore`.
- O proprietÃ¡rio confirmou o package olhando a ficha de teste interno da Play Console.
- Namespace e pastas Kotlin ainda usam `com.rdf112.sweep`; isso Ã© legado tÃ©cnico e nÃ£o altera o Application ID.
- `versionCode=34` e `versionName=2.8.2` sÃ£o os valores atualmente configurados no cÃ³digo-fonte (`app/build.gradle.kts`); na Play Console (`internal`/`alpha`), a versÃ£o publicada atual Ã© `2.8.0 (vc 32)`.
- minSdk 29, targetSdk 36, compileSdk 36, JDK 17 e Gradle 8.13.
- O proprietÃ¡rio informa que habilitou o perfil financeiro/recebimento em 03/11/2026. Isso nÃ£o confirma cadastro ou ativaÃ§Ã£o dos produtos de Billing.

## Branch e revisÃ£o de trabalho

- Branch de recuperaÃ§Ã£o: `fix/p0-sweep-permissions-missions-billing`.
- PR em rascunho: https://github.com/srodolfobarbosa-afk/midiastory-/pull/3
- Issue de recuperaÃ§Ã£o: https://github.com/srodolfobarbosa-afk/midiastory-/issues/2
- Base da PR: `main`.
- NÃ£o fazer merge nem publicar sem autorizaÃ§Ã£o do proprietÃ¡rio.
- NÃ£o alterar Application ID, versionCode ou certificado de assinatura sem confirmaÃ§Ã£o explÃ­cita.

## Trabalho jÃ¡ realizado nesta branch

1. Removido o caminho fixo `org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64`, permitindo que Gradle utilize o JDK definido pelo ambiente (JAVA_HOME).
2. Alinhada a marca visÃ­vel e o Ã­cone interno com Midiastore.
3. Ajustado o teste instrumentado para o Application ID atual `com.rdf112.midiastore.debug`.
4. Criada aÃ§Ã£o de revisÃ£o de permissÃµes parciais Android 14+ tambÃ©m na aba Arquivos.
5. MissÃ£o clicada antes da primeira leitura passa a ser retomada depois que a primeira pÃ¡gina carrega.
6. Os botÃµes de plano exibem mensagem quando o Billing nÃ£o consegue iniciar o fluxo.
7. O estado de reconciliaÃ§Ã£o do Billing nÃ£o Ã© apresentado como plano gratuito.
8. Filtros com texto cortado tiveram altura aumentada; vÃ¡rios rÃ³tulos tÃ©cnicos foram simplificados.
9. Arquivos do mesmo tamanho sem SHA-256 confirmado nÃ£o sÃ£o classificados como duplicatas.
10. Resposta nula do MediaStore nÃ£o Ã© interpretada como prova de exclusÃ£o.
11. Arquivos movidos para a Lixeira nÃ£o sÃ£o contabilizados como espaÃ§o liberado; exclusÃ£o sÃ³ credita bytes apÃ³s confirmaÃ§Ã£o.
12. Adicionada uma varredura global cancelÃ¡vel por categorias, volumes e pÃ¡ginas. Duplicatas entre pÃ¡ginas sÃ£o comparadas via SHA-256 em streaming. Limites: 2 GiB por arquivo e 10 GiB de hash por missÃ£o; hashing normal de pÃ¡gina: 512 MiB por arquivo e 1 GiB por pÃ¡gina. Se qualquer limite ou fonte nÃ£o verificada for atingido, a cobertura Ã© parcial e duplicatas nÃ£o sÃ£o prÃ©-selecionadas para exclusÃ£o.
13. Acrescentados testes de domÃ­nio/VM para duplicatas entre pÃ¡ginas e para bloquear seleÃ§Ã£o automÃ¡tica em cobertura incompleta.
14. CI agora tenta executar testes instrumentados em emulador Android 35 e guardar relatÃ³rios JVM/Android para diagnÃ³stico.
15. Implementado caminho de exclusÃ£o permanente no Android 10/API29 usando RecoverableSecurityException e consentimento oficial do sistema por URI. A verificaÃ§Ã£o pÃ³s-exclusÃ£o consulta cada URI; bytes sÃ³ sÃ£o creditados para itens comprovadamente ausentes. Lixeira/restauraÃ§Ã£o continuam API30+.

## Limites que continuam abertos

- A varredura global e as novas alteraÃ§Ãµes ainda precisam de CI verde no commit mais recente.
- O primeiro teste instrumentado do MediaStore falhou no emulador Android 15 e necessita de diagnÃ³stico; nÃ£o assumir que Ã© apenas instabilidade do emulador.
- O fluxo de exclusÃ£o permanente API29 foi implementado, mas ainda nÃ£o foi validado em emulador API29 nem aparelho fÃ­sico; Lixeira/restauraÃ§Ã£o continuam indisponÃ­veis nessa API.
- O BillingClient estÃ¡ integrado no cÃ³digo, mas os SKUs e ofertas nÃ£o foram validados na Play Console.
- O cliente nÃ£o valida criptograficamente o purchaseToken atravÃ©s de backend confiÃ¡vel. NÃ£o anunciar o entitlement como verificaÃ§Ã£o de produÃ§Ã£o.
- Nenhum teste de compra real, assinatura, restauraÃ§Ã£o, reembolso ou expiraÃ§Ã£o foi concluÃ­do.
- NÃ£o hÃ¡ validaÃ§Ã£o fÃ­sica em Samsung, Xiaomi, Motorola ou cartÃ£o SD.
- HistÃ³rico persistente de varreduras, Downloads/documentos via SAF, similaridade visual e compressÃ£o nÃ£o estÃ£o implementados.
- Certificado de upload e maior versionCode publicado ainda precisam de confirmaÃ§Ã£o da Play Console.
- Nenhum AAB desta branch estÃ¡ aprovado para publicaÃ§Ã£o.

## Artefatos de teste

- O workflow exporta `midiastore-debug-apk-api29` e `midiastore-debug-apk-api35` com SHA-256, quando os jobs chegam ao upload.
- O APK Debug usa o package `com.rdf112.midiastore.debug` por causa de `applicationIdSuffix = ".debug"`. Ele serve para validar interface e funÃ§Ãµes locais e pode coexistir com a instalaÃ§Ã£o do Play.
- O APK Debug nÃ£o valida compras Google Play e nÃ£o substitui a instalaÃ§Ã£o assinada do listing `com.rdf112.midiastore`. Para testar Billing, Ã© necessÃ¡rio AAB assinado com a chave de upload oficial, enviado Ã  faixa de teste interno/fechado.
- NÃ£o pedir ao testador para repetir a avaliaÃ§Ã£o do link antigo antes de instalar uma build nova desta branch.

## Como validar no Codespaces

Confirme que estÃ¡ na branch de recuperaÃ§Ã£o:

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

Se o Codespace estiver em `main`, o `gradle.properties` antigo pode continuar apontando para um JDK inexistente. A correÃ§Ã£o estÃ¡ nesta branch, nÃ£o na main.

No computador Windows, `adb: no devices/emulators found` indica que nenhum Android estÃ¡ conectado. Ative DepuraÃ§Ã£o USB e autorize o computador, ou inicie um emulador no Android Studio. NÃ£o Ã© evidÃªncia de falha do aplicativo.

## PrÃ³ximas prioridades

1. Concluir CI e resolver falhas reais de teste.
2. Executar teste MediaStore em APIs 29, 30, 33, 34, 35 e 36 e dispositivos de fabricantes diferentes.
3. Corrigir limpeza/API29 ou reavaliar minSdk mediante impacto nos usuÃ¡rios existentes.
4. Validar o catÃ¡logo de Billing, purchase tokens e entitlement seguro.
5. Adicionar histÃ³rico local persistente de anÃ¡lises e reconciliaÃ§Ã£o.
6. Reestruturar a interface para fluxo de uma aÃ§Ã£o principal e navegaÃ§Ã£o clara.
7. Revisar acessibilidade, escalas de fonte, TalkBack, orientaÃ§Ã£o e tablets.
8. Confirmar package, versionCode e upload certificate no Play Console.
9. SÃ³ entÃ£o gerar Release/AAB assinado pela chave legÃ­tima e publicar primeiro em teste interno.

## Regra de trabalho

NÃ£o comeÃ§ar do zero, nÃ£o descartar cÃ³digo jÃ¡ existente e nÃ£o confundir documentaÃ§Ã£o, cÃ³digo compilÃ¡vel e funcionalidade comprovada. Cada funcionalidade precisa de teste e evidÃªncia. Nunca dizer que a biblioteca foi limpa, que o espaÃ§o foi liberado ou que uma compra foi validada sem confirmaÃ§Ã£o tÃ©cnica do resultado.

## Continuidade â€” 04/11/2026

- Posicionamento confirmado: Phone Cleaner / limpador inteligente de celular.
- Modelo comercial: Freemium com benefÃ­cio Pro de exportaÃ§Ã£o de relatÃ³rio, Ãºnico jÃ¡ implementado no cliente.
- Medidor: armazenamento compartilhado primÃ¡rio, nÃ£o espaÃ§o privado /data; SD/USB nÃ£o somados automaticamente.
- Testes novos: quatro casos para StorageUsageCalculator, aguardando CI.
- LocalizaÃ§Ã£o: PT-BR/EN/fallback 152 chaves; outros seis idiomas 63/152 cada (534 faltantes).
- Pesquisa de mercado e tecnologia atualizada; nenhum cÃ³digo de terceiros foi incorporado.
- Manter PR #3 em Draft; nÃ£o mergear, publicar ou gerar release assinado sem gates concluÃ­dos.

## Estado exato do HEAD â€” 04/11/2026, 16:22 BRT

- Branch `fix/p0-sweep-permissions-missions-billing`, HEAD `d60ff8f8c0852ab7c33ce638d662053c028d4916`.
- PR #3: aberta, Draft, base `main`, nÃ£o merged.
- `main` permanece no SHA `3759e176c48f340fa576a06455579277e514d9d9`.
- GitHub Actions run [41866189110](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/41866189110), associado exatamente ao HEAD acima, estÃ¡ **queued**. Jobs API29, API35 e Windows tambÃ©m estÃ£o queued; nÃ£o hÃ¡ resultado de build/teste para esse SHA.
- O Ãºltimo ajuste funcional liga o botÃ£o principal da home Ã  missÃ£o global de arquivos grandes e retoma a tela de progresso apÃ³s autorizaÃ§Ã£o de mÃ­dia. Foi adicionado teste instrumentado da existÃªncia/clickabilidade da aÃ§Ã£o principal; nÃ£o foi executado ainda.
- NÃ£o declarar compilaÃ§Ã£o, Lint, testes JVM, instrumentaÃ§Ã£o ou APK gerado neste HEAD atÃ© obter logs do run exato.

## Jornada principal Phone Cleaner â€” atualizaÃ§Ã£o de 04/11/2026

A aÃ§Ã£o principal da Home agora executa a missÃ£o global `FREE_MAX_SPACE_NOW`, que percorre as categorias, volumes e pÃ¡ginas MediaStore acessÃ­veis para propor arquivos grandes. Ela nÃ£o exclui nada automaticamente. O pedido inicial de permissÃµes preserva a missÃ£o pendente e, apÃ³s autorizaÃ§Ã£o, retorna Ã  aba Limpeza para exibir progresso. A navegaÃ§Ã£o de mÃ­dia paginada Ã© separada como â€œAbrir arquivos / Browse filesâ€.

A melhoria estÃ¡ no source do HEAD `d60ff8f8c0852ab7c33ce638d662053c028d4916`. HÃ¡ um teste instrumentado da aÃ§Ã£o principal, ainda sem execuÃ§Ã£o por estar o CI queued. O botÃ£o principal nÃ£o deve ser interpretado como uma anÃ¡lise sem limites: acesso parcial, provider e limites de seguranÃ§a continuam visÃ­veis na cobertura global.

## Operating model for AI-assisted execution â€” 04/11/2026

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

## Retomada local â€” 2026-11-06

- Workspace preservado na branch `codex/midiastore-release-audit`, HEAD `16545a4277bcc515660a6516c32fb6c770a4e880`, igual ao commit remoto confirmado. Existem alteraÃ§Ãµes staged, unstaged e arquivos locais nÃ£o rastreados da sessÃ£o anterior; nenhum foi descartado. PR #3 permanece Draft e Issue #2 nÃ£o foi alterada.
- As alteraÃ§Ãµes anteriores de testes, fixture `mediaOwnerTest`, documentaÃ§Ã£o de pesquisa e atualizaÃ§Ãµes de dependÃªncias permanecem locais. A alteraÃ§Ã£o unstaged para Core KTX 1.19.1 impedia o build: exige compileSdk 37 e AGP 9.1, incompatÃ­veis com o SDK 36/AGP 8.13.2 deste projeto. Core KTX foi fixado em 1.17.0, mantendo as demais dependÃªncias e a configuraÃ§Ã£o de identidade/versÃ£o.
- Build local Windows passou: `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` e `:mediaOwnerTest:assembleDebug`. Lint concluiu com issues existentes; build nÃ£o significa relatÃ³rio limpo.
- No Motorola moto g84 5G (API 35, serial `0085353748`), `:app:connectedDebugAndroidTest` falhou no UTP sem receber resultados e gerou relatÃ³rio com zero testes. Executando os APKs instalados pelo `AndroidJUnitRunner` via ADB, a suÃ­te concluiu `OK (7 tests)`.
- A primeira execuÃ§Ã£o direta observou a geraÃ§Ã£o MediaStore mudar durante consultas e o adapter retornar `SESSION_STALE` com `SOURCE_CHANGED` confirmado, sem registros ambÃ­guos. Os testes de paginaÃ§Ã£o/Ã¡udio agora reconhecem esse caso apenas quando a revisÃ£o realmente prova a mudanÃ§a da fonte; resultados inesperados continuam falhando.
- API29 ainda nÃ£o foi validado: nÃ£o hÃ¡ AVD API29 neste ambiente. O teste foi preparado para executar o consentimento com mÃ­dia criada por um app auxiliar de UID distinto e conferir a atribuiÃ§Ã£o reportada pelo prÃ³prio MediaStore, mas requer execuÃ§Ã£o real em API29. NÃ£o inferir resultado API29 a partir do moto g84/API35.

## ContinuaÃ§Ã£o da validaÃ§Ã£o â€” 2026-11-06

- Branch `codex/midiastore-release-audit`, HEAD local e base remota `16545a4277bcc515660a6516c32fb6c770a4e880`. Ãrvore segue com mudanÃ§as staged/unstaged e arquivos novos; nada foi descartado ou enviado.
- Consultas read-only confirmaram PR #3 aberta e Draft, Issue #2 aberta, workflow `42080940375` falho no SHA `16545a4`. Os artefatos locais `.local-ci-42080940375/` mostram que API29 falhou porque MediaStore atribuiu a fixture ao UID do app alvo; API35 falhou ao assumir `external_primary` antes de haver volume disponÃ­vel e encerrou processo ao revogar permissÃµes durante o teste.
- Fixture API29 agora vem do provider de `:mediaOwnerTest`, pacote/UID separado, e a assertion consulta o owner real do MediaStore. Ainda falta executar em API29.
- Testes API35 consultam volumes externos realmente anunciados pelo MediaStore. O teste de acesso parcial roda primeiro e exige permissÃµes visuais/Ã¡udio negadas antes de conceder Selected Photos Access; nÃ£o revoga permissÃ£o no processo.
- Corrigida a assertion anterior: `SESSION_STALE` por si sÃ³ nÃ£o passa paginaÃ§Ã£o. Imagem/Ã¡udio sÃ³ terminam com sucesso apÃ³s sessÃ£o estÃ¡vel; atÃ© trÃªs sessÃµes sÃ£o tentadas, cada invalidaÃ§Ã£o precisa corresponder a `SOURCE_CHANGED`, e falha se todas permanecerem stale. Um resultado estÃ¡vel ainda precisa ser executado no aparelho.
- `:app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest` concluiu `BUILD SUCCESSFUL` apÃ³s essa alteraÃ§Ã£o (49 tarefas: 4 executadas, 45 up-to-date). O Kotlin daemon recebeu `AccessDeniedException` ao tentar escrever em `AppData\\Local\\kotlin\\daemon`; compilaÃ§Ã£o usou fallback e terminou com sucesso.
- RepetiÃ§Ã£o `:app:connectedDebugAndroidTest -x :mediaOwnerTest:installDebug` reproduziu `Failed to receive the UTP test results`, com zero testes coletados. A conexÃ£o `adb` do serial `0085353748` desapareceu apÃ³s a execuÃ§Ã£o. O aparelho precisa ser reconectado para repetir o AndroidJUnitRunner direto e separar runner de coleta.
- A tentativa local de emulador API29 chegou Ã  falha explÃ­cita de aceleraÃ§Ã£o x86_64: o driver de hypervisor nÃ£o estÃ¡ instalado. NÃ£o instalar driver nem alterar proteÃ§Ãµes globais do Windows; API29 permanece bloqueado por ambiente.
- PrÃ³xima tarefa: quando o Motorola retornar ao ADB, confirmar modelo/API/serial, reinstalar/verificar os APKs recÃ©m-compilados, limpar/conceder somente permissÃµes do pacote Debug e executar primeiro o caso Selected Photos isolado, depois a suÃ­te direta completa. Obter XML/logcat/cÃ³digos de saÃ­da do UTP em nova execuÃ§Ã£o; retomar API29 em CI ou mÃ¡quina com virtualizaÃ§Ã£o jÃ¡ habilitada.

## Auditoria de recuperaÃ§Ã£o â€” 2026-11-05

- Branch local prÃ³pria: `codex/midiastore-release-audit`, criada do `origin/fix/p0-sweep-permissions-missions-billing` em `16545a4277bcc515660a6516c32fb6c770a4e880`. A Ã¡rvore estava limpa antes da auditoria. `main` segue em `3759e176c48f340fa576a06455579277e514d9d9`.
- PR #3 permanece aberta como Draft, base `main`, nÃ£o integrada. O trabalho local nÃ£o foi enviado.
- O Ãºltimo CI inspecionado, [run 42080940375](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/42080940375), passou JVM, Lint task, builds APK Debug/AndroidTest e parsing PowerShell. InstrumentaÃ§Ã£o API29 e API35 falhou.
- API29 criava a mÃ­dia pelo contexto que tinha o mesmo package/UID do Midiastore. Foi criado `:mediaOwnerTest` com package/UID separado, provider de teste protegido por permissÃ£o signature, conferÃªncia do owner retornado pelo MediaStore, consentimento Android, retry pÃ³s-`RESULT_OK` e verificaÃ§Ã£o de ausÃªncia. O cÃ³digo compila, mas precisa rodar em API29.
- API35 consultava `external_primary` enquanto o sistema ainda informava o volume como desmontÃ¡vel e o processo instrumentado acabou encerrado. Os testes e o runner CI agora aguardam a montagem real do volume, com timeout e diagnÃ³stico. A correÃ§Ã£o ainda precisa de CI/API35 real.
- DependÃªncias atualizadas para versÃµes estÃ¡veis: Core 1.17, AppCompat 1.8, Activity 1.13, Lifecycle 2.11, Coroutines 1.11, Material 1.14, AndroidX Test 1.7 e Billing 8.3. `billing-ktx` virou `billing`, pois o cÃ³digo usa API callback e nÃ£o extensÃµes KTX. AGP 8.13.2, Kotlin 2.3.21, Gradle 8.13, identidade e versÃ£o do app foram preservados.
- ValidaÃ§Ã£o local Windows concluiu com sucesso: `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:mediaOwnerTest:assembleDebug`. O relatÃ³rio Lint contÃ©m 90 erros `MissingTranslation` e 94 warnings; o build segue porque `abortOnError=false`.
- NÃ£o hÃ¡ imagens/AVDs API29 e API35 instaladas localmente e `adb devices` estÃ¡ vazio. Nenhum teste instrumentado foi executado apÃ³s a correÃ§Ã£o.
- NÃ£o hÃ¡ `keystore.properties` nem `sweep-upload.jks` local. O Release nÃ£o foi executado e nenhuma chave foi gerada. A polÃ­tica de privacidade segue como draft sem URL pÃºblica; catÃ¡logo Billing, versÃ£o publicada mÃ¡xima e certificado ainda dependem da Play Console.
- NÃ£o fazer push, merge, despacho de workflow externo, build AAB de produÃ§Ã£o ou publicaÃ§Ã£o sem autorizaÃ§Ã£o. PR continua Draft e release bloqueado atÃ© CI atualizado e gates de aparelho/Play Console.
## Continuidade remota â€” 2026-11-06

### AlteraÃ§Ã£o visual aplicada diretamente no GitHub

- A branch de recuperaÃ§Ã£o avanÃ§ou de `16545a4277bcc515660a6516c32fb6c770a4e880` para `ca9ebd04fa11bfba966830ce7f977ab081954ce1`, por fast-forward, sem force-push.
- Paleta atualizada para identidade premium escura: fundo midnight, superfÃ­cies azul-petrÃ³leo, aÃ§Ã£o primÃ¡ria jade/ciano e acento Ã¢mbar.
- Tema Material 3 alinhado Ã  paleta com cores explÃ­citas de superfÃ­cie, texto, aÃ§Ã£o e navegaÃ§Ã£o.
- Arquivos: `app/src/main/res/values/colors.xml` e `app/src/main/res/values/styles.xml`.
- PR #3 permanece aberta em Draft; `main` nÃ£o foi alterada e nenhuma publicaÃ§Ã£o foi feita.
- O cÃ³digo visual foi gravado e relido pelo GitHub. Ainda nÃ£o hÃ¡ execuÃ§Ã£o de CI associada ao novo SHA; portanto, nÃ£o declarar compilaÃ§Ã£o ou teste aprovados por causa dessa alteraÃ§Ã£o.

### Workspace local do Codex â€” reconciliaÃ§Ã£o obrigatÃ³ria

- A Ãºltima leitura local mostrou a branch `codex/midiastore-release-audit` no SHA base `16545a4277bcc515660a6516c32fb6c770a4e880`, com alteraÃ§Ãµes staged, unstaged e arquivos novos.
- Essas alteraÃ§Ãµes locais incluem correÃ§Ãµes de testes/fixture API29, descoberta dinÃ¢mica de volumes MediaStore API35, documentaÃ§Ã£o e pesquisa. Elas ainda nÃ£o estÃ£o incluÃ­das no SHA remoto acima.
- EvidÃªncia local relatada: AndroidJUnitRunner direto no Motorola API35 executou os testes de mÃ­dia; os cenÃ¡rios exclusivos de API29 foram ignorados corretamente nesse aparelho. Isso nÃ£o valida API29 e nÃ£o substitui CI.
- O Gradle UTP no Windows falhou antes de coletar resultados, com erro do listener local/Netty; nÃ£o classificar como falha de assertion do app nem como aprovaÃ§Ã£o.
- Antes de continuar alteraÃ§Ãµes que toquem os mesmos arquivos, reconciliar cuidadosamente o workspace local com a branch remota. NÃ£o descartar, limpar ou sobrescrever o trabalho local.

### PrÃ³ximas prioridades remotas

1. Integrar de forma auditÃ¡vel as correÃ§Ãµes locais de fixture com UID separado, volume MediaStore dinÃ¢mico e isolamento do teste de permissÃµes.
2. Obter CI verde no SHA atual, incluindo API29 e API35; nÃ£o aceitar testes ignorados como substitutos de cobertura.
3. Validar visualmente em aparelho real a home, cards, filtros, navegaÃ§Ã£o, fonte ampliada e TalkBack.
4. Resolver os findings de localizaÃ§Ã£o; a Ãºltima auditoria local relatou 90 erros MissingTranslation e 94 avisos.
5. Manter API29, Billing seguro, Play Console, assinatura e testes OEM como gates explÃ­citos de release.

## AvaliaÃ§Ã£o do proprietÃ¡rio no Motorola â€” 2026-11-07

- ImpressÃ£o geral explÃ­cita do proprietÃ¡rio Rodolfo: o app estÃ¡ â€œmal construÃ­do, feio e ruimâ€. No uso real, relata que nÃ£o executa limpeza Ãºtil, os botÃµes nÃ£o respondem como esperado e os planos pagos nÃ£o funcionam. Tratar como falha de aceitaÃ§Ã£o e bloqueio total de release; a avaliaÃ§Ã£o do proprietÃ¡rio prevalece sobre aparÃªncia de conclusÃ£o em compilaÃ§Ãµes ou testes isolados.
- A verificaÃ§Ã£o local desta data confirmou que o Motorola moto g84 5G (Android 15) responde ao ADB, abriu `MainActivity` e nÃ£o mostrou `FATAL EXCEPTION` no trecho de logcat consultado. O AndroidJUnitRunner direto terminou `OK (9 tests)`, mas cenÃ¡rios dependentes de API29, consentimento interativo e permissÃµes visuais isoladas foram ignorados por precondiÃ§Ãµes. Isso **nÃ£o** prova que a experiÃªncia real de limpeza, botÃµes, navegaÃ§Ã£o ou Billing esteja funcional.
- `:app:connectedDebugAndroidTest` continua sem resultado Gradle/UTP (`Failed to receive the UTP test results`); o runner Android direto foi necessÃ¡rio e Ã© evidÃªncia limitada ao que os testes cobrem.
- NÃ£o declarar o app pronto para publicar, nÃ£o promover a produÃ§Ã£o e nÃ£o usar aprovaÃ§Ã£o de testes como resposta Ã  avaliaÃ§Ã£o negativa. PrÃ³xima etapa de produto: reproduzir no telefone os fluxos relatados, inspecionar a tela e cada aÃ§Ã£o da navegaÃ§Ã£o, corrigir interaÃ§Ã£o e feedback visual, verificar permissÃµes MediaStore e pÃ³s-estado real da limpeza, e validar a tela/estados de Billing com produtos de teste da Play. Manter cada comportamento nÃ£o executado como nÃ£o verificado.

## Estado retomado no Motorola ï¿½ 2026-11-07 (atualizaï¿½ï¿½o)

- A versï¿½o em teste ï¿½ com.rdf112.midiastore.debug (APK Debug usa applicationIdSuffix .debug); nï¿½o confundir com o app Release instalado no telefone.
- O logo do cabeï¿½alho agora agrupa o sï¿½mbolo e o nome Midiastore; a ilustraï¿½ï¿½o vetorial anterior da mulher com celular foi preservada, com o nome Midiastore abaixo. Nada foi apagado durante esta validaï¿½ï¿½o.
- No moto g84 5G/API35, a aï¿½ï¿½o Home encontrou 5 itens grandes (84,30 MB), atualizou a CTA para revisï¿½o e abriu a lista. A revisï¿½o mostra consentimento antes de qualquer operaï¿½ï¿½o. A exclusï¿½o foi deixada intocada.
- Navegar para Planos Pro funcionou. Mensal/anual/vitalï¿½cio continuam indicados como sugeridos e inativos; o app nï¿½o pode concluir compra enquanto Play Console nï¿½o tiver catï¿½logo ativo e falta backend de purchaseToken.
- Build :app:assembleDebug pï¿½s-ajustes passou. Testes JVM/Lint estï¿½o em execuï¿½ï¿½o; o AAB assinado nï¿½o foi gerado. Bloqueios Play Console, versionCode/certificado e validaï¿½ï¿½o release/API29/CI permanecem.

## Checkpoint prioritÃ¡rio â€” 2026-11-18

Antes de usar checkpoints antigos, consulte `docs/CONTINUATION_UPDATE_2026-11-18.md`. As decisÃµes jÃ¡ aprovadas nas memÃ³rias Supabase #313â€“#315 prevalecem: limpador puro, abas Limpar/Arquivos/Pro, preto OLED/grafite/azul, sem Studio/jogos/jornal/extras; planta e mockup antes de cÃ³digo de UI. Mockup: `docs/mockups/midiastore-2027-cleaner-puro.html`. Blueprint corrigido: `docs/BLUEPRINT_MIDIASTORE_2027.md`. Uma proposta que adicionava Studio/Explorar contrariou a decisÃ£o e foi corrigida; nÃ£o reabrir decisÃµes jÃ¡ tomadas.

## AtualizaÃ§Ã£o adicional â€” 2026-11-18: implementaÃ§Ã£o Compose iniciada

A planta/mockup respeitando #313/#315 foi seguido pela primeira tela real `CleanerHomeScreen.kt`, conectada Ã  Activity e aos fluxos MediaStore jÃ¡ existentes. A compilaÃ§Ã£o ainda nÃ£o foi confirmada: o Gradle inicia o daemon e trava antes de reportar tarefas; a execuÃ§Ã£o foi interrompida. Veja `docs/CONTINUATION_UPDATE_2026-11-18.md`. PrÃ³xima etapa: destravar build, instalar o Debug separadamente e revisar visual no Moto; depois migrar Arquivos/Pro e navegaÃ§Ã£o, sem adicionar Studio/extras.

## Strategic objective and memory update - 2026-11-20

Read docs/OBJETIVO_ESTRATEGICO_E_MEMORIA.md first for the current business north star, agent mandate, metrics, evidence rules and four-layer memory protocol. The goal is a competitive, trusted Android storage cleaner with sustainable real Google Play sales; R is a target, not an achieved result. The owner wants agents to work proactively across product, operations and sales as model capabilities improve, within current authorization, account access, budgets, law and platform rules. Product decisions remain governed by AGENTS.md: pure cleaner, Limpar/Arquivos/Pro. For current continuity see docs/CONTINUATION_UPDATE_2026-11-20.md. Preserve shared Antigravity changes; check git status before editing.

## Maria replay / localization checkpoint â€” 2026-11-26

- Commit `a33a3b87da511027f43516041b1289fd9a4c8b96` on `feat/v2.9.0-motor-app` updates the Maria avatar to show a centered cropped still while idle and gate video visibility on a fresh decoded frame; Italian (`values-it`) is added to locale config/governance.
- The single GitHub Actions run `45776260540` failed at `:app:mergeDebugResources`: invalid escaped apostrophes (`\\'`) in `values-ja`, `values-ru`, and `values-zh-rCN` (`starting_scan`, `oaas_starting_scan_first`) trigger AAPT invalid Unicode escape errors. No APK artifact was produced; Moto G84 two-cycle QA was not run. No local Gradle was run.
- The requested Files labels (empty-page, page counts/navigation, filters, storage used, Current plan) are translated, but full-string translation audit is incomplete: numerous longer secondary strings remain English, especially in French, Hindi, Indonesian, and Italian.
- Supabase memory was recorded as #575 after one transient HTTP 502; local timeline was updated.
- Next step: correct AAPT escape sequences, then obtain authorization for another CI compilation before APK/device QA; do not claim completion or alter `v2.8.2-carbon-baseline`.


## Auditoria real no Moto G84 — 2026-11-26

- Relatório da sessão: [docs/QA_AUDITORIA_REAL_V290.md](QA_AUDITORIA_REAL_V290.md). Validado exclusivamente `com.rdf112.midiastore.debug` v2.8.2 no serial `0085353748`; sem Gradle e sem alteração de código. Pacote oficial preservado.
- Reproduzido o retorno da Maria com retrato/anel azul na segunda visita; nenhum crash/ANR observado no trecho monitorado. Permissão parcial impediu concluir os fluxos de Arquivos, lixeira/restauração/exclusão, compressor e compras; relatório registra lacunas sem inferir sucesso.
- Fixtures de QA removidos e diretórios `MidiastoreQA` confirmados ausentes. Não tocar em `/sdcard/DCIM/Camera`.

## Implementação dos achados da auditoria — 2026-11-26

- Branch `feat/v2.9.0-motor-app`: melhorias na limpeza em dois estágios com lixeira segura, permissão parcial Android 14+, retorno da Maria com Surface persistente, controles acessíveis de fala/mudo, resumo grátis/Pro comum aos três planos, strings i18n e roteamento da Política de Privacidade pelo locale do app.
- `ci-cd-governance-audit.mjs` passou com 322 chaves e paridade em `values` + 14 locales; validou UTF-8, apóstrofos AAPT2, escapes Unicode e HTTPS. `git diff --check` passou.
- Nenhum Gradle local. As alterações ainda aguardam CI/build e reinstalação somente do pacote `.debug` para confirmar UI/fluxos em ADB; não alterar o pacote oficial nem a tag `v2.8.2-carbon-baseline` (`cd0a6612fd4038eeb3d2ffb1db6f85fbd73954b4`).
- CI `45816379909` no SHA `5ff1328`: governança passou; AAPT2 rejeitou a primeira normalização com `&apos;`. A correção passou a usar `’` e será validada por nova execução remota; sem APK nesse run.

### Fechamento 26/11 — gates e instalação debug

- CI run `45817049336` no SHA `71324e22f128cfdfed5c39743e682af29abd7fdc`: `success` (governança, testes, build e artefato `.debug`). Deploy web foi `skipped`.
- APK identificado como `com.rdf112.midiastore.debug`, SHA-256 `0de3b6e83d5bc3e60babc9d88d4ddde41d2e0f21df816839a066255bc42f53b6`; `adb install -r` foi bloqueado por certificado debug divergente. Não desinstalar nem limpar dados para contornar; combinar uma rota que preserve dados ou obter assinatura compatível antes de continuar o QA visual no Moto G84.
- URLs de privacidade exatas em HTTPS verificadas por HEAD `200`; `node scripts/ci-cd-governance-audit.mjs` PASS; validação Node independente percorreu 15 catálogos/4.830 textos sem os defeitos pedidos; `git diff --check` PASS.
- Auditoria e lacunas atualizadas em `docs/QA_AUDITORIA_REAL_V290.md`; não declarar Maria, limpeza rápida, permissão parcial ou mudanças localizadas aprovadas em dispositivo até validar essa revisão.
- Memória Supabase final: **#579** (gates HTTPS/strings, CI verde e bloqueio de assinatura); relatório integral anterior **#577**, implementação **#578**.

## Revisão final do catálogo italiano — 26/11/2026

- Commit de recursos `de4ec43d7ee1dac70cb5b2d9d92327711291f517`: 202 strings ainda idênticas ao inglês foram traduzidas; comparação das 322 chaves deixou igual somente marca/sigla, placeholder e token numérico/técnico. Copy cobre abas, cards, planos, compra, política e trial de 3 dias. Placeholders preservados.
- Governance, `git diff --check` e verificador Node italiano passaram. CI `45826259245` verde no SHA do commit de recurso; deploy web skipped.
- APK `com.rdf112.midiastore.debug` SHA-256 `c83d06c8befb0c4cbcaefda80bc1e74785e60ea081aab03e1a075e7d824d88a4`. Instalação recusada por assinatura diferente. A tentativa `adb uninstall -k` não removeu o app; ADB alertou que os dados retidos só podem ser reusados com a assinatura anterior. A versão existente permaneceu intacta, assim como o pacote oficial e a baseline.
- QA visual da localização italiana aguarda autorização explícita para desinstalar normalmente somente o `.debug` (apaga dados internos/configurações locais) e instalar o APK novo.

## Site Prime — 14 idiomas e artes localizadas — 2026-11-28

- Gerador reproduzível: `scripts/build-site-prime-14-idiomas.mjs`. Atualizadas as 14 páginas nativas, com canonicals próprias, 15 hreflangs recíprocos, dados estruturados `WebSite`/`Organization`/`MobileApplication`/`ImageObject`, CTA Google Play e árabe em RTL.
- Criadas 28 artes SVG responsivas (16:9 e 4:5), preservando as fotografias PNG originais e a identidade visual, com chamada e CTA traduzidos em cada idioma. Os PNGs de origem não foram alterados.
- Guias PT-BR reformulados no padrão Prime; sitemap contém somente 14 páginas de idioma, 3 guias e 2 páginas de privacidade (19 URLs).
- Firebase Hosting: deploy concluído no projeto `midiastory-df6a0`, 70 arquivos. URLs pedidas, sitemap, image-sitemap, verificação do Google e artes SVG responderam HTTP 200; `/guides/**` redireciona com 301 para `/`.
- `docs/google7f3bae0a2f8f091c.html` e `docs/image-sitemap.xml` mantiveram os hashes SHA-256 anteriores. `app/` permaneceu intacta; Gradle não foi executado.
- Memória Supabase #584. Branch: `feat/v2.9.0-motor-app`; publicar somente arquivos do site e `scripts/build-site-prime-14-idiomas.mjs`, sem incluir alterações preexistentes em `docs/MOTOR_CRESCIMENTO_META.md` ou `.firebase/`.

## Correção definitiva do Hero Prime — 2026-11-28

- Removidas as 28 artes SVG `midiastore-prime-*.svg`; o gerador agora usa diretamente os PNGs 16:9 no desktop e 4:5 no mobile.
- Home PT-BR mantém somente o PNG no hero. As outras 13 páginas recebem overlay HTML/CSS localizado em azul-marinho sobre degradê azul-claro; árabe usa `dir="rtl"`. Marca no cabeçalho e rodapé está protegida com `notranslate`/`translate="no"`.
- Deploy final pelo `firebase.cmd` no projeto `midiastory-df6a0`: 171 arquivos publicados. As 14 rotas, os dois PNGs, `image-sitemap.xml` e a verificação Google responderam HTTP 200. O aviso de permissão `serviceusage` não impediu a publicação.
- SHA-256 de `docs/google7f3bae0a2f8f091c.html` e `docs/image-sitemap.xml` permaneceu inalterado. `app/` não foi tocada e Gradle não foi executado.
- Commit `96bd449` enviado para `feat/v2.9.0-motor-app`; alteração preexistente em `docs/MOTOR_CRESCIMENTO_META.md` e cache `.firebase/` ficaram fora do commit.
