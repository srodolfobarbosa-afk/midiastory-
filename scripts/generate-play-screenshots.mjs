#!/usr/bin/env node
/**
 * generate-play-screenshots.mjs
 *
 * Generates 32 Play Store phone screenshots (4 slides x 8 locales) at 1080x1920 (9:16 PNG)
 * wrapping clean captured Midiastore Android screens inside a device showcase frame via
 * Windows WPF DirectWrite/Direct2D hardware-independent rasterizer (scripts/render-screenshots.ps1).
 *
 * Policy safeguards:
 * - Zero prohibited claims (no blur/similarity, compression, other-app cache, RAM boost,
 *   CPU cooling, MP3 audio conversion, or misleading free-month promos).
 * - Highlights only real implemented features: on-device MediaStore map, review-first
 *   large/old candidates, byte-exact SHA-256 duplicates, Android Trash/consent flow,
 *   and built-in Media Studio (Photo filters, EXIF GPS cleaner, M4A extractor, QR/PDF A4).
 *
 * Usage:
 *   node scripts/generate-play-screenshots.mjs
 *   node scripts/generate-play-screenshots.mjs --locale=pt-BR
 */

import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { execFileSync } from "node:child_process";

const ROOT = process.cwd();
const OUT_ROOT = path.join(ROOT, "docs", "campaigns", "store-screenshots");
const BASE_DIR = path.join(OUT_ROOT, "_base");
const PS_RENDERER = path.join(ROOT, "scripts", "render-screenshots.ps1");

const SLIDE_SPECS = [
  { id: "01_storage", baseFile: "home.png", accent: "#00d9a3" },
  { id: "02_large_files", baseFile: "review_result.png", accent: "#38bdf8" },
  { id: "03_duplicates_sha256", baseFile: "trash_safety.png", accent: "#10b981" },
  { id: "04_privacy_studio", baseFile: "pro_privacy.png", accent: "#2dd4bf" },
];

