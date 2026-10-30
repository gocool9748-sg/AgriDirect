@echo off
rem Recompile AgriDirect.jar from the source code after you change it. Needs a JDK 17+ (javac).
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out --release 17 @sources.txt
if errorlevel 1 (
  del sources.txt
  echo Build failed.
  pause
  exit /b 1
)
del sources.txt
xcopy /e /i /q src\main\resources out >nul
echo Main-Class: com.agrimarket.Main> manifest.txt
echo Class-Path: lib/mariadb-java-client-2.7.6.jar lib/h2-2.2.220.jar>> manifest.txt
jar cfm AgriDirect.jar manifest.txt -C out .
del manifest.txt
echo Built AgriDirect.jar. Run it with run.bat
pause
