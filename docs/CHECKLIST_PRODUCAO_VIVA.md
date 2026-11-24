# 📋 CHECKLIST VIVA DE PRIORIDADES — MIDIASTORE (ÚNICA PAREDE ATIVA)
> 🧭 **Índice Central:** [**`docs/INDICE_MESTRE.md`**](INDICE_MESTRE.md) · [**`AGENTS.md`**](../AGENTS.md) · [**`CONTINUATION_HANDOFF.md`**](CONTINUATION_HANDOFF.md) · [**`CHECKLIST_EVOLUCAO_CARBON.md`**](CHECKLIST_EVOLUCAO_CARBON.md) · [**`PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`**](PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md) · [**`LINHA_DO_TEMPO_AUTONOMA.md`**](LINHA_DO_TEMPO_AUTONOMA.md)

**Última Verificação Ao Vivo:** `2026-11-24 07:45 BRT` (**Dia 12/14** do Teste Fechado • Faltam **2 dias** para Produção: **quinta-feira 26/11**)  
**Objetivo Único:** Publicar o Midiastore (`com.rdf112.midiastore`) em **Produção na Google Play Store** com padrão premium (nível Apple/MacPaw) para gerar caixa recorrente rumo a **R$ 1.000.000,00** (Regra 50/50 — Pilar 1), mantendo a evolução da IA Central FRIDAY e Maria 2.0 (Pilar 2 — roteiro completo em [`docs/CHECKLIST_EVOLUCAO_CARBON.md`](CHECKLIST_EVOLUCAO_CARBON.md)).

---

## 🟢 O QUE ESTÁ 100% VERIFICADO AO VIVO HOJE (`2026-11-24`)
- [x] **Versão Final Candidata `v2.8.2 (vc 34)` compilada, assinada e validada no Moto G84 (`0085353748`):**
  - Tag de segurança travada no Git: **`v2.8.2-carbon-baseline`** (`cd0a661` / HEAD `6fd8020`, CI `Run 45342357954`, `45342358085` e `45343870292` **100% verdes**).
  - **AAB Release Assinado (`.tmp/qa/v282-carbon-aab/app-release.aab`):** **`7,26 MB`** (`7.263.824 bytes`, SHA-256 `5151aa51ac753efb78427d90591d6faaa13d7bdc16038642cda2704f017634f1`), auditado via `bundletool` (`com.rdf112.midiastore`, `vc 34`, `v2.8.2`, **zero `MANAGE_EXTERNAL_STORAGE`**, **zero `AD_ID`**, **zero `ACTION_SEND`**, `631/631` chaves em 14 idiomas).
  - **QA Físico Real:** validado em retrato (`PT-BR` e `EN-US`) nas 3 abas (`Limpar`, `Arquivos`, `Pro`), zero erros no `logcat`.
- [x] **Google Play Console Store Presence 14 Idiomas (`Edit 15816757970827593747` -> `HTTP 200`):**
  - `internal` e `alpha` intactos em `v2.8.0 (vc 32) [completed]` com **23 testadores fechados 100% preservados**; 3 SKUs Billing (`sweep_pro_monthly`, `sweep_pro_annual`, `sweep_pro_lifetime`) `ACTIVE`.
  - **14/14 fichas sincronizadas ao vivo:** textos `< 8 MB`, `bn-BD` sem caracteres cirílicos, **56 novos `phoneScreenshots` `1080x1920`** da `v2.8.2` (zero fotos pessoais, zero telas vazias repetidas), **14 `featureGraphic` `1024x500`** (cobrindo os 6 idiomas antes zerados) e **14 ícones `512x512`**.
- [x] **Políticas de Privacidade + Formulário de Produção (`HTTP 200`):**
  - `/privacidade.html` e `/privacy.html` sincronizadas e publicadas no Firebase Hosting (`Release 1795515742674000`, zero menção a Estúdio, contato `srodolfobarbosa@gmail.com` visível).
  - Respostas de 300 chars e Data Safety prontas em [`docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`](PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md).
- [x] **Backend Supabase (`20,29 MB / 500 MB`):**
  - Relógio dinâmico corrigido em `public.play_console_sync_state` + `get_prelaunch_stats()` (`23 testadores`, `Dia 12 de 14` em BRT).
  - 5 Edge Functions `ACTIVE` (`verify-play-purchase` v9, `play-rtdn-webhook` v5, `get-paywall-variant` v3, `ingest-product-event` v4, `native-orchestrator` v6); `play_purchases = 1` venda testada; `product_events` com `44,1%` CTR.
- [x] **Máquina de Aquisição (`@aconteceunobr` + Google Ads + Reels/Shorts):**
  - Tokens Meta Graph API v27.0 permanentes validados (`8` Reels em alta prontos em `--dry-run` para `26/11`) + lote `google-ads-editor-lote-14-idiomas.zip` pronto.

---

## 🔴 FILA ÚNICA DE EXECUÇÃO (ORDEM ESTRATÉGICA)

1. **[QUINTA-FEIRA 26/11 — PORTÃO DE CAIXA] Clicar em "Solicitar a produção" na Google Play Console:** colar as respostas auditadas de [`docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`](PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md), confirmar declarações e promover `v2.8.2 (vc 34)` (ou aprovar com `v2.8.0 vc 32` e subir `v2.8.2` em seguida, preservando zero risco).
2. **[PÓS-APROVAÇÃO DE PRODUÇÃO — DIA 1 DE VENDAS] Ligar Aquisição Orgânica + Paga (Regra 50/50):**
   - Ativar comentários/Stories no `@aconteceunobr` + publicar Reels/Shorts globais e importar lote no Google Ads Editor (`649-729-9513`), validando a 1ª compra real de Produção em `public.play_purchases`.
3. **[EM BRANCH ISOLADA — ZERO RISCO À `v2.8.2`] Executar Roadmap `v2.9.0` + Maria 2.0 ([`docs/CHECKLIST_EVOLUCAO_CARBON.md`](CHECKLIST_EVOLUCAO_CARBON.md)):**
   - Fotos Parecidas IA (`dHash/pHash`), Cache SQLite Instantâneo, Compressor de Vídeo H.265 Pro, modularização da `MainActivity.kt` e **Laboratório Maria Humana de Última Geração** (**PROIBIDO 3D/boneco**; Maria atual 100% intocada até protótipo humano fotorrealista aprovado pelo Rodolfo — `Diretriz 12`).
