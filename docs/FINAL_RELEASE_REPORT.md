# Relatório de engenharia — recuperação do Midiastore

Atualizado: 04/11/2026

## Decisão atual

**EM DESENVOLVIMENTO — NÃO É RELEASE CANDIDATE.** O feedback do testador confirmou problemas de fluxo, UX, limpeza e Billing. Foi aberta a PR #3 em rascunho para corrigir uma branch isolada. Nenhum merge ou AAB de produção foi realizado.

- PR: https://github.com/srodolfobarbosa-afk/midiastory-/pull/3
- Issue P0: https://github.com/srodolfobarbosa-afk/midiastory-/issues/2
- Branch: fix/p0-sweep-permissions-missions-billing
- Package confirmado pelo proprietário na ficha interna: com.rdf112.midiastore.
- Namespace interno: com.rdf112.sweep (legado técnico).
- Build config atual: versionCode 17 / versionName 2.2.1 / minSdk 29 / targetSdk 36 / compileSdk 36. O versionCode máximo já publicado ainda precisa ser consultado na Play Console.

## O que foi modificado na branch de recuperação

- Corrigida a configuração de JDK local fixa que impedia o Gradle de usar JAVA_HOME no Codespaces/CI.
- Marca, strings, tema, paleta e ícone foram alinhados à identidade Midiastore.
- Ação de revisão de permissões parciais Android 14+ incluída também na aba Arquivos.
- Missão selecionada na primeira utilização é retomada depois que a primeira leitura de mídia termina.
- Missões podem ser canceladas na própria tela; botões ficam bloqueados durante execução.
- Implementada varredura global paginada por tipos de mídia e volumes acessíveis, com limites de segurança, progresso e cancelamento.
- Hash SHA-256 para candidatos entre páginas é feito em streaming/IO.
- Duplicatas só são confirmadas por hash não vazio e idêntico; cobertura parcial bloqueia pré-seleção de duplicatas.
- Mover para a Lixeira não é contabilizado como espaço liberado. Resposta nula do provider não é prova de exclusão.
- UI de Billing mostra quando o fluxo não inicia e diferencia reconciliação de plano gratuito.
- Scripts Windows de Release foram desativados porque alternavam package IDs, geravam dois AABs e restauravam package antigo.
- Novo Release exige package/version confirmados, aprovação explícita e fingerprint correto; workflow não publica diretamente na Play.
- Documentação de continuidade, matriz de funcionalidades, Data Safety, Billing e QA foi corrigida; adicionado AGENTS.md.

## Validação que existe e que falta

Há uma baseline anterior de 03/11/2026 com 58 testes de domínio, 7 testes JVM do app, Lint e build Debug bem-sucedidos. Essa evidência pertence à versão anterior, não à branch de recuperação.

Na recuperação houve:
- falha inicial de CI por caminho fixo de JDK, já removido;
- falha instrumentada MediaStore em emulador Android 15, ainda sem diagnóstico conclusivo;
- teste JVM com expectativa de marca antiga, corrigido;
- múltiplos workflows intermediários cancelados à medida que os commits eram atualizados.

A aprovação deve ser verificada somente no workflow do HEAD atual da PR #3. Um run antigo, cancelado ou ainda pendente não é prova de aprovação.

Nenhum teste de compra real, restore/reembolso/expiração, limpeza em aparelho Samsung/Motorola/Xiaomi, matriz API29–36 ou teste físico de desempenho foi concluído nesta recuperação.

## Bloqueadores de Release

1. CI verde no HEAD exato da PR #3, incluindo teste instrumentado Android 35.
2. Diagnóstico e correção do teste MediaStore instrumentado.
3. Provar varredura global e fail-closed em datasets grandes.
4. Validar o fluxo recém-implementado de exclusão permanente API29 via RecoverableSecurityException em emulador e aparelho físico; Lixeira/restauração permanecem API30+.
5. Confirmar SKUs, tipos, ofertas, países e preços no Play Console.
6. Implementar validação de purchaseToken por backend confiável / Google Play Developer API.
7. Testar billing completo com license testers.
8. Confirmar maior versionCode publicado, chave/certificado de upload e assinatura.
9. Testar em aparelhos físicos e revisar acessibilidade, privacidade e Data Safety.

## Segurança da cadeia de Release

O script Windows scripts/preparar_release.ps1 agora só bloqueia Release e não toca no package. A rotina Windows principal executa apenas testes/Debug. A geração assinada está reservada ao environment protegido play-release.

Secrets requeridos pelo workflow:
- SWEEP_KEYSTORE_BASE64
- SWEEP_STORE_PASSWORD
- SWEEP_KEY_ALIAS
- SWEEP_KEY_PASSWORD
- MIDIASTORE_RELEASE_APPROVED = YES
- MIDIASTORE_MAX_PUBLISHED_VERSION_CODE (maior versionCode já publicado; o source precisa ser estritamente maior)
- MIDIASTORE_EXPECTED_CERT_SHA256

Não criar chave temporária nem inserir credenciais privadas no ZIP, APK, repositório ou chat.

## Conclusão

O repositório está em recuperação controlada. O próximo marco é CI verde, diagnóstico instrumentado e validação física. Até lá, não solicitar nova rodada de teste do usuário nem enviar atualização à Google Play.

## Atualização de continuidade — 04/11/2026

O posicionamento confirmado é Phone Cleaner com modelo Freemium. O cartão principal de armazenamento foi corrigido para medir o volume compartilhado primário. Quatro testes de domínio foram adicionados, mas ainda dependem de execução no CI. A pesquisa de mercado e tecnologia reforça que o diferenciador deve ser confiança, clareza de cobertura e prova de recuperação real, não a quantidade de ferramentas.

A PR #3 permanece Draft, sem merge. O workflow do HEAD documentado anteriormente estava queued; não declarar build aprovado. Nenhum AAB assinado ou publicação foi produzido.
