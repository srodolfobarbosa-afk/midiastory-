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

    const trackRes = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/tracks/${track}`,
      {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          track,
          releases: [
            {
              name: `${bundle.versionCode}`,
              versionCodes: [String(bundle.versionCode)],
              status: "completed",
            },
          ],
        }),
      }
    );
    const trackResult = await trackRes.json();
    if (!trackRes.ok) throw new Error(JSON.stringify(trackResult));
    console.log("Track atualizada:", trackResult);

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
