# Plano Global & Roadmap Midiastore (`com.rdf112.midiastore`)

Visão do proprietário: transformar o **Midiastore** no **Super App Mundial de Limpeza + Utilidades Diárias + Inclusão Digital**, funcionando em qualquer país do mundo (inclusive **100% offline no ônibus, metrô ou avião**), acessível para **idosos e jovens**, com **preço justo em moeda local** e **retenção diária alta** (fundamental para o teste fechado de 14 dias e crescimento mundial).

---

## 1. Idiomas de Alta População & Inclusão Global
Já ativos: `values` (EN), `pt-BR`, `es`, `fr`, `de`, `hi` (Índia), `ja` (Japão), `zh-CN` (China).
Expansão imediata:
- **`ar` (Árabe + RTL completo)** — Oriente Médio e Norte da África (~400M+ falantes)
- **`ru` (Russo)** — Europa Oriental e Ásia Central (~250M+ falantes)
- **`in` (Indonésio)** — Sudeste Asiático (~275M+ habitantes)
- **`bn` (Bengali)** — Bangladesh e Leste da Índia (~270M+ falantes)
- **`ko` (Coreano)** — Coreia do Sul

---

## 2. Acessibilidade Universal ("Modo Fácil para Idosos")
- **Botão 1-Toque `👓+` no Topo**: alterna o app entre modo padrão e **Modo Fácil / Letras Grandes**.
- Fontes ampliadas (`+25%`), botões grandes com alto contraste (`>= 56dp`), explicações simples no lugar de siglas técnicas (`SHA-256` explicado como *"cópia idêntica segura"*).

---

## 3. Lazer & Utilidade Diária Offline (Retenção no Ônibus / Sem Internet)
- **🐍 Jogo da Cobrinha Clássico (estilo Nokia Retrô):**
  - Funciona **100% sem internet**.
  - Controles duplos: **setas grandes visíveis (ótimo para idosos)** OU toque/deslize.
  - Placar máximo persistido offline, velocidade ajustável e visual Neon Verde / Dourado Pro.
- **🌍 Relógio Mundial dos Países:**
  - Horário exato em tempo real (`TimeZone` oficial offline) dos principais polos do mundo: Brasil, EUA, Índia, Dubai/Riad, Moscou, Tóquio, Pequim, Paris, Berlim, Madri, Jacarta, Londres.
  - Mostra claramente a diferença de horas em relação ao relógio do usuário.
- **💱 Conversor de Moedas Mundial (Offline + Ao Vivo):**
  - Suporta `BRL`, `USD`, `EUR`, `GBP`, `INR`, `SAR`, `AED`, `RUB`, `JPY`, `CNY`, `IDR`, `KRW`, `MXN`, `ARS`.
  - Funciona offline no ônibus e atualiza cotações reais quando conectado (exibindo sempre a origem e se está offline ou atualizado online, sem taxas inventadas).
- **☀️ Clima Real, Guia de Países, Turismo & Restaurantes Reais:**
  - Perfil sazonal offline + pontos turísticos icônicos verificados por país + consulta opcional de clima ao vivo via Open-Meteo (sem API key nem GPS invasivo!).
  - Botões diretos **"Restaurantes Bem Avaliados Perto"** e **"Pontos Turísticos"** integrados ao Google Maps real do dispositivo (sem fabricar avaliações falsas nem exigir permissão extra de GPS no app).

---

## 4. Evoluções Futuras Planejadas (`v2.6+` até `v3.0`)
- **`v2.6.0` — Motor Inteligente de Bateria & Rotina Diária:** lembretes locais opcionais de limpeza semanal e alerta de espaço crítico (via `WorkManager`, zero drenagem de bateria).
- **`v2.7.0` — Guias Regionais Expandidos + Lista Offline de Emergências do Viajante:** números de emergência globais por país (ex.: `190/192`, `112`, `911`, `100`) e conversor de medidas (°C/°F, km/mi, kg/lb, voltagem/tomadas).
- **`v2.8.0` — Otimização Regional de Planos no Google Play Console:** revisão trimestral de preços regionais por paridade de poder de compra (PPP) e ofertas introdutórias de teste gratuito por país.
- **`v3.0.0` — Backend Seguro de Validação de Assinaturas & Sync Opcional:** validação oficial de *purchase tokens* via Google Play Developer API no Supabase Edge Functions antes da liberação Pro em produção global.
