@echo off
setlocal
cd /d "%~dp0"
set "JAVA_CMD=java"
if defined JAVA_HOME set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_HOME for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do set "JAVA_CMD=%%D\bin\java.exe"
if not defined JAVA_HOME for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do set "JAVA_CMD=%%D\bin\java.exe"
if not defined JAVA_HOME for /d %%D in ("%~dp0..\..\work\toolchain\jdk-21*") do set "JAVA_CMD=%%D\bin\java.exe"
where node >nul 2>nul
if errorlevel 1 (
 echo Instale Node.js 24 LTS e Java 21. Veja README.md.
 pause
 exit /b 1
)
if not defined ENCRYPTION_KEY (
 node -e "const fs=require('fs'),c=require('crypto'),p=process.argv[1];fs.mkdirSync(require('path').dirname(p),{recursive:true});if(!fs.existsSync(p))fs.writeFileSync(p,c.randomBytes(32).toString('hex'));" "%~dp0.runtime\encryption.key"
 set /p ENCRYPTION_KEY=<"%~dp0.runtime\encryption.key"
)
set "DEMO_MODE=true"
echo API Sentinela: mantenha este terminal aberto. Ctrl+C encerra.
echo Modo demonstracao: base sintetica .test habilitada.
cd services\api
"%JAVA_CMD%" -jar "..\..\dist\sentinela-api.jar" --server.address=127.0.0.1
pause
