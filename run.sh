#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
java \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.text=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
  -cp "$DIR/dist/*:$DIR/dist/lib/*" client.Main "$@"
