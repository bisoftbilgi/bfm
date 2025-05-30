#!/usr/bin/env bash
set -euo pipefail

echo "Launching BFM Application…"
exec java -jar /app/bfm-app.jar
