# Diretrizes do Projeto Midiastore (`com.rdf112.midiastore`)

Este documento define as regras, padrões arquiteturais e diretrizes operacionais para qualquer agente ou desenvolvedor atuando neste repositório.

---

## 1. Escopo & Arquitetura Geral

- **Aplicativo Android:** `com.rdf112.midiastore` (v2.3.1, versionCode 20).
  - Stack: Kotlin, Jetpack Compose, Material 3, AndroidX MediaStore API, Room/SQLite.
  - Missão: Limpeza e otimização de mídias (WhatsApp Cleaner Pro, detecção de duplicatas SHA-256 e arquivos gigantes).
- **Funil Web Internacional:**
  - Hospedagem: Firebase Hosting (`https://midiastory-df6a0.web.app/`).
  - Suporte a 8 idiomas: PT, EN, ES, DE, FR, HI, JA, ZH e artigos de SEO.
- **Backend & Orquestração:**
  - Supabase PostgreSQL (`usmqtnlsaavxqguwrtix`) com extensão `pgvector`.
  - Tabelas: `public.beta_testers`, `public.memories`, `public.documents`, `public.orchestrator_logs`, `public.orchestrator_tasks`.
- **Inteligência Artificial & Failover:**
  - Motor: `scripts/ai-failover-router.mjs` (Gemini 3.8 Flash → Gemini 3.1 Flash-Lite → Gemini 4 Argon → GPT-4o Mini → Llama 3.3 70B → OpenAI Codex CLI local).

---

## 2. Regras Estritas de Scoped Storage & Google Play

1. **PROIBIDO `MANAGE_EXTERNAL_STORAGE`:** Sob nenhuma hipótese adicione esta permissão ao `AndroidManifest.xml`.
2. **MediaStore Exclusivo:** Toda listagem deve utilizar `MediaStore.Files.getContentUri("external")` ou queries específicas de áudio/vídeo/imagem.
3. **Exclusões de Mídia:** Utilize `MediaStore.createDeleteRequest(contentResolver, uris)` para exclusões em massa em Android 11+ (API 30+).
4. **Requisito de 20 Testadores Fechados:** A liberação para produção na Google Play depende de 20 testadores humanos ativos durante 14 dias contínuos. A lista é gerada em `docs/campaigns/lista-20-testadores-google-play.txt`.

---

## 3. Diretrizes de Qualidade, Build e Testes

1. **Verificação de Compilação:** Antes de finalizar qualquer alteração em Kotlin, execute `./gradlew compileDebugKotlin` ou `./gradlew testDebugUnitTest`.
2. **Trava de Release:** A tarefa `bundleRelease` possui validação estrita em `app/build.gradle.kts` que exige `keystore.properties` e `sweep-upload.jks` válidos. Não remova essa trava de segurança.

---

## 4. Gestão do Supabase & Cota Zero-Cost

1. O banco opera no plano gratuito com limite de 500 MB.
2. A rotina `scripts/supabase-maintenance.mjs` monitora o volume utilizado e previne ultrapassagens acima de 400 MB.
3. Registros de `public.orchestrator_logs` e tarefas concluídas com mais de 7/14 dias são elegíveis para purga automática.

---

## 5. Segurança Operacional e Governança

1. O agente opera estritamente dentro dos limites da sessão, respeitando aprovações humanas.
2. É terminantemente proibido criar laços recursivos infinitos de processos do agente ou tentar burlar confirmações de segurança do sistema operacional.
