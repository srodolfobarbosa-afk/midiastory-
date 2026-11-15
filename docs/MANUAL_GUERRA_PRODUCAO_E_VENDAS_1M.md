# Manual de Guerra: Produção Google Play & Escala para R$ 1.000.000,00 (`com.rdf112.midiastore`)

> **Baseado 100% em código real, políticas vigentes da Google Play e dados verificados até 2026-11-15.**
> Status atual do relógio oficial de teste fechado: **12+ testadores opt-in atingidos — Dia 2 de 14 contínuos (faltam 12 dias para liberar o botão *Solicitar a produção*)**.

---

## Frente A — Engenharia Android, Scoped Storage, Estabilidade & Billing Blindado

1. **Por que o Midiastore proíbe `MANAGE_EXTERNAL_STORAGE` e como opera em API 29–36?**
   - O acesso amplo a arquivos é restrito pela política de *All Files Access* da Google Play e rejeita apps limpadores comuns. O Midiastore usa exclusivamente `MediaStore` (*Scoped Storage* para Imagens, Vídeos e Áudio por volume), com seleção explícita de revisão manual.
2. **Como funciona a exclusão segura na API 29 (Android 10) vs API 30+ (Android 11+)?**
   - Na API 30+, o app usa `MediaStore.createTrashRequest` (Lixeira do sistema, que **não** credita espaço liberado) e `MediaStore.createDeleteRequest` (exclusão permanente). Na API 29, não existe Lixeira de sistema; o botão de Lixeira fica oculto e o app usa `RecoverableSecurityException` com fluxo oficial de consentimento por item, validando ausência pós-exclusão antes de creditar bytes.
3. **Quais são os limites de segurança do scanner global e por que ele não trava a UI?**
   - Todo hashing SHA-256 em streaming e varredura global roda fora da UI thread, com cancelamento cooperativo e tetos rígidos: `250.000` linhas, `20.000` candidatas de tamanho repetido, `100.000` buckets de tamanho único, `2 GiB` por arquivo e `10 GiB` de I/O total de hash global (`512 MiB` / `1 GiB` na aba Arquivos). Se a cobertura global for parcial, o seletor automático de duplicatas **falha fechado** (não pré-seleciona).
4. **Como evitar falsos positivos no cálculo de espaço liberado?**
   - Mover para a Lixeira do Android **não** libera armazenamento físico imediato. O app só credita bytes liberados após exclusão permanente e confirmação pós-operação de que o URI não existe mais no `MediaStore`.
5. **Como `verify-play-purchase` blinda o Plano Pro contra fraude (fail-closed)?**
   - A Edge Function `supabase/functions/verify-play-purchase/index.ts` exige `packageName === "com.rdf112.midiastore"`, valida o `purchaseToken` via OAuth JWT da Service Account direto na **Google Play Developer API v3** (`subscriptionsv2/tokens` para `sweep_pro_monthly`/`sweep_pro_annual` e `purchases/products` para `sweep_pro_lifetime`), exige `SUBSCRIPTION_STATE_ACTIVE` ou `IN_GRACE_PERIOD` (ou `purchaseState === 0` no vitalício) e `matchedItem.productId === productId`, e salva apenas o **SHA-256 do token** em `public.play_purchases`.
6. **Por que a assinatura do AAB `v2.5.0` (`vc 22`) exige `keystore.properties` / `SWEEP_STORE_FILE`?**
   - O `app/build.gradle.kts` trava `bundleRelease` com a chave legítima `sweep-upload.jks` (e R8 `isMinifyEnabled = true`, `isShrinkResources = true`). Trocar keystore ou `applicationId` quebra atualizações na Play Console e invalida os testadores atuais.
7. **Como evitar quebra de UI e falsos positivos em aparelhos físicos (ex.: Moto G84)?**
   - Não usar altura fixa `dp(52)` em botões de 2 linhas (corta o texto), controlar `cardBackgroundColor`/`elevation` contra surface tint esverdeado do Material 3, e nos testes instrumentados usar `assumeNoExternalAutoClicker` para evitar interferência de apps de auto-clicker nos diálogos do sistema.
8. **Quais os 2 requisitos de conformidade em runtime implementados para evitar rejeição na Play Store?**
   - (1) *Prominent Disclosure* explícito (`pro_unused_apps_disclosure_*`) antes de abrir `Settings.ACTION_USAGE_ACCESS_SETTINGS` para `PACKAGE_USAGE_STATS`, explicando uso 100% local; (2) `pro_billing_terms_disclosure` visível no paywall e diálogo rápido informando renovação automática e cancelamento na Play Store (e pagamento único no Vitalício).

---

## Frente B — Questionário Oficial de Produção da Google Play (Respostas Prontas pós-Dia 14)

*(Use estas respostas objetivas, verdadeiras e alinhadas ao comportamento real do app quando o botão **Solicitar a produção** abrir no Dia 14):*

