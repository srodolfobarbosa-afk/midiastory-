import fs from "node:fs";
import crypto from "node:crypto";

const keyPath = ".secrets/play-service-account.json";
const packageName = "com.rdf112.midiastore";
const versionCode = process.argv[2] || "20";
const targetTrack = process.argv[3] || "production";
const releaseStatus = process.argv[4] || "completed";

const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

async function getAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/service.management",
      aud: key.token_uri,
      iat: now,
      exp: now + 3600,
    })
  ).toString("base64url");

  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const signature = sign.sign(key.private_key, "base64url");
  const jwt = `${header}.${payload}.${signature}`;

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
  const token = await getAccessToken();

  for (const svc of ["playdeveloperreporting.googleapis.com", "searchconsole.googleapis.com"]) {
    const enRes = await fetch(
      `https://serviceusage.googleapis.com/v1/projects/${key.project_id}/services/${svc}:enable`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: "{}",
      }
    );
    console.log(`Enable ${svc}:`, enRes.status, await enRes.text());
  }

  const editRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: "{}",
    }
  );
  const edit = await editRes.json();
  if (!editRes.ok) throw new Error(JSON.stringify(edit));

  const trackRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/tracks/${targetTrack}`,
    {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        track: targetTrack,
        releases: [
          {
            name: `2.5.0 (${versionCode})`,
            versionCodes: [String(versionCode)],
            status: releaseStatus,
            releaseNotes: [
              { language: "pt-BR", text: "v2.5.0: Visual redesenhado em 7 abas organizadas (Início limpo, Limpeza, Arquivos, Estúdio, Jogos Offline, Mundo e Planos Pro), cabeçalho refinado, Modo Fácil (Aa+), Tema VIP Preto & Ouro 24K, Caça-Palavras Pro 9x9, Cobrinha, Blocos, Hub Mundial offline, Notas/Calculadora/QR Pro e 14 idiomas!" },
              { language: "en-US", text: "v2.5.0: Clean 7-tab separation (Home, Cleanup, Files, Media Studio, Offline Games, World Hub & Pro), polished header, Senior Easy Mode (Aa+), Royal Black & 24K Gold VIP theme, Pro Word Search 9x9, Retro Snake, Falling Blocks, offline World Clock & Currency, Notes/Calc/QR Pro & 14 languages!" },
              { language: "es-ES", text: "v2.5.0: Diseño limpio en 7 pestañas separadas (Inicio, Limpieza, Archivos, Estudio, Juegos Offline, Mundo y Pro), Modo Fácil (Aa+), Tema VIP Oro 24K, Sopa de Letras Pro, Serpiente, Bloques, Reloj Mundial y Conversor offline, Notas/Calculadora/QR Pro en 14 idiomas." },
              { language: "fr-FR", text: "v2.5.0 : Interface épurée en 7 onglets dédiés (Accueil, Nettoyage, Fichiers, Studio, Jeux hors-ligne, Monde et Pro), Mode Facile (Aa+), Thème VIP Or 24K, Mots Mêlés Pro, Serpent, Blocs, Horloge Mondiale et Devises hors-ligne en 14 langues." },
              { language: "de-DE", text: "v2.5.0: Saubere 7-Tab-Struktur (Start, Bereinigung, Dateien, Studio, Offline-Spiele, Welt & Pro), Senioren-Modus (Aa+), VIP Schwarz-Gold-Design, Wortsuche Pro, Snake, Blöcke, Offline-Weltzeituhr & Währungsrechner in 14 Sprachen." },
              { language: "hi-IN", text: "v2.5.0: 7 साफ अलग-अलग टैब (होम, सफाई, फ़ाइलें, स्टूडियो, ऑफ़लाइन गेम्स, दुनिया और Pro), बड़ा फ़ॉन्ट मोड (Aa+), VIP ब्लैक-गोल्ड थीम, शब्द खोज Pro 9x9, स्नेक, ब्लॉक्स, विश्व घड़ी व मुद्रा परिवर्तक 14 भाषाओं में!" },
              { language: "ja-JP", text: "v2.5.0：機能別に整理された7タブUI（ホーム・整理・ファイル・スタジオ・オフラインゲーム・世界・Pro）、大文字モード(Aa+)、ゴールドVIPテーマ、単語探しPro・スネーク・ブロック、世界時計＆通貨換算を14言語で搭載！" },
              { language: "zh-CN", text: "v2.5.0：全新清晰的7大独立标签页（首页、清理、文件、媒体工作室、离线游戏、世界工具、Pro）、长辈大字模式(Aa+)、黑金VIP主题、寻字Pro/贪吃蛇/方块、离线世界时钟与汇率换算，支持14种语言！" }
            ],
          },
        ],
      }),
    }
  );
  console.log(`PUT track ${targetTrack}:`, trackRes.status, await trackRes.text());

  const commitRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  console.log(`COMMIT ${targetTrack}:`, commitRes.status, await commitRes.text());
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
