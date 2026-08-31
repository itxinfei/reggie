@echo off
chcp 65001 >nul
rem ==========================================
rem  Reggie 打印代理 - 取消开机自启
rem ==========================================
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v ReggiePrintAgent /f >nul 2>&1
echo 已移除开机自启。
echo.
pause
