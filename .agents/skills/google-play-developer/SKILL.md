---
name: google-play-developer
description: Guia avançado de conformidade e automação da Google Play Console. Use para gerenciamento do teste fechado de 20 testadores (requisito de 14 dias), Scoped Storage (zero MANAGE_EXTERNAL_STORAGE), Google Play Developer API v3, Billing v7 e empacotamento de AAB.
---

# Google Play Developer & Compliance Skill (Midiastore)

Este skill define os procedimentos obrigatórios para publicação, conformidade de políticas e automação da Google Play Console para o aplicativo Midiastore (`com.rdf112.midiastore`).

---

## 1. Requisito Obrigatório dos 20 Testadores (14 Dias)

Para contas de desenvolvedor criadas após 13/11/2023, a Google Play exige:
- **Mínimo:** 20 testadores humanos com opt-in ativo na faixa de Teste Fechado (Closed Testing).
- **Tempo:** 14 dias ininterruptos de retenção antes de solicitar acesso à Produção.
- **Opt-in:** O testador precisa aceitar o convite web (`https://play.google.com/apps/testing/com.rdf112.midiastore`) ou baixar via Google Play Store no dispositivo Android cadastrado.

### Comandos de Automação de Testadores:
```bash
# Monitorar e sincronizar lista de testadores cadastrados
node scripts/monitor-beta-testers.mjs

# Painel consolidado
node scripts/painel-testadores.mjs

# Sincronização via Play Console API (Service Account)
node scripts/sync-play-testers.mjs
```

A lista oficial para copiar/colar ou sincronizar fica em:
`docs/campaigns/lista-20-testadores-google-play.txt`

---

## 2. Política Estrita de Scoped Storage (Zero Exceções)

A Google Play rejeita sumariamente aplicativos utilitários que solicitem `MANAGE_EXTERNAL_STORAGE` sem justificativa de gerenciador de arquivos central com navegação de sistema de arquivos completo.

### Regras de Implementação no Midiastore:
1. **Nunca declare** `android.permission.MANAGE_EXTERNAL_STORAGE` no `AndroidManifest.xml`.
2. **Utilize exclusivamente** `MediaStore.Files.getContentUri("external")` com filtros por MIME type ou extensão.
3. **Exclusão de Mídia:** Utilize `MediaStore.createDeleteRequest(contentResolver, uris)` (Android 11+ / API 30+) para acionar a folha de diálogo nativa do sistema operacional.
4. **Permissões Granulares (Android 13+ / API 33):**
   - `android.permission.READ_MEDIA_IMAGES`
   - `android.permission.READ_MEDIA_VIDEO`
   - `android.permission.READ_MEDIA_AUDIO`
5. **Legado (Android 9 e anteriores):** `READ_EXTERNAL_STORAGE` com `maxSdkVersion="32"`.

---

## 3. Google Play Developer API (Automação de Tracks e Listings)

O projeto possui credenciais de Service Account configuradas em `.secrets/play-service-account.json`.

### Escopos Obrigatórios:
- `https://www.googleapis.com/auth/androidpublisher`

### Scripts Disponíveis:
- `scripts/play-console-api.mjs`: Teste de conexão e listagem de edições.
- `scripts/play-update-store-listings.mjs`: Atualização de títulos, descrições curtas e completas nos 8 idiomas suportados.
- `scripts/play-setup-products.mjs`: Configuração do produto In-App Lifetime (`midiastore_pro_lifetime`).
- `scripts/play-promote-tracks.mjs`: Promoção do bundle entre faixas (Internal → Alpha/Closed → Production).

---

## 4. Geração do Android App Bundle (AAB) Assinado

O arquivo `app/build.gradle.kts` possui uma trava de segurança intencional:
- `uploadSigningReady` verifica a presença de `keystore.properties` e do arquivo de chave `sweep-upload.jks`.
- Sem esses arquivos locais, `bundleRelease` falha propositalmente para impedir a geração de binários não assinados.

Para gerar a release oficial:
```bash
# Executado apenas após certificar keystore.properties local
./gradlew bundleRelease
```
