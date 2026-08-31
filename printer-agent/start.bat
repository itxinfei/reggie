@echo off
rem ============================================
rem Reggie 门店 PC 打印代理 - 启动脚本
rem 前台运行，日志输出到 printer-agent.log
rem ============================================
chcp 65001 >nul
cd /d "%~dp0"

python printer_agent.py
if errorlevel 1 (
    echo.
    echo 代理异常退出，错误码 %errorlevel%
    pause
)
