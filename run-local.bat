@echo off
setlocal

:: ── Try C:\maven first (clean install) ───────────────────────
if exist "C:\maven\bin\mvn.cmd" (
    set MVN=C:\maven\bin\mvn.cmd
    goto found
)

:: ── Try the downloaded folder directly ───────────────────────
set DL=C:\Users\Admin\Downloads
for /d %%D in ("%DL%\apache-maven-*") do (
    if exist "%%D\bin\mvn.cmd" (
        set MVN=%%D\bin\mvn.cmd
        goto found
    )
    :: One level deeper (zip extracted with extra folder)
    for /d %%E in ("%%D\apache-maven-*") do (
        if exist "%%E\bin\mvn.cmd" (
            set MVN=%%E\bin\mvn.cmd
            goto found
        )
    )
)

echo [ERROR] Maven not found. Please copy Maven to C:\maven
echo         so that C:\maven\bin\mvn.cmd exists.
pause
exit /b 1

:found
echo [OK] Using Maven: %MVN%
echo.

set JAVA_HOME=C:\Program Files\Java\jdk-25
set ROOT=%~dp0

echo [*] Starting E-Vote web app...
echo     Open http://localhost:8080 in your browser
echo     Press Ctrl+C to stop
echo.

"%MVN%" -f "%ROOT%pom.xml" spring-boot:run

pause
endlocal
