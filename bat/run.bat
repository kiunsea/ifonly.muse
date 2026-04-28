@echo off
REM ifonly.muse Launcher
REM Description: Runs the ifonly.muse Spring Boot application

setlocal enabledelayedexpansion

echo.
echo ========================================
echo    ifonly.muse Launcher
echo ========================================
echo.

REM Set project root directory based on script location
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

REM Check if project root exists
if not exist "%PROJECT_ROOT%" (
    echo ERROR: Project root not found.
    echo Path: %PROJECT_ROOT%
    pause
    exit /b 1
)

REM Change to project directory
cd /d "%PROJECT_ROOT%"

REM Resolve ifonly.muse version from build.gradle
set APP_VERSION=unknown
if exist "%PROJECT_ROOT%\build.gradle" (
    for /f "tokens=2 delims==" %%v in ('findstr /R /C:"^version *= *'.*'" "%PROJECT_ROOT%\build.gradle"') do (
        set RAW_VERSION=%%v
        set RAW_VERSION=!RAW_VERSION:'=!
        set RAW_VERSION=!RAW_VERSION: =!
        set APP_VERSION=!RAW_VERSION!
    )
)

echo Directory: %cd%
echo Version: v!APP_VERSION!
echo.
echo Stopping existing ifonly.muse process...

REM Kill existing process using port 8484
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8484') do (
    taskkill /PID %%a /F 2>nul
)

REM Wait briefly
timeout /t 2 /nobreak

echo.
echo Starting Spring Boot application...
echo.
echo Log location: %PROJECT_ROOT%\logs\muse-agent.log
echo.

echo Cleaning log files...
if exist "%PROJECT_ROOT%\logs" (
    del /q "%PROJECT_ROOT%\logs\*" 2>nul
)

REM Run Spring Boot application with clean build (console output)
call gradlew.bat clean bootRun

if !errorlevel! neq 0 (
    echo.
    echo ERROR: Failed to start ifonly.muse.
    pause
    exit /b !errorlevel!
)

pause
