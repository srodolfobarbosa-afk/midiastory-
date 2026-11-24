#!/usr/bin/env node
/**
 * Sincroniza as 14 fichas da Google Play Store (100% Limpador Puro OLED, <=30 chars título, <=80 chars shortDescription),
 * envia os 56 phoneScreenshots (4 slides 1080x1920 x 14 idiomas) e os 14 featureGraphics (1024x500)
 * de docs/campaigns/store-screenshots/<lang>/ para a Google Play Console API v3
 * SEM NUNCA tocar em tracks ou testers.
 */

import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");
const keyPath = path.join(projectRoot, ".secrets", "play-service-account.json");
const packageName = "com.rdf112.midiastore";

const STORE_LISTINGS_14 = {
  "pt-BR": {
    title: "Midiastore",
    shortDescription: "Limpador leve (< 8 MB): duplicatas SHA-256, vídeos grandes e revisão segura.",
    fullDescription:
      "Celular avisando 'Armazenamento cheio'? O Midiastore é um limpador de armazenamento puro, leve (menos de 8 MB) e 100% privado no seu aparelho.\n\n" +
      "POR QUE ESCOLHER O MIDIASTORE:\n" +
      "• Menos de 8 MB: instala rápido até em aparelhos quase sem espaço.\n" +
      "• Limpeza Real em 2 Toques: revise miniaturas reais e confirme no diálogo oficial do Android.\n" +
      "• Duplicatas Exatas (SHA-256): verifica cópias idênticas byte a byte mantendo sempre 1 original protegido.\n" +
      "• Vídeos Grandes (>10 MB) e Mídias Antigas (+30 dias): encontre rapidamente o que mais consome memória.\n" +
      "• 100% On-Device (Scoped Storage): suas fotos, vídeos e áudios nunca saem do seu celular e nada é apagado sem sua permissão.",
  },
  "pt-PT": {
    title: "Midiastore",
    shortDescription: "Limpeza leve (< 8 MB): duplicados SHA-256, vídeos grandes e revisão segura.",
    fullDescription:
      "Telemóvel com armazenamento cheio? O Midiastore é um limpador de armazenamento rápido (< 8 MB) e 100% privado no seu equipamento.\n\n" +
      "DESTAQUES:\n" +
      "• Menos de 8 MB: leve e sem bloatware.\n" +
      "• Revisão em 2 Toques com miniaturas reais e confirmação oficial do Android.\n" +
      "• Duplicados Exatos (SHA-256): comparação byte a byte mantendo sempre o original.\n" +
      "• Ficheiros Grandes (>10 MB) e Antigos (+30 dias).\n" +
      "• 100% Local (Scoped Storage): os seus ficheiros nunca são enviados para a nuvem.",
  },
  "en-US": {
    title: "Midiastore",
    shortDescription: "Fast < 8 MB cleaner: exact SHA-256 duplicates & large videos. 100% on-device.",
    fullDescription:
      "Seeing 'Storage space running out'? Midiastore is a focused, ultra-lightweight (< 8 MB) Android storage cleaner built for speed, transparency, and privacy.\n\n" +
      "WHY MIDIASTORE:\n" +
      "• Under 8 MB: installs fast even when your phone is nearly full.\n" +
      "• 2-Tap Review Cleanup: inspect real thumbnails and confirm via Android's official system dialog.\n" +
      "• Exact SHA-256 Duplicates: proves byte-for-byte identical copies while keeping one original safe.\n" +
      "• Large Files (>10 MB) & Old Media (+30 days): surface space hogs in seconds.\n" +
      "• 100% On-Device Privacy: built on Android Scoped Storage — your media never leaves your device.",
  },
  "es-ES": {
    title: "Midiastore",
    shortDescription: "Limpiador ligero (< 8 MB): duplicados SHA-256 y videos grandes. 100% local.",
    fullDescription:
      "¿Tu Android dice 'Almacenamiento lleno'? Midiastore es un limpiador de almacenamiento puro, ligero (< 8 MB) y 100% privado.\n\n" +
      "VENTAJAS PRINCIPALES:\n" +
      "• Menos de 8 MB: rápido y sin bloatware.\n" +
      "• Revisión en 2 Toques con miniaturas reales y confirmación oficial de Android.\n" +
      "• Duplicados Exactos (SHA-256): verificación byte a byte protegiendo siempre el original.\n" +
      "• Archivos Grandes (>10 MB) y Antiguos (+30 días).\n" +
      "• 100% En tu Dispositivo: tus fotos y videos nunca se suben a la nube.",
  },
  "fr-FR": {
    title: "Midiastore",
    shortDescription: "Léger (< 8 Mo) : doublons SHA-256 et grosses vidéos. 100 % privé sur mobile.",
    fullDescription:
      "Stockage Android saturé ? Midiastore est un nettoyeur de stockage léger (< 8 Mo), rapide et 100 % local.\n\n" +
      "POINTS FORTS :\n" +
      "• Moins de 8 Mo : s'installe même quand l'espace manque.\n" +
      "• Nettoyage en 2 gestes avec miniatures réelles et confirmation officielle Android.\n" +
      "• Doublons Exacts (SHA-256) : vérification octet par octet protégeant l'original.\n" +
      "• Gros Fichiers (>10 Mo) et Médias Anciens (+30 jours).\n" +
      "• 100 % Hors Ligne : vos fichiers ne quittent jamais votre téléphone.",
  },
  "de-DE": {
    title: "Midiastore",
    shortDescription: "Unter 8 MB: Exakte SHA-256 Duplikate & große Videos sicher prüfen. 100% lokal.",
    fullDescription:
      "Android-Speicher voll? Midiastore ist ein schlanker (< 8 MB), transparenter Speicherreiniger ohne Bloatware.\n\n" +
      "VORTEILE:\n" +
      "• Unter 8 MB klein und sofort einsatzbereit.\n" +
      "• Bereinigung in 2 Tipps mit echten Vorschauen und offiziellem Android-Systemdialog.\n" +
      "• Exakte SHA-256 Duplikate: bitgenauer Vergleich mit Schutz der Originaldatei.\n" +
      "• Große Dateien (>10 MB) & Alte Medien (+30 Tage).\n" +
      "• 100 % Lokal auf Ihrem Gerät: kein Cloud-Upload, kein automatisches Löschen.",
  },
  "hi-IN": {
    title: "Midiastore",
    shortDescription: "< 8 MB हल्का क्लीनर: सटीक SHA-256 डुप्लीकेट और बड़े वीडियो सुरक्षित हटाएं।",
    fullDescription:
      "क्या आपका Android फ़ोन 'Storage Full' दिखा रहा है? Midiastore < 8 MB का तेज़ और 100% प्राइवेट स्टोरेज क्लीनर है।\n\n" +
      "मुख्य विशेषताएं:\n" +
      "• < 8 MB हल्का आकार: कम जगह होने पर भी तुरंत इंस्टॉल होता है।\n" +
      "• 2 टैप में असली थंबनेल के साथ सुरक्षित समीक्षा और Android पुष्टि।\n" +
      "• सटीक SHA-256 डुप्लीकेट जांच (ओरिजिनल फ़ाइल सुरक्षित रहती है)।\n" +
      "• बड़ी फ़ाइलें (>10 MB) और पुराना मीडिया (+30 दिन)।\n" +
      "• 100% ऑन-डिवाइस: आपकी फ़ोटो या वीडियो कभी क्लाउड पर अपलोड नहीं होते।",
  },
  "ja-JP": {
    title: "Midiastore",
    shortDescription: "軽量(< 8 MB)クリーナー：SHA-256重複検出と大容量動画を安全に整理。",
    fullDescription:
      "「空き容量が不足しています」と表示されたら。Midiastoreは8MB未満（< 8 MB）の軽量・高プライバシーなAndroidストレージ整理アプリです。\n\n" +
      "主な特長：\n" +
      "• 8MB未満（< 8 MB）の軽量設計。\n" +
      "• 実サムネイルを確認して2タップで安全に整理（Android公式確認ダイアログ対応）。\n" +
      "• 厳密なSHA-256重複ファイル検出（元ファイルを保護）。\n" +
      "• 大容量ファイル（10MB以上）や古いメディア（30日以上）を素早く発見。\n" +
      "• 100%端末内処理：クラウドへのアップロードや自動削除は一切ありません。",
  },
  "zh-CN": {
    title: "Midiastore",
    shortDescription: "< 8 MB轻量清理：SHA-256精准查重与大视频清理，100%本地隐私安全。",
    fullDescription:
      "手机提示“存储空间不足”？Midiastore 是一款小于 8 MB（< 8 MB）的纯粹 Android 存储空间清理工具，100% 在本机运行。\n\n" +
      "核心亮点：\n" +
      "• 小于 8 MB 超轻量，无冗余捆绑。\n" +
      "• 2 步快速核对真实缩略图，通过 Android 官方系统弹窗安全确认删除。\n" +
      "• 字节级 SHA-256 精准查重，始终保留原始文件。\n" +
      "• 快速定位超大视频（>10 MB）与陈旧媒体（+30 天）。\n" +
      "• 100% 本地处理（Scoped Storage）：绝不上传云端，绝不静默删除。",
  },
  "ar": {
    title: "Midiastore",
    shortDescription: "منظف خفيف (< 8 MB): كشف التكرار SHA-256 والفيديوهات الكبيرة بأمان تام.",
    fullDescription:
      "هل تعاني من امتلاء ذاكرة الهاتف؟ Midiastore هو منظف تخزين أندرويد خفيف (أقل من 8 ميجابايت) ويعمل 100% محلياً على جهازك.\n\n" +
      "المميزات الرئيسية:\n" +
      "• حجم خفيف جداً (< 8 MB) يعمل بسرعة فائقة.\n" +
      "• تنظيف آمن بخطوتين مع معاينة الصور المصغرة وتأكيد نظام أندرويد الرسمي.\n" +
      "• كشف الملفات المكررة بدقة SHA-256 مع حماية النسخة الأصلية دائماً.\n" +
      "• العثور على الملفات الكبيرة (>10 MB) والوسائط القديمة (+30 يوماً).\n" +
      "• خصوصية 100% على الجهاز: لا يتم رفع أي صور أو ملفات إلى السحابة أبداً.",
  },
  "ru-RU": {
    title: "Midiastore",
    shortDescription: "Легкий клинер (< 8 МБ): точные дубликаты SHA-256 и большие видео. Локально.",
    fullDescription:
      "Не хватает памяти на Android? Midiastore — это легкий (менее 8 МБ) и прозрачный очиститель хранилища, работающий 100% локально.\n\n" +
      "ПРЕИМУЩЕСТВА:\n" +
      "• Менее 8 МБ (< 8 МБ): быстрая установка даже при заполненной памяти.\n" +
      "• Очистка в 2 касания с реальными миниатюрами и официальным подтверждением Android.\n" +
      "• Точные дубликаты SHA-256: побайтовая проверка с защитой оригинала.\n" +
      "• Крупные файлы (>10 МБ) и старые медиафайлы (+30 дней).\n" +
      "• 100% на устройстве (Scoped Storage): без отправки файлов в облако и без автоудаления.",
  },
  "id": {
    title: "Midiastore",
    shortDescription: "Pembersih ringan < 8 MB: duplikat SHA-256 akurat & video besar. 100% lokal.",
    fullDescription:
      "Memori Android penuh? Midiastore adalah pembersih penyimpanan yang ringan (< 8 MB), cepat, dan 100% privat di perangkat Anda.\n\n" +
      "KEUNGGULAN UTAMA:\n" +
      "• Di bawah 8 MB (< 8 MB): ringan tanpa bloatware.\n" +
      "• Pembersihan 2 Ketukan dengan pratinjau thumbnail asli dan dialog konfirmasi resmi Android.\n" +
      "• Duplikat SHA-256 Akurat: pemeriksaan byte-demi-byte dengan menjaga 1 file asli tetap aman.\n" +
      "• File Besar (>10 MB) & Media Lama (+30 hari).\n" +
      "• 100% Di Perangkat: foto dan video Anda tidak pernah diunggah ke cloud.",
  },
  "bn-BD": {
    title: "Midiastore",
    shortDescription: "< 8 MB হালকা ক্লিনার: সঠিক SHA-256 ডুপ্লিকেট ও বড় ভিডিও নিরাপদে পরিষ্কার।",
    fullDescription:
      "ফোনের স্টোরেজ কি ফুল দেখাচ্ছে? Midiastore < 8 MB আকারের দ্রুত এবং 100% প্রাইভেট Android স্টোরেজ ক্লিনার।\n\n" +
      "প্রধান বৈশিষ্ট্য:\n" +
      "• < 8 MB হালকা আকার: কম স্টোরেজেও সহজেই ইনস্টল হয়।\n" +
      "• ২ ট্যাপে আসল থাম্বনেইল দেখে নিরাপদ রিভিউ এবং Android কনফার্মেশন।\n" +
      "• সঠিক SHA-256 ডুপ্লিকেট শনাক্তকরণ (মূল ফাইল সুরক্ষিত রেখে)।\n" +
      "• বড় ফাইল (>10 MB) এবং পুরনো মিডিয়া (+30 দিন) দ্রুত খুঁজে বের করে।\n" +
      "• 100% অন-ডিভাইস: আপনার ছবি বা ভিডিও কখনোই ক্লাউডে আপলোড হয় না।",
  },
  "ko-KR": {
    title: "Midiastore",
    shortDescription: "< 8 MB 초경량 클리너: 정확한 SHA-256 중복 파일 및 대용량 동영상 안전 정리.",
    fullDescription:
      "저장공간 부족 알림이 뜨나요? Midiastore는 8MB 미만(< 8 MB) 초경량으로 기기 내부에서 100% 안전하게 작동하는 Android 저장공간 정리 앱입니다.\n\n" +
      "주요 특징:\n" +
      "• 8MB 미만(< 8 MB) 초경량 설계로 빠른 실행.\n" +
      "• 실제 썸네일을 확인하고 2번의 탭으로 안전하게 정리(Android 공식 삭제 확인 지원).\n" +
      "• 바이트 단위 SHA-256 정밀 중복 검사(원본 파일 자동 보호).\n" +
      "• 대용량 파일(>10MB) 및 오래된 미디어(+30일) 빠른 탐색.\n" +
      "• 100% 기기 내 처리(Scoped Storage): 클라우드 전송이나 임의 자동 삭제가 없습니다.",
  },
};

