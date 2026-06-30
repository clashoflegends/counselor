#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
# Apply a staged portable update (UpdateDownloader put the new dist/ at .update/dist). Done before
# the JVM starts so no files are locked; the old dist/ is kept until the new one is verified in
# place, so a failed swap rolls back rather than bricking the install.
if [ -d "$DIR/.update/dist" ]; then
  echo "Applying Counselor update..."
  rm -rf "$DIR/dist.old"
  [ -d "$DIR/dist" ] && mv "$DIR/dist" "$DIR/dist.old"
  if mv "$DIR/.update/dist" "$DIR/dist" && [ -f "$DIR/dist/PbmCounselor.jar" ] && [ -d "$DIR/dist/lib" ]; then
    rm -rf "$DIR/.update" "$DIR/dist.old"
  else
    # failed/incomplete: discard the broken new dist first (it may already exist from the mv), then
    # restore the backup into the now-empty slot so the rollback can't land inside a stale dir.
    rm -rf "$DIR/dist"
    [ -d "$DIR/dist.old" ] && mv "$DIR/dist.old" "$DIR/dist"
  fi
fi
java \
  -splash:"$DIR/dist/teaser_whs01.png" \
  -Dclash.distro=portable-jar \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.text=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
  -cp "$DIR/dist/*:$DIR/dist/lib/*" client.Main "$@"
