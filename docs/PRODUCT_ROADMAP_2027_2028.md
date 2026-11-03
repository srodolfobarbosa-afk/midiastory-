# Sweep — roteiro de produto 2027–2028

Este roteiro é uma direção de produto, não uma lista de funções já implementadas.

## P0 — Segurança e continuidade da atualização

- Recuperar o fonte original, ou comparar a reconstrução com todas as telas, dados, produtos e fluxos da versão Play atual.
- Reintegrar Google Play Billing Library 8+ e validar compras, restauração, estados de compra, entitlement e modo offline.
- Confirmar certificado/upload key, versionCode, target API e manifesto do AAB.
- Executar testes de permissões, MediaStore, Lixeira e restauração em aparelhos físicos.
- Corrigir qualquer bloqueio de build e manter um pipeline reproduzível.

## P1 — Gerenciamento de armazenamento confiável

- Tornar a paginação MediaStore verificável por provider, sem declarar cobertura completa quando o provider não confirma a consulta.
- Adicionar seleção e revisão de arquivos por categorias, com prévia e revalidação das URIs antes de operações.
- Cobrir cartões SD e volumes removíveis em aparelhos físicos de fabricantes diferentes.
- Acrescentar histórico local de análises, com retenção transparente e opção de apagar histórico.
- Integrar Downloads e documentos usando Storage Access Framework e pastas escolhidas pelo usuário.

## P2 — Inteligência local de mídia

- Identificar fotos visualmente semelhantes por perceptual hashing/embeddings no dispositivo; manter duplicatas exatas separadas de semelhanças.
- Detectar screenshots, imagens desfocadas e sequências de fotos como recomendações revisáveis, nunca remoção automática.
- Otimizar a análise de grandes bibliotecas para consumo de CPU, bateria, memória e cancelamento.
- Criar compressão de mídia com prévia, configuração de qualidade e preservação do original até confirmação.

## P3 — Monetização e expansão

- Definir Free/Pro com valor recorrente real, sem bloquear operações essenciais de segurança.
- Integrar Play Billing somente depois de recuperar e testar o modelo de produtos/entitlements existente.
- Localizar strings, suporte, privacidade, ficha Play e preços por mercado.
- Medir ativação, sucesso da primeira análise, recuperação comprovada, retenção, cancelamentos, ANR/crash e avaliações.

## Critérios de qualidade

- Nenhuma exclusão sem ação explícita e confirmação do sistema quando exigida.
- Pelo menos uma cópia preservada em cada grupo de duplicatas exatas.
- Resultados parciais claramente identificados.
- Nenhuma promessa de limpar cache privado de outros apps, aumentar RAM, acelerar CPU ou resfriar o aparelho.
- Testes e métricas reais antes de aumentar aquisição ou monetização.
