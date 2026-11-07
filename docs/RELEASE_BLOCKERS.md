# Bloqueadores de release — Midiastore

## Reavaliação local mais recente — 2026-11-07

- Home: a ação grátis executa a varredura MediaStore de arquivos grandes no próprio app. Manualmente no Moto G84 5G/API35, encontrou 5 candidatos para revisão (84,30 MB) e abriu a lista interna; nenhum arquivo pessoal foi apagado. O teste não aprovou/cancelou a caixa de consentimento de exclusão permanente e não fecha o gate de deleção.
- A Home não abre mais Files by Google nem pede `MANAGE_EXTERNAL_STORAGE`. O usuário revisa os arquivos selecionados e a exclusão segue o consentimento oficial do Android. Lixeira não é contabilizada como espaço liberado.
- O botão de compartilhamento abriu o Sharesheet nativo e foi fechado sem publicação.
- `:app:assembleDebug` passou no Windows para a versão instalada no aparelho. Essa verificação manual não substitui CI, lint e instrumentação.
- Produtos Google Play mensal/anual/vitalício continuam indisponíveis na consulta Billing. Valores propostos foram documentados, mas não são preços ativos na Play Console. Não há backend confiável de validação de purchaseToken.
- O app exibe os valores propostos (R$14,90/mês, R$79,90/ano, R$149,90 vitalício), identificados como ainda não ativos até a Play retornar os preços oficiais. Home/limpeza/compartilhamento foram localizados em ES, FR, DE, HI, JA e zh-CN; as demais telas ainda não têm tradução completa nesses idiomas.
- Publicação pública permanece **NÃO LIBERADA**. Consulte `docs/FINAL_PUBLICATION_CHECKLIST.md` para o resumo atualizado e os gates ainda abertos.

## Reavaliação da retomada — 2026-11-06

- **API29: OPEN / NOT RUN.** Fixture auxiliar com UID independente e assertions do owner real estão compilados, mas sem execução API29. Fechar somente quando o runner API29 provar attribution, pedido recuperável, cancelamento sem remoção, aprovação, ausência da URI e bytes confirmados.
- **Consentimento API30+: OPEN / SKIP no Motorola.** O teste direto API35 foi afetado por serviço de auto-click de terceiro habilitado no dispositivo, que tocou na janela de consentimento automaticamente. O harness agora pula esse cenário por assumption ambiental, sem relaxar assertions. Fechar com cancelamento e aprovação realmente observados em aparelho/emulador sem automação externa.
- **UTP: OPEN / FAIL.** Execução filtrada mais recente no Motorola, com APK AndroidTest SHA `55EAA999F40E3F0424A2271A5265AADA08167D3C23611A3F52ABDBD13C20E3CD`, falhou antes de coletar casos; relatório indica 0 testes e não existe XML. Logs debug anteriores mostram falha de leitura RSA PEM no servidor local do listener UTP. Runner direto aprovado não resolve o gate do UTP.
- **API35 MediaStore sem consentimento: PASS local limitado.** A classe reportou `OK (5 tests)`, mas o log mostrou 4 métodos executados e o caso partial skipped porque o pacote já tinha permissões totais. O caso Selected Photos foi então preparado via ADB e passou isoladamente (`OK (1 test)`); permissões Debug foram restauradas. Não valida API29, consentimento ou UTP.
- A alteração de guard de auto-click é exclusivamente do harness de teste e foi compilada/testada no Motorola. Não altera configurações do dispositivo nem fluxo de produção.

## Reavaliação após execução local — 2026-11-06

- **P0 API29 segue aberto (NOT RUN).** App auxiliar agora cria fixture em UID separado; teste exige metadata `OWNER_PACKAGE_NAME` do MediaStore, leitura pelo alvo, consentimento recuperável, cancelamento sem remoção, novo pedido/aceite e verificação posterior da ausência da URI. Critério de fechamento: rodar o APK atual em API29 e observar todos esses estados, incluindo resultado por URI/bytes; CI em SHA atual também deve passar.
- **P0 UTP segue aberto (FAIL do coletor).** No APK AndroidTest final, `connectedDebugAndroidTest` falha sem XML; execução direta no mesmo APK passou. Logs sugerem falha BouncyCastle/PEM no servidor de resultado local UTP, sem causa definitiva. Critério: obter XML/casos pelo UTP no runner suportado ou uma correção de toolchain e evidência separada do runner direto.
- **API35 instrumentação local:** AndroidJUnitRunner direto passou 5 casos de MediaStore, com partial access efetivamente executado e fixtures sintéticas auditadas como removidas; Main passou Home e ignorou dois testes condicionais API29. Isso não substitui sucesso UTP/CI no commit atual.
- **CI remoto permanece antigo:** run `42080940375` testa SHA `16545a4`, não as alterações locais. Necessário enviar o conjunto revisado à branch da PR com autorização do proprietário para executar CI API29/API35.
- Demais gates de release — Lint findings, política/Data Safety, permissão ampla, Billing verificado por backend, Play Console, versionCode publicado e certificado legítimo — permanecem abertos conforme abaixo.

