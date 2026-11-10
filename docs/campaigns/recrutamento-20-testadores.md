# Plano de Ação: Recrutamento dos 20 Testadores do Google Play (Meta: 24 a 48 Horas)

O Google Play exige que contas de desenvolvedores pessoa física mantenham **pelo menos 20 testadores** optados no teste fechado por 14 dias antes de liberar a produção.

O nosso funil oficial está no ar com cadastro de 1 clique:
👉 **https://midiastory-df6a0.web.app/#vip-testers**

---

## 📲 1. Mensagens Prontas para WhatsApp e Telegram

### Opção A: Para Grupos de Família e Amigos Próximos (Conversão Rápida ~8 a 12 pessoas)
> *"Fala pessoal, tudo bem? 🚀 Terminei de desenvolver meu aplicativo para Android, o **Midiastore** (ele ajuda a achar vídeos gigantes e liberar espaço no celular com segurança).*  
> *Para o Google liberar o app na Play Store para todo mundo, eles exigem 20 testadores voluntários.*  
> *Quem puder me dar essa força rápida, só precisa colocar o e-mail do Google Play aqui na página (leva 10 segundos):*  
> 👉 **https://midiastory-df6a0.web.app/#vip-testers**  
> *Em agradecimento, todo mundo que entrar ganha 3 meses de conta PRO de graça! Valeu demais!"*

---

### Opção B: Para Grupos de Tecnologia, Programação e Faculdades (~5 a 10 pessoas)
> *"E aí devs e entusiastas! Lancei o Midiastore, um limpador de armazenamento inteligente para Android 13/14 com processamento 100% local, detecção SHA-256 e sem permissões abusivas de sistema.*  
> *Estou rodando a fase de Closed Testing do Google Play e preciso fechar a cota de 20 testers.*  
> *Quem quiser testar a build e ganhar licença Pro antecipada:*  
> 👉 **https://midiastory-df6a0.web.app/#vip-testers**  
> *Se alguém tiver app no teste fechado também, troco o teste com vocês! 🤝"*

---

## 📸 2. Estratégia para Instagram (Stories + Direct)

### Story 1 (Enquete de Engajamento):
* **Fundo:** Foto ou print de tela do celular com aviso de armazenamento cheio.
* **Texto:** *"Seu celular vive travando sem espaço na pior hora?"*
* **Enquete:** `[SIM! Todo dia 🤦‍♂️]` vs `[Não, tá tranquilo]`

### Story 2 (Chamada com Link direto):
* **Fundo:** Print do Midiastore mostrando o medidor verde ("Mais espaço. Mais tranquilidade").
* **Texto:** *"Acabei de lançar o Midiastore no Android pra resolver isso de vez. Preciso de 20 pessoas pra testar antes da loja abrir pro mundo!"*
* **Figurinha de Link:** `https://midiastory-df6a0.web.app/#vip-testers` (Texto da figurinha: **Entrar no Teste VIP 🚀**)

### Direct Automático / Resposta de Stories:
> *"Opa! Vi que você votou na enquete. Criei o Midiastore exatamente pra resolver isso sem você precisar apagar suas fotos importantes. Coloca seu e-mail aqui pra eu liberar seu acesso com 3 meses de Pro grátis:*  
> 👉 **https://midiastory-df6a0.web.app/#vip-testers**"

---

## 🌐 3. Comunidades de Troca Mútua (Reddit & Discord)
*Existem milhares de desenvolvedores no mesmo barco que você trocando testes diários.*

### Reddit (Subreddit: r/AndroidClosedTesting e r/AndroidDev)
* **Título:** `[Midiastore] Need 20 testers for storage cleaner app - will test your app back instantly! 🤝`
* **Texto em Inglês:**
  > *"Hey guys! I built Midiastore, a privacy-first transparent storage cleaner for Android (100% on-device SHA-256).  
  > I need 20 testers to satisfy Google's 14-day requirement.  
  > Drop your Google Play email here: **https://midiastory-df6a0.web.app/#vip-testers** or DM me.  
  > Leave your app link in the comments and I will install, test, and keep your app active for 14 days too!"*

---

## ⚡ 4. O que a nossa Automação faz a cada cadastro:
1. O usuário cadastra o e-mail no site `https://midiastory-df6a0.web.app/#vip-testers`.
2. O dado é salvo instantaneamente na tabela `public.beta_testers` do Supabase.
3. O script `scripts/sync-play-testers.mjs` exporta a lista atualizada para `docs/campaigns/lista-20-testadores-google-play.txt`.
4. No Google Play Console, vá em **Teste fechado (Alpha) -> Testadores -> Lista de e-mails** e cole a lista para liberar o download no celular de todos de uma só vez!
