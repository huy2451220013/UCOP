@echo off
chcp 65001 >nul
title UCOP - Universal Commerce & Operations Platform (Port 8088)
cd /d "%~dp0"

echo =======================================================================
echo   KHOI DONG DU AN FINAL TEST: UCOP (Spring MVC + Thymeleaf + SQL Server)
echo =======================================================================
echo.

set "MAVEN_OPTS=-Xms256m -Xmx512m --enable-native-access=ALL-UNNAMED"

netstat -ano | findstr ":8088 " | findstr "LISTENING" >nul
if %errorlevel%==0 (
    echo [OK] Server UCOP dang chay san tai http://localhost:8088 !
    echo      Dang mo trinh duyet ngay bay gio...
    start http://localhost:8088
    timeout /t 3 /nobreak >nul
    exit /b 0
)

if not exist "target\UCOP.jar" (
    echo [1/2] Dang bien dich va dong goi du an UCOP lan dau...
    call mvnw.cmd clean package -DskipTests
    if errorlevel 1 (
        echo.
        echo [LOI] Bien dich that bai! Vui long kiem tra thong bao loi phia tren.
        pause
        exit /b 1
    )
)

echo [2/2] Dang khoi dong Server tai dia chi: http://localhost:8088
echo       (Dung cong 8088 de khong bi dung voi XAMPP Apache dang chay cong 8080)
echo       Sau khoang 8 giay, trinh duyet se tu dong mo trang chu UCOP!
echo       Nhan Ctrl + C de dung Server.
echo.

start "" cmd /c "timeout /t 8 /nobreak >nul && start http://localhost:8088"
java -Xms256m -Xmx512m --enable-native-access=ALL-UNNAMED -jar "target\UCOP.jar"
pause
