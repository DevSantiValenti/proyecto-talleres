#!/usr/bin/env bash
set -euo pipefail
if [[ ${1:-} != "--confirm" || $# != 2 ]]; then
  echo "Uso: $0 --confirm ruta/respaldo.sql.gz"
  echo "Restaura la base configurada en compose.yaml. Detener la app y conservar un respaldo previo."
  exit 1
fi
backup_file=$(realpath "$2")
gzip -t "$backup_file"
cd "$(dirname "$0")/.."
if docker compose ps --status running --services | grep -qx app; then
  echo "Detené la aplicación antes: docker compose stop app"
  exit 1
fi
gzip -dc "$backup_file" | docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u "$MYSQL_USER" "$MYSQL_DATABASE"'
echo "Restauración terminada. Iniciá la aplicación y verificá órdenes, historial y recordatorios."
