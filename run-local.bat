@echo off
setlocal
title eVOTE Local Server

echo ============================================
echo   eVOTE Secured Ballot - Local Server
echo ============================================
echo.

:: ── 1. Find Java ─────────────────────────────────────────────
set JAVA_HOME=
if exist "C:\Program Files\Java\jdk-25\bin\java.exe" set JAVA_HOME=C:\Program Files\Java\jdk-25
if exist "C:\Program Files\Java\jdk-21\bin\java.exe" set JAVA_HOME=C:\Program Files\Java\jdk-21
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set JAVA_HOME=C:\Program Files\Java\jdk-17
if defined JAVA_HOME goto java_found
where java >nul 2>&1
if %errorlevel%==0 goto java_found
echo [ERROR] Java not found. Please install Java 17+ from https://adoptium.net
pause
exit /b 1
:java_found
echo [OK] Java found

:: ── 2. Find Maven ─────────────────────────────────────────────
set MVN=
if exist "C:\maven\bin\mvn.cmd"                          set MVN=C:\maven\bin\mvn.cmd
if exist "C:\tools\maven\bin\mvn.cmd"                    set MVN=C:\tools\maven\bin\mvn.cmd
if exist "C:\Program Files\Maven\bin\mvn.cmd"            set MVN=C:\Program Files\Maven\bin\mvn.cmd
if defined MVN goto mvn_found
where mvn >nul 2>&1
if %errorlevel%==0 set MVN=mvn & goto mvn_found

:: ── Auto-download Maven ───────────────────────────────────────
echo [*] Maven not found. Downloading Maven 3.9.6 (one-time setup)...
powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile 'C:\maven-dl.zip' -UseBasicParsing"
powershell -Command "Expand-Archive -Path 'C:\maven-dl.zip' -DestinationPath 'C:\maven-tmp' -Force"
xcopy /E /I /Y "C:\maven-tmp\apache-maven-3.9.6" "C:\maven\" >nul
del "C:\maven-dl.zip" >nul 2>&1
rmdir /S /Q "C:\maven-tmp" >nul 2>&1
set MVN=C:\maven\bin\mvn.cmd

:mvn_found
echo [OK] Maven: %MVN%
echo.

:: ── 3. Set PATH and Run ───────────────────────────────────────
if defined JAVA_HOME set PATH=%JAVA_HOME%\bin;%PATH%

set ROOT=%~dp0

echo [*] Building and starting eVOTE...
echo.
echo     Open your browser and go to:
echo     ^>^>^> http://localhost:8080 ^<^<^<
echo.
echo     Admin login : Admin / Admin123
echo     Press Ctrl+C to stop
echo.

"%MVN%" -f "%ROOT%pom.xml" spring-boot:run

echo.
echo [*] Server stopped.
pause
endlocal
