# Auditoria de Segurança e Privacidade

Data: 03/11/2026. Escopo: inspeção estática do source extraído, execução Debug e testes JVM; não é pentest nem validação em aparelho.

## Dados e permissões

- O Manifest declara `READ_EXTERNAL_STORAGE` até API 32, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` e `READ_MEDIA_VISUAL_USER_SELECTED` (API 34+). `minSdk=29`, `targetSdk=36`.
- A aplicação solicita fotos, vídeos e áudio no mesmo fluxo. Como a funcionalidade principal analisa os três tipos, acesso amplo pode ser necessário; a UX deve solicitar por categoria no momento em que o usuário escolhe essa análise e explicar que o Photo Picker basta para seleção pontual, mas não para varredura/organização de toda a biblioteca.
- A cobertura parcial Android 14+ é reportada e a ação para rever seleção fica disponível. Permissões podem ser revogadas/alteradas; a sessão precisa ser invalidada no retorno.
- Downloads/documentos devem usar SAF; não acessar diretórios privados de outros apps nem solicitar `MANAGE_EXTERNAL_STORAGE` sem necessidade central demonstrada e aprovação Play.
- O manifesto tem `allowBackup=false`; não declara permissão ampla de arquivos, uso de root/ADB ou serviços próprios.

## Integridade e limpeza

- SHA-256 usa fluxo de 8 KiB, com cancelamento; hash ausente/falha não confirma duplicata. Comparação só encontra duplicatas na página atual, portanto não anunciar detecção global.
- Antes de operação, revalidar URI, tamanho, MIME, timestamp e SHA quando disponível. Alteração de conteúdo ou falha deve rejeitar/adiar a ação.
- Android R+ usa `MediaStore.createTrashRequest`/`createDeleteRequest` e IntentSender do sistema. API 29 não habilita essas operações no app; não substituir por exclusão silenciosa.
- Verificação pós-ação: Cursor nulo, coluna ilegível ou exceção resultam em `UNVERIFIED_ERROR`; somente URI ausente em consulta válida ou lixeira explicitamente confirmada pode atualizar estados. Itens em Lixeira não contam como liberados. Somente exclusão aceita pelo usuário e ausência confirmada de todos os URIs soma bytes efetivamente liberados.
- Ainda falta teste instrumentado em aparelhos/ROMs para comprovar revalidação, cancelamento e resultado em operação real.

## Privacidade e exportação

- Hashes, consulta e sugestões rodam localmente no código observado; nenhuma API própria de upload foi encontrada no Manifest. Google Play Billing comunica-se com serviços Google.
- Exportação de relatório usa `ACTION_SEND` e chooser, enviando os nomes/detalhes selecionados ao app escolhido pela pessoa; explicar isso no consentimento/UX e revisar política de privacidade/Data safety.
- Telemetria OaaS é armazenada em SharedPreferences local; consentimento de sugestões customizadas é opt-in e existe ação para limpar. Não enviar nomes, hashes ou URIs a métricas externas.
- Nenhuma chave JKS, `keystore.properties` real ou segredo foi encontrado. O SDK de compilação foi instalado em `/tmp`, não dentro do repositório.

## Billing e confiança

**P0:** `AndroidPlayBillingGateway` usa allowlist de IDs e estados fail-closed, mas `purchase.signature`/`originalJson` não vazios e `packageName` correto não equivalem a validação criptográfica. O cliente pode ser instrumentado/adulterado. Não vender entitlement definitivo baseado somente nessas verificações: implementar validação pelo backend/Google Play Developer API ou justificar tecnicamente uma arquitetura confiável, com token idempotente, acknowledgement, reembolso, expiração e revogação.

A consulta falha deve permanecer `RECONCILIATION_REQUIRED`; produto explicitamente não encontrado não pode manter `ProductDetails` antigo. Compra, restauração, cancelamento e falhas não foram testados com Play Console.

## Cadeia de suprimentos

- Gradle Wrapper configurado para 8.13 e checksum da distribuição fixado. O JAR do Wrapper no ZIP foi anteriormente identificado com hash associado a outra versão; precisa ser regenerado/validado contra o Wrapper 8.13 oficial antes de release.
- Lint Debug compilou, porém reportou referências `InlinedApi` e dependências com versões mais novas disponíveis. Referências são nomes de permissão em caminhos por API; corrigir/confirmar em novo Lint após substituição por constantes de domínio. Não atualizar versões em massa sem release notes e testes.
- Não foi executada análise completa de dependências/CVEs/SBOM, nem auditoria de binário R8. Fazer com ferramentas aprovadas antes da publicação.
- Nenhum certificado de upload, AAB histórico ou comprovante de Play App Signing disponível. Não criar chave substituta.

## Riscos remanescentes

1. Código do app ainda sem validação instrumentada de MediaStore/consentimento em dispositivo.
2. Identidade e versionCode da Play Console desconhecidos; endpoint público do pacote retorna 404, que não prova ausência de publicação.
3. Verificação local de compra insuficiente para garantir Pro contra cliente adulterado.
4. Permissões amplas de mídia precisam de justificação e aprovação na Play Console.
5. Scanner tem fallback local sem extras, mas custo aumenta em offsets profundos e estabilidade concorrente de biblioteca não é transacional.
6. Hash completo de itens candidatos pode aumentar CPU/tempo/bateria; medir em bibliotecas grandes e fornecer cancelamento.
7. Política de privacidade, Data safety, permissões Play e licenças de todas as dependências precisam ser confrontadas com o binário final.

## Política de permissões e medição — 04/11/2026

O manifesto da branch declara permissões de leitura de mídia e acesso parcial selecionado para Android 14+, mas não declara MANAGE_EXTERNAL_STORAGE. Manter esse limite. O Google Play restringe All files access e exige declaração quando aplicável; para READ_MEDIA_IMAGES/VIDEO, é necessário demonstrar que o acesso amplo serve a uma função central e que Photo Picker não é suficiente. Downloads/documentos específicos devem usar SAF.

O posicionamento Phone Cleaner pode justificar análise da biblioteca inteira se isso for realmente a função principal; não garante aprovação da Play. A declaração precisa explicar a necessidade de cada permissão e a interface deve acomodar usuários que concedem acesso parcial ou negam a permissão.

O medidor agora consulta o armazenamento compartilhado principal; não afirmar que inclui automaticamente cartão SD/USB. Fontes: [política All files access](https://support.google.com/googleplay/android-developer/answer/10467955), [política de fotos e vídeos](https://support.google.com/googleplay/android-developer/answer/16935362), [SAF](https://developer.android.com/guide/topics/providers/document-provider).
