#!/usr/bin/env bash

set -Eeuo pipefail

MYSQL_DATABASE="${MYSQL_DATABASE:-xxl_job}"
MYSQL_USER="${MYSQL_USER:-xxl_job}"
SERVER_PORT="${SERVER_PORT:-8080}"
SERVER_SERVLET_CONTEXT_PATH="${SERVER_SERVLET_CONTEXT_PATH:-/xxl-job-admin}"
DATABASE_STARTUP_TIMEOUT="${DATABASE_STARTUP_TIMEOUT:-180}"

if [[ -z "${MYSQL_PASSWORD:-}" || -z "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  echo "MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD are required" >&2
  exit 1
fi

if [[ "$MYSQL_DATABASE" != "xxl_job" ]]; then
  echo "MYSQL_DATABASE must be xxl_job because the bundled schema uses that database" >&2
  exit 1
fi

if [[ -z "$MYSQL_USER" || "$MYSQL_USER" == "root" ]]; then
  echo "MYSQL_USER must be a non-root application user" >&2
  exit 1
fi

export MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD MYSQL_ROOT_PASSWORD
export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL:-jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai}"
export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$MYSQL_USER}"
export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$MYSQL_PASSWORD}"
export SERVER_PORT SERVER_SERVLET_CONTEXT_PATH
export LOG_HOME="${LOG_HOME:-/data/applogs}"
JAVA_OPTS="${JAVA_OPTS:-}"
PARAMS="${PARAMS:-}"

mysql_pid=""
admin_pid=""

shutdown_children() {
  local exit_code=$?

  trap - EXIT INT TERM

  if [[ -n "$admin_pid" ]] && kill -0 "$admin_pid" 2>/dev/null; then
    kill -TERM "$admin_pid" 2>/dev/null || true
  fi
  if [[ -n "$mysql_pid" ]] && kill -0 "$mysql_pid" 2>/dev/null; then
    kill -TERM "$mysql_pid" 2>/dev/null || true
  fi

  [[ -z "$admin_pid" ]] || wait "$admin_pid" 2>/dev/null || true
  [[ -z "$mysql_pid" ]] || wait "$mysql_pid" 2>/dev/null || true

  exit "$exit_code"
}

trap 'exit 143' TERM
trap 'exit 130' INT
trap shutdown_children EXIT

docker-entrypoint.sh mysqld &
mysql_pid=$!

echo "Waiting for the bundled MySQL database..."
deadline=$((SECONDS + DATABASE_STARTUP_TIMEOUT))
until MYSQL_PWD="$MYSQL_PASSWORD" mysqladmin ping --silent --host=127.0.0.1 --user="$MYSQL_USER"; do
  if ! kill -0 "$mysql_pid" 2>/dev/null; then
    wait "$mysql_pid"
    exit $?
  fi
  if (( SECONDS >= deadline )); then
    echo "MySQL did not become ready within ${DATABASE_STARTUP_TIMEOUT}s" >&2
    exit 1
  fi
  sleep 1
done

echo "Starting XXL-JOB Boost admin on port ${SERVER_PORT}..."
# JAVA_OPTS and PARAMS intentionally support the same interface as the admin-only image.
# shellcheck disable=SC2086
java $JAVA_OPTS -Dlogging.config=file:/opt/xxl-job-boost/logback.xml \
  ${LOG_HOME:+-DLOG_HOME="$LOG_HOME"} -jar /opt/xxl-job-boost/app.jar $PARAMS &
admin_pid=$!

set +e
wait -n "$mysql_pid" "$admin_pid"
exit_code=$?
set -e

if ! kill -0 "$mysql_pid" 2>/dev/null; then
  echo "Bundled MySQL exited; stopping the container" >&2
elif ! kill -0 "$admin_pid" 2>/dev/null; then
  echo "XXL-JOB Boost admin exited; stopping the container" >&2
fi

exit "$exit_code"
