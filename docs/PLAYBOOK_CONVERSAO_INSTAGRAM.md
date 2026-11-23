# 🎬 PLAYBOOK OFICIAL DE CONVERSÃO ORGÂNICA — INSTAGRAM `@aconteceunobr` → MIDIASTORE PRO

> **Canal:** `@aconteceunobr` (`27.847` seguidores | `2,0M` views/30 dias | Página `CRBnotícias`)  
> **Funil:** `@aconteceunobr` → `https://midiastory-df6a0.web.app/?utm_source=instagram&...` → Google Play (`com.rdf112.midiastore`) → Assinaturas Pro (`R$ 14,99` / `R$ 79,99` / `R$ 149,99`)  
> **Trava de Segurança:** Comentários automáticos ficam **100% travados em modo `--dry-run` até a abertura da Produção na Google Play em quinta-feira `26/11`**.

---

## 1. Regra de Ouro — Como Converter Sem Derrubar o Alcance
- **NÃO postar propaganda fria no feed** (cai para ~500 views e derruba a entrega do perfil).
- **Usar as 3 alavancas de retenção intacta:**
  1. **Comentário Fixado em Reels subindo (`> 2.000 views` ou `> 30 curtidas`):** máximo **1 comentário natural por vídeo** (anti-spam), levando para o link na bio.
  2. **Vinheta de 2 segundos só no final dos Reels virais:** `C:\Users\rodolfo\Downloads\midiastore-kit-organico\vinheta-final-2s-reels-9x16.mp4` (`1080x1920`, `46,5 KB`) — colocada nos últimos 2s depois que o público já assistiu o vídeo inteiro.
  3. **1 Story diário de 10 segundos com Sticker de Link UTM:** mostrar armazenamento cheio → 1 toque no Midiastore → GB liberados + link clicável direto.

---

## 2. Escala Global Sem Barreira de Idioma
- Vídeo puramente visual pronto: `C:\Users\rodolfo\Downloads\midiastore-kit-organico\video-global-10s-sem-idioma-9x16.mp4` (`1080x1920`, `10s`, `157,5 KB`: `99%` vermelho → `SHA-256` azul → `+12.4 GB` verde + `Midiastore` + `Google Play`).
- Publicar em perfil dedicado **Midiastore** no YouTube Shorts, TikTok e Reels para EUA, Europa, Índia, LatAm e Ásia.

---

## 3. Links Exatos com Rastreamento UTM (`public.product_events` + Play Store `referrer`)
- **Bio (`@aconteceunobr`):** `https://midiastory-df6a0.web.app/?utm_source=instagram&utm_medium=bio&utm_campaign=aconteceunobr`
- **Stories:** `https://midiastory-df6a0.web.app/?utm_source=instagram&utm_medium=story&utm_campaign=aconteceunobr`
- **Reels:** `https://midiastory-df6a0.web.app/?utm_source=instagram&utm_medium=reels&utm_campaign=aconteceunobr`
- **YouTube Shorts:** `https://midiastory-df6a0.web.app/en/?utm_source=shorts&utm_medium=video&utm_campaign=global_10s`
- **TikTok:** `https://midiastory-df6a0.web.app/en/?utm_source=tiktok&utm_medium=bio&utm_campaign=global_10s`

---

## 4. Automação Segura (100% Node.js, Zero Python)
- **Detectar Reels subindo (modo dry-run seguro até 26/11):**
  ```bash
  node scripts/instagram-api.mjs watch-viral
  ```
- **Cruzar Instagram + Cliques Web + Vendas Pro e gravar em `public.aprendizado_ia`:**
  ```bash
  node scripts/growth-learning-loop.mjs
  ```
- **Regra 50/50:** com as primeiras vendas pós-26/11, reservar ~R$ 70–90 dos 50% de reinvestimento para comprar o domínio próprio **`midiastore.app`** e apontar para o Firebase Hosting.
