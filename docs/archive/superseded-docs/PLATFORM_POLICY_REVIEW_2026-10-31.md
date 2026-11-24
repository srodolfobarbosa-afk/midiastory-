# Revisão de plataforma e Google Play — 31/10/2026

Esta nota orienta a preparação do Sweep, mas não substitui a revisão final do manifesto mesclado, do AAB nem das declarações na Play Console.

## Target API

A regra publicada para novos apps e atualizações em telefones exige target Android 16 / API 36 desde 31/08/2026, salvo extensão aplicável concedida pela Play. O candidato usa `targetSdk 36`; o valor deverá ser confirmado no manifesto mesclado do AAB.

Fonte: https://support.google.com/googleplay/android-developer/answer/11926878

## Acesso a fotos, vídeos e áudio

O app declara `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` e `READ_MEDIA_VISUAL_USER_SELECTED`. O acesso a áudio é granular e independente; a seleção visual parcial nunca implica acesso a áudio. A Play exige declaração e justificativa para permissões amplas de foto/vídeo: a função central deve realmente exigir leitura contínua/ampla da biblioteca, e opções como Photo Picker devem ser avaliadas quando o caso de uso for selecionar itens específicos. A equipe deverá explicar a análise local de biblioteca de mídia (imagens, vídeos e áudio) como função central, sem afirmar que a aprovação é garantida.

Fonte: https://support.google.com/googleplay/android-developer/answer/14115180

## Google Play Billing

O AAB auditado anteriormente indicava Billing Library 8.0.0. A tabela oficial consultada em 31/10/2026 apresenta 31/08/2027 como prazo para novos apps/updates em Billing 8, com possível extensão até 01/11/2027. Assim, a versão 8.0.0 não está vencida nesta data. O bloqueio do candidato é funcional: ele ainda não tem o cliente Billing, catálogo de produtos, reconhecimento de compras, acknowledgement, restauração e entitlement que o app atual possa usar.

Fonte: https://developer.android.com/google/play/billing/deprecation-faq

## Assinatura e identidade

A atualização precisa manter `applicationId` e passar pela cadeia de assinatura/upload aceita pela Play Console. O `versionCode 9` é provisório e só pode ser usado se o maior código publicado continuar sendo 8. Não incluir JKS, senha ou chave privada no ZIP ou Git.

Fonte: https://developer.android.com/studio/publish/app-signing

## Política de privacidade e Data safety

A política incluída no projeto é somente rascunho. Deve ser publicada em URL acessível e o formulário Data safety deve corresponder ao AAB final, às permissões, SDKs de terceiros, coleta, compartilhamento, retenção e exclusão de dados reais. O candidato analisado não declara Internet nem possui Firebase/analytics, mas isso precisa ser confirmado no artefato final e nos SDKs transitivos.

Fonte: https://support.google.com/googleplay/android-developer/answer/10787469

## O que continua condicionado à conta

A necessidade de teste fechado com 12 participantes por 14 dias aplica-se a determinadas contas pessoais novas, segundo as regras da Play. A condição exata deve ser verificada na página de Dashboard/Testing da conta Sweep; não presuma que se aplica ou que está dispensada apenas com base neste projeto.

Fonte: https://support.google.com/googleplay/android-developer/answer/14151465
