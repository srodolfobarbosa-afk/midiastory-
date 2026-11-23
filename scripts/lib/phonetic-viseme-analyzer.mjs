/**
 * Motor Fonético-Visêmico F1/F2 em 60 FPS (100% Node.js, Zero Python)
 *
 * Analisa áudio PCM 16-bit mono (16 kHz ou nativo) usando:
 * - Pré-ênfase de primeira ordem (0.97) para destacar formantes vocais
 * - Janela Hann + FFT Radix-2 de 512 pontos (resolução ~31.25 Hz @ 16 kHz)
 * - Estimativa real de Formante F1 (250-950 Hz: abertura mandibular)
 *   e Formante F2 (800-2800 Hz: frontalidade vs arredondamento)
 * - Razão de energia fricativa (3200-7500 Hz) + Taxa de Cruzamento por Zero (ZCR)
 *   para detectar sibilantes (S/Z/X/CH/F/V -> visema 2, nunca boca escancarada)
 * - Gate de silêncio rígido com hysteresis rápida para fechar a boca IMEDIATAMENTE
 *   entre palavras e em oclusões bilabiais (P/B/M -> visema 0)
 *
 * Mapeamento de visemas (escala 0..8 para Maria Android):
 *   0 = Boca fechada (silêncio, pausa entre palavras, bilabiais P/B/M)
 *   1 = Quase fechada (transição suave / nasais / labiodentais leves)
 *   2 = Sibilantes & vogais fechadas frontais (S/Z/F/V/I)
 *   3 = Vogais médias-fechadas / líquidas (E/Ê/L/R/N)
 *   4 = Vogais arredondadas médias (O/Ô/U forte)
 *   5 = Vogais abertas médias (A/É/Ó)
 *   6 = Vogais abertas tônicas (A/Á forte)
 *   7 = Pico de abertura máxima (vogal aberta tônica intensa)
 *   8 = Sorriso acolhedor (apenas silêncio inicial/final ou pausa longa de frase > 320ms)
 */

import { execFileSync } from "node:child_process";
import fs from "node:fs";

const FFT_SIZE = 512;
const DEFAULT_FFMPEG = "C:\\Users\\rodolfo\\tools\\ffmpeg\\bin\\ffmpeg.exe";

// Pré-calcula janela Hann e tabelas seno/cosseno para FFT Radix-2 ultrarrápida
const HANN_WINDOW = new Float32Array(FFT_SIZE);
for (let i = 0; i < FFT_SIZE; i++) {
  HANN_WINDOW[i] = 0.5 * (1 - Math.cos((2 * Math.PI * i) / (FFT_SIZE - 1)));
}

const BIT_REV = new Uint16Array(FFT_SIZE);
{
  const bits = Math.log2(FFT_SIZE);
  for (let i = 0; i < FFT_SIZE; i++) {
    let rev = 0;
    let x = i;
    for (let b = 0; b < bits; b++) {
      rev = (rev << 1) | (x & 1);
      x >>= 1;
    }
    BIT_REV[i] = rev;
  }
}

const COS_TABLE = new Float32Array(FFT_SIZE / 2);
const SIN_TABLE = new Float32Array(FFT_SIZE / 2);
for (let i = 0; i < FFT_SIZE / 2; i++) {
  const angle = (-2 * Math.PI * i) / FFT_SIZE;
  COS_TABLE[i] = Math.cos(angle);
  SIN_TABLE[i] = Math.sin(angle);
}

/**
 * FFT Radix-2 in-place (512 pontos) -> retorna array de magnitudes (256 bins)
 */
function computeMagnitudeSpectrum(frame, re, im, mag) {
  for (let i = 0; i < FFT_SIZE; i++) {
    const j = BIT_REV[i];
    re[j] = frame[i];
    im[j] = 0;
  }

  for (let size = 2; size <= FFT_SIZE; size <<= 1) {
    const half = size >> 1;
    const step = FFT_SIZE / size;
    for (let i = 0; i < FFT_SIZE; i += size) {
      let tableIdx = 0;
      for (let j = i; j < i + half; j++) {
        const k = j + half;
        const c = COS_TABLE[tableIdx];
        const s = SIN_TABLE[tableIdx];
        const tRe = re[k] * c - im[k] * s;
        const tIm = re[k] * s + im[k] * c;
        re[k] = re[j] - tRe;
        im[k] = im[j] - tIm;
        re[j] += tRe;
        im[j] += tIm;
        tableIdx += step;
      }
    }
  }

  const bins = FFT_SIZE >> 1;
  for (let i = 0; i < bins; i++) {
    mag[i] = Math.hypot(re[i], im[i]);
  }
}

