import fs from "node:fs";
import crypto from "node:crypto";

const aiKeys = JSON.parse(fs.readFileSync(".secrets/ai-media-keys.json", "utf8"));
const playKey = JSON.parse(fs.readFileSync(".secrets/play-service-account.json", "utf8"));
const packageName = "com.rdf112.midiastore";
const playStoreUrl = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";

async function getPlayAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: playKey.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: playKey.token_uri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");
  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const jwt = `${header}.${payload}.${sign.sign(playKey.private_key, "base64url")}`;

  const res = await fetch(playKey.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  return data.access_token;
}

async function generateAiReply(contextPrompt) {
  const res = await fetch(aiKeys.OPENROUTER_BASE_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${aiKeys.OPENROUTER_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "openai/gpt-4o-mini",
      messages: [{ role: "user", content: contextPrompt }],
    }),
  });
  const data = await res.json();
  return data.choices?.[0]?.message?.content?.trim() || "";
}

async function checkAndReplyPlayStoreReviews() {
  const token = await getPlayAccessToken();
  const res = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/reviews`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const data = await res.json();
  const reviews = data.reviews || [];
  console.log(`[Google Play Reviews] Avaliações encontradas: ${reviews.length}`);

  for (const rev of reviews) {
    const comments = rev.comments || [];
    const userComment = comments[0]?.userComment;
    const hasDevReply = comments.some((c) => c.developerComment);
    if (userComment && !hasDevReply) {
      const replyText = await generateAiReply(
        `Responda de forma calorosa, curta (máximo 280 caracteres) e profissional a esta avaliação de ${userComment.starRating} estrelas no aplicativo Android Midiastore ("${userComment.text}"):`
      );
      if (replyText) {
        await fetch(
          `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/reviews/${rev.reviewId}:reply`,
          {
            method: "POST",
            headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
            body: JSON.stringify({ replyText }),
          }
        );
        console.log(`Respondida avaliação ${rev.reviewId}:`, replyText);
      }
    }
  }
}

async function main() {
  console.log("=== INICIANDO MOTOR DO 'LOOP INVISÍVEL' (MIDIASTORE) ===");
  await checkAndReplyPlayStoreReviews();

  const reelHook = await generateAiReply(
    `Crie 1 legenda magnética e curta para um Instagram Reel / TikTok do aplicativo Android "Midiastore: Limpador de Espaço" (que mostra a nova interface esmeralda estilo Big Tech, limpa arquivos >10MB grátis e acha cópias exatas com SHA-256). Use a estratégia do "Loop Invisível": peça para a pessoa comentar a palavra "ESPAÇO" para receber o link oficial da Google Play (${playStoreUrl}) e a condição especial do Plano Pro no Direct.`
  );
  console.log("\n--- LEGENDA PRONTA DO 'LOOP INVISÍVEL' PARA O SEU VÍDEO (midiastore_reel_instagram.mp4) ---\n");
  console.log(reelHook);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
