#!/usr/bin/env bash

set -euo pipefail

RUN_ROOT="${RUN_ROOT:-/tmp/xxl-job-boost-run}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-xxljob-mysql}"

print_service() {
  local name="$1"
  local pid_file="$2"

  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(<"$pid_file")"
    if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
      echo "$name: running (pid $pid)"
      return
    fi
    echo "$name: stale pid file ($pid)"
    return
  fi

  echo "$name: stopped"
}

print_service admin "$RUN_ROOT/admin.pid"
print_service executor "$RUN_ROOT/executor.pid"

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
