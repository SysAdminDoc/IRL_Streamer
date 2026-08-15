[CmdletBinding()]
param(
    [string]$Serial,
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9_-]*$')]
    [string]$ScreenId,
    [string]$NavigationPath = '',
    [string]$StartingState = '',
    [ValidateRange(0, 15000)]
    [int]$SettleMilliseconds = 1200,
    [switch]$AllowOverwrite
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$script:Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$script:AuditRoot = Split-Path -Parent $PSScriptRoot

function Write-Utf8Text {
    param([string]$Path, [AllowEmptyString()][string]$Value)
    [System.IO.File]::WriteAllText($Path, $Value, $script:Utf8NoBom)
}

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
            throw "Requested device '$RequestedSerial' is not connected and authorized. Connected: $($connected -join ', ')"
        }
        return $RequestedSerial
    }
    if ($connected.Count -ne 1) {
        throw "Exactly one authorized device is required when -Serial is omitted. Found $($connected.Count): $($connected -join ', ')"
    }
    return $connected[0]
}

function Invoke-AdbText {
    param(
        [string[]]$Arguments,
        [string]$OutputPath,
        [switch]$AllowFailure
    )
    $allArgs = @('-s', $script:DeviceSerial) + $Arguments
    $output = @(& $script:AdbPath @allArgs 2>&1)
    $exitCode = $LASTEXITCODE
    $text = ($output | ForEach-Object { [string]$_ }) -join [Environment]::NewLine
    if ($OutputPath) { Write-Utf8Text -Path $OutputPath -Value ($text + [Environment]::NewLine) }
    if (($exitCode -ne 0) -and (-not $AllowFailure)) {
        throw "adb $($Arguments -join ' ') failed with exit code ${exitCode}: $text"
    }
    return [pscustomobject]@{ ExitCode = $exitCode; Text = $text }
}

function Assert-AvailablePath {
    param([string]$Path)
    if ((Test-Path -LiteralPath $Path) -and (-not $AllowOverwrite)) {
        throw "Refusing to overwrite existing evidence: $Path. Use a new ScreenId or pass -AllowOverwrite intentionally."
    }
}

function Get-PngDimensions {
    param([string]$Path)
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $header = New-Object byte[] 24
        $read = $stream.Read($header, 0, $header.Length)
        if ($read -lt 24) { throw "PNG header is incomplete: $Path" }
        $width = [System.Net.IPAddress]::NetworkToHostOrder([System.BitConverter]::ToInt32($header, 16))
        $height = [System.Net.IPAddress]::NetworkToHostOrder([System.BitConverter]::ToInt32($header, 20))
        return [pscustomobject]@{ Width = $width; Height = $height }
    }
    finally {
        $stream.Dispose()
    }
}

function Get-RelevantLines {
    param(
        [AllowEmptyString()][string]$Text,
        [AllowEmptyString()][string]$PackageName,
        [string[]]$Patterns
    )
    $allPatterns = New-Object 'System.Collections.Generic.List[string]'
    if ($PackageName) { $allPatterns.Add([Regex]::Escape($PackageName)) }
    foreach ($pattern in $Patterns) { if ($pattern) { $allPatterns.Add($pattern) } }
    if ($allPatterns.Count -eq 0) { return 'No target/focus filter could be constructed.' }
    $combined = $allPatterns -join '|'
    $matches = @($Text -split '\r?\n' | Where-Object { $_ -match $combined })
    if ($matches.Count -eq 0) { return 'No target/focused-surface lines were present.' }
    return ($matches -join [Environment]::NewLine)
}

$script:AdbPath = Resolve-Adb
$script:DeviceSerial = Resolve-DeviceSerial -AdbPath $script:AdbPath -RequestedSerial $Serial

$directories = @(
    (Join-Path $script:AuditRoot 'evidence\screenshots'),
    (Join-Path $script:AuditRoot 'evidence\ui-xml'),
    (Join-Path $script:AuditRoot 'evidence\activity'),
    (Join-Path $script:AuditRoot 'screens\screen-specs')
)
foreach ($directory in $directories) {
    if (-not (Test-Path -LiteralPath $directory)) { [void](New-Item -ItemType Directory -Path $directory) }
}

$screenshotPath = Join-Path $script:AuditRoot "evidence\screenshots\${ScreenId}.png"
$xmlPath = Join-Path $script:AuditRoot "evidence\ui-xml\${ScreenId}.xml"
$activityPath = Join-Path $script:AuditRoot "evidence\activity\${ScreenId}_activity-top.txt"
$windowPath = Join-Path $script:AuditRoot "evidence\activity\${ScreenId}_window.txt"
$imePath = Join-Path $script:AuditRoot "evidence\activity\${ScreenId}_input-method.txt"
$metadataPath = Join-Path $script:AuditRoot "evidence\activity\${ScreenId}_capture.json"
$specPath = Join-Path $script:AuditRoot "screens\screen-specs\${ScreenId}.json"

