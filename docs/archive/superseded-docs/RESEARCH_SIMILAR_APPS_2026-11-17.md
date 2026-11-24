# Pesquisa de projetos semelhantes e evolução contínua

**Data:** 17/11/2026  
**Escopo:** GitHub e documentação oficial Android para limpadores, gestão de mídia e revisão segura. Pesquisa de referências; não é auditoria de qualidade nem autorização para copiar código.

## Referências observadas

| Projeto | O que oferece | O que pode ensinar ao Midiastore | Licença / limite de uso |
|---|---|---|---|
| [OpenSweep](https://github.com/dinesh-devkota/OpenSweep) | Limpador Android/Windows, revisão de candidatos, duplicatas, arquivos grandes e ações por caminhos oficiais do Android. | Separar análise, revisão e confirmação; mostrar cobertura e espaço recuperável; abrir configurações oficiais para controles que o app não pode executar diretamente. | O repositório declara MIT. Revisar o arquivo de licença e preservar avisos antes de reutilizar qualquer código. |
| [CleanPhotos](https://github.com/winterszhangdong/cleanphotos) | Revisão de uma foto por vez, favoritos, lixeira, restauração e desfazer; declara ausência de permissão de Internet/analytics e trata acesso parcial. | Fluxo de revisão focado, ação reversível e explicação de permissões simples. | A licença não foi confirmada nesta pesquisa; usar somente como referência de produto até confirmar os termos. |
| [CleanSweep](https://github.com/LoopOtto/cleansweep) | Organização por gestos, tutorial inicial, detecção exata e visual, cache local e tarefas em segundo plano. | Tutorial curto, desfazer e controles claros podem inspirar UX depois da correção da jornada atual. | GPL-3.0. Não copiar código para o app sem análise de compatibilidade com a distribuição do Midiastore. Claims de detecção visual no README não são evidência independente de precisão. |
| [SpaceClean](https://github.com/VarunKumarMup/SpaceClean) | Limpador offline com categorias, prévia, análise local e métodos exatos/visuais. | Bom exemplo para estudar como explicar uma prévia antes da limpeza. | Licença source-available que proíbe redistribuição; não reutilizar código, assets ou binários. |
| [SpaceSweep](https://github.com/Dhiva-Labs/SpaceSweep) | Scanner multi-categoria, arquivos grandes, junk, cache e duplicatas por conteúdo; descreve amostragem seguida de hash completo dos candidatos. | Exibir candidatos por benefício, manter um original e fazer revisão antes da ação. | Nenhuma licença reutilizável foi confirmada nesta pesquisa. O projeto menciona acesso amplo a arquivos; isso não deve ser copiado sem análise da política da Play. |

## Plataforma Android

A documentação oficial explica que, a partir do Android 14, o usuário pode conceder acesso apenas a fotos e vídeos selecionados. Apps que mantêm um seletor próprio precisam tratar `READ_MEDIA_VISUAL_USER_SELECTED` e atualizar a experiência para nova seleção; o Photo Picker é a alternativa recomendada quando o app só precisa que o usuário escolha itens. A cobertura parcial precisa ser explícita na UI e nos resultados. [Documentação Android sobre acesso parcial a fotos e vídeos](https://developer.android.com/about/versions/14/changes/partial-photo-video-access)

## O que aplicar ao Midiastore

1. **Consertar a jornada antes de adicionar inteligência:** testar primeiro acesso, botões, análise, revisão, cancelamento e confirmação no Moto G84. O handoff registra reclamações reais de botões e limpeza; isso tem prioridade sobre uma nova tela.
2. **Tornar cada ação compreensível e reversível quando a plataforma permitir:** prévia, item protegido, confirmação oficial, desfazer/restaurar e conferência do estado posterior. Não contar itens na Lixeira como espaço liberado.
3. **Mostrar cobertura com honestidade:** dizer se a análise foi completa, parcial, cancelada ou limitada. Permissão parcial não pode parecer varredura total.
4. **Defender uma promessa verificável:** manter SHA-256 para duplicatas exatas; não promover “fotos parecidas”, blur ou qualidade até existir implementação, conjunto de avaliação e taxa de falso positivo medida.
5. **Auditar a frase “nunca saem do celular”:** conferir permissões, dependências, analytics/crash reporting, exportação e chamadas de rede antes de manter uma promessa absoluta de privacidade.
6. **Preservar a arquitetura Views/Kotlin existente:** padrões de UX e segurança são transferíveis; migrar toolkit aumentaria risco e não resolve os defeitos de aceitação observados.
7. **Evoluir uma fraqueza de cada vez:** registrar defeito reproduzido, mudança, teste no Moto, resultado e decisão de release em cada atualização. Só então escolher a próxima melhoria funcional.

## Prioridades observadas nesta sessão

- Build Debug e AndroidTest compilaram a partir do commit local `440345f` (`2.7.3`, vc 28).
- No Moto G84 / Android 15, o relatório instrumentado registrou 9 casos, 0 falhas/erros e 4 skips. Consentimento de exclusão (API 29/API 30+) e acesso visual parcial seguem sem validação nessa execução.
- A `MainActivity` abriu. Uma captura em paisagem mostrou o texto da caixa de boas-vindas cortado pela área fixa do botão; repetir em retrato antes de corrigir layout/insets.
- O app instalado em produção é `2.5.0` / vc 22; o build de diagnóstico foi instalado com applicationId separado `com.rdf112.midiastore.debug`.
- O CI `43982604609` foi informado pelo Antigravity como verde, mas ainda falta confirmar SHA e relatório dos jobs.

## Regra para a próxima atualização

O próximo ciclo deve fechar defeitos reproduzidos e os gates P0 de MediaStore/Billing/Play antes de acrescentar novas promessas. Cada atualização precisa deixar evidência que um usuário consegue perceber: ação que responde, análise que explica seu alcance, exclusão segura e resultado conferido.
