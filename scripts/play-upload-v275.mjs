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
const aabPath = path.join(projectRoot, "release-artifacts", "v275-release", "app-release.aab");
const packageName = "com.rdf112.midiastore";

const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

const RELEASE_NOTES = [
  { language: "pt-BR", text: "v2.7.5: Limpeza real em 2 cliques! Novo Bottom Sheet OLED com miniaturas reais e exclusão definitiva direta, recuperação automática contra session_stale em aparelhos cheios, correção total de permissão parcial no Android 14/15 e Home mais limpa sem botões duplicados." },
  { language: "en-US", text: "v2.7.5: Real 2-click cleanup! New OLED review Bottom Sheet with real media thumbnails and direct permanent deletion, automatic session_stale recovery on large libraries, full Android 14/15 partial-permission fix, and cleaner Home UI." },
  { language: "es-ES", text: "v2.7.5: ¡Limpieza real en 2 clics! Nuevo panel OLED con miniaturas reales y eliminación definitiva directa, recuperación automática de sesión, corrección total de permisos en Android 14/15 y pantalla principal más limpia." },
  { language: "fr-FR", text: "v2.7.5 : Nettoyage réel en 2 clics ! Nouveau panneau OLED avec miniatures réelles et suppression définitive directe, reprise automatique de session, correction des autorisations Android 14/15 et accueil épuré." },
  { language: "de-DE", text: "v2.7.5: Echte 2-Klick-Bereinigung! Neues OLED-Prüfpanel mit echten Vorschauen und direktem dauerhaftem Löschen, automatische Sitzungswiederherstellung, Android-14/15-Berechtigungsfix und aufgeräumte Startseite." },
  { language: "hi-IN", text: "v2.7.5: 2 क्लिक में असली क्लीनअप! असली थंबनेल और सीधे स्थायी डिलीट के साथ नया OLED पैनल, ऑटो सेशन रिकवरी, Android 14/15 अनुमति सुधार और साफ होम स्क्रीन।" },
  { language: "ja-JP", text: "v2.7.5：2クリックで確実に整理！実サムネイル付きの新しいOLEDレビューパネル、セッション自動復旧、Android 14/15の部分権限修正、より見やすいホーム画面に刷新。" },
  { language: "zh-CN", text: "v2.7.5：2步完成真实清理！全新OLED审核面板支持真实缩略图与直接永久删除，自动恢复扫描会话，全面修复 Android 14/15 权限问题并优化首页界面。" },
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
              name: "2.7.5 (30)",
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
