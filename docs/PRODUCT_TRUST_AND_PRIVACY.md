# Sweep — Trust, Privacy & Store Readiness

**Status:** engineering requirements / not a release approval  
**Applies to:** the Sweep Android cleaner in this repository  
**Product principle:** user control and verifiable outcomes take priority over aggressive cleanup claims.

## 1. Product promise

The product should help a user understand storage usage, review candidate files and make deliberate cleanup decisions. It must not imply that it can clean another app's private cache, boost RAM, cool the CPU or accelerate the device unless a real, policy-compliant implementation exists.

The core promise is:

- Scan only media and locations that Android makes accessible to the app.
- Explain scan coverage and disclose partial or unavailable results.
- Let the user review candidates before destructive actions.
- Use Android's system consent UI wherever the platform requires it.
- Report storage as freed only after post-operation verification confirms the relevant item is gone.

Do not use “100% offline” as a blanket claim unless every shipped feature, SDK and analytics path has been audited. Prefer the precise statement “file analysis and hashing run on-device” only while that remains true for the relevant flows.

## 2. Three separate storage states

The UI, telemetry and exported reports must keep these concepts separate:

1. **Potentially recoverable:** estimated bytes represented by candidates. This is not a promise that the space can be reclaimed.
2. **User-authorized:** files included in an operation the user approved. Authorization is not proof of deletion.
3. **Effectively freed:** bytes for which post-operation verification confirms the corresponding content is no longer present at the checked MediaStore URI/provider state.

Moving an item to Trash is not permanent deletion and must not be reported as freed storage. A cancelled, denied, timed-out or unverifiable operation contributes zero to the effectively-freed metric. For partial batches, count only individually verified missing URIs; never discard verified partial success and never count unverified items.

## 3. SHA-256: correct use and limits

- Use SHA-256 to identify exact byte-for-byte matches when the entire relevant content was read successfully.
- A digest match is evidence of identical content, not evidence that either copy is safe to delete.
- Hashing must be cancellable, bounded, off the main thread and subject to explicit I/O limits.
- If a source cannot be fully read, changes during hashing, or exceeds a safety budget, mark it unverified and do not auto-select it for deletion.
- Avoid presenting a hash as proof of authenticity, malware safety, ownership or successful deletion.
- If a report exposes a digest, make that an optional advanced detail; do not expose private file paths or unnecessary personal metadata by default.

## 4. Permission and Play policy guardrails

- Request only permissions required for a feature the user has invoked and can understand.
- Explain why each media permission is needed before the Android permission prompt.
- Respect Android version differences, partial photo/video access, denied permissions and revocation.
- Do not request MANAGE_EXTERNAL_STORAGE as a shortcut. Any future use requires a documented core-functionality and Play policy assessment before implementation.
- Local processing and SHA-256 do not automatically justify broad media permissions or guarantee Play approval.
- Keep the privacy policy, Play Console Data safety answers, manifest declarations, SDK behavior and actual shipped binary consistent.
- Re-check current Google Play policy and Android permission requirements at release time; this document is an engineering checklist, not legal or policy approval.

## 5. Auditable report requirements

A future exportable cleanup report should include, at minimum:

- Scan date/time and Android API level.
- Scan coverage: media categories/volumes/pages checked, partial access, skipped sources and reasons.
- Candidate count and estimated candidate bytes, explicitly labelled as estimates.
- User-authorized operation count/bytes, explicitly labelled as authorization.
- Verified removed count/bytes, calculated only from post-operation checks.
- Failed, cancelled and unverifiable counts with reason categories.
- A clear statement that Trash is not permanent deletion.

Privacy constraints for exports:

- Do not include raw file contents.
- Do not include full filesystem paths, account identifiers or sensitive filenames by default.
- Prefer aggregate counts and reason codes.
- Make export an explicit user action and show what will be included before sharing.
- Do not claim the report is tamper-proof unless it is cryptographically signed and the verification design is implemented and tested. A SHA-256 value alone does not make a report tamper-proof.

## 6. UX and monetization guardrails

- Keep safety, user consent and truthful deletion accounting available in the free experience.
- Do not paywall Android consent, safe deletion, privacy controls or truthful operation results.
- Clearly disclose the exact Pro benefit before purchase. Current client-side export capability is not proof of secure purchase validation.
- Do not unlock entitlements solely from client-provided purchase JSON/signature fields. Production entitlement requires trusted purchase-token verification and lifecycle reconciliation.
- Do not publish invented storage percentages, benchmark claims, “AI-powered” claims or guaranteed cleanup amounts.
- Use “review”, “potentially recoverable” and “verified freed” consistently across screens, store listing and reports.

## 7. Release acceptance checklist

- [ ] Unit tests cover full, partial, denied, cancelled and unverifiable deletion outcomes.
- [ ] Instrumented tests run successfully on supported Android API levels; an emulator boot failure is an infrastructure failure, not a passing test.
- [ ] Physical-device tests cover permission grant/deny/revoke, partial media access, MediaStore changes and Android system confirmation.
- [ ] Trash and permanent deletion are represented separately.
- [ ] SHA-256 work is bounded, cancellable and never blocks the UI thread.
- [ ] Exported reports disclose partial coverage and separate estimated, authorized and verified values.
- [ ] Manifest permissions are minimal and justified by user-facing core functionality.
- [ ] Privacy policy and Data safety declarations match the final binary and SDK behavior.
- [ ] Billing products, purchase-token verification, acknowledgement, restore and subscription lifecycle are tested.
- [ ] Store listing and screenshots contain only implemented, evidenced claims.

## 8. Current CI evidence (2026-11-04)

Workflow run [41905130649](https://github.com/srodolfobarbosa-afk/midiastory-/actions/runs/41905130649):

- Domain/unit tests, app unit tests, lint and Debug assembly completed successfully on both matrix jobs.
- PowerShell script parsing passed.
- API 29 instrumentation did not start: ADB/device reporting identified emulator API level 1 and APK installation failed; report contains zero instrumentation tests.
- API 35 emulator did not finish booting before timeout; instrumentation did not run.
- Debug APKs and test-report artifacts were uploaded, but these artifacts do not establish runtime correctness.

**Conclusion:** this run is not an Android instrumentation pass and does not qualify the app for Play release. Keep the release gate closed until emulator and physical-device evidence is available.
