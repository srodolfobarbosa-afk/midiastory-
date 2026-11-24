# Planta de engenharia para elevar a qualidade do Midiastore

**Estado:** plano pesquisado; nenhuma funcionalidade, versão, faixa de publicação ou configuração foi alterada por este documento.  
**Base examinada:** `com.rdf112.midiastore`, branch `codex/midiastore-release-audit`, HEAD `440345f`, código local `2.7.3` / versionCode `28`.  
**Estado Play verificado nesta data:** `production` sem release; `internal` e `alpha` mostram `2.5.0` / versionCode `22`. A 2.7.3 local ainda não está publicada em produção.

## Objetivo e significado de “padrão Apple”

Usar os princípios de acabamento associados à Apple — clareza, consistência, resposta imediata, acessibilidade, respeito ao contexto e recuperação de erros — dentro de um app Android nativo. Isso não significa copiar a interface do iOS nem trocar a arquitetura por moda. O padrão é medido pelo que a pessoa consegue fazer, entender e desfazer com confiança.

## Diagnóstico de partida

### Evidência adicional do app instalado pela Play no Moto G84

- Em 2026-11-17, o pacote `com.rdf112.midiastore` instalado pelo Play Store estava em 2.5.0 / versionCode 22 (track de teste; a consulta da Play API continua indicando produção vazia). Não foi substituído pelo APK de diagnóstico.
- A permissão de áudio estava disponível, mas imagens/vídeos não. A tela informou que a análise era parcial e encontrou 4 itens de áudio (3,70 MB); nenhum nome ou conteúdo de mídia foi registrado.
- Com 0 itens selecionados, o CTA da tela inicial “Mover para lixeira” abriu o consentimento oficial para mover os 4 candidatos. O consentimento foi negado. Não houve exclusão nem movimentação de arquivos.
- A mesma falha foi localizada no código 2.7.3: os CTAs de mover/excluir usavam todos os candidatos interativos como fallback quando não havia seleção. Correção local em `MainActivity.kt`: os CTAs usam somente seleção explícita e a linha destrutiva permanece oculta sem seleção. Regressão `fallbackCandidatesDoNotExposeHomeDestructiveButtonsWithoutExplicitSelection` passou no Moto G84 / Android 15 (1 teste, 0 falhas). A correção ainda não foi publicada nem instalada no pacote Play.
- A auditoria de navegação alcançou Início, Arquivos, Estúdio e Planos Pro; o botão Aa+ foi alternado e retornado ao estado original. Pagamentos e consentimentos destrutivos não foram aprovados. A verificação de cada controle secundário do Estúdio segue como trabalho aberto.

- O app é Android Views, com `MainActivity.kt` grande, um `domain` separado, `MediaScanViewModel`, repositórios MediaStore, Billing e testes JVM/instrumentados. Evoluir por extração gradual de responsabilidades; não iniciar uma reescrita integral em Compose.
- Há paginação de 100 itens, revisão antes da limpeza, SHA-256 para duplicatas e limites de segurança no scanner global. Precisamos provar os estados de ponta a ponta, principalmente cobertura parcial, cancelamento, consentimento e resultado da operação.
- A validação recente no Moto G84 / Android 15 terminou sem falhas reportadas, mas 4 de 9 cenários foram ignorados. Consentimentos API 29/API 30+ e acesso visual parcial permanecem sem fechamento nesta execução. Uma captura horizontal mostrou texto de boas-vindas cortado pelo CTA fixo.
- O projeto tem tradução incompleta em seis idiomas além de PT-BR/EN, acessibilidade sem auditoria concluída e Billing sem validação confiável de `purchaseToken` no servidor.
- A API da Play consultada não mostrou release em produção. Tratar a 2.7.3 como código em desenvolvimento, não como versão já distribuída.

## Princípios de produto e engenharia

