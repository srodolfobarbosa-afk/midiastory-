# Arquitetura técnica — Sweep v0.3 / especificação v1.9

## Princípio desta fatia

A planta v1.9 orienta primeiro integrar paginação real do MediaStore e uma interface que possa revisar páginas em modo somente leitura. Nenhuma operação de Lixeira ou exclusão fica exposta nesta fatia.

## Módulos e responsabilidades

### `:domain`

- `model/MediaDomain.kt`: `MediaRecord`, permissões e modelos independentes do Android.
- `model/MediaPagingDomain.kt`: `MediaPageRequest`, `MediaPage`, `MediaScanSession`, revisões por volume, estados de página e issues.
- `repository/MediaPageRepository.kt`: contrato para criar sessão, carregar página e reavaliar frescor.
- `paging/MediaPageSequenceGuard.kt`: guarda limitada de páginas observadas, para detectar URIs repetidas entre offsets e conteúdo alterado no mesmo offset.
- `selection/MediaSelectionLedger.kt`: seleção local limitada a 500 URIs, apenas para revisão. Não oferece método de apagar.
- `billing/BillingStatePolicy.kt`: política pura fail-closed dos estados do Play; ainda não é um cliente Billing.

### `:app`

- `data/media/AndroidMediaPageRepository.kt`: adaptador Android que enumera volumes, lê permissões e revisões, consulta MediaStore com paginação estruturada e cancellation signal.
- `presentation/scan/MediaScanViewModel.kt`: sessão, carregamento incremental, navegação, seleção por URI, cancelamento e guarda de consistência.
- `MainActivity.kt`: interface Android Views com estado de armazenamento, permissões, categorias de fotos/vídeos/áudio, volume, página, miniaturas e revisão local.

## Caminho de leitura

1. O usuário inicia uma sessão explicitamente.
2. O repositório captura os níveis de acesso (integral/parcial/negado), conjunto de volumes externos e marcadores `getVersion`/`getGeneration` disponíveis.
3. O usuário escolhe uma categoria (foto, vídeo ou áudio) e um volume. O app cria `MediaPageRequest` com 100 registros.
4. O adaptador envia uma projeção mínima (`_ID`, nome, tamanho, MIME e data de modificação), mais `IS_PENDING` e, desde API 30, `IS_TRASHED`. Envia `QUERY_ARG_LIMIT`, `QUERY_ARG_OFFSET` e ordenação por `DATE_MODIFIED` e `_ID`. Como `QUERY_ARG_SORT_DIRECTION` é um único valor para toda a lista de colunas, esta implementação usa ordem descendente para ambas (ID descendente como desempate), preservando uma ordem determinística estruturada; o estudo v1.8 sugeria ID ascendente, diferença registrada em `BASELINE_DECISIONS.md`. Os estados pendente/na Lixeira são filtrados localmente após a leitura da página; não há seleção SQL na consulta.
5. A consulta usa a sobrecarga de `ContentResolver.query` com `CancellationSignal`. Cancelamento do coroutine sinaliza o provider; `OperationCanceledException` é tratado como cancelamento, não como biblioteca vazia.
6. A resposta só é aceita como página quando o cursor confirma limite, offset e ordenação em `EXTRA_HONORED_ARGS`, fornece `EXTRA_TOTAL_COUNT` (contagem ou `-1` desconhecida) e não excede os limites nem contradiz a contagem. O total e os offsets representam linhas brutas do provider, inclusive linhas ocultadas localmente por `IS_PENDING`/`IS_TRASHED`; a UI identifica essa medida como registros do provider.
7. O app verifica novamente acesso, conjunto de volumes e revisões após a consulta. Se houver mudança, a página é descartada e a sessão invalidada. Se a revisão não puder ser verificada, a página é marcada como parcial/informativa.
8. `MediaPageSequenceGuard` compara URIs com páginas recentemente observadas e bloqueia sequências sobrepostas ou conteúdo alterado no mesmo offset.
9. A Activity renderiza apenas a página atual, com miniaturas concorrentes limitadas a quatro. A seleção é por URI e não dispara operação destrutiva.

## Regras de cobertura

- O acesso parcial do Android 14+ sempre produz `PARTIAL_ACCESS` na página. Ao retornar do background, uma sessão com acesso parcial é descartada, incluindo a seleção, pois o conjunto de URIs escolhidas não é um snapshot estável enumerável pelo app.
- A ausência de `getGeneration`, falha de versão ou marcador `unavailable` resulta em `UNVERIFIABLE`, nunca em `REVISION_UNCHANGED`.
- O provedor precisa honrar limite, offset, colunas/direção de ordenação; se não comprovar isso, o app marca paginação não suportada. Não há seleção SQL nesta fatia: as flags de estado são filtradas localmente dentro da página.
- `EXTRA_TOTAL_COUNT = -1` significa total desconhecido, não erro. Sem o campo, paginação não é comprovada.
- Linha inválida é ignorada com aviso; quantidade de linhas lidas continua contando os registros do cursor para o offset. Chaves URI de todas as linhas válidas são preservadas para a guarda de sobreposição mesmo quando a linha é filtrada por estado do sistema.
- Nenhuma mensagem afirma que o dispositivo está limpo, que há espaço recuperável ou que bytes já foram liberados.

## Limites atuais

- Fotos, vídeos e áudio em volumes conhecidos pelo MediaStore. Imagens/vídeos podem ter acesso parcial no Android 14+; áudio usa concessão granular independente no Android 13+.
- `minSdk 29`; Android 29 não oferece geração por volume pelo mesmo contrato, portanto a estabilidade é explicitamente não verificável.
- O acesso parcial não expõe à sessão um snapshot enumerável de todos os URIs concedidos; o sistema continua sendo a autoridade do acesso.
- O contrato do provider e os marcadores do MediaStore precisam de teste em Android 29, 30, 33, 34, 35 e 36 e em OEMs reais.
- A seleção é efêmera e limitada; ainda não existe persistência, restore da seleção ou revalidação destrutiva item a item.
- Sem BillingClient/entitlement/exportação Pro; código de política Billing é somente lógica pura.
- Sem lixeira, exclusão permanente, duplicatas, fotos semelhantes, documentos/Downloads, compressão ou tarefas em segundo plano.

## Gates antes de avançar para operações destrutivas

1. Build Android, JUnit e Lint sobre este commit.
2. Testes instrumentados do adaptador com `ContentResolver`/MediaStore em API 29, 30, 33, 34, 35 e 36.
3. Testes de `EXTRA_HONORED_ARGS`, `EXTRA_TOTAL_COUNT` conhecido/desconhecido/ausente, cursor nulo/erro, página curta, offset ignorado, sobreposição, mudança de geração, revogação e volume removido.
4. Teste de acesso integral, parcial e negado com mudanças de seleção no Android 14+.
5. Testes de usabilidade, TalkBack, escalonamento de fonte, rotação e memória com bibliotecas grandes.
6. Só então projetar e implementar a Lixeira e restauração com confirmação do sistema, revalidação e reconciliação item a item.
7. Separadamente, integrar Billing e entitlement com os product IDs reais e a preservação das compras Pro existentes.
