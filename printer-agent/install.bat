@echo off
rem ============================================
rem Reggie 门店 PC 打印代理 - 安装脚本
rem 1) 安装 Python 3.8+（勾选 Add to PATH）
rem 2) 双击本脚本安装依赖
rem ============================================
chcp 65001 >nul
cd /d "%~dp0"

where python >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 python，请先安装 Python 3.8 及以上版本并勾选 "Add Python to PATH"
    pause
    exit /b 1
)

echo [1/2] 安装核心依赖（requests + pywin32）...
python -m pip install requests pywin32 -i https://pypi.tuna.tsinghua.edu.cn/simple

echo [2/2] 安装可选依赖（二维码打印，失败可忽略）...
python -m pip install qrcode pillow -i https://pypi.tuna.tsinghua.edu.cn/simple

echo.
echo 安装完成。请按顺序：
echo   1) 修改 config.json 中的 server_url（后端地址）与 store_code（门店编码）
echo   2) 双击 start.bat 启动代理
echo   3) 在后台「打印终端」页将新注册终端设为「启用」
pause
