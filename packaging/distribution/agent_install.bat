@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1
cd /d "%~dp0"

echo ========================================
echo Muse-Agent Integrated Install
echo ========================================

set "DASHBOARD_URL=http://127.0.0.1:8484"
set "DASHBOARD_WAIT_SEC=45"
set "SERVICE_PORT=8484"
set "APP_JAR="

for /f "delims=" %%F in ('dir /b /o:-d "muse-agent-*.jar" 2^>nul') do (
    if not defined APP_JAR set "APP_JAR=%%F"
)

if not exist "service\muse-agent-service.exe" (
    echo [ERROR] service\muse-agent-service.exe not found.
    echo Please place WinSW executable first.
    pause
    exit /b 1
)

if not defined APP_JAR (
    echo [ERROR] muse-agent-*.jar not found.
    pause
    exit /b 1
)

echo [INFO] Application JAR: %APP_JAR%

if not exist "Muse-Agent-Tray.ps1" (
    echo [ERROR] Muse-Agent-Tray.ps1 not found.
    pause
    exit /b 1
)

echo.
echo [0/5] Unblock downloaded files...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-ChildItem -Path . -Recurse -File | Unblock-File -ErrorAction SilentlyContinue"

echo.
echo [1/5] Ensure bundled JRE is available...
if not exist "jre\bin\java.exe" (
    if exist "download-jre.ps1" (
        powershell -NoProfile -ExecutionPolicy Bypass -File ".\download-jre.ps1"
    ) else (
        echo [ERROR] download-jre.ps1 not found.
        pause
        exit /b 1
    )
)

if not exist "jre\bin\java.exe" (
    echo [ERROR] Bundled JRE setup failed. Cannot continue.
    pause
    exit /b 1
)

echo.
echo [INFO] Sync service XML with detected JAR...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$jar='%APP_JAR%'; $path='service\\muse-agent-service.xml'; try { [xml]$xml = Get-Content -Raw -Encoding UTF8 $path; $node = $xml.SelectSingleNode('/service/arguments'); if ($null -eq $node) { throw 'Missing /service/arguments node.' }; $node.InnerText = ('-Xms256m -Xmx1024m -jar "%%BASE%%\\..\\{0}"' -f $jar); $xml.Save((Resolve-Path $path)); [xml]$verify = Get-Content -Raw -Encoding UTF8 $path; $verifyNode = $verify.SelectSingleNode('/service/arguments'); Write-Host ('[INFO] Service XML sync complete: ' + $verifyNode.InnerText) } catch { Write-Host ('[ERROR] Failed to sync service XML: ' + $_.Exception.Message); exit 1 }"
if errorlevel 1 (
    echo [ERROR] Service XML sync failed. Install aborted.
    pause
    exit /b 1
)

echo.
echo [OPTION] Cleanup existing Muse-Agent processes before service start
echo [WARN] If selected, currently running Muse-Agent app/tray processes will be terminated.
set "CLEANUP_OLD="
set /p CLEANUP_OLD=Do cleanup now? [Y/N, default N]: 
if /I "%CLEANUP_OLD%"=="Y" (
    echo [INFO] Stopping existing Muse-Agent service if present...
    if exist "service\muse-agent-service.exe" (
        pushd service
        muse-agent-service.exe stop >nul 2>&1
        popd
    )

    echo [INFO] Terminating existing Muse-Agent app/tray processes...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$targets = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and ( $_.CommandLine -like '*Muse-Agent-Tray.ps1*' -or $_.CommandLine -like '*muse-agent-*.jar*' ) }; if (-not $targets) { Write-Host '[INFO] No existing Muse-Agent processes found.'; exit 0 }; foreach ($p in $targets) { try { Invoke-CimMethod -InputObject $p -MethodName Terminate | Out-Null; Write-Host ('[KILL] PID ' + $p.ProcessId) } catch { Write-Host ('[WARN] Failed to terminate PID ' + $p.ProcessId + ': ' + $_.Exception.Message) } }"
) else (
    echo [INFO] Skipping existing process cleanup.
)

