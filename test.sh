#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
chrome="${CHROME_BIN:-google-chrome}"
if [[ -z "${CHROME_BIN:-}" && "$OSTYPE" == darwin* ]]; then
  chrome="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
fi

test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
"$chrome" --headless --disable-gpu --no-sandbox \
  --allow-file-access-from-files --user-data-dir="$test_dir/profile" \
  --virtual-time-budget=5000 --dump-dom \
  "file://$PWD/tests/browser-smoke.html" > "$test_dir/result.html" 2> "$test_dir/chrome.log"
if ! grep -q '<pre id="result">PASS: browser smoke test</pre>' "$test_dir/result.html"; then
  cat "$test_dir/result.html" "$test_dir/chrome.log"
  exit 1
fi
echo "PASS: browser smoke test"
