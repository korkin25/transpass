#!/usr/bin/env bash
set -euo pipefail
apk=${1:-app/build/outputs/apk/debug/app-debug.apk}
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
aapt="$ANDROID_HOME/build-tools/35.0.0/aapt"
permissions=$("$aapt" dump permissions "$apk")
if printf '%s\n' "$permissions" | grep -q 'uses-permission:'; then
  printf 'FAIL: APK requests permissions\n%s\n' "$permissions" >&2
  exit 1
fi
printf 'PASS: packaged APK requests no permissions\n'
