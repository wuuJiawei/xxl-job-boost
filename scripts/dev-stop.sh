#!/usr/bin/env bash

set -euo pipefail

RUN_ROOT="${RUN_ROOT:-/tmp/xxl-job-boost-run}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-xxljob-mysql}"

stop_pid_file() {
  local name="$1"
  local pid_file="$2"

  if [[ ! -f "$pid_file" ]]; then
    echo "$name not running"
    return
  fi

  local pid
  pid="$(<"$pid_file")"

  if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
    for _ in {1..20}; do
      if ! kill -0 "$pid" >/dev/null 2>&1; then
        break
      fi
      sleep 1
    done
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
    echo "stopped $name pid $pid"
  else
    echo "$name pid file existed but process already gone"
  fi

  rm -f "$pid_file"
}

stop_pid_file admin "$RUN_ROOT/admin.pid"
stop_pid_file executor "$RUN_ROOT/executor.pid"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  docker stop "$MYSQL_CONTAINER" >/dev/null
  echo "stopped mysql container $MYSQL_CONTAINER"
fi
