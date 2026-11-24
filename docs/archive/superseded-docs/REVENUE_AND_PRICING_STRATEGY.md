# Revenue and Pricing Strategy

Data: 04/11/2026. Não há dados internos de conversão, churn ou custos por usuário. O proprietário informou que habilitou o perfil financeiro em 03/11/2026; catálogo de produtos, ativação, preços e vendas in-app ainda não foram verificados.

## Decisão confirmada pelo proprietário: Pro + Freemium (04/11/2026)

O produto é um limpador de celular com posicionamento premium. O plano Free deve entregar utilidade real: análise local das mídias acessíveis, arquivos grandes, cópias exatas, revisão manual e limpeza segura com confirmação do Android. Pro monetiza conveniência e relatórios, não segurança. No código atual, exportar o resumo da seleção revisada é o benefício pago implementado; histórico avançado, previsão e análise visual ainda não devem ser anunciados como disponíveis. Novas compras seguem pausadas até que a validação segura de purchaseToken esteja configurada e testada.

## Modelo inicial

| Free | Pro (hipótese a testar) |
|---|---|
| Medição `StatFs`, inspeção por categorias com permissões claras, paginação, seleção/preview, proteção de original, consentimento oficial, lixeira quando suportada e confirmação de resultado | Histórico local, análise avançada de biblioteca inteira, agrupamento visual revisável, relatórios/exportação, previsões depois de dados suficientes e alertas analíticos opt-in |

Segurança, explicação, revisão e consentimento não são vantagens Pro. Não cobrar para recuperar acesso ao arquivo, esconder cancelamento, alegar limpeza inexistente ou bloquear funções essenciais do sistema.

## Identidade e SKUs

O código contém `sweep_pro_monthly`, `sweep_pro_annual` (subscriptions) e `sweep_pro_lifetime` (in-app one-time). Manter IDs até acesso autorizado à Play Console confirmar status/tipos/benefícios. O proprietário confirmou a ficha de teste interno como `com.rdf112.midiastore`. O retorno 404 observado para o package legado `com.rdf112.sweep` não representa essa ficha. Status, preços, disponibilidade, países e direitos de compra existentes continuam **não verificados** até inspeção da Play Console. Não criar IDs novos nem inferir preço do nome do produto.

As páginas de concorrentes não expuseram preços transacionais verificáveis nesta coleta. Preços dependem de país, moeda, impostos, promoções, offers e base da Play. A UI deve renderizar apenas `ProductDetails` atualizado e localização oficial.

## Requisitos para Billing

- Rever Billing Library: build usa 8.0.0; Lint encontrou 9.1.0 disponível no repositório durante a baseline. Confirmar changelog e política mínima do Play, migrar numa alteração separada, executar testes compra/restore/ack/network/cancelamento e inspecionar AAB antes de escolher.
- Distinguir catálogo carregando, produto não encontrado, consulta indisponível e erro. Não inventar preço nem deixar `ProductDetails` antigo para produto que Play marcou `unfetched`.
- Validar token, pacote, product ID, estado, assinatura/compra, acknowledgement e reembolso/expiração usando fluxo de confiança suportado (preferencialmente backend + Google Play Developer API). Comparar custo de operação e privacidade antes de criar backend.
- Fazer acknowledgement só depois da compra apropriada estar validada; callbacks/reconciliações idempotentes.
- Compras pendentes não concedem Pro; falha de consulta não revoga silenciosamente direitos previamente verificados.
- Testar conta licenciada, licença de teste, países/maiorias, renovação, grace period, cancelamento e reembolso.

## Teste de preço/valor

1. Entrevistar e testar protótipos com usuários de diferentes regiões; não transformar cinco reviews em estimativa.
2. Validar o que gera valor antes de definir preço: relatórios recorrentes, análise visual, histórico ou exportação.
3. Rodar experimento transparente de preço/embalagem dentro das regras Play e sem dark patterns; registrar população/intervalo e não reduzir acesso gratuito de segurança.
4. Medir trial-to-paid se houver trial, conversão, retenção D30/D90, renovação, churn, reembolso, falha de cobrança, cancelamento e satisfação.
5. Calcular margem com taxas Play, impostos, suporte, backend, custo de IA e reembolsos antes de oferecer compra vitalícia. Lifetime requer escopo finito e risco de suporte/feature future documentados.

## Sinais de confiança

Preço/ciclo explícitos; data de renovação e cancelamento conforme Play; botão gerenciar assinatura; confirmação do entitlement; instrução simples de restauração; nenhuma urgência artificial. Avaliar relatar cancelamento confuso em avaliações recentes da AVG como sinal pontual, não como tendência comprovada.

**Status:** estratégia a validar. Sem acesso Play Console e sem dados de teste, não há proposta de preço numérica nem previsão de receita.