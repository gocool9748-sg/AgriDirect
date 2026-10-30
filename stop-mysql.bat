@echo off
rem Stops the portable MySQL that run.bat started (it keeps running in the background until you stop it or restart the PC).
taskkill /IM mysqld.exe /F >nul 2>nul && echo MySQL stopped. || echo MySQL was not running.
timeout /t 2 >nul
