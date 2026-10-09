#!/bin/bash
# 一次性建库容器：按 README「自助建表」路径初始化数据库。
# 幂等：schema*.sql 全部为 CREATE TABLE IF NOT EXISTS，seed-minimal.sql 先清后插，
# 重复执行不会丢数据（但也不会动已有业务数据）。
set -euo pipefail

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"

MYSQL_CMD=(mysql -h mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4)
DB=reggie

echo "[db-init] waiting for mysql ..."
for i in $(seq 1 60); do
  if mysqladmin ping -h mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent 2>/dev/null; then
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "[db-init] mysql not ready after 60s" >&2
    exit 1
  fi
  sleep 1
done

echo "[db-init] ensuring database ${DB} (utf8mb4)"
"${MYSQL_CMD[@]}" -e "CREATE DATABASE IF NOT EXISTS ${DB} CHARACTER SET utf8mb4;"

echo "[db-init] import base schema.sql"
"${MYSQL_CMD[@]}" "${DB}" < /schemas/schema.sql

for f in /schemas/schema-*.sql; do
  echo "[db-init] import $(basename "${f}")"
  "${MYSQL_CMD[@]}" "${DB}" < "${f}"
done

echo "[db-init] import seed-minimal.sql (admin / 123456)"
"${MYSQL_CMD[@]}" "${DB}" < /seed/seed-minimal.sql

echo "[db-init] done"
