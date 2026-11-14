<div align="center">

# 🧹🌍 Midiastore (`com.rdf112.midiastore`)
### Pro Phone Cleaner · Local Media Studio · Worldwide Offline Hub & Mystery Vault

[![Android API](https://img.shields.io/badge/Android-29%20to%2036-00E5B0?style=for-the-badge&logo=android&logoColor=070B14)](https://developer.android.com)
[![Languages](https://img.shields.io/badge/Global%20Locales-14%20Languages%20(incl.%20AR--RTL)-38BDF8?style=for-the-badge)](app/src/main/res/xml/locales_config.xml)
[![Scoped Storage](https://img.shields.io/badge/Scoped%20Storage-100%25%20Compliant-10B981?style=for-the-badge)](app/src/main/AndroidManifest.xml)
[![Play Billing](https://img.shields.io/badge/Google%20Play%20Billing-8.3.0-FFD54F?style=for-the-badge&logo=googleplay&logoColor=070B14)](app/build.gradle.kts)

**Clean up gigabytes safely with official Android confirmation — and carry useful offline global tools, accessible Senior Easy Mode, retro arcade classics, and a hidden riddle & card-magic lab everywhere you go.**

</div>

---

## ✨ What makes Midiastore different from ordinary cleaners

| Pillar | Highlights (Implemented on-device) |
|---|---|
| 🧹 **Real Free Phone Cleaner** | 1-tap cleanup scan with **thumbnails & review checkboxes**, exact duplicates (**streaming SHA-256**), large files (`>10 MB`), older media (`+30d`), **WhatsApp Cleaner**, official Android Trash & permanent delete consent. |
| 🎨 **Free Local Media Studio** | Live photo filters, **Video ➔ M4A audio extractor**, **EXIF/GPS privacy scrubber**, **Designer QR Studio (5 luxury card styles)**, Photos-to-A4 PDF, and direct public web video saver. |
| 🌍 **Worldwide & Inclusive Hub** | **14 native locales** (`PT-BR, EN, ES, DE, FR, HI, JA, ZH-CN, AR-RTL, RU, ID, BN, KO`), **1-tap Senior Easy Mode (`👓 Aa+`)** for large text/buttons, offline **World Clock (16 hubs)**, **Global Currency Converter (offline + live rates)**, **Live Weather & Travel/Restaurant Maps guide**. |
| 🎮🔮 **Offline Arcade & Mystery Lab** | Play **Retro Snake** & **Falling Blocks Puzzle** 100% offline on the bus or flight — plus a **5-Tap Secret Logo Easter Egg**, **3 unlockable riddles**, mathematical **21-Card Mind Reader Magic**, and **Illusionist Magic Dice**! |
| 👑 **Pro VIP Edition (174 Countries)** | Transforms the interface into **Royal Black & 24K Gold VIP Theme**, unlocking **Pro Word Search (100% Offline + Daily Date Puzzle)**, **Pro Private Notes Vault**, **Pro Smart Calculator + Storage Estimator**, **Strong Password & Wi-Fi QR Vault**, **90-day Unused Apps finder**, and **Audited Reports**. |

---

## Posicionamento e modelo comercial confirmados: Pro + Freemium

O Midiastore é um **limpador inteligente de celular (phone cleaner) + suíte global de utilidades diárias** com apresentação premium em 6 abas espaçosas (`Início`, `Limpeza`, `Arquivos`, `Estúdio`, `Mundo` e `Pro VIP`). O plano gratuito inclui análise das mídias acessíveis no aparelho via MediaStore, localização de arquivos grandes, itens antigos e cópias exatas por SHA-256, revisão manual, limpeza com confirmação oficial do Android, Estúdio de Mídia local e o Hub Mundial Offline. Recursos não implementados — como detecção de desfoque e análise de similaridade visual — continuam no roadmap e não devem ser anunciados.

A verificação de assinatura/compras usa Google Play Billing (`billing:8.3.0`) com validação de `purchaseToken` via Edge Function (`verify-play-purchase` + Google Play Developer API v3) e requer testes completos de compra, restauração e reembolso com License Testers antes da liberação comercial. Segurança, proteção do original e consentimento do Android permanecem gratuitos.

**Status:** pre-production hardening (closed testing & P0 verification in progress). Consulte [`docs/RELEASE_BLOCKERS.md`](docs/RELEASE_BLOCKERS.md) e [`docs/ROADMAP_GLOBAL_V3.md`](docs/ROADMAP_GLOBAL_V3.md) antes de promover qualquer build para produção na Google Play Console.

## Confirmed application identity

| Field | Current source configuration | Evidence / qualification |
|---|---|---|
| Commercial name | Midiastore | Owner confirmed the Play Console listing name |
| Google Play package | `com.rdf112.midiastore` | Confirmed in Play Console (`app/build.gradle.kts`) |
| Android namespace | `com.rdf112.sweep` | Internal Kotlin/R namespace; do not confuse it with applicationId |
| versionCode | `22` | Current `app/build.gradle.kts` value (`targetSdk 36`, `compileSdk 36`) |
| versionName | `2.5.0` | Current `app/build.gradle.kts` value |
| minSdk / targetSdk | `29` / `36` | Current source configuration |
| compileSdk | `36` | Current source configuration |
| Java / Kotlin JVM | `17` | Current source configuration |
| Billing Library | `8.3.0` | `com.android.billingclient:billing:8.3.0` + Edge Function token validation |
| Supported Locales | 14 directories (`values` + 13 locales) | Includes `ar` (RTL), `bn`, `de`, `en`, `es`, `fr`, `hi`, `in`, `ja`, `ko`, `pt-rBR`, `ru`, `zh-rCN` |

The source project retains the legacy `com.rdf112.sweep` namespace and resource names for compatibility with the reconstructed code. The installable application identity is `com.rdf112.midiastore`. Do not change `applicationId`, signing certificate, versionCode or Play listing without the owner's explicit approval.

The owner reports that the financial/payment profile was enabled on 03/11/2026. This does **not** by itself prove that the three in-app products are active, correctly typed, available in Brazil, or tested through Google Play Billing.

## Current engineering branch

- Working branch: `fix/p0-sweep-permissions-missions-billing`
- Review: [PR #3](https://github.com/srodolfobarbosa-afk/midiastory-/pull/3)
- Recovery issue: [Issue #2](https://github.com/srodolfobarbosa-afk/midiastory-/issues/2)
- Do not merge directly into `main`.
- Do not create a new signing key or upload a temporary certificate.
- Do not submit a release AAB while any P0 blocker remains open.

## Architecture and current behavior

The Android application uses native Android Views and the MediaStore APIs. The application can query photos, videos and audio exposed by MediaStore, one bounded page at a time (100 rows in the UI). It tracks permissions, storage volumes and source revision markers where Android exposes them. On Android 14 and later, selected-photo/video access is reported as partial coverage.

The recovery branch adds a global mission scanner that iterates through accessible media kinds, volumes and MediaStore pages. It is cancelable and has explicit safety limits: 250,000 rows, 20,000 repeated-size candidate rows, 100,000 size buckets, 2 GiB per global hash and 10 GiB total global hash I/O. Normal page hashing is capped at 512 MiB per file and 1 GiB per page. It reports partial coverage when a permission, provider, revision check or safety limit prevents a complete scan. Exact duplicates are only confirmed when non-empty SHA-256 hashes match. If global duplicate coverage is partial, the scanner does not auto-select duplicate files for deletion.

Large-file and old-media missions prepare review candidates. A result is not a deletion: the user must review the selection and approve the Android system consent flow. The review/operation code revalidates each URI before requesting an operation.

### Known platform limits

- The recovery branch now enables permanent deletion on Android 10/API 29 through RecoverableSecurityException and the system consent screen, one URI at a time. This implementation still requires API29 emulator and physical-device validation. Android 10 does not expose the same batch Trash/restore APIs; Trash/restore remain API30+.
- Moving a file to the Android trash is not counted as freed storage. Space is credited only after permanent deletion is confirmed by the provider.
- MediaStore does not cover every arbitrary document or Downloads location. Generic documents require a user-selected Storage Access Framework workflow; that is not implemented yet.
- The app does not clean private caches belonging to other apps, boost RAM, cool the device, or accelerate the CPU.
- Similar-photo detection, blur/quality detection, video compression and persistent storage history are not implemented.

## Monetization state

The source integrates Google Play Billing Client and currently queries these product IDs:

- `sweep_pro_monthly` — subscription
- `sweep_pro_annual` — subscription
- `sweep_pro_lifetime` — one-time product

These IDs match the active Google Play Developer API catalog (`sweep_pro_monthly`, `sweep_pro_annual`, `sweep_pro_lifetime`). The Android client delegates `purchaseToken` verification and Play Store acknowledgement to the Supabase Edge Function (`verify-play-purchase`) backed by the Google Play Developer API v3 service account. Real license-tester purchases, pending/cancelled/restored purchases, subscription renewals, refunds and expiry lifecycles must still be tested end-to-end before marketing production availability.

## Build and verification

Requirements:

- JDK 17
- Gradle Wrapper 8.13
- Android SDK Platform 36
- Android Build Tools 35.0.0
- Network access on the first run to download Gradle and dependencies

Debug/unit verification:

```bash
./gradlew --no-daemon \
  :domain:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

Instrumented tests require a connected Android device or emulator:

```bash
./gradlew --no-daemon :app:connectedDebugAndroidTest
```

In GitHub Codespaces, use the branch listed above. The `main` branch still contains an old machine-specific `org.gradle.java.home` value; the recovery branch removes it and uses the JDK selected by the environment. A local PowerShell prompt reporting `adb: no devices/emulators found` means no Android device or emulator is connected; it is not an application test result.

The CI workflow builds Debug, runs unit tests and Lint, then runs instrumented tests on an Android 35 emulator. A green build is necessary but not sufficient: physical-device permission, MediaStore, cleanup, Billing, accessibility and OEM tests are still required.

## Signing and release

Release tasks are intentionally blocked unless the legitimate upload keystore and matching credentials are provided. Never generate a replacement JKS by guessing. The certificate must match the upload certificate accepted by Play App Signing.

Before a release:

1. Confirm the highest published versionCode and package in Play Console.
2. Confirm the upload certificate.
3. Verify real product IDs, product types, offers and country availability.
4. Finish purchase-token validation and entitlement reconciliation.
5. Test permissions, global scanning, safe cleanup and restore on real devices.
6. Pass unit, lint, emulator and representative physical-device tests.
7. Verify privacy policy, Data Safety, manifest and SDK declarations against the final binary.
8. Generate and inspect a correctly signed AAB, then use internal/closed testing before production.

A successful Debug build or an emulator run does not make the app production-ready.


## Trust, privacy and store-readiness requirements

The product guardrails, audit-report requirements, permission principles and current CI evidence are maintained in [`docs/PRODUCT_TRUST_AND_PRIVACY.md`](docs/PRODUCT_TRUST_AND_PRIVACY.md). Treat this as an engineering gate, not a claim that Play review or release readiness has been achieved.