**Estado: BLOQUEADO.** Não gerar nem submeter AAB de produção enquanto os gates P0 abaixo não estiverem fechados.

## Estado de investigação local — 2026-11-06

- Run remoto `42080940375` no SHA `16545a4` falhou em API29/API35. Os XML/logs foram baixados localmente para `.local-ci-42080940375/`; diagnóstico: fixture API29 pertencia ao UID do alvo, volume `external_primary` ainda não existia no momento das inserções API35 e o processo API35 foi encerrado após revogação de permissão durante o teste.
- Correções locais: fixture API29 em app auxiliar com UID distinto e verificação do owner real; seleção de volume baseada no conjunto runtime do MediaStore; cenário de permissão parcial sem revogação durante processo. Esses testes ainda não foram repetidos na versão atual dos APKs.
- Compilação AndroidTest atual passou. UTP no Motorola não coletou XML/testes e perdeu conexão ADB; execução direta anterior `OK (7 tests)` é de APK anterior e não fecha o gate atual.
- API29 continua sem teste executado: emulator não iniciou por ausência de aceleração/hypervisor x86_64 neste host. A fixture corrigida ainda requer execução em API29.
- Critério de fechamento segue sendo teste instrumentado API29/API35 no SHA candidato, com XML/logs associados e sem skips que ocultem cenário requerido; CI verde no SHA exato da PR continua obrigatório.

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

## Decisão de produto e atualização de execução — 04/11/2026

- Produto inicial: Phone Cleaner / limpador inteligente de celular, não gerenciador universal.
- Modelo: Freemium; análise, revisão, proteção de original e limpeza segura permanecem gratuitas. Exportação do resumo revisado é o benefício Pro atualmente implementado.
- Compra Pro permanece bloqueada até verificação segura de purchaseToken por backend confiável.
- O medidor mede o armazenamento compartilhado primário; não somar SD/USB sem enumeração/validação própria.
- PT-BR/EN/fallback: 152 strings; seis outros idiomas: 63/152 cada; 534 traduções pendentes.
- O workflow do código antes das atualizações documentais estava queued; o estado do CI precisa ser reconsultado no HEAD exato após a atualização.

## Estado exato do HEAD — 04/11/2026, 16:22 BRT

- Branch `fix/p0-sweep-permissions-missions-billing`, HEAD `d60ff8f8c0852ab7c33ce638d662053c028d4916`.
- PR #3: aberta, Draft, base `main`, não merged.
- `main` permanece no SHA `3759e176c48f340fa576a06455579277e514d9d9`.
- GitHub Actions run [41866189110](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/41866189110), associado exatamente ao HEAD acima, está **queued**. Jobs API29, API35 e Windows também estão queued; não há resultado de build/teste para esse SHA.
- O último ajuste funcional liga o botão principal da home à missão global de arquivos grandes e retoma a tela de progresso após autorização de mídia. Foi adicionado teste instrumentado da existência/clickabilidade da ação principal; não foi executado ainda.
- Não declarar compilação, Lint, testes JVM, instrumentação ou APK gerado neste HEAD até obter logs do run exato.

## Auditoria técnica — atualização 2026-11-05

- PR #3 continua aberta como Draft. O trabalho atual é local e não foi enviado.
- O run remoto [42080940375](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/42080940375) passou JVM, builds e parser Windows, mas falhou na instrumentação API29/API35. API29 atribuiu a fixture ao próprio Midiastore; API35 consultou MediaStore antes de `external_primary` montar.
- A branch local adiciona app de fixture com UID independente, checa owner via MediaStore, executa a confirmação Android API29 e verifica o resultado após retry. Testes/runner aguardam o volume emulado montado. Tudo compila, mas ainda não rodou em aparelho/emulador.
- Build local Windows passou testes JVM, Lint task, APK Debug, APK AndroidTest e APK auxiliar. Lint registra 90 erros `MissingTranslation` e 94 warnings, ocultos como gate pelo `abortOnError=false`.
- O ambiente local tem JDK 17, SDK 36/Build Tools 35, mas nenhum dispositivo conectado ou system image API29/API35.
- `targetSdk=36` atende a exigência vigente para novas submissões/atualizações a partir de 31/08/2026. A declaração e elegibilidade de `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` seguem exigindo avaliação na Play Console. [Requisitos de API](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en) · [Declaração de permissões](https://support.google.com/googleplay/android-developer/answer/9214102?hl=en)
- Billing 8.3.0 foi configurado; Billing 8 segue dentro do prazo de suporte para updates até 31/08/2027. Isso não comprova SKU, oferta, entitlement ou compra. [Prazos oficiais Billing](https://developer.android.com/google/play/billing/deprecation-faq)
- Permanecem pendentes: nova execução CI API29/API35, validação em aparelhos físicos, política de privacidade pública e vinculada, Data Safety, catálogo/licenças Play, verificação server-side do purchaseToken, versãoCode publicada máxima e certificado. O Release/AAB não foi executado porque a chave legítima não está disponível.