1. Uma tela deve responder ao toque, explicar o estado atual e oferecer o próximo passo.
2. A pessoa escolhe e revisa arquivos; o app nunca apaga automaticamente.
3. Cobertura incompleta, permissão parcial, erro de provider e limite de segurança precisam aparecer como incompletude, não como “limpeza concluída”.
4. Não pedir permissão antes de explicar a tarefa que precisa dela; pedir acesso incremental e pelo menor escopo compatível com a função principal e as políticas Play.
5. Medir primeiro, otimizar depois. Nenhum número de velocidade ou espaço liberado sem medição repetível.
6. Manter o núcleo útil offline e no aparelho. Não enviar nomes, miniaturas, conteúdo ou hashes de mídia a modelos/telemetria por padrão.
7. Toda melhoria tem critério de aceite, teste automatizado adequado e evidência no SHA exato.

## Plano por fases

### Fase 0 — estabelecer a verdade da release e do build

**Trabalho:** conferir status do CI na branch/SHA candidato; relacionar código, APK/AAB, package, certificado e maior versionCode da Play; resolver inconsistências entre documentação antiga e API atual. Separar claramente production, closed/alpha e internal testing. Não alterar `applicationId`, assinatura ou versões sem aprovação do proprietário.

**Aceite:** release map com SHA, variante, package, versionCode, faixa, estado e evidência; CI verde no SHA exato; nenhuma alegação de público/produção baseada somente em source ou build local.

### Fase 1 — reparar a jornada principal de limpeza

**Trabalho:** reproduzir a tela inicial em retrato, paisagem, fonte ampliada e janela pequena; corrigir rolagem/insets e colisão com CTA. Fazer inventário de botões e estados vazios/erro/loading para encontrar controles sem ação ou ações sem retorno. Revisar uma jornada única e previsível: entender permissão → analisar → mostrar cobertura e progresso → revisar propostas → confirmar ação no diálogo oficial Android → verificar o resultado.

**Aceite:** roteiro executado sem assistência no Moto G84; cada botão crítico tem resposta visual imediata; zero texto/controle encoberto nas configurações testadas; cancelamento volta a estado coerente; nenhum item fica pré-selecionado para exclusão sob cobertura parcial/incompleta.

### Fase 2 — fechar segurança e confiabilidade MediaStore

**Trabalho:** executar consentimento em API 29 (aceitar e cancelar via `RecoverableSecurityException`) e API 30+ (aceitar/cancelar via fluxo oficial); verificar ausência/presença por URI depois da resposta. Cobrir permissões total, parcial, negada, revogada e áudio independente; provider indisponível, paginação longa, troca de volume, cancelamento e limites de leitura/hash. Usar somente fixtures sintéticas criadas pelos testes.

**Aceite:** API 29, 30/35 e API mais recente cobertas por casos sem skip ocultando o comportamento; teste de integração confirma proteção do original e resultado real; bytes são creditados como liberados somente após exclusão permanente confirmada. A matriz atual inclui Android 33, 34 e 36, além das demais versões suportadas, antes de release.

### Fase 3 — acabamento, acessibilidade e localização

**Trabalho:** alinhar hierarquia visual, espaçamento, tipografia, cores semânticas, estados de foco/pressed/disabled e feedback acessível. Auditar TalkBack, rótulos/ordem de foco, contraste, alvos táteis, orientação, escala de fonte, teclado e layout RTL. Completar traduções com revisão humana; manter PT-BR e EN sincronizados e verificar placeholders/strings longas.

**Aceite:** fluxo completo pode ser operado com TalkBack; nenhum corte em escala de fonte ampliada; contraste e hit targets passam revisão; 8 idiomas disponíveis sem texto inesperado em inglês/fallback nas telas principais.

### Fase 4 — confiança, privacidade e compra

**Trabalho:** confrontar manifesto, dependências/SDKs, tráfego de rede, ACTION_SEND, política de privacidade e Data Safety com o comportamento real. Confirmar declaração e elegibilidade para permissões de fotos/vídeos na Play. No Billing, conferir IDs/tipos/ofertas/países e contas de teste; validar `purchaseToken` em backend confiável, idempotência, reconhecimento e estados pending/cancelado/restaurado/reembolsado/expirado. Manter recursos pagos bloqueados quando a verificação falhar.

**Aceite:** auditoria reproduzível de dados/permissões; compra e entitlement testados por cenário com comprovante sem dados de pagamento; token inválido nunca libera Pro; nenhum claim de privacidade ou monetização sem evidência.

### Fase 5 — performance mensurável e arquitetura sustentável

