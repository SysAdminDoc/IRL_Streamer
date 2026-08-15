[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [switch]$Release
)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
$package = if ($Release) { 'com.irlstreamer.reconstruction' } else { 'com.irlstreamer.reconstruction.debug' }
if ($package -notlike 'com.irlstreamer.reconstruction*') { throw 'Refusing to clear a non-replica package.' }
$log = New-ValidationLog 'reset-replica'
Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'shell', 'pm', 'clear', $package) -LogPath $log
Write-Host "Cleared only replica data for $package on $Serial"
