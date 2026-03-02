@echo off
title Underscore Life
cd /d "%~dp0"

echo.
echo   Starting Underscore Life...
echo.

:: ── Check for Node.js ──
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo   Node.js is not installed.
    echo.
    echo   To install it:
    echo     1. Go to https://nodejs.org
    echo     2. Download the LTS version
    echo     3. Run the installer (click Next through everything^)
    echo     4. Restart your computer
    echo     5. Double-click this file again
    echo.
    pause
    exit /b 1
)

:: ── Install dependencies if needed ──
if not exist "node_modules" (
    echo   First time setup — installing dependencies...
    echo   (This takes about 30 seconds^)
    echo.
    call npm install --no-audit --no-fund
    echo.
)

:: ── Start with tunnel so it works on any phone ──
echo   Launching... (keep this window open^)
echo.

set TUNNEL=true
npx tsx src/index.ts

pause
