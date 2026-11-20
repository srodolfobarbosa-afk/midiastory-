#!/usr/bin/env node
/**
 * FÁBRICA DIÁRIA DE CONTEÚDO VIRAL — MIDIASTORE (Módulo 5B)
 *
 * Gera pacotes diários prontos para gravação/edição rápida (Reels, TikTok, Shorts)
 * em PT-BR, EN-US e ES-419 (20–30s: Gancho visual nos 3s + Dor real + Prova real 5,9 MB/SHA-256 + CTA),
 * salvando em marketing/daily-scripts/YYYY-MM-DD/.
 *
 * Uso:
 *   node scripts/viral-content-factory.mjs [--date 2026-11-19]
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, "..");

const PLAY_URL = "https://play.google.com/store/apps/details?id=com.rdf112.midiastore";

const DAILY_BLUEPRINTS = [
  {
    id: "01_whatsapp_hidden_gb",
    theme: "WhatsApp Hidden Gigabytes + Exact SHA-256 Duplicates",
    scripts: {
      "pt-BR": {
        title: "Seu celular não está velho — tem 4 GB escondidos em vídeos repetidos",
        durationSec: 26,
        hook3s: "Para de apagar foto importante achando que vai liberar espaço — olha o que realmente tá comendo sua memória.",
        visualHook: "Close na tela de aviso 'Armazenamento quase cheio' → corte seco (0.8s) abrindo o Midiastore OLED preto puro.",
        beats: [
          "00-03s [GANCHO]: 'Para de apagar foto importante para liberar espaço no Android.'",
          "03-10s [DOR]: 'O que lota seu celular não são suas fotos — são vídeos gigantes encaminhados em grupos e cópias idênticas escondidas.'",
          "10-21s [PROVA REAL]: Gravar tela do Moto G84 tocando em 'Analisar meu espaço' → barra segmentada + lista de Duplicatas SHA-256 + Arquivos Grandes (apenas 5,9 MB, 100% offline, sem apagar nada sozinho).",
          "21-26s [CTA]: 'Você revisa tudo antes de apagar. O link do Midiastore (5,9 MB) tá na bio / comentário fixado.'",
        ],
        caption: "Celular travando com memória cheia? Em vez de apagar lembranças ou instalar limpador pesado de 100 MB cheio de anúncio, testa o Midiastore: apenas 5,9 MB, 100% no aparelho e verificação SHA-256 real. Baixe na Google Play: " + PLAY_URL,
        hashtags: ["#android", "#dicasdecelular", "#memoriacheia", "#whatsapp", "#tecnologia", "#midiastore"],
      },
      "en-US": {
        title: "Stop deleting memories — find exact duplicate GBs in 15 seconds",
        durationSec: 25,
        hook3s: "Your Android isn't out of space because of your photos — it's hiding gigabytes of exact duplicate videos.",
        visualHook: "Phone showing 'Storage space running out' notification → snappy cut to pure OLED black Midiastore scan.",
        beats: [
          "00-03s [HOOK]: 'Stop uninstalling apps because your Android says Storage Full.'",
          "03-10s [PAIN]: 'Most cleaner apps are 100 MB of bloatware and fake RAM warnings that never show what's actually taking space.'",
          "10-20s [REAL PROOF]: Screen recording of Midiastore (5.9 MB, 100% on-device) running exact SHA-256 duplicate scan + large video review — zero auto-delete, official Android confirmation.",
          "20-25s [CTA]: '5.9 MB, zero bloatware, 100% private. Search Midiastore on Google Play or tap the link in bio.'",
        ],
        caption: "Tired of bloated phone cleaners full of fake alerts? Midiastore is 5.9 MB, pure OLED dark, runs 100% on-device, and uses exact SHA-256 hashing so you never delete the wrong photo. Get it on Google Play: " + PLAY_URL,
        hashtags: ["#androidtips", "#storagefull", "#techtok", "#phonehacks", "#privacy", "#midiastore"],
      },
      "es-419": {
        title: "Deja de borrar fotos importantes — recupera gigas en 15 segundos",
        durationSec: 26,
        hook3s: "No borres tus fotos cuando tu Android diga memoria llena — mira dónde están escondidos tus gigas.",
        visualHook: "Pantalla con alerta de 'Almacenamiento casi lleno' → corte rápido a la interfaz OLED negra de Midiastore.",
        beats: [
          "00-03s [GANCHO]: 'Deja de desinstalar apps porque tu Android se quedó sin espacio.'",
          "03-10s [DOLOR]: 'Los videos reenviados de WhatsApp y archivos duplicados se comen tu memoria sin que te des cuenta.'",
          "10-21s [PRUEBA REAL]: Grabación real mostrando Midiastore (solo 5,9 MB, 100% local) detectando duplicados exactos con SHA-256 y archivos gigantes — tú revisas todo antes de borrar.",
          "21-26s [CTA]: 'Solo pesa 5,9 MB y no sube tus fotos a la nube. Descarga Midiastore gratis en Google Play.'",
        ],
        caption: "¿Celular lento y sin espacio? Prueba Midiastore: solo 5,9 MB, 100% privado en tu dispositivo y detección exacta SHA-256 sin borrar nada sin tu permiso. Google Play: " + PLAY_URL,
        hashtags: ["#android", "#trucosandroid", "#memoriallena", "#whatsapp", "#tecnologia", "#midiastore"],
      },
    },
  },
  {
    id: "02_anti_bloatware_5_9mb",
    theme: "5.9 MB Cleaner vs 120 MB Ad-Filled Cleaners (Competitor Pain Attack)",
    scripts: {
      "pt-BR": {
        title: "Por que limpadores de 100 MB pioram seu celular (e a alternativa de 5,9 MB)",
        durationSec: 24,
        hook3s: "A ironia de baixar um 'limpador de celular' de 120 MB quando você já está sem espaço...",
        visualHook: "Texto grande na tela: 'Limpador comum: 120 MB + anúncios' vs 'Midiastore: 5,9 MB + 100% offline'.",
        beats: [
          "00-03s [GANCHO]: 'Baixar um limpador de 120 MB cheio de anúncio quando o celular tá lotado não faz sentido.'",
          "03-10s [CONSTRASTE]: 'A maioria promete milagre falso e ainda apaga coisa sem avisar.'",
          "10-19s [PROVA REAL]: Mostrar tamanho real do Midiastore (5,9 MB), 3 abas limpas (Limpar, Arquivos, Pro) e confirmação nativa do Android antes de apagar qualquer mídia.",
          "19-24s [CTA]: 'Leve, direto e transparente. Procura Midiastore na Google Play.'",
        ],
        caption: "Um limpador de armazenamento precisa ser leve de verdade. O Midiastore tem 5,9 MB, visual OLED escuro, zero bloatware e revisão humana em cada limpeza. Google Play: " + PLAY_URL,
        hashtags: ["#androidbrasil", "#celularlento", "#produtividade", "#appsandroid", "#midiastore"],
      },
      "en-US": {
        title: "Why a 120 MB phone cleaner makes zero sense (try 5.9 MB instead)",
        durationSec: 24,
        hook3s: "The irony of downloading a 120 MB 'storage cleaner' when your phone has zero space left...",
        visualHook: "Split caption on screen: 'Typical cleaner: 120 MB + scare ads' vs 'Midiastore: 5.9 MB + local SHA-256'.",
        beats: [
          "00-03s [HOOK]: 'Downloading a 120 MB cleaner when your phone is already full makes zero sense.'",
          "03-10s [CONTRAST]: 'No fake CPU cooling, no shady background permissions — just real storage cleanup.'",
          "10-19s [REAL PROOF]: Show Midiastore's 3-tab OLED interface (Clean, Files, Pro), exact SHA-256 duplicates, and Android's official delete confirmation.",
          "19-24s [CTA]: '5.9 MB on Google Play. Search Midiastore and reclaim your storage today.'",
        ],
        caption: "A storage cleaner shouldn't bloat your phone. Midiastore is 5.9 MB, 100% on-device, and built on Android Scoped Storage with zero auto-deletions. Google Play: " + PLAY_URL,
        hashtags: ["#android", "#minimaltech", "#storagecleaner", "#smartphone", "#midiastore"],
      },
      "es-419": {
        title: "La ironía de bajar un limpiador de 120 MB cuando no tienes espacio",
        durationSec: 24,
        hook3s: "La ironía de descargar un 'limpiador' de 120 MB cuando tu celular ya no tiene espacio...",
        visualHook: "Comparativa visual limpia: 'Limpiador típico: 120 MB' vs 'Midiastore: 5,9 MB'.",
        beats: [
          "00-03s [GANCHO]: '¿Por qué instalar un limpiador gigante lleno de anuncios cuando te falta memoria?'",
          "03-10s [CONTRASTE]: 'Midiastore pesa solo 5,9 MB, no promete milagros falsos y respeta tus archivos.'",
          "10-19s [PRUEBA REAL]: Mostrar las 3 pestañas limpias (Limpiar, Archivos, Pro) y limpieza confirmada por Android.",
          "19-24s [CTA]: 'Búscalo como Midiastore en Google Play.'",
        ],
        caption: "Ligero (5,9 MB), oscuro OLED y 100% local. Recupera espacio real en Android sin bloatware con Midiastore: " + PLAY_URL,
        hashtags: ["#android", "#appsrecomendadas", "#celulares", "#espacio", "#midiastore"],
      },
    },
  },
];

function renderMarkdown(dateStr, blueprint) {
  const sections = Object.entries(blueprint.scripts)
    .map(([lang, s]) => {
      const beatsMd = s.beats.map((b) => `- ${b}`).join("\n");
      return `## [${lang}] ${s.title} (${s.durationSec}s)

- **Gancho 3s (Curiosity Gap):** ${s.hook3s}
- **Direção Visual (0–3s):** ${s.visualHook}

### Roteiro Segundo a Segundo
${beatsMd}

### Legenda Pronta
> ${s.caption}

### Hashtags
\`${s.hashtags.join(" ")}\`
`;
    })
    .join("\n---\n\n");

  return `# 🎬 Pacote Viral Midiastore — ${dateStr} — ${blueprint.theme}

${sections}`;
}

export function generateDailyViralPack(dateStr = new Date().toISOString().slice(0, 10)) {
  const outDir = path.join(projectRoot, "marketing", "daily-scripts", dateStr);
  fs.mkdirSync(outDir, { recursive: true });

  const files = [];
  for (const bp of DAILY_BLUEPRINTS) {
    const mdPath = path.join(outDir, `${bp.id}.md`);
    fs.writeFileSync(mdPath, renderMarkdown(dateStr, bp), "utf8");
    files.push(mdPath);
  }

  const manifestPath = path.join(outDir, "manifest.json");
  fs.writeFileSync(
    manifestPath,
    JSON.stringify(
      {
        date: dateStr,
        generatedAt: new Date().toISOString(),
        languages: ["pt-BR", "en-US", "es-419"],
        packsCount: DAILY_BLUEPRINTS.length,
        totalScripts: DAILY_BLUEPRINTS.length * 3,
        files: files.map((f) => path.basename(f)),
      },
      null,
      2,
    ),
    "utf8",
  );

  return { outDir, manifestPath, packsCount: DAILY_BLUEPRINTS.length, totalScripts: DAILY_BLUEPRINTS.length * 3 };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const dateIdx = process.argv.indexOf("--date");
  const dateStr = dateIdx > -1 ? process.argv[dateIdx + 1] : new Date().toISOString().slice(0, 10);
  const res = generateDailyViralPack(dateStr);
  console.log("🎬 FÁBRICA DIÁRIA DE CONTEÚDO VIRAL CONCLUÍDA:");
  console.log(JSON.stringify(res, null, 2));
}
