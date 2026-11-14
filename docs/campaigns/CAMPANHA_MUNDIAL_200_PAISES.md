# Campanha Mundial de Lançamento — Midiastore (`com.rdf112.midiastore`)

> [!IMPORTANT]
> Documento estratégico operacional construído apenas sobre recursos realmente implementados no app `v2.4.0 (versionCode 21)` e preços reais ativos na Google Play Developer API (`2026-11-14`). Sem alegações proibidas pela política da Google Play / Google Ads.

---

## 1. Links Oficiais & Arquitetura de Tracking Global (UTMs)

- **Download Oficial (Google Play):** `https://play.google.com/store/apps/details?id=com.rdf112.midiastore`
- **Funil Web 8 Idiomas (Ponte Mundial & Pré-aquecimento):** `https://midiastory-df6a0.web.app/`
- **Rotas Localizadas Prontas no Firebase Hosting:**
  - PT-BR: `https://midiastory-df6a0.web.app/`
  - EN (EUA/Reino Unido/Global): `https://midiastory-df6a0.web.app/en/`
  - ES (Espanha/LATAM): `https://midiastory-df6a0.web.app/es/`
  - DE (DACH — Alemanha/Áustria/Suíça): `https://midiastory-df6a0.web.app/de/`
  - FR (França/Canadá/África Francófona): `https://midiastory-df6a0.web.app/fr/`
  - HI (Índia): `https://midiastory-df6a0.web.app/hi/`
  - JA (Japão): `https://midiastory-df6a0.web.app/ja/`
  - ZH (Mercados Chineses Globais / Web): `https://midiastory-df6a0.web.app/zh/`

### Padrão Obrigatório de Link Rastreável da Play Store (Referrer / UTM)
Para saber exatamente **qual país, plataforma e criativo gera instalações e compras Pro**, todo link publicado em anúncios, bio ou vídeos deve usar `referrer=` na URL oficial da Play Store:

```text
https://play.google.com/store/apps/details?id=com.rdf112.midiastore&referrer=utm_source%3D<CANAL>%26utm_medium%3D<FORMATO>%26utm_campaign%3Dlaunch_<ONDA>_<PAIS>
```

---

## 2. Estratégia Mundial por Ondas (175+ Mercados Google Play + 200+ Regiões Web/Social)

A Google Play Console permite distribuição em **todos os países/territórios suportados na loja (~175+ mercados)**; somando páginas web e redes sociais, a aquisição alcança **200+ países e territórios** direcionando usuários Android compatíveis (Android 10+ / `minSdk 29`) para o download oficial.

### Onda 1 — Alta Receita & Retorno Rápido de Caixa (Tier 1)
- **Mercados:** EUA (`en-US`), Reino Unido, Canadá (`en`/`fr`), Austrália, Alemanha/Áustria/Suíça (`de-DE`), França (`fr-FR`), Japão (`ja-JP`).
- **Preços Ativos Verificados via Google Play API (`2026-11-14`):** EUA `US $4.99/mês`, `US $29.99/ano`, `US $49.99` vitalício (`GB £4.49 / £26.99 / £45.49`, `DE/FR €5.49 / €31.99–32.99 / €54.99`, `CA CA$6.99 / CA$42.99 / CA$70.99`, `AU A$7.99 / A$48.99 / A$80.99`, `JP ¥880 / ¥5.300 / ¥8.800`; taxa Play de 15% nas assinaturas e até US$ 1M/ano).
- **Ângulo que mais converte em Tier 1:** **Privacidade 100% no Aparelho + Limpador EXIF GPS + Cópia Exata SHA-256** (*"Limpe gigabytes sem enviar suas fotos para a nuvem de ninguém"*).
- **Orçamento pago sugerido após aprovação em Produção:** campanhas **Google Ads App Campaigns (ACi)** segmentando tCPA por instalação e depois evento de assinatura confirmada (`in_app_purchase`).

### Onda 2 — Escala Massiva com Alto Volume & Boa Conversão (Tier 2)
- **Mercados:** Brasil (`pt-BR`), México, Colômbia, Argentina, Chile, Espanha (`es-ES`), Portugal, Itália, Polônia, Coreia do Sul, Taiwan, Singapura.
- **Preços Ativos no Brasil:** `R$ 14,99/mês`, `R$ 79,99/ano` (âncora de maior economia), `R$ 149,99` vitalício.
- **Ângulo que mais converte em Tier 2:** **WhatsApp / Vídeos Grandes (>10 MB) + Fotos para PDF + Extrator M4A**, resolvendo o aviso *"Armazenamento quase cheio"* em 60 segundos com confirmação oficial do Android antes de apagar qualquer arquivo.

### Onda 3 — Tração Orgânica Mundial & Volume Gigante (Tier 3 / 200+ Territórios)
- **Mercados:** Índia (`hi-IN` / `en`), Indonésia, Filipinas, Vietnã, Nigéria, Quênia, África do Sul, Egito, América Central, Caribe e Oriente Médio.
- **Estratégia de Custo Zero/Baixo CPI:** Shorts/Reels/TikTok verticais sem texto complexo, SEO local no site Firebase (`diagnostico.html`, `ferramentas.html`), comunidades Android e compartilhamento nativo pelo próprio botão do app.
- **Ajuste de Preço Regional (PPP) pós-validação:** utilizar a tabela de Paridade de Poder de Compra (PPP) nativa do Google Play Console para mercados emergentes para maximizar volume de assinantes ativos.

---

## 3. Matemática Real Rumo a R$ 1.000.000,00 Líquidos

