#!/bin/bash
# Double-clickable macOS launcher. Finder runs .command files in Terminal;
# it will not run a bare .sh. Delegates to run.sh so the Java flags live in one place.
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/run.sh" "$@"
