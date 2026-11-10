import fs from "node:fs";
import zlib from "node:zlib";
import crypto from "node:crypto";

const keyPath = ".secrets/play-service-account.json";
const packageName = "com.rdf112.midiastore";
const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i];
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
  }
  return (c ^ 0xffffffff) >>> 0;
}

function pngChunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const typeBuf = Buffer.from(type, "ascii");
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([len, typeBuf, data, crcBuf]);
}

function pointInPolygon(px, py, poly) {
  let inside = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const xi = poly[i][0], yi = poly[i][1];
    const xj = poly[j][0], yj = poly[j][1];
    const intersect = yi > py !== yj > py && px < ((xj - xi) * (py - yi)) / (yj - yi + 1e-9) + xi;
    if (intersect) inside = !inside;
  }
  return inside;
}

function inRoundedRect(x, y, x1, y1, x2, y2, r) {
  if (x < x1 || x > x2 || y < y1 || y > y2) return false;
  const cx = Math.max(x1 + r, Math.min(x, x2 - r));
  const cy = Math.max(y1 + r, Math.min(y, y2 - r));
  const dx = x - cx;
  const dy = y - cy;
  return dx * dx + dy * dy <= r * r;
}

function generateBrandPng(width, height) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; // 8-bit
  ihdr[9] = 6; // 32-bit RGBA (exigido pelo Google Play para ícones 512x512)
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  const rowSize = 1 + width * 4;
  const raw = Buffer.alloc(rowSize * height);
  const scale = Math.min(width, height) / 108;
  const offsetX = (width - 108 * scale) / 2;
  const offsetY = (height - 108 * scale) / 2;

  const checkPoly = [
    [43, 45],
    [49, 51],
    [63, 37],
    [68, 42],
    [49, 61],
    [38, 50],
  ];
  const starPoly = [
    [79, 16],
    [82, 23],
    [89, 26],
    [82, 29],
    [79, 36],
    [76, 29],
    [69, 26],
    [76, 23],
  ];

  for (let y = 0; y < height; y++) {
    const rowStart = y * rowSize;
    raw[rowStart] = 0; // filter None
    for (let x = 0; x < width; x++) {
      const idx = rowStart + 1 + x * 4;
      const vx = (x - offsetX) / scale;
      const vy = (y - offsetY) / scale;

      // Deep Obsidian background (#070B14 -> #0F172A subtle radial glow)
      const gdx = (x - width / 2) / (width * 0.6);
      const gdy = (y - height / 2) / (height * 0.6);
      const glow = Math.max(0, 1 - Math.sqrt(gdx * gdx + gdy * gdy));
      let r = Math.round(7 + glow * 10);
      let g = Math.round(11 + glow * 22);
      let b = Math.round(20 + glow * 34);

      // Outer phone body: M26,13..82,95 R=14 (#00E5B0)
      if (inRoundedRect(vx, vy, 26, 13, 82, 95, 14)) {
        r = 0;
        g = 229;
        b = 176;
      }
      // Inner screen: M35,22..73,81 R=6 (#101827)
      if (inRoundedRect(vx, vy, 35, 22, 73, 81, 6)) {
        r = 16;
        g = 24;
        b = 39;
      }
      // Storage bars (#22D3EE)
      if (
        (vx >= 42 && vx <= 49 && vy >= 68 && vy <= 73) ||
        (vx >= 52 && vx <= 59 && vy >= 60 && vy <= 73) ||
        (vx >= 62 && vx <= 69 && vy >= 51 && vy <= 73)
      ) {
        r = 34;
        g = 211;
        b = 238;
      }
      // White checkmark (#FFFFFF)
      if (pointInPolygon(vx, vy, checkPoly)) {
        r = 255;
        g = 255;
        b = 255;
      }
      // Sparkle star (#5DF5D3)
      if (pointInPolygon(vx, vy, starPoly)) {
        r = 93;
        g = 245;
        b = 211;
      }

      raw[idx] = r;
      raw[idx + 1] = g;
      raw[idx + 2] = b;
      raw[idx + 3] = 255;
    }
  }

  const idat = zlib.deflateSync(raw, { level: 6 });
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    pngChunk("IHDR", ihdr),
    pngChunk("IDAT", idat),
    pngChunk("IEND", Buffer.alloc(0)),
  ]);
}

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
  if (!data.access_token) throw new Error(JSON.stringify(data));
  return data.access_token;
}

