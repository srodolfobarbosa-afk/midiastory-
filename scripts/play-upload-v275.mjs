#!/usr/bin/env node
/**
 * play-upload-v275.mjs
 * Faz upload do AAB Release v2.7.5 (vc 30) auditado via bundletool e publica nas faixas
 * 'internal' e 'alpha' (Teste Fechado) SEM alterar listas de testadores.
 */
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const aabPath = process.argv[2]
  ? path.resolve(projectRoot, process.argv[2])
  : path.join(projectRoot, "release-artifacts", "v280-release", "app-release.aab");
const packageName = "com.rdf112.midiastore";

const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

const RELEASE_NOTES = [
  { language: "pt-BR", text: "v2.8.0: Nova Assistente Pessoal Maria com orientação por voz e sincronia labial, Chat Midiastore IA no padrão moderno, limpeza global de mensageiros e cálculo ao vivo em MB/GB em todas as abas." },
  { language: "en-US", text: "v2.8.0: New Maria Personal Assistant with voice guidance and lip-sync, modern Chat Midiastore AI modal, global messenger media cleanup, and live MB/GB sizes across all tabs." },
  { language: "es-ES", text: "v2.8.0: Nueva Asistente Personal Maria con guía por voz, Chat Midiastore IA moderno, limpieza global de mensajería y tamaños en vivo en MB/GB en todas las pestañas." },
  { language: "fr-FR", text: "v2.8.0 : Nouvelle Assistante Personnelle Maria, Chat Midiastore IA moderne, nettoyage des messageries et tailles MB/GB en direct sur tous les onglets." },
  { language: "de-DE", text: "v2.8.0: Neue persönliche Assistentin Maria, moderner Chat Midiastore KI-Dialog, Messenger-Bereinigung und Live-MB/GB-Größen in allen Tabs." },
  { language: "hi-IN", text: "v2.8.0: नई मारिया पर्सनल असिस्टेंट, आधुनिक Chat Midiastore AI, चैट मीडिया क्लीनअप और सभी टैब में लाइव MB/GB आकार।" },
  { language: "ja-JP", text: "v2.8.0：新しいパーソナルアシスタントMaria、モダンなChat Midiastore AI、SNS・チャット整理、全タブでのリアルタイムMB/GB表示。" },
  { language: "zh-CN", text: "v2.8.0：全新 Maria 智能语音助手、现代风格 Chat Midiastore AI 对话框、聊天与下载缓存清理及全标签页实时 MB/GB 大小显示。" },
];

async function getAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: key.token_uri,
      iat: now,
      exp: now + 3600,
    }),
  ).toString("base64url");

  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const jwt = `${header}.${payload}.${sign.sign(key.private_key, "base64url")}`;

  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!data.access_token) throw new Error(JSON.stringify(data));
  return data.access_token;
}

async function main() {
  if (!fs.existsSync(aabPath)) {
    throw new Error(`AAB nao encontrado: ${aabPath}`);
  }
  const aabBuffer = fs.readFileSync(aabPath);
  console.log(`AAB carregado: ${aabPath} (${aabBuffer.length} bytes)`);

  const token = await getAccessToken();

  const editRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: "{}",
    },
  );
  const edit = await editRes.json();
  if (!editRes.ok || !edit.id) throw new Error(`Create edit failed: ${JSON.stringify(edit)}`);
  console.log(`Edit criado: ${edit.id}`);

  const uploadRes = await fetch(
    `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/bundles?uploadType=media`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/octet-stream",
      },
      body: aabBuffer,
    },
  );
  const bundle = await uploadRes.json();
  if (!uploadRes.ok || !bundle.versionCode) {
    throw new Error(`Upload bundle failed (${uploadRes.status}): ${JSON.stringify(bundle)}`);
  }
  console.log(`Bundle enviado: versionCode=${bundle.versionCode}, sha256=${bundle.sha256}`);

  for (const track of ["internal", "alpha"]) {
    const trackRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/tracks/${track}`,
      {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          track,
          releases: [
            {
              name: `2.8.0 (${bundle.versionCode})`,
              versionCodes: [String(bundle.versionCode)],
              status: "completed",
              releaseNotes: RELEASE_NOTES,
            },
          ],
        }),
      },
    );
    const trackData = await trackRes.json();
    if (!trackRes.ok) {
      throw new Error(`Track ${track} update failed (${trackRes.status}): ${JSON.stringify(trackData)}`);
    }
    console.log(`Track '${track}' atualizada: versionCode=${bundle.versionCode}`);
  }

  const commitRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    },
  );
  const commitText = await commitRes.text();
  if (!commitRes.ok) {
    throw new Error(`Commit edit failed (${commitRes.status}): ${commitText}`);
  }
  console.log(`✅ PUBLICADO COM SUCESSO! Edit ${edit.id} commitado HTTP ${commitRes.status}`);
}

main().catch((err) => {
  console.error("❌ Erro no upload Play Console v2.7.5:", err.message);
  process.exit(1);
});
