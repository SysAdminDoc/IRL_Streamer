[CmdletBinding()]
param(
    [string]$Serial,
    [string]$PackageName = 'app.irlpro.android',
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
    $connected = @($lines | ForEach-Object { if ($_ -match '^(\S+)\s+device(?:\s|$)') { $Matches[1] } })
    if ($RequestedSerial) {
        if ($connected -notcontains $RequestedSerial) { throw "Requested device '$RequestedSerial' is not connected and authorized." }
        return $RequestedSerial
    }
    if ($connected.Count -ne 1) { throw "Exactly one authorized device is required when -Serial is omitted. Found $($connected.Count)." }
    return $connected[0]
}

function Invoke-AdbText {
    param([string[]]$Arguments, [string]$OutputPath, [switch]$AllowFailure)
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

function Get-RelevantLines {
    param(
        [AllowEmptyString()][string]$Text,
        [AllowEmptyString()][string]$PackageName,
        [string[]]$Patterns
    )
    $allPatterns = New-Object 'System.Collections.Generic.List[string]'
    if ($PackageName) { $allPatterns.Add([Regex]::Escape($PackageName)) }
    foreach ($pattern in $Patterns) { if ($pattern) { $allPatterns.Add($pattern) } }
    if ($allPatterns.Count -eq 0) { return 'No filter could be constructed.' }
    $combined = $allPatterns -join '|'
    $matches = @($Text -split '\r?\n' | Where-Object { $_ -match $combined })
    if ($matches.Count -eq 0) { return 'No target/focused-surface lines were present.' }
    return ($matches -join [Environment]::NewLine)
}

$script:AdbPath = Resolve-Adb
$script:DeviceSerial = Resolve-DeviceSerial -AdbPath $script:AdbPath -RequestedSerial $Serial
if ($PackageName -notmatch '^[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+$') { throw "Invalid package name: $PackageName" }

$packageDir = Join-Path $script:AuditRoot 'evidence\package'
$measurementsDir = Join-Path $script:AuditRoot 'evidence\measurements'
foreach ($directory in @($packageDir, $measurementsDir, (Join-Path $script:AuditRoot 'device'))) {
    if (-not (Test-Path -LiteralPath $directory)) { [void](New-Item -ItemType Directory -Path $directory) }
}

$outputPaths = @(
    (Join-Path $packageDir 'dumpsys-package.txt'),
    (Join-Path $packageDir 'appops.txt'),
    (Join-Path $packageDir 'resolve-activity.txt'),
    (Join-Path $packageDir 'split-apk-paths.txt'),
    (Join-Path $packageDir 'activity-activities.txt'),
    (Join-Path $packageDir 'activity-services.txt'),
    (Join-Path $packageDir 'package-summary.json'),
    (Join-Path $measurementsDir 'dumpsys-display.txt'),
    (Join-Path $measurementsDir 'dumpsys-window.txt'),
    (Join-Path $measurementsDir 'dumpsys-input-method.txt'),
    (Join-Path $script:AuditRoot 'device\device-environment.json')
)
foreach ($path in $outputPaths) {
    if ((Test-Path -LiteralPath $path) -and (-not $AllowOverwrite)) {
        throw "Refusing to overwrite existing evidence: $path. Pass -AllowOverwrite intentionally or use a fresh audit directory."
    }
}

$packageList = Invoke-AdbText -Arguments @('shell', 'pm', 'list', 'packages', $PackageName)
if ($packageList.Text -notmatch [regex]::Escape("package:${PackageName}")) { throw "Package $PackageName is not installed for the current Android user." }

$packageDump = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'package', $PackageName) -OutputPath (Join-Path $packageDir 'dumpsys-package.txt')
[void](Invoke-AdbText -Arguments @('shell', 'appops', 'get', $PackageName) -OutputPath (Join-Path $packageDir 'appops.txt') -AllowFailure)
[void](Invoke-AdbText -Arguments @('shell', 'cmd', 'package', 'resolve-activity', '--brief', $PackageName) -OutputPath (Join-Path $packageDir 'resolve-activity.txt'))
[void](Invoke-AdbText -Arguments @('shell', 'pm', 'path', $PackageName) -OutputPath (Join-Path $packageDir 'split-apk-paths.txt'))
$activities = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'activity', 'activities', $PackageName) -AllowFailure
$activitySummary = Get-RelevantLines -Text $activities.Text -PackageName $PackageName -Patterns @(
    'topResumedActivity', 'mResumedActivity', 'ResumedActivity', 'mState=', 'mVisible=', 'mAppStopped='
)
Write-Utf8Text -Path (Join-Path $packageDir 'activity-activities.txt') -Value ($activitySummary + [Environment]::NewLine)

