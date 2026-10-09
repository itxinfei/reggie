#!/bin/bash
# 恢复备份：把 docker/backup.sh 导出的 SQL 压缩包灌回 reggie 库（覆盖现有数据）。
# 上传文件包（reggie-uploads-*.tar.gz）可选一并恢复。
# 用法：bash docker/restore.sh backups/reggie-20261006-120000.sql.gz [backups/reggie-uploads-20261006-120000.tar.gz]
set -euo pipefail
cd "$(dirname "$0")/.."

file="${1:?用法: bash docker/restore.sh backups/xxx.sql.gz [uploads.tar.gz]}"
[ -f "$file" ] || { echo "文件不存在: $file" >&2; exit 1; }

uploads_file="${2:-}"
if [ -n "$uploads_file" ] && [ ! -f "$uploads_file" ]; then
  echo "上传文件包不存在: $uploads_file" >&2
  exit 1
fi

echo "==> 即将覆盖 reggie 库现有数据，来源: $file"
[ -n "$uploads_file" ] && echo "==> 同时恢复上传文件，来源: $uploads_file（覆盖同名文件，不删除新增文件）"
read -r -p "确认恢复？(y/N) " confirm
[ "$confirm" = "y" ] || exit 0

gunzip -c "$file" | docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 reggie'

if [ -n "$uploads_file" ]; then
  gunzip -c "$uploads_file" | docker compose exec -T app tar xzf - -C /data/uploads
fi

echo "==> 恢复完成"