**Trabalho:** estabelecer medições de cold/warm startup, navegação, memória, jank, bateria, duração de varredura e consumo de I/O em aparelhos de referência. Ativar StrictMode apenas em builds/debug de diagnóstico. Depois do baseline, avaliar Baseline Profiles para abertura, navegação e fluxo de análise; manter se benchmarks demonstrarem ganho. Extrair gradualmente da `MainActivity` componentes de tela/coordenadores com contratos testáveis, sem mover regras Android para `domain`. Considerar WorkManager apenas para trabalho realmente retomável e explicitamente iniciado pelo usuário; nunca agendar limpeza automática.

**Aceite:** relatório antes/depois em aparelhos e builds comparáveis; nenhuma regressão de latência, RAM, ANR ou tamanho sem justificativa; cancelamento e lifecycle corretos; arquitetura extraída sem alterar comportamento público.

### Fase 6 — uma evolução funcional depois da confiança

Selecionar uma só melhoria por ciclo a partir do que usuários/testes mostrarem. Ordem sugerida:

1. Resumo da análise que explique cobertura, permissões, limites atingidos e fontes que falharam.
2. Histórico local, privado e verificável de operações, com resultado e tamanho real confirmado; sem conteúdo ou nomes de arquivos além do estritamente necessário.
3. Missões de armazenamento com revisão segura, se métricas e entrevistas demonstrarem que ajudam.

Análise visual de qualidade/blur, compressão, cache privado de outros apps, previsão de espaço, organização SAF ou ferramenta de IA no aparelho ficam fora até existir caso de uso, desenho de privacidade, protótipo, validação técnica e aceite comercial.

## Arquitetura alvo, sem reescrita

```text
Views / Activity
    ↓ eventos e estado de tela
ViewModel / coordenadores de fluxo
    ↓ contratos de domínio
domain (políticas puras, seleção, cobertura, estados)
    ↓ interfaces
Android data (MediaStore, Billing, Storage, consentimento)
```

- `domain` continua independente de Android e testável em JVM.
- UI não faz hashing/I/O longo. Scanner deve ser cancelável, paginado, limitado e publicar progresso/cobertura sem coleções ilimitadas.
- Adaptadores Android são responsáveis por permissões e diálogos oficiais; a política de domínio define apenas o estado permitido.
- Tela de resultados conserva contexto entre rotação/interrupção e permite retomar ou cancelar sem duplicar operações.
- Não acrescentar login, cloud sync ou telemetria de conteúdo para resolver problemas que podem ser resolvidos localmente.

## Observabilidade e recursos disponíveis

- **GitHub Actions:** gate obrigatório com relatórios vinculados ao SHA; separar falha de código, falha do runner e cenário não executado.
- **Moto G84 + ADB:** aceite visual e fluxo real do dispositivo. Emuladores API 29/30/33/35/36 completam versões não presentes no aparelho.
- **Play Console:** tracks e Android vitals após existir distribuição; não inferir métricas de aquisição ou crash sem dados reais.
- **Supabase:** continuidade da equipe e memórias operacionais; não guardar mídia, hashes de conteúdo ou dados pessoais de usuários para “analisar o app”.
- **Firebase:** usar apenas capacidades já configuradas que tenham caso de uso medido e política revisada; evitar adicionar SDK/telemetria como reflexo automático.
- **Antigravity + Codex + roteador:** colaboração para pesquisa/revisão/implementação, não como dependência do produto Android nem como substituto de testes reais.

## Estratégia de LLM e cotas

O arquivo local de configuração possui campos de chave preenchidos e `.secrets/` está ignorado pelo Git; os valores não foram exibidos, registrados em log nem enviados a um provedor nesta análise. O roteador tem uma cascata atual, mas ela merece revisão antes de confiar nela: tenta modelos Google AI Studio na ordem do persona e pode tentar vários modelos pagos do OpenRouter quando o endpoint de crédito entende que há saldo acima de `$0.005`; modelos gratuitos vêm depois. Os IDs e disponibilidade precisam ser reconciliados com o catálogo vigente antes de cada ciclo.

