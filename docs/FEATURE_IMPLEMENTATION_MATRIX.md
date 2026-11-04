# Matriz de Implementação de Funcionalidades — Midiastore

Auditoria da branch `fix/p0-sweep-permissions-missions-billing`, revisada em 04/11/2026. Esta matriz distingue código presente, testes executados e validação real em aparelho. Uma implementação no source não significa que o recurso foi aprovado para produção.

## Identidade e release

| Área | Estado | Evidência atual | Pendência / limite | Próxima validação |
|---|---|---|---|---|
| Marca / listing | Atualizada na branch | `app_name=Midiastore`, textos PT/EN e marca M na barra superior | Código só foi alterado nesta branch; ainda precisa recompilar e confirmar visual em aparelho | Screenshots reais em telefone pequeno e fonte ampliada |
| Application ID | Confirmado pelo proprietário | Listing de teste interno fornecida: `com.rdf112.midiastore`; build.gradle usa mesmo ID | Maior versionCode e upload certificate não foram conferidos na Play Console | Confirmar versionCode máximo e certificado |
| Namespace interno | Legado, preservado | `namespace=com.rdf112.sweep` | Nomes de pacote, cores e classes continuam Sweep; isso não muda applicationId | Manter compatibilidade ou planejar refactor separado |
| Versionamento | Configuração atual | `versionCode=17`, `versionName=2.2.1` | São valores do source; não provam valor efetivamente publicado | Consultar Play Console |
| Debug / unit / Lint | Aguardando confirmação do último HEAD | CI executa `:domain:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` e `:app:assembleDebugAndroidTest` | Houve falhas de teste em commits intermediários; os workflows mais recentes estão sendo reexecutados | Verificar o commit mais recente no CI |
| Instrumentação Android | Não aprovada | Workflow executa `:app:connectedDebugAndroidTest` em emulador Android 35 e salva relatórios | O primeiro teste MediaStore falhou no emulador API35; causa ainda não está diagnosticada; no PC do proprietário não havia dispositivo conectado | Ler XML/JUnit mais recente, corrigir e repetir |

## Leitura e cobertura

| Área | Estado | Evidência atual | Pendência / limite | Próxima validação |
|---|---|---|---|---|
| Fotos, vídeos e áudio | Implementado | `AndroidMediaPageRepository` consulta collections MediaStore por tipo e volume | Não cobre arbitrariamente qualquer documento/Downloads | Testar permissões, volume interno e SD em aparelhos |
| Permissões granulares | Implementado parcialmente | Política Android 29–36; IMAGE/VIDEO/AUDIO; selected visual access Android 14+ | A solicitação inicial pede os três tipos; Android target 37 e possíveis mudanças futuras não foram validados | Testar denied/full/partial/revoked por API |
| Gerenciar acesso parcial | Corrigido na branch | Ação de rever permissões aparece quando há acesso parcial e também na aba Arquivos | Ainda não validado manualmente em Pixel/Samsung | Teste Android 14+ selecionando e ampliando fotos/vídeos |
| Paginação | Implementado, em validação | Página limitada a 100; args HonoredArgs, offset, fallback controlado, sequência guard | Alguns providers podem não honrar offset/sort; paginação não forma snapshot transacional | Testar AOSP, Samsung, Motorola, Xiaomi e biblioteca grande |
| Revisão da sessão | Implementado parcialmente | Permissões, volumes e revision markers são revalidados | Android/API29 ou provider sem generation pode resultar em UNVERIFIABLE | Validar alteração de mídia e revogação durante a leitura |
| Cancelamento | Implementado | CancellationSignal no MediaStore e cancelamento de coroutine no hash streaming/global scan | Precisa de teste real com biblioteca grande e no meio de hash | Cancelar após 1, 10 e 100 páginas |

## Missões globais

