# Midiastore publication checklist

Updated 2026-11-07. This checklist records the current local build and release gates; it is not a declaration that the app is release-ready.

## Implemented and checked in this session

- [x] Home uses the navy and pink identity, original vector artwork, one prominent free-cleanup action, and a visible Pro entry point.
- [x] The free-cleanup action runs the app's MediaStore large-file mission instead of opening Files by Google. On the connected Moto G84 5G (Android 15/API 35), it found 5 review candidates totaling 84.30 MB and opened Midiastore's review screen.
- [x] The candidates remained selected for review; no personal video was deleted during this check. Permanent deletion and post-delete byte verification still need a consented, controlled test.
- [x] The home share action opens Android's system Sharesheet with the Midiastore Play link. The test opened and dismissed the chooser without posting anything.
- [x] `:app:assembleDebug` succeeded on Windows after the current home-cleanup/share changes, and the Debug APK installed on the Moto G84 5G.
- [x] English and Brazilian Portuguese string key sets match.

## Product and prices

- [x] Free: find large media files and review candidates before removal.
- [x] Pro monthly, annual, and lifetime feature split and source IDs are documented in `PRICING_AND_PLAN_MATRIX.md`.
- [x] Suggested Brazil prices are documented and displayed as proposed, not active: R$ 14.99/month, R$ 79.99/year, and R$ 149.99 once. Google Play must return the actual localized price before checkout is enabled.
- [ ] The three products currently return unavailable in the app. Configure/activate their base plans, prices, countries, and license testers in Play Console, then verify a real test purchase.
- [ ] Deploy trusted server-side purchase-token verification and test purchase, pending, restore, acknowledgement, refund, and expiry before enabling paid access.

## Still blocks a public release

- [ ] Validate permanent deletion consent and post-operation absence on API 29 and current Android versions. Do not count Trash as freed space.
- [ ] Green CI on the exact commit for domain/JVM tests, lint, Debug, and supported instrumented Android API 29/35 runs. Previous CI/UTP/API 29 blockers are recorded in `RELEASE_BLOCKERS.md`; the current local build is not a substitute.
- [ ] Test partial/denied/revoked media access, cancellation, coverage reporting, paging, and deletion on representative OEM devices. The connected-device run in this session was a manual scan/UI check, not the full instrumented matrix.
- [ ] Review the production manifest, media permission eligibility, privacy policy, Data Safety, SDK declarations, and Play listing as one release candidate.
- [ ] Confirm the highest Play-published version code and upload certificate in Play Console before producing a production-signed AAB. Do not change the current package/version/signing identity by assumption.
- [ ] Finish reviewed translations and localized Play listings. English and Brazilian Portuguese have the full resource set; Spanish, French, German, Hindi, Japanese, and Simplified Chinese now cover the main Home/cleanup/share/plan flow, while secondary screens still fall back to English.
- [ ] Generate and review a photorealistic AI portrait. The current original vector character now depicts a smiling dark-skinned woman with blonde hair, but it is still an illustration, not a photo.

**Release status: NOT READY for public Google Play publication.** The app can continue through controlled internal testing after the owner completes the Play Console product setup and the remaining device/CI/security checks.
