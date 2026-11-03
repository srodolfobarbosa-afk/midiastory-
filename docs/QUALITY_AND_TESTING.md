# Quality and Testing

Última execução registrada: 03/11/2026, Codespace Ubuntu 24.04, JDK 17.0.17, Gradle 8.13, AGP 8.13.2, Android SDK Platform 36, Build Tools 35.0.0. SDK foi instalado em diretório temporário `/tmp/tmp.Pnk13LDOXN/android-sdk`; não há configuração persistente `local.properties`.

## Resultado Debug VC14

Comando executado sem `clean`:

```bash
JAVA_HOME=/usr/local/sdkman/candidates/java/17.0.17-ms \
ANDROID_HOME=/tmp/tmp.Pnk13LDOXN/android-sdk \
ANDROID_SDK_ROOT=/tmp/tmp.Pnk13LDOXN/android-sdk \
./gradlew --no-daemon :domain:test :app:testDebugUnitTest :app:lintDebug \
  :app:assembleDebug :app:assembleDebugAndroidTest
```

Resultado final VC14: `BUILD SUCCESSFUL`, 82 tarefas (16 executadas, 66 up-to-date). `:domain:test`: 58 testes aprovados. `:app:testDebugUnitTest`: 7 testes aprovados. Lint sem erros, 76 warnings restantes. APKs gerados em `app/build/outputs/apk/debug/app-debug.apk` e `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

O Gradle também emitiu warning: parser SDK reconhece XML até versão 3, mas a plataforma instalada contém XML versão 4. A compilação terminou, mas alinhar versão de command-line tools/AGP para remover o aviso e garantir suporte.

## Executado vs. pendente

| Gate | Estado |
|---|---|
| `:domain:test` | Aprovado: 58/58 |
| `:app:testDebugUnitTest` | Aprovado: 7/7 |
| `:app:lintDebug` | Passou sem erros; 76 warnings |
| `:app:assembleDebug` | Aprovado; APK produzido |
| `:app:assembleDebugAndroidTest` | Aprovado; APK instrumentado produzido |
| `:app:connectedDebugAndroidTest` | Não executado em aparelho/emulador |
| `:app:compileReleaseKotlin`, `:app:lintRelease` | Passaram com chave temporária em `/tmp`; Lint tem warnings |
| `:app:assembleRelease`, `:app:bundleRelease` | Passaram com chave temporária; APK/AAB não são para Play |
| Teste de compra real/Play Console | Não executado; conta/catálogo indisponíveis |
| MediaStore OEM/API matrix, restore/delete, performance | Não executados em dispositivo |

## Matriz de testes obrigatória para release

1. Unit: permissão API 29/32/33/34/36/37; paginação HonoredArgs/fallback; cursor nulo/erro/cancelamento; geração que muda; SHA diferente/ausente; proteção de original; saturação de soma de bytes; seleção e itens alterados.
2. Instrumented: inserir milhares de registros por tipo/volume e percorrer offsets sem duplicar/perder; teste de partial access; revogar permissões; remover mídia durante cursor/hash; consentimento aceitar/cancelar/parcial; confirmar estado posterior e bytes.
3. Dispositivos: Pixel/AOSP, Samsung One UI, Motorola e Xiaomi/HyperOS, APIs acordadas; SD/removível quando suportado. Registrar modelo/API, permissões, log e hash do build.
4. Billing: teste Play license, SKU ausente, atraso/rede offline, purchase pending, acknowledgement, restore, subscription expiry/refund/cancel. Não conceder entitlement em client offline.
5. Qualidade: Lint sem warnings críticos, testes Debug, compilação de Release, AAB legítimo assinado/inspecionado, testes large-library com memória/CPU/bateria/temperatura e acessibilidade/fonte grande/tablet/dobravel.

## Reproduzir no Codespace

Após SDK persistente instalado e licenças aceitas:

```bash
export JAVA_HOME=/usr/local/sdkman/candidates/java/17.0.17-ms
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew --no-daemon :domain:test :app:testDebugUnitTest :app:lintDebug \
  :app:assembleDebug :app:assembleDebugAndroidTest
```

Instrumentados requerem emulador/aparelho conectado: `./gradlew --no-daemon :app:connectedDebugAndroidTest`.

## CI configurada nesta missão

O arquivo `.github/workflows/android.yml` executa verificação Debug em push/PR e reserva geração assinada para `workflow_dispatch` no Environment `play-release`, usando secrets do proprietário e sem publicar na Play. O workflow ainda não foi executado no GitHub; emuladores compatíveis precisam de job separado. Nenhuma aprovação de Actions ou assinatura é presumida.

O script `scripts/build_signed_aab.sh` exige certificado esperado e credenciais via ambiente, executa `clean`/gates, valida assinaturas e exporta APK/AAB mais SHA-256 na raiz sem sobrescrever arquivos existentes. `bash -n` passou; não foi executado com segredos de produção. Workflow GitHub Actions ainda não rodou no remote.