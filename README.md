# Midiastore — Android storage manager

**Status:** development / recovery. This repository is not a release candidate and must not be submitted to Google Play until all P0 gates in `docs/RELEASE_BLOCKERS.md` are closed.

## Confirmed application identity

| Field | Current source configuration | Evidence / qualification |
|---|---|---|
| Commercial name | Midiastore | Owner confirmed the Play Console listing name |
| Google Play package | `com.rdf112.midiastore` | Owner supplied the listing with this package on 04/11/2026 |
| Android namespace | `com.rdf112.sweep` | Internal Kotlin/R namespace; do not confuse it with applicationId |
| versionCode | `17` | Current source value only; the highest version already published must still be checked in Play Console |
| versionName | `2.2.1` | Current source value only |
| minSdk / targetSdk | `29` / `36` | Current source configuration |
| compileSdk | `36` | Current source configuration |
| Java / Kotlin JVM | `17` | Current source configuration |
| Billing Library | `8.0.0` | Current dependency; product activation and live purchase have not been verified |

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

These IDs and purchase types are source configuration, not proof of activation in the Play Console. The current client checks the presence of the Play purchase signature/JSON fields but does not perform cryptographic token validation through a trusted backend. Do not represent local entitlement as production-grade purchase verification. Real catalog queries, test purchases, pending/cancelled/restored purchases, acknowledgement and subscription lifecycle must be tested before selling Pro.

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
