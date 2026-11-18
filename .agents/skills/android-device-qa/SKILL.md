---
name: android-device-qa
description: Protocolo obrigatório de QA real no dispositivo Android conectado via ADB (Motorola/Samsung), captura visual de telas, inspeção de AAB com bundletool, testes E2E com Maestro e coleta de logcat antes de declarar qualquer alteração de UI ou fluxo como pronta.
---

# 📱 Skill: Android Device QA & Visual Verification

Use esta skill sempre que alterar UI, fluxos de permissão, exclusão (`createDeleteRequest` / `RecoverableSecurityException`), billing ou antes de subir um novo release/AAB para a Google Play.

## 1. Identificar o dispositivo real conectado (nunca assumir de memória)
```powershell
adb devices
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
```

## 2. Compilar e instalar a versão debug no aparelho
```powershell
./gradlew --no-daemon :app:installDebug
adb shell am start -n com.rdf112.midiastore/com.rdf112.sweep.MainActivity
```

## 3. Inspeção visual real (olhos na tela antes de aprovar)
Capture a tela atual do aparelho e leia a imagem com `read_file` para validar contraste, corte de texto, margens, navegação e ausência de erros visuais:
```powershell
New-Item -ItemType Directory -Force -Path ".tmp/qa" | Out-Null
adb exec-out screencap -p > .tmp/qa/screen.png
```
- Se precisar espelhar interativamente para o usuário acompanhar: `scrcpy`.
- Nunca aprovar mudança visual apenas porque compilou.

## 4. Coleta de erros e crashes em tempo real
```powershell
adb logcat -d -s AndroidRuntime:E System.err:W SweepDebug:* | Select-Object -Last 60
```

## 5. Auditoria obrigatória do `.aab` antes da Play Console (`bundletool`)
Sempre que gerar um `bundleRelease`:
```powershell
java -jar C:\Users\rodolfo\tools\bundletool.jar dump manifest --bundle=app/build/outputs/bundle/release/app-release.aab
```
Confirmar:
- `applicationId` = `com.rdf112.midiastore`
- **Zero** `MANAGE_EXTERNAL_STORAGE` no manifest mesclado
- `versionCode` e `versionName` esperados
