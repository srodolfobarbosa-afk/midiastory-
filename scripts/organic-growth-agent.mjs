import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const secrets = JSON.parse(fs.readFileSync(path.join(projectRoot, ".secrets", "ai-media-keys.json"), "utf8"));

async function generateGrowthKit() {
  const prompt = `Você é o Diretor de Growth AI do aplicativo Android "Midiastore: Limpador de Espaço" (pacote com.rdf112.midiastore, site oficial https://midiastory-df6a0.web.app/, link Play Store https://play.google.com/store/apps/details?id=com.rdf112.midiastore).
Gere um pacote JSON estruturado cobrindo todas as 20 opções de divulgação orgânica, parcerias e retenção:
1. "googleSearchKeywords": 10 palavras-chave transacionais de alta intenção em pt-BR e en-US.
2. "reelsAndShortsHooks": 5 roteiros curtos de 10 segundos (gancho visual + legenda + hashtags) focados em liberar espaço real e achar cópias exatas SHA-256.
3. "tiktokCommentReplies": 5 respostas inteligentes para comentar em vídeos virais de dicas de celular/memória cheia oferecendo uma solução imediata dentro do app.
4. "pinterestPins": 3 títulos e descrições otimizadas para SEO no Pinterest com link direto para a Play Store.
5. "telegramBroadcast": 1 mensagem pronta de alto impacto para canais de ofertas/dicas no Telegram.
6. "microInfluencerOutreach": 3 mensagens diretas (DM/E-mail) para prospectar micro-influenciadores (5k a 20k seguidores) propondo permuta automática (acesso Vitalício ao Midiastore Pro).
7. "dealPortalsPromobitPelando": 2 textos prontos de postagem de oferta/achadinho para Promobit e Pelando.
8. "inboundBlogOutreach": 2 propostas de artigo guest-post para blogs parceiros oferecendo solução imediata dentro do app.
9. "smartPushNotifications": 4 notificações push / in-app inteligentes baseadas em comportamento (novos arquivos grandes >10MB detectados, lembrete de limpeza mensal e oportunidade no Plano Pro).
10. "viralReferralInvite": 2 mensagens magnéticas de "Indique e Compartilhe no WhatsApp" para usuários satisfeitos após liberarem espaço.
11. "abandonedProRecoveryEmails": 2 e-mails curtos e persuasivos escritos por IA para recuperar usuários que abriram os Planos Pro mas ainda não finalizaram.
12. "productHuntLaunch": 1 kit de lançamento para Product Hunt / TabNews (tagline de 60 caracteres + descrição do criador + primeiro comentário técnico).
13. "gamificationDailyChallenges": 3 desafios diários automáticos in-app estilo streak (ex: "Abra o app por 5 dias seguidos e ganhe benefício especial no Plano Pro") para manter o usuário ativo sem gastar com mídia.
Responda APENAS com JSON válido.`;

  const res = await fetch(secrets.OPENROUTER_BASE_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${secrets.OPENROUTER_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "openai/gpt-4o-mini",
      response_format: { type: "json_object" },
      messages: [{ role: "user", content: prompt }],
    }),
  });

  if (!res.ok) {
    throw new Error(`OpenRouter status ${res.status}: ${await res.text()}`);
  }
  const data = await res.json();
  const content = JSON.parse(data.choices[0].message.content);
  const campaignsDir = path.join(projectRoot, "docs", "campaigns");
  fs.mkdirSync(campaignsDir, { recursive: true });
  fs.writeFileSync(path.join(campaignsDir, "latest-campaign.json"), JSON.stringify(content, null, 2));
  console.log("Kit completo das 20 frentes (1 a 20) gerado em docs/campaigns/latest-campaign.json!");
}

generateGrowthKit().catch((err) => {
  console.error(err);
  process.exit(1);
});
