/**
 * Gerador Node.js (Zero Python) de Trilhas de Visemas F1/F2 em 60 FPS
 * para os 8 áudios neurais fixos da Assistente Maria no Midiastore Android.
 *
 * Gera: app/src/main/java/com/rdf112/sweep/presentation/ui/MariaLipSyncEnvelopes.kt
 * Formato compacto: 1 caractere ('0'..'8') por frame de 16.67ms (60 FPS) -> O(1) lookup, zero split().
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { decodeToPcm16k, analyzePhoneticVisemes } from "./lib/phonetic-viseme-analyzer.mjs";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, "..");

const RAW_DIR = path.join(projectRoot, "app", "src", "main", "res", "raw");
const OUT_KT = path.join(
  projectRoot,
  "app",
  "src",
  "main",
  "java",
  "com",
  "rdf112",
  "sweep",
  "presentation",
  "ui",
  "MariaLipSyncEnvelopes.kt"
);

const AUDIOS = [
  { key: "CLEAN", res: "maria_clean" },
  { key: "FILES", res: "maria_files" },
  { key: "PRO", res: "maria_pro" },
  { key: "DUP", res: "maria_dup" },
  { key: "VIDEOS", res: "maria_videos" },
  { key: "WHATSAPP", res: "maria_whatsapp" },
  { key: "SCREENSHOTS", res: "maria_screenshots" },
  { key: "NANO", res: "maria_nano" },
];

const FPS = 60;
const results = [];
let totalChars = 0;

for (const item of AUDIOS) {
  const mp3Path = path.join(RAW_DIR, `${item.res}.mp3`);
  if (!fs.existsSync(mp3Path)) {
    throw new Error(`Áudio não encontrado: ${mp3Path}`);
  }
  const { samples, sampleRate } = decodeToPcm16k(mp3Path);
  const analysis = analyzePhoneticVisemes(samples, sampleRate, {
    fps: FPS,
    maxLevel: 7,
    includeSmile: true,
  });

  const trackStr = Array.from(analysis.visemes, (v) => String(v)).join("");
  totalChars += trackStr.length;

  const pct = (codes) => {
    const sum = codes.reduce((acc, c) => acc + (analysis.histogram[c] || 0), 0);
    return ((sum / analysis.frameCount) * 100).toFixed(1);
  };

  console.log(
    `[60 FPS F1/F2] ${item.res.padEnd(18)} | ${String(analysis.durationMs).padStart(5)}ms | ${String(analysis.frameCount).padStart(4)} frames | ` +
      `fechada(0)=${pct([0])}% sib(1-2)=${pct([1, 2])}% med(3-4)=${pct([3, 4])}% abert(5-7)=${pct([5, 6, 7])}% smile(8)=${pct([8])}%`
  );

  results.push({ ...item, trackStr, durationMs: analysis.durationMs, frameCount: analysis.frameCount });
}

const ktLines = [
  "package com.rdf112.sweep.presentation.ui",
  "",
  "import com.rdf112.sweep.R",
  "",
  "/**",
  " * Trilhas Fonético-Visêmicas F1/F2 pré-calculadas em 60 FPS (passo 16.67ms) via Node.js",
  " * (`scripts/generate-maria-visemes.mjs`, zero Python) para os 8 áudios neurais fixos da Maria.",
  " *",
  " * Cada caractere representa 1 frame a 60 FPS (lookup O(1), sem alocação de CSV/split):",
  " * - '0' = maria_lip_0 (gate de silêncio rígido entre palavras + oclusões bilabiais P/B/M)",
  " * - '1' = maria_lip_1 (transição suave / nasais)",
  " * - '2' = maria_lip_2 (sibilantes S/Z/F/V + vogal frontal fechada I)",
  " * - '3' = maria_lip_3 (vogais médias-fechadas E/Ê + líquidas L/R/N)",
  " * - '4' = maria_lip_4 (vogais arredondadas O/Ô/U)",
  " * - '5' = maria_lip_5 (vogais abertas médias A/É/Ó)",
  " * - '6' = maria_lip_6 (vogais abertas tônicas)",
  " * - '7' = maria_lip_7 (pico tônico de abertura máxima)",
  " * - '8' = maria_smile (sorriso acolhedor em pausas longas > 320ms e início/fim)",
  " */",
  "object MariaLipSyncEnvelopes {",
  `    const val FPS: Int = ${FPS}`,
  "",
  ...results.map((r) => `    private const val ${r.key} = "${r.trackStr}"`),
  "",
  "    fun trackForAudioRes(resId: Int): String = when (resId) {",
  ...results.map((r) => `        R.raw.${r.res} -> ${r.key}`),
  "        else -> CLEAN",
  "    }",
  "",
  "    /**",
  "     * Retorna o visema exato para o instante `posMs` (em milissegundos) na trilha de 60 FPS.",
  "     */",
  "    fun visemeAtMs(track: String, posMs: Int): Int {",
  "        if (track.isEmpty()) return 0",
  "        val idx = ((posMs.coerceAtLeast(0).toLong() * FPS) / 1000L).toInt().coerceIn(0, track.lastIndex)",
  "        return (track[idx] - '0').coerceIn(0, 8)",
  "    }",
  "}",
  "",
];

fs.writeFileSync(OUT_KT, ktLines.join("\n"), "utf8");
console.log(`\nGerado com sucesso: ${OUT_KT} (${totalChars} chars totais, ${(Buffer.byteLength(ktLines.join("\n")) / 1024).toFixed(2)} KB)`);
