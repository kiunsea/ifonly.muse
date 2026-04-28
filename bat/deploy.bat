@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul 2>&1

REM ------------------------------------------------------------
REM deploy.bat
REM Build -> Package -> Deploy(copy) for ifonly.muse
REM ------------------------------------------------------------

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "GRADLE_BAT=%PROJECT_ROOT%\gradlew.bat"
set "APP_YML=%PROJECT_ROOT%\src\main\resources\application.yml"
set "WEB_STATIC_DIR=%PROJECT_ROOT%\src\main\resources\static"
set "WEB_STATIC_IMG_DIR=%WEB_STATIC_DIR%\img"
set "DIST_SOURCE=%PROJECT_ROOT%\packaging\distribution"
set "MUSE_AGENT_ICON_SOURCE=%PROJECT_ROOT%\img\muse-agent.svg"
set "MUSE_AGENT_ICON_STATIC_FILE=%WEB_STATIC_IMG_DIR%\muse-agent.svg"
set "MUSE_AGENT_PNG_SOURCE=%PROJECT_ROOT%\img\muse-agent.png"
set "MUSE_AGENT_PNG_STATIC_FILE=%WEB_STATIC_IMG_DIR%\muse-agent.png"
set "MUSE_BRAND_PNG_SOURCE=%PROJECT_ROOT%\img\muse-brand.png"
set "MUSE_BRAND_PNG_STATIC_FILE=%WEB_STATIC_IMG_DIR%\muse-brand.png"
set "FAVICON_SOURCE=%PROJECT_ROOT%\img\favicon.ico"
set "FAVICON_STATIC_FILE=%WEB_STATIC_DIR%\favicon.ico"
set "FAVICON_DIST_DIR=%DIST_SOURCE%\img"
set "FAVICON_DIST_FILE=%FAVICON_DIST_DIR%\favicon.ico"
set "USER_DOC_SOURCE=%PROJECT_ROOT%\doc\user"
set "CHANGELOG_SOURCE=%PROJECT_ROOT%\CHANGELOG.md"
set "LIBS_DIR=%PROJECT_ROOT%\build\libs"
set "DEFAULT_DEPLOY_DIR=%PROJECT_ROOT%\build\deploy"
set "LOCAL_DEPLOY_DIR=%SCRIPT_DIR%"

set "DEFAULT_BUNDLE_DIR="
set "DEFAULT_ZIP="
set "LOCAL_ZIP="
set "JAR_FILE="
set "VERSION="
set "VERSION_DOC_NAME=VERSION-INFO.md"
set "GIT_COMMIT_HASH=unknown"

echo ========================================
echo ifonly.muse Build/Pack/Deploy Script
echo Project Root: %PROJECT_ROOT%
echo ========================================

if not exist "%GRADLE_BAT%" (
    echo [ERROR] gradlew.bat not found: %GRADLE_BAT%
    exit /b 1
)

if not exist "%APP_YML%" (
    echo [ERROR] application.yml not found: %APP_YML%
    exit /b 1
)

findstr /i /c:"url: https://echo-server.omnibuscode.com" "%APP_YML%" >nul
if errorlevel 1 (
    echo [ERROR] application.yml default echo-server URL mismatch.
    echo         Expected: https://echo-server.omnibuscode.com
    exit /b 1
)

if not exist "%DIST_SOURCE%" (
    echo [ERROR] distribution source not found: %DIST_SOURCE%
    exit /b 1
)

if not exist "%FAVICON_SOURCE%" (
    echo [ERROR] favicon source not found: %FAVICON_SOURCE%
    exit /b 1
)

if not exist "%WEB_STATIC_DIR%" (
    echo [ERROR] web static resource directory not found: %WEB_STATIC_DIR%
    exit /b 1
)

if not exist "%WEB_STATIC_IMG_DIR%" mkdir "%WEB_STATIC_IMG_DIR%"

echo.
echo [sync] Syncing web image assets...
if exist "%MUSE_AGENT_ICON_SOURCE%" (
    copy /y "%MUSE_AGENT_ICON_SOURCE%" "%MUSE_AGENT_ICON_STATIC_FILE%" >nul
    if errorlevel 1 (
        echo [ERROR] Failed to sync muse-agent.svg to static resources.
        exit /b 1
    )
)
if exist "%MUSE_AGENT_PNG_SOURCE%" (
    copy /y "%MUSE_AGENT_PNG_SOURCE%" "%MUSE_AGENT_PNG_STATIC_FILE%" >nul
    if errorlevel 1 (
        echo [ERROR] Failed to sync muse-agent.png to static resources.
        exit /b 1
    )
)
if exist "%MUSE_BRAND_PNG_SOURCE%" (
    copy /y "%MUSE_BRAND_PNG_SOURCE%" "%MUSE_BRAND_PNG_STATIC_FILE%" >nul
    if errorlevel 1 (
        echo [ERROR] Failed to sync muse-brand.png to static resources.
        exit /b 1
    )
)
copy /y "%FAVICON_SOURCE%" "%FAVICON_STATIC_FILE%" >nul
if errorlevel 1 (
    echo [ERROR] Failed to sync favicon.ico to static resources.
    exit /b 1
)
echo [INFO] Synced web assets:
echo        - %MUSE_AGENT_ICON_STATIC_FILE%
echo        - %MUSE_AGENT_PNG_STATIC_FILE%
echo        - %MUSE_BRAND_PNG_STATIC_FILE%
echo        - %FAVICON_STATIC_FILE%

