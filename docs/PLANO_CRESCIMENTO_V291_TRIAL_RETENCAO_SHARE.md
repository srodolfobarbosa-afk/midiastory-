# 📈 PLANO EXECUTIVO & ARQUITETURAL (`v2.9.1 GROWTH PACK` — `feat/v2.9.0-motor-app`)
> 🧭 **Índice Central:** [**`docs/INDICE_MESTRE.md`**](INDICE_MESTRE.md) · [**`CHECKLIST_EVOLUCAO_CARBON.md`**](CHECKLIST_EVOLUCAO_CARBON.md) · [**`ESTUDO_MARIA_2_HUMANA_E_ESCALA.md`**](ESTUDO_MARIA_2_HUMANA_E_ESCALA.md) · [**`CHECKLIST_PRODUCAO_VIVA.md`**](CHECKLIST_PRODUCAO_VIVA.md)
**Status:** Planejado e Auditado (Aguardando comando `"Pode codar o Growth Pack"` pós-abertura de Produção em `26/11` ou na `feat/v2.9.0-motor-app`, mantendo `tag v2.8.2-carbon-baseline` 100% intocada).

---

## 🎯 Objetivo Estratégico (Rumo a R$ 1.000.000,00 com Regra 50/50)

Combinar as **3 maiores alavancas mundiais de crescimento orgânico, retenção e assinatura recorrente** sem violar nenhuma regra da Google Play Console e mantendo o AAB enxuto (`< 8 MB`):
1. **Conversão (+131% estimada):** Teste grátis de **3 dias (`P3D`) exclusivo no Plano Anual (`sweep_pro_annual` · R$ 79,99/ano)** com verificação dinâmica de elegibilidade.
2. **Retenção D30 (`8% → 25%+`):** **Notificação Semanal Inteligente** (`>= 300 MB` reais acumulados, máximo **1 disparo por semana**, zero spam, zero consumo em segundo plano).
3. **Viralidade Orgânica (`K-Factor` Custo Zero):** **Botão Nativo de Compartilhar Resultado / App** para **WhatsApp, Telegram, Instagram (Stories/Direct) e Sharesheet do Android** com link UTM rastreável no Supabase.

---

## 🧩 Frente 1 — Teste Grátis de 3 Dias (`P3D`) Exclusivo no Plano Anual (`R$ 79,99/ano`)

### 1.1 Configuração na Google Play Console (Pós-Abertura de Produção `26/11`)
- **Produto:** `sweep_pro_annual` → Plano Base `annual-base` (`R$ 79,99/ano`).
- **Nova Oferta:** ID `annual-trial-3d`
  - **Critério de qualificação:** *Aquisição de clientes (Nunca tiveram esta assinatura — Google Play impede múltiplos testes grátis por conta automaticamente)*.
  - **Fase Gratuita:** `3 dias` (`R$ 0,00`), seguida de renovação automática em `R$ 79,99/ano` (cancelável a qualquer momento na Play Store).
- **Planos sem Trial (Ancoragem):** `sweep_pro_monthly` (`R$ 14,99/mês`) e `sweep_pro_lifetime` (`R$ 149,99`) permanecem **sem teste grátis**, direcionando +78% das escolhas para o Plano Anual.

### 1.2 Mudanças Cirúrgicas no Código (`feat/v2.9.0-motor-app`)
1. **`app/src/main/java/com/rdf112/sweep/data/billing/AndroidPlayBillingGateway.kt`:**
   - Estender `PlayCatalogProductUi` com:
     - `hasFreeTrialOffer: Boolean = false`
     - `freeTrialPeriodIso: String? = null` (ex.: `"P3D"`)
     - `preferredOfferToken: String? = null`
   - Em `updateCatalogMap()`: procurar dentro de `pd.subscriptionOfferDetails` primeiro uma oferta que possua fase gratuita (`pricingPhaseList.any { it.priceAmountMicros == 0L }`).
     - **Se existir (usuário elegível):** salvar o `offerToken` do trial + preço da fase paga (`pricingPhaseList.last()`) + `hasFreeTrialOffer = true`.
     - **Se não existir (ex-assinante não elegível):** cair automaticamente no `offerToken` do plano base pago com `hasFreeTrialOffer = false`.
   - Em `launchPurchaseFlow()`: usar `preferredOfferToken` para abrir exatamente a folha do Google Play com os **3 dias grátis**.
