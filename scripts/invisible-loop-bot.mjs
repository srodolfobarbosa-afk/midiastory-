import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { executeWithFailover } from "./ai-failover-router.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const packageName = "com.rdf112.midiastore";
const playStoreUrl = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";

function loadPlayServiceAccount() {
  if (process.env.PLAY_SERVICE_ACCOUNT_JSON) {
    return JSON.parse(process.env.PLAY_SERVICE_ACCOUNT_JSON);
  }
  const playKeyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
  if (fs.existsSync(playKeyPath)) {
    return JSON.parse(fs.readFileSync(playKeyPath, "utf8"));
  }
  throw new Error("Credenciais da Google Play Service Account não encontradas.");
}

async function getPlayAccessToken() {
  const playKey = loadPlayServiceAccount();
  const now = Math.floor(Date.now() / 1000);
  const tokenUri = playKey.token_uri || "https://oauth2.googleapis.com/token";
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: playKey.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: tokenUri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");
  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const jwt = `${header}.${payload}.${sign.sign(playKey.private_key, "base64url")}`;

  const res = await fetch(tokenUri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!res.ok || !data.access_token) {
    throw new Error(`Falha ao obter token OAuth da Google Play (HTTP ${res.status})`);
  }
  return data.access_token;
}

export async function generateAiReply(contextPrompt) {
  const result = await executeWithFailover(contextPrompt, { maxTokens: 400 });
  return (result.content || "").trim();
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

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
