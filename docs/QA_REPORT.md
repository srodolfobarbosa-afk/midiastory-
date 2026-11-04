# Relatório de QA histórico — não usar como aprovação da branch atual

Este arquivo substitui o relatório antigo Sweep v2.1.0 / VC13. O relatório anterior descrevia uma baseline diferente e não comprova que a branch atual do Midiastore passou nos testes.

## Identidade do source atual

- Nome/listing: Midiastore.
- Application ID: com.rdf112.midiastore (confirmado pelo proprietário na ficha de teste interno).
- Namespace interno: com.rdf112.sweep.
- Build atual no source: versionCode 17 / versionName 2.2.1.
- minSdk 29 / targetSdk 36 / compileSdk 36.
- Gradle Wrapper 8.13 / JDK 17 / Billing KTX 8.0.0.

O versionCode 17 é a configuração do repositório; não confirma que seja maior do que o último enviado à Play Console. O source histórico do AAB publicado e o certificado de upload ainda precisam ser conferidos pelo proprietário.

## Achados históricos que motivaram a recuperação

1. A UI exibia identidade Sweep e textos técnicos demais.
2. Missões eram executadas somente com a página aberta e não cobriam toda a biblioteca.
3. A permissão parcial Android 14+ aparecia como aviso sem controle evidente de ampliação.
4. O fluxo de compra não comunicava quando Billing não conseguia iniciar.
5. A política de duplicatas aceitava grupo sem SHA-256 quando todos os hashes estavam ausentes.
6. O contador de espaço podia confundir movimento para a Lixeira com bytes liberados.
7. Scripts Windows alternavam entre dois application IDs e restauravam package antigo como padrão.
8. O Gradle estava preso a caminho de JDK específico de uma máquina.

## Correções presentes na branch de recuperação

Ver PR #3 e docs/FEATURE_IMPLEMENTATION_MATRIX.md para detalhes. Incluem varredura global cancelável por tipo/volume/página, hash SHA-256 fail-closed, feedback de permissão/Billing, distinção de Lixeira/exclusão, marca e paleta Midiastore e scripts de Release bloqueados até verificação de identidade/certificado.

## Validação — status separado

- Baseline anterior de 03/11: 58 testes de domínio e 7 testes JVM do app passaram, Lint sem erros (76 warnings), APK Debug e APK instrumentado compilados. Isso não valida os commits da recuperação.
- CI inicial da PR falhou por caminho org.gradle.java.home; removido na branch.
- Um workflow instrumentado MediaStore em emulador Android 15 falhou; a causa precisa ser confirmada por logs/XML de teste.
- Houve teste JVM intermediário com expectativa da antiga marca; atualizado para Midiastore.
- Workflows de SHA intermediários foram cancelados à medida que a branch avançava. Verificar apenas o workflow associado ao HEAD atual da PR #3.

## Bloqueadores atuais

- CI verde no HEAD exato, inclusive instrumentação MediaStore Android 35.
- Teste de MediaStore sem falhas em emulador e aparelhos físicos.
- API29 cleanup ou minSdk reavaliada.
- Produtos Billing ativados e testados na Play Console.
- PurchaseToken validado em backend seguro.
- Maior versionCode e certificado de upload confirmados.
- Teste físico em fabricantes representativos, acessibilidade, desempenho, privacidade/Data Safety.

**Nenhuma aprovação de Release ou publicação está registrada neste relatório.**
