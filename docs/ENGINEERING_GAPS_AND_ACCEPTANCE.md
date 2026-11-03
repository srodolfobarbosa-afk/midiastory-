# Lacunas de engenharia e critérios de aceite — Sweep v0.3

## P0 — gates de leitura e paginação

- [x] Contratos independentes de página e sessão no `:domain`.
- [x] Interface `MediaPageRepository` e adaptador Android com projeção explícita para imagens, vídeos e áudio.
- [x] Query estruturada com limite, offset e ordenação estável; estados pendente/na Lixeira filtrados localmente por flags explícitas (sem seleção SQL).
- [x] `CancellationSignal` ligado ao cancelamento da coroutine.
- [x] Verificação de argumentos honrados, `EXTRA_TOTAL_COUNT`, total desconhecido `-1` e limite real do cursor.
- [x] Revalidação de permissões, conjunto de volumes e versão/geração antes/depois da página.
- [x] Estado explícito para revisão não verificável e acesso parcial; sessões com acesso parcial são invalidadas ao retomar o app.
- [x] Guarda limitada de sequência de páginas; selection ledger por URI.
- [x] UI Views somente leitura, navegação incremental e revisão limitada.
- [ ] Executar JUnit/Gradle, Lint, APK e compilar APK instrumentado em ambiente Android.
- [ ] Testes instrumentados de query real e provider, por API/OEM.
- [ ] Teste de mudança do conjunto de imagens/vídeos concedido parcialmente no Android 14+ e de concessão/revogação independente de áudio; teste instrumentado inicial está escrito, mas não executado.
- [ ] Confirmar suporte real do provedor para limite, offset, ordenação e `EXTRA_HONORED_ARGS` em todos os dispositivos-alvo; validar também o desempate ID descendente documentado.
- [ ] Testes de biblioteca grande e perfil de CPU/memória/bateria.
- [ ] Testes TalkBack, contraste, escala de fonte, rotação e edge-to-edge.

## P0 — Billing e continuidade Pro

- [x] Política pura de mapeamento dos estados oficiais 0/1/2 e fail-closed para estados desconhecidos.
- [ ] Integrar BillingClient (versão 8 ou posterior conforme prazos vigentes quando publicar).
- [ ] Recuperar catálogo de produto real/allowlist da Play Console.
- [ ] Validar token e estado `PURCHASED`; tratar pending e consulta indisponível separadamente.
- [ ] Persistir entitlement idempotente e implementar acknowledgement/retry.
- [ ] Reconciliar compras em onResume/reconexão e restaurar acesso já comprado.
- [ ] Reimplementar exportação de resumo Pro sem quebrar quem já comprou.
- [ ] Decidir e implementar fonte de verdade/servidor de entitlement para assinatura, sem credenciais privadas no APK.

## P1 — operações seguras de limpeza (fatia futura)

- [ ] Revalidar cada URI selecionada imediatamente antes da operação.
- [ ] Implementar Lixeira com consentimento do Android; sem exclusão silenciosa.
- [ ] Recuperação parcial por URI; manter referência das operações falhas/não restauradas.
- [ ] Provar pelo estado retornado quais itens foram movidos/restaurados; nunca presumir sucesso do lote inteiro.
- [ ] Medir espaço usado depois da atualização do provider; não prometer espaço recuperado antes disso.

## P2 — produto depois dos gates

- [ ] Duplicatas exatas com orçamento de I/O e hashing cancelável após a camada de leitura.
- [ ] Fotos visualmente semelhantes (perceptual hash/embeddings local, separadas de hash exato).
- [ ] Documentos/Downloads através de SAF e escolha explícita de pasta.
- [ ] Tendência/histórico local, política de retenção e remoção do histórico.
- [ ] Compressão de mídia com prévia e preservação do original até confirmação.
- [ ] Billing Free/Pro, localizações e ASO depois de recuperar o comportamento Pro existente.

## Critério global

A presença de código ou testes não é aprovação. O critério é executar a suíte em ambiente reproduzível, em aparelhos representativos e na faixa de Android declarada, com logs vinculados ao mesmo ZIP/commit. A planta v1.9 define que a ação destrutiva fica bloqueada até os gates de paginação, seleção e revalidação serem comprovados.
