# Midiastore OaaS — Estratégia de produto e hipóteses 10x

> **Natureza deste documento:** estratégia e metas de produto, não prova de implementação ou validação comercial. A branch de recuperação adiciona uma varredura global paginada, mas ela ainda precisa passar em CI e testes de aparelhos. A matriz `docs/FEATURE_IMPLEMENTATION_MATRIX.md` é a fonte de verdade sobre o que existe e o que foi testado.

## 1. A Virada de Chave: De Ferramenta (SaaS) para Resultado Entregue (OaaS)

Enquanto aplicativos tradicionais vendem **ferramentas passivas** (onde o usuário tem o trabalho de procurar, comparar, marcar arquivo por arquivo e limpar), o **Midiastore** opera sob o princípio **Outcome-as-a-Service (OaaS)**:

> *"Me diz o resultado que você quer. Eu faço o trabalho por você."*

## 2. As 6 Etapas de Execução Automatizada no Código (`OutcomeServiceEngine.kt`)

Inspirado na pergunta central de avaliação de processos (*"As pessoas são pagas para pensar — ou para executar processos? Buscar · conferir · preencher · reconciliar · ligar · acompanhar"*):

1. **BUSCAR**: Varre páginas determinísticas do `MediaStore` sem estourar memória.
2. **CONFERIR**: Valida tamanho, MIME, estado pendente/lixeira e confirma duplicatas apenas com SHA-256 não vazio e idêntico; preserva uma referência original por grupo confirmado.
3. **PREENCHER**: Pré-seleciona automaticamente até 500 URIs elegíveis ordenadas pelo maior impacto em bytes (`OutcomeExecutionPlan`).
4. **RECONCILIAR**: Revalida cada URI imediatamente antes da ação (`AndroidTrashOperationCoordinator`) e verifica o estado real no `MediaStore` após a operação (`TrashRecoveryLedger`).
5. **LIGAR**: Encaminha o lote revisado ao fluxo oficial de consentimento do Android; o usuário precisa conferir e confirmar.
6. **ACOMPANHAR**: Registra as métricas de execução e os pedidos de novos resultados no **Data Flywheel** (`AndroidOutcomeTelemetryStore`).

## 3. As 5 Dimensões 10x do Midiastore

- **10x Velocidade**: Em vez de marcar dezenas de checkboxes manualmente, 1 toque na Missão OaaS filtra, protege originais e preenche todo o lote de revisão.
- **10x Custo**: Processamento 100% *on-device* (custo marginal de servidor próximo de zero), permitindo planos acessíveis pelos meios de pagamento que a Google Play disponibiliza para cada país e produto (`sweep_pro_monthly`, `sweep_pro_annual`, `sweep_pro_lifetime`).
- **10x Capacidade**: Arquitetura paginada que analisa bibliotecas de qualquer tamanho mais múltiplos volumes externos/SD Card.
- **10x Qualidade e Segurança**: Proteção de uma referência original por grupo de hash confirmado. A Lixeira oficial é usada a partir do Android 11; limpeza API29 continua pendente.
- **10x Disponibilidade**: A leitura e a análise de mídia ocorrem no dispositivo e podem ser canceladas. Billing exige conexão com Google Play; a limpeza API29 ainda não está disponível.
