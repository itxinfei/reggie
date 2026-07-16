#!/bin/bash
# Allure 报告生成脚本
# 用法: ./generate-report.sh

set -e

echo "========================================"
echo "  生成 Allure 测试报告"
echo "========================================"

# 清理旧报告
if [ -d "allure-report" ]; then
    rm -rf allure-report
fi

# 生成报告
allure generate allure-results --clean -o allure-report

echo ""
echo "✅ 报告生成完成！"
echo ""
echo "查看报告："
echo "  allure open allure-report     # 打开浏览器查看"
echo "  allure serve allure-results   # 实时预览"
echo ""
