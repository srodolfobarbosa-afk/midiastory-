# Segurança dos dados — Midiastore (`com.rdf112.midiastore`)

Este documento é um inventário técnico preliminar, não uma resposta pronta para enviar à Play Console. Preencha o formulário Data Safety com base no AAB final, nas declarações de SDKs e nos fluxos realmente habilitados na versão distribuída.

## Identidade

- Listing / nome comercial: Midiastore.
- Application ID: `com.rdf112.midiastore`.
- Namespace legado no código: `com.rdf112.sweep`.

## O que o código atual faz localmente

- Consulta fotos, vídeos e áudios acessíveis via MediaStore.
- Calcula SHA-256 em streaming no próprio dispositivo para comparar conteúdo.
- Mantém seleção e estado de revisão localmente.
- Armazena telemetria agregada local do Radar OaaS e sugestões opt-in em SharedPreferences.
- Não há backend próprio de mídia/telemetria configurado nesta branch.

Não afirmar que nenhum dado sai do aparelho sem considerar o fluxo de compartilhamento do relatório (`ACTION_SEND`), Google Play Billing, tráfego de SDKs, diagnósticos/relatórios de falha e qualquer serviço que venha a ser adicionado.

## Google Play Billing

O cliente usa Billing Library 8.0.0 para consultar produtos e compras. Transações são processadas pelo Google Play. A presença de campos de assinatura/JSON no objeto Purchase não equivale à validação criptográfica do purchaseToken por backend. Antes da publicação comercial, definir e documentar a fonte de verdade do entitlement e verificar as compras conforme a integração final.

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