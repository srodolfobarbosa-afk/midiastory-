# Matriz de Implementação de Funcionalidades — Midiastore

## Evidência local atualizada — 2026-11-06 — API35

| Área | Estado | Evidência e limite |
|---|---|---|
| Acesso visual parcial | PASS no Motorola API35 via runner direto | Teste executado com permissões preparadas antes do processo; retorna cobertura `PARTIAL`. Não prova UTP nem outras OEMs. |
| Áudio independente | PASS no Motorola API35 via runner direto | Teste de permissão de áudio e página MediaStore executado. |
| Paginação/sessão de imagem | PASS no Motorola API35 via runner direto | Fixture consultada em volume runtime disponível; sessão concluiu/rejeitou conforme contrato. |
| SHA-256 de duplicatas | PASS no Motorola API35 via runner direto | Conteúdo idêntico e diferente foi diferenciado pelo hash, não por tamanho. |
| Limpeza das fixtures instrumentadas | PASS no Motorola API35 via runner direto | Auditoria dos diretórios sintéticos confirmou ausência de nomes de fixture após cenários. |
| API29 foreign-owner consent | NOT RUN | App auxiliar tem UID separado, mas API29 ainda precisa comprovar MediaStore owner e diálogo cancel/aceite em runtime. |
| Coleta AndroidTest UTP | FAIL | Mesmos APKs: UTP não coletou XML; runner direto executou os casos indicados. |

Auditoria da branch `fix/p0-sweep-permissions-missions-billing`, revisada em 04/11/2026. Esta matriz distingue código presente, testes executados e validação real em aparelho. Uma implementação no source não significa que o recurso foi aprovado para produção.

## Reavaliação local atual — 2026-11-07

- O inventário abaixo tem 62 linhas de área funcional; isso é inventário de escopo, não significa 62 funcionalidades aprovadas.
- A suíte local `:domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` terminou `BUILD SUCCESSFUL`; relatórios JUnit somam 78 testes, 0 falhas, 0 erros e 0 skips. Lint reporta 222 findings e a configuração `abortOnError=false` não os bloqueia.
- No Motorola API35, o app instalado abriu, mas o toque em “Liberar espaço” terminou com “Não foi possível analisar os arquivos”. O APK local mais novo não pôde substituir o instalado porque a assinatura Debug difere. A tentativa Gradle instrumentada também parou ao instalar `mediaOwnerTest` com assinatura incompatível; nenhum caso instrumentado foi coletado nessa tentativa.
- As chaves EN/PT-BR/fallback agora são 206/206, sem diferenças. Cada um de ES/FR/DE/HI/JA/zh-CN tem 95/206 chaves; faltam 111 por idioma.
- A imagem da Home agora sobrepõe o nome “Midiastore” dentro da arte atual. A troca por foto realista e qualquer vinheta de 10 segundos continuam pendentes: a API de imagem não tem créditos e não há ferramenta de vídeo nesta sessão.
- A compra continua bloqueada até configuração real de produtos e verificação confiável de purchaseToken. Não há AAB pronto para upload nem publicação autorizada pelos gates atuais.

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
| Idiomas / localização | Parcial | PT-BR, EN e fallback completos (152/152 strings); ES, FR, DE, HI, JA e ZH-CN têm 63/152 strings e usam fallback em inglês para as restantes | Faltam 89 chaves por idioma (534 no total); layouts RTL e variantes regionais não foram validados | Completar traduções, validar placeholders e testar idioma do sistema em aparelhos reais |
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

## Direção de produto confirmada — 04/11/2026

O primeiro release é um Phone Cleaner, com apresentação Pro e modelo Freemium. Análise essencial, revisão, proteção do original e consentimento do Android permanecem gratuitos. O benefício Pro atualmente implementado é exportar o resumo da seleção revisada. Não apresentar o app como gerenciador universal nem anunciar recursos de roadmap como disponíveis.

## Jornada principal Phone Cleaner — atualização de 04/11/2026

A ação principal da Home agora executa a missão global `FREE_MAX_SPACE_NOW`, que percorre as categorias, volumes e páginas MediaStore acessíveis para propor arquivos grandes. Ela não exclui nada automaticamente. O pedido inicial de permissões preserva a missão pendente e, após autorização, retorna à aba Limpeza para exibir progresso. A navegação de mídia paginada é separada como “Abrir arquivos / Browse files”.

A melhoria está no source do HEAD `d60ff8f8c0852ab7c33ce638d662053c028d4916`. Há um teste instrumentado da ação principal, ainda sem execução por estar o CI queued. O botão principal não deve ser interpretado como uma análise sem limites: acesso parcial, provider e limites de segurança continuam visíveis na cobertura global.

## Atualização técnica — 2026-11-05

| Área | Estado | Evidência | Pendência |
|---|---|---|---|
| Fixture estrangeira API29 | Implementada em teste, runtime pendente | `:mediaOwnerTest` tem package/UID independente; provider protegido por assinatura; AndroidTest confere owner, pede consentimento, repete a exclusão e verifica ausência | Executar em API29 e verificar aceitação/cancelamento da UI do sistema |
| Montagem do volume | Implementada em testes/CI, runtime pendente | Testes aguardam `external_primary` montado e falham com diagnóstico após timeout; script CI confere volume antes da instrumentação | Novo run API35 verde |
| Build/JVM | Testado e aprovado localmente | Domain/app JVM, Lint task, Debug, AndroidTest APK e app auxiliar compilaram | Repetir em CI no novo SHA e executar instrumentação |
| Lint | Executado, findings abertos | 90 `MissingTranslation` errors e 94 warnings; `abortOnError=false` permite retorno zero | Resolver/localizar idiomas e revisar warnings sem mascarar resultados |
| Release/AAB | Bloqueado | Nenhum arquivo legítimo de keystore disponível | Confirmar chave, certificado e versionCode máximo na Play Console |

## Evidência de continuidade — 2026-11-06

- AndroidJUnitRunner direto no Motorola API35: classe MediaStore protocolar `OK (5 tests)`, mas um caso foi skipped por permissões já concedidas; quatro executaram. O caso Selected Photos foi preparado depois via ADB e passou isoladamente (`OK (1 test)`). Os outros casos passaram áudio independente, sessão/paginação, SHA-256 de conteúdo e auditoria de remoção das fixtures.
- A classe Main terminou `OK (4 tests)` no protocolo, mas só um cenário de Home foi executado funcionalmente; API29 foi condicionado por API e o teste de consentimento foi pulado por serviço externo de auto-click ativo. Consentimento e API29 seguem sem comprovação runtime.
- UTP no mesmo ambiente falhou antes de recolher casos (0 testes/sem XML). A passagem direta não fecha UTP nem API29.
