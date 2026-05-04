@echo off
cd /d "%~dp0"
git add .
git commit -m "Fix ID photo upload: centered preview, compact size, hide hint on upload"
git push origin main
echo.
echo Done! Railway will redeploy automatically.
pause
