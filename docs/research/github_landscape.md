# Ecossistema público Android: cleaners, mídia e armazenamento

**Consulta:** 6 nov. 2026. Este é um levantamento de repositórios e alegações de README/releases, não uma validação de cada build. Licença e estado devem ser rechecados no commit que se pretende usar. Não copiar código/modelos sem revisar licença, copyright, NOTICE e dependências transitivas.

| Repositório | Arquitetura/tecnologia e utilidade | Atividade, testes e compatibilidade observados | Licença / cautela |
|---|---|---|---|
| [d4rken-org/sdmaid-se](https://github.com/d4rken-org/sdmaid-se) | Kotlin modular, MediaStore/SAF, dedupe e Media Squeeze; referência mais próxima do produto. | Release `v2.2.2-rc0` 19 out. 2026; changelog corrige seleção insegura, bytes falsos, cancelamento e OEM; estudar testes/cancelamento. | GPL-3.0; marca, docs, traduções e assets excluídos; não copiar sem revisão de copyleft e branding. |
| [Dhiva-Labs/SpaceSweep](https://github.com/Dhiva-Labs/SpaceSweep) | Flutter + bridge Kotlin; agrupa tamanho, hash de amostra e SHA-256 completo, scanner cancelável. | README declara testes e API26+, mas a compatibilidade não foi executada nesta auditoria. | Licença não confirmada; pede permissões de amplo acesso/visibilidade/uso, alto risco Play. |
| [rehaan212006/StorageLens](https://github.com/rehaan212006/StorageLens) | Endereço obrigatório. | Não foi possível confirmar repositório público sob esse caminho. | Não atribuir licença ou recursos; não confundir com Storage-Lens abaixo. |
| [aiyu-ayaan/Findex](https://github.com/aiyu-ayaan/Findex) | Endereço obrigatório. | Não foi possível confirmar app Android correspondente. | Não atribuir licença/tecnologia. |
| [daetz-coder/Storage-Lens](https://github.com/daetz-coder/Storage-Lens) | Flutter, inventário local; explicita cobertura e lacunas sem all-files. | README informa v0.5.2, API 29+ recomendada, grátis; verificar os testes no checkout. | MIT; licença mais permissiva, mas código requer auditoria própria. Não é o StorageLens do endereço acima. |
| [winterszhangdong/cleanphotos](https://github.com/winterszhangdong/cleanphotos) | Compose/Kotlin, MediaStore, revisão por dia, undo/trash e pedido oficial de exclusão. | README declara API26+, permissão parcial Android14, testes JVM e instrumentados. | Apache-2.0; boa referência de UX/safety, sem pressupor qualidade sem build próprio. |
| [KrunalWankhade9021/ImageSense](https://github.com/KrunalWankhade9021/ImageSense) | ONNX/CLIP quantizado, Room, indexação batch retomável e WorkManager; busca semântica. | Testes de paridade/modelo relatados no README; medir cold-start, memória e bateria. | Licença não confirmada nesta consulta; modelo/tokenizer têm licenças próprias. |
| [MZarc/Clearo](https://github.com/MZarc/Clearo) | Flutter/Kotlin, MediaStore, dHash e lixeira app-private criptografada alegada. | Projeto jovem; histórico curto e cobertura independente limitada. | MIT conforme README; confirmar LICENSE real e criptografia antes de reutilizar. |
| [diaoyunxi/cleaner](https://github.com/diaoyunxi/cleaner) | Kotlin, dedupe por tamanho → XXH64 parcial → SHA-256 total e confirmação manual. | README declara Android API29–35; validar código/testes e custo de I/O. | MIT; requer MANAGE_EXTERNAL_STORAGE, risco de política Play. |
| [aminechraibi/Storage-Cleaner](https://github.com/aminechraibi/Storage-Cleaner) | Compose/Kotlin, cleaner/duplicados. | README tem caminho placeholder e somente poucos commits; maturidade baixa. | Apache-2.0 declarado, conferir arquivo LICENSE. |
| [android/storage-samples](https://github.com/android/storage-samples) | Amostras oficiais de MediaStore, SAF, scoped storage e paginação. | Arquivado/depreciado em 29 ago. 2025; referência histórica, não dependência atual. | Apache-2.0; documentação Android vigente prevalece. |
| [zhanghai/MaterialFiles](https://github.com/zhanghai/MaterialFiles) | File manager Kotlin/Material, SAF e árvore de documentos. | Projeto de referência de navegação e provider; verificar release/CI atual antes de adoção. | GPL-3.0; obrigações de distribuição de derivados. |
| [deckerst/aves](https://github.com/deckerst/aves) | Galeria Flutter, metadados e formatos de mídia. | Histórico amplo; referência de UX de biblioteca, não cleaner. | BSD-3-Clause. |
| [coil-kt/coil](https://github.com/coil-kt/coil) | Loader de imagem Android, caching, downsample e cancelamento. | Atividade observada até 30 out. 2026; benchmark e API devem ser testados com as versões Gradle do Midiastore. | Apache-2.0; dependência só se thumbnail própria justificar. |
| [androidx/media](https://github.com/androidx/media) | Media3 Transformer para edição/transcodificação de vídeo. | Fonte oficial/demo; medir codecs OEM, espaço temporário, cancelamento e energia. | Rever licença e versão do módulo antes de integrar. |
| [tensorflow/examples](https://github.com/tensorflow/examples) | Exemplo Android de classificação on-device. | Referência de integração; não prova precisão para blur/duplicidade. | Licença do repo e do modelo precisa confirmação separada. |
| [tanvirr007/quarry-app](https://github.com/tanvirr007/quarry-app) | Analisador com treemap, SAF/MediaStore e módulos de duplicados descritos. | Release/testes/licença não confirmados neste levantamento; conferir o código. | Licença não confirmada; leitura apenas. |
| [PingWangWang/MediaCleaner](https://github.com/PingWangWang/MediaCleaner) | Pipeline descrita de dHash, LSH, Union-Find, pHash e ORB. | README em chinês declara MVVM/Clean; qualidade/precisão não verificada. | Licença e releases não confirmadas. |
| [EngShabban/DedupeX](https://github.com/EngShabban/DedupeX) | MediaStore e casos de uso Kotlin relatados. | Testes/release/licença não confirmados. | Não reutilizar até revisão. |
| [VarunKumarMup/SpaceClean](https://github.com/VarunKumarMup/SpaceClean) | Compose, MediaStore Trash e modelos locais descritos. | README traz limites de redistribuição/publicação; uso apenas para estudo conceitual. | Source-available, não permite distribuir/publicar derivados. |
| [Kiowx/Deep_Cleaner](https://github.com/Kiowx/Deep_Cleaner) | Cleaner, análise e duplicados SHA-256 descritos. | README declara Android 8–16; verificar build, licença e testes. | Licença não confirmada. |
| [gitubpatrice/APP-MANAGER-TECH](https://github.com/gitubpatrice/APP-MANAGER-TECH) | Kotlin/Room/DataStore e operações por consentimento/SAF descritas. | Distribuição F-Droid alegada; testar implementação real. | Licença não confirmada. |
| [norsehorse-dev/ScrubPonyAndroid](https://github.com/norsehorse-dev/ScrubPonyAndroid) | URI picker, gravação MediaStore e preservação original. | Android 10+ conforme F-Droid; teste e compatibilidade a conferir. | Apache-2.0 segundo F-Droid; verificar repo. |

Foram encontrados 20+ repositórios/projetos distintos ligados ao problema; alguns são amostras/componentes e vários têm licença/qualidade inconclusivas. Uma busca semântica precisa ser repetida quando os dois endereços obrigatórios ficarem publicamente verificáveis.

## Decisões que se aplicam ao Midiastore

- Hash parcial apenas reduz candidatos; igualdade exige SHA-256 integral e leitura completa do arquivo.
- Agrupamento visual é sugestão; precisa de recall/precision medidos, revisão humana e reversão viável.
- Acesso amplo em SpaceSweep/DiskUsage aumenta capacidade, mas `MANAGE_EXTERNAL_STORAGE` é restrito no Play. Priorizar MediaStore, Photo Picker e SAF explícito.
- Revalidar URI e estado imediatamente antes de apagar; usar pedido de consentimento oficial; conferir pós-operação.
- Cache de thumbnails deve ser limitado e cancelável. Nunca indexar conteúdo em segundo plano sem demanda/consentimento.
- Não importar GPL-3.0 para o app comercial proprietário sem decisão jurídica explícita; não usar SpaceClean para distribuição derivada.

Licenças/fontes: links do próprio repositório nesta tabela; consulta e páginas de releases em 6 nov. 2026. As declarações de recursos são dos projetos, salvo onde indicado como verificação local.
