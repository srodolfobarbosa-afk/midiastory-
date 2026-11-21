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
  : path.join(projectRoot, "release-artifacts", "v276-release", "app-release.aab");
const packageName = "com.rdf112.midiastore";

const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

const RELEASE_NOTES = [
  { language: "pt-BR", text: "v2.7.6: Limpeza rápida em 1 botão (máximo 2 cliques) com pré-análise automática ao abrir, fim do falso alerta 'Nenhuma mídia encontrada' sob cobertura parcial e novo painel OLED de benefícios Pro." },
  { language: "en-US", text: "v2.7.6: Fast 1-button cleanup (max 2 taps) with automatic pre-scan on launch, fix for false 'No media found' under partial coverage, and new OLED Pro benefits card." },
  { language: "es-ES", text: "v2.7.6: Limpieza rápida en 1 botón (máximo 2 toques) con preanálisis automático al abrir, corrección de 'No se encontraron medios' en cobertura parcial y nuevo panel OLED Pro." },
  { language: "fr-FR", text: "v2.7.6 : Nettoyage rapide en 1 bouton (2 gestes max) avec pré-analyse automatique, correction de l'alerte 'Aucun média trouvé' et nouveau panneau OLED Pro." },
  { language: "de-DE", text: "v2.7.6: Schnelle 1-Knopf-Bereinigung (max. 2 Tipps) mit automatischer Voranalyse beim Start, Behebung von 'Keine Medien gefunden' und neue OLED-Pro-Übersicht." },
  { language: "hi-IN", text: "v2.7.6: ऐप खोलते ही ऑटो प्री-स्कैन के साथ 1-बटन तेज़ क्लीनअप (अधिकतम 2 टैप), आंशिक एक्सेस में 'कोई मीडिया नहीं मिला' सुधार और नया OLED Pro कार्ड।" },
  { language: "ja-JP", text: "v2.7.6：起動時の自動プレスキヤンによる1ボタン高速整理（最大2タップ）、部分権限時の「メディアが見つかりません」修正、新しいOLED Proカード。" },
  { language: "zh-CN", text: "v2.7.6：启动自动预扫描与一键快速清理（最多2步）、修复部分权限下“未找到媒体”提示，以及全新 OLED Pro 权益卡片。" },
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
              name: `2.7.6 (${bundle.versionCode})`,
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