const LOCALES = {
  "pt-BR": {
    wpfFont: "Segoe UI",
    titleFontSize: 52,
    titleLineHeight: 60,
    slides: [
      {
        badge: "RAIO-X DO APARELHO",
        title: "Veja onde está seu\nespaço em segundos",
        subtitle: "Mapa real de fotos, vídeos e áudios direto no aparelho, sem enviar nada à nuvem.",
        chips: ["100% No aparelho", "Scoped Storage", "Zero risco"],
      },
      {
        badge: "REVISÃO GUIADA",
        title: "Ache vídeos gigantes e\npesados para revisar",
        subtitle: "Você confere cada item proposto e decide o que manter antes de qualquer ação.",
        chips: ["Arquivos grandes", "Itens antigos", "Controle total"],
      },
      {
        badge: "SHA-256 + LIXEIRA",
        title: "Cópias idênticas e\nexclusão segura",
        subtitle: "Hash SHA-256 exato + confirmação nativa do Android com suporte à lixeira.",
        chips: ["Hash SHA-256", "Lixeira Android 11+", "Sem exclusão oculta"],
      },
      {
        badge: "PRIVACIDADE & ESTÚDIO",
        title: "Privacidade local e\nEstúdio de Mídia Pro",
        subtitle: "Remova GPS EXIF, extraia áudio M4A, aplique filtros, gere QR e PDF A4 offline.",
        chips: ["Limpa GPS EXIF", "Extrai M4A", "Planos Google Play"],
      },
    ],
  },
  "en-US": {
    wpfFont: "Segoe UI",
    titleFontSize: 52,
    titleLineHeight: 60,
    slides: [
      {
        badge: "ON-DEVICE STORAGE MAP",
        title: "See what fills your\nstorage in seconds",
        subtitle: "Clear breakdown of photos, videos and audio processed privately on your phone.",
        chips: ["100% On-Device", "Scoped Storage", "No Cloud Upload"],
      },
      {
        badge: "USER-FIRST REVIEW",
        title: "Spot heavy & old\nvideos fast",
        subtitle: "Review every suggested candidate yourself before moving or deleting anything.",
        chips: ["Large Files", "Old Media", "You Stay in Control"],
      },
      {
        badge: "SHA-256 + TRASH",
        title: "Exact SHA-256 copies\n& safe removal",
        subtitle: "Byte-exact SHA-256 matching plus Android system confirmation and Trash restore.",
        chips: ["Exact SHA-256", "Android Trash", "Zero Auto-Delete"],
      },
      {
        badge: "PRIVACY & STUDIO PRO",
        title: "Local privacy & built-in\nMedia Studio",
        subtitle: "Strip EXIF GPS, extract M4A audio, edit filters, create QR codes and A4 PDFs.",
        chips: ["EXIF GPS Cleaner", "M4A Extractor", "Google Play Billing"],
      },
    ],
  },
  "es-ES": {
    wpfFont: "Segoe UI",
    titleFontSize: 52,
    titleLineHeight: 60,
    slides: [
      {
        badge: "MAPA LOCAL EN EL TELÉFONO",
        title: "Mira qué ocupa tu\nespacio al instante",
        subtitle: "Analiza fotos, videos y audios en tu dispositivo sin subir archivos a la nube.",
        chips: ["100% En el móvil", "Scoped Storage", "Sin nube"],
      },
      {
        badge: "REVISIÓN SEGURA",
        title: "Detecta videos pesados\ny antiguos primero",
        subtitle: "Tú revisas cada archivo sugerido y eliges qué conservar antes de borrar.",
        chips: ["Archivos grandes", "Antiguos", "Control total"],
      },
      {
        badge: "SHA-256 + PAPELERA",
        title: "Duplicados exactos y\nborrado protegido",
        subtitle: "Verificación exacta SHA-256 y confirmación oficial de Android con papelera.",
        chips: ["Hash SHA-256", "Papelera Android", "Sin borrado oculto"],
      },
      {
        badge: "PRIVACIDAD Y ESTUDIO",
        title: "Privacidad local y\nEstudio Multimedia",
        subtitle: "Borra GPS EXIF, extrae audio M4A, aplica filtros y crea QR o PDF A4 sin internet.",
        chips: ["Limpia GPS EXIF", "Audio M4A", "Google Play Billing"],
      },
    ],
  },
  "fr-FR": {
    wpfFont: "Segoe UI",
    titleFontSize: 50,
    titleLineHeight: 58,
    slides: [
      {
        badge: "ANALYSE LOCALE",
        title: "Visualisez votre espace\nen quelques secondes",
        subtitle: "Cartographie privée des photos, vidéos et audios directement sur votre téléphone.",
        chips: ["100% Hors ligne", "Scoped Storage", "Zéro cloud"],
      },
      {
        badge: "CONTRÔLE UTILISATEUR",
        title: "Repérez les vidéos\nlourdes à vérifier",
        subtitle: "Examinez chaque fichier proposé avant toute action dans votre galerie.",
        chips: ["Gros fichiers", "Médias anciens", "Votre choix"],
      },
      {
        badge: "SHA-256 + CORBEILLE",
        title: "Doublons exacts et\ncorbeille Android",
        subtitle: "Empreinte SHA-256 exacte et confirmation système Android avec restauration.",
        chips: ["Exact SHA-256", "Corbeille Android", "Sûr par défaut"],
      },
      {
        badge: "VIE PRIVÉE & STUDIO",
        title: "Confidentialité locale &\nStudio Média Pro",
        subtitle: "Supprimez le GPS EXIF, extrayez en M4A, filtrez vos photos et créez QR ou PDF A4.",
        chips: ["Sans GPS EXIF", "Audio M4A", "Google Play Billing"],
      },
    ],
  },
  "de-DE": {
    wpfFont: "Segoe UI",
    titleFontSize: 50,
    titleLineHeight: 58,
    slides: [
      {
        badge: "LOKALE SPEICHERKARTE",
        title: "Sieh sofort, was deinen\nSpeicher füllt",
        subtitle: "Privater Überblick über Fotos, Videos und Audio direkt auf deinem Gerät.",
        chips: ["100% Lokal", "Scoped Storage", "Ohne Cloud"],
      },
      {
        badge: "KLARE PRÜFUNG",
        title: "Große & alte Videos\nsicher prüfen",
        subtitle: "Du kontrollierst jeden Vorschlag selbst, bevor etwas verschoben oder gelöscht wird.",
        chips: ["Große Dateien", "Alte Medien", "Volle Kontrolle"],
      },
      {
        badge: "SHA-256 + PAPIERKORB",
        title: "Exakte Duplikate &\nAndroid-Papierkorb",
        subtitle: "Bitgenauer SHA-256-Hash mit nativer Android-Bestätigung und Wiederherstellung.",
        chips: ["Exaktes SHA-256", "Papierkorb", "Kein Auto-Löschen"],
      },
      {
        badge: "DATENSCHUTZ & STUDIO",
        title: "Lokaler Datenschutz &\nMedien-Studio Pro",
        subtitle: "EXIF-GPS entfernen, M4A extrahieren, Fotofilter, QR-Codes und A4-PDF offline.",
        chips: ["EXIF-GPS-Schutz", "M4A-Audio", "Google Play Billing"],
      },
    ],
  },
  "hi-IN": {
    wpfFont: "Nirmala UI",
    titleFontSize: 48,
    titleLineHeight: 60,
    slides: [
      {
        badge: "ऑन-डिवाइस स्टोरेज मैप",
        title: "सेकंडों में देखें फ़ोन में\nजगह कहाँ भरी है",
        subtitle: "फ़ोटो, वीडियो और ऑडियो की सुरक्षित जांच — सीधे आपके डिवाइस पर, बिना क्लाउड अपलोड।",
        chips: ["100% ऑफ़लाइन", "Scoped Storage", "कोई क्लाउड नहीं"],
      },
      {
        badge: "सुरक्षित समीक्षा",
        title: "बड़े और पुराने वीडियो\nआसानी से जांचें",
        subtitle: "किसी भी फ़ाइल को हटाने से पहले हर सुझाव की समीक्षा आप स्वयं करते हैं।",
        chips: ["बड़ी फ़ाइलें", "पुराना मीडिया", "पूरा नियंत्रण"],
      },
      {
        badge: "SHA-256 + ट्रैश",
        title: "सटीक SHA-256 कॉपी\nऔर सुरक्षित ट्रैश",
        subtitle: "बाइट-सटीक SHA-256 मिलान और Android सिस्टम की आधिकारिक पुष्टि व ट्रैश सुरक्षा।",
        chips: ["सटीक SHA-256", "Android ट्रैश", "बिना ऑटो-डिलीट"],
      },
      {
        badge: "गोपनीयता और स्टूडियो",
        title: "लोकल प्राइवेसी और\nमीडिया स्टूडियो Pro",
        subtitle: "EXIF GPS हटाएं, M4A ऑडियो निकालें, फ़ोटो फ़िल्टर, QR और A4 PDF ऑफ़लाइन बनाएं।",
        chips: ["EXIF GPS सफ़ाई", "M4A ऑडियो", "Google Play बिलिंग"],
      },
    ],
  },
  "ja-JP": {
    wpfFont: "Yu Gothic UI",
    titleFontSize: 48,
    titleLineHeight: 58,
    slides: [
      {
        badge: "端末内ストレージ解析",
        title: "容量の使い道を数秒で\nわかりやすく表示",
        subtitle: "写真・動画・音声の内訳を端末内だけで解析。クラウドへの送信はありません。",
        chips: ["100% 端末内処理", "Scoped Storage", "クラウド送信なし"],
      },
      {
        badge: "確認してから整理",
        title: "容量の大きい動画や\n古い項目をすぐ確認",
        subtitle: "候補ファイルを1つずつ自分で確認してから移動・削除を選択できます。",
        chips: ["大容量ファイル", "古いメディア", "安全な確認フロー"],
      },
      {
        badge: "SHA-256 + ゴミ箱",
        title: "完全一致SHA-256と\nAndroidゴミ箱保護",
        subtitle: "バイト単位のSHA-256一致判定とAndroid公式の削除確認・ゴミ箱復元に対応。",
        chips: ["厳密SHA-256", "Androidゴミ箱", "自動削除なし"],
      },
      {
        badge: "プライバシー & スタジオ",
        title: "ローカル保護と\nメディアスタジオPro",
        subtitle: "EXIF GPS削除、M4A音声抽出、写真フィルター、QRコード・A4 PDFをオフライン作成。",
        chips: ["EXIF GPS削除", "M4A音声抽出", "Google Play決済"],
      },
    ],
  },
  "zh-CN": {
    wpfFont: "Microsoft YaHei UI",
    titleFontSize: 50,
    titleLineHeight: 60,
    slides: [
      {
        badge: "本机存储全景分析",
        title: "几秒看清手机空间\n到底被什么占用",
        subtitle: "纯本地扫描照片、视频与音频占用，无需联网或上传云端，隐私安全。",
        chips: ["100% 本地处理", "Scoped Storage", "绝不上传云端"],
      },
      {
        badge: "人工逐项确认",
        title: "快速找出大视频与\n长期闲置媒体",
        subtitle: "所有待清理文件均先呈交列表由您亲自核对，不擅自删改任何内容。",
        chips: ["超大文件", "陈旧媒体", "完全自主掌控"],
      },
      {
        badge: "SHA-256 + 回收站",
        title: "精准 SHA-256 查重与\n系统级回收站保护",
        subtitle: "采用字节级 SHA-256 哈希核验，搭配 Android 官方弹窗确认与回收站恢复。",
        chips: ["精准 SHA-256", "系统回收站", "绝不静默删除"],
      },
      {
        badge: "本地隐私与媒体工作室",
        title: "本地隐私防护与\nPro 媒体工作室",
        subtitle: "支持清除照片 EXIF GPS、提取 M4A 音频、滤镜调色及离线生成二维码/A4 PDF。",
        chips: ["清除 EXIF GPS", "提取 M4A 音频", "Google Play 计费"],
      },
    ],
  },
};

