# Midiastore 2027 — planta de produto e engenharia

**Direção aprovada pelo proprietário e recuperada da memória #313/#315 (16/11/2026).** Este documento corrige a proposta anterior que contrariou decisões já tomadas. O produto é um **limpador puro**. A planta e o mockup precedem qualquer alteração visual no app.

## O que estamos construindo

O Midiastore é um **limpador de armazenamento**, com fluxo principal de analisar → entender → revisar → escolher → confirmar. Nada de Studio, ferramentas genéricas, jogos, jornal, abas extras ou cartões fora do produto. Esse recorte vem da correção direta do fundador registrada em 16/11; não é uma nova decisão que precise ser pedida de novo.

### Experiência proposta

1. **Limpar:** visão de armazenamento com barra segmentada ao estilo iPhone Storage, ação “Analisar agora” e atalhos só para categorias do limpador.
2. **Arquivos:** filtros e resultados de fotos, vídeos e áudios; busca por grandes, duplicados, antigos e capturas de tela. Cada linha mostra por que foi encontrada e o tamanho.
3. **Pro:** benefícios ligados exclusivamente à limpeza, planos existentes e estado real de compra. Não inventar vantagens ou ofertas.
4. Resultados abrem um **bottom sheet de revisão** com seleção explícita, prévia em miniatura via cache limitado e confirmação Android. Depois, mostrar o resultado real.

### Direção visual proposta

- Identidade aprovada: preto OLED `#000000`, grafite `#1C1C1E`, azul Apple `#0A84FF`; zero emojis nos ícones de interface, zero cores neon decorativas.
- Acabamento inspirado em princípios de clareza e precisão da Apple, implementado como app Android nativo; não copiar telas iOS.
- Três abas exatamente: **Limpar**, **Arquivos**, **Pro**. No tablet/adaptive, transformar a mesma navegação em rail sem criar novas áreas de produto.
- Barra de armazenamento segmentada, bottom sheets bem acabados, miniaturas com `LruCache` limitado, tipografia sem cortes, targets confortáveis e estados completos.
- Prioridades de produto previamente registradas: corrigir os 3 microbugs observados nos screenshots v2.7.2, adicionar filtro de capturas antigas (>30d) e avaliar detecção local de fotos borradas por variância Laplaciana com métricas de falsos positivos e revisão humana.
- Acesso parcial, falha, cancelamento, vazio e sucesso são estados projetados; cobertura incompleta aparece claramente.

## Decisão tecnológica recomendada

