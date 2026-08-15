[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$ScreenId = '',
    [ValidateSet('', 'loading', 'empty', 'network_error', 'validation_error')][string]$State = '',
    [switch]$Release
)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
$package = if ($Release) { 'com.irlstreamer.reconstruction' } else { 'com.irlstreamer.reconstruction.debug' }
$component = "$package/com.irlstreamer.reconstruction.MainActivity"
$log = New-ValidationLog 'launch-replica'
Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'am', 'force-stop', $package) -LogPath $log
$arguments = @('-s', $Serial, 'shell', 'am', 'start', '-W', '-n', $component)
if (-not [string]::IsNullOrWhiteSpace($ScreenId)) { $arguments += @('--es', 'screen_id', $ScreenId) }
if (-not [string]::IsNullOrWhiteSpace($State)) { $arguments += @('--es', 'replica_state', $State) }
Invoke-Checked -FilePath $android.Adb -Arguments $arguments -LogPath $log
Write-Host "Launched $package on $Serial"
