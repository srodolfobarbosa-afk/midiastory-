# Declaração de Segurança dos Dados (Data Safety) — Google Play Console (Sweep)

Use este guia direto para preencher ou revisar o formulário **Segurança dos dados (Data safety)** no Google Play Console para o aplicativo **Sweep (`com.rdf112.sweep`)**.

## 1. Resumo de Coleta e Compartilhamento
- **O app coleta ou compartilha algum dos tipos de dados obrigatórios do usuário?**
  - **Fotos, vídeos, áudios e arquivos no dispositivo:** **NÃO coletados e NÃO compartilhados.** Todo o escaneamento (`MediaStore`), cálculo de hash SHA-256 em fluxo e preparação de lotes OaaS ocorre **100% localmente** na memória do dispositivo Android. Nenhuma mídia, miniatura, nome de arquivo ou URI sai do aparelho.
  - **Histórico de compras / Assinaturas (`Financial info / Purchase history`):** Gerenciado exclusivamente pelo **Google Play Billing Library 8.0.0** para verificar o direito de acesso aos planos `sweep_pro_monthly`, `sweep_pro_annual` e `sweep_pro_lifetime`.
- **O Radar OaaS envia dados para servidores externos?**
  - **Não.** O Radar OaaS armazena estatísticas agregadas e sugestões opcionais apenas em `SharedPreferences` privado do aplicativo no próprio dispositivo, mediante caixa de consentimento explícita (*opt-in* desmarcado por padrão), com botão dedicado de exclusão imediata (*"Apagar histórico local do Radar OaaS"*).

## 2. Respostas Recomendadas no Formulário da Play Console
1. **Coleta de dados:**
   - Se você mantiver o app sem backend próprio de telemetria (como está no código-fonte v2.1.0), marque que o aplicativo **não envia dados pessoais ou arquivos de mídia para servidores externos**, declarando apenas o processamento de transações da própria Google Play caso exigido pelo fluxo de Billing.
2. **Práticas de segurança:**
   - Comunicações com a Google Play Store usam criptografia em trânsito (TLS oficial do Google Play Services).
   - O usuário pode limpar integralmente os dados locais a qualquer momento pelo botão interno **Apagar histórico local do Radar OaaS** ou pelas configurações do Android.
