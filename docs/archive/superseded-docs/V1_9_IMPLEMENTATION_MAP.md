# Mapa de implementação da planta técnica v1.9

| Requisito da planta | Implementação nesta v0.3 | Estado real |
|---|---|---|
| Manter Android Views, evitar migração Compose desnecessária | `MainActivity.kt` segue em Views | Implementado no fonte; build Android não executado |
| Contrato de página com tamanho padrão 100 e teto 500 | `MediaPageRequest` no `:domain` | Implementado e compilação Kotlin pura validada |
| Sessão com permissões, volumes e revisões | `MediaScanSession`, `MediaSourceRevision` | Implementado; depende de MediaStore real/OEM para homologação |
| Consulta MediaStore incremental com limit/offset | `AndroidMediaPageRepository` para imagens, vídeos e áudio | Implementada estaticamente; não testada em provider real |
| Ordenação estável e provider args honored | sort por data + ID; verificação `EXTRA_HONORED_ARGS` | Implementado no adaptador; falta instrumentação por API/OEM |
| Total count conhecido/desconhecido | `EXTRA_TOTAL_COUNT`, com `-1` como desconhecido e offsets baseados em linhas brutas | Implementado; falta validar o comportamento dos providers |
| Cancelamento da query | `CancellationSignal` e cancelamento da coroutine | Implementado no fonte; falta teste real |
| Mudança de acesso/volume/geração | revalidação antes/depois da página; `UNVERIFIABLE` explícito | Implementado no fonte; falta testar em dispositivos |
| Paginação repetida/overlap | `MediaPageSequenceGuard`, janela limitada de oito páginas | Implementado e coberto por testes declarados |
| UI por páginas, sem guardar 50 mil linhas | `MediaScanViewModel`/`StateFlow`, 100 linhas por página | Implementado no fonte; falta build e teste UI |
| Seleção por URI, limite e revisão | `MediaSelectionLedger`, máximo 500 itens | Implementado; apenas revisão, sem exclusão |
| Não habilitar Lixeira antes dos gates de leitura | sem ação de Lixeira nesta fatia | Implementado por omissão |
| Billing estados oficiais e fail-closed | `BillingStatePolicy`, política pura e testes | Parcial: não conectado a BillingClient/Play |
| Restore/benefício Pro de exportar resumo | ainda não implementado | Bloqueador P0 |
| Testes instrumentados MediaStore em API 29/30/33/34/35/36 | fixtures de imagem, áudio e acesso visual parcial estão escritas; não executadas na matriz | Pendente |
| AAB assinado, certificado e Play Console | não gerados/validados | Pendente |

## Delimitação

O incremento é uma etapa de integração/paginação somente leitura alinhada ao próximo slice autorizado pela planta. Não é uma edição final de todas as funcionalidades descritas para o produto. Ver `RELEASE_BLOCKERS.md` e `VALIDATION_REPORT.md` para os gates abertos.
