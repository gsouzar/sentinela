#!/bin/sh
set -eu
# Credentials stay in separate environment variables, never in command arguments.
if [ -z "${DATABASE_URL:-}" ]; then
  : "${DATABASE_HOST:?Configure DATABASE_HOST}"
  : "${DATABASE_NAME:?Configure DATABASE_NAME}"
  export DATABASE_URL="jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT:-5432}/${DATABASE_NAME}?sslmode=require"
fi
: "${DATABASE_USER:?Configure DATABASE_USER}"
: "${DATABASE_PASSWORD:?Configure DATABASE_PASSWORD}"
: "${ENCRYPTION_KEY:?Configure ENCRYPTION_KEY and preserve it}"
: "${WEB_ORIGIN:?Configure WEB_ORIGIN}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:--XX:MaxRAMPercentage=65.0 -XX:+ExitOnOutOfMemoryError}"
exec java -jar /app/app.jar
