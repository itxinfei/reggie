@echo off
chcp 65001 >nul
rem ==========================================
rem  Reggie 打印代理 - 安装开机自启
rem  注册到当前用户启动项，开机后自动后台运行
rem ==========================================
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v ReggiePrintAgent /t REG_SZ /d "\"%~dp0ReggiePrintAgent.exe\"" /f >nul
if %errorlevel%==0 (
    echo 已安装开机自启：下次开机将自动后台运行打印代理。
    echo 如需本次立即启动，请双击 ReggiePrintAgent.exe。
) else (
    echo 安装失败，请右键本脚本选择“以管理员身份运行”。
)
echo.
pause
