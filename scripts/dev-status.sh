#!/usr/bin/env bash

set -euo pipefail

RUN_ROOT="${RUN_ROOT:-/tmp/xxl-job-boost-run}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-xxljob-mysql}"
PROJECT_VERSION="${PROJECT_VERSION:-0.9.4}"

ADMIN_PATTERN="${ADMIN_PATTERN:-xxl-job-admin/target/xxl-job-admin-${PROJECT_VERSION}.jar}"
EXECUTOR_PATTERN="${EXECUTOR_PATTERN:-xxl-job-executor-samples/xxl-job-executor-sample-springboot/target/xxl-job-executor-sample-springboot-${PROJECT_VERSION}.jar}"

resolve_service_pid() {
  local pattern="$1"
  pgrep -f "$pattern" | tail -n 1
}

print_service() {
  local name="$1"
  local pid_file="$2"
  local pattern="$3"

  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(<"$pid_file")"
    if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
      echo "$name: running (pid $pid)"
      return
    fi
  fi

  local actual_pid
  actual_pid="$(resolve_service_pid "$pattern" || true)"
  if [[ -n "$actual_pid" ]]; then
    echo "$actual_pid" >"$pid_file"
    echo "$name: running (pid $actual_pid)"
    return
  fi

  echo "$name: stopped"
}

print_service admin "$RUN_ROOT/admin.pid" "$ADMIN_PATTERN"
print_service executor "$RUN_ROOT/executor.pid" "$EXECUTOR_PATTERN"

if command -v docker >/dev/null 2>&1; then
  if docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
    echo "mysql: running ($MYSQL_CONTAINER)"
  elif docker ps -a --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
    echo "mysql: exists but stopped ($MYSQL_CONTAINER)"
  else
    echo "mysql: missing ($MYSQL_CONTAINER)"
  fi
fi

for port in 8080 8081 9999 3306; do
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "port $port: listening"
  else
    echo "port $port: not listening"
  fi
done