const listingsByLanguage = {
  "pt-BR": {
    title: "Midiastore: Limpador de Espaço",
    shortDescription: "Libere espaço com segurança: arquivos grandes, cópias exatas e antigos.",
    fullDescription:
      "Midiastore é o limpador e organizador inteligente de armazenamento para Android, projetado com transparência e processamento 100% no seu aparelho.\n\n" +
      "PRINCIPAIS RECURSOS:\n" +
      "• Medidor Real de Armazenamento: Visualize imediatamente quanto espaço está ocupado e livre no armazenamento principal.\n" +
      "• Limpeza de Arquivos Grandes (>10 MB): Encontre vídeos, fotos e áudios pesados em segundos e revise antes de remover.\n" +
      "• Detector de Cópias Exatas (SHA-256 · Pro): Identifica arquivos duplicados reais byte a byte usando verificação criptográfica SHA-256, mantendo sempre uma cópia original protegida.\n" +
      "• Arquivos Antigos (+30 dias · Pro): Localize mídias antigas que ocupam espaço sem necessidade.\n" +
      "• Sugestão de Apps sem Uso (+90 dias · Pro): Descubra aplicativos instalados que você não abre há mais de 3 meses.\n" +
      "• Segurança Oficial do Android: Nada é apagado sem a sua revisão e confirmação no diálogo oficial do sistema Android (suporte à Lixeira do Android 11+ com restauração e exclusão verificada).\n\n" +
      "PRIVACIDADE EM PRIMEIRO LUGAR:\n" +
      "Suas fotos, vídeos e áudios são analisados localmente no seu dispositivo.",
  },
  "en-US": {
    title: "Midiastore: Storage Cleaner",
    shortDescription: "Safely free up space: find large files, exact duplicates, and old media.",
    fullDescription:
      "Midiastore is a transparent, privacy-first storage cleaner and media organizer for Android with 100% on-device analysis.\n\n" +
      "KEY FEATURES:\n" +
      "• Real-Time Storage Gauge: Instantly see used and free space on your primary shared storage.\n" +
      "• Large File Finder (>10 MB): Quickly spot heavy videos, photos, and audio files to review before removal.\n" +
      "• Exact Duplicate Finder (SHA-256 · Pro): Finds true byte-for-byte duplicate files using SHA-256 cryptographic hashing while automatically protecting one original copy.\n" +
      "• Older Media Finder (30+ days · Pro): Locate older media files taking up valuable storage.\n" +
      "• Unused App Suggestions (90+ days · Pro): Identify installed apps you haven't opened in over 90 days.\n" +
      "• Official Android Safety: Nothing is ever deleted automatically. You review every candidate and confirm via Android's official system dialog (supports Android 11+ Trash & restore).\n\n" +
      "PRIVACY FIRST:\n" +
      "Your photos, videos, and audio are analyzed locally on your device.",
  },
  "es-ES": {
    title: "Midiastore: Limpiador Espacio",
    shortDescription: "Libera espacio seguro: archivos grandes, copias exactas y antiguos.",
    fullDescription:
      "Midiastore es un limpiador y organizador de almacenamiento para Android con análisis 100% local en tu dispositivo.\n\n" +
      "FUNCIONES PRINCIPALES:\n" +
      "• Medidor de Almacenamiento Real: Consulta al instante el espacio usado y libre.\n" +
      "• Archivos Grandes (>10 MB): Encuentra vídeos, fotos y audios pesados y revísalos antes de eliminarlos.\n" +
      "• Copias Exactas (SHA-256 · Pro): Detecta archivos duplicados reales mediante verificación SHA-256 protegiendo siempre el original.\n" +
      "• Archivos Antiguos (+30 días · Pro) y Apps sin Uso (+90 días · Pro).\n" +
      "• Seguridad Oficial de Android: Nada se borra sin tu confirmación en el diálogo oficial de Android.",
  },
  "fr-FR": {
    title: "Midiastore : Nettoyeur Espace",
    shortDescription: "Libérez de l'espace : fichiers lourds, doublons exacts et anciens.",
    fullDescription:
      "Midiastore est un nettoyeur de stockage transparent pour Android avec analyse 100 % locale sur votre appareil.\n\n" +
      "FONCTIONNALITÉS CLÉS :\n" +
      "• Jauge de Stockage Réelle : Visualisez l'espace utilisé et libre.\n" +
      "• Fichiers Volumineux (>10 Mo) : Repérez les vidéos, photos et audios lourds avant de les supprimer.\n" +
      "• Doublons Exacts (SHA-256 · Pro) : Détection par empreinte SHA-256 avec protection automatique de l'original.\n" +
      "• Fichiers Anciens (+30 jours · Pro) et Applications Inutilisées (+90 jours · Pro).\n" +
      "• Sécurité Android Officielle : Aucune suppression automatique sans votre confirmation système.",
  },
  "de-DE": {
    title: "Midiastore: Speicher-Reiniger",
    shortDescription: "Speicherplatz freigeben: Große Dateien, Duplikate & alte Medien.",
    fullDescription:
      "Midiastore ist ein transparenter Speicher-Reiniger für Android mit 100 % lokaler Analyse auf Ihrem Gerät.\n\n" +
      "HAUPTFUNKTIONEN:\n" +
      "• Echtzeit-Speicheranzeige: Belegter und freier Speicherplatz auf einen Blick.\n" +
      "• Große Dateien (>10 MB): Finden und prüfen Sie große Videos, Fotos und Audiodateien.\n" +
      "• Exakte Duplikate (SHA-256 · Pro): Erkennt echte Duplikate per SHA-256-Prüfsumme und schützt das Original.\n" +
      "• Alte Dateien (+30 Tage · Pro) & Ungenutzte Apps (+90 Tage · Pro).\n" +
      "• Offizielle Android-Sicherheit: Nichts wird ohne Ihre Bestätigung im Android-Systemdialog gelöscht.",
  },
  "hi-IN": {
    title: "Midiastore: स्टोरेज क्लीनर",
    shortDescription: "सुरक्षित रूप से स्पेस खाली करें: बड़ी फ़ाइलें और डुप्लिकेट खोजें।",
    fullDescription:
      "Midiastore Android के लिए एक पारदर्शी और सुरक्षित स्टोरेज क्लीनर है जो 100% आपके फ़ोन पर काम करता है।\n\n" +
      "मुख्य विशेषताएं:\n" +
      "• रियल-टाइम स्टोरेज मीटर: इस्तेमाल और खाली जगह तुरंत देखें।\n" +
      "• बड़ी फ़ाइलें (>10 MB): भारी वीडियो, फ़ोटो और ऑडियो फ़ाइलें खोजें।\n" +
      "• सटीक डुप्लिकेट (SHA-256 · Pro): मूल फ़ाइल को सुरक्षित रखते हुए डुप्लिकेट कॉपी पहचानें।\n" +
      "• पुरानी फ़ाइलें (+30 दिन · Pro) और बिना उपयोग वाले ऐप्स (+90 दिन · Pro)।",
  },
  "ja-JP": {
    title: "Midiastore: ストレージクリーナー",
    shortDescription: "安全に容量を確保：大容量ファイル、重複コピー、古いメディアを検出。",
    fullDescription:
      "Midiastoreは、端末内で100%ローカル解析を行う透明性の高いAndroid向けストレージクリーナーです。\n\n" +
      "主な機能：\n" +
      "• リアルタイム容量メーター：使用中・空き容量を一目で確認。\n" +
      "• 大容量ファイル検出（10MB以上）：大きな動画・写真・音声を確認して整理。\n" +
      "• 完全一致の重複ファイル検出（SHA-256・Pro）：原本を保護しながら重複コピーを特定。\n" +
      "• 古いファイル（30日以上・Pro）＆未使用アプリ検出（90日以上・Pro）。",
  },
  "zh-CN": {
    title: "Midiastore：存储空间清理工具",
    shortDescription: "安全释放手机空间：查找大文件、SHA-256精确重复项和旧媒体。",
    fullDescription:
      "Midiastore 是一款透明、安全的 Android 存储清理与媒体整理工具，100% 在设备本地完成分析。\n\n" +
      "核心功能：\n" +
      "• 实时存储仪表盘：直观查看已用空间与可用空间。\n" +
      "• 大文件查找（>10 MB）：快速定位占用空间的大视频、照片与音频。\n" +
      "• 精确重复文件检测（SHA-256 · Pro）：通过 SHA-256 校验识别完全相同的副本并自动保护原件。\n" +
      "• 旧文件清理（30天以上 · Pro）与闲置应用建议（90天未用 · Pro）。",
  },
};

