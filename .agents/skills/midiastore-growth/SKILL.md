---
name: midiastore-growth
description: Protocolo de Operação e Crescimento do Midiastore. Use para procedimentos de gestão dos 20 testadores fechados, sincronização da Google Play Console API, manutenção do Supabase e deployment do funil no Firebase Hosting.
---

# Guia Operacional & Procedimentos do Midiastore

Este skill define o padrão oficial para gerenciamento de qualidade, captação e publicação do aplicativo Midiastore (`com.rdf112.midiastore`).

---

## 1. Monitoramento de Testadores Fechados (Meta 20)

Para auditar e exportar a lista de testadores:
```bash
# Ver painel de controle
node scripts/painel-testadores.mjs

# Executar monitor contínuo
node scripts/monitor-beta-testers.mjs
```
A lista formatada para a Google Play Store é gerada automaticamente em:
`docs/campaigns/lista-20-testadores-google-play.txt`

---

## 2. Manutenção e Compactação do Supabase

O banco do Supabase possui cota de 500 MB no plano gratuito.
Para verificar armazenamento e executar deduplicação preventiva:
```bash
node scripts/supabase-maintenance.mjs
```

---

## 3. Deployment das Páginas em 8 Idiomas (Firebase Hosting)

Quando qualquer copy ou estilo for atualizado:
```bash
# 1. Compilar as 8 variações de idioma (docs/{en,es,de,fr,hi,ja,zh}/index.html)
node scripts/generate-multilang-pages.mjs

# 2. Publicar no Firebase Hosting
node scripts/firebase-deploy-site.mjs
```

---

## 4. Ciclo de Auto-Aperfeiçoamento (Self-Healing)

Para validar o código com autocorreção automática via Codex CLI:
```bash
node scripts/self-healing-engine.mjs "gradlew.bat testDebugUnitTest"
```

---

## 5. Regras de Ouro de Scoped Storage (Google Play)

1. **Nunca solicitar `MANAGE_EXTERNAL_STORAGE`:** Utilizar apenas `MediaStore.Files.getContentUri("external")` e Storage Access Framework (SAF).
2. **Exclusão em Lote:** Sempre delegar pedidos de remoção em massa para `MediaStore.createDeleteRequest(contentResolver, uris)` para exibir um único diálogo nativo do sistema Android.
3. **Validação SHA-256:** Arquivos duplicados devem ser confirmados por hash binário antes da sugestão de limpeza.
