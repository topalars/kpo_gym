@echo off
setlocal
cd /d "%~dp0"
where node >nul 2>nul
if %errorlevel% equ 0 (
  node server.mjs
  exit /b
)
set "FITLIFE_NODE=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
if exist "%FITLIFE_NODE%" (
  "%FITLIFE_NODE%" server.mjs
  exit /b
)
echo Node.js 20+ is required. Install Node.js and run this file again.
pause