/**
 * Decodifica qualquer áudio (MP3, WAV, M4A, OGG) para PCM Float32 mono @ 16 kHz via ffmpeg
 */
export function decodeToPcm16k(filePath, ffmpegBin = null) {
  // Fast-path para WAV PCM 16-bit sem precisar invocar ffmpeg
  if (filePath.toLowerCase().endsWith(".wav") && fs.existsSync(filePath)) {
    const parsed = tryReadWavPcm(filePath);
    if (parsed) return parsed;
  }

  const ff = ffmpegBin || (fs.existsSync(DEFAULT_FFMPEG) ? DEFAULT_FFMPEG : "ffmpeg");
  const pcmBuf = execFileSync(
    ff,
    ["-v", "error", "-i", filePath, "-ac", "1", "-ar", "16000", "-f", "s16le", "-"],
    { maxBuffer: 64 * 1024 * 1024 }
  );

  const sampleCount = Math.floor(pcmBuf.length / 2);
  const samples = new Float32Array(sampleCount);
  for (let i = 0; i < sampleCount; i++) {
    samples[i] = pcmBuf.readInt16LE(i * 2) / 32768.0;
  }
  return { samples, sampleRate: 16000 };
}

/**
 * Leitura nativa ultrarrápida de WAV PCM 16-bit (usada no widget desktop para <25ms latência)
 */
export function tryReadWavPcm(wavPath) {
  const buf = fs.readFileSync(wavPath);
  if (buf.length < 44) return null;
  if (buf.toString("ascii", 0, 4) !== "RIFF" || buf.toString("ascii", 8, 12) !== "WAVE") {
    return null;
  }

  let audioFormat = 1;
  let channels = 1;
  let sampleRate = 24000;
  let bitsPerSample = 16;
  let dataOffset = -1;
  let dataSize = 0;

  let pos = 12;
  while (pos + 8 <= buf.length) {
    const id = buf.toString("ascii", pos, pos + 4);
    const size = buf.readUInt32LE(pos + 4);
    if (id === "fmt ") {
      audioFormat = buf.readUInt16LE(pos + 8);
      channels = buf.readUInt16LE(pos + 10);
      sampleRate = buf.readUInt32LE(pos + 12);
      bitsPerSample = buf.readUInt16LE(pos + 22);
    } else if (id === "data") {
      dataOffset = pos + 8;
      dataSize = Math.min(size, buf.length - dataOffset);
      break;
    }
    pos += 8 + size + (size & 1);
  }

  if (audioFormat !== 1 || bitsPerSample !== 16 || dataOffset < 0 || dataSize <= 0) {
    return null;
  }

  const frameSize = channels * 2;
  const totalFrames = Math.floor(dataSize / frameSize);
  const mono = new Float32Array(totalFrames);
  for (let i = 0; i < totalFrames; i++) {
    let sum = 0;
    const base = dataOffset + i * frameSize;
    for (let ch = 0; ch < channels; ch++) {
      sum += buf.readInt16LE(base + ch * 2) / 32768.0;
    }
    mono[i] = sum / channels;
  }

  // Reamostra linearmente para 16000 Hz se necessário (mantém bins FFT idênticos!)
  if (sampleRate === 16000) {
    return { samples: mono, sampleRate: 16000 };
  }
  const targetLen = Math.max(1, Math.round((totalFrames * 16000) / sampleRate));
  const resampled = new Float32Array(targetLen);
  const ratio = sampleRate / 16000;
  for (let i = 0; i < targetLen; i++) {
    const src = i * ratio;
    const idx = Math.floor(src);
    const frac = src - idx;
    const a = mono[Math.min(idx, totalFrames - 1)];
    const b = mono[Math.min(idx + 1, totalFrames - 1)];
    resampled[i] = a + (b - a) * frac;
  }
  return { samples: resampled, sampleRate: 16000 };
}

