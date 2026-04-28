$ErrorActionPreference = 'Stop'

$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$desktop = [Environment]::GetFolderPath('Desktop')
$startMenu = [Environment]::GetFolderPath('Programs')
$faviconPath = Join-Path $baseDir 'img\favicon.ico'
$shortcutIcon = if (Test-Path $faviconPath) { "$faviconPath,0" } else { "$env:SystemRoot\System32\shell32.dll,220" }
$dashboardIconFile = if (Test-Path $faviconPath) { $faviconPath } else { "$env:SystemRoot\System32\shell32.dll" }
$dashboardIconIndex = if (Test-Path $faviconPath) { 0 } else { 220 }

$shell = New-Object -ComObject WScript.Shell

# 1) Tray app shortcut on Desktop
$trayShortcutPath = Join-Path $desktop 'Muse-Agent Tray.lnk'
$trayShortcut = $shell.CreateShortcut($trayShortcutPath)
$trayShortcut.TargetPath = 'powershell.exe'
$trayShortcut.Arguments = '-NoProfile -ExecutionPolicy Bypass -STA -WindowStyle Hidden -File "' + (Join-Path $baseDir 'Muse-Agent-Tray.ps1') + '"'
$trayShortcut.WorkingDirectory = $baseDir
$trayShortcut.Description = 'Launch Muse-Agent tray app'
$trayShortcut.IconLocation = $shortcutIcon
$trayShortcut.Save()

# 2) Dashboard URL shortcut on Desktop
$dashboardShortcutPath = Join-Path $desktop 'Muse-Agent Dashboard.url'
@(
	'[InternetShortcut]'
	'URL=http://127.0.0.1:8484'
	"IconFile=$dashboardIconFile"
	"IconIndex=$dashboardIconIndex"
) | Set-Content -Path $dashboardShortcutPath -Encoding ASCII

# 3) Start Menu shortcut
$startMenuShortcutPath = Join-Path $startMenu 'Muse-Agent Tray.lnk'
$startMenuShortcut = $shell.CreateShortcut($startMenuShortcutPath)
$startMenuShortcut.TargetPath = 'powershell.exe'
$startMenuShortcut.Arguments = '-NoProfile -ExecutionPolicy Bypass -STA -WindowStyle Hidden -File "' + (Join-Path $baseDir 'Muse-Agent-Tray.ps1') + '"'
$startMenuShortcut.WorkingDirectory = $baseDir
$startMenuShortcut.Description = 'Launch Muse-Agent tray app'
$startMenuShortcut.IconLocation = $shortcutIcon
$startMenuShortcut.Save()

Write-Host 'Shortcuts created:'
Write-Host " - $trayShortcutPath"
Write-Host " - $dashboardShortcutPath"
Write-Host " - $startMenuShortcutPath"
Write-Host ''
Write-Host 'Pin to taskbar:'
Write-Host '  1) Right-click "Muse-Agent Tray" on Desktop'
Write-Host '  2) Select "Pin to taskbar"'
