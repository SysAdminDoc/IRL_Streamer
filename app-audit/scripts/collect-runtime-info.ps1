[CmdletBinding()]
param(
    [string]$Serial,
    [string]$PackageName = 'app.irlpro.android',
    [switch]$AllowOverwrite
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$auditRoot = Split-Path -Parent $PSScriptRoot

function Resolve-Adb {
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $command) { throw 'adb was not found on PATH.' }
    return $command.Source
}

function Resolve-DeviceSerial {
    param([string]$AdbPath, [string]$RequestedSerial)
    $lines = @(& $AdbPath devices -l 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "adb devices failed: $($lines -join [Environment]::NewLine)" }
    $connected = @($lines | ForEach-Object {
        if ($_ -match '^(\S+)\s+device(?:\s|$)') { $Matches[1] }
    })
    if ($RequestedSerial) {
        if ($connected -notcontains $RequestedSerial) {
            throw "Requested device '$RequestedSerial' is not connected and authorized."
        }
        return $RequestedSerial
    }
    if ($connected.Count -ne 1) {
        throw "Exactly one authorized device is required when -Serial is omitted. Found $($connected.Count)."
    }
    return $connected[0]
}

function Invoke-AdbText {
    param([string[]]$Arguments)
    $output = @(& $script:AdbPath -s $script:DeviceSerial @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Arguments -join ' ') failed: $($output -join [Environment]::NewLine)"
    }
    return (($output | ForEach-Object { [string]$_ }) -join [Environment]::NewLine)
}

function Write-Utf8Text {
    param([string]$Path, [AllowEmptyString()][string]$Value)
    [System.IO.File]::WriteAllText($Path, $Value + [Environment]::NewLine, $utf8NoBom)
}

function Sanitize-TargetLog {
    param([string]$Text)
    $sanitized = $Text
    $sanitized = $sanitized -replace '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b', '[REDACTED_EMAIL]'
    $sanitized = $sanitized -replace '(?i)\b(?:https?|rtmps?|srt)://\S+', '[REDACTED_URL]'
    $sanitized = $sanitized -replace '(?i)(?:bearer\s+)[A-Z0-9._~+\-/]+=*', 'Bearer [REDACTED]'
    $sanitized = $sanitized -replace '(?i)((?:access[_-]?token|refresh[_-]?token|api[_-]?key|stream[_-]?key|secret|password|passwd)\s*[=:]\s*)[^\s,;\]}]+', '$1[REDACTED]'
    return $sanitized
}

function Get-MatchingContext {
    param([string]$Text, [string]$Pattern, [int]$Before = 3, [int]$After = 12)
    $lines = @($Text -split '\r?\n')
    $indexes = New-Object 'System.Collections.Generic.HashSet[int]'
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $Pattern) {
            $start = [Math]::Max(0, $i - $Before)
            $end = [Math]::Min($lines.Count - 1, $i + $After)
            for ($j = $start; $j -le $end; $j++) { [void]$indexes.Add($j) }
        }
    }
    return (($indexes | Sort-Object | ForEach-Object { $lines[$_] }) -join [Environment]::NewLine)
}

$script:AdbPath = Resolve-Adb
$script:DeviceSerial = Resolve-DeviceSerial -AdbPath $script:AdbPath -RequestedSerial $Serial
$logsDir = Join-Path $auditRoot 'evidence\logs'
$measurementsDir = Join-Path $auditRoot 'evidence\measurements'
foreach ($directory in @($logsDir, $measurementsDir)) {
    if (-not (Test-Path -LiteralPath $directory)) { [void](New-Item -ItemType Directory -Path $directory) }
}

$outputPaths = @(
    (Join-Path $measurementsDir 'meminfo-idle-camera.txt'),
    (Join-Path $measurementsDir 'gfxinfo-idle-camera.txt'),
    (Join-Path $measurementsDir 'notification-target-snippet.txt'),
    (Join-Path $measurementsDir 'jobscheduler-target-snippet.txt'),
    (Join-Path $measurementsDir 'alarm-target-snippet.txt'),
    (Join-Path $measurementsDir 'runtime-capture.json'),
    (Join-Path $logsDir 'pid-scoped-sanitized.log')
)
foreach ($path in $outputPaths) {
    if ((Test-Path -LiteralPath $path) -and (-not $AllowOverwrite)) {
        throw "Refusing to overwrite existing evidence: $path. Pass -AllowOverwrite intentionally or use a fresh audit directory."
    }
}