| Área | Estado | Evidência atual | Pendência / limite | Próxima validação |
|---|---|---|---|---|
| Scanner global | Implementado na branch, ainda não aprovado por CI | `MediaScanViewModel.executeGlobalOutcomeMission()` percorre tipos acessíveis, volumes e páginas | Limites de segurança: 250.000 linhas, 20.000 candidatos duplicados, 100.000 tamanhos distintos, 2 GiB por arquivo para hash global e 10 GiB de I/O de hash por missão; atingir limite significa parcial | CI e testes de volume alto; perfil de memória/bateria |
| Progresso da missão | Implementado na branch | Contadores de páginas, registros e fontes são expostos via `GlobalMissionProgress` | UI precisa de teste de layout, cancelamento, acessibilidade e estados vazios | Verificar missão completa, parcial, cancelada e falha |
| Arquivos grandes | Implementado globalmente na branch | Limite >=10 MB; mantém até 500 candidatos de maior tamanho por missão | Resultado não significa espaço automaticamente recuperável; exige revisão | Dataset de arquivos de tamanhos variados e comparação da ordem |
| Mídias antigas | Implementado globalmente na branch | Filtro >=30 dias sobre todos os pages acessíveis | Data de modificação pode estar ausente; resultado então não é elegível | Testar datas ausentes e limites de 30 dias |
| Duplicatas exatas | Implementado globalmente na branch | Mapa de tamanhos entre páginas/volumes; SHA-256 completo e streaming via repositório | Hash consome I/O/tempo; limite de candidatos e orçamento de leitura (2 GiB por arquivo / 10 GiB por missão) interrompem cobertura; a leitura normal por página também é limitada a 512 MiB por arquivo / 1 GiB por página | Testar pares em páginas, categorias e volumes diferentes |
| Proteção do original | Fail-closed na branch | Grupo de hash idêntico preserva uma referência; cobertura global parcial bloqueia pré-seleção de duplicatas | Precisa provar que nenhum falso positivo chega à seleção em erros de leitura/hash | Testes de hash ausente, misto, divergente e provider parcial |
| Similaridade visual / pHash / embeddings | Ausente | Nenhuma implementação funcional | Não confundir com SHA-256; é outro produto técnico | Só desenvolver com dataset e avaliação de falsos positivos |
| Screenshots, blur, baixa qualidade | Ausente | Nenhuma classificação em source | Não anunciar na Play nem no paywall | Roadmap após P0 |
| Vídeos: duração/resolução/codec | Ausente/parcial | Metadados básicos MIME, tamanho, data e miniatura | Não existe análise consistente de duração, codec ou compressão | MediaMetadataRetriever + testes OEM |

## Revisão, limpeza e storage

| Área | Estado | Evidência atual | Pendência / limite | Próxima validação |
|---|---|---|---|---|
| Seleção e revisão | Implementada parcialmente | Selection ledger limitado a 500 URIs; revisão por URI; permite remover seleção de missão global | A lista e o preview ainda precisam ser testados com múltiplas páginas e 500 itens | Instrumentação de seleção e telas pequenas |
| Lixeira Android | API 30+ no source | Usa consentimento oficial MediaStore e revalidação | Android 10 não possui o mesmo fluxo batch de Lixeira | Testar mover/restaurar em API30+ e manter a limitação explícita |
| Exclusão permanente | API 29+ implementada na branch | API29 usa RecoverableSecurityException por URI; API30+ usa createDeleteRequest; estado posterior é consultado | Fluxo API29 recém-implementado e ainda sem validação em emulador/aparelho; API30+ também requer QA de aceitação/cancelamento/parcial | Testar API29 aceitar/cancelar/itens próprios e de outros apps; testar API30+ aceitar/cancelar/parcial |
| Recuperação da Lixeira | Implementada parcialmente | Ledger local de URIs e createTrashRequest(moveToTrash=false) | Ainda sem prova instrumentada de restore parcial entre OEMs | Testar restore de lote e URIs que falham |
| Espaço liberado | Corrigido parcialmente | Mover para Lixeira não é registrado como liberado; exclusão só contabiliza URIs ausentes confirmadas após resultado OK | Ainda precisa confrontar com delta de StatFs real; consultas podem ter atraso de provider | Medir antes/depois em telefone real |
| Medidor storage | Parcial, origem corrigida no commit `d32bb92` | `StatFs` no armazenamento compartilhado principal via `Environment.getExternalStorageDirectory()` | O medidor não soma cartão SD nem outros volumes; volumes adicionais aparecem separadamente em Arquivos quando acessíveis | Validar em aparelhos físicos e comparar com Configurações do Android |
| Histórico de análise | Ausente | Há histórico de paginação em memória, ledger da Lixeira e telemetria local separados | Não existe histórico persistente de sessões com snapshots por categoria | Implementar Storage History depois de P0 |
| SAF / Downloads / Documentos | Ausente | Não há fluxo de pasta selecionada via SAF | MediaStore não representa todo arquivo do usuário | Adicionar SAF opt-in e testes de árvore URI |
| WorkManager / agendamento | Ausente | Sem worker funcional | Sem alertas ou limpeza agendada | Planejar só após estabilidade; nunca limpeza automática |

## Monetização