$services = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'activity', 'services', $PackageName) -AllowFailure
$serviceSummary = Get-RelevantLines -Text $services.Text -PackageName $PackageName -Patterns @(
    'foregroundId=', 'isForeground=', 'foregroundServiceType=', 'createTime=', 'lastActivity=', 'startRequested='
)
Write-Utf8Text -Path (Join-Path $packageDir 'activity-services.txt') -Value ($serviceSummary + [Environment]::NewLine)
# JobScheduler output can include unrelated apps even when some Android builds accept a
# package argument. collect-runtime-info.ps1 writes a package-filtered snippet instead.

$manufacturer = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.product.manufacturer')
$model = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.product.model')
$device = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.product.device')
$androidRelease = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.build.version.release')
$sdk = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.build.version.sdk')
$buildId = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.build.id')
$fingerprint = Invoke-AdbText -Arguments @('shell', 'getprop', 'ro.build.fingerprint')
$locale = Invoke-AdbText -Arguments @('shell', 'getprop', 'persist.sys.locale')
$timeZone = Invoke-AdbText -Arguments @('shell', 'getprop', 'persist.sys.timezone')
$wmSize = Invoke-AdbText -Arguments @('shell', 'wm', 'size')
$wmDensity = Invoke-AdbText -Arguments @('shell', 'wm', 'density')
$fontScale = Invoke-AdbText -Arguments @('shell', 'settings', 'get', 'system', 'font_scale')
$navigationMode = Invoke-AdbText -Arguments @('shell', 'settings', 'get', 'secure', 'navigation_mode')
$userRotation = Invoke-AdbText -Arguments @('shell', 'settings', 'get', 'system', 'user_rotation')
$autoRotation = Invoke-AdbText -Arguments @('shell', 'settings', 'get', 'system', 'accelerometer_rotation')
$nightMode = Invoke-AdbText -Arguments @('shell', 'cmd', 'uimode', 'night')
$displayDump = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'display')
$displaySummary = Get-RelevantLines -Text $displayDump.Text -PackageName '' -Patterns @(
    'DisplayDeviceInfo.*Built-in Screen', 'DisplayInfo.*Built-in Screen', 'mStableDisplaySize',
    'mBaseDisplayInfo', 'mOverrideDisplayInfo', 'supportedModes', 'density', 'rotation',
    'displayCutout', 'roundedCorners', 'logicalWidth', 'logicalHeight', 'appWidth', 'appHeight'
)
Write-Utf8Text -Path (Join-Path $measurementsDir 'dumpsys-display.txt') -Value ($displaySummary + [Environment]::NewLine)

$windowDump = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'window')
$windowSummary = Get-RelevantLines -Text $windowDump.Text -PackageName $PackageName -Patterns @(
    'mCurrentFocus', 'mFocusedApp', 'mTopFullscreenOpaqueWindow', 'mInputMethodTarget',
    'mImeLayeringTarget', 'mImeInputTarget', 'mImeControlTarget', 'mOrientationRequest'
)
Write-Utf8Text -Path (Join-Path $measurementsDir 'dumpsys-window.txt') -Value ($windowSummary + [Environment]::NewLine)

$inputMethodDump = Invoke-AdbText -Arguments @('shell', 'dumpsys', 'input_method')
$imeSummary = Get-RelevantLines -Text $inputMethodDump.Text -PackageName $PackageName -Patterns @(
    'mCurFocusedWindow', 'mServedView', 'mNextServedView', 'mInputShown',
    'mImeWindowVis', 'mShowRequested', 'inputMethodTarget'
)
Write-Utf8Text -Path (Join-Path $measurementsDir 'dumpsys-input-method.txt') -Value ($imeSummary + [Environment]::NewLine)

