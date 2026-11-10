import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
function loadFirebaseKey() {
  if (process.env.FIREBASE_ADMIN_KEY_JSON) {
    try { return JSON.parse(process.env.FIREBASE_ADMIN_KEY_JSON); } catch (e) {}
  }
  const localSecret = path.join(projectRoot, ".secrets", "firebase-admin.json");
  if (fs.existsSync(localSecret)) {
    return JSON.parse(fs.readFileSync(localSecret, "utf8"));
  }
  const downloadsPath = "C:\\Users\\rodolfo\\Downloads\\midiastory-df6a0-firebase-adminsdk-fbsvc-d0543767cb.json";
  if (fs.existsSync(downloadsPath)) {
    return JSON.parse(fs.readFileSync(downloadsPath, "utf8"));
  }
  throw new Error("Credencial do Firebase Admin não encontrada.");
}

const key = loadFirebaseKey();
const siteId = "midiastory-df6a0";

async function getAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/firebase",
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
  console.log("Firebase OAuth2 Token obtido para:", key.client_email);

  for (const svc of ["searchconsole.googleapis.com", "playdeveloperreporting.googleapis.com"]) {
    const enRes = await fetch(
      `https://serviceusage.googleapis.com/v1/projects/${key.project_id}/services/${svc}:enable`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: "{}",
      }
    );
    console.log(`Enable (${key.project_id}) ${svc}:`, enRes.status);
  }

  const fileMappings = [
    ["/index.html", "index.html"],
    ["/en/index.html", "en/index.html"],
    ["/es/index.html", "es/index.html"],
    ["/de/index.html", "de/index.html"],
    ["/fr/index.html", "fr/index.html"],
    ["/hi/index.html", "hi/index.html"],
    ["/ja/index.html", "ja/index.html"],
    ["/zh/index.html", "zh/index.html"],
    ["/artigos/como-liberar-espaco-whatsapp.html", "artigos/como-liberar-espaco-whatsapp.html"],
    ["/robots.txt", "robots.txt"],
    ["/sitemap.xml", "sitemap.xml"],
    ["/llms.txt", "llms.txt"],
    ["/icon-512.png", "icon-512.png"],
  ];

  const filesToDeploy = {};
  for (const [urlPath, relPath] of fileMappings) {
    const fullPath = path.join(projectRoot, "docs", relPath);
    if (fs.existsSync(fullPath)) {
      filesToDeploy[urlPath] = fs.readFileSync(fullPath);
    }
  }

  const gzippedByPath = {};
  const hashesByPath = {};
  for (const [urlPath, content] of Object.entries(filesToDeploy)) {
    const gz = zlib.gzipSync(content);
    const sha256 = crypto.createHash("sha256").update(gz).digest("hex");
    gzippedByPath[sha256] = gz;
    hashesByPath[urlPath] = sha256;
  }

  // 1. Create new Hosting version
  const verRes = await fetch(`https://firebasehosting.googleapis.com/v1beta1/sites/${siteId}/versions`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      config: {
        cleanUrls: true,
      },
    }),
  });
  const verData = await verRes.json();
  console.log("Create version:", verRes.status, JSON.stringify(verData));
  if (!verRes.ok) return;

  const versionName = verData.name;

  // 2. Populate files
  const popRes = await fetch(`https://firebasehosting.googleapis.com/v1beta1/${versionName}:populateFiles`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ files: hashesByPath }),
  });
  const popData = await popRes.json();
  console.log("Populate files:", popRes.status, JSON.stringify(popData));
  if (!popRes.ok) return;

  // 3. Upload required hashes
  const uploadUrl = popData.uploadUrl;
  const required = popData.uploadRequiredHashes || [];
  for (const hash of required) {
    const upRes = await fetch(`${uploadUrl}/${hash}`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/octet-stream",
      },
      body: gzippedByPath[hash],
    });
    console.log(`Upload ${hash}:`, upRes.status);
  }

  // 4. Finalize version
  const finRes = await fetch(`https://firebasehosting.googleapis.com/v1beta1/${versionName}?update_mask=status`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ status: "FINALIZED" }),
  });
  console.log("Finalize version:", finRes.status, await finRes.text());

  // 5. Release version
  const relRes = await fetch(
    `https://firebasehosting.googleapis.com/v1beta1/sites/${siteId}/releases?versionName=${encodeURIComponent(versionName)}`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: "{}",
    }
  );
  console.log("Release site:", relRes.status, await relRes.text());
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
