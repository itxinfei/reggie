@echo off
setlocal enabledelayedexpansion

set MYSQL="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
set DB_HOST=localhost
set DB_USER=root
set DB_PASS=
set DB_NAME=reggie_db

echo ============================================
echo MySQL Test Data Import Script
echo ============================================
echo.

echo Step 1: Checking if database exists...
%MYSQL% -u %DB_USER% -p%DB_PASS% -e "SHOW DATABASES;" | findstr /i "%DB_NAME%" >nul
if errorlevel 1 (
    echo Database '%DB_NAME%' not found. Creating...
    %MYSQL% -u %DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if errorlevel 1 (
        echo Failed to create database!
        pause
        exit /b 1
    )
    echo Database created successfully.
) else (
    echo Database '%DB_NAME%' already exists.
)

echo.
echo Step 2: Creating tables...
%MYSQL% -u %DB_USER% -p%DB_PASS% %DB_NAME% < "%~dp0schema.sql"
if errorlevel 1 (
    echo Failed to create tables!
    pause
    exit /b 1
)
echo Tables created successfully.

echo.
echo Step 3: Importing test data...
%MYSQL% -u %DB_USER% -p%DB_PASS% %DB_NAME% < "%~dp0data-test.sql"
if errorlevel 1 (
    echo Failed to import data!
    pause
    exit /b 1
)
echo Test data imported successfully.

echo.
echo Step 4: Verifying data...
echo.
echo Employee count:
%MYSQL% -u %DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT COUNT(*) AS '员工数量' FROM employee;"
echo Dish count:
%MYSQL% -u %DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT COUNT(*) AS '菜品数量' FROM dish;"
echo Order count:
%MYSQL% -u %DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT COUNT(*) AS '订单数量' FROM orders;"
echo Member count:
%MYSQL% -u %DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT COUNT(*) AS '会员数量' FROM member;"

echo.
echo ============================================
echo Import completed successfully!
echo ============================================
pause
