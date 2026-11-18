---
name: play-store-assets
description: Fábrica automática de screenshots e assets da Google Play Store nos 14 idiomas usando Maestro + ADB + FFmpeg + ImageMagick e upload direto via play-upload-screenshots.mjs.
---

# 🎨 Skill: Play Store Assets & Multi-Language Screenshots

Use esta skill para gerar, padronizar e subir screenshots e assets gráficos para a Google Play Store.

## 1. Fluxo de captura limpa no dispositivo (`moto g84 5G`)
1. Garantir tema aprovado e dados limpos na tela.
2. Rodar fluxo `Maestro` por idioma/tela e capturar via `adb exec-out screencap -p`.
3. Padronizar resolução Play Store (`1080x1920` ou `1080x2400`) com `magick` ou `ffmpeg`.

## 2. Comandos de mídia prontos
- Redimensionar/padronizar PNG:
  `magick input.png -resize 1080x1920^ -gravity center -extent 1080x1920 output.png`
- Converter gravação `scrcpy` em vídeo curto promocional (MP4 1080p):
  `ffmpeg -i demo.mp4 -vf "scale=1080:-2" -c:v libx264 -crf 20 -preset fast promo_1080p.mp4`

## 3. Upload para a Play Console
```powershell
node scripts/play-upload-screenshots.mjs
```
Sempre validar os arquivos gerados visualmente com `read_file` antes de subir.
