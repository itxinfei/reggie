@echo off
chcp 65001 >nul
rem ==========================================
rem  Reggie 打印代理 - 停止后台运行
rem ==========================================
taskkill /IM ReggiePrintAgent.exe /F >nul 2>&1
if %errorlevel%==0 (
    echo 打印代理已停止。
) else (
    echo 打印代理未在运行。
)
echo.
pause
