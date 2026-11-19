#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/xraylink}"
TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
mkdir -p "${BACKUP_DIR}"

pg_dump "${DATABASE_URL:?DATABASE_URL is required}" | gzip > "${BACKUP_DIR}/xraylink_${TIMESTAMP}.sql.gz"
find "${BACKUP_DIR}" -type f -name "*.sql.gz" -mtime +14 -delete
echo "Backup completed: ${BACKUP_DIR}/xraylink_${TIMESTAMP}.sql.gz"
