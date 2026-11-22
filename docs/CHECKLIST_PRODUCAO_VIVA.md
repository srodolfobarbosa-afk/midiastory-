# 📋 CHECKLIST VIVA DE PRIORIDADES — MIDIASTORE (ÚNICA PAREDE ATIVA)

**Última Verificação Ao Vivo:** `2026-11-21 23:50 BRT`  
**Objetivo Único:** Publicar o Midiastore (`com.rdf112.midiastore`) em **Produção na Google Play Store** com qualidade impecável para gerar vendas reais rumo a **R$ 1.000.000,00** (Regra 50/50).

---

## 🟢 O QUE JÁ ESTÁ VERIFICADO E PRONTO AO VIVO HOJE
- [x] **App `v2.7.6 (vc 31)` publicado em `internal` e `alpha` (`completed`)** — confirmado via Google Play Developer API v3.
- [x] **GitHub Actions CI/CD:** último run (`2026-11-22T02:45Z`) **`success`** (verde).
- [x] **Todos os 3 Produtos Ativos na Play Console:** `sweep_pro_monthly` (`ACTIVE`), `sweep_pro_annual` (`ACTIVE`) e `sweep_pro_lifetime` (`oneTimeProducts` `lifetime-buy:ACTIVE` em 170+ países, `US $49.99`) — confirmados ao vivo hoje.
- [x] **Android Vitals:** conectado e com dados frescos até `2026-11-21` / `2026-11-22 UTC` (zero reviews negativos).
- [x] **Backend de Compras (Supabase):** conexão direta via `@supabase/supabase-js` OK (`play_purchases = 1` venda testada validada na Edge Function `verify-play-purchase`).
- [x] **Funil Web (`https://midiastory-df6a0.web.app/`):** `HTTP 200` (`597 ms`), `14` idiomas (`hreflang`), `3` Schema JSON-LD e `3` CTAs para a Play Store.

---

## 🔴 FILA DE PRIORIDADES REAIS (EM ORDEM EXATA DE EXECUÇÃO)

### PRIORIDADE #1 — Destravar a Aprovação de Produção na Google Play (PORTÃO DE CAIXA)
- [x] **Respostas do Formulário de Produção + Data Safety 100% auditadas contra `AndroidManifest.xml` real (`v2.7.6 vc 31`):** zero `MANAGE_EXTERNAL_STORAGE`, zero `AD_ID`, prontas para copiar e colar em `docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md`.
- [ ] **Acompanhar contagem do Teste Fechado (14 dias contínuos com 12+ testadores):** aguardar liberação do botão **"Solicitar a produção"** na Play Console sem derrubar testadores.

### PRIORIDADE #2 — Acabamento de Conversão Antes da Abertura Pública
- [x] **Auditoria completa de 145 páginas HTML do funil (`cheerio`):** **0 links locais quebrados**, 138/145 páginas públicas 100% válidas em SEO e `meta description` corrigida em `docs/privacidade.html`.
- [ ] **Manter 0 crashes / 0 ANRs no Android Vitals** acompanhando os testadores no Teste Fechado (`v2.7.6 vc 31`).

### PRIORIDADE #3 — Primeiras Vendas Reais, Marketing FRIDAY & Escala (Pós-Aprovação de Produção)
- [x] **Upgrade da Apresentadora FRIDAY (Realismo Humano):** 6 frames fotorrealistas de estúdio 85mm + 16 níveis de boca Optical-Flow a 28,5 FPS ativos no Companion ao vivo.
- [ ] **Midiastore `v2.8.0` (Apenas após aprovar Produção da `v2.7.6`):** Assistente **Maria** em destaque Hero Clean na Home (14 idiomas offline, ~150 KB) + **IA Nativa `Gemini Nano` (Android `AICore`, 0 MB no APK)** como benefício exclusivo do **Midiastore Pro** (decisão do fundador em `2026-11-22`).
- [ ] **Fábrica de Vídeos de Anúncio da FRIDAY (14 Idiomas):** gravar vídeos curtos (`9:16` Shorts/Reels/TikTok + `16:9` Google Ads App Campaigns) com 3 ganchos psicológicos de conversão por idioma + legendas grandes + telas reais do Midiastore.
- [ ] **Testar 1 compra real em Produção** (Mensal / Anual / Vitalício) e validar crédito em `public.play_purchases`.
- [ ] **Acionar tração orgânica custo zero + Google Ads (Regra 50/50):** usar os 50% de reinvestimento do primeiro caixa (`R$ 5.000+`) para VPS Linux 24/7 + API `Gemini 4 Carbon` + escala de anúncios.
