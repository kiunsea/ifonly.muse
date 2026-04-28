Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$serviceExe = Join-Path $baseDir 'service\muse-agent-service.exe'
$trayIconIcoCandidates = @(
    [System.IO.Path]::GetFullPath((Join-Path $baseDir 'img\favicon.ico')),
    [System.IO.Path]::GetFullPath((Join-Path $baseDir '..\..\img\favicon.ico'))
)
$trayIconImageCandidates = @(
    [System.IO.Path]::GetFullPath((Join-Path $baseDir '..\..\tmp\image_icon.png')),
    [System.IO.Path]::GetFullPath((Join-Path $baseDir '..\..\img\image_icon.png'))
)
$dashboardUrl = 'http://127.0.0.1:8484'

function New-MuseAgentTrayIcon {
    foreach ($iconPath in $trayIconIcoCandidates) {
        if (Test-Path $iconPath) {
            try {
                return New-Object System.Drawing.Icon ($iconPath)
            }
            catch {
                # Fall through to other sources if ico loading fails.
            }
        }
    }

    foreach ($trayIconImage in $trayIconImageCandidates) {
        if (Test-Path $trayIconImage) {
            $srcBmp = $null
            $iconBmp = $null
            try {
                $srcBmp = New-Object System.Drawing.Bitmap $trayIconImage
                $iconBmp = New-Object System.Drawing.Bitmap $srcBmp, 16, 16
                $hIcon = $iconBmp.GetHicon()
                return [System.Drawing.Icon]::FromHandle($hIcon)
            }
            catch {
                # Fall through to generated icon if custom image loading fails.
            }
            finally {
                if ($iconBmp -ne $null) { $iconBmp.Dispose() }
                if ($srcBmp -ne $null) { $srcBmp.Dispose() }
            }
        }
    }

    $bmp = New-Object System.Drawing.Bitmap 64, 64
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g.Clear([System.Drawing.Color]::Transparent)

        # Hair base (blonde, high contrast)
        $hairBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 247, 197, 63))
        $g.FillEllipse($hairBrush, 6, 8, 52, 30)
        $g.FillEllipse($hairBrush, 6, 18, 10, 20)
        $g.FillEllipse($hairBrush, 48, 18, 10, 20)

        # Face (light beige)
        $faceBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 243, 225, 201))
        $g.FillEllipse($faceBrush, 10, 16, 44, 42)

        # Face outline for small icon clarity
        $faceOutline = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 130, 96, 68), 2)
        $g.DrawEllipse($faceOutline, 10, 16, 44, 42)

        # Cute eyes
        $eyeBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 45, 35, 28))
        $g.FillEllipse($eyeBrush, 20, 31, 6, 8)
        $g.FillEllipse($eyeBrush, 38, 31, 6, 8)

        # Eye sparkle
        $sparkleBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
        $g.FillEllipse($sparkleBrush, 22, 33, 2, 2)
        $g.FillEllipse($sparkleBrush, 40, 33, 2, 2)

        # Blush
        $blushBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(170, 255, 170, 175))
        $g.FillEllipse($blushBrush, 14, 38, 8, 5)
        $g.FillEllipse($blushBrush, 42, 38, 8, 5)

        # Hair shine accent
        $hairShine = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(200, 255, 229, 132))
        $g.FillEllipse($hairShine, 16, 11, 20, 8)

        # Smile
        $penSmile = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 138, 62, 26), 2)
        $g.DrawArc($penSmile, 24, 42, 16, 8, 15, 150)

        $hIcon = $bmp.GetHicon()
        return [System.Drawing.Icon]::FromHandle($hIcon)
    }
    finally {
        $g.Dispose()
        $bmp.Dispose()
    }
}

function Open-Dashboard {
    Start-Process $dashboardUrl | Out-Null
}

function Invoke-ServiceCommand {
    param([string]$Command)

    if (-not (Test-Path $serviceExe)) {
        [System.Windows.Forms.MessageBox]::Show(
            "service\\muse-agent-service.exe was not found.",
            'Muse-Agent Tray',
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        ) | Out-Null
        return
    }

    try {
        $output = & $serviceExe $Command 2>&1 | Out-String
        if ([string]::IsNullOrWhiteSpace($output)) {
            $output = "$Command completed"
        }

        [System.Windows.Forms.MessageBox]::Show(
            $output.Trim(),
            "Muse-Agent Service: $Command",
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Information
        ) | Out-Null
    }
    catch {
        [System.Windows.Forms.MessageBox]::Show(
            "Service command failed.`n$($_.Exception.Message)",
            'Muse-Agent Tray',
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        ) | Out-Null
    }
}

$notifyIcon = New-Object System.Windows.Forms.NotifyIcon
$notifyIcon.Icon = New-MuseAgentTrayIcon
$notifyIcon.Text = 'Muse-Agent Tray'
$notifyIcon.Visible = $true

$contextMenu = New-Object System.Windows.Forms.ContextMenuStrip

$menuOpen = $contextMenu.Items.Add('Open Muse-Agent Dashboard')
$menuOpen.Add_Click({ Open-Dashboard })

$contextMenu.Items.Add('-') | Out-Null

$menuStatus = $contextMenu.Items.Add('Service Status')
$menuStatus.Add_Click({ Invoke-ServiceCommand -Command 'status' })

$menuStart = $contextMenu.Items.Add('Start Service')
$menuStart.Add_Click({ Invoke-ServiceCommand -Command 'start' })

$menuStop = $contextMenu.Items.Add('Stop Service')
$menuStop.Add_Click({ Invoke-ServiceCommand -Command 'stop' })

$menuRestart = $contextMenu.Items.Add('Restart Service')
$menuRestart.Add_Click({ Invoke-ServiceCommand -Command 'restart' })

$contextMenu.Items.Add('-') | Out-Null

$menuExit = $contextMenu.Items.Add('Exit Tray')
$menuExit.Add_Click({
    $notifyIcon.Visible = $false
    $notifyIcon.Dispose()
    [System.Windows.Forms.Application]::Exit()
})

$notifyIcon.ContextMenuStrip = $contextMenu
$notifyIcon.Add_DoubleClick({ Open-Dashboard })
$notifyIcon.ShowBalloonTip(3000, 'Muse-Agent Tray', 'Right-click for dashboard link and service controls.', [System.Windows.Forms.ToolTipIcon]::Info)

[System.Windows.Forms.Application]::Run()
