# Benchmark de Preços e Posicionamento Comercial — Sweep Pro (Google Play Billing 8.0.0)

> **Rascunho histórico não validado. Não usar preços nem afirmações abaixo na Play Console ou em marketing.** Os valores descritos são hipóteses antigas, não foram capturados de `ProductDetails` da conta Midiastore nem verificados para os países/datas indicados. A pesquisa atual e a estratégia sem preços numéricos estão em [MARKET_INTELLIGENCE_2027.md](MARKET_INTELLIGENCE_2027.md) e [REVENUE_AND_PRICING_STRATEGY.md](REVENUE_AND_PRICING_STRATEGY.md).

**Data da consulta:** 02 de novembro de 2026
**Mercados analisados:** Brasil (BRL / `R$`) e Internacional / Estados Unidos (USD / `$`)
**Pacote de produção:** `com.rdf112.sweep`
**IDs oficiais preservados na Google Play Console:**
- Assinatura Mensal (`ProductType.SUBS`): `sweep_pro_monthly`
- Assinatura Anual (`ProductType.SUBS`): `sweep_pro_annual`
- Compra Única Vitalícia (`ProductType.INAPP`): `sweep_pro_lifetime`

---

## 1. Aplicativos Concorrentes Comparados

| Aplicativo | Desenvolvedor | Modelo de Monetização | Preços Observados (Brasil / Global) | País / Moeda | Data da Consulta | Fonte Oficial |
|---|---|---|---|---|---|---|
| **Avast Cleanup – Phone Cleaner** | Avast Software | Freemium + Assinatura Anual / Multi-dispositivo | Loja oficial Brasil a partir de **R$ 36,33/mês** (equivalente anual nos pacotes móveis/multi-dispositivo) e **US$ 69,99/ano** no catálogo global | Brasil (`BRL`) / EUA (`USD`) | 02/11/2026 | [Google Play — Avast Cleanup](https://play.google.com/store/apps/details?id=com.avast.android.cleaner) / [Loja Avast Brasil](https://www.avast.com/pt-br/store) |
| **AVG Cleaner – Storage Cleaner** | AVG Mobile | Freemium + Assinatura Pro In-App | Faixa equivalente ao grupo Gen Digital (Avast/AVG), com planos anuais promocionais de 1º ano na faixa de **R$ 59,90 a R$ 119,90/ano** no Google Play | Brasil (`BRL`) / Global | 02/11/2026 | [Google Play — AVG Cleaner](https://play.google.com/store/apps/details?id=com.avg.cleaner&hl=en) |
| **CCleaner – Phone Cleaner** | Piriform (Gen Digital) | Freemium + Assinatura CCleaner Pro Android | Assinatura mensal e anual via Google Play; foco em limpeza de cache e arquivos grandes com anúncios na versão gratuita | Brasil (`BRL`) / Global | 02/11/2026 | [Google Play — CCleaner Android](https://play.google.com/store/apps/details?id=com.piriform.ccleaner) / [CCleaner Brasil](https://www.ccleaner.com/pt-br/ccleaner/professional) |
| **SD Maid 2/SE – System Cleaner** | darken | Freemium + Compra Única / Pro | Foco técnico avançado em sistema e resíduos de apps; referência de compra única acessível sem anúncios invasivos | Global (`BRL` / `USD`) | 02/11/2026 | [Google Play — SD Maid 2/SE](https://play.google.com/store/apps/details?id=eu.darken.sdmse) |
| **Files by Google** | Google LLC | Gratuito (Utilitário base do sistema) | **Gratuito** — funciona como explorador manual sem motor OaaS de preparação automatizada de lotes auditados com trava de referência original | Global | 02/11/2026 | [Google Play — Files by Google](https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files) |

---

## 2. Recomendação de Preços para a Google Play Console (Sweep Pro)

Para posicionar o **Sweep** de forma competitiva frente ao [Avast Cleanup](https://play.google.com/store/apps/details?id=com.avast.android.cleaner) e ao [CCleaner Android](https://play.google.com/store/apps/details?id=com.piriform.ccleaner), entregando **zero anúncios**, **processamento 100% local** e **motor OaaS de 6 etapas com proteção SHA-256 de originais**:

| Produto na Play Console | ID do Produto (`productId`) | Tipo no Billing 8.0.0 | Preço Recomendado — Brasil (`BRL`) | Preço Recomendado — Internacional (`USD`) | Posicionamento Estratégico |
|---|---|---|---|---|---|
| **Sweep Pro Mensal** | `sweep_pro_monthly` | `ProductType.SUBS` | **R$ 9,90 / mês** | **US$ 2.99 / month** | Porta de entrada acessível para quem deseja organizar o armazenamento em um mês específico sem compromisso longo. |
| **Sweep Pro Anual** | `sweep_pro_annual` | `ProductType.SUBS` | **R$ 59,90 / ano** *(equivale a ~R$ 4,99/mês — economia real de 50%)* | **US$ 17.99 / year** *(economia real de 50%)* | Plano âncora de maior retenção (LTV), custando metade das suítes tradicionais com anúncios. |
| **Sweep Pro Vitalício** | `sweep_pro_lifetime` | `ProductType.INAPP` | **R$ 119,90** *(compra única)* | **US$ 34.99** *(one-time)* | Alternativa para usuários que rejeitam assinaturas recorrentes (público similar ao do [SD Maid 2/SE](https://play.google.com/store/apps/details?id=eu.darken.sdmse)). |

---

## 3. Regras Técnicas de Exibição de Preço e Desconto no App

1. **Fonte única da verdade (`ProductDetails`)**: o aplicativo **nunca** exibe preços fixos em reais ou dólares no código-fonte. Todos os valores são lidos dinamicamente de `ProductDetails` (`formattedPrice`, `priceAmountMicros` e `priceCurrencyCode`) retornados pela biblioteca oficial Google Play Billing `8.0.0`.
2. **Cálculo real de desconto anual (`BillingStatePolicy.calculateRealAnnualDiscountPercent`)**: o percentual de economia do plano anual em relação a `12 × mensal` é calculado matematicamente a partir de `priceAmountMicros`. Caso a Play Store ainda não tenha retornado os dois preços na mesma moeda, o app não exibe nenhum percentual fictício.
3. **Transparência sobre meios de pagamento**: os métodos disponíveis (cartão, saldo Google Play ou meios locais habilitados pela própria Google Play para cada tipo de produto e país) são gerenciados exclusivamente pelo fluxo nativo da Google Play Store.
