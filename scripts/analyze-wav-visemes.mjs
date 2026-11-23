/**
 * CLI ultrarrápido (Node.js puro, zero Python) para o Widget Desktop FRIDAY/Maria.
 * Recebe um arquivo WAV PCM e devolve uma string compacta de 60 FPS (hex '0'..'f', 16 níveis)
 * com Análise Fonético-Visêmica F1/F2 + gate de silêncio rígido para fechar a boca imediatamente entre palavras.
 *
 * Uso: node scripts/analyze-wav-visemes.mjs <caminho.wav>
 */

import { decodeToPcm16k, analyzePhoneticVisemes } from "./lib/phonetic-viseme-analyzer.mjs";

const wavPath = process.argv[2];
if (!wavPath) {
  process.exit(1);
}

try {
  const { samples, sampleRate } = decodeToPcm16k(wavPath);
  const res = analyzePhoneticVisemes(samples, sampleRate, {
    fps: 60,
    maxLevel: 15,
    includeSmile: false,
  });

  // Emite 1 caractere hex ('0'..'f') por frame de 16.67ms (60 FPS)
  const hex = Array.from(res.visemes, (v) => v.toString(16)).join("");
  process.stdout.write(hex);
} catch {
  process.exit(1);
}
