# MIDIastore — PONTO DE CONTINUIDADE

Data: 03/11/2026

## Identificação do projeto

Nome comercial: Midiastore
Projeto técnico legado: Sweep
ApplicationId: com.rdf112.sweep
VersionName: 2.1.1
VersionCode: 14
Target SDK: 36

## Estado do Git

Branch: feature/midiastore-2027-release-consolidation
Commit anterior: 1ffab4c

## Estado técnico confirmado

- Compilação Debug executada com sucesso na última validação.
- 58 testes de domínio aprovados.
- 7 testes JVM do aplicativo aprovados.
- Lint Debug executado.
- APK Debug gerado.
- APK AndroidTest compilado.
- Release VC14/2.1.1 compilado anteriormente para validação.
- AAB validado com bundletool.
- APK validado com apksigner.
- AAB verificado com jarsigner.

## ATENÇÃO: ASSINATURA

Os artefatos Release de validação utilizam certificado temporário.

Eles NÃO estão autorizados para publicação ou atualização na Google Play.

A última tentativa de bundleRelease foi bloqueada corretamente porque faltam:

- keystore.properties
- sweep-upload.jks legítimo

Investigar Play App Signing e obter a chave de upload legítima ou solicitar redefinição, se aplicável.

Não inventar nem substituir a chave de produção.

## Artefatos de validação

APK: Midiastore-2.1.1-VC14-VALIDATION-ONLY.apk
SHA-256: 8b6ba0de35871776144a672fa348e9f3540abfcfb1bf2e2c1e13edb07795666e

AAB: Midiastore-2.1.1-VC14-VALIDATION-ONLY.aab
SHA-256: aa09330939b0b4c8a6046920849e663ec4a55d371075228ff019e043256b7116

Os artefatos VALIDATION-ONLY são ignorados pelo Git.
Podem ser regenerados a partir do código-fonte.

## Trabalho já realizado

- Auditoria inicial.
- Correções de segurança de duplicatas.
- Revalidação SHA-256.
- Melhorias no MediaStore.
- Proteção contra exclusões não confirmadas.
- Correções de permissões.
- Melhorias de Billing e estados de interface.
- Identidade comercial Midiastore nas strings.
- Documentação de mercado e arquitetura.
- Pesquisa de tecnologias open source.
- Matriz de funcionalidades.
- Scripts de build e validação.
- Workflow GitHub Actions.
- Preparação de release VC14.

## Próximas tarefas prioritárias

1. Verificar se o último estado documental está commitado.
2. Executar novamente a compilação Debug.
3. Corrigir warnings relevantes do Lint.
4. Implementar detecção global de duplicatas em toda a biblioteca.
5. Testar segurança de exclusão em dispositivo real.
6. Desenvolver inteligência visual de mídia.
7. Desenvolver Storage History.
8. Melhorar o gerenciamento de arquivos.
9. Validar Billing e SKUs reais.
10. Resolver assinatura oficial.
11. Gerar APK e AAB definitivos.
12. Preparar publicação na Google Play.

## Regras para o próximo colaborador

- Não começar o projeto do zero.
- Não descartar alterações anteriores.
- Ler este documento antes de modificar o código.
- Ler docs/FEATURE_IMPLEMENTATION_MATRIX.md.
- Ler docs/RELEASE_BLOCKERS.md.
- Preservar applicationId e identidade técnica.
- Não publicar artefatos assinados com certificado temporário.
- Priorizar código funcional e testes.
- Investigar alternativas quando encontrar bloqueios.
- Não ficar limitado à criação de relatórios.

## Objetivo

Concluir o Midiastore como produto Android completo, seguro,
competitivo e comercializável para 2027/2028.
