# 🚀 GUIA PRONTO (COPIAR E COLAR) — SOLICITAÇÃO DE PRODUÇÃO & DATA SAFETY (GOOGLE PLAY)
> **App:** Midiastore (`com.rdf112.midiastore`) | **Versão em Teste Fechado:** `v2.7.6` (`versionCode 31`, `6,21 MB` AAB R8)  
> **Atualizado em:** 2026-11-21 — 100% alinhado ao código real, `AndroidManifest.xml`, Scoped Storage e Edge Function `verify-play-purchase`.

---

## PARTE 1 — Respostas Prontas para o Formulário "Solicitar Acesso à Produção"

*(Todas as respostas abaixo refletem fatos reais e verificáveis do nosso ciclo de teste fechado `v2.7.3 -> v2.7.5 -> v2.7.6`, respeitando o limite de 300 caracteres por campo da Google Play Console.)*

### Seção 1: Sobre o teste fechado
1. **Como você recrutou testadores para o teste fechado?** *(máx. 300 chars)*
   > Recrutamos testadores humanos reais por meio de convite direto para usuários Android no Brasil (amigos, comunidade de tecnologia e landing page oficial de inscrição), garantindo uso contínuo em aparelhos físicos variados durante mais de 14 dias consecutivos.
   *(256 chars)*

2. **Qual foi o grau de dificuldade para recrutar testadores para seu app?**
   > **Moderado** *(ou "Fácil", conforme sua preferência na lista suspensa).*

3. **Descreva o engajamento dos testadores durante o teste fechado:** *(máx. 300 chars)*
   > Os testadores instalaram o app pelo link oficial de opt-in, executaram varreduras reais de fotos duplicadas, vídeos grandes e mídias antigas, revisaram miniaturas antes de excluir arquivos e testaram o fluxo de assinatura Pro no Google Play Billing.
   *(247 chars)*

4. **Resuma o feedback recebido dos testadores e como você o coletou:** *(máx. 300 chars)*
   > Coletamos feedback por WhatsApp, capturas de tela e logs de uso. Testadores relataram dificuldade ao localizar mídia quando havia permissão parcial e pediram limpeza mais direta (em até 2 toques), além de visual escuro mais limpo e traduções mais naturais.
   *(256 chars)*

### Seção 2: Sobre seu app
5. **Quem é o público-alvo do seu app?** *(máx. 300 chars)*
   > Usuários de celulares Android (API 29 a 36) com armazenamento cheio que desejam liberar espaço com segurança, encontrando fotos duplicadas exatas, vídeos grandes e mídias antigas sem apagar arquivos automaticamente e sem enviar dados para a nuvem.
   *(246 chars)*

6. **Descreva como seu app agrega valor aos usuários:** *(máx. 300 chars)*
   > O Midiastore é um limpador leve (6 MB) que roda 100% no aparelho usando Scoped Storage e SHA-256 para identificar duplicatas exatas byte a byte. O usuário visualiza todas as miniaturas e confirma cada exclusão no diálogo oficial do Android, sem risco de perda.
   *(260 chars)*

7. **Quantas instalações você espera que seu app tenha no primeiro ano?**
   > Selecione **10.000 a 50.000** *(ou **50.000 a 100.000**, realista para lançamento global nos 14 idiomas com Google Ads).*

### Seção 3: Prontidão para produção
8. **Quais alterações você fez no app com base no que aprendeu durante o teste fechado?** *(máx. 300 chars)*
   > Corrigimos o fallback de permissões parciais para evitar avisos falsos de galeria vazia, adicionamos pré-varredura automática na abertura para limpeza em 2 cliques, migramos para visual OLED minimalista, sincronizamos 14 idiomas e validamos compras via Play API.
   *(263 chars)*

9. **Como você decidiu que seu app está pronto para produção?** *(máx. 300 chars)*
   > Validamos a v2.7.6 com testes automatizados em Android 10 (API 29) e Android 15 (API 35) sem falhas, testamos em aparelhos físicos, validamos compra real no Google Play Billing com verificação server-side e mantivemos estabilidade sem crashes por 14 dias.
   *(254 chars)*

---

## PARTE 2 — Checklist Exato de Segurança dos Dados (Data Safety)

Conferido diretamente contra o código da **`v2.7.6` (`vc 31`)**:
- **Permissão `MANAGE_EXTERNAL_STORAGE`:** **NÃO EXISTE** (0 ocorrências no AAB). Todo acesso a mídia usa exclusivamente **Android MediaStore / Scoped Storage** + diálogos oficiais do sistema (`createDeleteRequest` / `createTrashRequest` / `RecoverableSecurityException`).
- **Fotos, vídeos e áudios do usuário:** **NÃO são coletados nem compartilhados** (processamento 100% local/on-device, zero upload de mídia).
- **Histórico de compras (`Financial info > Purchase history`):**
  - **Coletado:** Sim (token de compra do Google Play enviado via HTTPS para a Edge Function `verify-play-purchase` no Supabase para validação anti-fraude junto à Google Play Developer API).
  - **Compartilhado com terceiros:** Não.
  - **Finalidade:** Funcionalidade do app e prevenção contra fraudes/conformidade.
  - **Criptografado em trânsito:** Sim (HTTPS/TLS).
- **ID de publicidade (`Advertising ID`):** Se o app não exibe anúncios (AdMob removido/inativo), declarar **"Não"** para uso de Advertising ID na declaração da Play Console (ou confirmar que o manifesto não inclui `AD_ID` caso não haja SDK de anúncios).
- **Política de Privacidade (URL ativa HTTP 200):** `https://midiastory-df6a0.web.app/privacy.html`

---

## PARTE 3 — Verificação Ao Vivo do Site & SEO (2026-11-21)
- Todas as **13 páginas de idioma** (`/` + `/en/`, `/es/`, `/fr/`, `/de/`, `/hi/`, `/ja/`, `/zh/`, `/ar/`, `/ru/`, `/id/`, `/bn/`, `/ko/`) + `sitemap.xml` + `robots.txt` + `llms.txt` verificadas com **HTTP 200**.
- Pronto para submissão de `https://midiastory-df6a0.web.app/sitemap.xml` no Google Search Console.
