@echo off
setlocal enabledelayedexpansion
title eVOTE Local Server

echo ============================================
echo   eVOTE Secured Ballot - Local Server
echo ============================================
echo.

:: ── 1. Find Java ─────────────────────────────────────────────
set JAVA_HOME=
for %%J in (
    "C:\Program Files\Java\jdk-25"
    "C:\Program Files\Java\jdk-21"
    "C:\Program Files\Java\jdk-17"
    "C:\Program Files\Eclipse Adoptium\jdk-21"
    "C:\Program Files\Microsoft\jdk-21"
) do (
    if exist "%%~J\bin\java.exe" (
        set JAVA_HOME=%%~J
        goto java_found
    )
)
:: fallback: use java from PATH
where java >nul 2>&1
if %errorlevel%==0 goto java_found
echo [ERROR] Java not found. Please install Java 17+ from https://adoptium.net
pause & exit /b 1
:java_found
echo [OK] Java: %JAVA_HOME%

:: ── 2. Find or Download Maven ─────────────────────────────────
set MVN=
:: Check common locations
for %%M in (
    "C:\maven\bin\mvn.cmd"
    "C:\tools\maven\bin\mvn.cmd"
    "C:\Program Files\Maven\bin\mvn.cmd"
) do (
    if exist %%M (
        set MVN=%%M
        goto mvn_found
    )
)
:: Check Downloads folder
for /d %%D in ("C:\Users\%USERNAME%\Downloads\apache-maven-*") do (
    if exist "%%D\bin\mvn.cmd" ( set MVN=%%D\bin\mvn.cmd & goto mvn_found )
    for /d %%E in ("%%D\apache-maven-*") do (
        if exist "%%E\bin\mvn.cmd" ( set MVN=%%E\bin\mvn.cmd & goto mvn_found )
    )
)
:: Check if mvn is on PATH
where mvn >nul 2>&1
if %errorlevel%==0 ( set MVN=mvn & goto mvn_found )

:: ── Auto-download Maven ───────────────────────────────────────
echo [*] Maven not found. Downloading Maven 3.9.6...
set MVN_URL=https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip
set MVN_ZIP=C:\maven-download.zip
set MVN_DIR=C:\maven

powershell -Command "Write-Host 'Downloading...'; Invoke-WebRequest -Uri '%MVN_URL%' -OutFile '%MVN_ZIP%' -UseBasicParsing"
if not exist "%MVN_ZIP%" (
    echo [ERROR] Download failed. Please install Maven manually from https://maven.apache.org
    pause & exit /b 1
)
echo [*] Extracting Maven to C:\maven ...
powershell -Command "Expand-Archive -Path '%MVN_ZIP%' -DestinationPath 'C:\maven-tmp' -Force"
:: Move the inner folder to C:\maven
for /d %%D in ("C:\maven-tmp\apache-maven-*") do (
    if exist "%%D\bin\mvn.cmd" (
        xcopy /E /I /Y "%%D" "C:\maven\" >nul
    )
)
del "%MVN_ZIP%" >nul 2>&1
rmdir /S /Q "C:\maven-tmp" >nul 2>&1
set MVN=C:\maven\bin\mvn.cmd

:mvn_found
echo [OK] Maven: %MVN%
echo.

:: ── 3. Build and Run ──────────────────────────────────────────
set ROOT=%~dp0

echo [*] Building application (first time may take 1-2 minutes)...
echo [*] This connects to your Railway MySQL database automatically.
echo.
echo     Once started, open your browser and go to:
echo     >>> http://localhost:8080 <<<
echo.
echo     Admin login:  Admin / Admin123
echo     Press Ctrl+C to stop the server
echo.

if defined JAVA_HOME (
    set PATH=%JAVA_HOME%\bin;%PATH%
)

"%MVN%" -f "%ROOT%pom.xml" spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=8080"

echo.
echo [*] Server stopped.
pause
endlocal
