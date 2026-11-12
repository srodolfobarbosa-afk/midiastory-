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
    title: "Midiastore: Limpador & Estúdio",
    shortDescription: "Libere espaço com segurança: Estúdio de Mídia, duplicatas e arquivos grandes.",
    fullDescription:
      "Midiastore é o limpador inteligente de armazenamento e estúdio de mídia para Android, projetado com transparência e processamento 100% no seu aparelho.\n\n" +
      "NOVIDADE — ESTÚDIO DE MÍDIA & FERRAMENTAS:\n" +
      "• Filtros de Fotos ao Vivo: Aplique efeitos Cinema, Vintage, P&B e Nitidez instantaneamente.\n" +
      "• Extrator de Áudio (MP3/M4A): Extraia faixas de áudio cristalinas de vídeos salvos.\n" +
      "• Limpador de Privacidade EXIF: Remova localização GPS oculta e metadados de suas fotos antes de compartilhar.\n" +
      "• Gerador de QR Code: Crie códigos QR para PIX, links, WhatsApp e redes Wi-Fi em segundos.\n" +
      "• Fotos para PDF: Converta e agrupe fotos em documentos PDF prontos para impressão.\n" +
      "• Importação Rápida por Compartilhamento: Abra qualquer mídia ou link diretamente no app pelo menu de compartilhamento do Android.\n\n" +
      "RECURSOS ESSENCIAIS DE LIMPEZA:\n" +
      "• Medidor Real de Armazenamento: Visualize imediatamente o espaço ocupado e livre.\n" +
      "• Limpeza de Arquivos Grandes (>10 MB): Encontre vídeos, fotos e áudios pesados em segundos.\n" +
      "• Detector de Cópias Exatas (SHA-256 · Pro): Identifica arquivos duplicados reais byte a byte com verificação criptográfica SHA-256, protegendo o original.\n" +
      "• Arquivos Antigos (+30 dias · Pro) e Sugestão de Apps sem Uso (+90 dias · Pro).\n" +
      "• Segurança Oficial do Android: Nada é apagado sem a sua confirmação no diálogo oficial do sistema Android.\n\n" +
      "PRIVACIDADE TOTAL:\n" +
      "Seus arquivos são analisados 100% localmente no seu dispositivo.",
  },
  "en-US": {
    title: "Midiastore: Cleaner & Studio",
    shortDescription: "Safely free up space: Media Studio, exact duplicates, and large files.",
    fullDescription:
      "Midiastore is a transparent, privacy-first storage cleaner and media studio suite for Android with 100% on-device processing.\n\n" +
      "NEW — MEDIA STUDIO & PRODUCTIVITY TOOLS:\n" +
      "• Live Photo Filters: Apply Cinematic, Vintage, B&W, and Sharpness effects instantly.\n" +
      "• Video-to-Audio Extractor (MP3/M4A): Extract crystal-clear audio tracks from your videos.\n" +
      "• EXIF Privacy Cleaner: Strip hidden GPS coordinates and sensitive metadata before sharing photos.\n" +
      "• QR Code Generator: Generate fast QR codes for URLs, Wi-Fi, WhatsApp, and text.\n" +
      "• Photos to PDF: Convert and bundle multiple photos into clean, printable PDF documents.\n" +
      "• Quick Share Import: Open media directly into tools via Android's native share menu.\n\n" +
      "CORE CLEANING FEATURES:\n" +
      "• Real-Time Storage Gauge: Instantly see used and free space on your storage.\n" +
      "• Large File Finder (>10 MB): Spot heavy videos, photos, and audio files before removal.\n" +
      "• Exact Duplicate Finder (SHA-256 · Pro): Detect true byte-for-byte duplicates using cryptographic SHA-256 while protecting the original.\n" +
      "• Older Media Finder (30+ days · Pro) & Unused App Suggestions (90+ days · Pro).\n" +
      "• Official Android Safety: Nothing is ever deleted automatically without your explicit confirmation.\n\n" +
      "PRIVACY FIRST:\n" +
      "Your photos, videos, and files are analyzed locally on your device.",
  },
  "es-ES": {
    title: "Midiastore: Limpiador Estudio",
    shortDescription: "Libera espacio seguro: Estudio de Medios, duplicatas y archivos grandes.",
    fullDescription:
      "Midiastore es un limpiador de almacenamiento y suite de herramientas multimedia para Android con análisis 100% local.\n\n" +
      "NOVEDAD — ESTUDIO DE MEDIOS Y HERRAMIENTAS:\n" +
      "• Filtros de Fotos en Vivo: Aplica efectos Cine, Vintage, Blanco y Negro y Nitidez al instante.\n" +
      "• Extractor de Audio (MP3/M4A): Extrae pistas de audio nítidas de cualquier vídeo.\n" +
      "• Limpiador de Privacidad EXIF: Elimina coordenadas GPS y metadatos ocultos de tus imágenes.\n" +
      "• Generador de Códigos QR: Crea códigos QR rápidos para enlaces, Wi-Fi y texto.\n" +
      "• Fotos a PDF: Convierte y agrupa fotos en documentos PDF de alta calidad.\n\n" +
      "FUNCIONES DE LIMPIEZA:\n" +
      "• Medidor de Almacenamiento Real: Espacio usado y libre al instante.\n" +
      "• Archivos Grandes (>10 MB) y Copias Exactas (SHA-256 · Pro).\n" +
      "• Archivos Antiguos (+30 días · Pro) y Apps sin Uso (+90 días · Pro).\n" +
      "• Seguridad Oficial de Android: Confirmación obligatoria del sistema antes de cualquier borrado.",
  },
  "fr-FR": {
    title: "Midiastore: Nettoyeur Studio",
    shortDescription: "Libérez de l'espace : Studio Média, doublons exacts et gros fichiers.",
    fullDescription:
      "Midiastore est un nettoyeur de stockage et studio multimédia pour Android avec traitement 100 % local sur votre appareil.\n\n" +
      "NOUVEAU — STUDIO MÉDIA & OUTILS :\n" +
      "• Filtres Photo en Direct : Effets Cinéma, Vintage, Noir & Blanc et Netteté instantanés.\n" +
      "• Extracteur Audio (MP3/M4A) : Isolez le son de vos vidéos en haute qualité.\n" +
      "• Nettoyeur de Confidentialité EXIF : Supprimez les coordonnées GPS de vos photos.\n" +
      "• Générateur de QR Code : Créez des QR codes pour liens, Wi-Fi et messages.\n" +
      "• Photos vers PDF : Assemblez des photos en documents PDF nets et imprimables.\n\n" +
      "FONCTIONNALITÉS DE NETTOYAGE :\n" +
      "• Jauge de Stockage Réelle : Espace disponible en un coup d'œil.\n" +
      "• Fichiers Volumineux (>10 Mo) et Doublons Exacts (SHA-256 · Pro).\n" +
      "• Fichiers Anciens (+30 jours · Pro) et Applications Inutilisées (+90 jours · Pro).\n" +
      "• Sécurité Android Officielle : Confirmation système obligatoire avant toute action.",
  },
  "de-DE": {
    title: "Midiastore: Reiniger & Studio",
    shortDescription: "Speicher freigeben: Medien-Studio, Duplikate & große Dateien.",
    fullDescription:
      "Midiastore ist ein transparenter Speicher-Reiniger und Medien-Studio für Android mit 100 % lokaler Analyse.\n\n" +
      "NEU — MEDIEN-STUDIO & WERKZEUGE:\n" +
      "• Live-Fotofilter: Wenden Sie Kino-, Vintage-, S/W- und Schärfe-Effekte an.\n" +
      "• Audio-Extraktor (MP3/M4A): Kristallklare Audiospuren aus Videos extrahieren.\n" +
      "• EXIF-Datenschutzbereinigung: Entfernt GPS-Daten und Metadaten vor dem Teilen.\n" +
      "• QR-Code-Generator: Erstellen Sie QR-Codes für Links, WLAN und Text.\n" +
      "• Fotos zu PDF: Fotos schnell in saubere PDF-Dokumente umwandeln.\n\n" +
      "HAUPTREINIGUNGSFUNKTIONEN:\n" +
      "• Echtzeit-Speicheranzeige: Sofortige Übersicht über freien Speicherplatz.\n" +
      "• Große Dateien (>10 MB) und Exakte Duplikate (SHA-256 · Pro).\n" +
      "• Alte Dateien (+30 Tage · Pro) & Ungenutzte Apps (+90 Tage · Pro).\n" +
      "• Offizielle Android-Sicherheit: Bestätigung über das Android-Systemfenster.",
  },
  "hi-IN": {
    title: "Midiastore: क्लीनर स्टूडियो",
    shortDescription: "सुरक्षित स्टोरेज क्लीनर: मीडिया स्टूडियो, डुप्लिकेट और बड़ी फ़ाइलें।",
    fullDescription:
      "Midiastore Android के लिए एक सुरक्षित स्टोरेज क्लीनर और मीडिया स्टूडियो टूलकिट है जो 100% फ़ोन पर काम करता है।\n\n" +
      "नया — मीडिया स्टूडियो टूल्स:\n" +
      "• लाइव फ़ोटो फ़िल्टर: सिनेमा, विंटेज, B&W और शार्पनेस इफ़ेक्ट।\n" +
      "• ऑडियो एक्सट्रैक्टर (MP3/M4A): वीडियो से साफ़ ऑडियो निकालें।\n" +
      "• EXIF प्राइवेसी क्लीनर: शेयर करने से पहले फ़ोटो से GPS लोकेशन हटाएं।\n" +
      "• QR कोड जनरेटर: लिंक, वाई-फ़ाई और टेक्स्ट के लिए तुरंत QR कोड बनाएं।\n" +
      "• फ़ोटो से PDF: फ़ोटो को तुरंत PDF दस्तावेज़ में बदलें।\n\n" +
      "सफ़ाई विशेषताएं:\n" +
      "• रियल-टाइम स्टोरेज मीटर: इस्तेमाल और खाली जगह देखें।\n" +
      "• बड़ी फ़ाइलें (>10 MB) और सटीक डुप्लिकेट (SHA-256 · Pro)।\n" +
      "• पुरानी फ़ाइलें (+30 दिन · Pro) और बिना उपयोग वाले ऐप्स (+90 दिन · Pro)।",
  },
  "ja-JP": {
    title: "Midiastore: クリーナー＆スタジオ",
    shortDescription: "安全に容量確保：メディアスタジオ、完全重複検出、大容量整理。",
    fullDescription:
      "Midiastoreは、端末内100%ローカルで動作するAndroid向けストレージ整理＆メディアスタジオです。\n\n" +
      "新機能 — メディアスタジオ＆便利機能：\n" +
      "• ライブ写真フィルター：シネマ、ヴィンテージ、モノクロ、鮮明化。\n" +
      "• 音声抽出（MP3/M4A）：動画から高音質な音声を簡単抽出。\n" +
      "• EXIFプライバシークリーナー：共有前に位置情報（GPS）とメタデータを消去。\n" +
      "• QRコード生成：リンク、Wi-Fi、テキストのQRコードを作成。\n" +
      "• 写真PDF変換：写真をまとめて綺麗なPDF文書へ変換。\n\n" +
      "主な整理機能：\n" +
      "• リアルタイム容量メーター：空き容量と使用状況を可視化。\n" +
      "• 大容量ファイル検出（10MB以上）＆完全一致の重複ファイル検出（SHA-256・Pro）。\n" +
      "• 古いファイル（30日以上・Pro）＆未使用アプリ検出（90日以上・Pro）。",
  },
  "zh-CN": {
    title: "Midiastore：清理管家与媒体工坊",
    shortDescription: "安全释放空间：媒体工作室、精确重复文件与大文件清理。",
    fullDescription:
      "Midiastore 是一款透明安全的 Android 存储清理与媒体处理工坊，100% 在本地运行。\n\n" +
      "全新功能 — 媒体工坊与实用工具：\n" +
      "• 实时照片滤镜：电影色调、复古、黑白与清晰度提升。\n" +
      "• 音频提取（MP3/M4A）：一键将视频原声提取为高品质音频。\n" +
      "• EXIF 隐私清理：分享前一键抹除照片中隐藏的 GPS 位置与元数据。\n" +
      "• 二维码生成：快速制作链接、Wi-Fi与文本二维码。\n" +
      "• 照片转 PDF：将多张照片合并为标准清晰 PDF 文档。\n\n" +
      "核心清理能力：\n" +
      "• 实时存储仪表盘：一目了然已用与可用空间。\n" +
      "• 大文件查找（>10 MB）与精确重复文件检测（SHA-256 · Pro）。\n" +
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
