#!/usr/bin/env node
/**
 * GERADOR & VALIDADOR DE ASO MULTI-IDIOMA — MIDIASTORE (Módulo 5C)
 *
 * Constrói e valida matematicamente (limites oficiais Google Play: título <= 30 chars,
 * descrição curta <= 80 chars) os textos de alta conversão baseados em mineração ética
 * das maiores dores de 1–2 estrelas de concorrentes (anúncios invasivos, exclusão sem
 * permissão, app pesado/bloatware, privacidade).
 *
 * Saídas:
 *   marketing/aso/global-aso-copy-pack.json
 *   marketing/aso/GLOBAL_ASO_COPY_PACK.md
 */

import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const COMPETITOR_PAINS_ADDRESSED = [
  "Anúncios invasivos a cada toque -> UX limpa OLED sem interrupções agressivas",
  "Apagou fotos sem permissão -> Revisão humana + diálogo oficial Android MediaStore",
  "App pesado/bloatware (80-150 MB) -> Apenas 5,9 MB otimizados com R8",
  "Medo de privacidade/nuvem -> Varredura e hash SHA-256 100% on-device",
];

const ASO_PACK = {
  "pt-BR": {
    title: "Midiastore: Limpar Memória",
    shortDescription: "Libere espaço com segurança (5,9 MB): duplicatas SHA-256 e vídeos grandes.",
    paywallHeadline: "Desbloqueie limpezas ilimitadas e controle total do seu espaço",
    paywallBullets: [
      "Varredura global completa de duplicatas exatas (SHA-256)",
      "Filtros avançados de vídeos gigantes e capturas antigas (>30 dias)",
      "100% privado no seu aparelho — zero nuvem, zero bloatware",
    ],
    longDescription:
      "Celular avisando 'Armazenamento cheio'? O Midiastore é um limpador de armazenamento puro, leve (apenas 5,9 MB) e 100% privado.\n\nPor que escolher o Midiastore:\n• Apenas 5,9 MB: cabe até em aparelhos quase sem espaço.\n• Duplicatas Exatas (SHA-256): verifica cópias byte a byte para você manter sempre o original.\n• Arquivos Grandes e Capturas Antigas: encontre rapidamente vídeos esquecidos e prints antigos.\n• Controle Total e Transparência: nenhum arquivo é excluído automaticamente — você revisa tudo e aprova no diálogo oficial do Android.\n• 100% On-Device: suas fotos, vídeos e áudios nunca saem do seu celular.",
  },
  "en-US": {
    title: "Midiastore: Storage Cleaner",
    shortDescription: "Fast 5.9 MB cleaner: exact SHA-256 duplicates & large videos. 100% private.",
    paywallHeadline: "Unlock unlimited cleanups and full storage control",
    paywallBullets: [
      "Full-library exact SHA-256 duplicate verification",
      "Fast review of oversized videos and old screenshots (>30 days)",
      "5.9 MB, pure OLED dark UI, 100% on-device privacy",
    ],
    longDescription:
      "Seeing 'Storage space running out'? Midiastore is a focused, lightweight (5.9 MB) Android storage cleaner built for speed and privacy.\n\nWhy Midiastore:\n• Ultra-lightweight (5.9 MB): installs fast even when storage is nearly full.\n• Exact SHA-256 Duplicates: proves byte-for-byte identical copies before you delete.\n• Large Videos & Old Screenshots: surface the biggest space hogs in seconds.\n• Zero Auto-Delete: you review every candidate and confirm via Android's official system dialog.\n• 100% On-Device: built on Android Scoped Storage — your media never leaves your phone.",
  },
  "es-419": {
    title: "Midiastore: Limpiar Memoria",
    shortDescription: "Limpiador ligero (5,9 MB): duplicados SHA-256 y videos grandes. 100% local.",
    paywallHeadline: "Desbloquea limpiezas ilimitadas y control total de tu memoria",
    paywallBullets: [
      "Detección exacta de duplicados por SHA-256 en toda tu galería",
      "Revisión rápida de videos gigantes y capturas antiguas (>30 días)",
      "Solo 5,9 MB, diseño OLED oscuro y 100% privado en tu teléfono",
    ],
    longDescription:
      "¿Tu Android dice 'Almacenamiento lleno'? Midiastore es un limpiador de almacenamiento puro, rápido (solo 5,9 MB) y 100% privado.\n\nLo que hace diferente a Midiastore:\n• Solo 5,9 MB: sin bloatware ni promesas falsas.\n• Duplicados Exactos (SHA-256): detecta copias idénticas byte a byte.\n• Videos Grandes y Capturas Antiguas: recupera gigabytes en minutos.\n• Control Total: nunca borra archivos automáticamente; siempre revisas y confirmas con Android.\n• 100% Local: tus fotos y videos nunca se suben a la nube.",
  },
  "hi-IN": {
    title: "Midiastore: स्टोरेज क्लीनर",
    shortDescription: "5.9 MB हल्का क्लीनर: SHA-256 डुप्लीकेट और बड़े वीडियो सुरक्षित हटाएं।",
    paywallHeadline: "अनलिमिटेड क्लीनअप और स्टोरेज पर पूरा नियंत्रण पाएं",
    paywallBullets: [
      "पूरी गैलरी में सटीक SHA-256 डुप्लीकेट जांच",
      "बड़े वीडियो और पुराने स्क्रीनशॉट की तेज़ पहचान",
      "सिर्फ 5.9 MB, 100% ऑफ़लाइन और पूरी तरह सुरक्षित",
    ],
    longDescription:
      "क्या आपका Android फ़ोन बार-बार 'Storage Full' दिखा रहा है? Midiastore सिर्फ 5.9 MB का हल्का, तेज़ और 100% प्राइवेट स्टोरेज क्लीनर है।\n\nमुख्य विशेषताएं:\n• सिर्फ 5.9 MB: कम स्टोरेज में भी आसानी से इंस्टॉल होता है।\n• सटीक SHA-256 डुप्लीकेट: केवल बिल्कुल एक जैसी कॉपी दिखाता है।\n• बड़े वीडियो और पुराने स्क्रीनशॉट तुरंत खोजें।\n• आपकी अनुमति के बिना कुछ भी डिलीट नहीं होता।\n• 100% ऑन-डिवाइस स्कैन: आपकी फ़ोटो कभी सर्वर पर अपलोड नहीं होतीं।",
  },
  "de-DE": {
    title: "Midiastore: Speicher Reiniger",
    shortDescription: "Nur 5,9 MB: Exakte SHA-256 Duplikate & große Videos sicher löschen. Lokal.",
    paywallHeadline: "Unbegrenzte Bereinigungen & volle Speicherkontrolle",
    paywallBullets: [
      "Exakte SHA-256 Duplikaterkennung auf dem gesamten Gerät",
      "Große Videos und alte Screenshots (>30 Tage) sofort prüfen",
      "Nur 5,9 MB, OLED-Design, 100 % lokal ohne Cloud-Upload",
    ],
    longDescription:
      "Android-Speicher voll? Midiastore ist ein schlanker (5,9 MB), privater Speicherreiniger ohne Bloatware.\n\nVorteile:\n• Nur 5,9 MB klein und blitzschnell.\n• Exakte SHA-256 Duplikatprüfung statt unsicherer Schätzungen.\n• Große Videos & alte Screenshots übersichtlich prüfen.\n• Kein automatisches Löschen: Jede Löschung erfolgt erst nach Ihrer Bestätigung im Android-Systemdialog.\n• 100 % lokal auf Ihrem Gerät.",
  },
  "fr-FR": {
    title: "Midiastore: Nettoyeur Stockage",
    shortDescription: "Léger (5,9 Mo) : doublons SHA-256 et grosses vidéos. 100 % privé sur mobile.",
    paywallHeadline: "Débloquez le nettoyage illimité et le contrôle total",
    paywallBullets: [
      "Détection exacte des doublons par SHA-256 sur tout l'appareil",
      "Repérage rapide des grosses vidéos et anciennes captures (>30 j)",
      "5,9 Mo, interface OLED noire, 100 % hors ligne et privé",
    ],
    longDescription:
      "Stockage Android saturé ? Midiastore est un nettoyeur de stockage léger (5,9 Mo), rapide et 100 % privé.\n\nPoints forts :\n• Seulement 5,9 Mo : sans bloatware.\n• Doublons exacts (SHA-256) : comparaison fiable octet par octet.\n• Grosses vidéos et anciennes captures identifiées en quelques secondes.\n• Aucune suppression automatique : vous vérifiez et validez via le dialogue officiel Android.\n• 100 % local : vos médias ne quittent jamais votre téléphone.",
  },
};

