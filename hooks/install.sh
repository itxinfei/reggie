#!/bin/bash
# 安装 UI 规范 git hook 门禁：让 git 使用仓库内 hooks/ 目录（core.hooksPath）。
# 用法：bash hooks/install.sh
set -euo pipefail
cd "$(dirname "$0")/.."

git config core.hooksPath hooks
echo "==> 已启用 pre-commit 门禁（UI 规范检查）。"
echo "    卸载：git config --unset core.hooksPath"