| Área | Estado | Evidência atual | Pendência / limite | Próxima validação |
|---|---|---|---|---|
| BillingClient | Integrado no source | Billing KTX 8.0.0 consulta catálogo e purchases, inicia fluxo e tenta reconhecer compras | Não houve compra real nem validação Play Console | Teste com license testers |
| SKUs | Presentes no source | `sweep_pro_monthly`, `sweep_pro_annual`, `sweep_pro_lifetime` | Não confirmar ativação/ofertas/país/moeda sem Play Console | Verificar IDs, purchase type, base plans e offers |
| Feedback de compra | Melhorado na branch | Botões mostram quando o fluxo não inicia; status diferencia reconciliação de plano gratuito | Precisa teste de todos os estados Billing e falhas de rede | Testar conexão, indisponível, cancelado, pending e owned |
| Segurança do entitlement | Não pronto para produção | Cliente verifica presença de assinatura e JSON, package e estado | Não valida criptograficamente purchaseToken em backend/API confiável; risco de entitlement falsificado | Implementar backend confiável e reconciliação idempotente |
| Pro / restore | Parcial | Consulta compras e faz acknowledge se elegível localmente | Não testado em reembolso, expiração, cancelamento, troca de plano e migração de usuário | Ciclo completo com licença Play |
| Exportação Pro | Implementada no cliente; comercialização bloqueada | ACTION_SEND exporta o resumo da seleção revisada somente com entitlement Pro verificado | Novas compras permanecem desativadas até validação de purchaseToken por backend confiável | Configurar backend, testar entitlement e compartilhar relatório em aparelho |
| Recebimento financeiro | Informado pelo proprietário como habilitado em 03/11/2026 | Perfil financeiro da conta está destravado segundo o proprietário | Isso não confirma produtos in-app ativos ou compra testada | Conferir produtos e executar compra de teste no listing |

## UX, identidade e política

| Área | Estado | Evidência atual | Pendência / limite | Próxima validação |
|---|---|---|---|---|
| Idiomas / localização | Parcial | PT-BR e EN completos (148/148 strings); ES, FR, DE, HI, JA e ZH-CN têm 63/148 strings e usam fallback em inglês para as restantes | Não declarar suporte multilíngue completo até traduzir e revisar todas as strings; layouts RTL e variantes regionais não foram validados | Completar traduções, validar placeholders e testar idioma do sistema em aparelhos reais |
| Marca Midiastore | Alinhada na branch | app_name, topbar M e strings comerciais | Ícone do launcher já era M/check; precisa instalar e validar launcher em OEMs | Verificar Android 12–16, adaptive icon |
| Navegação | Parcial | HOME, FILES, OAAS, PRO | Conteúdo ainda pode parecer complexo; não há estudo usability com testador | Teste guiado com 3 usuários sem assistência |
| Acessibilidade | Não verificada | Views e MaterialButtons | TalkBack, contraste, hit targets, escala de fonte ainda não passaram QA | Testes manuais e automated accessibility scan |
| Privacidade / Data Safety | Draft técnico | Documento revisto para não afirmar respostas definitivas | Revisão depende do binário final, Billing, ACTION_SEND e SDKs | Auditoria do manifesto e formulário Play final |

## Decisão comercial atual

O posicionamento aprovado é limpador de celular com apresentação Pro e modelo Freemium. Análise, revisão manual, proteção do original e limpeza com confirmação continuam gratuitos. O benefício Pro implementado é a exportação do resumo da seleção revisada. Histórico, previsão de armazenamento e análise visual não são funcionalidades disponíveis.

## Alegações proibidas até validação

- Não anunciar “limpa o telefone inteiro” quando acesso for parcial ou a cobertura global for incompleta.
- Não anunciar duplicatas confirmadas se SHA-256 estiver ausente ou divergente.
- Não dizer que mover para Lixeira liberou espaço.
- Não anunciar suporte de limpeza Android 10 enquanto API29 estiver bloqueada.
- Não anunciar Pro “verificado” sem validação confiável do purchaseToken.
- Não anunciar fotos semelhantes, blur, qualidade, compressão, limpeza de cache privado de apps, aumento de RAM, resfriamento, aceleração, organização SAF ou Storage Forecast enquanto não existirem e passarem testes.
- Não considerar BUILD SUCCESSFUL como evidência de UX, operação de arquivo ou compra real.

## Critério global

A presença de código não é aprovação. Cada funcionalidade precisa de teste em CI, teste instrumentado quando aplicável e, para APIs MediaStore/Billing/limpeza, confirmação em dispositivos físicos representativos. A PR #3 é recuperação em desenvolvimento, não um Release Candidate.