/**
 * Estima frequência dominante (centroide ponderado por energia^2 ao redor do pico)
 * dentro de uma faixa de frequências [minHz, maxHz].
 */
function estimateFormantBand(mag, binHz, minHz, maxHz) {
  const bStart = Math.max(1, Math.floor(minHz / binHz));
  const bEnd = Math.min(mag.length - 2, Math.ceil(maxHz / binHz));

  let bandEnergy = 0;
  let peakBin = bStart;
  let peakVal = -1;

  for (let b = bStart; b <= bEnd; b++) {
    // Suavização espectral de 3 bins para reduzir harmônicos individuais de pitch (F0)
    const smoothed = 0.25 * mag[b - 1] + 0.5 * mag[b] + 0.25 * mag[b + 1];
    const p = smoothed * smoothed;
    bandEnergy += p;
    if (smoothed > peakVal) {
      peakVal = smoothed;
      peakBin = b;
    }
  }

  // Centroide local ao redor do pico (±2 bins) para estabilidade sub-bin
  let weightedHz = 0;
  let weightSum = 0;
  const lo = Math.max(bStart, peakBin - 2);
  const hi = Math.min(bEnd, peakBin + 2);
  for (let b = lo; b <= hi; b++) {
    const w = mag[b] * mag[b];
    weightedHz += b * binHz * w;
    weightSum += w;
  }

  const freqHz = weightSum > 1e-12 ? weightedHz / weightSum : peakBin * binHz;
  return { freqHz, energy: bandEnergy, peakVal };
}

/**
 * Analisa PCM mono e retorna trilha de visemas a 60 FPS (stepMs = 16.6667 ms)
 *
 * @param {Float32Array} samples PCM mono @ 16 kHz
 * @param {object} options
 *   - fps: 60 (padrão)
 *   - maxLevel: 7 (Android: 0..7 + 8=smile) ou 15 (Desktop: 0..15)
 *   - includeSmile: true (Android) | false (Desktop)
 */
