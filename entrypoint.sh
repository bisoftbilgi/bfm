#!/bin/bash
set -e

echo "Starting BFM Spring Boot Application..."

exec java \
  -Dserver.pglist="${BFM_SERVER_HOST}:${BFM_SERVER_PORT},${MINIPG_SECONDARY_HOST}:${MINIPG_SECONDARY_PORT}" \
  -Dserver.pguser=${BFM_SERVER_USERNAME} \
  -Dserver.pgpassword=${BFM_SERVER_PASSWORD} \
  -Dspring.jackson.serialization.FAIL_ON_EMPTY_BEANS=${SPRING_JACKSON_SERIALIZATION_FAIL_ON_EMPTY_BEANS} \
  -jar /app/app.jar
