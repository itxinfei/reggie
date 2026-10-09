#!/bin/bash
# 数据备份：全库 SQL + 上传文件卷（菜品图/头像/发票 PDF 等），均导出到 backups/。
# 用法：bash docker/backup.sh [--db-only]
set -euo pipefail
cd "$(dirname "$0")/.."

mkdir -p backups
ts=$(date +%Y%m%d-%H%M%S)

if [ "${1:-}" != "--db-only" ]; then
  uploads_file="backups/reggie-uploads-$ts.tar.gz"
  # 直接从 app 容器打包 /data/uploads（卷名带 compose 项目前缀，按卷名定位不可靠）
  docker compose exec -T app tar czf - -C /data/uploads . > "$uploads_file"
  echo "==> 已备份上传文件：$uploads_file ($(du -h "$uploads_file" | cut -f1))"
fi

db_file="backups/reggie-$ts.sql.gz"
docker compose exec -T mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 --single-transaction --routines --triggers reggie' \
  | gzip > "$db_file"

echo "==> 已备份数据库：$db_file ($(du -h "$db_file" | cut -f1))"
