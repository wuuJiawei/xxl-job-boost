#!/usr/bin/env bash

set -euo pipefail

RUN_ROOT="${RUN_ROOT:-/tmp/xxl-job-boost-run}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-xxljob-mysql}"

ADMIN_PATTERN="${ADMIN_PATTERN:-xxl-job-admin/target/xxl-job-admin-3.4.1-SNAPSHOT.jar}"
EXECUTOR_PATTERN="${EXECUTOR_PATTERN:-xxl-job-executor-samples/xxl-job-executor-sample-springboot/target/xxl-job-executor-sample-springboot-3.4.1-SNAPSHOT.jar}"

resolve_service_pid() {
  local pattern="$1"
  pgrep -f "$pattern" | tail -n 1
}

stop_pid_file() {
  local name="$1"
  local pid_file="$2"
  local pattern="$3"

  local pid=""
  if [[ -f "$pid_file" ]]; then
    pid="$(<"$pid_file")"
  fi

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
    pid="$(resolve_service_pid "$pattern" || true)"
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
    elif [[ -f "$pid_file" ]]; then
      echo "$name pid file existed but process already gone"
    else
      echo "$name not running"
    fi
  fi

  rm -f "$pid_file"
}

stop_pid_file admin "$RUN_ROOT/admin.pid" "$ADMIN_PATTERN"
stop_pid_file executor "$RUN_ROOT/executor.pid" "$EXECUTOR_PATTERN"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  docker stop "$MYSQL_CONTAINER" >/dev/null
  echo "stopped mysql container $MYSQL_CONTAINER"
fi
