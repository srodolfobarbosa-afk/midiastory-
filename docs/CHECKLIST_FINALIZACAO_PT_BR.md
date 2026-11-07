# Checklist de finalização do Midiastore

Objetivo do produto: uma pessoa com o telefone cheio abre o Midiastore, entende a ação gratuita em poucos segundos e consegue revisar e liberar espaço com segurança. O Pro é opcional e só deve ser oferecido depois que a limpeza gratuita entregar valor.

## 1. Limpeza gratuita, rápida e confiável

- [x] A Home tem uma ação principal gratuita para encontrar arquivos grandes.
- [x] No Motorola moto g84 5G/API35, abrir explicitamente `com.rdf112.midiastore.debug` e tocar em “Liberar espaço” encontrou 5 candidatos (84,30 MB); a ação “Revisar 5 arquivos” abriu a lista de revisão. Nenhum arquivo foi apagado. Uma tentativa anterior terminou em erro, mas a repetição com o app Debug atual concluiu a análise; acompanhar logs e repetir após mudanças de permissões continua necessário.
- [x] A primeira ação mostra o resultado encontrado e abre a revisão. O usuário continua confirmando cada item e o Android pede consentimento antes da exclusão.
- [ ] Revisar cada candidato antes da operação; nunca apagar arquivos pessoais sem a pessoa confirmar no fluxo oficial do Android.
- [ ] Contabilizar espaço liberado somente após exclusão permanente e confirmação de que cada URI deixou de existir.
- [ ] Tratar ausência de arquivos, permissão parcial/negada, cancelamento, falha do provedor e limite de segurança com mensagens claras e ações seguintes úteis.
- [ ] Validar o fluxo de limpeza em API 29, 30, 33, 34, 35 e 36 e em aparelhos representativos. API 29 precisa comprovar aceitar, cancelar e estado após exclusão.

## 2. Botões e jornada

- [ ] Percorrer no aparelho todos os botões de todas as telas. No teste atual, a ação Home abriu a lista de revisão e a navegação para Planos Pro funcionou.
- [x] Os planos estão marcados como ainda não ativos e não iniciam cobrança. Botões podem ser tocados e explicam a indisponibilidade; compras reais seguem bloqueadas até os produtos serem configurados/ativados na Play Console e a verificação de entitlement estar pronta.
- [ ] Validar navegação, filtros, permissões, paginação, seleção/revisão, cancelar análise, excluir, restaurar, exportar, compartilhar, restaurar compra e gerenciar assinatura.
- [ ] Quando uma ação depender de seleção, permissão, API do Android ou Pro, mostrar claramente o que falta para continuar.
- [ ] Fazer a ação gratuita funcionar antes de promover os planos; manter segurança e revisão gratuitas.

## 3. Marca e imagem da Home

- [x] No aparelho, o logo da barra superior mostra o símbolo junto do nome Midiastore; a ilustração anterior da mulher com telefone foi preservada e o nome também aparece logo abaixo dela. O ícone do Android mantém o símbolo.
- [ ] Substituir a ilustração vetorial da pessoa com o celular por uma imagem realista, de bom gosto, de uma mulher negra jovem adulta, sorrindo e com cabelo loiro.
- [ ] Se houver vídeo curto, produzir uma vinheta de até 10 segundos mostrando a pessoa usando o Midiastore para analisar o telefone; respeitar tamanho do APK, consumo de dados/bateria e movimento reduzido.
- [ ] Revisar enquadramento, contraste, acessibilidade, direitos/termos do asset e aparência em telas pequenas antes de publicar.

## 4. Idiomas e apresentação

- [x] Inglês, português brasileiro e fallback têm 206 chaves sincronizadas (checagem XML local: sem chaves faltantes ou extras entre EN/PT/base).
- [ ] Completar e revisar espanhol, francês, alemão, hindi, japonês e chinês simplificado: cada idioma tem 95/206 chaves e faltam 111 por idioma, com telas secundárias em fallback para inglês.
- [ ] Conferir chaves sincronizadas PT-BR/EN, placeholders, plural, nomes de planos, diálogos de permissão e ausência de texto cortado em telefone pequeno e fonte ampliada.
- [ ] Rever preços sugeridos na interface para deixar inequívoco que ainda não são preços ativos da Google Play.

## 5. Planos e pagamentos

Valores sugeridos para o Brasil no source: R$ 14,90/mês, R$ 79,90/ano e R$ 149,90 pagamento único. Eles ainda não estão configurados nem confirmados na Play Console.

- [ ] Na Play Console, confirmar os IDs e tipos: `sweep_pro_monthly` e `sweep_pro_annual` como assinaturas; `sweep_pro_lifetime` como compra única.
- [ ] Configurar produtos, planos-base/ofertas, países, preços e contas de teste. A interface deve exibir o preço localizado retornado pela Google Play, não tratar sugestão local como preço ativo.
- [ ] Implementar e validar backend confiável para verificar `purchaseToken` antes de conceder Pro.
- [ ] Testar compra aprovada, pendente, cancelada, restaurada, repetida, reembolsada e expirada, incluindo acknowledgement e falha de rede.
- [ ] Preservar entitlement já comprado e manter a limpeza básica disponível sem pagamento.

## 6. Provas de qualidade e publicação

- [x] Verificação local anterior em 2026-11-07: Domain tests, app JVM tests, Lint task, `assembleDebug` e `assembleDebugAndroidTest` terminaram `BUILD SUCCESSFUL`; relatórios JUnit locais somam 78 testes, 0 falhas, 0 erros e 0 skips. A compilação Debug após a correção visual/log atual também passou; repetir testes JVM/Lint está em andamento.
- [ ] Limpar os 222 findings do Lint local (111 MissingTranslation, 48 PluralsCandidate, 30 UseKtx, 24 UnusedResources, 5 StringFormatCount, 2 GradleDependency, 1 OldTargetApi e 1 ObsoleteSdkInt); `abortOnError=false` permite que a task termine com sucesso apesar dos findings.
- [ ] CI verde no SHA exato da PR para testes de domínio/JVM, lint, build Debug e instrumentação suportada API 29/35.
- [ ] Rodar a suíte instrumentada no Motorola: a tentativa atual parou antes dos testes porque a assinatura do app auxiliar instalado diverge do APK local. Não desinstalar apps do telefone nem apagar dados para contornar isso sem uma decisão explícita.
- [ ] Revisar permissões de mídia e elegibilidade da declaração na Play Console, manifesto final, política de privacidade, Data Safety e SDKs.
- [ ] Confirmar na Play Console o maior `versionCode` publicado, package e certificado de upload aceito. O source `versionCode=17` não comprova que seja maior que o publicado.
- [ ] Fechar os bloqueios de API 29, UTP/CI e Billing antes do release.
- [ ] Só então gerar e inspecionar AAB com a chave de upload legítima, instalar em teste interno/fechado e validar o pacote/versão/certificado antes de qualquer publicação pública.

**Estado atual: não pronto para AAB de produção nem publicação pública.** No Motorola/API35 a versão Debug atual analisou e abriu 5 itens para revisão, sem apagá-los. Ainda faltam validação instrumentada atual/API29 e CI do SHA candidato, revisão de todos os botões, traduções completas, resolução dos findings do Lint, catálogo/preços de compras ativos na Play Console, backend de purchaseToken, confirmação do versionCode/certificado legítimo e revisão final de política/Data Safety. Não foi gerado AAB.
