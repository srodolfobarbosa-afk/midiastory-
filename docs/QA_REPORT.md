# Relatório de Engenharia e QA — Sweep v2.1.0 (`versionCode = 13`)

## 1. Auditoria da Árvore de Código e Versões Reais
- **Gradle Wrapper:** `8.13`
- **Android Gradle Plugin (AGP):** `8.9.2`
- **Kotlin:** `2.0.21` (JVM Target 17)
- **Android SDK:** `compileSdk = 36`, `targetSdk = 36`, `minSdk = 29`
- **Bibliotecas Core:** `androidx.core:core-ktx:1.16.0`, `androidx.appcompat:appcompat:1.7.1`, `androidx.activity:activity-ktx:1.10.1`, `androidx.lifecycle:lifecycle-*-ktx:2.9.4`, `kotlinx-coroutines-android:1.10.2`, `com.google.android.material:material:1.13.0`
- **Google Play Billing:** `com.android.billingclient:billing-ktx:8.0.0`

## 2. Problemas Corrigidos Nesta Versão
1. **Estrutura Raiz Plana no ZIP (Fim da subpasta duplicada):** todos os arquivos do projeto agora ficam diretamente na raiz do pacote `.zip`, evitando que o `Expand-Archive` do Windows extraia o código em uma subpasta separada.
2. **Duplicatas Exatas por Conteúdo SHA-256 em Fluxo (`AndroidMediaPageRepository.kt` + `SmartMediaGroupingPolicy.kt`):** arquivos de mesmo tamanho em bytes passam por leitura em fluxo (`BufferedInputStream` de 8 KB + `MessageDigest("SHA-256")`). Arquivos de mesmo tamanho mas com conteúdo diferente são rejeitados como falsos positivos e listados como *não confirmados*.
3. **Motor OaaS de 6 Etapas e 4 Indicadores de Espaço (`OutcomeServiceEngine.kt` + `MainActivity.kt`):** separação visual e lógica rigorosa entre *Espaço potencialmente recuperável*, *Espaço preparado para revisão*, *Espaço autorizado* e *Espaço efetivamente liberado*.
4. **Cálculo Real de Desconto Anual (`BillingStatePolicy.kt`):** eliminação de descontos fictícios quando o catálogo da Play Store não está carregado; cálculo matemático exato via `priceAmountMicros`.
5. **Eliminação de Warnings de Compilação Kotlin (`ModernDashboardWidgets.kt`):** substituição de `scaledDensity` por `TypedValue.applyDimension(COMPLEX_UNIT_SP, ...)`.
