[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Serial)

. (Join-Path $PSScriptRoot 'Common.ps1')
$android = Initialize-AndroidEnvironment
Assert-ReplicaDevice -Serial $Serial -Adb $android.Adb
$apk = Join-Path $script:ProjectRoot 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "Release APK not found: $apk" }
$log = New-ValidationLog 'install-release'
Invoke-Checked -FilePath $android.Adb -Arguments @('-s', $Serial, 'install', '-r', $apk) -LogPath $log
Write-Host "Installed com.irlstreamer.reconstruction on $Serial"