export function generateAsoCopyPack() {
  const validation = [];
  for (const [locale, data] of Object.entries(ASO_PACK)) {
    const titleLen = [...data.title].length;
    const shortLen = [...data.shortDescription].length;
    assert.ok(titleLen <= 30, `[${locale}] Título excede 30 chars (${titleLen}): ${data.title}`);
    assert.ok(shortLen <= 80, `[${locale}] Descrição curta excede 80 chars (${shortLen}): ${data.shortDescription}`);
    validation.push({ locale, titleLen, shortLen, compliant: true });
  }

  const outDir = path.join(projectRoot, "marketing", "aso");
  fs.mkdirSync(outDir, { recursive: true });

  const jsonPath = path.join(outDir, "global-aso-copy-pack.json");
  fs.writeFileSync(
    jsonPath,
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        competitorPainsAddressed: COMPETITOR_PAINS_ADDRESSED,
        validation,
        locales: ASO_PACK,
      },
      null,
      2,
    ),
    "utf8",
  );

  const mdLines = [
    "# 🌍 Pacote Global ASO & Copy de Conversão — Midiastore",
    "",
    "## Dores de Concorrentes (1–2★) Atacadas",
    ...COMPETITOR_PAINS_ADDRESSED.map((p) => `- ${p}`),
    "",
    "## Validação Oficial Google Play (Título ≤ 30 | Curta ≤ 80)",
    "| Idioma | Título (chars) | Descrição Curta (chars) | Status |",
    "| :--- | :---: | :---: | :---: |",
    ...validation.map((v) => `| \`${v.locale}\` | ${v.titleLen}/30 | ${v.shortLen}/80 | ✅ |`),
    "",
    ...Object.entries(ASO_PACK).flatMap(([loc, d]) => [
      `## ${loc}`,
      `- **Título:** \`${d.title}\``,
      `- **Descrição Curta:** \`${d.shortDescription}\``,
      `- **Paywall Headline:** ${d.paywallHeadline}`,
      ...d.paywallBullets.map((b) => `  - ${b}`),
      "",
      "```text",
      d.longDescription,
      "```",
      "",
    ]),
  ];

  const mdPath = path.join(outDir, "GLOBAL_ASO_COPY_PACK.md");
  fs.writeFileSync(mdPath, mdLines.join("\n"), "utf8");

  return { jsonPath, mdPath, localesValidated: validation.length, validation };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const res = generateAsoCopyPack();
  console.log("🌍 PACOTE ASO MULTI-IDIOMA GERADO E VALIDADO:");
  console.log(JSON.stringify(res, null, 2));
}