$pidText = (Invoke-AdbText -Arguments @('shell', 'pidof', $PackageName)).Trim()
if (-not $pidText) { throw "No running process was found for $PackageName." }
$pidValue = ($pidText -split '\s+')[0]

$meminfo = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'meminfo', $PackageName)
$gfxinfo = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'gfxinfo', $PackageName)
$logcat = Invoke-AdbText -Arguments @('logcat', '--pid', $pidValue, '-d', '-v', 'threadtime', '-t', '500')
$notification = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'notification')
$jobs = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'jobscheduler')
$alarms = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'alarm')

$escapedPackage = [Regex]::Escape($PackageName)
$recordMatch = [Regex]::Match(
    $notification,
    "(?ms)^    NotificationRecord\([^\r\n]*pkg=${escapedPackage}.*?(?=^    NotificationRecord\(|^  TimeoutPendingIntent:|\z)"
)
$channelMatches = [Regex]::Matches(
    $notification,
    "(?m)^\s*NotificationChannel\{mId='com\.wmspanel\.streamer\.channel[^\r\n]*$"
)
$notificationParts = New-Object 'System.Collections.Generic.List[string]'
if ($recordMatch.Success) { $notificationParts.Add($recordMatch.Value.TrimEnd()) }
foreach ($match in $channelMatches) {
    if (-not $notificationParts.Contains($match.Value.Trim())) { $notificationParts.Add($match.Value.Trim()) }
}
$notificationSnippet = if ($notificationParts.Count) {
    $notificationParts -join ([Environment]::NewLine + [Environment]::NewLine)
} else {
    "No target notification record or channel was present for $PackageName."
}

$jobsLines = @($jobs -split '\r?\n' | Where-Object { $_ -match $escapedPackage })
$jobsSnippet = if ($jobsLines.Count) {
    "Target-package matches only; no unrelated scheduler state retained." + [Environment]::NewLine + ($jobsLines -join [Environment]::NewLine)
} else {
    "No target-package JobScheduler entries were present for $PackageName."
}
$alarmLines = @($alarms -split '\r?\n' | Where-Object { $_ -match $escapedPackage })
$alarmsSnippet = if ($alarmLines.Count) {
    "Target-package matches only; no unrelated alarm state retained." + [Environment]::NewLine + ($alarmLines -join [Environment]::NewLine)
} else {
    "No target-package AlarmManager entries were present for $PackageName."
}

Write-Utf8Text -Path (Join-Path $measurementsDir 'meminfo-idle-camera.txt') -Value $meminfo
Write-Utf8Text -Path (Join-Path $measurementsDir 'gfxinfo-idle-camera.txt') -Value $gfxinfo
Write-Utf8Text -Path (Join-Path $measurementsDir 'notification-target-snippet.txt') -Value $notificationSnippet
Write-Utf8Text -Path (Join-Path $measurementsDir 'jobscheduler-target-snippet.txt') -Value $jobsSnippet
Write-Utf8Text -Path (Join-Path $measurementsDir 'alarm-target-snippet.txt') -Value $alarmsSnippet
Write-Utf8Text -Path (Join-Path $logsDir 'pid-scoped-sanitized.log') -Value (Sanitize-TargetLog -Text $logcat)

$summary = [ordered]@{
    collected_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    device_serial = $script:DeviceSerial
    package_name = $PackageName
    process_id_at_capture = [int]$pidValue
    state = 'foreground, idle live console, camera and audio preview active, no broadcast or recording'
    privacy = 'PID-scoped logcat; URLs, email addresses, and credential-like values sanitized. Notification, job, and alarm files contain target-matching context only.'
    evidence = [ordered]@{
        meminfo = 'evidence/measurements/meminfo-idle-camera.txt'
        gfxinfo = 'evidence/measurements/gfxinfo-idle-camera.txt'
        notification = 'evidence/measurements/notification-target-snippet.txt'
        jobscheduler = 'evidence/measurements/jobscheduler-target-snippet.txt'
        alarm = 'evidence/measurements/alarm-target-snippet.txt'
        logcat = 'evidence/logs/pid-scoped-sanitized.log'
    }
}
Write-Utf8Text -Path (Join-Path $measurementsDir 'runtime-capture.json') -Value ($summary | ConvertTo-Json -Depth 6)

Write-Host "Collected target-scoped runtime evidence for $PackageName on $($script:DeviceSerial)."
