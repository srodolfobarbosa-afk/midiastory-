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

export const DOWNLOAD_INTENT_KEYWORDS = [
  "eu quero",
  "limpar",
  "link",
  "app",
  "quero",
  "baixar",
  "manda",
  "espaco",
  "memoria",
  "download",
];

const DOWNLOAD_INTENT_REGEX =
  /(?<![a-z0-9])(?:eu\s+quero|limpar|link|app|quero|baixar|manda|espaco|memoria|download)(?![a-z0-9])/i;

export function normalizeIntentText(message) {
  return String(message || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

export function matchDownloadIntentKeyword(message) {
  const clean = normalizeIntentText(message);
  const m = clean.match(DOWNLOAD_INTENT_REGEX);
  return m ? m[0].replace(/\s+/g, " ") : null;
}

export function isLimparKeyword(message) {
  return Boolean(matchDownloadIntentKeyword(message));
}

const PHONE_PAIN_REGEX =
  /(?<![a-z0-9])(?:pesado|travando|lento|cheio|cheia|lotado|lotada|armazenamento|whatsapp|videos?\s+grandes?|duplicad[oa]s?)(?![a-z0-9])/i;
const VIDEO_TOPIC_REGEX =
  /(?<![a-z0-9])(?:video|reel|reels|noticia|post|reportagem|historia|caso|materia|vi\s+aqui|assist[ii])(?![a-z0-9])/i;
const NEGATIVE_OR_IOS_REGEX =
  /(?<![a-z0-9])(?:iphone|ios|apple|nao\s+quero|nao\s+preciso|pare|parar)(?![a-z0-9])/i;

/**
 * Funil Conversacional de 3 Mensagens da Maria do Midiastore (100% Determinístico, Zero LLM):
 * - Turno 1: "Olá, tudo bem!" + pergunta aberta acolhedora.
 * - Turno 2: Responde o assunto da pessoa + apresenta "Eu sou a Maria do Midiastore" + puxa a dor do telefone pesado.
 * - Turno 3 (ou atalho imediato se já pedir link/app/limpar): Manda baixar na Play Store + benefícios reais (< 7 MB, SHA-256, confirmação).
 */
export function buildMariaInboxTurnReply({ userMessage = "", nextTurn = 1 }) {
  const clean = normalizeIntentText(userMessage);
  const directKeyword = matchDownloadIntentKeyword(clean);
  const hasPhonePain = PHONE_PAIN_REGEX.test(clean);
  const isNegativeOrIos = NEGATIVE_OR_IOS_REGEX.test(clean);
  const inboxLink = buildGoPlayUrl("ig_inbox", "maria_3msg");

  if (isNegativeOrIos && nextTurn >= 2) {
    return {
      effectiveTurn: 3,
      closeConversation: true,
      intentDetected: "IOS_OR_OPT_OUT",
      reply:
        `Sem problemas! Muito obrigada por acompanhar o @aconteceunobr 💙 Se alguém da família usar Android com memória cheia, o Midiastore (< 7 MB) fica disponível na Google Play: ${inboxLink}`,
    };
  }

  // Atalho agressivo: se já pediu app/link/limpar ou falou de telefone pesado/travando, já fecha na hora!
  if (directKeyword || hasPhonePain || nextTurn >= 3) {
    return {
      effectiveTurn: 3,
      closeConversation: true,
      intentDetected: directKeyword ? `DIRECT_INTENT:${directKeyword}` : hasPhonePain ? "PHONE_HEAVY_PAIN" : "TURN_3_CLOSER",
      reply:
        `Prazer, eu sou a Maria do Midiastore! Já baixa nosso app oficial na Google Play 👇\n${inboxLink}\n\n` +
        `Por que vale baixar no telefone pesado:\n` +
        `• Ultraleve (< 7 MB): instala rápido até com memória quase cheia\n` +
        `• Acha vídeos grandes e cópias exatas (SHA-256) sem apagar suas conversas\n` +
        `• 100% seguro: só apaga o que você confirmar!`,
    };
  }

  if (nextTurn === 1) {
    return {
      effectiveTurn: 1,
      closeConversation: false,
      intentDetected: "ICEBREAKER_TURN_1",
      reply:
        `Olá, tudo bem? 😊 Que bom receber sua mensagem aqui no @aconteceunobr! Me conta rapidinho: você mandou sobre algum vídeo que viu aqui ou precisa de uma ajuda?`,
    };
  }

  // Turno 2: responde o contexto da pessoa + apresenta a Maria do Midiastore + gancho do telefone pesado
  const contextAck = VIDEO_TOPIC_REGEX.test(clean)
    ? `Entendi! Esse conteúdo repercutiu bastante por aqui, obrigada por acompanhar e comentar com a gente!`
    : `Perfeito, obrigada por falar com a gente e acompanhar a página!`;

  return {
    effectiveTurn: 2,
    closeConversation: false,
    intentDetected: VIDEO_TOPIC_REGEX.test(clean) ? "TURN_2_VIDEO_ACK" : "TURN_2_GENERAL_ACK",
    reply:
      `${contextAck}\n\n` +
      `Aproveitando, prazer: eu sou a Maria, do Midiastore! Quem recebe vídeos todo dia no WhatsApp e Instagram quase sempre fica com o telefone pesado e sem espaço. O seu celular também anda ficando com a memória cheia?`,
  };
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
