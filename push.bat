@echo off
cd /d "%~dp0"
git add .
git commit -m "Rewrite voter dashboard to match admin layout - fix all errors"
git push origin main
echo.
echo Done! Railway will redeploy automatically.
pause