async function main() {
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
  if (!editRes.ok) throw new Error(JSON.stringify(edit));
  console.log("Edit criado:", edit.id);

  const detailsRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/details`,
    {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        defaultLanguage: "pt-BR",
        contactWebsite: "https://midiastory-df6a0.web.app",
        contactEmail: "srodolfobarbosa@gmail.com",
      }),
    }
  );
  console.log("Details atualizados:", detailsRes.status, await detailsRes.text());

  for (const [lang, listing] of Object.entries(listingsByLanguage)) {
    const res = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}`,
      {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          language: lang,
          title: listing.title,
          shortDescription: listing.shortDescription,
          fullDescription: listing.fullDescription,
        }),
      }
    );
    console.log(`Listing ${lang}:`, res.status, await res.text());
  }

  // Upload icon (512x512) and featureGraphic (1024x500) to ALL 8 languages via /listings/{lang}/{imageType}
  const iconPng = generateBrandPng(512, 512);
  const featurePng = generateBrandPng(1024, 500);
  fs.writeFileSync("docs/icon-512.png", iconPng);
  const screen1 = "C:\\Users\\rodolfo\\.gemini\\antigravity-cli\\brain\\3ff33c3b-6285-4c82-8b8d-e1eadc8b4659\\midiastore_v20_clean.png";
  const screen2 = "C:\\Users\\rodolfo\\.gemini\\antigravity-cli\\brain\\3ff33c3b-6285-4c82-8b8d-e1eadc8b4659\\midiastore_v19_clean.png";

  for (const lang of Object.keys(listingsByLanguage)) {
    await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/icon`,
      { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
    );
    const iconRes = await fetch(
      `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/icon?uploadType=media`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "image/png" },
        body: iconPng,
      }
    );
    console.log(`Icon (${lang}):`, iconRes.status, await iconRes.text());

    await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/featureGraphic`,
      { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
    );
    const fgRes = await fetch(
      `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/featureGraphic?uploadType=media`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "image/png" },
        body: featurePng,
      }
    );
    console.log(`FeatureGraphic (${lang}):`, fgRes.status);
  }

  for (const lang of ["pt-BR", "en-US"]) {
    await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/phoneScreenshots`,
      { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
    );
    for (const sp of [screen1, screen2]) {
      if (fs.existsSync(sp)) {
        const scRes = await fetch(
          `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/phoneScreenshots?uploadType=media`,
          {
            method: "POST",
            headers: { Authorization: `Bearer ${token}`, "Content-Type": "image/png" },
            body: fs.readFileSync(sp),
          }
        );
        console.log(`Screenshot (${lang}):`, scRes.status);
      }
    }
  }

  const commitRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  console.log("Commit listings:", commitRes.status, await commitRes.text());
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