function assertValidPng1080x1920(filePath) {
  const buf = fs.readFileSync(filePath);
  if (buf.length < 32) {
    throw new Error(`PNG too small: ${filePath} (${buf.length} bytes)`);
  }
  const pngSig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  if (!buf.subarray(0, 8).equals(pngSig)) {
    throw new Error(`Invalid PNG signature in ${filePath}`);
  }
  const ihdrType = buf.subarray(12, 16).toString("ascii");
  if (ihdrType !== "IHDR") {
    throw new Error(`Missing IHDR chunk in ${filePath}`);
  }
  const width = buf.readUInt32BE(16);
  const height = buf.readUInt32BE(20);
  if (width !== 1080 || height !== 1920) {
    throw new Error(`Unexpected dimensions in ${filePath}: ${width}x${height} (expected 1080x1920)`);
  }
  return { width, height, bytes: buf.length };
}

function main() {
  if (!fs.existsSync(PS_RENDERER)) {
    throw new Error(`Missing WPF renderer script: ${PS_RENDERER}`);
  }

  const localeArg = process.argv.find((arg) => arg.startsWith("--locale="));
  const filterLocale = localeArg ? localeArg.slice("--locale=".length).trim() : null;

  if (filterLocale && !LOCALES[filterLocale]) {
    throw new Error(
      `Unknown locale '${filterLocale}'. Valid locales: ${Object.keys(LOCALES).join(", ")}`
    );
  }

  const targetLocales = filterLocale
    ? [[filterLocale, LOCALES[filterLocale]]]
    : Object.entries(LOCALES);

  const manifest = [];
  for (const [localeCode, config] of targetLocales) {
    for (let i = 0; i < SLIDE_SPECS.length; i++) {
      const spec = SLIDE_SPECS[i];
      const slide = config.slides[i];
      const baseImagePath = path.join(BASE_DIR, spec.baseFile);
      if (!fs.existsSync(baseImagePath)) {
        throw new Error(`Missing base screenshot: ${baseImagePath}`);
      }
      manifest.push({
        locale: localeCode,
        id: spec.id,
        wpfFont: config.wpfFont,
        titleFontSize: config.titleFontSize,
        titleLineHeight: config.titleLineHeight,
        accent: spec.accent,
        badge: slide.badge,
        title: slide.title,
        subtitle: slide.subtitle,
        chips: slide.chips,
        baseImagePath,
        outPngPath: path.join(OUT_ROOT, localeCode, `${spec.id}.png`),
      });
    }
  }

  const tmpRoot = fs.mkdtempSync(path.join(os.tmpdir(), "midiastore-wpf-"));
  const manifestPath = path.join(tmpRoot, "manifest.json");
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), "utf8");

  try {
    const out = execFileSync(
      "powershell.exe",
      [
        "-Sta",
        "-NoProfile",
        "-Command",
        "& ([ScriptBlock]::Create([System.IO.File]::ReadAllText($env:MIDI_SHOTS_PS1, [System.Text.Encoding]::UTF8)))",
      ],
      {
        encoding: "utf8",
        timeout: 120000,
        env: {
          ...process.env,
          MIDI_SHOTS_MANIFEST: manifestPath,
          MIDI_SHOTS_PS1: PS_RENDERER,
        },
      }
    );
    if (out.trim()) console.log(out.trim());
  } finally {
    try {
      fs.rmSync(tmpRoot, { recursive: true, force: true });
    } catch {}
  }

  let totalBytes = 0;
  for (const item of manifest) {
    const meta = assertValidPng1080x1920(item.outPngPath);
    totalBytes += meta.bytes;
  }

  console.log(`\nDONE: Generated & verified ${manifest.length} PNGs (all 1080x1920).`);
  console.log(`Total size: ${(totalBytes / 1024 / 1024).toFixed(2)} MB`);
}

main();
