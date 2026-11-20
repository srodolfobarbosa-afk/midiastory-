# Objetivo estratégico e memória operacional do Midiastore

**Versão:** 1.0 · **Atualizado:** 2026-11-20 · **Responsável pela direção:** Rodolfo  
**App:** Midiastore · **Package:** `com.rdf112.midiastore`

Este é o brief durável para qualquer agente, modelo ou automação que trabalhe no Midiastore. Ele registra o norte do negócio e como transformar esse norte em trabalho verificável. O modelo usado pode mudar; o objetivo, as decisões do proprietário, a verdade dos dados e as regras de segurança continuam valendo.

## Norte do negócio

Construir um limpador de armazenamento Android confiável, simples e competitivo, capaz de conquistar usuários, retê-los e gerar vendas reais sustentáveis pela Google Play. A ambição financeira de longo prazo é **R$ 1.000.000 em receita real**, medida por dados verificáveis da loja. É uma meta, não uma venda realizada, previsão garantida ou autorização para sacrificar qualidade e confiança.

O produto resolve uma dor concreta: ajudar a pessoa a entender o que ocupa espaço e liberar armazenamento com segurança. A identidade aprovada é um limpador puro, com as abas **Limpar, Arquivos e Pro**. Mudanças nessa direção precisam de aprovação do proprietário; documentos históricos que mencionem Studio, jogos, notícias, hubs genéricos ou outra identidade visual não reabrem decisões mais recentes.

## Como competir

1. **Ganhar confiança:** explicar o que será removido, pedir consentimento Android quando necessário, nunca apagar automaticamente e comprovar o estado depois da operação. Não chamar itens movidos para a Lixeira de espaço liberado.
2. **Reduzir atrito:** tornar a análise e a revisão fáceis; a ação de limpeza deve poder ser iniciada em até dois toques, preservando a escolha informada antes de excluir.
3. **Entregar valor comprovado:** priorizar bugs reproduzidos, cobertura de mídia honesta, desempenho, acessibilidade, localização, estabilidade e suporte nos aparelhos e versões Android que os usuários realmente usam.
4. **Monetizar com integridade:** oferecer Pro somente para benefícios implementados, com produtos e preços confirmados na Play Console, compra validada por backend confiável e estados de compra tratados corretamente.
5. **Encontrar usuários de forma sustentável:** melhorar ASO, páginas multilíngues, conteúdo útil, demonstrações verdadeiras, onboarding e suporte; medir cada etapa e concentrar esforço no canal com evidência de aquisição e retenção.
6. **Aprender do mercado:** acompanhar avaliações e solicitações dos usuários, concorrentes, buscas, conversão e falhas. Pesquisa informa decisões; não autoriza copiar marca, código, conteúdo ou práticas abusivas.

## Indicadores: medir antes de declarar avanço

Manter linha de base, período e fonte. Quando os dados não estiverem disponíveis, registrar **não medido** em vez de estimar.

- **Aquisição:** impressões da ficha, visitas, taxa de instalação, origem/país e custo por aquisição quando houver gasto.
- **Ativação e valor:** primeira análise concluída, candidatos revisados, limpeza confirmada pelo sistema e bytes permanentemente liberados e verificados.
- **Qualidade e confiança:** crashes/ANRs, falhas de provider/permissão, avaliações, reclamações e pedidos de suporte.
- **Retenção:** retorno D1/D7/D30 e uso repetido das funções principais.
- **Receita:** visualização do paywall, início e conclusão de compra, receita bruta e líquida, conversão, reembolso, cancelamento e renovação — com origem e janela de atribuição.
- **Eficiência:** custo de aquisição, infraestrutura e operação comparado à receita líquida. A regra 50/50 se aplica ao lucro líquido validado, não a faturamento bruto nem a valores imaginados.

Nenhuma métrica de memória, lead, clique, publicação de conteúdo ou CI equivale a venda. Só contabilizar receita confirmada pelas fontes oficiais e operações financeiras correspondentes.

## Mandato dos agentes

Os agentes devem trabalhar continuamente para melhorar o produto e vender o Midiastore: investigar problemas, comparar oportunidades, implementar melhorias reversíveis no repositório, preparar conteúdo e ASO, analisar funil, automatizar relatórios, propor experimentos e executar ações de crescimento em canais próprios/autorizados quando as permissões existentes cobrirem a ação. Não esperar uma ordem para cada passo técnico rotineiro.

Usar o modelo mais capaz que esteja realmente disponível no ambiente e dentro do orçamento autorizado. Manter o trabalho agnóstico de modelo: entregar a todo novo agente este brief, decisões atuais, estado verificado e próximo passo; testar capacidades novas antes de depender delas; nunca declarar que uma IA executou algo que só foi planejado.

