# Bloqueadores de release — Midiastore

**Estado: BLOQUEADO.** Não gerar nem submeter AAB de produção enquanto os gates P0 abaixo não estiverem fechados.

## Identidade e continuidade de atualização

1. O proprietário confirmou o listing de teste interno `Midiastore — com.rdf112.midiastore`.
2. O código atual configura `applicationId=com.rdf112.midiastore`, `versionCode=17`, `versionName=2.2.1`; esses valores não provam qual foi o maior versionCode efetivamente publicado.
3. Confirmar na Play Console o maior versionCode já publicado, o package do listing, a chave de upload aceita e Play App Signing. O próximo versionCode do source precisa ser estritamente maior que esse máximo.
4. Não alterar Application ID, versionCode, versionName ou certificado sem aprovação do proprietário.
5. O source histórico que gerou o AAB publicado ainda não foi recuperado; a reconstrução não pode ser declarada byte-a-byte equivalente.

## P0 — confiabilidade e segurança de limpeza

1. Obter CI verde no commit mais recente da PR #3: testes de domínio, JVM, Lint, build Debug e testes instrumentados.
2. Investigar o teste instrumentado MediaStore que falhou no emulador Android 15; não mascarar erro de provider, permissão ou processo.
3. Executar MediaStore em Android 29, 30, 33, 34, 35 e 36 e dispositivos representativos (Pixel/AOSP, Samsung, Xiaomi/Motorola).
4. Testar permissão total, parcial, negada, revogada e áudio independente; verificar se a tela de gerenciar acesso aparece corretamente.
5. Testar paginação com listas grandes, offsets, ordem estável, cursor nulo, falha do provider, mudanças de volume, cancelamento e mudanças de geração.
6. A nova varredura global percorre tipos, volumes e páginas acessíveis; deve provar cobertura, respeitar os limites e reportar parcial quando um source falhar.
7. Para duplicatas, só SHA-256 completo, não vazio e idêntico comprova conteúdo igual. Se a cobertura global for parcial, nenhuma duplicata pode ser pré-selecionada para exclusão.
8. Revalidar cada URI imediatamente antes da operação e validar resultado por item depois da confirmação do Android.
9. Mover para Lixeira não é espaço liberado. Só exclusão permanente confirmada pode alimentar a métrica de bytes liberados.
10. Validar em emulador API29 e aparelho físico o novo fluxo de exclusão permanente via RecoverableSecurityException (aceitar, cancelar, URI própria, URI de outro app e verificação pós-operação). Lixeira/restauração permanecem API30+; não anunciar essas duas funções para API29.
11. Executar testes de TalkBack, fonte ampliada, contraste, rotação, telas pequenas, tablet/dobrável, memória, bateria e ANR.

## P0 — Billing e recebimento

1. Verificar na Play Console se existem e estão ativos os IDs e tipos atualmente configurados no código:
   - `sweep_pro_monthly` — SUBS
   - `sweep_pro_annual` — SUBS
   - `sweep_pro_lifetime` — INAPP
2. Verificar ofertas-base, offerToken, países, preços, status de ativação e contas de license tester.
3. Implementar verificação confiável do purchaseToken pela Google Play Developer API/backend antes de liberar Pro em produção.
4. Tratar compras pendentes, canceladas, restauradas, reembolsadas, expiradas, repetidas e falhas de rede; processamento e acknowledgement devem ser idempotentes.
5. Preservar entitlement de usuários que já compraram em versões anteriores.
6. Testar a jornada real de compra pelo link de teste interno. A configuração financeira/recebimento da conta, informada pelo proprietário como habilitada em 03/11/2026, não comprova que os produtos in-app estejam ativos ou testados.

## P0 — assinatura, política e publicação

1. Não criar um novo JKS por tentativa. Usar somente a chave de upload legítima aceita pela Play Console ou fluxo oficial de redefinição.
2. Validar manifesto mesclado, permissões de mídia, targetSdk, dependências e SDK declarations.
3. Revisar política de privacidade e Data Safety com base no binário, SDKs e fluxos reais (incluindo exportação via ACTION_SEND e Billing).
4. Verificar exigências de acesso amplo a fotos/vídeos e elegibilidade da permissão para a função principal do app.
5. Gerar AAB assinado apenas após todos os gates e inspecionar package, versionCode, certificado e conteúdo do bundle.
6. Passar por teste interno/fechado com testadores reais antes de produção.

## Regra de produto

Não anunciar detecção de fotos semelhantes, blur/baixa qualidade, compressão, limpeza de cache privado de outros apps, limpeza de RAM, aumento de velocidade, histórico permanente ou gerenciador completo de documentos enquanto esses recursos não estiverem implementados e testados.

A PR #3 é uma etapa de recuperação, não um release candidate.