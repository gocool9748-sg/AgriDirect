@echo off
rem Start AgriDirect as a WEBSITE. Needs only Java 17 or newer (https://adoptium.net).
rem   web.bat        start the website and open http://localhost:8080 in your browser
rem   web.bat lan    also let phones/laptops on the same Wi-Fi open it (http://THIS-PC-IP:8080)
rem Keep the black window open while you use the website; close it to stop the website.
cd /d "%~dp0"
title AgriDirect website
where java >nul 2>nul
if errorlevel 1 (
  echo Java is not installed. Install Java 17 or newer from https://adoptium.net and try again.
  pause
  exit /b 1
)
java -cp "AgriDirect.jar;lib\*" com.agrimarket.web.WebServer open %1
echo.
echo The AgriDirect website has stopped.
pause
