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
            name: `${versionCode} (2.3.1)`,
            versionCodes: [String(versionCode)],
            status: releaseStatus,
            releaseNotes: [
              {
                language: "pt-BR",
                text: "Novo painel Big Tech 2027, medidor de armazenamento real, ações rápidas de limpeza e suporte a 8 idiomas.",
              },
              {
                language: "en-US",
                text: "New 2027 dashboard, real-time storage gauge, quick cleanup actions, and 8-language support.",
              },
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
