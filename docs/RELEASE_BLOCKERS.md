# Bloqueadores de release — Sweep

Nenhum AAB de produção deve ser gerado ou submetido enquanto qualquer item P0 permanecer aberto.

## P0 — preservar os usuários existentes

1. Receber/inspecionar o AAB de produção mais recente e a configuração Play Console; reconstrução atual não é o source histórico.
2. Confirmar o maior `versionCode` efetivamente publicado. `9` é apenas candidato com base na informação do proprietário.
3. Confirmar `applicationId` e certificado de upload aceito.
4. Identificar no app antigo os product IDs, purchase types, entitlement Pro, exportação de resumo e regras de acesso compradas.
5. Corrigir e testar o parser Billing: `PURCHASED=1`, `PENDING=2`, `UNSPECIFIED=0`, demais valores desconhecidos. Não conceder Pro para PENDING/UNSPECIFIED/UNKNOWN.
6. Distinguir consulta sem compras de erro/timeout/rede indisponível. Consulta falha não é compra vazia nem revogação automática.
7. Processar callbacks/reconsulta de forma idempotente; validar productId/token/estado; acknowledgement só após compra validada e apropriada.
8. Restaurar entitlement existente e o benefício Pro de exportação de resumo, com testes de compra/restauração/reembolso/expiração. Não inventar SKUs ou produtos.
9. Resolver recuperação parcial da Lixeira somente após a implementação da Lixeira ser retomada; referências não restauradas devem permanecer recuperáveis/reconciliáveis.
10. Testar instalação/atualização sobre a versão atual e migração de dados/preferências.

## P0 — confiabilidade do código Android

1. Fechar a divergência do Gradle Wrapper JAR 9.8.2 versus distribuição 8.13, regenerando o JAR com Gradle 8.13 e verificando o SHA-256 oficial.
2. Executar `:domain:test`, testes de app, Lint, APK debug e compilação de teste instrumentado em ambiente Android suportado.
3. Executar teste real do adaptador MediaStore e provar a forma de paginação em cada API/OEM compatível.
4. Validar permissões total/parcial/negada, revogação, mídia removida, mudança de volume, cursor nulo, falha, cancelamento e grandes bibliotecas.
5. Manter v0.3 somente leitura até a paginação, sessão, seleção e revalidação estarem aprovadas conforme a planta v1.9.
6. Depois reintroduzir Lixeira/restore em lote separado, com consentimento do Android e testes por item.

## P0 — Play Console e assinatura

1. Verificar chave de upload e Play App Signing; não criar JKS por tentativa.
2. Validar o manifesto mesclado, permissões, targetSdk, dependências/SDKs e AAB final.
3. Publicar/revisar política de privacidade e preencher Data safety com base no binário final.
4. Justificar permissões de fotos/vídeos na Play Console para a função principal; não assumir aprovação.
5. Realizar teste interno/fechado e cumprir exigências específicas da conta.

## Status

Os itens acima permanecem abertos nesta revisão. Este ZIP é uma etapa de desenvolvimento v0.3, não um Release Candidate.