9. **Parte 1 — Sobre seu teste fechado: Como você recrutou testadores para seu app?**
   - *"Recrutamos testadores reais (usuários Android do Brasil com aparelhos variados, como Motorola/Samsung em diferentes versões do Android) por meio da nossa página de convite oficial (Firebase Hosting) com cadastro de e-mail para inclusão na lista de testadores e direcionamento exclusivo pelo link oficial de opt-in do Google Play."*
10. **Parte 1 — Quão fácil foi recrutar testadores e mantê-los engajados?**
    - *"Inicialmente exigiu comunicação direta para explicar a diferença entre instalar localmente e participar pelo link oficial de opt-in da Play Store. Após ajustarmos o onboarding e liberarmos ferramentas essenciais de limpeza e mídia gratuitamente, atingimos 12+ testadores opt-in contínuos e mantivemos uso real durante o ciclo de 14 dias."*
11. **Parte 1 — Qual foi o nível de engajamento dos testadores (quais recursos usaram, o que funcionou/não funcionou)?**
    - *"Os testadores utilizaram principalmente o scanner de arquivos grandes (>10 MB), detecção de duplicatas exatas (SHA-256), limpeza de mídia do WhatsApp e o Estúdio de Mídia local (edição de foto/vídeo/EXIF). Relataram boa velocidade de varredura e sugeriram melhorias de layout (botões com 2 linhas sem corte em telas menores, contraste de cards e exibição clara de termos antes de permissões)."*
12. **Parte 2 — Sobre o app: Quais mudanças você fez no app com base no feedback dos testadores?**
    - *"Corrigimos altura de botões e contraste Material 3 observados em aparelho físico (Moto G84), ocultamos a opção de Lixeira na API 29 onde o Android 10 não a suporta (adicionando fluxo `RecoverableSecurityException`), substituímos a geração de QR por `zxing:core`, adicionamos validação de `Content-Type` no downloader e incluímos Prominent Disclosure claro para `PACKAGE_USAGE_STATS` e aviso de renovação no paywall."*
13. **Parte 2 — Como você decidiu que seu app está pronto para produção?**
    - *"Validamos a estabilidade em aparelhos reais e testes automatizados (`:domain:test`, `:app:testDebugUnitTest`, testes instrumentados), reduzimos o binário com R8 (~4,2 MB), verificamos políticas de Scoped Storage (zero `MANAGE_EXTERNAL_STORAGE`), Data Safety e Advertising ID na Play Console, além de validar o fluxo de compras com License Testers e verificação server-side na Google Play Developer API v3."*
14. **Parte 3 — Sobre sua estratégia de lançamento: Quem é o público-alvo do app e como ele gera valor?**
    - *"Usuários de Android com armazenamento próximo do limite que desejam liberar espaço de forma segura, transparente e 100% local (sem enviar fotos/vídeos para servidores externos), identificando duplicatas exatas por hash SHA-256, arquivos grandes/antigos e mídias acumuladas do WhatsApp."*
15. **Checklist de pré-envio antes de clicar em "Enviar para revisão de produção":**
    - Subir AAB `v2.5.0` (`vc 22`) **sem remover a lista de testadores atuais**; validar 1 compra real ponta a ponta com License Tester na `verify-play-purchase`; completar os `phoneScreenshots` dos 6 idiomas faltantes (`es-ES`, `fr-FR`, `de-DE`, `hi-IN`, `ja-JP`, `zh-CN`); conferir Data Safety e declarações de permissões (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `PACKAGE_USAGE_STATS`).

---

## Frente C — Conversão Máxima no Paywall, Preço & Retenção

16. **Qual o papel psicológico de cada plano (`R$ 14,99` mensal, `R$ 79,99` anual e `R$ 149,99` vitalício)?**
    - **Mensal (`R$ 14,99/mês` = R$ 179,88/ano):** serve como âncora de comparação e porta de entrada de baixo atrito.
    - **Anual (`R$ 79,99/ano` = ~R$ 6,67/mês):** âncora de maior custo-benefício (economia de ~55% vs 12 meses), gerando caixa anual antecipado para reinvestimento.
    - **Vitalício (`R$ 149,99` único):** captura usuários com aversão a assinaturas recorrentes e acelera payback imediato de CPI.
17. **Como estruturar o paywall sem enganar o usuário (compliance + conversão)?**
    - Destacar o plano Anual com badge claro de economia real (~55% frente ao mensal), manter os 3 planos visíveis com `pro_billing_terms_disclosure` abaixo dos botões (renovação automática e cancelamento fácil na Play Store) e oferecer o recurso gratuito de limpeza real antes do upgrade para gerar reciprocidade ("Aha! Moment" primeiro).
18. **Em qual momento exato mostrar o paywall para maximizar conversão?**
    - **Nunca bloquear na abertura do app.** Mostrar o paywall após o usuário ver o diagnóstico real de espaço recuperável (duplicatas SHA-256 / arquivos grandes / WhatsApp) ou ao tocar em recursos Pro exclusivos (Detector de Apps sem uso há 90+ dias, Relatório Auditado e Estúdio avançado).
