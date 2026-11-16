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
            name: `2.6.0 (${versionCode})`,
            versionCodes: [String(versionCode)],
            status: releaseStatus,
            releaseNotes: [
              { language: "pt-BR", text: "v2.6.0: Padrão Apple Dark OLED, Escudo de Proteção Familiar com blindagem de fotos favoritas, grade rápida com toque suave sem poluição visual, anel de armazenamento ultra nítido e foco total em segurança e tranquilidade para toda a família." },
              { language: "en-US", text: "v2.6.0: Apple Dark OLED design, Family Shield with favorite photos protection, polished non-cluttered quick-clean grid, ultra-sharp storage ring, and peace-of-mind cleanup for the whole family." },
              { language: "es-ES", text: "v2.6.0: Diseño Apple Dark OLED, Escudo de Protección Familiar con resguardo de favoritos, cuadrícula de limpieza sin saturación, medidor de almacenamiento nítido y tranquilidad total para la familia." },
              { language: "fr-FR", text: "v2.6.0 : Design Apple Dark OLED, Bouclier Familial protégeant les photos favorites, grille de nettoyage rapide épurée, jauge de stockage nette et sérénité absolue pour toute la famille." },
              { language: "de-DE", text: "v2.6.0: Apple Dark OLED Design, Familienschutz-Schild zum Schutz von Favoriten, aufgeräumtes Schnellreinigungs-Raster, hochpräziser Speicherring und sichere Bereinigung für die ganze Familie." },
              { language: "hi-IN", text: "v2.6.0: Apple Dark OLED डिज़ाइन, पसंदीदा फ़ोटो सुरक्षा के लिए पारिवारिक शील्ड, स्वच्छ और सरल त्वरित सफाई ग्रिड, स्पष्ट स्टोरेज रिंग और पूरे परिवार के लिए安心!" },
              { language: "ja-JP", text: "v2.6.0：Apple基準のDark OLEDデザイン、お気に入り写真を保護するファミリーシールド、洗練されたクイック整理グリッド、見やすいストレージリングを搭載！" },
              { language: "zh-CN", text: "v2.6.0：全新Apple级纯黑OLED设计、保护家人与收藏照片的安全盾牌、极简触控快捷清理网格与超高清存储环，让全家放心使用！" }
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
