#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_HOME_DEFAULT="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "missing command: $1" >&2
    exit 1
  }
}

require_file() {
  [[ -f "$1" ]] || {
    echo "missing file: $1" >&2
    exit 1
  }
}

java_major_version() {
  local java_bin="$1"
  local raw version

  raw="$("$java_bin" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
  version="${raw%%.*}"
  if [[ "$version" == "1" ]]; then
    version="$(echo "$raw" | awk -F. '{print $2}')"
  fi
  echo "$version"
}

resolve_java() {
  local candidate_home="${JAVA_HOME:-}"
  local candidate_bin=""
  local major=""

  if [[ -n "$candidate_home" && -x "$candidate_home/bin/java" ]]; then
    candidate_bin="$candidate_home/bin/java"
    major="$(java_major_version "$candidate_bin")"
  fi

  if [[ -z "$candidate_bin" || -z "$major" || "$major" -lt 17 ]]; then
    if [[ -x "$JAVA_HOME_DEFAULT/bin/java" ]]; then
      candidate_home="$JAVA_HOME_DEFAULT"
    elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
      candidate_home="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    fi
    candidate_bin="$candidate_home/bin/java"
    require_file "$candidate_bin"
    major="$(java_major_version "$candidate_bin")"
  fi

  if [[ -z "$major" || "$major" -lt 17 ]]; then
    echo "JDK 17+ is required, current java is too old" >&2
    exit 1
  fi

  export JAVA_HOME="$candidate_home"
  export PATH="$JAVA_HOME/bin:$PATH"
}

require_cmd pnpm
require_cmd mvn
require_cmd docker
resolve_java

(
  cd "$ROOT_DIR/xxl-job-admin-ui"
  pnpm install --frozen-lockfile
  pnpm build
)

(
  cd "$ROOT_DIR"
  mvn -P '!release' -pl xxl-job-admin,xxl-job-executor-samples/xxl-job-executor-sample-springboot -am -DskipTests package
  docker compose -f docker/docker-compose.yml build xxl-job-admin xxl-job-executor-sample-springboot
)