export function analyzePhoneticVisemes(samples, sampleRate = 16000, options = {}) {
  const fps = options.fps ?? 60;
  const maxLevel = options.maxLevel ?? 7;
  const includeSmile = options.includeSmile ?? true;

  const stepSamples = sampleRate / fps;
  const frameCount = Math.max(1, Math.ceil(samples.length / stepSamples));
  const binHz = sampleRate / FFT_SIZE;

  const rmsArr = new Float32Array(frameCount);
  const zcrArr = new Float32Array(frameCount);
  const f1HzArr = new Float32Array(frameCount);
  const f2HzArr = new Float32Array(frameCount);
  const f1RatioArr = new Float32Array(frameCount);
  const fricRatioArr = new Float32Array(frameCount);

  const windowed = new Float32Array(FFT_SIZE);
  const re = new Float32Array(FFT_SIZE);
  const im = new Float32Array(FFT_SIZE);
  const mag = new Float32Array(FFT_SIZE >> 1);

  let maxRms = 1e-6;
  const rmsSorted = [];

  for (let f = 0; f < frameCount; f++) {
    const center = Math.round(f * stepSamples);
    const start = center - (FFT_SIZE >> 1);

    let sumSq = 0;
    let zeroCross = 0;
    let prevRaw = 0;

    for (let i = 0; i < FFT_SIZE; i++) {
      const idx = start + i;
      const s = idx >= 0 && idx < samples.length ? samples[idx] : 0;
      sumSq += s * s;
      if (i > 0 && ((s >= 0 && prevRaw < 0) || (s < 0 && prevRaw >= 0))) {
        zeroCross++;
      }
      // Pré-ênfase para realçar F1/F2 e fricativas
      const pre = i > 0 ? s - 0.97 * prevRaw : s;
      windowed[i] = pre * HANN_WINDOW[i];
      prevRaw = s;
    }

    const rms = Math.sqrt(sumSq / FFT_SIZE);
    rmsArr[f] = rms;
    zcrArr[f] = zeroCross / FFT_SIZE;
    if (rms > maxRms) maxRms = rms;
    if (rms > 1e-5) rmsSorted.push(rms);

    computeMagnitudeSpectrum(windowed, re, im, mag);

    const lowVoice = estimateFormantBand(mag, binHz, 240, 960);   // F1 (abertura mandibular)
    const midVoice = estimateFormantBand(mag, binHz, 950, 2900);  // F2 (frontalidade / arredondamento)
    const fricBand = estimateFormantBand(mag, binHz, 3200, 7500); // Fricativas S/Z/X/CH/F/V
    const totalVoiceE = lowVoice.energy + midVoice.energy + fricBand.energy + 1e-12;

    f1HzArr[f] = lowVoice.freqHz;
    f2HzArr[f] = midVoice.freqHz;
    f1RatioArr[f] = lowVoice.energy / totalVoiceE;
    fricRatioArr[f] = fricBand.energy / totalVoiceE;
  }

  // Referência robusta de fala (percentil 92 evita que um único pico distorça toda a curva)
  rmsSorted.sort((a, b) => a - b);
  const refRms =
    rmsSorted.length > 10
      ? rmsSorted[Math.floor(rmsSorted.length * 0.92)]
      : maxRms;
  const noiseFloor =
    rmsSorted.length > 20
      ? rmsSorted[Math.floor(rmsSorted.length * 0.08)]
      : 0.002;

  // Gate de silêncio RÍGIDO: fecha imediatamente entre palavras!
  // -26 dB (~5% do refRms) para desligar, -22 dB (~8% do refRms) para abrir
  const gateCloseThresh = Math.max(noiseFloor * 2.2, refRms * 0.065, 0.006);
  const gateOpenThresh = Math.max(noiseFloor * 3.0, refRms * 0.095, 0.009);

  const visemes = new Uint8Array(frameCount);
  const isSilent = new Uint8Array(frameCount);

  let speechOpen = false;
  let silentStreak = 0;

  for (let f = 0; f < frameCount; f++) {
    const rms = rmsArr[f];
    const zcr = zcrArr[f];
    const f1 = f1HzArr[f];
    const f2 = f2HzArr[f];
    const fricRatio = fricRatioArr[f];

    // Hysteresis rígida de gate de silêncio
    if (speechOpen) {
      if (rms < gateCloseThresh) {
        speechOpen = false;
        silentStreak = 1;
      } else {
        silentStreak = 0;
      }
    } else {
      if (rms >= gateOpenThresh) {
        speechOpen = true;
        silentStreak = 0;
      } else {
        silentStreak++;
      }
    }

    if (!speechOpen) {
      isSilent[f] = 1;
      visemes[f] = 0;
      continue;
    }

    // Energia normalizada (0..1) com compressão suave
    const normE = Math.min(
      1.0,
      Math.max(0.0, (rms - gateCloseThresh) / Math.max(1e-5, refRms - gateCloseThresh))
    );

    // 1) Sibilantes e Fricativas (S, Z, X, CH, F, V):
    // Alta energia em 3.2-7.5 kHz ou ZCR alto com pouca energia F1
    const isSibilant = fricRatio > 0.34 || (zcr > 0.22 && f1RatioArr[f] < 0.32);

    // 2) Vale consonantal / oclusão bilabial (P, B, M, T, D) no meio de palavra:
    // Queda local abrupta de energia comparado aos vizinhos (-35% vs média ±2 frames)
    const prevRms = f > 1 ? rmsArr[f - 2] : rms;
    const nextRms = f + 2 < frameCount ? rmsArr[f + 2] : rms;
    const localAvg = 0.5 * (prevRms + nextRms);
    const isConsonantDip = normE < 0.24 && rms < localAvg * 0.62;

    let openness = 0.0; // 0.0 .. 1.0

    if (isConsonantDip) {
      // Fechamento labial rápido em consoantes bilabiais / oclusivas
      openness = 0.0;
    } else if (isSibilant) {
      // S/Z/F/V: dentes próximos, lábios levemente entreabertos (visema 2 no Android, ~3-4 no Desktop)
      openness = 0.24 + normE * 0.10;
    } else {
      // Vogais e sonorantes: mapeamento Fonético-Visêmico F1/F2 real!
      // F1 controla abertura da mandíbula:
      //   260 Hz (I/U fechado) -> 0.22
      //   500 Hz (E/O médio)   -> 0.55
      //   820 Hz (A/Á aberto)  -> 1.00
      const f1Open = Math.min(1.0, Math.max(0.0, (f1 - 260) / 560));

      // F2 modula o formato labial:
      //   F2 alto (> 1850 Hz = vogais frontais I/Ê) -> reduz abertura vertical (boca mais horizontal)
      //   F2 baixo (< 1200 Hz = vogais posteriores arredondadas O/U) -> moderação suave
      let f2Factor = 1.0;
      if (f2 > 1850) {
        f2Factor = 0.72; // I / Ê frontal fechado
      } else if (f2 < 1150 && f1 < 540) {
        f2Factor = 0.78; // U / Ô arredondado
      }

      // Combina abertura fonética F1/F2 (68%) com tonicidade/energia silábica (32%)
      const rawOpen = (0.68 * f1Open * f2Factor) + (0.32 * Math.pow(normE, 0.75));
      openness = Math.min(1.0, Math.max(0.14, rawOpen * (0.55 + 0.50 * Math.pow(normE, 0.6))));
    }

    if (openness <= 0.04) {
      visemes[f] = 0;
    } else {
      const level = Math.round(openness * maxLevel);
      visemes[f] = Math.max(1, Math.min(maxLevel, level));
    }
  }

  // Pós-processamento: filtro mediano de 3 frames APENAS em frames sonoros (preserva silêncio rígido = 0!)
  const cleaned = new Uint8Array(visemes);
  for (let f = 1; f < frameCount - 1; f++) {
    if (isSilent[f]) {
      cleaned[f] = 0;
      continue;
    }
    // Se um único frame de 16ms ficou isolado entre dois silêncios, zera (anti-jitter)
    if (visemes[f - 1] === 0 && visemes[f + 1] === 0 && visemes[f] <= 2) {
      cleaned[f] = 0;
      continue;
    }
    // Remove micro-spikes de 1 frame (16ms) dentro da fala sem atrasar fechamentos
    if (visemes[f - 1] > 0 && visemes[f + 1] > 0) {
      const a = visemes[f - 1];
      const b = visemes[f];
      const c = visemes[f + 1];
      // Mediana de 3
      cleaned[f] = a + b + c - Math.min(a, b, c) - Math.max(a, b, c);
    }
  }

  // Marca sorriso acolhedor (visema 8) no Android apenas em pausas longas (> 320ms) ou início/fim silencioso
  if (includeSmile && maxLevel === 7) {
    const minSmileRun = Math.round(0.32 * fps); // 320ms = ~19 frames @ 60fps
    let runStart = -1;
    for (let f = 0; f <= frameCount; f++) {
      const silentHere = f < frameCount && cleaned[f] === 0 && isSilent[f] === 1;
      if (silentHere) {
        if (runStart < 0) runStart = f;
      } else if (runStart >= 0) {
        const runLen = f - runStart;
        const isEdge = runStart === 0 || f === frameCount;
        if (isEdge && runLen >= Math.round(0.15 * fps)) {
          // Deixa 2 frames (33ms) de boca fechada neutra antes de abrir fala / depois de terminar
          const padStart = runStart === 0 ? 0 : runStart + 2;
          const padEnd = f === frameCount ? f : Math.max(padStart, f - 2);
          for (let k = padStart; k < padEnd; k++) cleaned[k] = 8;
        } else if (runLen >= minSmileRun) {
          // Pausa longa de frase: fecha boca 3 frames (50ms), sorri no meio, fecha 3 frames antes de retomar
          const padStart = runStart + 3;
          const padEnd = f - 3;
          for (let k = padStart; k < padEnd; k++) cleaned[k] = 8;
        }
        runStart = -1;
      }
    }
  }

  // Estatísticas para diagnóstico e auditoria real
  const hist = {};
  for (let f = 0; f < frameCount; f++) {
    const v = cleaned[f];
    hist[v] = (hist[v] || 0) + 1;
  }

  return {
    fps,
    stepMs: 1000 / fps,
    durationMs: Math.round((samples.length / sampleRate) * 1000),
    frameCount,
    visemes: cleaned,
    histogram: hist,
  };
}
