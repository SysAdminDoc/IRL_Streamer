[CmdletBinding()]
param(
    [string]$Serial,
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9_-]*$')]
    [string]$FlowId,
    [ValidateRange(1, 60)]
    [int]$DurationSeconds = 15,
    [ValidateRange(100000, 100000000)]
    [int]$BitRate = 8000000,
    [string]$Description = '',
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
    $connected = @($lines | ForEach-Object { if ($_ -match '^(\S+)\s+device(?:\s|$)') { $Matches[1] } })
    if ($RequestedSerial) {
        if ($connected -notcontains $RequestedSerial) { throw "Requested device '$RequestedSerial' is not connected and authorized." }
        return $RequestedSerial
    }
    if ($connected.Count -ne 1) { throw "Exactly one authorized device is required when -Serial is omitted. Found $($connected.Count)." }
    return $connected[0]
}

$adbPath = Resolve-Adb
$deviceSerial = Resolve-DeviceSerial -AdbPath $adbPath -RequestedSerial $Serial
$recordingsDir = Join-Path $auditRoot 'evidence\recordings'
if (-not (Test-Path -LiteralPath $recordingsDir)) { [void](New-Item -ItemType Directory -Path $recordingsDir) }
$videoPath = Join-Path $recordingsDir "${FlowId}.mp4"
$metadataPath = Join-Path $recordingsDir "${FlowId}.json"
foreach ($path in @($videoPath, $metadataPath)) {
    if ((Test-Path -LiteralPath $path) -and (-not $AllowOverwrite)) {
        throw "Refusing to overwrite existing evidence: $path. Use a new FlowId or pass -AllowOverwrite intentionally."
    }
}

$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssfffZ')
$remotePath = "/sdcard/Download/codex_audit_flow_${FlowId}_${stamp}.mp4"
$startedAt = (Get-Date).ToUniversalTime()
try {
    Write-Host "Recording ${FlowId} for up to ${DurationSeconds}s on ${deviceSerial}. Perform only the intended safe interaction now."
    $recordOutput = @(& $adbPath -s $deviceSerial shell screenrecord --bit-rate $BitRate --time-limit $DurationSeconds $remotePath 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "screenrecord failed: $($recordOutput -join [Environment]::NewLine)" }
    $pullOutput = @(& $adbPath -s $deviceSerial pull $remotePath $videoPath 2>&1)
    if ($LASTEXITCODE -ne 0) { throw "adb pull failed: $($pullOutput -join [Environment]::NewLine)" }
    if (-not (Test-Path -LiteralPath $videoPath)) { throw "Recording pull did not create $videoPath" }
    $metadata = [ordered]@{
        flow_id = $FlowId
        description = $Description
        device_serial = $deviceSerial
        started_at_utc = $startedAt.ToString('o')
        completed_at_utc = (Get-Date).ToUniversalTime().ToString('o')
        requested_duration_seconds = $DurationSeconds
        bit_rate = $BitRate
        recording = "evidence/recordings/${FlowId}.mp4"
    }
    [System.IO.File]::WriteAllText($metadataPath, (($metadata | ConvertTo-Json -Depth 5) + [Environment]::NewLine), $utf8NoBom)
    Write-Host "Recording: $videoPath"
}
finally {
    $cleanup = @(& $adbPath -s $deviceSerial shell rm -f $remotePath 2>&1)
    if ($LASTEXITCODE -ne 0) { Write-Warning "Could not remove temporary device recording: $($cleanup -join ' ')" }
}