Autonomia significa iniciativa com responsabilidade. Não inventar resultados, contornar políticas, enviar spam, usar dados pessoais fora da finalidade, assumir orçamento inexistente, prometer benefícios não implementados ou comprometer credenciais. Para ações externas, aplicar as autorizações do proprietário já registradas, as regras da plataforma e os controles de acesso. Se uma ação exigir conta, segredo, orçamento ou consentimento que não esteja autorizado/disponível, preparar tudo que for possível e registrar precisamente o bloqueio; não fingir execução.

## Ciclo de trabalho de cada agente

1. **Recuperar contexto:** ler esta estratégia, `AGENTS.md`, decisões de produto aprovadas, memória Supabase relevante, handoff e o snapshot mais recente. Consultar `git status` antes de editar e identificar trabalho concorrente.
2. **Atualizar fatos:** verificar branch/SHA, CI, versão publicada, métricas e estado de serviços em suas fontes atuais. Fato mutável precisa de fonte e horário; memória antiga não é estado atual.
3. **Escolher uma prioridade:** selecionar o maior ganho provável para confiança, ativação, retenção ou receita, ponderando evidência, impacto, esforço, custo e risco. Preferir corrigir uma falha comprovada a adicionar uma função sem demanda.
4. **Executar e comprovar:** concluir a parte autorizada, manter mudanças isoladas e reversíveis, e anexar prova proporcional (commit/SHA, relatório, métrica, captura ou resultado de operação). Separar **PASSOU**, **FALHOU**, **NÃO EXECUTADO** e **BLOQUEADO**.
5. **Registrar continuidade:** gravar resumo conciso na memória compartilhada e atualizar o handoff. Escrever o que mudou, evidência, limitações e próximo passo. Corrigir/superseder memória errada em vez de deixar versões conflitantes sem explicação.

## Padrão de memória para preservar inteligência entre sessões

Organizar conhecimento em quatro camadas e não misturá-las:

1. **Norte estável:** objetivo do negócio, definição do produto, valores e mandato dos agentes. Mudar somente por decisão explícita do proprietário.
2. **Decisões do proprietário:** escolha, data, escopo e decisões substituídas. A decisão mais recente e explícita prevalece; não perguntar novamente algo que já foi decidido.
3. **Estado operacional mutável:** branch, SHA, versão, CI, métricas, Play Console, dispositivos e serviços. Guardar fonte, data/hora, ambiente e validade; verificar novamente no próximo uso.
4. **Evidência e aprendizado:** tentativa, resultado, relatório/link, causa confirmada ou hipótese, correção, limitações e próximo passo. Hipótese nunca vira fato só por repetição.

Cada registro deve ser curto, pesquisável, sem segredos ou dumps desnecessários, ter categoria/confiança adequadas e apontar evidência quando houver. Evitar duplicar memórias; indicar qual registro foi supersedido. Em conflito, preservar a proveniência e sinalizar o conflito até a fonte mais confiável resolver. O resumo de sessão deve acabar com a próxima ação de maior impacto, não com uma lista sem prioridade.

## Regras de segurança e verdade

- Nenhum arquivo pessoal é apagado sem escolha e fluxo de consentimento oficial; validar cada URI e o resultado real.
- Não alegar cobertura completa, compra validada, receita, aprovação de loja ou teste em dispositivo sem a evidência correspondente.
- Não alterar package, certificado ou versionamento protegido, não fazer merge/publicação/upload nem gastar dinheiro sem autorização aplicável do proprietário.
- Cumprir leis, privacidade, políticas da Google Play, regras dos canais de marketing e direitos de terceiros.
- Qualidade, segurança e confiança são condições de crescimento e receita, não tarefas posteriores à venda.

## Fontes que este brief organiza

- `AGENTS.md`: regras de engenharia, autorização e direção visual atuais.
- `CONTEXTO_MESTRE.md`: contexto histórico/estratégico; trechos antigos que conflitem com decisões atuais devem ser tratados como superados.
- `docs/CONTINUATION_HANDOFF.md` e `docs/CONTINUATION_UPDATE_*.md`: estado operacional e continuidade.
- `public.memories`: decisões, preferências, correções e evidências duráveis; ler por tema com `node scripts/sync-agent-memory.mjs read "<tema>"`.
- `docs/FEATURE_IMPLEMENTATION_MATRIX.md` e `docs/RELEASE_BLOCKERS.md`: estado e gates das funções/release.

