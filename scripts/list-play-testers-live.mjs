import fs from "node:fs";
import crypto from "node:crypto";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const packageName = "com.rdf112.midiastore";

function resolveSecretFile(relPath) {
  const localPath = path.join(projectRoot, relPath);
  if (fs.existsSync(localPath)) return localPath;
  try {
    const gitCommonDir = execFileSync("git", ["rev-parse", "--git-common-dir"], {
      cwd: projectRoot,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
    if (gitCommonDir) {
      const mainRepoRoot = path.resolve(projectRoot, gitCommonDir, "..");
      const mainPath = path.join(mainRepoRoot, relPath);
      if (fs.existsSync(mainPath)) return mainPath;
    }
  } catch {
    // ignore
  }
  return localPath;
}

const keyPath = resolveSecretFile(path.join(".secrets", "play-service-account.json"));
if (!fs.existsSync(keyPath)) {
  throw new Error(`Credencial Google Play não encontrada em .secrets/play-service-account.json`);
}
const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

async function getToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = Buffer.from(JSON.stringify({ alg: "RS256", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(JSON.stringify({
    iss: key.client_email,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: key.token_uri,
    iat: now,
    exp: now + 3600,
  })).toString("base64url");
  const sign = crypto.createSign("RSA-SHA256");
  sign.update(`${header}.${payload}`);
  const jwt = `${header}.${payload}.${sign.sign(key.private_key, "base64url")}`;
  const res = await fetch(key.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
  });
  const data = await res.json();
  if (!data.access_token) {
    throw new Error(`Falha ao obter token OAuth2 Google Play (HTTP ${res.status})`);
  }
  return data.access_token;
}

async function run() {
  const token = await getToken();
  console.log("Token Google Play Developer obtido.");

  const editRes = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
  });
  const edit = await editRes.json();
  if (!edit.id) {
    console.error("Erro ao criar Edit:", JSON.stringify(edit));
    return;
  }
  console.log("Edit ID:", edit.id);

  try {
    const tracksRes = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/tracks`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const tracksData = await tracksRes.json();
    console.log("Tracks disponíveis:", tracksData.tracks?.map(t => `${t.track} (releases: ${t.releases?.length || 0})`));

    for (const trackName of ["alpha", "internal", "beta"]) {
      const tRes = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/testers/${trackName}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (tRes.ok) {
        const tData = await tRes.json();
        console.log(`\n=== TESTERS NA TRACK '${trackName}' ===`);
        console.log(JSON.stringify(tData, null, 2));
      } else {
        console.log(`Track '${trackName}' testers status:`, tRes.status, await tRes.text());
      }
    }
  } finally {
    await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
  }
}

run().catch(e => {
  console.error("Erro:", e.message);
  process.exit(1);
});
