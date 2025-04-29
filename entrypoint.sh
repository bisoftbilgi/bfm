#!/bin/bash
set -e

echo "Initializing and starting PostgreSQL if needed..."


if [ ! -s "/var/lib/pgsql/data/PG_VERSION" ]; then
  echo "Running initdb..."
  /usr/bin/initdb -D /var/lib/pgsql/data
fi


echo "Starting PostgreSQL server..."
/usr/bin/pg_ctl -D /var/lib/pgsql/data -o "-c listen_addresses='*'" -l /var/lib/pgsql/data/logfile start


sleep 5

echo "Starting BFM Spring Boot Application..."

exec java \
  -Dspring.jackson.serialization.FAIL_ON_EMPTY_BEANS=${SPRING_JACKSON_SERIALIZATION_FAIL_ON_EMPTY_BEANS} \
  -Dbfm.localhost.clustercheck.enabled=false \
  -Dminipg.servers[0].host=${BFM_SERVER_HOST} \
  -Dminipg.servers[0].port=${BFM_SERVER_PORT} \
  -Dminipg.servers[0].username=${BFM_SERVER_USERNAME} \
  -Dminipg.servers[0].password=${BFM_SERVER_PASSWORD} \
  -Dminipg.servers[1].host=${MINIPG_SECONDARY_HOST} \
  -Dminipg.servers[1].port=${MINIPG_SECONDARY_PORT} \
  -Dminipg.servers[1].username=${MINIPG_SECONDARY_USERNAME} \
  -Dminipg.servers[1].password=${MINIPG_SECONDARY_PASSWORD} \
  -jar /app/app.jar