$widthPx = 0
$heightPx = 0
$densityDpi = 0
if ($wmSize.Text -match '(?:Override|Physical) size:\s*(\d+)x(\d+)') { $widthPx = [int]$Matches[1]; $heightPx = [int]$Matches[2] }
if ($wmDensity.Text -match '(?:Override|Physical) density:\s*(\d+)') { $densityDpi = [int]$Matches[1] }
$widthDp = if ($densityDpi -gt 0) { [Math]::Round($widthPx * 160.0 / $densityDpi, 2) } else { 0 }
$heightDp = if ($densityDpi -gt 0) { [Math]::Round($heightPx * 160.0 / $densityDpi, 2) } else { 0 }

$deviceJson = [ordered]@{
    classification = 'CONFIRMED'
    captured_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    adb_serial = $script:DeviceSerial
    manufacturer = $manufacturer.Text.Trim()
    model = $model.Text.Trim()
    device_code_name = $device.Text.Trim()
    android_release = $androidRelease.Text.Trim()
    sdk_level = [int]$sdk.Text.Trim()
    build_id = $buildId.Text.Trim()
    build_fingerprint = $fingerprint.Text.Trim()
    locale = $locale.Text.Trim()
    time_zone = $timeZone.Text.Trim()
    display = [ordered]@{
        width_px = $widthPx
        height_px = $heightPx
        density_dpi = $densityDpi
        width_dp = $widthDp
        height_dp = $heightDp
        orientation = if ($widthPx -le $heightPx) { 'portrait' } else { 'landscape' }
        font_scale = [double]$fontScale.Text.Trim()
        user_rotation = $userRotation.Text.Trim()
        accelerometer_rotation = $autoRotation.Text.Trim()
        navigation_mode_setting = $navigationMode.Text.Trim()
        night_mode = $nightMode.Text.Trim()
    }
    raw_evidence = [ordered]@{
        display_dump = 'evidence/measurements/dumpsys-display.txt'
        window_dump = 'evidence/measurements/dumpsys-window.txt'
        input_method_dump = 'evidence/measurements/dumpsys-input-method.txt'
    }
}
Write-Utf8Text -Path (Join-Path $script:AuditRoot 'device\device-environment.json') -Value (($deviceJson | ConvertTo-Json -Depth 8) + [Environment]::NewLine)

$summary = [ordered]@{
    package_name = $PackageName
    device_serial = $script:DeviceSerial
    captured_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    version_name = if ($packageDump.Text -match 'versionName=([^\s]+)') { $Matches[1] } else { '' }
    version_code = if ($packageDump.Text -match 'versionCode=(\d+)') { [long]$Matches[1] } else { 0 }
    target_sdk = if ($packageDump.Text -match 'targetSdk=(\d+)') { [int]$Matches[1] } else { 0 }
    min_sdk = if ($packageDump.Text -match 'minSdk=(\d+)') { [int]$Matches[1] } else { 0 }
    installer_package = if ($packageDump.Text -match 'installerPackageName=([^\s]+)') { $Matches[1] } else { '' }
    evidence = [ordered]@{
        dumpsys_package = 'evidence/package/dumpsys-package.txt'
        appops = 'evidence/package/appops.txt'
        resolve_activity = 'evidence/package/resolve-activity.txt'
        split_apk_paths = 'evidence/package/split-apk-paths.txt'
        activities = 'evidence/package/activity-activities.txt'
        services = 'evidence/package/activity-services.txt'
        jobscheduler = 'evidence/measurements/jobscheduler-target-snippet.txt'
    }
}
Write-Utf8Text -Path (Join-Path $packageDir 'package-summary.json') -Value (($summary | ConvertTo-Json -Depth 6) + [Environment]::NewLine)

Write-Host "Collected package and device baseline for $PackageName on $script:DeviceSerial"
Write-Host "Package evidence: $packageDir"
Write-Host "Device environment: $(Join-Path $script:AuditRoot 'device\device-environment.json')"
