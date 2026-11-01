@echo off
setlocal
cd /d "%~dp0apps\web"
if not exist node_modules (
 call npm.cmd ci
 if errorlevel 1 exit /b 1
)
if not exist .next\BUILD_ID (
 call npm.cmd run build
 if errorlevel 1 exit /b 1
)
echo Abra http://localhost:3000. Ctrl+C encerra este terminal.
call npm.cmd run start
pause