| Camada | Decisão proposta | Razão |
|---|---|---|
| Linguagem | Kotlin | Mantém código Android nativo e reaproveita domínio e integrações existentes. |
| Interface | **Jetpack Compose + Material 3** como UI padrão; remover Views conforme cada fluxo for reconstruído | É o toolkit moderno recomendado pelo Android. Em maio de 2026, Android anunciou estratégia “Compose First” e colocou Views em manutenção. [Fonte Android](https://developer.android.com/blog/posts/android-ui-development-is-compose-first) |
| Navegação | **Navigation 3 estável 1.2.x**, usando versão estável disponível na implementação; não usar alpha para a navegação principal | Navigation 3 estável desde setembro de 2026 e recomendado pela arquitetura Android atual. [Recomendações](https://developer.android.com/topic/architecture/recommendations) · [versões](https://developer.android.com/jetpack/androidx/releases/navigation3) |
| Arquitetura | UI → ViewModel/state holder → use cases necessários → repositories → APIs Android/serviços | Estado unidirecional e fonte única de verdade tornam cada tela mais simples de testar. Sem criar camadas ou módulos só por moda. |
| Concorrência | Kotlin Coroutines + Flow; I/O, miniaturas e SHA-256 fora da main thread; cancelamento ligado ao ciclo de vida | Responsividade, progresso e cancelamento real. |
| Dados locais | DataStore para preferências. Room só quando introduzirmos histórico local pesquisável ou filas persistentes | Não criar banco para configurações simples; histórico pode ser útil, mas só registra resultados necessários e fica no aparelho. |
| Arquivos e mídia | MediaStore para descoberta; Android Photo Picker para seleção pontual; SAF para documentos/pastas quando o usuário pedir | O Photo Picker permite selecionar itens sem conceder acesso à biblioteca inteira. [Fonte Android](https://developer.android.com/training/data-storage/shared/photo-picker) |
| Trabalho longo | Execução em foreground/coroutines para análise interativa; WorkManager apenas para tarefa iniciada pelo usuário que realmente precise continuar após sair do app | Não agendar limpeza ou varredura silenciosa. |
| Pagamentos | Google Play Billing na versão estável suportada na data de implementação; validação de token no backend existente antes de liberar entitlement | Evita inventar preços ou liberar Pro a partir de sinal local não verificado. |
| Performance | Macrobenchmark + Baseline Profile para abertura, análise e revisão, se a comparação no Moto G84 mostrar ganho | Android recomenda compilar trajetos reais antecipadamente e medir no aparelho físico. [Fonte Android](https://developer.android.com/topic/performance/baselineprofiles/overview) |
| Dependências | Atualizar AGP/Gradle/Kotlin/Compose em conjunto, travar versões estáveis e testar antes de avançar cada grupo | Evita “upgrade geral” que quebra assinatura, build ou compatibilidade sem benefício medido. |

### Escolha de migração

**Reconstruir a interface do limpador em Compose; não reconstruir o produto como um hub.** Preservar MediaStore, SHA-256, consentimento e Billing que passarem pela validação. Migrar as três abas tela por tela, reduzindo `MainActivity` a host. A tecnologia da UI é nova; produto, identidade e âmbito continuam os que o fundador já aprovou.

## Arquitetura alvo

```text
app (Application, Activity host, DI/composition root)
├── core:designsystem (tokens, componentes, temas claro/escuro)
├── core:ui (estado comum, acessibilidade, previews)
├── core:domain (contratos e regras puras existentes)
├── core:data (MediaStore, preferências, histórico quando aprovado)
├── feature:home
├── feature:cleanup (análise, resultado, revisão e operação)
├── feature:files (filtros e resultados MediaStore)
└── feature:pro (catálogo e entitlement verificados)
```

Essa é a separação de destino, não uma ordem para criar oito módulos no primeiro commit. Primeiro extraímos design system e fluxo Compose dentro do módulo atual; dividimos módulos quando limites de build/equipe justificarem. Regras de negócio e contratos são extraídos antes da UI correspondente. A navegação é adaptativa por tamanho de janela. [Guia adaptativo Android](https://developer.android.com/develop/ui/compose/build-adaptive-apps)

## Plano de construção

### Etapa 1 — planta visual e mockup (antes de mexer na UI)

- Inventário só das três abas Limpar, Arquivos e Pro e seus fluxos do limpador.
- Desenhar Limpar, lista/filtros, sheet de revisão e Pro no Moto G84; criar variantes de fonte grande e janela adaptável.
- Aplicar a identidade já aprovada: OLED black, grafite e azul, ícones vetoriais sem emoji, acabamento limpo.
- Entregar HTML/mockup navegável e comparação com os screenshots v2.7.2 antes de implementar UI.

**Saída:** planta e mockup concretos que seguem a decisão existente do proprietário; corrigir até aderir, sem reabrir decisões já feitas.

### Etapa 2 — base moderna

- Criar tema/design system Compose e navegação; configurar previews, acessibilidade semântica e testes de UI.
- Fazer a Home nova consumir estado real do app sem mudar algoritmo, permissões nem operações de mídia.
- Reduzir a Activity a host e manter a interface antiga disponível apenas durante a migração.

**Saída:** APK navegável com nova Home; sem regressão nas operações existentes.

### Etapa 3 — produto principal completo

- Migrar análise, progresso, resultados, filtros, seleção, revisão, confirmação Android e resultado final.
- Consertar o P0 de ação destrutiva sem seleção antes que a nova jornada chegue ao usuário.
- Cobrir estados de acesso completo/parcial/negado, cancelamento, falha do provider, zero resultados e biblioteca grande.

**Saída:** jornada de limpeza nova operável no Moto G84 e em API 29+ de teste, sem ação surpresa.

### Etapa 4 — completar o limpador e Pro

- Completar filtros, capturas antigas e histórico/resumo estritamente relacionados a análise e limpeza.
- Avaliar fotos borradas como melhoria local opcional; calibrar com biblioteca de teste, medir falsos positivos, nunca pré-selecionar para exclusão.
- Corrigir Pro para conter somente vantagens do limpador já implementadas e verificadas; não carregar utilitários genéricos antigos para dentro dele.

**Saída:** controles visíveis correspondem a funções utilizáveis.

### Etapa 5 — qualidade, desempenho e distribuição

- TalkBack, escala de fonte, contraste, teclado, RTL, retrato/paisagem, janela pequena e tela grande.
- Medir cold start, navegação, jank, memória, bateria e análise antes/depois; adicionar Baseline Profile onde melhorar resultado real.
- Rodar testes no SHA exato, revisar política de dados e permissões com o binário, instalar o candidato e validar fluxos no Motorola.
- Só então preparar bundle assinado com identidade, certificado e versionCode corretos. Publicação requer sua ordem explícita.

**Saída:** candidato de atualização com evidência rastreável, sem publicar automaticamente.

## Critérios de aceite da nova versão

- A pessoa entende em poucos segundos quanto espaço está usado, o que o Midiastore pode analisar e qual botão inicia a tarefa.
- Nenhum botão promete ou sugere limpeza sem análise/revisão; operação destrutiva exige seleção explícita e confirmação Android.
- Resultado informa cobertura, progresso e o que realmente ocorreu; “mandado para lixeira” não aparece como espaço liberado.
- Cada controle abre a função descrita; onde a função não existe, não há botão falso.
- A interface nova usa Compose nas telas migradas, respeita tema do aparelho e funciona com fonte ampliada e TalkBack.
- A análise continua local; nomes/conteúdo/hash de mídia não vão para analytics, Supabase ou LLM.
- Desempenho medido no Moto G84 melhora ou se mantém sem regressões de crash/ANR, memória ou bateria.
- Compatibilidade Play, Billing, assinatura e identidade do app continuam preservadas.

## Aprovações para iniciar a implementação

Direção de produto já confirmada nas memórias #313 e #315:

1. Produto de limpeza puro; sem jogos, jornal, Studio, ferramentas genéricas ou extras nas abas.
2. Três abas: Limpar, Arquivos, Pro.
3. Preto OLED `#000000`, grafite `#1C1C1E`, azul `#0A84FF`; zero emojis e sem carnaval de cores.
4. Planta + mockup antes de código visual. A regra foi reiterada pelo fundador.
5. UI moderna Android em Compose + Material 3 e Navigation 3 estável é a proposta técnica desta migração; preservar as funções centrais e corrigir os microbugs do mockup aprovado.

O mockup visual que acompanha a planta é a próxima peça de revisão. A direção do produto não aguarda nova aprovação: ela já está registrada. Depois do mockup, a implementação começa pela Home/Limpar em Compose e segue para Arquivos e Pro, mantendo package, assinatura e versão atuais durante o desenvolvimento.

## Referências oficiais pesquisadas em 17/11/2026

- Android anuncia estratégia Compose First e Views em manutenção: https://developer.android.com/blog/posts/android-ui-development-is-compose-first
- Recomendações de arquitetura Android: https://developer.android.com/topic/architecture/recommendations
- Navigation 3 stable e notas de versão: https://developer.android.com/jetpack/androidx/releases/navigation3
- Compose para layouts adaptativos: https://developer.android.com/develop/ui/compose/build-adaptive-apps
- Android Photo Picker: https://developer.android.com/training/data-storage/shared/photo-picker
- Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
