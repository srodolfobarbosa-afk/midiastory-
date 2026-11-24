# Política de Privacidade do Sweep — rascunho v0.3

**Última revisão:** 31 de outubro de 2026  
**Status:** rascunho técnico; não publicar sem revisão do responsável legal e conferência do binário final.

## Resumo do incremento atual

Esta versão de desenvolvimento analisa metadados de fotos, vídeos e áudio, e miniaturas de imagens/vídeos, que o usuário autorizou o Android a disponibilizar ao Sweep. As consultas e a renderização das miniaturas ocorrem localmente no dispositivo. O código deste incremento não implementa conta, anúncios, analytics, telemetria, sincronização, rede ou envio de mídia a servidor.

## Dados acessados

Com a permissão concedida, o Sweep consulta o MediaStore para obter URI/ID, nome de exibição quando fornecido, tamanho em bytes, MIME e data de modificação de fotos, vídeos e áudio, por volume acessível. Para exibir itens visuais, solicita miniaturas ao provedor local via `ContentResolver.loadThumbnail`; itens de áudio usam um ícone local e não são enviados a outro app. O app não calcula hashes de conteúdo nesta fatia v0.3.

## Permissões

O app solicita acesso de leitura conforme a versão do Android, incluindo acesso parcial a imagens/vídeos no Android 14+ e `READ_MEDIA_AUDIO` no Android 13+ para áudio. Se o usuário conceder acesso parcial, a interface informa que os resultados não cobrem a biblioteca inteira. A versão atual não solicita `MANAGE_EXTERNAL_STORAGE`, permissões de exclusão, acesso à Internet ou a arquivos privados de outros aplicativos.

## Operações e compartilhamento

Esta fatia não inclui Lixeira, exclusão, upload, compartilhamento ou abertura de conteúdo em outro app. A seleção é apenas uma lista temporária para revisão local.

## Retenção

Os resultados e a seleção permanecem em memória durante a sessão da aplicação. Não são enviados para conta/nuvem nem persistidos no dispositivo por este incremento.

## Contato

**[Inserir nome legal do responsável, endereço de contato de privacidade e URL pública antes de publicar.]**

---

Este documento descreve o escopo atual do código v0.3. Qualquer SDK, Billing, anúncios, analytics, backend, exportação Pro ou mudança de coleta exige revisão desta política, do formulário Data safety e da ficha da Play Store com base no artefato final.