@($screenshotPath, $xmlPath, $activityPath, $windowPath, $imePath, $metadataPath, $specPath) |
    ForEach-Object { Assert-AvailablePath -Path $_ }

if ($SettleMilliseconds -gt 0) { Start-Sleep -Milliseconds $SettleMilliseconds }

$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssfffZ')
$remoteBase = "/sdcard/Download/codex_audit_${ScreenId}_${stamp}"
$remotePng = "${remoteBase}.png"
$remoteXml = "${remoteBase}.xml"

try {
    [void](Invoke-AdbText -Arguments @('shell', 'screencap', '-p', $remotePng))
    [void](Invoke-AdbText -Arguments @('pull', $remotePng, $screenshotPath))
    if (-not (Test-Path -LiteralPath $screenshotPath)) { throw "Screenshot pull did not create $screenshotPath" }

    $xmlCaptured = $false
    $xmlMessages = @()
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $dump = Invoke-AdbText -Arguments @('shell', 'uiautomator', 'dump', '--compressed', $remoteXml) -AllowFailure
        $xmlMessages += "Attempt ${attempt}: $($dump.Text)"
        $pull = Invoke-AdbText -Arguments @('pull', $remoteXml, $xmlPath) -AllowFailure
        if (($pull.ExitCode -eq 0) -and (Test-Path -LiteralPath $xmlPath)) { $xmlCaptured = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $xmlCaptured) {
        Write-Utf8Text -Path $xmlPath -Value ("<!-- UI Automator dump unavailable. " + ($xmlMessages -join ' | ') + " -->")
    }

    # Only target/focused-surface summaries are persisted. Raw device-wide activity,
    # window, and IME dumps are intentionally kept in memory and discarded.
    $activityTop = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'activity', 'top')
    $wmSize = Invoke-AdbText -Arguments @('shell', 'wm', 'size')
    $wmDensity = Invoke-AdbText -Arguments @('shell', 'wm', 'density')
    $rotation = Invoke-AdbText -Arguments @('shell', 'settings', 'get', 'system', 'user_rotation')

    $pngDimensions = Get-PngDimensions -Path $screenshotPath
    $widthPx = [int]$pngDimensions.Width
    $heightPx = [int]$pngDimensions.Height
    $densityDpi = 0
    if ($wmDensity.Text -match '(?:Override|Physical) density:\s*(\d+)') { $densityDpi = [int]$Matches[1] }
    $widthDp = if ($densityDpi -gt 0) { [Math]::Round($widthPx * 160.0 / $densityDpi, 2) } else { 0 }
    $heightDp = if ($densityDpi -gt 0) { [Math]::Round($heightPx * 160.0 / $densityDpi, 2) } else { 0 }

    $packageName = ''
    $activityName = ''
    $focusText = $activityTop.Text
    $xmlPackageName = ''
    if (Test-Path -LiteralPath $xmlPath) {
        $xmlText = Get-Content -LiteralPath $xmlPath -Raw
        if ($xmlText -match 'package="([A-Za-z0-9._]+)"') { $xmlPackageName = $Matches[1] }
    }
    if ($xmlPackageName -and ($focusText -match ("ACTIVITY\s+" + [Regex]::Escape($xmlPackageName) + "/(\S+)"))) {
        $packageName = $xmlPackageName
        $activityName = $Matches[1]
    } elseif ($focusText -match 'topResumedActivity=.*?\su\d+\s+([A-Za-z0-9._]+)/(\S+)') {
        $packageName = $Matches[1]
        $activityName = $Matches[2]
    } elseif ($focusText -match '(?:mResumedActivity|ResumedActivity):.*?\su\d+\s+([A-Za-z0-9._]+)/(\S+)') {
        $packageName = $Matches[1]
        $activityName = $Matches[2]
    } elseif ($focusText -match 'ACTIVITY\s+([A-Za-z0-9._]+)/(\S+)') {
        $packageName = $Matches[1]
        $activityName = $Matches[2]
    }
    if ((-not $packageName) -and $xmlPackageName) { $packageName = $xmlPackageName }

    $activitySummary = Get-RelevantLines -Text $activityTop.Text -PackageName $packageName -Patterns @(
        '^\s*(?:ACTIVITY|TASK)\s', 'topResumedActivity', 'mResumedActivity', 'ResumedActivity',
        'mLastReported', 'mState=', 'mVisible=', 'mAppStopped=', 'mActivityComponent'
    )
    Write-Utf8Text -Path $activityPath -Value ($activitySummary + [Environment]::NewLine)

    $window = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'window', 'windows')
    $windowSummary = Get-RelevantLines -Text $window.Text -PackageName $packageName -Patterns @(
        'mCurrentFocus', 'mFocusedApp', 'mTopFullscreenOpaqueWindow', 'mInputMethodTarget',
        'mImeLayeringTarget', 'mImeInputTarget', 'mImeControlTarget', 'mOrientationRequest'
    )
    Write-Utf8Text -Path $windowPath -Value ($windowSummary + [Environment]::NewLine)

    $ime = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'input_method')
    $imeSummary = Get-RelevantLines -Text $ime.Text -PackageName $packageName -Patterns @(
        'mCurFocusedWindow', 'mServedView', 'mNextServedView', 'mInputShown',
        'mImeWindowVis', 'mShowRequested', 'inputMethodTarget'
    )
    Write-Utf8Text -Path $imePath -Value ($imeSummary + [Environment]::NewLine)

    $captureTime = (Get-Date).ToUniversalTime().ToString('o')
    $metadata = [ordered]@{
        screen_id = $ScreenId
        captured_at_utc = $captureTime
        device_serial = $script:DeviceSerial
        package_name = $packageName
        activity = $activityName
        navigation_path = $NavigationPath
        required_starting_state = $StartingState
        settle_milliseconds = $SettleMilliseconds
        screenshot = "evidence/screenshots/${ScreenId}.png"
        ui_xml = "evidence/ui-xml/${ScreenId}.xml"
        activity_dump = "evidence/activity/${ScreenId}_activity-top.txt"
        window_dump = "evidence/activity/${ScreenId}_window.txt"
        input_method_dump = "evidence/activity/${ScreenId}_input-method.txt"
        ui_automator_messages = $xmlMessages
    }
    Write-Utf8Text -Path $metadataPath -Value (($metadata | ConvertTo-Json -Depth 6) + [Environment]::NewLine)

    $spec = [ordered]@{
        screen_id = $ScreenId
        screen_name = ''
        state_name = ''
        classification = 'CONFIRMED'
        package_name = $packageName
        activity = $activityName
        entry_conditions = if ($StartingState) { ,$StartingState } else { @() }
        navigation_path = if ($NavigationPath) { ,$NavigationPath } else { @() }
        evidence = [ordered]@{
            screenshot = "evidence/screenshots/${ScreenId}.png"
            ui_xml = "evidence/ui-xml/${ScreenId}.xml"
            activity_dump = "evidence/activity/${ScreenId}_activity-top.txt"
            recording = ''
        }
        display = [ordered]@{
            width_px = $widthPx
            height_px = $heightPx
            density_dpi = $densityDpi
            width_dp = $widthDp
            height_dp = $heightDp
            orientation = if ($widthPx -le $heightPx) { 'portrait' } else { 'landscape' }
            rotation_setting = $rotation.Text.Trim()
            edge_to_edge = $false
        }
        system_bars = [ordered]@{}
        layout_regions = @()
        elements = @()
        visible_text = @()
        interactions = @()
        states = @()
        transitions = @()
        validation = @()
        loading_behavior = @()
        error_behavior = @()
        persistence_behavior = @()
        accessibility = [ordered]@{}
        design_measurements = [ordered]@{}
        inferences = @()
        unknowns = @()
        notes = ''
    }
    Write-Utf8Text -Path $specPath -Value (($spec | ConvertTo-Json -Depth 12) + [Environment]::NewLine)

    $manifestPath = Join-Path $script:AuditRoot 'evidence\evidence-manifest.csv'
    $manifestRow = [pscustomobject][ordered]@{
        screen_id = $ScreenId
        captured_at_utc = $captureTime
        screenshot = "evidence/screenshots/${ScreenId}.png"
        ui_xml = "evidence/ui-xml/${ScreenId}.xml"
        activity_dump = "evidence/activity/${ScreenId}_activity-top.txt"
        recording = ''
        navigation_path = $NavigationPath
        required_starting_state = $StartingState
        classification = 'CONFIRMED'
        notes = ''
    }
    if ((Test-Path -LiteralPath $manifestPath) -and $AllowOverwrite) {
        $existingRows = @(Import-Csv -LiteralPath $manifestPath | Where-Object { $_.screen_id -ne $ScreenId })
        @($existingRows + $manifestRow) | Export-Csv -LiteralPath $manifestPath -NoTypeInformation -Encoding UTF8
    } elseif (Test-Path -LiteralPath $manifestPath) {
        $manifestRow | Export-Csv -LiteralPath $manifestPath -NoTypeInformation -Append -Encoding UTF8
    } else {
        $manifestRow | Export-Csv -LiteralPath $manifestPath -NoTypeInformation -Encoding UTF8
    }

    Write-Host "Captured $ScreenId from $script:DeviceSerial"
    Write-Host "Screenshot: $screenshotPath"
    Write-Host "UI XML: $xmlPath"
    Write-Host "Screen spec: $specPath"
}
finally {
    [void](Invoke-AdbText -Arguments @('shell', 'rm', '-f', $remotePng, $remoteXml) -AllowFailure)
}
