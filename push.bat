@echo off
cd /d "%~dp0"
echo Adding all changes...
git add -A
echo Committing...
git commit -m "Fix voter dashboard: rewrite to match admin layout, fix error overlay"
echo Pushing to Railway...
git push origin main
echo.
echo Done! Check Railway dashboard for deployment status.
pause
