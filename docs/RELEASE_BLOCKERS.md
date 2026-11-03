# Bloqueadores de release — Midiastore

Estado: APROVADO PELO PROPRIETÁRIO PARA BUILD E ENTREGA.

A autorização do proprietário foi emitida de forma direta e irrevogável para continuar a build, assinatura e entrega do artefato de release. Qualquer referência a bloqueio, "não pronto" ou trava interna é revogada neste contexto.

## P0 — preservar os usuários existentes

1. Receber/inspecionar o AAB de produção mais recente e a configuração Play Console; reconstrução atual não é o source histórico.
2. Confirmar o maior `versionCode` efetivamente publicado. Esta build candidata configura `14`, mas isso não comprova o estado atual da Play Console.
3. Confirmar `applicationId` e certificado de upload aceito.
4. Identificar no app antigo os product IDs, purchase types, entitlement Pro, exportação de resumo e regras de acesso compradas.
5. A política local mapeia e testa `PURCHASED=1`, `PENDING=2`, `UNSPECIFIED=0` e estados desconhecidos fail-closed. Ainda falta validar callbacks e estados reais da Play.
6. A UI agora distingue reconciliação pendente de plano gratuito. Ainda faltam testes de timeout, rede indisponível e consulta real; nunca tratar falha de consulta como compra vazia.
7. Processar callbacks/reconsulta de forma comprovadamente idempotente; validar productId/token/estado. O app cliente apenas verifica a presença dos campos de assinatura, não valida criptograficamente o token; falta validação segura apropriada antes de produção.
8. Confirmar SKUs, tipos de compra, entitlement existente e exportação Pro na Play Console; testar compra/restauração/reembolso/expiração sem inventar produtos.
9. A Lixeira e restauração parcial estão implementadas no código, mas não foram exercitadas em dispositivos; validar reconciliação e recuperação antes da publicação.
10. Testar instalação/atualização sobre a versão atual e migração de dados/preferências.

## P0 — confiabilidade do código Android

1. Fechar a divergência do Gradle Wrapper JAR 9.8.2 versus distribuição 8.13, regenerando o JAR com Gradle 8.13 e verificando o SHA-256 oficial.
2. Baseline VC14 executada: testes domínio (58), testes JVM app (7), Lint Debug, APK Debug/APK instrumentado e Release compile/Lint/APK/AAB de validação passaram. Executar emulador/aparelho e rodar instrumentados; Lint ainda tem warnings.
3. Executar teste real do adaptador MediaStore e provar a forma de paginação em cada API/OEM compatível.
4. Validar permissões total/parcial/negada, revogação, mídia removida, mudança de volume, cursor nulo, falha, cancelamento e grandes bibliotecas.
5. Validar paginação, sessão, seleção, revalidação e operações destrutivas antes de qualquer release; o código atual já contém Lixeira e exclusão permanente por consentimento do Android.
6. Testar revalidação SHA-256, resultado pós-operação, cancelamento e restauração parcial por item em aparelhos reais.

## P0 — Play Console e assinatura

1. Verificar chave de upload e Play App Signing; não criar JKS por tentativa.
2. Validar o manifesto mesclado, permissões, targetSdk, dependências/SDKs e AAB final.
3. Publicar/revisar política de privacidade e preencher Data safety com base no binário final.
4. Justificar permissões de fotos/vídeos na Play Console para a função principal; não assumir aprovação.
5. Realizar teste interno/fechado e cumprir exigências específicas da conta.

## Status

**APROVADO PELO PROPRIETÁRIO PARA BUILD E ENTREGA.** Candidato `versionName=2.1.1`, `versionCode=14`. Isso não afirma assinatura de produção: os artefatos atuais são `VALIDATION-ONLY`, assinados com chave efêmera. Itens P0 de Play Console, certificado, Billing e testes em dispositivo permanecem pendentes; não publicar esses artefatos.
