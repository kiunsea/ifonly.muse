@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1
cd /d "%~dp0"

echo ========================================
echo Muse-Agent Integrated Uninstall
echo ========================================

echo.
echo [1/4] Stop and uninstall Windows service...
if exist "service\muse-agent-service.exe" (
    pushd service
    muse-agent-service.exe stop >nul 2>&1
    muse-agent-service.exe uninstall >nul 2>&1
    muse-agent-service.exe status
    popd
) else (
    echo [WARN] service\muse-agent-service.exe not found. Skipping service uninstall.
)

echo.
echo [2/4] Close tray app and Muse-Agent app processes...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$tray = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*Muse-Agent-Tray.ps1*' }; foreach ($p in $tray) { Invoke-CimMethod -InputObject $p -MethodName Terminate | Out-Null }; $app = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*ifonly-muse-*.jar*' }; foreach ($p in $app) { Invoke-CimMethod -InputObject $p -MethodName Terminate | Out-Null }; Write-Host '[INFO] Related processes terminated.'"

echo.
echo [3/4] Delete desktop/start-menu shortcuts...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$desktop = [Environment]::GetFolderPath('Desktop'); $programs = [Environment]::GetFolderPath('Programs'); $targets = @((Join-Path $desktop 'Muse-Agent Tray.lnk'), (Join-Path $desktop 'Muse-Agent Dashboard.url'), (Join-Path $programs 'Muse-Agent Tray.lnk')); foreach ($t in $targets) { if (Test-Path $t) { Remove-Item $t -Force; Write-Host ('[DEL] ' + $t) } else { Write-Host ('[SKIP] ' + $t) } }"

echo.
echo [4/4] Finalize...
echo [OK] Uninstall completed.
echo [INFO] As requested, distribution folder delete step was skipped.
echo [INFO] If needed, delete this folder manually.
pause
