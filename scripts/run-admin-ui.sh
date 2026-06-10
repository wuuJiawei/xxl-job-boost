#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UI_DIR="$ROOT_DIR/xxl-job-admin-ui"
NODE_BIN_DEFAULT="/Users/song/.nvm/versions/node/v20.19.0/bin/node"
VITE_BIN_REL="node_modules/vite/bin/vite.js"
PORT="${PORT:-5173}"

resolve_node() {
  local candidate="${NODE_BIN:-}"

  if [[ -n "$candidate" && -x "$candidate" ]]; then
    echo "$candidate"
    return
  fi

  if [[ -x "$NODE_BIN_DEFAULT" ]]; then
    echo "$NODE_BIN_DEFAULT"
    return
  fi

  command -v node
}

NODE_BIN="$(resolve_node)"
export PATH="$(dirname "$NODE_BIN"):/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
export HOME="${HOME:-/Users/song}"

cd "$UI_DIR"
if [[ ! -f "$VITE_BIN_REL" ]]; then
  echo "missing vite binary: $UI_DIR/$VITE_BIN_REL" >&2
  exit 1
fi

exec "$NODE_BIN" "$VITE_BIN_REL" --mode test --host 0.0.0.0 --port "$PORT"
