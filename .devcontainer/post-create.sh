#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

if mvn clean install -P no-gpg --no-transfer-progress; then
  echo "Build OK"
else
  echo "Build FAILED - revisa los errores"
  exit 1
fi
