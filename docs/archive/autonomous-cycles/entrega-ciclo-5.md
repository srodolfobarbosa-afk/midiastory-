Iniciei o diagnóstico: li as instruções do projeto, os documentos de continuidade e os bloqueios de release, além das cinco memórias mais recentes de `public.memories`. Elas registram ciclos de conteúdo, mas não comprovam avanço nos 20 testadores.

O bloqueio concreto continua sendo validar o fluxo de exclusão no Android 29, obter CI verde no commit atual e confirmar produtos e compras no Play Console, incluindo validação de `purchaseToken` no backend. Os documentos também dizem para manter a PR como Draft e não publicar enquanto esses gates estiverem abertos.

**Encontrei um problema de segurança:** `scripts/registrar-memoria.mjs` contém uma credencial Supabase fixa no código. Não a usei nem a reproduzi. Recomendo revogá-la e removê-la; o registro de memória deve usar uma credencial fornecida pelo ambiente. Não fiz alterações, commits, push ou ações no Play Console nesta rodada.
