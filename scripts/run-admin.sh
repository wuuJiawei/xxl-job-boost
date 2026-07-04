#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_HOME_DEFAULT="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
PROJECT_VERSION="${PROJECT_VERSION:-1.0.0}"
LOG_ROOT="${LOG_ROOT:-${LOG_HOME:-/tmp/xxl-job-runtime-logs}}"
RUNTIME_ROOT="${RUNTIME_ROOT:-/tmp/xxl-job-boost-run/runtime}"
SOURCE_JAR="$ROOT_DIR/xxl-job-admin/target/xxl-job-admin-${PROJECT_VERSION}.jar"
RUNTIME_JAR="$RUNTIME_ROOT/xxl-job-admin.jar"

resolve_java_home() {
  local candidate="${JAVA_HOME:-}"
  local version=""

  if [[ -n "$candidate" && -x "$candidate/bin/java" ]]; then
    version="$("$candidate/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
    if [[ "$version" == 1.* ]]; then
      version="$(echo "$version" | awk -F. '{print $2}')"
    else
      version="${version%%.*}"
    fi
    if [[ "$version" -ge 17 ]]; then
      echo "$candidate"
      return
    fi
  fi

  echo "$JAVA_HOME_DEFAULT"
}

JAVA_HOME="$(resolve_java_home)"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
LOG_HOME="$LOG_ROOT"
export LOG_HOME

mkdir -p "$LOG_ROOT/xxl-job"
mkdir -p "$RUNTIME_ROOT"

cp "$SOURCE_JAR" "$RUNTIME_JAR"

exec "$JAVA_HOME/bin/java" \
  -DLOG_HOME="$LOG_HOME" \
  -jar "$RUNTIME_JAR"
