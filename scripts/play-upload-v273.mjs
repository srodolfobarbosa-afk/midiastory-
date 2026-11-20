#!/usr/bin/env node
/**
 * play-upload-v273.mjs
 * Faz upload do AAB Release final (v2.7.3 / vc 28) e publica nas faixas
 * 'internal' e 'alpha' (Teste Fechado) SEM alterar listas de testadores.
 */
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const aabPath = path.join(projectRoot, "release-artifacts", "v273-release-final", "app-release.aab");
const packageName = "com.rdf112.midiastore";

const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

const RELEASE_NOTES = [
  { language: "pt-BR", text: "v2.7.3: Novo visual Limpador Puro em Preto OLED, anel de armazenamento em tempo real, 3 abas limpas (Limpar, Arquivos, Pro), scanner local de fotos borradas, 14 idiomas completos, novo ícone OLED e revisão segura em 1 toque." },
  { language: "en-US", text: "v2.7.3: New Pure Cleaner OLED Dark UI, live storage ring gauge, clean 3-tab navigation (Clean, Files, Pro), on-device blurry photo scanner, 14 full languages, refined OLED app icon, and safe 1-tap review." },
  { language: "es-ES", text: "v2.7.3: Nuevo diseño Limpiador Puro OLED, indicador circular de almacenamiento, 3 pestañas limpias (Limpiar, Archivos, Pro), detector local de fotos borrosas, 14 idiomas completos y revisión segura en 1 toque." },
  { language: "fr-FR", text: "v2.7.3 : Nouveau design Pure Cleaner OLED, jauge circulaire de stockage, 3 onglets épurés (Nettoyer, Fichiers, Pro), détection locale des photos floues, 14 langues complètes et révision sécurisée." },
  { language: "de-DE", text: "v2.7.3: Neues Pure-Cleaner-OLED-Design, Live-Speicherring, 3 klare Tabs (Bereinigen, Dateien, Pro), lokale Erkennung unscharfer Fotos, 14 vollständige Sprachen und sichere Prüfung." },
  { language: "hi-IN", text: "v2.7.3: नया प्योर क्लीनर OLED डार्क डिज़ाइन, लाइव स्टोरेज रिंग, 3 स्वच्छ टैब, ऑन-डिवाइस धुंधली फ़ोटो स्कैनर और 14 पूर्ण भाषाएँ।" },
  { language: "ja-JP", text: "v2.7.3：新しいPure Cleaner OLEDダークUI、ストレージリング表示、3つのシンプルなタブ、端末内ピンボケ写真スキャナー、14言語完全対応。" },
  { language: "zh-CN", text: "v2.7.3：全新纯净清理OLED深色界面、实时存储环形图、3个简洁标签页、本地模糊照片检测及14种完整语言支持。" },
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
              name: `2.7.3 (${vc})`,
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
    console.log(`Track '${track}' atualizado para 2.7.3 (${vc}) [HTTP ${trackRes.status}]`);
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
