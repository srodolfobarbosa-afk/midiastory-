# Checklist da Google Play Console — Midiastore

**Estado: NÃO PRONTO PARA RELEASE.** Este documento substitui as instruções antigas de Sweep VC13 que mencionavam dois cadastros e dois AABs.

## 1. Identidade confirmada pelo proprietário

- [x] Nome comercial da ficha: **Midiastore**.
- [x] Package da ficha de teste interno mostrado pelo proprietário: `com.rdf112.midiastore`.
- [x] `applicationId` do código atual: `com.rdf112.midiastore`.
- [x] Namespace Kotlin legado: `com.rdf112.sweep` (não é o package publicado).
- [x] `versionCode=17` e `versionName=2.2.1` no código atual.
- [ ] Confirmar qual é o maior versionCode efetivamente enviado/publicado para este listing.
- [ ] Confirmar certificado de upload que a Play Console aceita e confrontar com a chave legítima.
- [ ] Confirmar que o app de teste instalado pertence a este listing/package.

**Não use instruções antigas para criar dois AABs.** O script Windows de Release foi desativado porque alterava applicationId e podia restaurar `com.rdf112.sweep` como padrão. A aplicação em continuidade é somente `com.rdf112.midiastore`.

## 2. SDK, manifesto e permissões

- [x] Código configura minSdk 29, targetSdk 36 e compileSdk 36.
- [ ] Verificar o manifesto mesclado e permissões solicitadas no AAB final.
- [ ] Validar a declaração/justificativa de acesso a fotos e vídeos conforme política atual da Play.
- [ ] Testar acesso total, parcial, negado e revogado em Android 13, 14, 15 e 16.
- [ ] Resolver a operação de limpeza Android 10/API29 ou aprovar explicitamente uma mudança de minSdk após estudo de impacto.
- [ ] Testar em dispositivos Pixel/AOSP, Samsung, Motorola e Xiaomi/HyperOS, incluindo SD/removível quando disponível.

## 3. Monetização dentro do aplicativo

O proprietário informou que o perfil financeiro/recebimento foi habilitado em 03/11/2026. Isso não confirma que produtos in-app estejam cadastrados ou ativos.

O source atualmente espera:

| Product ID | Tipo no Billing |
|---|---|
| `sweep_pro_monthly` | SUBS |
| `sweep_pro_annual` | SUBS |
| `sweep_pro_lifetime` | INAPP |

- [ ] Conferir os IDs exatos no Play Console.
- [ ] Conferir base plans, offers, períodos, status ativo, países/regions e preços.
- [ ] Adicionar contas de license tester.
- [ ] Testar preços dinâmicos recebidos de ProductDetails.
- [ ] Testar compra aprovada, pendente, cancelada, já possuída, restauração e reconexão.
- [ ] Testar renovação, cancelamento, expiração, reembolso e mudança de plano.
- [ ] Implementar validação de purchaseToken por backend confiável usando Google Play Developer API.
- [ ] Preservar entitlement de qualquer comprador existente.

Não cadastrar preços de benchmark como se fossem preços ativos. Não prometer Pix/cartão ou outros meios: a própria Google Play determina os meios disponíveis para o país e tipo de compra.

## 4. QA obrigatório antes de teste fechado

- [ ] CI verde no commit exato da PR #3: domain tests, app JVM tests, Lint, APK Debug e AndroidTest.
- [ ] Testes instrumentados MediaStore sem falhas no emulador Android 35.
- [ ] Varredura global de todos os tipos/volumes acessíveis comprovada com fixtures em múltiplas páginas.
- [ ] Cobertura parcial/limite/provider failure não produz resultado “completo”.
- [ ] Duplicatas só são confirmadas com SHA-256 não vazio e igual.
- [ ] Cobertura global parcial bloqueia pré-seleção de duplicatas para exclusão.
- [ ] Consentimento Android accept/cancel/partial testado em aparelho.
- [ ] Lixeira não contabiliza espaço como liberado; exclusão permanente exige confirmação por item.
- [ ] API29 cleanup resolvida ou claramente excluída da promessa.
- [ ] TalkBack, escala de fonte, contraste, rotação, tela pequena e tablet testados.
- [ ] Política de privacidade e Data Safety conferidos com o binário final, inclusive ACTION_SEND e Billing.

## 5. Assinatura e geração de Release

Não criar novo JKS por tentativa. Usar somente a chave de upload legítima aceita para o listing Midiastore ou o processo oficial de redefinição.

O script assinado agora exige secrets no environment protegido `play-release`:

- `SWEEP_KEYSTORE_BASE64`
- `SWEEP_STORE_PASSWORD`
- `SWEEP_KEY_ALIAS`
- `SWEEP_KEY_PASSWORD`
- `MIDIASTORE_RELEASE_APPROVED` com valor `YES`
- `MIDIASTORE_CONFIRMED_VERSION_CODE` com maior versionCode confirmado no Play Console
- `MIDIASTORE_EXPECTED_CERT_SHA256` com fingerprint SHA-256 do certificado de upload correto

O build falha fechado se package, versionCode, aprovação ou certificado não coincidirem. O workflow gera apenas artifacts para inspeção; não publica diretamente na Play.

## 6. Rollout

- [ ] Inspecionar AAB: package, versionCode, versionName, manifesto, permissões, SDK, Billing, assinatura e hashes.
- [ ] Instalar/atualizar sobre a versão de teste existente e confirmar que o package e certificado são compatíveis.
- [ ] Subir primeiro em teste interno/fechado.
- [ ] Pedir nova validação ao testador somente após os fluxos P0 passarem.
- [ ] Analisar crash/ANR, feedback, primeira análise concluída, seleção, exclusão confirmada, espaço efetivamente liberado e compra de teste.
- [ ] Produção somente após aprovação expressa do proprietário.

## Regra

A PR #3 é uma recuperação em desenvolvimento, não um Release Candidate. Não clicar em publicar, não subir AAB e não fechar os bloqueadores P0 com base somente em `BUILD SUCCESSFUL`.