echo.
echo [INFO] Checking service port availability (%SERVICE_PORT%)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p=%SERVICE_PORT%; $listeners = Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue; if ($listeners) { $pids = ($listeners | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '; Write-Host ('[ERROR] Port ' + $p + ' is already in use. PID: ' + $pids); exit 1 } else { Write-Host ('[INFO] Port ' + $p + ' is available.') }"
if errorlevel 1 (
    echo [ERROR] Please stop the process using port %SERVICE_PORT% and run install again.
    pause
    exit /b 1
)

echo.
echo [2/5] Install and start Windows service...
pushd service
muse-agent-service.exe install >nul 2>&1
muse-agent-service.exe start
if errorlevel 1 (
    echo [ERROR] Service start failed.
    popd
    pause
    exit /b 1
)
muse-agent-service.exe status
popd

set "_SERVICE_RUNNING=0"
for /l %%I in (1,1,20) do (
    sc query muse-agent | find "RUNNING" >nul
    if not errorlevel 1 (
        set "_SERVICE_RUNNING=1"
        goto :service_running
    )
    timeout /t 1 /nobreak >nul
)

:service_running
if "%_SERVICE_RUNNING%"=="0" (
    echo [ERROR] Service is not RUNNING yet.
    echo [INFO] Current SCM state:
    sc query muse-agent

    echo [INFO] Collecting recent logs from service\logs and logs...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$cutoff = (Get-Date).AddMinutes(-10); if (Test-Path 'service\\logs') { $files = Get-ChildItem -Path 'service\\logs' -File | Sort-Object LastWriteTime -Descending; if ($files) { foreach ($f in $files | Select-Object -First 5) { Write-Host (''); Write-Host ('[INFO] Service log file: ' + $f.FullName); Get-Content -Path $f.FullName -Tail 80 } } else { Write-Host '[INFO] No files found in service\\logs.' } } else { Write-Host '[INFO] service\\logs folder not found.' }"

    if exist "logs\muse-agent.log" (
        echo.
        echo [INFO] Last application logs:
        powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-Content -Path 'logs\\muse-agent.log' -Tail 80"
    ) else (
        echo [INFO] Application log not found: logs\muse-agent.log
    )

    echo.
    echo [INFO] Tip: verify Java process account permission for this install folder.
    pause
    exit /b 1
)

echo [INFO] Waiting for dashboard endpoint (max %DASHBOARD_WAIT_SEC%s)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$uri='%DASHBOARD_URL%/'; $deadline=(Get-Date).AddSeconds(%DASHBOARD_WAIT_SEC%); $ok=$false; while ((Get-Date) -lt $deadline) { try { $r = Invoke-WebRequest -UseBasicParsing -Uri $uri -TimeoutSec 2; if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 400) { $ok=$true; break } } catch { }; Start-Sleep -Milliseconds 500 }; if ($ok) { Write-Host '[INFO] Dashboard endpoint is reachable.'; exit 0 } else { Write-Host '[WARN] Dashboard endpoint is not reachable within timeout.'; exit 1 }"
if errorlevel 1 (
    set "_READY=0"
    echo [WARN] Dashboard did not become ready within timeout.
    echo [INFO] Browser auto-open is skipped to avoid 404.
) else (
    set "_READY=1"
    echo [INFO] Dashboard is ready.
)

echo.
echo [3/5] Create desktop/start-menu shortcuts...
if exist "create-shortcuts.ps1" (
    powershell -NoProfile -ExecutionPolicy Bypass -File ".\create-shortcuts.ps1"
) else (
    echo [WARN] create-shortcuts.ps1 not found. Skipping shortcut creation.
)

echo.
echo [4/5] Start tray icon app...
start "Muse-Agent Tray" powershell -NoProfile -ExecutionPolicy Bypass -STA -WindowStyle Hidden -File ".\Muse-Agent-Tray.ps1"

echo.
echo [5/5] Open management dashboard in browser...
if "%_READY%"=="1" (
    start "" "%DASHBOARD_URL%"
) else (
    echo [INFO] Open manually after startup: %DASHBOARD_URL%
)

echo.
echo [OK] Install completed.
echo - Service: installed and started
echo - Tray icon: launched
echo - Dashboard: opened in browser
echo - User guide: 사용설명서.md
echo - Install guide: 설치가이드.md
pause
