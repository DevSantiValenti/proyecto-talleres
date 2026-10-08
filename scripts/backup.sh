#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "$0")/.."
mkdir -p backups
backup_file=$(mktemp "backups/lavadero-$(date -u +%Y%m%dT%H%M%SZ)-XXXXXX.sql.gz")
trap 'rm -f "$backup_file"' ERR
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --single-transaction --no-tablespaces --set-gtid-purged=OFF --triggers -u "$MYSQL_USER" "$MYSQL_DATABASE"' | gzip > "$backup_file"
gzip -t "$backup_file"
echo "Respaldo creado: $backup_file"