function b64u(input) {
  return Buffer.from(input).toString("base64url");
}

async function getAccessToken() {
  const key = JSON.parse(fs.readFileSync(keyPath, "utf8"));
  const now = Math.floor(Date.now() / 1000);
  const header = b64u(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = b64u(
    JSON.stringify({
      iss: key.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: "https://oauth2.googleapis.com/token",
      exp: now + 3600,
      iat: now,
    })
  );
  const sig = crypto.createSign("RSA-SHA256").update(`${header}.${payload}`).sign(key.private_key, "base64url");
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${header}.${payload}.${sig}`,
  });
  const data = await res.json();
  if (!res.ok || !data.access_token) {
    throw new Error(`OAuth failed (${res.status}): ${JSON.stringify(data)}`);
  }
  return data.access_token;
}

async function main() {
  for (const [lang, listing] of Object.entries(STORE_LISTINGS_14)) {
    const tLen = [...listing.title].length;
    const sLen = [...listing.shortDescription].length;
    if (tLen > 30) throw new Error(`[${lang}] title > 30 chars (${tLen}): ${listing.title}`);
    if (sLen > 80) throw new Error(`[${lang}] shortDescription > 80 chars (${sLen}): ${listing.shortDescription}`);
    if (lang !== "ru-RU" && /[\u0400-\u04FF]/.test(listing.shortDescription + listing.fullDescription)) {
      throw new Error(`[${lang}] Cyrillic characters detected in non-Russian listing!`);
    }
  }
  const token = await getAccessToken();
  const editRes = await fetch(`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: "{}",
  });
  const edit = await editRes.json();
  if (!editRes.ok || !edit.id) {
    throw new Error(`Create edit failed (${editRes.status}): ${JSON.stringify(edit)}`);
  }
  console.log(`Edit criado: ${edit.id}`);

  let totalUploaded = 0;
  let totalFeatureGraphics = 0;
  for (const [lang, listing] of Object.entries(STORE_LISTINGS_14)) {
    // 1. Upsert store listing text
    const listRes = await fetch(
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
    if (!listRes.ok) {
      throw new Error(`Falha listing ${lang}: HTTP ${listRes.status} ${await listRes.text()}`);
    }

    // 1b. Update 512x512 app icon if docs/icon-512.png exists
    const iconPath = path.join(projectRoot, "docs", "icon-512.png");
    if (fs.existsSync(iconPath)) {
      await fetch(
        `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/icon`,
        { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
      );
      const icRes = await fetch(
        `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/icon?uploadType=media`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}`, "Content-Type": "image/png" },
          body: fs.readFileSync(iconPath),
        }
      );
      if (!icRes.ok) {
        console.warn(`Aviso icon ${lang}: HTTP ${icRes.status} ${await icRes.text()}`);
      }
    }

    if (process.argv.includes("--skip-screenshots")) {
      console.log(`${lang}: title='${listing.title}' + icon 512x512 atualizados`);
      continue;
    }

    const dir = path.join(projectRoot, "docs", "campaigns", "store-screenshots", lang);
    if (!fs.existsSync(dir)) {
      throw new Error(`Pasta ausente para ${lang}: ${dir}`);
    }

    // 2. Upload 1024x500 featureGraphic
    const fgPath = path.join(dir, "feature_graphic.png");
    if (!fs.existsSync(fgPath)) {
      throw new Error(`feature_graphic.png ausente para ${lang}: ${fgPath}`);
    }
    await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/featureGraphic`,
      { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
    );
    const fgRes = await fetch(
      `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/featureGraphic?uploadType=media`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "image/png" },
        body: fs.readFileSync(fgPath),
      }
    );
    if (fgRes.ok) {
      totalFeatureGraphics += 1;
    } else {
      throw new Error(`Falha upload featureGraphic ${lang}: HTTP ${fgRes.status} ${await fgRes.text()}`);
    }

    // 3. Upload 4 phoneScreenshots (1080x1920) — filter strictly /^0[1-4]_.*\.png$/i
    const pngs = fs
      .readdirSync(dir)
      .filter((f) => /^0[1-4]_.*\.png$/i.test(f))
      .sort();
    if (pngs.length !== 4) {
      throw new Error(`Esperados 4 slides 01_..04_ em ${lang}, encontrados ${pngs.length}: ${pngs.join(", ")}`);
    }

    await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/phoneScreenshots`,
      { method: "DELETE", headers: { Authorization: `Bearer ${token}` } }
    );

    let uploaded = 0;
    for (const file of pngs) {
      const scRes = await fetch(
        `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${edit.id}/listings/${lang}/phoneScreenshots?uploadType=media`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}`, "Content-Type": "image/png" },
          body: fs.readFileSync(path.join(dir, file)),
        }
      );
      if (scRes.ok) {
        uploaded += 1;
        totalUploaded += 1;
      } else {
        throw new Error(`Falha upload ${lang}/${file}: HTTP ${scRes.status} ${await scRes.text()}`);
      }
    }
    console.log(`${lang}: listing + icon + featureGraphic (1024x500) + ${uploaded}/${pngs.length} phoneScreenshots enviados`);
  }

  const commitRes = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/edits/${edit.id}:commit`,
    {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }
  );
  const commitText = await commitRes.text();
  console.log(
    `Commit Play Console (${totalUploaded} screenshots + ${totalFeatureGraphics} featureGraphics em 14 idiomas): HTTP ${commitRes.status} ${commitText}`
  );
  if (!commitRes.ok) {
    throw new Error(`Commit failed: ${commitText}`);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
