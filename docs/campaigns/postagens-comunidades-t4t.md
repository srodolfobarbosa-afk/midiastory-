# 🚀 Kit de Disparo Direto: Captação dos 19 Testadores Google Play (T4T & Orgânico)

Use estes templates prontos para postar nas comunidades mais ativas de teste fechado do mundo e nos canais de tecnologia do Brasil.

---

## 1. Reddit: `r/AndroidClosedTesting` & `r/TestersCommunity` (Estratégia T4T - Test For Test)
> **Dica de Ouro:** A comunidade de closed testing do Reddit opera no modelo "Test for Test" (eu testo o seu app, você testa o meu). A taxa de resposta é imediata (1 a 2 horas para preencher 10 a 20 vagas).

**Título do Post:**
```
[T4T] Need 19 testers for WhatsApp & Media Cleaner (Midiastore) - Will test yours immediately!
```

**Corpo do Post:**
```markdown
Hi fellow developers! 👋

I'm finalizing the 14-day closed testing requirement for **Midiastore** (`com.rdf112.midiastore`), a native Android media & WhatsApp cleaner built with Jetpack Compose (100% Scoped Storage compliant, zero `MANAGE_EXTERNAL_STORAGE`).

I will gladly **test your app back immediately** for the full 14 days and leave honest feedback!

### How to Join:
1. **Direct Web Signup & VIP Pro (3 Months Free):**
   👉 https://midiastory-df6a0.web.app/en/
   *(Enter your Google Play email and I will add you to the Google Play Console Alpha list right away)*

2. **Google Play Testing Links (Once added):**
   - **Join on Android:** https://play.google.com/store/apps/details?id=com.rdf112.midiastore
   - **Join on Web:** https://play.google.com/apps/testing/com.rdf112.midiastore

Drop your app link, Google Group, or signup URL in the comments below with a screenshot or confirmation and I will install and keep yours active daily! 🤝
```

---

## 2. TabNews (Comunidade Tech Brasil)
**Título do Post:**
```
Como construí um limpador de WhatsApp no Android 15 sem MANAGE_EXTERNAL_STORAGE (e preciso de 19 testadores)
```

**Corpo do Post:**
```markdown
Fala pessoal do TabNews!

Recentemente me deparei com um problema clássico: o WhatsApp acumulando dezenas de gigabytes em áudios `.opus`, vídeos encaminhados e memes em pastas ocultas `.nomedia` que o app nativo de Galeria simplesmente esconde.

Para resolver isso respeitando 100% as diretrizes estritas do Google Play (onde utilitários que pedem `MANAGE_EXTERNAL_STORAGE` são sumariamente rejeitados), desenvolvi o **Midiastore**:
- **Stack:** Kotlin, Jetpack Compose, Material 3 e corrotinas assíncronas.
- **Consultas Otimizadas:** Varredura direta via `MediaStore.Files.getContentUri("external")` com ordenação `SIZE DESC` no SQLite para evitar travamentos de UI (ANR).
- **Exclusão Segura:** Delegando a confirmação exclusivamente ao diálogo nativo do sistema operacional (`MediaStore.createDeleteRequest`).

### Teste Fechado Google Play (20 Testadores)
Como muitos sabem, o Google agora exige 20 testadores voluntários por 14 dias contínuos antes de liberar o app para produção.

Se você tem um aparelho Android e quer liberar espaço (e dar uma olhada na arquitetura):
👉 Cadastre seu e-mail no nosso funil: **https://midiastory-df6a0.web.app/**

Como agradecimento pelo feedback, você ganha **3 meses de Midiastore Pro grátis** com exclusão em lote com 1 toque e zero anúncios.

Quem tiver sugestões de arquitetura Android moderna ou quiser trocar feedback de apps, os comentários estão abertos!
```

---

## 3. Disparo WhatsApp / Grupos de Amigos & Família
```
Oi pessoal! Acabei de desenvolver um aplicativo oficial para Android chamado Midiastore que ajuda a descobrir e apagar com segurança aqueles vídeos pesados e áudios esquecidos do WhatsApp que lotam a memória do celular.

O Google Play exige 20 voluntários para testar antes do lançamento mundial. Quem puder me dar uma força e testar no seu celular, entra nesse link rápido e coloca seu e-mail da Play Store:
👉 https://midiastory-df6a0.web.app/

Vocês ganham 3 meses de acesso Pro grátis para limpar o celular! Valeu demais! 🚀
```