Considerando a **taxa de 15% da Google Play** em assinaturas e no primeiro **US$ 1.000.000/ano** no programa de desenvolvedores:

| Plano Ativo na Play Console | Preço Bruto Referência | Líquido Estimado (~85%) | Volume p/ R$ 1.000.000,00 |
|---|---:|---:|---:|
| `sweep_pro_annual` | R$ 79,99/ano (BR) / US$ 29.99 (EUA) | ~R$ 67,99 (BR) / ~US$ 25.49 | **~7.000–14.708 assinaturas anuais** *(menor volume necessário vendendo em Tier 1 USD/EUR/GBP)* |
| `sweep_pro_monthly` | R$ 14,99/mês (BR) / US$ 4.99 (EUA) | ~R$ 12,74 (BR) / ~US$ 4.24/mês | ~3.400–6.541 assinantes médios ativos por 12 meses |
| `sweep_pro_lifetime` | R$ 149,99 (BR) / US$ 49.99 (EUA) | ~R$ 127,49 (BR) / ~US$ 42.49 | ~4.250–7.844 licenças vitalícias |
| **Mix Internacional Ponderado (Tier 1 USD + BR)** | **US$ 29.99–49.99 (Tier 1) / R$ 79,99–149,99 (BR)** | **~R$ 110–180 / pagante** | **~5.600 a 9.100 clientes pagantes globais** |

---

## 4. "Pulo do Gato" na Política da Google Ads & Play Store para Anúncios Escaláveis

Para rodar campanhas globais sem reprovação da conta de anúncios nem da Play Console:
1. **Permitido e altamente persuasivo:** mostrar o medidor real de espaço, a detecção exata SHA-256 protegendo o original, a lista de arquivos >10 MB, o limpador de coordenadas GPS EXIF antes de enviar fotos e as ferramentas do Estúdio (M4A, QR, Fotos em PDF).
2. **Estritamente PROIBIDO em anúncios/vitrine (gera reprovação automática por *"Misleading Claims / Unwanted Software"*):**
   - Nunca prometer *"deixar RAM mais rápida / turbo / resfriar CPU"*;
   - Nunca prometer *"limpar vírus / cache oculto de outros apps"*;
   - Nunca afirmar *"fotos borradas / IA de similaridade visual / compressão de 80%"*;
   - Nunca divulgar *"3 meses grátis"* se não existir oferta promocional ativa cadastrada no Google Play Billing.

---

## 5. Roteiros Prontos de Anúncios & Vídeos Curtos (Reels / TikTok / YouTube Shorts / Google ACi)

### Roteiro PT-BR (Brasil — 15 segundos)
- **Hook Visual (0–3s):** Tela real do app mostrando armazenamento ocupado + pasta pesada de vídeos `>10 MB`.
- **Narração / Texto na Tela (3–10s):** *"Celular cheio de vídeo grande e foto repetida do WhatsApp? O Midiastore encontra arquivos gigantes e cópias exatas com cálculo SHA-256 — 100% no seu aparelho, sem mandar nenhuma foto pra nuvem."*
- **Prova & Segurança (10–13s):** *"Você revisa tudo antes de apagar e confirma direto no aviso oficial do Android. Ainda vem com limpador de GPS EXIF e conversor de fotos para PDF."*
- **CTA Final (13–15s):** *"Baixe grátis o Midiastore na Google Play."* + UTM `utm_campaign=launch_w2_br`.

### Roteiro EN-US (Global / EUA / Reino Unido — 15 segundos)
- **Hook (0–3s):** *"Running out of phone storage—and hate apps that upload your private photos?"*
- **Body (3–11s):** *"Midiastore scans large files (>10 MB) and verifies exact byte-for-byte duplicates with SHA-256 100% locally on your Android device. Originals stay protected. Plus strip hidden GPS EXIF tags and build PDFs offline."*
- **CTA (11–15s):** *"Review every file before Android asks to delete. Get Midiastore free on Google Play."*

### Roteiro ES (América Latina & Espanha — 15 segundos)
- **Hook (0–3s):** *"¿Sin espacio en el celular por videos pesados y archivos duplicados?"*
- **Body (3–11s):** *"Midiastore analiza todo 100% local en tu teléfono sin subir tus fotos. Encuentra archivos >10 MB y copias exactas por SHA-256 protegiendo el original, con herramientas locales de EXIF, QR y PDF."*
- **CTA (11–15s):** *"Descárgalo gratis en Google Play y decide qué borrar con total control."*

---

## 6. Roadmap Pós-Aprovação (Automação Escalável)

1. **`PLAY-03` (Imediato pré-lançamento global):** publicar os 32 screenshots localizados (`docs/campaigns/store-screenshots/<lang>/`) para eliminar os `0 screenshots` em `es-ES`, `fr-FR`, `de-DE`, `hi-IN`, `ja-JP` e `zh-CN`.
2. **`PLAY-02` (Conversão + LTV):** testar oferta introdutória / **Free Trial (3 ou 7 dias)** nos base plans `monthly-base` e `annual-base` após os primeiros testes com License Testers.
3. **`AI-02` (Destravamento Carbon):** vincular Google Cloud `$300` Free Trial antes de `16/11/2026` no projeto da `GOOGLE_AI_STUDIO_API_KEY` para liberar `gemini-4-carbon` em pipelines editoriais/ASO de custo zero inicial.
4. **`GROWTH-02` (Concierge Telegram Inbound Opt-in):** lançar bot oficial `@MidiastoreBot` alimentado por links de Reels/Shorts/TikTok/site (zero spam de cold DM para proteger conta/marca) conduzindo usuários interessados em limpar espaço até a instalação e assinatura Pro na Play Store.
