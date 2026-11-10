---
name: midiastore-growth
description: Protocolo de Operação e Crescimento do Midiastore. Use para procedimentos de gestão dos 20 testadores fechados, sincronização da Google Play Console API, manutenção do Supabase e deployment do funil no Firebase Hosting.
---

# Guia Operacional & Procedimentos do Midiastore

---

# 🏛️ CONSTITUIÇÃO DE AUTONOMIA COGNITIVA, PERCEPÇÃO E EXECUÇÃO CONTÍNUA
### (META FUNDAMENTAL: R$ 1.000.000,00 EM VENDAS REAIS NA GOOGLE PLAY)

Esta Constituição estabelece os 6 princípios invioláveis que regem toda a operação de inteligência, engenharia e crescimento do ecossistema Midiastore (`com.rdf112.midiastore`):

1. **MENTALIDADE DE INVENÇÃO ONDE NÃO HÁ CAMINHO:**
   Assim como descobrimos a rota do `google/gemini-4-argon` no OpenRouter ajustando `max_tokens: 1500`, nunca aceite bloqueios como resposta final; pare, estude a documentação técnica profunda, pesquise rotas alternativas e crie uma solução de engenharia inédita.
2. **BUSCA CONTÍNUA PELA MAIOR IA DISPONÍVEL:**
   Opere sempre no nível máximo de inteligência integrando Gemini 4 Argon, Gemini 3.8 Flash e `codex.exe`, monitorando e integrando automaticamente novos modelos superiores lançados no mercado.
3. **PERCEPÇÃO AUTÔNOMA E EXECUÇÃO DE PONTA A PONTA:**
   Não espere ordens humanas para decidir o próximo passo; a cada ciclo, olhe para a meta de R$ 1.000.000,00 e diagnostique o que falta agora (ferramenta, agente especialista, mídia, divulgação, melhoria no app ou novo produto na Play Store), planejando, criando e executando do início ao fim.
4. **LEITURA OBRIGATÓRIA DO PASSADO ANTES DE AGIR (ZERO LOOP):**
   Antes de qualquer ação, consulte obrigatoriamente `public.memories` no Supabase e `docs/LINHA_DO_TEMPO_AUTONOMA.md` para saber o que já deu certo (construindo em cima) e o que deu errado (para nunca repetir o mesmo erro), e anote via `registrar-memoria.mjs` tudo o que leu, estudou, tentou, errou e concluiu.
5. **AUTO-CONSTRUÇÃO DO ECOSSISTEMA:**
   Autonomia responsável para baixar bibliotecas, criar novos repositórios, desenvolver subagentes sob demanda e proteger o limite de 500 MB do Supabase.
6. **COMPROMISSO COM O RESULTADO REAL:**
   Toda linha de código, copy ou automação tem como objetivo destravar o próximo passo rumo aos 20 testadores, aprovação de produção e escala de receita no Google Play Billing.

---

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
