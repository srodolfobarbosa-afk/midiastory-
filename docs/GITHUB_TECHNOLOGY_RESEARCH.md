# GitHub Technology Research

Consulta de metadados: 03/11/2026, GitHub REST API sem autenticação. Contagens de estrelas/issues são snapshots mutáveis; não são indicadores de segurança. Código foi lido como referência arquitetural e não incorporado.

## Repositórios solicitados

| Repositório | Licença | Atividade observada | Qualidade/sinais e limites | Decisão |
|---|---|---|---|---|
| [d4rken-org/sdmaid-se](https://github.com/d4rken-org/sdmaid-se) | GPL-3.0 | 7.967 stars, 25 issues abertas, último push/commit observado 02/11/2026 (`cac8e0472b0b`); vários workflows de CI, emulator e wrapper | Código de produto maduro, testes e automação visíveis. Issues recentes tratam de compatibilidade SAF/root/ADB e falhas do App Cleaner; demonstra custo real de manter capacidades específicas por API/OEM. Não foi feita análise completa de dependências nem auditoria de segurança. | Estudar desenho/limites. **Não copiar** código para o app sem decisão jurídica sobre GPL e distribuição. Não adotar root/ADB como premissa do produto. |
| [MZarc/Clearo](https://github.com/MZarc/Clearo) | MIT | Criado 20/07/2026, último push 07/08/2026, 1 star, 0 issues abertas no snapshot; último commit `7908e5b69115` foi atualização de README. Endpoint de workflows retornou 404. | README declara Flutter + canal nativo Kotlin, MediaStore/StorageStatsManager e dHash 9x8 em isolate. São claims do README, não resultados medidos. Repositório muito novo, sem sinal público suficiente sobre manutenção, testes, release ou segurança. `0 issues` não significa ausência de defeitos. | Útil como referência de decomposição dHash/UX; não adicionar dependência nem basear produção nele. MIT permite reutilização sujeita ao texto da licença, avisos e auditoria de dependências. |
| [FossifyOrg/Gallery](https://github.com/FossifyOrg/Gallery) | GPL-3.0 | 3.879 stars, 338 issues abertas, push observado em 03/11/2026; commit observado de localização coreana; workflows de CI, testes e Fastlane presentes | Aplicação adjacente de galeria/edição e privacidade, com comunidade ativa. O número de issues também pode refletir volume de usuários. Licença copyleft; risco de integração/cópia em app fechado. Não foi feita auditoria de dependências nem de CVEs. | Estudar fluxos de galeria, localização e edição. Reutilização de código exige análise jurídica; não incorporar. |

As issues consultadas foram amostras da primeira página da API, não inventário completo. `security_policy: null` significa que não foi encontrada política SECURITY.md via metadado, **não** que o projeto esteja livre de vulnerabilidades. Não foi possível consultar alertas Dependabot privados; não se presume inexistência de CVEs.

## Tecnologias pertinentes

- **MediaStore/ContentResolver:** API da plataforma, melhor fonte para mídia compartilhada e permissões concedidas. O app já usa; fallback precisa ser validado em aparelho/ROM, pois Cursor extras variam.
- **dHash/pHash:** calcular descritor compacto e distância de Hamming para gerar candidatos visuais. A afirmação de Clearo demonstra uma implementação declarada, não acurácia. Fazer benchmark com fotos reais consentidas; agrupar como “parecidas”, nunca apagar automaticamente.
- **SSIM:** comparação mais cara e normalmente dependente de pixels alinhados. Só avaliar em candidatos já reduzidos por dimensões/thumbnail; comparar custo CPU/bateria antes.
- **LiteRT / MediaPipe:** candidatos para modelo local apenas se um benchmark provar ganho sobre heurísticas de blur/qualidade. Não instalar modelo até conhecer tamanho, licença do modelo, memória, NPU/CPU, latência, consumo e cobertura de dispositivos. Links: [Google AI Edge LiteRT](https://github.com/google-ai-edge/litert), [MediaPipe](https://github.com/google-ai-edge/mediapipe). Auditoria de release/licença/dependências ainda requerida antes de inclusão.
- **Room:** opção futura para histórico de amostras, catálogo de análises e grupos; hoje o app usa `SharedPreferences` em armazenamento pequeno e efêmero. Não migrar sem caso de uso/volume e teste de migração. [Android Room](https://developer.android.com/training/data-storage/room).
- **WorkManager:** adequado para relatórios/alertas periódicos flexíveis, não para exclusão nem processamento que precise ocorrer em horário exato. Requer opt-in, constraints de bateria/rede e cancelamento. [Android WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent).
- **SAF:** mecanismo recomendado para Downloads/documentos escolhidos pela pessoa; não substituí-lo por `MANAGE_EXTERNAL_STORAGE` sem necessidade central e aprovação da Play. [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider).

Os links do LiteRT/MediaPipe/AndroidX são pistas para pesquisa adicional, não aprovação de versões ou licenças de artefatos específicos. Antes de adicionar qualquer pacote: fixar versão, verificar licença do código e do modelo, SBOM/transitivas, advisories, releases/commits, compatibilidade e custo de manutenção. Não foi adicionada dependência nesta etapa.

## Critérios de seleção

1. Preferir API Android/Jetpack madura a código de terceiros para MediaStore, SAF, WorkManager e Billing.
2. Para pHash/dHash, manter implementação pequena e própria só após teste de correção e benchmarks; algoritmo simples também precisa de corpus de validação e análise de falsos positivos.
3. GPL-3.0 não é copiada para o app. MIT tampouco dispensa preservação de aviso, revisão de dependências ou análise de patentes/claims.
4. Modelos de IA só entram quando oferecem ganho mensurável, funcionam offline e não elevam risco de privacidade, footprint ou suporte OEM.
5. Reexecutar consulta de licença, atividade, issues e advisories na data de cada release; os snapshots deste documento envelhecem.