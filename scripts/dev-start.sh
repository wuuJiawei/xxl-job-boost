#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_ROOT="${LOG_ROOT:-/tmp/xxl-job-runtime-logs}"
RUN_ROOT="${RUN_ROOT:-/tmp/xxl-job-boost-run}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-xxljob-mysql}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root_pwd}"
MYSQL_DATABASE="${MYSQL_DATABASE:-xxl_job}"
PROJECT_VERSION="${PROJECT_VERSION:-1.0.0}"
JAVA_HOME_DEFAULT="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

ADMIN_PID_FILE="$RUN_ROOT/admin.pid"
EXECUTOR_PID_FILE="$RUN_ROOT/executor.pid"
ADMIN_LOG_FILE="$RUN_ROOT/admin.out"
EXECUTOR_LOG_FILE="$RUN_ROOT/executor.out"

ADMIN_JAR="$ROOT_DIR/xxl-job-admin/target/xxl-job-admin-${PROJECT_VERSION}.jar"
EXECUTOR_JAR="$ROOT_DIR/xxl-job-executor-samples/xxl-job-executor-sample-springboot/target/xxl-job-executor-sample-springboot-${PROJECT_VERSION}.jar"
SQL_FILE="$ROOT_DIR/docs/db/install-xxl-job-boost.sql"

mkdir -p "$LOG_ROOT/xxl-job" "$LOG_ROOT/jobhandler" "$RUN_ROOT"

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
  local version raw

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
    candidate_home="$JAVA_HOME_DEFAULT"
    candidate_bin="$candidate_home/bin/java"
    require_file "$candidate_bin"
    major="$(java_major_version "$candidate_bin")"
  fi

  if [[ -z "$major" || "$major" -lt 17 ]]; then
    echo "JDK 17+ is required, current java is too old" >&2
    exit 1
  fi

  JAVA_HOME="$candidate_home"
  JAVA_BIN="$candidate_bin"
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
}

is_pid_running() {
  local pid="$1"
  kill -0 "$pid" >/dev/null 2>&1
}

start_mysql() {
  require_cmd docker

  if ! docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
    if docker ps -a --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
      docker start "$MYSQL_CONTAINER" >/dev/null
    else
      docker run -d \
        --name "$MYSQL_CONTAINER" \
        -e "MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD" \
        -e "MYSQL_DATABASE=$MYSQL_DATABASE" \
        -p "$MYSQL_PORT:3306" \
        mysql:8.0 \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci >/dev/null
    fi
  fi

  until docker exec "$MYSQL_CONTAINER" mysqladmin ping -uroot "-p$MYSQL_ROOT_PASSWORD" --silent >/dev/null 2>&1; do
    sleep 2
  done

  if ! docker exec "$MYSQL_CONTAINER" mysql -uroot "-p$MYSQL_ROOT_PASSWORD" -Nse \
    "select 1 from information_schema.tables where table_schema='$MYSQL_DATABASE' and table_name='xxl_job_user'" \
    | grep -qx '1'; then
    docker exec "$MYSQL_CONTAINER" sh -lc "mysql -uroot -p$MYSQL_ROOT_PASSWORD < /dev/stdin" < "$SQL_FILE"
  fi
}

build_jars() {
  require_cmd mvn

  if [[ ! -f "$ADMIN_JAR" || ! -f "$EXECUTOR_JAR" ]]; then
    (
      cd "$ROOT_DIR"
      mvn -P 'apps,!release' -pl xxl-job-admin,xxl-job-executor-samples/xxl-job-executor-sample-springboot -am -DskipTests package
    )
  fi
}

kill_xxl_processes() {
  local pattern="$1"
  local pids=""

  pids="$(pgrep -f "$pattern" || true)"
  if [[ -z "$pids" ]]; then
    return
  fi

  while read -r pid; do
    [[ -n "$pid" ]] || continue
    kill "$pid" >/dev/null 2>&1 || true
  done <<< "$pids"

  sleep 2

  while read -r pid; do
    [[ -n "$pid" ]] || continue
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  done <<< "$pids"
}

