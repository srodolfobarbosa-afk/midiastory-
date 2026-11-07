# Midiastore: visão de produto 2027

**Responsável:** Rodolfo Barbosa
**Levantamento:** 6 nov. 2026
**Estado:** direção de produto; recursos não listados como existentes são propostas, não funcionalidades entregues.

## Promessa

O Midiastore ajuda a pessoa a entender o armazenamento compartilhado do telefone, revisar candidatos com contexto e executar operações que o Android autoriza. A tela inicial deve responder, sem exagero: quanto o armazenamento principal tem, quanto está usado, quais categorias o app conseguiu analisar, o que pode ser recuperado e o que ainda exige revisão ou permissão.

O produto não promete acelerar CPU, liberar RAM, resfriar o aparelho, apagar caches privados de outros apps nem medir “saúde” por percentuais inventados. Só contabiliza bytes como liberados depois de confirmar a ausência do item no provider; mover para a Lixeira não é espaço recuperado.

## Princípios de experiência

1. **Analisar → revisar → confirmar → verificar.** O app mostra origem, tipo, tamanho, data, cobertura da análise e consequência antes da operação.
2. **Seguro por padrão.** Fotos, vídeos, documentos, APKs e downloads pessoais começam desmarcados. Sem seleção automática para grupos visualmente semelhantes.
3. **Cobertura honesta.** Acesso parcial, volumes ausentes, resultados não lidos e operações não verificadas aparecem como limites, não como biblioteca completa.
4. **Uma tarefa principal por tela.** A ação principal da Home conduz a uma análise real e retorna um resultado verificável; funções indisponíveis explicam a alternativa oficial.
5. **Privacidade local primeiro.** Índices e hashes permanecem locais por padrão; telemetria opcional deve ser agregada, consentida e sem nomes, caminhos ou conteúdo.
6. **Baixo custo.** Paginação cancelável, thumbnails reduzidas, hashes só para candidatos de mesmo tamanho e processamento pesado solicitado pelo usuário.

## Hierarquia da Home

1. Armazenamento principal usado / total / livre, com fonte e horário da medição.
2. Espaço candidato na sessão atual, separado de bytes em revisão, autorizados e efetivamente liberados.
3. Maior categoria e maiores itens que o app conseguiu consultar.
4. Ação primária “Analisar armazenamento”.
5. Atalhos secundários para fotos, vídeos, áudio e Lixeira; nenhum atalho deve iniciar exclusão sem revisão.

O screenshot do moto g84 (Android 15, API 35, 6 nov. 2026) confirma que há painel de armazenamento, botão principal e quatro abas. Ele também mostra uma frase genérica de “proteção de referência original”, contadores em zero antes da análise e um CTA circular que domina a dobra. A próxima iteração deve substituir a frase por linguagem verificável, distinguir “sem análise” de “0 B” e medir legibilidade em telas menores, escala de fonte e tema claro. Evidência local: `app/build/outputs/androidTest-results/manual-adb/moto_g84_api35_home.png`.

## Escopo de produto e critérios

| Prioridade | Capacidade | Critério de aceite |
|---|---|---|
| P0 | Paginação, permissões, exclusão/restore e Billing sem perda de dados | Sessões inválidas são descartadas claramente; operações protegidas usam consentimento Android; resultado é reconciliado antes de contabilizar bytes. |
| P1 | Painel, análise de mídia e revisão simples | O app informa cobertura real, estados vazios/erro/permissão e apresenta ação segura por item. |
| P2 | Duplicados e semelhantes | SHA-256 integral confirma duplicado exato; similaridade só sugere grupos, com revisão visual, limiar e teste de falsos positivos. |
| P3 | Compressão, histórico avançado e automação opcional | Preview, preservação do original, cancelamento, consumo mensurado e consentimento explícito. |

## Funções versus limites da plataforma

| Função proposta | Impacto / viabilidade | Energia | Acesso / risco Play | Decisão |
|---|---|---|---|---|
| Painel de armazenamento e categorias | Alto / alta para armazenamento compartilhado; categorias do Android podem incluir dados inacessíveis | Baixo | APIs públicas; risco baixo se a cobertura for descrita | P1, medir bytes e explicitar desconhecidos |
| Mídia grande/antiga | Alto / alta com MediaStore | Baixo a médio | Permissões granulares e acesso parcial; baixo risco | P1, nunca apagar sem confirmação |
| Duplicados exatos | Médio-alto / alta; SHA-256 integral caro em vídeos grandes | Médio-alto | URIs MediaStore, autorização para apagar item alheio | P2, candidatos por tamanho, hash em fluxo cancelável |
| Fotos semelhantes/baixa qualidade | Médio / média; limiar e precisão dependem da biblioteca/dispositivo | Médio-alto | Acesso de leitura de imagens; risco de falsos positivos | P2, manter revisão manual, nada pré-selecionado |
| Pastas vazias/resíduos de app | Médio / baixa sem acesso amplo; MediaStore não expõe toda árvore | Médio | SAF por árvore escolhida; `MANAGE_EXTERNAL_STORAGE` tem restrição de política | Não prometer varredura universal; avaliar SAF opt-in |
| Cache de outros apps / boost RAM | Baixo benefício / não permitido por APIs ordinárias | — | Privado do app; promessa enganosa | Fora de escopo |
| Vídeo comprimido | Médio-alto / média | Alto e possivelmente longo | Origem URI autorizada; permissões de escrita/consentimento | P3, transcodificação local, preview e original preservado |
| Lixeira | Alto / média | Baixo | MediaStore trash + aprovação do sistema; restore sujeito a disponibilidade/expiração | P1, não contar bytes até exclusão permanente confirmada |
| Histórico de operação | Médio / alta | Baixo | Armazenamento app-private; política de retenção e exportação | P3, minimização de dados |

Fontes Android consultadas em 6 nov. 2026: [acesso compartilhado a mídia](https://developer.android.com/training/data-storage/shared/media), [acesso parcial Android 14](https://developer.android.com/about/versions/14/changes/partial-photo-video-access), [SAF](https://developer.android.com/training/data-storage/shared/documents-files), [política de All files access](https://support.google.com/googleplay/android-developer/answer/10467955).

## Métricas de qualidade (a instrumentar com consentimento)

- Tempo de análise por 1.000 itens e por volume; cancelamento responde em tempo previsível.
- Proporção de páginas completas, parciais, stale, falhas de provider e volumes não acessíveis.
- Bytes candidatos, bytes autorizados, bytes verificados como removidos e divergência pós-operação.
- Taxa de undo/restore, cancelamentos do consentimento do sistema e falhas de reconciliação.
- Memória de pico, uso de CPU e consumo por 1.000 thumbnails/hashes em aparelhos de referência.
- Erros/crashes e acessibilidade: TalkBack, fonte ampliada, contraste, foco e alvos táteis.

Não coletar nomes/caminhos de arquivo, thumbnails ou hashes fora do aparelho para medir essas métricas.
