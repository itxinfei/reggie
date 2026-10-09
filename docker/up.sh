#!/bin/bash
# Reggie 一键启动：构建镜像 + 拉起 MySQL/Redis/应用 + 自动建库导种子数据。
# 用法：bash scripts/docker-up.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> docker compose up -d --build"
docker compose up -d --build

echo "==> waiting for app health ..."
for i in $(seq 1 60); do
  status=$(docker inspect --format '{{.State.Health.Status}}' reggie-app 2>/dev/null || echo unknown)
  if [ "$status" = "healthy" ]; then
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "==> app 未在 5 分钟内就绪，查看日志：docker compose logs -f app" >&2
    exit 1
  fi
  sleep 5
done

port=$(docker compose port app 8080 | sed 's/.*://')
base="http://localhost:${port:-8080}"
echo ""
echo "==> Reggie 已就绪：${base}"
echo "    管理后台  ${base}/backend/index.html   (admin / 123456)"
echo "    用户点餐  ${base}/front/index.html"
echo "    骑手端    ${base}/rider/login.html"
echo ""
echo "==> 手机/局域网访问：把 .env 中 REGGIE_SERVER_URL 改为宿主机局域网 IP 后重启。"
echo "==> 首次登录后请立即修改默认密码。"
