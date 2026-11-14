import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const packageName = process.argv[2] || "com.rdf112.midiastore";
const action = process.argv[3] || "status";
const arg4 = process.argv[4] || "";
const arg5 = process.argv[5] || "internal";

const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

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
  const signature = sign.sign(key.private_key, "base64url");
  const jwt = `${header}.${payload}.${signature}`;

  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!data.access_token) {
    throw new Error(`OAuth token error: ${JSON.stringify(data)}`);
  }
  return data.access_token;
}

async function main() {
  const token = await getAccessToken();
  console.log("OAuth2 Access Token obtido com sucesso para:", key.client_email);

  if (action === "status") {
    const editRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: "{}",
      }
    );

    const editText = await editRes.text();
    console.log("HTTP Status (edits.insert):", editRes.status);
    if (!editRes.ok) {
      console.log("Resposta da API:", editText);
      return;
    }

    const edit = JSON.parse(editText);
    console.log("Edit ID criado:", edit.id);

    const tracksRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/tracks`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log("Tracks:", await tracksRes.text());

    const bundlesRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/bundles`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log("Bundles:", await bundlesRes.text());

    const detailsRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/details`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log("Details:", await detailsRes.text());

    const listingsRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log("Listings:", await listingsRes.text());

    await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}`,
      { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
    );

    const inappRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/inappproducts`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log("InAppProducts:", inappRes.status, await inappRes.text());

    const subsRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/subscriptions`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log("Subscriptions:", subsRes.status, await subsRes.text());
  } else if (action === "promote") {
    for (const targetTrack of ["alpha", "production"]) {
      const editRes = await fetch(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
          body: "{}",
        }
      );
      const edit = await editRes.json();
      await fetch(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/tracks/${targetTrack}`,
        {
          method: "PUT",
          headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
          body: JSON.stringify({
            track: targetTrack,
            releases: [{ name: "20 (2.3.1)", versionCodes: ["20"], status: "completed" }],
          }),
        }
      );
      const commitRes = await fetch(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
        { method: "POST", headers: { Authorization: `Bearer ${token}` } }
      );
      console.log(`Commit ${targetTrack} (completed):`, commitRes.status, await commitRes.text());
    }
  } else if (action === "upload") {
    const aabPath = arg4 || "app/build/outputs/bundle/release/app-release.aab";
    const track = arg5 || "internal";
    if (!fs.existsSync(aabPath)) {
      throw new Error(`Arquivo AAB nao encontrado: ${aabPath}`);
    }
    const aabBuffer = fs.readFileSync(aabPath);
    console.log(`Iniciando upload de ${aabPath} (${aabBuffer.length} bytes) para a faixa '${track}'...`);

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
    if (!uploadRes.ok) throw new Error(JSON.stringify(bundle));
    console.log("Bundle enviado com sucesso:", bundle);

    const releaseNotes = [
      { language: "pt-BR", text: "v2.5.0: Visual redesenhado em 7 abas organizadas (Início limpo, Limpeza, Arquivos, Estúdio, Jogos Offline, Mundo e Planos Pro), cabeçalho refinado, Modo Fácil (Aa+), Tema VIP Preto & Ouro 24K, Caça-Palavras Pro 9x9, Cobrinha, Blocos, Hub Mundial offline, Notas/Calculadora/QR Pro e 14 idiomas!" },
      { language: "en-US", text: "v2.5.0: Clean 7-tab separation (Home, Cleanup, Files, Media Studio, Offline Games, World Hub & Pro), polished header, Senior Easy Mode (Aa+), Royal Black & 24K Gold VIP theme, Pro Word Search 9x9, Retro Snake, Falling Blocks, offline World Clock & Currency, Notes/Calc/QR Pro & 14 languages!" },
      { language: "es-ES", text: "v2.5.0: Diseño limpio en 7 pestañas separadas (Inicio, Limpieza, Archivos, Estudio, Juegos Offline, Mundo y Pro), Modo Fácil (Aa+), Tema VIP Oro 24K, Sopa de Letras Pro, Serpiente, Bloques, Reloj Mundial y Conversor offline, Notas/Calculadora/QR Pro en 14 idiomas." },
      { language: "fr-FR", text: "v2.5.0 : Interface épurée en 7 onglets dédiés (Accueil, Nettoyage, Fichiers, Studio, Jeux hors-ligne, Monde et Pro), Mode Facile (Aa+), Thème VIP Or 24K, Mots Mêlés Pro, Serpent, Blocs, Horloge Mondiale et Devises hors-ligne en 14 langues." },
      { language: "de-DE", text: "v2.5.0: Saubere 7-Tab-Struktur (Start, Bereinigung, Dateien, Studio, Offline-Spiele, Welt & Pro), Senioren-Modus (Aa+), VIP Schwarz-Gold-Design, Wortsuche Pro, Snake, Blöcke, Offline-Weltzeituhr & Währungsrechner in 14 Sprachen." },
      { language: "hi-IN", text: "v2.5.0: 7 साफ अलग-अलग टैब (होम, सफाई, फ़ाइलें, स्टूडियो, ऑफ़लाइन गेम्स, दुनिया और Pro), बड़ा फ़ॉन्ट मोड (Aa+), VIP ब्लैक-गोल्ड थीम, शब्द खोज Pro 9x9, स्नेक, ब्लॉक्स, विश्व घड़ी व मुद्रा परिवर्तक 14 भाषाओं में!" },
      { language: "ja-JP", text: "v2.5.0：機能別に整理された7タブUI（ホーム・整理・ファイル・スタジオ・オフラインゲーム・世界・Pro）、大文字モード(Aa+)、ゴールドVIPテーマ、単語探しPro・スネーク・ブロック、世界時計＆通貨換算を14言語で搭載！" },
      { language: "zh-CN", text: "v2.5.0：全新清晰的7大独立标签页（首页、清理、文件、媒体工作室、离线游戏、世界工具、Pro）、长辈大字模式(Aa+)、黑金VIP主题、寻字Pro/贪吃蛇/方块、离线世界时钟与汇率换算，支持14种语言！" }
    ];

    const targetTracks = track === "all" ? ["internal", "alpha"] : [track];
    for (const t of targetTracks) {
      const trackRes = await fetch(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/tracks/${t}`,
        {
          method: "PUT",
          headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
          body: JSON.stringify({
            track: t,
            releases: [
              {
                name: `2.5.0 (${bundle.versionCode})`,
                versionCodes: [String(bundle.versionCode)],
                status: "completed",
                releaseNotes,
              },
            ],
          }),
        }
      );
      const trackResult = await trackRes.json();
      if (!trackRes.ok) throw new Error(JSON.stringify(trackResult));
      console.log(`Track ${t} atualizada:`, trackResult);
    }

    const commitRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      }
    );
    const commitResult = await commitRes.json();
    if (!commitRes.ok) throw new Error(JSON.stringify(commitResult));
    console.log("PUBLICACAO CONCLUIDA COM SUCESSO:", commitResult);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
