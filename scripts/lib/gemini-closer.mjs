#!/usr/bin/env node
/**
 * DETERMINISTIC CLOSER (Versão Blindada — Zero Chave Gratuita Gemini API)
 *
 * Protege 100% a conta Google operando exclusivamente com templates determinísticos auditados:
 * - Sem chamadas externas de LLM.
 * - Sem promessas proibidas (RAM booster, CPU cooler, exclusão automática).
 * - Sempre reforça < 7 MB e confirmação explícita do usuário.
 */

export const GO_PLAY_URL = "https://midiastory-df6a0.web.app/go/play";
const PLAY_REVIEW_MAX_CHARS = 350;

export function buildGoPlayUrl(utmSource = "ig_dm", utmCampaign = "private_reply") {
  const u = new URL(GO_PLAY_URL);
  if (utmSource) u.searchParams.set("src", utmSource);
  if (utmCampaign) u.searchParams.set("cmp", utmCampaign);
  return u.toString();
}

export function isLimparKeyword(message) {
  const clean = String(message || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim();
  return /\blimpar\b/i.test(clean);
}

export async function replyInstagramDm(userMessage, options = {}) {
  const targetUrl = buildGoPlayUrl(options.utmSource || "ig_dm", options.utmCampaign || "private_reply");
  return {
    usedApi: false,
    mode: "DETERMINISTIC_BLINDADO",
    matchesLimpar: isLimparKeyword(userMessage),
    reply:
      `Aqui está o link oficial do Midiastore na Google Play:\n${targetUrl}\n\n` +
      `App ultraleve (< 7 MB): encontra vídeos grandes e duplicatas exatas (SHA-256) sem mexer nas suas conversas e só apaga o que você confirmar.`,
  };
}

const POSITIVE_REVIEW_TEMPLATES = {
  "pt-BR": [
    "Muito obrigado pela avaliação! Ficamos felizes em ajudar a liberar espaço. O Midiastore seguirá sempre ultraleve (< 7 MB) e 100% seguro, apagando apenas o que você confirmar.",
    "Obrigado pelo apoio! Nosso compromisso é manter o Midiastore rápido, leve (< 7 MB) e transparente no Android, sempre com sua confirmação antes de qualquer exclusão.",
  ],
  "en-US": [
    "Thank you for your review! We're glad Midiastore helped you reclaim storage. We keep the app ultra-light (< 7 MB) and safe—nothing is ever deleted without your confirmation.",
    "Thanks for the great feedback! Midiastore is built to stay compact (< 7 MB), private on-device, and 100% under your control with explicit confirmation before deleting files.",
  ],
  "it-IT": [
    "Grazie mille per la recensione! Siamo felici che Midiastore ti sia utile: resta sempre ultraleggero (< 7 MB) e sicuro, eliminando i file solo dopo la tua conferma.",
  ],
  "es-ES": [
    "¡Muchas gracias por tu valoración! Nos alegra ayudarte a liberar espacio con una app ligera (< 7 MB) y segura, donde nada se borra sin tu confirmación.",
  ],
};

export async function replyPlayStoreReview({ reviewText = "", starRating = 5, locale = "pt-BR", index = 0 }) {
  if (Number(starRating) < 4) {
    return {
      usedApi: false,
      action: "ENQUEUE_MANUAL_DRAFT",
      reply: null,
      draftForOwner:
        "Obrigado pelo relato. Queremos analisar seu caso com atenção — o Midiastore (< 7 MB) opera 100% no aparelho com confirmação do usuário. Conte mais detalhes pelo e-mail de suporte na ficha da Play Store.",
    };
  }
  const list = POSITIVE_REVIEW_TEMPLATES[locale] || POSITIVE_REVIEW_TEMPLATES["en-US"];
  const picked = list[Math.abs(Number(index) || 0) % list.length].slice(0, PLAY_REVIEW_MAX_CHARS);
  return {
    usedApi: false,
    action: "AUTO_TEMPLATE_4_5_STARS",
    charCount: picked.length,
    reply: picked,
    reviewPreview: String(reviewText).slice(0, 80),
  };
}
