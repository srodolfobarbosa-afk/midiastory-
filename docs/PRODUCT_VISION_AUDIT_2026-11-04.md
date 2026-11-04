
# Auditoria da visão e execução do Midiastore — 04/11/2026

## Decisão executiva

**A primeira versão deve ser Phone Cleaner / limpador inteligente de celular, com apresentação Pro e modelo Freemium.** “Gerenciador de armazenamento” descreve uma capacidade técnica, não a razão principal para o consumidor instalar o aplicativo.

Promessa ao usuário: **descobrir o que ocupa espaço, identificar o que pode ser removido, proteger o que importa e recuperar espaço com segurança.**

Não transformar a primeira versão em explorador universal, cofre, backup, IA genérica ou painel de ferramentas. Esses serviços são extensões possíveis, não o produto inicial.

## Objetivo original versus realidade no repositório

| Área | Evidência atual | Estado real | Lacuna determinante |
|---|---|---|---|
| Identidade Midiastore | App label, applicationId com.rdf112.midiastore, marca Midiastore | Parcialmente aplicada | Confirmar na Play Console maior versionCode e certificado; namespace interno continua com.rdf112.sweep |
| Phone Cleaner | Missões globais de arquivos grandes, cópias exatas e mídias antigas; revisão antes de limpar | Núcleo implementado na branch, não validado no HEAD | UX final precisa de testes de uso e aparelho; scanner/instrumentação pendentes |
| Duplicatas exatas | SHA-256 em streaming, limites, proteção do original e cobertura parcial | Código presente; lógica de domínio testada em baseline anterior | Testes instrumentados do scanner global no HEAD atual |
| Similaridade visual | Nenhum pHash/dHash funcional no Midiastore | Não implementada | SD Maid já oferece hash perceptual; qualquer protótipo precisa provar precisão, custo e UX |
| Inteligência de armazenamento | StatFs do volume compartilhado primário; cálculo isolado em StorageUsageCalculator | Medidor de base corrigido no commit d32bb923 | Não soma automaticamente SD/USB; sem breakdown por categoria, histórico ou forecast |
| Limpeza segura | Consentimento do sistema, lixeira API30+, caminho API29 com RecoverableSecurityException, revalidação e verificação posterior | Implementação parcial | Consentimento/API29, lixeira e restauração ainda precisam de testes instrumentados e aparelhos reais |
| Gerenciador de arquivos | Navegação por mídia consultável via MediaStore | Parcial e limitada a mídia | Downloads/documentos/pastas via SAF não implementados |
| Automação inteligente | Telemetria local agregada; sem WorkManager | Não implementada como serviço periódico | Depois da estabilidade: alertas/relatórios opt-in, sem exclusão automática |
| IA no dispositivo | Nenhum modelo LiteRT/MediaPipe integrado | Não implementada | Não adicionar modelo sem benchmark que supere métodos clássicos |
| Billing/Pro | Billing fail-closed; exportação de relatório descrita como Pro no cliente | Bloqueado para vendas | Backend de purchaseToken, catálogo/offerings Play e lifecycle não verificados/testados |
| Internacionalização | PT-BR/EN/fallback 152 chaves; DE/ES/FR/HI/JA/ZH-CN 63 cada | Parcial | 89 chaves ausentes por idioma; 534 no total; revisão humana e QA visual necessários |
| Arquitetura extensível | Dois módulos Gradle, domínio sem Android e adaptadores Android | Base adequada, ainda concentrada | MainActivity extensa; refatorar depois do baseline, sem módulos vazios |
| API 37 | Target atual 36 | Atende requisito Play observado para 2026 | Testes adaptáveis em telas grandes e migração para target 37 até agosto de 2027 |

## Estado de preservação e CI

- Repositório: srodolfobarbosa-afk/midiastory-.
- Branch: fix/p0-sweep-permissions-missions-billing.
- HEAD auditado: d11c1b952c5edc7def8a12989e002d424dda638b.
- PR #3: aberta, Draft, base main, não merged.
- Workflow no HEAD: [41862544945](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/41862544945), observado como **queued**; não prova build nem testes.
- O run anterior no SHA 8081d049 chegou a concluir passos Gradle/PowerShell, mas a instrumentação API29/API35 foi cancelada ao ser substituída. Resultado antigo não pode ser transferido ao HEAD atual.
- O commit d32bb923 adicionou quatro testes para cálculo de capacidade, espaço disponível, entrada inválida e percentual; aguardam execução no CI deste HEAD.
- Identidade preservada no source: applicationId=com.rdf112.midiastore, versionCode=17, versionName=2.2.1. Não há comprovação Play Console do maior versionCode ou certificado.
- Não houve merge, publicação, AAB assinado ou alteração de secrets.

## Pesquisa de mercado: resultado que altera a direção

| Concorrente | Google Play: nota / avaliações / downloads observados em 04/11/2026 | O que já oferece |
|---|---|---|
| Files by Google | 4,5 / 9,03M / 5B+ | Limpeza recomendada, duplicatas, screenshots, busca, navegação e partilha |
| Avast Cleanup | 4,6 / 1,52M / 50M+ | Mídia, arquivos grandes, apps não usados, anúncios e compras |
| AVG Cleaner | 4,6 / 1,97M / 100M+ | Mídia, duplicatas, fotos de baixa qualidade, arquivos/apps, anúncios e compras |
| CCleaner | 4,5 / 3,08M / 100M+ | Limpeza, downloads, resíduos, análise e desinstalação de apps |
| SD Maid 2/SE | 4,6 / 9,67 mil / 1M+ | Analisador, resíduos, duplicatas exatas/perceptuais, compressão de mídia e scheduler |

