# Snapshot de Continuidade & Ciclo Sistêmico Integrado — 2026-11-23 (`v2.8.2` • `versionCode 34`)

## 1. Diretrizes Permanentes Gravadas no `AGENTS.md`
- **Diretriz 8 (Cofundador Técnico & Crescimento):** nunca esperar ordens picadas, nunca implementar gambiarras visuais, auditar sempre os 6 pilares com `node scripts/goal-growth-engine.mjs`.
- **Diretriz 9 (Visão Sistêmica Integrada):** preservar integralmente os **2 Pilares** (Pilar 1 = Midiastore rumo a R$ 1M + Regra 50/50; Pilar 2 = IA Central FRIDAY como fábrica multiproduto + Decomposição Total / Roteamento por Peso) e manter as **7 frentes sincronizadas**.

## 2. Status Real das 7 Frentes (Verificado Ao Vivo em `2026-11-23`)
1. **App Android (`v2.8.2` • `vc 34`):**
   - Commit `735dcd3` | CI GitHub Actions `Run 45118253546` + `Run 45118253451` **100% verdes**.
   - **AAB Release Assinado:** **`7,26 MB`** (`7.256.107 bytes`, SHA-256 `b0d0fe6e47f90eee124b7a53e818b8514e60bf9f623e8a17d7a584926e5de`), auditado via `bundletool` (`com.rdf112.midiastore`, `vc 34`, `v2.8.2`, zero `MANAGE_EXTERNAL_STORAGE`, zero `AD_ID`).
   - Removido modal cinza bloqueador na abertura e os 9 frames artificiais `maria_lip_0..7` + `maria_talk_blink`.
   - Nova barra compacta da Maria (`76.dp`, padrão Apple/Gemini Live): retrato editorial natural + piscada + respiração leve + anel neon + ondas de voz 60 FPS sob demanda (14 idiomas, `630/630` strings válidas em todos os locales).
   - **Pendência física:** Moto G84 estava desconectado do USB no instante do install (`adb devices` vazio); APK Debug e Release já estão baixados em `.tmp/qa/v282-apk/` prontos para validação visual assim que reconectar.
2. **Sistema FRIDAY Desktop (`scripts/antigravity-companion.ps1`):**
   - Somadas todas as conquistas sem regressão: **16 níveis Optical-Flow F1/F2 60 FPS + crossfade + gate de silêncio rígido + anel luminoso neon + 5 ondas de voz 60 FPS** (`PS1_PARSE_ERRORS=0`, rodando ao vivo).
3. **Google Play Console (Dia 10/14 • Previsão Produção: 26/11):**
   - `internal` e `alpha` intactos em `v2.8.0 (vc 32) [completed]` com **23 testadores preservados**.
   - `docs/PLAY_CONSOLE_PRODUCTION_APPLICATION_READY.md` auditado e atualizado para `v2.8.2 (vc 34)` + `PACKAGE_USAGE_STATS` local + declaração Fotos e Vídeos (**0 bloqueios P0**).
4. **Funil Web (`https://midiastory-df6a0.web.app/`):**
   - `HTTP 200` (`102 ms`), `14` idiomas (`hreflang`), `4` CTAs Play Store, `148` visitas (`114` únicos, `16` nas últimas 24h).
5. **Google Ads & APIs (`midiastory-df6a0` / conta `649-729-9513`):**
   - `Downloads/google-ads-midiastore/google-ads-editor-lote-14-idiomas.zip` (`5,80 MB`) + 28 banners + CSV EN/PT validados (`0` erros no `--dry-run`). Importação imediata via Google Ads Editor pronta (API `--execute` depende apenas de `GOOGLE_ADS_DEVELOPER_TOKEN` + OAuth2 se quiser automação REST).
6. **Conteúdo / Reels / Shorts:**
   - 2 MP4 prontos (`marketing/videos/midiastore-reel-ptbr-2026-11-19.mp4` e `enus`) + `youtube-shorts-uploader.mjs --dry-run` (`ready: true`).
7. **Supabase & Memória:**
   - `19,77 MB / 500 MB`, `play_purchases = 1`, `beta_testers = 2`, Score de Prontidão Comercial = **`75/100`** em `docs/MOTOR_CRESCIMENTO_META.md`.
