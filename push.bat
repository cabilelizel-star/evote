@echo off
cd /d "%~dp0"
git add .
git commit -m "Fix DB connection, add voter fields, fix login redirect"
git push origin main --force
echo.
echo Done! Railway will redeploy automatically.
pause
