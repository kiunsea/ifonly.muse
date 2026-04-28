$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outFile = Join-Path $scriptDir 'muse-agent-service.exe'
$url = 'https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe'

Write-Host "Downloading WinSW from: $url"
Invoke-WebRequest -Uri $url -OutFile $outFile
Write-Host "Saved: $outFile"
