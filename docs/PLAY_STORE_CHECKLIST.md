# Checklist da Google Play Console — Midiastore

**Estado (`2026-11-14`): EM VALIDAÇÃO P0 ANTES DE PRODUÇÃO.** Este documento reflete os fatos verificados na Play Developer API v3 e na branch atual (`codex/midiastore-release-audit`).

## 1. Identidade confirmada pelo proprietário e Play Console API

- [x] Nome comercial da ficha: **Midiastore**.
- [x] Package da ficha de teste interno e alpha: `com.rdf112.midiastore`.
- [x] `applicationId` do código atual: `com.rdf112.midiastore`.
- [x] Namespace Kotlin legado: `com.rdf112.sweep` (não é o package publicado).
- [x] `versionCode=21` e `versionName=2.4.0` no código atual e publicados em `internal` / `alpha` (`status: completed`, 7 bundles no Histórico API v3).
- [ ] Confirmar certificado de upload (`sweep-upload.jks`) no próximo upload de AAB (`> 21`) apenas quando aprovado pelo proprietário.
- [ ] Confirmar na UI da Play Console que os 12+ testadores opt-in atingiram os 14 dias contínuos exigidos.

**Não use instruções antigas para criar dois AABs.** O script Windows de Release foi desativado porque alterava applicationId e podia restaurar `com.rdf112.sweep` como padrão. A aplicação em continuidade é somente `com.rdf112.midiastore`.

## 2. SDK, manifesto e permissões

- [x] Código configura minSdk 29, targetSdk 36 e compileSdk 36.
- [ ] Verificar o manifesto mesclado e permissões solicitadas no AAB final.
- [ ] Validar a declaração/justificativa de acesso a fotos e vídeos conforme política atual da Play.
- [ ] Testar acesso total, parcial, negado e revogado em Android 13, 14, 15 e 16.
- [ ] Resolver a operação de limpeza Android 10/API29 ou aprovar explicitamente uma mudança de minSdk após estudo de impacto.
- [ ] Testar em dispositivos Pixel/AOSP, Samsung, Motorola e Xiaomi/HyperOS, incluindo SD/removível quando disponível.

## 3. Monetização dentro do aplicativo

Produtos conferidos ao vivo na Google Play Developer API v3 (`2026-11-14`) em `com.rdf112.midiastore`:

| Product ID | Tipo no Billing | Status API v3 & Preço BR/US |
|---|---|---|
| `sweep_pro_monthly` | SUBS (`monthly-base`, `P1M`) | **ACTIVE** — `R$ 14,99` / `US $2.69` (`offers: []`) |
| `sweep_pro_annual` | SUBS (`annual-base`, `P1Y`) | **ACTIVE** — `R$ 79,99` / `US $13.99` (`offers: []`) |
| `sweep_pro_lifetime` | INAPP (`lifetime-buy`) | **ACTIVE** — `R$ 149,99` / `US $26.99` |

- [x] Conferir os IDs exatos, base plans, períodos e status `ACTIVE` na Play Console API.
- [x] Implementar validação de `purchaseToken` via backend confiável (`verify-play-purchase` v6 no Supabase consultando `subscriptionsv2` / `products` na Google Play Developer API).
- [ ] Adicionar contas de license tester e executar testes ponta a ponta de compra aprovada, pendente, cancelada, renovação, reembolso e expiração.
- [ ] Avaliar criação opcional de ofertas de período de avaliação gratuito (`Free Trial` / oferta introdutória) nos base plans de assinatura.

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