echo.
echo [1/5] Building ifonly.muse (bootJar)...
pushd "%PROJECT_ROOT%"
call "%GRADLE_BAT%" clean bootJar -x test
if errorlevel 1 (
    popd
    echo [ERROR] Build failed.
    exit /b 1
)
popd

echo.
echo [2/5] Resolving generated JAR...
for /f "delims=" %%F in ('dir /b /o:-d "%LIBS_DIR%\ifonly-muse-*.jar" 2^>nul') do (
    set "JAR_FILE=%%F"
    goto :jar_found
)

echo [ERROR] Generated JAR not found in %LIBS_DIR%
exit /b 1

:jar_found
echo [INFO] JAR: %JAR_FILE%

set "VERSION=%JAR_FILE:ifonly-muse-=%"
set "VERSION=%VERSION:.jar=%"
if "%VERSION%"=="" set "VERSION=2.0.0"

echo [INFO] Version: %VERSION%

pushd "%PROJECT_ROOT%"
for /f "delims=" %%H in ('git rev-parse --short=12 HEAD 2^>nul') do (
    set "GIT_COMMIT_HASH=%%H"
    goto :git_hash_found
)

:git_hash_found
popd
echo [INFO] Git Commit: %GIT_COMMIT_HASH%

echo.
echo [3/5] Syncing JAR into distribution folder...
for %%F in (%DIST_SOURCE%\ifonly-muse-*.jar) do (
    if /I not "%%~nxF"=="ifonly-muse-%VERSION%.jar" (
        del /f /q "%%~fF" >nul 2>&1
    )
)
copy /y "%LIBS_DIR%\%JAR_FILE%" "%DIST_SOURCE%\ifonly-muse-%VERSION%.jar" >nul
if errorlevel 1 (
    echo [ERROR] Failed to copy JAR to distribution.
    exit /b 1
)

if not exist "%FAVICON_DIST_DIR%" mkdir "%FAVICON_DIST_DIR%"
copy /y "%FAVICON_SOURCE%" "%FAVICON_DIST_FILE%" >nul
if errorlevel 1 (
    echo [ERROR] Failed to sync favicon to distribution.
    exit /b 1
)
echo [INFO] Synced favicon: %FAVICON_DIST_FILE%

echo.
echo [4/5] Packaging distribution ZIP...
set "DEFAULT_BUNDLE_DIR=%DEFAULT_DEPLOY_DIR%\ifonly.muse-distribution-%VERSION%"
set "DEFAULT_ZIP=%DEFAULT_DEPLOY_DIR%\ifonly.muse-distribution-%VERSION%.zip"

if exist "%DEFAULT_BUNDLE_DIR%" rmdir /s /q "%DEFAULT_BUNDLE_DIR%"
if exist "%DEFAULT_ZIP%" del /f /q "%DEFAULT_ZIP%"

if not exist "%DEFAULT_DEPLOY_DIR%" mkdir "%DEFAULT_DEPLOY_DIR%"

xcopy "%DIST_SOURCE%" "%DEFAULT_BUNDLE_DIR%\" /e /i /y >nul
if errorlevel 1 (
    echo [ERROR] Failed to stage distribution files.
    exit /b 1
)

REM Reset runtime-generated files from deployment bundle (if present)
for /r "%DEFAULT_BUNDLE_DIR%" %%F in (secure-config.properties) do (
    del /f /q "%%~fF" >nul 2>&1
)
for /r "%DEFAULT_BUNDLE_DIR%" %%F in (muse-agent.db) do (
    del /f /q "%%~fF" >nul 2>&1
)
for /r "%DEFAULT_BUNDLE_DIR%" %%F in (muse-agent.db-wal) do (
    del /f /q "%%~fF" >nul 2>&1
)
for /r "%DEFAULT_BUNDLE_DIR%" %%F in (muse-agent.db-shm) do (
    del /f /q "%%~fF" >nul 2>&1
)
for /r "%DEFAULT_BUNDLE_DIR%" %%F in (muse-agent.db-journal) do (
    del /f /q "%%~fF" >nul 2>&1
)

