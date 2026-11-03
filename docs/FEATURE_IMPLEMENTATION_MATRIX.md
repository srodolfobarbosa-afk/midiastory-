# Matriz de Implementação de Funcionalidades

Auditoria estática do source atual em 03/11/2026. A baseline Debug anterior passou, mas as alterações finais de strings/branding ainda não foram recompiladas; Release e execução em dispositivo não estão verificadas.

| Área | Estado | Evidência no código | Limite / diferença da promessa | Próxima validação |
|---|---|---|---|---|
| Identidade exibida | Implementada parcialmente | `app_name` e strings PT/EN mostram Midiastore | Pacote/namespace seguem Sweep legado; listing pública 404 não comprova status/identidade atual da Play | Recompilar; confirmar console/certificado com proprietário |
| Medidor de armazenamento | Implementada parcialmente | `MainActivity.refreshStorageStats()` usa `StatFs` em `/data` | Não é distribuição por categoria nem necessariamente o armazenamento total removível; sem histórico | Dispositivo com volumes diferentes; validar rótulo/denominador |
| Categorias de mídia | Implementada parcialmente | `AndroidMediaPageRepository` consulta Images/Video/Audio por volume | Apenas tipos MediaStore, uma categoria/volume/página por vez; não cobre Downloads/documentos genéricos | Verificar cobertura e permissões em API/OEM |
| Paginação MediaStore | Implementada parcialmente | limite 100; extras HonoredArgs; fallback consulta ordenada e pagina localmente; sequence guard | Fallback pode reler a biblioteca em offsets profundos; cursor/provider não forma snapshot transacional | Cursor real em provider AOSP/Samsung/Motorola/Xiaomi e biblioteca grande |
| Permissões | Implementada parcialmente | política para API 29–36; permissões granulares e selected access Android 14+ | Solicita as três categorias no mesmo fluxo; política de target API37 ainda não validada | Fluxos denied/partial/revoked por API, Photo Picker vs acesso amplo |
| Arquivos grandes | Implementada parcialmente | filtro `>=10 MB` em `SmartMediaGroupingPolicy` | Aplica à página visível, não à biblioteca completa; bytes são oportunidades, não automaticamente espaço recuperável | Cobrir todas as páginas/volumes e não duplicar contagem |
| Duplicatas SHA-256 exatas | Implementada parcialmente | hash streaming 8 KiB para candidatos de mesmo tamanho na página; protege referência; revalida antes de operação | Detecção limitada aos registros da página atual; não acha pares em páginas/volumes diferentes; custo de hash candidato não medido | Scanner global em lotes com persistência/retomada, testes de milhares de registros |
| Proteção do original | Implementada parcialmente | `SmartMediaGroupingPolicy` protege uma referência por grupo confirmado | Só para subgrupo encontrado na página atual; não garante grupo global | Teste entre páginas, arquivos mudando, original removido |
| Similaridade visual / dHash / pHash / SSIM | Ausente | Nenhuma implementação no source | Documento apenas recomenda avaliação; nenhuma classificação ou preview visual | Dataset local consentido, acurácia/falsos positivos e benchmark |
| Capturas de tela / qualidade de foto | Ausente | Nenhuma classificação detectada | Copy de Pro descreve funções que não existem | Implementar apenas após benchmark; nunca excluir automaticamente |
| Vídeos: duração/resolução/codec | Ausente/parcial | MIME/tamanho/data e miniatura estão disponíveis | Não há extração consistente de duração/resolução nem oportunidade de compressão | MediaMetadataRetriever em ContentResolver e teste de codec/OEM |
| Revisão/seleção | Implementada parcialmente | seleção local e preview/miniaturas; limite 500 URIs | seleção não agrega todas as páginas; não é uma sessão completa de biblioteca | Filtros, estado vazio, fonte grande, TalkBack e tablets |
| Lixeira e exclusão | Implementada parcialmente | API 30+ requests oficiais, revalidação e verificação posterior | API29 não executa; cancelamento/parcial/revalidação sem instrumentação; Lixeira não libera bytes imediatamente | Instrumented accept/cancel/partial, desaparecimento e reconciliação |
| Espaço efetivamente liberado | Implementada parcialmente | accounting registra exclusão só após aceite e URIs confirmados ausentes | `StatFs` é amostrado após operação e falha do storage pode atrasar atualização; sem comparação de delta real como prova extra | Validar variação StatFs e divergências provider/OEM |
| Histórico de varredura | Ausente | histórico de paginação é memória da sessão; ledger da Lixeira e telemetria são distintos | Não existe histórico persistente de análises por data/categoria | Storage History local com retenção/cota e teste de migração |
| Storage Forecast | Apenas documentada | requisitos em `MIDIASTORE_PRODUCT_STRATEGY.md` | Sem amostras, algoritmo ou UI | Não exibir até mínimo de três amostras em sete dias e validação |
| Gerenciador de arquivos SAF | Ausente | nenhum `DocumentsContract`, SAF ou picker de árvore | Sem Downloads/documentos, pesquisa/ordenação/renomeação/movimentação | Adicionar somente workflows via SAF selecionados e testar permissões |
| Automação/WorkManager | Ausente | nenhuma dependência/worker detectado | Não há alertas periódicos ou relatório semanal | Priorizar análise opt-in, nunca limpeza automática |
| Quatro abas | Implementadas parcialmente | seções HOME/FILES/OAAS/PRO conectadas via `switchTab()` | Presença e navegação não foram testadas em aparelho; conteúdo das abas usa dados de página atual | Instrumented navegação, telas pequenas, tablet/dobrável e TalkBack |
| Billing/SKUs | Implementado parcialmente | BillingClient consulta produtos e compras, inicia flow e reconcilia states | IDs/tipos não confirmados na Play Console; assinatura não é verificada criptograficamente; nenhum backend; compra real não testada | Confirmar console, validar token com API/back-end e testar ciclo completo |
| Exportação Pro | Implementada parcialmente | `ACTION_SEND` abre chooser para resumo da seleção | Resumo reflete seleção corrente, não análise completa; entitlement local inseguro para produção | Privacy disclosure e entitlement de confiança |
| Storage History | Ausente | nenhum modelo/repositório de snapshot | Não existe histórico gráfico/nem previsões | Implementar depois de baseline e cobertura de capacidade |
| Debug | Implementada e compilada na baseline anterior | APK gerado no caminho em `QUALITY_AND_TESTING.md` | Nome e strings de permissão foram alterados depois do build | Recompilar estado atual |
| Release/AAB | Bloqueada | task `bundleRelease` falhou com mensagem de assinatura ausente | Sem keystore legítimo; nunca gerar substituto | Configurar secrets com proprietário e verificar certificado |

## Claims a corrigir antes de marketing/Play

- Não anunciar “encontra duplicatas na biblioteca inteira” até buscar e reconciliar todas as páginas e volumes.
- “SHA-256” atual significa comparação completa entre candidatos da página; não é classificação visual.
- “storage real” atual é a leitura `StatFs` do diretório de dados, sem atribuição por categoria.
- Não anunciar detecção de screenshots, blur, baixa qualidade, metadados avançados de vídeo, organização de documentos, Storage Forecast ou automação: estão ausentes.
- “zero arquivos enviados” deve considerar o relatório compartilhado via `ACTION_SEND` e tráfego do Billing; atualizar política/UX e Data Safety com o binário final.
- “Pro ativo/verificado” não deve ser comunicado como validação segura até haver confiança de entitlement apropriada.
