#!/usr/bin/env bash

set -euo pipefail

./millw pi.fullLinkJS
cp out/pi/fullLinkJS.dest/main.js public/js/app.js