if exist "%DEFAULT_BUNDLE_DIR%\doc\user" rmdir /s /q "%DEFAULT_BUNDLE_DIR%\doc\user"
if exist "%DEFAULT_BUNDLE_DIR%\사용설명서.md" del /f /q "%DEFAULT_BUNDLE_DIR%\사용설명서.md"
if exist "%DEFAULT_BUNDLE_DIR%\설치가이드.md" del /f /q "%DEFAULT_BUNDLE_DIR%\설치가이드.md"
if exist "%USER_DOC_SOURCE%" (
    set "_DOC_COPIED=0"
    for /f "delims=" %%F in ('dir /b /a:-d "%USER_DOC_SOURCE%\*.md" 2^>nul') do (
        if /I not "%%F"=="README.md" (
            copy /y "%USER_DOC_SOURCE%\%%F" "%DEFAULT_BUNDLE_DIR%\%%F" >nul
            if errorlevel 1 (
                echo [ERROR] Failed to copy document: %%F
                exit /b 1
            )
            set "_DOC_COPIED=1"
        )
    )
    if "!_DOC_COPIED!"=="1" (
        echo [INFO] Included user markdown documents at package root.
    ) else (
        echo [WARN] No user markdown documents found to copy from: %USER_DOC_SOURCE%
    )
) else (
    echo [INFO] User document folder not present. Skipping: %USER_DOC_SOURCE%
)

if exist "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%" del /f /q "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo # ifonly.muse Version Info> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo.>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo - Package Version: %VERSION%>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo - JAR File: %JAR_FILE%>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo - Git Commit: %GIT_COMMIT_HASH%>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo - Package Created At (Local): %DATE% %TIME%>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo - Source Script: bat\deploy.bat>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo.>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo ## Notes>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo - This file is auto-generated during packaging.>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"
echo - For release details, see CHANGELOG.md in this package root.>> "%DEFAULT_BUNDLE_DIR%\%VERSION_DOC_NAME%"

if exist "%CHANGELOG_SOURCE%" (
    copy /y "%CHANGELOG_SOURCE%" "%DEFAULT_BUNDLE_DIR%\CHANGELOG.md" >nul
    if errorlevel 1 (
        echo [ERROR] Failed to copy CHANGELOG.md to distribution bundle.
        exit /b 1
    )
    echo [INFO] Included version documents: %VERSION_DOC_NAME%, CHANGELOG.md
) else (
    echo [WARN] CHANGELOG.md not found. Included only %VERSION_DOC_NAME%
)

if exist "%DEFAULT_BUNDLE_DIR%\jre" (
    rmdir /s /q "%DEFAULT_BUNDLE_DIR%\jre"
)

if exist "%DEFAULT_BUNDLE_DIR%\logs" (
    rmdir /s /q "%DEFAULT_BUNDLE_DIR%\logs"
    echo [INFO] Excluded runtime logs folder from package.
)

for %%F in (%DEFAULT_BUNDLE_DIR%\ifonly-muse-*.jar) do (
    if /I not "%%~nxF"=="ifonly-muse-%VERSION%.jar" (
        del /f /q "%%~fF" >nul 2>&1
    )
)
copy /y "%LIBS_DIR%\%JAR_FILE%" "%DEFAULT_BUNDLE_DIR%\ifonly-muse-%VERSION%.jar" >nul

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%DEFAULT_BUNDLE_DIR%\*' -DestinationPath '%DEFAULT_ZIP%' -Force"
if errorlevel 1 (
    echo [ERROR] Failed to create ZIP package.
    exit /b 1
)

echo [INFO] Default package: %DEFAULT_ZIP%

echo.
echo [5/5] Deploy copy to bat\...
if not exist "%LOCAL_DEPLOY_DIR%" mkdir "%LOCAL_DEPLOY_DIR%"

set "LOCAL_ZIP=%LOCAL_DEPLOY_DIR%ifonly.muse-distribution-%VERSION%.zip"
if exist "%LOCAL_ZIP%" del /f /q "%LOCAL_ZIP%"
copy /y "%DEFAULT_ZIP%" "%LOCAL_ZIP%" >nul
if errorlevel 1 (
    echo [ERROR] Failed to copy ZIP to %LOCAL_DEPLOY_DIR%
    exit /b 1
)

echo.
echo ========================================
echo [SUCCESS] ifonly.muse deployment complete.
echo - Default package : %DEFAULT_ZIP%
echo - Copied package  : %LOCAL_ZIP%
echo ========================================

exit /b 0