As contagens da loja variam por região e mudam com o tempo; não provam usuários ativos nem receita. Avaliações individuais são anedóticas e não estimam prevalência.

A vantagem sustentável não será apenas “tem pHash” ou “tem um botão limpar”. SD Maid já oferece hash perceptual e compressão. A proposta recomendada é **clareza + evidência + segurança + resultado confirmado**:
- dizer exatamente quais tipos/volumes foram lidos e quais ficaram fora;
- explicar por que cada arquivo foi sugerido;
- separar cópia exata de mídia visualmente semelhante;
- preservar uma referência original e permitir revisão humana;
- distinguir potencial, preparado, movido para Lixeira e bytes realmente liberados;
- manter processamento local e UX simples;
- declarar limitações em vez de prometer limpeza universal.

## Pesquisa de tecnologia e licenças

| Projeto | Licença | Snapshot GitHub | Uso recomendado |
|---|---|---|---|
| SD Maid SE | GPL-3.0 | 7.980 stars; push 02/11/2026 | Estudar testes, modularização e UX técnica; não copiar código para app fechado sem análise jurídica |
| Clearo | MIT | 1 star; push 07/08/2026 | Referência de ideias dHash/Flutter-Kotlin; maturidade pública insuficiente para depender dele |
| Fossify Gallery | GPL-3.0 | 3.884 stars; push 03/11/2026 | Estudar galeria/localização; não copiar código para app fechado sem análise jurídica |

WorkManager 2.12.0 foi lançado em setembro de 2026, mas não foi adicionado. SAF é a abordagem para documentos/Downloads escolhidos. MediaStore continua como API primária para fotos, vídeos e áudio. Nenhuma dependência externa nova foi adicionada.

## Prioridade de execução e pontuação

Notas internas de 0 a 100; pesos: valor usuário 25, diferenciação 20, viabilidade 20, comercial 15, segurança 15, manutenção 5. Para segurança/manutenção, nota alta significa menor risco/custo.

| Oportunidade | Usuário | Diferenciação | Viabilidade | Comercial | Segurança | Manutenção | Total | Decisão |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| Ledger de evidências da limpeza | 24 | 18 | 18 | 13 | 15 | 4 | 92 | Prioridade do núcleo |
| Storage Forecast local | 21 | 15 | 18 | 9 | 14 | 5 | 82 | Depois de histórico suficiente |
| Similaridade visual revisável | 21 | 16 | 13 | 11 | 10 | 3 | 74 | Protótipo após baseline e corpus |
| SAF Documents/Downloads | 20 | 8 | 15 | 10 | 11 | 4 | 68 | Próxima etapa, escopo limitado |
| Classificação ML de blur/qualidade | 16 | 12 | 10 | 9 | 9 | 2 | 58 | Não priorizar ainda |
| Backup em nuvem próprio | 20 | 7 | 7 | 12 | 7 | 1 | 54 | Backlog; custo/risco altos |

## Plataforma e política

- Google Play exige target API 36 ou superior para novos apps e atualizações desde 31/08/2026. O source atual tem target 36.
- A preparação para Android 17/target API 37 precisa incluir redimensionamento e orientação em telas grandes; o prazo Play indicado é agosto de 2027.
- O manifesto desta branch declara permissões específicas de mídia e acesso parcial selecionado; não declara MANAGE_EXTERNAL_STORAGE.
- A solicitação ampla de fotos/vídeos só é defensável se a análise da biblioteca inteira for realmente função central e a declaração Play justificar por que Photo Picker não basta. Para Downloads/documentos, começar por SAF.
- Não prometer apagar cache privado de outros apps, acelerar CPU, resfriar aparelho ou liberar RAM.

## Veredito

**NÃO PRONTO PARA RELEASE.** A direção de produto está coerente com o problema que o usuário quer resolver, mas o núcleo ainda precisa de validação. A próxima ação é obter CI verde no HEAD exato e resolver instrumentação API29/API35 antes de adicionar features avançadas.

## Fontes principais

- [Files by Google — Play](https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files)
- [Avast Cleanup — Play](https://play.google.com/store/apps/details?id=com.avast.android.cleaner)
- [AVG Cleaner — Play](https://play.google.com/store/apps/details?id=com.avg.cleaner)
- [CCleaner — Play](https://play.google.com/store/apps/details?id=com.piriform.ccleaner)
- [SD Maid 2/SE — Play](https://play.google.com/store/apps/details?id=eu.darken.sdmse)
- [SD Maid Deduplicator](https://sdmse.darken.eu/deduplicator)
- [Google Play target API](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Android 17 adaptive UI](https://developer.android.com/blog/posts/prepare-your-app-for-the-resizability-and-orientation-changes-in-android-17)
- [Photo/video permissions policy](https://support.google.com/googleplay/android-developer/answer/16935362)
- [All files access policy](https://support.google.com/googleplay/android-developer/answer/10467955)
- [Android 14 partial photo access](https://developer.android.com/about/versions/14/changes/partial-photo-video-access)
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work)
