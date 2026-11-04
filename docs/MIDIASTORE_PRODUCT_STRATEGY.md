# Midiastore: estratégia de produto 2027/2028

Data da revisão: 03/11/2026. A marca comercial e a ficha de teste interno confirmada pelo proprietário são **Midiastore**, package `com.rdf112.midiastore`. O namespace técnico e as pastas Kotlin permanecem `com.rdf112.sweep` como legado de implementação. Não alterar `applicationId`, versionCode ou assinatura sem verificar Play Console.

## Tese

Ser um **limpador inteligente de celular (phone cleaner) com posicionamento Pro**: explicar o que ocupa espaço, encontrar oportunidades verificáveis e deixar cada decisão destrutiva sob controle explícito da pessoa. A promessa não é “limpar tudo”; é mostrar evidência, impacto provável, cobertura e resultado confirmado.

## Usuário prioritário

Pessoas com armazenamento limitado, bibliotecas grandes de fotos/vídeos e pouca confiança em limpadores agressivos. Trabalho principal: entender o uso, localizar cópias exatas/itens grandes, revisar uma seleção e recuperar espaço sem perder um original.

## Diferenciação proposta

- Primeiro mostrar análise e cobertura; não afirmar que analisou todo o aparelho quando há acesso parcial, paginação incerta ou apenas uma página carregada.
- Distinguir espaço potencial, preparado, autorizado, na Lixeira e efetivamente liberado.
- Fornecer prévia, razão de recomendação, tamanho e tipo antes de cada ação.
- Processar hashes e análise visual localmente, com concorrência limitada e cancelamento.
- Manter o fluxo de seis etapas OaaS auditável e oferecer histórico local, sem telemetria de nomes/URIs.
- Dar à versão gratuita utilidade real e nenhuma exclusão automática.

## Decisões por etapa

1. **Estabilização:** compilar/testar Debug e Release, fechar Wrapper, verificar integridade e permissões, remover avisos de Lint com justificativa e validar exclusão em dispositivos.
2. **Núcleo:** biblioteca agregada por categorias, duplicatas SHA-256 abrangendo sessões/páginas com proteção de um original, revisão de mídias grandes/antigas e confirmação pós-operação.
3. **Diferenciais:** Storage Forecast local apenas após histórico suficiente; depois similaridade visual como revisão assistida, nunca identidade nem exclusão automática.
4. **Gestão de arquivos:** usar MediaStore para mídia e SAF para documentos/Downloads selecionados pelo usuário; não contornar scoped storage.
5. **Automação:** relatórios e alertas opt-in via WorkManager; nenhuma limpeza automática de conteúdo pessoal.

## Oportunidades pontuadas

Notas de 0 a 100 por critério; para segurança e manutenção, nota alta representa risco/custo menores. Total ponderado calculado com os pesos da missão.

| Oportunidade | Usuário 25% | Diferenciação 20% | Viabilidade 20% | Comercial 15% | Segurança 15% | Manutenção 5% | Total | Decisão |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| Storage Forecast local e explicável | 21 | 15 | 18 | 9 | 14 | 5 | 82 | Prioridade; MVP após baseline |
| Similaridade visual para revisão manual | 21 | 16 | 13 | 11 | 10 | 3 | 74 | Próxima etapa; protótipo medido |
| Gerenciador de arquivos SAF | 20 | 8 | 15 | 10 | 11 | 4 | 68 | Candidata; delimitar tipos/acesso |
| Backup em nuvem próprio | 20 | 7 | 7 | 12 | 7 | 1 | 54 | Backlog; custo, segurança e operação altos |
| Exclusão automática de fotos semelhantes | 18 | 10 | 14 | 8 | 0 | 2 | 0 | Rejeitada por risco de perda irreversível |

As notas são julgamento de produto, não medição de mercado. A pesquisa de concorrência e suas limitações está em [MARKET_INTELLIGENCE_2027.md](MARKET_INTELLIGENCE_2027.md).

## Storage Forecast: requisitos de produto

- Usar apenas amostras locais de `StatFs`, salvas no dispositivo e limitadas em quantidade.
- Registrar amostras quando o app abre; não inferir frequência diária quando o usuário não abriu o app.
- Não exibir previsão até haver pelo menos três amostras distribuídas por sete dias e capacidade comparável.
- Mostrar taxa observada, intervalo e premissa; interromper previsão se houver queda de uso, troca de volume/capacidade ou dados insuficientes.
- Sem rede, conta, analytics remoto ou exclusão automatizada.
- Métrica de sucesso: proporção de previsões que permanecem dentro do intervalo publicado após 14/30 dias; se não houver validação, manter o recurso como experimental.

## Monetização — decisão confirmada: Freemium

O plano gratuito mantém análise de fotos, vídeos e áudios acessíveis, localização de arquivos grandes e cópias exatas, revisão manual, proteção do original e limpeza com consentimento oficial do Android. O benefício Pro já presente no código é exportar o resumo da seleção revisada. Histórico avançado, previsão de armazenamento e agrupamento visual permanecem no roadmap, não como funcionalidades disponíveis. Nunca cobrar por segurança, proteção do original, revisão ou consentimento. SKUs e estado do Billing estão detalhados em [REVENUE_AND_PRICING_STRATEGY.md](REVENUE_AND_PRICING_STRATEGY.md).

## Limite de identidade

O proprietário mostrou o listing de teste interno `Midiastore — com.rdf112.midiastore`. A consulta pública de `com.rdf112.sweep` não determina o estado desse listing. Maior versionCode publicado e certificado de upload ainda precisam ser conferidos na Play Console. O código histórico do AAB de produção não foi recuperado.

## Direção do primeiro lançamento — Phone Cleaner + Pro Freemium

A identidade para o consumidor é **limpador inteligente de celular (Phone Cleaner)**. “Gerenciamento de armazenamento” é uma capacidade interna. A promessa é encontrar o que ocupa espaço, identificar o que pode ser removido, proteger o original e confirmar o espaço efetivamente recuperado.

O primeiro lançamento não será um gerenciador universal, antivírus, RAM booster, CPU cooler, limpador de cache privado de outros apps nem coleção de ferramentas. Downloads/documentos via SAF, Storage Forecast, similaridade visual e automação são extensões futuras, condicionadas à estabilização e à validação de demanda.

O modelo Freemium mantém o núcleo seguro gratuito. Pro deve monetizar conveniência e relatórios; o benefício pago já presente no cliente é exportação do resumo revisado. Compra permanece bloqueada até validação confiável de purchaseToken e confirmação do catálogo/offerings na Play Console.
