# Estratégia de aquisição do Midiastore Pro

**Status: plano de campanha; não é evidência de disponibilidade comercial.** O proprietário relatou uma compra de teste bem-sucedida. Compra de teste valida um cenário de teste, mas não comprova cobrança de usuários reais, catálogo aberto à produção, preços/ofertas ativos, nem validação confiável de entitlement. Antes de ativar anúncios pagos com objetivo de compra, confirmar esses pontos na Play Console e no backend.

## 1. ASO: termos para pesquisar e testar

Termos são hipóteses de descoberta, não promessa de topo. Play Store não funciona como busca por repetição de keywords: priorize título e descrição natural, aderência ao app e conversão. Faça testes por país/idioma e acompanhe impressões, visitas à página, instalações e retenção. Evite “RAM”, “acelerar”, “limpar cache de outros apps” e promessas de liberar uma quantidade fixa de espaço.

### pt-BR

**Núcleo de alta intenção**

- liberar espaço no celular
- armazenamento cheio Android
- limpar armazenamento do celular
- arquivos grandes Android
- localizar arquivos grandes
- gerenciar armazenamento Android
- limpar celular Android
- liberar memória do celular *(termo popular, embora tecnicamente memória e armazenamento sejam diferentes)*

**Descoberta por tarefa**

- encontrar vídeos grandes
- encontrar fotos duplicadas
- arquivos duplicados no celular
- analisar armazenamento Android
- organizar arquivos do celular
- limpeza segura de arquivos

**Marca / conversão**

- Midiastore
- Midiastore Pro
- limpador de espaço Android

### en-US

**High-intent core**

- free up phone storage
- Android storage full
- clean up phone storage
- storage cleaner for Android
- find large files Android
- Android storage manager
- phone storage analyzer
- free up space on Android

**Task discovery**

- find large videos Android
- find duplicate files Android
- Android file organizer
- review files before deleting
- safe storage cleanup

**Brand / conversion**

- Midiastore
- Midiastore Pro
- phone storage cleaner

**Aplicação:** manter “liberar espaço no celular” e “free up phone storage” em título/descrição curta somente se couberem naturalmente e forem compatíveis com a ficha e o produto. Distribuir os demais termos entre descrição longa, criativos e conteúdo editorial; não empilhar palavras. Revisar relatórios de termos de busca após cada ciclo.

## 2. Anúncios verticais: TikTok, Instagram Reels e YouTube Shorts

**Conceito criativo:** o aperto de “armazenamento cheio” interrompe uma ação cotidiana. Mostrar a notificação/aviso de armazenamento cheia encenada (sem expor dados pessoais), abrir Midiastore, revisar os arquivos grandes e deixar a pessoa escolher o que fazer. Não simular aceleração do aparelho, gigabytes liberados ou resultados que não ocorreram no dispositivo filmado.

### Roteiro principal — 20 segundos (pt-BR)

**0–3s — gancho:** pessoa tenta gravar um vídeo; aparece “armazenamento quase cheio”. Texto: “Bem na hora? Celular sem espaço de novo?”

**3–7s — dor:** “Vídeos e arquivos grandes vão se acumulando. E você não quer apagar algo importante no impulso.”

**7–14s — demonstração real:** gravação de tela do Midiastore analisando mídias acessíveis e mostrando lista de arquivos grandes. “Veja o que está ocupando espaço e revise cada item antes de qualquer ação.”

**14–18s — confiança:** “A decisão é sua. O Android pede confirmação para remover.”

**18–20s — CTA:** “Instale o Midiastore grátis. Depois, conheça o Pro para exportar o resumo da sua revisão.” Tela: “Baixe na Google Play · Midiastore”.

**Legenda:** “Armazenamento cheio na pior hora? Encontre arquivos grandes, revise com calma e escolha o que fazer. Análise e limpeza segura grátis. O Pro inclui exportação do resumo revisado. Consulte disponibilidade e condições no app. #Android #ArmazenamentoCheio #LiberarEspaço #Midiastore”

### Variações rápidas para teste A/B

1. **Dor imediata:** “Foi gravar e apareceu ‘armazenamento cheio’?” Mostrar a busca real por arquivos grandes; CTA: “Veja o que ocupa espaço. Você decide o que revisar.”
2. **Medo de apagar:** “Não apague no escuro.” Mostrar seleção manual e confirmação do Android; CTA: “Analise grátis com Midiastore.”
3. **Rotina acumulada:** “A galeria cresce sem você perceber.” Mostrar arquivos grandes acessíveis; CTA: “Revise seus arquivos. Conheça o Midiastore Pro no app.”

**CTA recomendado para mídia:** “Instale grátis e revise seus arquivos”. É mais fiel ao funil atual que prometer uma assinatura imediata: Pro é apresentado no app, e o benefício Pro documentado é exportar o resumo da seleção revisada. Não usar preço/desconto até conferir o preço oficial retornado pela Play.

### English adaptation — 20 seconds (en-US)

**0–3s:** “Storage full—right when you need your phone?” Show a staged storage warning.

**3–7s:** “Large videos and files add up. You shouldn’t have to delete anything blindly.”

**7–14s:** Show an actual Midiastore scan and large-file review list. “See large files in your accessible media and review them before taking action.”

**14–18s:** “You stay in control. Android asks you to confirm removal.”

**18–20s:** “Install Midiastore free. Explore Pro in the app to export your review summary.”

**Caption:** “Storage full at the worst time? Find large files, review them, and choose what happens next. Core analysis and safe cleanup are free; Pro adds review-summary export. Check availability and terms in the app. #Android #PhoneStorage #StorageFull #Midiastore”

### Execução e mensuração

- Produzir cortes 9:16 de 15–20 s, legendados, com a ação principal visível sem áudio. Capturar a interface real na versão distribuída ao público.
- Começar com 3 ganchos e 2 demonstrações; mudar uma variável por vez. Identificar cada criativo com UTM/campanha e deep link apenas se estiver configurado.
- Medir instalação atribuída, abertura, início/conclusão de análise, visita à tela Pro, início e compra confirmada. Separar compras de teste de compras de produção.
- Pausar anúncios se destino, SKU ou compra de produção não estiverem disponíveis. Não usar “seu celular vai ficar rápido”, “libere X GB”, “um toque apaga tudo”, depoimentos inventados nem urgência falsa.

## 3. Página de destino e conversão

Atualizada em [docs/index.html](../index.html), servida como página estática. A página conduz à instalação e apresenta o Pro como próximo passo dentro do app. O texto diferencia análise/revisão gratuitas da exportação de resumo Pro e evita alegar compra de produção confirmada.

**CTA principal:** “Instale grátis e revise seus arquivos”.

**CTA Pro:** “Conheça o Midiastore Pro no app” — direciona à ficha Google Play, onde disponibilidade, preço e condições devem ser mostrados pelo catálogo oficial.

**Checklist antes de tráfego pago:** confirmar SKU/tipo/oferta e país na Play Console; validar compra e entitlement em produção com backend; confirmar que o link público abre o listing correto; instalar a versão pública; checar a correspondência entre anúncio, ficha e comportamento real; instalar medição consentida e política de privacidade correspondente. A página e este plano não liberam lançamento público nem substituem esses gates.