- **Carbon:** OpenRouter lista `google/gemini-4-carbon` como modelo pago; a página consultada indica `$2/M` tokens de entrada e `$10/M` de saída. Não há pulo do gato que transforme cobrança/billing obrigatório em acesso grátis. A chamada direta ao Google que retornou 429 não é contornada ajustando tokens se a causa for faturamento/quota da conta.
- **Trabalho sem custo de inferência:** usar modelo gratuito `:free`/router gratuito vigente, Codex local já disponível, e ferramentas determinísticas. OpenRouter informa 50 solicitações/dia no plano free e limites por provedor; disponibilidade e qualidade variam.
- **Regra de segurança do roteador:** modo `budget=0` deve proibir chamadas pagas, não apenas esperar que saldo esteja abaixo de um limiar; orçamento positivo exige limite explícito por sessão/modelo. 402 deve fazer fallback direto para um ID free confirmado, sem repetir todos os modelos pagos. Registrar modelo, tokens e custo estimado, sem salvar prompts com segredo, mídia ou dado pessoal.
- **Para o app:** não incluir chaves OpenRouter/Google no APK. Não enviar biblioteca de fotos nem hashes a LLM sem escolha explícita, objetivo claro e política aprovada; a evolução do produto deve continuar local-first.

**Resposta prática à pergunta “usar a melhor LLM”:** sim, o roteador pode tentar Carbon por OpenRouter quando houver saldo e o ID estiver ativo; neste momento o caminho racional para continuar sem aumentar gasto é uma rota gratuita verificada + Codex local. Esta sessão não fez chamada de geração e não consumiu crédito. A disponibilidade de Carbon não elimina o custo informado pelo provedor.

## Processo de cada atualização

1. Ler memória, handoff, blockers, matriz e changelog do SHA distribuído.
2. Reproduzir uma falha ou necessidade com evidência e impacto para a pessoa usuária.
3. Criar proposta curta, estimar risco e definir critério observável antes da mudança.
4. Implementar em branch isolada, com mudanças pequenas e reversíveis.
5. Rodar testes que cubram a alteração e aparelhos/APIs afetados; guardar logs sem mídia/segredos.
6. Revisar privacidade, acessibilidade, localização, Billing e políticas afetadas.
7. Comparar o candidato ao SHA anterior, corrigir regressões e registrar limitações/skips.
8. Só então preparar a release para revisão; publicação continua uma decisão explícita do proprietário.

## Ordem recomendada para o próximo ciclo

1. Fechar a discrepância de distribuição: confirmar o estado de production vazio vs. source 2.7.3/vc28 e preparar o caminho de teste do candidato sem publicar.
2. Corrigir/revalidar o corte do onboarding e mapear controles sem resposta no Moto G84.
3. Destravar casos de API29 e consentimento API30+ com fixtures sintéticas e resultados verificáveis.
4. Confirmar CI no SHA exato e corrigir seus findings, sem declarar verde por run de outro commit.
5. Fazer o primeiro passe de TalkBack/escala de fonte e atualizar PT-BR/EN + traduções prioritárias.
6. Só depois decidir se a próxima feature é relatório de cobertura ou histórico local.

## Referências pesquisadas

- [Android Core App Quality Guidelines](https://developer.android.com/docs/quality-guidelines/core-app-quality): estabilidade, compatibilidade, interação e form factors.
- [Android vitals](https://developer.android.com/topic/performance/vitals/): crashes, ANRs, startup, rendering, memória e bateria como sinais de qualidade.
- [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview): técnica de otimização depois de medir fluxos importantes.
- [Apple Human Interface Guidelines — Design Principles](https://developer.apple.com/design/human-interface-guidelines/design-principles/) e [Feedback](https://developer.apple.com/design/human-interface-guidelines/feedback): consistência, controle, recuperação de erros e feedback proporcional.
- [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311) e [Photo/Video permissions](https://support.google.com/googleplay/android-developer/answer/18265346): minimização, transparência e justificativa de acesso sensível.
- [OpenRouter free models e cotas](https://openrouter.ai/collections/free-models) e [Gemini 4 Carbon](https://openrouter.ai/google/gemini-4-carbon/providers): disponibilidade/preço observados em 17/11/2026; reconfirmar antes de uso porque catálogo e tarifas mudam.
