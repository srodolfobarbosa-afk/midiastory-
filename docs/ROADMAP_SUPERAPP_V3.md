# Midiastore — Roadmap modular v3

**Direção registrada por decisão explícita do proprietário em 2026-11-28.** Este roadmap descreve fases futuras; uma função listada não é uma afirmação de que já foi implementada ou aprovada para produção. A tag `v2.8.2-carbon-baseline` permanece imutável.

## v2.9.1 — Loop Viral WhatsApp + base modular

- Compartilhar apenas por ação explícita do usuário, usando o Android Sharesheet e o card real de GB liberados.
- Incluir link direto da ficha Midiastore na Google Play, com `referrer` de campanha distinto para resultado e convite.
- Mostrar bytes/GB somente após confirmação da remoção permanente; arquivo na Lixeira não conta como espaço liberado.
- Criar contrato `ToolRegistry` com catálogo explícito, IDs estáveis, execução suspensa, URIs de entrada/saída e estados de cancelamento/falha. Sem descoberta por reflexão ou execução arbitrária.
- Manter o processamento local e não enviar mídia ou nomes de arquivo no compartilhamento.

## v3.0 — Caixa de Ferramentas local em Kotlin

Ferramentas on-device, registradas individualmente no `ToolRegistry` e sem upload de mídia:

1. **Compressor Zap/Reels:** transcodificação nativa com `MediaCodec`; produzir cópia, comparar tamanho/qualidade e preservar o original até revisão e consentimento oficial para removê-lo.
2. **Fotos para PDF:** selecionar imagens e gerar PDF local; pedir destino com Storage Access Framework.
3. **Extrator de Áudio:** extrair faixas compatíveis de vídeo com APIs Android; gravar somente em destino escolhido pelo usuário.
4. **Limpeza Inteligente WhatsApp:** apresentar candidatos acessíveis via MediaStore/SAF, com escopo e cobertura declarados; revisão item a item e fluxo de consentimento Android. Nunca prometer acesso a diretórios restritos nem apagar automaticamente.
5. **Cofre Biométrico:** protótipo local com Android Keystore e AndroidX Biometric, ameaça e recuperação documentadas; dados ficam criptografados e nenhum conteúdo é sincronizado sem nova decisão explícita.

Preservar as abas atuais `Limpar`, `Arquivos` e `Pro`. A localização/navegação da Caixa de Ferramentas depende de planta visual e aprovação explícita antes de qualquer mudança de UI.

## v3.5 — Maria + ToolRegistry

- Maria poderá selecionar ferramentas permitidas pelo registry usando argumentos tipados e validação determinística.
- Mostrar a ação, arquivos envolvidos e efeito previsto antes de execução; exigir confirmação para operações com efeitos externos ou destrutivos.
- Recusar ferramenta desconhecida, parâmetro inválido, cobertura parcial ou permissão ausente; registrar resultado verificável.
- Maria permanece intacta. Qualquer evolução visual requer protótipo isolado e aprovação visual explícita do proprietário.

## v4.0 — Super LLM híbrida sob demanda

- Roteamento explícito entre recursos locais/offline e modelo remoto apenas quando solicitado e necessário.
- Consentimento antes de transmitir qualquer dado; enviar contexto mínimo, nunca mídia bruta por padrão.
- Orçamento, limites de uso, timeout, cancelamento, fallback e indicação clara de processamento local/remoto.
- O LLM planeja e explica; o registry valida ações e o usuário confirma efeitos. Sem exclusão autônoma, execução de código arbitrário ou cobrança de plataforma paga sem autorização.

## Gates comuns

- Implementar por entregas pequenas e reversíveis; sem alterar `applicationId`, assinatura ou versionamento protegido.
- Verificar strings nos 14 idiomas, acessibilidade, consumo de memória/bateria, cancelamento, erros de provider e comportamento em API 29–36.
- Testar cópia de saída, URI e bytes realmente liberados antes de alegar economia. Android Trash não é espaço liberado.
- Exigir CI e QA em dispositivo para mudanças Android; build verde sozinho não valida o fluxo.
- Medir compartilhamentos, abertura da Play Store, instalação atribuída quando disponível e retenção com fonte/janela declaradas. Não tratar clique como instalação ou venda.
