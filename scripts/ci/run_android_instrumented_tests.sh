#!/usr/bin/env bash
set -euo pipefail

expected_api="\${SWEEP_EXPECTED_API_LEVEL:-}"
if [[ ! "$expected_api" =~ ^[0-9]+$ ]]; then
  echo "SWEEP_EXPECTED_API_LEVEL must be set to the matrix Android API level." >&2
  exit 2
fi

diagnostics_dir="app/build/ci-diagnostics"
mkdir -p "$diagnostics_dir"
serial="$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" { print $1; exit }')"

capture_diagnostics() {
  local reason="$1"
  printf '%s\n' "$reason" | tee "$diagnostics_dir/emulator-readiness-failure.txt"
  adb devices -l > "$diagnostics_dir/adb-devices.txt" 2>&1 || true
  if [[ -n "$serial" ]]; then
    adb -s "$serial" get-state > "$diagnostics_dir/adb-state.txt" 2>&1 || true
    adb -s "$serial" shell getprop > "$diagnostics_dir/emulator-getprop.txt" 2>&1 || true
    adb -s "$serial" shell service list > "$diagnostics_dir/android-services.txt" 2>&1 || true
    adb -s "$serial" logcat -b all -d -t 1500 > "$diagnostics_dir/emulator-logcat.txt" 2>&1 || true
  fi
}

if [[ -z "$serial" ]]; then
  capture_diagnostics "No booted emulator is visible to ADB."
  exit 1
fi

ready=false
api=""
boot=""
package_service=""
activity_service=""
for attempt in $(seq 1 90); do
  state="$(adb -s "$serial" get-state 2>/dev/null || true)"
  api="$(adb -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || true)"
  boot="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  package_service="$(adb -s "$serial" shell service check package 2>&1 || true)"
  activity_service="$(adb -s "$serial" shell service check activity 2>&1 || true)"

  if [[ "$state" == "device" &&
        "$api" == "$expected_api" &&
        "$boot" == "1" &&
        "$package_service" == *found* &&
        "$activity_service" == *found* ]]; then
    ready=true
    break
  fi
  sleep 2
done

if [[ "$ready" != true ]]; then
  capture_diagnostics "Emulator not Android-ready: expected_api=$expected_api actual_api=$api boot=$boot package_service=$package_service activity_service=$activity_service"
  exit 1
fi

echo "Emulator ready: serial=$serial api=$api package_service=found activity_service=found"
./gradlew --no-daemon :app:connectedDebugAndroidTest 2>&1 | tee "$diagnostics_dir/connectedDebugAndroidTest.log"

python3 - "$diagnostics_dir/instrumented-test-summary.txt" <<'PY'
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

report_root = Path("app/build/outputs/androidTest-results/connected")
testcases = []
errors = []
for xml_path in report_root.rglob("*.xml") if report_root.exists() else []:
    try:
        root = ET.parse(xml_path).getroot()
    except ET.ParseError as exc:
        errors.append(f"{xml_path}: invalid XML: {exc}")
        continue
    testcases.extend(root.findall(".//testcase"))

failure_count = sum(
    1 for case in testcases
    if case.find("failure") is not None or case.find("error") is not None
)
summary = (
    f"instrumented_testcases={len(testcases)} "
    f"failures_or_errors={failure_count} "
    f"malformed_reports={len(errors)}"
)
Path(sys.argv[1]).write_text(summary + "\n", encoding="utf-8")
print(summary)
for error in errors:
    print(error, file=sys.stderr)

if not testcases:
    print("No instrumented test cases were reported; refusing to mark this job successful.", file=sys.stderr)
    sys.exit(1)
if errors or failure_count:
    sys.exit(1)
PY