2. **Compliance Google Play Subscriptions Policy (Zero Reprovação):**
   - Exibir o CTA `"Testar 3 dias grátis • depois R$ 79,99/ano"` e o aviso claro de cobrança pós-3 dias **exclusivamente quando `catalog[PRODUCT_ANNUAL]?.hasFreeTrialOffer == true`**. Para usuários já utilizados do trial, manter o botão normal `"Assinar Anual • R$ 79,99/ano (-56%)"`.
3. **Backend Supabase (`supabase/functions/verify-play-purchase/index.ts`):**
   - Confirmar aceitação de `paymentState === 2` (*Free trial* ativo na Google Play Developer API `purchases.subscriptionsv2.get`) com expiração vinculada ao `expiryTime` oficial da Google — se o usuário cancelar e vencer os 3 dias, o acesso Pro fecha automaticamente (`fail-closed`).

---

## 🔔 Frente 2 — Notificação Semanal Inteligente (`>= 300 MB` · Máx. 1x/Semana)

### 2.1 Regras Anti-Spam e Economia de Bateria (Padrão Apple/Google)
- **Frequência máxima rígida:** no máximo **1 notificação a cada 7 dias** (`PeriodicWorkRequestBuilder<WeeklyStorageDigestWorker>(7, TimeUnit.DAYS)` + trava adicional em `SharedPreferences` `last_digest_epoch_ms`).
- **Condição de silêncio absoluto:** se a soma real de mídias novas/recuperáveis desde a última abertura for **`< 300 MB`** (`314_572_800 bytes`) ou se o usuário abriu o app nas últimas **48 horas**, o worker encerra em `< 120 ms` **sem emitir nenhuma notificação**.
- **Restrição de bateria:** `Constraints.Builder().setRequiresBatteryNotLow(true).build()` — nunca usa `AlarmManager` exato nem `ForegroundService`.

### 2.2 Momento Certo da Permissão (`POST_NOTIFICATIONS` no Android 13–16)
- Adicionar `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` no `AndroidManifest.xml` (permissão normal de notificação, zero impacto em Data Safety / Scoped Storage).
- **Nunca pedir no cold start:** solicitar apenas **depois da 1ª limpeza bem-sucedida** com explicação clara (*"Quer receber no máximo 1 aviso por semana somente quando acumular mais de 300 MB para limpar?"*).

### 2.3 Arquitetura de Arquivos
- **Dependência leve em `app/build.gradle.kts`:** `androidx.work:work-runtime-ktx:2.10.0` (`~140 KB` pós-R8, mantendo o AAB com folga abaixo de `8 MB`).
- **Novo `app/src/main/java/com/rdf112/sweep/work/WeeklyStorageDigestWorker.kt`:** consulta rápida `MediaStore` (`Videos >= 15 MB` + `Screenshots > 30d` + `WhatsApp/Chats`) em background I/O, monta mensagem concreta nos **14 idiomas** (ex.: *"1,4 GB acumulados em vídeos e capturas. Toque para revisar com segurança"*) e abre `MainActivity` direto na aba `Limpar`.

---

## 📲 Frente 3 — Botão Nativo de Compartilhar Resultado / App (WhatsApp, Telegram e Instagram)

### 3.1 Segurança de Manifesto (Regra Crítica Auditada)
- **IMPORTANTE:** Para o Midiastore **enviar** compartilhamentos para WhatsApp/Telegram/Instagram, **NÃO adicionamos `<intent-filter>` de `ACTION_SEND` no `AndroidManifest.xml`** (que faria o Midiastore parecer um editor/receptor de mídia). Usamos apenas `startActivity(Intent.createChooser(...))` + o `FileProvider` (`${applicationId}.fileprovider`) que **já está declarado e aprovado** nas linhas 51–59 do `AndroidManifest.xml`!

