# Kit de Outreach: Recrutamento de 20 Testadores - Comunidades Tech

## Objetivo
Converter desenvolvedores, power users e moderadores de grupos técnicos (Telegram, Discord, Reddit, LinkedIn) em testadores ativos no Play Console, validando o `com.rdf112.midiastore` e gerando reviews iniciais 5 estrelas.

---

## 1. Perfis-Alvo (ICP)
- **Devs Android/Kotlin**: Sensíveis a `MediaStore API`, `Scoped Storage`, performance de I/O.
- **Admins de Grupos "WhatsApp Dicas/Backups"**: Influenciam milhares com dor real de armazenamento.
- **Entusiastas de Privacidade/FOSS**: Valorizam app *offline-first*, sem analytics, open source (se aplicável).

---

## 2. Mensagens Prontas (Copy/Paste)

### A. Frio / Inicial (LinkedIn / Telegram DM)
> "Oi [Nome], vi seu perfil/participação no [Grupo]. Estou fechando o grupo de **20 Testadores Oficiais** do **Midiastore** (app que limpa lixo do WhatsApp *sem apagar conversas* usando MediaStore nativo).
>
> **O ganho para você:** Acesso vitalício Pro (R$ 0), nome nos créditos, e influência direta no roadmap (ex: suporte a pastas customizadas).
>
> **Requisito:** Android 10+, 5 min para testar fluxo de "Varredura Profunda" e reportar *crashes* via Play Console.
>
> Topa? Te mando o link do Opt-in agora."

### B. Follow-up Técnico (Devs)
> "Sem pressão. Só detalhe técnico: usamos `ContentResolver.query` com `MediaStore.Files` + `isPending` para achar `.opus/.jpg` órfãos em `Android/media/com.whatsapp` sem `MANAGE_EXTERNAL_STORAGE`. Roda em background com `WorkManager`. Se quiser dar uma olhada no código de varredura antes, avisa."

### C. Para Admins de Comunidade (Valor Coletivo)
> "[Nome], preparei um **Artigo Exclusivo + APK Antecipado** para seu grupo: "Como recuperar 5GB no WhatsApp sem root em 2026". Em troca, peço que fixe a mensagem com link do Play Console por 48h. Fechamos?"

---

## 3. Assets de Apoio (Entregar no Drive/Notion)
1. **One-pager PDF**: Benefícios do Testador (Vitalício, Roadmap, Suporte Direto).
2. **Guia Rápido (PDF)**: "Como entrar no Programa de Testes no Play Console" (prints passo a passo).
3. **Changelog v0.9.0-beta**: Foco no que testar: *Varredura Profunda, Agendamento, WhatsApp Business*.

---

## 4. Planilha de Controle (Notion/Airtable)
| Contato | Canal | Status (Contatado/Aguardando/Ativo/Rejeitou) | Link Opt-in Enviado? | Feedback Recebido? |
|---|---|---|---|---|

---

## 5. Gatilhos de Urgência Ética
- "Restam **X de 20 vagas" (Atualizar diariamente).
- "Build Release Candidate sai sexta, testadores entram na build estável primeiro."

---

## Métrica de Sucesso do Kit
**20 Opt-ins confirmados no Play Console até Ciclo #40.**