# Estratégia de monetização Midiastore

**Pesquisa:** 6 nov. 2026. Valores variam por país, conta e promoções; esta revisão não obteve uma grade de preços regional reproduzível das telas de checkout de 15+ apps. Portanto não publica preço de concorrente nem inventa preço Brasil/internacional. A recomendação comercial inicial é estruturar produtos e conduzir teste regional via Play Console antes de fixar valores.

## Proposta

### Gratuito

- Painel de capacidade/uso do armazenamento compartilhado.
- Análise básica de fotos, vídeos e áudios com permissão e cobertura visíveis.
- Revisão individual de candidatos grandes/antigos; seleção manual.
- Exclusão e lixeira pelos fluxos do sistema quando suportado.
- Pós-operação com bytes realmente confirmados e status claro.
- Nenhum anúncio interstitial durante análise, confirmação ou operação protegida.

### Midiastore Plus (Premium)

- Grupos de duplicatas exatas com hash SHA-256 completo e seleção em lote controlada.
- Agrupamento visual de semelhantes, com revisão manual e limites explícitos.
- Filtros salvos, relatórios/histórico e tendências locais.
- Compressão de mídia com preview, controle de qualidade e original preservado.
- Reanálises agendadas opt-in quando o sistema permitir e com limites de bateria.

Premium não deve bloquear confirmação de limpeza essencial nem vender “liberação” que dependa de uma permissão inexistente. Antes do paywall, exibir o valor encontrado e permitir que o usuário examine um exemplo dos recursos pagos. Restaurar compras é obrigatório e fácil de achar. Compra pendente não libera benefícios; cancelar compra não vira erro de UX nem causa exclusão de dados.

## Modelo de cobrança

1. **Anual** como opção de melhor valor apenas quando o usuário já viu benefício avançado recorrente.
2. **Mensal** para reduzir compromisso e validar willingness-to-pay.
3. **Compra única** só para funções on-device que não gerem custo contínuo; não prometer serviços de nuvem permanentes nessa modalidade.
4. Evitar anúncios como financiador de operações de limpeza, por afetar confiança e depender disponibilidade/rede.

## Experimento de preço Brasil e mercados externos

Não fixar preços antes de obter:

- Lista de concorrentes com preço observado dentro da Play Store por Brasil, EUA, Reino Unido, Índia e pelo menos um mercado da UE, com moeda/data e captura da tela/conta de teste.
- Preço regional disponível no catálogo da Play Console, impostos e conversão aplicável.
- Entrevistas/usabilidade para saber qual ação premium resolve dor recorrente.
- Teste de preço e conversão sem dark patterns: mesma proposta/escopo, teste A/B permitido e cohort/accounting claros.
- Retenção, refund, cancelamento, suporte e custo de transcodificação/armazenamento.

O preço recomendado deve sair desse experimento; os listings públicos encontrados descrevem modelos (subscription/IAP/compra única) mas não oferecem valor regional estável nesta consulta. Não inferir faturamento/usuários a partir de ranking ou review.

## Implementação técnica e bloqueios

- O projeto usa Google Play Billing 8.3.0. Documentação oficial consultada em 6 nov. 2026 lista 9.1.0 como versão atual; Billing 8 tem prazo publicado até 31 ago. 2027, então migração pode ser planejada com teste.
- Consultar catálogo real, acompanhar conexão, atender compras pendentes e reconhecer compra; reconciliar em inicialização/retorno.
- Verificação antifraude comercial depende de Play Console e possivelmente backend com Google Play Developer API; o ambiente local não prova configuração de produto/entitlement de produção.
- IDs de produto, base plans/offerings, países, impostos, conta de teste, app signing e fluxo de release devem ser configurados pelo proprietário.

Fontes: [Billing integration](https://developer.android.com/google/play/billing/integrate), [Billing security](https://developer.android.com/google/play/billing/security), [Billing release notes](https://developer.android.com/google/play/billing/release-notes), [prazo de depreciação](https://developer.android.com/google/play/billing/deprecation-faq), consultadas 6 nov. 2026. Concorrentes e modelos estão em [competitor_analysis.md](competitor_analysis.md).
