# Snapshot de Continuidade & Fechamento do Dia — 2026-11-22 (`v2.8.0` • `versionCode 32`)

## 1. Status Oficial Publicado na Google Play Store
- **Versão Publicada:** `2.8.0 (32)` (`versionCode = 32`, `versionName = "2.8.0"`).
- **Faixas Atualizadas (`[completed]`):** `internal` (Teste Interno) e `alpha` (Teste Fechado — 23 testadores ativos 100% preservados).
- **Edit ID Commitado (HTTP 200):** `14212325279113978395`.
- **SHA-256 do AAB Assinado (`sweep-upload.jks`):** `97035252009b5cf95a7f75892f9d5ab7eeb236d6fc764ca6ea8b804d730a5227` (`7.365.700 bytes`).
- **Runs GitHub Actions Verificados (`100% success`):**
  - `Run 45003986718` (`build-signed-release-aab` + `build-debug-apk`)
  - `Run 45003986707` (`Midiastore CI/CD & Security Pipeline`)
- **Dispositivo Físico:** Motorola Moto G84 5G (`0085353748`) atualizado e validado nas 3 abas (`Limpar`, `Arquivos`, `Pro`).

## 2. O Que Foi Construído e Aprovado Hoje (`v2.8.0`)
1. **Assistente Pessoal Maria (`MariaAssistantBanner.kt` & `MariaLipSyncEnvelopes.kt`):**
   - Retratos de estúdio widescreen `720x512` (`maria_lip_0..7`, `maria_blink_0..2`, `maria_talk_blink`, `maria_smile`) com extrusão scanline sem emendas laterais.
   - Marca **`Midiastore`** integrada dentro do topo esquerdo do quadro da Maria e badge limpa **`● MARIA • ASSISTENTE IA`** (sem texto sobreposto na roupa).
   - 8 falas neurais completas (`pt-BR-FranciscaNeural`, 8,4s a 28,9s) com **dublagem silábica ZCR + detecção de vales consonantais + compensação de `-85ms` da latência do `AudioTrack` do Android a 35,7 FPS**.
2. **Chat Midiastore IA (White Sheet estilo ChatGPT/Gemini):**
   - Modal em folha branca `#FFFFFF` com pílulas cinzas `#F0F4F9` e resposta em tempo real da Maria, sem caixa azul inline no rodapé da aba Pro.
3. **14 Idiomas (`MariaI18n`) + Limpador Global de Mensageiros (`WhatsAppCleanerDataSource.kt`):**
   - Tradução integral nas 14 línguas do app e varredura unificada de WhatsApp, Telegram, WeChat, LINE, KakaoTalk, Messenger, Instagram, TikTok e `Download/`.
4. **Tamanhos Reais em `MB / GB` e Estabilidade do Scanner (`MediaScanViewModel.kt` & `MainActivity.kt`):**
   - Todos os quadradinhos exibem tamanhos reais ao vivo (`MB / GB`), a aba `Arquivos` abre direto nas 100 fotos com miniaturas (`ALL_ITEMS`) e mudanças concorrentes do `MediaStore` (`SOURCE_CHANGED`) entregam os arquivos encontrados em vez de exibir erro.

## 3. Ambiente Pronto para Amanhã Ao Ligar o PC
- **Auto-Start Windows Configurado:** `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\Maria-FRIDAY-AutoStart.vbs` inicia automaticamente a Maria/FRIDAY no desktop (`scripts/antigravity-companion.ps1`) assim que o Windows iniciar.
- **Memória Cognitiva Sincronizada:** Supabase `public.memories` (`#497` e `#498`), `docs/CONTINUATION_HANDOFF.md` e `docs/LINHA_DO_TEMPO_AUTONOMA.md` sincronizados via `.agents/hooks.json`.
- **Próximo Foco de Amanhã:**
  1. Evoluir a dublagem da Maria para **Mapeamento Fonético-Visêmico F1/F2 em 60 FPS com `Crossfade` sub-frame** (diferenciando vogais abertas `A/É`, arredondadas `O/U`, sibilantes `S/Z` e bilabiais `P/B/M`) para o lançamento em Produção ficar 100% perfeito.
  2. Monitorar a contagem regressiva do Teste Fechado rumo ao botão **"Solicitar a produção"**.
