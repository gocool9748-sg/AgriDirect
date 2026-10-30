@echo off
rem Start AgriDirect. Needs only Java 17 or newer (https://adoptium.net).
rem The data is kept in the built-in database file data\agri_market.mv.db - no MySQL needed.
rem   run.bat          start the app
rem   run.bat setup    choose the database again (built-in, or MySQL if you install it later)
rem   run.bat check    run the 27-step self-test
rem   run.bat mysql    optional: download and start a portable MySQL, then use "run.bat setup"
rem   run.bat web      start the website version instead (same as web.bat)
cd /d "%~dp0"
title AgriDirect
where java >nul 2>nul
if errorlevel 1 (
  echo Java is not installed. Install Java 17 or newer from https://adoptium.net and try again.
  pause
  exit /b 1
)
if "%1"=="mysql" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\start-mysql.ps1"
  pause
  exit /b
)
if "%1"=="web" (
  call "%~dp0web.bat" %2
  exit /b
)
if "%1"=="check" (
  java -cp "AgriDirect.jar;lib\*" com.agrimarket.tools.SystemCheck
  pause
  exit /b
)
echo Starting AgriDirect...
start "" javaw -cp "AgriDirect.jar;lib\*" com.agrimarket.Main %1