cleanup_ports() {
  local port pid cmd

  for port in 8080 8081 9999; do
    pid="$(lsof -tiTCP:"$port" -sTCP:LISTEN || true)"
    [[ -n "$pid" ]] || continue

    cmd="$(ps -p "$pid" -o command= || true)"
    if [[ "$cmd" == *"xxl-job-admin"* || "$cmd" == *"xxl-job-executor-sample-springboot"* ]]; then
      kill "$pid" >/dev/null 2>&1 || true
      sleep 1
      if kill -0 "$pid" >/dev/null 2>&1; then
        kill -9 "$pid" >/dev/null 2>&1 || true
      fi
    else
      echo "port $port is occupied by non-xxl-job process: $cmd" >&2
      exit 1
    fi
  done
}

start_service() {
  local name="$1"
  local pid_file="$2"
  local log_file="$3"
  shift 3

  if [[ -f "$pid_file" ]]; then
    local old_pid
    old_pid="$(<"$pid_file")"
    if [[ -n "$old_pid" ]] && is_pid_running "$old_pid"; then
      echo "$name already running with pid $old_pid"
      return
    fi
    rm -f "$pid_file"
  fi

  nohup "$@" >"$log_file" 2>&1 &
  local pid=$!
  echo "$pid" >"$pid_file"
  echo "started $name with pid $pid"
}

wait_for_port() {
  local port="$1"
  local retries="${2:-30}"

  for ((i = 0; i < retries; i++)); do
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  return 1
}

resolve_service_pid() {
  local pattern="$1"
  pgrep -f "$pattern" | tail -n 1
}

refresh_pid_file() {
  local pid_file="$1"
  local pattern="$2"
  local pid=""

  pid="$(resolve_service_pid "$pattern" || true)"
  if [[ -n "$pid" ]]; then
    echo "$pid" >"$pid_file"
  fi
}

start_mysql
resolve_java
export LOG_HOME="$LOG_ROOT"
build_jars
require_file "$ADMIN_JAR"
require_file "$EXECUTOR_JAR"
kill_xxl_processes "xxl-job-admin-${PROJECT_VERSION}.jar"
kill_xxl_processes "xxl-job-executor-sample-springboot-${PROJECT_VERSION}.jar"
cleanup_ports

start_service \
  admin \
  "$ADMIN_PID_FILE" \
  "$ADMIN_LOG_FILE" \
  "$JAVA_BIN" -DLOG_HOME="$LOG_HOME" -jar "$ADMIN_JAR"

wait_for_port 8080 || {
  echo "admin failed to bind 8080, see $ADMIN_LOG_FILE" >&2
  exit 1
}
refresh_pid_file "$ADMIN_PID_FILE" "$ADMIN_JAR"

start_service \
  executor \
  "$EXECUTOR_PID_FILE" \
  "$EXECUTOR_LOG_FILE" \
  "$JAVA_BIN" \
    -DLOG_HOME="$LOG_HOME" \
    -Dxxl.job.executor.logpath="$LOG_ROOT/jobhandler" \
    -jar "$EXECUTOR_JAR"

wait_for_port 8081 || {
  echo "executor failed to bind 8081, see $EXECUTOR_LOG_FILE" >&2
  exit 1
}

wait_for_port 9999 || {
  echo "executor failed to bind 9999, see $EXECUTOR_LOG_FILE" >&2
  exit 1
}
refresh_pid_file "$EXECUTOR_PID_FILE" "$EXECUTOR_JAR"

echo "admin:    http://127.0.0.1:8080/xxl-job-admin/"
echo "admin-ui: http://127.0.0.1:8080/xxl-job-admin/admin-next/"
echo "executor: http://127.0.0.1:8081/"
