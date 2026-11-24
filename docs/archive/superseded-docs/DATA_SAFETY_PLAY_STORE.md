# Segurança dos dados — Midiastore (`com.rdf112.midiastore`)

Este documento é um inventário técnico preliminar, não uma resposta pronta para enviar à Play Console. Preencha o formulário Data Safety com base no AAB final, nas declarações de SDKs e nos fluxos realmente habilitados na versão distribuída.

## Identidade

- Listing / nome comercial: Midiastore.
- Application ID: `com.rdf112.midiastore`.
- Namespace legado no código: `com.rdf112.sweep`.

## O que o código atual faz localmente e via rede (`2026-11-14`)

- Consulta fotos, vídeos e áudios acessíveis via MediaStore (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED`; zero `MANAGE_EXTERNAL_STORAGE`).
- Calcula SHA-256 em streaming e executa as ferramentas do Estúdio de Mídia (filtros, extração M4A, limpeza GPS EXIF, QR Code e Fotos para PDF) integralmente no dispositivo. Mídias e miniaturas nunca são enviadas a servidores.
- O recurso opcional de apps sem uso há 90+ dias (`PACKAGE_USAGE_STATS`) exige **Prominent Disclosure + consentimento explícito** em diálogo no app antes de abrir Ajustes; timestamps ficam só no aparelho.
- Mantém seleção, estado de revisão e telemetria agregada opt-in localmente em `SharedPreferences`.
- Compartilhamento de relatório (`ACTION_SEND`) só ocorre quando acionado manualmente pelo usuário para o app de destino escolhido por ele.

## Google Play Billing e Verificação Server-Side (`verify-play-purchase`)

O cliente usa Google Play Billing Library 8.0.0 e envia `{ packageName, productId, purchaseToken }` via HTTPS para `https://usmqtnlsaavxqguwrtix.supabase.co/functions/v1/verify-play-purchase`, que consulta a Google Play Developer API e grava apenas metadados técnicos de auditoria em `public.play_purchases` (`package_name`, `product_id`, `order_id`, `raw_status` e **hash SHA-256 irreversível** do `purchase_token` — nunca dados de cartão ou token bruto).

### Mapeamento Prático para o Formulário Data Safety (Play Console)
1. **O app coleta ou compartilha algum dado obrigatório de usuário?** Declare com transparência **Transações financeiras / Histórico de compras** (Processamento de compra + Prevenção a fraudes via token de compra verificado no servidor de backend) com criptografia em trânsito (HTTPS).
2. **Fotos, vídeos e áudios:** Não coletados nem transferidos para fora do aparelho (processamento 100% on-device).
3. **Uso de aplicativos (`PACKAGE_USAGE_STATS`):** Processado exclusivamente localmente após Prominent Disclosure; não transmitido.
4. **Site de teste fechado (`midiastory-df6a0.web.app`):** Coleta opcional de e-mail Gmail apenas se o usuário preencher o formulário web para opt-in no teste fechado (descrito em `https://midiastory-df6a0.web.app/privacidade.html`).

## Regras para o formulário Data Safety

1. Não copiar respostas de uma versão anterior ou reconstrução.
2. Verificar todos os SDKs e declarações de coleta/compartilhamento presentes no AAB final.
3. Explicar que o acesso às mídias é usado para análise local e que o usuário autoriza operações de limpeza via Android.
4. Descrever claramente a exportação do relatório e que o usuário escolhe o destino de compartilhamento.
5. Revisar dados de transação/Billing conforme os requisitos aplicáveis da Play Console.
6. Manter política de privacidade, Data Safety, permissões do manifesto e comportamento real em conformidade.
7. Repetir a revisão sempre que forem adicionados backend, analytics, crash reporting, anúncios, compartilhamento automático ou novos SDKs.

## Retenção e controle

- O histórico de missão local pode ser apagado pelo próprio app.
- O usuário pode revogar acesso a fotos/vídeos nas configurações do Android.
- Nenhuma mídia deve ser enviada a um servidor para análise sem decisão explícita de produto, disclosure, consentimento e revisão jurídica/Play adequados.

**Bloqueio:** este documento não autoriza concluir o formulário Data Safety nem declarar conformidade. A revisão deve ser feita sobre o binário final e o conjunto de SDKs real.