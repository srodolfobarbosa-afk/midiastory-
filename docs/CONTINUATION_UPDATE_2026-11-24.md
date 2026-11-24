# Snapshot de Continuidade & Blindagem 360° Pré-Produção — 2026-11-24 (`v2.8.2` • `versionCode 34`)

## 1. Baseline Imutável & Protocolo Catraca Carbon
- **Tag Git Travada:** `v2.8.2-carbon-baseline` (`cd0a661` / HEAD `6fd8020`, CI `Run 45342357954`, `45342358085` e `45343870292` 100% verdes).
- **AAB Release Assinado Final (`.tmp/qa/v282-carbon-aab/app-release.aab`):** `7.263.824 bytes` (`7,26 MB`), SHA-256 `5151aa51ac753efb78427d90591d6faaa13d7bdc16038642cda2704f017634f1`, auditado via `bundletool` (`com.rdf112.midiastore`, `vc 34`, `v2.8.2`, zero `MANAGE_EXTERNAL_STORAGE`, zero `AD_ID`, zero `ACTION_SEND`, `631/631` chaves em 14 idiomas).
- **QA Físico no Moto G84 5G (`0085353748`):** validado em retrato (`PT-BR` e `EN-US`) nas 3 abas (`Limpar`, `Arquivos`, `Pro`), zero crashes no `logcat`.
- **Roadmap Completo Pós-Produção (`v2.9.0` + Maria 2.0):** consolidado em [`docs/CHECKLIST_EVOLUCAO_CARBON.md`](CHECKLIST_EVOLUCAO_CARBON.md).

## 2. Entregas & Correções Ao Vivo Concluídas Hoje (`2026-11-24`)
1. **Google Play Console Store Presence 14 Idiomas (`Edit 15816757970827593747` -> `HTTP 200`):**
   - Gerados e publicados **56 novos `phoneScreenshots` (`1080x1920`)** da `v2.8.2` (zero fotos pessoais, zero telas vazias repetidas, removido claim antigo de exportação) + **14 `featureGraphic` (`1024x500`)** preenchendo os 6 idiomas antes zerados (`pt-PT`, `ar`, `ru-RU`, `id`, `bn-BD`, `ko-KR`).
   - Textos `STORE_LISTINGS_14` atualizados para `< 8 MB` e corrigido texto misto cirílico em `bn-BD` (`সহজেই`). Faixas `internal`/`alpha` (`v2.8.0 vc 32`) e os **23 testadores fechados** permaneceram 100% intocados.
2. **Supabase (`usmqtnlsaavxqguwrtix` — `20,29 MB / 500 MB`):**
   - Corrigido relógio congelado em `public.play_console_sync_state` + `public.get_prelaunch_stats()`: agora retorna dinamicamente em `America/Sao_Paulo` **`official_testers_count = 23`** e **`closed_test_day = 12`** (`13/14` em `25/11` e `14/14` em `26/11`).
   - Todas as 5 Edge Functions `ACTIVE` (`verify-play-purchase` v9, `play-rtdn-webhook` v5, `get-paywall-variant` v3, `ingest-product-event` v4, `native-orchestrator` v6).
3. **Políticas de Privacidade no Firebase Hosting (`Release 1795515742674000`):**
   - Reescrita `https://midiastory-df6a0.web.app/privacidade.html` (removendo menção antiga ao Estúdio de 14/11 e adicionando e-mail `srodolfobarbosa@gmail.com`) e sincronizada com `https://midiastory-df6a0.web.app/privacy.html` (`HTTP 200`).
4. **Motores Orgânicos `@aconteceunobr` (`27.840` seguidores):**
   - Tokens Meta Graph API v27.0 validados (`PERMANENTE`), `watch-viral --dry-run` operacional (`8` Reels em alta prontos para `26/11`) e `product_events` com `44,1%` de CTR para a Play Store.
