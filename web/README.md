# Chat Midiastory

Aplicativo Next.js simples para conversar com documentos armazenados no Supabase.

## Configuração

1. Na raiz do repositório, copie `.env.example` para `.env.local`.
2. Preencha a chave privada de serviço do Supabase e uma nova chave do Gemini no `.env.local`. Não use a chave do Gemini que foi compartilhada anteriormente; gere outra no Google AI Studio.
3. No painel do Supabase, execute `supabase/migrations/20261107000100_chat_schema.sql` no SQL Editor.
4. Instale as dependências dentro de `web/` com `npm install`.
5. Inicie o chat com `npm run dev` dentro de `web/`.

A senha do banco não é necessária para o chat, que usa a API REST do Supabase. A chave `SUPABASE_SERVICE_ROLE_KEY` fica apenas no servidor e nunca deve começar com `NEXT_PUBLIC_`.

## Limites desta versão

O chat cria embeddings de perguntas, procura até cinco documentos por similaridade vetorial, envia o contexto ao Gemini e salva perguntas e respostas. A busca depende de documentos com embeddings já carregados na tabela `documents`. As integrações de Drive, Gmail, Calendar e Telegram precisam de credenciais OAuth ou token do bot e ainda não estão ligadas nesta versão.

O script `../backup_celular.py` copia as pastas de fotos comuns do Android via ADB para `backup/` e tenta registrar cada pasta na tabela `mobile_backups`.
