[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][string]$ScreenId,
    [int]$SettleMilliseconds = 900,
    [switch]$Warm,
    [switch]$SkipHierarchy
)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
$package = 'com.irlstreamer.reconstruction.debug'
$component = "$package/com.irlstreamer.reconstruction.MainActivity"
$safeId = [IO.Path]::GetFileNameWithoutExtension($ScreenId)
if ($safeId -ne $ScreenId -or $safeId -notmatch '^\d{3}_[A-Za-z0-9_-]+$') {
    throw "ScreenId must be a catalog ID such as 001_streamer_home_default: $ScreenId"
}
$current = Join-Path $script:ValidationRoot 'current'
New-Item -ItemType Directory -Path $current -Force | Out-Null
$output = Join-Path $current "$safeId.png"
$hierarchyDir = Join-Path $script:ValidationRoot 'hierarchy'
$hierarchyPath = Join-Path $hierarchyDir "$safeId.xml"
$resultPath = Join-Path $script:ValidationRoot "results\$safeId.json"
$remote = "/sdcard/irl_streamer_validation_$safeId.png"
$log = New-ValidationLog "capture-$safeId"

# Invalidate this state's artifacts before capturing anything. Without this a
# failed capture or a crashed compare leaves the previous run's screenshot,
# hierarchy dump and result JSON in place, and the sweep reports that stale
# result - PASS included - as though it belonged to this run. A state that can
# no longer be captured would stay green indefinitely.
foreach ($stale in @($output, $hierarchyPath, $resultPath)) {
    if (Test-Path -LiteralPath $stale -PathType Leaf) { Remove-Item -LiteralPath $stale -Force }
}

if (-not $Warm) {
    Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'am', 'force-stop', $package) -LogPath $log
}
$startArguments = @('-s', $Serial, 'shell', 'am', 'start', '-W', '-n', $component)
if ($Warm) { $startArguments += @('-f', '0x24000000') }
$startArguments += @('--es', 'screen_id', $safeId)
Invoke-Checked -FilePath $android.Adb -Arguments $startArguments -LogPath $log
Start-Sleep -Milliseconds $SettleMilliseconds
Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'screencap', '-p', $remote) -LogPath $log
Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'pull', $remote, $output) -LogPath $log
Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'rm', $remote) -LogPath $log
if (-not (Test-Path -LiteralPath $output -PathType Leaf)) { throw "Screenshot capture failed: $output" }

# Capture the replica UI hierarchy from the same state. SSIM says *that* a screen
# differs; only the hierarchy says *which element* moved, which is what the audit's
# 2 px layout-bounds target is measured against (scripts/geometry_diff.py).
if (-not $SkipHierarchy) {
    New-Item -ItemType Directory -Path $hierarchyDir -Force | Out-Null
    $remoteHierarchy = "/sdcard/irl_streamer_hierarchy_$safeId.xml"
    try {
        Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'uiautomator', 'dump', $remoteHierarchy) -LogPath $log
        Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'pull', $remoteHierarchy, $hierarchyPath) -LogPath $log
        Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'rm', $remoteHierarchy) -LogPath $log
    }
    catch {
        # A hierarchy dump can legitimately fail while an animation is in flight,
        # so this stays non-fatal - but it must leave no file behind. The geometry
        # gate can only report NO_REPLICA_DUMP when the dump is genuinely absent;
        # a leftover dump from a previous run would be scored as current.
        if (Test-Path -LiteralPath $hierarchyPath -PathType Leaf) { Remove-Item -LiteralPath $hierarchyPath -Force }
        Write-Warning "Hierarchy dump unavailable for ${safeId}: $($_.Exception.Message)"
    }
}
Write-Host $output