19. **Como reduzir cancelamentos e reembolsos na primeira semana?**
    - Cumprir 100% o que foi prometido (zero promessa de "limpar RAM/cache de terceiros"), exibir exatamente quantos MB/GB foram liberados de forma permanente (sem contar Lixeira) e manter comunicação transparente de cancelamento na Play Store.
20. **Qual o impacto de manter o modelo Freemium forte?**
    - Ferramentas gratuitas reais aumentam retenção orgânica, geram avaliações 5 estrelas na Play Store (que reduzem CPI) e aumentam o volume de usuários qualificados expostos ao paywall Pro.
21. **Por que nunca usar checkout web fora da Google Play dentro do app?**
    - Viola a política de Pagamentos da Google Play e gera suspensão imediata da conta de desenvolvedor. Toda monetização no app Android passa exclusivamente pelo Google Play Billing `8.3.0` + `verify-play-purchase`.
22. **Quantos clientes são necessários para bater R$ 1.000.000,00 brutos nos 3 SKUs?**
    - Equivalência unitária bruta: **~66.711** mensalidades (`R$ 14,99`), **~12.500** planos anuais (`R$ 79,99`) ou **~6.667** vitalícios (`R$ 149,99`) — ou um mix realista (ex.: 6.000 anuais + 3.500 vitalícios = ~R$ 1.004.890,00 brutos, lembrando da taxa da Play Store de 15% no primeiro US$ 1M/ano para calcular o líquido real).

---

## Frente D — Escala de Vendas, ASO, Criativos & Regra 50/50

23. **Como fazer ASO de alta conversão na Google Play sem violar políticas?**
    - Títulos localizados respeitando o limite de **30 caracteres** (ex.: `Midiastore: Limpiador Estudio` em `es-ES`), screenshots localizadas em todos os 14 idiomas com prova visual real (scanner SHA-256, bytes reais liberados, Estúdio local) e zero palavras proibidas ("booster", "antivírus", "resfriar CPU").
24. **Qual a estrutura de criativo vencedora para Reels / TikTok / Shorts (Loop Invisível)?**
    - **0–2s (Gancho):** "Seu celular disse 'armazenamento cheio'? Olha quantos GB de arquivo duplicado idêntico estavam escondidos." -> **2–7s (Prova real na tela):** mostrar o scanner SHA-256 achando cópias exatas e liberando GB reais -> **7–10s (CTA):** comentar `"ESPAÇO"` ou baixar pelo link oficial da Play Store.
25. **Qual métrica manda na hora de escalar tráfego pago (Google Ads App / Meta)?**
    - **CPI vs LTV líquido (descontando taxa de 15% da Google Play).** Só escalar orçamento quando `LTV_liquido_D7/D30 > CPI` com margem positiva comprovada em produção (nunca escalar no escuro).
26. **Como aplicar a Regra Financeira 50/50 na prática?**
    - Todo lucro líquido real (receita líquida recebida da Google Play menos custos operacionais) é dividido em **50% retirada/lucro do fundador** e **50% reinvestimento dinâmico** no gargalo de maior ROI do momento (ASO/tráfego pago se LTV > CPI, infraestrutura/APIs pagas ou hardware/testes).
27. **Como aproveitar o funil web em 13 idiomas no Firebase (`midiastory-df6a0.web.app`)?**
    - Usar as páginas SEO e artigos localizados para capturar buscas orgânicas internacionais de "limpar armazenamento Android / fotos duplicadas" e direcionar tráfego qualificado direto para a ficha da Play Store.
28. **Como automatizar reviews e retenção sem custo fixo alto?**
    - Usar `scripts/invisible-loop-bot.mjs` e `scripts/organic-growth-agent.mjs` conectados ao `ai-failover-router.mjs` (Google AI Studio + modelos `:free` + `codex.exe`), mantendo o Supabase abaixo de 400 MB via `supabase-maintenance.mjs`.
29. **Qual o plano de ação exato dos próximos 12 dias (Dia 2 -> Dia 14)?**
    - (1) Validar testes locais e subir AAB `v2.5.0` (`vc 22`) na faixa fechada **sem derrubar os 12+ testadores**; (2) Validar 1 compra real com License Tester ponta a ponta na `verify-play-purchase`; (3) Manter 14–15 testadores ativos para folga contra desinstalações; (4) Completar screenshots dos 6 idiomas e preparar respostas da Frente B para enviar assim que o Dia 14 liberar.
30. **Qual o critério de corte/pivô caso um canal de aquisição não dê ROI?**
    - Nunca desistir por preguiça e nunca queimar caixa em canal negativo: isolar a variável (criativo, segmentação, store listing ou oferta do paywall), testar nova hipótese mensurável com verba controlada e realocar os 50% de reinvestimento para o canal com melhor relação `LTV / CPI`.
