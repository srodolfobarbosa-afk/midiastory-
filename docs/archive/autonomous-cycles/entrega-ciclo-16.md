Corrigi a reserva de comandos em [fabrica-autonoma-24h.mjs](C:/Users/rodolfo/midiastory-/scripts/fabrica-autonoma-24h.mjs): o worker agora consulta apenas tarefas `CODEX`/`LOCAL_PC` e só processa a tarefa se a atualização condicional de `PENDING` para `IN_PROGRESS` retornar um registro.

A consulta à fila encontrou **zero tarefas** `PENDING` ou `IN_PROGRESS`. Registrei a auditoria na memória Supabase **#107** e na linha do tempo local.

Não executei o daemon, testes, deploy, commit ou push. Os bloqueios de API 29, CI, validação de compras e Play Console continuam sem evidência nova. A árvore já tinha alterações locais e a branch está 81 commits à frente da referência; preservei esse estado.