### 3.2 Dois Gatilhos Virais de Alta Conversão
1. **Gatilho A — "Compartilhar Espaço Recuperado" (Pós-Limpeza com Card Visual OLED `1080x1920` p/ Stories & Status):**
   - Logo após liberar espaço confirmado (ex.: `1,8 GB liberados`), exibir botão discreto **`Compartilhar resultado`** ao lado do resumo.
   - Novo gerador leve em `Canvas` puro (`0 KB` de biblioteca extra!): `ShareAchievementCardRenderer.kt` desenha em `40 ms` um card vertical `1080x1350` escuro OLED `#000000` com o anel azul `Precision Storage Ring`, o valor real recuperado (`"Liberei 1,8 GB no meu celular com segurança"`), selo `"Midiastore • < 8 MB na Google Play"` e envia via `FileProvider` (`image/png` + texto com link UTM).
   - Funciona nativamente em **WhatsApp (Conversas e Status)**, **Instagram (Stories e Direct)** e **Telegram**, além do botão rápido de texto direto!
2. **Gatilho B — "Indicar Midiastore para um Amigo" (Aba `Limpar` / `Pro`):**
   - Bottom sheet rápida com **atalhos diretos de 1 toque** quando instalados (`com.whatsapp`, `org.telegram.messenger`, `com.instagram.android`) + botão **`Mais apps`** (`Intent.createChooser`), usando `<queries>` de pacote apenas se necessário ou chooser universal limpo do Android 15 (que já fixa WhatsApp/Telegram/Instagram no topo sem pedir permissão extra de visibilidade de pacotes!).
   - **Nota Técnica de Privacidade Google Play:** Usar o **Android System Sharesheet nativo (`Intent.createChooser` com `EXTRA_TITLE` + `ClipData` de preview do card)** é **superior** a declarar `<queries>` de pacotes específicos porque no Android 14/15/16 o sistema já coloca WhatsApp, Instagram Direct/Stories e Telegram nos 4 primeiros ícones do usuário sem exigir justificativa extra de `QUERY_ALL_PACKAGES`/visibilidade!
3. **Rastreamento Real de Viralidade (UTM → Site → Google Play → Supabase):**
   - Link compartilhado: `https://midiastory-df6a0.web.app/?utm_source=app_share&utm_medium=viral_card` (que já redireciona por idioma e registra o clique em `public.product_events` para o `scripts/growth-learning-loop.mjs` medir quantas instalações e assinaturas vieram de indicação gratuita!).

---

## 🚦 Ordem Recomendada de Execução (Zero Risco para `26/11`)

| Passo | Onde | Quando | Impacto no AAB |
| :--- | :--- | :--- | :--- |
| **1. Implementar Frente 3 (Share Card OLED + Link UTM 14 idiomas)** | `feat/v2.9.0-motor-app` | Imediato sob seu aval | **`+0 KB`** (100% `Canvas` + `FileProvider` nativo) |
| **2. Implementar Frente 2 (`WeeklyStorageDigestWorker` `>= 300 MB`)** | `feat/v2.9.0-motor-app` | Imediato sob seu aval | **`+140 KB`** (`WorkManager` R8) |
| **3. Implementar Frente 1 (`AndroidPlayBillingGateway` Free Trial 3d)** | `feat/v2.9.0-motor-app` + Oferta `annual-trial-3d` na Play Console | Código na branch agora + ativar oferta na Play Console após clicar em **"Solicitar a produção"** na quinta `26/11` | **`+0 KB`** (já usa `billing:8.3.0`) |
| **4. QA Lado a Lado `.debug` no Moto G84 (`0085353748`)** | USB ADB | Antes de qualquer merge pós-`26/11` | AAB estimado final: **`~6,85 MB` (`< 8 MB`)** |
