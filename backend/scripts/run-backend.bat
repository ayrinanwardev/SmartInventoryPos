@echo off
rem Runs the backend tests, then starts the backend with backend\application.properties.
rem Double-click it, or run it from any folder. Logs go to backend\data (git-ignored).
cd /d "%~dp0..\.."
if not exist backend\data mkdir backend\data

echo [1/2] Running backend tests (H2, not Supabase)...
call gradlew.bat :backend:test --console=plain > backend\data\test-run.log 2>&1
if errorlevel 1 (
  echo TESTS FAILED. See backend\data\test-run.log
  pause
  exit /b 1
)
echo Tests passed.

echo [2/2] Starting the backend. Server log: backend\data\supabase-run.log
call gradlew.bat :backend:bootRun --console=plain > backend\data\bootrun-console.log 2>&1
echo Backend stopped. See backend\data\bootrun-console.log
pause
