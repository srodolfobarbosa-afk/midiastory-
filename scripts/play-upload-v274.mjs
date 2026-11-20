#!/usr/bin/env node
/**
 * play-upload-v274.mjs
 * Faz upload do AAB Release v2.7.4 (vc 29) auditado via bundletool e publica nas faixas
 * 'internal' e 'alpha' (Teste Fechado) SEM alterar listas de testadores.
 */
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const aabPath = path.join(projectRoot, "release-artifacts", "v274-release", "app-release.aab");
const packageName = "com.rdf112.midiastore";

const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

const RELEASE_NOTES = [
  { language: "pt-BR", text: "v2.7.4: Estabilidade total na análise e limpeza! Recuperação automática de sessão (Duplicatas/Vídeos), acesso direto às permissões de mídia no Android 14/15, carregamento automático da aba Arquivos e feedback instantâneo ao tocar em Analisar meu espaço." },
  { language: "en-US", text: "v2.7.4: Rock-solid scanning and cleanup reliability! Automatic session recovery (Duplicates/Videos), direct Android 14/15 media permission management, automatic Files tab loading, and instant feedback when tapping Analyze my storage." },
  { language: "es-ES", text: "v2.7.4: Máxima estabilidad al analizar y limpiar. Recuperación automática de sesión, acceso directo a permisos de medios en Android 14/15, carga automática de Archivos y respuesta instantánea." },
  { language: "fr-FR", text: "v2.7.4 : Stabilité renforcée pour l'analyse et le nettoyage, récupération automatique de session, gestion directe des autorisations Android 14/15 et chargement automatique de l'onglet Fichiers." },
  { language: "de-DE", text: "v2.7.4: Maximale Stabilität beim Scannen und Bereinigen, automatische Sitzungswiederherstellung, direkter Zugriff auf Android-14/15-Medienberechtigungen und automatisches Laden im Dateien-Tab." },
  { language: "hi-IN", text: "v2.7.4: स्कैनिंग और क्लीनअप में पूर्ण स्थिरता, ऑटोमैटिक सेशन रिकवरी, Android 14/15 मीडिया अनुमति का सीधा विकल्प और फ़ाइलें टैब का ऑटो-लोड।" },
  { language: "ja-JP", text: "v2.7.4：スキャンと整理の安定性を大幅に向上。セッション自動復旧、Android 14/15のメディア権限設定への直接アクセス、ファイルタブの自動読み込みに対応。" },
  { language: "zh-CN", text: "v2.7.4：全面提升扫描与清理稳定性，支持会话自动恢复、Android 14/15 媒体权限快捷管理以及文件标签页自动加载。" },
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
    })
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
    }
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
    }
  );
  const bundle = await uploadRes.json();
  if (!uploadRes.ok) throw new Error(`Bundle upload failed (${uploadRes.status}): ${JSON.stringify(bundle)}`);
  console.log(`Bundle enviado com sucesso: versionCode=${bundle.versionCode}, sha256=${bundle.sha256}`);

  const vc = String(bundle.versionCode);
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
              name: `2.7.4 (${vc})`,
              versionCodes: [vc],
              status: "completed",
              releaseNotes: RELEASE_NOTES,
            },
          ],
        }),
      }
    );
    const trackText = await trackRes.text();
    if (!trackRes.ok) throw new Error(`Track ${track} failed (${trackRes.status}): ${trackText}`);
    console.log(`Track '${track}' atualizado para 2.7.4 (${vc}) [HTTP ${trackRes.status}]`);
  }

  const commitRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  const commitText = await commitRes.text();
  if (!commitRes.ok) throw new Error(`Commit failed (${commitRes.status}): ${commitText}`);
  console.log(`COMMIT PLAY CONSOLE OK [HTTP ${commitRes.status}]: ${commitText}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
