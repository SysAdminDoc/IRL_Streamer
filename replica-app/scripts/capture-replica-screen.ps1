[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][string]$ScreenId,
    [int]$SettleMilliseconds = 900,
    [switch]$Warm
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
$remote = "/sdcard/irl_streamer_validation_$safeId.png"
$log = New-ValidationLog "capture-$safeId"

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
Write-Host $output
