import fs from "node:fs";
import crypto from "node:crypto";

const keyPath = ".secrets/play-service-account.json";
const packageName = "com.rdf112.midiastore";
const versionCode = process.argv[2] || "27";
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
            name: `2.7.2 (${versionCode})`,
            versionCodes: [String(versionCode)],
            status: releaseStatus,
            releaseNotes: [
              { language: "pt-BR", text: "v2.7.2: Novo visual Limpador Puro em Preto OLED, ícones vetoriais finos, abas limpas (Limpar, Arquivos, Pro), limpeza completa grátis (WhatsApp, Duplicatas, Grandes e Antigos) e 3 planos Pro." },
              { language: "en-US", text: "v2.7.2: New Pure Cleaner OLED Dark UI, crisp vector icons, clean 3-tab navigation (Clean, Files, Pro), full free cleanup (WhatsApp, Duplicates, Large & Old), and 3 Pro plans." },
              { language: "es-ES", text: "v2.7.2: Nuevo diseño Limpiador Puro OLED, íconos vectoriales nítidos, 3 pestañas limpias (Limpiar, Archivos, Pro), limpieza completa gratis y 3 planes Pro." },
              { language: "fr-FR", text: "v2.7.2 : Nouveau design Pure Cleaner OLED, icônes vectorielles épurées, 3 onglets clairs (Nettoyer, Fichiers, Pro), nettoyage complet gratuit et 3 offres Pro." },
              { language: "de-DE", text: "v2.7.2: Neues Pure-Cleaner-OLED-Design, klare Vektor-Icons, 3 übersichtliche Tabs (Bereinigen, Dateien, Pro), kostenlose Komplettreinigung und 3 Pro-Tarife." },
              { language: "hi-IN", text: "v2.7.2: नया प्योर क्लीनर OLED डार्क डिज़ाइन, शार्प वेक्टर आइकन, 3 स्वच्छ टैब, पूरी तरह मुफ़्त क्लीनअप और 3 Pro प्लान।" },
              { language: "ja-JP", text: "v2.7.2：洗練されたPure Cleaner OLEDデザイン、クリアなベクターアイコン、3つのシンプルなタブ、無料クリーンアップ機能と3つのProプラン。" },
              { language: "zh-CN", text: "v2.7.2：全新纯净清理OLED深色界面、精致矢量图标、3个简洁标签页、完整免费清理功能及3个Pro方案。" }
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
