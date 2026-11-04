# Market Intelligence 2027

**Consulta:** 03/11/2026. **Região/localização do recorte de reviews:** Google Play, `gl=US`, `hl=en`. Listing counts abaixo são snapshots observados, não previsões nem dados de receita.

## Concorrentes e listings

| Produto | Listing / desenvolvedor | Nota visível | `ratingCount` visível | Posicionamento observado |
|---|---|---:|---:|---|
| Files by Google | [Play](https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files&hl=en_US&gl=US), Google LLC | 4,51 | 9.104.853 | Gestão de arquivos e sugestões para liberar espaço; confiança e distribuição do Google |
| SD Maid 2/SE | [Play](https://play.google.com/store/apps/details?id=eu.darken.sdmse&hl=en_US&gl=US), darken | 4,43 | 11.112 | Limpador avançado com análise e recursos dependentes de acesso/API; comunidade técnica e opções pagas |
| CCleaner | [Play](https://play.google.com/store/apps/details?id=com.piriform.ccleaner&hl=en_US&gl=US), Piriform | 4,53 | 3.091.574 | Marca de limpeza conhecida, análise e ofertas premium |
| AVG Cleaner | [Play](https://play.google.com/store/apps/details?id=com.avg.cleaner&hl=en_US&gl=US), AVG Mobile | 4,58 | 1.976.966 | Limpeza/organização com assinatura e promoção de recursos premium |
| Avast Cleanup | [Play](https://play.google.com/store/apps/details?id=com.avast.android.cleaner&hl=en_US&gl=US), Avast Software | 4,59 | 1.523.806 | Otimização/limpeza e assinatura |
| Fossify Gallery (adjacente) | [GitHub](https://github.com/FossifyOrg/Gallery) | N/A | N/A | Galeria local, organização e edição; não é limpador generalista |

As páginas Play entregaram nome, desenvolvedor, nota e `ratingCount`. Não exibiram preço verificável de forma consistente nesta coleta; não inferimos preços, planos, conversão ou receita a partir de texto promocional. Os dados mudam e devem ser recapturados antes da precificação.

## Amostra de avaliações recentes

Método: 5 avaliações mais recentes retornadas por listing, en-US, ordenação NEWEST (valor 2 documentado pela biblioteca open source `google-play-scraper`), coletadas via endpoint web público do Google Play em 03/11/2026. Total: **25 reviews lidas, 5 por produto**, com datas entre 29/10 e 02/11/2026. Foram lidos texto, estrelas e data; os autores não são reproduzidos. A amostra não é aleatória, representa um idioma/região, contém textos muito curtos e pode ser manipulada/atualizada; não permite estimar prevalência nem comparar satisfação.

- Files by Google: quatro das cinco eram avaliações curtas positivas/genéricas; uma avaliação de 1 estrela descreveu erro de download/exclusão após abrir o app em segundo plano, em um dispositivo Lenovo. A reclamação é um sinal de confiabilidade a investigar, não uma taxa de falha.
- SD Maid 2/SE: a amostra incluiu um relato de falha do App Cleaner após atualização para Android 17, outro associado a Motorola Razr Ultra pedindo correção/reembolso, uma avaliação valorizando pagamento único e dois textos positivos curtos. Isso justifica matriz de aparelho/API; não prova incompatibilidade geral.
- CCleaner: cinco avaliações positivas; quatro eram quase sem detalhe. A amostra não permite concluir ausência de reclamações.
- AVG Cleaner: três avaliações positivas curtas e uma sobre preço/idioma; uma avaliação de 1 estrela relatou confusão entre oferta gratuita, assinatura mensal e data efetiva de cancelamento. É um exemplo individual de risco de clareza comercial.
- Avast Cleanup: duas avaliações positivas sobre facilidade/uso continuado; duas avaliações de 1 estrela relatam expectativa de muito mais espaço recuperado e insatisfação com o resultado; uma era irrelevante ao produto. Isso reforça a necessidade de mostrar bytes verificáveis e explicar limites.

URLs reproduzíveis das avaliações: mesmas URLs Play da tabela com `&showAllReviews=true`; usar ordenação “mais recentes”. A resposta capturada usa `https://play.google.com/_/PlayStoreUi/data/batchexecute` (RPC `oCPfdb`); esse endpoint não é API estável nem contrato suportado pelo Google.

## O que usuários precisam e onde há oportunidade

1. Saber o que ocupa o armazenamento e por que um item foi recomendado.
2. Distinguir espaço previsto de espaço efetivamente liberado e compreender o efeito da Lixeira.
3. Verificar conteúdo, similaridade e originais antes de uma ação irreversível.
4. Saber quais categorias/volumes estão cobertos por permissões completas ou parciais.
5. Ter preços, renovação, período, cancelamento e resultado Pro apresentados sem ambiguidade.
6. Receber tratamento consistente de falhas de provider, atualização de Android e OEM.

Limpeza genérica, listas de arquivos grandes e prompts de assinatura são áreas saturadas. Oportunidade menos comoditizada para este produto: um **ledger de evidências** que explique cobertura, método, seleção, consentimento e resultado confirmado, acompanhado de análise local e comparação visual revisável. A promessa deve permanecer estreita até haver medições reais.

## Matriz competitiva qualitativa

| Produto | Força aparente | Lacuna/oportunidade para Midiastore |
|---|---|---|
| Files by Google | Marca, distribuição, utilidade gratuita e gestão cotidiana | Diferenciar por auditoria detalhada, previsões transparentes e revisão de similaridade, sem copiar marca/UI |
| SD Maid 2/SE | Profundidade técnica, recursos avançados e comunidade de suporte | Evitar depender de root/ADB; desenhar UX compreensível e testar atualizações/OEM |
| CCleaner / AVG / Avast | Marcas reconhecidas, catálogo amplo e monetização estabelecida | Confiança: declarar exatamente o que a API permite limpar, sem claims de “RAM boost” ou espaço não confirmado; checkout/cancelamento claros |
| Fossify Gallery | Foco em privacidade e função de galeria | Integração entre visualização e diagnóstico de armazenamento, sem transformar o produto em suíte desconexa |

É uma leitura de posicionamento com base em listings/README, não uma auditoria interna dos aplicativos instalados. Instalação, UX em aparelho e teste de fluxos comerciais ficam pendentes.

## Implicações de monetização

A amostra sinaliza valor em pagamento único para alguns usuários e risco de confusão com assinatura para outros, mas 25 reviews não sustentam elasticidade de preço. Manter gratuidade para análise básica, prévia, consentimento e proteção de originais. Testar preço/região no Play Console e medir conversão, reembolso, renovação e churn em experimentos transparentes; não imprimir preço antes de receber `ProductDetails` da Play.

## Fontes adicionais

- [Google Files Help](https://support.google.com/files/)
- [SD Maid SE](https://github.com/d4rken-org/sdmaid-se)
- [CCleaner Android](https://www.ccleaner.com/ccleaner-android)
- [AVG Cleaner Android](https://www.avg.com/en-us/avg-cleaner-for-android)
- [Avast Cleanup Android](https://www.avast.com/cleanup-android)
- [Fossify Gallery](https://github.com/FossifyOrg/Gallery)
- [Fonte do scraper open source: reviews.js](https://github.com/facundoolano/google-play-scraper/blob/master/lib/reviews.js)

**Limitação:** o proprietário confirmou uma ficha de teste interno como `com.rdf112.midiastore`. A consulta pública antiga para `com.rdf112.sweep` não representa essa ficha. Este recorte de mercado não confirma o maior versionCode publicado, países de disponibilidade, vendas, preços ou estado atual do listing.