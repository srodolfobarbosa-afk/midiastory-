#!/usr/bin/env node
/**
 * INSTAGRAM GRAPH API v27.0 CLIENT — @aconteceunobr / Midiastore
 * 100% Node.js (Zero Python) | Segredos apenas no .env local (nunca no Git)
 *
 * Comandos disponíveis:
 *   node scripts/instagram-api.mjs setup                 -> Descobre FACEBOOK_PAGE_ID + INSTAGRAM_USER_ID e salva no .env
 *   node scripts/instagram-api.mjs status                -> Lê perfil ao vivo + últimos 10 posts/Reels (somente leitura)
 *   node scripts/instagram-api.mjs permissions           -> Lista permissões ativas do META_ACCESS_TOKEN
 *   node scripts/instagram-api.mjs comments <mediaId>    -> Lê comentários de um post/Reel
 *   node scripts/instagram-api.mjs insights <mediaId>    -> Lê métricas (reach, likes, comments, shares, saved) de um post
 *   node scripts/instagram-api.mjs comment <mediaId> "texto" --confirm
 *   node scripts/instagram-api.mjs publish-photo <imageUrl> "legenda" [--confirm]
 *   node scripts/instagram-api.mjs publish-reel <videoUrl> "legenda" [--confirm]
 *   node scripts/instagram-api.mjs publish-carousel <url1,url2,...> "legenda" [--confirm]
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const ENV_PATH = path.join(projectRoot, ".env");
const GRAPH_BASE = "https://graph.facebook.com/v27.0";

function loadDotEnv() {
  if (!fs.existsSync(ENV_PATH)) return {};
  const lines = fs.readFileSync(ENV_PATH, "utf8").split(/\r?\n/);
  const map = {};
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const idx = trimmed.indexOf("=");
    if (idx <= 0) continue;
    const key = trimmed.slice(0, idx).trim();
    const val = trimmed.slice(idx + 1).trim().replace(/^["']|["']$/g, "");
    map[key] = val;
    if (!process.env[key]) process.env[key] = val;
  }
  return map;
}

function upsertEnvVar(key, value) {
  let content = fs.existsSync(ENV_PATH) ? fs.readFileSync(ENV_PATH, "utf8") : "";
  const regex = new RegExp(`^${key}=.*$`, "m");
  if (regex.test(content)) {
    content = content.replace(regex, `${key}=${value}`);
  } else {
    if (content.length > 0 && !content.endsWith("\n")) content += "\n";
    content += `${key}=${value}\n`;
  }
  fs.writeFileSync(ENV_PATH, content, "utf8");
  process.env[key] = value;
}

function requireToken() {
  const token = process.env.META_ACCESS_TOKEN?.trim();
  if (!token) {
    throw new Error("META_ACCESS_TOKEN não encontrado no .env local.");
  }
  return token;
}

async function graphGet(endpoint, params = {}) {
  const token = requireToken();
  const url = new URL(`${GRAPH_BASE}${endpoint}`);
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null) url.searchParams.set(k, String(v));
  }
  url.searchParams.set("access_token", token);

  const res = await fetch(url, { signal: AbortSignal.timeout(20000) });
  const data = await res.json();
  if (!res.ok || data.error) {
    throw new Error(`Graph GET ${endpoint} falhou (HTTP ${res.status}): ${JSON.stringify(data.error || data)}`);
  }
  return data;
}

async function graphPost(endpoint, bodyParams = {}) {
  const token = requireToken();
  const url = `${GRAPH_BASE}${endpoint}`;
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(bodyParams)) {
    if (v !== undefined && v !== null) form.set(k, String(v));
  }
  form.set("access_token", token);

  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: form.toString(),
    signal: AbortSignal.timeout(30000),
  });
  const data = await res.json();
  if (!res.ok || data.error) {
    throw new Error(`Graph POST ${endpoint} falhou (HTTP ${res.status}): ${JSON.stringify(data.error || data)}`);
  }
  return data;
}

async function cmdPermissions() {
  const me = await graphGet("/me", { fields: "id,name" });
  const perms = await graphGet("/me/permissions");
  console.log(JSON.stringify({ me, permissions: perms.data || [] }, null, 2));
}

async function cmdSetup() {
  const accounts = await graphGet("/me/accounts", {
    fields: "id,name,instagram_business_account{id,username}",
  });
  const pages = accounts.data || [];
  const summary = pages.map((p) => ({
    pageId: p.id,
    pageName: p.name,
    igId: p.instagram_business_account?.id || null,
    igUsername: p.instagram_business_account?.username || null,
  }));

  // Prioriza @aconteceunobr ou a primeira página com instagram_business_account vinculado
  const target =
    pages.find((p) => p.instagram_business_account?.username?.toLowerCase() === "aconteceunobr") ||
    pages.find((p) => p.instagram_business_account?.id) ||
    pages[0];

  if (target) {
    upsertEnvVar("FACEBOOK_PAGE_ID", target.id);
    if (target.instagram_business_account?.id) {
      upsertEnvVar("INSTAGRAM_USER_ID", target.instagram_business_account.id);
    }
  }

  console.log(
    JSON.stringify(
      {
        graphApiVersion: "v27.0",
        pagesFound: pages.length,
        pages: summary,
        savedToEnv: {
          FACEBOOK_PAGE_ID: process.env.FACEBOOK_PAGE_ID || null,
          INSTAGRAM_USER_ID: process.env.INSTAGRAM_USER_ID || null,
        },
      },
      null,
      2
    )
  );
}

async function cmdStatus() {
  const igId = process.env.INSTAGRAM_USER_ID?.trim();
  if (!igId) {
    throw new Error("INSTAGRAM_USER_ID não definido no .env. Rode `node scripts/instagram-api.mjs setup` primeiro.");
  }

  const profile = await graphGet(`/${igId}`, {
    fields: "id,username,name,followers_count,follows_count,media_count,biography,website",
  });

  const media = await graphGet(`/${igId}/media`, {
    fields: "id,caption,media_type,media_product_type,permalink,timestamp,like_count,comments_count",
    limit: 10,
  });

  const posts = (media.data || []).map((m) => ({
    id: m.id,
    type: m.media_product_type || m.media_type,
    timestamp: m.timestamp,
    likes: m.like_count ?? null,
    comments: m.comments_count ?? null,
    permalink: m.permalink,
    captionPreview: (m.caption || "").replace(/\s+/g, " ").slice(0, 90),
  }));

  console.log(
    JSON.stringify(
      {
        graphApiVersion: "v27.0",
        profile,
        recentPostsCount: posts.length,
        recentPosts: posts,
      },
      null,
      2
    )
  );
}

async function cmdComments(mediaId) {
  if (!mediaId) throw new Error("Informe o mediaId: node scripts/instagram-api.mjs comments <mediaId>");
  const data = await graphGet(`/${mediaId}/comments`, {
    fields: "id,username,text,timestamp,like_count",
    limit: 50,
  });
  console.log(JSON.stringify({ mediaId, count: (data.data || []).length, comments: data.data || [] }, null, 2));
}

async function cmdPostComment(mediaId, text, confirm) {
  if (!mediaId || !text) throw new Error('Uso: node scripts/instagram-api.mjs comment <mediaId> "texto" --confirm');
  if (!confirm) {
    console.log(JSON.stringify({ dryRun: true, action: "post_comment", mediaId, message: text, note: "Adicione --confirm para publicar." }, null, 2));
    return;
  }
  const res = await graphPost(`/${mediaId}/comments`, { message: text });
  console.log(JSON.stringify({ published: true, mediaId, commentId: res.id }, null, 2));
}

async function cmdInsights(mediaId) {
  if (!mediaId) throw new Error("Informe o mediaId: node scripts/instagram-api.mjs insights <mediaId>");
  const metrics = "reach,likes,comments,shares,saved";
  const data = await graphGet(`/${mediaId}/insights`, { metric: metrics });
  console.log(JSON.stringify({ mediaId, insights: data.data || [] }, null, 2));
}

async function waitContainerReady(containerId, maxTries = 20) {
  for (let i = 0; i < maxTries; i++) {
    const st = await graphGet(`/${containerId}`, { fields: "status_code,status" });
    if (st.status_code === "FINISHED" || st.status_code === "PUBLISHED") return st;
    if (st.status_code === "ERROR") throw new Error(`Container ${containerId} falhou: ${JSON.stringify(st)}`);
    await new Promise((r) => setTimeout(r, 3000));
  }
  throw new Error(`Timeout aguardando container ${containerId} ficar pronto.`);
}

async function cmdPublishPhoto(imageUrl, caption, confirm) {
  const igId = process.env.INSTAGRAM_USER_ID?.trim();
  if (!igId) throw new Error("INSTAGRAM_USER_ID ausente no .env.");
  if (!imageUrl) throw new Error('Uso: node scripts/instagram-api.mjs publish-photo <imageUrl> "legenda" --confirm');
  if (!confirm) {
    console.log(JSON.stringify({ dryRun: true, action: "publish-photo", igId, imageUrl, caption, note: "Adicione --confirm para publicar." }, null, 2));
    return;
  }
  const container = await graphPost(`/${igId}/media`, { image_url: imageUrl, caption: caption || "" });
  await waitContainerReady(container.id);
  const pub = await graphPost(`/${igId}/media_publish`, { creation_id: container.id });
  console.log(JSON.stringify({ published: true, mediaId: pub.id, containerId: container.id }, null, 2));
}

async function cmdPublishReel(videoUrl, caption, confirm) {
  const igId = process.env.INSTAGRAM_USER_ID?.trim();
  if (!igId) throw new Error("INSTAGRAM_USER_ID ausente no .env.");
  if (!videoUrl) throw new Error('Uso: node scripts/instagram-api.mjs publish-reel <videoUrl> "legenda" --confirm');
  if (!confirm) {
    console.log(JSON.stringify({ dryRun: true, action: "publish-reel", igId, videoUrl, caption, note: "Adicione --confirm para publicar." }, null, 2));
    return;
  }
  const container = await graphPost(`/${igId}/media`, {
    media_type: "REELS",
    video_url: videoUrl,
    caption: caption || "",
    share_to_feed: "true",
  });
  await waitContainerReady(container.id, 30);
  const pub = await graphPost(`/${igId}/media_publish`, { creation_id: container.id });
  console.log(JSON.stringify({ published: true, mediaId: pub.id, containerId: container.id }, null, 2));
}

async function cmdPublishCarousel(urlsCsv, caption, confirm) {
  const igId = process.env.INSTAGRAM_USER_ID?.trim();
  if (!igId) throw new Error("INSTAGRAM_USER_ID ausente no .env.");
  const urls = (urlsCsv || "").split(",").map((u) => u.trim()).filter(Boolean);
  if (urls.length < 2) throw new Error('Uso: node scripts/instagram-api.mjs publish-carousel <url1,url2,...> "legenda" --confirm');
  if (!confirm) {
    console.log(JSON.stringify({ dryRun: true, action: "publish-carousel", igId, items: urls.length, urls, caption, note: "Adicione --confirm para publicar." }, null, 2));
    return;
  }
  const childIds = [];
  for (const u of urls) {
    const isVideo = /\.(mp4|mov)(\?|$)/i.test(u);
    const payload = isVideo
      ? { media_type: "VIDEO", video_url: u, is_carousel_item: "true" }
      : { image_url: u, is_carousel_item: "true" };
    const item = await graphPost(`/${igId}/media`, payload);
    await waitContainerReady(item.id);
    childIds.push(item.id);
  }
  const carousel = await graphPost(`/${igId}/media`, {
    media_type: "CAROUSEL",
    children: childIds.join(","),
    caption: caption || "",
  });
  await waitContainerReady(carousel.id);
  const pub = await graphPost(`/${igId}/media_publish`, { creation_id: carousel.id });
  console.log(JSON.stringify({ published: true, mediaId: pub.id, carouselId: carousel.id, children: childIds }, null, 2));
}

async function main() {
  loadDotEnv();
  const args = process.argv.slice(2);
  const confirm = args.includes("--confirm");
  const pos = args.filter((a) => a !== "--confirm");
  const cmd = pos[0] || "status";

  switch (cmd) {
    case "setup":
    case "accounts":
      await cmdSetup();
      break;
    case "status":
    case "profile":
      await cmdStatus();
      break;
    case "permissions":
      await cmdPermissions();
      break;
    case "comments":
      await cmdComments(pos[1]);
      break;
    case "comment":
      await cmdPostComment(pos[1], pos[2], confirm);
      break;
    case "insights":
      await cmdInsights(pos[1]);
      break;
    case "publish-photo":
      await cmdPublishPhoto(pos[1], pos[2], confirm);
      break;
    case "publish-reel":
      await cmdPublishReel(pos[1], pos[2], confirm);
      break;
    case "publish-carousel":
      await cmdPublishCarousel(pos[1], pos[2], confirm);
      break;
    default:
      console.error(`Comando desconhecido: ${cmd}`);
      process.exit(1);
  }
}

main().catch((err) => {
  console.error("INSTAGRAM_API_ERR:", err.message);
  process.exit(1);
});
